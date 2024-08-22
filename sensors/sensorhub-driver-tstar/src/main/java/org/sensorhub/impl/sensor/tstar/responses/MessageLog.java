package org.sensorhub.impl.sensor.tstar.responses;

import java.util.Date;

public class MessageLog {
    public int id;
    public Date timestamp;
    public Date delivered;
    public String direction;
    public String channel;
    public Meta meta;
    public int unit_id;
    public int campaign_id;
    public String position;
    public RawPacket raw_packet;
    public Message message;
    public String unit_name;

    // MessageLog Nested: Meta, RawPacket, Message
    public static class Meta {
        public int key_id;
        public ChannelInfo channel_info; //remote
        public int nonce_counter;
    }
    public static class RawPacket {
        public String type;
        public int[] data;
    }
    public static class Message {
        public Low low;
        public High high;
    }
    // Meta Nested: ChannelInfo
    public static class ChannelInfo {
        public String remote;
    }


    // Message nested: Low, High
    public static class Low {
        public GPS gps;
        public String arm_state;
        public EventLog[] event_log;
        public int timestamp;
        public String power_mode;
        public int battery_voltage;
        public int event_log_length;
        public int position_log_length;
        public int checkin_schedule_sec;
    }
    public static class High {
        public UiData ui_data;
        public Provisioning provisioning;
        public MainBoardData main_board_data;
        public ModemBoardData modem_board_data;
        public MainBoardConfig main_board_config;
        public ModemBoardConfig modem_board_config;
    }

    // Low nested: GPS, EventLog
    public static class GPS{
        public int hdop;
        public double pdop;
        public int vdop;
        public int speed;
        public int course;
        public int altitude;
        public String fix_type;
        public double latitude;
        public double longitude;
        public int timestamp;
        public int num_satellites;
    }
    public static class EventLog {
        public String fix_type;
        public double latitude;
        public double longitude;
        public int source_id;
        public int timestamp;
        public String event_type;
    }

    // High nested: UiData, Provisioning, MainBoardData
    // ModemBoardData, MainBoardConfig, ModemBoardConfig
    public static class UiData {
        public Sensors[] sensors;
        public String campaign_name;
        public String container_image;
    }
    // Ui nested: Sensors
    public static class Sensors {
        public String name;
        public int node_id;
        public int position_x;
        public int position_y;
    }
    public static class Provisioning {
        public int key_id;
        public int unit_id;
        public String unit_name;
        public String encryption_key;
    }
    public static class MainBoardData {
        public GPS gps;
        public int humidity;
        public int pressure;
        public int temperature;
        public PowerSupply power_supply;
        public int serial_number;
        public ZwaveSensors[] zwave_sensors;
        public String hardware_version;
        public String software_version;
        public int event_log_overruns;
        public int countdown_to_arming;
        public int power_mode_timestamp;
        public int position_log_overruns;
        public int countdown_to_modem_off;
    }
    public static class PowerSupply {
        public int iin;
        public int ichg;
        public int psys;
        public int vbat;
        public int vbus;
        public int vsys;
        public int cmpin;
        public int idchg;
        public boolean sysovp;
        public boolean in_vindpm;
        public int timestamp;
        public boolean acoc_fault;
        public boolean acov_fault;
        public boolean batoc_fault;
        public boolean pre_charging;
        public boolean fast_charging;
        public boolean fault_latch_off;
        public int status_register;
        public int prochot_register;
        public boolean external_connected;
    }
    public static class ZwaveSensors {
        public int rssi;
        public int node_id;
        public boolean lux_high;
        public boolean security;
        public boolean battery_low;
        public String sensor_type;
        public boolean contact_open;
        public int battery_level;
        public boolean motion_trigger;
        public boolean sensor_missing;
        public boolean tamper_trigger;
        public int last_report_timestamp;
        public int heartbeat_interval_sec;
    }
    public static class ModemBoardData {
        public int uptime;
        public int temperature;
        public int last_checkin;
        public String serial_number;
        public double fs_home_cap_mb;
        public double fs_root_cap_mb;
        public double fs_home_size_mb;
        public double fs_root_size_mb;
        public String hardware_version;
        public String software_version;
        public int last_full_checkin;
    }
    public static class MainBoardConfig {
        public String zwave_mode;
        public int arming_countdown_sec;
        public ZwaveSensorConfigs[] zwave_sensor_configs;
    }
    public static class ZwaveSensorConfigs {
        public boolean delete;
        public boolean monitor;
        public int node_id;
        public int lux_sensitivity;
        public int motion_sensitivity;
    }
    //Configs an array?
    public static class ModemBoardConfig {
        public String cellular_apn;
        public String tstar_server_url;
    }

}
