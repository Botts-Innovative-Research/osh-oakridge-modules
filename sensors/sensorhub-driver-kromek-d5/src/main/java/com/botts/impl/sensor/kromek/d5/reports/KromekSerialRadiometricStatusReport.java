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

import java.util.Arrays;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD_EXT;
import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_REPORTS_IN_RADIOMETRIC_STATUS_REPORT;

public class KromekSerialRadiometricStatusReport extends SerialReport {
    private boolean doseAlarmActive;
    private boolean gammaCpsAlarmActive;
    private boolean neutronCpsAlarmActive;
    private float latitude;
    private float longitude;
    private long deviceTimestamp;
    private int numNuclideResults;
    private byte[] nuclideData;

    public KromekSerialRadiometricStatusReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialRadiometricStatusReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD_EXT, KROMEK_SERIAL_REPORTS_IN_RADIOMETRIC_STATUS_REPORT);
    }

    @Override
    public void decodePayload(byte[] payload) {
        doseAlarmActive = byteToBoolean(payload[0]);
        gammaCpsAlarmActive = byteToBoolean(payload[1]);
        neutronCpsAlarmActive = byteToBoolean(payload[2]);
        latitude = bytesToFloat(payload[3], payload[4], payload[5], payload[6]);
        longitude = bytesToFloat(payload[7], payload[8], payload[9], payload[10]);
        deviceTimestamp = bytesToUInt(payload[11], payload[12], payload[13], payload[14]);
        numNuclideResults = bytesToUInt(payload[15], payload[16]);
        // For now, store the raw bytes
        nuclideData = new byte[payload.length - 17];
        System.arraycopy(payload, 17, nuclideData, 0, nuclideData.length);
    }

    @Override
    public String toString() {
        return KromekSerialRadiometricStatusReport.class.getSimpleName() + " {" +
                "doseAlarmActive=" + doseAlarmActive +
                ", gammaCpsAlarmActive=" + gammaCpsAlarmActive +
                ", neutronCpsAlarmActive=" + neutronCpsAlarmActive +
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
                .addField("doseAlarmActive", sweFactory.createBoolean()
                        .label("Dose Alarm Active")
                        .description("Dose Alarm Active")
                        .definition(SWEHelper.getPropertyUri("doseAlarmActive")))
                .addField("gammaCpsAlarmActive", sweFactory.createBoolean()
                        .label("Gamma CPS Alarm Active")
                        .description("Gamma CPS Alarm Active")
                        .definition(SWEHelper.getPropertyUri("gammaCpsAlarmActive")))
                .addField("neutronCpsAlarmActive", sweFactory.createBoolean()
                        .label("Neutron CPS Alarm Active")
                        .description("Neutron CPS Alarm Active")
                        .definition(SWEHelper.getPropertyUri("neutronCpsAlarmActive")))
                .addField("latitude", sweFactory.createQuantity()
                        .label("Latitude")
                        .description("Latitude")
                        .definition(SWEHelper.getPropertyUri("latitude"))
                        .uom("deg")
                        .dataType(DataType.FLOAT))
                .addField("longitude", sweFactory.createQuantity()
                        .label("Longitude")
                        .description("Longitude")
                        .definition(SWEHelper.getPropertyUri("longitude"))
                        .uom("deg")
                        .dataType(DataType.FLOAT))
                .addField("deviceTimestamp", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Device Timestamp")
                        .description("Device Timestamp")
                        .definition(SWEHelper.getPropertyUri("deviceTimestamp")))
                .addField("numNuclideResults", sweFactory.createQuantity()
                        .label("Number of Nuclide Results")
                        .description("Indicates how many (if any) of the following are present")
                        .definition(SWEHelper.getPropertyUri("numNuclideResults"))
                        .dataType(DataType.INT))
                .addField("nuclideData", sweFactory.createText()
                        .label("Nuclide Data")
                        .description("an array of nuclide data, format: KromekSerialNuclideIdType, KromekSerialNuclideIdCategory, Confidence indication")
                        .definition(SWEHelper.getPropertyUri("nuclideData")))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setBooleanValue(++index, doseAlarmActive);
        dataBlock.setBooleanValue(++index, gammaCpsAlarmActive);
        dataBlock.setBooleanValue(++index, neutronCpsAlarmActive);
        dataBlock.setFloatValue(++index, latitude);
        dataBlock.setFloatValue(++index, longitude);
        dataBlock.setLongValue(++index, deviceTimestamp);
        dataBlock.setIntValue(++index, numNuclideResults);
        dataBlock.setStringValue(++index, Arrays.toString(nuclideData));
    }

    @Override
    void setReportInfo() {
        setReportName(KromekSerialRadiometricStatusReport.class.getSimpleName());
        setReportLabel("Radiometric Status");
        setReportDescription("Radiometric Status");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
    }
}
