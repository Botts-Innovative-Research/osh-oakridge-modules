package com.botts.impl.sensor.drivername;

import net.opengis.swe.v20.*;
import com.botts.sensorhub.impl.zwave.comms.ZwaveCommService;

import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveMultiLevelSwitchCommandClass;
import org.openhab.binding.zwave.internal.protocol.transaction.ZWaveCommandClassTransactionPayload;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class Control extends AbstractSensorControl<ZWaveSensor>  {

    public enum LightState {
        ON,
        OFF,
    }
    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this control
     */
    public Control(ZWaveSensor parentSensor)
    {
        super("Control", parentSensor);
    }

    public int configNodeId;
    public int zControllerId;
    public ZwaveCommService commService = this.parentSensor.commService;
    public Config.DRIVERNAMESensorDriverConfigurations sensorConfig =
            new Config().drivernameSensorDriverConfigurations;
    private DataRecord commandDataStruct;

    private void defineRecordStructure() {
        GeoPosHelper factory = new GeoPosHelper();

        commandDataStruct = factory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("DimmableLightbulb"))
                .label("DimmableLightbulb")
                .description("Zwave Controlled Smart Dimmable LED Lightbulb")
                .addField("LightStatus",
                        factory.createCategory()
                                .name("Light Status")
                                .label("Light Status")
                                .definition(SWEHelper.getPropertyUri("LightStatus"))
                                .description("The status of the lightbulb")
                                .addAllowedValues(
                                        LightState.OFF.name(),
                                        LightState.ON.name()
                                )
                                .value(LightState.OFF.name())
                                .build())
                .addField("DimmerLevel",
                        factory.createQuantityRange()
                                .name("Dimmer Level")
                                .label("Dimmer Level")
                                .definition(SWEHelper.getPropertyUri("DimmerLevel"))
                                .description("The dimness of the lightbulb")
                                .addAllowedInterval(0, 99)
                                .updatable(true)
                                .build())
                .build();
    }

    public void init() {

        defineRecordStructure();

    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {
        configNodeId = sensorConfig.nodeID;
        zControllerId = sensorConfig.controllerID;

        try {

            DataRecord commandData = commandDataStruct.copy();
            commandData.setData(command);

            DataComponent lightStatus = commandData.getField("LightStatus");
            LightState status = LightState.valueOf(lightStatus.getData().getStringValue());
            String statusStr = status.toString();

            DataComponent dimmerLevel = commandData.getField("DimmerLevel");
            DataBlock data = dimmerLevel.getData();
            int dimData = data.getIntValue();

            setLightState(statusStr, dimData);
            setDimmerLevel(dimData);

        } catch (Exception e) {

            throw new CommandException("Failed to command the SearchlightSensor module: ", e);
        }

        return true;
    }

    public void setLightState(String value, int dimLevel){
        int lightStatus = 0;
        int dimmerLevel;

        dimmerLevel = dimLevel;

        if (value == "ON") {
            lightStatus = 99;
        } else if (value == "OFF"){
            lightStatus = 0;
        } else if (value == null) {
            lightStatus = dimmerLevel;
        }
        ZWaveMultiLevelSwitchCommandClass zWaveMultiLevelSwitchCommandClass =
                (ZWaveMultiLevelSwitchCommandClass) commService.getZWaveNode(configNodeId).getCommandClass((ZWaveCommandClass.CommandClass.COMMAND_CLASS_SWITCH_MULTILEVEL));
        ZWaveCommandClassTransactionPayload sendValue = zWaveMultiLevelSwitchCommandClass.setValueMessage(lightStatus);
        commService.sendConfigurations(sendValue);
    }

    public void setDimmerLevel(int level){
        int dimmerLevel;

        dimmerLevel = level;

        ZWaveMultiLevelSwitchCommandClass zWaveMultiLevelSwitchCommandClass =
                (ZWaveMultiLevelSwitchCommandClass) commService.getZWaveNode(configNodeId).getCommandClass((ZWaveCommandClass.CommandClass.COMMAND_CLASS_SWITCH_MULTILEVEL));
        ZWaveCommandClassTransactionPayload sendValue = zWaveMultiLevelSwitchCommandClass.setValueMessage(dimmerLevel);
        commService.sendConfigurations(sendValue);
    }

    public void stop()
    {
        // TODO Auto-generated method stub

    }

}
