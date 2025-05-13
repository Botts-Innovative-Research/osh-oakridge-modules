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

package com.botts.ui.oscar.forms;

import org.sensorhub.ui.GenericConfigForm;
import org.sensorhub.ui.data.BaseProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Alex Almanza
 * @author Kalyn Stricklin
 * @since May 13 2025
 */
@SuppressWarnings("serial")
public class LaneConfigForm extends GenericConfigForm {

    private static final String LANE_CONFIG_PACKAGE = "com.botts.impl.system.lane.config.";
    private static final String PROP_RPM = "laneOptionsConfig.rpmConfig";
    private static final String PROP_CAMERA = "laneOptionsConfig.ffmpegConfig";

    @Override
    public Map<String, Class<?>> getPossibleTypes(String propId, BaseProperty<?> prop)
    {
        if (propId.equals(PROP_RPM))
        {
            Map<String, Class<?>> classList = new LinkedHashMap<>();
            try
            {
                // TODO: Make sure AspectRPMConfig is implemented/handled on main branch oakridge modules
                classList.put("Aspect", Class.forName(LANE_CONFIG_PACKAGE + "AspectRPMConfig"));
                classList.put("Rapiscan", Class.forName(LANE_CONFIG_PACKAGE + "RPMConfig"));
            }
            catch (ClassNotFoundException e)
            {
                getOshLogger().error("Cannot find RPM class", e);
            }
            return classList;
        }
        else if (propId.equals(PROP_CAMERA))
        {
            Map<String, Class<?>> classList = new LinkedHashMap<>();
            try
            {
                // TODO: Implement these classes as an extension of some generic camera config class
                classList.put("Sony", Class.forName(LANE_CONFIG_PACKAGE + "SonyCameraConfig"));
                classList.put("Axis", Class.forName(LANE_CONFIG_PACKAGE + "AxisCameraConfig"));
                classList.put("Custom", Class.forName(LANE_CONFIG_PACKAGE + "CustomCameraConfig"));
            }
            catch (ClassNotFoundException e)
            {
                getOshLogger().error("Cannot find Camera class", e);
            }
            return classList;
        }

        return super.getPossibleTypes(propId, prop);
    }
}
