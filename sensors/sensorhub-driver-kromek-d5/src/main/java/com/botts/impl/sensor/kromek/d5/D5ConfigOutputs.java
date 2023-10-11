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
    @DisplayInfo(label = "KromekSerialCompressionEnabledReport", desc = "Compression Enabled Report")
    public boolean enableKromekSerialCompressionEnabledReport = true;

    @DisplayInfo(label = "KromekSerialEthernetConfigReport", desc = "Ethernet Config Report")
    public boolean enableKromekSerialEthernetConfigReport = true;

    @DisplayInfo(label = "KromekSerialStatusReport", desc = "Status Report")
    public boolean enableKromekSerialStatusReport = true;
}
