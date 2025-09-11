package com.botts.impl.service.oscar;

import org.sensorhub.impl.sensor.AbstractSensorDriver;

public class OSCARSystem extends AbstractSensorDriver {

    public static String NAME = "OSCAR System";
    public static String DESCRIPTION = "System used for performing OSCAR operations";
    public static String UID = "urn:ornl:oscar:system:";

    protected OSCARSystem(String nodeId) {
        super(UID + nodeId, NAME);
    }

    @Override
    public String getName() {
        return getShortID();
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public boolean isConnected() {
        return true;
    }
}
