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

package com.botts.impl.service.bucket;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.security.SecurityConfig;
import org.sensorhub.api.service.HttpServiceConfig;

import java.util.Set;

public class BucketStorageServiceConfig extends HttpServiceConfig {


    @DisplayInfo(desc="Security related options")
    public SecurityConfig security = new SecurityConfig();

    @DisplayInfo(label = "Enable CORS")
    public boolean enableCORS = true;

    @DisplayInfo(desc = "Use this to initialize creation of buckets")
    public Set<String> buckets;

    public BucketStorageServiceConfig()
    {
        this.moduleClass = BucketStorageService.class.getCanonicalName();
        this.endPoint = "/buckets";
    }

}
