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

import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.SpatialFrame;
import net.opengis.sensorml.v20.impl.SpatialFrameImpl;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

/**
 * Sensor driver for the ... providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class D3sSensor extends AbstractSensorModule<D3sConfig> {

    private static final Logger logger = LoggerFactory.getLogger(D3sSensor.class);

    D3sOutput output;

    Object syncTimeLock = new Object();

    @Override
    protected void updateSensorDescription() {
        synchronized (sensorDescLock) {
            super.updateSensorDescription();
            if (!sensorDescription.isSetDescription()) {
                sensorDescription.setDescription("OpenSensorHub node for Kromek D3S Radiation Sensor");

                SMLHelper smlHelper = new SMLHelper();
                smlHelper.edit((PhysicalSystem)sensorDescription)
                        .addIdentifier(smlHelper.identifiers.serialNumber("1234567890"))
                        .addClassifier(smlHelper.classifiers.sensorType("Kromek D3S Radiation Sensor"))
                        .addCharacteristicList("operating_specs", smlHelper.characteristics.operatingCharacteristics()
                                .add("voltage", smlHelper.characteristics.operatingVoltageRange(3.3, 5, "V"))
                                .add("temperature", smlHelper.conditions.temperatureRange(-10.,75.,"Cel")))
                        .addCapabilityList("capabilities", smlHelper.capabilities.systemCapabilities()
                                .add("update_rate", smlHelper.capabilities.reportingFrequency(1.0)))
                                ;

                SpatialFrame localRefFrame = new SpatialFrameImpl();
                localRefFrame.setId("LOCAL_FRAME");
                localRefFrame.setOrigin("Center of the Kinect Device facet containing apertures for emitter and sensors");
                localRefFrame.addAxis("x", "The X axis is in the plane of the facet containing the apertures for emitter and sensors and apoints to the right");
                localRefFrame.addAxis("y", "The Y Axis is in the plane of the facet containing the aperture for emitter and sensors and points up");
                localRefFrame.addAxis("z", "The Z Axis points outward from the facet containing the aperture");
                ((PhysicalSystem) sensorDescription).addLocalReferenceFrame(localRefFrame);

            }
        }
    }

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:kromek:d3s", config.serialNumber);
        generateXmlID("KROMEK_D3S", config.serialNumber);

        // Create and initialize output
        output = new D3sOutput(this);

        addOutput(output, false);

        output.doInit();

        // TODO: Perform other initialization
    }

    @Override
    public void doStart() throws SensorHubException {

        if (null != output) {

            // Allocate necessary resources and start outputs
            output.doStart();;
        }

        // TODO: Perform other startup procedures
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != output) {

            output.doStop();
        }

        // TODO: Perform other shutdown procedures
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return output.isAlive();
    }
}
