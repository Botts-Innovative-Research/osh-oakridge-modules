package com.botts.impl.sensor.rapiscan;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.DataChoiceImpl;

import javax.xml.crypto.Data;

public class RapiscanControl extends AbstractSensorControl<RapiscanSensor> {
    DataChoice commandData;

    protected RapiscanControl(RapiscanSensor parentSensor) {
        super("updateTCP", parentSensor);
    }


    @Override
    public DataComponent getCommandDescription() {
        return commandData;
    }

    @Override
    protected boolean execCommand(DataBlock commandBlock)throws CommandException {
        boolean cmd =false;

        DataChoice commandMsg = commandData.copy();
        commandMsg.setData(commandBlock);

        DataComponent component= ((DataChoiceImpl)commandMsg).getSelectedItem();
        String itemName = component.getName();
        DataBlock data =component.getData();
        int itemVal = data.getIntValue();

        try{
            if(itemName.equals("COMMUNICATION_PROVIDER_TCP")){

            }
            cmd = true;

        }catch(Exception e){
            getLogger().error("Failed sending command: "+ e.getMessage());
        }
        return cmd;
    }

    protected void init(){
        RADHelper radHelper = new RADHelper();
        commandData = radHelper.createChoice()
                .name("updateTCP")
                .label("TCP Updater")
                .description("Command to update TCP communication protocal")
                .updatable(true)
                .build();

        commandData.setName("updateTCP");

        commandData.addItem("laneIndex", radHelper.createCount()
                .label("Lane Index")
                .definition(RADHelper.getRadUri("LaneIndex"))
                .value(0)
                .build());
        commandData.addItem("RemotePort", radHelper.createText()
                .label("Remote Port")
                .definition(RADHelper.getRadUri("LaneIndex"))
                .value("1600")
                .build());
        commandData.addItem("RemoteIP", radHelper.createText()
                .label("Remote IP")
                .definition(RADHelper.getRadUri("LaneIndex"))
                .value("192.168.1.69")
                .build());


    }
}
