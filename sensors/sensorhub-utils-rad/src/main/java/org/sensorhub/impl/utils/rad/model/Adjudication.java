package org.sensorhub.impl.utils.rad.model;

import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.DataArrayImpl;
import org.vast.swe.SWEHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntUnaryOperator;

public class Adjudication {

    private String feedback;
    private AdjudicationCode adjudicationCode;
    private List<String> isotopes;
    private SecondaryInspectionStatus secondaryInspectionStatus;
    private List<String> filePaths;
    private String occupancyId;
    private String vehicleId;


    public enum SecondaryInspectionStatus {
        NONE, REQUESTED, COMPLETED
    }

    public static String REAL_ALARM = "Real Alarm";
    public static String INNOCENT_ALARM = "Innocent Alarm";
    public static String FALSE_ALARM = "False Alarm";
    public static String OTHER_ALARM = "Other";
    public static String TAMPER_FAULT_ALARM = "Tamper/Fault";
    public static String TEST_MAINTENANCE_ALARM = "Test/Maintenance";

    public enum AdjudicationCode {
        UNKNOWN("", ""),
        CONTRABAND_FOUND("Code 1: Contraband Found", REAL_ALARM),
        REAL_ALARM_OTHER("Code 2: Other", REAL_ALARM),
        MEDICAL_ISOTOPE_FOUND( "Code 3: Medical Isotope Found", INNOCENT_ALARM),
        NORM_FOUND("Code 4: NORM Found", INNOCENT_ALARM),
        DECLARED_SHIPMENT_RADIOACTIVE_MATERIAL("Code 5: Declared Shipment of Radioactive Material", INNOCENT_ALARM),
        INSPECTION_NEGATIVE("Code 6: Physical Inspection Negative", FALSE_ALARM),
        RIID_ASP_BACKGROUND_ONLY("Code 7: RIID/ASP Indicates Background Only", FALSE_ALARM),
        FALSE_ALARM_OTHER("Code 8: Other", FALSE_ALARM),
        AUTHORIZED_TEST("Code 9: Authorized Test, Maintenance, or Training Activity", TEST_MAINTENANCE_ALARM),
        UNAUTHORIZED_ACTIVITY("Code 10: Unauthorized Activity", TAMPER_FAULT_ALARM),
        OTHER("Code 11: Other", OTHER_ALARM);

        private final String label;
        private final String group;

        AdjudicationCode(String label, String group) {
            this.label = label;
            this.group = group;
        }

        public String getLabel() {
            return label;
        }
        public String getGroup() {
            return group;
        }
    }

    public static class Builder {

        Adjudication instance;

        public Builder() {
            this.instance = new Adjudication();
        }

        public Builder feedback(String feedback) {
            instance.feedback = feedback;
            return this;
        }

        public Builder adjudicationCode(AdjudicationCode code) {
            instance.adjudicationCode = code;
            return this;
        }

        public Builder isotopes(List<String> isotopes) {
            instance.isotopes = isotopes;
            return this;
        }

        public Builder filePaths(List<String> filePaths) {
            instance.filePaths = filePaths;
            return this;
        }

        public Builder occupancyId(String occupancyId) {
            instance.occupancyId = occupancyId;
            return this;
        }

        public Builder vehicleId(String vehicleId) {
            instance.vehicleId = vehicleId;
            return this;
        }

        public Builder secondaryInspectionStatus(SecondaryInspectionStatus secondaryInspectionStatus) {
            instance.secondaryInspectionStatus = secondaryInspectionStatus;
            return this;
        }

        public Adjudication build() {
            return instance;
        }
    }

    public String getFeedback() {
        return feedback;
    }

    public AdjudicationCode getAdjudicationCode() {
        return adjudicationCode;
    }

    public List<String> getIsotopes() {
        return isotopes;
    }

    public SecondaryInspectionStatus getSecondaryInspectionStatus() {
        return secondaryInspectionStatus;
    }

    public List<String> getFilePaths() {
        return filePaths;
    }

    public String getOccupancyId() {
        return occupancyId;
    }

    public String getVehicleId() {
        return vehicleId;
    }


    public static DataBlock fromAdjudication(Adjudication adjudication) {
        RADHelper radHelper = new RADHelper();
        DataComponent resultStructure = radHelper.createAdjudicationRecord();
        DataBlock dataBlock = resultStructure.createDataBlock();
        resultStructure.setData(dataBlock);

        int index = 0;

        dataBlock.setStringValue(index++, adjudication.getFeedback());
        dataBlock.setStringValue(index++, adjudication.getAdjudicationCode().toString());

        var isotopeCount = adjudication.getIsotopes().size();
        dataBlock.setIntValue(index++, isotopeCount);

        var isotopesArray = ((DataArrayImpl) resultStructure.getComponent("isotopes"));
        isotopesArray.updateSize();
        dataBlock.updateAtomCount();

        for (int i = 0; i < isotopeCount; i++ )
            dataBlock.setStringValue(index++, adjudication.getIsotopes().get(i));
        
        dataBlock.setStringValue(index++, adjudication.getSecondaryInspectionStatus().toString());

        var filesCount = adjudication.getFilePaths().size();
        dataBlock.setIntValue(index++, filesCount);

        var filesArray = ((DataArrayImpl) resultStructure.getComponent("filePaths"));
        filesArray.updateSize();
        dataBlock.updateAtomCount();

        for (int i = 0; i < filesCount; i++ )
            dataBlock.setStringValue(index++, adjudication.getFilePaths().get(i));
        
        dataBlock.setStringValue(index++, adjudication.getOccupancyId());
        dataBlock.setStringValue(index, adjudication.getVehicleId());

        return dataBlock;
    }


    public static Adjudication toAdjudication(DataBlock dataBlock) {
        int index = 0;

        var feedback = dataBlock.getStringValue(index++);
        var adjudicationCode = AdjudicationCode.valueOf(dataBlock.getStringValue(index++));
        var isotopeCount = dataBlock.getIntValue(index++);

        List<String> isotopes = new ArrayList<>();
        for (int i = 0; i < isotopeCount; i++)
            isotopes.add(dataBlock.getStringValue(index++));

        var secondaryInspectionStatus = SecondaryInspectionStatus.valueOf(dataBlock.getStringValue(index++));
        var filePathCount = dataBlock.getIntValue(index++);

        List<String> filePaths = new ArrayList<>();
        for (int i = 0; i < filePathCount; i++)
            filePaths.add(dataBlock.getStringValue(index++));

        var occupancyId = dataBlock.getStringValue(index++);
        var vehicleId = dataBlock.getStringValue(index);

        return new Builder()
                .feedback(feedback)
                .adjudicationCode(adjudicationCode)
                .isotopes(isotopes)
                .secondaryInspectionStatus(secondaryInspectionStatus)
                .filePaths(filePaths)
                .occupancyId(occupancyId)
                .vehicleId(vehicleId)
                .build();
    }
}
