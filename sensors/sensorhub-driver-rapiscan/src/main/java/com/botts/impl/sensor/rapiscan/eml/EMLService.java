package com.botts.impl.sensor.rapiscan.eml;

import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.api.ERNIE_lane;
import gov.llnl.ernie.api.Results;
import gov.llnl.utility.io.ReaderException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
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
    List<String> occupancyDataList;

    public EMLService(RapiscanSensor parentSensor) {
        this.parentSensor = parentSensor;
        this.ernieLane = new ERNIE_lane(
                parentSensor.getConfiguration().name,
                parentSensor.getConfiguration().laneID,
                parentSensor.getConfiguration().
        );
        this.occupancyDataList = new ArrayList<>();
    }

    public void updateErnieLane() {

    }

    // TODO: Read record database
    // TODO: Write to record database
    // TODO: Process occupancy data

    public void addOccupancyLine(String[] scanData) {
        String scanJoined = String.join(",", csvLine);
        this.occupancyDataList.add(scanJoined);
    }

    public List<String> getOccupancyDataList() {
        return this.occupancyDataList;
    }

    public Results processCurrentOccupancy() {
        Results results;

//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
//        String dateStamp = dateFormat.format(new Date());
//        String fileName = "emlResults"+dateStamp+"_"+System.currentTimeMillis()+".xml";

        synchronized (this) {
            try {
                String[] streamArray = new String[this.occupancyDataList.size()];
                for(int i = 0; i < streamArray.length; i++) {
                    streamArray[i] = this.occupancyDataList.get(i);
                }
                Stream<String> stream = Stream.of(streamArray);
                results = ernieLane.process(stream);
                System.out.println("ERNIE Results");
                System.out.println(results.toXMLString());


//                //TODO: eml database
//                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
//                Document doc = docBuilder.newDocument();
//
//                Element rootElement = doc.createElement("EMLResults");
//                doc.appendChild(rootElement);
//
//                Element ernieResults = doc.createElement("ErnieResults");
//                ernieResults.appendChild(doc.createTextNode(results.toXMLString()));
//                rootElement.appendChild(ernieResults);
//
//                try(FileOutputStream output = new FileOutputStream(fileName)){
//                    writeXml(doc, output);
//                    System.out.println("Results written to xml file");
//                }


            } catch (ReaderException | AnalysisException | IOException | JAXBException e) {
                throw new RuntimeException(e);
            }
//            catch (ParserConfigurationException e) {
//                throw new RuntimeException(e);
//            } catch (TransformerException e) {
//                throw new RuntimeException(e);
//            }
        }
        //purge data occupancy for next occupancy!
        clearOccupancyList();

        return results;
    }
    public void clearOccupancyList(){
        this.occupancyDataList.clear();
    }

    @Override
    public void handleEvent(Event e) {
        // On data received, change ernie lane to have new setup values
        if(e instanceof DataEvent) {

            DataEvent dataEvent = (DataEvent)e;

            try
            {
                // Get setup values, then update ERNIE_lane with new setup values
                int holdin = dataEvent.getSource().getLatestRecord().getIntValue(4);
                int intervals = dataEvent.getSource().getLatestRecord().getIntValue(5);
                int nsigma = dataEvent.getSource().getLatestRecord().getIntValue(6);
                updateErnieLane(holdin, intervals, nsigma);
                // Needs
                //    String portId,
                //    int laneId,
                //    boolean isCollimated,
                //    double laneWidth,
                //    int intervals,
                //    int occupancyHoldi
                IStreamingDataInterface output = driver.getObservationOutputs().get(dataEvent.getOutputName());
                writer.setDataComponents(output.getRecordDescription().copy());
                writer.reset();
                writer.write(dataEvent.getRecords()[0]);
                writer.flush();

                sampleCount++;
            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }

            synchronized (this) { this.notify(); }
        }
    }
}
