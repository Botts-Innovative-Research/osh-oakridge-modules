# Authentification par session et clés d'accès (Passkeys)

Le module **Authentification par session et clés d'accès** authentifie les requêtes du navigateur, les clés Passkey/FIDO2, le SSO d'organisation OpenID Connect (OIDC) facultatif, les tunnels Zero Trust vérifiés et les jetons de fédération à l'aide de sessions persistantes côté serveur (`OSCAR_SESSION`).

## Termes expliqués simplement

| Terme | Signification |
| --- | --- |
| **Rester connecté (Délai `0`)** | Régler **Expiration d'inactivité** et **Durée maximale** sur `0` maintient la session active après fermeture du navigateur, redémarrage serveur ou `oscar upgrade` jusqu'à clic sur **Déconnexion** ou désactivation du compte. |
| **Clés d'accès (Passkeys / FIDO2)** | Connexion résistante à l'hameçonnage via Windows Hello, Touch ID, Face ID ou clés matérielles USB/NFC (YubiKey) — fonctionne 100 % hors ligne sur réseau isolé. |
| **Secours local + SSO OIDC** | Reliez Okta, Microsoft Entra ID ou Keycloak tout en conservant les mots de passe PBKDF2 et clés Passkey locaux en cas de coupure WAN ou de site sans téléphone. |
| **Jetons de fédération (`oscar_fed_...`)** | Jetons révocables à privilège minimal (`Viewer` ou `Operator`) créés dans **OSCAR Viewer -> Utilisateurs et accès** pour la fédération multi-nœuds. |

## Référence de configuration

- **Cookie de session (`OSCAR_SESSION`)** : Nom du témoin opaque stocké dans le navigateur.
- **Expiration d'inactivité / Durée maximale (s)** : `0` par défaut pour rester connecté jusqu'à déconnexion explicite.
- **Persister les sessions aux redémarrages** : Stocke les empreintes `SHA-256` dans `.moduledata/oscar-identity.json` (`0600`).
- **Domaine WebAuthn (RP ID)** : Domaine facultatif pour Passkeys (vide utilise l'hôte HTTPS actif, ex. `oscar.local`).
- **SSO OIDC / Tunnel de confiance** : Paramètres facultatifs pour OpenID Connect et en-têtes Octelium vérifiés (`X-Oscar-Tunnel-Secret`).
