# Session & Passkey Authentication

The **Session & Passkey Authentication** module authenticates browser, Passkey/FIDO2, optional OpenID Connect (OIDC) corporate SSO, verified Zero Trust tunnel, and scoped Federation Token requests using disk-backed server-side sessions (`OSCAR_SESSION`).

## Plain-Language Terms

| Term | Meaning |
| --- | --- |
| **Stay Signed In (`0` Timeout)** | Setting **Idle timeout** and **Absolute timeout** to `0` keeps workstations signed in across browser restarts, host reboots, and `oscar upgrade` until the user clicks **Log Out** or an Administrator disables the account. |
| **Passkeys (WebAuthn / FIDO2)** | Phishing-resistant sign-in using Windows Hello, Touch ID, Face ID, or USB/NFC FIDO2 security keys (such as YubiKey) — works 100% offline on air-gapped LANs without internet access. |
| **Local Fallback + Organization OIDC SSO** | Organizations can optionally link Okta, Microsoft Entra ID, Keycloak, or Authentik via OIDC while keeping local PBKDF2 password and passkey accounts active for instant WAN-outage resilience and phone-free facilities. |
| **Federation Service Tokens (`oscar_fed_...`)** | Revocable, least-privilege `Viewer` or `Operator` tokens created in **OSCAR Viewer -> Users & Access** so remote federated nodes never share human administrator passwords. |

## Configuration Reference

- **Session cookie name**: Name of the browser cookie holding the opaque session token (`OSCAR_SESSION`).
- **Idle timeout / Absolute timeout (seconds)**: Set to `0` (default) to stay signed in until explicit logout, or configure positive seconds for shared-kiosk auto-expiration.
- **Persist sessions across restarts**: Stores `SHA-256` session digests in `.moduledata/oscar-identity.json` (`0600`) so restarts do not log operators out.
- **WebAuthn Relying Party ID**: Optional explicit domain for Passkeys (blank uses the active HTTPS hostname, e.g. `oscar.local`).
- **Organization OIDC SSO**: Optional HTTPS issuer URL, client ID/secret, scopes, and group-to-role mappings (`admin`, `operator`, `viewer`).
- **Trusted Tunnel SSO**: Optional verified tunnel header (`X-Octelium-User`) gated by a `>= 16`-character `X-Oscar-Tunnel-Secret` shared secret.
