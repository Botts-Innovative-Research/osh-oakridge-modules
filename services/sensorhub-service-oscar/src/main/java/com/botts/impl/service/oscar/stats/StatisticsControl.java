package com.botts.impl.service.oscar.stats;

import com.botts.impl.service.oscar.OSCARSystem;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.*;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.swe.SWEHelper;
import org.vast.util.TimeExtent;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

public class StatisticsControl extends AbstractSensorControl<OSCARSystem> implements IStreamingControlInterfaceWithResult {

    public static final String NAME = "statsRequest";
    public static final String LABEL = "Stats Request";
    public static final String DESCRIPTION = "Control interface to request up to date stats";

    private DataComponent resultDescription;
    private DataComponent commandDescription;

    private StatisticsOutput statsOutput;

    public StatisticsControl(OSCARSystem parentSensor) {
        super(NAME, parentSensor);
        statsOutput = (StatisticsOutput) parentSensor.getOutputs().get(StatisticsOutput.NAME);

        RADHelper fac = new RADHelper();
        resultDescription = fac.createCountStatistics();
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
                .addField("laneUID", fac.createText()
                        .label("Lane Unique Identifiers")
                        .definition(SWEHelper.getPropertyUri("LaneUID"))
                        .description("Semicolon-separated lane identifiers used to scope the statistics")
                        .optional(true))
                .build();
    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {
        DataBlock params = command.getParams();
        return CompletableFuture.supplyAsync(() -> {
            var start = params.getTimeStamp(0);
            var end = params.getTimeStamp(1);
            var laneUIDValue = params.getStringValue(2);
            Set<String> laneUIDs = laneUIDValue == null || laneUIDValue.isBlank() || laneUIDValue.equals("NONE")
                    ? Set.of()
                    : Arrays.stream(laneUIDValue.split(";"))
                            .map(String::trim)
                            .filter(value -> !value.isBlank())
                            .collect(Collectors.toSet());
            var execStartTime = System.currentTimeMillis();

            if ((start == null && end != null) || (start != null && end == null))
                return CommandStatus.rejected(command.getID(), "Both start and end must be null or non-null");

            ICommandResult result;
            if (start != null) {
                // TODO: Return result of createCountStatistics
                DataBlock resultData = resultDescription.createDataBlock();
                statsOutput.populateDataBlock(resultData, 0, start, end, laneUIDs);
                result = CommandResult.withData(resultData);
            } else {
                if (!laneUIDs.isEmpty())
                    return CommandStatus.rejected(command.getID(), "Scoped statistics require both start and end");

                // TODO: Call update for latest site statistics output, and return output observation id
                statsOutput.publishLatestStatistics();
                var latestObsId = statsOutput.waitForLatestObservationId();
                if (latestObsId == null)
                    return CommandStatus.failed(command.getID(), "Could not generate latest statistics observation.");
                result = CommandResult.withObservation(latestObsId);
            }
            return new CommandStatus.Builder()
                    .withResult(result)
                    .withCommand(command.getID())
                    .withStatusCode(ICommandStatus.CommandStatusCode.ACCEPTED)
                    .withExecutionTime(TimeExtent.period(Instant.ofEpochMilli(execStartTime), Instant.now()))
                    .build();
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
