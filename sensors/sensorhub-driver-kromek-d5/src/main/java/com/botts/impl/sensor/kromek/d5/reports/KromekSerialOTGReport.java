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

import com.botts.impl.sensor.kromek.d5.enums.KromekSerialUSBOTGMode;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import org.vast.swe.SWEHelper;

import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD;
import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_REPORTS_IN_OTG_MODE_ID;

public class KromekSerialOTGReport extends SerialReport {
    private KromekSerialUSBOTGMode mode;

    public KromekSerialOTGReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialOTGReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_OTG_MODE_ID);
    }

    @Override
    public void decodePayload(byte[] payload) {
        mode = KromekSerialUSBOTGMode.values()[bytesToUInt(payload[0])];
    }

    @Override
    public String toString() {
        return KromekSerialOTGReport.class.getSimpleName() + " {" +
                "mode=" + mode +
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
                        .description("USB OTG Mode")
                        .definition(SWEHelper.getPropertyUri("mode")))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setStringValue(++index, mode.toString());
    }

    @Override
    void setReportInfo() {
        setReportName(KromekSerialOTGReport.class.getSimpleName());
        setReportLabel("USB OTG");
        setReportDescription("USB OTG");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
        setPollingRate(5);
    }
}
