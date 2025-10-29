package com.botts.impl.system.lane;

import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.*;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.Adjudication;
import org.sensorhub.impl.utils.rad.model.Occupancy;
import org.vast.data.DataArrayImpl;
import org.vast.swe.SWEHelper;
import org.vast.util.TimeExtent;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class AdjudicationControl extends AbstractSensorControl<LaneSystem> implements IStreamingControlInterfaceWithResult {

    public static final String NAME = "adjudicationControl";
    public static final String LABEL = "Adjudication Control";
    public static final String DESCRIPTION = "Control interface for adjudicating occupancy events";

    public static final String RES_NAME = "adjudicationResult";
    public static final String RES_LABEL = "Adjudication Result";
    public static final String RES_DESCRIPTION = "Adjudications of occupancy events";

    DataComponent commandStructure;
    DataComponent resultStructure;
    RADHelper fac;
    IdEncoder obsIdEncoder;
    IObsStore obsStore;

    protected AdjudicationControl(LaneSystem parentSensor) {
        super(NAME, parentSensor);

        var hub = getParentProducer().getParentHub();
        obsIdEncoder = hub.getIdEncoders().getObsIdEncoder();

        fac = new RADHelper();

        this.commandStructure = fac.createAdjudicationRecord();

        this.resultStructure = fac.createRecord()
                .name(RES_NAME)
                .label(RES_LABEL)
                .description(RES_DESCRIPTION)
                .definition(RADHelper.getRadUri("Adjudication"))
                .addField("username", fac.createText()
                        .label("Username")
                        .definition(SWEHelper.getPropertyUri("Username"))
                        .build())
                .addAllFields(fac.createAdjudicationRecord())
                .build();
    }

    public void setObsStore(IObsStore obsStore) {
        this.obsStore = obsStore;
    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {
        DataBlock cmdData = command.getParams();
        Instant start = Instant.now();
        return CompletableFuture.supplyAsync(() -> {
           try {
               var adj = Adjudication.toAdjudication(cmdData);

               // Validate obs ID is present
               String occupancyObsId = adj.getOccupancyObsId();
               if (occupancyObsId == null || occupancyObsId.isBlank())
                   return CommandStatus.failed(command.getID(), "Occupancy Observation ID field must not be blank.");

               // Validate obs ID is valid
               BigId decodedObsId = obsIdEncoder.decodeID(occupancyObsId);
               if (decodedObsId == null)
                   return CommandStatus.failed(command.getID(), "The provided occupancy observation ID is invalid.");

               if (obsStore == null)
                   return CommandStatus.failed(command.getID(), "Please attach this lane to a database, or restart the lane");

               // Validate obs ID is in database
               if (!obsStore.containsKey(decodedObsId))
                   return CommandStatus.failed(command.getID(), "The provided occupancy observation ID was not found in the database.");

               // Validate obs' data stream matches occupancy datastream
               var obs = obsStore.get(decodedObsId);
               if (obs == null)
                   return CommandStatus.failed(command.getID(), "The occupancy from the provided ID is null");

               var ds = obsStore.getDataStreams().get(new DataStreamKey(obs.getDataStreamID()));
               if (ds == null)
                   return CommandStatus.failed(command.getID(), "The provided occupancy observation ID is not part of an RPM");

               var dsDef = ds.getRecordStructure().getDefinition();
               var expectedDef = RADHelper.getRadUri("Occupancy");
               if (!Objects.equals(expectedDef, dsDef))
                   return CommandStatus.failed(command.getID(), "The provided occupancy observation ID is not part of an RPM");

               if (adj.getSecondaryInspectionStatus() == null)
                   return CommandStatus.failed(command.getID(), "Please specify a secondary inspection status");

               // ----------------------------------------------------------------------------------------------------------
               // update the occupancy adjudication command IDs array with latest command id
               var commandId = getParentProducer().getParentHub().getIdEncoders().getCommandIdEncoder().encodeID(command.getID());
               var result = obs.getResult();
               var occupancy = Occupancy.toOccupancy(result);

               occupancy.addAdjudicatedId(commandId);
               DataBlock newOccupancyResult = Occupancy.fromOccupancy(occupancy);
               obs.getResult().setUnderlyingObject(newOccupancyResult.getUnderlyingObject());
               obs.getResult().updateAtomCount();

               String systemUID = getParentProducer().getParentSystemUID() != null ?
                       getParentProducer().getParentSystemUID() :
                       getParentProducer().getUniqueIdentifier();
               var obsDb = getParentProducer().getParentHub().getSystemDriverRegistry().getDatabase(systemUID);

               obsDb.getObservationStore().put(decodedObsId, obs);

               DataBlock resultData = createResultData(adj, command.getSenderID());

               return new CommandStatus.Builder()
                       .withCommand(command.getID())
                       .withStatusCode(ICommandStatus.CommandStatusCode.COMPLETED)
                       .withResult(CommandResult.withData(resultData))
                       .withExecutionTime(TimeExtent.endNow(start))
                       .build();
           } catch (Exception e) {
               return CommandStatus.failed(command.getID(), "Failed to accept command: " + e.getMessage());
           }
        });
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandStructure;
    }

    @Override
    public DataComponent getResultDescription() {
        return resultStructure;
    }

    private DataBlock createResultData(Adjudication adjudication, String username) {
        resultStructure.clearData();
        DataBlock dataBlock = resultStructure.createDataBlock();
        dataBlock.updateAtomCount();
        resultStructure.setData(dataBlock);

        int index = 0;

        dataBlock.setStringValue(index++, username == null ? "Unknown" : username);
        dataBlock.setStringValue(index++, adjudication.getFeedback());
        dataBlock.setIntValue(index++, adjudication.getAdjudicationCode());

        int isotopeCount =  adjudication.getIsotopes().size();
        dataBlock.setIntValue(index++, isotopeCount);

        var isotopesArray =((DataArrayImpl) resultStructure.getComponent("isotopes"));
        isotopesArray.updateSize();
        dataBlock.updateAtomCount();

        for (int i = 0; i < isotopeCount; i++)
            dataBlock.setStringValue(index++, adjudication.getIsotopes().get(i));

        dataBlock.setStringValue(index++, adjudication.getSecondaryInspectionStatus().toString());

        int filePathCount = adjudication.getFilePaths().size();
        dataBlock.setIntValue(index++, filePathCount);

        var filePathsArray =((DataArrayImpl) resultStructure.getComponent("filePaths"));
        filePathsArray.updateSize();
        dataBlock.updateAtomCount();

        for (int i = 0; i < filePathCount; i++)
            dataBlock.setStringValue(index++, adjudication.getFilePaths().get(i));


        dataBlock.setStringValue(index++, adjudication.getOccupancyObsId());
        dataBlock.setStringValue(index, adjudication.getVehicleId());

        return dataBlock;
    }
}
