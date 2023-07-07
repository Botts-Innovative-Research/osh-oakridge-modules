package com.botts.impl.sensor.rs350;

import com.botts.impl.utils.n42.*;
import jakarta.xml.bind.JAXBElement;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;
import java.util.Locale;


public class RS350Message {

    public RS350Message(RadInstrumentDataType msg){

        SWEHelper help = new SWEHelper();
        GeoPosHelper geoPosHelper = new GeoPosHelper();
         geoPosHelper.createLocationVectorLatLon();


        // create Instrument Info
        RadInstrumentInformationType instrumentInfo = msg.getRadInstrumentInformation();

        RS350InstrumentInformation rs350InstrumentInformation = new RS350InstrumentInformation(instrumentInfo.getRadInstrumentManufacturerName(), instrumentInfo.getRadInstrumentIdentifier(), instrumentInfo.getRadInstrumentModelName(), instrumentInfo.getRadInstrumentClassCode().name() );

        // create Instrument Characteristics
        CharacteristicsType InstrumentInfoChars = msg.getRadInstrumentInformation().getRadInstrumentCharacteristics().get(0);
        CharacteristicType deviceName = (CharacteristicType) InstrumentInfoChars.getCharacteristicOrCharacteristicGroup().get(0);
        CharacteristicType batteryCharge = (CharacteristicType) InstrumentInfoChars.getCharacteristicOrCharacteristicGroup().get(1);

        RS350InstrumentCharacteristics rs350InstrumentCharacteristics = new RS350InstrumentCharacteristics(deviceName.getCharacteristicValue(), Double.parseDouble(batteryCharge.getCharacteristicValue()));

        // create Item
        CharacteristicsType ItemInfoChars = msg.getRadItemInformation().get(0).getRadItemCharacteristics().get(0);
        CharacteristicType scanMode = (CharacteristicType) ItemInfoChars.getCharacteristicOrCharacteristicGroup().get(0);
        CharacteristicType scanNumber = (CharacteristicType) ItemInfoChars.getCharacteristicOrCharacteristicGroup().get(1);
        CharacteristicType scanTimeoutNumber = (CharacteristicType) ItemInfoChars.getCharacteristicOrCharacteristicGroup().get(2);
        CharacteristicType analysisEnabled = (CharacteristicType) ItemInfoChars.getCharacteristicOrCharacteristicGroup().get(3);

        RS350Item rs350Item = new RS350Item(scanMode.getCharacteristicValue(), Double.parseDouble(scanNumber.getCharacteristicValue()), Double.parseDouble(scanTimeoutNumber.getCharacteristicValue()), analysisEnabled.getCharacteristicValue());

        // create Lin Energy Calibration
        JAXBElement<EnergyCalibrationType> linEnergyCalibrationTypeJAXBElement = (JAXBElement<EnergyCalibrationType>) msg.getRadMeasurementOrRadMeasurementGroupOrEnergyCalibration().get(0);
        EnergyCalibrationType linEnCal = linEnergyCalibrationTypeJAXBElement.getValue();

        // create Cmp Energy Calibration
        JAXBElement<EnergyCalibrationType> cmpEnergyCalibrationTypeJAXBElement = (JAXBElement<EnergyCalibrationType>) msg.getRadMeasurementOrRadMeasurementGroupOrEnergyCalibration().get(1);
        EnergyCalibrationType cmpEnCal = cmpEnergyCalibrationTypeJAXBElement.getValue();

        RS350EnergyCalibration rs350EnergyCalibration = new RS350EnergyCalibration(linEnCal.getCoefficientValues(), cmpEnCal.getCoefficientValues());

        ///// SKIP MEASUREMENT GROUP TYPE

        // create Background
        JAXBElement<RadMeasurementType> radMeasurementBackgroundTypeJAXBElement = (JAXBElement<RadMeasurementType>) msg.getRadMeasurementOrRadMeasurementGroupOrEnergyCalibration().get(3);
        RadMeasurementType background = radMeasurementBackgroundTypeJAXBElement.getValue();

        RS350BackgroundMeasurement rs350BackgroundMeasurement = new RS350BackgroundMeasurement(background.getMeasurementClassCode().name(), background.getStartDateTime().toGregorianCalendar().getTimeInMillis(), new Double(background.getRealTimeDuration().getSeconds()%60), background.getSpectrum().get(0).getChannelData().getValue(), background.getSpectrum().get(1).getChannelData().getValue(), background.getGrossCounts().get(0).getCountData().get(0), background.getGrossCounts().get(1).getCountData().get(0));

        if (msg.getRadMeasurementOrRadMeasurementGroupOrEnergyCalibration().get(4) != null) {
            // create Foreground
            JAXBElement<RadMeasurementType> radMeasurementForegroundTypeJAXBElement = (JAXBElement<RadMeasurementType>) msg.getRadMeasurementOrRadMeasurementGroupOrEnergyCalibration().get(4);
            RadMeasurementType foreground = radMeasurementForegroundTypeJAXBElement.getValue();

            RS350ForegroundMeasurement rs350ForegroundMeasurement = new RS350ForegroundMeasurement(foreground.getMeasurementClassCode().name(), foreground.getStartDateTime().toGregorianCalendar().getTimeInMillis(), new Double(foreground.getRealTimeDuration().getSeconds() % 60), foreground.getSpectrum().get(0).getChannelData().getValue(), foreground.getSpectrum().get(1).getChannelData().getValue(), foreground.getGrossCounts().get(0).getCountData().get(0), foreground.getGrossCounts().get(1).getCountData().get(0), foreground.getDoseRate().get(0).getDoseRateValue().getValue(), foreground.getRadItemState().get(0).getStateVector().getGeographicPoint().getLatitudeValue().getValue().doubleValue(), foreground.getRadItemState().get(0).getStateVector().getGeographicPoint().getLongitudeValue().getValue().doubleValue(), foreground.getRadItemState().get(0).getStateVector().getGeographicPoint().getElevationValue().doubleValue());
        }
        //create Alarm
        JAXBElement<DerivedDataType> derivedDataTypeJAXBElement = (JAXBElement<DerivedDataType>) msg.getRadMeasurementOrRadMeasurementGroupOrEnergyCalibration().get(5);
        DerivedDataType alarm = derivedDataTypeJAXBElement.getValue();

        RS350Alarm rs350Alarm = new RS350Alarm(alarm.getRemark().get(0), alarm.getMeasurementClassCode().name(), alarm.getStartDateTime().toGregorianCalendar().getTimeInMillis(), new Double(alarm.getRealTimeDuration().getSeconds()%60));

    }

    public class RS350InstrumentInformation {
        public RS350InstrumentInformation(String manufacturerName, String identifier, String modelName, String classCode){
            this.manufacturerName = manufacturerName;
            this.identifier = identifier;
            this.modelName = modelName;
            this.classCode = classCode;

        }
         String manufacturerName;
         String identifier;
         String modelName;
         String classCode;


    }

    public class RS350InstrumentCharacteristics {

        public RS350InstrumentCharacteristics(String deviceName, Double batteryCharge){
            this.deviceName = deviceName;
            this.batteryCharge = batteryCharge;
        }

        String deviceName;
        Double batteryCharge;
    }

    public class RS350DetectorInformation {

    }

    public class RS350Item {

        public RS350Item(String rsiScanMode, Double rsiScanNumber, Double rsiScanTimeoutNumber, String rsiAnalysisEnabled){
            this.rsiScanMode = rsiScanMode;
            this.rsiScanNumber = rsiScanNumber;
            this.rsiScanTimeoutNumber = rsiScanTimeoutNumber;
            this.rsiAnalysisEnabled = rsiAnalysisEnabled;
        }

        String rsiScanMode;
        Double rsiScanNumber;
        Double rsiScanTimeoutNumber;
        String rsiAnalysisEnabled;
    }

    public class RS350EnergyCalibration {
        public RS350EnergyCalibration(List<Double> linEnCal, List<Double> cmpEnCal){
            this.linEnCal = linEnCal;
            this.cmpEnCal = linEnCal;
        }
        List<Double> linEnCal;
        List<Double> cmpEnCal;
    }

    public class RS350BackgroundMeasurement {
        public RS350BackgroundMeasurement(String classCode, Long startDateTime, Double realTimeDuration, List<Double> linEnCalSpectrum, List<Double> cmpEnCalSpectrum, Double gammaGrossCount, Double neutronGrossCount){
            this.classCode = classCode;
            this.startDateTime = startDateTime;
            this.realTimeDuration = realTimeDuration;
            this.linEnCalSpectrum = linEnCalSpectrum;
            this.cmpEnCalSpectrum = cmpEnCalSpectrum;
            this.gammaGrossCount = gammaGrossCount;
            this.neutronGrossCount = neutronGrossCount;
        }
        String classCode;
        Long startDateTime;
        Double realTimeDuration;
        List<Double> linEnCalSpectrum;
        List<Double> cmpEnCalSpectrum;
        Double gammaGrossCount;
        Double neutronGrossCount;
    }

    public class RS350ForegroundMeasurement {
        public RS350ForegroundMeasurement(String classCode, Long startDateTime, Double realTimeDuration, List<Double> linEnCalSpectrum, List<Double> cmpEnCalSpectrum, Double gammaGrossCount, Double neutronGrossCount, Double doseRate, Double lat, Double lon, Double alt){
            this.classCode = classCode;
            this.startDateTime = startDateTime;
            this.realTimeDuration = realTimeDuration;
            this.linEnCalSpectrum = linEnCalSpectrum;
            this.cmpEnCalSpectrum = cmpEnCalSpectrum;
            this.gammaGrossCount = gammaGrossCount;
            this.neutronGrossCount = neutronGrossCount;
            this.doseRate = doseRate;
            this.lat = lat;
            this.lon = lon;
            this.alt = alt;
        }
        String classCode;
        Long startDateTime;
        Double realTimeDuration;
        List<Double> linEnCalSpectrum;
        List<Double> cmpEnCalSpectrum;
        Double gammaGrossCount;
        Double neutronGrossCount;
        Double doseRate;
        Double lat;
        Double lon;
        Double alt;
    }

    public class RS350Alarm {
        public RS350Alarm(String alarmType, String classCode, Long startDateTime, Double duration){
            this.alarmType = alarmType;
            this.classCode = classCode;
            this.startDateTime = startDateTime;
            this.duration = duration;
        }
        String alarmType;
        String classCode;
        Long startDateTime;
        Double duration;
    }

}
