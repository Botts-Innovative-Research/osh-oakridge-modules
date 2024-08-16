package org.sensorhub.impl.sensor.tstar.responses;

import java.util.Date;

public class PositionLog {

    public int id;
    public int unit_id;
    public double latitude;
    public double longitude;
    public String position;
    public int course;
    public int speed;
    public Date generated_timestamp;
    public Date received_timestamp;
    public int campaign_id;
    public String channel;

}
