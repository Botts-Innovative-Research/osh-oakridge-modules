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
import com.botts.impl.service.oscar.video.VideoRetentionConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.database.IObsSystemDatabaseModule;
import org.sensorhub.api.service.ServiceConfig;

public class OSCARServiceConfig extends ServiceConfig {

    public String spreadsheetConfigPath;

    public SiteDiagramConfig siteDiagramConfig;

    public VideoRetentionConfig videoRetentionConfig;

    @DisplayInfo.Required
    @DisplayInfo(label = "Node ID", desc = "Unique identifier of this OSCAR node")
    public String nodeId;

    @DisplayInfo.Required
    @DisplayInfo.ModuleType(IObsSystemDatabaseModule.class)
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.MODULE_ID)
    @DisplayInfo(desc = "Database connected to this OSCAR service")
    public String databaseID;

}
