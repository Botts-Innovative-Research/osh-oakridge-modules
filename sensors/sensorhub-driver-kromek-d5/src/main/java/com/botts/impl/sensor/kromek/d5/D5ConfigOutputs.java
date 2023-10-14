package com.botts.impl.sensor.kromek.d5;
/*
 *  The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file, You can obtain one
 *  at http://mozilla.org/MPL/2.0/.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the License.
 *
 *  Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 */

import org.sensorhub.api.config.DisplayInfo;

public class D5ConfigOutputs {
    @DisplayInfo(label = "KromekDetectorRadiometricsV1Report", desc = "Detector Radiometrics V1 Report")
    public boolean enableKromekDetectorRadiometricsV1Report = true;

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
}
