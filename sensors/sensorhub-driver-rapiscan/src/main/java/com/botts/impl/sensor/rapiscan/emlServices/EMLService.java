package com.botts.impl.sensor.rapiscan.emlServices;

import com.botts.impl.sensor.rapiscan.MessageHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EMLService {

    public EMLService(EMLOutput output, MessageHandler messageHandler) throws ParserConfigurationException, IOException, SAXException {
        doInit();

//        XMLStreamReader streamReader = null;
//        XMLReader reader;
        output.parser();
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
