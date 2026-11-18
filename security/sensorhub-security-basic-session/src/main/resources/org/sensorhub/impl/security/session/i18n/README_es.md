# Autenticación por sesión y llaves de acceso (Passkeys)

El módulo **Autenticación por sesión y llaves de acceso** autentica solicitudes del navegador, llaves Passkey/FIDO2, SSO corporativo OpenID Connect (OIDC) opcional, túneles Zero Trust verificados y tokens de federación mediante sesiones persistentes en el servidor (`OSCAR_SESSION`).

## Términos en lenguaje claro

| Término | Significado |
| --- | --- |
| **Mantener sesión iniciada (Tiempo `0`)** | Configurar **Inactividad máx.** y **Duración máx.** en `0` mantiene la sesión activa tras cerrar el navegador, reiniciar el servidor o ejecutar `oscar upgrade` hasta pulsar **Cerrar sesión** o deshabilitar la cuenta. |
| **Llaves de acceso (Passkeys / FIDO2)** | Inicio de sesión resistente a suplantación mediante Windows Hello, Touch ID, Face ID o llaves físicas USB/NFC (YubiKey) — funciona 100 % sin internet en redes aisladas. |
| **Invitaciones masivas por QR (`/login/enroll?token=oscar_inv_...`)** | Enlaces y códigos QR de un solo uso generados en **Usuarios y acceso -> Usuarios y roles -> Inscripción masiva (CSV / Pegar)** para activar Passkeys o contraseñas en un toque (con códigos alternativos para sitios sin teléfonos). |
| **Independencia entre contraseñas OSCAR y túnel Octelium** | Las contraseñas/Passkeys de OSCAR y las credenciales del túnel Zero-Trust Octelium son capas independientes (`oscar-tunnel-1` usa su propio token de servicio). Cambiar su contraseña de OSCAR **no** requiere cambiar la de Octelium; mantener credenciales distintas aporta defensa en profundidad. |
| **Respaldo local + OIDC SSO corporativo** | Permite integrar Okta, Microsoft Entra ID, Keycloak o el centro autohospedado [`ORNL/oscar-federation-hub`](https://github.com/ORNL/oscar-federation-hub) (Octelium + Authentik) manteniendo activas las contraseñas locales PBKDF2 y llaves Passkey ante cortes de internet o instalaciones sin teléfonos. |
| **Tokens de federación (`oscar_fed_...`)** | Credenciales revocables (`Viewer` u `Operator`): **(1)** créelas en el sitio remoto en **Usuarios y acceso (`/users`) -> Tokens de federación** y **(2)** péguelas en el sitio de monitoreo en **Servidores (`/servers`) -> Añadir servidor -> Token de servicio de federación**. |

## Referencia de configuración

- **Cookie de sesión (`OSCAR_SESSION`)**: Nombre de la cookie opaca en el navegador.
- **Inactividad máx. / Duración máx. (s)**: `0` por defecto para mantener la sesión hasta cerrar sesión manualmente.
- **Persistir sesiones tras reinicios**: Almacena resúmenes `SHA-256` en `.moduledata/oscar-identity.json` (`0600`).
- **Dominio WebAuthn (RP ID)**: Dominio opcional para Passkeys (en blanco usa el host HTTPS activo, ej. `oscar.local`).
- **SSO corporativo OIDC / Túnel confiable**: Opciones para proveedores OpenID Connect (incluida CA PEM privada opcional y acceso directo a la consola externa de usuarios federados en `/users`) y cabeceras verificadas de Octelium (`X-Octelium-User`, `X-Octelium-Groups` y `X-Oscar-Tunnel-Secret`).
