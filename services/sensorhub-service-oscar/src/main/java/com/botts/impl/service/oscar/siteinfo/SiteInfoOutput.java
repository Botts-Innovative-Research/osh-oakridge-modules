package com.botts.impl.service.oscar.siteinfo;

import com.botts.impl.service.oscar.OSCARSystem;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.helper.GeoPosHelper;

public class SiteInfoOutput extends AbstractSensorOutput<OSCARSystem> {

    public static final String NAME = "siteInfo";
    public static final String LABEL = "Site Info";
    public static final String DESCRIPTION = "Important information about this OSCAR node's site";

    DataComponent recordStructure;
    DataEncoding recordEncoding;
    GeoPosHelper fac;

    protected SiteInfoOutput(OSCARSystem parentSensor) {
        super(NAME, parentSensor);

        this.recordStructure = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .addField("sampleTime", fac.createTime().asSamplingTimeIsoUTC())
                .addField("siteDiagramPath", fac.createText()
                        .description("Path of site diagram image"))
                .addField("siteBoundingBox", fac.createRecord()
                        .description("Geographic bounding box coordinates of site diagram")
                        .addField("lowerLeftBound", fac.createLocationVectorLatLon())
                        .addField("upperRightBound", fac.createLocationVectorLatLon()))
                .build();

        this.recordEncoding = new TextEncodingImpl();
    }

    public void setData(String siteDiagramPath, double[] lowerLeftBound, double[] upperRightBound) {

        long timeMillis = System.currentTimeMillis();

        DataBlock dataBlock = latestRecord == null ? recordStructure.createDataBlock() : latestRecord.renew();

        dataBlock.setDoubleValue(0, timeMillis/1000d);
        dataBlock.setStringValue(1, siteDiagramPath);
        dataBlock.setDoubleValue(2, lowerLeftBound[0]);
        dataBlock.setDoubleValue(3, lowerLeftBound[1]);
        dataBlock.setDoubleValue(4, upperRightBound[0]);
        dataBlock.setDoubleValue(5, upperRightBound[1]);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
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
