package org.sensorhub.impl.sensor.tstar;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.tstar.responses.Unit;
import org.vast.data.DataArrayImpl;
import org.vast.swe.SWEHelper;

import java.text.SimpleDateFormat;
import java.util.Date;


public class TSTARUnitOutput extends AbstractSensorOutput<TSTARDriver> {
    private static final String SENSOR_OUTPUT_NAME = "Unit Output";
    protected DataRecord dataStruct;
    DataEncoding dataEncoding;
    TSTARHelper tstarHelper = new TSTARHelper();
    Long unitTimestamp;
    Long positionTimestamp;
    Long provisioningClaimedTime;
    Long provisioningGenTime;
    Long lastCheckinTimestamp;
    Long lastFullCheckinTimestamp;
    Long lastCommandTimestamp;

    public TSTARUnitOutput(TSTARDriver parentSensor) {super(SENSOR_OUTPUT_NAME, parentSensor);}

    protected void init() {
        dataStruct = createDataRecord();
        dataEncoding = tstarHelper.newTextEncoding(",", "\n");
    }

    public DataRecord createDataRecord(){
        TSTARHelper tstarHelper = new TSTARHelper();

        return tstarHelper.createRecord()
        // SWE Common data structure
                .name(getName())
                .label(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri("UnitData"))
                .addField("unitId", tstarHelper.createUnitId())
                .addField("unitSerial", tstarHelper.createUnitSerial())
                .addField("unitName", tstarHelper.createUnitName())
                .addField("iridiumImei", tstarHelper.createIridiumIMEI())
                .addField("unitTimestamp", tstarHelper.createTimestamp())
                .addField("unitLocation", tstarHelper.createLocationVectorLatLon())
                .addField("positionTimestamp", tstarHelper.createPositionTimestamp())
                .addField("unitCheckin", tstarHelper.createUnitCheckin())
                .addField("unitStatus", tstarHelper.createUnitStatus())
                .addField("uiData", tstarHelper.createUiData())
                .addField("pendingUiData", tstarHelper.createPendingUiData())
                .addField("sensorConfigsArray", tstarHelper.createSensorConfigsArray())
                .addField("pendingSensorConfigs", tstarHelper.createPendingSensorConfigs())
                .addField("unitProvisioning", tstarHelper.createUnitProvisioning())
                .addField("lastCheckin", tstarHelper.createLastCheckin())
                .addField("lastFullCheckin", tstarHelper.createLastFullCheckin())
                .addField("lastCommand", tstarHelper.createLastCommand())
                .addField("eventTable", tstarHelper.createEventTable())
                .addField("latchPowerModeHigh", tstarHelper.createLatchPowerModeHigh())
                .addField("campaignId", tstarHelper.createCampaignId())
                .addField("campaignName", tstarHelper.createCampaignName())
                .addField("managers", tstarHelper.createManagers())
                .addField("hasRecentAlarm", tstarHelper.createHasRecentAlarm())
                .addField("deleted", tstarHelper.createDeleted())
                .build();

    }

    public void parse(Unit unit) {

        dataStruct = createDataRecord();
        DataBlock dataBlock = dataStruct.createDataBlock();
        dataStruct.setData(dataBlock);

        latestRecordTime = System.currentTimeMillis() / 1000;
        setUnitTime(unit);

        int i = 0;
//_______________________________________________________________________________________________
//        Unit
//_______________________________________________________________________________________________
        dataBlock.setIntValue(i++, unit.id);
        dataBlock.setStringValue(i++, unit.unit_serial);
        dataBlock.setStringValue(i++, unit.name);
        dataBlock.setStringValue(i++, unit.iridium_imei);
        dataBlock.setLongValue(i++, unitTimestamp);
        dataBlock.setDoubleValue(i++, unit.latitude);
        dataBlock.setDoubleValue(i++, unit.longitude);
        dataBlock.setLongValue(i++, unit.position_timestamp);
        dataBlock.setIntValue(i++, unit.checkin_schedule_sec);
        dataBlock.setIntValue(i++, unit.norm_checkin_sec);
        dataBlock.setIntValue(i++, unit.ext_checkin_sec);
        dataBlock.setStringValue(i++, unit.power_mode);
        dataBlock.setStringValue(i++, unit.pending_power_mode);
        dataBlock.setBooleanValue(i++, unit.shore_power_latch);
        dataBlock.setStringValue(i++, unit.arm_state);
        dataBlock.setStringValue(i++, unit.pending_arm_state);
//_______________________________________________________________________________________________
//        Unit{UiData}
//_______________________________________________________________________________________________
        try {
            var sensors = unit.ui_data.sensors;
            dataBlock.setIntValue(i++, sensors.length);
//_______________________________________________________________________________________________
//        Unit{UiData[Sensors{}]}
//_______________________________________________________________________________________________
            var sensorArray =
                    ((DataArrayImpl) dataStruct.getComponent("uiData").getComponent("sensors").getComponent ("sensorArray"));
            sensorArray.updateSize();

            for (int ix = 0; ix < sensors.length; ix++) {
                dataBlock.setStringValue(i++, sensors[ix].name);
                dataBlock.setIntValue(i++, sensors[ix].node_id);
                dataBlock.setIntValue(i++, sensors[ix].position_x);
                dataBlock.setIntValue(i++, sensors[ix].position_y);
            }
        } catch (NullPointerException e) {}
//_______________________________________________________________________________________________

        dataBlock.setStringValue(i++, unit.ui_data.campaign_name);
        dataBlock.setStringValue(i++, unit.ui_data.container_image);
//_______________________________________________________________________________________________

        dataBlock.setStringValue(i++, unit.pending_ui_data);
//_______________________________________________________________________________________________
//        Unit[SensorConfigs{}]
//_______________________________________________________________________________________________
        try {
            var sensorConfigs = unit.sensor_configs;
            dataBlock.setIntValue(i++, sensorConfigs.length);

            var sensorConfigArray =
                    ((DataArrayImpl) dataStruct.getComponent("sensorConfigsArray").getComponent("sensorConfigs"));
            sensorConfigArray.updateSize();

            for (int ixx = 0; ixx < sensorConfigs.length; ixx++) {
                dataBlock.setBooleanValue(i++, sensorConfigs[ixx].delete);
                dataBlock.setBooleanValue(i++, sensorConfigs[ixx].monitor);
                dataBlock.setIntValue(i++, sensorConfigs[ixx].node_id);
                dataBlock.setIntValue(i++, sensorConfigs[ixx].lux_sensitivity);
                dataBlock.setIntValue(i++, sensorConfigs[ixx].motion_sensitivity);
            }
        } catch (NullPointerException e) {}
//_______________________________________________________________________________________________

        dataBlock.setStringValue(i++, unit.pending_sensor_configs);
//_______________________________________________________________________________________________
//      Unit{Provisioning}
//_______________________________________________________________________________________________
        dataBlock.setIntValue(i++, unit.provisioning.id);
        dataBlock.setLongValue(i++, provisioningGenTime);
        dataBlock.setLongValue(i++, provisioningClaimedTime);
        dataBlock.setIntValue(i++, unit.provisioning.unit_counter);
        dataBlock.setIntValue(i++, unit.provisioning.server_counter);
//_______________________________________________________________________________________________

//_______________________________________________________________________________________________
//        Unit {LastCheckin}
//_______________________________________________________________________________________________

        var lastCheckin = unit.last_checkin;
        dataBlock.setIntValue(i++, lastCheckin.id);
//_______________________________________________________________________________________________
//        Unit{LastCheckin{Meta}}
//_______________________________________________________________________________________________

        dataBlock.setIntValue(i++, lastCheckin.meta.key_id);
        dataBlock.setStringValue(i++, lastCheckin.meta.channel_info.remote);
        dataBlock.setIntValue(i++, lastCheckin.meta.nonce_counter);
//_______________________________________________________________________________________________

        dataBlock.setStringValue(i++, lastCheckin.channel);

//_______________________________________________________________________________________________
//        Unit{LastCheckin{Message}
//_______________________________________________________________________________________________
//        Unit{LastCheckin{Message{Low}}}
//_______________________________________________________________________________________________
//        LastCheckin{Message{Low{Gps}}}
//_______________________________________________________________________________________________
        try {
            try {
                var gpsLow = lastCheckin.message.low.gps;

                dataBlock.setDoubleValue(i++, gpsLow.hdop);
                dataBlock.setDoubleValue(i++, gpsLow.pdop);
                dataBlock.setDoubleValue(i++, gpsLow.vdop);
                dataBlock.setDoubleValue(i++, gpsLow.speed);
                dataBlock.setDoubleValue(i++, gpsLow.course);
                dataBlock.setDoubleValue(i++, gpsLow.altitude);
                dataBlock.setStringValue(i++, gpsLow.fix_type);
                dataBlock.setDoubleValue(i++, gpsLow.latitude);
                dataBlock.setDoubleValue(i++, gpsLow.longitude);
                dataBlock.setIntValue(i++, gpsLow.timestamp);
                dataBlock.setIntValue(i++, gpsLow.num_satellites);
            } catch (NullPointerException e){}
//_______________________________________________________________________________________________

        //Low
            dataBlock.setStringValue(i++, lastCheckin.message.low.arm_state);
        } catch (NullPointerException e) {}

//_______________________________________________________________________________________________
//        LastCheckin {Message {Low {Event log[]}}}
//_______________________________________________________________________________________________

        try {
            var event_log = lastCheckin.message.low.event_log;

            dataBlock.setStringValue(i++, event_log[0].fix_type);
            dataBlock.setDoubleValue(i++, event_log[0].latitude);
            dataBlock.setDoubleValue(i++, event_log[0].longitude);
            dataBlock.setIntValue(i++, event_log[0].source_id);
            dataBlock.setIntValue(i++, event_log[0].timestamp);
            dataBlock.setStringValue(i++, event_log[0].event_type);
        } catch (NullPointerException e) {}
//_______________________________________________________________________________________________

        //Low
        try {
            var low = lastCheckin.message.low;

            dataBlock.setIntValue(i++, low.timestamp);
            dataBlock.setStringValue(i++, low.power_mode);
            dataBlock.setIntValue(i++, low.battery_voltage);
            dataBlock.setIntValue(i++, low.event_log_length);
            dataBlock.setIntValue(i++, low.position_log_length);
            dataBlock.setIntValue(i++, low.checkin_schedule_sec);
        } catch (NullPointerException e) {}
//_______________________________________________________________________________________________

//_______________________________________________________________________________________________
        //LastCheckin{Message{High}}}
//_______________________________________________________________________________________________
        //LastCheckin{Message{High{UiData[Sensors{}]}}}
//_______________________________________________________________________________________________

        try {
            var sensors = lastCheckin.message.high.ui_data.sensors;
            dataBlock.setIntValue(i++, sensors.length);

            var sensorArray =
                    ((DataArrayImpl) dataStruct.getComponent("lastCheckin").getComponent("lastCheckinMessage").getComponent(
                            "high").getComponent("uiData").getComponent ("sensors").getComponent ("sensorArray"));
            sensorArray.updateSize();

            for (int ixx = 0; ixx < sensors.length; ixx++) {
                dataBlock.setStringValue(i++, sensors[ixx].name);
                dataBlock.setIntValue(i++, sensors[ixx].node_id);
                dataBlock.setIntValue(i++, sensors[ixx].position_x);
                dataBlock.setIntValue(i++, sensors[ixx].position_y);
            }
        } catch (NullPointerException e) {}
//_______________________________________________________________________________________________

        try {
            var uiDataHigh = lastCheckin.message.high.ui_data;

            dataBlock.setStringValue(i++, uiDataHigh.campaign_name);
            dataBlock.setStringValue(i++, uiDataHigh.container_image);
        } catch (NullPointerException e) {}

//_______________________________________________________________________________________________
//        LastCheckin{Message{High{Provisioning}}}
//_______________________________________________________________________________________________

        try{
            var provisioning = lastCheckin.message.high.provisioning;

            dataBlock.setIntValue(i++, provisioning.key_id);
            dataBlock.setIntValue(i++, provisioning.unit_id);
            dataBlock.setStringValue(i++, provisioning.unit_name);
            dataBlock.setStringValue(i++, provisioning.encryption_key);
        } catch (NullPointerException e) {}
//_______________________________________________________________________________________________

//_______________________________________________________________________________________________
//        LastCheckin{Message{High{MainBoardData}}}
//_______________________________________________________________________________________________
//        LastCheckin{Message{High{MainBoardData{Gps}}}}
//_______________________________________________________________________________________________

        try{
            var mainBoardGps = lastCheckin.message.high.main_board_data.gps;

                dataBlock.setDoubleValue(i++, mainBoardGps.hdop);
                dataBlock.setDoubleValue(i++, mainBoardGps.pdop);
                dataBlock.setDoubleValue(i++, mainBoardGps.vdop);
                dataBlock.setDoubleValue(i++, mainBoardGps.speed);
                dataBlock.setDoubleValue(i++, mainBoardGps.course);
                dataBlock.setDoubleValue(i++, mainBoardGps.altitude);
                dataBlock.setStringValue(i++, mainBoardGps.fix_type);
                dataBlock.setDoubleValue(i++, mainBoardGps.latitude);
                dataBlock.setDoubleValue(i++, mainBoardGps.longitude);
                dataBlock.setIntValue(i++, mainBoardGps.timestamp);
                dataBlock.setIntValue(i++, mainBoardGps.num_satellites);
            } catch (NullPointerException e){}
//_______________________________________________________________________________________________


        try {
            var mainBoardData = lastCheckin.message.high.main_board_data;

                dataBlock.setIntValue(i++, mainBoardData.humidity);
                dataBlock.setIntValue(i++, mainBoardData.pressure);
                dataBlock.setIntValue(i++, mainBoardData.temperature);

//_______________________________________________________________________________________________
//            LastCheckin{Message{High{MainBoardData{PowerSupply}}}}
//_______________________________________________________________________________________________

                try{
                    var powerSupply = mainBoardData.power_supply;

                    dataBlock.setIntValue(i++, powerSupply.iin);
                    dataBlock.setIntValue(i++, powerSupply.ichg);
                    dataBlock.setIntValue(i++, powerSupply.psys);
                    dataBlock.setIntValue(i++, powerSupply.vbat);
                    dataBlock.setIntValue(i++, powerSupply.vbus);
                    dataBlock.setIntValue(i++, powerSupply.vsys);
                    dataBlock.setIntValue(i++, powerSupply.cmpin);
                    dataBlock.setIntValue(i++, powerSupply.idchg);
                    dataBlock.setBooleanValue(i++, powerSupply.sysovp);
                    dataBlock.setBooleanValue(i++, powerSupply.in_vindpm);
                    dataBlock.setIntValue(i++, powerSupply.timestamp);
                    dataBlock.setBooleanValue(i++, powerSupply.acoc_fault);
                    dataBlock.setBooleanValue(i++, powerSupply.acov_fault);
                    dataBlock.setBooleanValue(i++, powerSupply.batoc_fault);
                    dataBlock.setBooleanValue(i++, powerSupply.pre_charging);
                    dataBlock.setBooleanValue(i++, powerSupply.fast_charging);
                    dataBlock.setBooleanValue(i++, powerSupply.fault_latch_off);
                    dataBlock.setIntValue(i++, powerSupply.status_register);
                    dataBlock.setIntValue(i++, powerSupply.prochot_register);
                    dataBlock.setBooleanValue(i++, powerSupply.external_connected);
                } catch (NullPointerException e){}
//_______________________________________________________________________________________________

                dataBlock.setStringValue(i++, mainBoardData.serial_number);
//_______________________________________________________________________________________________
//            LastCheckin{Message{High{MainBoardData[ZwaveSensors{}]}}}
//_______________________________________________________________________________________________

                try {
                    var zWaveSensors = mainBoardData.zwave_sensors;
                    dataBlock.setIntValue(i++, zWaveSensors.length);

                        var sensorArray =
                                ((DataArrayImpl) dataStruct.getComponent("lastCheckin").getComponent("lastCheckinMessage").getComponent(
                                        "high").getComponent("mainBoardData").getComponent ("zWaveSensors").getComponent ("zWaveSensorArray"));
                        sensorArray.updateSize();

                        for (int ixx = 0; ixx < zWaveSensors.length; ixx++) {
                            dataBlock.setIntValue(i++, zWaveSensors[ixx].rssi);
                            dataBlock.setIntValue(i++, zWaveSensors[ixx].node_id);
                            dataBlock.setBooleanValue(i++, zWaveSensors[ixx].lux_high);
                            dataBlock.setBooleanValue(i++, zWaveSensors[ixx].security);
                            dataBlock.setBooleanValue(i++, zWaveSensors[ixx].battery_low);
                            dataBlock.setStringValue(i++, zWaveSensors[ixx].sensor_type);
                            dataBlock.setBooleanValue(i++, zWaveSensors[ixx].contact_open);
                            dataBlock.setDoubleValue(i++, zWaveSensors[ixx].battery_level);
                            dataBlock.setBooleanValue(i++, zWaveSensors[ixx].motion_trigger);
                            dataBlock.setBooleanValue(i++, zWaveSensors[ixx].sensor_missing);
                            dataBlock.setBooleanValue(i++, zWaveSensors[ixx].tamper_trigger);
                            dataBlock.setLongValue(i++, zWaveSensors[ixx].last_report_timestamp);
                            dataBlock.setIntValue(i++, zWaveSensors[ixx].heartbeat_interval_sec);
                        }
                    } catch (NullPointerException e) {}
//_______________________________________________________________________________________________


                dataBlock.setStringValue(i++, mainBoardData.hardware_version);
                dataBlock.setStringValue(i++, mainBoardData.software_version);
                dataBlock.setIntValue(i++, mainBoardData.event_log_overruns);
                dataBlock.setIntValue(i++, mainBoardData.countdown_to_arming);
                dataBlock.setIntValue(i++, mainBoardData.power_mode_timestamp);
                dataBlock.setIntValue(i++, mainBoardData.position_log_overruns);
                dataBlock.setIntValue(i++, mainBoardData.countdown_to_modem_off);

        } catch (NullPointerException e) {}
//_______________________________________________________________________________________________

//_______________________________________________________________________________________________
//        LastCheckin{Message{High{ModemBoardData}}}
//_______________________________________________________________________________________________

        try {
            var modemBoardData = lastCheckin.message.high.modem_board_data;

            dataBlock.setIntValue(i++, modemBoardData.uptime);
            dataBlock.setIntValue(i++, modemBoardData.temperature);
            dataBlock.setIntValue(i++, modemBoardData.last_checkin);
            dataBlock.setStringValue(i++, modemBoardData.serial_number);
            dataBlock.setDoubleValue(i++, modemBoardData.fs_home_cap_mb);
            dataBlock.setDoubleValue(i++, modemBoardData.fs_root_cap_mb);
            dataBlock.setDoubleValue(i++, modemBoardData.fs_home_size_mb);
            dataBlock.setDoubleValue(i++, modemBoardData.fs_root_size_mb);
            dataBlock.setStringValue(i++, modemBoardData.hardware_version);
            dataBlock.setStringValue(i++, modemBoardData.software_version);
            dataBlock.setIntValue(i++, modemBoardData.last_full_checkin);
        } catch (NullPointerException e) {}
//_______________________________________________________________________________________________

//_______________________________________________________________________________________________
//        LastCheckin{Message{High{MainBoardConfig}}}
//_______________________________________________________________________________________________

        try{
            var mainBoardConfig = lastCheckin.message.high.main_board_config;

            dataBlock.setStringValue(i++, mainBoardConfig.zwave_mode);
            dataBlock.setIntValue(i++, mainBoardConfig.arming_countdown_sec);
//_______________________________________________________________________________________________
//        LastCheckin{Message{High{MainBoardConfig[ZwaveSensorConfigs{}]}}}
//_______________________________________________________________________________________________

            try{
                var sensorConfigs = mainBoardConfig.zwave_sensor_configs;
                dataBlock.setIntValue(i++, sensorConfigs.length);

                var sensorArray =
                        ((DataArrayImpl) dataStruct.getComponent("lastCheckin").getComponent("lastCheckinMessage").getComponent(
                                "high").getComponent("mainBoardConfig").getComponent ("zWaveSensorConfigs").getComponent (
                                        "sensorConfigs"));
                sensorArray.updateSize();

                for (int ixx = 0; ixx < sensorConfigs.length; ixx++) {
                    dataBlock.setBooleanValue(i++, sensorConfigs[ixx].delete);
                    dataBlock.setBooleanValue(i++, sensorConfigs[ixx].monitor);
                    dataBlock.setIntValue(i++, sensorConfigs[ixx].node_id);
                    dataBlock.setIntValue(i++, sensorConfigs[ixx].lux_sensitivity);
                    dataBlock.setIntValue(i++, sensorConfigs[ixx].motion_sensitivity);
                }
            } catch (NullPointerException e) {}
//_______________________________________________________________________________________________
        } catch (NullPointerException e) {}
//_______________________________________________________________________________________________

//_______________________________________________________________________________________________
//        LastCheckin{Message{High{ModemBoardConfig}}}
//_______________________________________________________________________________________________

        try{
            var modemBoardConfig = lastCheckin.message.high.modem_board_config;

            dataBlock.setStringValue(i++, modemBoardConfig.cellular_apn);
            dataBlock.setStringValue(i++, modemBoardConfig.tstar_server_url);
        } catch (NullPointerException e) {}
//_______________________________________________________________________________________________

        dataBlock.setLongValue(i++, lastCheckinTimestamp);

//_______________________________________________________________________________________________
//        Unit {LastFullCheckin}
//_______________________________________________________________________________________________

        var lastFullCheckin = unit.last_full_checkin;
        dataBlock.setIntValue(i++, lastFullCheckin.id);
//_______________________________________________________________________________________________
//        Unit{LastFullCheckin{Meta}}
//_______________________________________________________________________________________________

        dataBlock.setIntValue(i++, lastFullCheckin.meta.key_id);
        dataBlock.setStringValue(i++, lastFullCheckin.meta.channel_info.remote);
        dataBlock.setIntValue(i++, lastFullCheckin.meta.nonce_counter);
//_______________________________________________________________________________________________

        dataBlock.setStringValue(i++, lastFullCheckin.channel);



//_______________________________________________________________________________________________
//        Unit{LastFullCheckin{Message}
//_______________________________________________________________________________________________
//        Unit{LastFullCheckin{Message{Low}}}
//_______________________________________________________________________________________________
//        LastFullCheckin{Message{Low{Gps}}}
//_______________________________________________________________________________________________

        try {
            var gpsLow = lastFullCheckin.message.low.gps;

            dataBlock.setDoubleValue(i++, gpsLow.hdop);
            dataBlock.setDoubleValue(i++, gpsLow.pdop);
            dataBlock.setDoubleValue(i++, gpsLow.vdop);
            dataBlock.setDoubleValue(i++, gpsLow.speed);
            dataBlock.setDoubleValue(i++, gpsLow.course);
            dataBlock.setDoubleValue(i++, gpsLow.altitude);
            dataBlock.setStringValue(i++, gpsLow.fix_type);
            dataBlock.setDoubleValue(i++, gpsLow.latitude);
            dataBlock.setDoubleValue(i++, gpsLow.longitude);
            dataBlock.setIntValue(i++, gpsLow.timestamp);
            dataBlock.setIntValue(i++, gpsLow.num_satellites);
        } catch (NullPointerException e){}

        //Low
        try {
            dataBlock.setStringValue(i++, lastFullCheckin.message.low.arm_state);
        } catch (NullPointerException e) {}

        // LastFullCheckin {Message {Low {Event log[]}}}
        try {
            var event_log = lastFullCheckin.message.low.event_log;

            dataBlock.setStringValue(i++, event_log[0].fix_type);
            dataBlock.setDoubleValue(i++, event_log[0].latitude);
            dataBlock.setDoubleValue(i++, event_log[0].longitude);
            dataBlock.setIntValue(i++, event_log[0].source_id);
            dataBlock.setIntValue(i++, event_log[0].timestamp);
            dataBlock.setStringValue(i++, event_log[0].event_type);
        } catch (NullPointerException e) {}

        //LastFullCheckin {Message {Low}}
        try {
            var low = lastFullCheckin.message.low;

            dataBlock.setIntValue(i++, low.timestamp);
            dataBlock.setStringValue(i++, low.power_mode);
            dataBlock.setIntValue(i++, low.battery_voltage);
            dataBlock.setIntValue(i++, low.event_log_length);
            dataBlock.setIntValue(i++, low.position_log_length);
            dataBlock.setIntValue(i++, low.checkin_schedule_sec);
        } catch (NullPointerException e) {}


        //LastFullCheckin{Message {High {UiData{Sensors[]}}}}
        try {
            var sensors = lastFullCheckin.message.high.ui_data.sensors;
            dataBlock.setIntValue(i++, sensors.length);

            var sensorArray =
                    ((DataArrayImpl) dataStruct.getComponent("lastFullCheckin").getComponent("lastFullCheckinMessage").getComponent(
                            "high").getComponent("uiData").getComponent ("sensors").getComponent ("sensorArray"));
            sensorArray.updateSize();

            for (int ixx = 0; ixx < sensors.length; ixx++) {
                dataBlock.setStringValue(i++, sensors[ixx].name);
                dataBlock.setIntValue(i++, sensors[ixx].node_id);
                dataBlock.setIntValue(i++, sensors[ixx].position_x);
                dataBlock.setIntValue(i++, sensors[ixx].position_y);
            }
        } catch (NullPointerException e) {}

        try {
            var uiDataHigh = lastFullCheckin.message.high.ui_data;

            dataBlock.setStringValue(i++, uiDataHigh.campaign_name);
            dataBlock.setStringValue(i++, uiDataHigh.container_image);
        } catch (NullPointerException e) {}

        //LastFullCheckin{Message {High {Provisioning}}}
        try{
            var provisioning = lastFullCheckin.message.high.provisioning;

            dataBlock.setIntValue(i++, provisioning.key_id);
            dataBlock.setIntValue(i++, provisioning.unit_id);
            dataBlock.setStringValue(i++, provisioning.unit_name);
            dataBlock.setStringValue(i++, provisioning.encryption_key);
        } catch (NullPointerException e) {}

        //LastFullCheckin{Message{High{MainBoardData}}}
        // {Gps}
        try{
            var mainBoardGps = lastFullCheckin.message.high.main_board_data.gps;

            dataBlock.setDoubleValue(i++, mainBoardGps.hdop);
            dataBlock.setDoubleValue(i++, mainBoardGps.pdop);
            dataBlock.setDoubleValue(i++, mainBoardGps.vdop);
            dataBlock.setDoubleValue(i++, mainBoardGps.speed);
            dataBlock.setDoubleValue(i++, mainBoardGps.course);
            dataBlock.setDoubleValue(i++, mainBoardGps.altitude);
            dataBlock.setStringValue(i++, mainBoardGps.fix_type);
            dataBlock.setDoubleValue(i++, mainBoardGps.latitude);
            dataBlock.setDoubleValue(i++, mainBoardGps.longitude);
            dataBlock.setIntValue(i++, mainBoardGps.timestamp);
            dataBlock.setIntValue(i++, mainBoardGps.num_satellites);
        } catch (NullPointerException e){}

        try {
            var mainBoardData = lastFullCheckin.message.high.main_board_data;

            dataBlock.setIntValue(i++, mainBoardData.humidity);
            dataBlock.setIntValue(i++, mainBoardData.pressure);
            dataBlock.setIntValue(i++, mainBoardData.temperature);

            try{
                var powerSupply = mainBoardData.power_supply;

                dataBlock.setIntValue(i++, powerSupply.iin);
                dataBlock.setIntValue(i++, powerSupply.ichg);
                dataBlock.setIntValue(i++, powerSupply.psys);
                dataBlock.setIntValue(i++, powerSupply.vbat);
                dataBlock.setIntValue(i++, powerSupply.vbus);
                dataBlock.setIntValue(i++, powerSupply.vsys);
                dataBlock.setIntValue(i++, powerSupply.cmpin);
                dataBlock.setIntValue(i++, powerSupply.idchg);
                dataBlock.setBooleanValue(i++, powerSupply.sysovp);
                dataBlock.setBooleanValue(i++, powerSupply.in_vindpm);
                dataBlock.setIntValue(i++, powerSupply.timestamp);
                dataBlock.setBooleanValue(i++, powerSupply.acoc_fault);
                dataBlock.setBooleanValue(i++, powerSupply.acov_fault);
                dataBlock.setBooleanValue(i++, powerSupply.batoc_fault);
                dataBlock.setBooleanValue(i++, powerSupply.pre_charging);
                dataBlock.setBooleanValue(i++, powerSupply.fast_charging);
                dataBlock.setBooleanValue(i++, powerSupply.fault_latch_off);
                dataBlock.setIntValue(i++, powerSupply.status_register);
                dataBlock.setIntValue(i++, powerSupply.prochot_register);
                dataBlock.setBooleanValue(i++, powerSupply.external_connected);
            } catch (NullPointerException e){}

            dataBlock.setStringValue(i++, mainBoardData.serial_number);

            try {
                var zWaveSensors = mainBoardData.zwave_sensors;
                dataBlock.setIntValue(i++, zWaveSensors.length);
                try {
                    var sensorArray =
                            ((DataArrayImpl) dataStruct.getComponent("lastFullCheckin").getComponent(
                                    "lastFullCheckinMessage").getComponent(
                                    "high").getComponent("mainBoardData").getComponent ("zWaveSensors").getComponent ("zWaveSensorArray"));
                    sensorArray.updateSize();

                    for (int ixx = 0; ixx < zWaveSensors.length; ixx++) {
                        dataBlock.setIntValue(i++, zWaveSensors[ixx].rssi);
                        dataBlock.setIntValue(i++, zWaveSensors[ixx].node_id);
                        dataBlock.setBooleanValue(i++, zWaveSensors[ixx].lux_high);
                        dataBlock.setBooleanValue(i++, zWaveSensors[ixx].security);
                        dataBlock.setBooleanValue(i++, zWaveSensors[ixx].battery_low);
                        dataBlock.setStringValue(i++, zWaveSensors[ixx].sensor_type);
                        dataBlock.setBooleanValue(i++, zWaveSensors[ixx].contact_open);
                        dataBlock.setDoubleValue(i++, zWaveSensors[ixx].battery_level);
                        dataBlock.setBooleanValue(i++, zWaveSensors[ixx].motion_trigger);
                        dataBlock.setBooleanValue(i++, zWaveSensors[ixx].sensor_missing);
                        dataBlock.setBooleanValue(i++, zWaveSensors[ixx].tamper_trigger);
                        dataBlock.setLongValue(i++, zWaveSensors[ixx].last_report_timestamp);
                        dataBlock.setIntValue(i++, zWaveSensors[ixx].heartbeat_interval_sec);
                    }
                } catch (NullPointerException e) {}

            } catch (NullPointerException e) {}

            dataBlock.setStringValue(i++, mainBoardData.hardware_version);
            dataBlock.setStringValue(i++, mainBoardData.software_version);
            dataBlock.setIntValue(i++, mainBoardData.event_log_overruns);
            dataBlock.setIntValue(i++, mainBoardData.countdown_to_arming);
            dataBlock.setIntValue(i++, mainBoardData.power_mode_timestamp);
            dataBlock.setIntValue(i++, mainBoardData.position_log_overruns);
            dataBlock.setIntValue(i++, mainBoardData.countdown_to_modem_off);

        } catch (NullPointerException e) {}

        try {
            var modemBoardData = lastFullCheckin.message.high.modem_board_data;

            dataBlock.setIntValue(i++, modemBoardData.uptime);
            dataBlock.setIntValue(i++, modemBoardData.temperature);
            dataBlock.setIntValue(i++, modemBoardData.last_checkin);
            dataBlock.setStringValue(i++, modemBoardData.serial_number);
            dataBlock.setDoubleValue(i++, modemBoardData.fs_home_cap_mb);
            dataBlock.setDoubleValue(i++, modemBoardData.fs_root_cap_mb);
            dataBlock.setDoubleValue(i++, modemBoardData.fs_home_size_mb);
            dataBlock.setDoubleValue(i++, modemBoardData.fs_root_size_mb);
            dataBlock.setStringValue(i++, modemBoardData.hardware_version);
            dataBlock.setStringValue(i++, modemBoardData.software_version);
            dataBlock.setIntValue(i++, modemBoardData.last_full_checkin);
        } catch (NullPointerException e) {}

        //LastFullCheckin{Message{High{MainBoardConfig}}}
        try{
            var mainBoardConfig = lastFullCheckin.message.high.main_board_config;

            dataBlock.setStringValue(i++, mainBoardConfig.zwave_mode);
            dataBlock.setIntValue(i++, mainBoardConfig.arming_countdown_sec);

            try{
                var sensorConfigs = mainBoardConfig.zwave_sensor_configs;
                dataBlock.setIntValue(i++, sensorConfigs.length);

                var sensorArray =
                        ((DataArrayImpl) dataStruct.getComponent("lastFullCheckin").getComponent(
                                "lastFullCheckinMessage").getComponent(
                                "high").getComponent("mainBoardConfig").getComponent ("zWaveSensorConfigs").getComponent (
                                "sensorConfigs"));
                sensorArray.updateSize();

                for (int ixx = 0; ixx < sensorConfigs.length; ixx++) {
                    dataBlock.setBooleanValue(i++, sensorConfigs[ixx].delete);
                    dataBlock.setBooleanValue(i++, sensorConfigs[ixx].monitor);
                    dataBlock.setIntValue(i++, sensorConfigs[ixx].node_id);
                    dataBlock.setIntValue(i++, sensorConfigs[ixx].lux_sensitivity);
                    dataBlock.setIntValue(i++, sensorConfigs[ixx].motion_sensitivity);
                }
            } catch (NullPointerException e) {}
        } catch (NullPointerException e) {}

        //LastFullCheckin{Message{High{ModemBoardConfig}}}
        try{
            var modemBoardConfig = lastFullCheckin.message.high.modem_board_config;

            dataBlock.setStringValue(i++, modemBoardConfig.cellular_apn);
            dataBlock.setStringValue(i++, modemBoardConfig.tstar_server_url);
        } catch (NullPointerException e) {}

        dataBlock.setLongValue(i++, lastFullCheckinTimestamp);

//_______________________________________________________________________________________________
//_______________________________________________________________________________________________
        //LastCommand
        try{
            var lastCommand = unit.last_command;

            dataBlock.setIntValue(i++, lastCommand.id);
            dataBlock.setIntValue(i++, lastCommand.meta.key_id);
            dataBlock.setStringValue(i++, lastCommand.meta.channel_info.remote);
            dataBlock.setIntValue(i++, lastCommand.meta.nonce_counter);
            dataBlock.setStringValue(i++, lastCommand.channel);
            dataBlock.setIntValue(i++, lastCommand.message.event_log_ack);
            dataBlock.setLongValue(i++, lastCommandTimestamp);
        } catch (NullPointerException e) {}

        //EventTable
        try{
            var unitEvents = unit.event_table.unit_events;

            dataBlock.setBooleanValue(i++, unitEvents.EVENT_GPS_FAILURE);
            dataBlock.setBooleanValue(i++, unitEvents.EVENT_UNIT_MISSING);
            dataBlock.setBooleanValue(i++, unitEvents.EVENT_IMPROPER_DISARM);
            dataBlock.setBooleanValue(i++, unitEvents.EVENT_MAINBOARD_FAILURE);
            dataBlock.setBooleanValue(i++, unitEvents.EVENT_ARMING_NO_CAMPAIGN);;
        } catch (NullPointerException e) {}

        dataBlock.setBooleanValue(i++, unit.latch_power_mode_high);
        dataBlock.setIntValue(i++, unit.campaign_id);
        dataBlock.setStringValue(i++, unit.campaign_name);
        dataBlock.setStringValue(i++, unit.managers);
        dataBlock.setBooleanValue(i++, unit.has_recent_alarm);
        dataBlock.setBooleanValue(i++, unit.deleted);



        // update latest record and send event
        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(latestRecordTime, TSTARUnitOutput.this, dataBlock));
    }

    public void setUnitTime(Unit unit){
        // parse UTC  to epoch time for 'last_activity'
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        if (unit.timestamp != null) {
            Date unitTime = unit.timestamp;
            unitTimestamp = unitTime.getTime() / 1000;
        }

        if (unit.provisioning.generated_timestamp != null) {
            Date proGenTime = unit.provisioning.generated_timestamp;
            provisioningGenTime = proGenTime.getTime()/1000;
        }

        if (unit.provisioning.claimed_timestamp != null) {
            Date proClaimTime = unit.provisioning.claimed_timestamp;
            provisioningClaimedTime = proClaimTime.getTime()/1000;
        }

        if (unit.last_checkin.timestamp != null) {
            Date lastChkinTime = unit.last_checkin.timestamp;
            lastCheckinTimestamp = lastChkinTime.getTime()/1000;
        }
        if (unit.last_full_checkin.timestamp != null) {
            Date lastChkinTime = unit.last_full_checkin.timestamp;
            lastFullCheckinTimestamp = lastChkinTime.getTime()/1000;
        }

        if (unit.last_command.timestamp != null) {
            Date lastCmdTime = unit.last_command.timestamp;
            lastCommandTimestamp = lastCmdTime.getTime()/1000;
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
        return 1;
    }
}

