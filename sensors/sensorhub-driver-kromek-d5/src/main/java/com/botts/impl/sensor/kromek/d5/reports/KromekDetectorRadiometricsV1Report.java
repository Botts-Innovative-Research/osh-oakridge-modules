package com.botts.impl.sensor.kromek.d5.reports;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import org.vast.swe.SWEHelper;

import static com.botts.impl.sensor.kromek.d5.message.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD;
import static com.botts.impl.sensor.kromek.d5.message.Constants.KROMEK_SERIAL_REPORTS_IN_RADIOMETRICS_V1_ID;

public class KromekDetectorRadiometricsV1Report extends SerialMessage {
    public byte value;

    public KromekDetectorRadiometricsV1Report(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekDetectorRadiometricsV1Report() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_RADIOMETRICS_V1_ID);
    }

    @Override
    public void decodePayload(byte[] payload) {
        value = payload[0];
    }

    @Override
    public String toString() {
        return KromekDetectorRadiometricsV1Report.class.getSimpleName() + " {" +
                "value=" + value +
                '}';
    }

    @Override
    public DataRecord createDataRecord() {
        return null;
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, double timestamp) {

    }

    @Override
    void setReportInfo() {
        setReportName("KromekDetectorRadiometricsV1Report");
        setReportLabel("Radiometrics V1 Report");
        setReportDescription("Radiometrics V1 Report");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
    }
}
