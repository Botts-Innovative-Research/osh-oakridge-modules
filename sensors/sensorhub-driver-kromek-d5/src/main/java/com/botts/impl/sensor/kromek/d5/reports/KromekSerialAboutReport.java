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

import java.util.Arrays;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.*;

public class KromekSerialAboutReport extends SerialReport {
    private String firmware;
    private String modelrev;
    private String productname;
    private String serialnumber;

    public KromekSerialAboutReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialAboutReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_ABOUT_ID);
    }

    @Override
    public void decodePayload(byte[] payload) {
        String firmware1 = String.format("%02X", payload[1]);
        // Trim off the leading 0 if it has one
        if (firmware1.startsWith("0")) firmware1 = firmware1.substring(1);
        String firmware2 = String.format("%02X", payload[0]);
        firmware = firmware1 + '.' + firmware2;

        String modelrev1 = String.format("%02X", payload[3]);
        // Trim off the leading 0 if it has one
        if (modelrev1.startsWith("0")) modelrev1 = modelrev1.substring(1);
        String modelrev2 = String.format("%02X", payload[2]);
        modelrev = modelrev1 + '.' + modelrev2;

        // Read in all KROMEK_SERIAL_REPORTS_PRODUCTNAME_SIZE bytes
        byte[] productNameBytes = Arrays.copyOfRange(payload, 4, 4 + KROMEK_SERIAL_REPORTS_PRODUCTNAME_SIZE);
        // Convert to a string. The string is null terminated, so we need to find the null terminator
        int nullTerminatorIndex = 0;
        for (int i = 0; i < productNameBytes.length; i++) {
            if (productNameBytes[i] == 0) {
                nullTerminatorIndex = i;
                break;
            }
        }
        productname = new String(Arrays.copyOfRange(productNameBytes, 0, nullTerminatorIndex));

        // Read in all KROMEK_SERIAL_REPORTS_SERIALNUMBER_SIZE bytes
        byte[] serialNumberBytes = Arrays.copyOfRange(payload, 4 + KROMEK_SERIAL_REPORTS_PRODUCTNAME_SIZE, 4 + KROMEK_SERIAL_REPORTS_PRODUCTNAME_SIZE + KROMEK_SERIAL_REPORTS_SERIALNUMBER_SIZE);
        // Convert to a string. The string is null terminated, so we need to find the null terminator
        nullTerminatorIndex = 0;
        for (int i = 0; i < serialNumberBytes.length; i++) {
            if (serialNumberBytes[i] == 0) {
                nullTerminatorIndex = i;
                break;
            }
        }
        serialnumber = new String(Arrays.copyOfRange(serialNumberBytes, 0, nullTerminatorIndex));
    }

    @Override
    public String toString() {
        return KromekSerialAboutReport.class.getSimpleName() + " {" +
                "firmware=" + firmware +
                ", modelrev=" + modelrev +
                ", productname='" + productname + '\'' +
                ", serialnumber='" + serialnumber + '\'' +
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
                .addField("firmware", sweFactory.createText()
                        .label("Firmware")
                        .description("Firmware")
                        .definition(SWEHelper.getPropertyUri("firmware")))
                .addField("modelrev", sweFactory.createText()
                        .label("Model Revision")
                        .description("Model Revision")
                        .definition(SWEHelper.getPropertyUri("modelrev")))
                .addField("productname", sweFactory.createText()
                        .label("Product Name")
                        .description("Product Name")
                        .definition(SWEHelper.getPropertyUri("productname")))
                .addField("serialnumber", sweFactory.createText()
                        .label("Serial Number")
                        .description("Serial Number")
                        .definition(SWEHelper.getPropertyUri("serialnumber")))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setStringValue(++index, firmware);
        dataBlock.setStringValue(++index, modelrev);
        dataBlock.setStringValue(++index, productname);
        dataBlock.setStringValue(++index, serialnumber);
    }

    @Override
    void setReportInfo() {
        setReportName(KromekSerialAboutReport.class.getSimpleName());
        setReportLabel("About");
        setReportDescription("About");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
        setPollingRate(0);
    }
}
