# Basic Session Authentication

This module accepts an initial username and password, then replaces those credentials with an opaque session cookie. Session records remain in server memory and contain the authenticated identity, not the password. Restarting OSCAR invalidates all sessions.

The module is designed for OSCAR's split Jetty layout: the viewer is served at `/`, while APIs are served under `/sensorhub`. Its cookie has `Path=/`, and the shared authenticator validates it in both handler contexts.

## Defaults

- Cookie: `OSCAR_SESSION`
- Browser-session cookie (no persistent `Max-Age`)
- `HttpOnly`
- `Secure`
- `SameSite=Strict`
- 30-minute idle timeout
- 8-hour absolute timeout

Unauthenticated HTML navigation is redirected to `/login`. The login page is rendered by the authenticator so no unprotected viewer assets are required. API clients may still authenticate with a Basic header; a successful request establishes the same session cookie.

## External nodes

Keep `SameSite=Strict` for the OSCAR node serving the viewer. A different-IP external node is cross-site from the viewer. To attempt direct browser cookie authentication to that external node, its administrator must use HTTPS, configure credentialed CORS for the viewer origin, and set this module's `sameSite` option to `None`.

Some browsers or site policies block third-party cookies even with `SameSite=None`. The viewer therefore also supports a **Basic-only node** mode. In that mode credentials exist only in the current JavaScript runtime and are discarded when the page reloads or closes; they are never written to browser storage.

## Logout

`GET /logout` invalidates the server-side session, expires the cookie, and redirects to `/`. Non-GET logout requests return `204 No Content`.
