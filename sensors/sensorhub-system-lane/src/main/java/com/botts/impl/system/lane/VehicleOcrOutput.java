package com.botts.impl.system.lane;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.VarRateSensorOutput;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.sensorhub.impl.utils.rad.model.VehicleOcrResult;
import org.vast.data.TextEncodingImpl;

/**
 * Lane output carrying OCR'd vehicle IDs (container numbers / license plates)
 * read from camera recordings of alarming occupancies. Only registered when
 * the lane's OCR config is enabled.
 */
public class VehicleOcrOutput extends VarRateSensorOutput<LaneSystem> {

    public static final String NAME = "vehicleOcr";

    DataComponent recordStruct;
    DataEncoding recordEncoding;

    public VehicleOcrOutput(LaneSystem parentSensor) {
        super(NAME, parentSensor, 0);

        recordStruct = new RADHelper().createVehicleOcrRecord();
        recordEncoding = new TextEncodingImpl();
    }

    public void publishData(VehicleOcrResult result) {
        DataBlock dataBlock = VehicleOcrResult.fromVehicleOcrResult(result);
        eventHandler.publish(new DataEvent(result.getSampleTime().toEpochMilli(), VehicleOcrOutput.this, dataBlock));
    }

    @Override
    public DataComponent getRecordDescription() {
        return recordStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return recordEncoding;
    }
}
