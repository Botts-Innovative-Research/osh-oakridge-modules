package com.botts.impl.service.oscar;

import com.botts.impl.service.oscar.reports.RequestReportControl;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import com.botts.impl.service.oscar.reports.helpers.Utils;
import net.opengis.swe.v20.DataChoice;
import org.junit.Test;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.IStreamingControlInterface;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.utils.rad.RADHelper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;

public class ReportTests {
    OSCARServiceModule module = new OSCARServiceModule();
    OSCARSystem system = new OSCARSystem("nodeId");
    RequestReportControl requestReportControl = new RequestReportControl(system, module);


    @Test
    public void generateLaneReport() throws Exception {

        DataChoice cmdChoice = (DataChoice) module.reportControl.getCommandDescription().copy();

        cmdChoice.assignNewDataBlock();
        cmdChoice.setSelectedItem(ReportCmdType.LANE.name());
        cmdChoice.getSelectedItem().getData().setTimeStamp(0,Instant.now());
        cmdChoice.getSelectedItem().getData().setTimeStamp(1, Instant.now());
        cmdChoice.getSelectedItem().getData().setStringValue(2, "urn:osh:system:lane1");

        requestReportControl.submitCommand(new CommandData(1, cmdChoice.getData()));
    }

    @Test
    public void generateSiteReport() throws Exception {

        DataChoice cmdChoice = (DataChoice) module.reportControl.getCommandDescription().copy();

        cmdChoice.assignNewDataBlock();
        cmdChoice.setSelectedItem(ReportCmdType.RDS_SITE.name());
        cmdChoice.getSelectedItem().getData().setTimeStamp(0,Instant.now());
        cmdChoice.getSelectedItem().getData().setTimeStamp(1, Instant.now());

        requestReportControl.submitCommand(new CommandData(2, cmdChoice.getData()));
    }


    @Test
    public void generateAdjudicationReport() throws Exception {


        DataChoice cmdChoice = (DataChoice) module.reportControl.getCommandDescription().copy();

        cmdChoice.assignNewDataBlock();
        cmdChoice.setSelectedItem(ReportCmdType.ADJUDICATION.name());
        cmdChoice.getSelectedItem().getData().setTimeStamp(0,Instant.now());
        cmdChoice.getSelectedItem().getData().setTimeStamp(1, Instant.now());

        requestReportControl.submitCommand(new CommandData(3, cmdChoice.getData()));
    }

//    @Test
//    public void generateEventReport() throws Exception {
//
//
//        DataChoice cmdChoice = (DataChoice) module.reportControl.getCommandDescription().copy();
//
//        cmdChoice.assignNewDataBlock();
//        cmdChoice.setSelectedItem(ReportCmdType.EVENT.name());
//        cmdChoice.getSelectedItem().getData().setTimeStamp(0,Instant.now());
//        cmdChoice.getSelectedItem().getData().setTimeStamp(1, Instant.now());
//
//        requestReportControl.submitCommand(new CommandData(1, cmdChoice.getData()));
//    }


    public static Instant begin = Instant.now().minus(1, ChronoUnit.YEARS);
    public static Instant end = Instant.now();

    @Test
    public void compareCounts() throws Exception {
        long gammaCount1 = iterateCount();
        long gammaCount2 = predicateCount();

        assertEquals("Counts should be equal", gammaCount1, gammaCount2);
    }


    public long iterateCount() throws Exception {
        var query = module.getParentHub().getDatabaseRegistry().getFederatedDatabase().getObservationStore().select(new ObsFilter.Builder()
                .withDataStreams(new DataStreamFilter.Builder()
                        .withObservedProperties(RADHelper.DEF_OCCUPANCY)
                        .withValidTimeDuring(begin, end)
                        .build())
                .build()).iterator();

        var gammaCount = 0;
        while (query.hasNext()) {
            var entry  = query.next();

            var result = entry.getResult();

            var gamma = result.getBooleanValue(5);
            var neutron = result.getBooleanValue(6);

            if(gamma && !neutron){
                gammaCount++;
            }
        }
        return gammaCount;
    }


    public long predicateCount() throws Exception {
        Predicate<IObsData> gammaPredicate = (obsData) -> {return obsData.getResult().getBooleanValue(5) && !obsData.getResult().getBooleanValue(6);};
        long gammaAlarmCount = Utils.countObservations(new String[]{RADHelper.DEF_OCCUPANCY}, module, gammaPredicate, begin, end);

        return gammaAlarmCount;
    }


}
