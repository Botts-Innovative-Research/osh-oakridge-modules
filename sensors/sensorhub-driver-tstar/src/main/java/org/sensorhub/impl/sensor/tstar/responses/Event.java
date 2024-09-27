package org.sensorhub.impl.sensor.tstar.responses;

import java.util.Date;

public class Event {
    public int id;
    public String event_type;
    public Boolean alarm;
    public int campaign_id;
    public int unit_id;
    public Date generated_timestamp;
    public Date received_timestamp;
    public AcknowledgedBy acknowledged_by;
    public static class AcknowledgedBy {
        public int user_id;
        public String name;
    }
    public double latitude;
    public double longitude;
    public Date ack_timestamp;
    public EventMsgData msg_data;
    public static class EventMsgData {
        public int source_id;
        public String unit_name;
        public String sensor_name;
    }
    public Boolean notification_sent;


}

