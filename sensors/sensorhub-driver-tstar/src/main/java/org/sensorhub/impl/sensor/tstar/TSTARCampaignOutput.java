package org.sensorhub.impl.sensor.tstar;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.tstar.responses.Campaign;
import org.vast.swe.SWEHelper;

import java.text.SimpleDateFormat;
import java.util.Date;


public class TSTARCampaignOutput extends AbstractSensorOutput<TSTARDriver> {
    private static final String SENSOR_OUTPUT_NAME = "Campaign Output";
    protected DataRecord dataStruct;
    DataEncoding dataEncoding;
    DataBlock dataBlock;
    Long campaignLastActivity;

    public TSTARCampaignOutput(TSTARDriver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init() {
        TSTARHelper tstarHelper = new TSTARHelper();

        // SWE Common data structure
        dataStruct = tstarHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri("CampaignData"))
                .addField("campaignSamplingTime", tstarHelper.createTimestamp())
                .addField("campaignId", tstarHelper.createCampaignId())
                .addField("campaignName", tstarHelper.createCampaignName())
                .addField("lastActivity", tstarHelper.createLastActivity())
                .addField("unitId", tstarHelper.createUnitId())
                .addField("unitName", tstarHelper.createUnitName())
                .addField("totalAlerts", tstarHelper.createTotalAlerts())
                .addField("unacknowledgedAlerts", tstarHelper.createUnacknowledgedAlerts())
                .addField("unitMessageCount", tstarHelper.createUnitMessageCount())
                .addField("enabled", tstarHelper.createEnabled())
                .addField("armed", tstarHelper.createArmed())
                .addField("archived", tstarHelper.createArchived())
                .addField("deleted", tstarHelper.createDeleted())
                .addField("vehicle", tstarHelper.createVehicle())
                .addField("cargo", tstarHelper.createCargo())
                .addField("watchers", tstarHelper.createWatchers())
                .addField("users", tstarHelper.createUsers())
                .addField("fences", tstarHelper.createFences())
                .build();

        // set encoding to CSV
        dataEncoding = tstarHelper.newTextEncoding(",", "\n");
    }

    public void parse(Campaign campaign) {

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {
            dataBlock = latestRecord.renew();
        }

        latestRecordTime = System.currentTimeMillis() / 1000;
        setCampaignTime(campaign);

            dataBlock.setLongValue(0, latestRecordTime);
            dataBlock.setIntValue(1, campaign.id);
            dataBlock.setStringValue(2, campaign.name);
            dataBlock.setLongValue(3, campaignLastActivity);
            dataBlock.setIntValue(4, campaign.unit_id);
            dataBlock.setStringValue(5, campaign.unit_name);
            dataBlock.setIntValue(6, campaign.total_alerts);
            dataBlock.setIntValue(7, campaign.unacknowledged_alerts);
            dataBlock.setIntValue(8, campaign.unit_message_count);
            dataBlock.setBooleanValue(9, campaign.enabled);
            dataBlock.setBooleanValue(10, campaign.armed);
            dataBlock.setBooleanValue(11, campaign.archived);
            dataBlock.setBooleanValue(12, campaign.deleted);
            dataBlock.setStringValue(13, campaign.vehicle);
            dataBlock.setStringValue(14, campaign.cargo);
            dataBlock.setStringValue(15, campaign.watchers);
            dataBlock.setStringValue(16, campaign.users);
            dataBlock.setStringValue(17, campaign.fences);

            // update latest record and send event
            latestRecord = dataBlock;
            eventHandler.publish(new DataEvent(latestRecordTime, TSTARCampaignOutput.this, dataBlock));
        }

    public void setCampaignTime(Campaign campaign){

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        Date lastActivity = campaign.last_activity;
        campaignLastActivity = lastActivity.getTime()/ 1000;
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
        return 1;
    }
}