package com.botts.impl.sensor.rapiscan.eml;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import gov.llnl.ernie.api.Results;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataArrayImpl;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEBuilders;

import static com.botts.impl.sensor.rapiscan.eml.EMLFieldFactory.*;


public class EMLAnalysisOutput extends AbstractSensorOutput<RapiscanSensor> {
    private static final String SENSOR_OUTPUT_NAME = "ERNIEAnalysis";
    private static final String SENSOR_OUTPUT_LABEL = "EML ERNIE Analysis";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "EML ERNIE Analysis Data parsed from XML";

    private static final Logger logger = LoggerFactory.getLogger(EMLAnalysisOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    public EMLAnalysisOutput(RapiscanSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        dataStruct = getDataRecord();
        dataEncoding = new TextEncodingImpl(",", "\n");
    }
    private DataRecord getDataRecord(){
        var emlFieldFactory = new EMLFieldFactory();

        SWEBuilders.DataRecordBuilder dataRecordBuilder = emlFieldFactory.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("eml-analyis"))
                .description(SENSOR_OUTPUT_DESCRIPTION);

        dataRecordBuilder.addField(RESULT_FIELD_NAME, emlFieldFactory.createResultsField());
        dataRecordBuilder.addField(INVESTIGATIVE_PROBABILITY_FIELD_NAME, emlFieldFactory.createInvestigativeProbabilityField());
        dataRecordBuilder.addField(RELEASE_PROBABILITY_FIELD_NAME, emlFieldFactory.createReleaseProbabilityField());
        dataRecordBuilder.addField(GAMMA_ALERT_FIELD_NAME, emlFieldFactory.createErnieGammaAlertField());
        dataRecordBuilder.addField(NEUTRON_ALERT_FIELD_NAME, emlFieldFactory.createErnieNeutronAlertField());
        dataRecordBuilder.addField("sourceCount", emlFieldFactory.createCount().id("sourceCountId"));

        SWEBuilders.DataRecordBuilder sourceArrayBuilder = emlFieldFactory.createRecord();
        sourceArrayBuilder.addField(SOURCE_TYPE_FIELD_NAME, emlFieldFactory.createSourceTypeField());
        sourceArrayBuilder.addField(CLASSIFIER_FIELD_NAME, emlFieldFactory.createClassifierUsedField());
        sourceArrayBuilder.addField(X_LOCATION_1_FIELD_NAME, emlFieldFactory.createXLocation1Field());
        sourceArrayBuilder.addField(X_LOCATION_2_FIELD_NAME, emlFieldFactory.createXLocation2Field());
        sourceArrayBuilder.addField(Y_LOCATION_FIELD_NAME, emlFieldFactory.createYLocationField());
        sourceArrayBuilder.addField(Z_LOCATION_FIELD_NAME, emlFieldFactory.createZLocationField());
        sourceArrayBuilder.addField(PROBABILITY_NON_EMITTING_FIELD_NAME, emlFieldFactory.createProbabilityNonEmittingField());
        sourceArrayBuilder.addField(PROBABILITY_NORM_FIELD_NAME, emlFieldFactory.createProbabilityNormField());
        sourceArrayBuilder.addField(PROBABILITY_THREAT_FIELD_NAME, emlFieldFactory.createProbabilityThreatField());
        dataRecordBuilder.addField("sources", emlFieldFactory.createArray()
                       .withVariableSize("sourceCountId")
                       .withElement("source", sourceArrayBuilder));

        dataRecordBuilder.addField(OVERALL_SOURCE_TYPE_FIELD_NAME, emlFieldFactory.createOverallSourceTypeField());
        dataRecordBuilder.addField(OVERALL_CLASSIFIER_FIELD_NAME, emlFieldFactory.createOverallClassifierUsedField());
        dataRecordBuilder.addField(OVERALL_X_LOCATION_1_FIELD_NAME, emlFieldFactory.createOverallXLocation1Field());
        dataRecordBuilder.addField(OVERALL_X_LOCATION_2_FIELD_NAME, emlFieldFactory.createOverallXLocation2Field());
        dataRecordBuilder.addField(OVERALL_Y_LOCATION_FIELD_NAME, emlFieldFactory.createOverallYLocationField());
        dataRecordBuilder.addField(OVERALL_Z_LOCATION_FIELD_NAME, emlFieldFactory.createOverallZLocationField());
        dataRecordBuilder.addField(OVERALL_PROBABILITY_NON_EMITTING_FIELD_NAME, emlFieldFactory.createOverallProbabilityNonEmittingField());
        dataRecordBuilder.addField(OVERALL_PROBABILITY_NORM_FIELD_NAME, emlFieldFactory.createOverallProbabilityNormField());
        dataRecordBuilder.addField(OVERALL_PROBABILITY_THREAT_FIELD_NAME, emlFieldFactory.createOverallProbabilityThreatField());
        dataRecordBuilder.addField(VEHICLE_CLASS_FIELD_NAME, emlFieldFactory.createVehicleClassField());
        dataRecordBuilder.addField(VEHICLE_LENGTH_FIELD_NAME, emlFieldFactory.createVehicleLengthField());
        dataRecordBuilder.addField(YELLOW_LIGHT_MESSAGE_FIELD_NAME, emlFieldFactory.createYellowLightMessageField());

        return dataRecordBuilder.build();

    }

    public void handleAnalysisMessage(Results results, long timeStamp)
    {
        dataStruct = getDataRecord();
        DataBlock dataBlock = dataStruct.createDataBlock();
        dataStruct.setData(dataBlock);

        int index = 0;
        dataBlock.setStringValue(index++, results.getResult().toString());
        dataBlock.setDoubleValue(index++, results.getInvestigateProbability());
        dataBlock.setDoubleValue(index++, results.getReleaseProbability());
        dataBlock.setBooleanValue(index++, results.getERNIEGammaAlert());
        dataBlock.setBooleanValue(index++, results.getERNIENeutronAlert());

        int sourceCount = results.getNumberOfSources();
        dataBlock.setIntValue(index++, sourceCount);

        var array = ((DataArrayImpl) dataStruct.getComponent("sources"));
        array.updateSize();

        if(sourceCount > 0){
            for(int i = 0; i < sourceCount; i++){
                dataBlock.setStringValue(index++, results.getSource(i).getSourceType());
                dataBlock.setStringValue(index++, results.getSource(i).getClassifierUsed());
                dataBlock.setDoubleValue(index++, results.getSource(i).getxLocation1());
                dataBlock.setDoubleValue(index++, results.getSource(i).getxLocation2());
                dataBlock.setDoubleValue(index++, results.getSource(i).getyLocation());
                dataBlock.setDoubleValue(index++, results.getSource(i).getzLocation());
                dataBlock.setDoubleValue(index++, results.getSource(i).getProbabilityNonEmitting());
                dataBlock.setDoubleValue(index++, results.getSource(i).getProbabilityNORM());
                dataBlock.setDoubleValue(index++, results.getSource(i).getProbabilityThreat());
            }
            dataBlock.setStringValue(index++, results.getOverallSource().getSourceType());
            dataBlock.setStringValue(index++, results.getOverallSource().getClassifierUsed());
            dataBlock.setDoubleValue(index++, results.getOverallSource().getxLocation1());
            dataBlock.setDoubleValue(index++, results.getOverallSource().getxLocation2());
            dataBlock.setDoubleValue(index++, results.getOverallSource().getyLocation());
            dataBlock.setDoubleValue(index++, results.getOverallSource().getzLocation());
            dataBlock.setDoubleValue(index++, results.getOverallSource().getProbabilityNonEmitting());
            dataBlock.setDoubleValue(index++, results.getOverallSource().getProbabilityNORM());
            dataBlock.setDoubleValue(index++, results.getOverallSource().getProbabilityThreat());
        }
        dataBlock.setIntValue(index++, results.getVehicleClass());
        dataBlock.setDoubleValue(index++, results.getVehicleLength());
//        dataBlock.setStringValue(index++, results.getMessage());
        dataBlock.setStringValue(index++, results.getYellowLightMessage());

//        dataBlock.setStringValue(resultIndex, results.getResult().toString());
//        dataBlock.setDoubleValue(investigateProbabilityIndex, results.getInvestigateProbability());
//        dataBlock.setDoubleValue(releaseProbabilityIndex, results.getReleaseProbability());
//        dataBlock.setBooleanValue(gammaAlertIndex, results.getERNIEGammaAlert());
//        dataBlock.setBooleanValue(neutronAlertIndex, results.getERNIENeutronAlert());
//
//        int sourceCount = results.getNumberOfSources();
//        dataBlock.setIntValue(sourceCountIdIndex, sourceCount);
//
//        var array = ((DataArrayImpl) dataStruct.getComponent("sources"));
//        array.updateSize();
//
//        if(sourceCount > 0){
//            for(int i = 0; i < sourceCount; i++){
//                dataBlock.setStringValue(sourceTypeIndex, results.getSource(i).getSourceType());
//                dataBlock.setStringValue(classifierUsedIndex, results.getSource(i).getClassifierUsed());
//                dataBlock.setDoubleValue(xLocation1Index, results.getSource(i).getxLocation1());
//                dataBlock.setDoubleValue(xLocation2Index, results.getSource(i).getxLocation2());
//                dataBlock.setDoubleValue(yLocationIndex, results.getSource(i).getyLocation());
//                dataBlock.setDoubleValue(zLocationIndex, results.getSource(i).getzLocation());
//                dataBlock.setDoubleValue(probabilityNonEmittingIndex, results.getSource(i).getProbabilityNonEmitting());
//                dataBlock.setDoubleValue(probabilityNORMIndex, results.getSource(i).getProbabilityNORM());
//                dataBlock.setDoubleValue(probabilityThreatIndex, results.getSource(i).getProbabilityThreat());
//            }
//            dataBlock.setStringValue(overallSourceTypeIndex, results.getOverallSource().getSourceType());
//            dataBlock.setStringValue(overallClassifierUsedIndex, results.getOverallSource().getClassifierUsed());
//            dataBlock.setDoubleValue(overallXLocation1Index, results.getOverallSource().getxLocation1());
//            dataBlock.setDoubleValue(overallXLocation2Index, results.getOverallSource().getxLocation2());
//            dataBlock.setDoubleValue(overallYLocationIndex, results.getOverallSource().getyLocation());
//            dataBlock.setDoubleValue(overallZLocationIndex, results.getOverallSource().getzLocation());
//            dataBlock.setDoubleValue(overallProbabilityNonEmittingIndex, results.getOverallSource().getProbabilityNonEmitting());
//            dataBlock.setDoubleValue(overallProbabilityNORMIndex, results.getOverallSource().getProbabilityNORM());
//            dataBlock.setDoubleValue(overallProbabilityThreatIndex, results.getOverallSource().getProbabilityThreat());
//        }
//        dataBlock.setIntValue(vehicleClassIndex, results.getVehicleClass());
//        dataBlock.setDoubleValue(vehicleLengthIndex, results.getVehicleLength());
//        dataBlock.setStringValue(messageIndex, results.getMessage());
//        dataBlock.setStringValue(yellowLightMessageIndex, results.getYellowLightMessage());


        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(System.currentTimeMillis(), EMLAnalysisOutput.this, dataBlock));
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 0;
    }

}