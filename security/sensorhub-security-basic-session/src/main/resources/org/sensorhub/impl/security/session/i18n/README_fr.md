# Authentification par session et clés d'accès (Passkeys)

Le module **Authentification par session et clés d'accès** authentifie les requêtes du navigateur, les clés Passkey/FIDO2, le SSO d'organisation OpenID Connect (OIDC) facultatif, les tunnels Zero Trust vérifiés et les jetons de fédération à l'aide de sessions persistantes côté serveur (`OSCAR_SESSION`).

## Termes expliqués simplement

| Terme | Signification |
| --- | --- |
| **Rester connecté (Délai `0`)** | Régler **Expiration d'inactivité** et **Durée maximale** sur `0` maintient la session active après fermeture du navigateur, redémarrage serveur ou `oscar upgrade` jusqu'à clic sur **Déconnexion** ou désactivation du compte. |
| **Clés d'accès (Passkeys / FIDO2)** | Connexion résistante à l'hameçonnage via Windows Hello, Touch ID, Face ID ou clés matérielles USB/NFC (YubiKey) — fonctionne 100 % hors ligne sur réseau isolé. |
| **Invitations QR groupées (`/login/enroll?token=oscar_inv_...`)** | Liens et codes QR à usage unique générés dans **Utilisateurs et accès -> Utilisateurs et rôles -> Inscription groupée (CSV / Coller)** pour configurer une clé Passkey ou un mot de passe en un geste. |
| **Séparation entre mots de passe OSCAR et tunnel Octelium** | Les mots de passe/Passkeys OSCAR et les identifiants du tunnel Zero-Trust Octelium sont indépendants (`oscar-tunnel-1` utilise son propre jeton de service). Changer votre mot de passe OSCAR **n'exige pas** de modifier celui d'Octelium. |
| **Secours local + SSO OIDC** | Reliez Okta, Microsoft Entra ID, Keycloak ou le hub auto-hébergé [`ORNL/oscar-federation-hub`](https://github.com/ORNL/oscar-federation-hub) (Octelium + Authentik) tout en conservant les mots de passe PBKDF2 et clés Passkey locaux en cas de coupure WAN ou de site sans téléphone. |
| **Jetons de fédération (`oscar_fed_...`)** | Jetons révocables (`Viewer` ou `Operator`) : **(1)** créez-les sur le site cible dans **Utilisateurs et accès (`/users`) -> Jetons de fédération**, puis **(2)** collez-les sur le site de supervision dans **Serveurs (`/servers`) -> Ajouter un serveur -> Jeton de service de fédération**. |

## Référence de configuration

- **Cookie de session (`OSCAR_SESSION`)** : Nom du témoin opaque stocké dans le navigateur.
- **Expiration d'inactivité / Durée maximale (s)** : `0` par défaut pour rester connecté jusqu'à déconnexion explicite.
- **Persister les sessions aux redémarrages** : Stocke les empreintes `SHA-256` dans `.moduledata/oscar-identity.json` (`0600`).
- **Domaine WebAuthn (RP ID)** : Domaine facultatif pour Passkeys (vide utilise l'hôte HTTPS actif, ex. `oscar.local`).
- **SSO OIDC / Tunnel de confiance** : Paramètres facultatifs pour OpenID Connect (certificat CA PEM privé facultatif et raccourci vers la console d'administration SSO externe sur `/users`) et en-têtes Octelium vérifiés (`X-Octelium-User`, `X-Octelium-Groups` et `X-Oscar-Tunnel-Secret`).
