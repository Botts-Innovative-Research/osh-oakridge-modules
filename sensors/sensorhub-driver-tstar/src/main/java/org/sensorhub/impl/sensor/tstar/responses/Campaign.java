package org.sensorhub.impl.sensor.tstar.responses;

import java.util.Date;

public class Campaign {

        public int id;
        public String name;
        public Date last_activity;
        public int unit_id;
        public String unit_name;
        public int total_alerts;
        public int unacknowledged_alerts;
        public int unit_message_count;
        public boolean enabled;
        public boolean armed;
        public boolean archived;
        public boolean deleted;
        public String vehicle;
        public String cargo;
        public String watchers;
        public String users;
        public String fences;
}