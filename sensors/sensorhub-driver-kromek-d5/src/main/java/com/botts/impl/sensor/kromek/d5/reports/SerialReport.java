package com.botts.impl.sensor.kromek.d5.reports;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import org.vast.swe.SWEHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static com.botts.impl.sensor.kromek.d5.message.Constants.*;

public abstract class SerialReport {
    private final byte componentId;
    private final byte reportId;

    private static String reportName = "Report";
    private static String reportLabel = "Report";
    private static String reportDescription = "Report";
    private static String reportDefinition = SWEHelper.getPropertyUri(reportName);


    /**
     * Create a new message with the given componentId and reportId.
     */
    public SerialReport(byte componentId, byte reportId) {
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
        // Write the length as uint16_t
        outputStream.write((byte) (length & 0xFF));
        outputStream.write((byte) (0));
        outputStream.write(KROMEK_SERIAL_MESSAGE_MODE);
        outputStream.write(componentId);
        outputStream.write(reportId);

        // CRC is always 0 for requests. Write two 0 bytes.
        outputStream.write((byte) 0);
        outputStream.write((byte) 0);

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

        outputStream.write(KROMEK_SERIAL_FRAMING_FRAME_BYTE);
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
     * If it finds any ESC bytes it replaces the escaped byte sequences with the original bytes.
     */
    public static byte[] decodeSLIP(byte[] input) {
        List<Byte> output = new ArrayList<>();
        for (int i = 0; i < input.length; ) {
            byte b = input[i];
            if (b == KROMEK_SERIAL_FRAMING_ESC_BYTE && i < input.length - 1) {
                byte nextByte = input[i + 1];
                if (nextByte == KROMEK_SERIAL_FRAMING_ESC_FRAME_BYTE) {
                    output.add(KROMEK_SERIAL_FRAMING_FRAME_BYTE);
                } else if (nextByte == KROMEK_SERIAL_FRAMING_ESC_ESC_BYTE) {
                    output.add(KROMEK_SERIAL_FRAMING_ESC_BYTE);
                } else {
                    throw new RuntimeException("Invalid SLIP escape sequence: " + nextByte);
                }
                i += 2;
            } else {
                output.add(b);
                i++;
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
     * Converts bytes representing a float into a float.
     *
     * @param bytes The bytes to convert.
     * @return The float.
     */
    public static float bytesToFloat(byte... bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    /**
     * Converts a byte representing uint8_t into a short.
     *
     * @return The short.
     */
    public static short bytesToUInt(byte byte1) {
        return (short) (byte1 & 0xFF);
    }

    /**
     * Converts bytes representing uint16_t into an int
     *
     * @return The int.
     */
    public static int bytesToUInt(byte byte1, byte byte2) {
        return ByteBuffer.wrap(new byte[]{byte1, byte2}).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
    }

    /**
     * Converts bytes representing uint32_t into a long.
     *
     * @return The long.
     */
    public static long bytesToUInt(byte byte1, byte byte2, byte byte3, byte byte4) {
        return ByteBuffer.wrap(new byte[]{byte1, byte2, byte3, byte4}).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
    }

    /**
     * Converts a byte representing int8_t into a byte.
     *
     * @return The short.
     */
    public static short bytesToInt(byte byte1) {
        return byte1;
    }

    /**
     * Converts bytes representing int16_t into an int
     *
     * @return The int.
     */
    public static int bytesToInt(byte byte1, byte byte2) {
        return ByteBuffer.wrap(new byte[]{byte1, byte2}).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    /**
     * Converts bytes representing int32_t into an int.
     *
     * @return The int.
     */
    public static int bytesToInt(byte byte1, byte byte2, byte byte3, byte byte4) {
        return ByteBuffer.wrap(new byte[]{byte1, byte2, byte3, byte4}).order(ByteOrder.LITTLE_ENDIAN).getInt();
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
        SerialReport.reportName = reportName;
    }

    public static String getReportLabel() {
        return reportLabel;
    }

    void setReportLabel(String reportLabel) {
        SerialReport.reportLabel = reportLabel;
    }

    public static String getReportDescription() {
        return reportDescription;
    }

    void setReportDescription(String reportDescription) {
        SerialReport.reportDescription = reportDescription;
    }

    public static String getReportDefinition() {
        return reportDefinition;
    }

    void setReportDefinition(String reportDefinition) {
        SerialReport.reportDefinition = reportDefinition;
    }

    public abstract void decodePayload(byte[] payload);

    public abstract String toString();

    public abstract DataRecord createDataRecord();

    public abstract void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp);

    /**
     * Called by the constructor to set the report info.
     * Implementations should set the report name, label, description, and definition.
     */
    abstract void setReportInfo();
}
