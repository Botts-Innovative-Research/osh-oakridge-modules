/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.wadwaz1;

import com.botts.sensorhub.impl.zwave.comms.ZwaveCommService;
import com.botts.sensorhub.impl.zwave.comms.IMessageListener;


import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.sensor.SensorException;

import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;


/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author Cardy
 * @since 11/15/23
 */
public class WADWAZ1Sensor extends AbstractSensorModule<WADWAZ1Config> {

    private static final Logger logger = LoggerFactory.getLogger(WADWAZ1Sensor.class);

    private ZwaveCommService commService;
    EntryAlarmOutput entryAlarmOutput;
    BatteryOutput batteryOutput;
    TamperAlarmOutput tamperAlarmOutput;
    ExternalSwitchAlarmOutput externalSwitchAlarmOutput;
    LocationOutput locationOutput;
    InputStream msgIn;


    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("WADWAZ-1 window and door sensor for monitoring open/closed " +
                        "status, device tampering, and optional external switch alarm status");

                SMLHelper smlWADWAZHelper = new SMLHelper();
                smlWADWAZHelper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlWADWAZHelper.identifiers.modelNumber("WADWAZ-1"))
                        .addClassifier(smlWADWAZHelper.classifiers.sensorType(""));
            }
        }
    }

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("[urn:osh:sensor:wadwaz-1]", config.serialNumber);
        generateXmlID("[WADWAZ-1]", config.serialNumber);

        initAsync = true;

//        logger = LoggerFactory.getLogger(WADWAZ1Sensor.class);
//        checkConfig();
//        uniqueID = config.urnId;

        ModuleRegistry moduleRegistry = getParentHub().getModuleRegistry();

        commService = moduleRegistry.getModuleByType(ZwaveCommService.class);

        if (commService == null) {

            throw new SensorHubException("CommService needs to be configured");

        } else {

            moduleRegistry.waitForModule(commService.getLocalID(), ModuleEvent.ModuleState.STARTED)
                    .thenRun(() -> commService.registerListener((IEventListener) this));

            CompletableFuture.runAsync(() -> {
                        try {

                            moduleRegistry.initModule("[urn:osh:sensor:wadwaz-1]");

                        } catch (SensorException e) {

                            throw new CompletionException(e);
                        } catch (SensorHubException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .thenRun(() -> setState(ModuleEvent.ModuleState.INITIALIZED))
                    .exceptionally(err -> {

                        reportError(err.getMessage(), err.getCause());

                        setState(ModuleEvent.ModuleState.LOADED);

                        return null;
                    });
        }

        // Create and initialize output
        entryAlarmOutput = new EntryAlarmOutput(this);
        addOutput(entryAlarmOutput, false);
        entryAlarmOutput.doInit();

        tamperAlarmOutput = new TamperAlarmOutput(this);
        addOutput(tamperAlarmOutput, false);
        tamperAlarmOutput.doInit();

        batteryOutput = new BatteryOutput(this);
        addOutput(batteryOutput, false);
        batteryOutput.doInit();

        externalSwitchAlarmOutput = new ExternalSwitchAlarmOutput(this);
        addOutput(externalSwitchAlarmOutput, false);
        externalSwitchAlarmOutput.doInit();

        locationOutput = new LocationOutput(this);
        addOutput(locationOutput, false);
        locationOutput.doInit();
    }


    @Override
    public void doStart() throws SensorHubException {

//        ZWaveMessageHandler zWaveConnect = new ZWaveMessageHandler(entryAlarmOutput,
//                tamperAlarmOutput, externalSwitchAlarmOutput, batteryOutput, locationOutput);
//
//        zWaveConnect.ZWaveConnect("COM5", 115200);



        locationOutput.setLocationOutput(config.getLocation());

        }


    @Override
    public void doStop() throws SensorHubException {

        if (commService != null) {

            try {

                commService.stop();

            } catch (Exception e) {

                logger.error("Uncaught exception attempting to stop comms module", e);

            } finally {

                commService = null;
            }
        }

//        messageHandler.stopProcessing();
    }

    @Override
    public boolean isConnected() {
        if (commService == null) {

            return false;

        } else {

            return commService.isStarted();
        }
    }
}

