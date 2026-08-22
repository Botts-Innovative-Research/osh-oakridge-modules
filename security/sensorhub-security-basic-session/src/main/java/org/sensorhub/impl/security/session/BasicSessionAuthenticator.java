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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.security.auth.Subject;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.sensorhub.api.security.ISecurityManager;
import org.slf4j.Logger;

/**
 * Authenticates one Basic request and replaces it with an opaque, server-side
 * session. This deliberately does not use HttpSession: OSCAR's static files
 * and SensorHub servlets live in separate Jetty contexts.
 */
public class BasicSessionAuthenticator extends LoginAuthenticator {
    private static final String AUTH_METHOD = "BASIC+SESSION";
    private static final String LOGIN_PATH = "/login";
    private static final String LOGOUT_PATH = "/logout";
    private static final int MAX_AUTH_HEADER_LENGTH = 16 * 1024;
    private static final long PRUNE_INTERVAL_MILLIS = 60_000L;

    private final Logger log;
    private final ISecurityManager securityManager;
    private final String cookieName;
    private final long idleTimeoutMillis;
    private final long absoluteTimeoutMillis;
    private final boolean secureCookie;
    private final String sameSite;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, SessionRecord> sessions = new ConcurrentHashMap<>();
    private volatile long nextPruneAt;

    public BasicSessionAuthenticator(BasicSessionConfig config, ISecurityManager securityManager, Logger log) {
        if (config.cookieName == null || !config.cookieName.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+"))
            throw new IllegalArgumentException("Invalid session cookie name");
        if (config.idleTimeoutSeconds <= 0 || config.absoluteTimeoutSeconds <= 0)
            throw new IllegalArgumentException("Session timeouts must be positive");
        if (config.sameSite == null || !(config.sameSite.equalsIgnoreCase("Strict")
                || config.sameSite.equalsIgnoreCase("Lax") || config.sameSite.equalsIgnoreCase("None")))
            throw new IllegalArgumentException("SameSite must be Strict, Lax, or None");
        if (config.sameSite.equalsIgnoreCase("None") && !config.secureCookie)
            throw new IllegalArgumentException("SameSite=None requires a secure cookie");

        this.cookieName = config.cookieName;
        this.idleTimeoutMillis = Math.multiplyExact((long) config.idleTimeoutSeconds, 1000L);
        this.absoluteTimeoutMillis = Math.multiplyExact((long) config.absoluteTimeoutSeconds, 1000L);
        this.secureCookie = config.secureCookie;
        this.sameSite = Character.toUpperCase(config.sameSite.charAt(0))
                + config.sameSite.substring(1).toLowerCase();
        this.securityManager = securityManager;
        this.log = log;
    }

    @Override
    public String getAuthMethod() {
        return AUTH_METHOD;
    }

    @Override
    public void prepareRequest(ServletRequest request) {
        // No request wrapper is required.
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

            if (isLogoutRequest(request)) {
                logout(request, response);
                return Authentication.SEND_CONTINUE;
            }

            if (isLoginRequest(request)) {
                handleLogin(request, response);
                return Authentication.SEND_CONTINUE;
            }

            Authentication sessionAuthentication = authenticateSession(request, response);
            if (sessionAuthentication != null)
                return sessionAuthentication;

            Authentication basicAuthentication = authenticateBasic(request, response);
            if (basicAuthentication != null)
                return basicAuthentication;

            if (!mandatory)
                return Authentication.NOT_CHECKED;

            if (acceptsHtml(request) && "GET".equalsIgnoreCase(request.getMethod())) {
                response.sendRedirect(LOGIN_PATH);
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

    private Authentication authenticateSession(HttpServletRequest request, HttpServletResponse response) {
        String token = readSessionCookie(request);
        if (token == null)
            return null;

        SessionRecord session = sessions.get(token);
        long now = System.currentTimeMillis();
        if (session == null || session.isExpired(now, idleTimeoutMillis, absoluteTimeoutMillis)
                || securityManager.getUserInfo(session.username) == null) {
            sessions.remove(token);
            expireCookie(response);
            return null;
        }

        session.lastAccessAt = now;
        return new UserAuthentication(AUTH_METHOD, withoutCredentials(session.username));
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

        String credentials = new String(decoded, StandardCharsets.ISO_8859_1);
        int separator = credentials.indexOf(':');
        if (separator <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed Basic authorization header");
            return Authentication.SEND_FAILURE;
        }

        String username = credentials.substring(0, separator);
        String password = credentials.substring(separator + 1);
        UserIdentity identity = login(username, password, request);
        if (identity == null) {
            log.warn("Failed Basic authentication for user '{}'", username);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return Authentication.SEND_FAILURE;
        }

        identity = withoutCredentials(username);
        createSession(request, response, username);
        log.debug("Created authentication session for user '{}'", username);
        return new UserAuthentication(AUTH_METHOD, identity);
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        return LOGOUT_PATH.equals(request.getServletPath()) || LOGOUT_PATH.equals(request.getRequestURI());
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return LOGIN_PATH.equals(request.getServletPath()) || LOGIN_PATH.equals(request.getRequestURI());
    }

    private boolean acceptsHtml(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            renderLogin(response, false);
            return;
        }
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        if (username == null || password == null || username.length() > 256 || password.length() > 4096) {
            renderLogin(response, true);
            return;
        }

        UserIdentity identity = login(username, password, request);
        if (identity == null) {
            log.warn("Failed form authentication for user '{}'", username);
            renderLogin(response, true);
            return;
        }

        identity = withoutCredentials(username);
        createSession(request, response, username);
        log.debug("Created authentication session for user '{}'", username);
        response.sendRedirect("/");
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = readSessionCookie(request);
        if (token != null)
            sessions.remove(token);
        expireCookie(response);

        if ("GET".equalsIgnoreCase(request.getMethod()))
            response.sendRedirect("/");
        else
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private void renderLogin(HttpServletResponse response, boolean failed) throws IOException {
        String error = failed ? "<p class=\"error\">Invalid username or password.</p>" : "";
        String page = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>OSCAR sign in</title><style>body{font:16px system-ui,sans-serif;background:#f4f6f8;"
                + "display:grid;place-items:center;min-height:100vh;margin:0}main{background:white;padding:2rem;"
                + "border-radius:.5rem;box-shadow:0 4px 18px #0002;width:min(22rem,calc(100% - 3rem))}"
                + "label,input,button{display:block;width:100%;box-sizing:border-box}label{margin-top:1rem}"
                + "input,button{font:inherit;padding:.65rem;margin-top:.35rem}button{margin-top:1.5rem;cursor:pointer}"
                + ".error{color:#a40000}</style></head><body><main><h1>OSCAR</h1><p>Sign in to continue.</p>"
                + error + "<form method=\"post\" action=\"/login\" autocomplete=\"off\">"
                + "<label>Username<input name=\"username\" required maxlength=\"256\" autofocus autocomplete=\"off\"></label>"
                + "<label>Password<input name=\"password\" type=\"password\" required maxlength=\"4096\" autocomplete=\"off\"></label>"
                + "<button type=\"submit\">Sign in</button></form></main></body></html>";
        byte[] body = page.getBytes(StandardCharsets.UTF_8);
        response.setStatus(failed ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_OK);
        response.setContentType("text/html; charset=UTF-8");
        response.setContentLength(body.length);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.getOutputStream().write(body);
    }

    private String readSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return null;
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()))
                return cookie.getValue();
        }
        return null;
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void createSession(HttpServletRequest request, HttpServletResponse response, String username) {
        String previousToken = readSessionCookie(request);
        if (previousToken != null)
            sessions.remove(previousToken);
        String token = newToken();
        long now = System.currentTimeMillis();
        sessions.put(token, new SessionRecord(username, now));
        setSessionCookie(response, token);
    }

    private UserIdentity withoutCredentials(String username) {
        return new CredentialFreeUserIdentity(username,
                new HashSet<>(securityManager.getUserInfo(username).getRoles()));
    }

    private void setSessionCookie(HttpServletResponse response, String value) {
        response.addHeader("Set-Cookie", cookieName + "=" + value + cookieAttributes());
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
        sessions.entrySet().removeIf(entry ->
                entry.getValue().isExpired(now, idleTimeoutMillis, absoluteTimeoutMillis));
    }

    void close() {
        sessions.clear();
    }

    private static final class SessionRecord {
        final String username;
        final long createdAt;
        volatile long lastAccessAt;

        SessionRecord(String username, long now) {
            this.username = username;
            this.createdAt = now;
            this.lastAccessAt = now;
        }

        boolean isExpired(long now, long idleTimeout, long absoluteTimeout) {
            return now - lastAccessAt >= idleTimeout || now - createdAt >= absoluteTimeout;
        }
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
