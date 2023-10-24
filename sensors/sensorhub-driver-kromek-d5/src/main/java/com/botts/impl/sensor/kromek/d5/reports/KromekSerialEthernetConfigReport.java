package com.botts.impl.sensor.kromek.d5.reports;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import org.vast.swe.SWEHelper;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD;
import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_REPORTS_IN_ETHERNET_CONFIG_ID;

public class KromekSerialEthernetConfigReport extends SerialReport {
    private boolean dhcp;
    private int[] address;
    private int[] netmask;
    private int[] gateway;
    private int port;

    public KromekSerialEthernetConfigReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialEthernetConfigReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_ETHERNET_CONFIG_ID);
    }

    @Override
    public void decodePayload(byte[] payload) {
        dhcp = byteToBoolean(payload[0]);
        address = new int[4];
        address[0] = bytesToUInt(payload[1]);
        address[1] = bytesToUInt(payload[2]);
        address[2] = bytesToUInt(payload[3]);
        address[3] = bytesToUInt(payload[4]);
        netmask = new int[4];
        netmask[0] = bytesToUInt(payload[5]);
        netmask[1] = bytesToUInt(payload[6]);
        netmask[2] = bytesToUInt(payload[7]);
        netmask[3] = bytesToUInt(payload[8]);
        gateway = new int[4];
        gateway[0] = bytesToUInt(payload[9]);
        gateway[1] = bytesToUInt(payload[10]);
        gateway[2] = bytesToUInt(payload[11]);
        gateway[3] = bytesToUInt(payload[12]);
        port = bytesToUInt(payload[13], payload[14]);
    }

    @Override
    public String toString() {
        return KromekSerialEthernetConfigReport.class.getSimpleName() + " {" +
                "dhcp=" + dhcp +
                ", address=" + address[0] + "." + address[1] + "." + address[2] + "." + address[3] +
                ", netmask=" + netmask[0] + "." + netmask[1] + "." + netmask[2] + "." + netmask[3] +
                ", gateway=" + gateway[0] + "." + gateway[1] + "." + gateway[2] + "." + gateway[3] +
                ", port=" + port +
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
                .addField("dhcp", sweFactory.createBoolean()
                        .label("DHCP")
                        .description("DHCP")
                        .definition(SWEHelper.getPropertyUri("dhcp")))
                .addField("address", sweFactory.createText()
                        .label("Address")
                        .description("Address")
                        .definition(SWEHelper.getPropertyUri("address")))
                .addField("netmask", sweFactory.createText()
                        .label("Netmask")
                        .description("Netmask")
                        .definition(SWEHelper.getPropertyUri("netmask")))
                .addField("gateway", sweFactory.createText()
                        .label("Gateway")
                        .description("Gateway")
                        .definition(SWEHelper.getPropertyUri("gateway")))
                .addField("port", sweFactory.createQuantity()
                        .label("Port")
                        .description("Port")
                        .definition(SWEHelper.getPropertyUri("port"))
                        .dataType(DataType.INT))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setBooleanValue(++index, dhcp);
        dataBlock.setStringValue(++index, address[0] + "." + address[1] + "." + address[2] + "." + address[3]);
        dataBlock.setStringValue(++index, netmask[0] + "." + netmask[1] + "." + netmask[2] + "." + netmask[3]);
        dataBlock.setStringValue(++index, gateway[0] + "." + gateway[1] + "." + gateway[2] + "." + gateway[3]);
        dataBlock.setIntValue(++index, port);
    }

    @Override
    void setReportInfo() {
        setReportName(KromekSerialEthernetConfigReport.class.getSimpleName());
        setReportLabel("Ethernet Config");
        setReportDescription("Configuration for the ethernet interface.");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
        setPollingRate(30);
    }
}
