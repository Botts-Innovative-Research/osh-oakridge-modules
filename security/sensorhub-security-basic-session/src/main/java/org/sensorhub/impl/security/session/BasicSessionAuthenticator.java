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
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;
import org.eclipse.jetty.server.UserIdentity;
import org.sensorhub.api.security.ISecurityManager;
import org.slf4j.Logger;

public class BasicSessionAuthenticator extends LoginAuthenticator {
    private static final String AUTH_METHOD = "BASIC+SESSION";
    private static final String LOGOUT_PATH = "/logout";
    private static final String JSESSIONID_COOKIE = "JSESSIONID";

    private final Logger log;
    private final ISecurityManager securityManager;

    private final ConcurrentHashMap<String, Authentication.User> authCache = new ConcurrentHashMap<>();

    public BasicSessionAuthenticator(ISecurityManager securityManager, Logger log) {
        this.securityManager = securityManager;
        this.log = log;
    }

    @Override
    public String getAuthMethod() {
        return AUTH_METHOD;
    }

    @Override
    public void prepareRequest(ServletRequest request) {
        // nothing to prepare
    }

    @Override
    public boolean secureResponse(ServletRequest request, ServletResponse response, boolean mandatory, Authentication.User validatedUser) throws ServerAuthException {
        return false;
    }

    @Override
    public Authentication validateRequest(ServletRequest req, ServletResponse resp, boolean mandatory) throws ServerAuthException {
        try {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) resp;

            // catch logout case
            if (request.getServletPath() != null && LOGOUT_PATH.equals(request.getServletPath())) {
                try
                {
                    request.logout();

                    HttpSession session = getSession(request, false);
                    if (session != null) {
                        var sessionId = session.getId();
                        authCache.remove(sessionId);
                        session.invalidate();
                        log.debug("Log out from session @ {}", sessionId);
                    } else {
                        // no session handler in scope — clear from cache using cookie value
                        String cookieId = getSessionIdFromCookie(request);
                        if (cookieId != null) {
                            authCache.remove(cookieId);
                            log.debug("Log out from auth cache @ {}", cookieId);
                        }
                    }

                    var adminUrl = request.getRequestURL().toString().replace(LOGOUT_PATH, "/admin");
                    response.sendRedirect(adminUrl);
                    return Authentication.SEND_CONTINUE;
                }
                catch (Exception e)
                {
                    log.error("Error while logging out", e);
                    return Authentication.SEND_FAILURE;
                }
            }

            // check for cached session (JSESSIONID cookie)
            HttpSession session = getSession(request, false);
            if (session != null) {
                var cachedSession = session.getAttribute(SessionAuthentication.__J_AUTHENTICATED);
                if (cachedSession != null && cachedSession instanceof Authentication.User) {
                    if (!_loginService.validate(((Authentication.User) cachedSession).getUserIdentity()))
                        session.removeAttribute(SessionAuthentication.__J_AUTHENTICATED);
                    else
                        return (User) cachedSession;
                }
            } else {
                String cookieId = getSessionIdFromCookie(request);
                if (cookieId != null) {
                    Authentication.User cached = authCache.get(cookieId);
                    if (cached != null) {
                        if (!_loginService.validate(cached.getUserIdentity()))
                            authCache.remove(cookieId);
                        else
                            return cached;
                    }
                }
            }

            // check for basic auth credentials
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Basic ")) {
                String credentials = authHeader.substring("Basic ".length());
                credentials = new String(Base64.getDecoder().decode(credentials), StandardCharsets.ISO_8859_1);
                int i = credentials.indexOf(':');
                if (i > 0) {
                    String username = credentials.substring(0, i);
                    String password = credentials.substring(i + 1);

                    UserIdentity user = login(username, password, req);
                    if (user != null) {
                        UserAuthentication userAuth = new UserAuthentication(getAuthMethod(), user);

                        // cache auth in session so subsequent requests use JSESSIONID
                        session = getSession(request, true);
                        if (session != null) {
                            session.setAttribute(SessionAuthentication.__J_AUTHENTICATED, userAuth);
                            authCache.put(session.getId(), userAuth);
                            log.debug("Authenticated user '{}', session cached @ {}", username, session.getId());
                        } else {
                            log.debug("Authenticated user '{}' (no session manager available)", username);
                        }

                        return userAuth;
                    } else {
                        log.warn("Failed Basic auth login for user '{}'", username);
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return Authentication.SEND_FAILURE;
                    }
                }
            }

            // no session and no credentials
            if (!mandatory)
                return Authentication.NOT_CHECKED;

            // send 401 with Basic auth challenge
            response.setHeader("WWW-Authenticate", "Basic realm=\"OpenSensorHub\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return Authentication.SEND_CONTINUE;
        } catch (IOException e) {
            log.error("Cannot send HTTP error", e);
            return Authentication.SEND_FAILURE;
        }
    }


    private HttpSession getSession(HttpServletRequest request, boolean create) {
        try {
            return request.getSession(create);
        } catch (IllegalStateException e) {
            // no SessionHandler in scope (request outside servlet context)
            return null;
        }
    }

    private String getSessionIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (JSESSIONID_COOKIE.equals(cookie.getName())) {
                    // strip the ".nodeX" worker suffix from the extended ID
                    // so it matches session.getId()
                    String value = cookie.getValue();
                    int dot = value.lastIndexOf('.');
                    return (dot > 0) ? value.substring(0, dot) : value;
                }
            }
        }
        return null;
    }

}
