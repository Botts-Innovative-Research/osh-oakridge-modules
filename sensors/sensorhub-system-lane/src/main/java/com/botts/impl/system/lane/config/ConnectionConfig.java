package com.botts.impl.system.lane.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Connection Configuration
 * @author Kalyn Stricklin
 * @since May 13, 2025
 */

public class ConnectionConfig {

    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.REMOTE_ADDRESS)
    @DisplayInfo(label = "Remote Host")
    public String remoteHost;

    /**
     * Package-private shadow of the last endpoint synchronized between {@code LaneOptionsConfig}
     * and the generated child sensor {@code ModuleConfig}. Persisted by Gson in {@code config.json}
     * while remaining invisible in SensorHub's Vaadin {@code MyBeanItem} form builder so manual
     * edits in either {@code config.json} block or either Admin UI form can be detected cleanly.
     */
    String lastSyncedEndpoint;

    public String lastSyncedEndpoint() {
        return lastSyncedEndpoint;
    }

    public void markSyncedEndpoint(String endpoint) {
        this.lastSyncedEndpoint = endpoint;
    }

}
