//package com.botts.impl.sensor.rapiscan;
//
//import net.opengis.swe.v20.DataBlock;
//import net.opengis.swe.v20.DataComponent;
//import net.opengis.swe.v20.DataEncoding;
//import net.opengis.swe.v20.DataRecord;
//import org.sensorhub.api.data.DataEvent;
//import org.sensorhub.api.sensor.PositionConfig;
//import org.sensorhub.impl.sensor.AbstractSensorOutput;
//import org.sensorhub.impl.utils.rad.RADHelper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.vast.data.DataArrayImpl;
//import org.vast.data.TextEncodingImpl;
//import org.vast.swe.SWEBuilders;
//
//import javax.xml.crypto.Data;
//
//public class RapiscanOutput extends AbstractSensorOutput<RapiscanSensor> {
//    private static final String SENSOR_OUTPUT_NAME = "Rapiscan Output";
//    private static final Logger logger = LoggerFactory.getLogger(RapiscanOutput.class);
//    protected DataRecord dataStruct;
//    protected DataEncoding dataEncoding;
//    DataBlock locationBlock;
//    DataBlock gammaBlock;
//    DataBlock neutronBlock;
//    DataBlock speedBlock;
//    DataBlock tamperBlock;
//    DataBlock occupancyBlock;
//    DataRecord gammaRecordBuilder;
//    DataRecord occupancyRecordBuilder;
//    DataRecord tamperRecordBuilder;
//    DataRecord speedRecordBuilder;
//    DataRecord locationRecordBuilder;
//    DataRecord neutronRecordBuilder;
//    DataRecord laneRecordBuilder;
//    SWEBuilders.DataArrayBuilder gammaBuilder;
//    SWEBuilders.DataArrayBuilder neutronBuilder;
//    SWEBuilders.DataArrayBuilder lanesBuilder;
//
//    public RapiscanOutput(RapiscanSensor parentSensor) {
//        super(SENSOR_OUTPUT_NAME, parentSensor);
//    }
//
//    protected void init(){
//
//        dataStruct = createDataRecord();
//        locationBlock = locationRecordBuilder.createDataBlock();
//        speedBlock = speedRecordBuilder.createDataBlock();
//        tamperBlock = tamperRecordBuilder.createDataBlock();
//        occupancyBlock = occupancyRecordBuilder.createDataBlock();
//        neutronBlock = neutronRecordBuilder.createDataBlock();
//        gammaBlock = gammaRecordBuilder.createDataBlock();
//    }
//
////    DataRecord createLocationRecord(){
////        RADHelper radHelper = new RADHelper();
////
////        return  locationStruct = radHelper.createRecord()
////                .name(getName())
////                .label("Location")
////                .definition(RADHelper.getRadUri("location-output"))
////                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
////                .addField("Sensor Location", radHelper.createLocationVectorLLA())
////                .build();
////
////    }
////    DataRecord createSpeedRecord(){
////        RADHelper radHelper = new RADHelper();
////        return  speedStruct = radHelper.createRecord()
////                .name(getName())
////                .label("Speed")
////                .definition(RADHelper.getRadUri("speed"))
////                .addField("Timestamp", radHelper.createPrecisionTimeStamp())
////                .addField("Speed", radHelper.createSpeed())
////                .build();
////    }
////    DataRecord createTamperRecord(){
////        RADHelper radHelper = new RADHelper();
////        return  tamperStruct = radHelper.createRecord()
////                .name(getName())
////                .label("Tamper")
////                .definition(RADHelper.getRadUri("tamper"))
////                .addField("Timestamp", radHelper.createPrecisionTimeStamp())
////                .addField("TamperState", radHelper.createTamperStatus())
////                .build();
////    }
////    DataRecord createOccupancyRecord(){
////        RADHelper radHelper = new RADHelper();
////        return occupancyStruct = radHelper.createRecord()
////                .name(getName())
////                .label("Occupancy")
////                .definition(RADHelper.getRadUri("occupancy"))
////                .addField("Timestamp", radHelper.createPrecisionTimeStamp())
////                .addField("OccupancyCount", radHelper.createOccupancyCount())
////                .addField("StartTime", radHelper.createOccupancyStartTime())
////                .addField("EndTime", radHelper.createOccupancyEndTime())
////                .addField("NeutronBackground", radHelper.createNeutronBackground())
////                .addField("GammaAlarm",
////                        radHelper.createBoolean()
////                                .name("gamma-alarm")
////                                .label("Gamma Alarm")
////                                .definition(RADHelper.getRadUri("gamma-alarm")))
////                .addField("NeutronAlarm",
////                        radHelper.createBoolean()
////                                .name("neutron-alarm")
////                                .label("Neutron Alarm")
////                                .definition(RADHelper.getRadUri("neutron-alarm")))
////                .build();
////    }
////    DataRecord createGammaRecord(){
////        RADHelper radHelper = new RADHelper();
////
////        SWEBuilders.DataRecordBuilder gammaRecBuilder = radHelper.createRecord()
////                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
////                .addField("Alarm State", radHelper.createCategory()
////                        .name("Alarm")
////                        .label("Alarm")
////                        .definition(RADHelper.getRadUri("alarm"))
////                        .addAllowedValues("Alarm", "Background", "Scan", "Fault - Gamma High", "Fault - Gamma Low"));
////
////        for (int i = 1; i < parent.gammaCount + 1; i++) {
////            gammaRecBuilder.addField("GammaGrossCount " + i, radHelper.createCount().name("GammaGrossCount")
////                    .label("Gamma Gross Count " + i)
////                    .definition(radHelper.getRadUri("gamma-gross-count")));
////        }
////
//////        gammaBuilder = radHelper.createArray()
//////                .name(name)
//////                .label("Gamma Scan")
//////                .definition(RADHelper.getRadUri("gamma-scan"))
//////                .withVariableSize("numLanes")
//////                .withElement("gamma", gammaRecordBuilder);
////
////
////        return gammaStruct = radHelper.createRecord()
////                .addField("Gamma", gammaRecBuilder).build();
////    }
////    DataRecord createNeutronRecord(){
////        RADHelper radHelper = new RADHelper();
////
////        SWEBuilders.DataRecordBuilder neutronRecBuilder = radHelper.createRecord()
////                .addField("SamplingTime", radHelper.createPrecisionTimeStamp())
////                .addField("AlarmState", radHelper.createCategory()
////                        .name("Alarm")
////                        .label("Alarm")
////                        .definition(RADHelper.getRadUri("alarm"))
////                        .addAllowedValues("Alarm", "Background", "Scan", "Fault - Neutron High"));
////        for (int i = 1; i < parent.neutronCount + 1; i++) {
////            neutronRecBuilder.addField("NeutronGrossCount " + i, radHelper.createCount().name("NeutronGrossCount")
////                    .label("Neutron Gross Count " + i)
////                    .definition(radHelper.getRadUri("neutron-gross-count")));
////        }
////
////        return neutronStruct = radHelper.createRecord()
////                .addField("Neutron", neutronRecBuilder).build();
////    }
//    DataRecord createDataRecord() {
//        RADHelper radHelper = new RADHelper();
//        dataEncoding = new TextEncodingImpl(",", "\n");
//
//        speedRecordBuilder = radHelper.createRecord()
//                .name(getName())
//                .label("Speed")
//                .definition(RADHelper.getRadUri("speed"))
//                .addField("Timestamp", radHelper.createPrecisionTimeStamp())
//                .addField("Speed", radHelper.createSpeed()).build();
//        tamperRecordBuilder = radHelper.createRecord()
//                .name(getName())
//                .label("Tamper")
//                .definition(RADHelper.getRadUri("tamper"))
//                .addField("Timestamp", radHelper.createPrecisionTimeStamp())
//                .addField("TamperState", radHelper.createTamperStatus()).build();
//        locationRecordBuilder = radHelper.createRecord()
//                .name(getName())
//                .label("Location")
//                .definition(RADHelper.getRadUri("location-output"))
//                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
//                .addField("Sensor Location", radHelper.createLocationVectorLLA()).build();
//        gammaRecordBuilder = radHelper.createRecord()
//                .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
//                .addField("Alarm State", radHelper.createCategory()
//                        .name("Alarm")
//                        .label("Alarm")
//                        .definition(RADHelper.getRadUri("alarm"))
//                        .addAllowedValues("Alarm", "Background", "Scan", "Fault - Gamma High", "Fault - Gamma Low")).build();
//        for (int i = 1; i < parent.gammaCount + 1; i++) {
//            gammaRecordBuilder.addField("GammaGrossCount " + i, radHelper.createCount().name("GammaGrossCount")
//                    .label("Gamma Gross Count " + i)
//                    .definition(radHelper.getRadUri("gamma-gross-count")).build());
//        }
//        gammaBuilder = radHelper.createArray()
//                .name(name)
//                .label("Gamma Scan")
//                .definition(RADHelper.getRadUri("gamma-scan"))
//                .withVariableSize("numLanes")
//                .withElement("gamma", gammaRecordBuilder);
//
//        neutronRecordBuilder = radHelper.createRecord()
//                .addField("SamplingTime", radHelper.createPrecisionTimeStamp())
//                .addField("AlarmState", radHelper.createCategory()
//                        .name("Alarm")
//                        .label("Alarm")
//                        .definition(RADHelper.getRadUri("alarm"))
//                        .addAllowedValues("Alarm", "Background", "Scan", "Fault - Neutron High")).build();
//        for (int i = 1; i < parent.neutronCount + 1; i++) {
//            neutronRecordBuilder.addField("NeutronGrossCount " + i, radHelper.createCount().name("NeutronGrossCount")
//                    .label("Neutron Gross Count " + i)
//                    .definition(radHelper.getRadUri("neutron-gross-count")).build());
//        }
//
//        occupancyRecordBuilder = radHelper.createRecord()
//                .name(getName())
//                .label("Occupancy")
//                .definition(RADHelper.getRadUri("occupancy"))
//                .addField("Timestamp", radHelper.createPrecisionTimeStamp())
//                .addField("OccupancyCount", radHelper.createOccupancyCount())
//                .addField("StartTime", radHelper.createOccupancyStartTime())
//                .addField("EndTime", radHelper.createOccupancyEndTime())
//                .addField("NeutronBackground", radHelper.createNeutronBackground())
//                .addField("GammaAlarm",
//                        radHelper.createBoolean()
//                                .name("gamma-alarm")
//                                .label("Gamma Alarm")
//                                .definition(RADHelper.getRadUri("gamma-alarm")))
//                .addField("NeutronAlarm",
//                        radHelper.createBoolean()
//                                .name("neutron-alarm")
//                                .label("Neutron Alarm")
//                                .definition(RADHelper.getRadUri("neutron-alarm"))).build();
//        neutronBuilder = radHelper.createArray()
//                .name(name)
//                .label("Neutron Scan")
//                .definition(RADHelper.getRadUri("neutron-scan"))
//                .withVariableSize("numLanes")
//                .withElement("neutron", neutronRecordBuilder);
//
//        laneRecordBuilder = radHelper.createRecord()
//                .addField("Location", locationRecordBuilder)
//                .addField("Speed", speedRecordBuilder)
//                .addField("Tamper", tamperRecordBuilder)
//                .addField("Occupancy", occupancyRecordBuilder)
//                .addField("Gamma", gammaBuilder)
//                .addField("Neutron", neutronBuilder).build();
//
//        lanesBuilder= radHelper.createArray()
//                .label("Lanes")
//                .description("List of Rapiscan Lane outputs")
//                .withVariableSize("numLanes")
//                .withElement("lane", laneRecordBuilder);
//
//        return dataStruct = radHelper.createRecord()
//                .name(SENSOR_OUTPUT_NAME)
//                .label("Rapiscan Output")
//                .description("Rapiscan Output")
//                .updatable(true)
//                .addField("sampleTime", radHelper.createTime()
//                        .asSamplingTimeIsoUTC()
//                        .description("Time of data collection"))
//                .addField("numLanes", radHelper.createCount()
//                        .label("Number of Lanes")
//                        .definition(radHelper.getRadUri("numLanes"))
//                        .description("Number of Traffic Lanes")
//                        .id("numLanes")
//                        .build())
//                .addField("Lanes", lanesBuilder)
//                .build();
//    }
//
//    public void setData(){
//        dataStruct = createDataRecord();
//        DataBlock dataBlock = dataStruct.createDataBlock();
//        dataStruct.setData(dataBlock);
//
//        long timestamp = System.currentTimeMillis() / 1000l;
//
//        int index = 0;
//
//        dataBlock.setLongValue(index++,timestamp);
//        dataBlock.setIntValue(index++, parent.rapiscanLayerConfig.presets.size());
//
//        var laneArray = (DataArrayImpl) dataStruct.getComponent("Lanes");
//        laneArray.updateSize();
//        dataBlock.updateAtomCount();
//
//        for(int i=0; i< parent.rapiscanLayerConfig.presets.size(); i++){
//            DataRecord laneDataBlock = (DataRecord) laneArray.getComponent(i);
//
//            dataBlock.setLongValue(index++, locationBlock.getLongValue(0));
//            dataBlock.setDoubleValue(index++, locationBlock.getDoubleValue(1));
//            dataBlock.setDoubleValue(index++, locationBlock.getDoubleValue(2));
//            dataBlock.setDoubleValue(index++, locationBlock.getDoubleValue(3));
//
//            dataBlock.setLongValue(index++, speedBlock.getLongValue());
//            dataBlock.setDoubleValue(index++, speedBlock.getDoubleValue());
//
//            dataBlock.setLongValue(index++, tamperBlock.getLongValue(0));
//            dataBlock.setBooleanValue(index++, tamperBlock.getBooleanValue(1));
//
//            dataBlock.setLongValue(index++, occupancyBlock.getLongValue(0));
//            dataBlock.setIntValue(index++, occupancyBlock.getIntValue(1));
//            dataBlock.setLongValue(index++, occupancyBlock.getLongValue(2));
//            dataBlock.setLongValue(index++, occupancyBlock.getLongValue(3));
//            dataBlock.setDoubleValue(index++, occupancyBlock.getDoubleValue(4));
//            dataBlock.setBooleanValue(index++, occupancyBlock.getBooleanValue(5));
//            dataBlock.setBooleanValue(index++, occupancyBlock.getBooleanValue(6));
//
//            dataBlock.setLongValue(index++, gammaBlock.getLongValue(0));
//            dataBlock.setStringValue(index++, gammaBlock.getStringValue(1));
//            dataBlock.setIntValue(index++, gammaBlock.getIntValue(2));
//
//            dataBlock.setLongValue(index++, neutronBlock.getLongValue(0));
//            dataBlock.setStringValue(index++, neutronBlock.getStringValue(1));
//            dataBlock.setIntValue(index++, neutronBlock.getIntValue(2));
//        }
//
//        latestRecord = dataBlock;
//        latestRecordTime = System.currentTimeMillis();
//        eventHandler.publish(new DataEvent(latestRecordTime, RapiscanOutput.this, dataBlock));
//
//    }
//    public void setLocationOutput(PositionConfig.LLALocation gpsLocation) {
//
//
//        locationRecordBuilder.setData(locationBlock);
//
//        long timestamp = System.currentTimeMillis() / 1000l;
//
//        locationBlock.setLongValue(0, timestamp);
//        locationBlock.setDoubleValue(1, gpsLocation.lat);
//        locationBlock.setDoubleValue(2, gpsLocation.lon);
//        locationBlock.setDoubleValue(3, gpsLocation.alt);
//        eventHandler.publish(new DataEvent(System.currentTimeMillis(), RapiscanOutput.this, locationBlock));
//    }
//    public void onNewSpeedMessage(String[] csvString) {
//
////        speedBlock = speedRecordBuilder.createDataBlock();
//        speedRecordBuilder.setData(speedBlock);
//
//        long timestamp = System.currentTimeMillis() / 1000l;
//        speedBlock.setLongValue(0, timestamp);
//        speedBlock.setDoubleValue(1, Double.parseDouble(csvString[1]));
//        eventHandler.publish(new DataEvent(System.currentTimeMillis(), RapiscanOutput.this, speedBlock));
//
//    }
//    public void onNewTamperMessage(boolean tamperState) {
//
//
//        tamperRecordBuilder.setData(tamperBlock);
//
//        long timestamp = System.currentTimeMillis() / 1000l;
//        tamperBlock.setLongValue(0, timestamp);
//        tamperBlock.setBooleanValue(1, tamperState);
//        eventHandler.publish(new DataEvent(System.currentTimeMillis(), RapiscanOutput.this, tamperBlock));
//    }
//    public void onNewOccupancyMessage(long startTime, long endTime, Boolean isGammaAlarm, Boolean isNeutronAlarm, String[] csvString) {
//
//
//        occupancyRecordBuilder.setData(occupancyBlock);
//
//        long timestamp = System.currentTimeMillis() / 1000l;
//        occupancyBlock.setLongValue(0, timestamp);
//        occupancyBlock.setIntValue(1, Integer.parseInt(csvString[1]));
//        occupancyBlock.setLongValue(2, startTime / 1000);
//        occupancyBlock.setLongValue(3, endTime / 1000);
//        occupancyBlock.setDoubleValue(4, Double.parseDouble(csvString[2]) / 1000);
//        occupancyBlock.setBooleanValue(5, isGammaAlarm);
//        occupancyBlock.setBooleanValue(6, isNeutronAlarm);
//
//        eventHandler.publish(new DataEvent(System.currentTimeMillis(), RapiscanOutput.this, occupancyBlock));
//
//    }
//
//    public void onNewNeutronMessage(String[] csvString, long timeStamp, String alarmState){
//
//
//        neutronRecordBuilder.setData(neutronBlock);
//
//        neutronBlock.setLongValue(0,timeStamp/1000);
//        neutronBlock.setStringValue(1, alarmState);
//        for(int i=2; i< parent.neutronCount+2; i++){
//            neutronBlock.setIntValue(i, Integer.parseInt(csvString[i-1]));
//        }
//        eventHandler.publish(new DataEvent(System.currentTimeMillis(), RapiscanOutput.this, neutronBlock));
//
//    }
//    public void onNewGammaMessage(String[] csvString, long timeStamp, String alarmState){
//
//
//        gammaRecordBuilder.setData(gammaBlock);
//
//        gammaBlock.setLongValue(0,timeStamp/1000);
//        gammaBlock.setStringValue(1, alarmState);
//        for(int i=2; i< parent.gammaCount+2; i++){
//            gammaBlock.setIntValue(i, Integer.parseInt(csvString[i-1]));
//        }
//
//        eventHandler.publish(new DataEvent(System.currentTimeMillis(), RapiscanOutput.this, gammaBlock));
//    }
//    @Override
//    public DataComponent getRecordDescription() {
//        return dataStruct;
//    }
//    @Override
//    public DataEncoding getRecommendedEncoding() {
//        return dataEncoding;
//    }
//    @Override
//    public double getAverageSamplingPeriod() {
//        return 0;
//    }
//}
