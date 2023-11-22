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
    private String modelRev;
    private String productName;
    private String serialNumber;

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

        String modelRev1 = String.format("%02X", payload[3]);
        // Trim off the leading 0 if it has one
        if (modelRev1.startsWith("0")) modelRev1 = modelRev1.substring(1);
        String modelRev2 = String.format("%02X", payload[2]);
        modelRev = modelRev1 + '.' + modelRev2;

        byte[] productNameBytes = Arrays.copyOfRange(payload, 4, 4 + KROMEK_SERIAL_REPORTS_PRODUCTNAME_SIZE);
        // Convert to a string. The string is null terminated, so we need to find the null terminator
        int nullTerminatorIndex = 0;
        for (int i = 0; i < productNameBytes.length; i++) {
            if (productNameBytes[i] == 0) {
                nullTerminatorIndex = i;
                break;
            }
        }
        productName = new String(Arrays.copyOfRange(productNameBytes, 0, nullTerminatorIndex));

        byte[] serialNumberBytes = Arrays.copyOfRange(payload, 4 + KROMEK_SERIAL_REPORTS_PRODUCTNAME_SIZE, 4 + KROMEK_SERIAL_REPORTS_PRODUCTNAME_SIZE + KROMEK_SERIAL_REPORTS_SERIALNUMBER_SIZE);
        // Convert to a string. The string is null terminated, so we need to find the null terminator
        nullTerminatorIndex = 0;
        for (int i = 0; i < serialNumberBytes.length; i++) {
            if (serialNumberBytes[i] == 0) {
                nullTerminatorIndex = i;
                break;
            }
        }
        serialNumber = new String(Arrays.copyOfRange(serialNumberBytes, 0, nullTerminatorIndex));
    }

    @Override
    public String toString() {
        return KromekSerialAboutReport.class.getSimpleName() + " {" +
                "firmware=" + firmware +
                ", modelRev=" + modelRev +
                ", productName='" + productName + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
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
                .addField("modelRev", sweFactory.createText()
                        .label("Model Revision")
                        .description("Model Revision")
                        .definition(SWEHelper.getPropertyUri("modelRev")))
                .addField("productName", sweFactory.createText()
                        .label("Product Name")
                        .description("Product Name")
                        .definition(SWEHelper.getPropertyUri("productName")))
                .addField("serialNumber", sweFactory.createText()
                        .label("Serial Number")
                        .description("Serial Number")
                        .definition(SWEHelper.getPropertyUri("serialNumber")))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setStringValue(++index, firmware);
        dataBlock.setStringValue(++index, modelRev);
        dataBlock.setStringValue(++index, productName);
        dataBlock.setStringValue(++index, serialNumber);
    }

    @Override
    void setReportInfo() {
        setReportName("KromekSerialAboutReport");
        setReportLabel("About Report");
        setReportDescription("Information about the device");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
        setPollingRate(10);
    }
}
