package com.botts.impl.sensor.rapiscan.eml.outputs;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.sensor.rapiscan.eml.EMLFieldFactory;
import gov.llnl.ernie.api.Results;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.DataArrayImpl;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEBuilders;

import static com.botts.impl.sensor.rapiscan.eml.EMLFieldFactory.*;


public class EMLAnalysisOutput extends AbstractSensorOutput<RapiscanSensor> {
    private static final String SENSOR_OUTPUT_NAME = "emlAnalysis";
    private static final String SENSOR_OUTPUT_LABEL = "EML ERNIE Analysis";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "EML ERNIE Analysis Data parsed from XML";

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

        dataRecordBuilder.addField(VEHICLE_CLASS_FIELD_NAME, emlFieldFactory.createVehicleClassField());
        dataRecordBuilder.addField(VEHICLE_LENGTH_FIELD_NAME, emlFieldFactory.createVehicleLengthField());
        dataRecordBuilder.addField(YELLOW_LIGHT_MESSAGE_FIELD_NAME, emlFieldFactory.createYellowLightMessageField());

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
        return dataRecordBuilder.build();

    }

    public void handleAnalysisMessage(Results results)
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

        dataBlock.setIntValue(index++, results.getVehicleClass());
        dataBlock.setDoubleValue(index++, results.getVehicleLength());
        dataBlock.setStringValue(index++, results.getYellowLightMessage());

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
            dataBlock.setDoubleValue(index, results.getOverallSource().getProbabilityThreat());
        }

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