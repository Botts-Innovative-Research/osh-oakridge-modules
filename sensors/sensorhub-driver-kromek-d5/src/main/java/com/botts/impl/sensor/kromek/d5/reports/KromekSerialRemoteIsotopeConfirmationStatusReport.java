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

import com.botts.impl.sensor.kromek.d5.enums.KromekSerialNuclideIdCategory;
import com.botts.impl.sensor.kromek.d5.enums.KromekSerialNuclideIdType;
import com.botts.impl.sensor.kromek.d5.enums.KromekSerialRemoteControlMode;
import com.botts.impl.sensor.kromek.d5.enums.KromekSerialRemoteModeState;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import org.vast.swe.SWEHelper;

import java.util.Arrays;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD;
import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_REPORTS_IN_REMOTE_ISOTOPE_CONFIRMATION_STATUS_ID;

public class KromekSerialRemoteIsotopeConfirmationStatusReport extends SerialReport {
    private KromekSerialRemoteControlMode mode;
    private KromekSerialRemoteModeState state;
    private int numNuclideResults;
    private KromekSerialNuclideIdType nuclideID_1;
    private KromekSerialNuclideIdCategory categoryID_1;
    private float confidence_1;
    private KromekSerialNuclideIdType nuclideID_2;
    private KromekSerialNuclideIdCategory categoryID_2;
    private float confidence_2;
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

    public KromekSerialRemoteIsotopeConfirmationStatusReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialRemoteIsotopeConfirmationStatusReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_REMOTE_ISOTOPE_CONFIRMATION_STATUS_ID);
    }

    @Override
    public void decodePayload(byte[] payload) {
        mode = KromekSerialRemoteControlMode.values()[(int) bytesToUInt(payload[0], payload[1], payload[2], payload[3])];
        state = KromekSerialRemoteModeState.values()[bytesToUInt(payload[4])];
        numNuclideResults = bytesToUInt(payload[5], payload[6]);
        nuclideID_1 = KromekSerialNuclideIdType.values()[bytesToUInt(payload[7])];
        categoryID_1 = KromekSerialNuclideIdCategory.values()[bytesToUInt(payload[8])];
        confidence_1 = bytesToFloat(payload[9], payload[10], payload[11], payload[12]);
        nuclideID_2 = KromekSerialNuclideIdType.values()[bytesToUInt(payload[13])];
        categoryID_2 = KromekSerialNuclideIdCategory.values()[bytesToUInt(payload[14])];
        confidence_2 = bytesToFloat(payload[15], payload[16], payload[17], payload[18]);
        totalGammaCounts = bytesToUInt(payload[19], payload[20], payload[21], payload[22]);
        totalNeutronCounts = bytesToUInt(payload[23], payload[24], payload[25], payload[26]);
        totalDose = bytesToFloat(payload[27], payload[28], payload[29], payload[30]);
        averageDoseRate = bytesToFloat(payload[31], payload[32], payload[33], payload[34]);
        averageGammaCps = bytesToFloat(payload[35], payload[36], payload[37], payload[38]);
        averageNeutronCps = bytesToFloat(payload[39], payload[40], payload[41], payload[42]);
        maxDoseRate = bytesToFloat(payload[43], payload[44], payload[45], payload[46]);
        maxGammaCps = bytesToUInt(payload[47], payload[48], payload[49], payload[50]);
        maxNeutronCps = bytesToUInt(payload[51], payload[52], payload[53], payload[54]);
        latitude = bytesToFloat(payload[55], payload[56], payload[57], payload[58]);
        longitude = bytesToFloat(payload[59], payload[60], payload[61], payload[62]);
        deviceTimestamp = bytesToUInt(payload[63], payload[64], payload[65], payload[66]);
    }

    @Override
    public String toString() {
        return KromekSerialRemoteIsotopeConfirmationStatusReport.class.getSimpleName() + " {" +
                "mode=" + mode +
                ", state=" + state +
                ", numNuclideResults=" + numNuclideResults +
                ", nuclideID_1=" + nuclideID_1 +
                ", categoryID_1=" + categoryID_1 +
                ", confidence_1=" + confidence_1 +
                ", nuclideID_2=" + nuclideID_2 +
                ", categoryID_2=" + categoryID_2 +
                ", confidence_2=" + confidence_2 +
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
                .addField("numNuclideResults", sweFactory.createQuantity()
                        .label("Number of Nuclide Results")
                        .description("0 - 2. Indicates how many (if any) of the following are valid")
                        .definition(SWEHelper.getPropertyUri("numNuclideResults"))
                        .dataType(DataType.INT))
                .addField("nuclideID_1", sweFactory.createCategory()
                        .label("Nuclide ID 1")
                        .description("Nuclide ID 1")
                        .definition(SWEHelper.getPropertyUri("nuclideID_1"))
                        .addAllowedValues(Arrays.toString(KromekSerialNuclideIdType.values())))
                .addField("categoryID_1", sweFactory.createCategory()
                        .label("Category ID 1")
                        .description("Category ID 1")
                        .definition(SWEHelper.getPropertyUri("categoryID_1"))
                        .addAllowedValues(Arrays.toString(KromekSerialNuclideIdCategory.values())))
                .addField("confidence_1", sweFactory.createQuantity()
                        .label("Confidence 1")
                        .description("Confidence indication")
                        .definition(SWEHelper.getPropertyUri("confidence_1")))
                .addField("nuclideID_2", sweFactory.createCategory()
                        .label("Nuclide ID 2")
                        .description("Nuclide ID 2")
                        .definition(SWEHelper.getPropertyUri("nuclideID_2"))
                        .addAllowedValues(Arrays.toString(KromekSerialNuclideIdType.values())))
                .addField("categoryID_2", sweFactory.createCategory()
                        .label("Category ID 2")
                        .description("Category ID 2")
                        .definition(SWEHelper.getPropertyUri("categoryID_2"))
                        .addAllowedValues(Arrays.toString(KromekSerialNuclideIdCategory.values())))
                .addField("confidence_2", sweFactory.createQuantity()
                        .label("Confidence 2")
                        .description("Confidence indication")
                        .definition(SWEHelper.getPropertyUri("confidence_2")))
                .addField("totalGammaCounts", sweFactory.createQuantity()
                        .label("Total Gamma Counts")
                        .description("Total Gamma Counts")
                        .definition(SWEHelper.getPropertyUri("totalGammaCounts"))
                        .dataType(DataType.INT))
                .addField("totalNeutronCounts", sweFactory.createQuantity()
                        .label("Total Neutron Counts")
                        .description("Total Neutron Counts")
                        .definition(SWEHelper.getPropertyUri("totalNeutronCounts"))
                        .dataType(DataType.INT))
                .addField("totalDose", sweFactory.createQuantity()
                        .label("Total Dose")
                        .description("Total Dose")
                        .definition(SWEHelper.getPropertyUri("totalDose")))
                .addField("averageDoseRate", sweFactory.createQuantity()
                        .label("Average Dose Rate")
                        .description("Average Dose Rate")
                        .definition(SWEHelper.getPropertyUri("averageDoseRate")))
                .addField("averageGammaCps", sweFactory.createQuantity()
                        .label("Average Gamma Cps")
                        .description("Average Gamma Cps")
                        .definition(SWEHelper.getPropertyUri("averageGammaCps")))
                .addField("averageNeutronCps", sweFactory.createQuantity()
                        .label("Average Neutron Cps")
                        .description("Average Neutron Cps")
                        .definition(SWEHelper.getPropertyUri("averageNeutronCps")))
                .addField("maxDoseRate", sweFactory.createQuantity()
                        .label("Max Dose Rate")
                        .description("Max Dose Rate")
                        .definition(SWEHelper.getPropertyUri("maxDoseRate")))
                .addField("maxGammaCps", sweFactory.createQuantity()
                        .label("Max Gamma Cps")
                        .description("Max Gamma Cps")
                        .definition(SWEHelper.getPropertyUri("maxGammaCps"))
                        .dataType(DataType.INT))
                .addField("maxNeutronCps", sweFactory.createQuantity()
                        .label("Max Neutron Cps")
                        .description("Max Neutron Cps")
                        .definition(SWEHelper.getPropertyUri("maxNeutronCps"))
                        .dataType(DataType.INT))
                .addField("latitude", sweFactory.createQuantity()
                        .label("Latitude")
                        .description("Latitude")
                        .definition(SWEHelper.getPropertyUri("latitude"))
                        .uom("deg"))
                .addField("longitude", sweFactory.createQuantity()
                        .label("Longitude")
                        .description("Longitude")
                        .definition(SWEHelper.getPropertyUri("longitude"))
                        .uom("deg"))
                .addField("deviceTimestamp", sweFactory.createQuantity()
                        .label("Device Timestamp")
                        .description("Timestamp from the device associated with the report")
                        .definition(SWEHelper.getPropertyUri("deviceTimestamp"))
                        .uom("s"))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setStringValue(++index, mode.toString());
        dataBlock.setStringValue(++index, state.toString());
        dataBlock.setIntValue(++index, numNuclideResults);
        dataBlock.setStringValue(++index, nuclideID_1.toString());
        dataBlock.setStringValue(++index, categoryID_1.toString());
        dataBlock.setDoubleValue(++index, confidence_1);
        dataBlock.setStringValue(++index, nuclideID_2.toString());
        dataBlock.setStringValue(++index, categoryID_2.toString());
        dataBlock.setDoubleValue(++index, confidence_2);
        dataBlock.setLongValue(++index, totalGammaCounts);
        dataBlock.setLongValue(++index, totalNeutronCounts);
        dataBlock.setDoubleValue(++index, totalDose);
        dataBlock.setDoubleValue(++index, averageDoseRate);
        dataBlock.setDoubleValue(++index, averageGammaCps);
        dataBlock.setDoubleValue(++index, averageNeutronCps);
        dataBlock.setDoubleValue(++index, maxDoseRate);
        dataBlock.setLongValue(++index, maxGammaCps);
        dataBlock.setLongValue(++index, maxNeutronCps);
        dataBlock.setDoubleValue(++index, latitude);
        dataBlock.setDoubleValue(++index, longitude);
        dataBlock.setLongValue(++index, deviceTimestamp);
    }

    @Override
    void setReportInfo() {
        setReportName(KromekSerialRemoteIsotopeConfirmationStatusReport.class.getSimpleName());
        setReportLabel("Remote Isotope Confirmation Status Report");
        setReportDescription("Remote Isotope Confirmation Status Report");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
    }
}
