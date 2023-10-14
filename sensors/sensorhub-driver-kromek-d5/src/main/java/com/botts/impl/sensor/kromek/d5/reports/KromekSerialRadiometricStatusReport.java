package com.botts.impl.sensor.kromek.d5.reports;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import org.vast.swe.SWEHelper;

import static com.botts.impl.sensor.kromek.d5.message.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD_EXT;
import static com.botts.impl.sensor.kromek.d5.message.Constants.KROMEK_SERIAL_REPORTS_IN_RADIOMETRIC_STATUS_REPORT;

public class KromekSerialRadiometricStatusReport extends SerialReport {
    private byte value;

    public KromekSerialRadiometricStatusReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialRadiometricStatusReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD_EXT, KROMEK_SERIAL_REPORTS_IN_RADIOMETRIC_STATUS_REPORT);
    }

    @Override
    public void decodePayload(byte[] payload) {
        value = payload[0];
    }

    @Override
    public String toString() {
        return KromekSerialRadiometricStatusReport.class.getSimpleName() + " {" +
                "value=" + value +
                '}';
    }

    @Override
    public DataRecord createDataRecord() {
        return null;
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp) {

    }

    @Override
    void setReportInfo() {
        setReportName(KromekSerialRadiometricStatusReport.class.getSimpleName());
        setReportLabel("Radiometric Status");
        setReportDescription("Radiometric Status");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
    }
}
