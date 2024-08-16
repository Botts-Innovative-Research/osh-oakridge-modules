package org.sensorhub.impl.sensor.tstar.responses;

import java.util.Date;

public class MessageLog {
    public int id;
    public Date timestamp;
    public Date delivered;
    public String in;
    public String channel;
    public Object meta = new Object(){
        public int key_id;
        public Object channel_info = new Object(){
            public String remote;
        };
        public int nonce_counter;
    };
    public int unit_id;
    public int campaign_id;
    public String position;
    public Object raw_packet = new Object(){
        public String type;
        public int[] data;
    };
    public Object message = new Object(){
        public Object low = new Object() {
            public Object gps = new Object() {
                public int hdop;
                public double pdop;
                public int vdop;
                public int speed;
                public int course;
                public int altitude;
                public String fix_type;
                public double latitude;
                public double longitude;
                public Date timestamp;
                public int num_satellites;
            };
            public String arm_state;
            public EventLog[] event_log;
                class EventLog {
                    public String fix_type;
                    public double latitude;
                    public double longitude;
                    public int source_id;
                    public Date timestamp;
                    public String event_type;
            }
            public Date timestamp;
            public String power_mode;
            public int battery_voltage;
            public int event_log_length;
            public int position_log_length;
            public int checkin_schedule_sec;
        };
        public Object high = new Object(){
            public Object ui_data = new Object(){
                public Sensors[] sensors;
                class Sensors {
                    public String name;
                    public int node_id;
                    public int position_x;
                    public int position_y;
                }
                public String campaign_name;
                public String container_image;
            };
            public Object provisioning = new Object() {
                public int key_id;
                public int unit_id;
                public String unit_name;
                public String encryption_key;
            };
            public Object main_board_data = new Object() {
                public Object gps = new Object(){
                    public int hdop;
                    public double pdop;
                    public int vdop;
                    public int speed;
                    public int course;
                    public int altitude;
                    public String fix_type;
                    public double latitude;
                    public double longitude;
                    public Date timestamp;
                    public int num_satellites;
                };
                public int humidity;
                public int pressure;
                public int temperature;
                public Object power_supply = new Object(){
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
                    public Date timestamp;
                    public boolean acoc_fault;
                    public boolean acov_fault;
                    public boolean batoc_fault;
                    public boolean pre_charging;
                    public boolean fast_charging;
                    public boolean fault_latch_off;
                    public int status_register;
                    public int prochot_register;
                    public boolean external_connected;

                };
                public int serial_number;
                public ZwaveSensors[] zwave_sensors;
                    class ZwaveSensors{
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
                        public Date last_report_timestamp;
                        public int heartbeat_interval_sec;

                }
                public String hardware_version;
                public String software_version;
                public int event_log_overruns;
                public int countdown_to_arming;
                public int power_mode_timestamp;
                public int position_log_overruns;
                public int countdown_to_modem_off;

            };
            public Object moden_board_data = new Object() {
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
            };
            public Object main_board_config = new Object() {
                public String zwave_mode;
                public int arming_countdown_sec;
                public ZwaveSensorConfigs[] zwave_sensor_configs;
                class ZwaveSensorConfigs{
                    public boolean delete;
                    public boolean monitor;
                    public int node_id;
                    public int lux_sensitivity;
                    public int motion_sensitivity;
                }
            };
            public Object moden_board_config = new Object() {
                public String cellular_apn;
                public String tstar_server_url;
            };
        };
    };
    public String unit_name;

}
