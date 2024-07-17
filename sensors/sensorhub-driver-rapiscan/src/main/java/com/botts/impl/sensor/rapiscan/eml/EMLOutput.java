package com.botts.impl.sensor.rapiscan.eml;

import com.botts.impl.sensor.rapiscan.output.GammaOutput;
import com.botts.impl.sensor.rapiscan.output.NeutronOutput;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import gov.llnl.ernie.api.Results;
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
import java.awt.*;
import java.io.*;
import java.lang.Boolean;

public class EMLOutput extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "ERNIEAnalysis";
    private static final String SENSOR_OUTPUT_LABEL = "EML ERNIE Analysis";

    private static final Logger logger = LoggerFactory.getLogger(NeutronOutput.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public EMLOutput(RapiscanSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    public void init() {
        dataStruct = createDataRecord();
        dataEncoding = new TextEncodingImpl(",", "\n");
    }

    DataRecord createDataRecord() {
        RADHelper radHelper = new RADHelper();

        var samplingTime = radHelper.createPrecisionTimeStamp();
        var laneID = radHelper.createLaneID();

        return radHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_LABEL)
                .definition(RADHelper.getRadUri("ERNIEAnalysis"))
                .addField(samplingTime.getName(), samplingTime)
                .addField(laneID.getName(), laneID)
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
                .addField("vehicleClass", radHelper.createCount()
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

    public void onNewMessage(Results results, long timeStamp) {
        if (latestRecord == null) {
            dataBlock = dataStruct.createDataBlock();
        } else {

            dataBlock = latestRecord.renew();
        }

//        int index = 0;
//        dataBlock.setLongValue(index++,timeStamp/1000);
//        dataBlock.setStringValue(index++, parent.laneName);
//
//        dataBlock.setStringValue(index++, results.getResult().name());
//        dataBlock.setDoubleValue(index++, results.getInvestigateProbability());
//        dataBlock.setDoubleValue(index++, results.getReleaseProbability());
//        dataBlock.setBooleanValue(index++, results.getERNIEGammaAlert());
//        dataBlock.setBooleanValue(index++, results.getERNIENeutronAlert());
//
//        dataBlock.setIntValue(index++, results.getNumberOfSources());
//
//        for(int srcIndex = 0; srcIndex < results.getNumberOfSources(); srcIndex++) {
//            var source = results.getSource(srcIndex);
//            dataBlock.setStringValue(index++, source.getSourceType());
//            dataBlock.setStringValue(index++, source.getClassifierUsed());
//            dataBlock.setDoubleValue(index++, source.getxLocation1());
//            dataBlock.setDoubleValue(index++, source.getxLocation2());
//            dataBlock.setDoubleValue(index++, source.getyLocation());
//            dataBlock.setDoubleValue(index++, source.getzLocation());
//            dataBlock.setDoubleValue(index++, source.getProbabilityNonEmitting());
//            dataBlock.setDoubleValue(index++, source.getProbabilityNORM());
//            dataBlock.setDoubleValue(index++, source.getProbabilityThreat());
//        }
//
//        if(results.getNumberOfSources() == 0) {
//            index++;
//        }
//
//        var overallSource = results.getOverallSource();
//        if(overallSource != null) {
//            dataBlock.setStringValue(index++, overallSource.getSourceType());
//            dataBlock.setStringValue(index++, overallSource.getClassifierUsed());
//            dataBlock.setDoubleValue(index++, overallSource.getxLocation1());
//            dataBlock.setDoubleValue(index++, overallSource.getxLocation2());
//            dataBlock.setDoubleValue(index++, overallSource.getyLocation());
//            dataBlock.setDoubleValue(index++, overallSource.getzLocation());
//            dataBlock.setDoubleValue(index++, overallSource.getProbabilityNonEmitting());
//            dataBlock.setDoubleValue(index++, overallSource.getProbabilityNORM());
//            dataBlock.setDoubleValue(index++, overallSource.getProbabilityThreat());
//        } else {
//            index += 7;
//        }
//
//        dataBlock.setIntValue(index++, results.getVehicleClass());
//        dataBlock.setDoubleValue(index++, results.getVehicleLength());
//        dataBlock.setStringValue(index++, results.getMessage());
//        dataBlock.setStringValue(index, results.getYellowLightMessage());

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(timeStamp, EMLOutput.this, dataBlock));

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

}