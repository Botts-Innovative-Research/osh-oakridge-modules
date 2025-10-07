package com.botts.impl.service.oscar.stats;

import com.botts.impl.service.oscar.OSCARSystem;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.*;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.swe.SWEHelper;

import java.util.concurrent.CompletableFuture;

public class StatisticsControl extends AbstractSensorControl<OSCARSystem> implements IStreamingControlInterfaceWithResult {

    public static final String NAME = "statsRequest";
    public static final String LABEL = "Stats Request";
    public static final String DESCRIPTION = "Control interface to request up to date stats";

    private DataComponent resultDescription;
    private DataComponent commandDescription;

    private StatisticsOutput statsOutput;

    protected StatisticsControl(OSCARSystem parentSensor, StatisticsOutput statsOutput) {
        super(NAME, parentSensor);
        this.statsOutput = statsOutput;

        RADHelper fac = new RADHelper();
        resultDescription = fac.createSiteStatistics();
        commandDescription = fac.createRecord()
                .name(NAME)
                .label(LABEL)
                .description(DESCRIPTION)
                .addField("startDateTime", fac.createTime()
                        .definition(SWEHelper.getPropertyUri("StartDateTime"))
                        .withIso8601Format()
                        .description("Start datetime (ISO 8601)")
                        .optional(true))
                .addField("endDateTime", fac.createTime()
                        .definition(SWEHelper.getPropertyUri("EndDateTime"))
                        .withIso8601Format()
                        .description("End datetime (ISO 8601)")
                        .optional(true))
                .build();
    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {
        DataBlock params = command.getParams();
        return CompletableFuture.supplyAsync(() -> {
            var start = params.getTimeStamp(0);
            var end = params.getTimeStamp(1);

            if (start != null && end != null) {

            }

        });
    }

    @Override
    public DataComponent getResultDescription() {
        return resultDescription;
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDescription;
    }
}
