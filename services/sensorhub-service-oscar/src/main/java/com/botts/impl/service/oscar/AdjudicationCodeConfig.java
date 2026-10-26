/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is GeoRobotix Innovative Research, LLC. Portions created by the Initial
 Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package com.botts.impl.service.oscar;

import org.sensorhub.api.config.DisplayInfo;

public class AdjudicationCodeConfig {
    @DisplayInfo.Required
    @DisplayInfo(label = "Code", desc = "Unique adjudication code greater than 11")
    @DisplayInfo.ValueRange(min = 12)
    public int code = 12;

    @DisplayInfo.Required
    @DisplayInfo(label = "Label", desc = "Description of this disposition code")
    public String label;
}
