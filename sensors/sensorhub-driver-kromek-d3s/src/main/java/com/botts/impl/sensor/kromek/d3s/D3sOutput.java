/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.kromek.d3s;

import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Boolean;
import java.util.Random;

/**
 * Output specification and provider for ...
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class D3sOutput extends AbstractSensorOutput<D3sSensor> implements Runnable {

    private static final String SENSOR_OUTPUT_NAME = "[NAME]";
    private static final String SENSOR_OUTPUT_LABEL = "[LABEL]";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "[DESCRIPTION]";

    private static final Logger logger = LoggerFactory.getLogger(D3sOutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    Thread worker;

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    D3sOutput(D3sSensor parentSensor) {

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
//        GeoPosHelper sweFactory = new GeoPosHelper();
        SWEHelper sweFactory = new SWEHelper();

        // TODO: Create data record description
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .definition("urn:osh:data:weather")
                .description("Weather measurements")
                .addField("time", sweFactory.createTime().asSamplingTimeIsoUTC())
                .addField("temperature", sweFactory.createQuantity()
                        .definition(SWEHelper.getCfUri("air_temperature"))
                        .label("Air Temperature")
                        .uomCode("Cel"))
                .addField("pressure", sweFactory.createQuantity()
                    .definition(SWEHelper.getCfUri("air_pressure"))
                    .label("Atmospheric Pressure")
                    .uomCode("hPa"))
                .addField("windSpeed", sweFactory.createQuantity()
                        .definition(SWEHelper.getCfUri("wind_speed"))
                        .label("Wind Speed")
                        .uomCode("m/s"))
                .addField("windDirection", sweFactory.createQuantity()
                        .definition(SWEHelper.getCfUri("wind_from_direction"))
                        .label("Wind Direction")
                        .uomCode("deg")
                        .refFrame(SWEConstants.REF_FRAME_NED, "z"))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        worker = new Thread(this, this.name);

        logger.debug("Initializing Output Complete");
    }

    /**
     * Begins processing data for output
     */
    public void doStart() {

        logger.info("Starting worker thread: {}", worker.getName());

        worker.start();
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

        return dataStruct;
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

    //reference values
    double tempRef = 20.0;
    double pressRef = 1013.0;
    double windSpeedRef = 5.0;
    double directionRef = 0.0;

    double temp = tempRef;
    double press = pressRef;
    double windSpeed = windSpeedRef;
    double windDir = directionRef;

    private Random rand = new Random();

    private double variation(double val, double ref, double dampingCoef, double noiseSigma) {
        return -dampingCoef*(val-ref) + noiseSigma * rand.nextGaussian();
    }

    @Override
    public void run() {

        boolean processSets = true;

        long lastSetTimeMillis = System.currentTimeMillis();

        try {

            while (processSets) {

                DataBlock dataBlock;
                if (latestRecord == null) {

                    dataBlock = dataStruct.createDataBlock();

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

                // TODO: Populate data block
                double time = System.currentTimeMillis() / 1000.0;

                temp += variation(temp, tempRef, 0.001, 0.1);
                press += variation(press, pressRef, 0.001, 0.1);

                windSpeed += variation(windSpeed, windSpeedRef, 0.001, 0.1);
                windSpeed = windSpeed < 0.0 ? 0.0 : windSpeed;

                windDir += 1.0 * (2.0 * Math.random() - 1.0);
                windDir = windDir < 0.0 ? windDir + 360.0 : windDir;
                windDir = windDir > 360.0 ? windDir-360.0 : windDir;

                parentSensor.getLogger().trace(String.format("temp=%5.2f, press=%4.2f, wind speed=%5.2f", temp, press, windSpeed, windDir));

                dataBlock.setDoubleValue(0, time);
                dataBlock.setDoubleValue(1, temp);
                dataBlock.setDoubleValue(2, press);
                dataBlock.setDoubleValue(3, windSpeed);
                dataBlock.setDoubleValue(4, windDir);

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publish(new DataEvent(latestRecordTime, D3sOutput.this, dataBlock));

                Thread.sleep(5000);

                synchronized (processingLock) {

                    processSets = !stopProcessing;
                }
            }

        } catch (Exception e) {

            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), stringWriter.toString());

        } finally {

            logger.debug("Terminating worker thread: {}", this.name);
        }
    }
}
