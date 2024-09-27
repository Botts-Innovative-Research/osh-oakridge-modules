package org.sensorhub.impl.sensor.tstar;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.tstar.responses.MessageLog;
import org.vast.data.DataArrayImpl;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;


public class TSTARMessageLogOutput extends AbstractSensorOutput<TSTARDriver> {
    private static final String SENSOR_OUTPUT_NAME = "Message Log Output";
    protected DataRecord dataStruct;
    DataEncoding dataEncoding;
    Long msgLogTimestamp;
    Long msgLogDelivered;
    Long eventLogAck;
    TSTARHelper tstarHelper = new TSTARHelper();

    public TSTARMessageLogOutput(TSTARDriver parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

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
                .definition(SWEHelper.getPropertyUri("MessageLogData"))
                .addField("messageLogId", tstarHelper.createMessageLogId())
                .addField("messageLogTimestamp", tstarHelper.createTimestamp())
                .addField("delivered", tstarHelper.createDelivered())
                .addField("direction", tstarHelper.createDirection())
                .addField("messageLogChannel", tstarHelper.createChannel())
                .addField("meta", tstarHelper.createMeta()) //Fields(3): key_id, channel_info{remote}, nonce_counter
                .addField("unitId", tstarHelper.createUnitId())
                .addField("campaignId", tstarHelper.createCampaignId())
                .addField("messageLogPosition", tstarHelper.createPosition())
                .addField("rawPacketData", tstarHelper.createRawPacket())
                .addField("messageLogMessage",tstarHelper.createMsgLogMessage()) //Fields(2): low, high
                .addField("unitName", tstarHelper.createUnitName())
                .build();

        // set encoding to CSV
    }

    public void parse(MessageLog msgLog) {

        dataStruct = createDataRecord();
        DataBlock dataBlock = dataStruct.createDataBlock();
        dataStruct.setData(dataBlock);

        latestRecordTime = System.currentTimeMillis() / 1000;
        setMessageLogTime(msgLog);

        int i = 0;
        int numArrayFields = msgLog.raw_packet.data.length;
        int[] packetData = msgLog.raw_packet.data;

            dataBlock.setIntValue(i++, msgLog.id);
            dataBlock.setLongValue(i++, msgLogTimestamp);
            try {
                dataBlock.setLongValue(i++, msgLogDelivered);
            } catch (NullPointerException e) {}
            dataBlock.setStringValue(i++, msgLog.direction);
            dataBlock.setStringValue(i++, msgLog.channel);
            dataBlock.setIntValue(i++, msgLog.meta.key_id);
            dataBlock.setStringValue(i++, msgLog.meta.channel_info.remote);
            dataBlock.setIntValue(i++, msgLog.meta.nonce_counter);
            dataBlock.setIntValue(i++, msgLog.unit_id);
            dataBlock.setIntValue(i++, msgLog.campaign_id);

        try {
            dataBlock.setStringValue(i++, msgLog.position);
        } catch (NullPointerException e) {
//            dataBlock.setStringValue(i++, "null");
        }


            dataBlock.setStringValue(i++, msgLog.raw_packet.type);
            dataBlock.setIntValue(i++, msgLog.raw_packet.data.length);

            var array = ((DataArrayImpl) dataStruct.getComponent("rawPacketData").getComponent("rawPacketArray"));
            array.updateSize();

            for (int ix = 0; ix < msgLog.raw_packet.data.length; ix++) {
                dataBlock.setIntValue(i++, msgLog.raw_packet.data[ix]);
            }
            try{
                dataBlock.setIntValue(i++, msgLog.message.event_log_ack);
            } catch (NullPointerException e) {
//                dataBlock.setIntValue(0);
            }
//        //Low {Gps}
            try {
                var gpsLow = msgLog.message.low.gps;
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
//            } catch (NullPointerException e) {}

            //Low
//            try {
                dataBlock.setStringValue(i++, msgLog.message.low.arm_state);
//            } catch (NullPointerException e) {}

            //Low {Event log[]}
//            try {
                var event_log = msgLog.message.low.event_log;

                dataBlock.setStringValue(i++, event_log[0].fix_type);
                dataBlock.setDoubleValue(i++, event_log[0].latitude);
                dataBlock.setDoubleValue(i++, event_log[0].longitude);
                dataBlock.setIntValue(i++, event_log[0].source_id);
                dataBlock.setIntValue(i++, event_log[0].timestamp);
                dataBlock.setStringValue(i++, event_log[0].event_type);
//            } catch (NullPointerException e) {}

            //Low
//            try {
                var low = msgLog.message.low;

                dataBlock.setIntValue(i++, low.timestamp);
                dataBlock.setStringValue(i++, low.power_mode);
                dataBlock.setIntValue(i++, low.battery_voltage);
                dataBlock.setIntValue(i++, low.event_log_length);
                dataBlock.setIntValue(i++, low.position_log_length);
                dataBlock.setIntValue(i++, low.checkin_schedule_sec);
//            } catch (NullPointerException e) {}

            //High {UiData{Sensors[]}
//            try {
                var sensors = msgLog.message.high.ui_data.sensors;
                dataBlock.setIntValue(i++, sensors.length);

                var sensorArray =
                        ((DataArrayImpl) dataStruct.getComponent("messageLogMessage").getComponent("high").getComponent(
                                "uiData").getComponent("sensors").getComponent("sensorArray"));
                sensorArray.updateSize();

                for (int ixx = 0; ixx < sensors.length; ixx++) {
                    dataBlock.setStringValue(i++, sensors[ixx].name);
                    dataBlock.setIntValue(i++, sensors[ixx].node_id);
                    dataBlock.setIntValue(i++, sensors[ixx].position_x);
                    dataBlock.setIntValue(i++, sensors[ixx].position_y);
                }
//            } catch (NullPointerException e) {}

//            try {
                var uiDataHigh = msgLog.message.high.ui_data;

                dataBlock.setStringValue(i++, uiDataHigh.campaign_name);
                dataBlock.setStringValue(i++, uiDataHigh.container_image);

//            } catch (NullPointerException e) {
//            }

//            try {
                var provisioning = msgLog.message.high.provisioning;

                dataBlock.setIntValue(i++, provisioning.key_id);
                dataBlock.setIntValue(i++, provisioning.unit_id);
                dataBlock.setStringValue(i++, provisioning.unit_name);
                dataBlock.setStringValue(i++, provisioning.encryption_key);
//            } catch (NullPointerException e) {}

            //High{MainBoardData}}}
            // {Gps}
//            try {
                var mainBoardGps = msgLog.message.high.main_board_data.gps;

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
//            } catch (NullPointerException e) {}

//            try {
                var mainBoardData = msgLog.message.high.main_board_data;

                dataBlock.setIntValue(i++, mainBoardData.humidity);
                dataBlock.setIntValue(i++, mainBoardData.pressure);
                dataBlock.setIntValue(i++, mainBoardData.temperature);

//                try {
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
//                } catch (NullPointerException e) {
//                }

                dataBlock.setStringValue(i++, mainBoardData.serial_number);

//                try {
                    var zWaveSensors = mainBoardData.zwave_sensors;
                    dataBlock.setIntValue(i++, zWaveSensors.length);
//                    try {
                        var sensorArray2 =
                                ((DataArrayImpl) dataStruct.getComponent("messageLogMessage").getComponent(
                                        "high").getComponent("mainBoardData").getComponent("zWaveSensors").getComponent("zWaveSensorArray"));
                        sensorArray2.updateSize();

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
//                    } catch (NullPointerException e) {
//                    }

//                } catch (NullPointerException e) {
//                }

                dataBlock.setStringValue(i++, mainBoardData.hardware_version);
                dataBlock.setStringValue(i++, mainBoardData.software_version);
                dataBlock.setIntValue(i++, mainBoardData.event_log_overruns);
                dataBlock.setIntValue(i++, mainBoardData.countdown_to_arming);
                dataBlock.setIntValue(i++, mainBoardData.power_mode_timestamp);
                dataBlock.setIntValue(i++, mainBoardData.position_log_overruns);
                dataBlock.setIntValue(i++, mainBoardData.countdown_to_modem_off);

//            } catch (NullPointerException e) {
//            }

//            try {
                var modemBoardData = msgLog.message.high.modem_board_data;

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
//            } catch (NullPointerException e) {
//            }

            //High{MainBoardConfig}}
//            try {
                var mainBoardConfig = msgLog.message.high.main_board_config;

                dataBlock.setStringValue(i++, mainBoardConfig.zwave_mode);
                dataBlock.setIntValue(i++, mainBoardConfig.arming_countdown_sec);

//                try {
                    var sensorConfigs = mainBoardConfig.zwave_sensor_configs;
                    dataBlock.setIntValue(i++, sensorConfigs.length);

                    var sensorArray3 =
                            ((DataArrayImpl) dataStruct.getComponent("messageLogMessage").getComponent(
                                    "high").getComponent("mainBoardConfig").getComponent("zWaveSensorConfigs").getComponent(
                                    "sensorConfigs"));
                    sensorArray3.updateSize();

                    for (int ixx = 0; ixx < sensorConfigs.length; ixx++) {
                        dataBlock.setBooleanValue(i++, sensorConfigs[ixx].delete);
                        dataBlock.setBooleanValue(i++, sensorConfigs[ixx].monitor);
                        dataBlock.setIntValue(i++, sensorConfigs[ixx].node_id);
                        dataBlock.setIntValue(i++, sensorConfigs[ixx].lux_sensitivity);
                        dataBlock.setIntValue(i++, sensorConfigs[ixx].motion_sensitivity);
                    }
//                } catch (NullPointerException e) {
//                }
//            } catch (NullPointerException e) {
//            }

            //High{ModemBoardConfig}}
//            try {
                var modemBoardConfig = msgLog.message.high.modem_board_config;

                dataBlock.setStringValue(i++, modemBoardConfig.cellular_apn);
                dataBlock.setStringValue(i++, modemBoardConfig.tstar_server_url);
//            } catch (NullPointerException e) {}

//        try {
            dataBlock.setStringValue(i++, msgLog.unit_name);
        } catch (NullPointerException e) {}


        // update latest record and send event
        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(latestRecordTime, TSTARMessageLogOutput.this, dataBlock));
    }

    public void setMessageLogTime(MessageLog msgLog) {
        // parse UTC  to epoch time
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        if (msgLog.timestamp != null) {
            Date messageLogTimestamp = msgLog.timestamp;
            msgLogTimestamp = messageLogTimestamp.getTime() / 1000;
        } else {
            msgLogTimestamp = 0L;
        }
        if (msgLog.delivered != null) {
            Date messageLogDelivered = msgLog.delivered;
            msgLogDelivered = messageLogDelivered.getTime() / 1000;
        }
//        if(msgLog.message.event_log_ack != null) {
//            Date eventLogAcknoledgement = msgLog.message.event_log_ack;
//            eventLogAck = eventLogAcknoledgement.getTime() / 1000;
//        }
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
