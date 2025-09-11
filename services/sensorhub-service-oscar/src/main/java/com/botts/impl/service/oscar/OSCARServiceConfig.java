package com.botts.impl.service.oscar;

import com.botts.impl.service.oscar.siteinfo.SiteDiagramConfig;
import com.botts.impl.service.oscar.video.VideoRetentionConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.service.ServiceConfig;

public class OSCARServiceConfig extends ServiceConfig {

    public String spreadsheetConfigPath;

    public SiteDiagramConfig siteDiagramConfig;

    public VideoRetentionConfig videoRetentionConfig;

    @DisplayInfo.Required
    @DisplayInfo(label = "Node ID", desc = "Unique identifier of this OSCAR node")
    public String nodeId;

    @DisplayInfo.Required
    @DisplayInfo(desc = "Database connected to this OSCAR service")
    public String databaseId;

}
