package com.botts.impl.sensor.kromek.d5.reports;

import com.botts.impl.sensor.kromek.d5.message.SerialMessage;

import java.io.ByteArrayOutputStream;

import static com.botts.impl.sensor.kromek.d5.message.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD;
import static com.botts.impl.sensor.kromek.d5.message.Constants.KROMEK_SERIAL_REPORTS_IN_COARSE_GAIN_ID;

public class Template extends SerialMessage {
    public byte value;

    public Template(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public Template() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_COARSE_GAIN_ID);
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
        return Template.class.getSimpleName() + " {" +
                "value=" + value +
                '}';
    }
}
