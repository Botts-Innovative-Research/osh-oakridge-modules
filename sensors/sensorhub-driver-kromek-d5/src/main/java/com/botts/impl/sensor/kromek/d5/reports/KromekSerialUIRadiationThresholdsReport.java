package com.botts.impl.sensor.kromek.d5.reports;

import com.botts.impl.sensor.kromek.d5.enums.KromekSerialRadiationThresholdType;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import org.vast.swe.SWEHelper;

import java.util.Arrays;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD;
import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_REPORTS_IN_RADIATION_THRESHOLD_INDEXED_ID;

public class KromekSerialUIRadiationThresholdsReport extends SerialReport {
    private int index;
    private KromekSerialRadiationThresholdType thresholdType;
    private String thresholdText1;
    private String thresholdText2;
    private float thresholdValue;
    private long alertBitmap;

    public KromekSerialUIRadiationThresholdsReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialUIRadiationThresholdsReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_RADIATION_THRESHOLD_INDEXED_ID);
    }

    @Override
    public void decodePayload(byte[] payload) {
        index = bytesToUInt(payload[0], payload[1]);
        thresholdType = KromekSerialRadiationThresholdType.values()[payload[2]];

        //Read in all 34 bytes of the threshold text
        byte[] thresholdTextBytes = Arrays.copyOfRange(payload, 3, 37);
        //Convert to a string. The string is null terminated, so we need to find the null terminator
        int nullTerminatorIndex = 0;
        for (int i = 0; i < thresholdTextBytes.length; i++) {
            if (thresholdTextBytes[i] == 0) {
                nullTerminatorIndex = i;
                break;
            }
        }
        thresholdText1 = new String(Arrays.copyOfRange(thresholdTextBytes, 0, nullTerminatorIndex));

        //Read in all 34 bytes of the threshold text
        thresholdTextBytes = Arrays.copyOfRange(payload, 37, 71);
        //Convert to a string. The string is null terminated, so we need to find the null terminator
        nullTerminatorIndex = 0;
        for (int i = 0; i < thresholdTextBytes.length; i++) {
            if (thresholdTextBytes[i] == 0) {
                nullTerminatorIndex = i;
                break;
            }
        }
        thresholdText2 = new String(Arrays.copyOfRange(thresholdTextBytes, 0, nullTerminatorIndex));

        thresholdValue = bytesToFloat(payload[71], payload[72], payload[73], payload[74]);
        alertBitmap = bytesToUInt(payload[75], payload[76], payload[77], payload[78]);
    }

    @Override
    public String toString() {
        return KromekSerialUIRadiationThresholdsReport.class.getSimpleName() + " {" +
                "index=" + index +
                ", thresholdType=" + thresholdType +
                ", thresholdText1='" + thresholdText1 + '\'' +
                ", thresholdText2='" + thresholdText2 + '\'' +
                ", thresholdValue=" + thresholdValue +
                ", alertBitmap=" + alertBitmap +
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
                .addField("index", sweFactory.createQuantity()
                        .label("Index")
                        .description("Index")
                        .definition(SWEHelper.getPropertyUri("index"))
                        .dataType(DataType.INT))
                .addField("thresholdType", sweFactory.createCategory()
                        .label("Threshold Type")
                        .description("Threshold Type")
                        .definition(SWEHelper.getPropertyUri("thresholdType"))
                        .addAllowedValues(Arrays.toString(KromekSerialRadiationThresholdType.values())))
                .addField("thresholdText1", sweFactory.createText()
                        .label("Threshold Text 1")
                        .description("Threshold Text 1")
                        .definition(SWEHelper.getPropertyUri("thresholdText1")))
                .addField("thresholdText2", sweFactory.createText()
                        .label("Threshold Text 2")
                        .description("Threshold Text 2")
                        .definition(SWEHelper.getPropertyUri("thresholdText2")))
                .addField("thresholdValue", sweFactory.createQuantity()
                        .label("Threshold Value")
                        .description("Threshold Value")
                        .definition(SWEHelper.getPropertyUri("thresholdValue"))
                        .dataType(DataType.FLOAT))
                .addField("alertBitmap", sweFactory.createQuantity()
                        .label("Alert Bitmap")
                        .description("Alert Bitmap")
                        .definition(SWEHelper.getPropertyUri("alertBitmap"))
                        .dataType(DataType.INT))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setIntValue(++index, this.index);
        dataBlock.setStringValue(++index, thresholdType.toString());
        dataBlock.setStringValue(++index, thresholdText1);
        dataBlock.setStringValue(++index, thresholdText2);
        dataBlock.setFloatValue(++index, thresholdValue);
        dataBlock.setLongValue(++index, alertBitmap);
    }

    @Override
    void setReportInfo() {
        setReportName(KromekSerialUIRadiationThresholdsReport.class.getSimpleName());
        setReportLabel("Radiation Thresholds");
        setReportDescription("Radiation Thresholds");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
    }
}
