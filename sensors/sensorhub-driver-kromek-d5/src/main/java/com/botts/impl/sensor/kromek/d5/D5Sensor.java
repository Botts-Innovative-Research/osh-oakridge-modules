/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.kromek.d5;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sensor driver for the Kromek D5 providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Michael Elmore
 * @since Oct. 2023
 */
public class D5Sensor extends AbstractSensorModule<D5Config> {

    private static final Logger logger = LoggerFactory.getLogger(D5Sensor.class);

    D5Output output;
    ICommProvider<?> commProvider;

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("[URN]", config.serialNumber);
        generateXmlID("[XML-PREFIX]", config.serialNumber);

        // Create and initialize output
        output = new D5Output(this);
        addOutput(output, false);
        output.doInit();
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
            }
        }
    }

    @Override
    public void doStop() throws SensorHubException {
        if (output != null) {
            output.doStop();
        }

        if (commProvider != null) {
            commProvider.stop();
            commProvider = null;
        }
    }

    @Override
    public boolean isConnected() {
        // Determine if sensor is connected
        return commProvider.isInitialized();
    }
}
