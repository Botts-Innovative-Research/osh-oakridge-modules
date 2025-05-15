/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.
 ******************************************************************************/

package com.botts.impl.system.lane.config;


import com.botts.impl.sensor.aspect.comm.ModbusTCPConfig;
import org.sensorhub.api.config.DisplayInfo;


/**
 * Configuration for automatic setup of RPM submodule
 *
 * @author Kalyn Stricklin
 * @since May 2025
 */
public class AspectRPMConfig extends RPMConfig{

    // aspect specific
    @DisplayInfo(label = "Find device within address range")
    @DisplayInfo.Required
    public ModbusTCPConfig.AddressRange addressRange = new ModbusTCPConfig.AddressRange();

    public static class AddressRange {
        @DisplayInfo(label = "From")
        @DisplayInfo.Required
        public int from = 1;

        @DisplayInfo(label = "To")
        @DisplayInfo.Required
        public int to = 32;
    }
}
