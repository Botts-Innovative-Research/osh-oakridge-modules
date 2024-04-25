/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.dsb45;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.helper.GeoPosHelper;

import java.util.Objects;

/**
 * Output specification and provider for {@link DSB45Sensor}.
 *
 * @author your_name
 * @since date
 */
public class FloodAlarmOutput extends AbstractSensorOutput<DSB45Sensor> {

    private static final String SENSOR_OUTPUT_NAME = "[DSB45 Water Sensor]";
    private static final String SENSOR_OUTPUT_LABEL = "[LABEL]";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "[DESCRIPTION]";

    private static final Logger logger = LoggerFactory.getLogger(FloodAlarmOutput.class);

    private DataRecord floodData;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();


    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    FloodAlarmOutput(DSB45Sensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Output created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit() {

        logger.debug("Initializing Output");

        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper floodHelper = new GeoPosHelper();

        // TODO: Create data record description
        floodData = floodHelper.createRecord()
                .name(getName())
                .label("Flood Alarm")
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", floodHelper.createTime()
                        .asSamplingTimeIsoUTC())
                .addField("Flood Alarm", floodHelper.createBoolean()
                        .name("flood-alarm")
                        .label("Flood Alarm" + " Status")
                        .definition("http://sensorml.com/ont/swe/property/Alarm")
                        .description("Status of FLood Alarm"))
                .build();

        dataEncoding = floodHelper.newTextEncoding(",", "\n");

        logger.debug("Initializing Output Complete");
    }


    /**
     * Terminates processing data for output
     */
    public void doStop() {

        synchronized (processingLock) {

            stopProcessing = true;
        }

        // TODO: Perform other shutdown procedures
    }


    @Override
    public DataComponent getRecordDescription() {

        return floodData;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {

        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {

        long accumulator = 0;

        synchronized (histogramLock) {

            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {

                accumulator += timingHistogram[idx];
            }
        }

        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }

    public void onNewMessage(int key, String value, Boolean isFlood) {

        if (key == 32 && Objects.equals(value, "255")) {
            isFlood = true;
        } else if (key == 32 && Objects.equals(value, "0")){
            isFlood = false;
        }


        boolean processSets = true;

        long lastSetTimeMillis = System.currentTimeMillis();

        try {

//            while (processSets) {

                DataBlock dataBlock;
                if (latestRecord == null) {

                    dataBlock = floodData.createDataBlock();

                } else {

                    dataBlock = latestRecord.renew();
                }

                synchronized (histogramLock) {

                    int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;

                    // Get a sampling time for latest set based on previous set sampling time
                    timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;

                    // Set latest sampling time to now
                    lastSetTimeMillis = timingHistogram[setIndex];
                }

                ++setCount;

                double timestamp = System.currentTimeMillis() / 1000d;

                // TODO: Populate data block
                dataBlock.setDoubleValue(0, timestamp);
                dataBlock.setBooleanValue(1, isFlood);

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publish(new DataEvent(latestRecordTime, FloodAlarmOutput.this, dataBlock));

//                synchronized (processingLock) {
//
//                    processSets = !stopProcessing;
//                }
//            }

        } catch (Exception e) {

            logger.error("Error in worker thread: {}", Thread.currentThread().getName(), e);

        } finally {

            // Reset the flag so that when driver is restarted loop thread continues
            // until doStop called on the output again
            stopProcessing = false;

            logger.debug("Terminating worker thread: {}", this.name);
        }
    }
}
