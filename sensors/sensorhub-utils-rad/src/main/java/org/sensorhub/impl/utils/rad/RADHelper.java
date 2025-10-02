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

    public static final String DEF_GAMMA = getRadUri("gammaGrossCount");
    public static final String DEF_NEUTRON = getRadUri("neutronGrossCount");
    public static final String DEF_OCCUPANCY = getRadUri("pillarOccupancyCount");
    public static final String DEF_ALARM = getRadUri("alarm");
    public static final String DEF_TAMPER = getRadUri("tamperStatus");
    public static final String DEF_THRESHOLD = getRadUri("threshold");
    public static final String DEF_ADJUDICATION = getRadUri("adjudicationCode");

    public static String getRadUri(String propName) {
        return RADConstants.RAD_URI + propName;
    }

    public static String getRadInstrumentUri(String propName) {
        return RADConstants.RAD_INSTRUMENT_URI + propName;
    }
    public static String getRadDetectorURI(String propName) {
        return RADConstants.RAD_URI + "radDetector-" + propName;
    }
    public static String getRadItemURI(String propName) {
        return RADConstants.RAD_URI + "radItem-" + propName;
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
                .name("samplingTime")
                .description("time stamp: when the message was received")
                .build();
    }

    public Quantity createSpeedTimeStamp() {
        return createQuantity()
                .name("speedTime")
                .label("Speed Time")
                .definition(getRadUri("speedTime"))
                .description("time it takes to cover 1 foot of distance")
                .uomCode("s")
                .build();
    }

    public Quantity createMaxGamma(){
        return createQuantity()
                .name("maxGamma")
                .label("Max Gamma")
                .definition(getRadUri("maxGamma"))
                .build();
    }

    public Quantity createMaxNeutron(){
        return createQuantity()
                .name("maxNeutron")
                .label("Max Neutron")
                .definition(getRadUri("maxNeutron"))
                .build();
    }

    public Boolean createIsAdjudicated() {
        return createBoolean()
                .name("isAdjudicated")
                .label("Is Adjudicated")
                .definition(getRadUri("isAdjudicated"))
                .build();
    }

    public Text createAspectMessageFile(){
        return createText()
                .name("aspectMessage")
                .label("Aspect Message")
                .definition(getRadUri("aspectMessage"))
                .build();
    }

    public Time createBackgroundTime() {
        return createTime()
                .name("backgroundTime")
                .name("Background Time")
                .definition(getRadUri("backgroundTime"))
                //range 20-120seconds
                .build();
    }
    public Time createOccupancyStartTime(){
        return createTime()
                .asPhenomenonTimeIsoUTC()
                .name("startTime")
                .label("Start Time")
                .definition(getRadUri("occupancyStartTime"))
                .description("The start time of occupancy data")
                .build();
    }

    public Time createOccupancyEndTime(){
        return createTime()
                .asPhenomenonTimeIsoUTC()
                .name("endTime")
                .label("End Time")
                .definition(getRadUri("occupancyEndTime"))
                .description("The end time of occupancy data")
                .build();
    }

    public Boolean createGammaAlarm() {
        return createBoolean()
                .name("gammaAlarm")
                .label("Gamma Alarm")
                .definition(getRadUri("gammaAlarm"))
                .build();
    }

    public Boolean createNeutronAlarm() {
        return createBoolean()
                .name("neutronAlarm")
                .label("Neutron Alarm")
                .definition(getRadUri("neutronAlarm"))
                .build();
    }

    public Boolean createTamperStatus(){
        return createBoolean()
                .name("tamperStatus")
                .label("Tamper Status")
                .definition(DEF_TAMPER)
                .description("True if the rpm is currently reporting a Tamper state")
                .build();
    }


    public Quantity createNeutronBackground(){
        return createQuantity()
                .name("neutronBackground")
                .label("Neutron Background")
                .definition(getRadUri("neutronBackground"))
                .description("Neutron count to start occupancy")
                .build();
    }



    public Quantity createGammaBackground() {
        return createQuantity()
                .name("gammaBackground")
                .label("Gamma Background")
                .definition(getRadUri("gammaBackground"))
                .description("Gamma count to start occupancy")
                .build();
    }

    public Quantity createLatestGammaBackground() {
        return createQuantity()
                .name("latestGammaBackground")
                .label("Latest Gamma Background")
                .definition(getRadUri("latestGammaBackground"))
                .description("Latest gamma background used in threshold and sigma calculations")
                .build();
    }

    public Quantity createGammaVariance() {
        return createQuantity()
                .name("gammaVariance")
                .label("Gamma Variance")
                .definition(getRadUri("gammaVariance"))
                .build();
    }

    public Quantity createNeutronVariance() {
        return createQuantity()
                .name("neutronVariance")
                .label("Neutron Variance")
                .definition(getRadUri("neutronVariance"))
                .build();
    }

    public Quantity createGammaVarianceBackground() {
        return createQuantity()
                .name("gammaVarianceBackground")
                .label("Gamma Variance/Background")
                .definition(getRadUri("gammaVarianceBackground"))
                .build();
    }

    public Quantity createNeutronVarianceBackground() {
        return createQuantity()
                .name("neutronVarianceBackground")
                .label("Neutron Variance/Background")
                .definition(getRadUri("neutronVarianceBackground"))
                .build();
    }

    public Quantity createObjectMark() {
        return createQuantity()
                .name("objectMark")
                .label("Object Mark")
                .definition(getRadUri("objectMark"))
                .build();
    }

    public Quantity createNSigma(){
        return createQuantity()
                .name("nSigma")
                .label("N Sigma")
                .definition(getRadUri("nSigma"))
                .description("Number of standard deviations above average background to test alarm threshold against")
                .build();
    }

    public Quantity createSigmaValue() {
        return createQuantity()
                .name("sigma")
                .label("Sigma Value")
                .definition(getRadUri("sigma"))
                .description("Standard deviation of the average background counts")
                .build();
    }

    public Quantity createThreshold() {
        return createQuantity()
                .name("threshold")
                .label("Threshold")
                .definition(DEF_THRESHOLD)
                .description("Calculated threshold for an alarm")
                .build();
    }

    public Quantity createZMaxValue(){
        return createQuantity()
                .name("zmax")
                .label("ZMax")
                .definition(getRadUri("zMax"))
                .description("Maximum z-value")
                .build();
    }

    public Quantity createAlphaValue(){
        return createQuantity()
                .name("alpha")
                .label("Alpha")
                .definition(getRadUri("alpha"))
                .description("value used in calculations")
                .build();
    }
    public Quantity createOccupancyHoldin(){
        return createQuantity()
                .name("occupancyHoldin")
                .label("Occupancy Holdin")
                .definition(getRadUri("occupancyHoldin"))
                .description("number of 200ms time intervals to hold in after occupancy signal indicates the system is vacant")
                .build();
    }

    public Quantity createIntervals(){
        return createQuantity()
                .name("intervals")
                .label("Intervals")
                .definition(getRadUri("intervals"))
                .description("the number of time intervals to be considered")
                .build();
    }

    public Quantity createSequentialIntervals(){
        return createQuantity()
                .name("sequentialIntervals")
                .label("Sequential Intervals")
                .definition(getRadUri("sequentialIntervals"))
                .build();
    }

    public Quantity createMaxIntervals(){
        return createQuantity()
                .name("neutronMaximumIntervals")
                .label("Neutron Maximum Intervals")
                .definition(getRadUri("maximumIntervals"))
                .build();
    }

    public Text createPlaceholder(){
        return createText()
                .name("placeholder")
                .label("Placeholder")
                .definition(getRadUri("placeholder"))
                .build();
    }

    public Quantity createBackgroundSigma(){
        return createQuantity()
                .name("backgroundSigma")
                .label("Background Sigma")
                .definition(getRadUri("backgroundSigma"))
                .description("sets a sigma value for a throw-through alarm")
//                .description("is used to set alarm thresholds for detecting anomalies in radiation levels")
                .build();
    }

    public Quantity createHighBackgroundFault(){
        return createQuantity()
                .name("highBackgroundFault")
                .label("High Background Fault")
                .definition(getRadUri("highBackgroundFault"))
                .description("threshold value measured in counts per second(cps) within a radiation detection system that will trigger a fault")
//                .uomCode("cps")
                .build();
    }
    public Quantity createLowBackgroundFault(){
        return createQuantity()
                .name("lowBackgroundFault")
                .label("Low Background Fault")
                .definition(getRadUri("lowBackgroundFault"))
                .description("threshold value measured in counts per second(cps) within a radiation detection system that will trigger a fault")
//                .uomCode("cps")
                .build();
    }
    public Text createDetectors(){
        return createText()
                .name("detectorsOnLine")
                .label("Detectors on line")
                .definition(getRadUri("detectorsOnLine"))
                .description("radiation detectors that are actively operating and connected to the monitoring system")
                .build();
    }

    public Quantity createSlaveULD(){
        return createQuantity()
                .name("slaveLevelUpperDiscriminator")
                .label("Slave Upper Level Discriminator")
                .definition(getRadUri("slaveLevelUpperDiscriminator"))
                .description("threshold setting that defines an upper limit for the energy of detected radiation events")
                .build();
    }

    public Quantity createSlaveLLD(){
        return createQuantity()
                .name("slaveLevelLowerDiscriminator")
                .label("Slave Lower Level Discriminator")
                .definition(getRadUri("slaveLevelLowerDiscriminator"))
                .description("threshold setting that defines an lower limit for the energy of detected radiation events")
                .build();
    }
    public Quantity createRelayOutput(){
        return createQuantity()
                .name("relayOutput")
                .label("Relay Output")
                .definition(getRadUri("relayOutput"))
                .description("electrical output signal that can activate or control other devices based on detection of radiation")
                .build();
    }
    public Text createAlgorithm(){
        return createText()
                .name("algorithm")
                .label("Algorithm")
                .definition(getRadUri("algorithm"))
                .description("SUM, HORIZONTAL, VERTICAL, SINGLE" +
                        "permits the operator to select which detectors will be included in the alarm calculations")
                .build();
    }

    public Text createSoftwareVersion(){
        return createText()
                .name("softwareVersion")
                .label("Software Version")
                .definition(getRadUri("softwareVersion"))
                .addAllowedValues("A", "T")
                .build();
    }
    public Text createFirmwareVersion(){
        return createText()
                .name("firmwareVersion")
                .label("Firmware version")
                .definition(getRadUri("firmwareVersion"))
                .build();
    }

    public Text createLaneID(){
        return createText()
                .name("laneID")
                .label("Lane ID")
                .definition(RADHelper.getRadUri("laneId"))
                .description("identifies the lane for each rpm system")
                .build();
    }

    public Quantity createSpeedMph(){
        return createQuantity()
                .name("speedMPH")
                .label("Speed (MPH)")
                .definition(getRadUri("speedMph"))
                .uomCode("mph")
                //max 99
                .build();
    }

    public Quantity createSpeedKph(){
        return createQuantity()
                .name("speedKPH")
                .label("Speed (KPH)")
                .definition(getRadUri("speedKph"))
                .uomCode("kph")
                //max 999
                .build();
    }

    public Quantity createSpeedMms() {
        return createQuantity()
                .name("speedMMS")
                .label("Speed (MM/S)")
                .definition(getRadUri("speedMms"))
                .uomCode("mm/s")
                .build();
    }

    public Count createOccupancyCount(){
        return createCount()
                .name("occupancyCount")
                .label("Pillar Occupancy Count")
                .definition(DEF_OCCUPANCY)
                .description("incremented count every time the pillar clears the occupancy, resets daily and on power cycle")
                .build();
    }

    public Quantity createBatteryCharge(){
        return createQuantity()
                .name("batteryCharge")
                .label("Battery Charge")
                .definition(getRadInstrumentUri("batteryCharge"))
                .uomCode("%")
                .build();
    }

    public DataArray createLinCalibration(){
        return createArray()
                .name("linCalibration")
                .label("Lin Calibration")
                .definition(getRadUri("linCal"))
                .withFixedSize(3)
                .withElement("linCalibrationValues", createQuantity()
                        .label("Lin Calibration Values")
                        .definition(getRadUri("linCalVals"))
                        .description("Linear Calibration Values")
                        .dataType(DataType.DOUBLE)
                        .build())
                .build();
    }

    public DataArray createCmpCalibration(){
        return createArray()
                .name("cmpCalibration")
                .label("Cmp Calibration")
                .definition(getRadUri("cmpCal"))
                .withFixedSize(3)
                .withElement("CmpCalibrationValues", createQuantity()
                        .label("Cmp Calibration Values")
                        .definition(getRadUri("cmpCalVals"))
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
                .name("linSpectrum")
                .label("Lin Spectrum")
                .definition(getRadUri("linSpectrum"))
                .withVariableSize(fieldID)
                .withElement("linSpectrumValues", createQuantity()
                        .label("Lin Spectrum Values")
                        .definition(getRadUri("linSpectrumVals"))
                        .description("Spectrum Values")
                        .dataType(DataType.DOUBLE)
                        .build())
                .build();
    }

    public DataArray createCmpSpectrum(String fieldID){
        return createArray()
                .name("cmpSpectrum")
                .label("Cmp Spectrum")
                .definition(getRadUri("cmpSpectrum"))
                .withVariableSize(fieldID)
                .withElement("cmpSpectrumValues", createQuantity()
                        .label("Cmp Spectrum Values")
                        .definition(getRadUri("cmpSpectrumVals"))
                        .description("Spectrum Values")
                        .dataType(DataType.DOUBLE)
                        .build())
                .build();
    }

    private SWEBuilders.CategoryBuilder createAlarmState() {
        return createCategory()
                .name("alarmState")
                .label("Alarm State")
                .definition(DEF_ALARM);
    }

    public Category createGammaAlarmState() {
        return createAlarmState()
                .addAllowedValues("Alarm", "Background", "Scan", "Fault - Gamma High", "Fault - Gamma Low")
                .build();
    }

    public Category createNeutronAlarmState() {
        return createAlarmState()
                .addAllowedValues("Alarm", "Background", "Scan", "Fault - Neutron High", "Fault - Neutron Low")
                .build();
    }

    public Count createGammaGrossCount(){
        return createCount()
                .name("gammaGrossCount")
                .label("Gamma Gross Count")
                .definition(DEF_GAMMA)
                .build();
    }

    public Count createGammaCount(int countID){
        return createCount()
                .name("gammaCount" + countID)
                .label("Gamma Count " + countID)
                .definition(getRadUri("gammaCount"))
                .build();
    }

    public Count createGammaCountPerInterval(int countID){
        return createCount()
                .name("gammaCountPerInterval" + countID)
                .label("Gamma Count (200ms) " + countID)
                .definition(getRadUri("gammaCountPerInterval"))
                .value(0)
                .build();
    }

    public Count createNeutronGrossCount(){
        return createCount()
                .name("neutronGrossCount")
                .label("Neutron Gross Count")
                .definition(DEF_NEUTRON)
                .build();
    }

    public Count createNeutronCount(int countID){
        return createCount()
                .name("neutronCount" + countID)
                .label("Neutron Count " + countID)
                .definition(getRadUri("neutronCount"))
                .build();
    }

    public Quantity createDoseUSVh(){
        return createQuantity()
                .name("dose")
                .label("Dose")
                .definition(getRadUri("dose"))
                .uomCode("uSv/h")
                .build();
    }

    public Category createMeasurementClassCode(){
        return createCategory()
                .name("measurementClassCode")
                .label("Measurement Class Code")
                .definition(getRadUri("measurementClassCode"))
                .addAllowedValues(MeasurementClassCodeSimpleType.FOREGROUND.value(), MeasurementClassCodeSimpleType.INTRINSIC_ACTIVITY.value(), MeasurementClassCodeSimpleType.BACKGROUND.value(), MeasurementClassCodeSimpleType.NOT_SPECIFIED.value(), MeasurementClassCodeSimpleType.CALIBRATION.value())
                .build();
    }

    public Category createAlarmCatCode(){
        return createCategory()
                .name("alarmCategoryCode")
                .label("Alarm Category Code")
                .definition(getRadUri("alarmCategoryCode"))
                .addAllowedValues(RadAlarmCategoryCodeSimpleType.ALPHA.value(),RadAlarmCategoryCodeSimpleType.NEUTRON.value(),RadAlarmCategoryCodeSimpleType.BETA.value(),RadAlarmCategoryCodeSimpleType.GAMMA.value(),RadAlarmCategoryCodeSimpleType.OTHER.value(),RadAlarmCategoryCodeSimpleType.ISOTOPE.value())
                .build();
    }


    public Quantity createMasterLLD(){
        return createQuantity()
                .name("masterLowerLevelDiscriminator")
                .label("Master Lower Level Discriminator")
                .definition(getRadUri("masterLowerLevelDiscriminator"))
                .build();
    }

    public Quantity createMasterULD(){
        return createQuantity()
                .name("masterUpperLevelDiscriminator")
                .label("Master UpperLevel Discriminator")
                .definition(getRadUri("masterUpperLevelDiscriminator"))
                .build();
    }

    //////////////////////////////// vvvv OLD vvvvvv ///////////////////////////////

    // RadInstrumentInformation
    public Text createRIManufacturerName() {
        return createText()
                .name("radInstrumentManufacturerName")
                .label("Rad Instrument Manufacturer Name")
                .definition(getRadInstrumentUri("manufacturerName"))
                .description("Manufacturer name of described RAD Instrument")
                .build();
    }

    public Category createRIIdentifier() {
        return createCategory()
                .name("radInstrumentIdentifier")
                .label("Rad Instrument Identifier")
                .definition(getRadInstrumentUri("identifier"))
                .description("Identifier for described RAD Instrument")
                .build();
    }

    public Text createRIModelName() {
        return createText()
                .name("radInstrumentModelName")
                .label("Rad Instrument Model Name")
                .definition(getRadInstrumentUri("modelName"))
                .description("Model name of described RAD Instrument")
                .build();
    }

    public Text createRIDescription() {
        return createText()
                .name("radInstrumentDescription")
                .label("Rad Instrument Description")
                .definition(getRadInstrumentUri("description"))
                .description("Description of RAD Instrument")
                .build();
    }

    public Category createRIClassCode() {
        return createCategory()
                .name("radInstrumentClassCode")
                .label("Rad Instrument Class Code")
                .definition(getRadInstrumentUri("classCode"))
                .description("Class Code for type of RAD Instrument")
                .addAllowedValues("Backpack", "Dosimeter", "Electronic Personal Emergency Radiation Detector", "Mobile System", "Network Area Monitor", "Neutron Handheld", "Personal Radiation Detector", "Radionuclide Identifier", "Portal Monitor", "Spectroscopic Portal Monitor", "Spectroscopic Personal Radiation Detector", "Gamma Handheld", "Transportable System", "Other")
                .build();
    }

    public DataRecord createRIVersion() {
        return createRecord()
                .name("radInstrumentVersion")
                .label("Rad Instrument Version")
                .definition(getRadInstrumentUri("version"))
                .addField("RadInstrumentComponentName", createRIComponentName())
                .addField("RadInstrumentComponentVersion", createRIComponentVersion())
                .build();
    }

    public Text createRIComponentName() {
        return createText()
                .name("radInstrumentComponentName")
                .label("Rad Instrument Component Name")
                .definition(getRadInstrumentUri("componenName"))
                .build();
    }

    public Text createRapiscanMessage(){
        return createText()
                .name("rapiscanMessage")
                .label("Rapiscan Message")
                .definition(getRadInstrumentUri("rapiscanMessage"))
                .build();
    }
    public Text createRIComponentVersion() {
        return createText()
                .name("radInstrumentComponentVersion")
                .label("Rad Instrument Component Version")
                .definition(getRadInstrumentUri("componentVersion"))
                .build();
    }
    // TODO: Create Record for Quality Control
//    public DataRecord createRIQualityControl(){
//        return createRecord()
//    }

    public DataRecord createRICharacteristics() {
        return createRecord()
                .name("radInstrumentCharacteristics")
                .label("Rad Instrument Characteristics")
                .definition(getRadInstrumentUri("characteristics"))
                .build();
    }

    public DataRecord createCharacteristicGroup() {
        return createRecord()
                .name("characteristicGroupName")
                .label("Characteristic Group Name")
                .definition(getRadInstrumentUri("characteristicGroup"))
                .build();
    }

    public DataRecord createCharacteristicText() {
        DataRecord record = createRecord()
                .name("")
                .addField("characteristicName",
                        createText()
                                .label("Characteristic Name")
                                .definition(getRadInstrumentUri("characteristicName"))
                                .build())
                .addField("characteristicValue",
                        createText()
                                .label("Characteristic Value")
                                .definition(getRadInstrumentUri("characteristicValue"))
                                .build())
                .addField("characteristicValueUnits",
                        createText()
                                .label("Characteristic Value Units")
                                .definition(getRadInstrumentUri("characteristicValueUnits"))
                                .build())
                .addField("characteristicValueDataClassCode",
                        createCategory()
                                .label("Characteristic Value Data Class Code")
                                .definition(getRadInstrumentUri("characteristicValueDataClassCode"))
                                .addAllowedValues(ValueDataClassCodeSimpleType.ANY_URI.value(), ValueDataClassCodeSimpleType.BASE_64_BINARY.value(), ValueDataClassCodeSimpleType.BOOLEAN.value(), ValueDataClassCodeSimpleType.BYTE.value(), ValueDataClassCodeSimpleType.DATE.value(), ValueDataClassCodeSimpleType.DATE_TIME.value(), ValueDataClassCodeSimpleType.DECIMAL.value(), ValueDataClassCodeSimpleType.DOUBLE.value(), ValueDataClassCodeSimpleType.DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.DURATION.value(), ValueDataClassCodeSimpleType.FLOAT.value(), ValueDataClassCodeSimpleType.HEX_BINARY.value(), ValueDataClassCodeSimpleType.ID.value(), ValueDataClassCodeSimpleType.IDREF.value(), ValueDataClassCodeSimpleType.IDREFS.value(), ValueDataClassCodeSimpleType.INT.value(), ValueDataClassCodeSimpleType.INTEGER.value(), ValueDataClassCodeSimpleType.LONG.value(), ValueDataClassCodeSimpleType.NAME.value(), ValueDataClassCodeSimpleType.NC_NAME.value(), ValueDataClassCodeSimpleType.NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_BLANK_STRING.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NORMALIZED_STRING.value(), ValueDataClassCodeSimpleType.PERCENT.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER_LIST.value(), ValueDataClassCodeSimpleType.SHORT.value(), ValueDataClassCodeSimpleType.STRING.value(), ValueDataClassCodeSimpleType.STRING_LIST.value(), ValueDataClassCodeSimpleType.TIME.value(), ValueDataClassCodeSimpleType.TOKEN.value(), ValueDataClassCodeSimpleType.UNSIGNED_BYTE.value(), ValueDataClassCodeSimpleType.UNSIGNED_INT.value(), ValueDataClassCodeSimpleType.UNSIGNED_LONG.value(), ValueDataClassCodeSimpleType.UNSIGNED_SHORT.value(), ValueDataClassCodeSimpleType.ZERO_TO_ONE_DOUBLE.value())
                                .build())
                .build();
        return record;
    }

    public DataRecord createCharacteristicQuantity() {
        DataRecord record = createRecord()
                .name("")
                .addField("characteristicName",
                        createText()
                                .label("Characteristic Name")
                                .definition(getRadInstrumentUri("characteristicName"))
                                .build())
                .addField("characteristicValue",
                        createQuantity()
                                .label("Characteristic Value")
                                .definition(getRadInstrumentUri("characteristicValue"))
                                .build())
                .addField("characteristicValueUnits",
                        createText()
                                .label("Characteristic Value Units")
                                .definition(getRadInstrumentUri("characteristicValueUnits"))
                                .build())
                .addField("characteristicValueDataClassCode",
                        createCategory()
                                .label("Characteristic Value Data Class Code")
                                .definition(getRadInstrumentUri("characteristicValueDataClassCode"))
                                .addAllowedValues(ValueDataClassCodeSimpleType.ANY_URI.value(), ValueDataClassCodeSimpleType.BASE_64_BINARY.value(), ValueDataClassCodeSimpleType.BOOLEAN.value(), ValueDataClassCodeSimpleType.BYTE.value(), ValueDataClassCodeSimpleType.DATE.value(), ValueDataClassCodeSimpleType.DATE_TIME.value(), ValueDataClassCodeSimpleType.DECIMAL.value(), ValueDataClassCodeSimpleType.DOUBLE.value(), ValueDataClassCodeSimpleType.DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.DURATION.value(), ValueDataClassCodeSimpleType.FLOAT.value(), ValueDataClassCodeSimpleType.HEX_BINARY.value(), ValueDataClassCodeSimpleType.ID.value(), ValueDataClassCodeSimpleType.IDREF.value(), ValueDataClassCodeSimpleType.IDREFS.value(), ValueDataClassCodeSimpleType.INT.value(), ValueDataClassCodeSimpleType.INTEGER.value(), ValueDataClassCodeSimpleType.LONG.value(), ValueDataClassCodeSimpleType.NAME.value(), ValueDataClassCodeSimpleType.NC_NAME.value(), ValueDataClassCodeSimpleType.NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_BLANK_STRING.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NORMALIZED_STRING.value(), ValueDataClassCodeSimpleType.PERCENT.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER_LIST.value(), ValueDataClassCodeSimpleType.SHORT.value(), ValueDataClassCodeSimpleType.STRING.value(), ValueDataClassCodeSimpleType.STRING_LIST.value(), ValueDataClassCodeSimpleType.TIME.value(), ValueDataClassCodeSimpleType.TOKEN.value(), ValueDataClassCodeSimpleType.UNSIGNED_BYTE.value(), ValueDataClassCodeSimpleType.UNSIGNED_INT.value(), ValueDataClassCodeSimpleType.UNSIGNED_LONG.value(), ValueDataClassCodeSimpleType.UNSIGNED_SHORT.value(), ValueDataClassCodeSimpleType.ZERO_TO_ONE_DOUBLE.value())
                                .build())
                .build();
        return record;
    }

    public DataRecord createCharacteristicCount() {
        DataRecord record = createRecord()
                .name("")
                .addField("characteristicName",
                        createText()
                                .label("Characteristic Name")
                                .definition(getRadInstrumentUri("characteristicName"))
                                .build())
                .addField("characteristicValue",
                        createCount()
                                .label("Characteristic Value")
                                .definition(getRadInstrumentUri("characteristicValue"))
                                .build())
                .addField("characteristicValueUnits",
                        createText()
                                .label("Characteristic Value Units")
                                .definition(getRadInstrumentUri("characteristicValueUnits"))
                                .build())
                .addField("characteristicValueDataClassCode",
                        createCategory()
                                .label("Characteristic Value Data Class Code")
                                .definition(getRadInstrumentUri("characteristicValueDataClassCode"))
                                .addAllowedValues(ValueDataClassCodeSimpleType.ANY_URI.value(), ValueDataClassCodeSimpleType.BASE_64_BINARY.value(), ValueDataClassCodeSimpleType.BOOLEAN.value(), ValueDataClassCodeSimpleType.BYTE.value(), ValueDataClassCodeSimpleType.DATE.value(), ValueDataClassCodeSimpleType.DATE_TIME.value(), ValueDataClassCodeSimpleType.DECIMAL.value(), ValueDataClassCodeSimpleType.DOUBLE.value(), ValueDataClassCodeSimpleType.DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.DURATION.value(), ValueDataClassCodeSimpleType.FLOAT.value(), ValueDataClassCodeSimpleType.HEX_BINARY.value(), ValueDataClassCodeSimpleType.ID.value(), ValueDataClassCodeSimpleType.IDREF.value(), ValueDataClassCodeSimpleType.IDREFS.value(), ValueDataClassCodeSimpleType.INT.value(), ValueDataClassCodeSimpleType.INTEGER.value(), ValueDataClassCodeSimpleType.LONG.value(), ValueDataClassCodeSimpleType.NAME.value(), ValueDataClassCodeSimpleType.NC_NAME.value(), ValueDataClassCodeSimpleType.NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_BLANK_STRING.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NORMALIZED_STRING.value(), ValueDataClassCodeSimpleType.PERCENT.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER_LIST.value(), ValueDataClassCodeSimpleType.SHORT.value(), ValueDataClassCodeSimpleType.STRING.value(), ValueDataClassCodeSimpleType.STRING_LIST.value(), ValueDataClassCodeSimpleType.TIME.value(), ValueDataClassCodeSimpleType.TOKEN.value(), ValueDataClassCodeSimpleType.UNSIGNED_BYTE.value(), ValueDataClassCodeSimpleType.UNSIGNED_INT.value(), ValueDataClassCodeSimpleType.UNSIGNED_LONG.value(), ValueDataClassCodeSimpleType.UNSIGNED_SHORT.value(), ValueDataClassCodeSimpleType.ZERO_TO_ONE_DOUBLE.value())
                                .build())
                .build();
        return record;
    }

    public DataRecord createCharacteristicBoolean() {
        DataRecord record = createRecord()
                .name("")
                .addField("characteristicName",
                        createText()
                                .label("Characteristic Name")
                                .definition(getRadInstrumentUri("characteristicName"))
                                .build())
                .addField("characteristicValue",
                        createBoolean()
                                .label("Characteristic Value")
                                .definition(getRadInstrumentUri("characteristicValue"))
                                .build())
                .addField("characteristicValueUnits",
                        createText()
                                .label("Characteristic Value Units")
                                .definition(getRadInstrumentUri("characteristicValueUnits"))
                                .build())
                .addField("characteristicValueDataClassCode",
                        createCategory()
                                .label("Characteristic Value Data Class Code")
                                .definition(getRadInstrumentUri("characteristicValueDataClassCode"))
                                .addAllowedValues(ValueDataClassCodeSimpleType.ANY_URI.value(), ValueDataClassCodeSimpleType.BASE_64_BINARY.value(), ValueDataClassCodeSimpleType.BOOLEAN.value(), ValueDataClassCodeSimpleType.BYTE.value(), ValueDataClassCodeSimpleType.DATE.value(), ValueDataClassCodeSimpleType.DATE_TIME.value(), ValueDataClassCodeSimpleType.DECIMAL.value(), ValueDataClassCodeSimpleType.DOUBLE.value(), ValueDataClassCodeSimpleType.DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.DURATION.value(), ValueDataClassCodeSimpleType.FLOAT.value(), ValueDataClassCodeSimpleType.HEX_BINARY.value(), ValueDataClassCodeSimpleType.ID.value(), ValueDataClassCodeSimpleType.IDREF.value(), ValueDataClassCodeSimpleType.IDREFS.value(), ValueDataClassCodeSimpleType.INT.value(), ValueDataClassCodeSimpleType.INTEGER.value(), ValueDataClassCodeSimpleType.LONG.value(), ValueDataClassCodeSimpleType.NAME.value(), ValueDataClassCodeSimpleType.NC_NAME.value(), ValueDataClassCodeSimpleType.NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_BLANK_STRING.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.NON_NEGATIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NON_POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.NORMALIZED_STRING.value(), ValueDataClassCodeSimpleType.PERCENT.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE_LIST.value(), ValueDataClassCodeSimpleType.POSITIVE_DOUBLE.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER.value(), ValueDataClassCodeSimpleType.POSITIVE_INTEGER_LIST.value(), ValueDataClassCodeSimpleType.SHORT.value(), ValueDataClassCodeSimpleType.STRING.value(), ValueDataClassCodeSimpleType.STRING_LIST.value(), ValueDataClassCodeSimpleType.TIME.value(), ValueDataClassCodeSimpleType.TOKEN.value(), ValueDataClassCodeSimpleType.UNSIGNED_BYTE.value(), ValueDataClassCodeSimpleType.UNSIGNED_INT.value(), ValueDataClassCodeSimpleType.UNSIGNED_LONG.value(), ValueDataClassCodeSimpleType.UNSIGNED_SHORT.value(), ValueDataClassCodeSimpleType.ZERO_TO_ONE_DOUBLE.value())
                                .build())
                .build();
        return record;
    }

    // RAD DETECTOR INFORMATION

    public Text createRadDetectorName(){
        return createText()
                .name("radDetectorName")
                .label("Rad Detector Name")
                .definition(getRadDetectorURI("name"))
                .build();
    }

    public Category createRadDetectorCategoryCode(){
        return createCategory()
                .name("radDetectorCategoryCode")
                .label("Rad Detector Category Code")
                .definition(getRadDetectorURI("categoryCode"))
                .addAllowedValues(RadDetectorCategoryCodeSimpleType.GAMMA.value(), RadDetectorCategoryCodeSimpleType.NEUTRON.value(), RadDetectorCategoryCodeSimpleType.ALPHA.value(), RadDetectorCategoryCodeSimpleType.BETA.value(), RadDetectorCategoryCodeSimpleType.X_RAY.value(), RadDetectorCategoryCodeSimpleType.OTHER.value())
                .build();
    }

    public Category createRadDetectorKindCode(){
        return createCategory()
                .name("radDetectorKindCode")
                .label("Rad Detector Kind Code")
                .definition(getRadDetectorURI("kindKode"))
                .addAllowedValues(RadDetectorKindCodeSimpleType.HP_GE.value(), RadDetectorKindCodeSimpleType.HP_XE.value(), RadDetectorKindCodeSimpleType.NA_I.value(), RadDetectorKindCodeSimpleType.LA_BR_3.value(), RadDetectorKindCodeSimpleType.LA_CL_3.value(), RadDetectorKindCodeSimpleType.BGO.value(), RadDetectorKindCodeSimpleType.CZT.value(), RadDetectorKindCodeSimpleType.CD_TE.value(), RadDetectorKindCodeSimpleType.CS_I.value(), RadDetectorKindCodeSimpleType.GMT.value(), RadDetectorKindCodeSimpleType.GMTW.value(), RadDetectorKindCodeSimpleType.LI_FIBER.value(), RadDetectorKindCodeSimpleType.PVT.value(), RadDetectorKindCodeSimpleType.PS.value(), RadDetectorKindCodeSimpleType.HE_3.value(), RadDetectorKindCodeSimpleType.HE_4.value(), RadDetectorKindCodeSimpleType.LI_GLASS.value(), RadDetectorKindCodeSimpleType.LI_I.value(), RadDetectorKindCodeSimpleType.SR_I_2.value(), RadDetectorKindCodeSimpleType.CLYC.value(), RadDetectorKindCodeSimpleType.CD_WO_4.value(), RadDetectorKindCodeSimpleType.BF_3.value(), RadDetectorKindCodeSimpleType.HG_I_2.value(), RadDetectorKindCodeSimpleType.CE_BR_4.value(), RadDetectorKindCodeSimpleType.LI_CAF.value(), RadDetectorKindCodeSimpleType.LI_ZN_S.value(), RadDetectorKindCodeSimpleType.OTHER.value())
                .build();
    }

    public Text createRadDetectorDescription(){
        return createText()
                .name("radDetectorDescription")
                .label("Rad Detector Description")
                .definition(getRadDetectorURI("description"))
                .build();
    }

    public Quantity createRadDetectorLengthValue(){
        return createQuantity()
                .name("radDetectorLengthValue")
                .label("Rad Detector Length Value")
                .definition(getRadDetectorURI("lengthValue"))
                .uom("cm")
                .build();
    }

    public Quantity createRadDetectorWidthValue(){
        return createQuantity()
                .name("radDetectorWidthValue")
                .label("Rad Detector Width Value")
                .definition(getRadDetectorURI("widthValue"))
                .uom("cm")
                .build();
    }

    public Quantity createRadDetectorDepthValue(){
        return createQuantity()
                .name("radDetectorDepthValue")
                .label("Rad Detector Depth Value")
                .definition(getRadDetectorURI("depthValue"))
                .uom("cm")
                .build();
    }

    public Quantity createRadDetectorDiameterValue(){
        return createQuantity()
                .name("radDetectorDiameterValue")
                .label("Rad Detector Diameter Value")
                .definition(getRadDetectorURI("diameterValue"))
                .uom("cm")
                .build();
    }

    public Quantity createRadDetectorVolumeValue(){
        return createQuantity()
                .name("radDetectorVolumeValue")
                .label("Rad Detector Volume Value")
                .definition(getRadDetectorURI("volumeValue"))
                .description("Detection Volume in cubic centimeters")
                .uom("cc")
                .build();
    }

    public DataRecord createRadDetectorCharacteristics() {
        return createRecord()
                .name("radDetectorCharacteristics")
                .label("Rad Detector Characteristics")
                .definition(getRadDetectorURI("characteristics"))
                .build();
    }

    // RAD ITEM INFORMATION

    public Text createRadItemDescription(){
        return createText()
                .name("radItemDescription")
                .label("Rad Item Description")
                .definition(getRadItemURI("description"))
                .build();
    }

    public DataRecord createRadItemQuantity(){
        return createRecord()
                .name("radItemQuantity")
                .label("Rad Item Quantity")
                .definition(getRadItemURI("quantity"))
                .addField("radItemQuantityValue",
                        createQuantity()
                                .label("Rad Item Quantity Value")
                                .definition(getRadItemURI("quantityValue"))
                                .build())
                .addField("radItemQuantityUncertaintyValue",
                        createQuantity()
                                .label("Rad Item Quantity Uncertainty Value")
                                .definition(getRadItemURI("quantityUncertaintyValue"))
                                .build())
                .addField("radItemQuantityUnits",
                        createText()
                                .label("Rad Item Quantity Units")
                                .definition(getRadItemURI("quantityUnits"))
                                .build())
                .build();
    }

    public DataRecord createRadItemCharacteristics() {
        return createRecord()
                .name("radItemCharacteristics")
                .label("Rad Item Characteristics")
                .definition(getRadItemURI("characteristics"))
                .build();
    }


    // Energy Calibration

}