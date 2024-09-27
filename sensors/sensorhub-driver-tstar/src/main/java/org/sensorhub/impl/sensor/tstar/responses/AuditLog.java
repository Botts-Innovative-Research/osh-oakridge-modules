package org.sensorhub.impl.sensor.tstar.responses;

import java.util.Date;

public class AuditLog {
    public String id;
    public Date timestamp;
    public String action;
    public String source_ip;
    public int user_id;
    public String target_table;
    public String target_id;
    public String target_name;
    public AuditLogData data;
    public static class AuditLogData{
        public String id;
        public int campaign_id;
    }
    public String user_name;
}
