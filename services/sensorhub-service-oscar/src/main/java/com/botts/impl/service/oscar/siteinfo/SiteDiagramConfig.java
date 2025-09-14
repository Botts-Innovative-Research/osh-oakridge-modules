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

package com.botts.impl.service.oscar.siteinfo;

import org.sensorhub.api.config.DisplayInfo;

public class SiteDiagramConfig {

    @DisplayInfo(label = "Path to Site Diagram (.png/.jpg)", desc = "Site diagram to use for systems connected to this OSCAR node")
    public String siteDiagramPath;

    public static class LatLonLocation
    {
        @DisplayInfo(label="Latitude", desc="Geodetic latitude, in degrees")
        public double lat;

        @DisplayInfo(label="Longitude", desc="Longitude, in degrees")
        public double lon;
    }

    @DisplayInfo(desc = "Lower left point of site map (latitude, longitude)")
    public LatLonLocation siteLowerLeftBound;

    @DisplayInfo(desc = "Upper right point of site map (latitude, longitude)")
    public LatLonLocation siteUpperRightBound;

}
