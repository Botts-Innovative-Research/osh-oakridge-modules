package com.botts.impl.sensor.rapiscan.eml;

import com.botts.impl.sensor.rapiscan.MessageHandler;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import gov.llnl.ernie.analysis.AnalysisException;
import gov.llnl.ernie.api.DailyFileLoader;
import gov.llnl.ernie.api.Results;
import gov.llnl.ernie.vm250.VM250RecordDatabase;
import gov.llnl.ernie.vm250.VM250RecordDatabaseReader;
import gov.llnl.ernie.vm250.data.VM250Occupancy;
import gov.llnl.ernie.vm250.data.VM250Record;
import gov.llnl.ernie.vm250.data.VM250RecordInternal;
import gov.llnl.ernie.vm250.tools.DailyFileWriter;
import gov.llnl.ernie.vm250.tools.VM250OccupancyConverter;
import gov.llnl.utility.io.ReaderException;
import org.javatuples.Triplet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;


public class EMLService {

    RapiscanSensor parentSensor;
    List<String> occupancyDataList;

    public EMLService(RapiscanSensor parentSensor) {
        this.parentSensor = parentSensor;
        this.occupancyDataList = new ArrayList<>();
    }

    // TODO: Read record database
    // TODO: Write to record database
    // TODO: Process occupancy data

    public void addOccupancyLine(String scanData) {
        this.occupancyDataList.add(scanData);
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
                results = parentSensor.getErnieLane().process(stream);
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


//    private static void writeXml(Document doc, OutputStream outputStream) throws TransformerException {
//        TransformerFactory transformerFactory = TransformerFactory.newInstance();
//        Transformer transformer = transformerFactory.newTransformer();
//        DOMSource source = new DOMSource(doc);
//        StreamResult result = new StreamResult(outputStream);
//
//        transformer.transform(source, result);
//    }
}
