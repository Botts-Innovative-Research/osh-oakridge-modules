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
package com.botts.impl.sensor.kromek.d5;

import com.botts.impl.sensor.kromek.d5.reports.SerialReport;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Output specification and provider for R5Sensor driver.
 *
 * @author Michael Elmore
 * @since Oct. 2023
 */
public class D5Output extends AbstractSensorOutput<D5Sensor> {
    public static final Logger logger = LoggerFactory.getLogger(D5Output.class);
    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    private final int samplingPeriod;

    private DataRecord dataRecord;
    private DataEncoding dataEncoding;
    long lastSetTimeMillis = System.currentTimeMillis();
    private int setCount = 0;

    /**
     * Constructor for the output.
     *
     * @param parentSensor Sensor driver providing this output
     */
    D5Output(D5Sensor parentSensor, String outputName, int samplingPeriod) {
        super(outputName, parentSensor);
        this.samplingPeriod = samplingPeriod;

        logger.debug("Output {} created", outputName);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit(SerialReport data) {
        logger.debug("Initializing output");

        dataRecord = data.createDataRecord();

        dataEncoding = new SWEHelper().newTextEncoding(",", "\n");

        logger.debug("Initializing output Complete");
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
        return samplingPeriod;
    }

    public void setData(SerialReport data) {
        try {
            DataBlock dataBlock;
            if (latestRecord == null) {
                dataBlock = dataRecord.createDataBlock();
            } else {
                dataBlock = latestRecord.renew();
            }

            synchronized (histogramLock) {
                int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;

                // Get a sampling time for the latest set based on the previously set sampling time
                timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;

                // Set the latest sampling time to now
                lastSetTimeMillis = timingHistogram[setIndex];
            }

            ++setCount;

            double timestamp = System.currentTimeMillis() / 1000d;

            data.setDataBlock(dataBlock, timestamp);

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();

            eventHandler.publish(new DataEvent(latestRecordTime, D5Output.this, dataBlock));
        } catch (Exception e) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), stringWriter);
        }
    }
}
