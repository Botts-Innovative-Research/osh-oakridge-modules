/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.rapiscan;

import com.botts.impl.sensor.rapiscan.RapiscanConfig;
import com.botts.impl.sensor.rapiscan.RapiscanSensor;
import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.Boolean;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sensorhub.impl.utils.rad.RADHelper;

/**
 * Output specification and provider for ...
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class RapiscanOutput extends AbstractSensorOutput<RapiscanSensor> {

    private static final String SENSOR_OUTPUT_NAME = "RADIATION";
    private static final String SENSOR_OUTPUT_LABEL = "[LABEL]";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "[DESCRIPTION]";

    private static final Logger logger = LoggerFactory.getLogger(RapiscanOutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    private final Object samplingLock = new Object();

    Thread worker;

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    RapiscanOutput(RapiscanSensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Output created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit() {

        logger.debug("Initializing Output");

        RADHelper rad = new RADHelper();

        // TODO: Create data record description
        dataStruct = rad.createRecord()
                .name(getName())
                .definition(RADHelper.getRadUri("kromek-d3s"))
                .description("Radiation measurements")
                .addField("latestFile", rad.createText()
                        .label("Latest File"))
                .addField("samplingTime", rad.createTime().asSamplingTimeIsoUTC())
                .addField("phenomenonTime", rad.createTime().asPhenomenonTimeIsoUTC())
                .addField("detectionID",
                        rad.createText()
                                .label("Detection ID")
                                .definition(RADHelper.getRadUri("detection-id")))
                .addField("confidence",
                        rad.createQuantity()
                                .label("Confidence")
                                .definition(RADHelper.getRadUri("confidence")))
                .addField("location", rad.createLocationVectorLatLon())
                .addField("processingTime",
                        rad.createQuantity()
                                .definition(RADHelper.getRadUri("processing-time"))
                                .uomCode("ms"))
                .addField("sensorTemp",
                        rad.createQuantity()
                                .label("Sensor Temp")
                                .definition(RADHelper.getRadUri("sensor-temp"))
                                .uomCode("Cel"))
                .addField("batteryCharge", rad.createBatteryCharge())
                .addField("liveTime",
                        rad.createQuantity()
                                .label("Live Time")
                                .definition(RADHelper.getRadUri("live-time"))
                                .uomCode("ms"))
                .addField("neutronCount", rad.createNeutronGrossCount())
                .addField("dose", rad.createDoseUSVh())
                .build();

        dataEncoding = rad.newTextEncoding(",", "\n");

        logger.debug("Initializing Output Complete");
    }

    /**
     * Begins processing data for output
     */
    public void doStart() {

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

    String latestSpectraFilename = null;
    String deviceSerialNum = new String("");
    String DEVICE_SERIAL_NUMBER_PREFIX = "SGM";
    Pattern SPECTRA_FILENAME_REGEX = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)_(\\d+)\\.(\\d+)\\.(\\d+)_"+DEVICE_SERIAL_NUMBER_PREFIX+"(\\d+)_Spectra\\.csv");



}
