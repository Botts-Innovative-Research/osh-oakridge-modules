package com.botts.impl.service.oscar;

import java.util.List;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataArrayImpl;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEHelper;

/** Publishes the service's custom disposition choices for API clients. */
public class AdjudicationCodesOutput extends AbstractSensorOutput<OSCARSystem> {
    public static final String NAME = "adjudicationCodes";
    public static final String DEFINITION = "urn:osh:def:property:CustomAdjudicationCodes";
    private final DataComponent record;

    public AdjudicationCodesOutput(OSCARSystem system) {
        super(NAME, system);
        var fac = new SWEHelper();
        record = fac.createRecord().name(NAME).label("Custom Disposition Codes")
                .addField("sampleTime", fac.createTime().asSamplingTimeIsoUTC())
                .addField("codeCount", fac.createCount().id("codeCount"))
                .addField("codes", fac.createArray().definition(DEFINITION)
                        .withVariableSize("codeCount")
                        .withElement("dispositionCode", fac.createRecord()
                                .addField("code", fac.createCount())
                                .addField("label", fac.createText())))
                .build();
    }

    public void publish(List<AdjudicationCodeConfig> codes) {
        record.clearData();
        var data = record.createDataBlock();
        record.setData(data);
        data.setDoubleValue(0, System.currentTimeMillis() / 1000d);
        int count = codes == null ? 0 : codes.size();
        data.setIntValue(1, count);
        ((DataArrayImpl) record.getComponent("codes")).updateSize();
        data.updateAtomCount();
        for (int i = 0; i < count; i++) {
            data.setIntValue(2 + i * 2, codes.get(i).code);
            data.setStringValue(3 + i * 2, codes.get(i).label);
        }
        latestRecord = data;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, data));
    }

    @Override
    public DataComponent getRecordDescription() { return record; }

    @Override
    public DataEncoding getRecommendedEncoding() { return new TextEncodingImpl(); }

    @Override
    public double getAverageSamplingPeriod() { return 0; }
}
