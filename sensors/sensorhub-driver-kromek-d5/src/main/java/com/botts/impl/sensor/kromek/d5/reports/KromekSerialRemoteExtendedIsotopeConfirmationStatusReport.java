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

import com.botts.impl.sensor.kromek.d5.enums.KromekSerialRemoteControlMode;
import com.botts.impl.sensor.kromek.d5.enums.KromekSerialRemoteModeState;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import org.vast.swe.SWEHelper;

import java.util.Arrays;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD;
import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_REPORTS_IN_REMOTE_EXT_ISOTOPE_CONFIRMATION_STATUS_ID;

public class KromekSerialRemoteExtendedIsotopeConfirmationStatusReport extends SerialReport {
    private KromekSerialRemoteControlMode mode;
    private KromekSerialRemoteModeState state;
    private long totalGammaCounts;
    private long totalNeutronCounts;
    private float totalDose;
    private float averageDoseRate;
    private float averageGammaCps;
    private float averageNeutronCps;
    private float maxDoseRate;
    private long maxGammaCps;
    private long maxNeutronCps;
    private float latitude;
    private float longitude;
    private long deviceTimestamp;
    private int numNuclideResults;
    private byte[] nuclideData;

    public KromekSerialRemoteExtendedIsotopeConfirmationStatusReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialRemoteExtendedIsotopeConfirmationStatusReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_REMOTE_EXT_ISOTOPE_CONFIRMATION_STATUS_ID);
    }

    @Override
    public void decodePayload(byte[] payload) {
        long modeValue = bytesToUInt(payload[0], payload[1], payload[2], payload[3]);
        mode = KromekSerialRemoteControlMode.values()[(int) modeValue];
        state = KromekSerialRemoteModeState.values()[payload[4]];
        totalGammaCounts = bytesToUInt(payload[5], payload[6], payload[7], payload[8]);
        totalNeutronCounts = bytesToUInt(payload[9], payload[10], payload[11], payload[12]);
        totalDose = bytesToFloat(payload[13], payload[14], payload[15], payload[16]);
        averageDoseRate = bytesToFloat(payload[17], payload[18], payload[19], payload[20]);
        averageGammaCps = bytesToFloat(payload[21], payload[22], payload[23], payload[24]);
        averageNeutronCps = bytesToFloat(payload[25], payload[26], payload[27], payload[28]);
        maxDoseRate = bytesToFloat(payload[29], payload[30], payload[31], payload[32]);
        maxGammaCps = bytesToUInt(payload[33], payload[34], payload[35], payload[36]);
        maxNeutronCps = bytesToUInt(payload[37], payload[38], payload[39], payload[40]);
        latitude = bytesToFloat(payload[41], payload[42], payload[43], payload[44]);
        longitude = bytesToFloat(payload[45], payload[46], payload[47], payload[48]);
        deviceTimestamp = bytesToUInt(payload[49], payload[50], payload[51], payload[52]);
        numNuclideResults = bytesToUInt(payload[53], payload[54]);
        // Add the rest of the payload to the nuclideData array for now
        nuclideData = new byte[payload.length - 55];
        System.arraycopy(payload, 55, nuclideData, 0, nuclideData.length);
    }

    @Override
    public String toString() {
        return KromekSerialRemoteExtendedIsotopeConfirmationStatusReport.class.getSimpleName() + " {" +
                "mode=" + mode +
                ", state=" + state +
                ", totalGammaCounts=" + totalGammaCounts +
                ", totalNeutronCounts=" + totalNeutronCounts +
                ", totalDose=" + totalDose +
                ", averageDoseRate=" + averageDoseRate +
                ", averageGammaCps=" + averageGammaCps +
                ", averageNeutronCps=" + averageNeutronCps +
                ", maxDoseRate=" + maxDoseRate +
                ", maxGammaCps=" + maxGammaCps +
                ", maxNeutronCps=" + maxNeutronCps +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", deviceTimestamp=" + deviceTimestamp +
                ", numNuclideResults=" + numNuclideResults +
                ", nuclideData=" + Arrays.toString(nuclideData) +
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
                .addField("mode", sweFactory.createCategory()
                        .label("Mode")
                        .description("Mode")
                        .definition(SWEHelper.getPropertyUri("mode"))
                        .addAllowedValues(Arrays.toString(KromekSerialRemoteControlMode.values())))
                .addField("state", sweFactory.createCategory()
                        .label("State")
                        .description("State")
                        .definition(SWEHelper.getPropertyUri("state"))
                        .addAllowedValues(Arrays.toString(KromekSerialRemoteModeState.values())))
                .addField("totalGammaCounts", sweFactory.createQuantity()
                        .label("Total Gamma Counts")
                        .description("Total Gamma Counts")
                        .definition(SWEHelper.getPropertyUri("totalGammaCounts"))
                        .dataType(DataType.LONG))
                .addField("totalNeutronCounts", sweFactory.createQuantity()
                        .label("Total Neutron Counts")
                        .description("Total Neutron Counts")
                        .definition(SWEHelper.getPropertyUri("totalNeutronCounts"))
                        .dataType(DataType.LONG))
                .addField("totalDose", sweFactory.createQuantity()
                        .label("Total Dose")
                        .description("Total Dose")
                        .definition(SWEHelper.getPropertyUri("totalDose"))
                        .dataType(DataType.FLOAT))
                .addField("averageDoseRate", sweFactory.createQuantity()
                        .label("Average Dose Rate")
                        .description("Average Dose Rate")
                        .definition(SWEHelper.getPropertyUri("averageDoseRate"))
                        .dataType(DataType.FLOAT))
                .addField("averageGammaCps", sweFactory.createQuantity()
                        .label("Average Gamma Cps")
                        .description("Average Gamma Cps")
                        .definition(SWEHelper.getPropertyUri("averageGammaCps"))
                        .dataType(DataType.FLOAT))
                .addField("averageNeutronCps", sweFactory.createQuantity()
                        .label("Average Neutron Cps")
                        .description("Average Neutron Cps")
                        .definition(SWEHelper.getPropertyUri("averageNeutronCps"))
                        .dataType(DataType.FLOAT))
                .addField("maxDoseRate", sweFactory.createQuantity()
                        .label("Max Dose Rate")
                        .description("Max Dose Rate")
                        .definition(SWEHelper.getPropertyUri("maxDoseRate"))
                        .dataType(DataType.FLOAT))
                .addField("maxGammaCps", sweFactory.createQuantity()
                        .label("Max Gamma Cps")
                        .description("Max Gamma Cps")
                        .definition(SWEHelper.getPropertyUri("maxGammaCps"))
                        .dataType(DataType.LONG))
                .addField("maxNeutronCps", sweFactory.createQuantity()
                        .label("Max Neutron Cps")
                        .description("Max Neutron Cps")
                        .definition(SWEHelper.getPropertyUri("maxNeutronCps"))
                        .dataType(DataType.LONG))
                .addField("latitude", sweFactory.createQuantity()
                        .label("Latitude")
                        .description("Latitude")
                        .definition(SWEHelper.getPropertyUri("latitude"))
                        .dataType(DataType.FLOAT)
                        .uom("deg"))
                .addField("longitude", sweFactory.createQuantity()
                        .label("Longitude")
                        .description("Longitude")
                        .definition(SWEHelper.getPropertyUri("longitude"))
                        .dataType(DataType.FLOAT)
                        .uom("deg"))
                .addField("deviceTimestamp", sweFactory.createQuantity()
                        .label("Device Timestamp")
                        .description("Device Timestamp")
                        .definition(SWEHelper.getPropertyUri("deviceTimestamp"))
                        .dataType(DataType.LONG)
                        .uom("s"))
                .addField("numNuclideResults", sweFactory.createQuantity()
                        .label("Number of Nuclide Results")
                        .description("Number of Nuclide Results")
                        .definition(SWEHelper.getPropertyUri("numNuclideResults"))
                        .dataType(DataType.INT))
                .addField("nuclideData", sweFactory.createText()
                        .label("Nuclide Data")
                        .description("Nuclide Data")
                        .definition(SWEHelper.getPropertyUri("nuclideData")))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setStringValue(++index, mode.toString());
        dataBlock.setStringValue(++index, state.toString());
        dataBlock.setLongValue(++index, totalGammaCounts);
        dataBlock.setLongValue(++index, totalNeutronCounts);
        dataBlock.setFloatValue(++index, totalDose);
        dataBlock.setFloatValue(++index, averageDoseRate);
        dataBlock.setFloatValue(++index, averageGammaCps);
        dataBlock.setFloatValue(++index, averageNeutronCps);
        dataBlock.setFloatValue(++index, maxDoseRate);
        dataBlock.setLongValue(++index, maxGammaCps);
        dataBlock.setLongValue(++index, maxNeutronCps);
        dataBlock.setFloatValue(++index, latitude);
        dataBlock.setFloatValue(++index, longitude);
        dataBlock.setLongValue(++index, deviceTimestamp);
        dataBlock.setIntValue(++index, numNuclideResults);
        dataBlock.setStringValue(++index, Arrays.toString(nuclideData));
    }

    @Override
    void setReportInfo() {
        setReportName(KromekSerialRemoteExtendedIsotopeConfirmationStatusReport.class.getSimpleName());
        setReportLabel("Remote Extended Isotope Confirmation Status");
        setReportDescription("Remote Extended Isotope Confirmation Status");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
    }
}
