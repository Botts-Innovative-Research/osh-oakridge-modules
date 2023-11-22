/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.zw100;

import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.api.sensor.SensorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author cardy
 * @since 11/16/23
 */
public class ZW100Sensor extends AbstractSensorModule<ZW100Config> {

    private static final Logger logger = LoggerFactory.getLogger(ZW100Sensor.class);

    ICommProvider<?> commProvider;
//    MessageHandler messageHandler;

    MotionOutput motionOutput;

    RelativeHumidityOutput relativeHumidityOutput;

    TemperatureOutput temperatureOutput;

    LuminanceOutput luminanceOutput;

    UltravioletOutput ultravioletOutput;
    TamperAlarmOutput tamperAlarmOutput;

    BatteryOutput batteryOutput;

    LocationOutput locationOutput;
    InputStream msgIn;


    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("ZW100-Multisensor-6 utilizes z-wave technology to monitor motion, " +
                        "temperature, relative humidity, UV index, luminance, and system vibration (used as a tamper " +
                        "alarm)");

                SMLHelper smlWADWAZHelper = new SMLHelper();
                smlWADWAZHelper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlWADWAZHelper.identifiers.modelNumber("ZW100-Mutlisensor-6"))
                        .addClassifier(smlWADWAZHelper.classifiers.sensorType(""));
            }
        }
    }
    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:zw100", config.serialNumber);
        generateXmlID("ZW100", config.serialNumber);

        // Create and initialize output
        motionOutput = new MotionOutput(this);
        addOutput(motionOutput, false);
        motionOutput.doInit();

        relativeHumidityOutput = new RelativeHumidityOutput(this);
        addOutput(relativeHumidityOutput, false);
        relativeHumidityOutput.doInit();

        temperatureOutput = new TemperatureOutput(this);
        addOutput(temperatureOutput, false);
        temperatureOutput.doInit();

        luminanceOutput = new LuminanceOutput(this);
        addOutput(luminanceOutput, false);
        luminanceOutput.doInit();

        ultravioletOutput = new UltravioletOutput(this);
        addOutput(ultravioletOutput, false);
        ultravioletOutput.doInit();

        tamperAlarmOutput = new TamperAlarmOutput(this);
        addOutput(tamperAlarmOutput, false);
        tamperAlarmOutput.doInit();

        batteryOutput = new BatteryOutput(this);
        addOutput(batteryOutput, false);
        batteryOutput.doInit();

        locationOutput = new LocationOuput(this);
        addOutput(locationOutput, false);
        locationOutput.doInit();

    }

    public void doStart() throws SensorHubException {
//        locationOutput.setLocationOuput(config.getLocation());
//
        // init comm provider
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

                throw e;
            }
        }

        // connect to data stream
        try {

            msgIn = new BufferedInputStream(commProvider.getInputStream());

//                messageHandler = new MessageHandler(msgIn, gammaOutput, neutronOutput, occupancyOutput);

//            csvMsgRead.readMessages(msgIn, gammaOutput, neutronOutput, occupancyOutput);

        } catch (IOException e) {

            throw new SensorException("Error while initializing communications ", e);
        }
    }


    @Override
    public void doStop() throws SensorHubException {

        if (commProvider != null) {

            try {

                commProvider.stop();

            } catch (Exception e) {

                logger.error("Uncaught exception attempting to stop comms module", e);

            } finally {

                commProvider = null;
            }
        }

        messageHandler.stopProcessing();
    }

    @Override
    public boolean isConnected() {
        if (commProvider == null) {

            return false;

        } else {

            return commProvider.isStarted();
        }
    }
}


