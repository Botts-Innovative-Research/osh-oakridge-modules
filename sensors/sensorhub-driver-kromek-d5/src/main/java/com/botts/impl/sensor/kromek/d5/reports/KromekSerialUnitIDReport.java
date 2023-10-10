package com.botts.impl.sensor.kromek.d5.reports;

import com.botts.impl.sensor.kromek.d5.message.SerialMessage;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static com.botts.impl.sensor.kromek.d5.message.Constants.*;

public class KromekSerialUnitIDReport extends SerialMessage {
    public byte[] unitID = new byte[KROMEK_SERIAL_MAX_UNIT_ID_LENGTH];

    public KromekSerialUnitIDReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialUnitIDReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_UNIT_ID_ID);
    }

    @Override
    public byte[] encodePayload() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (int i = 0; i < KROMEK_SERIAL_MAX_UNIT_ID_LENGTH; i++)
            stream.write(unitID[i]);
        return stream.toByteArray();
    }

    @Override
    public void decodePayload(byte[] payload) {
        System.arraycopy(payload, 0, unitID, 0, KROMEK_SERIAL_MAX_UNIT_ID_LENGTH);
    }

    @Override
    public String toString() {
        return KromekSerialUnitIDReport.class.getSimpleName() + " {" +
                "unitID=" + Arrays.toString(unitID) +
                '}';
    }
}
