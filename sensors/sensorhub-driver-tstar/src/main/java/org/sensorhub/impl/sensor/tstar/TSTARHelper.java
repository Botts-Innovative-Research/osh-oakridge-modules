package org.sensorhub.impl.sensor.tstar;

import net.opengis.swe.v20.*;
import net.opengis.swe.v20.Boolean;
import org.sensorhub.api.data.ObsData;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import javax.swing.text.StyledEditorKit;
import javax.xml.crypto.Data;
import java.net.URI;

public class TSTARHelper extends GeoPosHelper {

    String url;
    String campaignId;
    String unitId;

    public String getCampaignId(String propName) {
        campaignId = "http://" + url + "/#/campaign/get_campaigns__campaignId_" + propName;
        return campaignId;
    }

    public String getUnitId(String propName, int campaignId) {
        return unitId;
    }

    //Create helpers to put together String for get requests


//_____________________________________________________________________________
//    USER INFO FIELDS
//_____________________________________________________________________________

    public Category createUserName() {
        return createCategory()
                .name("userName")
                .label("User Name")
                .definition(SWEHelper.getPropertyUri("user_name"))
                .build();
    }

    public Category createUserId() {
        return createCategory()
                .name("userId")
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
                .name("timestamp")
                .label("Timestamp")
                .definition(SWEHelper.getPropertyUri("timestamp"))
                .optional(true)
                .build();
    }

    public Time createGeneratedTimestamp() {
        return createTime()
                .asSamplingTimeIsoGPS()
                .name("generatedTimestamp")
                .label("Generated Timestamp")
                .definition(SWEHelper.getPropertyUri("generated_timestamp"))
                .description("...")
                .build();
    }

    public Time createReceivedTimestamp() {
        return createTime()
                .asSamplingTimeIsoUTC()
                .name("receivedTimestamp")
                .label("Received Timestamp")
                .definition(SWEHelper.getPropertyUri("received_timestamp"))
                .description("...")
                .build();
    }

//_____________________________________________________________________________
//    POSITION OUTPUT FIELDS
//_____________________________________________________________________________

    public Category createPositionLogId() {
        return createCategory()
                .name("positionLogId")
                .label("Position Log ID")
                .definition(SWEHelper.getPropertyUri("position_log_id"))
                .build();
    }

    public Category createPosition() {
        return createCategory()
                .name("position")
                .label("Position")
                .definition(SWEHelper.getPropertyUri("position"))
                .build();
    }

    //Check if this is same as heading
    public Quantity createCourse() {
        return createQuantity()
                .name("course")
                .label("Course")
                .definition(SWEHelper.getPropertyUri("course"))
                .dataType(DataType.INT)
                .build();
    }

    public Quantity createSpeed() {
        return createQuantity()
                .name("speed")
                .label("Speed")
                .definition(SWEHelper.getPropertyUri("speed"))
                .dataType(DataType.INT)
                .build();
    }

    public Text createChannel() {
        return createText()
                .name("channel")
                .label("Channel")
                .definition(SWEHelper.getPropertyUri("channel"))
                .build();
    }

//_____________________________________________________________________________
//    EVENT OUTPUT FIELDS
//_____________________________________________________________________________

    public Category createEventId() {
        return createCategory()
                .name("eventId")
                .label("Event ID")
                .definition(SWEHelper.getPropertyUri("event_id"))
                .build();
    }

    public Category createEventType() {
        return createCategory()
                .name("eventType")
                .label("Event Type")
                .definition(SWEHelper.getPropertyUri("event_type"))
                .build();
    }

    public Boolean createAlarm() {
        return createBoolean()
                .name("alarm")
                .label("Alarm")
                .definition(SWEHelper.getPropertyUri("alarm"))
                .build();
    }

    public DataComponent createAcknowledgedBy() {
        return createRecord()
                .name("acknowledgedBy")
                .label("Acknowledged By")
                .definition(SWEHelper.getPropertyUri("acknowledged_by"))
                .addField("userId", createCategory()
                        .label("User ID")
                        .definition(SWEHelper.getPropertyUri("ack_by_user_id"))
                        .build())
                .addField("name", createText()
                        .label("Name")
                        .definition(SWEHelper.getPropertyUri("ack_by_name"))
                        .build())
                .build();
    }

    public Time createAckTimestamp() {
        return createTime()
                .asPhenomenonTimeIsoUTC()
                .name("ackTimestamp")
                .label("Ack Timestamp")
                .definition(SWEHelper.getPropertyUri("ack_timestamp"))
                .optional(true)
                .addNilValue(0, "https://www.opengis.net/def/nil")
                .build();
    }

    public DataComponent createMsgData() {
        return createRecord()
                .name("msgData")
                .label("Msg Data")
                .definition(SWEHelper.getPropertyUri("msg_data"))
                .addField("source_id", createCategory()
                        .name("sourceID")
                        .label("Source ID")
                        .definition(SWEHelper.getPropertyUri("source_id"))
                        .build())
                .addField("unit_name", createText()
                        .name("unitName")
                        .label("Unit Name")
                        .definition(SWEHelper.getPropertyUri("unit_name"))
                        .build())
                .addField("sensor_name", createText()
                        .name("sensorName")
                        .label("Sensor Name")
                        .definition(SWEHelper.getPropertyUri("sensor_name"))
                        .build())
                .build();
    }

    public Boolean createNotificationSent() {
        return createBoolean()
                .name("notificationSent")
                .label("Notification Sent")
                .definition(SWEHelper.getPropertyUri("notification_sent"))
                .build();
    }

//_____________________________________________________________________________
//    CAMPAIGN FIELDS
//_____________________________________________________________________________

    public Category createCampaignId() {
        return createCategory()
                .name("campaignId")
                .label("Campaign ID")
                .definition(SWEHelper.getPropertyUri("campaign_id"))
                .build();
    }

    public Text createCampaignName() {
        return createText()
                .name("campaignName")
                .label("Campaign Name")
                .definition(SWEHelper.getPropertyUri("campaign_name"))
                .build();
    }

    public Time createLastActivity() {
        return createTime()
                .asSamplingTimeIsoUTC()
                .name("lastActivity")
                .label("Last Activity")
                .definition(SWEHelper.getPropertyUri("last_activity"))
                .build();
    }

    public Quantity createTotalAlerts() {
        return createQuantity()
                .name("totalAlerts")
                .label("Total Alerts")
                .definition(SWEHelper.getPropertyUri("total_alerts"))
                .build();
    }

    public Quantity createUnacknowledgedAlerts() {
        return createQuantity()
                .name("unacknowledgedAlerts")
                .label("Unacknowledged Alerts")
                .definition(SWEHelper.getPropertyUri("unacknowledged_alerts"))
                .build();
    }
    public Quantity createUnitMessageCount() {
        return createQuantity()
                .name("unitMessageCount")
                .label("Unit Message Count")
                .definition(SWEHelper.getPropertyUri("unit_message_count"))
                .build();
    }
    public Boolean createEnabled() {
        return createBoolean()
                .name("enabled")
                .label("Enabled")
                .definition(SWEHelper.getPropertyUri("enabled"))
                .dataType(DataType.BOOLEAN)
                .build();
    }

    public Boolean createArmed() {
        return createBoolean()
                .name("armed")
                .label("Armed")
                .definition(SWEHelper.getPropertyUri("armed"))
                .dataType(DataType.BOOLEAN)
                .build();
    }

    public Boolean createArchived() {
        return createBoolean()
                .name("archived")
                .label("Archived")
                .definition(SWEHelper.getPropertyUri("archived"))
                .build();
    }

    public Boolean createDeleted() {
        return createBoolean()
                .name("deleted")
                .label("Deleted")
                .definition(SWEHelper.getPropertyUri("deleted"))
                .dataType(DataType.BOOLEAN)
                .build();
    }

    public Text createVehicle() {
        return createText()
                .name("vehicle")
                .label("Vehicle")
                .definition(SWEHelper.getPropertyUri("vehicle"))
                .dataType(DataType.ASCII_STRING)
                .build();
    }

    public Text createCargo() {
        return createText()
                .name("cargo")
                .label("Cargo")
                .definition(SWEHelper.getPropertyUri("cargo"))
                .build();
    }

    public Category createWatchers() {
        return createCategory()
                .name("watchers")
                .label("Watchers")
                .definition(SWEHelper.getPropertyUri("watchers"))
                .build();
    }

    public Category createUsers() {
        return createCategory()
                .name("users")
                .label("Users")
                .definition(SWEHelper.getPropertyUri("users"))
                .build();
    }

    public Category createFences() {
        return createCategory()
                .name("fences")
                .label("Fences")
                .definition(SWEHelper.getPropertyUri("fences"))
                .build();
    }

//_____________________________________________________________________________
//    UNIT LOG FIELDS
//_____________________________________________________________________________

    public Category createUnitLogId() {
        return createCategory()
                .name("unitLogId")
                .label("Unit Log ID")
                .definition(SWEHelper.getPropertyUri("unit_log_id"))
                .build();
    }

    public Category createLevel() {
        return createCategory()
                .name("level")
                .label("Level")
                .definition(SWEHelper.getPropertyUri("level"))
                .build();
    }

    public Category createUnitLogMsg() {
        return createCategory()
                .name("unitLogMessage")
                .label("Unit Log Message")
                .definition(SWEHelper.getPropertyUri("unit_log_message"))
                .build();
    }

//_____________________________________________________________________________
//    UNIT FIELDS
//_____________________________________________________________________________

    public Category createUnitId() {
        return createCategory()
                .name("unitId")
                .label("Unit ID")
                .definition(SWEHelper.getPropertyUri("unit_id"))
                .build();
    }
    public Text createUnitSerial() {
        return createText()
                .name("unitSerial")
                .label("Unit Serial")
                .definition(SWEHelper.getPropertyUri("unit_serial"))
                .build();
    }
    public Text createUnitName() {
        return createText()
                .name("unitName")
                .label("Unit Name")
                .definition(SWEHelper.getPropertyUri("unit_name"))
                .build();
    }
    public Text createIridiumIMEI() {
        return createText()
                .name("iridiumIMEI")
                .label("Iridium IMEI")
                .definition(SWEHelper.getPropertyUri("iridium_imei"))
                .build();
    }
    public Time createPositionTimestamp(){
        return createTime()
                .asSamplingTimeIsoUTC()
                .name("positionTimestamp")
                .label("Position Timestamp")
                .definition(SWEHelper.getPropertyUri("position_timestamp"))
                .build();
    }

    public DataRecord createUnitCheckin() {
        return createRecord()
                .addField("CheckinScheduleSec", createCategory()
                        .label("Checkin schedule (Seconds)")
                        .build())
                .addField("normCheckinSec", createCategory()
                        .label("Norm Checkin (Seconds)")
                        .build())
                .addField("extCheckinSec", createCategory()
                        .label("Ext Checkin (Seconds)")
                        .build())
                .build();
    }
    public DataRecord createUnitStatus() {
        return createRecord()
                .addField("powerMode", createText()
                        .label("Power Mode")
                        .build())
                .addField("pendingPowerMode", createCategory()
                        .label("Pending Power Mode")
                        .build())
                .addField("shorePowerLatch", createBoolean()
                        .label("Shore Power Latch")
                        .build())
                .addField("armState", createText()
                        .label("Arm State")
                        .build())
                .addField("pendingArmState", createCategory()
                        .label("Pending Arm State")
                        .build())
                .build();
    }

    public Boolean createLatchPowerModeHigh(){
        return createBoolean()
                .label("Latch Power Mode High")
                .definition(SWEHelper.getPropertyUri("latch_power_mode_high"))
                .build();
    }
    public Text createManagers() {
        return createText()
                .label("Managers")
                .definition(SWEHelper.getPropertyUri("managers"))
                .build();
    }

    public Boolean createHasRecentAlarm(){
        return createBoolean()
                .label("Has Recent Alarm")
                .build();
    }

    public DataComponent createSensorConfigsArray(){
        return createRecord()
                .addField("sensorConfigsCount", createCount()
                        .label("Sensor Configs Count")
                        .id("sensorConfigsCountId"))
                .addField("sensorConfigs", createArray()
                        .label("Sensors Configs")
                        .definition(SWEHelper.getPropertyUri("sensor_configs_array"))
                        .description("Array of sensor configs properties")
                        .withVariableSize("sensorConfigsCountId")
                        .withElement("sensorConfigsValues", createRecord()
                                .addField("configDelete", createBoolean()
                                        .label("Delete")
                                        .definition(SWEHelper.getPropertyUri("config_delete")))
                                .addField("configMonitor", createBoolean()
                                        .label("Monitor")
                                        .definition(SWEHelper.getPropertyUri("config_monitor")))
                                .addField("configNodeId", createCategory()
                                        .label("Node ID")
                                        .definition(SWEHelper.getPropertyUri("config_node_id")))
                                .addField("configLuxSensitivity", createCategory()
                                        .label("Lux Sensitivity")
                                        .definition(SWEHelper.getPropertyUri("configLuxSensitivity")))
                                .addField("configMotionSensitivity", createCategory()
                                        .label("Motion Sensitivity")
                                        .definition(SWEHelper.getPropertyUri("config_motion_sensitivity")))))
                .build();
    }
    public Text createPendingUiData() {
        return createText()
                .label("Pending UI Data")
                .definition(SWEHelper.getPropertyUri("pending_ui_data"))
                .build();
    }
    public Text createPendingSensorConfigs() {
        return createText()
                .label("Pending Sensor Configs")
                .definition(SWEHelper.getPropertyUri("pending_sensor_configs"))
                .build();
    }

    public DataComponent createUnitProvisioning(){
        return createRecord()
                .label("Provisioning")
                .definition(SWEHelper.getPropertyUri("unit_provisioning"))
                .addField("provisioningId", createCategory()
                        .label("ID")
                        .definition(SWEHelper.getPropertyUri("provisioning_id")))
                .addField("generatedTimestamp", createGeneratedTimestamp())
                .addField("claimedTimestamp", createTime()
                        .asForecastTimeIsoUTC()
                        .label("Claimed Timestamp")
                        .definition(SWEHelper.getPropertyUri("claimed_timestamp")))
                .addField("unitCounter", createCount()
                        .label("Unit Counter")
                        .definition(SWEHelper.getPropertyUri("unit_counter")))
                .addField("serverCounter", createCount()
                        .label("Server Counter")
                        .definition(SWEHelper.getPropertyUri("server_counter")))
                .build();
    }

    public DataComponent createLastCheckin(){
        return createRecord()
                .label("Last Checkin")
                .definition(SWEHelper.getPropertyUri("last_checkin"))
                .addField("lastCheckinId", createCategory()
                        .label("ID")
                        .definition(SWEHelper.getPropertyUri("last_checkin_id")))
                .addField("lastCheckinMeta", createMeta())
                .addField("lastCheckinChannel", createChannel())
                .addField("lastCheckinMessage", createUnitMessage())
                .addField("lastCheckinTimestamp", createTimestamp())
                .build();
    }

    public DataComponent createLastFullCheckin(){
        return createRecord()
                .label("Last Full Checkin")
                .definition(SWEHelper.getPropertyUri("last_full_checkin"))
                .addField("lastFullCheckinId", createCategory()
                        .label("ID")
                        .definition(SWEHelper.getPropertyUri("last_full_checkin_id")))
                .addField("lastFullCheckinMeta", createMeta())
                .addField("lastFullCheckinChannel", createChannel())
                .addField("lastFullCheckinMessage", createUnitMessage())
                .addField("lastFullCheckinTimestamp", createTimestamp())
                .build();
    }

    public DataComponent createUnitMessage() {
        return createRecord()
                .label("Message")
                .definition(SWEHelper.getPropertyUri("message_log_message"))
                .description("Message received in message log containing high & low side data")
                .addField("low", createLow())
                .addField("high", createHigh())
                .build();
    }
    public DataComponent createLastCommand() {
        return createRecord()
                .label("Last Command")
                .definition(SWEHelper.getPropertyUri("last_command"))
                .addField("lastCommandId", createQuantity()
                        .label("ID"))
                .addField("lastCommandMeta", createMeta())
                .addField("lastCommandChannel", createChannel())
                .addField("message", createRecord()
                        .label("Message")
                        .addField("eventLogAck", createQuantity()
                                .label("Event Log Ack")))
                .addField("timestamp", createTimestamp())
                .build();
    }

    public DataComponent createEventTable() {
        return createRecord()
                .label("Event Table")
                .definition(SWEHelper.getPropertyUri("event_table"))
                .addField("unitEvents", createUnitEvents())
//                .addField("sensorEvents", createArray()
//                        .withFixedSize(0))
                .build();
    }
    public DataComponent createUnitEvents(){
        return createRecord()
                .label("Unit Events")
                .definition(SWEHelper.getPropertyUri("unit_events"))
                .addField("eventGpsFailure", createBoolean()
                        .label("EVENT GPS FAILURE"))
                .addField("eventUnitMissing", createBoolean()
                        .label("EVENT UNIT MISSING"))
                .addField("eventImproperDisarm", createBoolean()
                        .label("EVENT IMPROPER DISARM"))
                .addField("eventMainboardFailure", createBoolean()
                        .label("EVENT MAINBOARD FAILURE"))
                .addField("eventArmingNoCampaign", createBoolean()
                        .label("EVENT ARMING NO CAMPAIGN"))
                .build();
    }

    //TODO
    // Check timestamp
    public Time createUnitTimestamp() {
        return createTime()
                .name("unitTimestamp")
                .label("Unit Timestamp")
                .definition(SWEHelper.getPropertyUri("timestamp"))
                .build();
    }

//_____________________________________________________________________________
//    AUDIT LOG FIELDS
//_____________________________________________________________________________

    public Category createAuditLogId() {
        return createCategory()
                .name("auditLogId")
                .label("Audit Log ID")
                .definition(SWEHelper.getPropertyUri("audit_log_id"))
                .build();
    }

    public Category createAction() {
        return createCategory()
                .name("action")
                .label("Action")
                .definition(SWEHelper.getPropertyUri("action"))
                .build();
    }

    public Category createSourceIp() {
        return createCategory()
                .name("sourceIp")
                .label("Source IP")
                .definition(SWEHelper.getPropertyUri("source_ip"))
                .build();
    }

    public Category createTargetTable() {
        return createCategory()
                .name("targetTable")
                .label("Target Table")
                .definition(SWEHelper.getPropertyUri("target_table"))
                .build();
    }

    public Category createTargetId() {
        return createCategory()
                .name("targetId")
                .label("Target ID")
                .definition(SWEHelper.getPropertyUri("target_id"))
                .build();
    }

    public Category createTargetName() {
        return createCategory()
                .name("targetName")
                .label("Target Name")
                .definition(SWEHelper.getPropertyUri("target_name"))
                .build();
    }

    public DataComponent createAuditLogData() {
        return createRecord()
                .name("auditLogData")
                .label("Audit Log Data")
                .definition(SWEHelper.getPropertyUri("audit_log_data"))
                .addField("id", createText()
                        .label("ID")
                        .definition(SWEHelper.getPropertyUri("audit_log_data_id"))
                        .build())
                .addField("campaignId", createCategory()
                        .label("Campaign ID")
                        .definition(SWEHelper.getPropertyUri("audit_log_data_campaign_id"))
                        .build())
                .build();
    }


//_____________________________________________________________________________
//    MESSAGE LOG OUTPUTS
//_____________________________________________________________________________

    public Category createMessageLogId() {
        return createCategory()
                .name("messageLogId")
                .label("Message Log ID")
                .definition(SWEHelper.getPropertyUri("message_log_id"))
                .build();
    }

    public Time createDelivered() {
        return createTime()
                .asPhenomenonTimeIsoUTC()
                .name("delivered")
                .label("Delivered")
                .definition(SWEHelper.getPropertyUri("delivered"))
//                .optional(true)
                .addNilValue(0, "https://www.opengis.net/def/nil")
                .build();
    }

    public Text createDirection() {
        return createText()
                .name("direction")
                .label("Direction")
                .definition(SWEHelper.getPropertyUri("direction"))
                .build();
    }

    public DataComponent createMeta() {
        return createRecord()
                .name("meta")
                .label("Meta")
                .definition(SWEHelper.getPropertyUri("meta"))
                .addField("keyId", createCategory()
                        .label("Key ID")
                        .definition(SWEHelper.getPropertyUri("meta_key_id"))
                        .build())
                .addField("channelInfo", createRecord()
                        .label("Channel Info")
                        .definition(SWEHelper.getPropertyUri("meta_channel_info"))
                        .addField("remote", createText()
                                .label("Remote")
                                .definition(SWEHelper.getPropertyUri("meta_remote"))
                                .build())
                        .build())
                .addField("nonceCounter", createCategory()
                        .label("Nonce Counter")
                        .definition(SWEHelper.getPropertyUri("meta_nonce_counter"))
                        .build())
                .build();
    }

    public DataComponent createRawPacket() {
        return createRecord()
                .label("Raw Packet Data")
                .addField("type", createText()
                        .label("Type")
                        .definition(SWEHelper.getPropertyUri("type")))
                .addField("arrayCount", createCount()
                        .id("arrayCountID"))
                .addField("arrayData", createArray()
                        .label("Raw Packet")
                        .definition(SWEHelper.getPropertyUri("raw_packet"))
                        .description("Raw packet data")
                        .withVariableSize("arrayCountID")
                        .withElement("arrayElement", createQuantity()
//                                .label("Values")
                                .definition(SWEHelper.getPropertyUri("raw_packet_values"))))
                .build();
    }

    public DataComponent createMsgLogMessage() {
        return createRecord()
                .label("Message")
                .definition(SWEHelper.getPropertyUri("message_log_message"))
                .description("Message received in message log containing high & low side data")
                .addField("low", createLow())
                .addField("high", createHigh())
                .addField("timestamp", createTimestamp())
                .build();
    }

    public DataComponent createLow() {
        return createRecord()
                .name("low")
                .label("Low")
                .definition(SWEHelper.getPropertyUri("low"))
                .description("Data contained in the low side message including gps & event log")
                .addField("gps", createGps())
                .addField("armState", createText().label("Arm State").build())
                .addField("eventLog", createEventLog())
                .addField("timestamp", createTimestamp())
                .addField("powerMode", createText().label("Power Mode").build())
                .addField("batteryVoltage", createQuantity().label("Battery Voltage").build())
                .addField("eventLogLength", createQuantity().label("Event Log Length").build())
                .addField("positionLogLength", createQuantity().label("Position Log Length").build())
                .addField("checkinScheduleSec", createQuantity().label("Checkin Schdule Sec").build())
                .build();
    }

    public DataComponent createEventLog() {
        return createRecord()
                .label("Event Log")
                .description("")
                .addField("fixType", createText().label("Fix Type"))
                .addField("location", createLocationVectorLatLon())
                .addField("sourceId", createCategory().label("Source ID"))
                .addField("timestamp", createTimestamp())
                .addField("eventType", createText().label("Event Type"))
                .build();
    }

    public DataComponent createGps() {
        return createRecord()
                .name("GPS")
                .label("Gps")
                .definition(SWEHelper.getPropertyUri("gps"))
                .addField("hdop", createQuantity()
                        .label("hdop")
                        .definition(SWEHelper.getPropertyUri("hdop")))
                .addField("pdop", createQuantity().label("pdop")
                        .definition(SWEHelper.getPropertyUri("pdop")))
                .addField("vdop", createQuantity().label("vdop")
                        .definition(SWEHelper.getPropertyUri("vdop")))
                .addField("speed", createQuantity().label("Speed")
                        .definition(SWEHelper.getPropertyUri("speed")))
                .addField("course", createQuantity().label("Course")
                        .definition(SWEHelper.getPropertyUri("course")))
                .addField("altitude", createQuantity().label("Altitude")
                        .definition(SWEHelper.getPropertyUri("altitude")))
                .addField("fix_type", createText().label("Fix Type")
                        .definition(SWEHelper.getPropertyUri("fix_type")))
                .addField("location", createLocationVectorLatLon()
                        .definition(SWEHelper.getPropertyUri("location")))
                .addField("timestamp", createTimestamp())
//                        .label("Timestamp"))
                .addField("num_satellites", createQuantity()
                        .definition(SWEHelper.getPropertyUri("num_satellites")))
                .build();
    }

    public DataComponent createHigh() {
        return createRecord()
                .name("high")
                .label("High")
                .definition(SWEHelper.getPropertyUri("high"))
                .addField("uiData", createUiData())
                .addField("provisioning", createProvisioning())
                .addField("mainBoardData", createMainBoardData())
                .addField("modemBoardData", createModemBoardData())
                .addField("mainBoardConfig", createMainBoardConfig())
                .addField("modemBoardConfig", createModemBoardConfig())
                .build();
    }
    public DataComponent createUiData() {
        return createRecord()
                .name("uiData")
                .label("UI Data")
                .definition(SWEHelper.getPropertyUri("ui_data"))
                .addField("sensors", createSensorArray())
                .addField("campaignName", createCampaignName())
                .addField("containerImage", createContainerImage())
                .build();
    }
    public DataComponent createSensorArray() {
        return createRecord()
                .addField("sensorArrayCount", createCount()
                        .label("Sensor Array Count")
                        .id("sensorArrayCountId"))
                .addField("sensorArray", createArray()
                        .label("Sensors")
                        .definition(SWEHelper.getPropertyUri("sensor_array"))
                        .description("Array of sensor properties")
                        .withVariableSize("sensorArrayCountId")
                        .withElement("sensorValues", createRecord()
                                .addField("sensorName", createText()
                                        .label("Name")
                                        .definition(SWEHelper.getPropertyUri("sensor_name")))
                                .addField("nodeId", createNodeId())
                                .addField("positionX", createCategory()
                                        .label("Position X")
                                        .definition(SWEHelper.getPropertyUri("sensor_position_x")))
                                .addField("positionY", createCategory()
                                        .label("Position Y")
                                        .definition(SWEHelper.getPropertyUri("sensor_position_y")))))
                .build();
    }

    public Category createNodeId(){
        return createCategory()
                .name("nodeId")
                .label("Node ID")
                .definition(SWEHelper.getPropertyUri("sensor_node_id"))
                .build();
    }

    public Text createContainerImage() {
        return createText()
                .name("containerImage")
                .label("Container Image")
                .definition(SWEHelper.getPropertyUri("container_image"))
                .description("Description of container type e.g. Box-Truck-01 ")
                .build();
    }

    public DataComponent createProvisioning() {
        return createRecord()
                .name("provisioning")
                .label("Provisioning")
                .definition(SWEHelper.getPropertyUri("provisioning"))
                .addField("keyID", createCategory().label("Key ID").build())
                .addField("unitId", createUnitId())
                .addField("unitName", createUnitName())
                .addField("encryptionKey", createText().label("Encryption Key").build())
                .build();
    }

    public Text createHardwareVersion(){
        return createText()
                .label("Harware Version")
                .definition(SWEHelper.getPropertyUri("hardware_version"))
                .build();
    }
    public Text createSoftwareVersion(){
        return createText()
                .label("Software Version")
                .definition(SWEHelper.getPropertyUri("software_version"))
                .build();
    }

    public Text createSerialNumber(){
        return createText()
                .label("Serial Number")
                .definition(SWEHelper.getPropertyUri("serial_number"))
                .build();
    }
    public DataComponent createMainBoardData() {
        return createRecord()
                .name("mainBoardData")
                .label("Main Board Data")
                .definition(SWEHelper.getPropertyUri("main_board_data"))
                .addField("gps", createGps())
                .addField("humidity", createCategory().label("Humidity").build())
                .addField("pressure", createCategory().label("Pressure").build())
                .addField("temperature", createCategory().label("Temperature").build())
                .addField("powerSupply", createPowerSupply())
                .addField("serialNumber", createSerialNumber())
                .addField("zWaveSensors", createZWaveSensors())
                .addField("hardwareVersion", createHardwareVersion())
                .addField("softwareVersion", createSoftwareVersion())
                .addField("eventLogOverruns", createQuantity()
                        .label("Event Log Overruns")
                        .build())
                .addField("countdownToArming", createQuantity()
                        .label("Countdown to Arming")
                        .build())
                .addField("powerModeTimestamp", createQuantity()
                        .label("Power Mode Timestamp")
                        .build())
                .addField("positionLogOverruns", createQuantity()
                        .label("Position Log Overruns")
                        .build())
                .addField("countdownToModemOff", createQuantity()
                        .label("Countdown to Modem Off")
                        .build())
                .build();
    }

    public Category createPowerSupplyCat(String label, String propName){
        return createCategory()
                .label(label)
                .definition(SWEHelper.getPropertyUri(propName))
                .build();
    }
    public Boolean createPowerSupplyBool(String label, String propName){
        return createBoolean()
                .label(label)
                .definition(SWEHelper.getPropertyUri(propName))
                .build();
    }
    public DataComponent createPowerSupply() {
        return createRecord()
                .name("powerSupply")
                .label("Power Supply")
                .definition(SWEHelper.getPropertyUri("power_supply"))
                .addField("iin", createPowerSupplyCat("iin", "iin"))
                .addField("ichg", createPowerSupplyCat("ichg","ichg"))
                .addField("psys", createPowerSupplyCat("psys","psys"))
                .addField("vbat", createPowerSupplyCat("vbat", "vbat"))
                .addField("vbus", createPowerSupplyCat("vbus", "vbus"))
                .addField("vsys", createPowerSupplyCat("vsys", "vsys"))
                .addField("cmpin", createPowerSupplyCat("cmpin", "cmpin"))
                .addField("idchg", createPowerSupplyCat("idchg", "idchg"))
                .addField("sysovp", createPowerSupplyCat("sysovp", "sysovp"))
                .addField("in_vindpm", createPowerSupplyBool("in_vindpm", "in_vindpm"))
                .addField("timestamp", createTimestamp())
                .addField("acoc_fault", createPowerSupplyBool("acoc_fault", "acoc_fault"))
                .addField("acov_fault", createPowerSupplyBool("acov_fault", "acov_fault"))
                .addField("batoc_fault", createPowerSupplyBool("batoc_fault","batoc_fault"))
                .addField("pre_charging", createPowerSupplyBool("Pre Charging","pre_charging"))
                .addField("fast_charging", createPowerSupplyBool("Fast Charging","fast_charging"))
                .addField("fault_latch_off", createPowerSupplyBool("Fault Latch Off","fault_latch_off"))
                .addField("statusRegister", createPowerSupplyCat("Status Register", "status_register"))
                .addField("prochotRegister", createPowerSupplyCat("Prochot Register","prochot_register"))
                .addField("externalConnected", createPowerSupplyBool("External Connected","external_connected"))
                .build();

    }

    public DataComponent createZWaveSensors() {
        return createRecord()
                .name("zWaveSensors")
                .label("ZWave Sensors")
                .definition(SWEHelper.getPropertyUri("zwave_sensors"))
                .addField("zWaveSensorCount", createCount()
                        .label("Sensor Count")
                        .id("zWaveSensorCount"))
                .addField("zWaveSensorArray", createArray()
                        .label("Zwave Sensor Array")
                        .definition(SWEHelper.getPropertyUri("zwave_sensor_array"))
                        .withVariableSize("zWaveSensorCount")
                        .withElement("zWaveSensorArrayComponents", createRecord()
                                .addField("rssi", createCategory()
                                        .label("rssi")
                                        .definition(SWEHelper.getPropertyUri("rssi"))
                                        .build())
                                .addField("nodeId", createNodeId())
                                .addField("luxHigh", createBoolean()
                                        .label("Lux High")
                                        .definition(SWEHelper.getPropertyUri("lux"))
                                        .build())
                                .addField("security", createBoolean()
                                        .label("Security")
                                        .build())
                                .addField("batteryLow", createBoolean()
                                        .label("Battery Low")
                                        .build())
                                .addField("sensorType", createCategory()
                                        .label("Sensor Type")
                                        .build())
                                .addField("contactOpen", createBoolean()
                                        .label("Contact Open")
                                        .build())
                                .addField("batteryLevel", createQuantity()
                                        .label("Battery Level")
                                        .definition(SWEHelper.getPropertyUri("battery_level"))
                                        .build())
                                .addField("motionTrigger", createBoolean()
                                        .label("Motion Triggered")
                                        .build())
                                .addField("sensorMissing", createBoolean()
                                        .label("Sensor Missing")
                                        .build())
                                .addField("tamperTrigger", createBoolean()
                                        .label("Tamper Triggered")
                                        .build())
                                .addField("lastReportTimestamp", createTimestamp())
                                .addField("heartbeatIntervalSec", createQuantity()
                                        .label("Heartbeat Interval (Seconds)"))))
                .build();
    }

    public DataComponent createModemBoardData() {
        return createRecord()
                .label("Modem Board Data")
                .definition(SWEHelper.getPropertyUri("modem_board_data"))
                .addField("uptime", createQuantity()
                        .label("Uptime")
                        .definition(SWEHelper.getPropertyUri("uptime")))
                .addField("temperature", createQuantity()
                        .label("Temperature")
                        .definition(SWEHelper.getPropertyUri("temperature")))
                .addField("lastCheckin", createCategory()
                        .label("Last Checkin")
                        .definition(SWEHelper.getPropertyUri("last_checkin")))
                .addField("serialNumber", createSerialNumber())
                .addField("fsHomeCapMb", createQuantity()
                        .label("fs home cap mb")
                        .definition(SWEHelper.getPropertyUri("fs_home_cap_mb")))
                .addField("fsRootCapMb", createQuantity()
                        .label("fs root cap mb")
                        .definition(SWEHelper.getPropertyUri("fs_root_cap_mb")))
                .addField("fsHomeSizeMb", createQuantity()
                        .label("fs home size mb")
                        .definition(SWEHelper.getPropertyUri("fs_home_size_mb")))
                .addField("fsRootSizeMb", createQuantity()
                        .label("fs root size mb")
                        .definition(SWEHelper.getPropertyUri("fs_root_size_mb")))
                .addField("hardwareVersion", createHardwareVersion())
                .addField("softwareVersion", createSoftwareVersion())
                .addField("lastFullCheckin", createQuantity()
                        .label("Last Full Checkin")
                        .definition(SWEHelper.getPropertyUri("last_full_checkin")))
                .build();
    }

    public DataComponent createMainBoardConfig() {
        return createRecord()
                .label("Main Board Config")
                .definition(SWEHelper.getPropertyUri("main_board_config"))
                .addField("zWaveMode", createText()
                        .label("Zwave Mode")
                        .definition(SWEHelper.getPropertyUri("zwave_mode")))
                .addField("armingCountdownSec", createQuantity()
                        .label("Arming Countdown (Sec)")
                        .definition(SWEHelper.getPropertyUri("arming_countdown_sec")))
                .addField("zWaveSensorConfigs", createSensorConfigsArray())
                .build();
    }

    public DataComponent createModemBoardConfig() {
        return createRecord()
                .label("Modem Board Config")
                .definition(SWEHelper.getPropertyUri("modem_board_config"))
                .addField("cellularApn", createText()
                        .label("Cellular APN")
                        .definition(SWEHelper.getPropertyUri("cellular_apn")))
                .addField("tstarServerUrl", createText()
                        .label("TSTAR Server URL")
                        .definition(SWEHelper.getPropertyUri("tstar_server_url")))
                .build();
    }

}

















