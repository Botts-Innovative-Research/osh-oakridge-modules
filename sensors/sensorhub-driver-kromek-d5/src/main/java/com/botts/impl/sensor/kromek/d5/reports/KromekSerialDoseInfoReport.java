package com.botts.impl.sensor.kromek.d5.reports;

import com.botts.impl.sensor.kromek.d5.enums.KromekSerialDosemeterSource;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import org.vast.swe.SWEHelper;

import java.util.Arrays;

import static com.botts.impl.sensor.kromek.d5.message.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD;
import static com.botts.impl.sensor.kromek.d5.message.Constants.KROMEK_SERIAL_REPORTS_IN_DOSE_INFO_ID;

public class KromekSerialDoseInfoReport extends SerialReport {
    private float lifetimeDose;
    private float powerUpDose;
    private float userDose;
    private float doseRate;
    private long reserved0;
    private KromekSerialDosemeterSource selectedDoseDetector;

    public KromekSerialDoseInfoReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialDoseInfoReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_DOSE_INFO_ID);
    }

    @Override
    public void decodePayload(byte[] payload) {
        lifetimeDose = bytesToFloat(payload[0], payload[1], payload[2], payload[3]);
        powerUpDose = bytesToFloat(payload[4], payload[5], payload[6], payload[7]);
        userDose = bytesToFloat(payload[8], payload[9], payload[10], payload[11]);
        doseRate = bytesToFloat(payload[12], payload[13], payload[14], payload[15]);
        reserved0 = bytesToUInt(payload[16], payload[17], payload[18], payload[19]);
        selectedDoseDetector = KromekSerialDosemeterSource.values()[bytesToUInt(payload[20])];
    }

    @Override
    public String toString() {
        return KromekSerialDoseInfoReport.class.getSimpleName() + " {" +
                "lifetimeDose=" + lifetimeDose +
                ", powerUpDose=" + powerUpDose +
                ", userDose=" + userDose +
                ", doseRate=" + doseRate +
                ", reserved0=" + reserved0 +
                ", selectedDoseDetector=" + selectedDoseDetector +
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
                .addField("lifetimeDose", sweFactory.createQuantity()
                        .label("Lifetime Dose")
                        .description("Accumulated uSV since manufacture")
                        .definition(SWEHelper.getPropertyUri("lifetimeDose"))
                        .uom("uSv"))
                .addField("powerUpDose", sweFactory.createQuantity()
                        .label("Power Up Dose")
                        .description("Accumulated dose since last power up")
                        .definition(SWEHelper.getPropertyUri("powerUpDose"))
                        .uom("uSv"))
                .addField("userDose", sweFactory.createQuantity()
                        .label("User Dose")
                        .description("Accumulated dose since last reset command")
                        .definition(SWEHelper.getPropertyUri("userDose"))
                        .uom("uSv"))
                .addField("doseRate", sweFactory.createQuantity()
                        .label("Dose Rate")
                        .description("uSv per hour (typically averaged over 10 secs)")
                        .definition(SWEHelper.getPropertyUri("doseRate"))
                        .uom("uSv/h"))
                .addField("selectedDoseDetector", sweFactory.createCategory()
                        .label("Selected Dose Detector")
                        .description("Selected Dose Detector")
                        .definition(SWEHelper.getPropertyUri("selectedDoseDetector"))
                        .addAllowedValues(Arrays.toString(KromekSerialDosemeterSource.values())))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setDoubleValue(++index, lifetimeDose);
        dataBlock.setDoubleValue(++index, powerUpDose);
        dataBlock.setDoubleValue(++index, userDose);
        dataBlock.setDoubleValue(++index, doseRate);
        dataBlock.setStringValue(++index, selectedDoseDetector.toString());
    }

    @Override
    void setReportInfo() {
        setReportName(KromekSerialDoseInfoReport.class.getSimpleName());
        setReportLabel("Dose Info");
        setReportDescription("Kromek Serial Dose Info Report");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
    }
}
