package com.botts.impl.sensor.kromek.d5.reports;

import com.botts.impl.sensor.kromek.d5.enums.KromekSerialRemoteModeState;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import org.vast.swe.SWEHelper;

import java.util.Arrays;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD;
import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_REPORTS_IN_REMOTE_BACKGROUND_COLLECTION_STATUS_ID;

public class KromekSerialRemoteBackgroundStatusReport extends SerialReport {
    private KromekSerialRemoteModeState state;
    private long totalGammaCounts;
    private long totalNeutronCounts;
    private float latitude;
    private float longitude;
    private long deviceTimestamp;

    public KromekSerialRemoteBackgroundStatusReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialRemoteBackgroundStatusReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_REMOTE_BACKGROUND_COLLECTION_STATUS_ID);
    }

    @Override
    public void decodePayload(byte[] payload) {
        state = KromekSerialRemoteModeState.values()[payload[0]];
        totalGammaCounts = bytesToUInt(payload[1], payload[2], payload[3], payload[4]);
        totalNeutronCounts = bytesToUInt(payload[5], payload[6], payload[7], payload[8]);
        latitude = bytesToFloat(payload[9], payload[10], payload[11], payload[12]);
        longitude = bytesToFloat(payload[13], payload[14], payload[15], payload[16]);
        deviceTimestamp = bytesToUInt(payload[17], payload[18], payload[19], payload[20]);
    }

    @Override
    public String toString() {
        return KromekSerialRemoteBackgroundStatusReport.class.getSimpleName() + " {" +
                "state=" + state +
                ", totalGammaCounts=" + totalGammaCounts +
                ", totalNeutronCounts=" + totalNeutronCounts +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", deviceTimestamp=" + deviceTimestamp +
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
                .addField("state", sweFactory.createCategory()
                        .label("State")
                        .description("State")
                        .definition(SWEHelper.getPropertyUri("state"))
                        .addAllowedValues(Arrays.toString(KromekSerialRemoteModeState.values())))
                .addField("totalGammaCounts", sweFactory.createQuantity()
                        .label("Total Gamma Counts")
                        .description("Total Gamma Counts")
                        .definition(SWEHelper.getPropertyUri("totalGammaCounts")))
                .addField("totalNeutronCounts", sweFactory.createQuantity()
                        .label("Total Neutron Counts")
                        .description("Total Neutron Counts")
                        .definition(SWEHelper.getPropertyUri("totalNeutronCounts")))
                .addField("latitude", sweFactory.createQuantity()
                        .label("Latitude")
                        .description("Latitude")
                        .definition(SWEHelper.getPropertyUri("latitude"))
                        .uom("deg"))
                .addField("longitude", sweFactory.createQuantity()
                        .label("Longitude")
                        .description("Longitude")
                        .definition(SWEHelper.getPropertyUri("longitude"))
                        .uom("deg"))
                .addField("deviceTimestamp", sweFactory.createQuantity()
                        .label("Device Timestamp")
                        .description("Device Timestamp")
                        .definition(SWEHelper.getPropertyUri("deviceTimestamp"))
                        .uom("s"))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setStringValue(++index, state.toString());
        dataBlock.setLongValue(++index, totalGammaCounts);
        dataBlock.setLongValue(++index, totalNeutronCounts);
        dataBlock.setDoubleValue(++index, latitude);
        dataBlock.setDoubleValue(++index, longitude);
        dataBlock.setLongValue(++index, deviceTimestamp);
    }

    @Override
    void setReportInfo() {
        setReportName(KromekSerialRemoteBackgroundStatusReport.class.getSimpleName());
        setReportLabel("Remote Background Status");
        setReportDescription("Remote Background Status");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
    }
}
