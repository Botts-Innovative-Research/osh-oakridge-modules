# Autenticación por sesión y llaves de acceso (Passkeys)

El módulo **Autenticación por sesión y llaves de acceso** autentica solicitudes del navegador, llaves Passkey/FIDO2, SSO corporativo OpenID Connect (OIDC) opcional, túneles Zero Trust verificados y tokens de federación mediante sesiones persistentes en el servidor (`OSCAR_SESSION`).

## Términos en lenguaje claro

| Término | Significado |
| --- | --- |
| **Mantener sesión iniciada (Tiempo `0`)** | Configurar **Inactividad máx.** y **Duración máx.** en `0` mantiene la sesión activa tras cerrar el navegador, reiniciar el servidor o ejecutar `oscar upgrade` hasta pulsar **Cerrar sesión** o deshabilitar la cuenta. |
| **Llaves de acceso (Passkeys / FIDO2)** | Inicio de sesión resistente a suplantación mediante Windows Hello, Touch ID, Face ID o llaves físicas USB/NFC (YubiKey) — funciona 100 % sin internet en redes aisladas. |
| **Respaldo local + OIDC SSO corporativo** | Permite integrar Okta, Microsoft Entra ID o Keycloak manteniendo activas las contraseñas locales PBKDF2 y llaves Passkey ante cortes de internet o instalaciones sin teléfonos. |
| **Tokens de federación (`oscar_fed_...`)** | Credenciales revocables con rol `Viewer` u `Operator` creadas en **OSCAR Viewer -> Usuarios y acceso** para conectar nodos remotos sin compartir contraseñas de administrador. |

## Referencia de configuración

- **Cookie de sesión (`OSCAR_SESSION`)**: Nombre de la cookie opaca en el navegador.
- **Inactividad máx. / Duración máx. (s)**: `0` por defecto para mantener la sesión hasta cerrar sesión manualmente.
- **Persistir sesiones tras reinicios**: Almacena resúmenes `SHA-256` en `.moduledata/oscar-identity.json` (`0600`).
- **Dominio WebAuthn (RP ID)**: Dominio opcional para Passkeys (en blanco usa el host HTTPS activo, ej. `oscar.local`).
- **SSO corporativo OIDC / Túnel confiable**: Opciones para proveedores OpenID Connect y cabeceras verificadas de Octelium (`X-Oscar-Tunnel-Secret`).
