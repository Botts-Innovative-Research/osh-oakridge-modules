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

package com.botts.ui.oscar.forms;

import org.sensorhub.ui.GenericConfigForm;
import org.sensorhub.ui.data.BaseProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Curated form for Mobile Detector System modules: only the detector type and
 * connection are exposed. The fixed-lane options inherited from the lane config
 * (cameras, vehicle OCR, fixed position, raw subsystem list) stay hidden.
 *
 * Register in the Admin UI module's customForms with configClass
 * com.botts.impl.system.lane.mobile.config.MobileDetectorConfig.
 *
 * @since July 2026
 */
@SuppressWarnings("serial")
public class MobileDetectorConfigForm extends GenericConfigForm {

    private static final String LANE_CONFIG_PACKAGE = "com.botts.impl.system.lane.config.";
    private static final String PROP_DETECTOR = "detectorConfig";

    private static final Set<String> HIDDEN_PROPS = Set.of(
            "laneOptionsConfig",   // superseded by detectorConfig
            "ocrConfig",           // vehicle OCR needs lane cameras
            "location",            // mobile detectors report their own GPS position
            "orientation",
            "subsystems",          // managed by the lane runtime
            "sensorML",
            "lastUpdated");

    @Override
    protected boolean isFieldVisible(String propId)
    {
        if (HIDDEN_PROPS.contains(propId))
            return false;
        return super.isFieldVisible(propId);
    }

    @Override
    public Map<String, Class<?>> getPossibleTypes(String propId, BaseProperty<?> prop)
    {
        if (propId.equals(PROP_DETECTOR))
        {
            Map<String, Class<?>> classList = new LinkedHashMap<>();
            try
            {
                classList.put("RS-350 Backpack", Class.forName(LANE_CONFIG_PACKAGE + "RS350RPMConfig"));
                classList.put("Kromek D5", Class.forName(LANE_CONFIG_PACKAGE + "D5RPMConfig"));
            }
            catch (ClassNotFoundException e)
            {
                getOshLogger().error("Cannot find detector config class", e);
            }
            return classList;
        }

        return super.getPossibleTypes(propId, prop);
    }
}
