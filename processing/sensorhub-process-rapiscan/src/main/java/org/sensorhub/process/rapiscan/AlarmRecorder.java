package org.sensorhub.process.rapiscan;

import net.opengis.swe.v20.*;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IDatabaseRegistry;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.processing.ISensorHubProcess;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.DataBlockMixed;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class AlarmRecorder extends ExecutableProcessImpl implements ISensorHubProcess {
    public static final OSHProcessInfo INFO = new OSHProcessInfo("alarmrecorder", "Alarm data recording process", null, AlarmRecorder.class);
    ISensorHub hub;
    IDatabaseRegistry registry;
    DataRecord occupancyInput;
    Text dbInputParam;
    DataRecord neutronEntry;
    DataRecord gammaEntry;
    DataComponent video1;

    enum EntryType {
        NEUTRON, GAMMA
    }

    public AlarmRecorder() {
        super(INFO);

        RADHelper radHelper = new RADHelper();

        inputData.add("occupancy", occupancyInput = radHelper.createRecord()
                .label("Occupancy")
                .definition(RADHelper.getRadUri("occupancy"))
                .addField("Timestamp", radHelper.createPrecisionTimeStamp())
                .addField("OccupancyCount", radHelper.createOccupancyCount())
                .addField("StartTime", radHelper.createOccupancyStartTime())
                .addField("EndTime", radHelper.createOccupancyEndTime())
                .addField("NeutronBackground", radHelper.createNeutronBackground())
                .addField("GammaAlarm",
                        radHelper.createBoolean()
                                .name("gamma-alarm")
                                .label("Gamma Alarm")
                                .definition(RADHelper.getRadUri("gamma-alarm")))
                .addField("NeutronAlarm",
                        radHelper.createBoolean()
                                .name("neutron-alarm")
                                .label("Neutron Alarm")
                                .definition(RADHelper.getRadUri("neutron-alarm")))
                .build());

        outputData.add("neutronEntry", neutronEntry = radHelper.createRecord()
                        .label("Neutron Scan")
                        .definition(RADHelper.getRadUri("neutron-scan"))
                        .addField("SamplingTime", radHelper.createPrecisionTimeStamp())
                        .addField("Neutron1", radHelper.createNeutronGrossCount())
                        .addField("Neutron2", radHelper.createNeutronGrossCount())
                        .addField("Neutron3", radHelper.createNeutronGrossCount())
                        .addField("Neutron4", radHelper.createNeutronGrossCount())
                        .addField("AlarmState",
                                radHelper.createCategory()
                                        .name("Alarm")
                                        .label("Alarm")
                                        .definition(RADHelper.getRadUri("alarm"))
                                        .addAllowedValues("Alarm", "Background", "Scan", "Fault - Neutron High"))
                        .build());

        outputData.add("gammaEntry", gammaEntry = radHelper.createRecord()
                        .label("Gamma Scan")
                        .definition(RADHelper.getRadUri("gamma-scan"))
                        .addField("SamplingTime", radHelper.createPrecisionTimeStamp())
                        .addField("Gamma1", radHelper.createGammaGrossCount())
                        .addField("Gamma2", radHelper.createGammaGrossCount())
                        .addField("Gamma3", radHelper.createGammaGrossCount())
                        .addField("Gamma4", radHelper.createGammaGrossCount())
                        .addField("AlarmState",
                                radHelper.createCategory()
                                        .name("Alarm")
                                        .label("Alarm")
                                        .definition(RADHelper.getRadUri("alarm"))
                                        .addAllowedValues("Alarm", "Background", "Scan", "Fault - Gamma High", "Fault - Gamma Low"))
                        .build());

        VideoCamHelper vidHelper = new VideoCamHelper();
        outputData.add("video1", video1 = vidHelper.newVideoOutputMJPEG("video1", 640, 480).getElementType());

        paramData.add("databaseInput", dbInputParam = radHelper.createText()
                .label("Database Input")
                .description("Database to query historical results")
                .definition(SWEHelper.getPropertyUri("Database"))
                .value("")
                .build());
    }

    @Override
    public void execute() throws ProcessException {
        // Only populate data entry if alarm is triggered
        List<IObsData> alarmingData;
        List<IObsData> videoData;
        long startFrom = (long) occupancyInput.getComponent("StartTime").getData().getDoubleValue();
        long endFrom = (long) occupancyInput.getComponent("EndTime").getData().getDoubleValue();
        Instant start = Instant.ofEpochSecond(startFrom);
        Instant end = Instant.ofEpochSecond(endFrom);

        if(occupancyInput.getComponent("GammaAlarm").getData().getBooleanValue()) {
            System.out.println("Results from occupancy");

            alarmingData = getDataFromInterval(start, end, dbInputParam.getData().getStringValue(), EntryType.GAMMA);
            videoData = getVideoFromInterval(start, end, dbInputParam.getData().getStringValue());

            try {
                publishVideoOutput(videoData);
                publishEntryOutput(alarmingData, EntryType.GAMMA);
            } catch (InterruptedException | DataStoreException e) {
                throw new RuntimeException(e);
            }
        }

        if(occupancyInput.getComponent("NeutronAlarm").getData().getBooleanValue()) {
            alarmingData = getDataFromInterval(start, end, dbInputParam.getData().getStringValue(), EntryType.NEUTRON);
            videoData = getVideoFromInterval(start, end, dbInputParam.getData().getStringValue());

            try {
                publishVideoOutput(videoData);
                publishEntryOutput(alarmingData, EntryType.NEUTRON);
            } catch (InterruptedException | DataStoreException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<IObsData> getDataFromInterval(Instant start, Instant end, String dbModuleID, EntryType entryType) {
        String outputName = "";
        switch(entryType) {
            case GAMMA: outputName = "Gamma Scan";
            break;
            case NEUTRON: outputName = "Neutron Scan";
            break;
        }
        DataStreamFilter dsFilter = new DataStreamFilter.Builder()
                .withOutputNames(outputName)
                .build();

        ObsFilter filter = new ObsFilter.Builder()
                .withDataStreams(dsFilter)
                .withPhenomenonTimeDuring(start, end).build();

        var obsDb = getRegistry()
                //"29f2b677-95b1-4499-8e5b-459839ec3eb6"
                .getObsDatabaseByModuleID(dbModuleID)
                .getObservationStore()
                .select(filter);

        return obsDb.collect(Collectors.toList());
    }

    private void publishEntryOutput(List<IObsData> blockList, EntryType entryType) throws InterruptedException, DataStoreException {
        DataRecord output = null;
        switch (entryType) {
            case GAMMA: {
                output = gammaEntry;
            }
            break;
            case NEUTRON: {
                output = neutronEntry;
            }
            break;
        }
        if(!blockList.isEmpty()) {
            DataBlockMixed entry = (DataBlockMixed) output.getData();
            for (int i = 0; i < blockList.size(); i++) {
                entry.setDoubleValue(0, blockList.get(i).getResult().getDoubleValue(0));
                entry.setIntValue(1, blockList.get(i).getResult().getIntValue(1));
                entry.setIntValue(2, blockList.get(i).getResult().getIntValue(2));
                entry.setIntValue(3, blockList.get(i).getResult().getIntValue(3));
                entry.setIntValue(4, blockList.get(i).getResult().getIntValue(4));
                entry.setStringValue(5, blockList.get(i).getResult().getStringValue(5));

                output.setData(entry);

                publishData();
                publishData(output.getName());

                System.out.println("Entry: " + entry + " DataBlock: " + blockList.get(i).getResult());
            }
        }
    }

    private List<IObsData> getVideoFromInterval(Instant start, Instant end, String dbModuleID) {
        String outputName = "video";
        DataStreamFilter dsFilter = new DataStreamFilter.Builder()
                .withOutputNames(outputName)
                .build();

        ObsFilter filter = new ObsFilter.Builder()
                .withDataStreams(dsFilter)
                .withPhenomenonTimeDuring(start, end).build();

        var obsDb = getRegistry()
                .getObsDatabaseByModuleID(dbModuleID)
                .getObservationStore()
                .select(filter);

        return obsDb.collect(Collectors.toList());
    }

    private void publishVideoOutput(List<IObsData> blockList) {
        if(!blockList.isEmpty()) {
            DataBlockMixed videoOutput = (DataBlockMixed) outputData.getComponent("video1");
            for(int i = 0; i < blockList.size(); i++) {
                System.out.println(blockList.get(i).getResult());
            }
        }
    }

    private IDatabaseRegistry getRegistry() {
        if(hub != null) {
            registry = hub.getDatabaseRegistry();
        }
        return registry;
    }

    @Override
    public void setParentHub(ISensorHub hub) {
        this.hub = hub;
    }
}
