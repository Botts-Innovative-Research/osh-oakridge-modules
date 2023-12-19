/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.aspect;


import com.botts.impl.sensor.aspect.comm.IModbusTCPCommProvider;
import com.botts.impl.sensor.aspect.output.*;
import com.botts.impl.sensor.aspect.registers.DeviceDescriptionRegisters;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sensor driver for the Aspect sensor providing sensor description, output registration,
 * initialization and shutdown of the driver and outputs.
 *
 * @author Michael Elmore
 * @since December 2023
 */
public class AspectSensor extends AbstractSensorModule<AspectConfig> {
    private static final Logger log = LoggerFactory.getLogger(AspectSensor.class);
    IModbusTCPCommProvider<?> commProvider;
    MessageHandler messageHandler;
    GammaOutput gammaOutput;
    NeutronOutput neutronOutput;
    OccupancyOutput occupancyOutput;
    SpeedOutput speedOutput;
    SensorLocationOutput sensorLocationOutput;

    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:aspect:", config.serialNumber);
        generateXmlID("Aspect", config.serialNumber);

        // Initialize outputs
        gammaOutput = new GammaOutput(this);
        addOutput(gammaOutput, false);
        gammaOutput.init();

        neutronOutput = new NeutronOutput(this);
        addOutput(neutronOutput, false);
        neutronOutput.init();

        occupancyOutput = new OccupancyOutput(this);
        addOutput(occupancyOutput, false);
        occupancyOutput.init();

        speedOutput = new SpeedOutput(this);
        addOutput(speedOutput, false);
        speedOutput.init();

        sensorLocationOutput = new SensorLocationOutput(this);
        addOutput(sensorLocationOutput, false);
        sensorLocationOutput.init();
    }

    @Override
    protected void doStart() throws SensorHubException {
        sensorLocationOutput.setLocationOutput(config.getLocation());

        // Initialize comm provider
        if (commProvider == null) {
            // We need to recreate comm provider here because it can be changed by UI
            try {
                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");

                var moduleReg = getParentHub().getModuleRegistry();

                var commModule = moduleReg.loadSubModule(config.commSettings, true);
                if (!(commModule instanceof IModbusTCPCommProvider<?>))
                    throw new SensorHubException("Please select the Modbus TCP communication module.");

                commProvider = (IModbusTCPCommProvider<?>) commModule;
                commProvider.start();
                var connection = commProvider.getConnection();
                var deviceDescriptionRegisters = new DeviceDescriptionRegisters(connection);
                deviceDescriptionRegisters.readRegisters(1);
            } catch (Exception e) {
                commProvider = null;
                throw new SensorHubException("Error while initializing communications ", e);
            }
        }

        // Start message handler
        messageHandler = new MessageHandler(commProvider.getConnection(), gammaOutput, neutronOutput, occupancyOutput, speedOutput);
        messageHandler.start();
    }

    @Override
    public void doStop() {
        if (commProvider != null) {
            try {
                commProvider.stop();
            } catch (Exception e) {
                log.error("Uncaught exception attempting to stop comm module", e);
            } finally {
                commProvider = null;
            }
        }

        if (messageHandler != null) {
            messageHandler.stop();
            messageHandler = null;
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
