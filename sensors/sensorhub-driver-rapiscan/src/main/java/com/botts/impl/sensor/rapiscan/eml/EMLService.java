package com.botts.impl.sensor.rapiscan.eml;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import com.botts.impl.sensor.rapiscan.RepetitiveWarningFilter;
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
import java.util.function.Supplier;
import java.util.stream.Stream;


public class EMLService implements IEventListener {

    /**
     * LLNL's legacy XML binding registry is process-global and is not safe to
     * populate concurrently. A 50-lane restart otherwise lets multiple ERNIE
     * constructors race while registering the VM250 schema, leaving some
     * analyzers uninitialized even though their RPM modules report Started.
     */
    private static final Object ERNIE_INITIALIZATION_LOCK = new Object();

    private ERNIE_lane ernieLane;
    RapiscanSensor parentSensor;
    List<String> scanDataList;
    String latestGammaBackground;
    String latestNeutronBackground;

    public EMLService(RapiscanSensor parentSensor) {
        this.parentSensor = parentSensor;
        this.ernieLane = createErnieLane(
                parentSensor.getConfiguration().setupGammaConfig.intervals,
                parentSensor.getConfiguration().setupGammaConfig.occupancyHoldin);
        this.scanDataList = new ArrayList<>();
    }

    private void updateErnieSetup(int intervals, int holdin) {
        this.ernieLane = createErnieLane(intervals, holdin);
    }

    private ERNIE_lane createErnieLane(int intervals, int holdin) {
        return initializeErnie(() -> new ERNIE_lane(
                parentSensor.getConfiguration().name,
                parentSensor.getConfiguration().laneID,
                parentSensor.getConfiguration().emlConfig.isCollimated,
                parentSensor.getConfiguration().emlConfig.laneWidth,
                intervals,
                holdin
        ));
    }

    static <T> T initializeErnie(Supplier<T> initializer) {
        synchronized (ERNIE_INITIALIZATION_LOCK) {
            return initializer.get();
        }
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
                if(this.latestGammaBackground != null)
                    this.scanDataList.add(0, this.latestGammaBackground);
                if(this.latestNeutronBackground != null)
                    this.scanDataList.add(0, this.latestNeutronBackground);

                String[] streamArray = new String[this.scanDataList.size()];
                for(int i = 0; i < streamArray.length; i++) {
                    streamArray[i] = this.scanDataList.get(i);
                }


                Stream<String> stream = Stream.of(streamArray);
                // SensorHub may reload JUL configuration after module initialization,
                // clearing programmatic filters. Reassert the lightweight filter at
                // the call site so the per-occupancy EML fallback warning cannot flood
                // production logs after a runtime logging reconfiguration.
                RepetitiveWarningFilter.install();
                results = ernieLane.process(stream);
            } catch (ReaderException | AnalysisException | IOException e) {
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
