package org.sensorhub.impl.utils.rad;

import com.botts.impl.utils.n42.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import net.opengis.swe.v20.*;
import net.opengis.swe.v20.Boolean;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.io.StringReader;


public class RADHelper extends GeoPosHelper {

    public static final String DEF_GAMMA = getRadUri("GammaGrossCount");
    public static final String DEF_NEUTRON = getRadUri("NeutronGrossCount");
    public static final String DEF_OCCUPANCY = getRadUri("PillarOccupancyCount");
    public static final String DEF_ALARM = getRadUri("Alarm");
    public static final String DEF_TAMPER = getRadUri("TamperStatus");
    public static final String DEF_THRESHOLD = getRadUri("Threshold");
    public static final String DEF_ADJUDICATION = getRadUri("AdjudicationCode");
    public static final String DEF_EML_ANALYSIS = SWEHelper.getPropertyUri("EMLGammaAlert");
    public static final String DEF_EML_SCAN = SWEHelper.getPropertyUri("EMLRPMGammaAlert");
    public static final String DEF_VIDEO = getRadUri("");
    public static final String DEF_COMM = getRadUri("");

    public static String getRadUri(String propName) {
        return RADConstants.RAD_URI + propName;
    }

    public static String getRadInstrumentUri(String propName) {
        return RADConstants.RAD_INSTRUMENT_URI + propName;
    }
    public static String getRadDetectorURI(String propName) {
        return RADConstants.RAD_URI + "RadDetector" + propName;
    }
    public static String getRadItemURI(String propName) {
        return RADConstants.RAD_URI + "RadItem" + propName;
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
                .description("when the message was received")
                .build();
    }

    public Quantity createSpeedTimeStamp() {
        return createQuantity()
                .name("speedTime")
                .label("Speed Time")
                .definition(getRadUri("SpeedTime"))
                .description("time it takes to cover 1 foot of distance")
                .uomCode("s")
                .build();
    }

    public Quantity createMaxGamma(){
        return createQuantity()
                .name("maxGamma")
                .label("Max Gamma")
                .definition(getRadUri("MaxGamma"))
                .build();
    }

    public Quantity createMaxNeutron(){
        return createQuantity()
                .name("maxNeutron")
                .label("Max Neutron")
                .definition(getRadUri("MaxNeutron"))
                .build();
    }

    public Boolean createIsAdjudicated() {
        return createBoolean()
                .name("isAdjudicated")
                .label("Is Adjudicated")
                .definition(getRadUri("IsAdjudicated"))
                .build();
    }

    public Text createAspectMessageFile(){
        return createText()
                .name("aspectMessage")
                .label("Aspect Message")
                .definition(getRadUri("AspectMessage"))
                .build();
    }

    public Time createBackgroundTime() {
        return createTime()
                .name("backgroundTime")
                .label("Background Time")
                .definition(getRadUri("BackgroundTime"))
                //range 20-120seconds
                .build();
    }
    public Time createOccupancyStartTime(){
        return createTime()
                .asPhenomenonTimeIsoUTC()
                .name("startTime")
                .label("Start Time")
                .definition(getRadUri("OccupancyStartTime"))
                .description("The start time of occupancy data")
                .build();
    }

    public Time createOccupancyEndTime(){
        return createTime()
                .asPhenomenonTimeIsoUTC()
                .name("endTime")
                .label("End Time")
                .definition(getRadUri("OccupancyEndTime"))
                .description("The end time of occupancy data")
                .build();
    }

    public Boolean createGammaAlarm() {
        return createBoolean()
                .name("gammaAlarm")
                .label("Gamma Alarm")
                .definition(getRadUri("GammaAlarm"))
                .build();
    }

    public Boolean createNeutronAlarm() {
        return createBoolean()
                .name("neutronAlarm")
                .label("Neutron Alarm")
                .definition(getRadUri("NeutronAlarm"))
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
                .definition(getRadUri("NeutronBackground"))
                .description("Neutron count to start occupancy")
                .build();
    }



    public Quantity createGammaBackground() {
        return createQuantity()
                .name("gammaBackground")
                .label("Gamma Background")
                .definition(getRadUri("GammaBackground"))
                .description("Gamma count to start occupancy")
                .build();
    }

    public Quantity createLatestGammaBackground() {
        return createQuantity()
                .name("latestGammaBackground")
                .label("Latest Gamma Background")
                .definition(getRadUri("LatestGammaBackground"))
                .description("Latest gamma background used in threshold and sigma calculations")
                .build();
    }

    public Quantity createGammaVariance() {
        return createQuantity()
                .name("gammaVariance")
                .label("Gamma Variance")
                .definition(getRadUri("GammaVariance"))
                .build();
    }

    public Quantity createNeutronVariance() {
        return createQuantity()
                .name("neutronVariance")
                .label("Neutron Variance")
                .definition(getRadUri("NeutronVariance"))
                .build();
    }

    public Quantity createGammaVarianceBackground() {
        return createQuantity()
                .name("gammaVarianceBackground")
                .label("Gamma Variance/Background")
                .definition(getRadUri("GammaVarianceBackground"))
                .build();
    }

    public Quantity createNeutronVarianceBackground() {
        return createQuantity()
                .name("neutronVarianceBackground")
                .label("Neutron Variance/Background")
                .definition(getRadUri("NeutronVarianceBackground"))
                .build();
    }

    public Quantity createObjectMark() {
        return createQuantity()
                .name("objectMark")
                .label("Object Mark")
                .definition(getRadUri("ObjectMark"))
                .build();
    }

    public Quantity createNSigma(){
        return createQuantity()
                .name("nSigma")
                .label("N Sigma")
                .definition(getRadUri("NSigma"))
                .description("Number of standard deviations above average background to test alarm threshold against")
                .build();
    }

    public Quantity createSigmaValue() {
        return createQuantity()
                .name("sigma")
                .label("Sigma Value")
                .definition(getRadUri("Sigma"))
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
                .definition(getRadUri("ZMax"))
                .description("Maximum z-value")
                .build();
    }

    public Quantity createAlphaValue(){
        return createQuantity()
                .name("alpha")
                .label("Alpha")
                .definition(getRadUri("Alpha"))
                .description("value used in calculations")
                .build();
    }
    public Quantity createOccupancyHoldin(){
        return createQuantity()
                .name("occupancyHoldin")
                .label("Occupancy Holdin")
                .definition(getRadUri("OccupancyHoldin"))
                .description("number of 200ms time intervals to hold in after occupancy signal indicates the system is vacant")
                .build();
    }

    public Quantity createIntervals(){
        return createQuantity()
                .name("intervals")
                .label("Intervals")
                .definition(getRadUri("Intervals"))
                .description("the number of time intervals to be considered")
                .build();
    }

    public Quantity createSequentialIntervals(){
        return createQuantity()
                .name("sequentialIntervals")
                .label("Sequential Intervals")
                .definition(getRadUri("SequentialIntervals"))
                .build();
    }

    public Quantity createMaxIntervals(){
        return createQuantity()
                .name("neutronMaximumIntervals")
                .label("Neutron Maximum Intervals")
                .definition(getRadUri("MaximumIntervals"))
                .build();
    }

    public Text createPlaceholder(){
        return createText()
                .name("placeholder")
                .label("Placeholder")
                .definition(getRadUri("Placeholder"))
                .build();
    }

    public Text createVideoPaths() {
        return createText()
                .name("videoPaths")
                .label("Video Paths")
                .description("Comma separated video file paths")
                .definition(getRadUri("VideoPaths"))
                .build();
    }

    public Quantity createBackgroundSigma(){
        return createQuantity()
                .name("backgroundSigma")
                .label("Background Sigma")
                .definition(getRadUri("NackgroundSigma"))
                .description("sets a sigma value for a throw-through alarm")
//                .description("is used to set alarm thresholds for detecting anomalies in radiation levels")
                .build();
    }

    public Quantity createHighBackgroundFault(){
        return createQuantity()
                .name("highBackgroundFault")
                .label("High Background Fault")
                .definition(getRadUri("HighBackgroundFault"))
                .description("threshold value measured in counts per second(cps) within a radiation detection system that will trigger a fault")
//                .uomCode("cps")
                .build();
    }
    public Quantity createLowBackgroundFault(){
        return createQuantity()
                .name("lowBackgroundFault")
                .label("Low Background Fault")
                .definition(getRadUri("LowBackgroundFault"))
                .description("threshold value measured in counts per second(cps) within a radiation detection system that will trigger a fault")
//                .uomCode("cps")
                .build();
    }
    public Text createDetectors(){
        return createText()
                .name("detectorsOnLine")
                .label("Detectors on line")
                .definition(getRadUri("DetectorsOnLine"))
                .description("radiation detectors that are actively operating and connected to the monitoring system")
                .build();
    }

    public Quantity createSlaveULD(){
        return createQuantity()
                .name("slaveLevelUpperDiscriminator")
                .label("Slave Upper Level Discriminator")
                .definition(getRadUri("SlaveLevelUpperDiscriminator"))
                .description("threshold setting that defines an upper limit for the energy of detected radiation events")
                .build();
    }

    public Quantity createSlaveLLD(){
        return createQuantity()
                .name("slaveLevelLowerDiscriminator")
                .label("Slave Lower Level Discriminator")
                .definition(getRadUri("SlaveLevelLowerDiscriminator"))
                .description("threshold setting that defines an lower limit for the energy of detected radiation events")
                .build();
    }
    public Quantity createRelayOutput(){
        return createQuantity()
                .name("relayOutput")
                .label("Relay Output")
                .definition(getRadUri("RelayOutput"))
                .description("electrical output signal that can activate or control other devices based on detection of radiation")
                .build();
    }
    public Text createAlgorithm(){
        return createText()
                .name("algorithm")
                .label("Algorithm")
                .definition(getRadUri("Algorithm"))
                .description("SUM, HORIZONTAL, VERTICAL, SINGLE" +
                        "permits the operator to select which detectors will be included in the alarm calculations")
                .build();
    }

    public Text createSoftwareVersion(){
        return createText()
                .name("softwareVersion")
                .label("Software Version")
                .definition(getRadUri("SoftwareVersion"))
                .addAllowedValues("A", "T")
                .build();
    }
    public Text createFirmwareVersion(){
        return createText()
                .name("firmwareVersion")
                .label("Firmware version")
                .definition(getRadUri("FirmwareVersion"))
                .build();
    }

    public Text createLaneID(){
        return createText()
                .name("laneID")
                .label("Lane ID")
                .definition(RADHelper.getRadUri("LaneId"))
                .description("identifies the lane for each rpm system")
                .build();
    }

    public Quantity createSpeedMph(){
        return createQuantity()
                .name("speedMPH")
                .label("Speed (MPH)")
                .definition(getRadUri("SpeedMph"))
                .uomCode("mph")
                //max 99
                .build();
    }

    public Quantity createSpeedKph(){
        return createQuantity()
                .name("speedKPH")
                .label("Speed (KPH)")
                .definition(getRadUri("SpeedKph"))
                .uomCode("kph")
                //max 999
                .build();
    }

    public Quantity createSpeedMms() {
        return createQuantity()
                .name("speedMMS")
                .label("Speed (MM/S)")
                .definition(getRadUri("SpeedMms"))
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
                .definition(getRadInstrumentUri("BatteryCharge"))
                .uomCode("%")
                .build();
    }

    public DataArray createLinCalibration(){
        return createArray()
                .name("linearCalibration")
                .label("Linear Calibration")
                .definition(getRadUri("LinearCalibration"))
                .withFixedSize(3)
                .withElement("linearCalibrationValues", createQuantity()
                        .label("Linear Calibration Values")
                        .definition(getRadUri("LinearCalibration"))
                        .description("Linear Calibration Values")
                        .dataType(DataType.DOUBLE)
                        .build())
                .build();
    }

    public DataArray createCmpCalibration(){
        return createArray()
                .name("cmpCalibration")
                .label("Cmp Calibration")
                .definition(getRadUri("CmpCal"))
                .withFixedSize(3)
                .withElement("CmpCalibrationValues", createQuantity()
                        .label("Cmp Calibration Values")
                        .definition(getRadUri("CmpCalVals"))
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
                .definition(getRadUri("LinSpectrum"))
                .withVariableSize(fieldID)
                .withElement("linSpectrumValues", createQuantity()
                        .label("Lin Spectrum Values")
                        .definition(getRadUri("LinSpectrumVals"))
                        .description("Spectrum Values")
                        .dataType(DataType.DOUBLE)
                        .build())
                .build();
    }

    public DataArray createCmpSpectrum(String fieldID){
        return createArray()
                .name("cmpSpectrum")
                .label("Cmp Spectrum")
                .definition(getRadUri("CmpSpectrum"))
                .withVariableSize(fieldID)
                .withElement("cmpSpectrumValues", createQuantity()
                        .label("Cmp Spectrum Values")
                        .definition(getRadUri("CmpSpectrumVals"))
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
                .definition(getRadUri("GammaCount"))
                .build();
    }

    public Count createGammaCountPerInterval(int countID){
        return createCount()
                .name("gammaCountPerInterval" + countID)
                .label("Gamma Count (200ms) " + countID)
                .definition(getRadUri("GammaCountPerInterval"))
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
                .definition(getRadUri("NeutronCount"))
                .build();
    }

    public Quantity createDoseUSVh(){
        return createQuantity()
                .name("dose")
                .label("Dose")
                .definition(getRadUri("Dose"))
                .uomCode("uSv/h")
                .build();
    }

    public Category createMeasurementClassCode(){
        return createCategory()
                .name("measurementClassCode")
                .label("Measurement Class Code")
                .definition(getRadUri("MeasurementClassCode"))
                .addAllowedValues(MeasurementClassCodeSimpleType.FOREGROUND.value(), MeasurementClassCodeSimpleType.INTRINSIC_ACTIVITY.value(), MeasurementClassCodeSimpleType.BACKGROUND.value(), MeasurementClassCodeSimpleType.NOT_SPECIFIED.value(), MeasurementClassCodeSimpleType.CALIBRATION.value())
                .build();
    }

    public Category createAlarmCatCode(){
        return createCategory()
                .name("alarmCategoryCode")
                .label("Alarm Category Code")
                .definition(getRadUri("AlarmCategoryCode"))
                .addAllowedValues(RadAlarmCategoryCodeSimpleType.ALPHA.value(),RadAlarmCategoryCodeSimpleType.NEUTRON.value(),RadAlarmCategoryCodeSimpleType.BETA.value(),RadAlarmCategoryCodeSimpleType.GAMMA.value(),RadAlarmCategoryCodeSimpleType.OTHER.value(),RadAlarmCategoryCodeSimpleType.ISOTOPE.value())
                .build();
    }


    public Quantity createMasterLLD(){
        return createQuantity()
                .name("masterLowerLevelDiscriminator")
                .label("Master Lower Level Discriminator")
                .definition(getRadUri("MasterLowerLevelDiscriminator"))
                .build();
    }

    public Quantity createMasterULD(){
        return createQuantity()
                .name("masterUpperLevelDiscriminator")
                .label("Master UpperLevel Discriminator")
                .definition(getRadUri("MasterUpperLevelDiscriminator"))
                .build();
    }

    //////////////////////////////// vvvv OLD vvvvvv ///////////////////////////////

    // RadInstrumentInformation
    public Text createRIManufacturerName() {
        return createText()
                .name("radInstrumentManufacturerName")
                .label("Rad Instrument Manufacturer Name")
                .definition(getRadInstrumentUri("ManufacturerName"))
                .description("Manufacturer name of described RAD Instrument")
                .build();
    }

    public Category createRIIdentifier() {
        return createCategory()
                .name("radInstrumentIdentifier")
                .label("Rad Instrument Identifier")
                .definition(getRadInstrumentUri("Identifier"))
                .description("Identifier for described RAD Instrument")
                .build();
    }

    public Text createRIModelName() {
        return createText()
                .name("radInstrumentModelName")
                .label("Rad Instrument Model Name")
                .definition(getRadInstrumentUri("ModelName"))
                .description("Model name of described RAD Instrument")
                .build();
    }

    public Text createRIDescription() {
        return createText()
                .name("radInstrumentDescription")
                .label("Rad Instrument Description")
                .definition(getRadInstrumentUri("Description"))
                .description("Description of RAD Instrument")
                .build();
    }

    public Category createRIClassCode() {
        return createCategory()
                .name("radInstrumentClassCode")
                .label("Rad Instrument Class Code")
                .definition(getRadInstrumentUri("ClassCode"))
                .description("Class Code for type of RAD Instrument")
                .addAllowedValues("Backpack", "Dosimeter", "Electronic Personal Emergency Radiation Detector", "Mobile System", "Network Area Monitor", "Neutron Handheld", "Personal Radiation Detector", "Radionuclide Identifier", "Portal Monitor", "Spectroscopic Portal Monitor", "Spectroscopic Personal Radiation Detector", "Gamma Handheld", "Transportable System", "Other")
                .build();
    }

    public DataRecord createRIVersion() {
        return createRecord()
                .name("radInstrumentVersion")
                .label("Rad Instrument Version")
                .definition(getRadInstrumentUri("Version"))
                .addField("RadInstrumentComponentName", createRIComponentName())
                .addField("RadInstrumentComponentVersion", createRIComponentVersion())
                .build();
    }

    public Text createRIComponentName() {
        return createText()
                .name("radInstrumentComponentName")
                .label("Rad Instrument Component Name")
                .definition(getRadInstrumentUri("ComponentName"))
                .build();
    }

    public Text createRapiscanMessage(){
        return createText()
                .name("rapiscanMessage")
                .label("Rapiscan Message")
                .definition(getRadInstrumentUri("RapiscanMessage"))
                .build();
    }
    public Text createRIComponentVersion() {
        return createText()
                .name("radInstrumentComponentVersion")
                .label("Rad Instrument Component Version")
                .definition(getRadInstrumentUri("ComponentVersion"))
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
                .definition(getRadInstrumentUri("Characteristics"))
                .build();
    }

    public DataRecord createCharacteristicGroup() {
        return createRecord()
                .name("characteristicGroupName")
                .label("Characteristic Group Name")
                .definition(getRadInstrumentUri("CharacteristicGroup"))
                .build();
    }

    public DataRecord createCharacteristicText() {
        DataRecord record = createRecord()
                .name("")
                .addField("characteristicName",
                        createText()
                                .label("Characteristic Name")
                                .definition(getRadInstrumentUri("CharacteristicName"))
                                .build())
                .addField("characteristicValue",
                        createText()
                                .label("Characteristic Value")
                                .definition(getRadInstrumentUri("CharacteristicValue"))
                                .build())
                .addField("characteristicValueUnits",
                        createText()
                                .label("Characteristic Value Units")
                                .definition(getRadInstrumentUri("CharacteristicValueUnits"))
                                .build())
                .addField("characteristicValueDataClassCode",
                        createCategory()
                                .label("Characteristic Value Data Class Code")
                                .definition(getRadInstrumentUri("CharacteristicValueDataClassCode"))
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
                                .definition(getRadInstrumentUri("CharacteristicName"))
                                .build())
                .addField("characteristicValue",
                        createQuantity()
                                .label("Characteristic Value")
                                .definition(getRadInstrumentUri("CharacteristicValue"))
                                .build())
                .addField("characteristicValueUnits",
                        createText()
                                .label("Characteristic Value Units")
                                .definition(getRadInstrumentUri("CharacteristicValueUnits"))
                                .build())
                .addField("characteristicValueDataClassCode",
                        createCategory()
                                .label("Characteristic Value Data Class Code")
                                .definition(getRadInstrumentUri("CharacteristicValueDataClassCode"))
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
                                .definition(getRadInstrumentUri("CharacteristicName"))
                                .build())
                .addField("characteristicValue",
                        createCount()
                                .label("Characteristic Value")
                                .definition(getRadInstrumentUri("CharacteristicValue"))
                                .build())
                .addField("characteristicValueUnits",
                        createText()
                                .label("Characteristic Value Units")
                                .definition(getRadInstrumentUri("CharacteristicValueUnits"))
                                .build())
                .addField("characteristicValueDataClassCode",
                        createCategory()
                                .label("Characteristic Value Data Class Code")
                                .definition(getRadInstrumentUri("CharacteristicValueDataClassCode"))
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
                                .definition(getRadInstrumentUri("CharacteristicName"))
                                .build())
                .addField("characteristicValue",
                        createBoolean()
                                .label("Characteristic Value")
                                .definition(getRadInstrumentUri("CharacteristicValue"))
                                .build())
                .addField("characteristicValueUnits",
                        createText()
                                .label("Characteristic Value Units")
                                .definition(getRadInstrumentUri("CharacteristicValueUnits"))
                                .build())
                .addField("characteristicValueDataClassCode",
                        createCategory()
                                .label("Characteristic Value Data Class Code")
                                .definition(getRadInstrumentUri("CharacteristicValueDataClassCode"))
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
                .definition(getRadDetectorURI("Name"))
                .build();
    }

    public Category createRadDetectorCategoryCode(){
        return createCategory()
                .name("radDetectorCategoryCode")
                .label("Rad Detector Category Code")
                .definition(getRadDetectorURI("CategoryCode"))
                .addAllowedValues(RadDetectorCategoryCodeSimpleType.GAMMA.value(), RadDetectorCategoryCodeSimpleType.NEUTRON.value(), RadDetectorCategoryCodeSimpleType.ALPHA.value(), RadDetectorCategoryCodeSimpleType.BETA.value(), RadDetectorCategoryCodeSimpleType.X_RAY.value(), RadDetectorCategoryCodeSimpleType.OTHER.value())
                .build();
    }

    public Category createRadDetectorKindCode(){
        return createCategory()
                .name("radDetectorKindCode")
                .label("Rad Detector Kind Code")
                .definition(getRadDetectorURI("KindKode"))
                .addAllowedValues(RadDetectorKindCodeSimpleType.HP_GE.value(), RadDetectorKindCodeSimpleType.HP_XE.value(), RadDetectorKindCodeSimpleType.NA_I.value(), RadDetectorKindCodeSimpleType.LA_BR_3.value(), RadDetectorKindCodeSimpleType.LA_CL_3.value(), RadDetectorKindCodeSimpleType.BGO.value(), RadDetectorKindCodeSimpleType.CZT.value(), RadDetectorKindCodeSimpleType.CD_TE.value(), RadDetectorKindCodeSimpleType.CS_I.value(), RadDetectorKindCodeSimpleType.GMT.value(), RadDetectorKindCodeSimpleType.GMTW.value(), RadDetectorKindCodeSimpleType.LI_FIBER.value(), RadDetectorKindCodeSimpleType.PVT.value(), RadDetectorKindCodeSimpleType.PS.value(), RadDetectorKindCodeSimpleType.HE_3.value(), RadDetectorKindCodeSimpleType.HE_4.value(), RadDetectorKindCodeSimpleType.LI_GLASS.value(), RadDetectorKindCodeSimpleType.LI_I.value(), RadDetectorKindCodeSimpleType.SR_I_2.value(), RadDetectorKindCodeSimpleType.CLYC.value(), RadDetectorKindCodeSimpleType.CD_WO_4.value(), RadDetectorKindCodeSimpleType.BF_3.value(), RadDetectorKindCodeSimpleType.HG_I_2.value(), RadDetectorKindCodeSimpleType.CE_BR_4.value(), RadDetectorKindCodeSimpleType.LI_CAF.value(), RadDetectorKindCodeSimpleType.LI_ZN_S.value(), RadDetectorKindCodeSimpleType.OTHER.value())
                .build();
    }

    public Text createRadDetectorDescription(){
        return createText()
                .name("radDetectorDescription")
                .label("Rad Detector Description")
                .definition(getRadDetectorURI("Description"))
                .build();
    }

    public Quantity createRadDetectorLengthValue(){
        return createQuantity()
                .name("radDetectorLengthValue")
                .label("Rad Detector Length Value")
                .definition(getRadDetectorURI("LengthValue"))
                .uom("cm")
                .build();
    }

    public Quantity createRadDetectorWidthValue(){
        return createQuantity()
                .name("radDetectorWidthValue")
                .label("Rad Detector Width Value")
                .definition(getRadDetectorURI("Width"))
                .uom("cm")
                .build();
    }

    public Quantity createRadDetectorDepthValue(){
        return createQuantity()
                .name("radDetectorDepthValue")
                .label("Rad Detector Depth Value")
                .definition(getRadDetectorURI("Depth"))
                .uom("cm")
                .build();
    }

    public Quantity createRadDetectorDiameterValue(){
        return createQuantity()
                .name("radDetectorDiameterValue")
                .label("Rad Detector Diameter Value")
                .definition(getRadDetectorURI("Diameter"))
                .uom("cm")
                .build();
    }

    public Quantity createRadDetectorVolumeValue(){
        return createQuantity()
                .name("radDetectorVolumeValue")
                .label("Rad Detector Volume Value")
                .definition(getRadDetectorURI("Volume"))
                .description("Detection Volume in cubic centimeters")
                .uom("cc")
                .build();
    }

    public DataRecord createRadDetectorCharacteristics() {
        return createRecord()
                .name("radDetectorCharacteristics")
                .label("Rad Detector Characteristics")
                .definition(getRadDetectorURI("Characteristics"))
                .build();
    }

    // RAD ITEM INFORMATION

    public Text createRadItemDescription(){
        return createText()
                .name("radItemDescription")
                .label("Rad Item Description")
                .definition(getRadItemURI("Description"))
                .build();
    }

    public DataRecord createRadItemQuantity(){
        return createRecord()
                .name("radItemQuantity")
                .label("Rad Item Quantity")
                .definition(getRadItemURI("Quantity"))
                .addField("radItemQuantityValue",
                        createQuantity()
                                .label("Rad Item Quantity Value")
                                .definition(getRadItemURI("ItemQuantityValue"))
                                .build())
                .addField("radItemQuantityUncertaintyValue",
                        createQuantity()
                                .label("Rad Item Quantity Uncertainty Value")
                                .definition(getRadItemURI("ItemQuantityUncertainty"))
                                .build())
                .addField("radItemQuantityUnits",
                        createText()
                                .label("Rad Item Quantity Units")
                                .definition(getRadItemURI("QuantityUnits"))
                                .build())
                .build();
    }

    public DataRecord createRadItemCharacteristics() {
        return createRecord()
                .name("radItemCharacteristics")
                .label("Rad Item Characteristics")
                .definition(getRadItemURI("Characteristics"))
                .build();
    }

    // Adjudication
    public DataRecord createAdjudicationRecord() {
        return createRecord()
                .name("adjudication")
                .label("Adjudication")
                .description("Adjudication data associated to an lane occupancy")
                .addField("feedback", createText()
                        .label("Feedback")
                        .definition(SWEHelper.getPropertyUri("Feedback"))
                        .optional(true)
                        .build())
                .addField("adjudicationCode", createCount()
                        .label("Adjudication Code")
                        .definition(SWEHelper.getPropertyUri("AdjudicationCode"))
                        .addAllowedInterval(0, 11)
                        .optional(true)
                        .build())
                .addField("isotopes", createText()
                        .label("Isotopes")
                        .description("Comma separated isotopes")
                        .definition(SWEHelper.getPropertyUri("Isotopes"))
                        .optional(true)
                        .build())
                // TODO: Ask tyrone about this
                .addField("secondaryInspectionStatus", createText()
                        .label("Secondary Inspection Status")
                        .definition(SWEHelper.getPropertyUri("SecondaryInspectionStatus"))
                        .optional(true)
                        .build())
                .addField("filePaths", createText()
                        .label("File Paths")
                        .description("Comma separated relative file paths")
                        .definition(SWEHelper.getPropertyUri("FilePaths"))
                        .optional(true)
                        .build())
                .addField("occupancyId", createText()
                        .label("Occupancy ID")
                        .definition(SWEHelper.getPropertyUri("OccupancyID"))
                        .optional(false)
                        .build())
                .addField("vehicleId", createText()
                        .label("Vehicle ID")
                        .definition(SWEHelper.getPropertyUri("VehicleID"))
                        .optional(true)
                        .build())
                .build();
    }

    // Node stats
    public DataRecord createSiteStatistics() {
        return createRecord()
                .name("siteStatistics")
                .label("Site Statistics")
                .description("Statistics for this node's RPMs/lanes")
                .addField("samplingTime", createTime()
                        .asSamplingTimeIsoUTC())
                .addField("allTime", createRecord()
                        .label("All Time Total")
                        .definition(getRadUri("AllTimeCount"))
                        .addAllFields(createCountStatistics()))
                .addField("monthly", createRecord()
                        .label("Monthly Total")
                        .definition(getRadUri("MonthlyCount"))
                        .addAllFields(createCountStatistics()))
                .addField("weekly", createRecord()
                        .label("Weekly Total")
                        .definition(getRadUri("WeeklyCount"))
                        .addAllFields(createCountStatistics()))
                .addField("daily", createRecord()
                        .label("Daily Total")
                        .definition(getRadUri("DailyCount"))
                        .addAllFields(createCountStatistics()))
                .build();
    }

    public DataRecord createCountStatistics() {
        return createRecord()
                .name("counts")
                .label("Counts")
                // Occupancies
                .addField("numOccupancies", createQuantity()
                        .dataType(DataType.LONG)
                        .label("Total Number of Occupancies")
                        .definition(getRadUri("NumOccupancies")))
                .addField("numGammaAlarms", createQuantity()
                        .dataType(DataType.LONG)
                        .label("Total Number of Gamma Alarms")
                        .definition(getRadUri("NumGammaAlarms")))
                .addField("numNeutronAlarms", createQuantity()
                        .dataType(DataType.LONG)
                        .label("Total Number of Neutron Alarms")
                        .definition(getRadUri("NumNeutronAlarms")))
                .addField("numGammaNeutronAlarms", createQuantity()
                        .dataType(DataType.LONG)
                        .label("Total Number of Gamma-Neutron Alarms")
                        .definition(getRadUri("NumGammaNeutronAlarms")))
                // Faults
                .addField("numFaults", createQuantity()
                        .dataType(DataType.LONG)
                        .label("Total Number of Faults")
                        .definition(getRadUri("NumFaults")))
                .addField("numGammaFaults", createQuantity()
                        .dataType(DataType.LONG)
                        .label("Total Number of Gamma Faults")
                        .definition(getRadUri("NumGammaFaults")))
                .addField("numNeutronFaults", createQuantity()
                        .dataType(DataType.LONG)
                        .label("Total Number of Neutron Faults")
                        .definition(getRadUri("NumNeutronFaults")))
                .addField("numTampers", createQuantity()
                        .dataType(DataType.LONG)
                        .label("Total Number of Tampers")
                        .definition(getRadUri("NumTampers")))
                .build();
    }

}