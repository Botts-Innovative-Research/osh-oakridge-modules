//package com.botts.impl.sensor.rapiscan.unused;
//
//import com.botts.impl.sensor.rapiscan.MessageHandler;
//import com.botts.impl.sensor.rapiscan.RapiscanSensor;
//import net.opengis.swe.v20.DataBlock;
//import net.opengis.swe.v20.DataComponent;
//import net.opengis.swe.v20.DataEncoding;
//import net.opengis.swe.v20.DataRecord;
//import org.sensorhub.api.data.DataEvent;
//import org.sensorhub.impl.sensor.AbstractSensorOutput;
//import org.sensorhub.impl.utils.rad.RADHelper;
//import org.vast.data.DataArrayImpl;
//
//public class NeutronOut extends AbstractSensorOutput<RapiscanSensor> {
//    private static final String SENSOR_OUTPUT_NAME = "Neutron Scan";
//    protected DataRecord dataStruct;
//    protected DataEncoding dataEncoding;
//
//
//    public NeutronOut(RapiscanSensor parentSensor) {
//        super(SENSOR_OUTPUT_NAME, parentSensor);
//    }
//
//    public void init(){
//        createDataRecord();
//    }
//    protected void createDataRecord(){
//        RADHelper radHelper = new RADHelper();
//        dataStruct = radHelper.createRecord()
//                .name(getName())
//                .label("Neutron Scan")
//                .updatable(true)
//                .definition(RADHelper.getRadUri("neutron-scan"))
//                .description("")
//                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
//                .addField("numNeutronRecords", radHelper.createCount()
//                        .label("Neutron Records Count")
//                        .id("numNeutronRecords")
//                        .value(parent.rapiscanLayerConfig.presets.size()))
//                .addField("NeutronRecords", radHelper.createArray()
//                        .withVariableSize("numNeutronRecords")
//                        .withElement("Neutron Scan", radHelper.createRecord()
//                                .addField("LaneID", radHelper.createText()
//                                        .label("Lane ID")
//                                        .definition(RADHelper.getRadUri("LaneID")))
//                                .addField("AlarmState", radHelper.createCategory()
//                                        .name("Alarm")
//                                        .label("Alarm")
//                                        .definition(RADHelper.getRadUri("alarm"))
//                                        .addAllowedValues("Alarm", "Background", "Scan", "Fault - Neutron High"))
//                                .addField("neutronCount", radHelper.createCount()
//                                        .label("Neutron Count")
//                                        .description("Count of the neutrons reported by the Rapiscan neutron scanner")
//                                        .id("neutronCount")
//                                        .value(parent.neutronCount))
//                                .addField("NeutronGrossCount", radHelper.createArray()
//                                        .label("NeutronGrossCount Array")
//                                        .definition(radHelper.getRadUri("neutron-gross-count"))
//                                        .withElement("neutronGrossCount",radHelper.createText())
//                                        .withVariableSize("neutronCount"))))
//                .build();
//        dataEncoding = radHelper.newTextEncoding(",", "\n");
//
//    }
//
//    public void onNewMessage(String[] csvString, long timeStamp, String alarmState, MessageHandler messageHandler){
//        createDataRecord();
//        DataBlock dataBlock = dataStruct.createDataBlock();
//        dataStruct.setData(dataBlock);
//
//        int index =0;
//        int currentLane = parent.messageHandlerList.indexOf(messageHandler);
//
//        dataBlock.setDoubleValue(index++, timeStamp/1000);
//        dataBlock.setIntValue(index++, parent.rapiscanLayerConfig.presets.size());
//
//        var neutronArray = ((DataArrayImpl) dataStruct.getComponent("NeutronRecords"));
//        neutronArray.updateSize();
//        dataBlock.updateAtomCount();
//
//        for(int i=0; i< parent.rapiscanLayerConfig.presets.size(); i++){
//            DataRecord neutronDataBlock = (DataRecord) neutronArray.getComponent(i);
//
//            dataBlock.setIntValue(index++, currentLane);
//            dataBlock.setStringValue(index++, alarmState);
//            dataBlock.setIntValue(index++, parent.neutronCount);
//
//            var neutronCount = ((DataArrayImpl) neutronDataBlock.getComponent("neutronGrossCount"));
//            neutronCount.updateSize();
//            dataBlock.updateAtomCount();
//
//            for(int j=1; j< csvString.length; j++){
//                dataBlock.setIntValue(index++, Integer.parseInt(csvString[j]));
//            }
//        }
//
//        dataBlock.updateAtomCount();
//        latestRecord = dataBlock;
//        eventHandler.publish(new DataEvent(timeStamp, NeutronOut.this, dataBlock));
//
//
//    }
//    @Override
//    public DataComponent getRecordDescription() {
//        return dataStruct;
//    }
//
//    @Override
//    public DataEncoding getRecommendedEncoding() {
//        return dataEncoding;
//    }
//
//    @Override
//    public double getAverageSamplingPeriod() {
//        return 0;
//    }
//}
