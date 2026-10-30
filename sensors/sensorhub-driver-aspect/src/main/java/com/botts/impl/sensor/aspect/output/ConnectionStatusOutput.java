package com.botts.impl.sensor.aspect.output;


import com.botts.impl.sensor.aspect.AspectSensor;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

public class ConnectionStatusOutput extends AbstractSensorOutput<AspectSensor> {

    private static final String SENSOR_OUTPUT_NAME = "connectionStatus";
    private static final String SENSOR_OUTPUT_LABEL = "Connection Status";
    public static final long HEARTBEAT_INTERVAL_MILLIS = 30_000L;

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    private Boolean lastPublishedStatus;
    private long lastPublishedTimeMillis = Long.MIN_VALUE;

    public ConnectionStatusOutput(AspectSensor parentSensor){
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

    public synchronized void onNewMessage(boolean hasReceivedMessage) {
        onNewMessage(hasReceivedMessage, System.currentTimeMillis());
    }

    public synchronized void onNewMessage(boolean hasReceivedMessage, long nowMillis) {
        if (lastPublishedStatus != null
                && lastPublishedStatus == hasReceivedMessage
                && nowMillis - lastPublishedTimeMillis < HEARTBEAT_INTERVAL_MILLIS) {
            return;
        }

        DataBlock dataBlock;

        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }
        long timeStamp = nowMillis / 1000;

        dataBlock.setLongValue(0, timeStamp);
        dataBlock.setBooleanValue(1, hasReceivedMessage);
        latestRecord = dataBlock;
        latestRecordTime = timeStamp;
        lastPublishedStatus = hasReceivedMessage;
        lastPublishedTimeMillis = nowMillis;

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
