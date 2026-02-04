package com.botts.impl.system.lane;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.VarRateSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.WebIdAnalysis;
import org.vast.data.TextEncodingImpl;

public class WebIdOutput extends VarRateSensorOutput<LaneSystem> {

    public static final String NAME = "webIdAnalysis";

    DataComponent recordStruct;
    DataEncoding recordEncoding;

    public WebIdOutput(LaneSystem parentSensor) {
        super(NAME, parentSensor, 0);

        recordStruct = new RADHelper().createWebIdRecord();
        recordEncoding = new TextEncodingImpl();
    }

    public void publishData(WebIdAnalysis analysis) {
        DataBlock dataBlock = WebIdAnalysis.fromWebIdAnalysis(analysis);
        eventHandler.publish(new DataEvent(analysis.getSampleTime().toEpochMilli(), WebIdOutput.this, dataBlock));
    }

    @Override
    public DataComponent getRecordDescription() {
        return recordStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return recordEncoding;
    }

}
