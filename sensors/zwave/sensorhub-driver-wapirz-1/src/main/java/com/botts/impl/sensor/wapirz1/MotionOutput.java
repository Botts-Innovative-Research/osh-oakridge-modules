/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.wapirz1;

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
 * Output specification and provider for {@link WAPIRZ1Sensor}.
 *
 * @author your_name
 * @since date
 */
public class MotionOutput extends AbstractSensorOutput<WAPIRZ1Sensor> {

    private static final String SENSOR_OUTPUT_NAME = "Motion Sensor";
    private static final Logger logger = LoggerFactory.getLogger(MotionOutput.class);

    private DataRecord motionData;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    boolean isMotion = false;
    private Thread worker;

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    MotionOutput(WAPIRZ1Sensor parentSensor) {

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
        GeoPosHelper motionHelper = new GeoPosHelper();

        motionData = motionHelper.createRecord()
                .name(getName())
                .label(SENSOR_OUTPUT_NAME)
                .definition("http://sensorml.com/ont/swe/property/Motion")
                .addField("Sampling Time", motionHelper.createTime().asSamplingTimeIsoUTC())
                .addField(SENSOR_OUTPUT_NAME,
                        motionHelper.createBoolean()
                                .name("motion-sensor")
                                .label(SENSOR_OUTPUT_NAME)
                                .definition("http://sensorml.com/ont/swe/property/Motion")
                                .description("Detection of Movement"))

                .build();

        dataEncoding = motionHelper.newTextEncoding(",", "\n");

        logger.debug("Initializing Output Complete");
    }

    /**
     * Begins processing data for output
     */
//    public void doStart() {
//
//        // Instantiate a new worker thread
//        worker = new Thread(this, this.name);
//
//        logger.info("Starting worker thread: {}", worker.getName());
//
//        // Start the worker thread
//        worker.start();
//    }

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
//    public boolean isAlive() {
//
//        return worker.isAlive();
//    }

    @Override
    public DataComponent getRecordDescription() {

        return motionData;
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

    public void onNewMessage(int key, String value, int event, Boolean isMotion) {

        if (key == 7 && Objects.equals(value, "255") && event == 2) {
            isMotion = true;
        } else if (key == 7 && Objects.equals(value, "0") && event == 2){
            isMotion = false;
        }

//    public void onNewMessage(String alarmType, String alarmValue, int v1AlarmCode, Boolean isMotion) {
//
//        if (Objects.equals(alarmType, "COMMAND_CLASS_BASIC") && Objects.equals(alarmValue, "255")) {
//            isMotion = true;
//        } else if (Objects.equals(alarmType, "COMMAND_CLASS_BASIC") && Objects.equals(alarmValue, "0")) {
//            isMotion = false;
//        }
//        key = 7; value = 255-triggered/0-untriggered; event = 2; - shook it around
//        key = 32; value = 255; isMotion = true;
//        key = 32; value = 0; isMotion = false; - or is this resetting Tamper alarm?

        boolean processSets = true;

        long lastSetTimeMillis = System.currentTimeMillis();

        try {

//                while (processSets) {

            DataBlock dataBlock;
            if (latestRecord == null) {

                dataBlock = motionData.createDataBlock();

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
            dataBlock.setBooleanValue(1, isMotion);

            latestRecord = dataBlock;

            latestRecordTime = System.currentTimeMillis();

            eventHandler.publish(new DataEvent(latestRecordTime, MotionOutput.this, dataBlock));

//                    synchronized (processingLock) {
//
//                        processSets = !stopProcessing;
//                    }
//                }

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