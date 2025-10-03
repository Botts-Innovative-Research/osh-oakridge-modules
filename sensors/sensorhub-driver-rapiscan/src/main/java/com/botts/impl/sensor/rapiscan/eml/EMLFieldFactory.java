package com.botts.impl.sensor.rapiscan.eml;

import gov.llnl.ernie.Analysis;
import gov.llnl.ernie.api.Results;
import net.opengis.swe.v20.Boolean;
import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.swe.SWEHelper;

public class EMLFieldFactory extends RADHelper {

    public static final String VERSION_ID_FIELD_NAME = "versionId";
    public static final String VERSION_ID_FIELD_LABEL = "Version ID";
    public static final String VERSION_ID_FIELD_DESCRIPTION = "Provides the version ID for the EML VM250 RPM";
    public static final String VERSION_ID_FIELD_DEFINITION = SWEHelper.getPropertyUri("EmlVersionID");

    public static final String MODEL_ID_FIELD_NAME = "modelId";
    public static final String MODEL_ID_FIELD_LABEL = "Model ID";
    public static final String MODEL_ID_FIELD_DESCRIPTION = "Provides the model ID for the EML VM250 RPM";
    public static final String MODEL_ID_FIELD_DEFINITION = SWEHelper.getPropertyUri("EmlModelID");

    public static final String THRESHOLDS_FIELD_NAME = "thresholds";
    public static final String THRESHOLDS_FIELD_LABEL = "Thresholds";
    public static final String THRESHOLDS_FIELD_DESCRIPTION = "Provides the thresholds for the EML VM250 RPM";
    public static final String THRESHOLDS_FIELD_DEFINITION = SWEHelper.getPropertyUri("EmlThresholds");


    public static final String LANE_ID_FIELD_NAME = "laneId";
    public static final String LANE_ID_FIELD_LABEL = "Lane ID";
    public static final String LANE_ID_FIELD_DESCRIPTION = "Provides the lane ID for the EML VM250 RPM";
    public static final String LANE_ID_FIELD_DEFINITION = SWEHelper.getPropertyUri("EmlLaneID");

    public static final String PORT_ID_FIELD_NAME = "portID";
    public static final String PORT_ID_FIELD_LABEL = "Port ID";
    public static final String PORT_ID_FIELD_DESCRIPTION = "Provides the port ID for the EML VM250 RPM";
    public static final String PORT_ID_FIELD_DEFINITION = SWEHelper.getPropertyUri("EmlPortID");

    public static final String TIME_DATE_FIELD_NAME = "dateTime";
    public static final String TIME_DATE_FIELD_LABEL = "Date Time";
    public static final String TIME_DATE_FIELD_DESCRIPTION = "Provides the date- time for the EML VM250 RPM";
    public static final String TIME_DATE_FIELD_DEFINITION = SWEHelper.getPropertyUri("EmlDateTime");


    public static final String SEGMENT_ID_FIELD_NAME = "segmentId";
    public static final String SEGMENT_ID_FIELD_LABEL = "Segment ID";
    public static final String SEGMENT_ID_FIELD_DESCRIPTION = "Provides the segment ID for the EML VM250 RPM";
    public static final String SEGMENT_ID_FIELD_DEFINITION = SWEHelper.getPropertyUri("EmlSegmentID");


    public static final String RPM_RESULT_FIELD_NAME = "rpmResult";
    public static final String RPM_RESULT_FIELD_LABEL = "RPM Result";
    public static final String RPM_RESULT_FIELD_DESCRIPTION = "Provides the rpm result for the EML VM250 RPM";
    public static final String RPM_RESULT_FIELD_DEFINITION = SWEHelper.getPropertyUri("EmlRpmResult");


    public static final String RPM_GAMMA_ALERT_FIELD_NAME = "rpmGamma";
    public static final String RPM_GAMMA_ALERT_FIELD_LABEL = "RPM Gamma Alert";
    public static final String RPM_GAMMA_ALERT_FIELD_DESCRIPTION = "Provides the rpm gamma alert for the EML VM250 RPM";
    public static final String RPM_GAMMA_ALERT_FIELD_DEFINITION = SWEHelper.getPropertyUri("EmlRpmGammaAlert");

    public static final String RPM_NEUTRON_ALERT_FIELD_NAME = "rpmNeutronAlert";
    public static final String RPM_NEUTRON_ALERT_FIELD_LABEL = "RPM Neutron Alert";
    public static final String RPM_NEUTRON_ALERT_FIELD_DESCRIPTION = "Provides the rpm neutron alert for the EML VM250 RPM";
    public static final String RPM_NEUTRON_ALERT_FIELD_DEFINITION = SWEHelper.getPropertyUri("EmlRpmNeutronAlert");

    public static final String RPM_SCAN_ERROR_FIELD_NAME = "rpmScanError";
    public static final String RPM_SCAN_ERROR_FIELD_LABEL = "RPM Scan Error";
    public static final String RPM_SCAN_ERROR_FIELD_DESCRIPTION = "Provides the rpm scan error for the EML VM250 RPM";
    public static final String RPM_SCAN_ERROR_FIELD_DEFINITION = SWEHelper.getPropertyUri("EmlRpmScanError");


    public static final String SOURCE_TYPE_FIELD_NAME = "sourceType";
    public static final String SOURCE_TYPE_FIELD_LABEL = "Source Type";
    public static final String SOURCE_TYPE_FIELD_DESCRIPTION = "Provides the source type for the EML VM250 RPM";
    public static final String SOURCE_FIELD_DEFINITION = SWEHelper.getPropertyUri("EmlSourceType");

    public static final String CLASSIFIER_FIELD_NAME = "classifierUsed";
    public static final String CLASSIFIER_FIELD_LABEL = "Classifier Used";
    public static final String CLASSIFIER_FIELD_DESCRIPTION = "Describes the classifier used for EML service.";
    public static final String CLASSIFIER_FIELD_DEFINITION = SWEHelper.getPropertyUri("ClassifierUsed");

    public static final String X_LOCATION_1_FIELD_NAME = "xLocation1";
    public static final String X_LOCATION_1_FIELD_LABEL = "X Location 1";
    public static final String X_LOCATION_1_FIELD_DESCRIPTION = "Describes the xLocation1";
    public static final String X_LOCATION_1_FIELD_DEFINITION = SWEHelper.getPropertyUri("XLocation1");

    public static final String X_LOCATION_2_FIELD_NAME = "xLocation2";
    public static final String X_LOCATION_2_FIELD_LABEL = "X Location 2";
    public static final String X_LOCATION_2_FIELD_DESCRIPTION = "Describes the xLocation2.";
    public static final String X_LOCATION_2_FIELD_DEFINITION = SWEHelper.getPropertyUri("XLocation2");

    public static final String Y_LOCATION_FIELD_NAME = "yLocation";
    public static final String Y_LOCATION_FIELD_LABEL = "Y Location";
    public static final String Y_LOCATION_FIELD_DESCRIPTION = "Describes the yLocation";
    public static final String Y_LOCATION_FIELD_DEFINITION = SWEHelper.getPropertyUri("YLocation");

    public static final String Z_LOCATION_FIELD_NAME = "zLocation";
    public static final String Z_LOCATION_FIELD_LABEL = "Z Location";
    public static final String Z_LOCATION_FIELD_DESCRIPTION = "Describes the zLocation.";
    public static final String Z_LOCATION_FIELD_DEFINITION = SWEHelper.getPropertyUri("ZLocation");

    public static final String PROBABILITY_THREAT_FIELD_NAME = "probabilityThreat";
    public static final String PROBABILITY_THREAT_FIELD_LABEL = "Probability Threat";
    public static final String PROBABILITY_THREAT_FIELD_DESCRIPTION = "Describes the probabilityThreat.";
    public static final String PROBABILITY_THREAT_FIELD_DEFINITION = SWEHelper.getPropertyUri("ProbabilityThreat");


    public static final String PROBABILITY_NON_EMITTING_FIELD_NAME = "probabilityNonEmitting";
    public static final String PROBABILITY_NON_EMITTING_FIELD_LABEL = "Probability Non-Emitting";
    public static final String PROBABILITY_NON_EMITTING_FIELD_DESCRIPTION = "Describes the probabilityNonEmitting.";
    public static final String PROBABILITY_NON_EMITTING_FIELD_DEFINITION = SWEHelper.getPropertyUri("ProbabilityNonEmitting");


    public static final String PROBABILITY_NORM_FIELD_NAME = "probabilityNORM";
    public static final String PROBABILITY_NORM_FIELD_LABEL = "Probability NORM";
    public static final String PROBABILITY_NORM_FIELD_DESCRIPTION = "Describes the probabilityNORM.";
    public static final String PROBABILITY_NORM_FIELD_DEFINITION = SWEHelper.getPropertyUri("ProbabilityNORM");


    public static final String OVERALL_SOURCE_TYPE_FIELD_NAME = "overallSourceType";
    public static final String OVERALL_SOURCE_TYPE_FIELD_LABEL = "Overall Source Type";
    public static final String OVERALL_SOURCE_TYPE_FIELD_DESCRIPTION = "Provides the source type for the EML VM250 RPM";
    public static final String OVERALL_SOURCE_FIELD_DEFINITION = SWEHelper.getPropertyUri("EMLOverallSourceType");

    public static final String OVERALL_CLASSIFIER_FIELD_NAME = "overallClassifierUsed";
    public static final String OVERALL_CLASSIFIER_FIELD_LABEL = "Overall Classifier Used";
    public static final String OVERALL_CLASSIFIER_FIELD_DESCRIPTION = "Describes the classifier used for EML service.";
    public static final String OVERALL_CLASSIFIER_FIELD_DEFINITION = SWEHelper.getPropertyUri("OverallClassifierUsed");

    public static final String OVERALL_X_LOCATION_1_FIELD_NAME = "overallXLocation1";
    public static final String OVERALL_X_LOCATION_1_FIELD_LABEL = "Overall X Location 1";
    public static final String OVERALL_X_LOCATION_1_FIELD_DESCRIPTION = "Describes the xLocation1";
    public static final String OVERALL_X_LOCATION_1_FIELD_DEFINITION = SWEHelper.getPropertyUri("OverallCLocation1");

    public static final String OVERALL_X_LOCATION_2_FIELD_NAME = "overallXLocation2";
    public static final String OVERALL_X_LOCATION_2_FIELD_LABEL = "Overall X Location 2";
    public static final String OVERALL_X_LOCATION_2_FIELD_DESCRIPTION = "Describes the xLocation2.";
    public static final String OVERALL_X_LOCATION_2_FIELD_DEFINITION = SWEHelper.getPropertyUri("OverallXLocation2");

    public static final String OVERALL_Y_LOCATION_FIELD_NAME = "overallYLocation";
    public static final String OVERALL_Y_LOCATION_FIELD_LABEL = "Overall Y Location";
    public static final String OVERALL_Y_LOCATION_FIELD_DESCRIPTION = "Describes the yLocation";
    public static final String OVERALL_Y_LOCATION_FIELD_DEFINITION = SWEHelper.getPropertyUri("OverallYLocation");

    public static final String OVERALL_Z_LOCATION_FIELD_NAME = "overallZLocation";
    public static final String OVERALL_Z_LOCATION_FIELD_LABEL = "Overall Z Location";
    public static final String OVERALL_Z_LOCATION_FIELD_DESCRIPTION = "Describes the zLocation.";
    public static final String OVERALL_Z_LOCATION_FIELD_DEFINITION = SWEHelper.getPropertyUri("OverallZLocation");

    public static final String OVERALL_PROBABILITY_THREAT_FIELD_NAME = "overallProbabilityThreat";
    public static final String OVERALL_PROBABILITY_THREAT_FIELD_LABEL = "Overall Probability Threat";
    public static final String OVERALL_PROBABILITY_THREAT_FIELD_DESCRIPTION = "Describes the probabilityThreat.";
    public static final String OVERALL_PROBABILITY_THREAT_FIELD_DEFINITION = SWEHelper.getPropertyUri("OverallProbabilityThreat");

    public static final String OVERALL_PROBABILITY_NON_EMITTING_FIELD_NAME = "overallProbabilityNonEmitting";
    public static final String OVERALL_PROBABILITY_NON_EMITTING_FIELD_LABEL = "Overall Probability NonEmitting";
    public static final String OVERALL_PROBABILITY_NON_EMITTING_FIELD_DESCRIPTION = "Describes the probabilityNonEmitting.";
    public static final String OVERALL_PROBABILITY_NON_EMITTING_FIELD_DEFINITION = SWEHelper.getPropertyUri("OverallProbabilityNonEmitting");

    public static final String OVERALL_PROBABILITY_NORM_FIELD_NAME = "overallProbabilityNORM";
    public static final String OVERALL_PROBABILITY_NORM_FIELD_LABEL = "Overall Probability NORM";
    public static final String OVERALL_PROBABILITY_NORM_FIELD_DESCRIPTION = "Describes the probabilityNORM.";
    public static final String OVERALL_PROBABILITY_NORM_FIELD_DEFINITION = SWEHelper.getPropertyUri("OverallProbabilityNORM");

    public static final String VEHICLE_CLASS_FIELD_NAME = "vehicleClass";
    public static final String VEHICLE_CLASS_FIELD_LABEL = "Vehicle Class";
    public static final String VEHICLE_CLASS_FIELD_DESCRIPTION = "Provides the vehicle class for the EML VM250 RPM";
    public static final String VEHICLE_CLASS_FIELD_DEFINITION = SWEHelper.getPropertyUri("EMLVehicleClass");

    public static final String VEHICLE_LENGTH_FIELD_NAME = "vehicleLength";
    public static final String VEHICLE_LENGTH_FIELD_LABEL = "Vehicle Length";
    public static final String VEHICLE_LENGTH_FIELD_DESCRIPTION = "Provides the vehicleLength for the EML VM250 RPM";
    public static final String VEHICLE_LENGTH_FIELD_DEFINITION = SWEHelper.getPropertyUri("EMLVehicleLength");

    public static final String MESSAGE_FIELD_NAME = "message";
    public static final String MESSAGE_FIELD_LABEL = "Message";
    public static final String MESSAGE_FIELD_DESCRIPTION = "Provides the message for the EML VM250 RPM";
    public static final String MESSAGE_FIELD_DEFINITION = SWEHelper.getPropertyUri("EMLMessage");

    public static final String YELLOW_LIGHT_MESSAGE_FIELD_NAME = "yellowLightMessage";
    public static final String YELLOW_LIGHT_MESSAGE_FIELD_LABEL = "Yellow Light Message";
    public static final String YELLOW_LIGHT_MESSAGE_FIELD_DESCRIPTION = "Provides the yellow Light Message for the EML VM250 RPM";
    public static final String YELLOW_LIGHT_MESSAGE_FIELD_DEFINITION = SWEHelper.getPropertyUri("EML-yellow-light-message");

    public static final String GAMMA_ALERT_FIELD_NAME = "gammaAlert";
    public static final String GAMMA_ALERT_FIELD_LABEL = "Gamma Alert";
    public static final String GAMMA_ALERT_FIELD_DESCRIPTION = "Provides the gamma alert for the EML VM250 RPM";
    public static final String GAMMA_ALERT_FIELD_DEFINITION = SWEHelper.getPropertyUri("EMLGammaAlert");

    public static final String NEUTRON_ALERT_FIELD_NAME = "neutronAlert";
    public static final String NEUTRON_ALERT_FIELD_LABEL = "Neutron Alert";
    public static final String NEUTRON_ALERT_FIELD_DESCRIPTION = "Provides the neutron alert for the EML VM250 RPM";
    public static final String NEUTRON_ALERT_FIELD_DEFINITION = SWEHelper.getPropertyUri("EMLNeutronAlert");

    public static final String RESULT_FIELD_NAME = "result";
    public static final String RESULT_FIELD_LABEL = "Result";
    public static final String RESULT_FIELD_DESCRIPTION = "Provides the result for the EML VM250 RPM";
    public static final String RESULT_FIELD_DEFINITION = SWEHelper.getPropertyUri("EMLResult");

    public static final String INVESTIGATIVE_PROBABILITY_FIELD_NAME = "investigativeProbability";
    public static final String INVESTIGATIVE_PROBABILITY_FIELD_LABEL = "Investigative Probability";
    public static final String INVESTIGATIVE_PROBABILITY_FIELD_DESCRIPTION = "Provides the investigative probability for the EML VM250 RPM";
    public static final String INVESTIGATIVE_PROBABILITY_FIELD_DEFINITION = SWEHelper.getPropertyUri("EMLInvestigativeProbability");

    public static final String RELEASE_PROBABILITY_FIELD_NAME = "releaseProbability";
    public static final String RELEASE_PROBABILITY_FIELD_LABEL = "Release Probability";
    public static final String RELEASE_PROBABILITY_FIELD_DESCRIPTION = "Provides the release probability for the EML VM250 RPM";
    public static final String RELEASE_PROBABILITY_FIELD_DEFINITION = SWEHelper.getPropertyUri("EMLReleaseProbability");

    public Text createVersionIdField(){
        return createText()
                .definition(VERSION_ID_FIELD_DEFINITION)
                .description(VERSION_ID_FIELD_DESCRIPTION)
                .label(VERSION_ID_FIELD_LABEL)
                .build();
    }

    public Text createModelIdField(){
        return createText()
                .definition(MODEL_ID_FIELD_DEFINITION)
                .description(MODEL_ID_FIELD_DESCRIPTION)
                .label(MODEL_ID_FIELD_LABEL)
                .build();
    }
    public Quantity createThresholdsField(){
        return createQuantity()
                .definition(THRESHOLDS_FIELD_DEFINITION)
                .description(THRESHOLDS_FIELD_DESCRIPTION)
                .label(THRESHOLDS_FIELD_LABEL)
                .build();
    }
    public Text createPortIdField(){
        return createText()
                .definition(PORT_ID_FIELD_DEFINITION)
                .description(PORT_ID_FIELD_DESCRIPTION)
                .label(PORT_ID_FIELD_LABEL)
                .build();
    }
    public Text createLaneIdField(){
        return createText()
                .definition(LANE_ID_FIELD_DEFINITION)
                .description(LANE_ID_FIELD_DESCRIPTION)
                .label(LANE_ID_FIELD_LABEL)
                .build();
    }

    public Text createDateTimeField(){
        return createText()
                .definition(TIME_DATE_FIELD_DEFINITION)
                .description(TIME_DATE_FIELD_DESCRIPTION)
                .label(TIME_DATE_FIELD_LABEL)
                .build();
    }

    public Text createSegmentIdField(){
        return createText()
                .definition(SEGMENT_ID_FIELD_DEFINITION)
                .description(SEGMENT_ID_FIELD_DESCRIPTION)
                .label(SEGMENT_ID_FIELD_LABEL)
                .build();
    }
    public Category createRpmResultField(){
        return createCategory()
                .definition(RPM_RESULT_FIELD_DEFINITION)
                .description(RPM_RESULT_FIELD_DESCRIPTION)
                .label(RPM_RESULT_FIELD_LABEL)
                .addAllowedValues("INVESTIGATE", "NONE", "RELEASE")
                .build();
    }

    public Boolean createRpmGammaAlertField(){
        return createBoolean()
                .definition(RPM_GAMMA_ALERT_FIELD_DEFINITION)
                .description(RPM_GAMMA_ALERT_FIELD_DESCRIPTION)
                .label(RPM_GAMMA_ALERT_FIELD_LABEL)
                .build();
    }
    public Boolean createRpmNeutronAlertField(){
        return createBoolean()
                .definition(RPM_NEUTRON_ALERT_FIELD_DEFINITION)
                .description(RPM_NEUTRON_ALERT_FIELD_DESCRIPTION)
                .label(RPM_NEUTRON_ALERT_FIELD_LABEL)
                .build();
    }
    public Boolean createRpmScanErrorField(){
        return createBoolean()
                .definition(RPM_SCAN_ERROR_FIELD_DEFINITION)
                .description(RPM_SCAN_ERROR_FIELD_DESCRIPTION)
                .label(RPM_SCAN_ERROR_FIELD_LABEL)
                .build();
    }
    public Category createResultsField(){
        return createCategory()
                .definition(RESULT_FIELD_DEFINITION)
                .description(RESULT_FIELD_DESCRIPTION)
                .label(RESULT_FIELD_LABEL)
                .addAllowedValues(
                        Analysis.RecommendedAction.NONE.getValue(),
                        Analysis.RecommendedAction.INVESTIGATE.getValue(),
                        Analysis.RecommendedAction.RELEASE.getValue())
                .build();
    }
    public Quantity createReleaseProbabilityField(){
        return createQuantity()
                .definition(RELEASE_PROBABILITY_FIELD_DEFINITION)
                .description(RELEASE_PROBABILITY_FIELD_DESCRIPTION)
                .label(RELEASE_PROBABILITY_FIELD_LABEL)
                .build();
    }

    public Quantity createInvestigativeProbabilityField(){
        return createQuantity()
                .definition(INVESTIGATIVE_PROBABILITY_FIELD_DEFINITION)
                .description(INVESTIGATIVE_PROBABILITY_FIELD_DESCRIPTION)
                .label(INVESTIGATIVE_PROBABILITY_FIELD_LABEL)
                .build();
    }
    public Boolean createErnieGammaAlertField(){
        return createBoolean()
                .definition(GAMMA_ALERT_FIELD_DEFINITION)
                .description(GAMMA_ALERT_FIELD_DESCRIPTION)
                .label(GAMMA_ALERT_FIELD_LABEL)
                .build();
    }

    public Boolean createErnieNeutronAlertField(){
        return createBoolean()
                .definition(NEUTRON_ALERT_FIELD_DEFINITION)
                .description(NEUTRON_ALERT_FIELD_DESCRIPTION)
                .label(NEUTRON_ALERT_FIELD_LABEL)
                .build();
    }
    public Category createSourceTypeField(){
        return createCategory()
                .definition(SOURCE_FIELD_DEFINITION)
                .description(SOURCE_TYPE_FIELD_DESCRIPTION)
                .label(SOURCE_TYPE_FIELD_LABEL)
                .addAllowedValues(
//                        "CONTAMINATION",
//                        "FISSILE",
//                        "INDUSTRIAL",
//                        "INVALID",
//                        "MEDICAL",
//                        "NEUTRON",
                        "NONE",
                        "NonEmitting",
                        "NORM",
//                        "UNKNOWN",
                        "Threat")

                .build();
    }
    public Category createOverallSourceTypeField(){
        return createCategory()
                .definition(OVERALL_SOURCE_FIELD_DEFINITION)
                .description(OVERALL_SOURCE_TYPE_FIELD_DESCRIPTION)
                .label(OVERALL_SOURCE_TYPE_FIELD_LABEL)
                .addAllowedValues(
//                        "CONTAMINATION",
//                        "FISSILE",
//                        "INDUSTRIAL",
//                        "INVALID",
//                        "MEDICAL",
//                        "NEUTRON",
                        "NONE",
                        "NonEmitting",
                        "NORM",
//                        "UNKNOWN",
                        "Threat")
                .build();
    }
    public Text createClassifierUsedField(){
        return createText()
                .definition(CLASSIFIER_FIELD_DEFINITION)
                .description(CLASSIFIER_FIELD_DESCRIPTION)
                .label(CLASSIFIER_FIELD_LABEL)
                .build();
    }


    public Quantity createXLocation2Field() {
        return createQuantity()
                .definition(X_LOCATION_2_FIELD_DEFINITION)
                .description(X_LOCATION_2_FIELD_DESCRIPTION)
                .label(X_LOCATION_2_FIELD_LABEL)
                .build();
    }
    public Quantity createYLocationField() {
        return createQuantity()
                .definition(Y_LOCATION_FIELD_DEFINITION)
                .description(Y_LOCATION_FIELD_DESCRIPTION)
                .label(Y_LOCATION_FIELD_LABEL)
                .build();
    }
    public Quantity createZLocationField() {
        return createQuantity()
                .definition(Z_LOCATION_FIELD_DEFINITION)
                .description(Z_LOCATION_FIELD_DESCRIPTION)
                .label(Z_LOCATION_FIELD_LABEL)
                .build();
    }
    public Quantity createXLocation1Field() {
        return createQuantity()
                .definition(X_LOCATION_1_FIELD_DEFINITION)
                .description(X_LOCATION_1_FIELD_DESCRIPTION)
                .label(X_LOCATION_1_FIELD_LABEL)
                .build();
    }

    public Quantity createProbabilityNormField() {
        return createQuantity()
                .definition(PROBABILITY_NORM_FIELD_DEFINITION)
                .description(PROBABILITY_NORM_FIELD_DESCRIPTION)
                .label(PROBABILITY_NORM_FIELD_LABEL)
                .build();
    }
    public Quantity createProbabilityNonEmittingField() {
        return createQuantity()
                .definition(PROBABILITY_NON_EMITTING_FIELD_DEFINITION)
                .description(PROBABILITY_NON_EMITTING_FIELD_DESCRIPTION)
                .label(PROBABILITY_NON_EMITTING_FIELD_LABEL)
                .build();
    }
    public Quantity createProbabilityThreatField() {
        return createQuantity()
                .definition(PROBABILITY_THREAT_FIELD_DEFINITION)
                .description(PROBABILITY_THREAT_FIELD_DESCRIPTION)
                .label(PROBABILITY_THREAT_FIELD_LABEL)
                .build();
    }


    public Text createOverallClassifierUsedField(){
        return createText()
                .definition(OVERALL_CLASSIFIER_FIELD_DEFINITION)
                .description(OVERALL_CLASSIFIER_FIELD_DESCRIPTION)
                .label(OVERALL_CLASSIFIER_FIELD_LABEL)
                .build();
    }


    public Quantity createOverallXLocation2Field() {
        return createQuantity()
                .definition(OVERALL_X_LOCATION_2_FIELD_DEFINITION)
                .description(OVERALL_X_LOCATION_2_FIELD_DESCRIPTION)
                .label(OVERALL_X_LOCATION_2_FIELD_LABEL)
                .build();
    }
    public Quantity createOverallYLocationField() {
        return createQuantity()
                .definition(OVERALL_Y_LOCATION_FIELD_DEFINITION)
                .description(OVERALL_Y_LOCATION_FIELD_DESCRIPTION)
                .label(OVERALL_Y_LOCATION_FIELD_LABEL)
                .build();
    }
    public Quantity createOverallZLocationField() {
        return createQuantity()
                .definition(OVERALL_Z_LOCATION_FIELD_DEFINITION)
                .description(OVERALL_Z_LOCATION_FIELD_DESCRIPTION)
                .label(OVERALL_Z_LOCATION_FIELD_LABEL)
                .build();
    }
    public Quantity createOverallXLocation1Field() {
        return createQuantity()
                .definition(OVERALL_X_LOCATION_1_FIELD_DEFINITION)
                .description(OVERALL_X_LOCATION_1_FIELD_DESCRIPTION)
                .label(OVERALL_X_LOCATION_1_FIELD_LABEL)
                .build();
    }

    public Quantity createOverallProbabilityNormField() {
        return createQuantity()
                .definition(OVERALL_PROBABILITY_NORM_FIELD_DEFINITION)
                .description(OVERALL_PROBABILITY_NORM_FIELD_DESCRIPTION)
                .label(OVERALL_PROBABILITY_NORM_FIELD_LABEL)
                .build();
    }
    public Quantity createOverallProbabilityNonEmittingField() {
        return createQuantity()
                .definition(OVERALL_PROBABILITY_NON_EMITTING_FIELD_DEFINITION)
                .description(OVERALL_PROBABILITY_NON_EMITTING_FIELD_DESCRIPTION)
                .label(OVERALL_PROBABILITY_NON_EMITTING_FIELD_LABEL)
                .build();
    }
    public Quantity createOverallProbabilityThreatField() {
        return createQuantity()
                .definition(OVERALL_PROBABILITY_THREAT_FIELD_DEFINITION)
                .description(OVERALL_PROBABILITY_THREAT_FIELD_DESCRIPTION)
                .label(OVERALL_PROBABILITY_THREAT_FIELD_LABEL)
                .build();
    }
    public Text createMessageField(){
        return createText()
                .definition(MESSAGE_FIELD_DEFINITION)
                .description(MESSAGE_FIELD_DESCRIPTION)
                .label(MESSAGE_FIELD_LABEL)
                .build();
    }
    public Text createYellowLightMessageField(){
        return createText()
                .definition(YELLOW_LIGHT_MESSAGE_FIELD_DEFINITION)
                .description(YELLOW_LIGHT_MESSAGE_FIELD_DESCRIPTION)
                .label(YELLOW_LIGHT_MESSAGE_FIELD_LABEL)
                .build();
    }
    public Quantity createVehicleClassField() {
        return createQuantity()
                .definition(VEHICLE_CLASS_FIELD_DEFINITION)
                .description(VEHICLE_CLASS_FIELD_DESCRIPTION)
                .label(VEHICLE_CLASS_FIELD_LABEL)
                .build();
    }
    public Quantity createVehicleLengthField() {
        return createQuantity()
                .definition(VEHICLE_LENGTH_FIELD_DEFINITION)
                .description(VEHICLE_LENGTH_FIELD_DESCRIPTION)
                .label(VEHICLE_LENGTH_FIELD_LABEL)
                .build();
    }
}

