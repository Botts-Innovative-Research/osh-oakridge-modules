package org.sensorhub.impl.sensor.tstar;

import net.opengis.swe.v20.*;
import net.opengis.swe.v20.Boolean;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class TSTARHelper extends GeoPosHelper {

    String url;
    String campaignId;
    String unitId;

    public String getCampaignId(String propName) {
        campaignId = "http://" + url + "/#/campaign/get_campaigns__campaignId_" + propName;
            return campaignId;
    }
    public String getUnitId(String propName, int campaignId ) {
        return unitId;
    }

    //Create helpers to put together String for get requests


    //MULTIPLE-USE OUTPUT FIELDS
    public Category createId() {
        return createCategory()
                .name("ID")
                .label("ID")
                .definition(SWEHelper.getPropertyUri("id"))
                .dataType(DataType.INT)
                .build();
    }
    public Time createPrecisionTimeStamp() {
        return createTime()
                .asSamplingTimeIsoUTC()
                .name("Time")
                .description("...")
                .build();
    }
    public Category createCampaignId(){
        return createCategory()
                .name("CampaignId")
                .label("Campaign ID")
                .definition(SWEHelper.getPropertyUri("campaign_id"))
                .dataType(DataType.INT)
                .build();
    }
    public Category createUnitId(){
        return createCategory()
                .name("UnitId")
                .label("Unit ID")
                .definition(SWEHelper.getPropertyUri("unit_id"))
                .dataType(DataType.INT)
                .build();
    }

    public Time createGeneratedTimestamp(){
        return createTime()
                .asPhenomenonTimeIsoUTC()
                .name("GeneratedTimestamp")
                .label("Generated Timestamp")
                .definition(SWEHelper.getPropertyUri("generated_timestamp"))
                .dataType(DataType.ASCII_STRING)
                .description("...")
                .build();
    }
    public Time createReceivedTimestamp(){
        return createTime()
                .asPhenomenonTimeIsoUTC()
                .name("ReceivedTimestamp")
                .label("Received Timestamp")
                .definition(SWEHelper.getPropertyUri("received_timestamp"))
                .dataType(DataType.ASCII_STRING)
                .description("...")
                .build();
    }

    // POSITION OUTPUT FIELDS
    //Check if this is same as heading
    public Quantity createCourse(){
        return createQuantity()
                .name("Course")
                .label("Course")
                .definition(SWEHelper.getPropertyUri("course"))
                .dataType(DataType.INT)
                .build();
    }
    public Quantity createSpeed(){
        return createQuantity()
                .name("Speed")
                .label("Speed")
                .definition(SWEHelper.getPropertyUri("speed"))
                .dataType(DataType.INT)
                .build();
    }
    public Category createChannel(){
        return createCategory()
                .name("Channel")
                .label("Channel")
                .definition(SWEHelper.getPropertyUri("channel"))
                .dataType(DataType.UTF_STRING)
                .build();
    }

    // EVENT OUTPUT FIELDS
    public Boolean createAlarm() {
        return createBoolean()
                .name("Alarm")
                .label("Alarm")
                .definition(SWEHelper.getPropertyUri("alarm"))
                .dataType(DataType.BOOLEAN)
                .build();
    }
    public Category createEventType(){
        return createCategory()
                .name("EventType")
                .label("Event Type")
                .definition(SWEHelper.getPropertyUri("event_type"))
//                .addAllowedValues()
                .dataType(DataType.ASCII_STRING)
                .build();
    }
//    public DataArray createMsgData(){
//        return createArray()
//                .name("MessageData")
//                .label("Message Data")
//                .definition(SWEHelper.getPropertyUri("msg_data"))
//                .build();
//    }
    public Boolean createNotificationSent() {
        return createBoolean()
                .name("NotificationSent")
                .label("Notification Sent")
                .definition(SWEHelper.getPropertyUri("notification_sent"))
                .dataType(DataType.BOOLEAN)
                .build();
    }

    //CAMPAIGN FIELDS
    public Boolean createEnabled(){
        return createBoolean()
                .name("Enabled")
                .label("Enabled")
                .definition(SWEHelper.getPropertyUri("enabled"))
                .dataType(DataType.BOOLEAN)
                .build();
    }
    public Boolean createArmed(){
        return createBoolean()
                .name("Armed")
                .label("Armed")
                .definition(SWEHelper.getPropertyUri("armed"))
                .dataType(DataType.BOOLEAN)
                .build();
    }
    public Text createVehicle(){
        return createText()
                .name("Vehicle")
                .label("Vehicle")
                .definition(SWEHelper.getPropertyUri("vehicle"))
                .dataType(DataType.ASCII_STRING)
                .build();
    }
    public Text createCargo(){
        return createText()
                .name("Cargo")
                .label("Cargo")
                .definition(SWEHelper.getPropertyUri("cargo"))
                .dataType(DataType.ASCII_STRING)
                .build();
    }
    public Text createLastActivity(){
        return createText()
                .name("LastActivity")
                .label("Last Activity")
                .definition(SWEHelper.getPropertyUri("last_activity"))
                .dataType(DataType.ASCII_STRING)
                .build();
    }
    public Boolean createDeleted(){
        return createBoolean()
                .name("Deleted")
                .label("Deleted")
                .definition(SWEHelper.getPropertyUri("deleted"))
                .dataType(DataType.BOOLEAN)
                .build();
    }

    //UNIT FIELDS
    public Text createUnitSerial(){
        return createText()
                .name("UnitSerial")
                .label("Unit Serial")
                .definition(SWEHelper.getPropertyUri("unit_serial"))
                .dataType(DataType.ASCII_STRING)
                .build();
    }
    public Text createUnitName(){
        return createText()
                .name("Name")
                .label("Name")
                .definition(SWEHelper.getPropertyUri("name"))
                .dataType(DataType.ASCII_STRING)
                .build();
    }
    public Text createIridiumIMEI(){
        return createText()
                .name("IridiumIMEI")
                .label("Iridium IMEI")
                .definition(SWEHelper.getPropertyUri("iridium_imei"))
                .dataType(DataType.ASCII_STRING)
                .build();
    }
    //TODO
    // Check timestamp
    public Time createUnitTimestamp(){
        return createTime()
                .name("UnitTimestamp")
                .label("Unit Timestamp")
                .definition(SWEHelper.getPropertyUri("timestamp"))
                .dataType(DataType.ASCII_STRING)
                .build();
    }
    public Text createCampaignName(){
        return createText()
                .name("CampaignName")
                .label("Campaign Name")
                .definition(SWEHelper.getPropertyUri("campaign_name"))
                .dataType(DataType.ASCII_STRING)
                .build();
    }
}
