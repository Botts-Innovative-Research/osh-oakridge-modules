/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one
 * at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * Copyright (c) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 */

package com.botts.impl.sensor.kromek.d5.reports;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import org.vast.swe.SWEHelper;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.botts.impl.sensor.kromek.d5.Shared.encodeSLIP;
import static com.botts.impl.sensor.kromek.d5.reports.Constants.*;

public abstract class SerialReport {
    private final byte componentId;
    private final byte reportId;

    private static String reportName = "Report";
    private static String reportLabel = "Report";
    private static String reportDescription = "Report";
    private static String reportDefinition = SWEHelper.getPropertyUri(reportName);
    private static final int overheadLength = KROMEK_SERIAL_MESSAGE_OVERHEAD + KROMEK_SERIAL_REPORTS_HEADER_OVERHEAD;
    private int pollingRate = 1;

    /**
     * Create a new message with the given componentId and reportId.
     */
    public SerialReport(byte componentId, byte reportId) {
        this.componentId = componentId;
        this.reportId = reportId;
        setReportInfo();
    }

    /**
     * Get the component ID for the message.
     */
    public byte getComponentId() {
        return componentId;
    }

    /**
     * Get the report ID for the message.
     */
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
    public byte[] encodeRequest() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Write the length as uint16_t
        outputStream.write((byte) (overheadLength & 0xFF));
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
     * Converts bytes representing int16_t or int32_t into an int
     *
     * @param bytes The bytes to convert. Must be two or four bytes.
     * @return The int.
     */
    public static int bytesToInt(byte... bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (bytes.length == 2) {
            return buffer.getShort();
        } else if (bytes.length == 4) {
            return buffer.getInt();
        } else {
            throw new IllegalArgumentException("Invalid number of bytes for int: " + bytes.length + ". Expected 2 or 4.");
        }
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

    /**
     * Get the name for the report.
     * This is the internal name of the report.
     *
     * @return The name of the report.
     */
    public static String getReportName() {
        return reportName;
    }

    /**
     * Set the name for the report.
     * This is the internal name of the report.
     *
     * @param reportName The name of the report.
     */
    void setReportName(String reportName) {
        SerialReport.reportName = reportName;
    }

    /**
     * Get the label for the report.
     * This is the human-readable name of the report.
     *
     * @return The label for the report.
     */
    public static String getReportLabel() {
        return reportLabel;
    }

    /**
     * Set the label for the report.
     * This is the human-readable name of the report.
     *
     * @param reportLabel The label for the report.
     */
    void setReportLabel(String reportLabel) {
        SerialReport.reportLabel = reportLabel;
    }

    /**
     * Get the description for the report.
     * This is the human-readable description of the report.
     *
     * @return The description for the report.
     */
    public static String getReportDescription() {
        return reportDescription;
    }

    /**
     * Set the description for the report.
     * This is the human-readable description of the report.
     *
     * @param reportDescription The description for the report.
     */
    void setReportDescription(String reportDescription) {
        SerialReport.reportDescription = reportDescription;
    }

    /**
     * Get the definition for the report.
     * This is the URI for the SWE definition of the report.
     *
     * @return The definition for the report.
     */
    public static String getReportDefinition() {
        return reportDefinition;
    }

    /**
     * Set the definition for the report.
     * This is the URI for the SWE definition of the report.
     *
     * @param reportDefinition The definition for the report.
     */
    void setReportDefinition(String reportDefinition) {
        SerialReport.reportDefinition = reportDefinition;
    }

    /**
     * Get the polling rate for the report.
     * This is the number of seconds between requests for reports.
     * The default is 1 second.
     * Zero means a single request will be sent at startup.
     *
     * @return The polling rate for the report.
     */
    public int getPollingRate() {
        return pollingRate;
    }

    /**
     * Set the polling rate for the report.
     * This is the number of seconds between requests for reports.
     * The default is 1 second, and negative values are clamped to 1.
     * Zero means a single request will be sent at startup.
     *
     * @param pollingRate The polling rate for the report.
     */
    public void setPollingRate(int pollingRate) {
        if (pollingRate < 0) pollingRate = 1;
        this.pollingRate = pollingRate;
    }

    /**
     * Decode the payload of the message.
     * This is typically called by the constructor.
     *
     * @param payload The payload of the message.
     */
    public abstract void decodePayload(byte[] payload);

    /**
     * Get a string representation of the message.
     *
     * @return A string representation of the message.
     */
    public abstract String toString();

    /**
     * Create a data record for the message.
     * This is used to create the DataRecord for use in outputs.
     *
     * @return The data record for the message.
     */
    public abstract DataRecord createDataRecord();

    /**
     * Set the data block for the message.
     * This is used to set the values in the DataBlock for use in outputs.
     *
     * @param dataBlock The data block to set.
     */
    public abstract void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp);

    /**
     * Called by the constructor to set the report info.
     * Implementations should set the report name, label, description, definition, and (optionally) polling rate.
     *
     * @see #setReportName(String)
     * @see #setReportLabel(String)
     * @see #setReportDescription(String)
     * @see #setReportDefinition(String)
     * @see #setPollingRate(int)
     */
    abstract void setReportInfo();
}
