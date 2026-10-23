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
    public int timeToRetention = 0;

    @DisplayInfo(label = "Video Query Period (minutes)", desc = "Number of minutes between queries for occupancy video clips. " +
            "Larger values will result in more occupancies/video paths being loaded at once.")
    public int videoQueryPeriod = 5;

    @DisplayInfo(label = "Maximum Retention Catch-up (hours)", desc = "Maximum event-time span processed by one scheduled retention cycle. The durable cursor resumes remaining history in later cycles.")
    public int maximumCatchUpHours = 6;

    @DisplayInfo(label = "Late-arrival Allowance (hours)", desc = "Time allowed for a late commit before the contiguous delayed cursor finalizes an occupancy timestamp. Older late commits are recovered by full-history reconciliation.")
    public int lateArrivalOverlapHours = 24;

    @DisplayInfo(label = "Late-arrival Sweep Period (minutes)", desc = "Minimum time between checks for a newer delayed safe boundary. Interrupted frozen sweeps resume immediately from their durable cursor.")
    public int lateArrivalScanPeriodMinutes = 360;

    @DisplayInfo(label = "Late-arrival Catch-up per Cycle (hours)", desc = "Maximum event-time span of an active late-arrival sweep processed by one scheduled cycle.")
    public int lateArrivalCatchUpHours = 2;

    @DisplayInfo(label = "Historical Reconciliation per Cycle (hours)", desc = "Maximum event-time span processed per scheduler cycle while an active full-history sweep advances toward its frozen end.")
    public int reconciliationCatchUpHours = 24;

    @DisplayInfo(label = "Historical Reconciliation Period (hours)", desc = "Delay after a complete frozen full-history sweep before starting again at the earliest eligible occupancy. Active and failed sweeps resume on the next cycle.")
    public int reconciliationPeriodHours = 24;

    @DisplayInfo(label = "Occupancy Stream Refresh Period (minutes)", desc = "How often the retention scanner refreshes its cached occupancy datastream IDs. Observation queries use these IDs directly instead of repeatedly resolving output names.")
    public int occupancyStreamRefreshMinutes = 15;

    @DisplayInfo(label = "Media Maintenance Work Limit", desc = "Maximum catalog-reconciliation, source-lifecycle, and retry entries inspected per scheduled cycle.")
    public int maintenanceWorkLimit = 200;

    @DisplayInfo(label = "Media Metadata Reclamation Work Limit", desc = "Maximum direct-addressed UTC processed-observation hour buckets reclaimed per scheduled cycle, including missing hours. Each hour uses twelve fixed five-minute probes; the default catches up as many as 200 backlog hours without a directory scan.")
    public int metadataReclamationWorkLimit = 200;

    @DisplayInfo(label = "Enable Frame Retention", desc = "Publish the exact-five JPEG retained-media set instead of using legacy whole-video deletion.")
    public boolean enableFrameRetention = true;

    @DisplayInfo(label = "Retained Frame Count", desc = "Schema v1 requires exactly five frames. \"Enable Frame Retention\" must be enabled.")
    public int frameRetentionCount = 5;

    @DisplayInfo(label = "Retained JPEG Quality", desc = "Baseline sRGB JPEG quality from 1 to 100. The default is 85.")
    public int jpegQuality = 85;

    @DisplayInfo(label = "Source Rollback Grace (hours)", desc = "Hours to preserve the source MP4 after all five JPEGs and their manifest are durably published.")
    public int sourceRollbackGraceHours = 24;

}
