package org.sensorhub.impl.utils.rad;

import com.botts.impl.utils.n42.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import net.opengis.swe.v20.*;
import net.opengis.swe.v20.Boolean;
import org.vast.swe.SWEBuilders;
import org.vast.swe.helper.GeoPosHelper;

import java.io.StringReader;


public class RADHelper extends GeoPosHelper {


    public static String getRadUri(String propName) {
        return RADConstants.RAD_URI + propName;
    }

    public static String getRadInstrumentUri(String propName) {
        return RADConstants.RAD_INSTRUMENT_URI + propName;
    }
    public static String getRadDetectorURI(String propName) {
        return RADConstants.RAD_URI + "rad-detector-" + propName;
    }
    public static String getRadItemURI(String propName) {
        return RADConstants.RAD_URI + "rad-item-" + propName;
    }

    ///////// UNMARSHALLER ///////////////
    public RadInstrumentDataType getRadInstrumentData (String xmlString) {
        RadInstrumentDataType radInstrumentData = new RadInstrumentDataType();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(RadInstrumentDataType.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<RadInstrumentDataType> root = (JAXBElement<RadInstrumentDataType>) jaxbUnmarshaller.unmarshal(new StringReader(xmlString));
            radInstrumentData = root.getValue();
//            radInstrumentData = (RadInstrumentDataType) jaxbUnmarshaller.unmarshal(new StringReader(xmlString));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return radInstrumentData;
    }
    public Time createPrecisionTimeStamp() {
        return createTime()
                .asSamplingTimeIsoUTC()
                .name("time")
                .description("time stamp: when the message was received")
                .build();
    }

    public Quantity createSpeedTimeStamp() {
        return createQuantity()
                .name("speed-time")
                .label("Speed Time")
                .definition(getRadUri("speed-time"))
                .description("time it takes to cover 1 foot of distance")
                .uomCode("s")
                .build();
    }

    public Time createBackgroundTime() {
        return createTime()
                .name("background-time")
                .name("Background Time")
                .definition(getRadUri("background-time"))
                //range 20-120seconds
                .build();
    }
    public Time createOccupancyStartTime(){
        return createTime()
                .asPhenomenonTimeIsoUTC()
                .name("start-time")
                .label("Start Time")
                .definition(getRadUri("occupancy-start-time"))
                .description("The start time of occupancy data")
                .build();
    }

    public Time createOccupancyEndTime(){
        return createTime()
                .asPhenomenonTimeIsoUTC()
                .name("end-time")
                .label("End Time")
                .definition(getRadUri("occupancy-end-time"))
                .description("The end time of occupancy data")
                .build();
    }

    public Boolean createTamperStatus(){
        return createBoolean()
                .name("TamperStatus")
                .label("Tamper Status")
                .definition(getRadUri("tamper-status"))
                .description("True if the rpm is currently reporting a Tamper state")
                .build();
    }


    public Quantity createNeutronBackground(){
        return createQuantity()
                .name("NeutronBackgroundCount")
                .label("Neutron Background Count")
                .definition(getRadUri("neutron-background-count"))
                .description("neutron count to start occupancy")
                .build();
    }

    public Quantity createNSigma(){
        return createQuantity()
                .name("NSigma")
                .label("NSigma")
                .definition(getRadUri("n-sigma"))
                .description("formula determines the number of counts above background that will trigger a radiation alarm")
                .build();
    }
    public Quantity createZmaxValue(){
        return createQuantity()
                .name("zmax-value")
                .label("Zmax Value")
                .definition(getRadUri("zmax-value"))
                .description("max z-value")
                .build();
    }

    public Quantity createAlphaValue(){
        return createQuantity()
                .name("alpha-value")
                .label("Alpha Value")
                .definition(getRadUri("alpha-value"))
                .description("value used in calculations")
                .build();
    }
    public Quantity createOccupancyHoldin(){
        return createQuantity()
                .name("occupancy-holdin")
                .label("Occupancy Holdin")
                .definition(getRadUri("occupancy-holdin"))
                .description("number of 200ms time intervals to hold in after occupancy signal indicates the system is vacant")
                .build();
    }
    public Quantity createIntervals(){
        return createQuantity()
                .name("Intervals")
                .label("Intervals")
                .definition(getRadUri("intervals"))
                .description("the number of time intervals to be considered")
                .build();
    }

    public Quantity createSequentialIntervals(){
        return createQuantity()
                .name("sequential-intervals")
                .label("Sequential Intervals")
                .definition(getRadUri("sequential-intervals"))
                .build();
    }

    public Quantity createMaxIntervals(){
        return createQuantity()
                .name("Maximum Intervals")
                .label("Maximum Intervals")
                .definition(getRadUri("maximum-intervals"))
                .build();
    }
    public Text createPlaceholder(){
        return createText()
                .name("Placeholder")
                .label("Placeholder")
                .definition(getRadUri("placeholder"))
                .build();
    }
    public Quantity createBackgroundNSigma(){
        return createQuantity()
                .name("Background NSigma")
                .label("Background NSigma")
                .definition(getRadUri("background-nsigma"))
                .description("sets a sigma value for a \"throw-through alarm")
//                .description("is used to set alarm thresholds for detecting anomalies in radiation levels")
                .build();
    }

    public Quantity createHighBackgroundFault(){
        return createQuantity()
                .name("High Background Fault")
                .label("High Background Fault")
                .definition(getRadUri("high-background-fault"))
                .description("threshold value measured in counts per second(cps) within a radiation detection system that will trigger a fault")
//                .uomCode("cps")
                .build();
    }
    public Quantity createLowBackgroundFault(){
        return createQuantity()
                .name("Low Background Fault")
                .label("Low Background Fault")
                .definition(getRadUri("low-background-fault"))
                .description("threshold value measured in counts per second(cps) within a radiation detection system that will trigger a fault")
//                .uomCode("cps")
                .build();
    }
    public Quantity createDetectors(){
        return createQuantity()
                .name("detectors-on-line")
                .label("Detectors on line")
                .definition(getRadUri("detectors-on-line"))
                .description("radiation detectors that are actively operating and connected to the monitoring system")
                .build();
    }
    public Quantity createUpperControlDiscriminator(){
        return createQuantity()
                .name("control-upper-level-discriminator")
                .label("Upper Level Discriminator ULD")
                .definition(getRadUri("control-upper-level-discriminator"))
                .description("threshold setting in radiation detector that defines the max energy level for detecting radiation")
                .build();
    }
    public Quantity createLowerControlDiscriminator(){
        return createQuantity()
                .name("control-lower-level-discriminator")
                .label("Lower Level Discriminator LLD")
                .definition(getRadUri("control-lower-level-discriminator"))
                .description("threshold setting in radiation detector that defines the minimum energy level for detecting radiation")
                .build();
    }
    public Quantity createAuxiliaryUpperDiscriminator(){
        return createQuantity()
                .name("auxiliary-upper-level-discriminator")
                .label("Auxiliary Upper Level Discriminator")
                .definition(getRadUri("auxiliary-level-upper-discriminator"))
                .description("threshold setting that defines an upper limit for the energy of detected radiation events")
                .build();
    }

    public Quantity createAuxiliaryLowerDiscriminator(){
        return createQuantity()
                .name("auxiliary-lower-level-discriminator")
                .label("Auxiliary Lower Level Discriminator")
                .definition(getRadUri("auxiliary-level-lower-discriminator"))
                .description("threshold setting that defines an lower limit for the energy of detected radiation events")
                .build();
    }
    public Quantity createRelayOutput(){
        return createQuantity()
                .name("relay-output")
                .label("Relay Output")
                .definition(getRadUri("relay-output"))
                .description("electrical output signal that can activate or control other devices based on detection of radiation")
                .build();
    }
    public Quantity createAlgorithm(){
        return createQuantity()
                .name("Algorithm")
                .label("Algorithm")
                .definition(getRadUri("algorithm"))
                .description("permits the operator to select which detectors will be included in the alarm calculations")
                .build();
    }

    public Text createSoftwareVersion(){
        return createText()
                .name("software-version")
                .label("Software version")
                .definition(getRadUri("software-version"))
                .addAllowedValues("A", "T")
                .build();
    }
    public Quantity createFirmwareVersion(){
        return createQuantity()
                .name("firmware-version")
                .label("Firmware version")
                .definition(getRadUri("firmware-version"))
                .build();
    }

    public Text createLaneId(){
        return createText()
                .label("Lane ID")
                .definition(RADHelper.getRadUri("LaneID"))
                .description("identifies the lane for each rpm system")
                .build();
    }


    public Quantity createSpeedMph(){
        return createQuantity()
                .name("Speed")
                .label("Speed")
                .definition(getRadUri("speed-mph"))
                .uomCode("mph")
                //max 99
                .build();
    }
    public Quantity createSpeedKph(){
        return createQuantity()
                .name("Speed")
                .label("Speed")
                .definition(getRadUri("speed-kph"))
                .uomCode("kph")
                //max 999
                .build();
    }

    public Count createOccupancyCount(){
        return createCount()
                .name("occupancy-count")
                .label("Pillar Occupancy Count")
                .definition(getRadUri("pillar-occupancy-count"))
                .description("incremented count every time the pillar clears the occupancy, resets daily and on power cycle")
                .build();
    }

    public Quantity createBatteryCharge(){
        return createQuantity()
                .name("BatteryCharge")
                .label("Battery Charge")
                .definition(getRadInstrumentUri("battery-charge"))
                .uomCode("%")
                .build();
    }

    public Quantity createOccupancyNeutronBackground(){
        return createQuantity()
                .name("occupancy-neutron-background")
                .label("Occupancy Neutron Background")
                .definition(getRadUri("occupancy-neutron-background"))
                .build();
    }

    public DataArray createLinCalibration(){
        return createArray()
                .name("LinCalibration")
                .label("Lin Calibration")
                .definition(getRadUri("lin-cal"))
                .withFixedSize(3)
                .withElement("LinCalibrationValues", createQuantity()
                        .label("Lin Calibration Values")
                        .definition(getRadUri("lin-cal-vals"))
                        .description("Linear Calibration Values")
                        .dataType(DataType.DOUBLE)
                        .build())
                .build();
    }

    public DataArray createCmpCalibration(){

        return createArray()
                .name("CmpCalibration")
                .label("Cmp Calibration")
                .definition(getRadUri("cmp-cal"))
                .withFixedSize(3)
                .withElement("CmpCalibrationValues", createQuantity()
                        .label("Cmp Calibration Values")
                        .definition(getRadUri("cmp-cal-vals"))
                        .description("Calibration Values")
                        .dataType(DataType.DOUBLE)
                        .build())
                .build();
    }

    public Count createArraySize(String name, String fieldID){
        return createCount()
                .name(name)
                .label(name)
                .description("length of array")
                .id(fieldID)
                .build();
    }

    public DataArray createLinSpectrum(String fieldID){
        return createArray()
                .name("LinSpectrum")
                .label("Lin Spectrum")
                .definition(getRadUri("lin-spectrum"))
                .withVariableSize(fieldID)
                .withElement("LinSpectrumValues", createQuantity()
                        .label("Lin Spectrum Values")
                        .definition(getRadUri("lin-spectrum-vals"))
                        .description("Spectrum Values")
                        .dataType(DataType.DOUBLE)
                        .build())
                .build();
    }

    public DataArray createCmpSpectrum(String fieldID){
        return createArray()
                .name("CmpSpectrum")
                .label("Cmp Spectrum")
                .definition(getRadUri("cmp-spectrum"))
                .withVariableSize(fieldID)
                .withElement("CmpSpectrumValues", createQuantity()
                        .label("Cmp Spectrum Values")
                        .definition(getRadUri("cmp-spectrum-vals"))
                        .description("Spectrum Values")
                        .dataType(DataType.DOUBLE)
                        .build())
                .build();
    }

    public DataRecord newGammaOutput(int gammaCount){
        SWEBuilders.DataRecordBuilder recordBuilder;
        DataRecord datarec;

        recordBuilder = this.createRecord()
                .name("Gamma Scan")
                .label("Gamma Scan")
                .definition(RADHelper.getRadUri("gamma-scan"))
                .addField("Sampling Time", createPrecisionTimeStamp())
                .addField("Alarm State", createCategory()
                                .name("Alarm")
                                .label("Alarm")
                                .definition(RADHelper.getRadUri("alarm"))
                                .addAllowedValues("Alarm", "Background", "Scan", "Fault - Gamma High", "Fault - Gamma Low"));


        for(int i=1; i<gammaCount+1; i++){
            recordBuilder.addField("GammaGrossCount "+ i , createCount().name("GammaGrossCount")
                    .label("Gamma Gross Count "+ i)
                    .definition(getRadUri("gamma-gross-count")));
        }
        datarec = recordBuilder.build();

        return datarec;
    }
//    public DataRecord newLocationOutput(){
//        SWEBuilders.DataRecordBuilder recordBuilder;
//        DataRecord datarec;
//
//        recordBuilder = this.createRecord()
//                .name("Location")
//                .label("Location")
//                .definition(RADHelper.getRadUri("location-output"))
//                .addField("Sampling Time", createPrecisionTimeStamp())
//                .addField("Sensor Location", createLocationVectorLLA());
//
//        datarec = recordBuilder.build();
//
//        return datarec;
//    }
//    public DataRecord newSpeedOutput(){
//        SWEBuilders.DataRecordBuilder recordBuilder;
//        DataRecord datarec;
//
//        recordBuilder = this.createRecord()
//                .name("Speed")
//                .label("Speed")
//                .definition(RADHelper.getRadUri("speed"))
//                .addField("Timestamp",createPrecisionTimeStamp())
//                .addField("Speed", createSpeed());
//
//        datarec = recordBuilder.build();
//
//        return datarec;
//    }
//    public DataRecord newTamperOutput(){
//        SWEBuilders.DataRecordBuilder recordBuilder;
//        DataRecord datarec;
//
//        recordBuilder = this.createRecord()
//                .name("Tamper")
//                .label("Tamper")
//                .definition(RADHelper.getRadUri("tamper"))
//                .addField("Timestamp", createPrecisionTimeStamp())
//                .addField("TamperState", createTamperStatus());
//        datarec = recordBuilder.build();
//
//        return datarec;
//    }
//    public DataRecord newOccupancyOutput(){
//        SWEBuilders.DataRecordBuilder recordBuilder;
//        DataRecord datarec;
//
//        recordBuilder = this.createRecord()
//                .name("Occupancy")
//                .label("Occupancy")
//                .definition(RADHelper.getRadUri("occupancy"))
//                .addField("Timestamp", createPrecisionTimeStamp())
//                .addField("OccupancyCount", createOccupancyCount())
//                .addField("StartTime", createOccupancyStartTime())
//                .addField("EndTime", createOccupancyEndTime())
//                .addField("NeutronBackground", createNeutronBackground())
//                .addField("GammaAlarm", createBoolean()
//                                .name("gamma-alarm")
//                                .label("Gamma Alarm")
//                                .definition(RADHelper.getRadUri("gamma-alarm")))
//                .addField("NeutronAlarm", createBoolean()
//                                .name("neutron-alarm")
//                                .label("Neutron Alarm")
//                                .definition(RADHelper.getRadUri("neutron-alarm")));
//
//        datarec = recordBuilder.build();
//
//        return datarec;
//    }


    public DataRecord newNeutronOutput(int neutronCount){
        SWEBuilders.DataRecordBuilder recordBuilder;
        DataRecord datarec;

        recordBuilder = this.createRecord()
                .name("Neutron Scan")
                .label("Neutron Scan")
                .definition(RADHelper.getRadUri("neutron-scan"))
                .addField("SamplingTime", createPrecisionTimeStamp())
                .addField("AlarmState", createCategory()
                                .name("Alarm")
                                .label("Alarm")
                                .definition(RADHelper.getRadUri("alarm"))
                                .addAllowedValues("Alarm", "Background", "Scan", "Fault - Neutron High"));


        for(int i=1; i<neutronCount+1; i++){
            recordBuilder.addField("NeutronGrossCount "+ i, createCount().name("NeutronGrossCount")
                    .label("Neutron Gross Count "+i)
                    .definition(getRadUri("neutron-gross-count")));
        }


        datarec = recordBuilder.build();

        return datarec;
    }

    public Count createGammaGrossCount(){
        return createCount()
                .name("GammaGrossCount")
                .label("Gamma Gross Count")
                .definition(getRadUri("gamma-gross-count"))
                .build();
    }

    public Count createNeutronGrossCount(){
        return createCount()
                .name("NeutronGrossCount")
                .label("Neutron Gross Count")
                .definition(getRadUri("neutron-gross-count"))
                .build();
    }


    public Quantity createDoseUSVh(){
        return createQuantity()
                .name("Dose")
                .label("Dose")
                .definition(getRadUri("dose"))
                .uomCode("uSv/h")
                .build();
    }

    public Category createMeasurementClassCode(){
        return createCategory()
                .name("MeasurementClassCode")
                .label("Measurement Class Code")
                .definition(getRadUri("measurement-class-code"))
                .addAllowedValues(MeasurementClassCodeSimpleType.FOREGROUND.value(), MeasurementClassCodeSimpleType.INTRINSIC_ACTIVITY.value(), MeasurementClassCodeSimpleType.BACKGROUND.value(), MeasurementClassCodeSimpleType.NOT_SPECIFIED.value(), MeasurementClassCodeSimpleType.CALIBRATION.value())
                .build();
    }

    public Category createAlarmCatCode(){
        return createCategory()
                .name("AlarmCategoryCode")
                .label("Alarm Category Code")
                .definition(getRadUri("alarm-category-code"))
                .addAllowedValues(RadAlarmCategoryCodeSimpleType.ALPHA.value(),RadAlarmCategoryCodeSimpleType.NEUTRON.value(),RadAlarmCategoryCodeSimpleType.BETA.value(),RadAlarmCategoryCodeSimpleType.GAMMA.value(),RadAlarmCategoryCodeSimpleType.OTHER.value(),RadAlarmCategoryCodeSimpleType.ISOTOPE.value())
                .build();
    }


        //////////////////////////////// vvvv OLD vvvvvv ///////////////////////////////

    // RadInstrumentInformation
    public Text createRIManufacturerName() {
        return createText()
                .name("RadInstrumentManufacturerName")
                .label("Rad Instrument Manufacturer Name")
                .definition(getRadInstrumentUri("manufacturer-name"))
                .description("Manufacturer name of described RAD Instrument")
                .build();
    }

    public Category createRIIdentifier() {
        return createCategory()
                .name("RadInstrumentIdentifier")
                .label("Rad Instrument Identifier")
                .definition(getRadInstrumentUri("identifier"))
                .description("Identifier for described RAD Instrument")
                .build();
    }

    public Text createRIModelName() {
        return createText()
                .name("RadInstrumentModelName")
                .label("Rad Instrument Model Name")
                .definition(getRadInstrumentUri("model-name"))
                .description("Model name of described RAD Instrument")
                .build();
    }

    public Text createRIDescription() {
        return createText()
                .name("RadInstrumentDescription")
                .label("Rad Instrument Description")
                .definition(getRadInstrumentUri("description"))
                .description("Description of RAD Instrument")
                .build();
    }

    public Category createRIClassCode() {
        return createCategory()
                .name("RadInstrumentClassCode")
                .label("Rad Instrument Class Code")
                .definition(getRadInstrumentUri("class-code"))
                .description("Class Code for type of RAD Instrument")
                .addAllowedValues("Backpack", "Dosimeter", "Electronic Personal Emergency Radiation Detector", "Mobile System", "Network Area Monitor", "Neutron Handheld", "Personal Radiation Detector", "Radionuclide Identifier", "Portal Monitor", "Spectroscopic Portal Monitor", "Spectroscopic Personal Radiation Detector", "Gamma Handheld", "Transportable System", "Other")
                .build();
    }

    public DataRecord createRIVersion() {
        return createRecord()
                .name("RadInstrumentVersion")
                .label("Rad Instrument Version")
                .definition(getRadInstrumentUri("version"))
                .addField("RadInstrumentComponentName", createRIComponentName())
                .addField("RadInstrumentComponentVersion", createRIComponentVersion())
                .build();
    }

    public Text createRIComponentName() {
        return createText()
                .name("RadInstrumentComponentName")
                .label("Rad Instrument Component Name")
                .definition(getRadInstrumentUri("component-name"))
                .build();
    }

    public Text createRIComponentVersion() {
        return createText()
                .name("RadInstrumentComponentVersion")
                .label("Rad Instrument Component Version")
                .definition(getRadInstrumentUri("component-version"))
                .build();
    }
    // TODO: Create Record for Quality Control
//    public DataRecord createRIQualityControl(){
//        return createRecord()
//    }

    public DataRecord createRICharacteristics() {
        return createRecord()
                .name("RadInstrumentCharacteristics")
                .label("Rad Instrument Characteristics")
                .definition(getRadInstrumentUri("characteristics"))
                .build();
    }

    public DataRecord createCharacteristicGroup() {
        return createRecord()
                .name("CharacteristicGroupName")
                .label("Characteristic Group Name")
                .definition(getRadInstrumentUri("characteristic-group"))
                .build();
    }

    public DataRecord createCharacteristicText() {
        DataRecord record = createRecord()
                .name("")
                .addField("CharacteristicName",
                        createText()
                                .name("CharacteristicName")
                                .label("Characteristic Name")
                                .definition(getRadInstrumentUri("characteristic-name"))
                                .build())
                .addField("CharacteristicValue",
                        createText()
                                .name("CharacteristicValue")
                                .label("Characteristic Value")
                                .definition(getRadInstrumentUri("characteristic-value"))
                                .build())
                .addField("CharacteristicValueUnits",
                        createText()
                                .name("CharacteristicValueUnits")
                                .label("Characteristic Value Units")
                                .definition(getRadInstrumentUri("characteristic-value-units"))
                                .build())
                .addField("CharacteristicValueDataClassCode",
                        createCategory()
                                .name("CharacteristicValueDataClassCode")
                                .label("Characteristic Value Data Class Code")
                                .definition(getRadInstrumentUri("characteristic-value-data-class-code"))
                                .addAllowedValues(ValueDataClassCodeSimpleType.ANY_URI.value(), ValueDataClassCodeSimpleType.BASE_64_BINARY.value(), ValueDataClassCodeSimpleType.BOOLEAN.value(), ValueDataClassCodeSimpleType.BYTE.value(), ValueDataClassCodeSimpleType.DATE.value(), ValueDataClassCodeSimpleType.DATE_TIME.value(), ValueDataClassCodeSimpleType.DECIMAL.value(), ValueDataClassCodeSimpleType.DOUBLE.value(), ValueDataClassCodeSimpleType.DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.DURATION.value(), ValueDataClassCodeSimpleType.FLOAT.value(), ValueDataClassCodeSimpleType.HEX_BINARY.value(), ValueDataClassCodeSimpleType.ID.value(), ValueDataClassCodeSimpleType.IDREF.value(), ValueDataClassCodeSimpleType.IDREFS.value(), ValueDataClassCodeSimpleType.INT.value(), ValueDataClassCodeSimpleType.INTEGER.value(), ValueDataClassCodeSimpleType.LONG.value(), ValueDataClassCodeSimpleType.NAME.value(), ValueDataClassCodeSimpleType.NC_NAME.value(), ValueDataClassCodeSimpleType.NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_BLANK_STRING.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NORMALIZED_STRING.value(), ValueDataClassCodeSimpleType.PERCENT.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER_LIST.value(), ValueDataClassCodeSimpleType.SHORT.value(), ValueDataClassCodeSimpleType.STRING.value(), ValueDataClassCodeSimpleType.STRING_LIST.value(), ValueDataClassCodeSimpleType.TIME.value(), ValueDataClassCodeSimpleType.TOKEN.value(), ValueDataClassCodeSimpleType.UNSIGNED_BYTE.value(), ValueDataClassCodeSimpleType.UNSIGNED_INT.value(), ValueDataClassCodeSimpleType.UNSIGNED_LONG.value(), ValueDataClassCodeSimpleType.UNSIGNED_SHORT.value(), ValueDataClassCodeSimpleType.ZERO_TO_ONE_DOUBLE.value())
                                .build())
                .build();
        return record;
    }

    public DataRecord createCharacteristicQuantity() {
        DataRecord record = createRecord()
                .name("")
                .addField("CharacteristicName",
                        createText()
                                .name("CharacteristicName")
                                .label("Characteristic Name")
                                .definition(getRadInstrumentUri("characteristic-name"))
                                .build())
                .addField("CharacteristicValue",
                        createQuantity()
                                .name("CharacteristicValue")
                                .label("Characteristic Value")
                                .definition(getRadInstrumentUri("characteristic-value"))
                                .build())
                .addField("CharacteristicValueUnits",
                        createText()
                                .name("CharacteristicValueUnits")
                                .label("Characteristic Value Units")
                                .definition(getRadInstrumentUri("characteristic-value-units"))
                                .build())
                .addField("CharacteristicValueDataClassCode",
                        createCategory()
                                .name("CharacteristicValueDataClassCode")
                                .label("Characteristic Value Data Class Code")
                                .definition(getRadInstrumentUri("characteristic-value-data-class-code"))
                                .addAllowedValues(ValueDataClassCodeSimpleType.ANY_URI.value(), ValueDataClassCodeSimpleType.BASE_64_BINARY.value(), ValueDataClassCodeSimpleType.BOOLEAN.value(), ValueDataClassCodeSimpleType.BYTE.value(), ValueDataClassCodeSimpleType.DATE.value(), ValueDataClassCodeSimpleType.DATE_TIME.value(), ValueDataClassCodeSimpleType.DECIMAL.value(), ValueDataClassCodeSimpleType.DOUBLE.value(), ValueDataClassCodeSimpleType.DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.DURATION.value(), ValueDataClassCodeSimpleType.FLOAT.value(), ValueDataClassCodeSimpleType.HEX_BINARY.value(), ValueDataClassCodeSimpleType.ID.value(), ValueDataClassCodeSimpleType.IDREF.value(), ValueDataClassCodeSimpleType.IDREFS.value(), ValueDataClassCodeSimpleType.INT.value(), ValueDataClassCodeSimpleType.INTEGER.value(), ValueDataClassCodeSimpleType.LONG.value(), ValueDataClassCodeSimpleType.NAME.value(), ValueDataClassCodeSimpleType.NC_NAME.value(), ValueDataClassCodeSimpleType.NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_BLANK_STRING.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NORMALIZED_STRING.value(), ValueDataClassCodeSimpleType.PERCENT.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER_LIST.value(), ValueDataClassCodeSimpleType.SHORT.value(), ValueDataClassCodeSimpleType.STRING.value(), ValueDataClassCodeSimpleType.STRING_LIST.value(), ValueDataClassCodeSimpleType.TIME.value(), ValueDataClassCodeSimpleType.TOKEN.value(), ValueDataClassCodeSimpleType.UNSIGNED_BYTE.value(), ValueDataClassCodeSimpleType.UNSIGNED_INT.value(), ValueDataClassCodeSimpleType.UNSIGNED_LONG.value(), ValueDataClassCodeSimpleType.UNSIGNED_SHORT.value(), ValueDataClassCodeSimpleType.ZERO_TO_ONE_DOUBLE.value())
                                .build())
                .build();
        return record;
    }

    public DataRecord createCharacteristicCount() {
        DataRecord record = createRecord()
                .name("")
                .addField("CharacteristicName",
                        createText()
                                .name("CharacteristicName")
                                .label("Characteristic Name")
                                .definition(getRadInstrumentUri("characteristic-name"))
                                .build())
                .addField("CharacteristicValue",
                        createCount()
                                .name("CharacteristicValue")
                                .label("Characteristic Value")
                                .definition(getRadInstrumentUri("characteristic-value"))
                                .build())
                .addField("CharacteristicValueUnits",
                        createText()
                                .name("CharacteristicValueUnits")
                                .label("Characteristic Value Units")
                                .definition(getRadInstrumentUri("characteristic-value-units"))
                                .build())
                .addField("CharacteristicValueDataClassCode",
                        createCategory()
                                .name("CharacteristicValueDataClassCode")
                                .label("Characteristic Value Data Class Code")
                                .definition(getRadInstrumentUri("characteristic-value-data-class-code"))
                                .addAllowedValues(ValueDataClassCodeSimpleType.ANY_URI.value(), ValueDataClassCodeSimpleType.BASE_64_BINARY.value(), ValueDataClassCodeSimpleType.BOOLEAN.value(), ValueDataClassCodeSimpleType.BYTE.value(), ValueDataClassCodeSimpleType.DATE.value(), ValueDataClassCodeSimpleType.DATE_TIME.value(), ValueDataClassCodeSimpleType.DECIMAL.value(), ValueDataClassCodeSimpleType.DOUBLE.value(), ValueDataClassCodeSimpleType.DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.DURATION.value(), ValueDataClassCodeSimpleType.FLOAT.value(), ValueDataClassCodeSimpleType.HEX_BINARY.value(), ValueDataClassCodeSimpleType.ID.value(), ValueDataClassCodeSimpleType.IDREF.value(), ValueDataClassCodeSimpleType.IDREFS.value(), ValueDataClassCodeSimpleType.INT.value(), ValueDataClassCodeSimpleType.INTEGER.value(), ValueDataClassCodeSimpleType.LONG.value(), ValueDataClassCodeSimpleType.NAME.value(), ValueDataClassCodeSimpleType.NC_NAME.value(), ValueDataClassCodeSimpleType.NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_BLANK_STRING.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NORMALIZED_STRING.value(), ValueDataClassCodeSimpleType.PERCENT.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER_LIST.value(), ValueDataClassCodeSimpleType.SHORT.value(), ValueDataClassCodeSimpleType.STRING.value(), ValueDataClassCodeSimpleType.STRING_LIST.value(), ValueDataClassCodeSimpleType.TIME.value(), ValueDataClassCodeSimpleType.TOKEN.value(), ValueDataClassCodeSimpleType.UNSIGNED_BYTE.value(), ValueDataClassCodeSimpleType.UNSIGNED_INT.value(), ValueDataClassCodeSimpleType.UNSIGNED_LONG.value(), ValueDataClassCodeSimpleType.UNSIGNED_SHORT.value(), ValueDataClassCodeSimpleType.ZERO_TO_ONE_DOUBLE.value())
                                .build())
                .build();
        return record;
    }

    public DataRecord createCharacteristicBoolean() {
        DataRecord record = createRecord()
                .name("")
                .addField("CharacteristicName",
                        createText()
                                .name("CharacteristicName")
                                .label("Characteristic Name")
                                .definition(getRadInstrumentUri("characteristic-name"))
                                .build())
                .addField("CharacteristicValue",
                        createBoolean()
                                .name("CharacteristicValue")
                                .label("Characteristic Value")
                                .definition(getRadInstrumentUri("characteristic-value"))
                                .build())
                .addField("CharacteristicValueUnits",
                        createText()
                                .name("CharacteristicValueUnits")
                                .label("Characteristic Value Units")
                                .definition(getRadInstrumentUri("characteristic-value-units"))
                                .build())
                .addField("CharacteristicValueDataClassCode",
                        createCategory()
                                .name("CharacteristicValueDataClassCode")
                                .label("Characteristic Value Data Class Code")
                                .definition(getRadInstrumentUri("characteristic-value-data-class-code"))
                                .addAllowedValues(ValueDataClassCodeSimpleType.ANY_URI.value(), ValueDataClassCodeSimpleType.BASE_64_BINARY.value(), ValueDataClassCodeSimpleType.BOOLEAN.value(), ValueDataClassCodeSimpleType.BYTE.value(), ValueDataClassCodeSimpleType.DATE.value(), ValueDataClassCodeSimpleType.DATE_TIME.value(), ValueDataClassCodeSimpleType.DECIMAL.value(), ValueDataClassCodeSimpleType.DOUBLE.value(), ValueDataClassCodeSimpleType.DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.DURATION.value(), ValueDataClassCodeSimpleType.FLOAT.value(), ValueDataClassCodeSimpleType.HEX_BINARY.value(), ValueDataClassCodeSimpleType.ID.value(), ValueDataClassCodeSimpleType.IDREF.value(), ValueDataClassCodeSimpleType.IDREFS.value(), ValueDataClassCodeSimpleType.INT.value(), ValueDataClassCodeSimpleType.INTEGER.value(), ValueDataClassCodeSimpleType.LONG.value(), ValueDataClassCodeSimpleType.NAME.value(), ValueDataClassCodeSimpleType.NC_NAME.value(), ValueDataClassCodeSimpleType.NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_BLANK_STRING.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NORMALIZED_STRING.value(), ValueDataClassCodeSimpleType.PERCENT.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER_LIST.value(), ValueDataClassCodeSimpleType.SHORT.value(), ValueDataClassCodeSimpleType.STRING.value(), ValueDataClassCodeSimpleType.STRING_LIST.value(), ValueDataClassCodeSimpleType.TIME.value(), ValueDataClassCodeSimpleType.TOKEN.value(), ValueDataClassCodeSimpleType.UNSIGNED_BYTE.value(), ValueDataClassCodeSimpleType.UNSIGNED_INT.value(), ValueDataClassCodeSimpleType.UNSIGNED_LONG.value(), ValueDataClassCodeSimpleType.UNSIGNED_SHORT.value(), ValueDataClassCodeSimpleType.ZERO_TO_ONE_DOUBLE.value())
                                .build())
                .build();
        return record;
    }

    // RAD DETECTOR INFORMATION

    public Text createRadDetectorName(){
        return createText()
                .name("RadDetectorName")
                .label("Rad Detector Name")
                .definition(getRadDetectorURI("name"))
                .build();
    }

    public Category createRadDetectorCategoryCode(){
        return createCategory()
                .name("RadDetectorCategoryCode")
                .label("Rad Detector Category Code")
                .definition(getRadDetectorURI("category-code"))
                .addAllowedValues(RadDetectorCategoryCodeSimpleType.GAMMA.value(), RadDetectorCategoryCodeSimpleType.NEUTRON.value(), RadDetectorCategoryCodeSimpleType.ALPHA.value(), RadDetectorCategoryCodeSimpleType.BETA.value(), RadDetectorCategoryCodeSimpleType.X_RAY.value(), RadDetectorCategoryCodeSimpleType.OTHER.value())
                .build();
    }

    public Category createRadDetectorKindCode(){
        return createCategory()
                .name("RadDetectorKindCode")
                .label("Rad Detector Kind Code")
                .definition(getRadDetectorURI("kind-code"))
                .addAllowedValues(RadDetectorKindCodeSimpleType.HP_GE.value(), RadDetectorKindCodeSimpleType.HP_XE.value(), RadDetectorKindCodeSimpleType.NA_I.value(), RadDetectorKindCodeSimpleType.LA_BR_3.value(), RadDetectorKindCodeSimpleType.LA_CL_3.value(), RadDetectorKindCodeSimpleType.BGO.value(), RadDetectorKindCodeSimpleType.CZT.value(), RadDetectorKindCodeSimpleType.CD_TE.value(), RadDetectorKindCodeSimpleType.CS_I.value(), RadDetectorKindCodeSimpleType.GMT.value(), RadDetectorKindCodeSimpleType.GMTW.value(), RadDetectorKindCodeSimpleType.LI_FIBER.value(), RadDetectorKindCodeSimpleType.PVT.value(), RadDetectorKindCodeSimpleType.PS.value(), RadDetectorKindCodeSimpleType.HE_3.value(), RadDetectorKindCodeSimpleType.HE_4.value(), RadDetectorKindCodeSimpleType.LI_GLASS.value(), RadDetectorKindCodeSimpleType.LI_I.value(), RadDetectorKindCodeSimpleType.SR_I_2.value(), RadDetectorKindCodeSimpleType.CLYC.value(), RadDetectorKindCodeSimpleType.CD_WO_4.value(), RadDetectorKindCodeSimpleType.BF_3.value(), RadDetectorKindCodeSimpleType.HG_I_2.value(), RadDetectorKindCodeSimpleType.CE_BR_4.value(), RadDetectorKindCodeSimpleType.LI_CAF.value(), RadDetectorKindCodeSimpleType.LI_ZN_S.value(), RadDetectorKindCodeSimpleType.OTHER.value())
                .build();
    }

    public Text createRadDetectorDescription(){
        return createText()
                .name("RadDetectorDescription")
                .label("Rad Detector Description")
                .definition(getRadDetectorURI("description"))
                .build();
    }

    public Quantity createRadDetectorLengthValue(){
        return createQuantity()
                .name("RadDetectorLengthValue")
                .label("Rad Detector Length Value")
                .definition(getRadDetectorURI("length-value"))
                .uom("cm")
                .build();
    }

    public Quantity createRadDetectorWidthValue(){
        return createQuantity()
                .name("RadDetectorWidthValue")
                .label("Rad Detector Width Value")
                .definition(getRadDetectorURI("width-value"))
                .uom("cm")
                .build();
    }

    public Quantity createRadDetectorDepthValue(){
        return createQuantity()
                .name("RadDetectorDepthValue")
                .label("Rad Detector Depth Value")
                .definition(getRadDetectorURI("depth-value"))
                .uom("cm")
                .build();
    }

    public Quantity createRadDetectorDiameterValue(){
        return createQuantity()
                .name("RadDetectorDiameterValue")
                .label("Rad Detector Diameter Value")
                .definition(getRadDetectorURI("diameter-value"))
                .uom("cm")
                .build();
    }

    public Quantity createRadDetectorVolumeValue(){
        return createQuantity()
                .name("RadDetectorVolumeValue")
                .label("Rad Detector Volume Value")
                .definition(getRadDetectorURI("volume-value"))
                .description("Detection Volume in cubic centimeters")
                .uom("cc")
                .build();
    }

    public DataRecord createRadDetectorCharacteristics() {
        return createRecord()
                .name("RadDetectorCharacteristics")
                .label("Rad Detector Characteristics")
                .definition(getRadDetectorURI("characteristics"))
                .build();
    }

    // RAD ITEM INFORMATION

    public Text createRadItemDescription(){
        return createText()
                .name("RadItemDescription")
                .label("Rad Item Description")
                .definition(getRadItemURI("description"))
                .build();
    }

    public DataRecord createRadItemQuatity(){
        return createRecord()
                .name("RadItemQuantity")
                .label("Rad Item Quantity")
                .definition(getRadItemURI("quantity"))
                .addField("RadItemQuantityValue",
                        createQuantity()
                                .name("RadItemQuantityValue")
                                .label("Rad Item Quantity Value")
                                .definition(getRadItemURI("quantity-value"))
                                .build())
                .addField("RadItemQuantityUncertaintyValue",
                        createQuantity()
                                .name("RadItemQuantityUncertaintyValue")
                                .label("Rad Item Quantity Uncertainty Value")
                                .definition(getRadItemURI("quantity-uncertainty-value"))
                                .build())
                .addField("RadItemQuantityUnits",
                        createText()
                                .name("RadItemQuantityUnits")
                                .label("Rad Item Quantity Units")
                                .definition(getRadItemURI("quantity-units"))
                                .build())
                .build();
    }

    public DataRecord createRadItemCharacteristics() {
        return createRecord()
                .name("RadItemCharacteristics")
                .label("Rad Item Characteristics")
                .definition(getRadItemURI("characteristics"))
                .build();
    }

    // Energy Calibration









}
