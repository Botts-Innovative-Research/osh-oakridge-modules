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


//_____________________________________________________________________________
//    USER INFO FIELDS
//_____________________________________________________________________________

    public Category createUserName(){
        return createCategory()
                .name("UserName")
                .label("User Name")
                .definition(SWEHelper.getPropertyUri("user_name"))
                .build();
    }
    public Category createUserId() {
        return createCategory()
                .name("UserId")
                .label("User ID")
                .definition(SWEHelper.getPropertyUri("user_id"))
                .build();
    }
//_____________________________________________________________________________
//    DATE/TIME FIELDS
//_____________________________________________________________________________

    public Time createTimestamp() {
        return createTime()
                .asPhenomenonTimeIsoUTC()
                .name("Timestamp")
                .label("Timestamp")
                .definition(SWEHelper.getPropertyUri("timestamp"))
                .optional(true)
                .build();
    }
    public Time createGeneratedTimestamp(){
        return createTime()
                .asSamplingTimeIsoGPS()
                .name("GeneratedTimestamp")
                .label("Generated Timestamp")
                .definition(SWEHelper.getPropertyUri("generated_timestamp"))
                .description("...")
                .build();
    }
    public Time createReceivedTimestamp(){
        return createTime()
                .asSamplingTimeIsoUTC()
                .name("ReceivedTimestamp")
                .label("Received Timestamp")
                .definition(SWEHelper.getPropertyUri("received_timestamp"))
                .description("...")
                .build();
    }

//_____________________________________________________________________________
//    POSITION OUTPUT FIELDS
//_____________________________________________________________________________

    public Category createPositionLogId(){
        return createCategory()
                .name("PositionLogId")
                .label("Position Log ID")
                .definition(SWEHelper.getPropertyUri("position_log_id"))
                .build();
    }
    public Category createPosition() {
        return createCategory()
                .name("Position")
                .label("Position")
                .definition(SWEHelper.getPropertyUri("position"))
                .build();
    }
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
    public Text createChannel(){
        return createText()
                .name("Channel")
                .label("Channel")
                .definition(SWEHelper.getPropertyUri("channel"))
                .build();
    }

//_____________________________________________________________________________
//    EVENT OUTPUT FIELDS
//_____________________________________________________________________________

    public Category createEventId(){
        return createCategory()
                .name("EventId")
                .label("Event ID")
                .definition(SWEHelper.getPropertyUri("event_id"))
                .build();
    }
    public Category createEventType(){
        return createCategory()
                .name("EventType")
                .label("Event Type")
                .definition(SWEHelper.getPropertyUri("event_type"))
                .build();
    }
    public Boolean createAlarm() {
        return createBoolean()
                .name("Alarm")
                .label("Alarm")
                .definition(SWEHelper.getPropertyUri("alarm"))
                .build();
    }

    public Category createAcknowledgedBy(){
        return createCategory()
                .name("AcknowledgedBy")
                .label("Acknowledged By")
                .definition(SWEHelper.getPropertyUri("acknowledged_by"))
                .addQuality("user_id", createCategory().build()  )
                .addQuality("name", createText().build())
                .build();
    }
    public Time createAckTimestamp(){
        return createTime()
                .asPhenomenonTimeIsoUTC()
                .name("AckTimestamp")
                .label("Ack Timestamp")
                .definition(SWEHelper.getPropertyUri("ack_timestamp"))
                .optional(true)
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
                .build();
    }

//_____________________________________________________________________________
//    CAMPAIGN FIELDS
//_____________________________________________________________________________

    public Category createCampaignId(){
        return createCategory()
                .name("CampaignId")
                .label("Campaign ID")
                .definition(SWEHelper.getPropertyUri("campaign_id"))
                .build();
    }
    public Text createCampaignName(){
        return createText()
                .name("CampaignName")
                .label("Campaign Name")
                .definition(SWEHelper.getPropertyUri("campaign_name"))
                .build();
    }
    public Time createLastActivity(){
        return createTime()
                .asSamplingTimeIsoUTC()
                .name("LastActivity")
                .label("Last Activity")
                .definition(SWEHelper.getPropertyUri("last_activity"))
                .build();
    }
    public Quantity createTotalAlerts(){
        return createQuantity()
                .name("TotalAlerts")
                .label("Total Alerts")
                .definition(SWEHelper.getPropertyUri("total_alerts"))
                .build();
    }
    public Quantity createUnacknowledgedAlerts(){
        return createQuantity()
                .name("UnacknowledgedAlerts")
                .label("Unacknowledged Alerts")
                .definition(SWEHelper.getPropertyUri("unacknowledged_alerts"))
                .build();
    }
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
    public Boolean createArchived(){
        return createBoolean()
                .name("Archived")
                .label("Archived")
                .definition(SWEHelper.getPropertyUri("archived"))
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
                .build();
    }
    public Category createWatchers() {
        return createCategory()
                .name("Watchers")
                .label("Watchers")
                .definition(SWEHelper.getPropertyUri("watchers"))
                .build();
    }
    public Category createUsers() {
        return createCategory()
                .name("Users")
                .label("Users")
                .definition(SWEHelper.getPropertyUri("users"))
                .build();
    }
    public Category createFences() {
        return createCategory()
                .name("Fences")
                .label("Fences")
                .definition(SWEHelper.getPropertyUri("fences"))
                .build();
    }

//_____________________________________________________________________________
//    UNIT LOG FIELDS
//_____________________________________________________________________________

    public Category createUnitLogId() {
        return createCategory()
                .name("UnitLogId")
                .label("Unit Log ID")
                .definition(SWEHelper.getPropertyUri("unit_log_id"))
                .build();
    }
    public Category createLevel(){
        return createCategory()
                .name("Level")
                .label("Level")
                .definition(SWEHelper.getPropertyUri("level"))
                .build();
    }
    public Category createUnitLogMsg(){
        return createCategory()
                .name("UnitLogMessage")
                .label("Unit Log Messge")
                .definition(SWEHelper.getPropertyUri("unit_log_message"))
                .build();
    }

//_____________________________________________________________________________
//    UNIT FIELDS
//_____________________________________________________________________________

    public Category createUnitId(){
        return createCategory()
                .name("UnitId")
                .label("Unit ID")
                .definition(SWEHelper.getPropertyUri("unit_id"))
                .dataType(DataType.INT)
                .build();
    }
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
    public Quantity createUnitMessageCount() {
        return createQuantity()
                .name("UnitMessageCount")
                .label("Unit Message Count")
                .definition(SWEHelper.getPropertyUri("unit_message_count"))
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

//_____________________________________________________________________________
//    AUDIT LOG FIELDS
//_____________________________________________________________________________

    public Category createAuditLogId() {
        return createCategory()
                .name("AuditLogId")
                .label("Audit Log ID")
                .definition(SWEHelper.getPropertyUri("audit_log_id"))
                .build();
    }
    public Category createAction(){
        return createCategory()
                .name("Action")
                .label("Action")
                .definition(SWEHelper.getPropertyUri("action"))
                .build();
    }
    public Category createSourceIp(){
        return createCategory()
                .name("SourceIp")
                .label("Source IP")
                .definition(SWEHelper.getPropertyUri("source_ip"))
                .build();
    }
    public Category createTargetTable(){
        return createCategory()
                .name("TargetTable")
                .label("Target Table")
                .definition(SWEHelper.getPropertyUri("target_table"))
                .build();
    }
    public Category createTargetId(){
        return createCategory()
                .name("TargetId")
                .label("Target ID")
                .definition(SWEHelper.getPropertyUri("target_id"))
                .build();
    }
    public Category createTargetName() {
        return createCategory()
                .name("TargetName")
                .label("Target Name")
                .definition(SWEHelper.getPropertyUri("target_name"))
                .build();
    }

//_____________________________________________________________________________
//    MESSAGE LOG OUTPUTS
//_____________________________________________________________________________

    public Category createMessageLogId() {
        return createCategory()
                .name("MessageLogId")
                .label("Message Log ID")
                .definition(SWEHelper.getPropertyUri("message_log_id"))
                .build();
    }
    public Time createDelivered() {
        return createTime()
                .asPhenomenonTimeIsoUTC()
                .name("Delivered")
                .label("Delivered")
                .definition(SWEHelper.getPropertyUri("delivered"))
                .optional(true)
                .build();
    }
    public Text createIn(){
        return createText()
                .name("In")
                .label("In")
                .definition(SWEHelper.getPropertyUri("in"))
                .build();
    }

    public DataArray createMeta() {
        return createArray()
                .name("Meta")
                .label("Meta")
                .definition(SWEHelper.getPropertyUri("meta"))
                .withElement("key_id", createCategory().label("Key ID").build())
//                .withElement("channel_info", create)
                .build();
    }
}
