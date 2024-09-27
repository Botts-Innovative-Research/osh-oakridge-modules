package com.botts.impl.sensor.rapiscan.eml;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.sensor.rapiscan.output.GammaThresholdOutput;
import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.api.ERNIE_lane;
import gov.llnl.ernie.api.Results;
import gov.llnl.utility.io.ReaderException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.IEventListener;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


public class EMLService implements IEventListener {

    private ERNIE_lane ernieLane;
    RapiscanSensor parentSensor;
    List<String> scanDataList;
    String latestGammaBackground;
    String latestNeutronBackground;

    public EMLService(RapiscanSensor parentSensor) {
        this.parentSensor = parentSensor;
        this.ernieLane = new ERNIE_lane(
                parentSensor.getConfiguration().name,
                parentSensor.getConfiguration().laneID,
                parentSensor.getConfiguration().emlConfig.isCollimated,
                parentSensor.getConfiguration().emlConfig.laneWidth,
                parentSensor.getConfiguration().setupGammaConfig.intervals,
                parentSensor.getConfiguration().setupGammaConfig.occupancyHoldin
        );
        this.scanDataList = new ArrayList<>();
    }

    private void updateErnieSetup(int intervals, int holdin) {
        this.ernieLane = new ERNIE_lane(
                parentSensor.getConfiguration().name,
                parentSensor.getConfiguration().laneID,
                parentSensor.getConfiguration().emlConfig.isCollimated,
                parentSensor.getConfiguration().emlConfig.laneWidth,
                intervals,
                holdin
        );
    }

    public void setLatestGammaBackground(String [] scanData) {
        String scanJoined = String.join(",", scanData);
        this.latestGammaBackground = scanJoined;
    }

    public void setLatestNeutronBackground(String [] scanData) {
        String scanJoined = String.join(",", scanData);
        this.latestNeutronBackground = scanJoined;
    }

    public void addScanDataLine(String[] scanData) {
        String scanJoined = String.join(",", scanData);
        this.scanDataList.add(scanJoined);
    }

    public Results processCurrentOccupancy() {
        Results results;

        synchronized (this) {
            try {
                String[] streamArray = new String[this.scanDataList.size() + 2];
                streamArray[0] = this.latestGammaBackground != null ? this.latestGammaBackground : "";
                streamArray[1] = this.latestNeutronBackground != null ? this.latestNeutronBackground : "";
                for(int i = 2; i < streamArray.length; i++) {
                    streamArray[i] = this.scanDataList.get(i);
                }
                Stream<String> stream = Stream.of(streamArray);
                results = ernieLane.process(stream);
                System.out.println("ERNIE Results");
                System.out.println(results.toXMLString());

            } catch (ReaderException | AnalysisException | IOException | JAXBException e) {
                throw new RuntimeException(e);
            }
        }

        //purge data occupancy for next occupancy!
        clearOccupancyList();

        return results;
    }
    public void clearOccupancyList(){
        this.scanDataList.clear();
    }

    @Override
    public void handleEvent(Event e) {
        // On setup data received, change ernie lane to have new setup values
        if(e instanceof DataEvent) {
            DataEvent dataEvent = (DataEvent) e;

            // Get setup values, then update ERNIE_lane with new setup values
            int intervals = dataEvent.getSource().getLatestRecord().getIntValue(3);
            int holdin = dataEvent.getSource().getLatestRecord().getIntValue(4);
            updateErnieSetup(intervals, holdin);
        }
    }
}
