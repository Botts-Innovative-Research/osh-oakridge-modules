# OSCAR Node Federation Service

This module implements authenticated, one-hop federation between OSCAR nodes.
It keeps remote Basic Auth credentials on the serving node and exposes existing
remote OSH APIs without changing their paths or schemas.

## Routes

- `GET /federation/nodes` lists the local node and registered targets.
- `POST /federation/nodes` discovers and registers a target.
- `PUT /federation/nodes/{uid}` verifies and replaces a target connection.
- `DELETE /federation/nodes/{uid}` removes a target and its credentials.
- `/federation/{uid}/*` streams HTTP requests to the registered target.
- `/federation/{uid}/mqtt` bridges MQTT over WebSocket using the `mqtt` subprotocol.

The management routes require the configured administrator role. Proxy routes
require an authenticated OSH user. The gateway replaces browser authentication
with the target's server-side Basic Auth credentials.

## Enabling the module

The module is packaged in OSCAR releases but is not enabled in the default
`config.json`. Add it through the OSH administrator UI or module configuration:

```json
{
  "objClass": "com.botts.impl.service.oscar.federation.FederationServiceConfig",
  "endPoint": "/federation",
  "credentialKeyFile": "/run/secrets/oscar_federation_key",
  "administratorRole": "admin",
  "allowPrivateNetworkTargets": true,
  "allowInsecureHttp": false,
  "connectTimeoutSeconds": 10,
  "requestTimeoutSeconds": 120,
  "maxRegistrationBodyBytes": 65536,
  "remoteNodes": [],
  "security": {
    "objClass": "org.sensorhub.api.security.SecurityConfig",
    "enableAccessControl": true,
    "requireAuth": true
  },
  "id": "replace-with-a-stable-uuid",
  "autoStart": true,
  "moduleClass": "com.botts.impl.service.oscar.federation.FederationService",
  "name": "OSCAR Federation"
}
```

Container setup generates and mounts `secrets/oscar-federation-key.txt` as a
Docker secret. For a non-container deployment, generate 32 random bytes, encode
them with base64, restrict the file to the service account, and set
`credentialKeyFile` to that path. Losing or rotating this key without migrating
the encrypted credential state makes registered credentials unreadable.

Private targets require both `allowPrivateNetworkTargets: true` on the service
and `allowPrivateNetwork: true` on registration. Loopback, link-local, wildcard,
and multicast destinations are always rejected. Only origin URLs are accepted,
which prevents intentionally registering another federation route.

## Persistence and transport

- Non-secret UID/endpoint records are stored in the OSH module state directory.
- Credentials are encrypted with AES-256-GCM; the key remains in the separate secret file.
- DNS and address policy are rechecked before upstream HTTP and WebSocket connections.
- Redirect following is disabled. Same-origin upstream `Location` headers are rewritten.
- HTTP bodies are streamed and only an explicit header allowlist is forwarded.
- TLS certificate and hostname verification use the JVM/Jetty trust configuration and are never globally disabled.

The viewer retains direct multi-node support. Selecting secure federation during
node creation registers the target, stores only its UID and gateway route in the
browser, and does not persist the remote password.
