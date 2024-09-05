/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one
 * at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * Copyright (c) 2024 Botts Innovative Research, Inc. All Rights Reserved.
 */
package com.botts.impl.sensor.proximity;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;

import static com.botts.impl.sensor.proximity.GpioEnum.PIN_UNSET;

public class ProximitySensor extends AbstractSensorModule<ProximityConfig> {
    private static final String URN_PREFIX = "urn:osh:sensor:proximity:";
    private static final String XML_ID_PREFIX = "PROXIMITY_";

    private final GpioController gpio = GpioFactory.getInstance();

    private ProximityOutput output;
    private GpioPinDigitalInput inputPin;

    /**
     * Sets up the GPIO pins for input and attaches listeners to them.
     */
    private void setupGpioPins() {
        GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING));

        // Since each pin always has the opposite state of the other, we only need to set up one of them.
        if (config.configPinNC != PIN_UNSET) {
            setupGpioPin(config.configPinNC.getValue(), true);
        } else {
            setupGpioPin(config.configPinNO.getValue(), false);
        }
    }

    /**
     * Sets up a GPIO pin for input and attaches a listener to it.
     * The listener updates the sensor output based on the pin state.
     * This method also updates the output immediately with the initial value of the pin.
     *
     * @param pinAddress The GPIO pin address.
     * @param isNC       Indicates if the pin is Normally Closed (NC).
     */
    private void setupGpioPin(int pinAddress, boolean isNC) {
        Pin pin = RaspiPin.getPinByAddress(pinAddress);
        inputPin = gpio.provisionDigitalInputPin(pin);
        inputPin.addListener((GpioPinListenerDigital) event -> output.setData(isNC ? event.getState().isHigh() : event.getState().isLow()));
        output.setData(isNC ? inputPin.isHigh() : inputPin.isLow());
    }

    /**
     * Initializes the sensor output.
     */
    private void initializeOutput() {
        output = new ProximityOutput(this);
        addOutput(output, false);
    }

    /**
     * Removes the listeners and shuts down the GPIO pins.
     */
    private void shutdownGpioPins() {
        if (inputPin != null) {
            inputPin.removeAllListeners();
            gpio.unprovisionPin(inputPin);
            inputPin = null;
        }
    }

    /**
     * Validates the configuration.
     *
     * @throws SensorHubException if the configuration is invalid
     */
    private void validateConfig() throws SensorHubException {
        if (config.configPinNC == PIN_UNSET && config.configPinNO == PIN_UNSET) {
            throw new SensorHubException("GPIO pins not set in configuration.");
        }
    }

    @Override
    public void doInit() throws SensorHubException {
        generateUniqueID(URN_PREFIX, config.serialNumber);
        generateXmlID(XML_ID_PREFIX, config.serialNumber);

        validateConfig();
        initializeOutput();
    }

    @Override
    public void doStart() throws SensorHubException {
        validateConfig();
        setupGpioPins();
    }

    @Override
    public void doStop() {
        shutdownGpioPins();
    }

    @Override
    public boolean isConnected() {
        return inputPin != null;
    }
}
