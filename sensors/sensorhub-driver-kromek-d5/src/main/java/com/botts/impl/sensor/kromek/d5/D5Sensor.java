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
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Sensor driver for the Kromek D5 providing sensor description, output registration,
 * initialization and shutdown of the driver and outputs.
 *
 * @author Michael Elmore
 * @since Oct. 2023
 */
public class D5Sensor extends AbstractSensorModule<D5Config> {
    private static final Logger logger = LoggerFactory.getLogger(D5Sensor.class);

    // Map of report classes to their associated output instances
    HashMap<Class<?>, D5Output> outputs;
    ICommProvider<?> commProvider;
    Boolean processLock;
    D5MessageRouter messageRouter;

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
        if (commProvider == null) {
            // we need to recreate comm provider here because it can be changed by UI
            try {
                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");

                var moduleReg = getParentHub().getModuleRegistry();
                commProvider = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);
                commProvider.start();
            } catch (Exception e) {
                commProvider = null;
                throw new SensorException("Error while initializing communications ", e);
            }
        }

        // connect to data stream
        try {
            messageRouter = new D5MessageRouter(this, commProvider.getInputStream(), commProvider.getOutputStream());
            messageRouter.start();
        } catch (Exception e) {
            throw new SensorException("Error while initializing communications ", e);
        }

        processLock = false;
    }

    @Override
    public void doStop() throws SensorHubException {
        logger.info("Stopping sensor");
        processLock = true;

        for (D5Output output : outputs.values()) {
            output.doStop();
        }

        if (commProvider != null) {
            commProvider.stop();
            commProvider = null;
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
        // Create and initialize outputs
        if (config.outputs.enableKromekDetectorRadiometricsV1Report) {
            D5Output output = new D5Output(KromekDetectorRadiometricsV1Report.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekDetectorRadiometricsV1Report());
            outputs.put(KromekDetectorRadiometricsV1Report.class, output);
        }
        if (config.outputs.enableKromekSerialRadiometricStatusReport) {
            D5Output output = new D5Output(KromekSerialRadiometricStatusReport.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekSerialRadiometricStatusReport());
            outputs.put(KromekSerialRadiometricStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialCompressionEnabledReport) {
            D5Output output = new D5Output(KromekSerialCompressionEnabledReport.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekSerialCompressionEnabledReport());
            outputs.put(KromekSerialCompressionEnabledReport.class, output);
        }
        if (config.outputs.enableKromekSerialEthernetConfigReport) {
            D5Output output = new D5Output(KromekSerialEthernetConfigReport.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekSerialEthernetConfigReport());
            outputs.put(KromekSerialEthernetConfigReport.class, output);
        }
        if (config.outputs.enableKromekSerialStatusReport) {
            D5Output output = new D5Output(KromekSerialStatusReport.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekSerialStatusReport());
            outputs.put(KromekSerialStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialUnitIDReport) {
            D5Output output = new D5Output(KromekSerialUnitIDReport.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekSerialUnitIDReport());
            outputs.put(KromekSerialUnitIDReport.class, output);
        }
        if (config.outputs.enableKromekSerialDoseInfoReport) {
            D5Output output = new D5Output(KromekSerialDoseInfoReport.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekSerialDoseInfoReport());
            outputs.put(KromekSerialDoseInfoReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteIsotopeConfirmationReport) {
            D5Output output = new D5Output(KromekSerialRemoteIsotopeConfirmationReport.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekSerialRemoteIsotopeConfirmationReport());
            outputs.put(KromekSerialRemoteIsotopeConfirmationReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteIsotopeConfirmationStatusReport) {
            D5Output output = new D5Output(KromekSerialRemoteIsotopeConfirmationStatusReport.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekSerialRemoteIsotopeConfirmationStatusReport());
            outputs.put(KromekSerialRemoteIsotopeConfirmationStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialUTCReport) {
            D5Output output = new D5Output(KromekSerialUTCReport.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekSerialUTCReport());
            outputs.put(KromekSerialUTCReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteBackgroundStatusReport) {
            D5Output output = new D5Output(KromekSerialRemoteBackgroundStatusReport.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekSerialRemoteBackgroundStatusReport());
            outputs.put(KromekSerialRemoteBackgroundStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialRemoteExtendedIsotopeConfirmationStatusReport) {
            D5Output output = new D5Output(KromekSerialRemoteExtendedIsotopeConfirmationStatusReport.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekSerialRemoteExtendedIsotopeConfirmationStatusReport());
            outputs.put(KromekSerialRemoteExtendedIsotopeConfirmationStatusReport.class, output);
        }
        if (config.outputs.enableKromekSerialUIRadiationThresholdsReport) {
            D5Output output = new D5Output(KromekSerialUIRadiationThresholdsReport.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekSerialUIRadiationThresholdsReport());
            outputs.put(KromekSerialUIRadiationThresholdsReport.class, output);
        }
        if (config.outputs.enableKromekSerialAboutReport) {
            D5Output output = new D5Output(KromekSerialAboutReport.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekSerialAboutReport());
            outputs.put(KromekSerialAboutReport.class, output);
        }
        if (config.outputs.enableKromekSerialOTGReport) {
            D5Output output = new D5Output(KromekSerialOTGReport.getReportName(), this);
            addOutput(output, false);
            output.doInit(new KromekSerialOTGReport());
            outputs.put(KromekSerialOTGReport.class, output);
        }
    }
}
