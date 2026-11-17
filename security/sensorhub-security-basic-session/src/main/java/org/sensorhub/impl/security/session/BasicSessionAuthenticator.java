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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.api.security.IUserInfo;
import org.sensorhub.impl.security.session.OscarIdentityStore.BulkEnrolledUser;
import org.sensorhub.impl.security.session.OscarIdentityStore.BulkEnrollmentOutcome;
import org.sensorhub.impl.security.session.OscarIdentityStore.BulkRowError;
import org.sensorhub.impl.security.session.OscarIdentityStore.BulkUserSpec;
import org.sensorhub.impl.security.session.OscarIdentityStore.CreatedFederationToken;
import org.sensorhub.impl.security.session.OscarIdentityStore.CreatedOrResetUserResult;
import org.sensorhub.impl.security.session.OscarIdentityStore.EnrollmentInviteRecord;
import org.sensorhub.impl.security.session.OscarIdentityStore.FederationTokenRecord;
import org.sensorhub.impl.security.session.OscarIdentityStore.IssuedEnrollmentInvite;
import org.sensorhub.impl.security.session.OscarIdentityStore.PasskeyMatch;
import org.sensorhub.impl.security.session.OscarIdentityStore.PasskeyRecord;
import org.sensorhub.impl.security.session.OscarIdentityStore.PendingChallenge;
import org.sensorhub.impl.security.session.OscarIdentityStore.PersistedSession;
import org.sensorhub.impl.security.session.OscarIdentityStore.UserProfileRecord;
import org.slf4j.Logger;

/**
 * Multi-method OSCAR session authenticator supporting:
 * <ul>
 *   <li>Disk-backed opaque sessions ({@code OSCAR_SESSION}) with optional {@code 0}-timeout "stay signed in until logout"</li>
 *   <li>Local PBKDF2 username + password sign-in (offline &amp; phone-free facility ready) with rate limiting</li>
 *   <li>Built-in FIDO2 / WebAuthn Passkeys &amp; Hardware Security Keys (Conditional UI + modal + self-service CRUD)</li>
 *   <li>Optional OpenID Connect (OIDC + PKCE) corporate SSO with automatic local offline fallback</li>
 *   <li>Optional verified Zero Trust tunnel header SSO ({@code X-Octelium-User} + shared secret)</li>
 *   <li>Scoped {@code Bearer oscar_fed_...} federation service tokens for remote nodes</li>
 *   <li>Self-service account/passkey APIs and Administrator Users &amp; Access REST management</li>
 * </ul>
 */
public class BasicSessionAuthenticator extends LoginAuthenticator {
    private static final String AUTH_METHOD = "BASIC+SESSION";
    private static final String LOGIN_PATH = "/login";
    private static final String LOGIN_JS_PATH = "/login.js";
    private static final String LOGIN_WEBAUTHN_OPTIONS_PATH = "/login/webauthn/options";
    private static final String LOGIN_WEBAUTHN_VERIFY_PATH = "/login/webauthn/verify";
    private static final String LOGIN_OIDC_PATH = "/login/oidc";
    private static final String LOGIN_OIDC_CALLBACK_PATH = "/login/oidc/callback";
    private static final String LOGIN_PASSWORD_CHANGE_PATH = "/login/password-change";
    private static final String LOGIN_ENROLL_PATH = "/login/enroll";
    private static final String LOGIN_ENROLL_PASSWORD_PATH = "/login/enroll/password";
    private static final String LOGIN_ENROLL_PASSKEY_OPTIONS_PATH = "/login/enroll/passkey/options";
    private static final String LOGIN_ENROLL_PASSKEY_VERIFY_PATH = "/login/enroll/passkey/verify";
    private static final String LOGOUT_PATH = "/logout";
    private static final String SESSION_PATH = "/session";
    private static final String SENSORHUB_SESSION_PATH = "/sensorhub/session";
    private static final String LANGUAGE_COOKIE_NAME = "osh-language";
    private static final int LANGUAGE_COOKIE_MAX_AGE = 365 * 24 * 60 * 60;
    private static final int MAX_AUTH_HEADER_LENGTH = 16 * 1024;
    private static final int MAX_JSON_BODY_BYTES = 64 * 1024;
    private static final long PRUNE_INTERVAL_MILLIS = 60_000L;
    private static final String LOGIN_CSP =
            "default-src 'none'; style-src 'unsafe-inline'; script-src 'self'; connect-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'none'; form-action 'self'";

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final byte[] LOGIN_JS_BYTES = loadLoginJsResource();

    private final Logger log;
    private final ISensorHub hub;
    private final ISecurityManager securityManager;
    private final BasicSessionConfig config;
    private final OscarIdentityStore identityStore;
    private final OidcClientHelper oidcClient;
    private final String cookieName;
    private final boolean secureCookie;
    private final String sameSite;
    private final String defaultRedirectPath;

    private volatile long idleTimeoutMillis;
    private volatile long absoluteTimeoutMillis;
    private volatile boolean allowKeepSignedIn;
    private volatile int persistentCookieMaxAgeSeconds;
    private volatile long nextPruneAt;

    public BasicSessionAuthenticator(BasicSessionConfig config, ISecurityManager securityManager, Logger log) {
        this(config, null, securityManager, null, log);
    }

    public BasicSessionAuthenticator(BasicSessionConfig config,
                                     ISensorHub hub,
                                     ISecurityManager securityManager,
                                     Path identityStoreFile,
                                     Logger log) {
        this(config, hub, securityManager, identityStoreFile, new OidcClientHelper(), log);
    }

    BasicSessionAuthenticator(BasicSessionConfig config,
                              ISensorHub hub,
                              ISecurityManager securityManager,
                              Path identityStoreFile,
                              OidcClientHelper oidcClient,
                              Logger log) {
        if (config.cookieName == null || !config.cookieName.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+"))
            throw new IllegalArgumentException("Invalid session cookie name");
        if (config.idleTimeoutSeconds < 0 || config.absoluteTimeoutSeconds < 0)
            throw new IllegalArgumentException("Session timeouts must be zero or positive");
        if (config.sameSite == null || !(config.sameSite.equalsIgnoreCase("Strict")
                || config.sameSite.equalsIgnoreCase("Lax") || config.sameSite.equalsIgnoreCase("None")))
            throw new IllegalArgumentException("SameSite must be Strict, Lax, or None");
        if (config.sameSite.equalsIgnoreCase("None") && !config.secureCookie)
            throw new IllegalArgumentException("SameSite=None requires a secure cookie");
        if (!isSafeRedirectTarget(config.defaultRedirectPath))
            throw new IllegalArgumentException("Default redirect path must be a local absolute path");

        this.config = config;
        this.hub = hub;
        this.securityManager = securityManager;
        this.log = log;
        this.oidcClient = oidcClient;
        this.cookieName = config.cookieName;
        this.idleTimeoutMillis = Math.multiplyExact((long) config.idleTimeoutSeconds, 1000L);
        this.absoluteTimeoutMillis = Math.multiplyExact((long) config.absoluteTimeoutSeconds, 1000L);
        this.allowKeepSignedIn = config.allowKeepSignedIn;
        this.persistentCookieMaxAgeSeconds = config.persistentCookieMaxAgeSeconds > 0
                ? config.persistentCookieMaxAgeSeconds
                : 365 * 24 * 60 * 60;
        this.secureCookie = config.secureCookie;
        this.sameSite = Character.toUpperCase(config.sameSite.charAt(0))
                + config.sameSite.substring(1).toLowerCase(Locale.ROOT);
        this.defaultRedirectPath = config.defaultRedirectPath;
        this.identityStore = new OscarIdentityStore(
                hub,
                securityManager,
                identityStoreFile,
                config.persistSessions && identityStoreFile != null,
                log);
    }

    private static byte[] loadLoginJsResource() {
        try (InputStream in = BasicSessionAuthenticator.class.getResourceAsStream("login.js")) {
            if (in == null)
                return new byte[0];
            return in.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    public OscarIdentityStore getIdentityStore() {
        return identityStore;
    }

    @Override
    public String getAuthMethod() {
        return AUTH_METHOD;
    }

    @Override
    public void prepareRequest(ServletRequest request) {
    }

    @Override
    public boolean secureResponse(ServletRequest request, ServletResponse response,
                                  boolean mandatory, Authentication.User validatedUser) {
        return true;
    }

    @Override
    public Authentication validateRequest(ServletRequest req, ServletResponse resp, boolean mandatory)
            throws ServerAuthException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        try {
            pruneExpiredSessions();
            String path = normalizedRequestPath(request);

            if (LOGOUT_PATH.equals(path)) {
                logout(request, response);
                return Authentication.SEND_CONTINUE;
            }

            if (LOGIN_JS_PATH.equals(path)) {
                serveLoginScript(request, response);
                return Authentication.SEND_CONTINUE;
            }

            if (LOGIN_WEBAUTHN_OPTIONS_PATH.equals(path)) {
                handleLoginWebAuthnOptions(request, response);
                return Authentication.SEND_CONTINUE;
            }

            if (LOGIN_WEBAUTHN_VERIFY_PATH.equals(path)) {
                handleLoginWebAuthnVerify(request, response);
                return Authentication.SEND_CONTINUE;
            }

            if (LOGIN_OIDC_PATH.equals(path)) {
                handleOidcStart(request, response);
                return Authentication.SEND_CONTINUE;
            }

            if (LOGIN_OIDC_CALLBACK_PATH.equals(path)) {
                handleOidcCallback(request, response);
                return Authentication.SEND_CONTINUE;
            }

            if (LOGIN_ENROLL_PATH.equals(path)) {
                handleLoginEnrollPage(request, response);
                return Authentication.SEND_CONTINUE;
            }

            if (LOGIN_ENROLL_PASSWORD_PATH.equals(path)) {
                handleLoginEnrollPassword(request, response);
                return Authentication.SEND_CONTINUE;
            }

            if (LOGIN_ENROLL_PASSKEY_OPTIONS_PATH.equals(path)) {
                handleLoginEnrollPasskeyOptions(request, response);
                return Authentication.SEND_CONTINUE;
            }

            if (LOGIN_ENROLL_PASSKEY_VERIFY_PATH.equals(path)) {
                handleLoginEnrollPasskeyVerify(request, response);
                return Authentication.SEND_CONTINUE;
            }

            if (LOGIN_PATH.equals(path)) {
                handleLogin(request, response);
                return Authentication.SEND_CONTINUE;
            }

            // 1. Existing opaque session cookie
            PersistedSession activeSession = resolveActiveSession(request, response);
            if (activeSession != null) {
                String username = activeSession.username;
                if (LOGIN_PASSWORD_CHANGE_PATH.equals(path)) {
                    handleForcedPasswordChangePage(request, response, username);
                    return Authentication.SEND_CONTINUE;
                }

                String sessionSubpath = extractSessionApiSubpath(path);
                if (sessionSubpath != null) {
                    handleSessionApi(request, response, activeSession, sessionSubpath);
                    return Authentication.SEND_CONTINUE;
                }

                UserProfileRecord profile = identityStore.getProfile(username);
                if (profile != null && profile.mustChangePassword) {
                    if (acceptsHtml(request) && "GET".equalsIgnoreCase(request.getMethod())) {
                        String target = buildContinueFromRequest(request);
                        String changeUrl = LOGIN_PASSWORD_CHANGE_PATH + "?continue="
                                + URLEncoder.encode(redirectTarget(target), StandardCharsets.UTF_8);
                        response.sendRedirect(response.encodeRedirectURL(changeUrl));
                        return Authentication.SEND_CONTINUE;
                    }
                    sendJsonError(response, HttpServletResponse.SC_FORBIDDEN,
                            "Password change required before accessing OSCAR resources.");
                    return Authentication.SEND_CONTINUE;
                }

                return new UserAuthentication(AUTH_METHOD, withoutCredentials(username));
            }

            // 2. Trusted Zero Trust Tunnel Identity Header SSO (Octelium)
            Authentication tunnelAuth = authenticateTrustedTunnel(request, response);
            if (tunnelAuth != null) {
                String sessionSubpath = extractSessionApiSubpath(path);
                if (sessionSubpath != null) {
                    PersistedSession created = resolveActiveSession(request, response);
                    if (created != null) {
                        handleSessionApi(request, response, created, sessionSubpath);
                        return Authentication.SEND_CONTINUE;
                    }
                }
                return tunnelAuth;
            }

            // 3. Stateless Federation Service Token (Authorization: Bearer oscar_fed_...)
            Authentication bearerAuth = authenticateBearerToken(request, response);
            if (bearerAuth != null) {
                return bearerAuth;
            }

            // 4. HTTP Basic Authentication (API clients / legacy scripts)
            Authentication basicAuthentication = authenticateBasic(request, response);
            if (basicAuthentication != null)
                return basicAuthentication;

            if (!mandatory)
                return Authentication.NOT_CHECKED;

            if (acceptsHtml(request) && "GET".equalsIgnoreCase(request.getMethod())) {
                redirectToLogin(request, response);
                return Authentication.SEND_CONTINUE;
            }

            response.setHeader("WWW-Authenticate", "Basic realm=\"OpenSensorHub\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return Authentication.SEND_CONTINUE;
        } catch (IOException e) {
            log.error("Cannot complete authentication response", e);
            return Authentication.SEND_FAILURE;
        }
    }

    private PersistedSession resolveActiveSession(HttpServletRequest request, HttpServletResponse response) {
        String token = readSessionCookie(request);
        if (token == null)
            return null;
        PersistedSession session = identityStore.validateAndTouchSession(
                token, idleTimeoutMillis, absoluteTimeoutMillis);
        if (session == null) {
            expireCookie(response);
            return null;
        }
        return session;
    }

    private Authentication authenticateBearerToken(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7))
            return null;
        if (authHeader.length() > MAX_AUTH_HEADER_LENGTH) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Authorization header is too large");
            return Authentication.SEND_FAILURE;
        }
        String rawToken = authHeader.substring(7).trim();
        FederationTokenRecord fed = identityStore.authenticateFederationToken(rawToken);
        if (fed == null) {
            log.warn("Rejected invalid or expired Federation Bearer token from {}", clientIp(request));
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return Authentication.SEND_FAILURE;
        }
        if (readSessionCookie(request) == null) {
            createSession(request, response, fed.serviceUserId, allowKeepSignedIn);
        }
        return new UserAuthentication(AUTH_METHOD, withoutCredentials(fed.serviceUserId));
    }

    private Authentication authenticateTrustedTunnel(HttpServletRequest request, HttpServletResponse response) {
        if (!config.trustedTunnelSsoEnabled)
            return null;
        String sharedSecret = config.trustedTunnelSharedSecret;
        if (sharedSecret == null || sharedSecret.length() < 16)
            return null;

        String providedSecret = request.getHeader("X-Oscar-Tunnel-Secret");
        if (providedSecret == null || !MessageDigest.isEqual(
                sharedSecret.getBytes(StandardCharsets.UTF_8),
                providedSecret.getBytes(StandardCharsets.UTF_8))) {
            return null;
        }

        String headerName = (config.trustedTunnelUserHeader == null || config.trustedTunnelUserHeader.isBlank())
                ? "X-Octelium-User"
                : config.trustedTunnelUserHeader.trim();
        String rawTunnelUser = request.getHeader(headerName);
        if (rawTunnelUser == null || rawTunnelUser.isBlank())
            return null;

        String username = OidcClientHelper.sanitizeOidcUsername(rawTunnelUser);
        if (securityManager.getUserInfo(username) == null) {
            if (!config.trustedTunnelAutoProvisionViewer)
                return null;
            identityStore.provisionOrSyncExternalUser(
                    username, rawTunnelUser.trim(), OscarIdentityStore.ROLE_VIEWER, "tunnel");
        }
        if (!identityStore.isUserActive(username))
            return null;

        createSession(request, response, username, true);
        identityStore.recordAuthSuccess(clientIp(request), username);
        return new UserAuthentication(AUTH_METHOD, withoutCredentials(username));
    }

    private Authentication authenticateBasic(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Basic ", 0, 6))
            return null;
        if (authHeader.length() > MAX_AUTH_HEADER_LENGTH) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Authorization header is too large");
            return Authentication.SEND_FAILURE;
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(authHeader.substring(6).trim());
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed Basic authorization header");
            return Authentication.SEND_FAILURE;
        }

        String credentials = new String(decoded, StandardCharsets.UTF_8);
        int separator = credentials.indexOf(':');
        if (separator <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed Basic authorization header");
            return Authentication.SEND_FAILURE;
        }

        String username = credentials.substring(0, separator);
        String password = credentials.substring(separator + 1);
        String remoteIp = clientIp(request);

        if (identityStore.isRateLimited(remoteIp, username)) {
            response.sendError(429, "Too many failed sign-in attempts. Please wait a minute and try again.");
            return Authentication.SEND_FAILURE;
        }

        if (!verifyLocalCredentials(username, password, request)) {
            identityStore.recordAuthFailure(remoteIp, username);
            log.warn("Failed Basic authentication for user '{}'", username);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return Authentication.SEND_FAILURE;
        }

        identityStore.recordAuthSuccess(remoteIp, username);
        UserIdentity identity = withoutCredentials(username);
        createSession(request, response, username, allowKeepSignedIn);
        log.debug("Created authentication session for user '{}'", username);
        return new UserAuthentication(AUTH_METHOD, identity);
    }

    private boolean verifyLocalCredentials(String username, String password, HttpServletRequest request) {
        if (username == null || password == null)
            return false;
        UserProfileRecord profile = identityStore.getProfile(username);
        if (profile != null && !profile.enabled)
            return false;

        if (_loginService != null) {
            UserIdentity jettyIdentity = login(username, password, request);
            if (jettyIdentity != null)
                return true;
        }

        IUserInfo user = securityManager != null ? securityManager.getUserInfo(username) : null;
        if (user == null || user.getPassword() == null)
            return false;
        return PasswordHasher.verifyPassword(user.getPassword(), password);
    }

    // -------------------------------------------------------------------------
    // Form, WebAuthn & OIDC Login Flows
    // -------------------------------------------------------------------------

    private void serveLoginScript(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"GET".equalsIgnoreCase(request.getMethod()) && !"HEAD".equalsIgnoreCase(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/javascript; charset=UTF-8");
        response.setContentLength(LOGIN_JS_BYTES.length);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        if (!"HEAD".equalsIgnoreCase(request.getMethod())) {
            response.getOutputStream().write(LOGIN_JS_BYTES);
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String redirectTarget = redirectTarget(request.getParameter("continue"));
        String explicitLang = normalizeSupportedLanguage(request.getParameter("lang"));
        String lang = explicitLang != null ? explicitLang : resolveLoginLanguage(request);
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            if (explicitLang != null)
                setLanguageCookie(response, explicitLang);
            renderLogin(response, false, null, redirectTarget, lang);
            return;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        if (explicitLang != null)
            setLanguageCookie(response, explicitLang);
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        boolean keepSignedIn = allowKeepSignedIn && !"false".equalsIgnoreCase(request.getParameter("keepSignedIn"))
                && (request.getParameter("keepSignedIn") != null || request.getParameter("keepSignedInPresent") == null);

        if (username == null || password == null || username.length() > 256 || password.length() > 4096) {
            renderLogin(response, true, null, redirectTarget, lang);
            return;
        }

        String remoteIp = clientIp(request);
        if (identityStore.isRateLimited(remoteIp, username)) {
            LoginCopy copy = LoginCopy.forLanguage(lang);
            renderLogin(response, true, copy.rateLimited, redirectTarget, lang);
            return;
        }

        if (!verifyLocalCredentials(username, password, request)) {
            identityStore.recordAuthFailure(remoteIp, username);
            log.warn("Failed form authentication for user '{}'", username);
            renderLogin(response, true, null, redirectTarget, lang);
            return;
        }

        identityStore.recordAuthSuccess(remoteIp, username);
        createSession(request, response, username, keepSignedIn);
        log.debug("Created authentication session for user '{}'", username);

        UserProfileRecord profile = identityStore.getProfile(username);
        if (profile != null && profile.mustChangePassword) {
            String changeUrl = LOGIN_PASSWORD_CHANGE_PATH + "?continue="
                    + URLEncoder.encode(redirectTarget, StandardCharsets.UTF_8);
            response.sendRedirect(response.encodeRedirectURL(changeUrl));
            return;
        }
        response.sendRedirect(response.encodeRedirectURL(redirectTarget));
    }

    private void handleForcedPasswordChangePage(HttpServletRequest request,
                                                HttpServletResponse response,
                                                String username) throws IOException {
        String redirectTarget = redirectTarget(request.getParameter("continue"));
        String lang = resolveLoginLanguage(request);
        LoginCopy copy = LoginCopy.forLanguage(lang);

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            renderPasswordChange(response, false, null, redirectTarget, copy);
            return;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            renderPasswordChange(response, true, copy.passwordMismatch, redirectTarget, copy);
            return;
        }
        try {
            String currentToken = readSessionCookie(request);
            identityStore.completeForcedPasswordChange(username, newPassword, currentToken);
            response.sendRedirect(response.encodeRedirectURL(redirectTarget));
        } catch (IllegalArgumentException e) {
            renderPasswordChange(response, true, e.getMessage(), redirectTarget, copy);
        }
    }

    private String defaultLandingForUser(UserProfileRecord profile) {
        if (profile != null && profile.allowedViews != null && !profile.allowedViews.isEmpty()) {
            return "/?view=" + URLEncoder.encode(profile.allowedViews.get(0), StandardCharsets.UTF_8);
        }
        return defaultRedirectPath;
    }

    private void handleLoginEnrollPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"GET".equalsIgnoreCase(request.getMethod()) && !"HEAD".equalsIgnoreCase(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        String explicitLang = normalizeSupportedLanguage(request.getParameter("lang"));
        String lang = explicitLang != null ? explicitLang : resolveLoginLanguage(request);
        if (explicitLang != null)
            setLanguageCookie(response, explicitLang);

        String rawToken = request.getParameter("token");
        EnrollmentInviteRecord invite = identityStore.peekValidEnrollmentInvite(rawToken);
        if (invite == null) {
            renderEnrollPage(request, response, null, null, "/", lang, true, null);
            return;
        }
        UserProfileRecord profile = identityStore.ensureProfile(invite.username);
        String target = request.getParameter("continue") != null
                ? redirectTarget(request.getParameter("continue"))
                : defaultLandingForUser(profile);
        renderEnrollPage(request, response, rawToken.trim(), profile, target, lang, false, null);
    }

    private void handleLoginEnrollPassword(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        String lang = resolveLoginLanguage(request);
        LoginCopy copy = LoginCopy.forLanguage(lang);
        String rawToken = request.getParameter("token");
        EnrollmentInviteRecord invite = identityStore.peekValidEnrollmentInvite(rawToken);
        if (invite == null) {
            renderEnrollPage(request, response, null, null, "/", lang, true, null);
            return;
        }

        UserProfileRecord previewProfile = identityStore.ensureProfile(invite.username);
        String redirectTarget = request.getParameter("continue") != null
                ? redirectTarget(request.getParameter("continue"))
                : defaultLandingForUser(previewProfile);
        boolean keepSignedIn = allowKeepSignedIn && !"false".equalsIgnoreCase(request.getParameter("keepSignedIn"))
                && (request.getParameter("keepSignedIn") != null || request.getParameter("keepSignedInPresent") == null);

        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            renderEnrollPage(request, response, rawToken.trim(), previewProfile, redirectTarget, lang, true, copy.passwordMismatch);
            return;
        }

        try {
            UserProfileRecord completed = identityStore.consumeInviteWithPassword(rawToken.trim(), newPassword);
            createSession(request, response, completed.userId, keepSignedIn);
            response.sendRedirect(response.encodeRedirectURL(redirectTarget));
        } catch (IllegalArgumentException | SecurityException e) {
            renderEnrollPage(request, response, rawToken.trim(), previewProfile, redirectTarget, lang, true, e.getMessage());
        }
    }

    private void handleLoginEnrollPasskeyOptions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        if (!requireJsonSameOrigin(request, response))
            return;

        String rpId = resolveRpId(request);
        if (!isDomainRpId(rpId)) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Passkeys require opening OSCAR via a domain name (such as oscar.local) rather than a raw IP address.");
            return;
        }

        JsonObject body = parseJsonBody(request);
        String rawToken = getJsonString(body, "token");
        EnrollmentInviteRecord invite = identityStore.peekValidEnrollmentInvite(rawToken);
        if (invite == null) {
            sendJsonError(response, HttpServletResponse.SC_FORBIDDEN,
                    "Enrollment invite is invalid, expired, or has already been used.");
            return;
        }

        UserProfileRecord profile = identityStore.ensureProfile(invite.username);
        String redirectTarget = body.has("continue")
                ? redirectTarget(getJsonString(body, "continue"))
                : defaultLandingForUser(profile);
        boolean keepSignedIn = allowKeepSignedIn && getJsonBoolean(body, "keepSignedIn", true);

        PendingChallenge pending = identityStore.createChallenge(
                "webauthn-enroll",
                profile.userId,
                rpId,
                redirectTarget,
                keepSignedIn,
                null,
                rawToken.trim());

        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("challenge", pending.challenge());
        opts.put("rp", Map.of(
                "id", rpId,
                "name", (config.webAuthnRpName == null || config.webAuthnRpName.isBlank()) ? "OSCAR" : config.webAuthnRpName));
        opts.put("user", Map.of(
                "id", profile.passkeyUserId,
                "name", profile.userId,
                "displayName", (profile.displayName != null && !profile.displayName.isBlank()) ? profile.displayName : profile.userId));
        opts.put("pubKeyCredParams", List.of(
                Map.of("type", "public-key", "alg", WebAuthnCodec.COSE_ALG_ES256),
                Map.of("type", "public-key", "alg", WebAuthnCodec.COSE_ALG_RS256)));
        opts.put("timeout", 120_000);
        opts.put("attestation", "none");
        opts.put("authenticatorSelection", Map.of(
                "residentKey", "preferred",
                "userVerification", "preferred"));
        opts.put("excludeCredentials", List.of());
        sendJson(response, HttpServletResponse.SC_OK, opts);
    }

    private void handleLoginEnrollPasskeyVerify(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        if (!requireJsonSameOrigin(request, response))
            return;

        try {
            JsonObject body = parseJsonBody(request);
            String challengeToken = getJsonString(body, "challenge");
            String rawToken = getJsonString(body, "token");
            PendingChallenge pending = identityStore.consumeChallenge(challengeToken, "webauthn-enroll");
            if (pending == null || rawToken == null || !rawToken.trim().equals(pending.oidcNonce())) {
                sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Passkey enrollment challenge expired. Reload and try again.");
                return;
            }

            JsonObject credObj = body.has("credential") && body.get("credential").isJsonObject()
                    ? body.getAsJsonObject("credential")
                    : null;
            JsonObject respObj = (credObj != null && credObj.has("response") && credObj.get("response").isJsonObject())
                    ? credObj.getAsJsonObject("response")
                    : null;
            String credId = getJsonString(credObj, "id");
            String clientDataJSON = getJsonString(respObj, "clientDataJSON");
            String attestationObject = getJsonString(respObj, "attestationObject");
            String customName = getJsonString(body, "name");
            List<String> transports = getJsonStringList(respObj != null ? respObj.get("transports") : null);

            if (credId == null || clientDataJSON == null || attestationObject == null) {
                sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Incomplete WebAuthn registration response.");
                return;
            }

            WebAuthnCodec.VerifiedRegistration verified = WebAuthnCodec.verifyRegistration(
                    credId,
                    clientDataJSON,
                    attestationObject,
                    pending.challenge(),
                    pending.rpId(),
                    resolveAllowedOrigins(request, pending.rpId()),
                    false);

            UserProfileRecord completed = identityStore.consumeInviteWithPasskey(
                    rawToken.trim(), verified, customName, transports, null);
            createSession(request, response, completed.userId, pending.keepSignedIn());
            sendJson(response, HttpServletResponse.SC_OK, Map.of(
                    "enrolled", true,
                    "username", completed.userId,
                    "redirectUrl", pending.redirectTarget()));
        } catch (IllegalArgumentException | SecurityException e) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void renderEnrollPage(HttpServletRequest request,
                                  HttpServletResponse response,
                                  String rawToken,
                                  UserProfileRecord profile,
                                  String redirectTarget,
                                  String lang,
                                  boolean failed,
                                  String errorMessage) throws IOException {
        LoginCopy copy = LoginCopy.forLanguage(lang);
        String welcomeTitle;
        String welcomeSub;
        String invalidInviteMsg;
        String passkeySetupBtn;
        String passkeyNamePlaceholder;
        switch (copy.lang) {
            case "es":
                welcomeTitle = "Activar cuenta de OSCAR";
                welcomeSub = "Configure una llave Passkey en un toque o elija su contrase\u00f1a privada.";
                invalidInviteMsg = "Este enlace de invitaci\u00f3n no es v\u00e1lido, ha caducado o ya se ha utilizado.";
                passkeySetupBtn = "Configurar Passkey e iniciar sesi\u00f3n (Recomendado)";
                passkeyNamePlaceholder = "Etiqueta opcional (ej. iPhone, Windows Hello o YubiKey)";
                break;
            case "fr":
                welcomeTitle = "Activer votre compte OSCAR";
                welcomeSub = "Enregistrez une cl\u00e9 Passkey en un geste ou choisissez votre mot de passe personnel.";
                invalidInviteMsg = "Ce lien d\u2019invitation est invalide, expir\u00e9 ou d\u00e9j\u00e0 utilis\u00e9.";
                passkeySetupBtn = "Configurer une cl\u00e9 Passkey et se connecter (Recommand\u00e9)";
                passkeyNamePlaceholder = "Libell\u00e9 facultatif (ex. iPhone, Windows Hello ou YubiKey)";
                break;
            case "el":
                welcomeTitle = "\u0395\u03bd\u03b5\u03c1\u03b3\u03bf\u03c0\u03bf\u03af\u03b7\u03c3\u03b7 \u03bb\u03bf\u03b3\u03b1\u03c1\u03b9\u03b1\u03c3\u03bc\u03bf\u03cd OSCAR";
                welcomeSub = "\u03a1\u03c5\u03b8\u03bc\u03af\u03c3\u03c4\u03b5 \u03ba\u03bb\u03b5\u03b9\u03b4\u03af Passkey \u03bc\u03b5 \u03ad\u03bd\u03b1 \u03ac\u03b3\u03b3\u03b9\u03b3\u03bc\u03b1 \u03ae \u03bf\u03c1\u03af\u03c3\u03c4\u03b5 \u03c0\u03c1\u03bf\u03c3\u03c9\u03c0\u03b9\u03ba\u03cc \u03ba\u03c9\u03b4\u03b9\u03ba\u03cc.";
                invalidInviteMsg = "\u039f \u03c3\u03cd\u03bd\u03b4\u03b5\u03c3\u03bc\u03bf\u03c2 \u03c0\u03c1\u03cc\u03c3\u03ba\u03bb\u03b7\u03c3\u03b7\u03c2 \u03b4\u03b5\u03bd \u03b5\u03af\u03bd\u03b1\u03b9 \u03ad\u03b3\u03ba\u03c5\u03c1\u03bf\u03c2, \u03ad\u03c7\u03b5\u03b9 \u03bb\u03ae\u03be\u03b5\u03b9 \u03ae \u03ad\u03c7\u03b5\u03b9 \u03ae\u03b4\u03b7 \u03c7\u03c1\u03b7\u03c3\u03b9\u03bc\u03bf\u03c0\u03bf\u03b9\u03b7\u03b8\u03b5\u03af.";
                passkeySetupBtn = "\u03a1\u03cd\u03b8\u03bc\u03b9\u03c3\u03b7 Passkey & \u03c3\u03cd\u03bd\u03b4\u03b5\u03c3\u03b7 (\u03a3\u03c5\u03bd\u03b9\u03c3\u03c4\u03ac\u03c4\u03b1\u03b9)";
                passkeyNamePlaceholder = "\u03a0\u03c1\u03bf\u03b1\u03b9\u03c1\u03b5\u03c4\u03b9\u03ba\u03ae \u03b5\u03c4\u03b9\u03ba\u03ad\u03c4\u03b1 (\u03c0.\u03c7. iPhone, Windows Hello \u03ae YubiKey)";
                break;
            default:
                welcomeTitle = "Activate your OSCAR account";
                welcomeSub = "Set up a 1-tap Passkey or choose a private password to finish signing in.";
                invalidInviteMsg = "This enrollment invite link is invalid, expired, or has already been used.";
                passkeySetupBtn = "Set Up Passkey & Sign In (Recommended)";
                passkeyNamePlaceholder = "Optional device label (e.g. iPhone, Windows Hello, or YubiKey)";
                break;
        }

        StringBuilder bodyHtml = new StringBuilder();
        bodyHtml.append("<!doctype html><html lang=\"").append(escapeHtml(copy.lang)).append("\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<title>").append(escapeHtml(welcomeTitle)).append("</title><style>")
                .append("body{font:16px system-ui,-apple-system,sans-serif;background:#f4f6f8;color:#0f172a;display:grid;place-items:center;min-height:100vh;margin:0}")
                .append("main{background:#fff;padding:2rem;border-radius:.65rem;box-shadow:0 6px 24px #0f172a18;width:min(27rem,calc(100% - 2.5rem))}")
                .append("h1{margin:0 0 .3rem;font-size:1.45rem}p.sub{margin:0 0 1rem;color:#475569;font-size:.93rem}")
                .append(".badge-box{background:#f8fafc;border:1px solid #e2e8f0;border-radius:.45rem;padding:.75rem .9rem;margin-bottom:1rem;font-size:.9rem}")
                .append(".badge-box strong{color:#0f172a}")
                .append("label{display:block;margin-top:.95rem;font-weight:500;font-size:.92rem}")
                .append("input[type=text],input[type=password],button,a.btn-link{display:block;width:100%;box-sizing:border-box;font:inherit}")
                .append("input[type=text],input[type=password]{padding:.65rem .75rem;margin-top:.35rem;border:1px solid #cbd5e1;border-radius:.4rem}")
                .append(".pw-wrap{display:flex;gap:.4rem;margin-top:.35rem}.pw-wrap input{margin-top:0;flex:1}")
                .append(".pw-toggle{width:auto;padding:.65rem .75rem;margin:0;background:#f8fafc;border:1px solid #cbd5e1;border-radius:.4rem;cursor:pointer;font-size:.875rem}")
                .append(".checkbox-row{display:flex;align-items:center;gap:.55rem;margin-top:.95rem;font-weight:400;font-size:.9rem;color:#334155;cursor:pointer}")
                .append(".checkbox-row input{width:1.05rem;height:1.05rem;margin:0}")
                .append(".primary-btn{margin-top:.75rem;padding:.75rem;background:#0f4c81;color:#fff;border:none;border-radius:.4rem;font-weight:600;cursor:pointer;text-align:center;text-decoration:none}")
                .append(".primary-btn:hover{background:#0c3b66}")
                .append(".secondary-btn{margin-top:1.1rem;padding:.68rem;background:#fff;color:#0f172a;border:1px solid #94a3b8;border-radius:.4rem;font-weight:600;cursor:pointer}")
                .append(".secondary-btn:hover{background:#f8fafc}")
                .append(".divider{display:flex;align-items:center;gap:.65rem;color:#64748b;font-size:.82rem;margin:1.1rem 0}")
                .append(".divider::before,.divider::after{content:\"\";flex:1;border-top:1px solid #e2e8f0}")
                .append(".error{color:#b91c1c;background:#fef2f2;border:1px solid #fecaca;padding:.6rem .75rem;border-radius:.4rem;font-size:.9rem;margin:.75rem 0}")
                .append("</style></head><body><main><h1>").append(escapeHtml(welcomeTitle)).append("</h1>");

        if (rawToken == null || profile == null) {
            bodyHtml.append("<p class=\"error\" role=\"alert\">").append(escapeHtml(invalidInviteMsg)).append("</p>")
                    .append("<a class=\"btn-link primary-btn\" href=\"").append(LOGIN_PATH).append("\">")
                    .append(escapeHtml(copy.submit)).append("</a></main></body></html>");
            byte[] errBytes = bodyHtml.toString().getBytes(StandardCharsets.UTF_8);
            response.setStatus(HttpServletResponse.SC_GONE);
            response.setContentType("text/html; charset=UTF-8");
            response.setContentLength(errBytes.length);
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Content-Security-Policy", LOGIN_CSP);
            response.setHeader("Referrer-Policy", "no-referrer");
            if (!"HEAD".equalsIgnoreCase(request.getMethod()))
                response.getOutputStream().write(errBytes);
            return;
        }

        String primaryRole = identityStore.getPrimaryRole(profile.userId);
        String scopeSummary = (profile.allowedViews == null || profile.allowedViews.isEmpty())
                ? "All Lanes"
                : String.join(", ", profile.allowedViews);
        boolean rpSupported = isDomainRpId(resolveRpId(request));

        bodyHtml.append("<p class=\"sub\">").append(escapeHtml(welcomeSub)).append("</p>")
                .append("<div class=\"badge-box\"><div><strong>")
                .append(escapeHtml(profile.displayName != null ? profile.displayName : profile.userId))
                .append("</strong> (@").append(escapeHtml(profile.userId)).append(")</div>")
                .append("<div style=\"margin-top:.25rem;color:#475569\">Role: <strong>")
                .append(escapeHtml(primaryRole.toUpperCase(Locale.ROOT)))
                .append("</strong> &middot; Gates: <strong>").append(escapeHtml(scopeSummary)).append("</strong></div></div>");

        if (failed && errorMessage != null) {
            bodyHtml.append("<p class=\"error\" role=\"alert\">").append(escapeHtml(errorMessage)).append("</p>");
        }

        if (rpSupported) {
            bodyHtml.append("<div id=\"enroll-passkey-section\" hidden>")
                    .append("<input id=\"enroll-passkey-name\" type=\"text\" maxlength=\"64\" placeholder=\"")
                    .append(escapeHtml(passkeyNamePlaceholder)).append("\">")
                    .append("<button type=\"button\" id=\"enroll-passkey-btn\" class=\"primary-btn\" data-error-msg=\"")
                    .append(escapeHtml(copy.passkeyFailed)).append("\">")
                    .append(escapeHtml(passkeySetupBtn)).append("</button>")
                    .append("<p id=\"enroll-passkey-status\" class=\"error\" role=\"alert\" hidden></p>")
                    .append("<div class=\"divider\"><span>").append(escapeHtml(copy.orDivider)).append("</span></div>")
                    .append("</div>");
        }

        bodyHtml.append("<form method=\"post\" action=\"").append(LOGIN_ENROLL_PASSWORD_PATH).append("\">")
                .append("<input type=\"hidden\" id=\"enroll-token\" name=\"token\" value=\"").append(escapeHtml(rawToken)).append("\">")
                .append("<input type=\"hidden\" name=\"continue\" value=\"").append(escapeHtml(redirectTarget)).append("\">")
                .append("<label for=\"new-password\">").append(escapeHtml(copy.newPasswordLabel)).append(" (12+)</label>")
                .append("<div class=\"pw-wrap\">")
                .append("<input id=\"new-password\" name=\"newPassword\" type=\"password\" required minlength=\"12\" maxlength=\"512\"")
                .append(" autocomplete=\"new-password\" spellcheck=\"false\" autocorrect=\"off\" autocapitalize=\"none\">")
                .append("<button type=\"button\" id=\"toggle-new-password\" class=\"pw-toggle\" aria-controls=\"new-password\"")
                .append(" aria-pressed=\"false\" data-show-label=\"").append(escapeHtml(copy.showPassword)).append("\"")
                .append(" data-hide-label=\"").append(escapeHtml(copy.hidePassword)).append("\" hidden>")
                .append(escapeHtml(copy.showPassword)).append("</button></div>")
                .append("<label for=\"confirm-password\">").append(escapeHtml(copy.confirmPasswordLabel)).append("</label>")
                .append("<input id=\"confirm-password\" name=\"confirmPassword\" type=\"password\" required minlength=\"12\" maxlength=\"512\"")
                .append(" autocomplete=\"new-password\" spellcheck=\"false\" autocorrect=\"off\" autocapitalize=\"none\">");

        if (allowKeepSignedIn) {
            bodyHtml.append("<input type=\"hidden\" name=\"keepSignedInPresent\" value=\"1\">")
                    .append("<label class=\"checkbox-row\" for=\"keep-signed-in\">")
                    .append("<input id=\"keep-signed-in\" name=\"keepSignedIn\" type=\"checkbox\" value=\"true\" checked>")
                    .append("<span>").append(escapeHtml(copy.keepSignedIn)).append("</span></label>");
        }

        bodyHtml.append("<button type=\"submit\" class=\"").append(rpSupported ? "secondary-btn" : "primary-btn").append("\">")
                .append(escapeHtml(copy.savePasswordButton)).append("</button></form>")
                .append("<script src=\"").append(LOGIN_JS_PATH).append("\" defer></script></main></body></html>");

        byte[] outBytes = bodyHtml.toString().getBytes(StandardCharsets.UTF_8);
        response.setStatus(failed ? HttpServletResponse.SC_BAD_REQUEST : HttpServletResponse.SC_OK);
        response.setContentType("text/html; charset=UTF-8");
        response.setContentLength(outBytes.length);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Content-Security-Policy", LOGIN_CSP);
        response.setHeader("Referrer-Policy", "no-referrer");
        if (!"HEAD".equalsIgnoreCase(request.getMethod())) {
            response.getOutputStream().write(outBytes);
        }
    }

    private void handleLoginWebAuthnOptions(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        if (!requireJsonSameOrigin(request, response))
            return;

        String rpId = resolveRpId(request);
        if (!isDomainRpId(rpId)) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Passkeys require a domain name (such as oscar.local) rather than a raw IP address. Sign in with your password or open OSCAR via its hostname.");
            return;
        }

        JsonObject body = parseJsonBody(request);
        String usernameHint = getJsonString(body, "username");
        String redirectTarget = redirectTarget(getJsonString(body, "continue"));
        boolean keepSignedIn = allowKeepSignedIn && getJsonBoolean(body, "keepSignedIn", true);

        PendingChallenge pending = identityStore.createChallenge(
                "webauthn-login",
                usernameHint,
                rpId,
                redirectTarget,
                keepSignedIn,
                null,
                null);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("challenge", pending.challenge());
        out.put("rpId", rpId);
        out.put("timeout", 120_000);
        out.put("userVerification", "preferred");

        if (usernameHint != null && !usernameHint.isBlank()) {
            UserProfileRecord user = identityStore.getProfile(usernameHint.trim());
            if (user != null && user.enabled && user.passkeys != null && !user.passkeys.isEmpty()) {
                List<Map<String, Object>> allowList = new ArrayList<>();
                for (PasskeyRecord pk : user.passkeys) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("type", "public-key");
                    item.put("id", pk.id);
                    if (pk.transports != null && !pk.transports.isEmpty())
                        item.put("transports", pk.transports);
                    allowList.add(item);
                }
                out.put("allowCredentials", allowList);
            }
        }

        sendJson(response, HttpServletResponse.SC_OK, out);
    }

    private void handleLoginWebAuthnVerify(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        if (!requireJsonSameOrigin(request, response))
            return;

        String remoteIp = clientIp(request);
        if (identityStore.isRateLimited(remoteIp, null)) {
            sendJsonError(response, 429, "Too many sign-in attempts. Please wait a minute and try again.");
            return;
        }

        JsonObject body = parseJsonBody(request);
        String challengeToken = getJsonString(body, "challenge");
        PendingChallenge pending = identityStore.consumeChallenge(challengeToken, "webauthn-login");
        if (pending == null) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Passkey sign-in challenge expired. Please try again.");
            return;
        }

        JsonObject credObj = body.has("credential") && body.get("credential").isJsonObject()
                ? body.getAsJsonObject("credential")
                : null;
        JsonObject respObj = (credObj != null && credObj.has("response") && credObj.get("response").isJsonObject())
                ? credObj.getAsJsonObject("response")
                : null;
        String credentialId = getJsonString(credObj, "id");
        String clientDataJSON = getJsonString(respObj, "clientDataJSON");
        String authenticatorData = getJsonString(respObj, "authenticatorData");
        String signature = getJsonString(respObj, "signature");

        if (credentialId == null || clientDataJSON == null || authenticatorData == null || signature == null) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Incomplete WebAuthn assertion payload.");
            return;
        }

        PasskeyMatch match = identityStore.findPasskeyByCredentialId(credentialId);
        if (match == null) {
            // Per modern-web-guidance passkey-authentication: return HTTP 404 ONLY when
            // the credential has been deleted/unregistered on the server so the client
            // invokes PublicKeyCredential.signalUnknownCredential().
            Map<String, Object> notFound = new LinkedHashMap<>();
            notFound.put("unknownCredential", true);
            notFound.put("error", "This passkey is no longer registered on this OSCAR node.");
            sendJson(response, HttpServletResponse.SC_NOT_FOUND, notFound);
            return;
        }

        if (!identityStore.isUserActive(match.user().userId)) {
            identityStore.recordAuthFailure(remoteIp, match.user().userId);
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Account is disabled.");
            return;
        }

        try {
            WebAuthnCodec.VerifiedAssertion verified = WebAuthnCodec.verifyAssertion(
                    clientDataJSON,
                    authenticatorData,
                    signature,
                    match.credential().publicKeyCose,
                    match.credential().signCount,
                    pending.challenge(),
                    pending.rpId(),
                    resolveAllowedOrigins(request, pending.rpId()),
                    false);

            identityStore.recordPasskeyAssertion(match.user().userId, credentialId, verified);
            identityStore.recordAuthSuccess(remoteIp, match.user().userId);

            boolean keepSignedIn = allowKeepSignedIn && getJsonBoolean(body, "keepSignedIn", pending.keepSignedIn());
            createSession(request, response, match.user().userId, keepSignedIn);

            String target = redirectTarget(getJsonString(body, "continue"));
            if (target == null)
                target = pending.redirectTarget();
            if (match.user().mustChangePassword) {
                target = LOGIN_PASSWORD_CHANGE_PATH + "?continue="
                        + URLEncoder.encode(target, StandardCharsets.UTF_8);
            }

            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("authenticated", true);
            ok.put("username", match.user().userId);
            ok.put("redirectUrl", target);
            sendJson(response, HttpServletResponse.SC_OK, ok);
        } catch (IllegalArgumentException e) {
            identityStore.recordAuthFailure(remoteIp, match.user().userId);
            log.warn("Failed WebAuthn assertion for user '{}': {}", match.user().userId, e.getMessage());
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Passkey verification failed.");
        }
    }

    private void handleOidcStart(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!config.oidcEnabled) {
            response.sendRedirect(LOGIN_PATH);
            return;
        }
        String redirectTarget = redirectTarget(request.getParameter("continue"));
        String lang = resolveLoginLanguage(request);
        boolean keepSignedIn = allowKeepSignedIn && !"false".equalsIgnoreCase(request.getParameter("keepSignedIn"));

        String codeVerifier = PasswordHasher.randomBase64Url(32);
        String nonce = PasswordHasher.randomBase64Url(24);
        PendingChallenge stateChallenge = identityStore.createChallenge(
                "oidc-login",
                null,
                resolveRpId(request),
                redirectTarget,
                keepSignedIn,
                codeVerifier,
                nonce);

        String callbackUri = buildExternalOrigin(request) + LOGIN_OIDC_CALLBACK_PATH;
        try {
            String authUrl = oidcClient.buildAuthorizationUrl(
                    config, callbackUri, stateChallenge.challenge(), nonce, codeVerifier);
            response.sendRedirect(authUrl);
        } catch (Exception e) {
            log.warn("OIDC discovery/redirect failed (internet/IdP may be offline): {}", e.getMessage());
            LoginCopy copy = LoginCopy.forLanguage(lang);
            renderLogin(response, true, copy.oidcUnavailable, redirectTarget, lang);
        }
    }

    private void handleOidcCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String lang = resolveLoginLanguage(request);
        LoginCopy copy = LoginCopy.forLanguage(lang);
        if (!config.oidcEnabled) {
            response.sendRedirect(LOGIN_PATH);
            return;
        }

        String state = request.getParameter("state");
        String code = request.getParameter("code");
        PendingChallenge pending = identityStore.consumeChallenge(state, "oidc-login");
        if (pending == null || code == null || code.isBlank()) {
            renderLogin(response, true, copy.oidcUnavailable, defaultRedirectPath, lang);
            return;
        }

        String callbackUri = buildExternalOrigin(request) + LOGIN_OIDC_CALLBACK_PATH;
        try {
            OidcClientHelper.OidcUserClaims claims = oidcClient.exchangeCodeAndVerify(
                    config, code, callbackUri, pending.oidcNonce(), pending.oidcCodeVerifier());

            UserProfileRecord profile = identityStore.provisionOrSyncExternalUser(
                    claims.username, claims.displayName, claims.mappedRole, "oidc");
            if (!profile.enabled) {
                renderLogin(response, true, copy.error, pending.redirectTarget(), lang);
                return;
            }

            identityStore.recordAuthSuccess(clientIp(request), claims.username);
            createSession(request, response, claims.username, pending.keepSignedIn());
            response.sendRedirect(response.encodeRedirectURL(pending.redirectTarget()));
        } catch (Exception e) {
            log.warn("OIDC callback verification failed: {}", e.getMessage());
            renderLogin(response, true, copy.oidcUnavailable, pending.redirectTarget(), lang);
        }
    }

    // -------------------------------------------------------------------------
    // Authenticated Session, Self-Service Passkeys & Admin Users API
    // -------------------------------------------------------------------------

    private String extractSessionApiSubpath(String normalizedPath) {
        if (SESSION_PATH.equals(normalizedPath) || SENSORHUB_SESSION_PATH.equals(normalizedPath))
            return "";
        if (normalizedPath.startsWith(SESSION_PATH + "/"))
            return normalizedPath.substring(SESSION_PATH.length());
        if (normalizedPath.startsWith(SENSORHUB_SESSION_PATH + "/"))
            return normalizedPath.substring(SENSORHUB_SESSION_PATH.length());
        return null;
    }

    private void handleSessionApi(HttpServletRequest request,
                                  HttpServletResponse response,
                                  PersistedSession session,
                                  String subpath) throws IOException {
        String method = request.getMethod();
        String username = session.username;

        // Refresh rolling persistent cookie on /session heartbeat
        if ("".equals(subpath)) {
            if (!"GET".equalsIgnoreCase(method)) {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
            if (session.keepSignedIn && allowKeepSignedIn) {
                String rawCookie = readSessionCookie(request);
                if (rawCookie != null)
                    setSessionCookie(response, rawCookie, true);
            }
            renderCurrentSession(request, response, session);
            return;
        }

        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            if (!requireJsonSameOrigin(request, response))
                return;
        }

        try {
            // Self-service password change
            if ("/account/password".equals(subpath) && "POST".equalsIgnoreCase(method)) {
                JsonObject body = parseJsonBody(request);
                String currentPassword = getJsonString(body, "currentPassword");
                String newPassword = getJsonString(body, "newPassword");
                String keepToken = readSessionCookie(request);
                identityStore.changeOwnPassword(username, currentPassword, newPassword, keepToken);
                sendJson(response, HttpServletResponse.SC_OK, Map.of("updated", true));
                return;
            }

            // Self-service passkeys
            if ("/account/passkeys".equals(subpath) && "GET".equalsIgnoreCase(method)) {
                sendJson(response, HttpServletResponse.SC_OK, buildPasskeyListPayload(request, username));
                return;
            }

            if ("/account/passkeys/options".equals(subpath) && "POST".equalsIgnoreCase(method)) {
                String rpId = resolveRpId(request);
                if (!isDomainRpId(rpId)) {
                    sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                            "Passkeys require a domain name (for example oscar.local) rather than a raw IP address.");
                    return;
                }
                UserProfileRecord profile = identityStore.ensureProfile(username);
                PendingChallenge pending = identityStore.createChallenge(
                        "webauthn-register", username, rpId, "/", session.keepSignedIn, null, null);

                Map<String, Object> opts = new LinkedHashMap<>();
                opts.put("challenge", pending.challenge());
                opts.put("rp", Map.of(
                        "id", rpId,
                        "name", (config.webAuthnRpName == null || config.webAuthnRpName.isBlank()) ? "OSCAR" : config.webAuthnRpName));
                opts.put("user", Map.of(
                        "id", profile.passkeyUserId,
                        "name", profile.userId,
                        "displayName", (profile.displayName != null && !profile.displayName.isBlank()) ? profile.displayName : profile.userId));
                opts.put("pubKeyCredParams", List.of(
                        Map.of("type", "public-key", "alg", WebAuthnCodec.COSE_ALG_ES256),
                        Map.of("type", "public-key", "alg", WebAuthnCodec.COSE_ALG_RS256)));
                opts.put("timeout", 120_000);
                opts.put("attestation", "none");
                opts.put("authenticatorSelection", Map.of(
                        "residentKey", "preferred",
                        "userVerification", "preferred"));

                List<Map<String, Object>> exclude = new ArrayList<>();
                if (profile.passkeys != null) {
                    for (PasskeyRecord pk : profile.passkeys) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("type", "public-key");
                        item.put("id", pk.id);
                        if (pk.transports != null && !pk.transports.isEmpty())
                            item.put("transports", pk.transports);
                        exclude.add(item);
                    }
                }
                opts.put("excludeCredentials", exclude);
                sendJson(response, HttpServletResponse.SC_OK, opts);
                return;
            }

            if ("/account/passkeys/verify".equals(subpath) && "POST".equalsIgnoreCase(method)) {
                JsonObject body = parseJsonBody(request);
                String challengeToken = getJsonString(body, "challenge");
                PendingChallenge pending = identityStore.consumeChallenge(challengeToken, "webauthn-register");
                if (pending == null || !username.equals(pending.username())) {
                    sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Passkey registration challenge expired.");
                    return;
                }
                JsonObject credObj = body.has("credential") && body.get("credential").isJsonObject()
                        ? body.getAsJsonObject("credential")
                        : null;
                JsonObject respObj = (credObj != null && credObj.has("response") && credObj.get("response").isJsonObject())
                        ? credObj.getAsJsonObject("response")
                        : null;
                String credId = getJsonString(credObj, "id");
                String clientDataJSON = getJsonString(respObj, "clientDataJSON");
                String attestationObject = getJsonString(respObj, "attestationObject");
                String customName = getJsonString(body, "name");
                List<String> transports = getJsonStringList(respObj != null ? respObj.get("transports") : null);

                if (credId == null || clientDataJSON == null || attestationObject == null) {
                    sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Incomplete WebAuthn registration response.");
                    return;
                }

                WebAuthnCodec.VerifiedRegistration verified = WebAuthnCodec.verifyRegistration(
                        credId,
                        clientDataJSON,
                        attestationObject,
                        pending.challenge(),
                        pending.rpId(),
                        resolveAllowedOrigins(request, pending.rpId()),
                        false);

                identityStore.registerPasskey(username, verified, customName, transports);
                sendJson(response, HttpServletResponse.SC_OK, buildPasskeyListPayload(request, username));
                return;
            }

            if (subpath.startsWith("/account/passkeys/")) {
                String credId = urlDecodeSegment(subpath.substring("/account/passkeys/".length()));
                if ("PUT".equalsIgnoreCase(method)) {
                    JsonObject body = parseJsonBody(request);
                    identityStore.renamePasskey(username, credId, getJsonString(body, "name"));
                    sendJson(response, HttpServletResponse.SC_OK, buildPasskeyListPayload(request, username));
                    return;
                }
                if ("DELETE".equalsIgnoreCase(method)) {
                    boolean deleted = identityStore.deletePasskey(username, credId);
                    if (!deleted) {
                        sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "Passkey not found.");
                        return;
                    }
                    sendJson(response, HttpServletResponse.SC_OK, buildPasskeyListPayload(request, username));
                    return;
                }
            }

            // Admin-only routes
            if (subpath.startsWith("/admin/")) {
                if (!identityStore.isAdmin(username)) {
                    sendJsonError(response, HttpServletResponse.SC_FORBIDDEN,
                            "Administrator privileges are required to manage OSCAR users and security policies.");
                    return;
                }
                handleAdminSessionApi(request, response, username, subpath, method);
                return;
            }

            sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "Unknown session endpoint");
        } catch (SecurityException e) {
            sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void handleAdminSessionApi(HttpServletRequest request,
                                       HttpServletResponse response,
                                       String adminUsername,
                                       String subpath,
                                       String method) throws IOException {
        if ("/admin/overview".equals(subpath) && "GET".equalsIgnoreCase(method)) {
            sendJson(response, HttpServletResponse.SC_OK, buildAdminOverviewPayload(request));
            return;
        }

        if ("/admin/users".equals(subpath) && "POST".equalsIgnoreCase(method)) {
            JsonObject body = parseJsonBody(request);
            String newUsername = getJsonString(body, "username");
            String displayName = getJsonString(body, "displayName");
            String role = getJsonString(body, "role");
            List<String> allowedViews = getJsonStringList(body.get("allowedViews"));
            String password = getJsonString(body, "password");
            boolean mustChange = getJsonBoolean(body, "mustChangePassword", true);
            int inviteHours = body.has("inviteExpiresInHours") && body.get("inviteExpiresInHours").isJsonPrimitive()
                    ? body.get("inviteExpiresInHours").getAsInt()
                    : OscarIdentityStore.DEFAULT_INVITE_EXPIRY_HOURS;

            CreatedOrResetUserResult created = identityStore.createUser(
                    newUsername, displayName, role, allowedViews, password, mustChange);
            IssuedEnrollmentInvite invite = identityStore.issueEnrollmentInvite(
                    created.profile().userId, inviteHours, adminUsername);

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("user", serializeAdminUserSummary(created.profile()));
            if (created.generatedPassword() != null)
                out.put("temporaryPassword", created.generatedPassword());
            out.put("inviteToken", invite.rawToken());
            out.put("inviteExpiresAt", invite.metadata().expiresAt);
            sendJson(response, HttpServletResponse.SC_CREATED, out);
            return;
        }

        if ("/admin/users/bulk".equals(subpath) && "POST".equalsIgnoreCase(method)) {
            JsonObject body = parseJsonBody(request);
            boolean reissueExisting = getJsonBoolean(body, "reissueExisting", false);
            int inviteHours = body.has("inviteExpiresInHours") && body.get("inviteExpiresInHours").isJsonPrimitive()
                    ? body.get("inviteExpiresInHours").getAsInt()
                    : OscarIdentityStore.DEFAULT_INVITE_EXPIRY_HOURS;
            List<BulkUserSpec> specs = new ArrayList<>();
            if (body.has("users") && body.get("users").isJsonArray()) {
                for (JsonElement el : body.getAsJsonArray("users")) {
                    if (el != null && el.isJsonObject()) {
                        JsonObject rowObj = el.getAsJsonObject();
                        specs.add(new BulkUserSpec(
                                getJsonString(rowObj, "username"),
                                getJsonString(rowObj, "displayName"),
                                getJsonString(rowObj, "role"),
                                getJsonStringList(rowObj.get("allowedViews"))));
                    }
                }
            }
            BulkEnrollmentOutcome outcome = identityStore.bulkEnrollUsers(
                    specs, reissueExisting, inviteHours, adminUsername);

            List<Map<String, Object>> enrolledItems = new ArrayList<>();
            for (BulkEnrolledUser e : outcome.enrolled()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("user", serializeAdminUserSummary(e.profile()));
                item.put("createdNew", e.createdNew());
                item.put("temporaryPassword", e.temporaryPassword());
                item.put("inviteToken", e.inviteToken());
                item.put("inviteExpiresAt", e.inviteExpiresAt());
                enrolledItems.add(item);
            }
            List<Map<String, Object>> errItems = new ArrayList<>();
            for (BulkRowError err : outcome.errors()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("row", err.row());
                item.put("username", err.username());
                item.put("error", err.error());
                errItems.add(item);
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("createdCount", outcome.createdCount());
            out.put("reissuedCount", outcome.reissuedCount());
            out.put("skippedCount", outcome.skippedCount());
            out.put("enrolled", enrolledItems);
            out.put("errors", errItems);
            sendJson(response, HttpServletResponse.SC_OK, out);
            return;
        }

        if (subpath.startsWith("/admin/users/")) {
            String remainder = subpath.substring("/admin/users/".length());
            if (remainder.endsWith("/reset-password") && "POST".equalsIgnoreCase(method)) {
                String targetUser = urlDecodeSegment(
                        remainder.substring(0, remainder.length() - "/reset-password".length()));
                JsonObject body = parseJsonBody(request);
                String explicitPassword = getJsonString(body, "password");
                boolean mustChange = getJsonBoolean(body, "mustChangePassword", true);
                int inviteHours = body.has("inviteExpiresInHours") && body.get("inviteExpiresInHours").isJsonPrimitive()
                        ? body.get("inviteExpiresInHours").getAsInt()
                        : OscarIdentityStore.DEFAULT_INVITE_EXPIRY_HOURS;
                CreatedOrResetUserResult reset = identityStore.adminResetPassword(
                        targetUser, explicitPassword, mustChange);
                IssuedEnrollmentInvite invite = identityStore.issueEnrollmentInvite(
                        targetUser, inviteHours, adminUsername);
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("user", serializeAdminUserSummary(reset.profile()));
                if (reset.generatedPassword() != null)
                    out.put("temporaryPassword", reset.generatedPassword());
                out.put("inviteToken", invite.rawToken());
                out.put("inviteExpiresAt", invite.metadata().expiresAt);
                sendJson(response, HttpServletResponse.SC_OK, out);
                return;
            }
            if (remainder.endsWith("/invite") && "POST".equalsIgnoreCase(method)) {
                String targetUser = urlDecodeSegment(
                        remainder.substring(0, remainder.length() - "/invite".length()));
                JsonObject body = parseJsonBody(request);
                int inviteHours = body.has("inviteExpiresInHours") && body.get("inviteExpiresInHours").isJsonPrimitive()
                        ? body.get("inviteExpiresInHours").getAsInt()
                        : OscarIdentityStore.DEFAULT_INVITE_EXPIRY_HOURS;
                IssuedEnrollmentInvite invite = identityStore.issueEnrollmentInvite(
                        targetUser, inviteHours, adminUsername);
                UserProfileRecord targetProfile = identityStore.ensureProfile(targetUser);
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("user", serializeAdminUserSummary(targetProfile));
                out.put("inviteToken", invite.rawToken());
                out.put("inviteExpiresAt", invite.metadata().expiresAt);
                sendJson(response, HttpServletResponse.SC_OK, out);
                return;
            }
            if (remainder.endsWith("/revoke-sessions") && "POST".equalsIgnoreCase(method)) {
                String targetUser = urlDecodeSegment(
                        remainder.substring(0, remainder.length() - "/revoke-sessions".length()));
                String keepToken = targetUser.equals(adminUsername) ? readSessionCookie(request) : null;
                int revoked = identityStore.revokeSessionsForUser(targetUser, keepToken);
                sendJson(response, HttpServletResponse.SC_OK, Map.of("revokedSessions", revoked));
                return;
            }

            String targetUser = urlDecodeSegment(remainder);
            if ("PUT".equalsIgnoreCase(method)) {
                JsonObject body = parseJsonBody(request);
                String displayName = getJsonString(body, "displayName");
                String role = getJsonString(body, "role");
                Boolean enabled = body.has("enabled") && body.get("enabled").isJsonPrimitive()
                        ? body.get("enabled").getAsBoolean()
                        : null;
                List<String> allowedViews = body.has("allowedViews")
                        ? getJsonStringList(body.get("allowedViews"))
                        : null;
                UserProfileRecord updated = identityStore.updateUser(
                        adminUsername, targetUser, displayName, role, enabled, allowedViews);
                sendJson(response, HttpServletResponse.SC_OK, Map.of("user", serializeAdminUserSummary(updated)));
                return;
            }
            if ("DELETE".equalsIgnoreCase(method)) {
                identityStore.deleteUser(adminUsername, targetUser);
                sendJson(response, HttpServletResponse.SC_OK, Map.of("deleted", targetUser));
                return;
            }
        }

        if ("/admin/federation-tokens".equals(subpath) && "POST".equalsIgnoreCase(method)) {
            JsonObject body = parseJsonBody(request);
            String label = getJsonString(body, "label");
            String role = getJsonString(body, "role");
            int expiresInDays = body.has("expiresInDays") && body.get("expiresInDays").isJsonPrimitive()
                    ? body.get("expiresInDays").getAsInt()
                    : 0;
            CreatedFederationToken created = identityStore.createFederationToken(
                    label, role == null ? OscarIdentityStore.ROLE_VIEWER : role, expiresInDays, adminUsername);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("token", created.rawToken());
            out.put("record", serializeFederationToken(created.metadata()));
            sendJson(response, HttpServletResponse.SC_CREATED, out);
            return;
        }

        if (subpath.startsWith("/admin/federation-tokens/") && "DELETE".equalsIgnoreCase(method)) {
            String tokenId = urlDecodeSegment(subpath.substring("/admin/federation-tokens/".length()));
            boolean revoked = identityStore.revokeFederationToken(tokenId);
            if (!revoked) {
                sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "Federation token not found.");
                return;
            }
            sendJson(response, HttpServletResponse.SC_OK, Map.of("revoked", tokenId));
            return;
        }

        if ("/admin/policy".equals(subpath) && "PUT".equalsIgnoreCase(method)) {
            JsonObject body = parseJsonBody(request);
            updateSessionAndSsoPolicy(body);
            sendJson(response, HttpServletResponse.SC_OK, buildPolicySummary(request));
            return;
        }

        sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "Unknown admin session endpoint");
    }

    private synchronized void updateSessionAndSsoPolicy(JsonObject body) {
        if (body.has("idleTimeoutSeconds") && body.get("idleTimeoutSeconds").isJsonPrimitive()) {
            int idle = body.get("idleTimeoutSeconds").getAsInt();
            if (idle < 0 || idle > 365 * 86_400)
                throw new IllegalArgumentException("idleTimeoutSeconds must be between 0 and 31536000");
            config.idleTimeoutSeconds = idle;
            this.idleTimeoutMillis = Math.multiplyExact((long) idle, 1000L);
        }
        if (body.has("absoluteTimeoutSeconds") && body.get("absoluteTimeoutSeconds").isJsonPrimitive()) {
            int abs = body.get("absoluteTimeoutSeconds").getAsInt();
            if (abs < 0 || abs > 365 * 86_400)
                throw new IllegalArgumentException("absoluteTimeoutSeconds must be between 0 and 31536000");
            config.absoluteTimeoutSeconds = abs;
            this.absoluteTimeoutMillis = Math.multiplyExact((long) abs, 1000L);
        }
        if (body.has("allowKeepSignedIn") && body.get("allowKeepSignedIn").isJsonPrimitive()) {
            boolean allow = body.get("allowKeepSignedIn").getAsBoolean();
            config.allowKeepSignedIn = allow;
            this.allowKeepSignedIn = allow;
        }
        if (body.has("persistentCookieMaxAgeSeconds") && body.get("persistentCookieMaxAgeSeconds").isJsonPrimitive()) {
            int maxAge = body.get("persistentCookieMaxAgeSeconds").getAsInt();
            if (maxAge < 3600 || maxAge > 5 * 365 * 86_400)
                throw new IllegalArgumentException("persistentCookieMaxAgeSeconds must be between 3600 and 157680000");
            config.persistentCookieMaxAgeSeconds = maxAge;
            this.persistentCookieMaxAgeSeconds = maxAge;
        }
        if (body.has("webAuthnRpId")) {
            String rp = getJsonString(body, "webAuthnRpId");
            config.webAuthnRpId = rp == null ? "" : rp.trim().toLowerCase(Locale.ROOT);
        }
        if (body.has("oidcEnabled") && body.get("oidcEnabled").isJsonPrimitive()) {
            config.oidcEnabled = body.get("oidcEnabled").getAsBoolean();
        }
        if (body.has("oidcProviderName")) {
            String label = getJsonString(body, "oidcProviderName");
            config.oidcProviderName = (label == null || label.isBlank()) ? "Organization SSO" : label.trim();
        }
        if (body.has("oidcIssuerUrl")) {
            String iss = getJsonString(body, "oidcIssuerUrl");
            config.oidcIssuerUrl = iss == null ? "" : iss.trim();
        }
        if (body.has("oidcClientId")) {
            String cid = getJsonString(body, "oidcClientId");
            config.oidcClientId = cid == null ? "" : cid.trim();
        }
        if (body.has("oidcClientSecret")) {
            String sec = getJsonString(body, "oidcClientSecret");
            if (sec != null && !"[UNCHANGED]".equals(sec)) {
                config.oidcClientSecret = sec;
            }
        }
        if (body.has("oidcScopes")) {
            String scopes = getJsonString(body, "oidcScopes");
            config.oidcScopes = (scopes == null || scopes.isBlank()) ? "openid profile email groups" : scopes.trim();
        }
        if (body.has("oidcAdminGroup")) {
            String grp = getJsonString(body, "oidcAdminGroup");
            config.oidcAdminGroup = grp == null ? "" : grp.trim();
        }
        if (body.has("oidcOperatorGroup")) {
            String grp = getJsonString(body, "oidcOperatorGroup");
            config.oidcOperatorGroup = grp == null ? "" : grp.trim();
        }
        if (body.has("oidcDefaultRole")) {
            String defRole = getJsonString(body, "oidcDefaultRole");
            if (defRole != null && ("operator".equalsIgnoreCase(defRole) || "viewer".equalsIgnoreCase(defRole))) {
                config.oidcDefaultRole = defRole.toLowerCase(Locale.ROOT);
            }
        }
        if (body.has("trustedTunnelSsoEnabled") && body.get("trustedTunnelSsoEnabled").isJsonPrimitive()) {
            config.trustedTunnelSsoEnabled = body.get("trustedTunnelSsoEnabled").getAsBoolean();
        }
        if (body.has("trustedTunnelUserHeader")) {
            String hdr = getJsonString(body, "trustedTunnelUserHeader");
            config.trustedTunnelUserHeader = (hdr == null || hdr.isBlank()) ? "X-Octelium-User" : hdr.trim();
        }
        if (body.has("trustedTunnelSharedSecret")) {
            String sec = getJsonString(body, "trustedTunnelSharedSecret");
            if (sec != null && !"[UNCHANGED]".equals(sec)) {
                if (!sec.isEmpty() && sec.length() < 16)
                    throw new IllegalArgumentException("Trusted tunnel shared secret must be at least 16 characters");
                config.trustedTunnelSharedSecret = sec;
            }
        }
        if (body.has("trustedTunnelAutoProvisionViewer") && body.get("trustedTunnelAutoProvisionViewer").isJsonPrimitive()) {
            config.trustedTunnelAutoProvisionViewer = body.get("trustedTunnelAutoProvisionViewer").getAsBoolean();
        }

        if (hub != null && hub.getModuleRegistry() != null) {
            try {
                hub.getModuleRegistry().saveModulesConfiguration();
            } catch (Exception e) {
                log.warn("Could not save configuration after updating session policy", e);
            }
        }
    }

    private void renderCurrentSession(HttpServletRequest request,
                                      HttpServletResponse response,
                                      PersistedSession session) throws IOException {
        String username = session.username;
        UserProfileRecord profile = identityStore.ensureProfile(username);
        Set<String> roles = identityStore.getUserRoles(username);
        String primaryRole = identityStore.getPrimaryRole(username);
        boolean isAdmin = identityStore.isAdmin(username);
        boolean canAdjudicate = identityStore.canAdjudicate(username);
        String rpId = resolveRpId(request);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        payload.put("displayName", profile.displayName != null ? profile.displayName : username);
        payload.put("roles", new ArrayList<>(roles));
        payload.put("primaryRole", primaryRole);
        payload.put("isAdmin", isAdmin);
        payload.put("canAdjudicate", canAdjudicate);
        payload.put("isReadOnly", !canAdjudicate);
        payload.put("allowedViews", profile.allowedViews != null ? profile.allowedViews : List.of());
        payload.put("mustChangePassword", profile.mustChangePassword);
        payload.put("keepSignedIn", session.keepSignedIn);
        payload.put("authSource", profile.authSource != null ? profile.authSource : "local");
        payload.put("rpId", rpId);
        payload.put("rpIdSupported", isDomainRpId(rpId));
        payload.put("passkeyUserId", profile.passkeyUserId);
        payload.put("passkeyCount", profile.passkeys != null ? profile.passkeys.size() : 0);
        payload.put("idleTimeoutSeconds", (int) (idleTimeoutMillis / 1000L));
        payload.put("absoluteTimeoutSeconds", (int) (absoluteTimeoutMillis / 1000L));
        sendJson(response, HttpServletResponse.SC_OK, payload);
    }

    private Map<String, Object> buildPasskeyListPayload(HttpServletRequest request, String username) {
        UserProfileRecord profile = identityStore.ensureProfile(username);
        String rpId = resolveRpId(request);
        List<Map<String, Object>> credentials = new ArrayList<>();
        List<String> acceptedIds = new ArrayList<>();
        if (profile.passkeys != null) {
            for (PasskeyRecord pk : profile.passkeys) {
                acceptedIds.add(pk.id);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", pk.id);
                item.put("name", pk.name);
                item.put("aaguid", pk.aaguid);
                item.put("authenticatorLabel", WebAuthnCodec.friendlyAuthenticatorName(pk.aaguid));
                item.put("backupEligible", pk.backupEligible);
                item.put("backupState", pk.backupState);
                item.put("createdAt", pk.createdAt);
                item.put("lastUsedAt", pk.lastUsedAt);
                credentials.add(item);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rpId", rpId);
        out.put("rpIdSupported", isDomainRpId(rpId));
        out.put("passkeyUserId", profile.passkeyUserId);
        out.put("acceptedCredentialIds", acceptedIds);
        out.put("passkeys", credentials);
        return out;
    }

    private Map<String, Object> buildAdminOverviewPayload(HttpServletRequest request) {
        List<Map<String, Object>> users = new ArrayList<>();
        for (UserProfileRecord p : identityStore.listHumanUsers()) {
            users.add(serializeAdminUserSummary(p));
        }

        List<Map<String, Object>> tokens = new ArrayList<>();
        for (FederationTokenRecord t : identityStore.listFederationTokens()) {
            tokens.add(serializeFederationToken(t));
        }

        Map<String, Object> recovery = new LinkedHashMap<>();
        Path bootstrapFile = Path.of("/run/secrets/oscar_admin_password");
        boolean bootstrapScrubbed = true;
        if (Files.isRegularFile(bootstrapFile)) {
            try {
                String content = Files.readString(bootstrapFile, StandardCharsets.UTF_8).trim();
                bootstrapScrubbed = content.isEmpty() || content.startsWith("# CONSUMED_AT_INIT");
            } catch (IOException ignored) {
                bootstrapScrubbed = true;
            }
        }
        recovery.put("bootstrapPasswordScrubbed", bootstrapScrubbed);
        recovery.put("identityStorePersisted", config.persistSessions);
        recovery.put("includedInBackupArchive", "oscar-state.tar.gz + deployment-secrets.tar.gz.enc (with remote-access/)");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("users", users);
        out.put("federationTokens", tokens);
        out.put("policy", buildPolicySummary(request));
        out.put("recovery", recovery);
        return out;
    }

    private Map<String, Object> serializeAdminUserSummary(UserProfileRecord p) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("username", p.userId);
        item.put("displayName", p.displayName != null ? p.displayName : p.userId);
        item.put("role", identityStore.getPrimaryRole(p.userId));
        item.put("roles", new ArrayList<>(identityStore.getUserRoles(p.userId)));
        item.put("enabled", p.enabled);
        item.put("mustChangePassword", p.mustChangePassword);
        item.put("allowedViews", p.allowedViews != null ? p.allowedViews : List.of());
        item.put("authSource", p.authSource != null ? p.authSource : "local");
        item.put("passkeyCount", p.passkeys != null ? p.passkeys.size() : 0);
        item.put("activeSessions", identityStore.countActiveSessionsForUser(
                p.userId, idleTimeoutMillis, absoluteTimeoutMillis));
        item.put("createdAt", p.createdAt);
        item.put("lastLoginAt", p.lastLoginAt);
        return item;
    }

    private static Map<String, Object> serializeFederationToken(FederationTokenRecord t) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", t.id);
        item.put("serviceUserId", t.serviceUserId);
        item.put("label", t.label);
        item.put("role", t.role);
        item.put("tokenPrefix", t.tokenPrefix);
        item.put("enabled", t.enabled);
        item.put("createdAt", t.createdAt);
        item.put("createdBy", t.createdBy);
        item.put("expiresAt", t.expiresAt);
        item.put("lastUsedAt", t.lastUsedAt);
        return item;
    }

    private Map<String, Object> buildPolicySummary(HttpServletRequest request) {
        String rpId = resolveRpId(request);
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("idleTimeoutSeconds", (int) (idleTimeoutMillis / 1000L));
        policy.put("absoluteTimeoutSeconds", (int) (absoluteTimeoutMillis / 1000L));
        policy.put("persistSessions", config.persistSessions);
        policy.put("allowKeepSignedIn", allowKeepSignedIn);
        policy.put("persistentCookieMaxAgeSeconds", persistentCookieMaxAgeSeconds);
        policy.put("sameSite", sameSite);
        policy.put("secureCookie", secureCookie);
        policy.put("webAuthnRpId", config.webAuthnRpId == null ? "" : config.webAuthnRpId);
        policy.put("effectiveRpId", rpId);
        policy.put("rpIdSupported", isDomainRpId(rpId));
        policy.put("oidcEnabled", config.oidcEnabled);
        policy.put("oidcProviderName", config.oidcProviderName);
        policy.put("oidcIssuerUrl", config.oidcIssuerUrl);
        policy.put("oidcClientId", config.oidcClientId);
        policy.put("oidcHasClientSecret", config.oidcClientSecret != null && !config.oidcClientSecret.isEmpty());
        policy.put("oidcScopes", config.oidcScopes);
        policy.put("oidcAdminGroup", config.oidcAdminGroup);
        policy.put("oidcOperatorGroup", config.oidcOperatorGroup);
        policy.put("oidcDefaultRole", config.oidcDefaultRole);
        policy.put("trustedTunnelSsoEnabled", config.trustedTunnelSsoEnabled);
        policy.put("trustedTunnelUserHeader", config.trustedTunnelUserHeader);
        policy.put("trustedTunnelHasSharedSecret",
                config.trustedTunnelSharedSecret != null && config.trustedTunnelSharedSecret.length() >= 16);
        policy.put("trustedTunnelAutoProvisionViewer", config.trustedTunnelAutoProvisionViewer);
        return policy;
    }

    // -------------------------------------------------------------------------
    // Logout & HTML Rendering
    // -------------------------------------------------------------------------

    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = readSessionCookie(request);
        if (token != null)
            identityStore.removeSession(token);
        expireCookie(response);
        HttpSession servletSession = request.getSession(false);
        try {
            request.logout();
        } catch (ServletException e) {
            log.warn("Cannot clear the servlet authentication state during logout", e);
        }
        if (servletSession != null) {
            try {
                servletSession.invalidate();
            } catch (IllegalStateException e) {
                log.debug("Servlet session was already invalidated during logout");
            }
        }

        if ("GET".equalsIgnoreCase(request.getMethod()))
            response.sendRedirect(LOGIN_PATH);
        else
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private void renderLogin(HttpServletResponse response,
                             boolean failed,
                             String customError,
                             String redirectTarget,
                             String lang) throws IOException {
        LoginCopy copy = LoginCopy.forLanguage(lang);
        String errText = customError != null ? customError : copy.error;
        String error = failed ? "<p class=\"error\" role=\"alert\">" + escapeHtml(errText) + "</p>" : "";
        String continueInput = "<input type=\"hidden\" name=\"continue\" value=\""
                + escapeHtml(redirectTarget) + "\">";
        String langInput = "<input type=\"hidden\" name=\"lang\" value=\""
                + escapeHtml(copy.lang) + "\">";
        String encodedContinue = URLEncoder.encode(redirectTarget, StandardCharsets.UTF_8);

        String keepSignedInHtml = "";
        if (allowKeepSignedIn) {
            keepSignedInHtml = "<input type=\"hidden\" name=\"keepSignedInPresent\" value=\"1\">"
                    + "<label class=\"checkbox-row\" for=\"keep-signed-in\">"
                    + "<input id=\"keep-signed-in\" name=\"keepSignedIn\" type=\"checkbox\" value=\"true\" checked>"
                    + "<span>" + escapeHtml(copy.keepSignedIn) + "</span></label>";
        }

        String passkeySection = "<div id=\"passkey-section\" class=\"alt-auth\" hidden>"
                + "<div class=\"divider\"><span>" + escapeHtml(copy.orDivider) + "</span></div>"
                + "<button type=\"button\" id=\"passkey-signin-btn\" class=\"secondary-btn\""
                + " data-unknown-msg=\"" + escapeHtml(copy.passkeyUnknown) + "\""
                + " data-error-msg=\"" + escapeHtml(copy.passkeyFailed) + "\">"
                + escapeHtml(copy.passkeySignIn) + "</button>"
                + "<p id=\"passkey-status\" class=\"error\" role=\"alert\" hidden></p>"
                + "</div>";

        String oidcSection = "";
        if (config.oidcEnabled && config.oidcIssuerUrl != null && !config.oidcIssuerUrl.isBlank()) {
            String provider = (config.oidcProviderName == null || config.oidcProviderName.isBlank())
                    ? "Organization SSO"
                    : config.oidcProviderName.trim();
            String ssoLabel = copy.ssoSignInPrefix + " " + provider;
            String oidcHref = LOGIN_OIDC_PATH + "?continue=" + escapeHtml(encodedContinue);
            oidcSection = "<div class=\"alt-auth\"><a class=\"sso-link\" href=\"" + oidcHref + "\">"
                    + escapeHtml(ssoLabel) + "</a></div>";
        }

        String langNav = "<nav class=\"langs\" aria-label=\"" + escapeHtml(copy.languageAria) + "\">"
                + renderLanguageLink("en", "English", copy.lang, encodedContinue)
                + "<span aria-hidden=\"true\">&middot;</span>"
                + renderLanguageLink("es", "Espa\u00f1ol", copy.lang, encodedContinue)
                + "<span aria-hidden=\"true\">&middot;</span>"
                + renderLanguageLink("fr", "Fran\u00e7ais", copy.lang, encodedContinue)
                + "<span aria-hidden=\"true\">&middot;</span>"
                + renderLanguageLink("el", "\u0395\u03bb\u03bb\u03b7\u03bd\u03b9\u03ba\u03ac", copy.lang, encodedContinue)
                + "</nav>";

        String page = "<!doctype html><html lang=\"" + escapeHtml(copy.lang) + "\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + escapeHtml(copy.title) + "</title><style>"
                + "body{font:16px system-ui,-apple-system,sans-serif;background:#f4f6f8;color:#0f172a;"
                + "display:grid;place-items:center;min-height:100vh;margin:0}"
                + "main{background:#fff;padding:2rem;border-radius:.65rem;box-shadow:0 6px 24px #0f172a18;"
                + "width:min(25rem,calc(100% - 2.5rem))}"
                + "h1{margin:0 0 .25rem;font-size:1.6rem;letter-spacing:.02em}p.sub{margin:0 0 1rem;color:#475569}"
                + "label{display:block;margin-top:1rem;font-weight:500;font-size:.95rem}"
                + "input[type=text],input[type=password],button,.sso-link{display:block;width:100%;box-sizing:border-box;font:inherit}"
                + "input[type=text],input[type=password]{padding:.65rem .75rem;margin-top:.35rem;border:1px solid #cbd5e1;border-radius:.4rem}"
                + "input:focus{outline:2px solid #0f4c81;outline-offset:1px}"
                + ".pw-wrap{position:relative;display:flex;align-items:center;gap:.4rem;margin-top:.35rem}"
                + ".pw-wrap input{margin-top:0;flex:1}"
                + ".pw-toggle{width:auto;padding:.65rem .75rem;margin:0;background:#f8fafc;border:1px solid #cbd5e1;"
                + "border-radius:.4rem;color:#334155;font-size:.875rem;cursor:pointer}"
                + ".checkbox-row{display:flex;align-items:center;gap:.55rem;margin-top:1rem;font-weight:400;font-size:.9rem;color:#334155;cursor:pointer}"
                + ".checkbox-row input{width:1.05rem;height:1.05rem;margin:0}"
                + "button[type=submit]{margin-top:1.25rem;padding:.7rem;background:#0f4c81;color:#fff;border:none;border-radius:.4rem;font-weight:600;cursor:pointer}"
                + "button[type=submit]:hover{background:#0c3b66}"
                + ".alt-auth{margin-top:.9rem}.divider{display:flex;align-items:center;gap:.65rem;color:#64748b;font-size:.82rem;margin:.85rem 0}"
                + ".divider::before,.divider::after{content:\"\";flex:1;border-top:1px solid #e2e8f0}"
                + ".secondary-btn,.sso-link{padding:.65rem;background:#fff;color:#0f172a;border:1px solid #94a3b8;"
                + "border-radius:.4rem;font-weight:500;text-align:center;text-decoration:none;cursor:pointer}"
                + ".secondary-btn:hover,.sso-link:hover{background:#f8fafc}"
                + ".error{color:#b91c1c;background:#fef2f2;border:1px solid #fecaca;padding:.6rem .75rem;border-radius:.4rem;font-size:.9rem;margin:.75rem 0 0}"
                + ".langs{margin-top:1.35rem;padding-top:.85rem;border-top:1px solid #e2e8f0;"
                + "display:flex;flex-wrap:wrap;justify-content:center;gap:.45rem;font-size:.875rem;color:#475569}"
                + ".langs a{color:#0f4c81;text-decoration:none}.langs a[aria-current=\"true\"]{font-weight:700;color:#0f172a;text-decoration:underline}"
                + "</style></head><body><main><h1>OSCAR</h1><p class=\"sub\">" + escapeHtml(copy.subtitle) + "</p>"
                + error + "<form method=\"post\" action=\"" + LOGIN_PATH + "\">"
                + continueInput + langInput
                + "<label for=\"username\">" + escapeHtml(copy.username) + "</label>"
                + "<input id=\"username\" name=\"username\" type=\"text\" required maxlength=\"256\" autofocus"
                + " autocomplete=\"username webauthn\" spellcheck=\"false\" autocorrect=\"off\" autocapitalize=\"none\" enterkeyhint=\"next\">"
                + "<label for=\"current-password\">" + escapeHtml(copy.password) + "</label>"
                + "<div class=\"pw-wrap\">"
                + "<input id=\"current-password\" name=\"password\" type=\"password\" required maxlength=\"4096\""
                + " autocomplete=\"current-password\" spellcheck=\"false\" autocorrect=\"off\" autocapitalize=\"none\" enterkeyhint=\"done\">"
                + "<button type=\"button\" id=\"toggle-password\" class=\"pw-toggle\" aria-controls=\"current-password\""
                + " aria-pressed=\"false\" data-show-label=\"" + escapeHtml(copy.showPassword) + "\""
                + " data-hide-label=\"" + escapeHtml(copy.hidePassword) + "\" hidden>" + escapeHtml(copy.showPassword) + "</button>"
                + "</div>"
                + keepSignedInHtml
                + "<button type=\"submit\">" + escapeHtml(copy.submit) + "</button></form>"
                + passkeySection
                + oidcSection
                + langNav
                + "<script src=\"" + LOGIN_JS_PATH + "\" defer></script></main></body></html>";

        byte[] body = page.getBytes(StandardCharsets.UTF_8);
        response.setStatus(failed ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_OK);
        response.setContentType("text/html; charset=UTF-8");
        response.setContentLength(body.length);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Content-Security-Policy", LOGIN_CSP);
        response.setHeader("Referrer-Policy", "no-referrer");
        response.getOutputStream().write(body);
    }

    private void renderPasswordChange(HttpServletResponse response,
                                      boolean failed,
                                      String errorMsg,
                                      String redirectTarget,
                                      LoginCopy copy) throws IOException {
        String error = (failed && errorMsg != null)
                ? "<p class=\"error\" role=\"alert\">" + escapeHtml(errorMsg) + "</p>"
                : "";
        String page = "<!doctype html><html lang=\"" + escapeHtml(copy.lang) + "\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + escapeHtml(copy.changePasswordTitle) + "</title><style>"
                + "body{font:16px system-ui,-apple-system,sans-serif;background:#f4f6f8;color:#0f172a;display:grid;place-items:center;min-height:100vh;margin:0}"
                + "main{background:#fff;padding:2rem;border-radius:.65rem;box-shadow:0 6px 24px #0f172a18;width:min(25rem,calc(100% - 2.5rem))}"
                + "h1{margin:0 0 .35rem;font-size:1.35rem}p{margin:0 0 1rem;color:#475569;font-size:.92rem}"
                + "label{display:block;margin-top:1rem;font-weight:500}"
                + "input,button{display:block;width:100%;box-sizing:border-box;font:inherit;padding:.65rem .75rem;margin-top:.35rem;border-radius:.4rem}"
                + "input{border:1px solid #cbd5e1}.pw-wrap{display:flex;gap:.4rem;margin-top:.35rem}.pw-wrap input{margin-top:0;flex:1}"
                + ".pw-toggle{width:auto;margin:0;background:#f8fafc;border:1px solid #cbd5e1;cursor:pointer;font-size:.875rem}"
                + "button[type=submit]{margin-top:1.35rem;background:#0f4c81;color:#fff;border:none;font-weight:600;cursor:pointer}"
                + ".error{color:#b91c1c;background:#fef2f2;border:1px solid #fecaca;padding:.6rem .75rem;border-radius:.4rem}"
                + "</style></head><body><main><h1>" + escapeHtml(copy.changePasswordTitle) + "</h1>"
                + "<p>" + escapeHtml(copy.changePasswordSubtitle) + "</p>"
                + error
                + "<form method=\"post\" action=\"" + LOGIN_PASSWORD_CHANGE_PATH + "\">"
                + "<input type=\"hidden\" name=\"continue\" value=\"" + escapeHtml(redirectTarget) + "\">"
                + "<label for=\"new-password\">" + escapeHtml(copy.newPasswordLabel) + "</label>"
                + "<div class=\"pw-wrap\">"
                + "<input id=\"new-password\" name=\"newPassword\" type=\"password\" required minlength=\"12\" maxlength=\"512\""
                + " autocomplete=\"new-password\" spellcheck=\"false\" autocorrect=\"off\" autocapitalize=\"none\" autofocus>"
                + "<button type=\"button\" id=\"toggle-new-password\" class=\"pw-toggle\" aria-controls=\"new-password\""
                + " aria-pressed=\"false\" data-show-label=\"" + escapeHtml(copy.showPassword) + "\""
                + " data-hide-label=\"" + escapeHtml(copy.hidePassword) + "\" hidden>" + escapeHtml(copy.showPassword) + "</button>"
                + "</div>"
                + "<label for=\"confirm-password\">" + escapeHtml(copy.confirmPasswordLabel) + "</label>"
                + "<input id=\"confirm-password\" name=\"confirmPassword\" type=\"password\" required minlength=\"12\" maxlength=\"512\""
                + " autocomplete=\"new-password\" spellcheck=\"false\" autocorrect=\"off\" autocapitalize=\"none\">"
                + "<button type=\"submit\">" + escapeHtml(copy.savePasswordButton) + "</button></form>"
                + "<script src=\"" + LOGIN_JS_PATH + "\" defer></script></main></body></html>";

        byte[] body = page.getBytes(StandardCharsets.UTF_8);
        response.setStatus(failed ? HttpServletResponse.SC_BAD_REQUEST : HttpServletResponse.SC_OK);
        response.setContentType("text/html; charset=UTF-8");
        response.setContentLength(body.length);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Content-Security-Policy", LOGIN_CSP);
        response.setHeader("Referrer-Policy", "no-referrer");
        response.getOutputStream().write(body);
    }

    private static String renderLanguageLink(String code, String label, String activeLang, String encodedContinue) {
        String currentAttr = code.equals(activeLang) ? " aria-current=\"true\"" : "";
        return "<a href=\"" + LOGIN_PATH + "?lang=" + code + "&amp;continue=" + escapeHtml(encodedContinue)
                + "\" lang=\"" + code + "\"" + currentAttr + ">" + escapeHtml(label) + "</a>";
    }

    // -------------------------------------------------------------------------
    // Helpers (Cookies, Origins, JSON, Escaping, Localization)
    // -------------------------------------------------------------------------

    private String normalizedRequestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (uri != null && !uri.isEmpty()) {
            if (ctx != null && !ctx.isEmpty() && !"/".equals(ctx) && uri.startsWith(ctx)) {
                String stripped = uri.substring(ctx.length());
                return stripped.isEmpty() ? "/" : stripped;
            }
            return uri;
        }
        String sp = request.getServletPath();
        return sp == null ? "/" : sp;
    }

    private String buildContinueFromRequest(HttpServletRequest request) {
        String target = request.getRequestURI();
        if (target == null || target.isEmpty())
            target = defaultRedirectPath;
        if (request.getQueryString() != null && !request.getQueryString().isEmpty())
            target += "?" + request.getQueryString();
        return target;
    }

    private boolean acceptsHtml(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }

    private String resolveRpId(HttpServletRequest request) {
        if (config.webAuthnRpId != null && !config.webAuthnRpId.isBlank())
            return config.webAuthnRpId.trim().toLowerCase(Locale.ROOT);
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String host = (forwardedHost != null && !forwardedHost.isBlank())
                ? forwardedHost.split(",")[0].trim()
                : request.getHeader("Host");
        if (host == null || host.isBlank())
            host = request.getServerName();
        if (host == null || host.isBlank())
            return "localhost";
        if (host.startsWith("[")) {
            int end = host.indexOf(']');
            return end > 0 ? host.substring(1, end) : host;
        }
        int colon = host.indexOf(':');
        return (colon >= 0 ? host.substring(0, colon) : host).toLowerCase(Locale.ROOT);
    }

    static boolean isDomainRpId(String rpId) {
        if (rpId == null || rpId.isBlank())
            return false;
        if ("localhost".equals(rpId))
            return true;
        if (rpId.contains(":")) // IPv6 literal
            return false;
        if (rpId.matches("^([0-9]{1,3}\\.){3}[0-9]{1,3}$")) // IPv4 literal
            return false;
        return rpId.matches("^[a-z0-9]([a-z0-9.-]{0,251}[a-z0-9])?$");
    }

    private Set<String> resolveAllowedOrigins(HttpServletRequest request, String rpId) {
        Set<String> origins = new LinkedHashSet<>();
        String extOrigin = buildExternalOrigin(request);
        origins.add(extOrigin);
        origins.add("https://" + rpId);
        if (!secureCookie) {
            origins.add("http://" + rpId);
            origins.add("http://localhost:8282");
            origins.add("http://localhost:3000");
        }
        return origins;
    }

    private String buildExternalOrigin(HttpServletRequest request) {
        String proto = request.getHeader("X-Forwarded-Proto");
        if (proto == null || proto.isBlank())
            proto = secureCookie ? "https" : (request.getScheme() != null ? request.getScheme() : "https");
        else
            proto = proto.split(",")[0].trim().toLowerCase(Locale.ROOT);

        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank())
            host = request.getHeader("Host");
        else
            host = host.split(",")[0].trim();

        if (host == null || host.isBlank()) {
            String serverName = request.getServerName() != null ? request.getServerName() : "localhost";
            int port = request.getServerPort();
            boolean defaultPort = ("https".equals(proto) && port == 443)
                    || ("http".equals(proto) && port == 80)
                    || port <= 0;
            host = defaultPort ? serverName : serverName + ":" + port;
        }
        return proto + "://" + host;
    }

    private boolean requireJsonSameOrigin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contentType = request.getHeader("Content-Type");
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
            sendJsonError(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    "Content-Type must be application/json");
            return false;
        }
        String secFetchSite = request.getHeader("Sec-Fetch-Site");
        if (secFetchSite != null && !"same-origin".equalsIgnoreCase(secFetchSite)
                && !"none".equalsIgnoreCase(secFetchSite)) {
            sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Cross-site requests are not permitted");
            return false;
        }
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank() && !"null".equalsIgnoreCase(origin)) {
            Set<String> allowed = resolveAllowedOrigins(request, resolveRpId(request));
            if (!allowed.contains(origin.trim())) {
                sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Origin mismatch");
                return false;
            }
        }
        return true;
    }

    private static JsonObject parseJsonBody(HttpServletRequest request) throws IOException {
        InputStream in = request.getInputStream();
        if (in == null)
            return new JsonObject();
        byte[] bytes = in.readNBytes(MAX_JSON_BODY_BYTES + 1);
        if (bytes.length > MAX_JSON_BODY_BYTES)
            throw new IllegalArgumentException("JSON request body exceeds maximum size");
        if (bytes.length == 0)
            return new JsonObject();
        try {
            JsonElement parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Malformed JSON request body");
        }
    }

    private static String getJsonString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key))
            return null;
        JsonElement el = obj.get(key);
        return (el != null && el.isJsonPrimitive()) ? el.getAsString() : null;
    }

    private static boolean getJsonBoolean(JsonObject obj, String key, boolean defaultVal) {
        if (obj == null || !obj.has(key))
            return defaultVal;
        JsonElement el = obj.get(key);
        return (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean())
                ? el.getAsBoolean()
                : defaultVal;
    }

    private static List<String> getJsonStringList(JsonElement el) {
        List<String> list = new ArrayList<>();
        if (el == null || !el.isJsonArray())
            return list;
        JsonArray arr = el.getAsJsonArray();
        for (JsonElement item : arr) {
            if (item != null && item.isJsonPrimitive()) {
                list.add(item.getAsString());
            }
        }
        return list;
    }

    private static String urlDecodeSegment(String raw) {
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    private static String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank())
            return realIp.trim();
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "unknown";
    }

    private static void sendJson(HttpServletResponse response, int status, Object payload) throws IOException {
        byte[] body = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.setContentLength(body.length);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getOutputStream().write(body);
    }

    private static void sendJsonError(HttpServletResponse response, int status, String message) throws IOException {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error", message == null ? "Request failed" : message);
        sendJson(response, status, err);
    }

    private String resolveLoginLanguage(HttpServletRequest request) {
        String fromCookie = normalizeSupportedLanguage(readCookie(request, LANGUAGE_COOKIE_NAME));
        if (fromCookie != null)
            return fromCookie;
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null && !acceptLanguage.trim().isEmpty()) {
            try {
                List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguage);
                for (Locale.LanguageRange range : ranges) {
                    String candidate = normalizeSupportedLanguage(range.getRange());
                    if (candidate != null)
                        return candidate;
                }
            } catch (IllegalArgumentException ignored) {
                for (String token : acceptLanguage.split(",")) {
                    int semi = token.indexOf(';');
                    String tag = semi >= 0 ? token.substring(0, semi) : token;
                    String candidate = normalizeSupportedLanguage(tag);
                    if (candidate != null)
                        return candidate;
                }
            }
        }
        return "en";
    }

    static String normalizeSupportedLanguage(String raw) {
        if (raw == null)
            return null;
        String cleaned = raw.trim().toLowerCase(Locale.ROOT);
        if (cleaned.isEmpty() || "*".equals(cleaned))
            return null;
        int dash = cleaned.indexOf('-');
        if (dash >= 0)
            cleaned = cleaned.substring(0, dash);
        int underscore = cleaned.indexOf('_');
        if (underscore >= 0)
            cleaned = cleaned.substring(0, underscore);
        switch (cleaned) {
            case "en":
            case "es":
            case "fr":
            case "el":
                return cleaned;
            default:
                return null;
        }
    }

    private void setLanguageCookie(HttpServletResponse response, String lang) {
        response.addHeader("Set-Cookie", LANGUAGE_COOKIE_NAME + "=" + lang
                + "; Path=/; Max-Age=" + LANGUAGE_COOKIE_MAX_AGE
                + "; SameSite=Lax" + (secureCookie ? "; Secure" : ""));
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String target = buildContinueFromRequest(request);
        String loginUrl = LOGIN_PATH + "?continue="
                + URLEncoder.encode(redirectTarget(target), StandardCharsets.UTF_8);
        response.sendRedirect(response.encodeRedirectURL(loginUrl));
    }

    private String redirectTarget(String candidate) {
        return isSafeRedirectTarget(candidate) ? candidate : defaultRedirectPath;
    }

    private static boolean isSafeRedirectTarget(String target) {
        if (target == null || target.isEmpty() || !target.startsWith("/") || target.startsWith("//")
                || target.indexOf('\\') >= 0)
            return false;
        for (int i = 0; i < target.length(); i++) {
            char c = target.charAt(i);
            if (c < 0x20 || c == 0x7f)
                return false;
        }
        return true;
    }

    private static String escapeHtml(String value) {
        if (value == null)
            return "";
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            switch (value.charAt(i)) {
                case '&': escaped.append("&amp;"); break;
                case '"': escaped.append("&quot;"); break;
                case '<': escaped.append("&lt;"); break;
                case '>': escaped.append("&gt;"); break;
                default: escaped.append(value.charAt(i));
            }
        }
        return escaped.toString();
    }

    private String readSessionCookie(HttpServletRequest request) {
        return readCookie(request, cookieName);
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()))
                return cookie.getValue();
        }
        return null;
    }

    private void createSession(HttpServletRequest request,
                               HttpServletResponse response,
                               String username,
                               boolean keepSignedIn) {
        String previousToken = readSessionCookie(request);
        boolean persistentCookie = allowKeepSignedIn && keepSignedIn;
        String token = identityStore.createSession(previousToken, username, persistentCookie);
        setSessionCookie(response, token, persistentCookie);
    }

    private UserIdentity withoutCredentials(String username) {
        IUserInfo info = securityManager != null ? securityManager.getUserInfo(username) : null;
        Set<String> roles = (info != null && info.getRoles() != null)
                ? new HashSet<>(info.getRoles())
                : Set.of();
        return new CredentialFreeUserIdentity(username, roles);
    }

    private void setSessionCookie(HttpServletResponse response, String value, boolean persistentCookie) {
        String maxAgeAttr = "";
        if (persistentCookie) {
            long effectiveMaxAge = persistentCookieMaxAgeSeconds;
            if (absoluteTimeoutMillis > 0) {
                effectiveMaxAge = Math.min(effectiveMaxAge, Math.max(60L, absoluteTimeoutMillis / 1000L));
            }
            maxAgeAttr = "; Max-Age=" + effectiveMaxAge;
        }
        response.addHeader("Set-Cookie", cookieName + "=" + value + maxAgeAttr + cookieAttributes());
    }

    private void expireCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", cookieName + "=; Max-Age=0" + cookieAttributes());
    }

    private String cookieAttributes() {
        return "; Path=/; HttpOnly; SameSite=" + sameSite + (secureCookie ? "; Secure" : "");
    }

    private void pruneExpiredSessions() {
        long now = System.currentTimeMillis();
        if (now < nextPruneAt)
            return;
        nextPruneAt = now + PRUNE_INTERVAL_MILLIS;
        identityStore.pruneExpiredSessions(idleTimeoutMillis, absoluteTimeoutMillis);
    }

    void close() {
        identityStore.closeInMemory();
    }

    private static final class CredentialFreeUserIdentity implements UserIdentity {
        private final Principal principal;
        private final Set<String> roles;
        private final Subject subject;

        CredentialFreeUserIdentity(String username, Set<String> roles) {
            this.principal = new NamedPrincipal(username);
            this.roles = roles;
            this.subject = new Subject();
            this.subject.getPrincipals().add(principal);
            this.subject.setReadOnly();
        }

        @Override
        public Subject getSubject() {
            return subject;
        }

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        @Override
        public boolean isUserInRole(String role, Scope scope) {
            return roles.contains(role);
        }
    }

    private static final class LoginCopy {
        final String lang;
        final String title;
        final String subtitle;
        final String error;
        final String rateLimited;
        final String username;
        final String password;
        final String showPassword;
        final String hidePassword;
        final String keepSignedIn;
        final String submit;
        final String orDivider;
        final String passkeySignIn;
        final String passkeyUnknown;
        final String passkeyFailed;
        final String ssoSignInPrefix;
        final String oidcUnavailable;
        final String changePasswordTitle;
        final String changePasswordSubtitle;
        final String newPasswordLabel;
        final String confirmPasswordLabel;
        final String savePasswordButton;
        final String passwordMismatch;
        final String languageAria;

        private LoginCopy(String lang, String title, String subtitle, String error, String rateLimited,
                          String username, String password, String showPassword, String hidePassword,
                          String keepSignedIn, String submit, String orDivider,
                          String passkeySignIn, String passkeyUnknown, String passkeyFailed,
                          String ssoSignInPrefix, String oidcUnavailable,
                          String changePasswordTitle, String changePasswordSubtitle,
                          String newPasswordLabel, String confirmPasswordLabel,
                          String savePasswordButton, String passwordMismatch, String languageAria) {
            this.lang = lang;
            this.title = title;
            this.subtitle = subtitle;
            this.error = error;
            this.rateLimited = rateLimited;
            this.username = username;
            this.password = password;
            this.showPassword = showPassword;
            this.hidePassword = hidePassword;
            this.keepSignedIn = keepSignedIn;
            this.submit = submit;
            this.orDivider = orDivider;
            this.passkeySignIn = passkeySignIn;
            this.passkeyUnknown = passkeyUnknown;
            this.passkeyFailed = passkeyFailed;
            this.ssoSignInPrefix = ssoSignInPrefix;
            this.oidcUnavailable = oidcUnavailable;
            this.changePasswordTitle = changePasswordTitle;
            this.changePasswordSubtitle = changePasswordSubtitle;
            this.newPasswordLabel = newPasswordLabel;
            this.confirmPasswordLabel = confirmPasswordLabel;
            this.savePasswordButton = savePasswordButton;
            this.passwordMismatch = passwordMismatch;
            this.languageAria = languageAria;
        }

        private static final LoginCopy EN = new LoginCopy(
                "en",
                "OSCAR sign in",
                "Sign in to continue.",
                "Invalid username or password.",
                "Too many failed attempts. Please wait one minute before trying again.",
                "Username",
                "Password",
                "Show",
                "Hide",
                "Keep me signed in until I log out",
                "Sign in",
                "or",
                "Sign in with a passkey",
                "This passkey is no longer registered on this OSCAR node.",
                "Passkey sign-in could not be completed. Use your password or try again.",
                "Sign in with",
                "Organization SSO is currently unreachable. Sign in with your local OSCAR password or passkey.",
                "Update your OSCAR password",
                "Choose a new password (at least 12 characters) to finish signing in.",
                "New password",
                "Confirm new password",
                "Save password & continue",
                "Passwords do not match.",
                "Language");

        private static final LoginCopy ES = new LoginCopy(
                "es",
                "Iniciar sesi\u00f3n en OSCAR",
                "Inicie sesi\u00f3n para continuar.",
                "Usuario o contrase\u00f1a no v\u00e1lidos.",
                "Demasiados intentos fallidos. Espere un minuto antes de volver a intentarlo.",
                "Usuario",
                "Contrase\u00f1a",
                "Mostrar",
                "Ocultar",
                "Mantener la sesi\u00f3n iniciada hasta cerrar sesi\u00f3n",
                "Iniciar sesi\u00f3n",
                "o",
                "Iniciar sesi\u00f3n con llave de acceso (Passkey)",
                "Esta llave de acceso ya no est\u00e1 registrada en este nodo OSCAR.",
                "No se pudo completar el acceso con llave. Use su contrase\u00f1a o int\u00e9ntelo de nuevo.",
                "Iniciar sesi\u00f3n con",
                "El inicio de sesi\u00f3n corporativo no est\u00e1 disponible. Use su contrase\u00f1a o llave local de OSCAR.",
                "Actualizar contrase\u00f1a de OSCAR",
                "Elija una contrase\u00f1a nueva (m\u00ednimo 12 caracteres) para continuar.",
                "Nueva contrase\u00f1a",
                "Confirmar nueva contrase\u00f1a",
                "Guardar contrase\u00f1a y continuar",
                "Las contrase\u00f1as no coinciden.",
                "Idioma");

        private static final LoginCopy FR = new LoginCopy(
                "fr",
                "Connexion OSCAR",
                "Connectez-vous pour continuer.",
                "Nom d\u2019utilisateur ou mot de passe incorrect.",
                "Trop de tentatives infructueuses. Veuillez patienter une minute.",
                "Utilisateur",
                "Mot de passe",
                "Afficher",
                "Masquer",
                "Rester connect\u00e9 jusqu\u2019\u00e0 la d\u00e9connexion",
                "Se connecter",
                "ou",
                "Se connecter avec une cl\u00e9 d\u2019acc\u00e8s (Passkey)",
                "Cette cl\u00e9 d\u2019acc\u00e8s n\u2019est plus enregistr\u00e9e sur ce n\u0153ud OSCAR.",
                "La connexion par cl\u00e9 d\u2019acc\u00e8s a \u00e9chou\u00e9. Utilisez votre mot de passe ou r\u00e9essayez.",
                "Se connecter avec",
                "Le fournisseur SSO est inaccessible. Connectez-vous avec votre mot de passe ou cl\u00e9 locale OSCAR.",
                "Mettre \u00e0 jour votre mot de passe OSCAR",
                "Choisissez un nouveau mot de passe (au moins 12 caract\u00e8res) pour continuer.",
                "Nouveau mot de passe",
                "Confirmer le nouveau mot de passe",
                "Enregistrer et continuer",
                "Les mots de passe ne correspondent pas.",
                "Langue");

        private static final LoginCopy EL = new LoginCopy(
                "el",
                "\u03a3\u03cd\u03bd\u03b4\u03b5\u03c3\u03b7 OSCAR",
                "\u03a3\u03c5\u03bd\u03b4\u03b5\u03b8\u03b5\u03af\u03c4\u03b5 \u03b3\u03b9\u03b1 \u03c3\u03c5\u03bd\u03ad\u03c7\u03b5\u03b9\u03b1.",
                "\u039c\u03b7 \u03ad\u03b3\u03ba\u03c5\u03c1\u03bf \u03cc\u03bd\u03bf\u03bc\u03b1 \u03c7\u03c1\u03ae\u03c3\u03c4\u03b7 \u03ae \u03ba\u03c9\u03b4\u03b9\u03ba\u03cc\u03c2.",
                "\u03a0\u03ac\u03c1\u03b1 \u03c0\u03bf\u03bb\u03bb\u03ad\u03c2 \u03b1\u03c0\u03bf\u03c4\u03c5\u03c7\u03b7\u03bc\u03ad\u03bd\u03b5\u03c2 \u03c0\u03c1\u03bf\u03c3\u03c0\u03ac\u03b8\u03b5\u03b9\u03b5\u03c2. \u03a0\u03b5\u03c1\u03b9\u03bc\u03ad\u03bd\u03b5\u03c4\u03b5 \u03ad\u03bd\u03b1 \u03bb\u03b5\u03c0\u03c4\u03cc.",
                "\u038c\u03bd\u03bf\u03bc\u03b1 \u03c7\u03c1\u03ae\u03c3\u03c4\u03b7",
                "\u039a\u03c9\u03b4\u03b9\u03ba\u03cc\u03c2 \u03c0\u03c1\u03cc\u03c3\u03b2\u03b1\u03c3\u03b7\u03c2",
                "\u0395\u03bc\u03c6\u03ac\u03bd\u03b9\u03c3\u03b7",
                "\u0391\u03c0\u03cc\u03ba\u03c1\u03c5\u03c8\u03b7",
                "\u0394\u03b9\u03b1\u03c4\u03ae\u03c1\u03b7\u03c3\u03b7 \u03c3\u03cd\u03bd\u03b4\u03b5\u03c3\u03b7\u03c2 \u03ad\u03c9\u03c2 \u03c4\u03b7\u03bd \u03b1\u03c0\u03bf\u03c3\u03cd\u03bd\u03b4\u03b5\u03c3\u03b7",
                "\u03a3\u03cd\u03bd\u03b4\u03b5\u03c3\u03b7",
                "\u03ae",
                "\u03a3\u03cd\u03bd\u03b4\u03b5\u03c3\u03b7 \u03bc\u03b5 \u03ba\u03bb\u03b5\u03b9\u03b4\u03af \u03c0\u03c1\u03cc\u03c3\u03b2\u03b1\u03c3\u03b7\u03c2 (Passkey)",
                "\u0391\u03c5\u03c4\u03cc \u03c4\u03bf \u03ba\u03bb\u03b5\u03b9\u03b4\u03af \u03c0\u03c1\u03cc\u03c3\u03b2\u03b1\u03c3\u03b7\u03c2 \u03b4\u03b5\u03bd \u03b5\u03af\u03bd\u03b1\u03b9 \u03c0\u03bb\u03ad\u03bf\u03bd \u03ba\u03b1\u03c4\u03b1\u03c7\u03c9\u03c1\u03b7\u03bc\u03ad\u03bd\u03bf \u03c3\u03b5 \u03b1\u03c5\u03c4\u03cc\u03bd \u03c4\u03bf\u03bd \u03ba\u03cc\u03bc\u03b2\u03bf OSCAR.",
                "\u0397 \u03c3\u03cd\u03bd\u03b4\u03b5\u03c3\u03b7 \u03bc\u03b5 \u03ba\u03bb\u03b5\u03b9\u03b4\u03af \u03c0\u03c1\u03cc\u03c3\u03b2\u03b1\u03c3\u03b7\u03c2 \u03b1\u03c0\u03ad\u03c4\u03c5\u03c7\u03b5. \u03a7\u03c1\u03b7\u03c3\u03b9\u03bc\u03bf\u03c0\u03bf\u03b9\u03ae\u03c3\u03c4\u03b5 \u03c4\u03bf\u03bd \u03ba\u03c9\u03b4\u03b9\u03ba\u03cc \u03c3\u03b1\u03c2.",
                "\u03a3\u03cd\u03bd\u03b4\u03b5\u03c3\u03b7 \u03bc\u03b5",
                "\u0397 \u03c5\u03c0\u03b7\u03c1\u03b5\u03c3\u03af\u03b1 SSO \u03b4\u03b5\u03bd \u03b5\u03af\u03bd\u03b1\u03b9 \u03b4\u03b9\u03b1\u03b8\u03ad\u03c3\u03b9\u03bc\u03b7. \u03a3\u03c5\u03bd\u03b4\u03b5\u03b8\u03b5\u03af\u03c4\u03b5 \u03bc\u03b5 \u03c4\u03bf\u03c0\u03b9\u03ba\u03cc \u03ba\u03c9\u03b4\u03b9\u03ba\u03cc \u03ae Passkey.",
                "\u0395\u03bd\u03b7\u03bc\u03ad\u03c1\u03c9\u03c3\u03b7 \u03ba\u03c9\u03b4\u03b9\u03ba\u03bf\u03cd OSCAR",
                "\u0395\u03c0\u03b9\u03bb\u03ad\u03be\u03c4\u03b5 \u03bd\u03ad\u03bf \u03ba\u03c9\u03b4\u03b9\u03ba\u03cc (\u03c4\u03bf\u03c5\u03bb\u03ac\u03c7\u03b9\u03c3\u03c4\u03bf\u03bd 12 \u03c7\u03b1\u03c1\u03b1\u03ba\u03c4\u03ae\u03c1\u03b5\u03c2) \u03b3\u03b9\u03b1 \u03c3\u03c5\u03bd\u03ad\u03c7\u03b5\u03b9\u03b1.",
                "\u039d\u03ad\u03bf\u03c2 \u03ba\u03c9\u03b4\u03b9\u03ba\u03cc\u03c2",
                "\u0395\u03c0\u03b9\u03b2\u03b5\u03b2\u03b1\u03af\u03c9\u03c3\u03b7 \u03bd\u03ad\u03bf\u03c5 \u03ba\u03c9\u03b4\u03b9\u03ba\u03bf\u03cd",
                "\u0391\u03c0\u03bf\u03b8\u03ae\u03ba\u03b5\u03c5\u03c3\u03b7 \u03ba\u03b1\u03b9 \u03c3\u03c5\u03bd\u03ad\u03c7\u03b5\u03b9\u03b1",
                "\u039f\u03b9 \u03ba\u03c9\u03b4\u03b9\u03ba\u03bf\u03af \u03b4\u03b5\u03bd \u03c4\u03b1\u03b9\u03c1\u03b9\u03ac\u03b6\u03bf\u03c5\u03bd.",
                "\u0393\u03bb\u03ce\u03c3\u03c3\u03b1");

        static LoginCopy forLanguage(String lang) {
            if ("es".equals(lang))
                return ES;
            if ("fr".equals(lang))
                return FR;
            if ("el".equals(lang))
                return EL;
            return EN;
        }
    }

    private static final class NamedPrincipal implements Principal {
        private final String name;

        NamedPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
