/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.ds100;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.helper.GeoPosHelper;

/**
 * Output specification and provider for {@link DS100Sensor}.
 *
 * @author cardy
 * @since 09/09/24
 */
public class TamperAlarmOutput extends AbstractSensorOutput<DS100Sensor> {

    private static final String SENSOR_OUTPUT_NAME = "DS100 Tamper Alarm";

    private static final Logger logger = LoggerFactory.getLogger(TamperAlarmOutput.class);

    private DataRecord tamperAlarmData;
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
    TamperAlarmOutput(DS100Sensor parentSensor) {

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
        GeoPosHelper tamperAlarmHelper = new GeoPosHelper();

        String strTamperAlarmStatus = "Tamper Alarm Status";

        tamperAlarmData = tamperAlarmHelper.createRecord()
                .name(getName())
                .label("Tamper Alarm")
                .definition("http://sensorml.com/ont/swe/property/Alarm")
                .addField("Sampling Time", tamperAlarmHelper.createTime().asSamplingTimeIsoUTC())
                .addField(strTamperAlarmStatus,
                        tamperAlarmHelper.createBoolean()
                                .name("tamper-alarm-status")
                                .label(strTamperAlarmStatus)
                                .definition("http://sensorml.com/ont/swe/property/Alarm")
                                .description("Status of Tamper Alarm"))
                .build();

        dataEncoding = tamperAlarmHelper.newTextEncoding(",", "\n");

        logger.debug("Initializing Output Complete");
    }

    /**
     * Terminates processing data for output
     */
    public void doStop() {

        synchronized (processingLock) {

            stopProcessing = true;
        }

    }

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */

    @Override
    public DataComponent getRecordDescription() {

        return tamperAlarmData;
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
    public void onNewMessage(boolean isTamperAlarm) {

        boolean processSets = true;

        long lastSetTimeMillis = System.currentTimeMillis();

        try {

            DataBlock dataBlock;
            if (latestRecord == null) {

                dataBlock = tamperAlarmData.createDataBlock();

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

            double time = System.currentTimeMillis() / 1000.;


            dataBlock.setDoubleValue(0, time);
            dataBlock.setBooleanValue(1, isTamperAlarm);

            latestRecord = dataBlock;

            latestRecordTime = System.currentTimeMillis();

            eventHandler.publish(new DataEvent(latestRecordTime, TamperAlarmOutput.this, dataBlock));

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

