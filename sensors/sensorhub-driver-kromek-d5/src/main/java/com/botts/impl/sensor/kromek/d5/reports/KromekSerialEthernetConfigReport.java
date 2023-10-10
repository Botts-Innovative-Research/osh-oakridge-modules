package com.botts.impl.sensor.kromek.d5.reports;

import com.botts.impl.sensor.kromek.d5.message.SerialMessage;

import java.io.ByteArrayOutputStream;

import static com.botts.impl.sensor.kromek.d5.message.Constants.*;

public class KromekSerialEthernetConfigReport extends SerialMessage {
    public byte value;

    public KromekSerialEthernetConfigReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialEthernetConfigReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_ETHERNET_CONFIG_ID);
    }

    @Override
    public byte[] encodePayload() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(value);
        return stream.toByteArray();
    }

    @Override
    public void decodePayload(byte[] payload) {
        value = payload[0];
    }

    @Override
    public String toString() {
        return KromekSerialEthernetConfigReport.class.getSimpleName() + " {" +
                "value=" + value +
                '}';
    }
}
