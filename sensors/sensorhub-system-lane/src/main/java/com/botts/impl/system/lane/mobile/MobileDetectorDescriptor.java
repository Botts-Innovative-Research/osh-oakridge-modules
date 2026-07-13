/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.
 ******************************************************************************/

package com.botts.impl.system.lane.mobile;

import com.botts.impl.system.lane.mobile.config.MobileDetectorConfig;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

/**
 * @since July 2026
 */
public class MobileDetectorDescriptor extends JarModuleProvider implements IModuleProvider {

    @Override
    public String getModuleName() { return "Mobile Detector System"; }

    @Override
    public String getModuleDescription() {
        return "Deploys a mobile radiation detector (RS-350 backpack or Kromek D5) as a trackable system. " +
                "Use this instead of the raw RS-350/D5 drivers or a Lane System.";
    }

    @Override
    public Class<? extends IModule<?>> getModuleClass() {
        return MobileDetectorSystem.class;
    }

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass() {
        return MobileDetectorConfig.class;
    }
}
