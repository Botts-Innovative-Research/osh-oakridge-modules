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

package com.botts.impl.service.oscar;

import com.botts.impl.service.oscar.siteinfo.SiteDiagramConfig;
import com.botts.impl.service.oscar.retention.StoragePressureRetentionConfig;
import com.botts.impl.service.oscar.video.VideoRetentionConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.database.IObsSystemDatabaseModule;
import org.sensorhub.api.service.ServiceConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OSCARServiceConfig extends ServiceConfig {

    @DisplayInfo(label = "Custom Disposition Codes", desc = "Additional adjudication codes. Values must be unique integers greater than 11; codes 0-11 are built in.")
    public List<AdjudicationCodeConfig> customAdjudicationCodes = new ArrayList<>();

    /** Validates the configured codes and returns their numeric values. */
    public Set<Integer> getCustomAdjudicationCodeValues() {
        Set<Integer> values = new HashSet<>();
        if (customAdjudicationCodes == null)
            return values;

        for (var entry : customAdjudicationCodes) {
            if (entry == null || entry.code <= 11)
                throw new IllegalArgumentException("Custom disposition codes must be greater than 11; codes 0-11 are reserved.");
            if (entry.label == null || entry.label.isBlank())
                throw new IllegalArgumentException("Custom disposition code " + entry.code + " must have a label.");
            if (!values.add(entry.code))
                throw new IllegalArgumentException("Duplicate custom disposition code: " + entry.code);
        }
        return values;
    }

    public String spreadsheetConfigPath;

    public SiteDiagramConfig siteDiagramConfig;

    public VideoRetentionConfig videoRetentionConfig;

    public StoragePressureRetentionConfig storagePressureRetentionConfig;

    @DisplayInfo.Required
    @DisplayInfo(label = "Node ID", desc = "Unique identifier of this OSCAR node")
    public String nodeId;

    @DisplayInfo.ModuleType(IObsSystemDatabaseModule.class)
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.MODULE_ID)
    @DisplayInfo(desc = "Database connected to this OSCAR service")
    public String databaseID;

    @DisplayInfo(label = "Stats Frequency (min)", desc = "Frequency at which statistics will be published")
    public int statsFrequencyMinutes = 60;

    @DisplayInfo(label = "WebID API Root", desc = "Base URL of the Sandia Full Spectrum Web ID API")
    public String webIdApiRoot;

    public OSCARServiceConfig() {
        this.webIdApiRoot = "https://full-spectrum.sandia.gov/api/v1";
    }

}
