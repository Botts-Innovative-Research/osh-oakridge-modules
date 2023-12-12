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
import org.sensorhub.impl.module.ModuleRegistry;
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
public class D3sOutput extends AbstractSensorOutput<D3sSensor> implements Runnable {

    private static final String SENSOR_OUTPUT_NAME = "RADIATION";
    private static final String SENSOR_OUTPUT_LABEL = "[LABEL]";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "[DESCRIPTION]";

    private static final Logger log = LoggerFactory.getLogger(D3sOutput.class);

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
    D3sOutput(D3sSensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        log.debug("Output created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit() {

        log.debug("Initializing Output");

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

        log.debug("Initializing Output Complete");
    }

    /**
     * Begins processing data for output
     */
    public void doStart() {
        stopProcessing = false;
        worker = new Thread(this, this.name);
        log.info("Starting worker thread: {}", worker.getName());
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

    String latestSpectraFilename = null;
    String deviceSerialNum = new String("");
    String DEVICE_SERIAL_NUMBER_PREFIX = "SGM";
    Pattern SPECTRA_FILENAME_REGEX = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)_(\\d+)\\.(\\d+)\\.(\\d+)_"+DEVICE_SERIAL_NUMBER_PREFIX+"(\\d+)_Spectra\\.csv");


    @Override
    public void run() {

        D3sConfig config = parentSensor.getConfiguration();

        boolean processSets = true;

        long lastSetTimeMillis = System.currentTimeMillis();

        try {

            DataBlock dataBlock;
            int epochIndex = -1;
            long lastMeasurementEpoch = -1;

            while (processSets) {

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

                // read the all files in the data directory and determine the latest spectra .csv file

                synchronized (samplingLock) {

                    latestSpectraFilename = null;
                    File[] listOfFiles = null;
                    Calendar maxCalendar = null;    //calendar date of the latest file

                    File folder = new File(config.dataPath);
                    if (folder.exists()) {
                        listOfFiles = folder.listFiles();

                        for (int i = 0; i < listOfFiles.length; i++) {
                            if (listOfFiles[i].isFile()) {
                                String filename = listOfFiles[i].getName();
                                Matcher matcher = SPECTRA_FILENAME_REGEX.matcher(filename);
                                boolean matchFound = matcher.find();
                                if(matchFound) {
                                    Calendar cal = Calendar.getInstance();
                                    cal.set(Calendar.MONTH, Integer.parseInt(matcher.group(1)));
                                    cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(matcher.group(2)));
                                    cal.set(Calendar.YEAR, Integer.parseInt(matcher.group(3)));
                                    cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(matcher.group(4)));
                                    cal.set(Calendar.MINUTE, Integer.parseInt(matcher.group(5)));
                                    cal.set(Calendar.SECOND, Integer.parseInt(matcher.group(6)));

                                    /* if newer than current spectra .csv filename date*/
                                    if (maxCalendar == null || !cal.before(maxCalendar)) {

                                        //count lines of data in the file
                                        Integer lineCount = 0;
                                        String fullDataPath = config.dataPath+File.separator+filename;
                                        try (BufferedReader br = new BufferedReader(new FileReader(fullDataPath, StandardCharsets.UTF_8))) {
                                            while ((br.readLine()) != null) {
                                                lineCount++;
                                            }
                                        } catch (Exception e){
                                            log.error(e.toString());
                                        }

                                        /* if this is a valid data file with at least 4 lines... */
                                        if (lineCount >= 4) {
                                            /* set as the new latest file */
                                            maxCalendar = cal;
                                            latestSpectraFilename = filename;
                                            deviceSerialNum = DEVICE_SERIAL_NUMBER_PREFIX + matcher.group(6);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        log.error("dataPath '"+config.dataPath+"' does not exist");
                    }
                }

                /* read and output last (latest) row of radiation values from latest spectra file */
                if (latestSpectraFilename != null) {
                    String COMMA_DELIMITER = ",";
                    String fullDataPath = config.dataPath+File.separator+latestSpectraFilename;
                    try (BufferedReader br = new BufferedReader(new FileReader(fullDataPath, StandardCharsets.UTF_8))) {
                        String binRow = br.readLine();
                        String energyRow = br.readLine();
                        String initialDataRow = br.readLine();
                        String headerRow = br.readLine();
                        String line;
                        String lastLine = null;
                        while ((line = br.readLine()) != null) {
                            lastLine = line;
                        }
                        if ((lastLine != null) && lastLine.length()>0) {
                            String[] columns = lastLine.split(COMMA_DELIMITER);
                            int i = 0;  // datablock index
                            dataBlock.setStringValue(i++, latestSpectraFilename);

                            latestRecordTime = System.currentTimeMillis();
                            dataBlock.setLongValue(i++, latestRecordTime/1000); //samplingTime

                            int c = 0;  // column index
                            String timeString = columns[c++];
                            String dateString = columns[c++];
                            String dateTimeString = dateString + " " + timeString;
                            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
                            Date date = df.parse(dateTimeString);
                            long epoch = date.getTime()/1000;

                            epochIndex = i;
                            dataBlock.setLongValue(i++, epoch);  //phenomenonTime

                            dataBlock.setStringValue(i++, columns[c++]);  //detectionID
                            dataBlock.setDoubleValue(i++, Double.parseDouble(columns[c++]));  //confidence
                            c++; //UNUSED
                            c++; //UNUSED
                            dataBlock.setDoubleValue(i++, Double.parseDouble(columns[c++]));  //latitude
                            dataBlock.setDoubleValue(i++, Double.parseDouble(columns[c++]));  //longitude
                            dataBlock.setDoubleValue(i++, Double.parseDouble(columns[c++]));  //processingTime
                            dataBlock.setDoubleValue(i++, Double.parseDouble(columns[c++]));  //sensorTemp
                            dataBlock.setDoubleValue(i++, Double.parseDouble(columns[c++]));  //battery
                            dataBlock.setDoubleValue(i++, Double.parseDouble(columns[c++]));  //liveTime
                            dataBlock.setIntValue(i++, Integer.parseInt(columns[c++]));  //neutronCount

                            //dose
                            Double dose = Double.parseDouble(columns[c++]);
                            String doseUnit = columns[c++];
                            if (doseUnit.endsWith("Sv/h")) {
                                dataBlock.setDoubleValue(i++, dose);  //dose
                            } else {
                                dataBlock.setDoubleValue(i++, -999);
                                log.error("ERROR: unknown dose unit (please set to µSv/h)");
                            }

                        }
                    } catch (Exception e){
                        log.error(e.toString());
                    }

                    ++setCount;
                    latestRecord = dataBlock;
                    if ((epochIndex >= 0) && (dataBlock != null)) {
                        long measurementEpoch = dataBlock.getLongValue(epochIndex);
                        if (measurementEpoch != lastMeasurementEpoch) {
                            lastMeasurementEpoch = measurementEpoch;
                            eventHandler.publish(new DataEvent(latestRecordTime, D3sOutput.this, dataBlock));
                        }
                    }
                }

                Thread.sleep(1000);

                synchronized (processingLock) {
                    processSets = !stopProcessing;
                }
            }

        } catch (Exception e) {

            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            log.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), stringWriter.toString());

        } finally {

            log.debug("Terminating worker thread: {}", this.name);
        }
    }
}
