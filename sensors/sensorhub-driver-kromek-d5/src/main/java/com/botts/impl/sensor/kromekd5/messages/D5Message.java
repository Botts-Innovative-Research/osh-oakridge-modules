package com.botts.impl.sensor.kromekd5.messages;

import com.botts.impl.utils.n42.AnalysisResultsType;
import com.botts.impl.utils.n42.EnergyCalibrationType;
import com.botts.impl.utils.n42.RadInstrumentDataType;
import com.botts.impl.utils.n42.RadMeasurementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class D5Message {

    Logger logger = LoggerFactory.getLogger(D5Message.class);

    D5EnergyCalibration d5EnergyCalibration;
    D5ForegroundMeasurement d5ForegroundMeasurement;
    D5BackgroundMeasurement d5BackgroundMeasurement;
    D5Location d5Location;
    D5AnalysisResults d5AnalysisResults;

    public D5Message(RadInstrumentDataType msg){

        msg.getRadMeasurementOrRadMeasurementGroupOrEnergyCalibration().forEach(jaxbElement -> {

            Class<?> jaxbType = jaxbElement.getDeclaredType();

            if (jaxbType == EnergyCalibrationType.class){
                EnergyCalibrationType energyCalibrationType = (EnergyCalibrationType) jaxbElement.getValue();
                d5EnergyCalibration = new D5EnergyCalibration(energyCalibrationType.getCoefficientValues());
            }
            else if (jaxbType == RadMeasurementType.class) {
                RadMeasurementType radMeasurementType = (RadMeasurementType) jaxbElement.getValue();
                switch (radMeasurementType.getMeasurementClassCode().value()) {
                    case "Background": {
                        d5BackgroundMeasurement = new D5BackgroundMeasurement(radMeasurementType.getStartDateTime().toGregorianCalendar().getTimeInMillis(), new Double(radMeasurementType.getRealTimeDuration().getSeconds() % 60), radMeasurementType.getSpectrum().get(0).getChannelData().getValue(), radMeasurementType.getGrossCounts().get(0).getCountData().get(0));
                        if (radMeasurementType.getRadInstrumentState().getStateVector().getGeographicPoint() != null) {
                            d5Location = new D5Location(radMeasurementType.getRadInstrumentState().getStateVector().getGeographicPoint().getLatitudeValue().getValue().doubleValue(), radMeasurementType.getRadInstrumentState().getStateVector().getGeographicPoint().getLongitudeValue().getValue().doubleValue());
                        }
                    }
                    break;
                    case "Foreground": {
                        d5ForegroundMeasurement = new D5ForegroundMeasurement(radMeasurementType.getStartDateTime().toGregorianCalendar().getTimeInMillis(), new Double(radMeasurementType.getRealTimeDuration().getSeconds() % 60), radMeasurementType.getSpectrum().get(0).getChannelData().getValue(), radMeasurementType.getGrossCounts().get(0).getCountData().get(0), radMeasurementType.getDoseRate().get(0).getDoseRateValue().getValue());
                    }
                    break;
                    default:
                        logger.debug("Measurement Class Code: " + radMeasurementType.getMeasurementClassCode().value());

                }
            }
            else if (jaxbType == AnalysisResultsType.class){
                AnalysisResultsType analysisResultsType = (AnalysisResultsType) jaxbElement.getValue();
                d5AnalysisResults = new D5AnalysisResults(analysisResultsType.getAnalysisResultStatusCode().value(), analysisResultsType.getNuclideAnalysisResults().getNuclide());
            }
        });
    }

    public D5EnergyCalibration getD5EnergyCalibration() {
        return d5EnergyCalibration;
    }

    public D5ForegroundMeasurement getD5ForegroundMeasurement() {
        return d5ForegroundMeasurement;
    }

    public D5BackgroundMeasurement getD5BackgroundMeasurement() {
        return d5BackgroundMeasurement;
    }

    public D5Location getD5Location() {
        return d5Location;
    }

    public D5AnalysisResults getD5AnalysisResults() {
        return d5AnalysisResults;
    }
}
