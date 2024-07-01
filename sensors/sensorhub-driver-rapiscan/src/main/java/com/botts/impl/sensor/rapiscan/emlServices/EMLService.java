package com.botts.impl.sensor.rapiscan.emlServices;

import com.botts.impl.sensor.rapiscan.MessageHandler;
import org.vast.sensorML.SMLHelper;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;

public class EMLService {

    EMLService(EMLOutput output, MessageHandler messageHandler) throws ParserConfigurationException, IOException, SAXException {
        doInit();

        XMLStreamReader streamReader = null;
        XMLReader reader;
        output.parser(streamReader);
        output.setData();
    }

    private void doInit() {
        // TODO: Start EML service or just EML process for current lane
    }

    public String getXMLResults(int scanID, String scanData) {
        String results = "";

        // TODO: Pass scan data and scan ID (maybe) to EML service and return ERNIE results

        return results;
    }

}
