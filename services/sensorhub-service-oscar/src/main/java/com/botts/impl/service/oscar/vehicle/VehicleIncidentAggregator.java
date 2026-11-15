/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.service.oscar.vehicle;

import com.botts.impl.service.oscar.AdjudicationCodeConfig;
import com.botts.impl.service.oscar.OSCARServiceModule;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.helpers.ReportDataAggregator;
import com.botts.impl.service.oscar.reports.helpers.ReportDataAggregator.AdjudicationDetailRecord;
import com.botts.impl.service.oscar.reports.helpers.ReportDataAggregator.DispositionCodeSpec;
import com.botts.impl.service.oscar.reports.helpers.TableGenerator;
import com.botts.impl.service.oscar.reports.helpers.Utils;
import com.botts.impl.system.lane.AdjudicationControl;
import com.botts.impl.system.lane.LaneSystem;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.api.datastore.command.CommandStatusFilter;
import org.sensorhub.api.datastore.command.CommandStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.system.ISystemDriver;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.Adjudication;
import org.sensorhub.impl.utils.rad.model.IsotopeAnalysis;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.sensorhub.impl.utils.rad.model.OccupancyExtended;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;
import org.sensorhub.impl.utils.rad.model.WebIdAnalysis;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Node-wide vehicle-pass correlation and aggregation engine shared by
 * {@link VehicleSearchHandler} ({@code GET /buckets/adjudication/vehicle-search}) and
 * {@code VehicleIncidentReport} ({@code VEHICLE_INCIDENT} report generation).
 *
 * <p>Resolves vehicle transits across operator/automated adjudications
 * ({@code CommandStatusStore}) and camera OCR reads ({@code vehicleOcr} datastreams in
 * {@code ObservationStore}, including archived {@code PARQUET_PRIMARY} tiers), batch-loads
 * correlated {@link Occupancy} / {@link OccupancyExtended} and {@link WebIdAnalysis}
 * observations, and merges optional federated remote-node pass bundles without introducing
 * any auxiliary PostgreSQL tables.</p>
 */
public final class VehicleIncidentAggregator {

    public static final int MAX_SPEC_CHARS = 2 * 1024 * 1024;
    public static final int DEFAULT_SEARCH_LIMIT = 200;
    public static final int MAX_SEARCH_LIMIT = 1_000;
    public static final int MAX_FEDERATED_PASSES = 500;
    public static final int MAX_PROFILE_SAMPLES_PER_PASS = 240;
    public static final int MAX_PROFILED_PASSES = 16;

    public static final String SYSTEM_TYPE_GROSS = "Gross-Counting";
    public static final String SYSTEM_TYPE_SPECTROSCOPIC = "RS350 Spectroscopic";

    private static final Pattern ISO_6346_NORMALIZED = Pattern.compile("^[A-Z]{3}[UJZ][0-9]{7}$");
    private static final Pattern ISO_6346_PREFIX_10 = Pattern.compile("^[A-Z]{3}[UJZ][0-9]{6}$");
    private static final String VEHICLE_OCR_OUTPUT_NAME = "vehicleOcr";
    private static final String DEF_VEHICLE_OCR = RADHelper.getRadUri("VehicleOCR");
    private static final String DEF_VEHICLE_ID = RADHelper.getRadUri("VehicleID");

    private VehicleIncidentAggregator() {}

    public enum MatchMode {
        EQUALS("equals", "Exact Match"),
        CONTAINS("contains", "Contains"),
        STARTS_WITH("startsWith", "Starts With"),
        NOT_EQUALS("notEquals", "Not Equal"),
        IS_EMPTY("isEmpty", "Empty"),
        IS_NOT_EMPTY("isNotEmpty", "Not Empty");

        private final String wireName;
        private final String displayLabel;

        MatchMode(String wireName, String displayLabel) {
            this.wireName = wireName;
            this.displayLabel = displayLabel;
        }

        public String wireName() {
            return wireName;
        }

        public String displayLabel() {
            return displayLabel;
        }

        public boolean matches(String candidateNormalized, String queryNormalized) {
            String cand = candidateNormalized == null ? "" : candidateNormalized;
            String query = queryNormalized == null ? "" : queryNormalized;
            return switch (this) {
                case EQUALS -> !query.isEmpty() && cand.equals(query);
                case CONTAINS -> !query.isEmpty() && cand.contains(query);
                case STARTS_WITH -> !query.isEmpty() && cand.startsWith(query);
                case NOT_EQUALS -> !cand.isEmpty() && !cand.equals(query);
                case IS_EMPTY -> cand.isEmpty();
                case IS_NOT_EMPTY -> !cand.isEmpty();
            };
        }

        public static MatchMode parse(String raw, MatchMode fallback) {
            if (raw == null || raw.isBlank()) {
                return fallback != null ? fallback : EQUALS;
            }
            String cleaned = raw.trim().replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
            return switch (cleaned) {
                case "equals", "exact", "eq" -> EQUALS;
                case "contains", "like", "substring", "partial" -> CONTAINS;
                case "startswith", "prefix" -> STARTS_WITH;
                case "notequals", "neq" -> NOT_EQUALS;
                case "isempty", "empty", "null" -> IS_EMPTY;
                case "isnotempty", "notempty", "notnull", "any" -> IS_NOT_EMPTY;
                default -> fallback != null ? fallback : EQUALS;
            };
        }
    }

    public record OcrMatchSummary(
            String value,
            String normalizedValue,
            String idType,
            float confidence,
            boolean checksumValid,
            int readCount,
            String cameraUid,
            String frameTimeIso,
            String evidenceImagePath
    ) {}

    public record WebIdSummary(
            String obsId,
            String sampleTimeIso,
            String isotopeString,
            List<String> isotopes,
            String drf,
            double estimatedDose,
            double chi2,
            String errorMessage,
            List<String> warnings
    ) {}

    public record ProfileSample(
            long timestampMs,
            double offsetSeconds,
            int gammaCounts,
            int neutronCounts
    ) {}

    public record VehiclePassRecord(
            String nodeId,
            String nodeName,
            boolean federatedRemote,
            String laneUid,
            String laneName,
            String datastreamId,
            String occupancyObsId,
            int occupancyCount,
            String systemType,
            String startTimeIso,
            String endTimeIso,
            String samplingTimeIso,
            double durationSeconds,
            boolean gammaAlarm,
            boolean neutronAlarm,
            String alarmCategoryCode,
            String alarmStatusLabel,
            boolean emlAutoReleased,
            int maxGammaCount,
            int maxNeutronCount,
            double neutronBackground,
            String vehicleId,
            String normalizedVehicleId,
            String vehicleIdSource,
            OcrMatchSummary ocr,
            List<AdjudicationDetailRecord> adjudications,
            AdjudicationDetailRecord latestAdjudication,
            String emlDecision,
            String emlSourceType,
            Double emlNormProbPct,
            Double emlThreatProbPct,
            List<WebIdSummary> webIdAnalyses,
            List<String> confirmedIsotopes,
            List<String> videoPaths,
            List<ProfileSample> radiationProfile
    ) {
        public boolean isAlarming() {
            return gammaAlarm || neutronAlarm
                    || (alarmCategoryCode != null && !alarmCategoryCode.isBlank()
                    && !"None".equalsIgnoreCase(alarmCategoryCode)
                    && !"Background".equalsIgnoreCase(alarmCategoryCode));
        }

        public boolean isOpenAlarm() {
            return isAlarming() && !emlAutoReleased && latestAdjudication == null;
        }

        public Instant sortInstant() {
            for (String candidate : List.of(
                    startTimeIso != null ? startTimeIso : "",
                    samplingTimeIso != null ? samplingTimeIso : "",
                    endTimeIso != null ? endTimeIso : "")) {
                if (!candidate.isBlank()) {
                    try {
                        return Instant.parse(candidate);
                    } catch (Exception ignored) {
                    }
                }
            }
            return Instant.EPOCH;
        }
    }

    public record VehicleIncidentSpec(
            String rawVehicleId,
            String normalizedVehicleId,
            MatchMode matchMode,
            String laneUIDs,
            boolean includeProfiles,
            List<VehiclePassRecord> federatedPasses
    ) {}

    public record VehicleIncidentData(
            String nodeId,
            String rawVehicleId,
            String normalizedVehicleId,
            String formattedVehicleId,
            MatchMode matchMode,
            String scopeSummary,
            Instant start,
            Instant end,
            List<VehiclePassRecord> passes,
            int totalMatchedBeforeLimit,
            boolean truncated,
            int alarmingPassCount,
            int openAlarmPassCount,
            int emlAutoReleasePassCount,
            int spectroscopicPassCount,
            int distinctLaneCount,
            int distinctNodeCount,
            List<String> confirmedIsotopes,
            String latestDispositionLabel,
            String latestDispositionGroup
    ) {}

    /**
     * Canonical vehicle identifier normalization: strips spaces, hyphens, slashes, and punctuation
     * and converts ASCII alphanumerics to uppercase so {@code "MSCU 123456-5"}, {@code "mscu1234565"},
     * and {@code "MSCU-1234565"} correlate identically across operators and OCR engines.
     */
    public static String normalizeVehicleId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        if ("-".equals(trimmed)) {
            return "";
        }
        StringBuilder sb = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                sb.append(c);
            } else if (c >= 'a' && c <= 'z') {
                sb.append((char) (c - ('a' - 'A')));
            }
        }
        return sb.toString();
    }

    public static String formatVehicleIdDisplay(String raw, String normalized) {
        String norm = normalized != null ? normalizeVehicleId(normalized) : normalizeVehicleId(raw);
        String trimmed = raw == null ? "" : raw.trim();
        if ("-".equals(trimmed)) {
            trimmed = "";
        }
        String rawNorm = normalizeVehicleId(trimmed);
        boolean rawHasExtraneousTokens = !trimmed.isEmpty() && !norm.isEmpty() && !rawNorm.equals(norm);
        if (ISO_6346_NORMALIZED.matcher(norm).matches()
                && (trimmed.isEmpty() || trimmed.equalsIgnoreCase(norm) || rawHasExtraneousTokens)) {
            return norm.substring(0, 4) + " " + norm.substring(4, 10) + "-" + norm.substring(10);
        }
        if (ISO_6346_PREFIX_10.matcher(norm).matches()
                && (trimmed.isEmpty() || trimmed.equalsIgnoreCase(norm) || rawHasExtraneousTokens)) {
            return norm.substring(0, 4) + " " + norm.substring(4);
        }
        if (!trimmed.isEmpty() && !rawHasExtraneousTokens) {
            return trimmed;
        }
        if (!norm.isEmpty()) {
            return norm;
        }
        return trimmed;
    }

    /**
     * Parses the {@code laneUID} string field on {@code RequestReportControl} when
     * {@code reportType} is {@code VEHICLE_INCIDENT*}. Accepts either a JSON envelope
     * ({@code {"vehicleId":"...","laneUID":"...","matchMode":"equals","federatedPasses":[...]}}),
     * a semicolon-delimited {@code vehicle=<id>;lanes=<uids>;match=<mode>} specification,
     * or a plain vehicle ID string.
     */
    public static VehicleIncidentSpec parseRequestSpec(String rawSpec) {
        if (rawSpec == null || rawSpec.isBlank() || "NONE".equalsIgnoreCase(rawSpec.trim())) {
            return new VehicleIncidentSpec("", "", MatchMode.IS_NOT_EMPTY, "", true, List.of());
        }
        if (rawSpec.length() > MAX_SPEC_CHARS) {
            throw new IllegalArgumentException(
                    "Vehicle incident specification exceeds maximum size of " + MAX_SPEC_CHARS + " characters");
        }
        String trimmed = rawSpec.trim();
        if (trimmed.startsWith("{")) {
            JsonElement rootEl = JsonParser.parseString(trimmed);
            if (!rootEl.isJsonObject()) {
                throw new IllegalArgumentException("Vehicle incident JSON specification must be an object");
            }
            JsonObject obj = rootEl.getAsJsonObject();
            String rawVehicleId = getJsonString(obj, "vehicleId");
            String normVehicleId = normalizeVehicleId(rawVehicleId);
            MatchMode defaultMode = normVehicleId.isEmpty() ? MatchMode.IS_NOT_EMPTY : MatchMode.EQUALS;
            MatchMode matchMode = MatchMode.parse(getJsonString(obj, "matchMode"), defaultMode);
            String laneUIDs = extractLaneUidSpec(obj);
            boolean includeProfiles = !obj.has("includeProfiles")
                    || obj.get("includeProfiles").isJsonNull()
                    || obj.get("includeProfiles").getAsBoolean();
            List<VehiclePassRecord> federatedPasses = obj.has("federatedPasses") && obj.get("federatedPasses").isJsonArray()
                    ? parseFederatedPasses(obj.getAsJsonArray("federatedPasses"))
                    : List.of();
            return new VehicleIncidentSpec(rawVehicleId, normVehicleId, matchMode, laneUIDs, includeProfiles, federatedPasses);
        }

        if (trimmed.contains("vehicle=") || trimmed.contains("vehicleId=")) {
            String rawVehicle = "";
            String laneUIDs = "";
            String rawMatch = "";
            for (String token : trimmed.split(";")) {
                int eq = token.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String k = token.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                String v = token.substring(eq + 1).trim();
                switch (k) {
                    case "vehicle", "vehicleid" -> rawVehicle = v;
                    case "lane", "lanes", "laneuid", "laneuids" -> laneUIDs = v;
                    case "match", "matchmode" -> rawMatch = v;
                    default -> {
                    }
                }
            }
            String norm = normalizeVehicleId(rawVehicle);
            MatchMode mode = MatchMode.parse(rawMatch, norm.isEmpty() ? MatchMode.IS_NOT_EMPTY : MatchMode.EQUALS);
            return new VehicleIncidentSpec(rawVehicle, norm, mode, laneUIDs, true, List.of());
        }

        if (trimmed.startsWith("urn:")) {
            return new VehicleIncidentSpec("", "", MatchMode.IS_NOT_EMPTY, trimmed, true, List.of());
        }

        String norm = normalizeVehicleId(trimmed);
        return new VehicleIncidentSpec(trimmed, norm, MatchMode.EQUALS, "", true, List.of());
    }

    public static VehicleIncidentData queryVehicleIncident(
            OSCARServiceModule module,
            VehicleIncidentSpec spec,
            Instant start,
            Instant end,
            int maxResults
    ) {
        String localNodeId = (module != null && module.getConfiguration() != null
                && module.getConfiguration().nodeId != null && !module.getConfiguration().nodeId.isBlank())
                ? module.getConfiguration().nodeId.trim()
                : "oscar-node";

        VehicleIncidentSpec effectiveSpec = spec != null
                ? spec
                : new VehicleIncidentSpec("", "", MatchMode.IS_NOT_EMPTY, "", false, List.of());
        int cappedLimit = maxResults <= 0 ? MAX_SEARCH_LIMIT : Math.min(maxResults, MAX_SEARCH_LIMIT);

        LocalPassSelection localSelection = collectLocalNodePasses(
                module, localNodeId, effectiveSpec, start, end, cappedLimit);

        // Merge federated remote passes while deduplicating any composite (nodeId, laneUid, occupancyObsId) collisions
        Map<String, VehiclePassRecord> mergedByKey = new LinkedHashMap<>();
        for (VehiclePassRecord local : localSelection.passes()) {
            mergedByKey.put(compositePassKey(local), local);
        }
        if (effectiveSpec.federatedPasses() != null) {
            for (VehiclePassRecord remote : effectiveSpec.federatedPasses()) {
                if (remote == null) {
                    continue;
                }
                if (!effectiveSpec.matchMode().matches(remote.normalizedVehicleId(), effectiveSpec.normalizedVehicleId())) {
                    continue;
                }
                Instant passTs = remote.sortInstant();
                if (!Instant.EPOCH.equals(passTs)) {
                    if (start != null && passTs.isBefore(start)) {
                        continue;
                    }
                    if (end != null && passTs.isAfter(end)) {
                        continue;
                    }
                }
                String key = compositePassKey(remote);
                // Prefer authoritative local node record if the same nodeId was also included in federatedPasses
                mergedByKey.putIfAbsent(key, remote);
            }
        }

        List<VehiclePassRecord> allSorted = new ArrayList<>(mergedByKey.values());
        allSorted.sort(Comparator
                .comparing(VehiclePassRecord::sortInstant)
                .thenComparing(p -> nonNull(p.nodeId()))
                .thenComparing(p -> nonNull(p.laneUid()))
                .thenComparingInt(VehiclePassRecord::occupancyCount));

        int totalBeforeLimit = allSorted.size() + localSelection.preTruncatedCount();
        boolean truncated = totalBeforeLimit > cappedLimit;
        List<VehiclePassRecord> boundedPasses = allSorted.size() > cappedLimit
                ? List.copyOf(allSorted.subList(allSorted.size() - cappedLimit, allSorted.size()))
                : List.copyOf(allSorted);

        int alarmingCount = 0;
        int openAlarmCount = 0;
        int emlAutoCount = 0;
        int specCount = 0;
        Set<String> distinctLanes = new LinkedHashSet<>();
        Set<String> distinctNodes = new LinkedHashSet<>();
        Set<String> isotopes = new LinkedHashSet<>();
        String latestDispLabel = "No Adjudications";
        String latestDispGroup = "None";

        for (VehiclePassRecord pass : boundedPasses) {
            if (pass.isAlarming()) {
                alarmingCount++;
            }
            if (pass.isOpenAlarm()) {
                openAlarmCount++;
            }
            if (pass.emlAutoReleased()) {
                emlAutoCount++;
            }
            if (SYSTEM_TYPE_SPECTROSCOPIC.equalsIgnoreCase(pass.systemType()) || !pass.webIdAnalyses().isEmpty()) {
                specCount++;
            }
            distinctLanes.add(nonNull(pass.nodeId()) + "|" + nonNull(pass.laneUid()));
            distinctNodes.add(nonNull(pass.nodeId()));
            isotopes.addAll(pass.confirmedIsotopes());
            if (pass.latestAdjudication() != null && !pass.latestAdjudication().dispositionLabel().isBlank()) {
                latestDispLabel = pass.latestAdjudication().dispositionLabel();
                latestDispGroup = pass.latestAdjudication().dispositionGroup();
            } else if (pass.emlAutoReleased() && "No Adjudications".equals(latestDispLabel)) {
                latestDispLabel = "9999 (EML Non-Threat)";
                latestDispGroup = "Automated";
            }
        }
        if (openAlarmCount > 0 && "No Adjudications".equals(latestDispLabel)) {
            latestDispLabel = "Open / Unadjudicated (" + openAlarmCount + ")";
            latestDispGroup = "Open";
        }

        String baseScope = ReportDataAggregator.buildBannerScopeSummary(module, effectiveSpec.laneUIDs());
        long remoteNodeCount = distinctNodes.stream().filter(n -> !n.isBlank() && !n.equalsIgnoreCase(localNodeId)).count();
        String scopeSummary = remoteNodeCount > 0
                ? (baseScope + " + " + remoteNodeCount + (remoteNodeCount == 1 ? " federated node" : " federated nodes"))
                : baseScope;

        String displayVehicle = effectiveSpec.rawVehicleId() != null && !effectiveSpec.rawVehicleId().isBlank()
                ? formatVehicleIdDisplay(effectiveSpec.rawVehicleId(), effectiveSpec.normalizedVehicleId())
                : (boundedPasses.size() == 1 && !boundedPasses.get(0).vehicleId().isBlank()
                    ? boundedPasses.get(0).vehicleId()
                    : "All Tagged Vehicles");

        return new VehicleIncidentData(
                localNodeId,
                nonNull(effectiveSpec.rawVehicleId()),
                nonNull(effectiveSpec.normalizedVehicleId()),
                displayVehicle,
                effectiveSpec.matchMode(),
                scopeSummary,
                start,
                end,
                boundedPasses,
                totalBeforeLimit,
                truncated,
                alarmingCount,
                openAlarmCount,
                emlAutoCount,
                specCount,
                distinctLanes.size(),
                Math.max(1, distinctNodes.size()),
                List.copyOf(isotopes),
                latestDispLabel,
                latestDispGroup
        );
    }

    private record LaneStreamMetadata(
            String laneUid,
            String laneName,
            BigId occStreamBigId,
            String encodedOccStreamId,
            DataComponent occRecordStructure,
            boolean extendedSchema
    ) {}

    private record LocalPassSelection(
            List<VehiclePassRecord> passes,
            int preTruncatedCount
    ) {
        private static final LocalPassSelection EMPTY = new LocalPassSelection(List.of(), 0);
    }

    private static LocalPassSelection collectLocalNodePasses(
            OSCARServiceModule module,
            String localNodeId,
            VehicleIncidentSpec spec,
            Instant start,
            Instant end,
            int cappedLimit
    ) {
        if (module == null || module.getParentHub() == null || module.getParentHub().getDatabaseRegistry() == null) {
            return LocalPassSelection.EMPTY;
        }
        IObsSystemDatabase db = resolveDatabase(module);
        if (db == null || db.getObservationStore() == null) {
            return LocalPassSelection.EMPTY;
        }
        IObsStore obsStore = db.getObservationStore();
        IdEncoder obsIdEncoder = module.getParentHub().getIdEncoders().getObsIdEncoder();
        IdEncoder dsIdEncoder = module.getParentHub().getIdEncoders().getDataStreamIdEncoder();

        String[] explicitLanes = Utils.parseLaneUIDs(spec.laneUIDs());
        Set<String> allowedLaneFilter = explicitLanes.length > 0
                ? new LinkedHashSet<>(List.of(explicitLanes))
                : null;

        // 1. Discover lane names, member-sensor hierarchy, and occupancy datastream metadata across loaded modules + datastore
        LaneTopology laneTopology = discoverLaneTopology(module, db);
        Map<String, String> laneNamesByUid = laneTopology.laneNamesByUid();
        Map<String, LaneStreamMetadata> occStreamByDsKey = new LinkedHashMap<>();
        Map<String, LaneStreamMetadata> occStreamByLaneUid = new LinkedHashMap<>();

        DataStreamFilter.Builder occDsFilterBuilder = new DataStreamFilter.Builder()
                .withObservedProperties(RADHelper.DEF_OCCUPANCY);
        if (explicitLanes.length > 0) {
            occDsFilterBuilder.withSystems().withUniqueIDs(explicitLanes).includeMembers(true).done();
        }
        try (var stream = db.getDataStreamStore().selectEntries(occDsFilterBuilder.build())) {
            stream.forEach(entry -> {
                BigId dsBigId = entry.getKey().getInternalID();
                IDataStreamInfo info = entry.getValue();
                if (dsBigId == null || info == null || info.getSystemID() == null) {
                    return;
                }
                String laneUid = resolveOwningLaneUid(info.getSystemID().getUniqueID(), laneTopology);
                if (allowedLaneFilter != null && !allowedLaneFilter.contains(laneUid)
                        && !allowedLaneFilter.contains(info.getSystemID().getUniqueID())) {
                    return;
                }
                String laneName = laneNamesByUid.getOrDefault(laneUid,
                        info.getSystemID().getUniqueID() != null ? laneUid : "Lane");
                DataComponent struct = info.getRecordStructure();
                boolean extended = OccupancyExtended.isExtendedSchema(struct);
                String encodedDsId = dsIdEncoder != null ? dsIdEncoder.encodeID(dsBigId) : dsBigId.toString();
                LaneStreamMetadata meta = new LaneStreamMetadata(
                        laneUid, laneName, dsBigId, encodedDsId, struct, extended);
                occStreamByDsKey.put(bigIdLookupKey(dsBigId), meta);
                occStreamByLaneUid.put(laneUid, meta);
            });
        }

        // Determine candidate lanes for CommandStatusStore scan
        Set<String> candidateLanes = new LinkedHashSet<>();
        if (explicitLanes.length > 0) {
            candidateLanes.addAll(List.of(explicitLanes));
        } else {
            candidateLanes.addAll(laneNamesByUid.keySet());
            candidateLanes.addAll(occStreamByLaneUid.keySet());
            if (db.getCommandStore() != null && db.getCommandStore().getCommandStreams() != null) {
                try (var csStream = db.getCommandStore().getCommandStreams().select(
                        new CommandStreamFilter.Builder().build())) {
                    csStream.forEach(cs -> {
                        if (isAdjudicationCommandStream(cs)
                                && cs.getSystemID() != null
                                && cs.getSystemID().getUniqueID() != null) {
                            candidateLanes.add(cs.getSystemID().getUniqueID());
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        }

        // 2. Scan completed Adjudications across candidate lanes in a single CommandStatusStore pass
        // without issuing per-row CommandStore SQL queries or JSON parses on non-matching rows
        Map<Integer, DispositionCodeSpec> dispSpecs = buildDispositionSpecs(module);
        Map<String, List<RawAdjudicationEntry>> rawAdjudicationsByObsId = new LinkedHashMap<>();
        Map<String, String> cmdStreamToLaneByKey = new HashMap<>();
        Set<String> adjMatchedObsIds = new LinkedHashSet<>();

        scanAdjudicationsSinglePass(
                db,
                candidateLanes,
                start,
                end,
                spec,
                rawAdjudicationsByObsId,
                cmdStreamToLaneByKey,
                adjMatchedObsIds
        );

        // 3. Scan vehicleOcr observations across candidate lanes (with CQL2 normalizedValue pushdown!)
        Map<String, OcrMatchSummary> ocrByObsId = new LinkedHashMap<>();
        Map<String, String> ocrLaneByObsId = new HashMap<>();
        Set<String> ocrMatchedObsIds = new LinkedHashSet<>();

        scanVehicleOcrObservations(
                db,
                obsStore,
                explicitLanes,
                allowedLaneFilter,
                laneTopology,
                start,
                end,
                spec,
                ocrByObsId,
                ocrLaneByObsId,
                ocrMatchedObsIds
        );

        // Union matched occupancy observation IDs according to MatchMode
        Set<String> targetObsIds = new LinkedHashSet<>();
        if (spec.matchMode() == MatchMode.IS_EMPTY) {
            // IS_EMPTY on search endpoint returns empty unless caller inspects event table locally
            return LocalPassSelection.EMPTY;
        } else if (spec.matchMode() == MatchMode.NOT_EQUALS) {
            targetObsIds.addAll(adjMatchedObsIds);
            targetObsIds.addAll(ocrMatchedObsIds);
            // Exclude any occupancy whose authoritative adjudication matches queryNormalized
            targetObsIds.removeIf(obsId -> {
                List<RawAdjudicationEntry> adjs = rawAdjudicationsByObsId.get(obsId);
                if (adjs != null) {
                    for (RawAdjudicationEntry a : adjs) {
                        String norm = normalizeVehicleId(a.vehicleId());
                        if (!norm.isEmpty() && norm.equals(spec.normalizedVehicleId())) {
                            return true;
                        }
                    }
                }
                OcrMatchSummary ocr = ocrByObsId.get(obsId);
                return ocr != null && ocr.normalizedValue().equals(spec.normalizedVehicleId());
            });
        } else {
            targetObsIds.addAll(adjMatchedObsIds);
            targetObsIds.addAll(ocrMatchedObsIds);
        }

        if (targetObsIds.isEmpty()) {
            return LocalPassSelection.EMPTY;
        }

        // Bound matched occupancy IDs to the most recent cappedLimit records before issuing
        // per-occupancy observation point-loads across multi-day Parquet archives.
        int preTruncatedCount = 0;
        if (cappedLimit > 0 && targetObsIds.size() > cappedLimit) {
            List<String> rankedObsIds = new ArrayList<>(targetObsIds);
            rankedObsIds.sort(Comparator
                    .<String, Instant>comparing(
                            obsId -> estimateObsRecencyInstant(obsId, rawAdjudicationsByObsId, ocrByObsId),
                            Comparator.reverseOrder())
                    .thenComparing(
                            Comparator.comparingLong((String obsId) -> safeDecodedObsIdLong(obsIdEncoder, obsId))
                                    .reversed()));
            preTruncatedCount = rankedObsIds.size() - cappedLimit;
            targetObsIds = new LinkedHashSet<>(rankedObsIds.subList(0, cappedLimit));
        }

        // If any occupancy matched via adjudication, also fetch its OCR record (even if OCR read a slightly different string!)
        // so the operator can see OCR vs operator-entered ID side-by-side!
        enrichMissingOcrForMatchedOccupancies(
                db, obsStore, explicitLanes, laneTopology, start, end, targetObsIds, ocrByObsId);

        // 4. Batch-load matched Occupancy observations via exact multi-ID catalog pruning
        // scoped to occupancy datastreams so cold Parquet lookups run in a single DuckDB query
        // without touching high-rate gamma/neutron Parquet generations.
        Map<String, BigId> decodedOccByEncodedId = new LinkedHashMap<>();
        for (String encodedObsId : targetObsIds) {
            if (encodedObsId == null || encodedObsId.isBlank() || "-".equals(encodedObsId)) {
                continue;
            }
            try {
                BigId decoded = obsIdEncoder.decodeID(encodedObsId);
                if (decoded != null) {
                    decodedOccByEncodedId.put(encodedObsId, decoded);
                }
            } catch (Exception ignored) {
                // Non-Base32 synthetic IDs are handled by fallback pass synthesis below
            }
        }
        Set<BigId> occDataStreamIds = new LinkedHashSet<>();
        for (LaneStreamMetadata meta : occStreamByDsKey.values()) {
            if (meta != null && meta.occStreamBigId() != null) {
                occDataStreamIds.add(meta.occStreamBigId());
            }
        }
        Map<String, IObsData> loadedOccupancies = batchLoadObservations(
                obsStore, decodedOccByEncodedId, occDataStreamIds);

        // 5. Collect WebID observation IDs across loaded occupancies and batch-load them
        Map<String, Occupancy> parsedOccupancies = new LinkedHashMap<>();
        Map<String, BigId> decodedWebIdByEncodedId = new LinkedHashMap<>();
        for (Map.Entry<String, IObsData> entry : loadedOccupancies.entrySet()) {
            IObsData obs = entry.getValue();
            if (obs.getResult() == null) {
                continue;
            }
            String dsKey = bigIdLookupKey(obs.getDataStreamID());
            LaneStreamMetadata dsMeta = occStreamByDsKey.computeIfAbsent(
                    dsKey,
                    ignored -> resolveAdHocStreamMetadata(db, dsIdEncoder, obs.getDataStreamID(), laneTopology));
            try {
                Occupancy occ = OccupancyExtended.readOccupancy(
                        dsMeta != null ? dsMeta.occRecordStructure() : null,
                        obs.getResult());
                parsedOccupancies.put(entry.getKey(), occ);
                for (String webIdObsId : occ.getWebIdObsIds()) {
                    if (webIdObsId == null || webIdObsId.isBlank()) {
                        continue;
                    }
                    try {
                        BigId wBig = obsIdEncoder.decodeID(webIdObsId);
                        if (wBig != null) {
                            decodedWebIdByEncodedId.putIfAbsent(webIdObsId, wBig);
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }

        Map<String, IObsData> loadedWebIds = batchLoadObservations(
                obsStore, decodedWebIdByEncodedId, Set.of());
        Map<String, WebIdSummary> webIdByEncodedId = new HashMap<>();
        for (Map.Entry<String, BigId> wEntry : decodedWebIdByEncodedId.entrySet()) {
            String wEncoded = wEntry.getKey();
            try {
                IObsData wObs = loadedWebIds.get(wEncoded);
                if (wObs == null || wObs.getResult() == null) {
                    continue;
                }
                WebIdAnalysis analysis = WebIdAnalysis.toWebIdAnalysis(wObs.getResult());
                List<String> isoNames = new ArrayList<>();
                if (analysis.getIsotopes() != null) {
                    for (IsotopeAnalysis iso : analysis.getIsotopes()) {
                        if (iso != null && iso.getName() != null && !iso.getName().isBlank()) {
                            String label = iso.getConfidenceStr() != null && !iso.getConfidenceStr().isBlank()
                                    ? (iso.getName().trim() + " (" + iso.getConfidenceStr().trim() + ")")
                                    : iso.getName().trim();
                            isoNames.add(label);
                        }
                    }
                }
                if (isoNames.isEmpty() && analysis.getIsotopeString() != null && !analysis.getIsotopeString().isBlank()) {
                    isoNames.add(analysis.getIsotopeString().trim());
                }
                webIdByEncodedId.put(wEncoded, new WebIdSummary(
                        wEncoded,
                        analysis.getSampleTime() != null ? DateTimeFormatter.ISO_INSTANT.format(analysis.getSampleTime()) : "",
                        nonNull(analysis.getIsotopeString()),
                        List.copyOf(isoNames),
                        nonNull(analysis.getDrf()),
                        analysis.getEstimatedDose(),
                        analysis.getChi2(),
                        nonNull(analysis.getErrorMessage()),
                        analysis.getAnalysisWarnings() != null ? List.copyOf(analysis.getAnalysisWarnings()) : List.of()
                ));
            } catch (Exception ignored) {
            }
        }

        // 5b. Materialize AdjudicationDetailRecord entries ONLY for matched targetObsIds (avoiding N+1 CommandStore queries)
        Map<String, List<AdjudicationDetailRecord>> adjudicationsByObsId = new LinkedHashMap<>();
        Map<String, String> fallbackLaneByObsId = new HashMap<>();
        materializeMatchedAdjudications(
                db,
                targetObsIds,
                rawAdjudicationsByObsId,
                loadedOccupancies,
                occStreamByDsKey,
                ocrLaneByObsId,
                cmdStreamToLaneByKey,
                dispSpecs,
                adjudicationsByObsId,
                fallbackLaneByObsId
        );

        Map<String, Set<BigId>> gammaStreamsByLane = new HashMap<>();
        Map<String, Set<BigId>> neutronStreamsByLane = new HashMap<>();

        // 6. Assemble VehiclePassRecord for every matched occupancy (or fallback adjudication/OCR record)
        List<VehiclePassRecord> results = new ArrayList<>(targetObsIds.size());
        for (String obsId : targetObsIds) {
            IObsData obs = loadedOccupancies.get(obsId);
            Occupancy occ = parsedOccupancies.get(obsId);
            List<AdjudicationDetailRecord> adjs = adjudicationsByObsId.getOrDefault(obsId, List.of());
            OcrMatchSummary ocr = ocrByObsId.get(obsId);

            AdjudicationDetailRecord latestAdj = adjs.isEmpty() ? null : adjs.get(adjs.size() - 1);
            String adjVehicleRaw = findLatestAdjudicatedVehicleId(adjs);
            String adjVehicleNorm = normalizeVehicleId(adjVehicleRaw);
            String ocrVehicleRaw = ocr != null ? ocr.value() : "";
            String ocrVehicleNorm = ocr != null ? ocr.normalizedValue() : "";

            String effectiveRaw;
            String effectiveNorm;
            String vehicleSource;
            if (!adjVehicleNorm.isEmpty() && !ocrVehicleNorm.isEmpty()) {
                effectiveRaw = formatVehicleIdDisplay(adjVehicleRaw, adjVehicleNorm);
                effectiveNorm = adjVehicleNorm;
                vehicleSource = "both";
            } else if (!adjVehicleNorm.isEmpty()) {
                effectiveRaw = formatVehicleIdDisplay(adjVehicleRaw, adjVehicleNorm);
                effectiveNorm = adjVehicleNorm;
                vehicleSource = "adjudication";
            } else if (!ocrVehicleNorm.isEmpty()) {
                effectiveRaw = formatVehicleIdDisplay(ocrVehicleRaw, ocrVehicleNorm);
                effectiveNorm = ocrVehicleNorm;
                vehicleSource = "ocr";
            } else {
                effectiveRaw = "";
                effectiveNorm = "";
                vehicleSource = "none";
            }

            // Verify effective match (in case an earlier adjudication had a typo that was later corrected, check both chain & effective)
            if (!matchesAnyVehicleEvidence(spec.matchMode(), spec.normalizedVehicleId(), effectiveNorm, adjs, ocr)) {
                continue;
            }

            if (obs != null && occ != null) {
                LaneStreamMetadata dsMeta = occStreamByDsKey.get(bigIdLookupKey(obs.getDataStreamID()));
                String laneUid = dsMeta != null ? dsMeta.laneUid()
                        : fallbackLaneByObsId.getOrDefault(obsId, ocrLaneByObsId.getOrDefault(obsId, ""));
                if (allowedLaneFilter != null && !laneUid.isBlank() && !allowedLaneFilter.contains(laneUid)) {
                    continue;
                }
                String laneName = dsMeta != null ? dsMeta.laneName() : laneNamesByUid.getOrDefault(laneUid, laneUid);
                String datastreamId = dsMeta != null ? dsMeta.encodedOccStreamId() : "";

                Instant startInst = epochSecondsToInstant(occ.getStartTime(), obs.getPhenomenonTime());
                Instant endInst = epochSecondsToInstant(occ.getEndTime(), obs.getResultTime());
                Instant sampleInst = epochSecondsToInstant(occ.getSamplingTime(), obs.getResultTime());

                // Enforce requested [start, end] window against occupancy timestamps
                Instant checkInst = sampleInst != null ? sampleInst : (startInst != null ? startInst : endInst);
                if (checkInst != null) {
                    if (start != null && checkInst.isBefore(start)) {
                        continue;
                    }
                    if (end != null && checkInst.isAfter(end)) {
                        continue;
                    }
                }

                double durationSec = (startInst != null && endInst != null && !endInst.isBefore(startInst))
                        ? (endInst.toEpochMilli() - startInst.toEpochMilli()) / 1000.0
                        : 0.0;

                List<WebIdSummary> passWebIds = new ArrayList<>();
                for (String wId : occ.getWebIdObsIds()) {
                    WebIdSummary wSum = webIdByEncodedId.get(wId);
                    if (wSum != null) {
                        passWebIds.add(wSum);
                    }
                }

                boolean isExtended = (dsMeta != null && dsMeta.extendedSchema()) || (occ instanceof OccupancyExtended);
                String alarmCat = (occ instanceof OccupancyExtended ext && ext.getAlarmCategory() != null)
                        ? ext.getAlarmCategory().trim()
                        : "";
                String systemType = (isExtended || !passWebIds.isEmpty())
                        ? SYSTEM_TYPE_SPECTROSCOPIC
                        : SYSTEM_TYPE_GROSS;

                boolean emlAuto = occ.getAdjudicatedIds().stream()
                        .anyMatch(Adjudication.EML_NON_THREAT_MARKER::equalsIgnoreCase)
                        || (latestAdj != null && latestAdj.adjudicationCode() == Adjudication.EML_NON_THREAT_CODE);

                String alarmStatusLabel = buildAlarmStatusLabel(
                        occ.hasGammaAlarm(), occ.hasNeutronAlarm(), alarmCat, emlAuto);

                // Extract EML summary from adjudication chain if present
                String emlDecision = "";
                String emlSource = "";
                Double emlNormPct = null;
                Double emlThreatPct = null;
                for (AdjudicationDetailRecord adj : adjs) {
                    if (!adj.emlDecision().isEmpty() || !adj.emlSourceType().isEmpty()) {
                        emlDecision = adj.emlDecision();
                        emlSource = adj.emlSourceType();
                        emlNormPct = adj.emlNormProbPct();
                        emlThreatPct = adj.emlThreatProbPct();
                    }
                }

                Set<String> confirmedIsotopes = new LinkedHashSet<>();
                for (WebIdSummary w : passWebIds) {
                    for (String iso : w.isotopes()) {
                        if (iso != null && !iso.isBlank()) {
                            confirmedIsotopes.add(iso);
                        }
                    }
                }
                for (AdjudicationDetailRecord adj : adjs) {
                    for (String iso : adj.isotopes()) {
                        if (iso != null && !iso.isBlank()) {
                            confirmedIsotopes.add(iso);
                        }
                    }
                }

                results.add(new VehiclePassRecord(
                        localNodeId,
                        localNodeId,
                        false,
                        laneUid,
                        laneName,
                        datastreamId,
                        obsId,
                        occ.getOccupancyCount(),
                        systemType,
                        startInst != null ? DateTimeFormatter.ISO_INSTANT.format(startInst) : "",
                        endInst != null ? DateTimeFormatter.ISO_INSTANT.format(endInst) : "",
                        sampleInst != null ? DateTimeFormatter.ISO_INSTANT.format(sampleInst) : "",
                        Math.round(durationSec * 1000.0) / 1000.0,
                        occ.hasGammaAlarm(),
                        occ.hasNeutronAlarm(),
                        alarmCat,
                        alarmStatusLabel,
                        emlAuto,
                        occ.getMaxGammaCount(),
                        occ.getMaxNeutronCount(),
                        occ.getNeutronBackground(),
                        effectiveRaw,
                        effectiveNorm,
                        vehicleSource,
                        ocr,
                        List.copyOf(adjs),
                        latestAdj,
                        emlDecision,
                        emlSource,
                        emlNormPct,
                        emlThreatPct,
                        List.copyOf(passWebIds),
                        List.copyOf(confirmedIsotopes),
                        occ.getVideoPaths() != null ? List.copyOf(occ.getVideoPaths()) : List.of(),
                        List.of()
                ));
            } else {
                // Fallback record when occupancy observation aged past retention or uses a synthetic test ID
                String laneUid = fallbackLaneByObsId.getOrDefault(obsId, ocrLaneByObsId.getOrDefault(obsId, ""));
                if (allowedLaneFilter != null && !laneUid.isBlank() && !allowedLaneFilter.contains(laneUid)) {
                    continue;
                }
                String laneName = laneNamesByUid.getOrDefault(laneUid, laneUid.isEmpty() ? "Unknown Lane" : laneUid);
                LaneStreamMetadata dsMeta = occStreamByLaneUid.get(laneUid);
                String datastreamId = dsMeta != null ? dsMeta.encodedOccStreamId() : "";
                String tsIso = latestAdj != null && !latestAdj.timeUtcIso().isEmpty()
                        ? latestAdj.timeUtcIso()
                        : (ocr != null ? ocr.frameTimeIso() : "");
                boolean emlAuto = latestAdj != null && latestAdj.adjudicationCode() == Adjudication.EML_NON_THREAT_CODE;

                Set<String> confirmedIsotopes = new LinkedHashSet<>();
                String emlDecision = "";
                String emlSource = "";
                Double emlNormPct = null;
                Double emlThreatPct = null;
                for (AdjudicationDetailRecord adj : adjs) {
                    confirmedIsotopes.addAll(adj.isotopes());
                    if (!adj.emlDecision().isEmpty() || !adj.emlSourceType().isEmpty()) {
                        emlDecision = adj.emlDecision();
                        emlSource = adj.emlSourceType();
                        emlNormPct = adj.emlNormProbPct();
                        emlThreatPct = adj.emlThreatProbPct();
                    }
                }

                results.add(new VehiclePassRecord(
                        localNodeId,
                        localNodeId,
                        false,
                        laneUid,
                        laneName,
                        datastreamId,
                        obsId,
                        0,
                        dsMeta != null && dsMeta.extendedSchema() ? SYSTEM_TYPE_SPECTROSCOPIC : SYSTEM_TYPE_GROSS,
                        tsIso,
                        tsIso,
                        tsIso,
                        0.0,
                        latestAdj != null,
                        false,
                        "",
                        emlAuto ? "EML Auto-Released" : (latestAdj != null ? "Adjudicated Event" : "OCR Read"),
                        emlAuto,
                        0,
                        0,
                        0.0,
                        effectiveRaw,
                        effectiveNorm,
                        vehicleSource,
                        ocr,
                        List.copyOf(adjs),
                        latestAdj,
                        emlDecision,
                        emlSource,
                        emlNormPct,
                        emlThreatPct,
                        List.of(),
                        List.copyOf(confirmedIsotopes),
                        List.of(),
                        List.of()
                ));
            }
        }

        if (spec.includeProfiles() && !results.isEmpty()) {
            results.sort(Comparator.comparing(VehiclePassRecord::sortInstant)
                    .thenComparing(p -> nonNull(p.laneUid()))
                    .thenComparingInt(VehiclePassRecord::occupancyCount));
            int firstProfileIdx = Math.max(0, results.size() - MAX_PROFILED_PASSES);
            for (int i = firstProfileIdx; i < results.size(); i++) {
                VehiclePassRecord rec = results.get(i);
                if (rec.laneUid() == null || rec.laneUid().isBlank()
                        || rec.startTimeIso().isBlank() || rec.endTimeIso().isBlank()) {
                    continue;
                }
                Instant pStart;
                Instant pEnd;
                try {
                    pStart = Instant.parse(rec.startTimeIso());
                    pEnd = Instant.parse(rec.endTimeIso());
                } catch (Exception ignored) {
                    continue;
                }
                List<ProfileSample> profile = loadRadiationProfileSamples(
                        db, obsStore, rec.laneUid(), pStart, pEnd, gammaStreamsByLane, neutronStreamsByLane);
                if (!profile.isEmpty()) {
                    results.set(i, new VehiclePassRecord(
                            rec.nodeId(),
                            rec.nodeName(),
                            rec.federatedRemote(),
                            rec.laneUid(),
                            rec.laneName(),
                            rec.datastreamId(),
                            rec.occupancyObsId(),
                            rec.occupancyCount(),
                            rec.systemType(),
                            rec.startTimeIso(),
                            rec.endTimeIso(),
                            rec.samplingTimeIso(),
                            rec.durationSeconds(),
                            rec.gammaAlarm(),
                            rec.neutronAlarm(),
                            rec.alarmCategoryCode(),
                            rec.alarmStatusLabel(),
                            rec.emlAutoReleased(),
                            rec.maxGammaCount(),
                            rec.maxNeutronCount(),
                            rec.neutronBackground(),
                            rec.vehicleId(),
                            rec.normalizedVehicleId(),
                            rec.vehicleIdSource(),
                            rec.ocr(),
                            rec.adjudications(),
                            rec.latestAdjudication(),
                            rec.emlDecision(),
                            rec.emlSourceType(),
                            rec.emlNormProbPct(),
                            rec.emlThreatProbPct(),
                            rec.webIdAnalyses(),
                            rec.confirmedIsotopes(),
                            rec.videoPaths(),
                            profile
                    ));
                }
            }
        }

        return new LocalPassSelection(results, preTruncatedCount);
    }

    private static Instant estimateObsRecencyInstant(
            String obsId,
            Map<String, List<RawAdjudicationEntry>> rawAdjudicationsByObsId,
            Map<String, OcrMatchSummary> ocrByObsId
    ) {
        OcrMatchSummary ocr = ocrByObsId.get(obsId);
        if (ocr != null && ocr.frameTimeIso() != null && !ocr.frameTimeIso().isBlank()) {
            try {
                return Instant.parse(ocr.frameTimeIso());
            } catch (Exception ignored) {
            }
        }
        List<RawAdjudicationEntry> adjs = rawAdjudicationsByObsId.get(obsId);
        if (adjs != null && !adjs.isEmpty()) {
            Instant earliestAdj = null;
            for (RawAdjudicationEntry raw : adjs) {
                if (raw.reportTime() != null && (earliestAdj == null || raw.reportTime().isBefore(earliestAdj))) {
                    earliestAdj = raw.reportTime();
                }
            }
            if (earliestAdj != null) {
                return earliestAdj;
            }
        }
        return Instant.EPOCH;
    }

    private static long safeDecodedObsIdLong(IdEncoder obsIdEncoder, String encodedObsId) {
        if (obsIdEncoder == null || encodedObsId == null || encodedObsId.isBlank() || "-".equals(encodedObsId)) {
            return Long.MIN_VALUE;
        }
        try {
            BigId decoded = obsIdEncoder.decodeID(encodedObsId);
            return decoded != null ? decoded.getIdAsLong() : Long.MIN_VALUE;
        } catch (Exception ignored) {
            return Long.MIN_VALUE;
        }
    }

    private record RawAdjudicationEntry(
            int sequence,
            BigId commandId,
            Instant reportTime,
            DataBlock block,
            String occupancyObsId,
            String vehicleId
    ) {}

    private static void scanAdjudicationsSinglePass(
            IObsSystemDatabase db,
            Set<String> candidateLanes,
            Instant start,
            Instant end,
            VehicleIncidentSpec spec,
            Map<String, List<RawAdjudicationEntry>> rawAdjudicationsByObsId,
            Map<String, String> cmdStreamToLaneByKey,
            Set<String> adjMatchedObsIds
    ) {
        if (db.getCommandStatusStore() == null || db.getCommandStore() == null || candidateLanes.isEmpty()) {
            return;
        }
        Set<BigId> cmdStreamBigIds = new LinkedHashSet<>();
        try (var csEntries = db.getCommandStore().getCommandStreams().selectEntries(
                new CommandStreamFilter.Builder()
                        .withSystems().withUniqueIDs(candidateLanes).includeMembers(true).done()
                        .build())) {
            csEntries.forEach(entry -> {
                if (entry.getKey() != null
                        && isAdjudicationCommandStream(entry.getValue())
                        && entry.getValue().getSystemID() != null) {
                    BigId csId = entry.getKey().getInternalID();
                    if (csId != null) {
                        cmdStreamBigIds.add(csId);
                        cmdStreamToLaneByKey.put(bigIdLookupKey(csId), entry.getValue().getSystemID().getUniqueID());
                    }
                }
            });
        } catch (Exception ignored) {
        }
        if (cmdStreamBigIds.isEmpty()) {
            return;
        }

        CommandStatusFilter.Builder statusFilter = new CommandStatusFilter.Builder()
                .withStatus(ICommandStatus.CommandStatusCode.COMPLETED)
                .withCommands(new CommandFilter.Builder()
                        .withCommandStreams(cmdStreamBigIds)
                        .build());
        if (start != null && end != null) {
            // Widen adjudication reportTime scan slightly on end bound since operators adjudicate after occupancy endTime
            Instant widenedEnd = end.isAfter(Instant.MAX.minusSeconds(86_400L * 30L))
                    ? end
                    : end.plusSeconds(86_400L * 30L);
            statusFilter.withReportTimeDuring(start, widenedEnd);
        }

        int seq = 1;
        try (var stream = db.getCommandStatusStore().select(statusFilter.build())) {
            var it = stream.iterator();
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
                    int isotopeCount = Math.max(0, block.getIntValue(3));
                    int fileCountIdx = 5 + isotopeCount;
                    int fileCount = Math.max(0, block.getIntValue(fileCountIdx));
                    int occIdx = fileCountIdx + 1 + fileCount;
                    String occupancyObsId = nonNull(block.getStringValue(occIdx));
                    String vehicleId = nonNull(block.getStringValue(occIdx + 1));
                    if (occupancyObsId.isBlank()) {
                        occupancyObsId = "adj-cmd-" + (entry.getCommandID() != null ? entry.getCommandID().toString() : seq);
                    }

                    rawAdjudicationsByObsId.computeIfAbsent(occupancyObsId, k -> new ArrayList<>())
                            .add(new RawAdjudicationEntry(
                                    seq++,
                                    entry.getCommandID(),
                                    entry.getReportTime(),
                                    block,
                                    occupancyObsId,
                                    vehicleId));

                    String normVehicle = normalizeVehicleId(vehicleId);
                    if (spec.matchMode().matches(normVehicle, spec.normalizedVehicleId())) {
                        adjMatchedObsIds.add(occupancyObsId);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void materializeMatchedAdjudications(
            IObsSystemDatabase db,
            Set<String> targetObsIds,
            Map<String, List<RawAdjudicationEntry>> rawAdjudicationsByObsId,
            Map<String, IObsData> loadedOccupancies,
            Map<String, LaneStreamMetadata> occStreamByDsKey,
            Map<String, String> ocrLaneByObsId,
            Map<String, String> cmdStreamToLaneByKey,
            Map<Integer, DispositionCodeSpec> dispSpecs,
            Map<String, List<AdjudicationDetailRecord>> adjudicationsByObsId,
            Map<String, String> fallbackLaneByObsId
    ) {
        Map<String, String> cmdIdToLaneCache = new HashMap<>();
        for (String obsId : targetObsIds) {
            List<RawAdjudicationEntry> rawList = rawAdjudicationsByObsId.get(obsId);
            if (rawList == null || rawList.isEmpty()) {
                continue;
            }
            IObsData loadedObs = loadedOccupancies.get(obsId);
            LaneStreamMetadata dsMeta = loadedObs != null
                    ? occStreamByDsKey.get(bigIdLookupKey(loadedObs.getDataStreamID()))
                    : null;
            String knownLaneUid = dsMeta != null ? dsMeta.laneUid() : ocrLaneByObsId.getOrDefault(obsId, "");

            List<AdjudicationDetailRecord> materialized = new ArrayList<>(rawList.size());
            for (RawAdjudicationEntry raw : rawList) {
                try {
                    DataBlock block = raw.block();
                    int idx = 0;
                    String username = nonNull(block.getStringValue(idx++));
                    String feedback = nonNull(block.getStringValue(idx++));
                    int code = block.getIntValue(idx++);
                    int isotopeCount = Math.max(0, block.getIntValue(idx++));
                    List<String> isotopes = new ArrayList<>(isotopeCount);
                    for (int i = 0; i < isotopeCount; i++) {
                        String iso = nonNull(block.getStringValue(idx++));
                        if (!iso.isBlank()) {
                            isotopes.add(iso);
                        }
                    }
                    String secondaryStatus = nonNull(block.getStringValue(idx++));
                    int fileCount = Math.max(0, block.getIntValue(idx++));
                    List<String> filePaths = new ArrayList<>(fileCount);
                    for (int i = 0; i < fileCount; i++) {
                        String fp = nonNull(block.getStringValue(idx++));
                        if (!fp.isBlank()) {
                            filePaths.add(fp);
                        }
                    }

                    String laneUid = knownLaneUid;
                    if (laneUid.isBlank() && raw.commandId() != null && db.getCommandStore() != null) {
                        BigId cmdId = raw.commandId();
                        laneUid = cmdIdToLaneCache.computeIfAbsent(bigIdLookupKey(cmdId), ignored -> {
                            try {
                                ICommandData cmd = db.getCommandStore().get(cmdId);
                                return cmd != null
                                        ? cmdStreamToLaneByKey.getOrDefault(bigIdLookupKey(cmd.getCommandStreamID()), "")
                                        : "";
                            } catch (Exception e) {
                                return "";
                            }
                        });
                    }
                    if (!laneUid.isBlank()) {
                        fallbackLaneByObsId.putIfAbsent(obsId, laneUid);
                    }

                    DispositionCodeSpec matchedSpec = dispSpecs.computeIfAbsent(
                            code, c -> new DispositionCodeSpec(c, "Code " + c + ": Custom Disposition", "Custom"));
                    String formattedCode = !matchedSpec.label().isBlank()
                            ? ((code == Adjudication.EML_NON_THREAT_CODE || code == Adjudication.EXTENDED_OCCUPANCY_GAMMA_CODE)
                                ? (code + " (" + matchedSpec.label() + ")")
                                : matchedSpec.label())
                            : String.valueOf(code);

                    String reportTimeStr = raw.reportTime() != null
                            ? ReportDataAggregator.UTC_DATETIME_SHORT_FMT.format(raw.reportTime())
                            : "-";
                    String reportTimeIso = raw.reportTime() != null
                            ? DateTimeFormatter.ISO_INSTANT.format(raw.reportTime())
                            : "";

                    ParsedEml parsedEml = parseEmlFeedback(feedback);
                    materialized.add(new AdjudicationDetailRecord(
                            laneUid,
                            raw.sequence(),
                            reportTimeIso,
                            reportTimeStr,
                            username.isEmpty() ? "-" : username,
                            code,
                            formattedCode,
                            matchedSpec.group(),
                            List.copyOf(isotopes),
                            secondaryStatus.isEmpty() ? "-" : secondaryStatus,
                            raw.vehicleId().isEmpty() ? "-" : raw.vehicleId(),
                            raw.occupancyObsId(),
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
                }
            }
            if (!materialized.isEmpty()) {
                materialized.sort(Comparator.comparing(AdjudicationDetailRecord::timeUtcIso));
                adjudicationsByObsId.put(obsId, materialized);
            }
        }
    }

    private static boolean isAdjudicationCommandStream(org.sensorhub.api.command.ICommandStreamInfo cs) {
        if (cs == null) {
            return false;
        }
        String inputName = cs.getControlInputName();
        if (AdjudicationControl.NAME.equalsIgnoreCase(inputName) || "adjudication".equalsIgnoreCase(inputName)) {
            return true;
        }
        if (cs.getResultStructure() != null
                && AdjudicationControl.RES_NAME.equalsIgnoreCase(cs.getResultStructure().getName())) {
            return true;
        }
        return cs.getRecordStructure() != null
                && RADHelper.DEF_ADJUDICATION.equals(cs.getRecordStructure().getDefinition());
    }

    private static void scanVehicleOcrObservations(
            IObsSystemDatabase db,
            IObsStore obsStore,
            String[] explicitLanes,
            Set<String> allowedLaneFilter,
            LaneTopology laneTopology,
            Instant start,
            Instant end,
            VehicleIncidentSpec spec,
            Map<String, OcrMatchSummary> ocrByObsId,
            Map<String, String> ocrLaneByObsId,
            Set<String> ocrMatchedObsIds
    ) {
        Map<BigId, String> ocrStreamToLane = findVehicleOcrStreams(db, explicitLanes, allowedLaneFilter, laneTopology);
        if (ocrStreamToLane.isEmpty()) {
            return;
        }
        Map<String, String> ocrStreamToLaneByKey = new HashMap<>(ocrStreamToLane.size() * 2);
        for (Map.Entry<BigId, String> e : ocrStreamToLane.entrySet()) {
            ocrStreamToLaneByKey.put(bigIdLookupKey(e.getKey()), e.getValue());
        }

        ObsFilter.Builder filterBuilder = new ObsFilter.Builder()
                .withDataStreams(ocrStreamToLane.keySet());
        if (start != null && end != null) {
            Instant widenedStart = start.isBefore(Instant.MIN.plusSeconds(3_600L)) ? start : start.minusSeconds(3_600L);
            Instant widenedEnd = end.isAfter(Instant.MAX.minusSeconds(120L)) ? end : end.plusSeconds(120L);
            filterBuilder.withPhenomenonTimeDuring(widenedStart, widenedEnd);
            filterBuilder.withResultTimeDuring(start, widenedEnd);
        }

        // Safe OGC CQL2 pushdown on canonical uppercase alphanumeric normalizedValue
        String normQuery = spec.normalizedVehicleId();
        if (normQuery != null && normQuery.length() >= 2) {
            switch (spec.matchMode()) {
                case EQUALS -> filterBuilder.withCQLFilter("normalizedValue = '" + normQuery + "'");
                case STARTS_WITH -> filterBuilder.withCQLFilter("normalizedValue LIKE '" + normQuery + "%'");
                case CONTAINS -> filterBuilder.withCQLFilter("normalizedValue LIKE '%" + normQuery + "%'");
                default -> {
                }
            }
        }

        try (var stream = obsStore.select(filterBuilder.build())) {
            stream.forEach(obs -> {
                if (obs == null || obs.getResult() == null) {
                    return;
                }
                try {
                    VehicleOcrResult ocr = VehicleOcrResult.toVehicleOcrResult(obs.getResult());
                    String occObsId = nonNull(ocr.getOccupancyObsId()).trim();
                    if (occObsId.isEmpty()) {
                        return;
                    }
                    String candNorm = !ocr.getNormalizedValue().isBlank()
                            ? normalizeVehicleId(ocr.getNormalizedValue())
                            : normalizeVehicleId(ocr.getValue());
                    if (!spec.matchMode().matches(candNorm, spec.normalizedVehicleId())) {
                        return;
                    }
                    OcrMatchSummary summary = toOcrSummary(ocr, candNorm);
                    OcrMatchSummary prev = ocrByObsId.get(occObsId);
                    if (prev == null || summary.confidence() >= prev.confidence()) {
                        ocrByObsId.put(occObsId, summary);
                    }
                    String laneUid = ocrStreamToLaneByKey.getOrDefault(bigIdLookupKey(obs.getDataStreamID()), "");
                    if (!laneUid.isBlank()) {
                        ocrLaneByObsId.putIfAbsent(occObsId, laneUid);
                    }
                    ocrMatchedObsIds.add(occObsId);
                } catch (Exception ignored) {
                }
            });
        }
    }

    private static void enrichMissingOcrForMatchedOccupancies(
            IObsSystemDatabase db,
            IObsStore obsStore,
            String[] explicitLanes,
            LaneTopology laneTopology,
            Instant start,
            Instant end,
            Set<String> targetObsIds,
            Map<String, OcrMatchSummary> ocrByObsId
    ) {
        Set<String> missingOcrObsIds = new LinkedHashSet<>(targetObsIds);
        missingOcrObsIds.removeAll(ocrByObsId.keySet());
        if (missingOcrObsIds.isEmpty() || missingOcrObsIds.size() > 64) {
            return;
        }
        Map<BigId, String> ocrStreamToLane = findVehicleOcrStreams(db, explicitLanes, null, laneTopology);
        if (ocrStreamToLane.isEmpty()) {
            return;
        }
        List<String> safeClauses = new ArrayList<>();
        for (String obsId : missingOcrObsIds) {
            if (obsId != null && obsId.matches("^[A-Za-z0-9_-]{3,64}$")) {
                safeClauses.add("occupancyObsId = '" + obsId + "'");
            }
        }
        if (safeClauses.isEmpty()) {
            return;
        }
        ObsFilter.Builder fb = new ObsFilter.Builder()
                .withDataStreams(ocrStreamToLane.keySet())
                .withCQLFilter(String.join(" OR ", safeClauses));
        if (start != null && end != null) {
            Instant widenedStart = start.isBefore(Instant.MIN.plusSeconds(3_600L)) ? start : start.minusSeconds(3_600L);
            Instant widenedEnd = end.isAfter(Instant.MAX.minusSeconds(300L)) ? end : end.plusSeconds(300L);
            fb.withPhenomenonTimeDuring(widenedStart, widenedEnd);
            fb.withResultTimeDuring(start, widenedEnd);
        }
        try (var stream = obsStore.select(fb.build())) {
            stream.forEach(obs -> {
                if (obs == null || obs.getResult() == null) {
                    return;
                }
                try {
                    VehicleOcrResult ocr = VehicleOcrResult.toVehicleOcrResult(obs.getResult());
                    String occObsId = nonNull(ocr.getOccupancyObsId()).trim();
                    if (!missingOcrObsIds.contains(occObsId)) {
                        return;
                    }
                    String candNorm = !ocr.getNormalizedValue().isBlank()
                            ? normalizeVehicleId(ocr.getNormalizedValue())
                            : normalizeVehicleId(ocr.getValue());
                    OcrMatchSummary summary = toOcrSummary(ocr, candNorm);
                    OcrMatchSummary prev = ocrByObsId.get(occObsId);
                    if (prev == null || summary.confidence() >= prev.confidence()) {
                        ocrByObsId.put(occObsId, summary);
                    }
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private static Map<BigId, String> findVehicleOcrStreams(
            IObsSystemDatabase db,
            String[] explicitLanes,
            Set<String> allowedLaneFilter,
            LaneTopology laneTopology
    ) {
        Map<BigId, String> ocrStreamToLane = new LinkedHashMap<>();
        if (db.getDataStreamStore() == null) {
            return ocrStreamToLane;
        }
        for (DataStreamFilter filter : List.of(
                buildOcrStreamFilter(explicitLanes, true),
                buildOcrStreamFilter(explicitLanes, false))) {
            try (var stream = db.getDataStreamStore().selectEntries(filter)) {
                stream.forEach(entry -> {
                    BigId dsId = entry.getKey().getInternalID();
                    IDataStreamInfo info = entry.getValue();
                    if (dsId == null || info == null || info.getSystemID() == null) {
                        return;
                    }
                    String sysUid = info.getSystemID().getUniqueID();
                    String laneUid = resolveOwningLaneUid(sysUid, laneTopology);
                    if (allowedLaneFilter != null && !allowedLaneFilter.contains(laneUid)
                            && !allowedLaneFilter.contains(sysUid)) {
                        return;
                    }
                    ocrStreamToLane.putIfAbsent(dsId, laneUid);
                });
            } catch (Exception ignored) {
            }
        }
        return ocrStreamToLane;
    }

    private static DataStreamFilter buildOcrStreamFilter(String[] explicitLanes, boolean byOutputName) {
        DataStreamFilter.Builder b = new DataStreamFilter.Builder();
        if (byOutputName) {
            b.withOutputNames(VEHICLE_OCR_OUTPUT_NAME);
        } else {
            b.withObservedProperties(DEF_VEHICLE_OCR, DEF_VEHICLE_ID);
        }
        if (explicitLanes != null && explicitLanes.length > 0) {
            b.withSystems().withUniqueIDs(explicitLanes).includeMembers(true).done();
        }
        return b.build();
    }

    private static OcrMatchSummary toOcrSummary(VehicleOcrResult ocr, String candNorm) {
        Instant ft = ocr.getFrameTime() != null ? ocr.getFrameTime() : ocr.getSampleTime();
        return new OcrMatchSummary(
                nonNull(ocr.getValue()),
                candNorm,
                nonNull(ocr.getIdType()),
                ocr.getConfidence(),
                ocr.isChecksumValid(),
                ocr.getReadCount(),
                nonNull(ocr.getCameraUid()),
                ft != null ? DateTimeFormatter.ISO_INSTANT.format(ft) : "",
                nonNull(ocr.getEvidenceImagePath())
        );
    }

    private static List<ProfileSample> loadRadiationProfileSamples(
            IObsSystemDatabase db,
            IObsStore obsStore,
            String laneUid,
            Instant startInst,
            Instant endInst,
            Map<String, Set<BigId>> gammaStreamsByLane,
            Map<String, Set<BigId>> neutronStreamsByLane
    ) {
        Instant qStart = startInst.minusMillis(1_000L);
        Instant qEnd = endInst.plusMillis(2_000L);
        if (qEnd.toEpochMilli() - qStart.toEpochMilli() > 600_000L) {
            qEnd = qStart.plusSeconds(600L);
        }
        Instant rEnd = qEnd.isAfter(Instant.MAX.minusSeconds(60L)) ? qEnd : qEnd.plusSeconds(60L);
        Set<BigId> gammaStreams = gammaStreamsByLane.computeIfAbsent(
                laneUid, k -> resolveObservedPropertyStreams(db, k, RADHelper.DEF_GAMMA));
        Set<BigId> neutronStreams = neutronStreamsByLane.computeIfAbsent(
                laneUid, k -> resolveObservedPropertyStreams(db, k, RADHelper.DEF_NEUTRON));

        Map<Long, int[]> countsByBucketMs = new LinkedHashMap<>();
        if (!gammaStreams.isEmpty()) {
            try (var gStream = obsStore.select(new ObsFilter.Builder()
                    .withDataStreams(gammaStreams)
                    .withPhenomenonTimeDuring(qStart, qEnd)
                    .withResultTimeDuring(qStart, rEnd)
                    .withLimit(MAX_PROFILE_SAMPLES_PER_PASS)
                    .build())) {
                gStream.forEach(obs -> {
                    if (obs == null || obs.getPhenomenonTime() == null || obs.getResult() == null) {
                        return;
                    }
                    long ts = obs.getPhenomenonTime().toEpochMilli();
                    int sum = extractDetectorCountSum(obs.getResult());
                    countsByBucketMs.computeIfAbsent(ts, k -> new int[]{0, 0})[0] = Math.max(
                            countsByBucketMs.get(ts)[0], sum);
                });
            } catch (Exception ignored) {
            }
        }

        if (!neutronStreams.isEmpty()) {
            try (var nStream = obsStore.select(new ObsFilter.Builder()
                    .withDataStreams(neutronStreams)
                    .withPhenomenonTimeDuring(qStart, qEnd)
                    .withResultTimeDuring(qStart, rEnd)
                    .withLimit(MAX_PROFILE_SAMPLES_PER_PASS)
                    .build())) {
                nStream.forEach(obs -> {
                    if (obs == null || obs.getPhenomenonTime() == null || obs.getResult() == null) {
                        return;
                    }
                    long ts = obs.getPhenomenonTime().toEpochMilli();
                    int sum = extractDetectorCountSum(obs.getResult());
                    countsByBucketMs.computeIfAbsent(ts, k -> new int[]{0, 0})[1] = Math.max(
                            countsByBucketMs.get(ts)[1], sum);
                });
            } catch (Exception ignored) {
            }
        }

        if (countsByBucketMs.isEmpty()) {
            return List.of();
        }
        List<Long> sortedTs = new ArrayList<>(countsByBucketMs.keySet());
        Collections.sort(sortedTs);
        long baseMs = startInst.toEpochMilli();
        List<ProfileSample> samples = new ArrayList<>(Math.min(sortedTs.size(), MAX_PROFILE_SAMPLES_PER_PASS));
        for (int i = 0; i < sortedTs.size() && samples.size() < MAX_PROFILE_SAMPLES_PER_PASS; i++) {
            long ts = sortedTs.get(i);
            int[] pair = countsByBucketMs.get(ts);
            double offsetSec = Math.round(((ts - baseMs) / 1000.0) * 1000.0) / 1000.0;
            samples.add(new ProfileSample(ts, offsetSec, pair[0], pair[1]));
        }
        return List.copyOf(samples);
    }

    private static Set<BigId> resolveObservedPropertyStreams(
            IObsSystemDatabase db,
            String laneUid,
            String observedProperty
    ) {
        if (db == null || db.getDataStreamStore() == null || laneUid == null || laneUid.isBlank()) {
            return Set.of();
        }
        Set<BigId> out = new LinkedHashSet<>();
        try (var stream = db.getDataStreamStore().selectKeys(new DataStreamFilter.Builder()
                .withSystems().withUniqueIDs(laneUid).includeMembers(true).done()
                .withObservedProperties(observedProperty)
                .build())) {
            stream.forEach(key -> {
                if (key != null && key.getInternalID() != null) {
                    out.add(key.getInternalID());
                }
            });
        } catch (Exception ignored) {
        }
        return Set.copyOf(out);
    }

    private static int extractDetectorCountSum(DataBlock block) {
        if (block == null || block.getAtomCount() <= 1) {
            return 0;
        }
        // Atom 0 is samplingTime; remaining integer/numeric count atoms hold pillar or gross counts
        // Avoid double-counting if atom 1 is gross sum and remaining are pillars: take atom 1 when scalar gross count
        try {
            if (block.getAtomCount() == 2) {
                return Math.max(0, block.getIntValue(1));
            }
            int sum = 0;
            for (int i = 1; i < block.getAtomCount(); i++) {
                double v = block.getDoubleValue(i);
                if (Double.isFinite(v) && v >= 0 && v < 10_000_000) {
                    sum += (int) Math.round(v);
                }
            }
            return sum;
        } catch (Exception e) {
            return 0;
        }
    }

    public static void writeJsonPayload(JsonWriter jw, VehicleIncidentData data, boolean isReport) throws IOException {
        jw.beginObject();
        if (isReport) {
            jw.name("reportType").value(ReportCmdType.VEHICLE_INCIDENT.name());
            jw.name("reportTitle").value("Vehicle Incident Report");
        } else {
            jw.name("schemaVersion").value(1);
        }
        jw.name("nodeId").value(data.nodeId());
        jw.name("vehicleId").value(data.formattedVehicleId());
        jw.name("rawVehicleId").value(data.rawVehicleId());
        jw.name("normalizedVehicleId").value(data.normalizedVehicleId());
        jw.name("matchMode").value(data.matchMode().wireName());
        jw.name("scope").value(data.scopeSummary());
        jw.name("startDateTime").value(data.start() != null ? DateTimeFormatter.ISO_INSTANT.format(data.start()) : "");
        jw.name("endDateTime").value(data.end() != null ? DateTimeFormatter.ISO_INSTANT.format(data.end()) : "");
        jw.name("generatedAt").value(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        jw.name("totalMatched").value(data.totalMatchedBeforeLimit());
        jw.name("truncated").value(data.truncated());

        jw.name("summary").beginObject();
        jw.name("totalPasses").value(data.passes().size());
        jw.name("alarmingPasses").value(data.alarmingPassCount());
        jw.name("openAlarmPasses").value(data.openAlarmPassCount());
        jw.name("emlAutoReleasePasses").value(data.emlAutoReleasePassCount());
        jw.name("spectroscopicPasses").value(data.spectroscopicPassCount());
        jw.name("distinctLanes").value(data.distinctLaneCount());
        jw.name("distinctNodes").value(data.distinctNodeCount());
        jw.name("latestDisposition").value(data.latestDispositionLabel());
        jw.name("latestDispositionGroup").value(data.latestDispositionGroup());
        jw.name("confirmedIsotopes").beginArray();
        for (String iso : data.confirmedIsotopes()) {
            jw.value(iso);
        }
        jw.endArray();
        jw.endObject();

        jw.name("passes").beginArray();
        for (VehiclePassRecord pass : data.passes()) {
            writePassJson(jw, pass);
        }
        jw.endArray();
        jw.endObject();
    }

    private static void writePassJson(JsonWriter jw, VehiclePassRecord pass) throws IOException {
        jw.beginObject();
        jw.name("nodeId").value(pass.nodeId());
        jw.name("nodeName").value(pass.nodeName());
        jw.name("federatedRemote").value(pass.federatedRemote());
        jw.name("laneUid").value(pass.laneUid());
        jw.name("laneName").value(pass.laneName());
        jw.name("datastreamId").value(pass.datastreamId());
        jw.name("occupancyObsId").value(pass.occupancyObsId());
        jw.name("occupancyCount").value(pass.occupancyCount());
        jw.name("systemType").value(pass.systemType());
        jw.name("startTime").value(pass.startTimeIso());
        jw.name("endTime").value(pass.endTimeIso());
        jw.name("samplingTime").value(pass.samplingTimeIso());
        jw.name("durationSeconds").value(pass.durationSeconds());
        jw.name("gammaAlarm").value(pass.gammaAlarm());
        jw.name("neutronAlarm").value(pass.neutronAlarm());
        jw.name("alarmCategoryCode").value(pass.alarmCategoryCode());
        jw.name("alarmStatusLabel").value(pass.alarmStatusLabel());
        jw.name("emlAutoReleased").value(pass.emlAutoReleased());
        jw.name("maxGammaCount").value(pass.maxGammaCount());
        jw.name("maxNeutronCount").value(pass.maxNeutronCount());
        jw.name("neutronBackground").value(pass.neutronBackground());
        jw.name("vehicleId").value(pass.vehicleId());
        jw.name("normalizedVehicleId").value(pass.normalizedVehicleId());
        jw.name("vehicleIdSource").value(pass.vehicleIdSource());

        if (pass.ocr() != null) {
            jw.name("ocr").beginObject();
            jw.name("value").value(pass.ocr().value());
            jw.name("normalizedValue").value(pass.ocr().normalizedValue());
            jw.name("idType").value(pass.ocr().idType());
            jw.name("confidence").value(Math.round(pass.ocr().confidence() * 10000.0) / 10000.0);
            jw.name("checksumValid").value(pass.ocr().checksumValid());
            jw.name("readCount").value(pass.ocr().readCount());
            jw.name("cameraUid").value(pass.ocr().cameraUid());
            jw.name("frameTime").value(pass.ocr().frameTimeIso());
            jw.name("evidenceImagePath").value(pass.ocr().evidenceImagePath());
            jw.endObject();
        }

        if (!pass.emlDecision().isEmpty() || !pass.emlSourceType().isEmpty()
                || pass.emlNormProbPct() != null || pass.emlThreatProbPct() != null) {
            jw.name("eml").beginObject();
            jw.name("decision").value(pass.emlDecision());
            jw.name("sourceType").value(pass.emlSourceType());
            if (pass.emlNormProbPct() != null) {
                jw.name("normProbabilityPct").value(Math.round(pass.emlNormProbPct() * 100.0) / 100.0);
            }
            if (pass.emlThreatProbPct() != null) {
                jw.name("threatProbabilityPct").value(Math.round(pass.emlThreatProbPct() * 100.0) / 100.0);
            }
            jw.endObject();
        }

        jw.name("confirmedIsotopes").beginArray();
        for (String iso : pass.confirmedIsotopes()) {
            jw.value(iso);
        }
        jw.endArray();

        jw.name("videoPaths").beginArray();
        for (String vp : pass.videoPaths()) {
            jw.value(vp);
        }
        jw.endArray();

        jw.name("webIdAnalyses").beginArray();
        for (WebIdSummary w : pass.webIdAnalyses()) {
            jw.beginObject();
            jw.name("obsId").value(w.obsId());
            jw.name("sampleTime").value(w.sampleTimeIso());
            jw.name("isotopeString").value(w.isotopeString());
            jw.name("isotopes").beginArray();
            for (String iso : w.isotopes()) {
                jw.value(iso);
            }
            jw.endArray();
            jw.name("drf").value(w.drf());
            jw.name("estimatedDose").value(w.estimatedDose());
            jw.name("chi2").value(w.chi2());
            jw.name("errorMessage").value(w.errorMessage());
            jw.name("warnings").beginArray();
            for (String warn : w.warnings()) {
                jw.value(warn);
            }
            jw.endArray();
            jw.endObject();
        }
        jw.endArray();

        jw.name("adjudications").beginArray();
        for (AdjudicationDetailRecord adj : pass.adjudications()) {
            jw.beginObject();
            jw.name("timeUtc").value(adj.timeUtcIso().isEmpty() ? adj.timeUtcDisplay() : adj.timeUtcIso());
            jw.name("username").value(adj.username());
            jw.name("adjudicationCode").value(adj.adjudicationCode());
            jw.name("dispositionLabel").value(adj.dispositionLabel());
            jw.name("dispositionGroup").value(adj.dispositionGroup());
            jw.name("secondaryInspectionStatus").value(adj.secondaryInspectionStatus());
            jw.name("vehicleId").value(adj.vehicleId());
            jw.name("summary").value(adj.formattedSummary());
            jw.name("isotopes").beginArray();
            for (String iso : adj.isotopes()) {
                jw.value(iso);
            }
            jw.endArray();
            jw.name("filePaths").beginArray();
            for (String fp : adj.filePaths()) {
                jw.value(fp);
            }
            jw.endArray();
            jw.endObject();
        }
        jw.endArray();

        if (!pass.radiationProfile().isEmpty()) {
            jw.name("radiationProfile").beginArray();
            for (ProfileSample sample : pass.radiationProfile()) {
                jw.beginObject();
                jw.name("timestampMs").value(sample.timestampMs());
                jw.name("offsetSeconds").value(sample.offsetSeconds());
                jw.name("gammaCounts").value(sample.gammaCounts());
                jw.name("neutronCounts").value(sample.neutronCounts());
                jw.endObject();
            }
            jw.endArray();
        }

        jw.endObject();
    }

    public static List<VehiclePassRecord> parseFederatedPasses(JsonArray arr) {
        if (arr == null || arr.isEmpty()) {
            return List.of();
        }
        int cap = Math.min(arr.size(), MAX_FEDERATED_PASSES);
        List<VehiclePassRecord> parsed = new ArrayList<>(cap);
        for (int i = 0; i < cap; i++) {
            JsonElement el = arr.get(i);
            if (el == null || !el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String nodeId = getJsonString(o, "nodeId");
            if (nodeId.isEmpty()) {
                nodeId = "federated-node";
            }
            String nodeName = getJsonString(o, "nodeName");
            if (nodeName.isEmpty()) {
                nodeName = nodeId;
            }
            String laneUid = getJsonString(o, "laneUid");
            String laneName = getJsonString(o, "laneName");
            if (laneName.isEmpty()) {
                laneName = laneUid.isEmpty() ? "Remote Lane" : laneUid;
            }
            String occObsId = getJsonString(o, "occupancyObsId");
            if (occObsId.isEmpty()) {
                occObsId = "fed-pass-" + i;
            }
            String rawVehicle = getJsonString(o, "vehicleId");
            String normVehicle = getJsonString(o, "normalizedVehicleId");
            if (normVehicle.isEmpty()) {
                normVehicle = normalizeVehicleId(rawVehicle);
            } else {
                normVehicle = normalizeVehicleId(normVehicle);
            }
            String formattedVehicle = formatVehicleIdDisplay(rawVehicle, normVehicle);

            OcrMatchSummary ocr = null;
            if (o.has("ocr") && o.get("ocr").isJsonObject()) {
                JsonObject ocrObj = o.getAsJsonObject("ocr");
                String ocrVal = getJsonString(ocrObj, "value");
                String ocrNorm = normalizeVehicleId(getJsonString(ocrObj, "normalizedValue"));
                if (ocrNorm.isEmpty()) {
                    ocrNorm = normalizeVehicleId(ocrVal);
                }
                ocr = new OcrMatchSummary(
                        ocrVal,
                        ocrNorm,
                        getJsonString(ocrObj, "idType"),
                        (float) getJsonDoubleOrZero(ocrObj, "confidence"),
                        getJsonBoolean(ocrObj, "checksumValid"),
                        getJsonIntOrZero(ocrObj, "readCount"),
                        getJsonString(ocrObj, "cameraUid"),
                        getJsonString(ocrObj, "frameTime"),
                        getJsonString(ocrObj, "evidenceImagePath")
                );
            }

            List<AdjudicationDetailRecord> adjs = new ArrayList<>();
            if (o.has("adjudications") && o.get("adjudications").isJsonArray()) {
                JsonArray adjArr = o.getAsJsonArray("adjudications");
                int seq = 1;
                for (JsonElement aEl : adjArr) {
                    if (aEl == null || !aEl.isJsonObject()) {
                        continue;
                    }
                    JsonObject aObj = aEl.getAsJsonObject();
                    String tUtc = getJsonString(aObj, "timeUtc");
                    int code = getJsonIntOrZero(aObj, "adjudicationCode");
                    String dispLabel = getJsonString(aObj, "dispositionLabel");
                    if (dispLabel.isEmpty()) {
                        dispLabel = "Code " + code;
                    }
                    String dispGroup = getJsonString(aObj, "dispositionGroup");
                    if (dispGroup.isEmpty()) {
                        dispGroup = "Other";
                    }
                    String summary = getJsonString(aObj, "summary");
                    adjs.add(new AdjudicationDetailRecord(
                            laneUid,
                            seq++,
                            tUtc,
                            tUtc,
                            defaultIfBlank(getJsonString(aObj, "username"), "-"),
                            code,
                            dispLabel,
                            dispGroup,
                            readJsonStringList(aObj, "isotopes"),
                            defaultIfBlank(getJsonString(aObj, "secondaryInspectionStatus"), "-"),
                            defaultIfBlank(getJsonString(aObj, "vehicleId"), "-"),
                            occObsId,
                            summary,
                            summary,
                            "",
                            "",
                            null,
                            null,
                            null,
                            null,
                            null,
                            readJsonStringList(aObj, "filePaths")
                    ));
                }
            }
            AdjudicationDetailRecord latestAdj = adjs.isEmpty() ? null : adjs.get(adjs.size() - 1);

            String emlDecision = "";
            String emlSource = "";
            Double emlNormPct = null;
            Double emlThreatPct = null;
            if (o.has("eml") && o.get("eml").isJsonObject()) {
                JsonObject emlObj = o.getAsJsonObject("eml");
                emlDecision = getJsonString(emlObj, "decision");
                emlSource = getJsonString(emlObj, "sourceType");
                emlNormPct = getJsonNullableDouble(emlObj, "normProbabilityPct");
                emlThreatPct = getJsonNullableDouble(emlObj, "threatProbabilityPct");
            }

            List<WebIdSummary> webIds = new ArrayList<>();
            if (o.has("webIdAnalyses") && o.get("webIdAnalyses").isJsonArray()) {
                for (JsonElement wEl : o.getAsJsonArray("webIdAnalyses")) {
                    if (wEl == null || !wEl.isJsonObject()) {
                        continue;
                    }
                    JsonObject wObj = wEl.getAsJsonObject();
                    webIds.add(new WebIdSummary(
                            getJsonString(wObj, "obsId"),
                            getJsonString(wObj, "sampleTime"),
                            getJsonString(wObj, "isotopeString"),
                            readJsonStringList(wObj, "isotopes"),
                            getJsonString(wObj, "drf"),
                            getJsonDoubleOrZero(wObj, "estimatedDose"),
                            getJsonDoubleOrZero(wObj, "chi2"),
                            getJsonString(wObj, "errorMessage"),
                            readJsonStringList(wObj, "warnings")
                    ));
                }
            }

            boolean gAlarm = getJsonBoolean(o, "gammaAlarm");
            boolean nAlarm = getJsonBoolean(o, "neutronAlarm");
            String alarmCat = getJsonString(o, "alarmCategoryCode");
            boolean emlAuto = getJsonBoolean(o, "emlAutoReleased")
                    || (latestAdj != null && latestAdj.adjudicationCode() == Adjudication.EML_NON_THREAT_CODE);
            String alarmLabel = getJsonString(o, "alarmStatusLabel");
            if (alarmLabel.isEmpty()) {
                alarmLabel = buildAlarmStatusLabel(gAlarm, nAlarm, alarmCat, emlAuto);
            }
            String sysType = getJsonString(o, "systemType");
            if (sysType.isEmpty()) {
                sysType = (!alarmCat.isEmpty() || !webIds.isEmpty()) ? SYSTEM_TYPE_SPECTROSCOPIC : SYSTEM_TYPE_GROSS;
            }

            Set<String> isotopes = new LinkedHashSet<>(readJsonStringList(o, "confirmedIsotopes"));
            for (WebIdSummary w : webIds) {
                isotopes.addAll(w.isotopes());
            }
            for (AdjudicationDetailRecord adj : adjs) {
                isotopes.addAll(adj.isotopes());
            }

            parsed.add(new VehiclePassRecord(
                    nodeId,
                    nodeName,
                    true,
                    laneUid,
                    laneName,
                    getJsonString(o, "datastreamId"),
                    occObsId,
                    getJsonIntOrZero(o, "occupancyCount"),
                    sysType,
                    getJsonString(o, "startTime"),
                    getJsonString(o, "endTime"),
                    getJsonString(o, "samplingTime"),
                    getJsonDoubleOrZero(o, "durationSeconds"),
                    gAlarm,
                    nAlarm,
                    alarmCat,
                    alarmLabel,
                    emlAuto,
                    getJsonIntOrZero(o, "maxGammaCount"),
                    getJsonIntOrZero(o, "maxNeutronCount"),
                    getJsonDoubleOrZero(o, "neutronBackground"),
                    formattedVehicle,
                    normVehicle,
                    defaultIfBlank(getJsonString(o, "vehicleIdSource"), "adjudication"),
                    ocr,
                    List.copyOf(adjs),
                    latestAdj,
                    emlDecision,
                    emlSource,
                    emlNormPct,
                    emlThreatPct,
                    List.copyOf(webIds),
                    List.copyOf(isotopes),
                    readJsonStringList(o, "videoPaths"),
                    List.of()
            ));
        }
        return List.copyOf(parsed);
    }

    private record LaneTopology(
            Map<String, String> laneNamesByUid,
            Map<String, String> memberToLaneUid
    ) {}

    private static LaneTopology discoverLaneTopology(OSCARServiceModule module, IObsSystemDatabase db) {
        Map<String, String> names = new LinkedHashMap<>();
        Map<String, String> memberToLane = new LinkedHashMap<>();
        if (module != null && module.getParentHub() != null && module.getParentHub().getModuleRegistry() != null) {
            for (IModule<?> mod : module.getParentHub().getModuleRegistry().getLoadedModules()) {
                if (mod instanceof LaneSystem lane && lane.getUniqueIdentifier() != null && !lane.getUniqueIdentifier().isBlank()) {
                    String uid = lane.getUniqueIdentifier().trim();
                    String name = (lane.getName() != null && !lane.getName().isBlank()) ? lane.getName().trim() : uid;
                    names.put(uid, name);
                    try {
                        for (ISystemDriver member : lane.getMembers().values()) {
                            if (member != null && member.getUniqueIdentifier() != null && !member.getUniqueIdentifier().isBlank()) {
                                memberToLane.put(member.getUniqueIdentifier().trim(), uid);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (db != null && db.getSystemDescStore() != null) {
            Map<String, String> uidByBigIdKey = new LinkedHashMap<>();
            Map<String, BigId> idByUid = new LinkedHashMap<>();
            try (var sysStream = db.getSystemDescStore().selectEntries(
                    new org.sensorhub.api.datastore.system.SystemFilter.Builder().build())) {
                sysStream.forEach(entry -> {
                    var sys = entry.getValue();
                    if (sys == null || sys.getUniqueIdentifier() == null || sys.getUniqueIdentifier().isBlank()) {
                        return;
                    }
                    String uid = sys.getUniqueIdentifier().trim();
                    BigId sysId = entry.getKey() != null ? entry.getKey().getInternalID() : null;
                    if (sysId != null) {
                        uidByBigIdKey.put(bigIdLookupKey(sysId), uid);
                        idByUid.putIfAbsent(uid, sysId);
                    }
                    BigId parentId = sysId != null ? safeGetParentSystemId(db, sysId) : null;
                    boolean looksLikeLaneSystem = parentId == null
                            && !uid.contains(":sensor:")
                            && !uid.contains(":process:")
                            && (uid.contains(":lane:") || uid.startsWith("urn:oscar:lane:") || uid.contains(":system:lane"));
                    if (looksLikeLaneSystem) {
                        String name = (sys.getName() != null && !sys.getName().isBlank()) ? sys.getName().trim() : uid;
                        names.putIfAbsent(uid, name);
                    }
                });
            } catch (Exception ignored) {
            }

            // Walk parent links so member sensors (e.g. urn:osh:sensor:rapiscan:lane-41) map to parent LaneSystem
            for (Map.Entry<String, BigId> entry : idByUid.entrySet()) {
                String childUid = entry.getKey();
                if (names.containsKey(childUid) || memberToLane.containsKey(childUid)) {
                    continue;
                }
                BigId cur = safeGetParentSystemId(db, entry.getValue());
                int hops = 0;
                while (cur != null && hops++ < 4) {
                    String parentUid = uidByBigIdKey.get(bigIdLookupKey(cur));
                    if (parentUid != null && names.containsKey(parentUid)) {
                        memberToLane.put(childUid, parentUid);
                        break;
                    }
                    cur = safeGetParentSystemId(db, cur);
                }
            }
        }
        return new LaneTopology(names, memberToLane);
    }

    private static BigId safeGetParentSystemId(IObsSystemDatabase db, BigId systemId) {
        if (db == null || db.getSystemDescStore() == null || systemId == null) {
            return null;
        }
        try {
            return db.getSystemDescStore().getParent(systemId);
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveOwningLaneUid(String systemUid, LaneTopology topology) {
        if (systemUid == null || systemUid.isBlank()) {
            return "";
        }
        String trimmed = systemUid.trim();
        if (topology == null) {
            return trimmed;
        }
        Map<String, String> laneNames = topology.laneNamesByUid();
        if (laneNames.containsKey(trimmed)) {
            return trimmed;
        }
        String mappedParent = topology.memberToLaneUid().get(trimmed);
        if (mappedParent != null && !mappedParent.isBlank()) {
            return mappedParent;
        }
        for (String laneUid : laneNames.keySet()) {
            if (trimmed.startsWith(laneUid + ":")) {
                return laneUid;
            }
        }
        // Deterministic trailing lane-slug fallback (e.g. urn:osh:sensor:rapiscan:lane-41 -> urn:osh:system:lane:lane-41)
        String uniqueSlugMatch = null;
        for (String laneUid : laneNames.keySet()) {
            int lastColon = laneUid.lastIndexOf(':');
            if (lastColon < 0 || lastColon + 1 >= laneUid.length()) {
                continue;
            }
            String slug = laneUid.substring(lastColon + 1);
            if (slug.length() < 3) {
                continue;
            }
            if (trimmed.endsWith(":" + slug) || trimmed.contains(":" + slug + ":")) {
                if (uniqueSlugMatch != null && !uniqueSlugMatch.equals(laneUid)) {
                    uniqueSlugMatch = null;
                    break;
                }
                uniqueSlugMatch = laneUid;
            }
        }
        return uniqueSlugMatch != null ? uniqueSlugMatch : trimmed;
    }

    private static LaneStreamMetadata resolveAdHocStreamMetadata(
            IObsSystemDatabase db,
            IdEncoder dsIdEncoder,
            BigId dsId,
            LaneTopology topology
    ) {
        if (db == null || dsId == null || db.getDataStreamStore() == null) {
            return null;
        }
        IDataStreamInfo info = db.getDataStreamStore().get(new DataStreamKey(dsId));
        if (info == null) {
            return null;
        }
        String sysUid = info.getSystemID() != null ? nonNull(info.getSystemID().getUniqueID()) : "";
        String laneUid = resolveOwningLaneUid(sysUid, topology);
        String laneName = topology != null ? topology.laneNamesByUid().getOrDefault(laneUid, laneUid) : laneUid;
        DataComponent struct = info.getRecordStructure();
        return new LaneStreamMetadata(
                laneUid,
                laneName,
                dsId,
                dsIdEncoder != null ? dsIdEncoder.encodeID(dsId) : dsId.toString(),
                struct,
                OccupancyExtended.isExtendedSchema(struct)
        );
    }

    private static Map<Integer, DispositionCodeSpec> buildDispositionSpecs(OSCARServiceModule module) {
        Map<Integer, DispositionCodeSpec> specByCode = new LinkedHashMap<>();
        specByCode.put(0, new DispositionCodeSpec(0, "", "Other"));
        specByCode.put(1, new DispositionCodeSpec(1, "Code 1: Contraband Found", "Real Alarm"));
        specByCode.put(2, new DispositionCodeSpec(2, "Code 2: Other", "Real Alarm"));
        specByCode.put(3, new DispositionCodeSpec(3, "Code 3: Medical Isotope Found", "Innocent Alarm"));
        specByCode.put(4, new DispositionCodeSpec(4, "Code 4: NORM Found", "Innocent Alarm"));
        specByCode.put(5, new DispositionCodeSpec(5, "Code 5: Declared Shipment of Radioactive Material", "Innocent Alarm"));
        specByCode.put(6, new DispositionCodeSpec(6, "Code 6: Physical Inspection Negative", "False Alarm"));
        specByCode.put(7, new DispositionCodeSpec(7, "Code 7: RIID/ASP Indicates Background Only", "False Alarm"));
        specByCode.put(8, new DispositionCodeSpec(8, "Code 8: Other", "False Alarm"));
        specByCode.put(9, new DispositionCodeSpec(9, "Code 9: Authorized Test, Maintenance, or Training Activity", "Test/Maintenance"));
        specByCode.put(10, new DispositionCodeSpec(10, "Code 10: Unauthorized Activity", "Tamper/Fault"));
        specByCode.put(11, new DispositionCodeSpec(11, "Code 11: Other", "Other"));

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
                new DispositionCodeSpec(Adjudication.EXTENDED_OCCUPANCY_GAMMA_CODE, "Extended Occupancy Gamma Alarm", "Automated"));
        specByCode.put(
                Adjudication.EML_NON_THREAT_CODE,
                new DispositionCodeSpec(Adjudication.EML_NON_THREAT_CODE, "EML Non-Threat", "Automated"));
        return specByCode;
    }

    private static boolean matchesAnyVehicleEvidence(
            MatchMode mode,
            String queryNorm,
            String effectiveNorm,
            List<AdjudicationDetailRecord> adjs,
            OcrMatchSummary ocr
    ) {
        if (mode.matches(effectiveNorm, queryNorm)) {
            return true;
        }
        if (ocr != null && mode.matches(ocr.normalizedValue(), queryNorm)) {
            return true;
        }
        if (adjs != null) {
            for (AdjudicationDetailRecord adj : adjs) {
                if (mode.matches(normalizeVehicleId(adj.vehicleId()), queryNorm)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String findLatestAdjudicatedVehicleId(List<AdjudicationDetailRecord> adjs) {
        if (adjs == null || adjs.isEmpty()) {
            return "";
        }
        for (int i = adjs.size() - 1; i >= 0; i--) {
            String raw = adjs.get(i).vehicleId();
            if (!normalizeVehicleId(raw).isEmpty()) {
                return raw.trim();
            }
        }
        return "";
    }

    private static String buildAlarmStatusLabel(
            boolean gammaAlarm,
            boolean neutronAlarm,
            String alarmCategory,
            boolean emlAutoReleased
    ) {
        String cat = alarmCategory == null ? "" : alarmCategory.trim();
        if (gammaAlarm && neutronAlarm) {
            return emlAutoReleased ? "Gamma + Neutron (EML Auto-Release)" : "Gamma + Neutron Alarm";
        }
        if (gammaAlarm) {
            return emlAutoReleased ? "Gamma Alarm (EML Auto-Release)" : "Gamma Alarm";
        }
        if (neutronAlarm) {
            return "Neutron Alarm";
        }
        if (!cat.isEmpty() && !"None".equalsIgnoreCase(cat) && !"Background".equalsIgnoreCase(cat)) {
            return "RS350: " + cat;
        }
        return "Clear / Non-Alarming";
    }

    private static Instant epochSecondsToInstant(double epochSeconds, Instant fallback) {
        if (!Double.isFinite(epochSeconds) || epochSeconds <= 0.0) {
            return fallback;
        }
        long millis = Math.round(epochSeconds * 1000.0);
        return Instant.ofEpochMilli(millis);
    }

    private static IObsSystemDatabase resolveDatabase(OSCARServiceModule module) {
        if (module.getConfiguration() != null
                && module.getConfiguration().databaseID != null
                && !module.getConfiguration().databaseID.isBlank()) {
            try {
                IModule<?> mod = module.getParentHub().getModuleRegistry().getModuleById(module.getConfiguration().databaseID);
                if (mod instanceof IObsSystemDatabase db) {
                    return db;
                }
            } catch (Exception ignored) {
            }
        }
        return module.getParentHub().getDatabaseRegistry().getFederatedDatabase();
    }

    private static String compositePassKey(VehiclePassRecord p) {
        return nonNull(p.nodeId()) + "|" + nonNull(p.laneUid()) + "|" + nonNull(p.occupancyObsId());
    }

    private static String bigIdLookupKey(BigId id) {
        if (id == null) {
            return "";
        }
        byte[] bytes = id.getIdAsBytes();
        return id.getScope() + ":" + (bytes != null ? Base64.getEncoder().encodeToString(bytes) : "");
    }

    private record ParsedEml(
            String operatorNotes,
            String emlDecision,
            String emlSourceType,
            Double normProbPct,
            Double nonEmittingProbPct,
            Double threatProbPct,
            Integer vehicleClass,
            Double vehicleLengthMeters
    ) {}

    private static ParsedEml parseEmlFeedback(String rawFeedback) {
        if (rawFeedback == null || rawFeedback.isBlank()) {
            return new ParsedEml("", "", "", null, null, null, null, null);
        }
        int markerIdx = rawFeedback.indexOf(Adjudication.EML_METADATA_PREFIX);
        if (markerIdx < 0) {
            return new ParsedEml(rawFeedback.trim(), "", "", null, null, null, null, null);
        }
        String notes = rawFeedback.substring(0, markerIdx).trim();
        String jsonPart = rawFeedback.substring(markerIdx + Adjudication.EML_METADATA_PREFIX.length()).trim();
        try {
            JsonElement parsed = JsonParser.parseString(jsonPart);
            if (!parsed.isJsonObject()) {
                return new ParsedEml(notes, "", "", null, null, null, null, null);
            }
            JsonObject obj = parsed.getAsJsonObject();
            String decision = getJsonString(obj, "result");
            if (decision.isEmpty()) {
                decision = getJsonString(obj, "emlDecision");
            }
            String sourceType = getJsonString(obj, "overallSourceType");
            Double pNorm = scalePct(getJsonNullableDouble(obj, "probabilityNORM"));
            Double pNonEmit = scalePct(getJsonNullableDouble(obj, "probabilityNonEmitting"));
            Double pThreat = scalePct(getJsonNullableDouble(obj, "probabilityThreat"));
            Integer vClass = obj.has("vehicleClass") && !obj.get("vehicleClass").isJsonNull()
                    ? obj.get("vehicleClass").getAsInt()
                    : null;
            Double vLen = getJsonNullableDouble(obj, "vehicleLength");
            return new ParsedEml(notes, decision, sourceType, pNorm, pNonEmit, pThreat, vClass, vLen);
        } catch (Exception e) {
            return new ParsedEml(notes, "", "", null, null, null, null, null);
        }
    }

    private static Double scalePct(Double prob) {
        return prob == null ? null : prob * 100.0;
    }

    private static String extractLaneUidSpec(JsonObject obj) {
        for (String key : List.of("laneUID", "laneUIDs", "lanes")) {
            if (!obj.has(key) || obj.get(key).isJsonNull()) {
                continue;
            }
            JsonElement el = obj.get(key);
            if (el.isJsonPrimitive()) {
                return el.getAsString().trim();
            }
            if (el.isJsonArray()) {
                List<String> items = new ArrayList<>();
                for (JsonElement item : el.getAsJsonArray()) {
                    if (item != null && item.isJsonPrimitive() && !item.getAsString().isBlank()) {
                        items.add(item.getAsString().trim());
                    }
                }
                return String.join(",", items);
            }
        }
        return "";
    }

    private static List<String> readJsonStringList(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (JsonElement el : obj.getAsJsonArray(key)) {
            if (el != null && el.isJsonPrimitive() && !el.getAsString().isBlank()) {
                out.add(el.getAsString().trim());
            }
        }
        return List.copyOf(out);
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

    private static Double getJsonNullableDouble(JsonObject obj, String key) {
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

    private static double getJsonDoubleOrZero(JsonObject obj, String key) {
        Double v = getJsonNullableDouble(obj, key);
        return v != null ? v : 0.0;
    }

    private static int getJsonIntOrZero(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return 0;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean getJsonBoolean(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return false;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    private static Map<String, IObsData> batchLoadObservations(
            IObsStore obsStore,
            Map<String, BigId> decodedByEncodedId,
            Set<BigId> preferredDataStreamIds
    ) {
        Map<String, IObsData> orderedResult = new LinkedHashMap<>();
        if (obsStore == null || decodedByEncodedId == null || decodedByEncodedId.isEmpty()) {
            return orderedResult;
        }
        if (decodedByEncodedId.size() == 1) {
            Map.Entry<String, BigId> only = decodedByEncodedId.entrySet().iterator().next();
            BigId id = only.getValue();
            try {
                IObsData obs = obsStore.get(id);
                if (obs == null) {
                    try (var single = obsStore.select(
                            new ObsFilter.Builder().withInternalIDs(Set.of(id)).withLimit(1).build())) {
                        obs = single.findFirst().orElse(null);
                    }
                }
                if (obs != null) {
                    orderedResult.put(only.getKey(), obs);
                }
            } catch (Exception ignored) {
            }
            return orderedResult;
        }

        Map<String, List<String>> encodedKeysByBigIdKey = new LinkedHashMap<>(decodedByEncodedId.size() * 2);
        for (Map.Entry<String, BigId> entry : decodedByEncodedId.entrySet()) {
            encodedKeysByBigIdKey
                    .computeIfAbsent(bigIdLookupKey(entry.getValue()), ignored -> new ArrayList<>(1))
                    .add(entry.getKey());
        }

        Map<String, IObsData> byEncoded = new HashMap<>(decodedByEncodedId.size() * 2);
        final int chunkSize = 512;
        List<BigId> allBigIds = new ArrayList<>(decodedByEncodedId.values());

        if (preferredDataStreamIds != null && !preferredDataStreamIds.isEmpty()) {
            for (int offset = 0; offset < allBigIds.size(); offset += chunkSize) {
                List<BigId> chunk = allBigIds.subList(offset, Math.min(allBigIds.size(), offset + chunkSize));
                collectEntriesIntoMap(
                        obsStore,
                        new ObsFilter.Builder()
                                .withInternalIDs(chunk)
                                .withDataStreams(preferredDataStreamIds)
                                .build(),
                        encodedKeysByBigIdKey,
                        byEncoded
                );
            }
        }

        List<Map.Entry<String, BigId>> unresolved = new ArrayList<>();
        for (Map.Entry<String, BigId> entry : decodedByEncodedId.entrySet()) {
            if (!byEncoded.containsKey(entry.getKey())) {
                unresolved.add(entry);
            }
        }

        if (unresolved.size() > 1) {
            List<BigId> missingIds = new ArrayList<>(unresolved.size());
            for (Map.Entry<String, BigId> entry : unresolved) {
                missingIds.add(entry.getValue());
            }
            for (int offset = 0; offset < missingIds.size(); offset += chunkSize) {
                List<BigId> chunk = missingIds.subList(offset, Math.min(missingIds.size(), offset + chunkSize));
                collectEntriesIntoMap(
                        obsStore,
                        new ObsFilter.Builder().withInternalIDs(chunk).build(),
                        encodedKeysByBigIdKey,
                        byEncoded
                );
            }
        }

        for (Map.Entry<String, BigId> entry : decodedByEncodedId.entrySet()) {
            String encoded = entry.getKey();
            IObsData found = byEncoded.get(encoded);
            if (found == null) {
                try {
                    found = obsStore.get(entry.getValue());
                } catch (Exception ignored) {
                }
            }
            if (found != null) {
                orderedResult.put(encoded, found);
            }
        }
        return orderedResult;
    }

    private static void collectEntriesIntoMap(
            IObsStore obsStore,
            ObsFilter filter,
            Map<String, List<String>> encodedKeysByBigIdKey,
            Map<String, IObsData> target
    ) {
        try (var entries = obsStore.selectEntries(filter)) {
            if (entries == null) {
                return;
            }
            entries.forEach(entry -> {
                if (entry == null || entry.getValue() == null) {
                    return;
                }
                BigId keyId = entry.getKey();
                List<String> matchedKeys = keyId != null ? encodedKeysByBigIdKey.get(bigIdLookupKey(keyId)) : null;
                if (matchedKeys == null && keyId != null) {
                    matchedKeys = encodedKeysByBigIdKey.get("0:" + keyId.getIdAsLong());
                }
                if (matchedKeys != null) {
                    for (String enc : matchedKeys) {
                        target.putIfAbsent(enc, entry.getValue());
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }

    private static String defaultIfBlank(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }
}
