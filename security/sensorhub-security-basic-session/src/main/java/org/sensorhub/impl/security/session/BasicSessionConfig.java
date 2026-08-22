/*******************************************************************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is GeoRobotix Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.

 ******************************************************************************/

package org.sensorhub.impl.security.session;

import org.sensorhub.api.security.SecurityModuleConfig;
import org.sensorhub.api.config.DisplayInfo;


public class BasicSessionConfig extends SecurityModuleConfig {

    @DisplayInfo(label="Session cookie name", desc="Name of the opaque authentication cookie.")
    public String cookieName = "OSCAR_SESSION";

    @DisplayInfo(label="Idle timeout (seconds)", desc="Invalidate a session after this many seconds without a request.")
    public int idleTimeoutSeconds = 30 * 60;

    @DisplayInfo(label="Absolute timeout (seconds)", desc="Maximum session lifetime, even while active.")
    public int absoluteTimeoutSeconds = 8 * 60 * 60;

    @DisplayInfo(label="Secure cookie", desc="Only send the session cookie over HTTPS. Keep enabled in production.")
    public boolean secureCookie = true;

    @DisplayInfo(label="SameSite policy", desc="Use Strict for the local node. None permits credentialed cross-site access to an external node and requires HTTPS.")
    public String sameSite = "Strict";
}
