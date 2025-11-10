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

    @DisplayInfo(label = "Time to Keyframe Retention/Deletion (days)", desc = "Time in days until video data is either decimated or deleted.")
    public int timeToRetention = 7;

    @DisplayInfo(label = "Video Query Period (minutes)", desc = "Number of minutes between queries for occupancy video clips. " +
            "Larger values will result in more occupancies/video paths being loaded at once.")
    public int videoQueryPeriod = 1;

    @DisplayInfo(label = "Enable Frame Retention", desc = "Instead of deleting the entire clip, all but a certain number of frames will be removed.")
    public boolean enableFrameRetention = true;

    @DisplayInfo(label = "Keyframe Retention Count", desc = "Number of frames of video to preserve. \"Enable Frame Retention\" must be enabled.")
    public int frameRetentionCount = 5;

}