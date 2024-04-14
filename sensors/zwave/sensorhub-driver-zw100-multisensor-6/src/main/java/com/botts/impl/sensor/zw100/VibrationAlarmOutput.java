/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.zw100;

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
 * Output specification and provider for {@link ZW100Sensor}.
 *
 * @author cardy
 * @since 11/14/23
 */
public class VibrationAlarmOutput extends AbstractSensorOutput<ZW100Sensor>{

    private static final String SENSOR_OUTPUT_NAME = "ZW100 Vibration Alarm";

    private static final Logger logger = LoggerFactory.getLogger(VibrationAlarmOutput.class);

    private DataRecord vibrationAlarmData;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    private Thread worker;

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    VibrationAlarmOutput(ZW100Sensor parentSensor) {

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
        GeoPosHelper vibrationAlarmHelper = new GeoPosHelper();

        String strVibrationAlarm = "Vibration Alarm";

        vibrationAlarmData = vibrationAlarmHelper.createRecord()
                .name(getName())
                .label(strVibrationAlarm)
                .definition("http://sensorml.com/ont/swe/property/Alarm")
                .addField("Sampling Time", vibrationAlarmHelper.createTime().asSamplingTimeIsoUTC())
                .addField(strVibrationAlarm,
                        vibrationAlarmHelper.createCategory()
                                .name("vibration-alarm-status")
                                .label(strVibrationAlarm)
                                .definition("http://sensorml.com/ont/swe/property/Alarm")
                                .description("Status of Vibration Alarm"))
//                                .addAllowedValues("ON", "OFF"))

                .build();

        dataEncoding = vibrationAlarmHelper.newTextEncoding(",", "\n");

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
    public boolean isAlive() {

        return worker.isAlive();
    }

    @Override
    public DataComponent getRecordDescription() {

        return vibrationAlarmData;
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

//    public void onNewMessage(String alarmType, String alarmValue, int alarmEvent, Boolean isVibration) {
//
//        if (Objects.equals(alarmType, "BURGLAR") &&  alarmEvent == 3 && Objects.equals(alarmValue, "255")) {
//            isVibration = true;
//        } else if (Objects.equals(alarmType, "BURGLAR") && alarmEvent == 0){
//            isVibration = false;
//        }

    public void onNewMessage(int key, String value, int event, Boolean isVibration) {
        // key 7 = BURGLAR
        if (key == 7 && Objects.equals(value, "255") && event == 3) {
            isVibration = true;
        } else if (key == 7 && event == 0){
            isVibration = false;
        }

        boolean processSets = true;

        long lastSetTimeMillis = System.currentTimeMillis();

        try {

//            while (processSets) {

                DataBlock dataBlock;
                if (latestRecord == null) {

                    dataBlock = vibrationAlarmData.createDataBlock();

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
                dataBlock.setBooleanValue(1, isVibration);

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publish(new DataEvent(latestRecordTime, VibrationAlarmOutput.this, dataBlock));

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
