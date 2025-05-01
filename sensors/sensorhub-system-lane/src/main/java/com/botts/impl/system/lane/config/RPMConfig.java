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

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration for automatic setup of RPM submodule
 *
 * @author Alex Almanza
 * @since April 24 2025
 */
public class RPMConfig {

    @DisplayInfo.Required
    @DisplayInfo(label = "RPM Type", desc = "RPM module to generate for this lane")
    public RPMType rpmType;

    @DisplayInfo.Required
    @DisplayInfo(label = "RPM Unique ID", desc = "RPM UID for new submodule if RPM type is specified. Please do not include osh UID prefix (i.e. urn:osh:sensor:rapiscan)")
    public String rpmUniqueId;

    @DisplayInfo.Required
    @DisplayInfo(label = "RPM Label", desc = "Friendly name for RPM module")
    public String rpmLabel;

    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.REMOTE_ADDRESS)
    @DisplayInfo(label = "Remote Host")
    public String remoteHost;

    @DisplayInfo.Required
    @DisplayInfo(label = "Remote Port")
    public int remotePort;

    // Username ?
    // Password ?

}
