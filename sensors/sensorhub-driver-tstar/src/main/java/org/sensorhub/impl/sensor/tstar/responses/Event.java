package org.sensorhub.impl.sensor.tstar.responses;

public class Event {
    public int id;
    public int campaign_id;
    public int unit_id;
    public Boolean alarm;
    public String event_type;
    public double latitude;
    public double longitude;
    public Object msg_data;
    public String generated_timestamp;
    public String received_timestamp;
    public Boolean notification_sent;
}
