package com.botts.impl.service.oscar.clientconfig;

import com.botts.impl.service.oscar.OSCARSystem;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.helper.GeoPosHelper;

public class ClientConfigOutput extends AbstractSensorOutput<OSCARSystem> {

    public static final String NAME = "clientConfig";
    public static final String LABEL = "Client Config";
    public static final String DESCRIPTION = "Configuration of nodes for OSCAR-viewer connected to this node";

    DataComponent recordStructure;
    DataEncoding recordEncoding;
    GeoPosHelper fac;

    protected ClientConfigOutput(OSCARSystem parentSensor) {
        super(NAME, parentSensor);

        this.recordStructure = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .addField("sampleTime", fac.createTime().asSamplingTimeIsoUTC())
                // TODO: Add fields
                .build();

        this.recordEncoding = new TextEncodingImpl();
    }

    @Override
    public DataComponent getRecordDescription() {
        return recordStructure;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return recordEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 0;
    }
}
