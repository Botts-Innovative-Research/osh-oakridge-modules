package com.botts.impl.sensor.kromek.d5.reports;

import com.botts.impl.sensor.kromek.d5.message.SerialMessage;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static com.botts.impl.sensor.kromek.d5.message.Constants.*;

public class KromekSerialCompressionEnabledReport extends SerialMessage {
//    uint8_t                               enabled;                                // Disabled:0x00, Enabled:0x01
//    uint8_t                               windowSize;                             // Default: 0x09
//    uint8_t                               lookaheadSize;                          // Default: 0x08
//    uint8_t                               compressionDirection;                   // Always: 0x00
//    uint8_t                               reserved[2];                            // Reserved for future use
    public byte enabled;
    public byte windowSize;
    public byte lookaheadSize;
    public byte compressionDirection;
    public byte[] reserved = new byte[2];

    public KromekSerialCompressionEnabledReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialCompressionEnabledReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_COMPRESSION_ENABLED_ID);
    }

    @Override
    public byte[] encodePayload() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(enabled);
        stream.write(windowSize);
        stream.write(lookaheadSize);
        stream.write(compressionDirection);
        stream.write(reserved[0]);
        stream.write(reserved[1]);
        return stream.toByteArray();
    }

    @Override
    public void decodePayload(byte[] payload) {
        enabled = payload[0];
        windowSize = payload[1];
        lookaheadSize = payload[2];
        compressionDirection = payload[3];
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
}
