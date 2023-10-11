package com.botts.impl.sensor.kromek.d5.reports;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import org.vast.swe.SWEHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.botts.impl.sensor.kromek.d5.message.Constants.*;

public abstract class SerialMessage {
    private final byte componentId;
    private final byte reportId;

    private static String reportName = "Report";
    private static String reportLabel = "Report";
    private static String reportDescription = "Report";
    private static String reportDefinition = SWEHelper.getPropertyUri(reportName);


    /**
     * Create a new message with the given componentId and reportId.
     */
    public SerialMessage(byte componentId, byte reportId) {
        this.componentId = componentId;
        this.reportId = reportId;
        setReportInfo();
    }

    public byte getComponentId() {
        return componentId;
    }

    public byte getReportId() {
        return reportId;
    }

    /**
     * Encodes the message in the following format:
     * <p>
     *     <ul>
     *         <li>Length (2 bytes)</li>
     *         <li>Mode (1 byte)</li>
     *         <li>Payload header - Component ID (1 byte)</li>
     *         <li>Payload header - Report ID (1 byte)</li>
     *         <li>Payload (variable length; not included for requests)</li>
     *         <li>CRC-16 (2 bytes)</li>
     *     </ul>
     * <p>
     * The message is encoded using SLIP framing for devices that require it; otherwise it is encoded as-is.
     * SLIP framing also adds a framing byte to the start and end of the message.
     *
     * @return The encoded message.
     */
    public byte[] encodeRequest() throws IOException {
        // Requests have no payload. The length is just the overhead.
        int length = KROMEK_SERIAL_MESSAGE_OVERHEAD + KROMEK_SERIAL_REPORTS_HEADER_OVERHEAD;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(intToBytes(length));
        outputStream.write(KROMEK_SERIAL_MESSAGE_MODE);
        outputStream.write(componentId);
        outputStream.write(reportId);

        // CRC is always 0 for requests
        int crc = 0;
        outputStream.write(intToBytes(crc));

        byte[] message = outputStream.toByteArray();
        if (KROMEK_SERIAL_REPORTS_BUILD_FOR_PRODUCT_D5)
            message = encodeSLIP(message);

        return message;
    }

    /**
     * Encode the given message using SLIP framing.
     * If the FRAME byte occurs in the message, then it is replaced with the byte sequence ESC, ESC_FRAME.
     * If the ESC byte occurs in the message, then the byte sequence ESC, ESC_ESC is sent instead.
     * The message is then framed with the FRAME byte at the end.
     */
    public static byte[] encodeSLIP(byte[] data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (byte b : data) {
            if (b == KROMEK_SERIAL_FRAMING_FRAME_BYTE) {
                outputStream.write(KROMEK_SERIAL_FRAMING_ESC_BYTE);
                outputStream.write(KROMEK_SERIAL_FRAMING_ESC_FRAME_BYTE);
            } else if (b == KROMEK_SERIAL_FRAMING_ESC_BYTE) {
                outputStream.write(KROMEK_SERIAL_FRAMING_ESC_BYTE);
                outputStream.write(KROMEK_SERIAL_FRAMING_ESC_ESC_BYTE);
            } else {
                outputStream.write(b);
            }
        }
        outputStream.write(KROMEK_SERIAL_FRAMING_FRAME_BYTE);

        return outputStream.toByteArray();
    }

    /**
     * Decode the given message using SLIP framing.
     * Parses the received bytes until it detects a framing byte.
     * It parses all bytes since the last framing byte.
     * If it finds any ESC bytes it replaces the escaped byte sequences with the original bytes.
     */
    public static byte[] decodeSLIP(byte[] input) {
        List<Byte> output = new ArrayList<>();
        boolean escaped = false;
        for (byte b : input) {
            if (escaped) {
                if (b == KROMEK_SERIAL_FRAMING_ESC_FRAME_BYTE) {
                    output.add(KROMEK_SERIAL_FRAMING_FRAME_BYTE);
                } else if (b == KROMEK_SERIAL_FRAMING_ESC_ESC_BYTE) {
                    output.add(KROMEK_SERIAL_FRAMING_ESC_BYTE);
                }
                escaped = false;
            } else if (b == KROMEK_SERIAL_FRAMING_ESC_BYTE) {
                escaped = true;
            } else {
                output.add(b);
            }
        }

        byte[] result = new byte[output.size()];
        for (int i = 0; i < output.size(); i++) {
            result[i] = output.get(i);
        }
        return result;
    }

    /**
     * Calculate Cyclic Redundancy Checksum 16 (CRC-16) for the given byte array.
     * Copied from Kromek documentation. I don't think it's used since the CRC is always 0 for requests, and
     * requests are the only thing we send.
     */
    public static int crc16(final byte[] buffer) {
        int crc = KROMEK_SERIAL_MESSAGE_CRC_INITIAL_VALUE;
        for (byte b : buffer) {
            crc = ((crc >>> 8) | (crc << 8)) & 0xffff;
            crc ^= (b & 0xff); //byte to int, truncate sign
            crc ^= ((crc & 0xff) >> 4);
            crc ^= (crc << 12) & 0xffff;
            crc ^= ((crc & 0xFF) << 5) & 0xffff;
        }
        crc &= 0xffff;
        return crc;
    }

    /**
     * Converts an int into two bytes.
     *
     * @param value The int to convert.
     * @return The two bytes.
     */
    public byte[] intToBytes(int value) {
        byte[] bytes = new byte[2];
        // The & 0xFF masks all but the lowest eight bits.
        bytes[0] = (byte) (value & 0xFF);
        // The >> 8 discards the lowest eight bits.
        bytes[1] = (byte) ((value >> 8) & 0xFF);
        return bytes;
    }

    /**
     * Converts bytes representing an unsigned int into an int.
     *
     * @param bytes The bytes to convert.
     * @return The int.
     */
    public static int bytesToUInt(byte... bytes) {
        int result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result += (bytes[i] & 0xFF) << (8 * i);
        }
        return result;
    }

    /**
     * Converts bytes representing a signed int into an int.
     *
     * @param bytes The bytes to convert.
     * @return The int.
     */
    public static int bytesToInt(byte... bytes) {
        int result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result += bytes[i] << (8 * i);
        }
        return result;
    }

    /**
     * Converts a byte representing a boolean into a boolean.
     *
     * @param value The byte to convert.
     * @return The boolean.
     */
    public static boolean byteToBoolean(byte value) {
        return value != 0;
    }


    public static String getReportName() {
        return reportName;
    }

    void setReportName(String reportName) {
        SerialMessage.reportName = reportName;
    }

    public static String getReportLabel() {
        return reportLabel;
    }

    void setReportLabel(String reportLabel) {
        SerialMessage.reportLabel = reportLabel;
    }

    public static String getReportDescription() {
        return reportDescription;
    }

    void setReportDescription(String reportDescription) {
        SerialMessage.reportDescription = reportDescription;
    }

    public static String getReportDefinition() {
        return reportDefinition;
    }

    void setReportDefinition(String reportDefinition) {
        SerialMessage.reportDefinition = reportDefinition;
    }

    public abstract void decodePayload(byte[] payload);

    public abstract String toString();

    public abstract DataRecord createDataRecord();

    public abstract void setDataBlock(DataBlock dataBlock, double timestamp);

    /**
     * Called by the constructor to set the report info.
     * Implementations should set the report name, label, description, and definition.
     */
    abstract void setReportInfo();
}
