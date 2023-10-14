package com.botts.impl.sensor.kromek.d5.reports;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import org.vast.swe.SWEHelper;

import java.util.Arrays;

import static com.botts.impl.sensor.kromek.d5.message.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD;
import static com.botts.impl.sensor.kromek.d5.message.Constants.KROMEK_SERIAL_REPORTS_IN_COMPRESSION_ENABLED_ID;

public class KromekSerialCompressionEnabledReport extends SerialReport {
    private boolean enabled;
    private int windowSize;
    private int lookaheadSize;
    private int compressionDirection;
    private byte[] reserved = new byte[2];

    public KromekSerialCompressionEnabledReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialCompressionEnabledReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_COMPRESSION_ENABLED_ID);
    }

    @Override
    public void decodePayload(byte[] payload) {
        enabled = byteToBoolean(payload[0]);
        windowSize = bytesToUInt(payload[1]);
        lookaheadSize = bytesToUInt(payload[2]);
        compressionDirection = bytesToUInt(payload[3]);
        reserved[0] = payload[4];
        reserved[1] = payload[5];
    }

    @Override
    public String toString() {
        return KromekSerialCompressionEnabledReport.class.getSimpleName() + " {" +
                "enabled=" + enabled +
                ", windowSize=" + windowSize +
                ", lookaheadSize=" + lookaheadSize +
                ", compressionDirection=" + compressionDirection +
                ", reserved=" + Arrays.toString(reserved) +
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
                .addField("enabled", sweFactory.createBoolean()
                        .label("Enabled")
                        .description("Enabled")
                        .definition(SWEHelper.getPropertyUri("enabled")))
                .addField("windowSize", sweFactory.createQuantity()
                        .label("Window Size")
                        .description("Window Size")
                        .definition(SWEHelper.getPropertyUri("windowSize"))
                        .dataType(DataType.INT))
                .addField("lookaheadSize", sweFactory.createQuantity()
                        .label("Lookahead Size")
                        .description("Lookahead Size")
                        .definition(SWEHelper.getPropertyUri("lookaheadSize"))
                        .dataType(DataType.INT))
                .addField("compressionDirection", sweFactory.createQuantity()
                        .label("Compression Direction")
                        .description("Compression Direction")
                        .definition(SWEHelper.getPropertyUri("compressionDirection"))
                        .dataType(DataType.INT))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setBooleanValue(++index, enabled);
        dataBlock.setIntValue(++index, windowSize);
        dataBlock.setIntValue(++index, lookaheadSize);
        dataBlock.setIntValue(++index, compressionDirection);
    }

    @Override
    void setReportInfo() {
        setReportName(KromekSerialCompressionEnabledReport.class.getSimpleName());
        setReportLabel("Compression Enabled");
        setReportDescription("Reports if compression is enabled and the compression parameters");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
    }
}
