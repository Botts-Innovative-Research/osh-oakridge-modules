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

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration class for the Kromek D5 driver
 *
 * @author Michael Elmore
 * @since Oct. 2023
 */
public class D5ConfigOutputs {
    @DisplayInfo(label = "KromekDetectorRadiometricsV1Report", desc = "Detector Radiometrics V1 Report")
    public boolean enableKromekDetectorRadiometricsV1Report = true;

    @DisplayInfo(label = "KromekSerialRadiometricStatusReport", desc = "Radiometric Status Report")
    public boolean enableKromekSerialRadiometricStatusReport = true;

    @DisplayInfo(label = "KromekSerialCompressionEnabledReport", desc = "Compression Enabled Report")
    public boolean enableKromekSerialCompressionEnabledReport = false;

    @DisplayInfo(label = "KromekSerialEthernetConfigReport", desc = "Ethernet Config Report")
    public boolean enableKromekSerialEthernetConfigReport = false;

    @DisplayInfo(label = "KromekSerialStatusReport", desc = "Status Report")
    public boolean enableKromekSerialStatusReport = false;

    @DisplayInfo(label = "KromekSerialUnitIDReport", desc = "Unit ID Report")
    public boolean enableKromekSerialUnitIDReport = false;

    @DisplayInfo(label = "KromekSerialDoseInfoReport", desc = "Dose Info Report")
    public boolean enableKromekSerialDoseInfoReport = false;

    @DisplayInfo(label = "KromekSerialRemoteIsotopeConfirmationReport", desc = "Remote Isotope Confirmation Report")
    public boolean enableKromekSerialRemoteIsotopeConfirmationReport = false;

    @DisplayInfo(label = "KromekSerialRemoteIsotopeConfirmationStatusReport", desc = "Remote Isotope Confirmation Status Report")
    public boolean enableKromekSerialRemoteIsotopeConfirmationStatusReport = false;

    @DisplayInfo(label = "KromekSerialUTCReport", desc = "UTC Report")
    public boolean enableKromekSerialUTCReport = false;

    @DisplayInfo(label = "KromekSerialRemoteBackgroundStatusReport", desc = "Remote Background Status Report")
    public boolean enableKromekSerialRemoteBackgroundStatusReport = false;

    @DisplayInfo(label = "KromekSerialRemoteExtendedIsotopeConfirmationStatusReport", desc = "Remote Extended Isotope Confirmation Status Report")
    public boolean enableKromekSerialRemoteExtendedIsotopeConfirmationStatusReport = false;

    @DisplayInfo(label = "KromekSerialUIRadiationThresholdsReport", desc = "UI Radiation Thresholds Report")
    public boolean enableKromekSerialUIRadiationThresholdsReport = false;

    @DisplayInfo(label = "KromekSerialAboutReport", desc = "About Report")
    public boolean enableKromekSerialAboutReport = false;

    @DisplayInfo(label = "KromekSerialOTGReport", desc = "OTG Report")
    public boolean enableKromekSerialOTGReport = false;
}
