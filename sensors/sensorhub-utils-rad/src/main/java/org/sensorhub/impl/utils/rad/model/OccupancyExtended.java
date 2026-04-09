/*******************************************************************************
 * Extension of {@link Occupancy} that carries an additional raw alarm-category
 * string reported by the RS350 driver. Lives in sensorhub-utils-rad (rather than
 * the rs350-occupancy processing module) so that downstream consumers in
 * sensorhub-utils-rad and sensorhub-system-lane (WebIdAnalyzer, WebIdHelper,
 * AdjudicationControl) can read/write extended-schema occupancy datablocks
 * without taking a dependency on the RS350 processing module.
 ******************************************************************************/
package org.sensorhub.impl.utils.rad.model;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Text;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.output.OccupancyOutput;
import org.vast.data.DataArrayImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Occupancy variant that carries the raw alarm category string reported by the
 * driver (e.g. "Gamma", "Neutron", "Gamma-Neutron", "Alpha", "Isotope", or any
 * vendor-specific value). The base {@link OccupancyOutput} record schema is
 * extended with one trailing text field {@code alarmCategoryCode}; see
 * {@link #createExtendedRecordStructure()}.
 *
 * <p>The class also exposes <strong>schema-aware</strong> static helpers
 * ({@link #readOccupancy(DataComponent, DataBlock)} and
 * {@link #writeOccupancy(DataComponent, Occupancy)}) so that callers which
 * receive a generic {@link Occupancy} datablock can dispatch to the correct
 * (base vs. extended) serialization based on the datastream record description,
 * without having to know which RPM produced the observation.
 */
public class OccupancyExtended extends Occupancy {

    public static final String ALARM_CATEGORY_FIELD_NAME = "alarmCategoryCode";

    protected String alarmCategory = "";

    public String getAlarmCategory() {
        return alarmCategory;
    }

    public static class Builder extends Occupancy.Builder {

        public Builder() {
            instance = new OccupancyExtended();
        }

        @Override
        public OccupancyExtended build() {
            return (OccupancyExtended) instance;
        }

        /**
         * NOTE: returns the parent {@link Occupancy.Builder} so the rest of the
         * fluent chain can continue calling base setters. The underlying
         * {@code instance} is an {@link OccupancyExtended}, so chaining back to
         * {@code build()} still returns the extended instance. Call
         * {@code alarmCategory(...)} first in the chain, or cast to reuse it.
         */
        public Occupancy.Builder alarmCategory(String alarmCategory) {
            ((OccupancyExtended) instance).alarmCategory = alarmCategory == null ? "" : alarmCategory;
            return this;
        }
    }

    /**
     * Build a DataRecord that matches the base {@link OccupancyOutput} record
     * structure plus a trailing {@code alarmCategoryCode} text field. Used as
     * the output record schema for the RS350 occupancy process so that
     * observations persisted to the obs store carry the extra field.
     */
    public static DataRecord createExtendedRecordStructure() {
        // Reuse base OccupancyOutput to build the common fields exactly once so
        // they stay in sync with Rapiscan/Aspect schemas.
        @SuppressWarnings({"rawtypes", "unchecked"})
        OccupancyOutput base = new OccupancyOutput(new SensorSystem());
        DataRecord baseRecord = (DataRecord) base.getRecordDescription();

        RADHelper radHelper = new RADHelper();
        DataRecord extended = radHelper.createRecord()
                .name(baseRecord.getName())
                .label(baseRecord.getLabel())
                .updatable(true)
                .definition(baseRecord.getDefinition())
                .description(baseRecord.getDescription())
                .build();

        // Copy all existing fields from the base record (cloned so they can
        // be reparented safely).
        for (int i = 0; i < baseRecord.getComponentCount(); i++) {
            DataComponent child = baseRecord.getComponent(i).copy();
            extended.addComponent(child.getName(), child);
        }

        // Append the raw alarm category code as a plain text field. A Text
        // field (not a constrained Category) is intentional: the RS350 can
        // report arbitrary / combined categories (e.g. "Gamma-Neutron") that
        // don't fit RadAlarmCategoryCodeSimpleType's enum.
        Text alarmCategoryCode = radHelper.createText()
                .name(ALARM_CATEGORY_FIELD_NAME)
                .label("Alarm Category Code")
                .definition(RADHelper.getRadUri("AlarmCategoryCode"))
                .description("Raw alarm category string reported by the RPM driver")
                .build();
        extended.addComponent(ALARM_CATEGORY_FIELD_NAME, alarmCategoryCode);

        return extended;
    }

    /**
     * Returns true if the supplied datastream record description carries the
     * extended {@code alarmCategoryCode} field — i.e. the datablock matching
     * this record should be serialized via {@link #fromOccupancy(OccupancyExtended)}
     * rather than {@link Occupancy#fromOccupancy(Occupancy)}.
     */
    public static boolean isExtendedSchema(DataComponent recordStructure) {
        if (recordStructure == null)
            return false;
        try {
            return recordStructure.getComponent(ALARM_CATEGORY_FIELD_NAME) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Schema-aware reader: returns an {@link OccupancyExtended} when the record
     * has the extended {@code alarmCategoryCode} field, otherwise a plain
     * {@link Occupancy}. Callers that mutate and write back should pair this
     * with {@link #writeOccupancy(DataComponent, Occupancy)}.
     */
    public static Occupancy readOccupancy(DataComponent recordStructure, DataBlock dataBlock) {
        if (isExtendedSchema(recordStructure)) {
            return toOccupancy(dataBlock);
        }
        return Occupancy.toOccupancy(dataBlock);
    }

    /**
     * Schema-aware writer: serializes to a datablock matching the supplied
     * record structure. Used by callers that mutate occupancy data and need to
     * write it back to the obs store without having to know whether the
     * underlying datastream is base or extended.
     *
     * <p>If the record carries the extended field but the supplied
     * {@link Occupancy} is not an {@link OccupancyExtended} (e.g. it was
     * deserialized as a base occupancy), the alarm category is preserved as
     * an empty string so the produced datablock still matches the schema.
     */
    public static DataBlock writeOccupancy(DataComponent recordStructure, Occupancy occupancy) {
        if (isExtendedSchema(recordStructure)) {
            OccupancyExtended extended;
            if (occupancy instanceof OccupancyExtended) {
                extended = (OccupancyExtended) occupancy;
            } else {
                extended = wrapAsExtended(occupancy, "");
            }
            return fromOccupancy(extended);
        }
        return Occupancy.fromOccupancy(occupancy);
    }

    /**
     * Wrap a plain {@link Occupancy} as an {@link OccupancyExtended} with the
     * given alarm category, copying all base fields. Useful when a caller
     * already has a base Occupancy in hand but needs to push it through the
     * extended writer (e.g. AdjudicationControl mutating a base-read instance).
     */
    public static OccupancyExtended wrapAsExtended(Occupancy occupancy, String alarmCategory) {
        return (OccupancyExtended) new OccupancyExtended.Builder()
                .alarmCategory(alarmCategory)
                .samplingTime(occupancy.getSamplingTime())
                .occupancyCount(occupancy.getOccupancyCount())
                .startTime(occupancy.getStartTime())
                .endTime(occupancy.getEndTime())
                .neutronBackground(occupancy.getNeutronBackground())
                .gammaAlarm(occupancy.hasGammaAlarm())
                .neutronAlarm(occupancy.hasNeutronAlarm())
                .maxGammaCount(occupancy.getMaxGammaCount())
                .maxNeutronCount(occupancy.getMaxNeutronCount())
                .adjudicatedIds(occupancy.getAdjudicatedIds())
                .videoPaths(occupancy.getVideoPaths())
                .webIdObsIds(occupancy.getWebIdObsIds())
                .build();
    }

    /**
     * Serialize an {@link OccupancyExtended} to a DataBlock matching the
     * schema returned by {@link #createExtendedRecordStructure()}. Mirrors
     * {@link Occupancy#fromOccupancy(Occupancy)} field-for-field then appends
     * the trailing alarm category code atom.
     */
    public static DataBlock fromOccupancy(OccupancyExtended occupancy) {
        DataComponent resultStructure = createExtendedRecordStructure();
        DataBlock dataBlock = resultStructure.createDataBlock();
        dataBlock.updateAtomCount();
        resultStructure.setData(dataBlock);

        int index = 0;

        dataBlock.setDoubleValue(index++, occupancy.getSamplingTime());
        dataBlock.setIntValue(index++, occupancy.getOccupancyCount());
        dataBlock.setDoubleValue(index++, occupancy.getStartTime());
        dataBlock.setDoubleValue(index++, occupancy.getEndTime());
        dataBlock.setDoubleValue(index++, occupancy.getNeutronBackground());
        dataBlock.setBooleanValue(index++, occupancy.hasGammaAlarm());
        dataBlock.setBooleanValue(index++, occupancy.hasNeutronAlarm());
        dataBlock.setIntValue(index++, occupancy.getMaxGammaCount());
        dataBlock.setIntValue(index++, occupancy.getMaxNeutronCount());

        int adjCount = occupancy.getAdjudicatedIds().size();
        dataBlock.setDoubleValue(index++, adjCount);
        DataArrayImpl adjArr = (DataArrayImpl) resultStructure.getComponent("adjudicatedIds");
        if (adjCount > 0) {
            adjArr.updateSize();
            dataBlock.updateAtomCount();
            for (int i = 0; i < adjCount; i++) {
                dataBlock.setStringValue(index++, occupancy.getAdjudicatedIds().get(i));
            }
        }

        int videoCount = occupancy.getVideoPaths().size();
        dataBlock.setDoubleValue(index++, videoCount);
        DataArrayImpl videoArr = (DataArrayImpl) resultStructure.getComponent("videoPaths");
        if (videoCount > 0) {
            videoArr.updateSize();
            dataBlock.updateAtomCount();
            for (int i = 0; i < videoCount; i++) {
                dataBlock.setStringValue(index++, occupancy.getVideoPaths().get(i));
            }
        }

        int webIdObsIdsCount = occupancy.getWebIdObsIds().size();
        dataBlock.setDoubleValue(index++, webIdObsIdsCount);
        DataArrayImpl webIdObsIdsArray = (DataArrayImpl) resultStructure.getComponent("webIdObsIds");
        if (webIdObsIdsCount > 0) {
            webIdObsIdsArray.updateSize();
            dataBlock.updateAtomCount();
            for (int i = 0; i < webIdObsIdsCount; i++) {
                dataBlock.setStringValue(index++, occupancy.getWebIdObsIds().get(i));
            }
        }

        // Trailing alarm category code
        String alarmCat = occupancy.getAlarmCategory();
        dataBlock.setStringValue(index, alarmCat == null ? "" : alarmCat);

        return dataBlock;
    }

    /**
     * Deserialize an {@link OccupancyExtended} from a DataBlock produced by
     * {@link #fromOccupancy(OccupancyExtended)} or an equivalent extended
     * occupancy record. Missing trailing fields (older records that don't
     * carry the alarm category) are tolerated.
     */
    public static OccupancyExtended toOccupancy(DataBlock dataBlock) {
        int index = 0;

        var samplingTime = dataBlock.getDoubleValue(index++);
        var occupancyCount = dataBlock.getIntValue(index++);
        var startTime = dataBlock.getDoubleValue(index++);
        var endTime = dataBlock.getDoubleValue(index++);
        var neutronBackground = dataBlock.getDoubleValue(index++);
        var gammaAlarm = dataBlock.getBooleanValue(index++);
        var neutronAlarm = dataBlock.getBooleanValue(index++);
        var maxGammaCount = dataBlock.getIntValue(index++);
        var maxNeutronCount = dataBlock.getIntValue(index++);

        int adjCount = dataBlock.getIntValue(index++);
        List<String> adjIds = new ArrayList<>();
        for (int i = 0; i < adjCount; i++)
            adjIds.add(dataBlock.getStringValue(index++));

        int videoCount = dataBlock.getIntValue(index++);
        List<String> videoPaths = new ArrayList<>();
        for (int i = 0; i < videoCount; i++)
            videoPaths.add(dataBlock.getStringValue(index++));

        List<String> webIdObsIds = new ArrayList<>();
        String alarmCat = "";
        try {
            int webIdObsIdsCount = dataBlock.getIntValue(index++);
            for (int i = 0; i < webIdObsIdsCount; i++)
                webIdObsIds.add(dataBlock.getStringValue(index++));
            alarmCat = dataBlock.getStringValue(index);
        } catch (Exception ignored) {
            // backwards compatibility: older records may lack webIdObsIds
            // and/or the alarm category code trailing field
        }

        return (OccupancyExtended) new OccupancyExtended.Builder()
                .alarmCategory(alarmCat)
                .samplingTime(samplingTime)
                .occupancyCount(occupancyCount)
                .startTime(startTime)
                .endTime(endTime)
                .neutronBackground(neutronBackground)
                .maxGammaCount(maxGammaCount)
                .maxNeutronCount(maxNeutronCount)
                .gammaAlarm(gammaAlarm)
                .neutronAlarm(neutronAlarm)
                .adjudicatedIds(adjIds)
                .videoPaths(videoPaths)
                .webIdObsIds(webIdObsIds)
                .build();
    }
}
