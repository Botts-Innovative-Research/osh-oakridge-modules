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
import net.opengis.swe.v20.DataType;
import org.vast.swe.SWEHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.*;

public class KromekDetectorRadiometricsV1Report extends SerialReport {
    private long status;
    private long realTimeMs;
    private long sequenceNumber;
    private float dose;
    private float doseRate;
    private float doseUserAccumulated;
    private long neutronLiveTime;
    private long neutronCounts;
    private float neutronTemperature;
    private float neutronReserved;
    private long gammaLiveTime;
    private long gammaCounts;
    private float gammaTemperature;
    private float gammaReserved;
    private int spectrumBitsSize;
    private byte spectrumReserved;
    private final int[] spectrumBins = new int[KROMEK_SERIAL_REPORTS_IN_SPECTRUM_MAX_BINS];

    public KromekDetectorRadiometricsV1Report(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekDetectorRadiometricsV1Report() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_RADIOMETRICS_V1_ID);
    }

    @Override
    public void decodePayload(byte[] payload) {
        status = bytesToUInt(payload[0], payload[1], payload[2], payload[3]);
        realTimeMs = bytesToUInt(payload[4], payload[5], payload[6], payload[7]);
        sequenceNumber = bytesToUInt(payload[8], payload[9], payload[10], payload[11]);
        dose = bytesToFloat(payload[12], payload[13], payload[14], payload[15]);
        doseRate = bytesToFloat(payload[16], payload[17], payload[18], payload[19]);
        // Convert dose rate from Sv/h to uSv/h
        doseRate = doseRate * 1000000;
        doseUserAccumulated = bytesToFloat(payload[20], payload[21], payload[22], payload[23]);
        // Convert dose user accumulated from Sv to uSv
        doseUserAccumulated = doseUserAccumulated * 1000000;
        neutronLiveTime = bytesToUInt(payload[24], payload[25], payload[26], payload[27]);
        neutronCounts = bytesToUInt(payload[28], payload[29], payload[30], payload[31]);
        neutronTemperature = (float) bytesToInt(payload[32], payload[33]) / 100;
        neutronReserved = bytesToFloat(payload[34], payload[35], payload[36], payload[37]);
        gammaLiveTime = bytesToUInt(payload[38], payload[39], payload[40], payload[41]);
        gammaCounts = bytesToUInt(payload[42], payload[43], payload[44], payload[45]);
        gammaTemperature = (float) bytesToInt(payload[46], payload[47]) / 100;
        gammaReserved = bytesToFloat(payload[48], payload[49], payload[50], payload[51]);
        spectrumBitsSize = bytesToUInt(payload[52]);
        spectrumReserved = payload[53];

        byte[] data = Arrays.copyOfRange(payload, 54, payload.length);

        // Create a ByteBuffer for easier conversion from bytes to integers
        ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        // Convert bytes to integers
        for (int i = 0; i < 4096; i++) {
            spectrumBins[i] = byteBuffer.getShort() & 0xFFFF; // Get as short, then convert to int to handle as unsigned
        }
    }

    @Override
    public String toString() {
        return KromekDetectorRadiometricsV1Report.class.getSimpleName() + " {" +
                "status=" + status +
                ", realTimeMs=" + realTimeMs +
                ", sequenceNumber=" + sequenceNumber +
                ", dose=" + dose +
                ", doseRate=" + doseRate +
                ", doseUserAccumulated=" + doseUserAccumulated +
                ", neutronLiveTime=" + neutronLiveTime +
                ", neutronCounts=" + neutronCounts +
                ", neutronTemperature=" + neutronTemperature +
                ", neutronReserved=" + neutronReserved +
                ", gammaLiveTime=" + gammaLiveTime +
                ", gammaCounts=" + gammaCounts +
                ", gammaTemperature=" + gammaTemperature +
                ", gammaReserved=" + gammaReserved +
                ", spectrumBitsSize=" + spectrumBitsSize +
                ", spectrumReserved=" + spectrumReserved +
                ", spectrumBins=" + Arrays.toString(spectrumBins) +
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
                .addField("status", sweFactory.createQuantity()
                        .label("Status")
                        .description("Status")
                        .definition(SWEHelper.getPropertyUri("status"))
                        .dataType(DataType.INT))
                .addField("realTimeMs", sweFactory.createQuantity()
                        .label("Real Time Ms")
                        .description("Real Time Ms")
                        .definition(SWEHelper.getPropertyUri("realTimeMs"))
                        .uom("ms")
                        .dataType(DataType.INT))
                .addField("sequenceNumber", sweFactory.createQuantity()
                        .label("Sequence Number")
                        .description("Sequence Number")
                        .definition(SWEHelper.getPropertyUri("sequenceNumber"))
                        .dataType(DataType.INT))
                .addField("dose", sweFactory.createQuantity()
                        .label("Dose")
                        .description("Dose")
                        .definition(SWEHelper.getPropertyUri("dose"))
                        .uom("uSv"))
                .addField("doseRate", sweFactory.createQuantity()
                        .label("Dose Rate")
                        .description("Dose Rate")
                        .definition(SWEHelper.getPropertyUri("doseRate"))
                        .uom("uSv/h"))
                .addField("doseUserAccumulated", sweFactory.createQuantity()
                        .label("Dose User Accumulated")
                        .description("Dose User Accumulated")
                        .definition(SWEHelper.getPropertyUri("doseUserAccumulated"))
                        .uom("uSv"))
                .addField("neutronLiveTime", sweFactory.createQuantity()
                        .label("Neutron Live Time")
                        .description("Neutron Live Time")
                        .definition(SWEHelper.getPropertyUri("neutronLiveTime"))
                        .dataType(DataType.INT))
                .addField("neutronCounts", sweFactory.createQuantity()
                        .label("Neutron Counts")
                        .description("Neutron Counts")
                        .definition(SWEHelper.getPropertyUri("neutronCounts"))
                        .dataType(DataType.INT))
                .addField("neutronTemperature", sweFactory.createQuantity()
                        .label("Neutron Temperature")
                        .description("Neutron Temperature")
                        .definition(SWEHelper.getPropertyUri("neutronTemperature"))
                        .uom("C"))
                .addField("gammaLiveTime", sweFactory.createQuantity()
                        .label("Gamma Live Time")
                        .description("Gamma Live Time")
                        .definition(SWEHelper.getPropertyUri("gammaLiveTime"))
                        .dataType(DataType.INT))
                .addField("gammaCounts", sweFactory.createQuantity()
                        .label("Gamma Counts")
                        .description("Gamma Counts")
                        .definition(SWEHelper.getPropertyUri("gammaCounts"))
                        .dataType(DataType.INT))
                .addField("gammaTemperature", sweFactory.createQuantity()
                        .label("Gamma Temperature")
                        .description("Gamma Temperature")
                        .definition(SWEHelper.getPropertyUri("gammaTemperature"))
                        .uom("C"))
                .addField("spectrumBitsSize", sweFactory.createQuantity()
                        .label("Spectrum Bits Size")
                        .description("Size of spectrum binning in bits. Default 12-bits (4096 channels)")
                        .definition(SWEHelper.getPropertyUri("spectrumBitsSize"))
                        .dataType(DataType.INT))
                .addField("spectrumBins", sweFactory.createArray()
                        .label("Spectrum Bins")
                        .description("Spectrum Bins")
                        .definition(SWEHelper.getPropertyUri("spectrumBins"))
                        .withFixedSize(KROMEK_SERIAL_REPORTS_IN_SPECTRUM_MAX_BINS)
                        .withElement("spectrumBin", sweFactory.createQuantity()
                                .label("Spectrum Bin")
                                .description("Spectrum Bin")
                                .definition(SWEHelper.getPropertyUri("spectrumBin"))))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setLongValue(++index, status);
        dataBlock.setLongValue(++index, realTimeMs);
        dataBlock.setLongValue(++index, sequenceNumber);
        dataBlock.setDoubleValue(++index, dose);
        dataBlock.setDoubleValue(++index, doseRate);
        dataBlock.setDoubleValue(++index, doseUserAccumulated);
        dataBlock.setLongValue(++index, neutronLiveTime);
        dataBlock.setLongValue(++index, neutronCounts);
        dataBlock.setDoubleValue(++index, neutronTemperature);
        dataBlock.setLongValue(++index, gammaLiveTime);
        dataBlock.setLongValue(++index, gammaCounts);
        dataBlock.setDoubleValue(++index, gammaTemperature);
        dataBlock.setIntValue(++index, spectrumBitsSize);
        for (int i = 0; i < KROMEK_SERIAL_REPORTS_IN_SPECTRUM_MAX_BINS; i++) {
            dataBlock.setIntValue(++index, spectrumBins[i]);
        }
    }

    @Override
    void setReportInfo() {
        setReportName("KromekDetectorRadiometricsV1Report");
        setReportLabel("Radiometrics V1 Report");
        setReportDescription("Radiometrics V1 Report");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
        setPollingRate(1);
    }
}
