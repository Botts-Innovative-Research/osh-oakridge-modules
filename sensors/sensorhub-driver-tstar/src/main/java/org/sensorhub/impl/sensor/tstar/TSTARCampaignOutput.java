package org.sensorhub.impl.sensor.tstar;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;
import java.lang.Boolean;


public class TSTARCampaignOutput extends AbstractSensorOutput<TSTARDriver> {
    private static final String SENSOR_OUTPUT_NAME = "Campaign Output";
    DataRecord dataStruct;
    DataEncoding dataEncoding;

    public TSTARCampaignOutput(TSTARDriver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

//    Campaign {
//        id integer($int64)
//        name string
//        unit_id integer($int64)
//        enabled boolean
//        armed boolean
//        vehicle string
//        cargo string
//        last_activity string($date-time) archived boolean
//        deleted boolean
//    }

    protected void init() {
        TSTARHelper tstarHelper = new TSTARHelper();

        // SWE Common data structure
        dataStruct = tstarHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri("CampaignData"))
                .addField("id", tstarHelper.createId())
                .addField("name", tstarHelper.createCampaignName())
                .addField("unit_id", tstarHelper.createUnitId())
                .addField("enabled", tstarHelper.createEnabled())
                .addField("armed", tstarHelper.createArmed())
                .addField("vehicle", tstarHelper.createVehicle())
                .addField("cargo", tstarHelper.createCargo())
                .addField("last-activity", tstarHelper.createLastActivity())
                .addField("deleted", tstarHelper.createDeleted())
                .build();

        // set encoding to CSV
        dataEncoding = tstarHelper.newTextEncoding(",", "\n");
    }

    public void doStart() {

    }

    public void doStop() {

    }

    public void onNewMessage() {

        DataBlock dataBlock;

        int id = 0;
        String name = " ";
        int unit_id = 0;
        boolean enabled = false;
        boolean armed = false;
        String vehicle = " ";
        String cargo = " ";
        String last_activity = " ";
        boolean deleted = false;

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {
            dataBlock = latestRecord.renew();


            dataBlock.setIntValue(0, id);
            dataBlock.setStringValue(1, name);
            dataBlock.setIntValue(2, unit_id);
            dataBlock.setBooleanValue(3, enabled);
            dataBlock.setBooleanValue(4, armed);
            dataBlock.setStringValue(5, vehicle);
            dataBlock.setStringValue(6, cargo);
            dataBlock.setStringValue(7, last_activity);
            dataBlock.setBooleanValue(8, deleted);

            // update latest record and send event
            latestRecord = dataBlock;
            eventHandler.publish(new DataEvent(latestRecordTime, TSTARCampaignOutput.this, dataBlock));
        }
    }
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 5;
    }
}