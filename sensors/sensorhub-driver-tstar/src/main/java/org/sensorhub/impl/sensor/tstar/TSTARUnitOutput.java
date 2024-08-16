//package org.sensorhub.impl.sensor.tstar;
//
//import net.opengis.swe.v20.DataBlock;
//import net.opengis.swe.v20.DataComponent;
//import net.opengis.swe.v20.DataEncoding;
//import net.opengis.swe.v20.DataRecord;
//import org.sensorhub.api.data.DataEvent;
//import org.sensorhub.impl.sensor.AbstractSensorOutput;
//import org.sensorhub.impl.sensor.tstar.responses.Unit;
//import org.vast.swe.SWEHelper;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
//
//public class TSTARUnitOutput extends AbstractSensorOutput<TSTARDriver> {
//    private static final String SENSOR_OUTPUT_NAME = "Unit Output";
//    protected DataRecord dataStruct;
//    DataEncoding dataEncoding;
//    DataBlock dataBlock;
//    Long campaignLastActivity;
//
//    public TSTARUnitOutput(TSTARDriver parentSensor) {
//        super(SENSOR_OUTPUT_NAME, parentSensor);
//    }
//
//    protected void init() {
//        TSTARHelper tstarHelper = new TSTARHelper();
//
//        // SWE Common data structure
//        dataStruct = tstarHelper.createRecord()
//                .name(getName())
//                .label(SENSOR_OUTPUT_NAME)
//                .definition(SWEHelper.getPropertyUri("UnitData"))
//                .addField("samplingTime", tstarHelper.createPrecisionTimeStamp())
//                .addField("campaignId", tstarHelper.createId())
//                .addField("name", tstarHelper.createCampaignName())
//                .addField("unit_id", tstarHelper.createUnitId())
//                .addField("enabled", tstarHelper.createEnabled())
//                .addField("armed", tstarHelper.createArmed())
//                .addField("vehicle", tstarHelper.createVehicle())
//                .addField("cargo", tstarHelper.createCargo())
//                .addField("last-activity", tstarHelper.createLastActivity())
//                .addField("deleted", tstarHelper.createDeleted())
//                .build();
//
//        // set encoding to CSV
//        dataEncoding = tstarHelper.newTextEncoding(",", "\n");
//    }
//
//    public void parse(Unit unit) {
//
//        if (latestRecord == null) {
//
//            dataBlock = dataStruct.createDataBlock();
//
//        } else {
//            dataBlock = latestRecord.renew();
//        }
//
//        latestRecordTime = System.currentTimeMillis() / 1000;
//        setUnitTime(unit);
//
//        dataBlock.setLongValue(0, latestRecordTime);
//        dataBlock.setIntValue(1, campaign.id);
//        dataBlock.setStringValue(2, campaign.name);
//        dataBlock.setIntValue(3, campaign.unit_id);
//        dataBlock.setBooleanValue(4, campaign.enabled);
//        dataBlock.setBooleanValue(5, campaign.armed);
//        dataBlock.setStringValue(6, campaign.vehicle);
//        dataBlock.setStringValue(7, campaign.cargo);
//        dataBlock.setLongValue(8, campaignLastActivity);
//        dataBlock.setBooleanValue(9, campaign.deleted);
//
//        // update latest record and send event
//        latestRecord = dataBlock;
//        eventHandler.publish(new DataEvent(latestRecordTime, TSTARUnitOutput.this, dataBlock));
//    }
//
//    public void setUnitTime(Unit unit){
//        // parse UTC  to epoch time for 'last_activity'
//        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//
//        Date lastActivity = unit.last_activity;
//        campaignLastActivity = lastActivity.getTime()/ 1000;
//    }
//
//    public DataComponent getRecordDescription() {
//        return dataStruct;
//    }
//
//    @Override
//    public DataEncoding getRecommendedEncoding() {
//        return dataEncoding;
//    }
//
//    @Override
//    public double getAverageSamplingPeriod() {
//        return 1;
//    }
//}
//
