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

import org.sensorhub.api.comm.ICommProvider;

import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;

import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;




/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author Cardy
 * @since 11/15/23
 */
public class WAPIRZ1Sensor extends AbstractSensorModule<WAPIRZ1Config> {

    private static final Logger logger = LoggerFactory.getLogger(WAPIRZ1Sensor.class);

    ICommProvider<?> commProvider;
    MotionOutput motionOutput;
    TemperatureOutput temperatureOutput;
    BatteryOutput batteryOutput;
    TamperAlarmOutput tamperAlarmOutput;
    LocationOutput locationOutput;
    InputStream msgIn;


    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("WAPIRZ-1 ...");

                SMLHelper smlWADWAZHelper = new SMLHelper();
                smlWADWAZHelper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlWADWAZHelper.identifiers.modelNumber("WAPIRZ-1"))
                        .addClassifier(smlWADWAZHelper.classifiers.sensorType(""));
            }
        }
    }

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("[urn:osh:sensor:wapirz-1]", config.serialNumber);
        generateXmlID("[WAPIRZ-1]", config.serialNumber);

        // Create and initialize output
        motionOutput = new MotionOutput(this);
        addOutput(motionOutput, false);
        motionOutput.doInit();

        tamperAlarmOutput = new TamperAlarmOutput(this);
        addOutput(tamperAlarmOutput, false);
        tamperAlarmOutput.doInit();

        batteryOutput = new BatteryOutput(this);
        addOutput(batteryOutput, false);
        batteryOutput.doInit();

        temperatureOutput = new TemperatureOutput(this);
        addOutput(temperatureOutput, false);
        temperatureOutput.doInit();

        locationOutput = new LocationOutput(this);
        addOutput(locationOutput, false);
        locationOutput.doInit();
    }

    @Override
    public void doStart() throws SensorHubException {

        ZWaveMessageHandler zWaveConnect = new ZWaveMessageHandler(motionOutput,
                tamperAlarmOutput, temperatureOutput, batteryOutput, locationOutput);

        zWaveConnect.ZWaveConnect("COM5", 115200);

        locationOutput.setLocationOutput(config.getLocation());

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

//        messageHandler.stopProcessing();
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


