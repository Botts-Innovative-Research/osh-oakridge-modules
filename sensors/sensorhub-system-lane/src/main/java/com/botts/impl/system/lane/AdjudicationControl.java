package com.botts.impl.system.lane;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.command.CommandStatus;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.Adjudication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class AdjudicationControl extends AbstractSensorControl<LaneSystem> {

    public static final String NAME = "adjudicationControl";
    public static final String LABEL = "Adjudication Control";
    public static final String DESCRIPTION = "Control interface for adjudicating occupancy events";

    private static final Logger logger = LoggerFactory.getLogger(AdjudicationControl.class);

    DataComponent commandStructure;

    RADHelper fac;

    IdEncoder obsIdEncoder;
    IObsStore obsStore;

    protected AdjudicationControl(LaneSystem parentSensor) {
        super(NAME, parentSensor);

        var hub = getParentProducer().getParentHub();
        obsIdEncoder = hub.getIdEncoders().getObsIdEncoder();
        obsStore = hub.getSystemDriverRegistry().getDatabase(getParentProducer().getUniqueIdentifier()).getObservationStore();

        fac = new RADHelper();

        this.commandStructure = fac.createAdjudicationRecord();
    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {
        DataBlock cmdData = command.getParams();
        return CompletableFuture.supplyAsync(() -> {
           ICommandStatus status;
           try {
               // TODO: Ask about secondary inspection
               Adjudication adj = new Adjudication.Builder()
                       .feedback(cmdData.getStringValue(0))
                       .adjudicationCode(cmdData.getIntValue(1))
                       .isotopes(cmdData.getStringValue(2))
                       // TODO: Maybe enum
                       .secondaryInspectionStatus(cmdData.getStringValue(3))
                       .filePaths(cmdData.getStringValue(4))
                       .occupancyId(cmdData.getStringValue(5))
                       .vehicleId(cmdData.getStringValue(6))
                       .build();

               // TODO validate each
               String occupancyId = adj.getOccupancyId();
               if (occupancyId == null || occupancyId.isBlank())
                   return CommandStatus.failed(command.getID(), "Occupancy ID field must not be blank.");

               BigId decodedObsId = obsIdEncoder.decodeID(occupancyId);
               if (decodedObsId == null)
                   return CommandStatus.failed(command.getID(), "The provided occupancy ID is invalid.");

               if (!obsStore.containsKey(decodedObsId))
                   return CommandStatus.failed(command.getID(), "The provided occupancy ID was not found in the database.");

               var obs = obsStore.get(decodedObsId);
               obs.get


               // Secondary inspection

               // Maybe use adjudication statuses to represent secondary inspection
               // REQUESTED, COMPLETED, NONE

               // When REQUESTED, then occupancy adjudicated == false
               //


           } catch (Exception e) {

           }

            return status;
        });
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandStructure;
    }

}
