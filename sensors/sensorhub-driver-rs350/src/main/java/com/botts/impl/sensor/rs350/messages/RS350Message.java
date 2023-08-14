package com.botts.impl.sensor.rs350.messages;

import com.botts.impl.utils.n42.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


public class RS350Message {

    Logger logger = LoggerFactory.getLogger(RS350Message.class);

     RS350InstrumentInformation rs350InstrumentInformation;
     RS350InstrumentCharacteristics rs350InstrumentCharacteristics;
     RS350Item rs350Item;
     RS350LinEnergyCalibration rs350LinEnergyCalibration;
     RS350CmpEnergyCalibration rs350CmpEnergyCalibration;
     RS350BackgroundMeasurement rs350BackgroundMeasurement;
     RS350ForegroundMeasurement rs350ForegroundMeasurement;
     RS350DerivedData rs350DerivedData;
     RS350RadAlarm  rs350RadAlarm;


    public RS350Message(RadInstrumentDataType msg){


        SWEHelper help = new SWEHelper();
        GeoPosHelper geoPosHelper = new GeoPosHelper();
         geoPosHelper.createLocationVectorLatLon();


        // create Instrument Info
        RadInstrumentInformationType instrumentInfo = msg.getRadInstrumentInformation();

        rs350InstrumentInformation = new RS350InstrumentInformation(instrumentInfo.getRadInstrumentManufacturerName(), instrumentInfo.getRadInstrumentIdentifier(), instrumentInfo.getRadInstrumentModelName(), instrumentInfo.getRadInstrumentClassCode().name() );

        // create Instrument Characteristics
        CharacteristicsType InstrumentInfoChars = msg.getRadInstrumentInformation().getRadInstrumentCharacteristics().get(0);
        CharacteristicType deviceName = (CharacteristicType) InstrumentInfoChars.getCharacteristicOrCharacteristicGroup().get(0);
        CharacteristicType batteryCharge = (CharacteristicType) InstrumentInfoChars.getCharacteristicOrCharacteristicGroup().get(1);

        rs350InstrumentCharacteristics = new RS350InstrumentCharacteristics(deviceName.getCharacteristicValue(), Double.parseDouble(batteryCharge.getCharacteristicValue()));

        // create Item
        CharacteristicsType ItemInfoChars = msg.getRadItemInformation().get(0).getRadItemCharacteristics().get(0);
        CharacteristicType scanMode = (CharacteristicType) ItemInfoChars.getCharacteristicOrCharacteristicGroup().get(0);
        CharacteristicType scanNumber = (CharacteristicType) ItemInfoChars.getCharacteristicOrCharacteristicGroup().get(1);
        CharacteristicType scanTimeoutNumber = (CharacteristicType) ItemInfoChars.getCharacteristicOrCharacteristicGroup().get(2);
        CharacteristicType analysisEnabled = (CharacteristicType) ItemInfoChars.getCharacteristicOrCharacteristicGroup().get(3);

        rs350Item = new RS350Item(scanMode.getCharacteristicValue(), Double.parseDouble(scanNumber.getCharacteristicValue()), Double.parseDouble(scanTimeoutNumber.getCharacteristicValue()), analysisEnabled.getCharacteristicValue());

        msg.getRadMeasurementOrRadMeasurementGroupOrEnergyCalibration().forEach(jaxbElement -> {

            Class<?> jaxbType = jaxbElement.getDeclaredType();
            if (jaxbType == EnergyCalibrationType.class) {
                EnergyCalibrationType energyCalibrationType = (EnergyCalibrationType) jaxbElement.getValue();
                switch (energyCalibrationType.getId()) {
                    case "LinEnCal":{
                         rs350LinEnergyCalibration = new RS350LinEnergyCalibration(energyCalibrationType.getCoefficientValues());
                    }
                        break;
                    case "CmpEnCal":{
                        rs350CmpEnergyCalibration = new RS350CmpEnergyCalibration(energyCalibrationType.getCoefficientValues());
                    }
                    default: logger.debug("EnergyCalType ID: " + energyCalibrationType.getId());
                        break;
                }
            }
                else if (jaxbType == RadMeasurementType.class) {
                RadMeasurementType radMeasurementType = (RadMeasurementType) jaxbElement.getValue();
                switch (radMeasurementType.getMeasurementClassCode().value()) {
                    case "Background": {
                        rs350BackgroundMeasurement = new RS350BackgroundMeasurement(radMeasurementType.getMeasurementClassCode().name(), radMeasurementType.getStartDateTime().toGregorianCalendar().getTimeInMillis(), new Double(radMeasurementType.getRealTimeDuration().getSeconds() % 60), radMeasurementType.getSpectrum().get(0).getChannelData().getValue(), radMeasurementType.getSpectrum().get(1).getChannelData().getValue(), radMeasurementType.getGrossCounts().get(0).getCountData().get(0), radMeasurementType.getGrossCounts().get(1).getCountData().get(0));
                    }
                    break;
                    case "Foreground": {

                        Double lat = 0.0;
                        Double lon = 0.0;
                        Double elv = 0.0;
                        if (radMeasurementType.getRadInstrumentState().getStateVector().getGeographicPoint().getLongitudeValue() != null) {
                        lat = radMeasurementType.getRadInstrumentState().getStateVector().getGeographicPoint().getLatitudeValue().getValue().doubleValue();
                        lon = radMeasurementType.getRadInstrumentState().getStateVector().getGeographicPoint().getLongitudeValue().getValue().doubleValue();
                        elv = radMeasurementType.getRadInstrumentState().getStateVector().getGeographicPoint().getElevationValue().doubleValue();
                        }
                        rs350ForegroundMeasurement = new RS350ForegroundMeasurement(radMeasurementType.getMeasurementClassCode().name(), radMeasurementType.getStartDateTime().toGregorianCalendar().getTimeInMillis(), new Double(radMeasurementType.getRealTimeDuration().getSeconds() % 60), radMeasurementType.getSpectrum().get(0).getChannelData().getValue(), radMeasurementType.getSpectrum().get(1).getChannelData().getValue(), radMeasurementType.getGrossCounts().get(0).getCountData().get(0), radMeasurementType.getGrossCounts().get(1).getCountData().get(0), radMeasurementType.getDoseRate().get(0).getDoseRateValue().getValue(), lat, lon, elv);
                    }
                    break;
                    default:
                        logger.debug("Measurement Class Code: " + radMeasurementType.getMeasurementClassCode().value());

                }

            }
                else if (jaxbType == DerivedDataType.class){
                    DerivedDataType derivedDataType = (DerivedDataType) jaxbElement.getValue();
                    rs350DerivedData = new RS350DerivedData(derivedDataType.getRemark().get(0), derivedDataType.getMeasurementClassCode().name(), derivedDataType.getStartDateTime().toGregorianCalendar().getTimeInMillis(), new Double(derivedDataType.getRealTimeDuration().getSeconds()%60));

            }
                else if (jaxbType == RadAlarmType.class){
                    RadAlarmType radAlarmType = (RadAlarmType) jaxbElement.getValue();
                    rs350RadAlarm = new RS350RadAlarm(radAlarmType.getRadAlarmCategoryCode().value(), radAlarmType.getRadAlarmDescription());

            }

                else {
                    logger.debug(jaxbType.toString());
            }

        });

    }

    public RS350InstrumentInformation getRs350InstrumentInformation(){
        return rs350InstrumentInformation;
    }

    public RS350InstrumentCharacteristics getRs350InstrumentCharacteristics() {
        return rs350InstrumentCharacteristics;
    }

    public RS350Item getRs350Item() {
        return rs350Item;
    }

    public RS350LinEnergyCalibration getRs350LinEnergyCalibration() {
        return rs350LinEnergyCalibration;
    }

    public RS350CmpEnergyCalibration getRs350CmpEnergyCalibration() {
        return rs350CmpEnergyCalibration;
    }

    public RS350BackgroundMeasurement getRs350BackgroundMeasurement() {
        return rs350BackgroundMeasurement;
    }

    public RS350ForegroundMeasurement getRs350ForegroundMeasurement() {
        return rs350ForegroundMeasurement;
    }

    public RS350DerivedData getRs350DerivedData() {
        return rs350DerivedData;
    }

    public RS350RadAlarm getRs350RadAlarm() {
        return rs350RadAlarm;
    }
}
