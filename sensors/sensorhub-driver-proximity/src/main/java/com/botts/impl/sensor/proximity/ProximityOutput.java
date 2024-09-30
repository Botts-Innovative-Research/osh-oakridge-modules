/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one
 * at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * Copyright (c) 2024 Botts Innovative Research, Inc. All Rights Reserved.
 */
package com.botts.impl.sensor.proximity;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

public class ProximityOutput extends AbstractSensorOutput<ProximitySensor> {
    private static final String SENSOR_OUTPUT_NAME = "ProximitySwitchOutput";
    private static final String SENSOR_OUTPUT_LABEL = "Proximity Switch Output";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Output data from the proximity switch sensor.";

    private DataRecord dataRecord;
    private DataEncoding dataEncoding;

    /**
     * Constructs an Output instance for the given parent sensor.
     * Initializes the data record and data encoding for the sensor output.
     *
     * @param parentSensor the parent sensor associated with this output
     */
    public ProximityOutput(ProximitySensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);

        initializeDataRecord();
        initializeDataEncoding();
    }

    /**
     * Sets the data for the sensor output.
     *
     * @param objectPresent boolean indicating if the object is present
     */
    public void setData(boolean objectPresent) {
        long timestamp = System.currentTimeMillis();
        DataBlock dataBlock = latestRecord == null ? dataRecord.createDataBlock() : latestRecord.renew();

        dataBlock.setDoubleValue(0, timestamp / 1000d);
        dataBlock.setBooleanValue(1, objectPresent);

        latestRecord = dataBlock;
        latestRecordTime = timestamp;
        eventHandler.publish(new DataEvent(latestRecordTime, ProximityOutput.this, dataBlock));
    }

    /**
     * Initializes the data record for the sensor output.
     */
    private void initializeDataRecord() {
        SWEHelper sweHelper = new SWEHelper();

        dataRecord = sweHelper.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("objectPresent", sweHelper.createBoolean()
                        .label("Object Present")
                        .description("Indicates if an object is present"))
                .build();
    }

    /**
     * Initializes the data encoding for the sensor output.
     */
    private void initializeDataEncoding() {
        dataEncoding = new SWEHelper().newTextEncoding(",", "\n");
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataRecord;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return Double.NaN;
    }
}
