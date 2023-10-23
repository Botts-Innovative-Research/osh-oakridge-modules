package com.botts.impl.sensor.kromek.d5.reports;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import org.vast.swe.SWEHelper;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD;

public class Template extends SerialReport {
    private byte value;

    public Template(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public Template() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, (byte) 0);
    }

    @Override
    public void decodePayload(byte[] payload) {
        value = payload[0];
    }

    @Override
    public String toString() {
        return Template.class.getSimpleName() + " {" +
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
        setReportName(Template.class.getSimpleName());
        setReportLabel("Template");
        setReportDescription("Template");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
    }
}
