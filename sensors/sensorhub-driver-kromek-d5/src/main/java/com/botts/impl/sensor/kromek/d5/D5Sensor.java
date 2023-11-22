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

import com.botts.impl.sensor.kromek.d5.reports.*;
import com.fazecast.jSerialComm.SerialPort;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static com.botts.impl.sensor.kromek.d5.Shared.findSerialPort;

/**
 * Sensor driver for the Kromek D5 providing sensor description, output registration,
 * initialization and shutdown of the driver and outputs.
 *
 * @author Michael Elmore
 * @since Oct. 2023
 */
public class D5Sensor extends AbstractSensorModule<D5Config> {
    private static final Logger log = LoggerFactory.getLogger(D5Sensor.class);

    // Map of report classes to their associated output instances
    HashMap<Class<?>, D5Output> outputs;
    Boolean processLock;
    D5MessageRouter messageRouter;

    ICommProvider<?> commProvider;
    SerialPort commPort;

    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Generate identifiers
        generateUniqueID("[URN]", config.serialNumber);
        generateXmlID("[XML-PREFIX]", config.serialNumber);

        // Create and initialize output(s)
        outputs = new HashMap<>();
        createOutputs();
    }

    @Override
    public void doStart() throws SensorHubException {
        boolean connected = false;

        // Try connecting to the sensor via the comm provider
        if (config.commSettings != null && commProvider == null) {
            // We need to recreate comm provider here because it can be changed by UI
            try {
                log.info("Trying to connect to sensor via comm provider.");
                var moduleReg = getParentHub().getModuleRegistry();
                commProvider = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);
                commProvider.start();

                if (commProvider.isStarted()) {
                    // Connect to data stream
                    messageRouter = new D5MessageRouter(this, commProvider.getInputStream(), commProvider.getOutputStream());
                    messageRouter.start();
                    connected = true;

                    log.info("Connected to sensor via comm provider.");
                } else {
                    log.info("Failed to connect to sensor via comm provider.");
                }
            } catch (Exception e) {
                commProvider = null;
                throw new SensorException("Error while initializing communications ", e);
            }
        }

        // Try establishing a connection to the sensor via USB if we haven't already connected
        if (!connected) {
            log.info("Trying to connect to sensor via USB.");
            String comPortName = findSerialPort();
            if (comPortName == null) {
                log.info("No serial port found.");
            } else {
                commPort = SerialPort.getCommPort(comPortName);
                commPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
                commPort.openPort();

                // Connect to data stream
                messageRouter = new D5MessageRouter(this, commPort.getInputStream(), commPort.getOutputStream());
                messageRouter.start();
                connected = true;
            }
        }

        if (connected)
            processLock = false;
        else {
            processLock = true;
            throw new SensorException("Failed to connect to sensor.");
        }
    }

    @Override
    public void doStop() throws SensorHubException {
        log.info("Stopping sensor");
        processLock = true;

        if (commProvider != null) {
            commProvider.stop();
            commProvider = null;
        }
        if (commPort != null) {
            commPort.closePort();
            commPort = null;
        }
        if (messageRouter != null) {
            messageRouter.stop();
            messageRouter = null;
        }
    }

    @Override
    public boolean isConnected() {
        // Determine if the sensor is connected
        return commProvider.isInitialized();
    }

    void createOutputs() {
        if (config.outputs.enableKromekDetectorRadiometricsV1Report) {
            KromekDetectorRadiometricsV1Report report = new KromekDetectorRadiometricsV1Report();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekDetectorRadiometricsV1Report.class, output);
        }
        if (config.outputs.enableKromekSerialRadiometricStatusReport) {
            KromekSerialRadiometricStatusReport report = new KromekSerialRadiometricStatusReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialRadiometricStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialCompressionEnabledReport) {
            KromekSerialCompressionEnabledReport report = new KromekSerialCompressionEnabledReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialCompressionEnabledReport.class, output);
        }
        if (config.outputs.enableKromekSerialEthernetConfigReport) {
            KromekSerialEthernetConfigReport report = new KromekSerialEthernetConfigReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialEthernetConfigReport.class, output);
        }
        if (config.outputs.enableKromekSerialStatusReport) {
            KromekSerialStatusReport report = new KromekSerialStatusReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialUnitIDReport) {
            KromekSerialUnitIDReport report = new KromekSerialUnitIDReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialUnitIDReport.class, output);
        }
        if (config.outputs.enableKromekSerialDoseInfoReport) {
            KromekSerialDoseInfoReport report = new KromekSerialDoseInfoReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialDoseInfoReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteIsotopeConfirmationReport) {
            KromekSerialRemoteIsotopeConfirmationReport report = new KromekSerialRemoteIsotopeConfirmationReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialRemoteIsotopeConfirmationReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteIsotopeConfirmationStatusReport) {
            KromekSerialRemoteIsotopeConfirmationStatusReport report = new KromekSerialRemoteIsotopeConfirmationStatusReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialRemoteIsotopeConfirmationStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialUTCReport) {
            KromekSerialUTCReport report = new KromekSerialUTCReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialUTCReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteBackgroundStatusReport) {
            KromekSerialRemoteBackgroundStatusReport report = new KromekSerialRemoteBackgroundStatusReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialRemoteBackgroundStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteExtendedIsotopeConfirmationStatusReport) {
            KromekSerialRemoteExtendedIsotopeConfirmationStatusReport report = new KromekSerialRemoteExtendedIsotopeConfirmationStatusReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialRemoteExtendedIsotopeConfirmationStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialUIRadiationThresholdsReport) {
            KromekSerialUIRadiationThresholdsReport report = new KromekSerialUIRadiationThresholdsReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialUIRadiationThresholdsReport.class, output);
        }
        if (config.outputs.enableKromekSerialAboutReport) {
            KromekSerialAboutReport report = new KromekSerialAboutReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialAboutReport.class, output);
        }
        if (config.outputs.enableKromekSerialOTGReport) {
            KromekSerialOTGReport report = new KromekSerialOTGReport();
            D5Output output = new D5Output(this, report.getReportName(), report.getPollingRate());
            addOutput(output, false);
            output.doInit(report);
            outputs.put(KromekSerialOTGReport.class, output);
        }
    }
}
