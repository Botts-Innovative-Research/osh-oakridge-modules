/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.system.lane;

import com.botts.impl.system.lane.config.LaneConfig;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

/**
 * @author Alex Almanza
 * @since March 2025
 */
public class Descriptor extends JarModuleProvider implements IModuleProvider {

    @Override
    public String getModuleName() { return "Lane System"; }

    @Override
    public String getModuleDescription() {
        return "Specialized Sensor System module to be used as parent system for RPM and Video drivers in a lane.";
    }

    @Override
    public Class<? extends IModule<?>> getModuleClass() {
        return LaneSystem.class;
    }

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass() {
        return LaneConfig.class;
    }
}
