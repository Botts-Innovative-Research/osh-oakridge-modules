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

        // TODO: Pass scan data and scan ID (maybe) to EML service and return ERNIE results
        // We get this string below from EML process
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<!-- Created with Liquid Technologies Online Tools 1.0 (https://www.liquid-technologies.com) -->\n" +
                "<results xsi:noNamespaceSchemaLocation=\"schema.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "  <ERNIEContextualInfo>\n" +
                "    <versionID>string</versionID>\n" +
                "    <modelID>string</modelID>\n" +
                "    <thresholds>\n" +
                "      <primary>12764800000000.2</primary>\n" +
                "    </thresholds>\n" +
                "  </ERNIEContextualInfo>\n" +
                "  <ScanContextualInfo>\n" +
                "    <portID>string</portID>\n" +
                "    <laneID>3357</laneID>\n" +
                "    <dateTime>string</dateTime>\n" +
                "    <segmentId>string</segmentId>\n" +
                "    <RPMResult>string</RPMResult>\n" +
                "    <RPMgammaAlert>false</RPMgammaAlert>\n" +
                "    <RPMneutronAlert>true</RPMneutronAlert>\n" +
                "    <RPMScanError>0</RPMScanError>\n" +
                "  </ScanContextualInfo>\n" +
                "  <ERNIEAnalysis>\n" +
                "    <result>string</result>\n" +
                "    <investigateProbability>-14782099999999.8</investigateProbability>\n" +
                "    <releaseProbability>30916800000000.2</releaseProbability>\n" +
                "    <gammaAlert>false</gammaAlert>\n" +
                "    <neutronAlert>1</neutronAlert>\n" +
                "    <sources>\n" +
                "      <sourceType>string</sourceType>\n" +
                "      <classifierUsed>string</classifierUsed>\n" +
                "      <xLocation1>575640.234905804</xLocation1>\n" +
                "      <xLocation2>4277760.2349058</xLocation2>\n" +
                "      <yLocation>-28544299999999.8</yLocation>\n" +
                "      <zLocation>4666000.2349058</zLocation>\n" +
                "      <probabilityNonEmitting>1510200000000.23</probabilityNonEmitting>\n" +
                "      <probabilityNORM>-4646699999999.76</probabilityNORM>\n" +
                "      <probabilityThreat>-35445199999999.8</probabilityThreat>\n" +
                "    </sources>\n" +
                "    <sources>\n" +
                "      <sourceType>string</sourceType>\n" +
                "      <classifierUsed>string</classifierUsed>\n" +
                "      <xLocation1>2637610.2349058</xLocation1>\n" +
                "      <xLocation2>-4994749.7650942</xLocation2>\n" +
                "      <yLocation>-14212899999999.8</yLocation>\n" +
                "      <zLocation>3026800.2349058</zLocation>\n" +
                "      <probabilityNonEmitting>-47427499999999.8</probabilityNonEmitting>\n" +
                "      <probabilityNORM>36104800000000.2</probabilityNORM>\n" +
                "      <probabilityThreat>-38720499999999.8</probabilityThreat>\n" +
                "    </sources>\n" +
                "    <sources>\n" +
                "      <sourceType>string</sourceType>\n" +
                "      <classifierUsed>string</classifierUsed>\n" +
                "      <xLocation1>-4051049.7650942</xLocation1>\n" +
                "      <xLocation2>-3340769.7650942</xLocation2>\n" +
                "      <yLocation>-30198099999999.8</yLocation>\n" +
                "      <zLocation>-1479879.7650942</zLocation>\n" +
                "      <probabilityNonEmitting>-27239599999999.8</probabilityNonEmitting>\n" +
                "      <probabilityNORM>17137200000000.2</probabilityNORM>\n" +
                "      <probabilityThreat>39423700000000.2</probabilityThreat>\n" +
                "    </sources>\n" +
                "    <sources>\n" +
                "      <sourceType>string</sourceType>\n" +
                "      <classifierUsed>string</classifierUsed>\n" +
                "      <xLocation1>-4986489.7650942</xLocation1>\n" +
                "      <xLocation2>-2614389.7650942</xLocation2>\n" +
                "      <yLocation>-30030799999999.8</yLocation>\n" +
                "      <zLocation>-3749039.7650942</zLocation>\n" +
                "      <probabilityNonEmitting>-31116899999999.8</probabilityNonEmitting>\n" +
                "      <probabilityNORM>-39444899999999.8</probabilityNORM>\n" +
                "      <probabilityThreat>-9769299999999.77</probabilityThreat>\n" +
                "    </sources>\n" +
                "    <sources>\n" +
                "      <sourceType>string</sourceType>\n" +
                "      <classifierUsed>string</classifierUsed>\n" +
                "      <xLocation1>-2710309.7650942</xLocation1>\n" +
                "      <xLocation2>3590620.2349058</xLocation2>\n" +
                "      <yLocation>6574200000000.24</yLocation>\n" +
                "      <zLocation>1485130.2349058</zLocation>\n" +
                "      <probabilityNonEmitting>-36973299999999.8</probabilityNonEmitting>\n" +
                "      <probabilityNORM>-36869299999999.8</probabilityNORM>\n" +
                "      <probabilityThreat>-36851299999999.8</probabilityThreat>\n" +
                "    </sources>\n" +
                "    <overallSource>\n" +
                "      <sourceType>string</sourceType>\n" +
                "      <classifierUsed>string</classifierUsed>\n" +
                "      <xLocation1>-902269.765094196</xLocation1>\n" +
                "      <xLocation2>3424760.2349058</xLocation2>\n" +
                "      <yLocation>-36143999999999.8</yLocation>\n" +
                "      <zLocation>-2976349.7650942</zLocation>\n" +
                "      <probabilityNonEmitting>21245600000000.2</probabilityNonEmitting>\n" +
                "      <probabilityNORM>-9889599999999.77</probabilityNORM>\n" +
                "      <probabilityThreat>15708800000000.2</probabilityThreat>\n" +
                "    </overallSource>\n" +
                "    <vehicleClass>4555</vehicleClass>\n" +
                "    <vehicleLength>-2685379.7650942</vehicleLength>\n" +
                "    <message>string</message>\n" +
                "    <yellowLightMessage>string</yellowLightMessage>\n" +
                "  </ERNIEAnalysis>\n" +
                "</results>";
    }

}
