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

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.FieldType;
import org.sensorhub.api.security.SecurityModuleConfig;


public class BasicSessionConfig extends SecurityModuleConfig {

    @DisplayInfo(label = "Session cookie name", desc = "Name of the opaque authentication cookie.")
    public String cookieName = "OSCAR_SESSION";

    @DisplayInfo(label = "Idle timeout (seconds)", desc = "Invalidate a session after this many seconds without a request. Set to 0 to stay signed in until explicit logout.")
    public int idleTimeoutSeconds = 0;

    @DisplayInfo(label = "Absolute timeout (seconds)", desc = "Maximum session lifetime in seconds, even while active. Set to 0 to stay signed in until explicit logout.")
    public int absoluteTimeoutSeconds = 0;

    @DisplayInfo(label = "Persist sessions across restarts", desc = "Store SHA-256 session digests on disk so signed-in workstations remain logged in across server restarts and upgrades.")
    public boolean persistSessions = true;

    @DisplayInfo(label = "Allow Keep Me Signed In", desc = "Allow operators to keep their browser session cookie across browser restarts until explicit logout.")
    public boolean allowKeepSignedIn = true;

    @DisplayInfo(label = "Persistent cookie lifetime (seconds)", desc = "Browser cookie Max-Age used when Keep Me Signed In is active (default 365 days).")
    public int persistentCookieMaxAgeSeconds = 365 * 24 * 60 * 60;

    @DisplayInfo(label = "Secure cookie", desc = "Only send the session cookie over HTTPS. Keep enabled in production.")
    public boolean secureCookie = true;

    @DisplayInfo(label = "SameSite policy", desc = "Use Strict for the local node. None permits credentialed cross-site access to an external node and requires HTTPS.")
    public String sameSite = "Strict";

    @DisplayInfo(label = "Default redirect path", desc = "Local path used after login when there is no saved destination.")
    public String defaultRedirectPath = "/";

    @DisplayInfo(label = "WebAuthn Relying Party ID", desc = "Optional explicit domain for Passkeys (e.g. oscar.local). Leave blank to match the active HTTPS hostname.")
    public String webAuthnRpId = "";

    @DisplayInfo(label = "WebAuthn Relying Party Name", desc = "Display name shown in browser and hardware security key passkey prompts.")
    public String webAuthnRpName = "OSCAR";

    @DisplayInfo(label = "Enable Organization OIDC SSO", desc = "Enable optional OpenID Connect Single Sign-On alongside local offline accounts.")
    public boolean oidcEnabled = false;

    @DisplayInfo(label = "OIDC Provider Label", desc = "Button label displayed on the sign-in page (for example Organization SSO, Okta, Entra ID, or Keycloak).")
    public String oidcProviderName = "Organization SSO";

    @DisplayInfo(label = "OIDC Issuer URL", desc = "HTTPS OpenID Connect issuer URL hosting /.well-known/openid-configuration.")
    public String oidcIssuerUrl = "";

    @DisplayInfo(label = "OIDC Client ID", desc = "OpenID Connect client identifier registered for this OSCAR node.")
    public String oidcClientId = "";

    @FieldType(FieldType.Type.PASSWORD)
    @DisplayInfo(label = "OIDC Client Secret", desc = "Optional confidential client secret (leave blank for public PKCE clients).")
    public String oidcClientSecret = "";

    @DisplayInfo(label = "OIDC Scopes", desc = "Space-delimited OIDC scopes requested during login.")
    public String oidcScopes = "openid profile email groups";

    @DisplayInfo(label = "OIDC Administrator Group", desc = "Identity provider group or role claim value mapped to the OSCAR Administrator role.")
    public String oidcAdminGroup = "oscar-admins";

    @DisplayInfo(label = "OIDC Operator Group", desc = "Identity provider group or role claim value mapped to the OSCAR Operator role.")
    public String oidcOperatorGroup = "oscar-operators";

    @DisplayInfo(label = "OIDC Default Role", desc = "Fallback OSCAR role (viewer or operator) when an authenticated SSO user matches no group mapping.")
    public String oidcDefaultRole = "viewer";

    @DisplayInfo(label = "Enable Trusted Tunnel SSO", desc = "Accept verified user identity headers injected by an authenticated Zero Trust tunnel sidecar (such as Octelium).")
    public boolean trustedTunnelSsoEnabled = false;

    @DisplayInfo(label = "Trusted Tunnel Identity Header", desc = "HTTP header carrying the verified tunnel username.")
    public String trustedTunnelUserHeader = "X-Octelium-User";

    @FieldType(FieldType.Type.PASSWORD)
    @DisplayInfo(label = "Trusted Tunnel Shared Secret", desc = "Required secret header (X-Oscar-Tunnel-Secret, >= 16 characters) proving the request originated from the local tunnel sidecar.")
    public String trustedTunnelSharedSecret = "";

    @DisplayInfo(label = "Auto-Provision Tunnel Users as Viewer", desc = "Automatically create a read-only Viewer profile when a verified tunnel user has no existing local OSCAR account.")
    public boolean trustedTunnelAutoProvisionViewer = false;
}
