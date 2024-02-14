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

import net.opengis.sensorml.v20.PhysicalSystem;
import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEventListener;
import org.openhab.binding.zwave.internal.protocol.ZWaveIoHandler;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveAlarmCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveMultiLevelSensorCommandClass;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveInitializationStateEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveNodeStatusEvent;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.UARTConfig;
import org.sensorhub.impl.comm.rxtx.RxtxSerialCommProvider;
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

    MotionOutput motionOutput;

    RelativeHumidityOutput relativeHumidityOutput;

    TemperatureOutput temperatureOutput;

    LuminanceOutput luminanceOutput;

    UltravioletOutput ultravioletOutput;

    TamperAlarmOutput tamperAlarmOutput;

    BatteryOutput batteryOutput;

    LocationOutput locationOutput;

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
                        .addIdentifier(smlWADWAZHelper.identifiers.modelNumber("ZW100-Multisensor-6"))
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

        locationOutput = new LocationOutput(this);
        addOutput(locationOutput, false);
        locationOutput.doInit();

    }

    @Override
    public void doStart() throws SensorHubException {

        locationOutput.setLocationOutput(config.getLocation());

        ZWaveMessageHandler zWaveConnect = new ZWaveMessageHandler(motionOutput,
                relativeHumidityOutput, temperatureOutput, luminanceOutput, ultravioletOutput, tamperAlarmOutput, batteryOutput, locationOutput);

        zWaveConnect.ZWaveConnect("COM5", 115200);
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


