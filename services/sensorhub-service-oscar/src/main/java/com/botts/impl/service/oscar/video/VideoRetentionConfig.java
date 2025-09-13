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

package com.botts.impl.service.oscar.video;

import org.sensorhub.api.config.DisplayInfo;

public class VideoRetentionConfig {

    @DisplayInfo(label = "Max Age (days)", desc = "Maximum time in days to store video clips")
    public int maxAge = 7;

    @DisplayInfo(label = "3-Frame Persistence", desc = "Enable to permanently save 3 frames from each video clip")
    public boolean use3FramePersistence = true;

}
