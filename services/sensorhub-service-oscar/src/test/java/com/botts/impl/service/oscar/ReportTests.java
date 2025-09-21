package com.botts.impl.service.oscar;

import com.botts.impl.service.oscar.reports.RequestReportControl;
import com.botts.impl.service.oscar.reports.helpers.ReportCmdType;
import net.opengis.swe.v20.DataChoice;
import org.junit.Test;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.IStreamingControlInterface;

import java.time.Instant;

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
}
