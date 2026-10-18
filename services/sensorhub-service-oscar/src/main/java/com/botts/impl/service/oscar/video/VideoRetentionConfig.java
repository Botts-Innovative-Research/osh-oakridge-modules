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

    @DisplayInfo(label = "Age Before Five-Frame Retention (days)", desc = "Age in days before an occupancy source video is converted to exactly five retained JPEG images; the source MP4 remains through the configured rollback grace period.")
    public int timeToRetention = 7;

    @DisplayInfo(label = "Video Query Period (minutes)", desc = "Number of minutes between queries for occupancy video clips. " +
            "Larger values will result in more occupancies/video paths being loaded at once.")
    public int videoQueryPeriod = 1;

    @DisplayInfo(label = "Maximum Retention Catch-up (hours)", desc = "Maximum event-time span processed by one scheduled retention cycle. The durable cursor resumes remaining history in later cycles.")
    public int maximumCatchUpHours = 6;

    @DisplayInfo(label = "Late-arrival Overlap (hours)", desc = "Recent event-time interval rescanned for occupancies committed after the forward cursor passed them. Durable observation IDs prevent duplicate work.")
    public int lateArrivalOverlapHours = 24;

    @DisplayInfo(label = "Historical Reconciliation per Cycle (hours)", desc = "Older event-time span slowly rechecked per cycle to repair uncatalogued media without an unbounded scan.")
    public int reconciliationCatchUpHours = 24;

    @DisplayInfo(label = "Media Maintenance Work Limit", desc = "Maximum catalog-reconciliation, source-lifecycle, and retry entries inspected per scheduled cycle.")
    public int maintenanceWorkLimit = 200;

    @DisplayInfo(label = "Enable Frame Retention", desc = "Publish the exact-five JPEG retained-media set instead of using legacy whole-video deletion.")
    public boolean enableFrameRetention = true;

    @DisplayInfo(label = "Retained Frame Count", desc = "Schema v1 requires exactly five frames. \"Enable Frame Retention\" must be enabled.")
    public int frameRetentionCount = 5;

    @DisplayInfo(label = "Retained JPEG Quality", desc = "Baseline sRGB JPEG quality from 1 to 100. The default is 85.")
    public int jpegQuality = 85;

    @DisplayInfo(label = "Source Rollback Grace (hours)", desc = "Hours to preserve the source MP4 after all five JPEGs and their manifest are durably published.")
    public int sourceRollbackGraceHours = 24;

}
