package org.sensorhub.impl.utils.rad.output;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.impl.data.AbstractDataInterface;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.TextEncodingImpl;

import java.time.Instant;

public class DailyFileOutput<SensorType extends IDataProducer> extends AbstractDataInterface<SensorType> {

    public static final String NAME = "dailyFile";
    private static final String LABEL = "Daily File";

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    public DailyFileOutput(SensorType parentSensor) {
        super(NAME, parentSensor);
        init();
    }

    public void init() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var rpmMessage = radHelper.createRpmMessage();

        dataStruct = radHelper.createRecord()
                .name(getName())
                .label(LABEL)
                .updatable(true)
                .definition(RADHelper.getRadUri("DailyFile"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(rpmMessage.getName(), rpmMessage)
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    public void onNewMessage(String csvString) {
        onNewMessage(csvString, Instant.now());
    }

    public void onNewMessage(String csvString, Instant timeStamp) {
        DataBlock dataBlock;

        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {
            dataBlock = latestRecord.renew();
        }
        dataBlock.setTimeStamp(0, timeStamp);
        dataBlock.setStringValue(1, csvString);

        eventHandler.publish(new DataEvent(timeStamp.toEpochMilli(), DailyFileOutput.this, dataBlock));
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
