package com.botts.impl.sensor.rapiscan.eml;

import com.botts.impl.sensor.rapiscan.NeutronOutput;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataArrayImpl;
import org.vast.data.TextEncodingImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

//TODO: Maybe use JAXB? socom/sensors/sensorhub-driver-tak

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.Boolean;

public class EMLOutput extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "EML Ernie Analysis";

    private static final Logger logger = LoggerFactory.getLogger(NeutronOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;

    String result;
    double releaseProbability;
    double investigateProbability;
    boolean gammaAlert;
    boolean neutronAlert;
    String overallSourceType;
    String overallClassifierUsed;
    double overallXLocation1;
    double overallXLocation2;
    double overallYLocation;
    double overallZLocation;
    double overallProbabilityNonEmitting;
    double overallProbabilityNORM;
    double overallProbabilityThreat;
    String sourceType;
    String classifierUsed;
    double xLocation1;
    double xLocation2;
    double yLocation;
    double zLocation;
    double probabilityNonEmitting;
    double probabilityNORM;
    double probabilityThreat;

    int sourceListSize;
    int vehicleClass;
    double vehicleLength;
    String message;
    String yellowLightMessage;

    public EMLOutput(RapiscanSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }
    public void init(){
        dataStruct = createDataRecord();
        dataEncoding = new TextEncodingImpl(",", "\n");
    }
    DataRecord createDataRecord(){
        RADHelper radHelper = new RADHelper();
        return radHelper.createRecord()
                .name("ERNIEAnalysis")
                .label("ERNIE Analysis")
                .definition(RADHelper.getRadUri("ERNIEAnalysis"))
                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
                .addField("result", radHelper.createText()
                        .name("result")
                        .label("Result")
                        .definition(RADHelper.getRadUri("result"))
                        .build())
                .addField("investigateProbability", radHelper.createQuantity()
                        .name("investigate-probability")
                        .label("Investigate Probability")
                        .dataType(DataType.DOUBLE)
                        .definition(RADHelper.getRadUri("investigate-probability"))
                        .build())
                .addField("releaseProbability", radHelper.createQuantity()
                        .name("release-probability")
                        .label("Release Probability")
                        .dataType(DataType.DOUBLE)
                        .definition(RADHelper.getRadUri("release-probability"))
                        .build())
                .addField("gammaAlert", radHelper.createBoolean()
                        .name("gamma-alert")
                        .label("Gamma Alert")
                        .definition(RADHelper.getRadUri("gamma-alert"))
                        .build())
                .addField("neutronAlert", radHelper.createBoolean()
                        .name("neutron-alert")
                        .label("Neutron Alert")
                        .definition(RADHelper.getRadUri("neutron-alert"))
                        .build())
                .addField("sourceCount", radHelper.createCount()
                        .id("sourceCountId"))
                .addField("sources", radHelper.createArray()
                        .label("sources")
                        .withVariableSize("sourceCountId")
                        .withElement("source", radHelper.createRecord()
                                .addField("sourceType", radHelper.createText())
                                .addField("classifierUsed", radHelper.createText())
                                .addField("xLocation1", radHelper.createQuantity())
                                .addField("xLocation2", radHelper.createQuantity())
                                .addField("yLocation", radHelper.createQuantity())
                                .addField("zLocation", radHelper.createQuantity())
                                .addField("probabilityNonEmitting", radHelper.createQuantity())
                                .addField("probabilityNORM", radHelper.createQuantity())
                                .addField("probabilityThreat", radHelper.createQuantity())
                        )
                        .build())
                .addField("overallSource", radHelper.createRecord()
                        .label("Overall Source")
                        .addField("sourceType", radHelper.createText())
                        .addField("classifierUsed", radHelper.createText())
                        .addField("xLocation1", radHelper.createQuantity())
                        .addField("xLocation2", radHelper.createQuantity())
                        .addField("yLocation", radHelper.createQuantity())
                        .addField("zLocation", radHelper.createQuantity())
                        .addField("probabilityNonEmitting", radHelper.createQuantity())
                        .addField("probabilityNORM", radHelper.createQuantity())
                        .addField("probabilityThreat", radHelper.createQuantity())
                        .build())
                .addField("vehicleClass", radHelper.createQuantity()
                        .name("vehicle-class")
                        .label("Vehicle Class")
                        .dataType(DataType.INT)
                        .definition(RADHelper.getRadUri("vehicle-class"))
                        .build())
                .addField("vehicleLength", radHelper.createQuantity()
                        .name("vehicle-length")
                        .label("Vehicle Length")
                        .dataType(DataType.DOUBLE)
                        .definition(RADHelper.getRadUri("vehicle-length"))
                        .build())
                .addField("message", radHelper.createText()
                        .name("message")
                        .label("Message")
                        .definition(RADHelper.getRadUri("message"))
                        .build())
                .addField("yellowMessageLight", radHelper.createText()
                        .name("yellow-message-light")
                        .label("Yellow Message Light")
                        .definition(RADHelper.getRadUri("yellow-message-light"))
                        .build())

                .build();

    }

    public void setData(){
        dataStruct = createDataRecord();
        DataBlock dataBlock;
        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }
        dataStruct.setData(dataBlock);

        //TODO: parse xml reader for the values

        int index =0;
        dataBlock.setDoubleValue(index++, System.currentTimeMillis()/1000d);
        dataBlock.setStringValue(index++, getResult());
        dataBlock.setDoubleValue(index++, getInvestigateProbability());
        dataBlock.setDoubleValue(index++, getReleaseProbability());
        dataBlock.setBooleanValue(index++, isGammaAlert() );
        dataBlock.setBooleanValue(index++, isNeutronAlert());
        dataBlock.setIntValue(index++, sourceListSize);


//        var sourceArray = ((DataBlockParallel) this.dataStruct.getComponent("sources").getData());
        var sourceArray = ((DataArrayImpl) this.dataStruct.getComponent("sources"));
        sourceArray.updateSize();
        //dataBlock.updateAtomCount();

        for(int j=0; j<sourceListSize; j++) {
            dataBlock.setStringValue(index++, getSourceType());
            dataBlock.setStringValue(index++, getClassifierUsed());
            dataBlock.setDoubleValue(index++, getxLocation1());
            dataBlock.setDoubleValue(index++, getxLocation2());
            dataBlock.setDoubleValue(index++, getyLocation());
            dataBlock.setDoubleValue(index++, getzLocation());
            dataBlock.setDoubleValue(index++, getProbabilityNonEmitting());
            dataBlock.setDoubleValue(index++, getProbabilityNORM());
            dataBlock.setDoubleValue(index++, getProbabilityThreat());

        }
//            dataBlock.setStringValue(index++, getSourceType());
//            dataBlock.setStringValue(index++, getClassifierUsed());
//            dataBlock.setDoubleValue(index++, getxLocation1());
//            dataBlock.setDoubleValue(index++, getxLocation2());
//            dataBlock.setDoubleValue(index++, getyLocation());
//            dataBlock.setDoubleValue(index++, getzLocation());
//            dataBlock.setDoubleValue(index++, getProbabilityNonEmitting());
//            dataBlock.setDoubleValue(index++, getProbabilityNORM());
//            dataBlock.setDoubleValue(index++, getProbabilityThreat());

//            dataBlock.updateAtomCount();
//        }

        dataBlock.setStringValue(index++, getOverallSourceType());
        dataBlock.setStringValue(index++, getOverallClassifierUsed());
        dataBlock.setDoubleValue(index++, getOverallXLocation1());
        dataBlock.setDoubleValue(index++, getOverallXLocation2());
        dataBlock.setDoubleValue(index++, getOverallYLocation());
        dataBlock.setDoubleValue(index++, getOverallZLocation());
        dataBlock.setDoubleValue(index++, getOverallProbabilityNonEmitting());
        dataBlock.setDoubleValue(index++, getOverallProbabilityNORM());
        dataBlock.setDoubleValue(index++, getOverallProbabilityThreat());

        dataBlock.setIntValue(index++, getVehicleClass());
        dataBlock.setDoubleValue(index++,getVehicleLength());
        dataBlock.setStringValue(index++, getMessage());
        dataBlock.setStringValue(index++, getYellowLightMessage());

        latestRecord = dataBlock;

        eventHandler.publish(new DataEvent(System.currentTimeMillis(), EMLOutput.this, dataBlock));
    }


    //credit to https://mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
    public void parser(String emlResults) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

//        Document document = builder.parse(new File("fakeEMLOutputData.xml"));

//        EMLService emlService = new EMLService(this, this.parentSensor.getMessageHandler());
//        String emlResults = emlService.getXMLResults(0, "example scan data");
        InputStream targetStream = new ByteArrayInputStream(emlResults.getBytes());
        Document document = builder.parse(new BufferedInputStream(targetStream));
        document.getDocumentElement().normalize();
        NodeList ernieList = document.getElementsByTagName("ERNIEAnalysis");
        Element ernieElement = (Element) ernieList.item(0);

        result = ernieElement.getElementsByTagName("result").item(0).getTextContent();
        investigateProbability = Double.parseDouble(ernieElement.getElementsByTagName("investigateProbability").item(0).getTextContent());
        releaseProbability = Double.parseDouble(ernieElement.getElementsByTagName("releaseProbability").item(0).getTextContent());
        gammaAlert = Boolean.parseBoolean(ernieElement.getElementsByTagName("gammaAlert").item(0).getTextContent());
        neutronAlert = Boolean.parseBoolean(ernieElement.getElementsByTagName("neutronAlert").item(0).getTextContent());

        NodeList sourceList = ernieElement.getElementsByTagName("sources");
        sourceListSize = sourceList.getLength();
        for(int i=0; i<sourceListSize; i++){
            Element source = (Element) sourceList.item(i);
            sourceType = source.getElementsByTagName("sourceType").item(0).getTextContent();
            classifierUsed = source.getElementsByTagName("classifierUsed").item(0).getTextContent();
            xLocation1 = Double.parseDouble(source.getElementsByTagName("xLocation1").item(0).getTextContent());
            xLocation2 = Double.parseDouble(source.getElementsByTagName("xLocation2").item(0).getTextContent());
            yLocation = Double.parseDouble(source.getElementsByTagName("yLocation").item(0).getTextContent());
            zLocation = Double.parseDouble(source.getElementsByTagName("zLocation").item(0).getTextContent());
            probabilityNonEmitting = Double.parseDouble(source.getElementsByTagName("probabilityNonEmitting").item(0).getTextContent());
            probabilityNORM = Double.parseDouble(source.getElementsByTagName("probabilityNORM").item(0).getTextContent());
            probabilityThreat = Double.parseDouble(source.getElementsByTagName("probabilityThreat").item(0).getTextContent());

        }

        //overall source
        NodeList overallSourceList = ernieElement.getElementsByTagName("overallSource");
        for(int i=0; i< overallSourceList.getLength(); i++){
            Element overallSource = (Element) overallSourceList.item(i);
            overallSourceType = overallSource.getElementsByTagName("sourceType").item(0).getTextContent();
            overallClassifierUsed = overallSource.getElementsByTagName("classifierUsed").item(0).getTextContent();
            overallXLocation1 = Double.parseDouble(overallSource.getElementsByTagName("xLocation1").item(0).getTextContent());
            overallXLocation2 = Double.parseDouble(overallSource.getElementsByTagName("xLocation2").item(0).getTextContent());
            overallYLocation = Double.parseDouble(overallSource.getElementsByTagName("yLocation").item(0).getTextContent());
            overallZLocation = Double.parseDouble(overallSource.getElementsByTagName("zLocation").item(0).getTextContent());
            overallProbabilityNonEmitting = Double.parseDouble(overallSource.getElementsByTagName("probabilityNonEmitting").item(0).getTextContent());
            overallProbabilityNORM = Double.parseDouble(overallSource.getElementsByTagName("probabilityNORM").item(0).getTextContent());
            overallProbabilityThreat = Double.parseDouble(overallSource.getElementsByTagName("probabilityThreat").item(0).getTextContent());
        }


        vehicleClass = Integer.parseInt(ernieElement.getElementsByTagName("vehicleClass").item(0).getTextContent());
        vehicleLength = Double.parseDouble(ernieElement.getElementsByTagName("vehicleLength").item(0).getTextContent());
        message = ernieElement.getElementsByTagName("message").item(0).getTextContent();
        yellowLightMessage = ernieElement.getElementsByTagName("yellowLightMessage").item(0).getTextContent();
    }



    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 0;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public double getReleaseProbability() {
        return releaseProbability;
    }

    public void setReleaseProbability(double releaseProbability) {
        this.releaseProbability = releaseProbability;
    }

    public double getInvestigateProbability() {
        return investigateProbability;
    }

    public void setInvestigateProbability(double investigateProbability) {
        this.investigateProbability = investigateProbability;
    }

    public boolean isGammaAlert() {
        return gammaAlert;
    }

    public void setGammaAlert(boolean gammaAlert) {
        this.gammaAlert = gammaAlert;
    }

    public boolean isNeutronAlert() {
        return neutronAlert;
    }

    public void setNeutronAlert(boolean neutronAlert) {
        this.neutronAlert = neutronAlert;
    }

    public String getOverallSourceType() {
        return overallSourceType;
    }

    public void setOverallSourceType(String overallSourceType) {
        this.overallSourceType = overallSourceType;
    }

    public String getOverallClassifierUsed() {
        return overallClassifierUsed;
    }

    public void setOverallClassifierUsed(String overallClassifierUsed) {
        this.overallClassifierUsed = overallClassifierUsed;
    }

    public double getOverallXLocation1() {
        return overallXLocation1;
    }

    public void setOverallXLocation1(double overallXLocation1) {
        this.overallXLocation1 = overallXLocation1;
    }

    public double getOverallXLocation2() {
        return overallXLocation2;
    }

    public void setOverallXLocation2(double overallXLocation2) {
        this.overallXLocation2 = overallXLocation2;
    }

    public double getOverallYLocation() {
        return overallYLocation;
    }

    public void setOverallYLocation(double overallYLocation) {
        this.overallYLocation = overallYLocation;
    }

    public double getOverallZLocation() {
        return overallZLocation;
    }

    public void setOverallZLocation(double overallZLocation) {
        this.overallZLocation = overallZLocation;
    }

    public double getOverallProbabilityNonEmitting() {
        return overallProbabilityNonEmitting;
    }

    public void setOverallProbabilityNonEmitting(double overallProbabilityNonEmitting) {
        this.overallProbabilityNonEmitting = overallProbabilityNonEmitting;
    }

    public double getOverallProbabilityNORM() {
        return overallProbabilityNORM;
    }

    public void setOverallProbabilityNORM(double overallProbabilityNORM) {
        this.overallProbabilityNORM = overallProbabilityNORM;
    }

    public double getOverallProbabilityThreat() {
        return overallProbabilityThreat;
    }

    public void setOverallProbabilityThreat(double overallProbabilityThreat) {
        this.overallProbabilityThreat = overallProbabilityThreat;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getClassifierUsed() {
        return classifierUsed;
    }

    public void setClassifierUsed(String classifierUsed) {
        this.classifierUsed = classifierUsed;
    }

    public double getxLocation1() {
        return xLocation1;
    }

    public void setxLocation1(double xLocation1) {
        this.xLocation1 = xLocation1;
    }

    public double getxLocation2() {
        return xLocation2;
    }

    public void setxLocation2(double xLocation2) {
        this.xLocation2 = xLocation2;
    }

    public double getyLocation() {
        return yLocation;
    }

    public void setyLocation(double yLocation) {
        this.yLocation = yLocation;
    }

    public double getzLocation() {
        return zLocation;
    }

    public void setzLocation(double zLocation) {
        this.zLocation = zLocation;
    }

    public double getProbabilityNonEmitting() {
        return probabilityNonEmitting;
    }

    public void setProbabilityNonEmitting(double probabilityNonEmitting) {
        this.probabilityNonEmitting = probabilityNonEmitting;
    }

    public double getProbabilityNORM() {
        return probabilityNORM;
    }

    public void setProbabilityNORM(double probabilityNORM) {
        this.probabilityNORM = probabilityNORM;
    }

    public double getProbabilityThreat() {
        return probabilityThreat;
    }

    public void setProbabilityThreat(double probabilityThreat) {
        this.probabilityThreat = probabilityThreat;
    }

    public int getVehicleClass() {
        return vehicleClass;
    }

    public void setVehicleClass(int vehicleClass) {
        this.vehicleClass = vehicleClass;
    }

    public double getVehicleLength() {
        return vehicleLength;
    }

    public void setVehicleLength(double vehicleLength) {
        this.vehicleLength = vehicleLength;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getYellowLightMessage() {
        return yellowLightMessage;
    }

    public void setYellowLightMessage(String yellowLightMessage) {
        this.yellowLightMessage = yellowLightMessage;
    }
}
