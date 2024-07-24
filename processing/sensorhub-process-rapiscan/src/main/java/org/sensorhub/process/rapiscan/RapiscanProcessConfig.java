package org.sensorhub.process.rapiscan;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.database.IDatabase;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.api.sensor.ISensorModule;

public class RapiscanProcessConfig extends ProcessConfig {

    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "process001";

    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.SYSTEM_UID)
    @DisplayInfo(label = "Rapiscan Datastream Source", desc = "Datasource to read occupancy data")
    public String datastreamUID;

    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.MODULE_ID)
    @DisplayInfo.ModuleType(IDatabase.class)
    @DisplayInfo(label = "Input Database", desc = "Module ID of system database to query")
    public String databaseModuleID;


}
