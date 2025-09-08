package org.sensorhub.impl.sensor.ffmpeg.controls;

import net.opengis.swe.v20.Boolean;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.*;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.vast.swe.SWEHelper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class FileControl<FFmpegConfigType extends FFMPEGConfig> extends AbstractSensorControl<FFMPEGSensorBase<FFmpegConfigType>> {
    public static final String SENSOR_CONTROL_NAME = "ffmpegFileControl";
    private static final String SENSOR_CONTROL_LABEL = "FFmpeg File Control";
    public static final String CMD_OPEN_FILE = "startFile";
    public static final String CMD_CLOSE_FILE = "endFile";
    public static final String FILE_IO = "fileIO";
    DataRecord commandData;
    String fileName = "";

    public FileControl(FFMPEGSensorBase<FFmpegConfigType> sensor) { super(SENSOR_CONTROL_NAME, sensor); }

    public void init() {
        SWEHelper fac = new SWEHelper();

        commandData = fac.createRecord()
                .name(getName())
                .label(SENSOR_CONTROL_LABEL)
                .addField(FILE_IO, fac.createChoice()
                        .label("File I/O")
                        .addItem(CMD_OPEN_FILE, fac.createText()
                                .label("Start File")
                                .build())
                        .addItem(CMD_CLOSE_FILE, fac.createBoolean()
                                .value(true)
                                .label("Save File")
                                .build())
                        .build())
                .build();
    }


    @Override
    public DataComponent getCommandDescription() {
        return commandData;
    }

    @Override
    protected boolean execCommand(DataBlock cmdData) throws CommandException {
        boolean commandStatus = true;
        DataRecord commandData = this.commandData.copy();
        commandData.setData(cmdData);
        var selected = ((DataChoice)commandData.getComponent(0)).getSelectedItem();
        if (selected == null)
            return true;

        if (selected.getName().equals(CMD_OPEN_FILE)) {
            if (!fileName.isEmpty())
                return true;
            fileName = selected.getData().getStringValue();
            try {
                this.parentSensor.getProcessor().openFile(fileName);
                this.parentSensor.reportStatus("Writing to file: " + fileName);
            } catch (Exception e) {
                commandStatus = false;
            }
        } else if (selected.getName().equals(CMD_CLOSE_FILE)) {
            if (fileName.isEmpty()) { return true; }
            Boolean item = (Boolean) selected;
            try {
                this.parentSensor.getProcessor().closeFile();
                this.parentSensor.reportStatus("Stopped writing to file: " + fileName);

                // Delete file if we do not want to save
                if (!item.getValue()) {
                    Files.deleteIfExists(Paths.get("./" + fileName));
                }
                fileName = "";
            } catch (Exception e) {
                commandStatus = false;
            }
        } else {
            throw new CommandException("Invalid Command");
        }
        return commandStatus;
    }
}
