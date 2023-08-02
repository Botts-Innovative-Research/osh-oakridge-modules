package com.botts.impl.sensor.kromekd5;

import com.botts.impl.sensor.kromekd5.messages.D5Message;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;

public class AnalysisOutput  extends OutputBase{
    private static final String SENSOR_OUTPUT_NAME  = "D5 Analysis Report";

    private static final Logger logger = LoggerFactory.getLogger(AnalysisOutput.class);

    public AnalysisOutput(D5Sensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
        logger.debug(SENSOR_OUTPUT_NAME + " output created");
    }

    @Override
    protected void init(){
        RADHelper radHelper = new RADHelper();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label("Analysis Report")
                .definition(RADHelper.getRadUri("analysis-report"))
                .addField("Result Status", radHelper.createAnalysisResultStatus())
                .addField("Nuclide Name", radHelper.createText().name("NuclideName").label("Nuclide Name").definition(RADHelper.getRadUri("nuclide-name")))
                .addField("Nuclide Confidence Value", radHelper.createQuantity().name("NuclideConfidenceValue").label("Nuclide Confidence Value").definition(RADHelper.getRadUri("nuclide-confidence")))
                .build();

        dataEncoding = new TextEncodingImpl(",","\n");

    }

    public void parseData(String resultStatus, String nuclideName, Double confidenceValue){
        if (latestRecord == null)
            dataBlock = dataStruct.createDataBlock();
        else
            dataBlock = latestRecord.renew();

        latestRecordTime = System.currentTimeMillis();

        dataBlock.setStringValue(0, resultStatus);
        dataBlock.setStringValue(1, nuclideName);
        dataBlock.setDoubleValue(2, confidenceValue);

        eventHandler.publish(new DataEvent(latestRecordTime, AnalysisOutput.this, dataBlock));
    }
}
