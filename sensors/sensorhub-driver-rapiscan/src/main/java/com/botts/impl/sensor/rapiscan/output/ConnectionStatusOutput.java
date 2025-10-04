package com.botts.impl.sensor.rapiscan.output;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

public class ConnectionStatusOutput extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "connectionStatus";
    private static final String SENSOR_OUTPUT_LABEL = "Connection Status";

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    public ConnectionStatusOutput(RapiscanSensor parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var isConnected = radHelper.createBoolean()
                .name("isConnected")
                .label("Is Connected")
                .definition(RADHelper.getRadUri("ConnectionStatus"))
                .description("Is sensor receiving messages")
                .build();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .updatable(true)
                .addField(samplingTime.getName(), samplingTime)
                .addField(isConnected.getName(), isConnected)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void onNewMessage(boolean hasReceivedMessage) {
        DataBlock dataBlock;

        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }
        long timeStamp = System.currentTimeMillis()/1000;

        dataBlock.setLongValue(0, timeStamp);
        dataBlock.setBooleanValue(1, hasReceivedMessage);

        eventHandler.publish(new DataEvent(timeStamp, ConnectionStatusOutput.this, dataBlock));
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
