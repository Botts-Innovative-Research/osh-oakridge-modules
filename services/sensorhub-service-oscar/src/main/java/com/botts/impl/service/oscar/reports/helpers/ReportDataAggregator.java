/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.service.oscar.reports.helpers;

import com.botts.impl.service.oscar.AdjudicationCodeConfig;
import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.vehicle.VehicleIncidentAggregator;
import com.botts.impl.system.lane.LaneSystem;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.ScalarComponent;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.module.IModule;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.Adjudication;
import org.sensorhub.impl.utils.rad.model.IsotopeAnalysis;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.model.OccupancyExtended;
import org.sensorhub.impl.utils.rad.model.WebIdAnalysis;

import java.io.IOException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Single-pass datastore aggregation engine for OSCAR reports.
 * Replaces repeated per-metric, per-day, and per-code full-store scans with single-pass stream scans
 * while preserving exact observation, fault, tamper, EML, isotope, and adjudication counts.
 */
public final class ReportDataAggregator {

    public static final DateTimeFormatter UTC_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    public static final DateTimeFormatter UTC_DATETIME_SHORT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public static final Map<String, String> STANDARD_ISOTOPES;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        // Special Nuclear Material & Actinides
        map.put("Np", "Neptunium");
        map.put("Pu", "Plutonium");
        map.put("U-233", "Uranium233");
        map.put("U-235", "Uranium235");
        map.put("U-238", "Uranium238");
        map.put("Am", "Americium");
        map.put("Cf", "Californium");
        // Industrial
        map.put("Ba", "Barium");
        map.put("Bi", "Bismuth");
        map.put("Co-57", "Cobalt57");
        map.put("Co-60", "Cobalt60");
        map.put("Cs-134", "Cesium134");
        map.put("Cs-137", "Cesium137");
        map.put("Eu", "Europium");
        map.put("Ir", "Iridium");
        map.put("Mn", "Manganese");
        map.put("Na", "Sodium");
        map.put("Se", "Selenium");
        map.put("Sr", "Strontium");
        // Medical
        map.put("F", "Fluorine");
        map.put("Ga", "Gallium");
        map.put("I-123", "Iodine123");
        map.put("I-131", "Iodine131");
        map.put("In", "Indium");
        map.put("Pd", "Palladium");
        map.put("Tc", "Technetium");
        map.put("Xe", "Xenon");
        // NORM & Unknown
        map.put("K", "Potassium");
        map.put("Ra", "Radium");
        map.put("Th", "Thorium");
        map.put("Unk", "Unknown");
        STANDARD_ISOTOPES = Collections.unmodifiableMap(map);
    }

    public record DispositionCodeSpec(int code, String label, String group) {}

    private static final List<DispositionCodeSpec> BUILTIN_DISPOSITION_CODES = List.of(
            new DispositionCodeSpec(0, "", "Other"),
            new DispositionCodeSpec(1, "Code 1: Contraband Found", "Real Alarm"),
            new DispositionCodeSpec(2, "Code 2: Other", "Real Alarm"),
            new DispositionCodeSpec(3, "Code 3: Medical Isotope Found", "Innocent Alarm"),
            new DispositionCodeSpec(4, "Code 4: NORM Found", "Innocent Alarm"),
            new DispositionCodeSpec(5, "Code 5: Declared Shipment of Radioactive Material", "Innocent Alarm"),
            new DispositionCodeSpec(6, "Code 6: Physical Inspection Negative", "False Alarm"),
            new DispositionCodeSpec(7, "Code 7: RIID/ASP Indicates Background Only", "False Alarm"),
            new DispositionCodeSpec(8, "Code 8: Other", "False Alarm"),
            new DispositionCodeSpec(9, "Code 9: Authorized Test, Maintenance, or Training Activity", "Test/Maintenance"),
            new DispositionCodeSpec(10, "Code 10: Unauthorized Activity", "Tamper/Fault"),
            new DispositionCodeSpec(11, "Code 11: Other", "Other")
    );

    public static final class ObservationMetrics {
        public long gammaAlarm;
        public long neutronAlarm;
        public long gammaNeutronAlarm;
        public long emlSuppressed;
        public long totalOccupancies;
        public long tamper;
        public long gammaHighFault;
        public long gammaLowFault;
        public long neutronHighFault;
        public long extendedOccupancyFault;

        public long totalAlarms() {
            return gammaAlarm + neutronAlarm + gammaNeutronAlarm;
        }

        public long totalFaultsAndTamper() {
            return tamper + gammaHighFault + gammaLowFault + neutronHighFault + extendedOccupancyFault;
        }

        public double alarmOccupancyRatePct() {
            return Utils.calculateAlarmingOccRatePct(totalAlarms(), totalOccupancies);
        }

        public double emlAlarmRatePct() {
            return Utils.calcEMLAlarmRatePct(emlSuppressed, totalAlarms());
        }

        public void add(ObservationMetrics other) {
            if (other == null) {
                return;
            }
            this.gammaAlarm += other.gammaAlarm;
            this.neutronAlarm += other.neutronAlarm;
            this.gammaNeutronAlarm += other.gammaNeutronAlarm;
            this.emlSuppressed += other.emlSuppressed;
            this.totalOccupancies += other.totalOccupancies;
            this.tamper += other.tamper;
            this.gammaHighFault += other.gammaHighFault;
            this.gammaLowFault += other.gammaLowFault;
            this.neutronHighFault += other.neutronHighFault;
            this.extendedOccupancyFault += other.extendedOccupancyFault;
        }
    }

    public record AdjudicationDetailRecord(
            String laneUid,
            int sequenceNumber,
            String timeUtcIso,
            String timeUtcDisplay,
            String username,
            int adjudicationCode,
            String dispositionLabel,
            String dispositionGroup,
            List<String> isotopes,
            String secondaryInspectionStatus,
            String vehicleId,
            String occupancyObsId,
            String operatorFeedback,
            String formattedSummary,
            String emlDecision,
            String emlSourceType,
            Double emlNormProbPct,
            Double emlNonEmittingProbPct,
            Double emlThreatProbPct,
            Integer emlVehicleClass,
            Double emlVehicleLengthMeters,
            List<String> filePaths
    ) {}

    public record AdjudicationLaneSummary(
            String laneUid,
            long totalRecords,
            List<DispositionCodeSpec> dispositionSpecs,
            Map<Integer, Long> countsByCode,
            Map<String, Long> isotopeCountsBySymbol,
            List<Map<String, String>> detailRows,
            List<AdjudicationDetailRecord> structuredRecords
    ) {}

    private record OccupancyIndices(int gammaIdx, int neutronIdx, int adjCountIdx) {}

    private ReportDataAggregator() {}

    public static List<String> resolveTargetLaneUIDs(OSCARServiceModule module, String laneUIDs) {
        String[] parsed = Utils.parseLaneUIDs(laneUIDs);
        if (parsed.length > 0) {
            return List.of(parsed);
        }
        List<String> discovered = new ArrayList<>();
        if (module != null && module.getParentHub() != null && module.getParentHub().getModuleRegistry() != null) {
            for (IModule<?> mod : module.getParentHub().getModuleRegistry().getLoadedModules()) {
                if (mod instanceof LaneSystem lane && lane.getUniqueIdentifier() != null && !lane.getUniqueIdentifier().isBlank()) {
                    discovered.add(lane.getUniqueIdentifier());
                }
            }
        }
        return discovered;
    }

    /**
     * Resolves human-readable lane labels alongside UIDs while always preserving the full UID text.
     */
    public static Map<String, String> resolveLaneDisplayLabels(OSCARServiceModule module, Iterable<String> laneUIDs, boolean multiline) {
        Map<String, String> uidToName = new HashMap<>();
        if (module != null && module.getParentHub() != null && module.getParentHub().getModuleRegistry() != null) {
            for (IModule<?> mod : module.getParentHub().getModuleRegistry().getLoadedModules()) {
                if (mod instanceof LaneSystem lane && lane.getUniqueIdentifier() != null) {
                    String name = lane.getName();
                    if (name != null && !name.isBlank()) {
                        uidToName.put(lane.getUniqueIdentifier(), name.trim());
                    }
                }
            }
        }

        Map<String, String> labels = new LinkedHashMap<>();
        if (laneUIDs != null) {
            for (String uid : laneUIDs) {
                if (uid == null || uid.isBlank()) {
                    continue;
                }
                String trimmed = uid.trim();
                String friendly = uidToName.get(trimmed);
                if (friendly != null && !friendly.equalsIgnoreCase(trimmed)) {
                    labels.put(trimmed, multiline ? (friendly + "\n" + trimmed) : (friendly + " (" + trimmed + ")"));
                } else {
                    labels.put(trimmed, trimmed);
                }
            }
        }
        return labels;
    }

    public static String buildBannerScopeSummary(OSCARServiceModule module, String laneUIDs) {
        String[] scoped = Utils.parseLaneUIDs(laneUIDs);
        if (scoped.length == 0) {
            List<String> allConfigured = resolveTargetLaneUIDs(module, null);
            if (allConfigured.isEmpty()) {
                return "Scope: All Site Lanes";
            }
            return "Scope: All Configured Site Lanes (" + allConfigured.size()
                    + (allConfigured.size() == 1 ? " lane)" : " lanes)");
        }
        Map<String, String> labels = resolveLaneDisplayLabels(module, List.of(scoped), false);
        List<String> items = new ArrayList<>(labels.values());
        if (items.size() <= 4) {
            return "Lanes: " + String.join(", ", items);
        }
        List<String> head = items.subList(0, 4);
        return "Lanes (" + items.size() + " selected): " + String.join(", ", head)
                + " (+" + (items.size() - 4) + " additional lanes)";
    }

    /**
     * Single-pass site/scope metrics aggregation (used by RDS Site Report).
     */
    public static ObservationMetrics aggregateScopeMetrics(
            OSCARServiceModule module,
            String laneUIDs,
            Instant start,
            Instant end
    ) {
        ObservationMetrics metrics = new ObservationMetrics();
        IObsSystemDatabase db = getDatabase(module);
        if (db == null) {
            return metrics;
        }
        String[] scopedLanes = Utils.parseLaneUIDs(laneUIDs);

        // 1. Occupancy indexed total count + single pass over alarming occupancies only
        DataStreamFilter occDsFilter = buildScopedDataStreamFilter(scopedLanes, RADHelper.DEF_OCCUPANCY);
        metrics.totalOccupancies = countOccupancies(module, db, new ObsFilter.Builder()
                .withResultTimeDuring(start, end)
                .withDataStreams(occDsFilter)
                .build());

        Map<BigId, OccupancyIndices> occIndicesCache = new HashMap<>();
        try (Stream<IObsData> stream = db.getObservationStore().select(new ObsFilter.Builder()
                .withResultTimeDuring(start, end)
                .withDataStreams(occDsFilter)
                .withCQLFilter(Utils.anyOccupancyAlarmCQL)
                .build())) {
            stream.forEach(obs -> {
                if (obs == null || obs.getResult() == null) {
                    return;
                }
                OccupancyIndices idx = occIndicesCache.computeIfAbsent(
                        obs.getDataStreamID(), dsId -> resolveOccupancyIndices(db, dsId));
                DataBlock block = obs.getResult();
                boolean g = safeBoolean(block, idx.gammaIdx());
                boolean n = safeBoolean(block, idx.neutronIdx());
                if (g && n) {
                    metrics.gammaNeutronAlarm++;
                } else if (g) {
                    metrics.gammaAlarm++;
                } else if (n) {
                    metrics.neutronAlarm++;
                }
                if ((g || n) && hasEmlNonThreatMarker(block, idx)) {
                    metrics.emlSuppressed++;
                }
            });
        }

        // 2. Fallback for standalone legacy DEF_EML_ANALYSIS streams when occupancy markers are absent
        if (metrics.emlSuppressed == 0L) {
            DataStreamFilter emlDsFilter = buildScopedDataStreamFilter(scopedLanes, RADHelper.DEF_EML_ANALYSIS);
            metrics.emlSuppressed = db.getObservationStore().countMatchingEntries(new ObsFilter.Builder()
                    .withResultTimeDuring(start, end)
                    .withDataStreams(emlDsFilter)
                    .withCQLFilter(Utils.emlSuppressedCQL)
                    .build());
        }

        // 3. Tamper single pass with CQL predicate pushdown
        DataStreamFilter tamperDsFilter = buildScopedDataStreamFilter(scopedLanes, RADHelper.DEF_TAMPER);
        metrics.tamper = db.getObservationStore().countMatchingEntries(new ObsFilter.Builder()
                .withResultTimeDuring(start, end)
                .withDataStreams(tamperDsFilter)
                .withCQLFilter(Utils.tamperCQL)
                .build());

        // 4. Gamma, Neutron & Extended Occupancy faults single pass with CQL pushdown
        DataStreamFilter faultDsFilter = buildScopedDataStreamFilter(
                scopedLanes,
                RADHelper.DEF_GAMMA,
                RADHelper.DEF_NEUTRON,
                RADHelper.DEF_ALARM,
                RADHelper.DEF_EXTENDED_OCCUPANCY_FAULT,
                RADHelper.DEF_EXTENDED_OCCUPANCY_ALARM_STATE);
        Map<BigId, Integer> alarmIdxCache = new HashMap<>();
        try (Stream<IObsData> stream = db.getObservationStore().select(new ObsFilter.Builder()
                .withResultTimeDuring(start, end)
                .withDataStreams(faultDsFilter)
                .withCQLFilter(Utils.faultCQL)
                .build())) {
            stream.forEach(obs -> {
                if (obs == null || obs.getResult() == null) {
                    return;
                }
                int alarmIdx = alarmIdxCache.computeIfAbsent(
                        obs.getDataStreamID(), dsId -> resolveAlarmStateIndex(db, dsId));
                String state = safeString(obs.getResult(), alarmIdx);
                classifyFaultState(state, metrics);
            });
        }

        return metrics;
    }

    /**
     * Single-pass per-lane metrics aggregation across all requested lanes (used by Lane Report).
     */
    public static Map<String, ObservationMetrics> aggregateMetricsByLane(
            OSCARServiceModule module,
            List<String> targetLanes,
            Instant start,
            Instant end
    ) {
        Map<String, ObservationMetrics> perLane = new LinkedHashMap<>();
        if (targetLanes == null || targetLanes.isEmpty()) {
            return perLane;
        }
        for (String laneUid : targetLanes) {
            perLane.put(laneUid, new ObservationMetrics());
        }

        IObsSystemDatabase db = getDatabase(module);
        if (db == null) {
            return perLane;
        }

        Map<BigId, String> occStreamToLane = new HashMap<>();
        Map<BigId, OccupancyIndices> occIndices = new HashMap<>();
        Map<BigId, String> emlStreamToLane = new HashMap<>();
        Map<BigId, Integer> emlResultIdx = new HashMap<>();
        Map<BigId, String> tamperStreamToLane = new HashMap<>();
        Map<BigId, Integer> tamperStatusIdx = new HashMap<>();
        Map<BigId, String> faultStreamToLane = new HashMap<>();
        Map<BigId, Integer> faultStateIdx = new HashMap<>();

        for (String laneUid : targetLanes) {
            DataStreamFilter laneDsFilter = new DataStreamFilter.Builder()
                    .withSystems().withUniqueIDs(laneUid).includeMembers(true).done()
                    .withObservedProperties(
                            RADHelper.DEF_OCCUPANCY,
                            RADHelper.DEF_EML_ANALYSIS,
                            RADHelper.DEF_TAMPER,
                            RADHelper.DEF_GAMMA,
                            RADHelper.DEF_NEUTRON,
                            RADHelper.DEF_ALARM,
                            RADHelper.DEF_EXTENDED_OCCUPANCY_FAULT,
                            RADHelper.DEF_EXTENDED_OCCUPANCY_ALARM_STATE
                    )
                    .build();

            try (var entries = db.getDataStreamStore().selectEntries(laneDsFilter)) {
                entries.forEach(entry -> {
                    BigId dsId = entry.getKey().getInternalID();
                    IDataStreamInfo dsInfo = entry.getValue();
                    DataComponent struct = dsInfo != null ? dsInfo.getRecordStructure() : null;
                    if (containsDefinition(struct, RADHelper.DEF_OCCUPANCY)) {
                        occStreamToLane.put(dsId, laneUid);
                        occIndices.put(dsId, extractOccupancyIndices(struct));
                    }
                    if (containsDefinition(struct, RADHelper.DEF_EML_ANALYSIS)) {
                        emlStreamToLane.put(dsId, laneUid);
                        emlResultIdx.put(dsId, findLeadingScalarIndex(struct, c -> "result".equalsIgnoreCase(c.getName()), 0));
                    }
                    if (containsDefinition(struct, RADHelper.DEF_TAMPER)) {
                        tamperStreamToLane.put(dsId, laneUid);
                        tamperStatusIdx.put(dsId, findLeadingScalarIndex(
                                struct, c -> RADHelper.DEF_TAMPER.equalsIgnoreCase(c.getDefinition()) || "tamperStatus".equalsIgnoreCase(c.getName()), 1));
                    }
                    if (containsDefinition(struct, RADHelper.DEF_GAMMA)
                            || containsDefinition(struct, RADHelper.DEF_NEUTRON)
                            || containsDefinition(struct, RADHelper.DEF_ALARM)
                            || containsDefinition(struct, RADHelper.DEF_EXTENDED_OCCUPANCY_FAULT)
                            || containsDefinition(struct, RADHelper.DEF_EXTENDED_OCCUPANCY_ALARM_STATE)) {
                        faultStreamToLane.put(dsId, laneUid);
                        faultStateIdx.put(dsId, extractAlarmStateIndex(struct));
                    }
                });
            }
        }

        // 1. Occupancy indexed per-stream totals + single pass over alarming occupancies across all lanes
        if (!occStreamToLane.isEmpty()) {
            for (Map.Entry<BigId, String> occDs : occStreamToLane.entrySet()) {
                ObservationMetrics m = perLane.get(occDs.getValue());
                if (m != null) {
                    m.totalOccupancies += countOccupancies(module, db, new ObsFilter.Builder()
                            .withResultTimeDuring(start, end)
                            .withDataStreams(occDs.getKey())
                            .build());
                }
            }

            try (Stream<IObsData> stream = db.getObservationStore().select(new ObsFilter.Builder()
                    .withResultTimeDuring(start, end)
                    .withDataStreams(occStreamToLane.keySet())
                    .withCQLFilter(Utils.anyOccupancyAlarmCQL)
                    .build())) {
                stream.forEach(obs -> {
                    if (obs == null || obs.getResult() == null) {
                        return;
                    }
                    String laneUid = occStreamToLane.get(obs.getDataStreamID());
                    ObservationMetrics m = perLane.get(laneUid);
                    if (m == null) {
                        return;
                    }
                    OccupancyIndices idx = occIndices.getOrDefault(obs.getDataStreamID(), new OccupancyIndices(5, 6, 9));
                    DataBlock block = obs.getResult();
                    boolean g = safeBoolean(block, idx.gammaIdx());
                    boolean n = safeBoolean(block, idx.neutronIdx());
                    if (g && n) {
                        m.gammaNeutronAlarm++;
                    } else if (g) {
                        m.gammaAlarm++;
                    } else if (n) {
                        m.neutronAlarm++;
                    }
                    if ((g || n) && hasEmlNonThreatMarker(block, idx)) {
                        m.emlSuppressed++;
                    }
                });
            }
        }

        // 2. Legacy EML Suppressed fallback across lanes that have standalone DEF_EML_ANALYSIS streams
        if (!emlStreamToLane.isEmpty()) {
            Set<String> lanesAlreadyWithOccEml = new java.util.HashSet<>();
            for (Map.Entry<String, ObservationMetrics> e : perLane.entrySet()) {
                if (e.getValue().emlSuppressed > 0L) {
                    lanesAlreadyWithOccEml.add(e.getKey());
                }
            }
            try (Stream<IObsData> stream = db.getObservationStore().select(new ObsFilter.Builder()
                    .withResultTimeDuring(start, end)
                    .withDataStreams(emlStreamToLane.keySet())
                    .withCQLFilter(Utils.emlSuppressedCQL)
                    .build())) {
                stream.forEach(obs -> {
                    if (obs == null || obs.getResult() == null) {
                        return;
                    }
                    String laneUid = emlStreamToLane.get(obs.getDataStreamID());
                    if (lanesAlreadyWithOccEml.contains(laneUid)) {
                        return;
                    }
                    ObservationMetrics m = perLane.get(laneUid);
                    if (m != null) {
                        int rIdx = emlResultIdx.getOrDefault(obs.getDataStreamID(), 0);
                        String res = safeString(obs.getResult(), rIdx);
                        if ("RELEASE".equalsIgnoreCase(res) || "1".equals(res)) {
                            m.emlSuppressed++;
                        }
                    }
                });
            }
        }

        // 3. Tamper pass across all target lanes combined
        if (!tamperStreamToLane.isEmpty()) {
            try (Stream<IObsData> stream = db.getObservationStore().select(new ObsFilter.Builder()
                    .withResultTimeDuring(start, end)
                    .withDataStreams(tamperStreamToLane.keySet())
                    .withCQLFilter(Utils.tamperCQL)
                    .build())) {
                stream.forEach(obs -> {
                    if (obs == null || obs.getResult() == null) {
                        return;
                    }
                    String laneUid = tamperStreamToLane.get(obs.getDataStreamID());
                    ObservationMetrics m = perLane.get(laneUid);
                    if (m != null) {
                        int tIdx = tamperStatusIdx.getOrDefault(obs.getDataStreamID(), 1);
                        if (safeBoolean(obs.getResult(), tIdx)) {
                            m.tamper++;
                        }
                    }
                });
            }
        }

        // 4. Fault pass across all target lanes combined
        if (!faultStreamToLane.isEmpty()) {
            try (Stream<IObsData> stream = db.getObservationStore().select(new ObsFilter.Builder()
                    .withResultTimeDuring(start, end)
                    .withDataStreams(faultStreamToLane.keySet())
                    .withCQLFilter(Utils.faultCQL)
                    .build())) {
                stream.forEach(obs -> {
                    if (obs == null || obs.getResult() == null) {
                        return;
                    }
                    String laneUid = faultStreamToLane.get(obs.getDataStreamID());
                    ObservationMetrics m = perLane.get(laneUid);
                    if (m != null) {
                        int aIdx = faultStateIdx.getOrDefault(obs.getDataStreamID(), 1);
                        String state = safeString(obs.getResult(), aIdx);
                        classifyFaultState(state, m);
                    }
                });
            }
        }

        return perLane;
    }

    /**
     * Single-pass daily UTC calendar-day aggregation for Event Report (ALARMS, ALARMS_OCCUPANCIES, SOH).
     */
    public static Map<String, ObservationMetrics> aggregateDailyEventMetrics(
            OSCARServiceModule module,
            String laneUIDs,
            Instant start,
            Instant end,
            EventReportType eventType
    ) {
        Map<LocalDate, ObservationMetrics> byUtcDate = seedUtcDayBuckets(start, end);
        IObsSystemDatabase db = getDatabase(module);
        if (db != null) {
            String[] scopedLanes = Utils.parseLaneUIDs(laneUIDs);
            if (eventType == EventReportType.ALARMS || eventType == EventReportType.ALARMS_OCCUPANCIES) {
                boolean includeAllOccupancies = (eventType == EventReportType.ALARMS_OCCUPANCIES);
                DataStreamFilter occDsFilter = buildScopedDataStreamFilter(scopedLanes, RADHelper.DEF_OCCUPANCY);

                if (includeAllOccupancies && start != null && end != null) {
                    for (Map.Entry<LocalDate, ObservationMetrics> dayEntry : byUtcDate.entrySet()) {
                        LocalDate day = dayEntry.getKey();
                        Instant bucketMin = day.atStartOfDay(ZoneOffset.UTC).toInstant();
                        if (bucketMin.isBefore(start)) {
                            bucketMin = start;
                        }
                        Instant bucketMax = day.plusDays(1L).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1L);
                        if (bucketMax.isAfter(end)) {
                            bucketMax = end;
                        }
                        if (!bucketMin.isAfter(bucketMax)) {
                            dayEntry.getValue().totalOccupancies += countOccupancies(
                                    module,
                                    db,
                                    new ObsFilter.Builder()
                                            .withResultTimeDuring(bucketMin, bucketMax)
                                            .withDataStreams(occDsFilter)
                                            .build()
                            );
                        }
                    }
                    LocalDate endDay = LocalDate.ofInstant(end, ZoneOffset.UTC);
                    if (end.isAfter(start) && end.equals(endDay.atStartOfDay(ZoneOffset.UTC).toInstant())) {
                        long exactBoundaryCount = countOccupancies(
                                module,
                                db,
                                new ObsFilter.Builder()
                                        .withResultTimeDuring(end, end)
                                        .withDataStreams(occDsFilter)
                                        .build()
                        );
                        if (exactBoundaryCount > 0L) {
                            byUtcDate.computeIfAbsent(endDay, d -> new ObservationMetrics()).totalOccupancies += exactBoundaryCount;
                        }
                    }
                }

                long occEmlSuppressedTotal = 0L;
                long[] occEmlHolder = {0L};
                Map<BigId, OccupancyIndices> occIndicesCache = new HashMap<>();
                try (Stream<IObsData> stream = db.getObservationStore().select(new ObsFilter.Builder()
                        .withResultTimeDuring(start, end)
                        .withDataStreams(occDsFilter)
                        .withCQLFilter(Utils.anyOccupancyAlarmCQL)
                        .build())) {
                    stream.forEach(obs -> {
                        if (obs == null || obs.getResult() == null || obs.getResultTime() == null) {
                            return;
                        }
                        LocalDate day = LocalDate.ofInstant(obs.getResultTime(), ZoneOffset.UTC);
                        ObservationMetrics m = byUtcDate.computeIfAbsent(day, d -> new ObservationMetrics());
                        OccupancyIndices idx = occIndicesCache.computeIfAbsent(
                                obs.getDataStreamID(), dsId -> resolveOccupancyIndices(db, dsId));
                        DataBlock block = obs.getResult();
                        boolean g = safeBoolean(block, idx.gammaIdx());
                        boolean n = safeBoolean(block, idx.neutronIdx());
                        if (g && n) {
                            m.gammaNeutronAlarm++;
                        } else if (g) {
                            m.gammaAlarm++;
                        } else if (n) {
                            m.neutronAlarm++;
                        }
                        if ((g || n) && hasEmlNonThreatMarker(block, idx)) {
                            m.emlSuppressed++;
                            occEmlHolder[0]++;
                        }
                    });
                }
                occEmlSuppressedTotal = occEmlHolder[0];

                if (occEmlSuppressedTotal == 0L) {
                    DataStreamFilter emlDsFilter = buildScopedDataStreamFilter(scopedLanes, RADHelper.DEF_EML_ANALYSIS);
                    Map<BigId, Integer> emlIdxCache = new HashMap<>();
                    try (Stream<IObsData> stream = db.getObservationStore().select(new ObsFilter.Builder()
                            .withResultTimeDuring(start, end)
                            .withDataStreams(emlDsFilter)
                            .withCQLFilter(Utils.emlSuppressedCQL)
                            .build())) {
                        stream.forEach(obs -> {
                            if (obs == null || obs.getResult() == null || obs.getResultTime() == null) {
                                return;
                            }
                            int rIdx = emlIdxCache.computeIfAbsent(obs.getDataStreamID(), dsId -> {
                                DataComponent s = getRecordStructure(db, dsId);
                                return findLeadingScalarIndex(s, c -> "result".equalsIgnoreCase(c.getName()), 0);
                            });
                            String res = safeString(obs.getResult(), rIdx);
                            if ("RELEASE".equalsIgnoreCase(res) || "1".equals(res)) {
                                LocalDate day = LocalDate.ofInstant(obs.getResultTime(), ZoneOffset.UTC);
                                byUtcDate.computeIfAbsent(day, d -> new ObservationMetrics()).emlSuppressed++;
                            }
                        });
                    }
                }
            } else if (eventType == EventReportType.SOH) {
                DataStreamFilter faultDsFilter = buildScopedDataStreamFilter(
                        scopedLanes,
                        RADHelper.DEF_GAMMA,
                        RADHelper.DEF_NEUTRON,
                        RADHelper.DEF_ALARM,
                        RADHelper.DEF_EXTENDED_OCCUPANCY_FAULT,
                        RADHelper.DEF_EXTENDED_OCCUPANCY_ALARM_STATE);
                Map<BigId, Integer> alarmIdxCache = new HashMap<>();
                try (Stream<IObsData> stream = db.getObservationStore().select(new ObsFilter.Builder()
                        .withResultTimeDuring(start, end)
                        .withDataStreams(faultDsFilter)
                        .withCQLFilter(Utils.faultCQL)
                        .build())) {
                    stream.forEach(obs -> {
                        if (obs == null || obs.getResult() == null || obs.getResultTime() == null) {
                            return;
                        }
                        int aIdx = alarmIdxCache.computeIfAbsent(
                                obs.getDataStreamID(), dsId -> resolveAlarmStateIndex(db, dsId));
                        String state = safeString(obs.getResult(), aIdx);
                        LocalDate day = LocalDate.ofInstant(obs.getResultTime(), ZoneOffset.UTC);
                        classifyFaultState(state, byUtcDate.computeIfAbsent(day, d -> new ObservationMetrics()));
                    });
                }

                DataStreamFilter tamperDsFilter = buildScopedDataStreamFilter(scopedLanes, RADHelper.DEF_TAMPER);
                Map<BigId, Integer> tamperIdxCache = new HashMap<>();
                try (Stream<IObsData> stream = db.getObservationStore().select(new ObsFilter.Builder()
                        .withResultTimeDuring(start, end)
                        .withDataStreams(tamperDsFilter)
                        .withCQLFilter(Utils.tamperCQL)
                        .build())) {
                    stream.forEach(obs -> {
                        if (obs == null || obs.getResult() == null || obs.getResultTime() == null) {
                            return;
                        }
                        int tIdx = tamperIdxCache.computeIfAbsent(obs.getDataStreamID(), dsId -> {
                            DataComponent s = getRecordStructure(db, dsId);
                            return findLeadingScalarIndex(
                                    s, c -> RADHelper.DEF_TAMPER.equalsIgnoreCase(c.getDefinition()) || "tamperStatus".equalsIgnoreCase(c.getName()), 1);
                        });
                        if (safeBoolean(obs.getResult(), tIdx)) {
                            LocalDate day = LocalDate.ofInstant(obs.getResultTime(), ZoneOffset.UTC);
                            byUtcDate.computeIfAbsent(day, d -> new ObservationMetrics()).tamper++;
                        }
                    });
                }
            }
        }

        // Convert to chronological yyyy-MM-dd LinkedHashMap
        List<LocalDate> sortedDays = new ArrayList<>(byUtcDate.keySet());
        Collections.sort(sortedDays);
        Map<String, ObservationMetrics> out = new LinkedHashMap<>(sortedDays.size());
        for (LocalDate d : sortedDays) {
            out.put(d.toString(), byUtcDate.get(d));
        }
        return out;
    }

    /**
     * Single-pass CommandStatusStore aggregation per lane for Adjudication Report.
     * Includes built-in codes (0-11), configured custom disposition codes (>11), and automated EML Non-Threat (9999).
     */
    public static AdjudicationLaneSummary aggregateAdjudicationsForLane(
            OSCARServiceModule module,
            String laneUid,
            Instant start,
            Instant end
    ) {
        Map<Integer, DispositionCodeSpec> specByCode = new LinkedHashMap<>();
        for (DispositionCodeSpec builtin : BUILTIN_DISPOSITION_CODES) {
            specByCode.put(builtin.code(), builtin);
        }

        // Include administrator-configured custom disposition codes (> 11)
        if (module != null && module.getConfiguration() != null && module.getConfiguration().customAdjudicationCodes != null) {
            List<AdjudicationCodeConfig> customSorted = new ArrayList<>(module.getConfiguration().customAdjudicationCodes);
            customSorted.removeIf(Objects::isNull);
            customSorted.sort(Comparator.comparingInt(c -> c.code));
            for (AdjudicationCodeConfig custom : customSorted) {
                if (custom.code > 11
                        && custom.code != Adjudication.EXTENDED_OCCUPANCY_GAMMA_CODE
                        && custom.code != Adjudication.EML_NON_THREAT_CODE
                        && custom.label != null
                        && !custom.label.isBlank()) {
                    String fullLabel = custom.label.trim().startsWith("Code ")
                            ? custom.label.trim()
                            : ("Code " + custom.code + ": " + custom.label.trim());
                    specByCode.put(custom.code, new DispositionCodeSpec(custom.code, fullLabel, "Custom"));
                }
            }
        }

        specByCode.put(
                Adjudication.EXTENDED_OCCUPANCY_GAMMA_CODE,
                new DispositionCodeSpec(Adjudication.EXTENDED_OCCUPANCY_GAMMA_CODE, "Extended Occupancy Gamma Alarm", "Automated")
        );
        specByCode.put(
                Adjudication.EML_NON_THREAT_CODE,
                new DispositionCodeSpec(Adjudication.EML_NON_THREAT_CODE, "EML Non-Threat", "Automated")
        );

        Map<Integer, Long> countsByCode = new LinkedHashMap<>();
        for (Integer code : specByCode.keySet()) {
            countsByCode.put(code, 0L);
        }

        Map<String, Long> isotopeCounts = new LinkedHashMap<>();
        for (String symbol : STANDARD_ISOTOPES.keySet()) {
            isotopeCounts.put(symbol, 0L);
        }

        List<Map<String, String>> detailRows = new ArrayList<>();
        List<AdjudicationDetailRecord> structuredRecords = new ArrayList<>();
        long totalRecords = 0L;

        if (module != null && module.getParentHub() != null && laneUid != null && !laneUid.isBlank()) {
            Iterator<ICommandStatus> it = Utils.queryCommandStatus(module, laneUid, start, end);
            int seq = 1;
            while (it.hasNext()) {
                ICommandStatus entry = it.next();
                if (entry == null || entry.getResult() == null || entry.getResult().getInlineRecords() == null) {
                    continue;
                }
                List<DataBlock> blocks = entry.getResult().getInlineRecords().stream().toList();
                if (blocks.isEmpty() || blocks.get(0) == null) {
                    continue;
                }

                DataBlock block = blocks.get(0);
                try {
                    int index = 0;
                    String username = nonNull(block.getStringValue(index++));
                    String feedback = nonNull(block.getStringValue(index++));
                    int code = block.getIntValue(index++);
                    int isotopeCount = Math.max(0, block.getIntValue(index++));

                    List<String> recordIsotopes = new ArrayList<>(isotopeCount);
                    for (int i = 0; i < isotopeCount; i++) {
                        String iso = nonNull(block.getStringValue(index++));
                        if (!iso.isBlank()) {
                            recordIsotopes.add(iso);
                        }
                    }

                    String secondaryStatus = nonNull(block.getStringValue(index++));
                    int fileCount = Math.max(0, block.getIntValue(index++));
                    List<String> filePaths = new ArrayList<>(fileCount);
                    for (int i = 0; i < fileCount; i++) {
                        String fp = nonNull(block.getStringValue(index++));
                        if (!fp.isBlank()) {
                            filePaths.add(fp);
                        }
                    }

                    String occupancyObsId = nonNull(block.getStringValue(index++));
                    String vehicleId = nonNull(block.getStringValue(index));

                    totalRecords++;

                    if (!specByCode.containsKey(code)) {
                        String dynamicLabel = "Code " + code + ": Custom Disposition";
                        specByCode.put(code, new DispositionCodeSpec(code, dynamicLabel, "Custom"));
                    }
                    countsByCode.merge(code, 1L, Long::sum);

                    for (String recordedIso : recordIsotopes) {
                        String normalizedKey = normalizeIsotopeSymbol(recordedIso);
                        if (!normalizedKey.isEmpty()) {
                            isotopeCounts.merge(normalizedKey, 1L, Long::sum);
                        }
                    }

                    DispositionCodeSpec matchedSpec = specByCode.get(code);
                    String formattedCode = (matchedSpec != null && !matchedSpec.label().isBlank())
                            ? ((code == Adjudication.EML_NON_THREAT_CODE || code == Adjudication.EXTENDED_OCCUPANCY_GAMMA_CODE)
                                ? (code + " (" + matchedSpec.label() + ")")
                                : matchedSpec.label())
                            : String.valueOf(code);

                    String reportTimeStr = entry.getReportTime() != null
                            ? UTC_DATETIME_SHORT_FMT.format(entry.getReportTime())
                            : "-";
                    String reportTimeIso = entry.getReportTime() != null
                            ? DateTimeFormatter.ISO_INSTANT.format(entry.getReportTime())
                            : "";

                    String formattedIsotopes = recordIsotopes.isEmpty()
                            ? "None (0)"
                            : String.join(", ", recordIsotopes) + " (" + isotopeCount + ")";
                    String formattedFiles = filePaths.isEmpty()
                            ? "None (0)"
                            : String.join(", ", filePaths) + " (" + fileCount + ")";

                    int currentSeq = seq++;
                    ParsedEmlFeedback parsedEml = parseEmlFeedback(feedback);

                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("#", String.valueOf(currentSeq));
                    row.put("Time (UTC)", reportTimeStr);
                    row.put("Username", username.isEmpty() ? "-" : username);
                    row.put("Feedback", feedback);
                    row.put("Adjudication Code", formattedCode);
                    row.put("Isotope Count", String.valueOf(isotopeCount));
                    row.put("Isotopes", formattedIsotopes);
                    row.put("Secondary Inspection Status", secondaryStatus.isEmpty() ? "-" : secondaryStatus);
                    row.put("File Path Count", String.valueOf(fileCount));
                    row.put("File Paths", formattedFiles);
                    row.put("Occupancy Obs ID", occupancyObsId.isEmpty() ? "-" : occupancyObsId);
                    row.put("Vehicle ID", vehicleId.isEmpty() ? "-" : vehicleId);
                    detailRows.add(row);

                    structuredRecords.add(new AdjudicationDetailRecord(
                            laneUid,
                            currentSeq,
                            reportTimeIso,
                            reportTimeStr,
                            username.isEmpty() ? "-" : username,
                            code,
                            formattedCode,
                            matchedSpec != null ? matchedSpec.group() : "Other",
                            List.copyOf(recordIsotopes),
                            secondaryStatus.isEmpty() ? "-" : secondaryStatus,
                            vehicleId.isEmpty() ? "-" : vehicleId,
                            occupancyObsId.isEmpty() ? "-" : occupancyObsId,
                            parsedEml.operatorNotes(),
                            TableGenerator.formatAdjudicationFeedback(feedback),
                            parsedEml.emlDecision(),
                            parsedEml.emlSourceType(),
                            parsedEml.normProbPct(),
                            parsedEml.nonEmittingProbPct(),
                            parsedEml.threatProbPct(),
                            parsedEml.vehicleClass(),
                            parsedEml.vehicleLengthMeters(),
                            List.copyOf(filePaths)
                    ));
                } catch (Exception ignored) {
                    // Skip malformed command status records safely without aborting the report
                }
            }
        }

        return new AdjudicationLaneSummary(
                laneUid,
                totalRecords,
                new ArrayList<>(specByCode.values()),
                countsByCode,
                isotopeCounts,
                detailRows,
                structuredRecords
        );
    }

    private record ParsedEmlFeedback(
            String operatorNotes,
            String emlDecision,
            String emlSourceType,
            Double normProbPct,
            Double nonEmittingProbPct,
            Double threatProbPct,
            Integer vehicleClass,
            Double vehicleLengthMeters
    ) {}

    private static ParsedEmlFeedback parseEmlFeedback(String rawFeedback) {
        if (rawFeedback == null || rawFeedback.isBlank()) {
            return new ParsedEmlFeedback("", "", "", null, null, null, null, null);
        }
        int markerIdx = rawFeedback.indexOf(Adjudication.EML_METADATA_PREFIX);
        if (markerIdx < 0) {
            return new ParsedEmlFeedback(rawFeedback.trim(), "", "", null, null, null, null, null);
        }
        String notes = rawFeedback.substring(0, markerIdx).trim();
        String jsonPart = rawFeedback.substring(markerIdx + Adjudication.EML_METADATA_PREFIX.length()).trim();
        try {
            JsonElement parsed = JsonParser.parseString(jsonPart);
            if (!parsed.isJsonObject()) {
                return new ParsedEmlFeedback(notes, "", "", null, null, null, null, null);
            }
            JsonObject obj = parsed.getAsJsonObject();
            String decision = getJsonString(obj, "result");
            if (decision.isEmpty()) {
                decision = getJsonString(obj, "emlDecision");
            }
            String sourceType = getJsonString(obj, "overallSourceType");
            Double pNorm = getJsonPct(obj, "probabilityNORM");
            Double pNonEmit = getJsonPct(obj, "probabilityNonEmitting");
            Double pThreat = getJsonPct(obj, "probabilityThreat");
            Integer vClass = getJsonInt(obj, "vehicleClass");
            Double vLen = getJsonDouble(obj, "vehicleLength");
            return new ParsedEmlFeedback(notes, decision, sourceType, pNorm, pNonEmit, pThreat, vClass, vLen);
        } catch (Exception e) {
            return new ParsedEmlFeedback(notes, "", "", null, null, null, null, null);
        }
    }

    private static String getJsonString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return "";
        }
        try {
            return obj.get(key).getAsString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static Double getJsonDouble(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        try {
            double v = obj.get(key).getAsDouble();
            return Double.isFinite(v) ? v : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Double getJsonPct(JsonObject obj, String key) {
        Double v = getJsonDouble(obj, key);
        return v == null ? null : v * 100.0;
    }

    private static Integer getJsonInt(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<LocalDate, ObservationMetrics> seedUtcDayBuckets(Instant start, Instant end) {
        Map<LocalDate, ObservationMetrics> buckets = new LinkedHashMap<>();
        if (start == null || end == null) {
            return buckets;
        }
        LocalDate firstDay = LocalDate.ofInstant(start, ZoneOffset.UTC);
        Instant inclusiveEnd = end.isAfter(start) ? end.minusNanos(1L) : start;
        LocalDate lastDay = LocalDate.ofInstant(inclusiveEnd, ZoneOffset.UTC);

        LocalDate cur = firstDay;
        while (!cur.isAfter(lastDay)) {
            buckets.put(cur, new ObservationMetrics());
            cur = cur.plusDays(1L);
        }
        return buckets;
    }

    private static DataStreamFilter buildScopedDataStreamFilter(String[] scopedLanes, String... observedProperties) {
        DataStreamFilter.Builder dsb = new DataStreamFilter.Builder()
                .withObservedProperties(observedProperties);
        if (scopedLanes != null && scopedLanes.length > 0) {
            dsb.withSystems()
                    .withUniqueIDs(scopedLanes)
                    .includeMembers(true)
                    .done();
        }
        return dsb.build();
    }

    private static void classifyFaultState(String alarmState, ObservationMetrics metrics) {
        if (alarmState == null || metrics == null) {
            return;
        }
        switch (alarmState) {
            case "Fault - Gamma High" -> metrics.gammaHighFault++;
            case "Fault - Gamma Low" -> metrics.gammaLowFault++;
            case "Fault - Neutron High" -> metrics.neutronHighFault++;
            case "Fault - Extended Occupancy" -> metrics.extendedOccupancyFault++;
            default -> {
                // Non-fault foreground states ignored
            }
        }
    }

    private static IObsSystemDatabase getDatabase(OSCARServiceModule module) {
        if (module == null || module.getParentHub() == null || module.getParentHub().getDatabaseRegistry() == null) {
            return null;
        }
        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase();
    }

    private static long countOccupancies(OSCARServiceModule module, IObsSystemDatabase db, ObsFilter filter) {
        if (db == null || db.getObservationStore() == null) {
            return 0L;
        }
        if (hasMvObsStore(module)) {
            try (var keys = db.getObservationStore().selectKeys(filter)) {
                return keys.count();
            }
        }
        return db.getObservationStore().countMatchingEntries(filter);
    }

    private static boolean hasMvObsStore(OSCARServiceModule module) {
        if (module == null || module.getParentHub() == null || module.getParentHub().getDatabaseRegistry() == null) {
            return false;
        }
        for (var database : module.getParentHub().getDatabaseRegistry().getObsSystemDatabases()) {
            if (database != null && database.getObservationStore() != null
                    && database.getObservationStore().getClass().getSimpleName().startsWith("MV")) {
                return true;
            }
        }
        return false;
    }

    private static DataComponent getRecordStructure(IObsSystemDatabase db, BigId dataStreamId) {
        if (db == null || dataStreamId == null || db.getDataStreamStore() == null) {
            return null;
        }
        IDataStreamInfo info = db.getDataStreamStore().get(new DataStreamKey(dataStreamId));
        return info != null ? info.getRecordStructure() : null;
    }

    private static OccupancyIndices resolveOccupancyIndices(IObsSystemDatabase db, BigId dsId) {
        return extractOccupancyIndices(getRecordStructure(db, dsId));
    }

    private static OccupancyIndices extractOccupancyIndices(DataComponent struct) {
        int gIdx = findLeadingScalarIndex(struct, c -> "gammaAlarm".equalsIgnoreCase(c.getName()), 5);
        int nIdx = findLeadingScalarIndex(struct, c -> "neutronAlarm".equalsIgnoreCase(c.getName()), 6);
        int adjCountIdx = findLeadingScalarIndex(
                struct,
                c -> c.getName() != null
                        && c.getName().toLowerCase(Locale.ROOT).contains("adjudicat")
                        && c.getName().toLowerCase(Locale.ROOT).contains("count"),
                9
        );
        return new OccupancyIndices(gIdx, nIdx, adjCountIdx);
    }

    private static boolean hasEmlNonThreatMarker(DataBlock block, OccupancyIndices idx) {
        if (block == null || idx == null || idx.adjCountIdx() < 0) {
            return false;
        }
        int count = safeInt(block, idx.adjCountIdx());
        if (count <= 0 || count > 256) {
            return false;
        }
        for (int i = 0; i < count; i++) {
            String marker = safeString(block, idx.adjCountIdx() + 1 + i);
            if (Adjudication.EML_NON_THREAT_MARKER.equalsIgnoreCase(marker)) {
                return true;
            }
        }
        return false;
    }

    private static int resolveAlarmStateIndex(IObsSystemDatabase db, BigId dsId) {
        return extractAlarmStateIndex(getRecordStructure(db, dsId));
    }

    private static int extractAlarmStateIndex(DataComponent struct) {
        return findLeadingScalarIndex(
                struct,
                c -> RADHelper.DEF_ALARM.equalsIgnoreCase(c.getDefinition())
                        || RADHelper.DEF_EXTENDED_OCCUPANCY_ALARM_STATE.equalsIgnoreCase(c.getDefinition())
                        || (c.getName() != null && c.getName().toLowerCase().contains("alarmstate")),
                1
        );
    }

    private static int findLeadingScalarIndex(DataComponent struct, Predicate<DataComponent> matcher, int fallbackIndex) {
        if (struct == null) {
            return fallbackIndex;
        }
        int atomIndex = 0;
        int count = struct.getComponentCount();
        for (int i = 0; i < count; i++) {
            DataComponent child = struct.getComponent(i);
            if (child == null) {
                continue;
            }
            if (matcher.test(child)) {
                return atomIndex;
            }
            if (child instanceof DataArray) {
                // Stop before variable-size arrays; all scalar alarm/status fields reside in the leading header
                break;
            }
            if (child instanceof ScalarComponent) {
                atomIndex++;
            } else {
                atomIndex += Math.max(1, child.getComponentCount());
            }
        }
        return fallbackIndex;
    }

    private static boolean containsDefinition(DataComponent comp, String targetDefinition) {
        if (comp == null || targetDefinition == null) {
            return false;
        }
        if (targetDefinition.equalsIgnoreCase(comp.getDefinition())) {
            return true;
        }
        int count = comp.getComponentCount();
        for (int i = 0; i < count; i++) {
            if (containsDefinition(comp.getComponent(i), targetDefinition)) {
                return true;
            }
        }
        return false;
    }

    private static boolean safeBoolean(DataBlock block, int idx) {
        try {
            return block != null && idx >= 0 && block.getBooleanValue(idx);
        } catch (Exception e) {
            return false;
        }
    }

    private static int safeInt(DataBlock block, int idx) {
        try {
            return block != null && idx >= 0 && idx < block.getAtomCount() ? block.getIntValue(idx) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String safeString(DataBlock block, int idx) {
        try {
            return block != null && idx >= 0 ? block.getStringValue(idx) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== UpSet Co-Occurrence & Canonical Nuclide Normalization ====================

    public static final int UPSET_BIT_GAMMA = 1;       // 1 << 0
    public static final int UPSET_BIT_NORM = 2;        // 1 << 1
    public static final int UPSET_BIT_UNKNOWN = 4;     // 1 << 2
    public static final int UPSET_BIT_INDUSTRIAL = 8;  // 1 << 3
    public static final int UPSET_BIT_MEDICAL = 16;    // 1 << 4
    public static final int UPSET_BIT_SNM = 32;        // 1 << 5
    public static final int UPSET_BIT_NEUTRON = 64;    // 1 << 6

    public static final List<String> UPSET_CANONICAL_SETS = List.of(
            "Gamma",
            "NORM",
            "Unknown",
            "Industrial",
            "Medical",
            "SNM",
            "Neutron"
    );

    public record UpSetSetTotal(String name, long count, boolean snm) {}

    public record UpSetIntersection(
            List<String> activeSets,
            long count,
            boolean snmPresent,
            double percentOfTotal
    ) {}

    public record UpSetCoOccurrenceData(
            long totalAlarmingOccupancies,
            List<UpSetSetTotal> sets,
            List<UpSetIntersection> intersections
    ) {
        public static UpSetCoOccurrenceData empty() {
            List<UpSetSetTotal> zeroSets = new ArrayList<>(UPSET_CANONICAL_SETS.size());
            for (String s : UPSET_CANONICAL_SETS) {
                zeroSets.add(new UpSetSetTotal(s, 0L, "SNM".equals(s)));
            }
            return new UpSetCoOccurrenceData(0L, zeroSets, List.of());
        }
    }

    /**
     * Normalizes operator UI selections (e.g. "Europium152", "Iodine131", "Potassium"),
     * Sandia WebID strings (e.g. "I131", "Ba133", "K40"), and RIID N42/SPE strings
     * (e.g. "I-131", "Iodine 131", "Unk-634") to canonical {@link #STANDARD_ISOTOPES} keys.
     */
    public static String normalizeIsotopeSymbol(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        for (Map.Entry<String, String> std : STANDARD_ISOTOPES.entrySet()) {
            if (trimmed.equalsIgnoreCase(std.getKey()) || trimmed.equalsIgnoreCase(std.getValue())) {
                return std.getKey();
            }
        }
        String compact = trimmed.replaceAll("[\\s_-]+", "").toLowerCase(Locale.ROOT);
        if (compact.isEmpty() || "none".equals(compact) || "background".equals(compact) || "nothing".equals(compact)) {
            return "";
        }
        if (compact.startsWith("unk")) return "Unk";
        if (compact.startsWith("u233") || compact.startsWith("uranium233")) return "U-233";
        if (compact.startsWith("u235") || compact.startsWith("uranium235") || compact.startsWith("heu")) return "U-235";
        if (compact.startsWith("u238") || compact.startsWith("uranium238") || compact.startsWith("du") || compact.startsWith("leu") || compact.startsWith("unat") || "uranium".equals(compact) || "u".equals(compact)) return "U-238";
        if (compact.startsWith("pu") || compact.startsWith("plutonium") || compact.startsWith("wgpu") || compact.startsWith("rgpu")) return "Pu";
        if (compact.startsWith("np") || compact.startsWith("neptunium")) return "Np";
        if (compact.startsWith("am241") || compact.startsWith("am") && compact.matches("am\\d*") || compact.startsWith("americium")) return "Am";
        if (compact.startsWith("cf") || compact.startsWith("californium")) return "Cf";
        if (compact.startsWith("co57") || compact.startsWith("cobalt57")) return "Co-57";
        if (compact.startsWith("co60") || compact.startsWith("cobalt60")) return "Co-60";
        if (compact.startsWith("cs134") || compact.startsWith("cesium134") || compact.startsWith("caesium134")) return "Cs-134";
        if (compact.startsWith("cs137") || compact.startsWith("cesium137") || compact.startsWith("caesium137")) return "Cs-137";
        if (compact.startsWith("eu152") || compact.startsWith("eu154") || compact.startsWith("europium")) return "Eu";
        if (compact.startsWith("ba133") || compact.startsWith("barium")) return "Ba";
        if (compact.startsWith("bi207") || compact.startsWith("bi212") || compact.startsWith("bi214") || compact.startsWith("bismuth")) return "Bi";
        if (compact.startsWith("ir192") || compact.startsWith("iridium")) return "Ir";
        if (compact.startsWith("mn54") || compact.startsWith("manganese")) return "Mn";
        if (compact.startsWith("na22") || compact.startsWith("na24") || compact.startsWith("sodium")) return "Na";
        if (compact.startsWith("se75") || compact.startsWith("selenium")) return "Se";
        if (compact.startsWith("sr90") || compact.startsWith("strontium")) return "Sr";
        if (compact.startsWith("i123") || compact.startsWith("iodine123")) return "I-123";
        if (compact.startsWith("i125") || compact.startsWith("iodine125")) return "I-125";
        if (compact.startsWith("i131") || compact.startsWith("iodine131") || "iodine".equals(compact)) return "I-131";
        if (compact.startsWith("f18") || compact.startsWith("fluorine")) return "F";
        if (compact.startsWith("ga67") || compact.startsWith("ga68") || compact.startsWith("gallium")) return "Ga";
        if (compact.startsWith("in111") || compact.startsWith("indium")) return "In";
        if (compact.startsWith("pd103") || compact.startsWith("palladium")) return "Pd";
        if (compact.startsWith("tc99") || compact.startsWith("technetium")) return "Tc";
        if (compact.startsWith("tl201") || compact.startsWith("thallium")) return "Tl-201";
        if (compact.startsWith("lu177") || compact.startsWith("lutetium")) return "Lu-177";
        if (compact.startsWith("ho166") || compact.startsWith("holmium")) return "Ho-166m";
        if (compact.startsWith("xe133") || compact.startsWith("xenon")) return "Xe";
        if (compact.startsWith("k40") || compact.startsWith("potassium") || "k".equals(compact)) return "K";
        if (compact.startsWith("ra226") || compact.startsWith("radium")) return "Ra";
        if (compact.startsWith("th232") || compact.startsWith("th228") || compact.startsWith("thorium")) return "Th";
        return trimmed;
    }

    /**
     * Classifies an isotope symbol, WebID type, or RS350 category string into the 5 source-category
     * bits ({@code SNM}, {@code Industrial}, {@code Medical}, {@code NORM}, {@code Unknown}).
     */
    public static int classifySourceCategoryBits(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return 0;
        }
        String upper = rawToken.trim().toUpperCase(Locale.ROOT);
        if ("SNM".equals(upper) || upper.contains("SPECIAL NUCLEAR") || "NUCLEAR".equals(upper)) {
            return UPSET_BIT_SNM;
        }
        if ("MEDICAL".equals(upper) || "MED".equals(upper) || upper.contains("USAGE:MED")) {
            return UPSET_BIT_MEDICAL;
        }
        if ("INDUSTRIAL".equals(upper) || "IND".equals(upper) || upper.contains("USAGE:IND")) {
            return UPSET_BIT_INDUSTRIAL;
        }
        if ("NORM".equals(upper) || upper.contains("NATURAL")) {
            return UPSET_BIT_NORM;
        }
        if ("UNKNOWN".equals(upper) || "UNK".equals(upper)) {
            return UPSET_BIT_UNKNOWN;
        }

        String sym = normalizeIsotopeSymbol(rawToken);
        return switch (sym) {
            case "Np", "Pu", "U-233", "U-235", "U-238", "Am", "Cf" -> UPSET_BIT_SNM;
            case "Ba", "Co-57", "Co-60", "Cs-134", "Cs-137", "Eu", "Ir", "Mn", "Na", "Se", "Sr" -> UPSET_BIT_INDUSTRIAL;
            case "F", "Ga", "I-123", "I-125", "I-131", "In", "Pd", "Tc", "Tl-201", "Lu-177", "Ho-166m", "Xe" -> UPSET_BIT_MEDICAL;
            case "K", "Ra", "Th", "Bi" -> UPSET_BIT_NORM;
            case "Unk" -> UPSET_BIT_UNKNOWN;
            default -> 0;
        };
    }

    private static int classifyDispositionCodeBits(int code) {
        return switch (code) {
            case 3 -> UPSET_BIT_MEDICAL;
            case 4, Adjudication.EML_NON_THREAT_CODE -> UPSET_BIT_NORM;
            case 5 -> UPSET_BIT_INDUSTRIAL;
            case 2, 11 -> UPSET_BIT_UNKNOWN;
            default -> 0;
        };
    }

    /**
     * Single-pass O(N) bitmask aggregation of occupancy alarm & source-category co-occurrences
     * across {@code Gamma}, {@code NORM}, {@code Unknown}, {@code Industrial}, {@code Medical},
     * {@code SNM}, and {@code Neutron}. Uses a fixed {@code long[128]} counter array (~1 KB heap).
     */
    public static UpSetCoOccurrenceData aggregateUpSetCoOccurrence(
            OSCARServiceModule module,
            List<String> targetLanes,
            Instant start,
            Instant end,
            Map<String, AdjudicationLaneSummary> preloadedSummaries
    ) {
        if (module == null) {
            return UpSetCoOccurrenceData.empty();
        }
        List<String> lanes = new ArrayList<>();
        if (targetLanes != null) {
            for (String uid : targetLanes) {
                if (uid != null && !uid.isBlank()) {
                    lanes.add(uid.trim());
                }
            }
        }

        // Index adjudication records by occupancyObsId so each occupancy receives the union of its adjudications
        List<String> adjLookupLanes = lanes;
        if (adjLookupLanes.isEmpty() && module.getParentHub() != null && module.getParentHub().getModuleRegistry() != null) {
            adjLookupLanes = new ArrayList<>();
            for (IModule<?> mod : module.getParentHub().getModuleRegistry().getLoadedModules()) {
                if (mod instanceof LaneSystem ls && ls.getUniqueIdentifier() != null && !ls.getUniqueIdentifier().isBlank()) {
                    adjLookupLanes.add(ls.getUniqueIdentifier().trim());
                }
            }
        }
        Map<String, Integer> adjMaskByOccId = new HashMap<>();
        List<AdjudicationDetailRecord> allAdjRecords = new ArrayList<>();
        for (String laneUid : adjLookupLanes) {
            AdjudicationLaneSummary s = (preloadedSummaries != null && preloadedSummaries.containsKey(laneUid))
                    ? preloadedSummaries.get(laneUid)
                    : aggregateAdjudicationsForLane(module, laneUid, start, end);
            if (s != null && s.structuredRecords() != null) {
                for (AdjudicationDetailRecord rec : s.structuredRecords()) {
                    allAdjRecords.add(rec);
                    int mask = buildMaskFromAdjudicationRecord(rec);
                    if (rec.occupancyObsId() != null && !rec.occupancyObsId().isBlank() && !"-".equals(rec.occupancyObsId())) {
                        adjMaskByOccId.merge(rec.occupancyObsId(), mask, (a, b) -> a | b);
                    }
                }
            }
        }

        long[] countsByMask = new long[128];
        long totalOccupancies = 0L;

        IObsSystemDatabase db = getDatabase(module);
        IdEncoder obsIdEncoder = (module.getParentHub() != null && module.getParentHub().getIdEncoders() != null)
                ? module.getParentHub().getIdEncoders().getObsIdEncoder()
                : null;

        if (db != null && db.getObservationStore() != null) {
            String[] scopedArray = lanes.toArray(new String[0]);
            DataStreamFilter occDsFilter = buildScopedDataStreamFilter(scopedArray, RADHelper.DEF_OCCUPANCY);
            Map<BigId, DataComponent> structCache = new HashMap<>();
            Map<BigId, OccupancyIndices> idxCache = new HashMap<>();

            // Pre-index device-level N42Report alarm/isotope classifications by occupancyObsId in one pass
            Map<String, Integer> n42MaskByOccId = new HashMap<>();
            try {
                DataStreamFilter n42DsFilter = buildScopedDataStreamFilter(scopedArray, RADHelper.getRadUri("N42Report"));
                ObsFilter.Builder n42FilterBuilder = new ObsFilter.Builder().withDataStreams(n42DsFilter);
                if (start != null && end != null) {
                    Instant widenedEnd = end.isAfter(Instant.MAX.minusSeconds(86_400L * 30L))
                            ? end
                            : end.plusSeconds(86_400L * 30L);
                    n42FilterBuilder.withResultTimeDuring(start, widenedEnd);
                }
                try (var n42Stream = db.getObservationStore().select(n42FilterBuilder.build())) {
                    n42Stream.forEach(n42Obs -> {
                        if (n42Obs == null || n42Obs.getResult() == null) {
                            return;
                        }
                        try {
                            DataComponent n42Proto = structCache.computeIfAbsent(
                                    n42Obs.getDataStreamID(), dsId -> getRecordStructure(db, dsId));
                            if (n42Proto == null) {
                                return;
                            }
                            DataComponent n42Struct = n42Proto.copy();
                            n42Struct.setData(n42Obs.getResult());
                            DataComponent occIdComp = n42Struct.getComponent("occupancyObsId");
                            String occId = (occIdComp != null && occIdComp.getData() != null)
                                    ? occIdComp.getData().getStringValue()
                                    : null;
                            if (occId == null || occId.isBlank()) {
                                return;
                            }
                            int n42Bits = 0;
                            DataComponent alarmsComp = n42Struct.getComponent("alarmReports");
                            if (alarmsComp instanceof net.opengis.swe.v20.DataArray alarmsArr) {
                                int count = alarmsArr.getComponentCount();
                                for (int ai = 0; ai < count; ai++) {
                                    DataComponent elem = alarmsArr.getComponent(ai);
                                    if (elem == null) {
                                        continue;
                                    }
                                    DataComponent catComp = elem.getComponent("alarmCategoryCode");
                                    if (catComp == null) catComp = elem.getComponent("alarmCategory");
                                    DataComponent descComp = elem.getComponent("alarmDescription");
                                    if (catComp != null && catComp.getData() != null) {
                                        n42Bits |= classifySourceCategoryBits(catComp.getData().getStringValue());
                                    }
                                    if (descComp != null && descComp.getData() != null) {
                                        String desc = descComp.getData().getStringValue();
                                        if (desc != null) {
                                            for (String token : desc.split("[,():;/]+")) {
                                                String t = token.trim();
                                                if (!t.isEmpty() && !t.equalsIgnoreCase("Isotope")
                                                        && !t.startsWith("Confidence")) {
                                                    n42Bits |= classifySourceCategoryBits(t);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (n42Bits != 0) {
                                n42MaskByOccId.merge(occId.trim(), n42Bits, (a, b) -> a | b);
                            }
                        } catch (Exception ignored) {
                        }
                    });
                }
            } catch (Exception ignored) {
            }

            ObsFilter.Builder filterBuilder = new ObsFilter.Builder()
                    .withDataStreams(occDsFilter)
                    .withCQLFilter(Utils.anyOccupancyAlarmCQL);
            if (start != null && end != null) {
                filterBuilder.withResultTimeDuring(start, end);
            }

            long[] totalHolder = {0L};
            try (var stream = db.getObservationStore().selectEntries(filterBuilder.build())) {
                stream.forEach(entry -> {
                    if (entry == null || entry.getValue() == null || entry.getValue().getResult() == null) {
                        return;
                    }
                    BigId obsKeyId = entry.getKey();
                    IObsData obs = entry.getValue();
                    DataComponent struct = structCache.computeIfAbsent(
                            obs.getDataStreamID(), dsId -> getRecordStructure(db, dsId));
                    OccupancyIndices idx = idxCache.computeIfAbsent(
                            obs.getDataStreamID(), dsId -> extractOccupancyIndices(struct));
                    DataBlock block = obs.getResult();
                    boolean g = safeBoolean(block, idx.gammaIdx());
                    boolean n = safeBoolean(block, idx.neutronIdx());
                    if (!g && !n) {
                        return;
                    }

                    int mask = 0;
                    if (g) mask |= UPSET_BIT_GAMMA;
                    if (n) mask |= UPSET_BIT_NEUTRON;

                    if (hasEmlNonThreatMarker(block, idx)) {
                        mask |= UPSET_BIT_NORM;
                    }

                    try {
                        Occupancy occ = OccupancyExtended.readOccupancy(struct, block);
                        if (occ instanceof OccupancyExtended ext && ext.getAlarmCategory() != null && !ext.getAlarmCategory().isBlank()) {
                            for (String part : ext.getAlarmCategory().split("[,;/+]")) {
                                mask |= classifySourceCategoryBits(part);
                            }
                        }
                        if (obsIdEncoder != null && occ.getWebIdObsIds() != null && !occ.getWebIdObsIds().isEmpty()) {
                            for (String webIdEncoded : occ.getWebIdObsIds()) {
                                if (webIdEncoded == null || webIdEncoded.isBlank()) {
                                    continue;
                                }
                                try {
                                    BigId webIdBig = obsIdEncoder.decodeID(webIdEncoded);
                                    IObsData webObs = db.getObservationStore().get(webIdBig);
                                    if (webObs != null && webObs.getResult() != null) {
                                        WebIdAnalysis wa = WebIdAnalysis.toWebIdAnalysis(webObs.getResult());
                                        if (wa != null && wa.getIsotopes() != null) {
                                            for (IsotopeAnalysis iso : wa.getIsotopes()) {
                                                if (iso != null) {
                                                    mask |= classifySourceCategoryBits(iso.getName());
                                                    mask |= classifySourceCategoryBits(iso.getType());
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    if (obsIdEncoder != null && obsKeyId != null) {
                        try {
                            String encodedOccId = obsIdEncoder.encodeID(obsKeyId);
                            Integer adjBits = adjMaskByOccId.get(encodedOccId);
                            if (adjBits != null) {
                                mask |= adjBits;
                            }
                            Integer n42Bits = n42MaskByOccId.get(encodedOccId);
                            if (n42Bits != null) {
                                mask |= n42Bits;
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    if (mask > 0 && mask < countsByMask.length) {
                        countsByMask[mask]++;
                        totalHolder[0]++;
                    }
                });
            }
            totalOccupancies = totalHolder[0];
        }

        // Fallback when adjudications exist without matching occupancy observations (e.g. standalone command tests)
        if (totalOccupancies == 0L && !allAdjRecords.isEmpty()) {
            Map<String, Integer> byOccKey = new LinkedHashMap<>();
            for (AdjudicationDetailRecord rec : allAdjRecords) {
                String key = (rec.occupancyObsId() != null && !rec.occupancyObsId().isBlank() && !"-".equals(rec.occupancyObsId()))
                        ? rec.occupancyObsId()
                        : (rec.laneUid() + "#" + rec.sequenceNumber());
                int recMask = buildMaskFromAdjudicationRecord(rec) | UPSET_BIT_GAMMA;
                byOccKey.merge(key, recMask, (a, b) -> a | b);
            }
            for (int mask : byOccKey.values()) {
                if (mask > 0 && mask < countsByMask.length) {
                    countsByMask[mask]++;
                    totalOccupancies++;
                }
            }
        }

        return finalizeUpSetData(countsByMask, totalOccupancies);
    }

    /**
     * Builds UpSet co-occurrence data directly from resolved vehicle incident passes.
     */
    public static UpSetCoOccurrenceData buildUpSetFromVehiclePasses(
            List<VehicleIncidentAggregator.VehiclePassRecord> passes
    ) {
        if (passes == null || passes.isEmpty()) {
            return UpSetCoOccurrenceData.empty();
        }
        long[] countsByMask = new long[128];
        long total = 0L;

        for (VehicleIncidentAggregator.VehiclePassRecord pass : passes) {
            if (pass == null || !pass.isAlarming()) {
                continue;
            }
            int mask = 0;
            if (pass.gammaAlarm()) mask |= UPSET_BIT_GAMMA;
            if (pass.neutronAlarm()) mask |= UPSET_BIT_NEUTRON;
            if (pass.emlAutoReleased()) mask |= UPSET_BIT_NORM;
            if (pass.alarmCategoryCode() != null && !pass.alarmCategoryCode().isBlank()) {
                for (String tok : pass.alarmCategoryCode().split("[,;/+]")) {
                    mask |= classifySourceCategoryBits(tok);
                }
            }
            if (pass.confirmedIsotopes() != null) {
                for (String iso : pass.confirmedIsotopes()) {
                    mask |= classifySourceCategoryBits(iso);
                }
            }
            if (pass.adjudications() != null) {
                for (AdjudicationDetailRecord rec : pass.adjudications()) {
                    mask |= buildMaskFromAdjudicationRecord(rec);
                }
            }
            if (mask == 0) {
                mask = UPSET_BIT_GAMMA;
            }
            countsByMask[mask & 0x7F]++;
            total++;
        }

        return finalizeUpSetData(countsByMask, total);
    }

    private static int buildMaskFromAdjudicationRecord(AdjudicationDetailRecord rec) {
        if (rec == null) {
            return 0;
        }
        int isoBits = 0;
        if (rec.isotopes() != null) {
            for (String iso : rec.isotopes()) {
                isoBits |= classifySourceCategoryBits(iso);
            }
        }
        int mask = isoBits;
        if (rec.emlSourceType() != null && !rec.emlSourceType().isBlank()) {
            mask |= classifySourceCategoryBits(rec.emlSourceType());
        }
        int codeBits = classifyDispositionCodeBits(rec.adjudicationCode());
        if (codeBits == UPSET_BIT_INDUSTRIAL && isoBits != 0) {
            // Declared shipment with explicit medical/SNM/NORM isotope keeps the explicit isotope category
        } else {
            mask |= codeBits;
        }
        if (rec.adjudicationCode() == 1 && (mask & (UPSET_BIT_SNM | UPSET_BIT_INDUSTRIAL | UPSET_BIT_MEDICAL | UPSET_BIT_NORM)) == 0) {
            mask |= UPSET_BIT_UNKNOWN;
        }
        return mask;
    }

    public static UpSetCoOccurrenceData finalizeUpSetData(long[] countsByMask, long totalOccupancies) {
        if (totalOccupancies <= 0L) {
            return UpSetCoOccurrenceData.empty();
        }

        long[] setTotals = new long[UPSET_CANONICAL_SETS.size()];
        for (int mask = 1; mask < 128; mask++) {
            long c = countsByMask[mask];
            if (c <= 0L) {
                continue;
            }
            for (int bit = 0; bit < UPSET_CANONICAL_SETS.size(); bit++) {
                if ((mask & (1 << bit)) != 0) {
                    setTotals[bit] += c;
                }
            }
        }

        List<Integer> sortedSetIndices = new ArrayList<>(UPSET_CANONICAL_SETS.size());
        for (int i = 0; i < UPSET_CANONICAL_SETS.size(); i++) {
            sortedSetIndices.add(i);
        }
        sortedSetIndices.sort((a, b) -> {
            int cmp = Long.compare(setTotals[b], setTotals[a]);
            return cmp != 0 ? cmp : Integer.compare(a, b);
        });

        List<UpSetSetTotal> orderedSets = new ArrayList<>(UPSET_CANONICAL_SETS.size());
        for (int idx : sortedSetIndices) {
            String name = UPSET_CANONICAL_SETS.get(idx);
            orderedSets.add(new UpSetSetTotal(name, setTotals[idx], idx == 5));
        }

        List<UpSetIntersection> intersections = new ArrayList<>();
        for (int mask = 1; mask < 128; mask++) {
            long c = countsByMask[mask];
            if (c <= 0L) {
                continue;
            }
            List<String> active = new ArrayList<>();
            for (int idx : sortedSetIndices) {
                if ((mask & (1 << idx)) != 0) {
                    active.add(UPSET_CANONICAL_SETS.get(idx));
                }
            }
            boolean snmPresent = (mask & UPSET_BIT_SNM) != 0;
            double pct = (c * 100.0) / totalOccupancies;
            intersections.add(new UpSetIntersection(active, c, snmPresent, pct));
        }

        intersections.sort((a, b) -> {
            int cmp = Long.compare(b.count(), a.count());
            if (cmp != 0) {
                return cmp;
            }
            if (a.snmPresent() != b.snmPresent()) {
                return a.snmPresent() ? -1 : 1;
            }
            return Integer.compare(b.activeSets().size(), a.activeSets().size());
        });

        return new UpSetCoOccurrenceData(totalOccupancies, orderedSets, intersections);
    }

    public static void writeUpSetJson(JsonWriter jw, UpSetCoOccurrenceData data) throws IOException {
        UpSetCoOccurrenceData safe = data != null ? data : UpSetCoOccurrenceData.empty();
        jw.name("upSetCoOccurrence").beginObject();
        jw.name("totalOccupancies").value(safe.totalAlarmingOccupancies());
        jw.name("sets").beginArray();
        for (UpSetSetTotal set : safe.sets()) {
            jw.beginObject();
            jw.name("name").value(set.name());
            jw.name("count").value(set.count());
            jw.name("snm").value(set.snm());
            jw.endObject();
        }
        jw.endArray();
        jw.name("intersections").beginArray();
        for (UpSetIntersection inter : safe.intersections()) {
            jw.beginObject();
            jw.name("sets").beginArray();
            for (String s : inter.activeSets()) {
                jw.value(s);
            }
            jw.endArray();
            jw.name("activeSets").beginArray();
            for (String s : inter.activeSets()) {
                jw.value(s);
            }
            jw.endArray();
            jw.name("label").value(String.join(" + ", inter.activeSets()));
            jw.name("count").value(inter.count());
            jw.name("snmPresent").value(inter.snmPresent());
            jw.name("percentPct").value(XlsxWorkbookWriter.roundTwoDecimals(inter.percentOfTotal()));
            jw.endObject();
        }
        jw.endArray();
        jw.endObject();
    }

    public static XlsxWorkbookWriter.SheetSpec buildUpSetXlsxSheet(UpSetCoOccurrenceData data) {
        UpSetCoOccurrenceData safe = data != null ? data : UpSetCoOccurrenceData.empty();
        List<List<XlsxWorkbookWriter.CellValue>> rows = new ArrayList<>(safe.intersections().size());
        int rank = 1;
        for (UpSetIntersection inter : safe.intersections()) {
            rows.add(List.of(
                    XlsxWorkbookWriter.CellValue.integer(rank++),
                    XlsxWorkbookWriter.CellValue.text(String.join(" + ", inter.activeSets())),
                    XlsxWorkbookWriter.CellValue.integer(inter.activeSets().size()),
                    XlsxWorkbookWriter.CellValue.text(inter.snmPresent() ? "YES (SNM)" : "No"),
                    XlsxWorkbookWriter.CellValue.integer(inter.count()),
                    XlsxWorkbookWriter.CellValue.decimal(inter.percentOfTotal())
            ));
        }
        return new XlsxWorkbookWriter.SheetSpec(
                "Alarm Co-Occurrence",
                List.of("Rank", "Alarm & Category Combination", "Category Count", "SNM Present", "Occupancies", "Percent (%)"),
                List.of(10.0, 38.0, 16.0, 16.0, 16.0, 14.0),
                rows,
                true
        );
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }
}
