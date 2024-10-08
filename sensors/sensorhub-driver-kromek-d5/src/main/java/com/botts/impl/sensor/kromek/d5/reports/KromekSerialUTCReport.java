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

import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD;
import static com.botts.impl.sensor.kromek.d5.reports.Constants.KROMEK_SERIAL_REPORTS_IN_UTC_TIME_ID;

public class KromekSerialUTCReport extends SerialReport {
    private long deviceTimestamp;
    private float timezoneOffset;
    private boolean dstEnabled;

    public KromekSerialUTCReport(byte componentId, byte reportId, byte[] data) {
        super(componentId, reportId);
        decodePayload(data);
    }

    public KromekSerialUTCReport() {
        super(KROMEK_SERIAL_COMPONENT_INTERFACE_BOARD, KROMEK_SERIAL_REPORTS_IN_UTC_TIME_ID);
    }

    @Override
    public void decodePayload(byte[] payload) {
        deviceTimestamp = bytesToUInt(payload[0], payload[1], payload[2], payload[3]);
        timezoneOffset = bytesToFloat(payload[4], payload[5], payload[6], payload[7]);
        dstEnabled = byteToBoolean(payload[8]);
    }

    @Override
    public String toString() {
        return KromekSerialUTCReport.class.getSimpleName() + " {" +
                "deviceTimestamp=" + deviceTimestamp +
                ", timezoneOffset=" + timezoneOffset +
                ", dstEnabled=" + dstEnabled +
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
                .addField("deviceTimestamp", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Device Timestamp")
                        .description("Seconds since 1st January 1970 UTC")
                        .definition(SWEHelper.getPropertyUri("deviceTimestamp")))
                .addField("timezoneOffset", sweFactory.createQuantity()
                        .label("Timezone Offset")
                        .description("offset in hours from UTC")
                        .definition(SWEHelper.getPropertyUri("timezoneOffset"))
                        .dataType(DataType.INT))
                .addField("dstEnabled", sweFactory.createBoolean()
                        .label("DST Enabled")
                        .description("1 = apply daylight saving time")
                        .definition(SWEHelper.getPropertyUri("dstEnabled")))
                .build();
    }

    @Override
    public void setDataBlock(DataBlock dataBlock, DataRecord dataRecord, double timestamp) {
        int index = 0;
        dataBlock.setDoubleValue(index, timestamp);
        dataBlock.setLongValue(++index, deviceTimestamp);
        dataBlock.setDoubleValue(++index, timezoneOffset);
        dataBlock.setBooleanValue(++index, dstEnabled);
    }

    @Override
    void setReportInfo() {
        setReportName(KromekSerialUTCReport.class.getSimpleName());
        setReportLabel("Kromek Serial UTC Report");
        setReportDescription("Kromek Serial UTC Report");
        setReportDefinition(SWEHelper.getPropertyUri(getReportName()));
    }
}
