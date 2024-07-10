package com.botts.impl.sensor.rapiscan.eml;

import com.botts.impl.sensor.rapiscan.MessageHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;


public class EMLService {

    public EMLService(EMLOutput output, MessageHandler messageHandler) throws ParserConfigurationException, IOException, SAXException {
        doInit();

//        XMLStreamReader streamReader = null;
//        XMLReader reader;
        output.parser(getXMLResults(0,"example"));
        output.setData();
    }

    private void doInit() {
        // TODO: Start EML service or just EML process for current lane
    }

    public String getXMLResults(int scanID, String scanData) {
        String results = "";

        // TODO: Pass scan data and scan ID (maybe) to EML service and return ERNIE results

        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
                "<results xsi:noNamespaceSchemaLocation=\"schema.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"+
                "<ERNIEContextualInfo>\n" +
                "        <versionID>string</versionID>\n" +
                "        <modelID>string</modelID>\n" +
                "        <thresholds>\n" +
                "            <primary>-9973999999999.77</primary>\n" +
                "        </thresholds>\n" +
                "    </ERNIEContextualInfo>\n" +
                "    <ScanContextualInfo>\n" +
                "        <portID>string</portID>\n" +
                "        <laneID>18</laneID>\n" +
                "        <dateTime>string</dateTime>\n" +
                "        <segmentId>string</segmentId>\n" +
                "        <RPMResult>string</RPMResult>\n" +
                "        <RPMgammaAlert>false</RPMgammaAlert>\n" +
                "        <RPMneutronAlert>0</RPMneutronAlert>\n" +
                "        <RPMScanError>0</RPMScanError>\n" +
                "    </ScanContextualInfo>\n" +
                "    <ERNIEAnalysis>\n" +
                "        <result>string</result>\n" +
                "        <investigateProbability>-9404399999999.77</investigateProbability>\n" +
                "        <releaseProbability>-38009999999999.8</releaseProbability>\n" +
                "        <gammaAlert>0</gammaAlert>\n" +
                "        <neutronAlert>false</neutronAlert>\n" +
                "        <sources>\n" +
                "            <sourceType>string</sourceType>\n" +
                "            <classifierUsed>string</classifierUsed>\n" +
                "            <xLocation1>182670.234905804</xLocation1>\n" +
                "            <xLocation2>2988970.2349058</xLocation2>\n" +
                "            <yLocation>-8713399999999.76</yLocation>\n" +
                "            <zLocation>4045150.2349058</zLocation>\n" +
                "            <probabilityNonEmitting>32972800000000.2</probabilityNonEmitting>\n" +
                "            <probabilityNORM>44271600000000.2</probabilityNORM>\n" +
                "            <probabilityThreat>40553700000000.2</probabilityThreat>\n" +
                "        </sources>\n" +
                "        <sources>\n" +
                "            <sourceType>string</sourceType>\n" +
                "            <classifierUsed>string</classifierUsed>\n" +
                "            <xLocation1>3998480.2349058</xLocation1>\n" +
                "            <xLocation2>-4953329.7650942</xLocation2>\n" +
                "            <yLocation>39714900000000.2</yLocation>\n" +
                "            <zLocation>-2356849.7650942</zLocation>\n" +
                "            <probabilityNonEmitting>28484500000000.2</probabilityNonEmitting>\n" +
                "            <probabilityNORM>15800000000.2349</probabilityNORM>\n" +
                "            <probabilityThreat>39605800000000.2</probabilityThreat>\n" +
                "        </sources>\n" +
                "        <overallSource>\n" +
                "            <sourceType>string</sourceType>\n" +
                "            <classifierUsed>string</classifierUsed>\n" +
                "            <xLocation1>-3576769.7650942</xLocation1>\n" +
                "            <xLocation2>2041070.2349058</xLocation2>\n" +
                "            <yLocation>-22927799999999.8</yLocation>\n" +
                "            <zLocation>1670560.2349058</zLocation>\n" +
                "            <probabilityNonEmitting>-39421699999999.8</probabilityNonEmitting>\n" +
                "            <probabilityNORM>-47698399999999.8</probabilityNORM>\n" +
                "            <probabilityThreat>43997800000000.2</probabilityThreat>\n" +
                "        </overallSource>\n" +
                "        <vehicleClass>3541</vehicleClass>\n" +
                "        <vehicleLength>-3378269.7650942</vehicleLength>\n" +
                "        <message>string</message>\n" +
                "        <yellowLightMessage>string</yellowLightMessage>\n" +
                "    </ERNIEAnalysis>\n" +
                "</results>";
    }

}
