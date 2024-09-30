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

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

public class ProximityConfig extends SensorConfig {
    @DisplayInfo.Required
    @DisplayInfo(label = "Serial Number", desc = "Serial number or unique identifier.")
    public String serialNumber = "sensor001";

    @DisplayInfo(label = "NC Pin", desc = "GPIO pin on the Raspberry Pi. Connects to the NC (Normally Closed) pin of the proximity switch (pin 2, white).")
    public GpioEnum configPinNC = GpioEnum.PIN_17;

    @DisplayInfo(label = "NO Pin", desc = "GPIO pin on the Raspberry Pi. Connects to the NO (Normally Open) pin of the proximity switch (pin 4, black).")
    public GpioEnum configPinNO = GpioEnum.PIN_UNSET;
}
