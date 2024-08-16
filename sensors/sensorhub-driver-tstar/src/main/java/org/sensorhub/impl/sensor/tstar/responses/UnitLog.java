package org.sensorhub.impl.sensor.tstar.responses;

import java.util.Date;

public class UnitLog {

    public int id;
    public int unit_id;
    public Date timestamp;
    public String level;
    //"info"
    public String msg;
    //"POWER_EXPLAIN: POWER_MODE_HIGH - unit is connected to shore power and set to stay on"
}
