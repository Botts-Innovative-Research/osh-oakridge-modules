# Session & Passkey Authentication

The **Session & Passkey Authentication** module authenticates browser, Passkey/FIDO2, optional OpenID Connect (OIDC) corporate SSO, verified Zero Trust tunnel, and scoped Federation Token requests using disk-backed server-side sessions (`OSCAR_SESSION`).

## Plain-Language Terms

| Term | Meaning |
| --- | --- |
| **Stay Signed In (`0` Timeout)** | Setting **Idle timeout** and **Absolute timeout** to `0` keeps workstations signed in across browser restarts, host reboots, and `oscar upgrade` until the user clicks **Log Out** or an Administrator disables the account. |
| **Passkeys (WebAuthn / FIDO2)** | Phishing-resistant sign-in using Windows Hello, Touch ID, Face ID, or USB/NFC FIDO2 security keys (such as YubiKey) — works 100% offline on air-gapped LANs without internet access. |
| **Bulk QR Invites (`/login/enroll?token=oscar_inv_...`)** | Single-use onboarding links and printable QR codes minted in **Users & Access -> Users & Roles -> Bulk Enroll (CSV / Paste)** so new operators and auditors set up a Passkey or local password in one tap (with readable fallback codes for phone-free facilities). |
| **OSCAR vs. Octelium Tunnel Credential Separation** | OSCAR local passwords/Passkeys and Octelium Zero-Trust tunnel credentials are independent layers (`oscar-tunnel-1` authenticates via its ServiceAccount token). Changing your OSCAR password does **not** require changing your Octelium password; keeping distinct credentials provides two-layer defense-in-depth. |
| **Local Fallback + Organization OIDC SSO** | Organizations can optionally link Okta, Microsoft Entra ID, Keycloak, or a self-hosted [`ORNL/oscar-federation-hub`](https://github.com/ORNL/oscar-federation-hub) (Octelium + Authentik) via OIDC while keeping local PBKDF2 password and passkey accounts active for instant WAN-outage resilience and phone-free facilities. |
| **Federation Service Tokens (`oscar_fed_...`)** | Revocable, least-privilege `Viewer` or `Operator` tokens: **(1)** create on the target site in **Users & Access (`/users`) -> Federation Tokens**, then **(2)** paste on the monitoring site in **Servers (`/servers`) -> Add Server -> Authentication Method: Federation Service Token** to feed **National View** and **Multi-Pass Vehicle Search** without sharing human passwords. |

## Configuration Reference

- **Session cookie name**: Name of the browser cookie holding the opaque session token (`OSCAR_SESSION`).
- **Idle timeout / Absolute timeout (seconds)**: Set to `0` (default) to stay signed in until explicit logout, or configure positive seconds for shared-kiosk auto-expiration.
- **Persist sessions across restarts**: Stores `SHA-256` session digests in `.moduledata/oscar-identity.json` (`0600`) so restarts do not log operators out.
- **WebAuthn Relying Party ID**: Optional explicit domain for Passkeys (blank uses the active HTTPS hostname, e.g. `oscar.local`).
- **Organization OIDC SSO**: Optional HTTPS issuer URL, client ID/secret, scopes, optional custom PEM CA certificate path (`oidcTrustedCaCertificatePath` for private/lab PKI), group-to-role mappings (`oscar-admins`, `oscar-operators`, fallback `viewer`), and optional **External SSO Admin Console URL** (`externalIdentityConsoleUrl`, surfaced as **Open Federated User Console** on `/users`).
- **Trusted Tunnel SSO**: Optional verified tunnel user (`X-Octelium-User`) and groups (`X-Octelium-Groups`) headers gated by a `>= 16`-character `X-Oscar-Tunnel-Secret` shared secret without overwriting `local` accounts.
