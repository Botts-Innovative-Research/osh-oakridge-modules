package com.botts.impl.sensor.kromek.d5.reports;

import com.botts.impl.sensor.kromek.d5.enums.KromekSerialPowerSource;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import org.vast.swe.SWEHelper;

import java.util.Arrays;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD;
import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_REPORTS_IN_STATUS_ID;

public class KromekSerialStatusReport extends SerialReport {
    private int appStatus;
    private KromekSerialPowerSource power;
    private int temperature;
    private int detectorStatus0;
    private int detectorStatus1;
    private int batteryLevel;
    private int batteryChargeRate;
    private int batteryTemperature;
    private byte usbStatus;
    private byte btStatus;
    private int detectorStatus2;

    public KromekSerialStatusReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialStatusReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_STATUS_ID);
    }

    @Override
    public void decodePayload(byte[] payload) {
        appStatus = bytesToUInt(payload[0]);
        power = KromekSerialPowerSource.values()[bytesToUInt(payload[1])];
        temperature = bytesToInt(payload[2]);
        detectorStatus0 = bytesToUInt(payload[3]);
        detectorStatus1 = bytesToUInt(payload[4]);
        batteryLevel = bytesToUInt(payload[5]);
        batteryChargeRate = bytesToInt(payload[6]);
        batteryTemperature = bytesToInt(payload[7]);
        usbStatus = payload[8];
        btStatus = payload[9];
        detectorStatus2 = bytesToUInt(payload[10]);
    }

    @Override
    public String toString() {
        return KromekSerialStatusReport.class.getSimpleName() + " {" +
                "appStatus=" + appStatus +
                ", power=" + power +
                ", temperature=" + temperature +
                ", detectorStatus0=" + detectorStatus0 +
                ", detectorStatus1=" + detectorStatus1 +
                ", batteryLevel=" + batteryLevel +
                ", batteryChargeRate=" + batteryChargeRate +
                ", batteryTemperature=" + batteryTemperature +
                ", usbStatus=" + usbStatus +
                ", btStatus=" + btStatus +
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
                .addField("appStatus", sweFactory.createQuantity()
                        .label("App Status")
                        .description("See \"Device Status Error Codes\"")
                        .definition(SWEHelper.getPropertyUri("appStatus"))
                        .dataType(DataType.INT))
                .addField("power", sweFactory.createCategory()
                        .label("Power")
                        .description("Power")
                        .definition(SWEHelper.getPropertyUri("power"))
                        .addAllowedValues(Arrays.toString(KromekSerialPowerSource.values())))
                .addField("temperature", sweFactory.createQuantity()
                        .label("Temperature")
                        .description("Temperature")
                        .definition(SWEHelper.getPropertyUri("temperature"))
                        .dataType(DataType.INT)
                        .uom("C"))
                .addField("detectorStatus0", sweFactory.createQuantity()
                        .label("Detector Status 0")
                        .description("See \"Device Gamma Status Error Codes\"")
                        .definition(SWEHelper.getPropertyUri("detectorStatus0"))
                        .dataType(DataType.INT))
                .addField("detectorStatus1", sweFactory.createQuantity()
                        .label("Detector Status 1")
                        .description("See \"Device Neutron Status Error Codes\"")
                        .definition(SWEHelper.getPropertyUri("detectorStatus1"))
                        .dataType(DataType.INT))
                .addField("batteryLevel", sweFactory.createQuantity()
                        .label("Battery Level")
                        .description("Battery Level")
                        .definition(SWEHelper.getPropertyUri("batteryLevel"))
                        .dataType(DataType.INT)
                        .uom("%"))
                .addField("batteryChargeRate", sweFactory.createQuantity()
                        .label("Battery Charge Rate")
                        .description("Battery Charge Rate")
                        .definition(SWEHelper.getPropertyUri("batteryChargeRate"))
                        .dataType(DataType.INT))
                .addField("batteryTemperature", sweFactory.createQuantity()
                        .label("Battery Temperature")
                        .description("Battery Temperature")
                        .definition(SWEHelper.getPropertyUri("batteryTemperature"))
                        .dataType(DataType.INT)
                        .uom("C"))
                .addField("usbStatus", sweFactory.createText()
                        .label("USB Status")
                        .description("USB Status")
                        .definition(SWEHelper.getPropertyUri("usbStatus")))
                .addField("btStatus", sweFactory.createText()
                        .label("Bluetooth Status")
                        .description("Bluetooth Status")
                        .definition(SWEHelper.getPropertyUri("btStatus")))
                .addField("detectorStatus2", sweFactory.createQuantity()
                        .label("Detector Status 2")
                        .description("See \"Device Dose Status Error Codes\"")
                        .definition(SWEHelper.getPropertyUri("detectorStatus2"))
                        .dataType(DataType.INT))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setIntValue(++index, appStatus);
        dataBlock.setStringValue(++index, power.toString());
        dataBlock.setIntValue(++index, temperature);
        dataBlock.setIntValue(++index, detectorStatus0);
        dataBlock.setIntValue(++index, detectorStatus1);
        dataBlock.setIntValue(++index, batteryLevel);
        dataBlock.setIntValue(++index, batteryChargeRate);
        dataBlock.setIntValue(++index, batteryTemperature);
        dataBlock.setStringValue(++index, String.format("0x%02X", usbStatus));
        dataBlock.setStringValue(++index, String.format("0x%02X", btStatus));
        dataBlock.setIntValue(++index, detectorStatus2);
    }

    @Override
    void setReportInfo() {
        setReportName(KromekSerialStatusReport.class.getSimpleName());
        setReportLabel("Status");
        setReportDescription("Reports the status of the device");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
    }
}
