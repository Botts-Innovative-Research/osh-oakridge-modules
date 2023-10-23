package com.botts.impl.sensor.kromek.d5.reports;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import org.vast.swe.SWEHelper;

import java.util.Arrays;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.*;

public class KromekSerialUnitIDReport extends SerialReport {
    private final int[] unitID = new int[KROMEK_SERIAL_MAX_UNIT_ID_LENGTH];

    public KromekSerialUnitIDReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialUnitIDReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_UNIT_ID_ID);
    }

    @Override
    public void decodePayload(byte[] payload) {
        for (int i = 0; i < KROMEK_SERIAL_MAX_UNIT_ID_LENGTH; i++)
            unitID[i] = bytesToUInt(payload[i]);
    }

    @Override
    public String toString() {
        return KromekSerialUnitIDReport.class.getSimpleName() + " {" +
                "unitID=" + Arrays.toString(unitID) +
                '}';
    }

    @Override
    public DataRecord createDataRecord() {
        SWEHelper sweFactory = new SWEHelper();
        return sweFactory.createRecord()
                .name(getReportName())
                .label(getReportLabel())
                .description(getReportDescription())
                .definition(getReportDefinition())
                .addField("timestamp", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Precision Time Stamp"))
                .addField("unitID", sweFactory.createArray()
                        .label("Unit ID")
                        .description("Unit ID")
                        .definition(SWEHelper.getPropertyUri("unitID"))
                        .withFixedSize(KROMEK_SERIAL_MAX_UNIT_ID_LENGTH)
                        .withElement("unitID", sweFactory.createQuantity()
                                .label("Unit ID")
                                .description("Unit ID")
                                .definition(SWEHelper.getPropertyUri("unitID"))
                                .dataType(DataType.INT)))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        for (int i = 0; i < KROMEK_SERIAL_MAX_UNIT_ID_LENGTH; i++)
            dataBlock.setIntValue(++index, unitID[i]);
    }

    @Override
    void setReportInfo() {
        setReportName(KromekSerialUnitIDReport.class.getSimpleName());
        setReportLabel("Unit ID");
        setReportDescription("Unit ID");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));

    }
}
