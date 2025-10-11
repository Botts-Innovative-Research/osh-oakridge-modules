package com.botts.impl.system.lane;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.CommandStatus;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.Adjudication;
import java.util.concurrent.CompletableFuture;

public class AdjudicationControl extends AbstractSensorControl<LaneSystem> {

    public static final String NAME = "adjudicationControl";
    public static final String LABEL = "Adjudication Control";
    public static final String DESCRIPTION = "Control interface for adjudicating occupancy events";

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
           try {
               Adjudication adj = new Adjudication.Builder()
                       .feedback(cmdData.getStringValue(0))
                       .adjudicationCode(cmdData.getIntValue(1))
                       .isotopes(cmdData.getStringValue(2))
                       .secondaryInspectionStatus(Adjudication.SecondaryInspectionStatus.valueOf(cmdData.getStringValue(3)))
                       .filePaths(cmdData.getStringValue(4))
                       .occupancyId(cmdData.getStringValue(5))
                       .vehicleId(cmdData.getStringValue(6))
                       .build();

               // Validate obs ID is present
               String occupancyId = adj.getOccupancyId();
               if (occupancyId == null || occupancyId.isBlank())
                   return CommandStatus.failed(command.getID(), "Occupancy ID field must not be blank.");

               // Validate obs ID is valid
               BigId decodedObsId = obsIdEncoder.decodeID(occupancyId);
               if (decodedObsId == null)
                   return CommandStatus.failed(command.getID(), "The provided occupancy ID is invalid.");

               // Validate obs ID is in database
               if (!obsStore.containsKey(decodedObsId))
                   return CommandStatus.failed(command.getID(), "The provided occupancy ID was not found in the database.");

               // Validate obs' data stream matches occupancy datastream
               var obs = obsStore.get(decodedObsId);
               if (obs == null)
                   return CommandStatus.failed(command.getID(), "The occupancy from the provided ID is null");

               // save files to bucket store
               String filePaths = adj.getFilePaths();
               if (filePaths != null && !filePaths.isEmpty()) {
                   // handle file paths to bucket store
               }

               var dsQuery = obsStore.getDataStreams().select(new DataStreamFilter.Builder()
                       .withInternalIDs(obs.getDataStreamID())
                       .withObservedProperties(RADHelper.getRadUri("Occupancy"))
                       .build());
               if (dsQuery.findAny().isEmpty())
                   return CommandStatus.failed(command.getID(), "The provided occupancy ID is not part of an RPM");


               if (adj.getSecondaryInspectionStatus() == null)
                   return CommandStatus.failed(command.getID(), "Please specify a secondary inspection status");

               // HERE U GO KALEN
               BigId dataStreamId = obs.getDataStreamID();
               DataComponent recordStructure = obsStore.getDataStreams().get(new DataStreamKey(dataStreamId)).getRecordStructure();

               // Secondary inspection

               parent.adjudicationOutput.setData(adj);


               var commandId = getParentProducer().getParentHub().getIdEncoders().getCommandIdEncoder().encodeID(command.getID());

               var result = obs.getResult();
               var adjIdCount = result.getIntValue(9);

               adjIdCount = adjIdCount + 1;


               // increment the count by one
               result.setIntValue(9, adjIdCount);


               // need to refresh the size of the data array

               result.setStringValue(9 + adjIdCount, commandId);

               String systemUID = getParentProducer().getParentSystemUID() != null ?
                       getParentProducer().getParentSystemUID() :
                       getParentProducer().getUniqueIdentifier();
               var obsDb = getParentProducer().getParentHub().getSystemDriverRegistry().getDatabase(systemUID);

               obsDb.getObservationStore().put(decodedObsId, obs);

               return CommandStatus.accepted(command.getID());
           } catch (Exception e) {
                return CommandStatus.failed(command.getID(), "Failed to accept command: " + e.getMessage());
           }
        });
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandStructure;
    }

}
