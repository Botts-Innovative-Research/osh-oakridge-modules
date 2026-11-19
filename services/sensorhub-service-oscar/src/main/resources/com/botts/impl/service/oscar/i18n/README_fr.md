# Manuel d'administration et d'exploitation d'OSCAR 3.11.0

Ce manuel couvre la première utilisation et l'exploitation complète : certificat, connexion, plan géoréférencé, import/création des voies, adjudication et preuves, événements, statistiques nationales, rapports et fédération de nœuds.

L'interface d'administration sélectionne automatiquement le manuel correspondant à la langue active :

- [English](README.md)
- [Español](README_es.md)
- [Ελληνικά](README_el.md)

> **Portée et sécurité.** Les valeurs visibles dans les captures ne sont que des exemples. Utilisez le nom d'hôte, les coordonnées, adresses, ports, identifiants, mots de passe, règles de conservation et base de données du site réel. Les changements nécessitent un compte administrateur OSCAR. Vérifiez l'empreinte du certificat auprès de l'administrateur du déploiement avant de lui faire confiance. N'importez jamais un certificat non vérifié et ne réutilisez pas les identifiants d'exemple.

## Sommaire

1. [Avant de commencer](#1-avant-de-commencer)
2. [Approuver le certificat OSCAR](#2-approuver-le-certificat-oscar)
3. [Se connecter et comprendre l'enregistrement](#3-se-connecter-et-comprendre-lenregistrement)
4. [Configurer le module de service OSCAR](#4-configurer-le-module-de-service-oscar)
5. [Créer et téléverser un plan géoréférencé](#5-créer-et-téléverser-un-plan-géoréférencé)
6. [Importer ou exporter les voies avec `config.csv`](#6-importer-ou-exporter-les-voies-avec-configcsv)
7. [Créer manuellement un Système de voie](#7-créer-manuellement-un-système-de-voie)
8. [Utiliser OSCAR Viewer](#8-utiliser-oscar-viewer)
9. [Statistiques nationales](#9-statistiques-nationales)
10. [Génération de rapports](#10-génération-de-rapports)
11. [Fédération de nœuds](#11-fédération-de-nœuds)
12. [Conservation des données et stockage](#12-conservation-des-données-et-stockage)
13. [Liste de validation](#13-liste-de-validation)
14. [Dépannage](#14-dépannage)
15. [Documentation associée](#15-documentation-associée)

## 1. Avant de commencer

Vous avez besoin :

- d'un déploiement OSCAR 3.11.0 installé et démarré ;
- de l'URL OSCAR, généralement `https://oscar.local/` sauf si un autre hôte a été choisi ;
- d'un compte administrateur pour `/sensorhub/admin` ;
- de l'image de site approuvée et des coordonnées des coins inférieur gauche et supérieur droit ;
- de l'adresse IP ou DNS et du port de chaque RPM ;
- de l'adresse IP/DNS, des identifiants, du fabricant et des informations de flux de chaque caméra ; et
- d'un navigateur pris en charge sur un poste capable de résoudre et de joindre l'hôte OSCAR.

Consultez le [Guide de démarrage rapide](https://github.com/ORNL/oscar/blob/main/dist/release/QUICKSTART.md) pour l'installation. Le [Guide de déploiement](https://github.com/ORNL/oscar/blob/main/dist/release/DEPLOYMENT.md) couvre l'initialisation, le DNS, les certificats, les commandes de cycle de vie, les contrôles de sécurité et les mises à niveau. OSCAR ne fournit aucun mot de passe administrateur par défaut ; il est défini pendant `oscar init`.

### Conventions de coordonnées et de réseau

- Latitude et longitude sont exprimées en degrés décimaux WGS 84. La latitude est positive au nord et négative au sud ; la longitude est positive à l'est et négative à l'ouest.
- Un rectangle valide exige `latitude inférieur gauche < latitude supérieur droit` et `longitude inférieur gauche < longitude supérieur droit`. La latitude doit être comprise entre -90 et 90, la longitude entre -180 et 180.
- Pour un RPM, hôte et port ont des champs séparés. La caméra n'a pas de champ de port séparé ; ajoutez un port non standard à l'hôte, par exemple `192.0.2.25:8554`.
- N'ajoutez pas `rtsp://` à l'hôte d'une caméra de voie. Le Système de voie génère l'URL RTSP.

### Termes clés expliqués

| Terme | Signification en langage clair |
| --- | --- |
| **Occupation (`Occupancy`)** | Un passage complet d'un véhicule, d'un conteneur ou d'un piéton dans une voie de détection entre le déclencheur d'entrée (`GS`) et celui de sortie (`GX`). |
| **CPS (Coups par seconde)** | Taux de comptage brut du détecteur de rayonnement mesuré à chaque intervalle d'échantillonnage pendant le bruit de fond ou l'occupation. |
| **NSIGMA (`N-Sigma`)** | Nombre d'écarts-types (`σ`) dont le taux de comptage gamma actuel dépasse la moyenne récente du bruit de fond ; permet un déclenchement cohérent des alarmes lorsque le fond varie. |
| **NORM (Matière radioactive d'origine naturelle)** | Cargaison bénigne contenant des radio-isotopes naturels (tels que le potassium-40 dans les engrais, la litière, la céramique ou les bananes, ou les séries thorium/uranium dans le granit et le carrelage). |
| **EML (Apprentissage automatique amélioré / Enhanced Machine Learning) et Code `9999`** | Algorithme pour portiques Rapiscan comparant les fenêtres gamma de basse, moyenne et haute énergie pour distinguer le fret **NORM** bénin des sources menaçantes. Lorsqu'une alarme est classée non menaçante, OSCAR enregistre une décision automatique (`Code 9999: EML Non-Threat`). |
| **Occupation prolongée (`Fault - Extended Occupancy`) et Code `9998`** | Lorsqu'un véhicule occupe une voie pendant au moins son **Seuil d'occupation prolongée (min)** (`extendedOccupancyThresholdMinutes`, `1`–`1440` min, défaut `1` min), OSCAR publie `Fault - Extended Occupancy` et comptabilise le défaut dans les rapports et statistiques jusqu'à la libération de la voie. Si cette occupation prolongée se termine par une **alarme gamma seule** (`gammaAlarm = true`, `neutronAlarm = false`), OSCAR la masque de la file active du tableau de bord et enregistre une décision automatique (`Code 9998: Extended Occupancy Gamma Alarm`). Toute occupation prolongée comportant une alarme neutron n'est jamais adjugée automatiquement. |
| **WebID et DRF (Fonction de réponse du détecteur)** | **WebID** est le service Sandia Full Spectrum d'identification isotopique des spectres téléversés (`.n42`, `.spe` ASCII Ortec, `.spc` binaire Ortec/Canberra et `.xml`), avec repli local automatique par photopics hors ligne ; les spectrogrammes gamma utilisent par défaut l'échelle logarithmique `Coups (Log)` avec bascule en un clic vers `Linéaire`. Une **DRF** indique à WebID le modèle physique et la géométrie du détecteur utilisé. |
| **Fichier quotidien et codes (`GB`, `GS`, `GA`, `NS`, `GX`, `SG1`–`SG3`, `SN1`–`SN2`)** | Journal CSV quotidien par voie contenant les messages RPM bruts : `GB`/`NB` = bruit de fond gamma/neutrons, `GS` = début d'occupation, `GC`/`NC` = profil de comptage, `GA`/`NA`/`NS` = alarme gamma/neutrons, `GX` = fin d'occupation, `TT`/`TC` = défaut/rétablissement de sabotage, et `SG1`–`SG3`/`SN1`–`SN2` = paramètres quotidiens de configuration gamma/neutrons (qui mettent automatiquement à jour les paramètres du pilote Rapiscan en mémoire et regroupent les modifications dans `config.json`). |
| **UUID d'événement (`Event UUID`)** | Identifiant unique global attribué à chaque événement d'occupation pour la recherche, l'exportation QR et le suivi dans les rapports et nœuds fédérés. |
| **Vue opérationnelle (`Operational View`)** | Filtre nommé sélectionnable depuis le menu déroulant **Vue opérationnelle** de la barre supérieure (ou via `?view=<clé>` / `/view/<clé>`) limitant le Tableau de bord, les Événements, l'État de santé, la Carte et le Générateur de rapports à un groupe de voies précis dans le respect des permissions `allowedViews`. |
| **Niveau froid Parquet (`PARQUET_PRIMARY`)** | Mode de stockage par défaut d'OSCAR 3.11.0 dans lequel PostgreSQL conserve une fenêtre de transit de 30 minutes et les partitions terminées sont exportées en fichiers Apache Parquet compressés ZSTD (`archive/parquet/`). |
| **Médias conservés à 5 images** | Pour chaque occupation avec alarme, OSCAR extrait 5 instantanés JPEG (`frame-01.jpg` à `frame-05.jpg`). Après 30 jours, les images secondaires (`01`, `02`, `04`, `05`) sont purgées et l'image centrale (`frame-03.jpg`) est conservée. |
| **OCR des véhicules (`ocrConfig`)** | Lecture optique facultative par voie (`Activer l'OCR des véhicules = false` par défaut) analysant de façon asynchrone les codes conteneur ISO 6346 (avec chiffre de contrôle) et les plaques d'immatriculation selon les ressources CPU et mémoire disponibles (`OcrResourceGovernor`), publiant des candidats `vehicleOcr` (`PARQUET_PRIMARY`), des vignettes JPEG partitionnées par date (`/sensorhub/buckets/ocr/crops/<YYYY-MM-DD>/`, alignées sur `frame-03.jpg` et `ocrCropRetentionDays = 365`) et préremplissant l'**ID véhicule** pour les conteneurs ISO valides ou les plaques `>= 85%`. Lorsque `Alarmes uniquement` est décoché, les séquences hors alarme sont analysées de manière transitoire puis supprimées immédiatement tout en gardant `occupancy.videoPaths` vide (`[]`). |
| **SAN (Nom alternatif du sujet)** | Liste des noms d'hôtes et adresses IP (tels que `oscar.local` et `192.0.2.10`) inscrits dans un certificat HTTPS que le navigateur vérifie avant d'établir la connexion. |
| **Clés d'accès (WebAuthn / FIDO2) et sessions persistantes** | Connexion par clé publique (`ES256` / `RS256`) résistante à l'hameçonnage via Windows Hello, Touch ID, Face ID ou clés matérielles USB/NFC (comme YubiKey), vérifiée localement par OSCAR sans internet. Les postes dédiés restent connectés après redémarrage du navigateur, du serveur et `oscar upgrade` jusqu'au clic sur **Déconnexion** ; les sites sans téléphones utilisent des mots de passe locaux PBKDF2 individuels. |
| **Jetons de service de fédération (`oscar_fed_...`)** | Identifiants révocables à privilège minimal (`Lecteur` ou `Opérateur`) créés dans **Utilisateurs et accès (`/users`)** pour fédérer les flux HTTP et MQTT sans partager de mots de passe administrateur. |

## 2. Approuver le certificat OSCAR

OSCAR utilise HTTPS. Un déploiement muni de son certificat autosigné affiche un avertissement de confidentialité jusqu'à ce que le poste lui fasse confiance. Un certificat émis par une autorité publique ou d'entreprise déjà approuvée ne nécessite pas cette procédure.

![Avertissement de certificat dans Chrome](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/01-certificate-warning.png)

### 2.1 Vérifier avant d'approuver

1. Vérifiez que la barre d'adresse contient exactement le nom d'hôte OSCAR communiqué par l'administrateur.
2. Demandez l'empreinte SHA-256 du certificat OSCAR ou de son autorité émettrice.
3. Ouvrez les détails du certificat dans le navigateur et comparez l'empreinte. Arrêtez si elle ne correspond pas.

Les captures utilisent Chrome sous Windows ; le libellé peut varier selon la version.

### 2.2 Exporter depuis Chrome sous Windows

1. Ouvrez `https://<hote-oscar>/sensorhub/admin`.
2. À l'avertissement, utilisez **Paramètres avancés** uniquement après avoir confirmé le serveur attendu.
3. Ouvrez les informations du site à côté de l'adresse et sélectionnez **Détails du certificat**.

![Ouvrir les détails du certificat](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/02-certificate-details.png)

4. Dans la visionneuse, sélectionnez le certificat, ouvrez **Détails**, puis **Exporter**.

![Exporter le certificat](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/03-certificate-export.png)

5. Enregistrez-le au format X.509 codé Base-64, de préférence avec l'extension `.cer` ou `.crt`. L'extension `.download` de l'exemple peut être importée si Windows la reconnaît, mais `.cer` est plus explicite.

![Enregistrer le certificat](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/04-certificate-save.png)

Si une autorité privée est utilisée, importez le **certificat d'AC** fourni par l'administrateur, et non un certificat de serveur obtenu sur une page non vérifiée.

### 2.3 Importer dans le magasin de confiance Windows

1. Ouvrez le certificat et sélectionnez **Installer le certificat**, ou exécutez `certmgr.msc`, ouvrez **Autorités de certification racines de confiance > Certificats**, puis **Importer**.
2. Choisissez **Utilisateur actuel** pour ce seul utilisateur. Choisissez **Ordinateur local** uniquement si la politique exige une confiance globale et que vous êtes autorisé à l'administrer.
3. Sélectionnez **Placer tous les certificats dans le magasin suivant**, puis **Autorités de certification racines de confiance**.
4. Sélectionnez le fichier exporté et terminez l'assistant.

![Choisir le certificat à importer](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/05-certificate-import.png)

5. Fermez toutes les fenêtres du navigateur, relancez-le et rechargez OSCAR. L'avertissement doit avoir disparu et le certificat doit être valide pour l'hôte.

Sur un poste administré, distribuez la confiance par le mécanisme de l'organisation. Sous macOS, importez le certificat ou l'AC vérifiée dans Trousseaux d'accès. Sous Linux, utilisez le magasin du navigateur ou du système prévu par la distribution. Le Guide de déploiement reste la référence pour créer ou remplacer le certificat côté serveur.

## 3. Se connecter et comprendre l'enregistrement

Ouvrez `https://<hote-oscar>/sensorhub/admin` et saisissez les identifiants créés lors du déploiement.

![Connexion à l'administration OSCAR](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/06-login.png)

### 3.1 Langue

Utilisez le sélecteur de langue en haut de l'administration ou d'OSCAR Viewer. Anglais, espagnol, français et grec sont pris en charge. Le choix est partagé entre les deux interfaces et s'applique au chargement ou rechargement. Cet onglet README ouvre automatiquement le manuel localisé.

### 3.2 Trois actions différentes

- **Téléverser** envoie immédiatement le fichier ; pour le CSV, cela crée les voies dans le registre actif des modules en mémoire. Cette action n'écrit pas la configuration persistante des modules.
- **Appliquer les modifications** valide le formulaire du module affiché et met à jour sa configuration en cours d'exécution.
- **Enregistrer** dans l'en-tête ouvre une boîte de confirmation. Sélectionnez **Oui** et attendez **Configuration de SensorHub enregistrée** ; alors seulement la configuration complète est écrite afin de survivre à un redémarrage.

Après toute configuration, utilisez **Appliquer les modifications** lorsqu'il est disponible, puis **Enregistrer**. Un message de téléversement réussi prouve que le fichier a été accepté, pas que tous les appareils se connectent ni que tout est persistant.

### 3.3 Utilisateurs et accès (`/users`), clés Passkey hors ligne, sessions persistantes et inscription groupée par QR

Ouvrez **Utilisateurs et accès** (`/users`) depuis le menu latéral d'OSCAR Viewer (ou cliquez sur votre nom d'utilisateur en haut) pour gérer les comptes sans ligne de commande :

![Utilisateurs, clés Passkey et contrôle d'accès OSCAR](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/36-users-and-passkeys.png)

- **Rester connecté jusqu'à Déconnexion :** Par défaut (`idleTimeoutSeconds = 0`, `absoluteTimeoutSeconds = 0`), l'option **Rester connecté jusqu'à la déconnexion** sur `/login` enregistre une empreinte SHA-256 dans `/var/lib/oscar/.moduledata/oscar-identity.json` afin que les postes dédiés restent connectés après redémarrage du navigateur, de l'hôte et `oscar upgrade`.
- **Clés Passkey hors ligne, noms d'hôte LAN, mots de passe sans téléphone et indépendance vis-à-vis d'Octelium (`Mon compte et Passkeys`) :** Chaque utilisateur peut enregistrer Windows Hello, Touch ID, Face ID ou une clé YubiKey USB/NFC, ou modifier son mot de passe local PBKDF2 (`>= 12` caractères). **Changer votre mot de passe OSCAR n'oblige pas à changer le mot de passe du tunnel Zero-Trust Octelium** (`oscar-tunnel` s'authentifie avec son propre jeton de compte de service et les deux couches restent indépendantes). Alors que le mot de passe fonctionne par domaine ou par adresse IP brute, les navigateurs exigent deux étapes uniques pour utiliser les Passkeys sur le réseau local : **(1)** ouvrir OSCAR via un nom de domaine (`https://oscar.local`, ou un nom distinct par nœud tel que `https://oscar.ubuntu` et `https://oscar.win`, résolu par le DNS local du routeur ou le fichier `hosts` du poste) et **(2)** approuver `tls/server.crt` (ou une autorité locale avec certificats `<= 825` jours pour macOS/iOS ; voir `DEPLOYMENT.md`). Les Passkeys étant liées au domaine (`rpId`), si vous accédez aussi via un tunnel public Zero-Trust, ajoutez une Passkey supplémentaire sur cette URL sous le même compte.
- **Inscription individuelle ou groupée (`Inscription groupée CSV`) avec message de démarrage et QR :** Les administrateurs attribuent les rôles (`Administrateur`, `Opérateur` ou `Lecteur`), limitent éventuellement l'accès à des **Vues opérationnelles** précises et peuvent importer une liste CSV/Excel (`username, role, display_name, views`). OSCAR crée les comptes et produit immédiatement un message texte prêt à envoyer, des fiches imprimables, un code QR à usage unique (`/login/enroll?token=oscar_inv_...`) permettant de configurer une Passkey ou un mot de passe en un geste, ainsi qu'un code temporaire lisible pour les postes sans téléphone.
- **Jetons de service de fédération et où les utiliser (`Jetons de fédération` -> `Serveurs`) :** Dans **Utilisateurs et accès -> Jetons de fédération**, générez un jeton révocable `oscar_fed_...` sur le site supervisé, puis cliquez sur **Ouvrir la page Serveurs (Mode jeton de fédération)** (`/servers?auth=bearer`) sur le poste de supervision/QG afin de coller le jeton dans **Méthode d'authentification : Jeton de service de fédération** (`HTTPS`, port `443`).
- **Hub auto-hébergé de fédération et SSO centralisé (`Politique de connexion et SSO` + `ORNL/oscar-federation-hub`) :** Pour relier plusieurs sites derrière un pare-feu et gérer les accès depuis une console unique sans IdP infonuagique complexe, déployez [`ORNL/oscar-federation-hub`](https://github.com/ORNL/oscar-federation-hub) (`./oscar-hub.sh init --domain hub.example.org --with-authentik` et `./oscar-hub.sh add-site <slug> --with-trusted-headers`), raccordez chaque nœud OSCAR avec `oscar-remote-access.sh` / `oscar-remote-access.ps1 configure` et reportez les paramètres OIDC / tunnel de confiance (`X-Octelium-User` + `X-Octelium-Groups`, certificat CA PEM privé facultatif et **URL de la console SSO externe** affichant un bouton d'accès direct sur `/users`), tout en conservant les Passkeys et mots de passe locaux hors ligne.

![Inscription groupée et invitations QR](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/37-bulk-enroll-qr-invites.png)

![Activation du compte en un geste par Passkey ou mot de passe](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/38-login-enroll-passkey.png)

![Jetons de service de fédération et accès direct à la page Serveurs](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/39-federation-tokens-and-servers.png)

<details class="oscar-video-guide">
  <summary>Guide vidéo (#19, 24.4s) : Gérer les utilisateurs, les invitations QR groupées, les Passkeys hors ligne et les jetons de fédération</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/19-viewer-users-passkeys-and-access.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/19-viewer-users-passkeys-and-access.vtt" srclang="en" label="English" default>
  </video>
</details>

## 4. Configurer le module de service OSCAR

Dans la navigation de gauche, ouvrez **Services > Module de service OSCAR**.

![Configuration générale du service OSCAR](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/14-service-general.png)

### 4.1 Onglet Général

| Champ | Signification et recommandations |
| --- | --- |
| Classe du module | Implémentation Java choisie ; laissez-la gérée par le système. |
| Nom du module | Nom lisible affiché dans l'arborescence. |
| ID du module | Identifiant local unique. Conservez celui généré sauf migration contrôlée. |
| Description | Description administrative facultative. |
| Chemin de configuration de feuille de calcul | Téléverse immédiatement un CSV de voies. **Télécharger** exporte les voies chargées dans `config.csv` ; indisponible s'il n'y a aucune voie. |
| Chemin du module EML | Téléverse une archive externe du module LLNL EML ou un module d'algorithme d'évaluation d'alarme personnalisé (`.jar`, `.tar.gz`, `.tgz` ou `.zip`) dans l'état privé d'OSCAR (`/var/lib/oscar/eml-plugin/`) et l'active à chaud sur les voies Rapiscan avec **Analyse EML** (`EMLEnabled=true`) activée. OSCAR ne distribue pas EML ; empaquetez l'archive autorisée `EML-VM250-*.tar.gz` avec `tools/package-eml-jar.sh` (ou `tools/package-eml-jar.ps1`), téléversez-la directement ou téléversez un JAR d'algorithme personnalisé (voir `docs/EML_INTEGRATION.md`). |
| ID du nœud | Identifiant obligatoire et unique de ce nœud OSCAR. Ne le réutilisez pas sur un autre nœud. |
| ID de base de données | Base d'observations utilisée. Vide, les statistiques utilisent la base fédérée ; les tâches de purge/export propres à une base ne démarrent qu'avec une sélection explicite. |
| Racine de l'API WebID | URL de Sandia Full Spectrum Web ID. Valeur par défaut : `https://full-spectrum.sandia.gov/api/v1`. Utilisez un service local approuvé en environnement isolé ou laissez vide pour ne pas créer le client Web ID. |
| Fréquence des statistiques (min) | Intervalle de publication, 60 minutes par défaut. |
| Déléguer le cycle de vie des observations à la base de données | Activez lorsque la base à niveaux gère la conservation active/archivée. OSCAR maintient encore l'état de connexion et le nettoyage des fichiers quotidiens validé par manifeste. |
| Limite de travail de purge des fichiers quotidiens | Nombre maximal de fenêtres voie/jour par invocation planifiée de rattrapage, y compris la relance horaire. Valeur par défaut : 50. |
| Démarrage automatique | Démarre le service au chargement du nœud. À activer normalement en exploitation. |

Le service requiert un Service de stockage par compartiments démarré. S'il est introuvable, démarrez et vérifiez **Services > Bucket Storage Service**, puis réinitialisez ou redémarrez le service OSCAR.

### 4.2 Onglet Configuration du plan de site

Il associe une image PNG/JPG/JPEG à ses limites inférieur gauche et supérieur droit. La section 5 détaille la procédure.

### 4.3 Onglet Conservation vidéo

| Champ | Défaut | Comportement |
| --- | ---: | --- |
| Âge avant conservation de cinq images (jours) | 0 | Âge minimal à partir duquel la vidéo source d'une occupation peut être convertie en exactement cinq JPEG conservés. Zéro rend les clips terminés immédiatement éligibles à la conversion asynchrone. |
| Période de requête vidéo (minutes) | 5 | Intervalle de recherche des vidéos nouvellement éligibles ; c'est aussi la largeur maximale en temps d'événement de chaque requête à la base. |
| Rattrapage maximal de conservation (heures) | 6 | Plage maximale de temps d'événement historique traitée par cycle. Le reliquat reprend depuis un curseur durable. |
| Tolérance des arrivées tardives (heures) | 24 | Délai avant que le curseur contigu des arrivées tardives ne finalise un instant d'événement. Les validations dans cette tolérance ne peuvent pas être omises. |
| Période de balayage des arrivées tardives (minutes) | 360 | Délai minimal entre les recherches d'une nouvelle limite sûre retardée. Un balayage figé interrompu reprend au cycle de cinq minutes suivant. |
| Rattrapage des arrivées tardives par cycle (heures) | 2 | Portion maximale d'un balayage retardé actif traitée en un cycle ; le travail est divisé en fenêtres de base de données de cinq minutes. |
| Rapprochement historique par cycle (heures) | 24 | Portion maximale d'un balayage complet et figé de l'historique traitée en un cycle. La progression active et la fin figée survivent au redémarrage. |
| Période de rapprochement historique (heures) | 24 | Délai après un balayage historique complet avant de recommencer à l'occupation éligible la plus ancienne. Ceci récupère les validations postérieures à la tolérance. |
| Période de rafraîchissement des flux d'occupation (minutes) | 15 | Fréquence de résolution des noms de sortie d'occupation en identifiants de flux mis en cache et utilisés directement par les requêtes. |
| Limite de travail de maintenance des médias | 200 | Nombre maximal d'entrées de rapprochement du catalogue, de nouvelle tentative et de cycle de vie source examinées par cycle. |
| Limite de récupération des métadonnées multimédias | 200 | Nombre maximal d'heures UTC d'observations traitées récupérées par cycle, y compris les heures absentes. La récupération ne dépasse jamais le curseur retardé durable ni la limite courante imposée par l'horloge. Chaque heure utilise douze accès fixes de cinq minutes ; ce n'est pas un nombre d'occupations. |
| Activer la conservation d'images | activé | Activé : convertit les anciens clips en un ensemble exact de cinq JPEG. Désactivé : utilise la suppression historique. |
| Nombre d'images conservées | 5 | Le schéma v1 exige exactement cinq images lorsque la conservation est activée. |
| Qualité JPEG conservée | 85 | Qualité d'encodage JPEG sRGB de base de 1 à 100. |
| Délai de retour arrière de la source (heures) | 24 | Durée minimale de conservation du clip original après validation des cinq JPEG et de leur manifeste. |

Ces paramètres concernent la vidéo historique d'occupation, pas le direct. L'analyseur emploie un
curseur avant durable et ne reparcourt pas tout l'historique à chaque intervalle. Les travaux en
échec et le nettoyage de la source après le délai utilisent des index durables et bornés distincts.
Le curseur retardé et le rapprochement répété, complet et figé de l'historique rendent finalement
visibles les occupations tardives ou précédemment non cataloguées, sans balayage illimité au
redémarrage ni limite mobile pouvant distancer la réparation. Après une correction arrière de
l'horloge, les balayages figés actifs s'arrêtent à la limite d'éligibilité courante sans perdre leur
fin durable. Un flux sans plage temporelle de résultats exploitable est sondé directement ; la
découverte initiale revient prudemment au début de l'époque uniquement si ce flux contient une
occupation éligible, au lieu de se fier à la plage plus récente d'un autre flux. Un flux vide sans
plage ne force pas ce retour. Avec les valeurs par défaut, un clip terminé est immédiatement éligible
et le traitement planifié de conservation le convertit de manière asynchrone. Le MP4 source est
conservé pendant au moins 24 heures après une publication réussie ; la suppression normale de la
source ne peut donc pas survenir avant cette échéance relative à la publication. Les cinq JPEG
restent la représentation probatoire conservée. Choisissez des valeurs conformes à la politique de
conservation des preuves du site avant d'activer le nettoyage.

Les environnements de régression jetables peuvent volontairement réduire la période de requête à 1
minute afin d'accélérer les tests déterministes. Cette valeur est réservée aux tests ; la période de
requête de production reste de 5 minutes par défaut.

### 4.4 Onglet Conservation sous pression de stockage

| Champ | Défaut | Comportement |
| --- | ---: | --- |
| Seuil de déclenchement (%) | 85 | Déclenche la purge à ce taux. Doit être supérieur à la cible et au plus égal à 100. |
| Utilisation cible (%) | 80 | La purge continue jusqu'à cette cible ou jusqu'à épuisement des fichiers éligibles. |
| Période de contrôle (minutes) | 1 | Fréquence des contrôles ; doit être positive. |
| Âge minimal de l'objet (minutes) | 10 | Protège les fichiers plus récents ; ne peut pas être négatif. |
| Nombre maximal d'ensembles multimédias par cycle | 200 | Limite les entrées de l'index direct inspectées pendant un cycle. La file durable reprend le travail restant après redémarrage ou au cycle suivant. |
| Chemin de stockage | `files` | Système de fichiers dont l'usage commande la purge. Doit viser le volume de fichiers OSCAR réel. |

La purge sous pression ne supprime que les objets éligibles du compartiment `videos` dans l'ordre durable de publication ; elle n'énumère ni ne trie tout l'arbre des médias conservés pendant un cycle. Les CSV quotidiens restent protégés. Surveillez aussi le stockage au niveau du système d'exploitation : ce mécanisme d'urgence ne remplace pas les sauvegardes.

Les identifiants d'observations traitées partagent au plus douze fichiers exacts de cinq minutes
avec somme de contrôle par heure UTC d'événement. Une heure complète n'est récupérée que si sa fin
précède strictement à la fois le curseur retardé durable et la limite courante imposée par l'horloge,
par douze accès directs bornés et sans parcours d'arbre. La mémoire reste ainsi bornée et le seuil par fichier dépasse 1,2 million
d'occupations réparties uniformément par heure. Un index compact exact, verrouillé par fichier,
mémorise les suppressions sous pression sans un
tombstone JSON permanent par caméra. Sauvegardez et restaurez comme une unité tout
`videos/retained-media/v1`, notamment index, points de contrôle, files, manifestes et journaux.

Ce format compact apparaît pour la première fois dans 3.11.0 et ne migre pas les prototypes
`.deleted.json` ni les arbres de marqueurs par observation. Les systèmes de test doivent démarrer
avec une base et un préfixe de médias 3.11.0 neufs. Pour revenir en arrière, restaurez les deux depuis
la même sauvegarde coordonnée antérieure à 3.11.0 ; n'exécutez jamais un ancien binaire sur ce stockage.

### Codes de disposition personnalisés

Dans la configuration du module de service OSCAR, ajoutez des entrées à **Custom Disposition Codes** (`customAdjudicationCodes`). Chaque entrée nécessite un entier `code` unique supérieur à 11 (à l'exception de `9998`, réservé aux adjudications automatiques **Alarme gamma d'occupation prolongée**, et de `9999`, réservé aux adjudications automatiques **Sans menace EML**) et un libellé `label` non vide. Les codes 0–11 sont intégrés et ne peuvent pas être remplacés. Exemple :

```json
"customAdjudicationCodes": [{"code": 12, "label": "Examen local terminé"}]
```

Appliquez la configuration du service avant d'envoyer `adjudicationCode: 12`. Les voies consultent la configuration actuelle à chaque soumission et rejettent les codes non configurés. Supprimer un code empêche les nouvelles soumissions avec ce code et conserve les adjudications existantes. Le service publie ces choix après l'enregistrement du système et de la base de données, y compris après modification de la configuration. Le Viewer lit le flux `adjudicationCodes` et propose les choix avec les codes intégrés dans les détails, l'aperçu du tableau de bord et l'adjudication en masse. Le Viewer s'abonne à la sortie de disposition via le client MQTT partagé et lit une seule fois la dernière observation pour l'état initial. Les ajouts, modifications et suppressions publiés actualisent immédiatement les listes ouvertes, sans interrogation périodique ni rechargement. Les abonnements sont déconnectés au retrait du composant ou au changement des nœuds cibles. La réouverture de la liste retente les abonnements échoués ou les flux absents. Les sélections sur plusieurs nœuds proposent uniquement les codes personnalisés dont le numéro et le libellé correspondent sur tous les nœuds cibles.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#04, 12.3s) : Configurer la rétention du Service OSCAR et les codes personnalisés</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/04-admin-oscar-service-and-custom-codes.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/04-admin-oscar-service-and-custom-codes.vtt" srclang="en" label="English" default>
  </video>
</details>

<details class="oscar-video-guide">
  <summary>Guide vidéo (#07, 11.8s) : Charger le module externe EML VM-250 et activer l’EML par voie</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/07-admin-eml-plugin-setup.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/07-admin-eml-plugin-setup.vtt" srclang="en" label="English" default>
  </video>
</details>

## 5. Créer et téléverser un plan géoréférencé

Le plan est une image rectangulaire orientée au nord, interpolée linéairement entre deux coins géographiques. OSCAR affiche OSM par défaut lorsqu'il est joignable et qu'aucun plan n'est configuré. Avec un plan valide, Viewer ajuste l'étendue à ses limites, place le plan comme couche raster supérieure et conserve les marqueurs de voie au-dessus.

### 5.1 Ouvrir le formulaire

1. Ouvrez **Services > Module de service OSCAR > Configuration du plan de site**.
2. Si la configuration facultative n'existe pas, sélectionnez **Ajouter**.

![Configuration vide du plan](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/07-site-diagram-empty.png)

Le formulaire contient le téléversement et deux paires de coordonnées.

![Champs du plan](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/08-site-diagram-fields.png)

### 5.2 Préparer l'image

1. Ouvrez une source cartographique ou un plan approuvé. Si vous utilisez Google Maps, respectez ses conditions et la politique de l'organisation.
2. Utilisez une vue nord en haut, sans rotation ni inclinaison ; le modèle OSCAR est un rectangle aligné sur les axes.
3. Cadrez le plus petit rectangle contenant la zone d'exploitation et les voies. Un cadrage serré améliore le zoom initial.
4. Capturez uniquement ce rectangle et enregistrez-le en `.png`, `.jpg` ou `.jpeg`.

![Carte source cadrée](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/09-source-map.png)

![Plan recadré](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/10-cropped-site-diagram.png)

Évitez les menus, le curseur, les contrôles du navigateur et les grandes marges. Les étiquettes ou épingles déjà présentes sont des pixels de l'image, pas des marqueurs OSCAR.

### 5.3 Relever les limites

Notez les coordonnées exactes des coins de l'image :

- **Limite inférieur gauche** : coin sud-ouest (`latitude`, `longitude`).
- **Limite supérieur droit** : coin nord-est (`latitude`, `longitude`).

Dans Google Maps, cliquez avec le bouton droit sur un point pour afficher et copier ses coordonnées décimales. Répétez aux deux coins opposés du recadrage.

![Copier les coordonnées d'un coin](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/11-corner-coordinate.png)

Vérifiez que :

- la latitude supérieure droite est supérieure à la latitude inférieure gauche ;
- la longitude supérieure droite est supérieure (plus à l'est) ; sur le site américain illustré, elle est moins négative ;
- aucune paire n'est `0, 0` ; et
- le rectangle n'est pas si large que le site n'en occupe qu'une petite partie.

### 5.4 Téléverser et rendre persistant

1. Choisissez l'image avec **Choisir un fichier**.
2. Saisissez les quatre limites avant **Téléverser**.

![Limites saisies](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/12-site-bounds-entered.png)

3. Sélectionnez **Téléverser** et attendez le message vert.

![Téléversement réussi](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/13-site-upload-success.png)

4. Sélectionnez **Appliquer les modifications**, puis **Enregistrer** globalement.
5. Ouvrez ou rechargez Viewer et vérifiez l'alignement ainsi que l'étendue initiale.

Un nouveau téléversement valide remplace la référence active sans modifier les coordonnées des voies. Si les limites sont invalides, corrigez leur ordre et leur plage.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#06, 14.9s) : Configurer un système de voie et un plan du site géoréférencé</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/06-admin-manual-lane-and-site-diagram.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/06-admin-manual-lane-and-site-diagram.vtt" srclang="en" label="English" default>
  </video>
</details>

## 6. Importer ou exporter les voies avec `config.csv`

L'importation en masse est recommandée pour de nombreuses voies. Sur un site existant, utilisez d'abord **Télécharger** afin d'obtenir un modèle exact. Le fichier est sensible : les noms d'utilisateur et mots de passe des caméras sont exportés en clair.

### 6.1 Téléverser le CSV

1. Ouvrez **Services > Module de service OSCAR > Général**.
2. Sous **Chemin de configuration de feuille de calcul**, choisissez un `.csv`.

![Choisir un CSV de voies](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/15-csv-selected.png)

3. Sélectionnez **Téléverser** et attendez la confirmation.

![CSV téléversé](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/16-csv-upload-success.png)

4. Ouvrez **Capteurs** et inspectez chaque voie et ses sous-modules RPM/caméra. Le chargement est asynchrone ; attendez l'apparition de toutes les lignes.
5. Corrigez les valeurs par voie, appliquez les changements, utilisez l'action globale **Enregistrer**, confirmez **Oui**, puis attendez **Configuration de SensorHub enregistrée**. Ouvrir la boîte sans sélectionner **Oui** n'enregistre rien. Tant que cet enregistrement global n'a pas réussi, les voies importées n'existent qu'en mémoire et peuvent disparaître au redémarrage.
6. Pendant la fenêtre de vérification planifiée, redémarrez OSCAR avec la commande de déploiement prise en charge, reconnectez-vous et vérifiez que toutes les voies attendues et leurs enfants RPM/caméra réapparaissent sous **Capteurs**. Ne considérez pas l'importation comme terminée avant la réussite de ce contrôle après redémarrage.

![Voies importées](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/17-sensors-overview.png)

Une ligne est ignorée si son `UniqueID` appartient déjà à un Système de voie chargé ; les voies existantes ne sont pas écrasées. Assurez aussi l'unicité dans le fichier lui-même. L'analyse précède le chargement, mais l'initialisation des enfants est asynchrone : la panne d'un appareil peut toucher une voie sans annuler les autres déjà acceptées.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#05, 10.6s) : Synchroniser la feuille de calcul multivoie (config.csv)</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/05-admin-lane-spreadsheet-sync.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/05-admin-lane-spreadsheet-sync.vtt" srclang="en" label="English" default>
  </video>
</details>

### 6.2 Schéma exact

Le schéma actuel comporte 14 en-têtes principaux obligatoires, sensibles à la casse et dans cet ordre exact. `OperationalViews` est la sixième colonne :

```csv
Name,UniqueID,AutoStart,Latitude,Longitude,OperationalViews,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth
```

L'import reste compatible avec l'ancien schéma à 13 en-têtes sans `OperationalViews` ; ces voies ne sont affectées à aucune vue opérationnelle. Les nouveaux exports utilisent toujours les 14 en-têtes. Avec l'en-tête actuel, chaque ligne doit contenir la cellule `OperationalViews`, même si sa valeur est vide.

Incluez ensuite au moins un groupe de six colonnes de caméra. Utilisez un groupe `CameraType0` vide si la voie n'a aucune caméra ; les index supplémentaires doivent rester continus :

```csv
CameraType0,CameraHost0,CameraPath0,Codec0,Username0,Password0
CameraType1,CameraHost1,CameraPath1,Codec1,Username1,Password1
```

L'analyseur CSV n'impose pas de nombre maximal de groupes séquentiels, mais la galerie de médias conservés à cinq images plafonne chaque événement à `8` caméras, `120` mégapixels et `64 MiB` sur l'ensemble des cinq images par caméra (jusqu'à `8` caméras par voie en `1080p`, `4` en `5 MP` `3072×1728` ou `2` en `4K` `3840×2160`), et l'hôte doit être dimensionné pour le total des flux du site. Chaque ligne doit comporter exactement autant de cellules que l'en-tête. L'analyseur sépare simplement sur les virgules : n'insérez ni virgule ni saut de ligne dans une valeur, même entre guillemets. Utilisez des cellules vides non citées.

### 6.3 Colonnes de voie et RPM

| Colonne | Obligatoire | Valeur et effet |
| --- | --- | --- |
| `Name` | oui | Nom affiché, limité à 12 caractères. |
| `UniqueID` | oui | Identifiant stable non réutilisable. Un suffixe simple devient une URN de voie. |
| `AutoStart` | oui | `true` ou `false` ; seul `true`, sans distinction de casse, vaut vrai. |
| `Latitude` / `Longitude` | ensemble | Degrés WGS 84. Laissez les deux vides pour omettre l'emplacement fixe. |
| `OperationalViews` | non | Clés de vue de poste séparées par des points-virgules, par exemple `north-gate;secondary`. Chaque clé doit compter 1 à 63 lettres ASCII minuscules (`a`–`z`), chiffres ou tirets, sans tiret initial ni final. Laissez vide pour ne pas affecter la voie à une vue limitée. |
| `RPMConfigType` | non | Vide, `Aspect`, `Rapiscan` ou `RS350`, sans distinction de casse. |
| `RPMHost` | avec RPM | Adresse IP ou DNS du RPM. |
| `RPMPort` | avec RPM | Port TCP entier. |
| `AspectAddressStart` / `AspectAddressEnd` | Aspect | Plage Modbus inclusive, deux entiers. Valeurs du formulaire : 1 à 32. |
| `EMLEnabled` | Rapiscan | `true` uniquement pour une voie VM250/EML, sinon `false`. |
| `EMLCollimated` | Rapiscan | État de collimation `true` ou `false`. |
| `LaneWidth` | Rapiscan | Largeur en mètres. Fournissez un nombre même si EML est désactivé ; défaut 4.82. |

### 6.4 Colonnes de caméra

| Colonne | Valeur et effet |
| --- | --- |
| `CameraTypeN` | Vide, `Sony`, `Axis` ou `Custom`, sans distinction de casse. |
| `CameraHostN` | IP/DNS non vide ; ajoutez `:port` si nécessaire. N'incluez pas `rtsp://`. |
| `CameraPathN` | Custom uniquement. Commence par `/`, par exemple `/stream1`. Sony et Axis génèrent leur chemin. |
| `CodecN` | Axis uniquement. `H264`/`H.264` choisit H.264 ; `MJPEG`/`JPEG` choisit Motion JPEG. |
| `UsernameN` / `PasswordN` | Identifiants de caméra. Protégez le CSV et supprimez les copies non sécurisées. |

Le CSV n'expose pas la **Longueur du tampon vidéo** ; les caméras importées utilisent `0`. Modifiez ensuite le module enfant si nécessaire.

### 6.5 Exemples minimaux

Une voie Rapiscan avec une caméra Axis :

```csv
Name,UniqueID,AutoStart,Latitude,Longitude,OperationalViews,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth,CameraType0,CameraHost0,CameraPath0,Codec0,Username0,Password0
Lane01,lane01,true,35.8858,-84.2121,north-gate,Rapiscan,192.0.2.10,1601,,,false,false,4.82,Axis,192.0.2.20,,H264,operator,replace-me
```

Une voie Aspect avec deux caméras :

```csv
Name,UniqueID,AutoStart,Latitude,Longitude,OperationalViews,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth,CameraType0,CameraHost0,CameraPath0,Codec0,Username0,Password0,CameraType1,CameraHost1,CameraPath1,Codec1,Username1,Password1
Lane02,lane02,true,35.8859,-84.2119,north-gate;secondary,Aspect,192.0.2.11,502,1,32,,,,Sony,192.0.2.21,,,operator,replace-me,Custom,192.0.2.22:8554,/stream1,,operator,replace-me
```

Remplacez toutes les adresses et tous les identifiants. Empêchez le tableur de reformater identifiants, booléens ou coordonnées.

## 7. Créer manuellement un Système de voie

La création manuelle convient à une voie, à un matériel inhabituel ou au dépannage. Pour un grand site, importez le CSV puis contrôlez chaque voie.

### 7.1 Choisir la bonne action

Cliquez avec le bouton droit sur **Capteurs** ou utilisez son action d'ajout :

- **Ajouter un nouveau module** crée un module de premier niveau. Utilisez-le pour un nouveau **Système de voie**.
- **Ajouter un sous-module** crée un enfant du système sélectionné. Utilisez-le pour rattacher manuellement un pilote à une voie existante. N'imbriquez pas un Système de voie dans un autre sans intention précise.
- **Redémarrer**, **Arrêter** et **Forcer l'initialisation** contrôlent le module sélectionné.
- **Supprimer le module** retire sa configuration. Si **Supprimer les données avec la voie** est activé, les observations, flux et enregistrements de cette voie sont aussi supprimés.
- **Tout sélectionner/désélectionner** ne change que la sélection de l'arborescence.

![Menu contextuel de capteur](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/20-lane-context-menu.png)

Sélectionnez **Système de voie**.

![Sélecteur de type de module](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/21-module-picker.png)

Le sélecteur montre tous les types compatibles installés. La version illustrée comprend :

| Type | Utilisation |
| --- | --- |
| Système de voie | Parent d'un RPM et d'une ou plusieurs caméras ; choix normal pour OSCAR. |
| Pilote Aspect | Pilote RPM direct, normalement généré dans une voie. |
| Pilote Rapiscan | Pilote RPM direct, normalement généré dans une voie. |
| Pilote RS-350 | Pilote direct ; la voie crée aussi le processus d'occupation. |
| Pilote vidéo FFmpeg | Caméra FFmpeg, normalement générée dans une voie. |
| Caméra RTSP/RTP | Intégration générique, distincte du modèle constructeur de la voie. |
| Kromek D3S / D5 | Intégrations directes de détecteurs Kromek. |
| Capteur virtuel SWE | Capteur générique à partir de données SWE/OSH existantes. |
| Système de capteurs | Parent générique sans le comportement OSCAR d'une voie. |

La liste varie selon les paquets installés. Pour une voie ordinaire, choisissez **Système de voie** puis sa configuration initiale.

### 7.2 Onglet Général

![Champs généraux d'une voie](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/22-lane-general.png)

| Champ | Recommandation |
| --- | --- |
| Classe du module | Implémentation gérée par le système. |
| Nom du module | Obligatoire, 12 caractères maximum ; utilisez une convention stable comme `Lane01`. |
| ID du module | ID local généré ; ne copiez pas celui d'un autre module. |
| Description | Facultative. |
| URL SensorML | URL facultative d'une description SensorML de base. Laissez vide si le site n'en maintient pas. |
| ID unique | Identifiant stable obligatoire. `lane01` devient `urn:osh:system:lane:lane01` ; une URN complète est acceptée. Évitez espaces et réutilisation. |
| Dernière mise à jour | Horodatage SensorML, normalement vide/géré par le système. |
| Démarrage automatique | Démarre la voie au chargement. Activez après vérification. |
| Clés de vue opérationnelle | Liste facultative des clés de poste autorisées à afficher cette voie. Ajoutez chaque clé séparément ; utilisez 1 à 63 lettres ASCII minuscules (`a`–`z`), chiffres ou tirets, sans tiret initial ni final. Laissez vide si la voie ne doit appartenir à aucune vue limitée. |
| Seuil d'occupation prolongée (min) | Durée en minutes (`1`–`1440`, défaut `1`) avant qu'une occupation active ne publie `Fault - Extended Occupancy` et n'incrémente le compteur de défauts `Ext-Occ`. Lorsqu'une occupation prolongée se termine par une alarme gamma seule (sans alarme neutron), OSCAR la masque du tableau de bord actif et l'adjuge automatiquement comme **Code 9998 : Alarme gamma d'occupation prolongée**. |
| OCR des véhicules | Réglages facultatifs par voie (`Activer l'OCR des véhicules`, `Alarmes uniquement`, `Répertoire des modèles`, `Lire les numéros de conteneur`, `Lire les plaques d'immatriculation`, `UID des caméras`, `Images par séquence`, `Confiance minimale conteneur`, `Confiance minimale plaque`, `Fraction supérieure ignorée`, `Fraction inférieure ignorée`, `enabled = false` par défaut). Lorsqu'il est activé sur une voie, OSCAR exécute la reconnaissance PP-OCRv3 ONNX en arrière-plan selon les ressources disponibles, publie les candidats `vehicleOcr` (`PARQUET_PRIMARY`) et des vignettes partitionnées par date (`ocrCropRetentionDays = 365`, conservées avec `frame-03.jpg`, les rétentions et les adjudications opérateur), supprime immédiatement les séquences transitoires hors alarme lorsque `Alarmes uniquement = false` et préremplit l'**ID véhicule** lors de l'adjudication ; les voies désactivées n'ajoutent aucune surcharge. |
| Supprimer les données avec la voie | Activé par défaut. La suppression de la voie efface ses données. Désactivez-le si l'historique doit être conservé. |
| Informations de source de données | Métadonnées héritées facultatives, seulement si le modèle SensorML du site les exige. |

### 7.3 Onglet Emplacement fixe

Saisissez latitude, longitude et altitude facultative (hauteur au-dessus de l'ellipsoïde WGS 84, en mètres).

Si un plan valide existe, il apparaît dans le formulaire. Cliquez à l'emplacement précis de la voie : OSCAR calcule latitude et longitude depuis les limites du plan. Vérifiez les valeurs avant d'enregistrer. Vous pouvez aussi les saisir directement.

![Choisir l'emplacement sur le plan](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/23-lane-location.png)

### 7.4 Onglet Orientation fixe

L'orientation utilise le repère local Nord-Est-Bas :

- **Cap** (lacet) : rotation autour de Z, en degrés ;
- **Tangage** : rotation autour de Y ;
- **Roulis** : rotation autour de X.

Laissez-la non définie si le site ne l'utilise pas. Elle ne remplace pas l'emplacement et ne géoréférence pas le plan.

### 7.5 Onglet Options de la voie

Sélectionnez **Ajouter** sous **Configuration RPM initiale** et le bouton plus/**Ajouter** sous **Configuration initiale de caméra**.

![Options initiales vides](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/24-lane-options-empty.png)

Ces options génèrent les modules enfants à l'initialisation. Elles ne constituent pas une seconde copie dynamique de tous leurs réglages. Ensuite, inspectez et gérez les enfants RPM/caméra dans l'arborescence Capteurs.

#### Types de RPM

Tous exigent **Hôte distant** et **Port distant**. Options supplémentaires :

| Type | Champs supplémentaires |
| --- | --- |
| Aspect | **Rechercher l'appareil dans la plage — De/À**. Plage Modbus inclusive obligatoire, 1 à 32 par défaut. |
| Rapiscan | **Activer l'analyse EML** uniquement pour VM250/EML ; **Est collimaté** ; **Largeur de voie (m)**, 4.82 par défaut. Lorsque le RPM publie ses messages quotidiens de configuration (`SG1`–`SG3` et `SN1`–`SN2`) dans le flux de fichier quotidien, le pilote met automatiquement à jour ses paramètres gamma et neutron en mémoire et regroupe les modifications dans une mise à jour asynchrone de `config.json` sans affecter la latence ni les performances. |
| RS350 | Aucun champ initial supplémentaire. La voie crée le pilote et le processus d'occupation au démarrage. |

Ne devinez pas le port ni la plage Aspect. La panne d'un RPM ne doit pas arrêter les voies indépendantes, mais cette voie n'aura pas d'occupations normales avant correction.

#### Types de caméra

Tous exposent **Hôte distant**, **Nom d'utilisateur**, **Mot de passe** et **Longueur du tampon vidéo** (défaut `0`). Conservez zéro sauf tests concluants : augmenter le tampon peut consommer de la mémoire et ajouter de la latence.

| Type | Comportement du flux |
| --- | --- |
| Sony | Génère `rtsp://[identifiants@]<hote>:554/media/video1`. Saisissez l'hôte sans schéma ni port en double. |
| Axis | **H264** (défaut, 640×480, intervalle d'image clé 15) ou **MJPEG** (JPEG 640×480). |
| Personnalisée | Saisissez un **Chemin de flux** commençant par `/`. Génère `rtsp://[identifiants@]<hote><chemin>` ; incluez le port personnalisé dans l'hôte. Un chemin vide retombe sur le chemin Axis H.264 et ne doit pas être utilisé volontairement. |

Les enfants FFmpeg générés utilisent TCP, demandent 24 i/s, activent HLS, désactivent les images individuelles, utilisent un délai de connexion de cinq secondes et des reconnexions. Le résultat réel dépend de la caméra et du réseau.

![RPM et caméras configurés](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/25-lane-options-filled.png)

**OCR véhicule optionnel par voie :** Ouvrez l'onglet **OCR véhicule** (**Vehicle OCR**) uniquement sur les voies nécessitant une reconnaissance asynchrone des conteneurs ou des plaques (désactivé par défaut, avec zéro mémoire ONNX sur les voies inactives). Vérifiez **Alarmes uniquement** (`Alarms Only`, recommandé `true` ; s'il est décoché, les clips sans alarme sont supprimés immédiatement après l'échantillonnage OCR), **Images par clip** (`6`), **Confiance minimale conteneur** (`0.5`) et **Confiance minimale plaque** (`0.6`). Les sorties `vehicleOcr` sont archivées dans `PARQUET_PRIMARY` et `/sensorhub/buckets/ocr/crops/<YYYY-MM-DD>/` conserve les vignettes JPEG partitionnées par date avec `frame-03.jpg` (`ocrCropRetentionDays = 365`, en préservant les passages sous rétention ou adjugés par un opérateur et en libérant les plus anciennes vignettes éligibles sous pression de stockage).

![Configuration optionnelle de l'OCR véhicule par voie](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/33-lane-vehicle-ocr.png)

### 7.6 Terminer et vérifier

1. Vérifiez chaque onglet.
2. Ajoutez le module ou appliquez les modifications d'une voie existante.
3. Attendez l'initialisation de la voie et des enfants.
4. Développez la voie : confirmez un RPM, toutes les caméras et, pour RS350, le processus d'occupation.
5. Corrigez les enfants si nécessaire et confirmez l'état **Démarré**.
6. Sélectionnez **Enregistrer** globalement.

Consultez la documentation [Rapiscan](https://github.com/ORNL/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-rapiscan), [Aspect](https://github.com/ORNL/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-aspect), [RS-350](https://github.com/ORNL/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-rs350) et [FFmpeg](https://github.com/ORNL/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-ffmpeg).

<details class="oscar-video-guide">
  <summary>Guide vidéo (#17, 32.4s) : Configurer l’OCR véhicules, comparer les passages multiples et adjuger</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/17-vehicle-ocr-and-adjudication.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/17-vehicle-ocr-and-adjudication.vtt" srclang="en" label="English" default>
  </video>
</details>

## 8. Utiliser OSCAR Viewer

Ouvrez `https://<hote-oscar>/`. Le tableau de bord doit afficher l'état des voies, la table des événements et la carte. Avec un plan configuré, il est l'image supérieure et l'étendue initiale correspond à ses limites. Les marqueurs OSCAR restent interactifs au-dessus ; une épingle dessinée dans la capture n'est qu'une partie de l'image.

![Tableau de bord OSCAR Viewer](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/18-viewer-dashboard.png)

### 8.1 Tableau de bord et file d'alarmes

1. Confirmez toutes les voies dans **État des voies**. Utilisez **Personnaliser le tableau de bord** dans l'en-tête lorsqu'un poste ou une Vue opérationnelle doit afficher, masquer ou réorganiser les cartes (**État des voies**, **Carte**, **Table des événements**).
2. Vérifiez la position de chaque marqueur. La carte ajuste automatiquement son zoom aux limites des marqueurs de voie actifs.
3. Utilisez les couches pour comparer le plan à OSM ou Esri.
4. Vérifiez que les nouvelles occupations arrivent et que heures, gamma, neutrons et état sont plausibles.
5. La table d'alarmes contient les occupations en alarme non encore adjugées. Deux catégories d'adjudication automatique sont masquées de la file active du tableau de bord et des notifications navigateur :
   - **Alarme gamma d'occupation prolongée (`Code 9998`)** : Lorsqu'un véhicule occupe une voie pendant au moins son `extendedOccupancyThresholdMinutes` (`1`–`1440` min, défaut `1` min), OSCAR publie `Fault - Extended Occupancy` sur `occupancyStatus`. Si cette occupation prolongée se termine par une **alarme gamma seule** (`gammaAlarm = true`, `neutronAlarm = false`), OSCAR la masque de la file active du tableau de bord et l'adjuge automatiquement comme **Alarme gamma d'occupation prolongée** (code `9998`, `username = "AUTO"`). Toute occupation prolongée comportant une alarme neutron n'est jamais adjugée automatiquement et reste dans la file active.
   - **Sans menace EML (`Code 9999`)** : Sur les voies Rapiscan VM250 avec **Analyse EML** activée et un module EML téléversé, les alarmes classées sans menace (`RELEASE`) par EML sont masquées du tableau de bord et des notifications navigateur et adjugées automatiquement comme **Sans menace EML** (code `9999`) avec métadonnées EML interrogeables ; les alarmes `INVESTIGATE` apparaissent normalement.
   Sélectionnez une ligne pour ouvrir/fermer l'aperçu; l'état colore Gamma, Neutron ou Gamma et Neutron. Les alarmes non lues du tableau de bord s'affichent en gras avec un point indicateur et l'état lu est partagé par voie entre les postes (uniquement sur le Tableau de bord, sans modifier **Événements**). Les opérateurs peuvent activer le filtre temporaire **Non lues uniquement** (depuis le bandeau ou la barre d'outils) ou réduire la fenêtre active via les préréglages rapides (`15m`, `1h`, `24h`, `15j` par défaut). Ouvrir l'aperçu ou les détails, adjuger ou utiliser **Acquitter la sélection** / **Tout acquitter** (limité à la Vue opérationnelle, à la fenêtre temporelle et aux filtres actifs) marque les alarmes comme lues ; dans **Paramètres**, les opérateurs peuvent activer **Bandeau d'alarmes non lues** (affichant le décompte complet au-delà de la première page, plafonné à `100+` au-delà de `150` avec clés individuelles) et/ou **Audio d'alarme continu** (coupé immédiatement dès l'acquittement).
6. Vérifiez les onglets CPS/NSIGMA, la bande interactive de 5 images JPEG retenues (`FIRST`, `QUARTER`, `MIDDLE`, `THREE_QUARTERS`, `LAST`) et chaque vidéo; les flèches changent de média.
7. Agrandissez ou double-cliquez la ligne pour ouvrir **Détails de l'événement**.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#09, 12.9s) : Surveiller le tableau de bord 50 voies en direct et inspecter les alarmes</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/09-dashboard-monitoring-and-quick-adjudication.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/09-dashboard-monitoring-and-quick-adjudication.vtt" srclang="en" label="English" default>
  </video>
</details>

### 8.2 Adjudication rapide depuis le tableau de bord

![Adjudication dans le tableau de bord](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/26-dashboard-adjudication.png)

Utilisez-la lorsque graphiques et vidéo suffisent, sans preuve ni WebID:

1. Saisissez l'**ID véhicule** s'il est connu (sur les voies où l'**OCR conteneur / plaque** est activé, OSCAR préremplit automatiquement les codes ISO 6346 valides ou les plaques de confiance `>= 85%` sauf modification par l'opérateur, et propose des puces de suggestion cliquables).
2. Choisissez un code **Adjuger** dans la liste groupée de 8.5.
3. Réglez **Inspection secondaire** sur **Aucune**, **Demandée** ou **Terminée**.
4. Ajoutez des **Notes** justifiant la décision.
5. Sélectionnez **Envoyer**. Le succès identifie l'occupation et retire l'alarme de la file. **Réinitialiser** efface le formulaire local sans envoi.

L'envoi ajoute un enregistrement sans modifier l'observation d'origine. Pour preuves, isotopes, QR, WebID ou historique, ouvrez les Détails.

### 8.3 Page Événements

![Liste des événements](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/27-event-list.png)

**Événements** est l'historique de tous les nœuds locaux et fédérés configurés, pas seulement des alarmes en attente. Il affiche voie/nœud, ID d'occupation, début/fin, maxima gamma/neutron, état et présence d'une adjudication. Les filtres, décomptes, pages et la sélection groupée s'appliquent à l'ensemble complet des résultats correspondants côté serveur, pas seulement à la page visible.

- Utilisez la barre `<search>` **ID de véhicule** (`Est égal à`, `Contient` ou `Commence par`) pour rechercher des codes de conteneur ISO 6346 et plaques normalisés dans les adjudications et lectures `vehicleOcr` sur les nœuds locaux et fédérés (insensible à la casse, aux espaces et aux tirets), puis sélectionnez **Comparer les passages (`N`)** pour ouvrir la **Comparaison des passages du véhicule** avec adjudication liée et export `VEHICLE_INCIDENT`.
- Dépliez **Co-occurrence des catégories d'alarme (UpSet)** afin d'afficher d'abord les catégories principales ayant des occupations (`Unknown`, `SNM`, `NORM`, `Medical`, `Industrial`, `Gamma`, `Neutron`), de filtrer en cliquant sur une catégorie peuplée ou une combinaison, et de regrouper les intersections `SNM` rares dans une colonne cramoisie **`Autre SNM`** sélectionnable afin que les barres verticales `SNM` totalisent toujours exactement le total `SNM` de gauche.
- Utilisez **Rechercher un événement par UUID** pour coller l'identifiant durable affiché dans les Détails de l'événement, un PDF ou un transfert d'alarme. OSCAR recherche exactement cette observation et ouvre l'événement sans reconstruire une suite de filtres. Il s'agit de l'ID public codé de l'observation OSCAR ; il est présenté comme UUID de l'événement aux opérateurs mais n'est pas tenu d'utiliser la ponctuation RFC-4122.
- **Colonnes** masque/affiche les champs, **Filtres** ouvre les filtres serveur, **Requêtes enregistrées** met en favori les recherches réutilisables, **Réinitialiser la requête** efface la requête active et **Densité** règle l'espacement.
- Créez des groupes avec **Toutes les conditions (ET)** ou **Une condition au choix (OU)** et imbriquez-les si les deux logiques sont nécessaires. Jusqu'à 20 règles et trois niveaux de groupes sont admis. Les conditions de nœud et de voie choisissent les flux ; celles d'ID d'occupation, d'heure, de maxima gamma/neutron, d'état, d'adjudication et de code d'adjudication sont envoyées au serveur pour chaque voie concernée. L'application revient à la première page.
- Utilisez les opérateurs proposés pour chaque champ. Les heures acceptent avant, après et entre ; les nombres, des limites inclusives, entre et l'égalité ; l'état, Aucun, Gamma, Neutron ou Gamma et Neutron ; l'adjudication, Oui, Non, **Sans menace EML** (pour interroger les alarmes adjugées automatiquement par EML) et vide/non vide ; et **Code d'adjudication** prend en charge est égal à, est différent de, fait partie de, est vide et n'est pas vide pour les codes intégrés `1–11`, **Code 9998 : Alarme gamma d'occupation prolongée**, **Code 9999 : EML sans menace** et les **Codes de disposition personnalisés** configurés.
- Cochez séparément des alarmes non adjugées ou choisissez **Sélectionner toutes les alarmes filtrées** pour figer l'ensemble admissible au moment du chargement, y compris les autres pages. Vous pouvez ensuite décocher des lignes. L'adjudication groupée applique à la sélection un code, un état d'inspection secondaire, un ID véhicule facultatif et des notes, avec au plus six événements traités simultanément. Elle ne joint ni preuves ni choix d'isotopes ; utilisez les Détails de l'événement lorsque ceux-ci sont nécessaires.
- Vérifiez le nombre et l'avertissement avant l'envoi. Les succès quittent la file d'alarmes ou apparaissent adjugés. Les échecs restent visibles et sélectionnés ; **Réessayer les échecs** ne relance qu'eux. Si OSCAR ne peut pas énumérer tout le résultat filtré, il ne sélectionne rien plutôt que d'adjuger silencieusement un sous-ensemble.
- Résultats du plus récent, par pages de 15. Sélection = aperçu; double-clic ou **Détails** = page complète.
- Un nœud fédéré indisponible peut rendre lignes/comptes incomplets; vérifiez la connexion avant d'interpréter zéro.

![Graphique UpSet avec filtres par catégorie principale et colonne Autre SNM](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/40-upset-category-filters-and-other-snm.png)

<details class="oscar-video-guide">
  <summary>Guide vidéo (#12, 27.0s) : Filtrer le journal des événements par catégories UpSet, véhicule, code d’adjudication et UUID</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/12-event-log-filtering-and-adjudication-codes.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/12-event-log-filtering-and-adjudication-codes.vtt" srclang="en" label="English" default>
  </video>
</details>

### 8.4 Détails de l'événement

![Résumé, graphiques et vidéo de l'événement](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/28-event-details-summary.png)

La page peut contenir :

- UUID de l'événement, voie, occupation, heures, alarme, vitesse et autres champs ;
- graphiques gamma, seuil, neutrons et RS-350 pris en charge, y compris **Afficher les détecteurs individuels** (combinant les détecteurs gamma `G1`–`G4+` sur un graphique et les détecteurs neutrons `N1`–`N4+` sur un second, avec un commutateur **Graphiques séparés (Accessibilité)** pour le daltonisme) ;
- la bande de 5 images JPEG retenues (`FIRST`, `QUARTER`, `MIDDLE`, `THREE_QUARTERS`, `LAST`) et la vidéo enregistrée pendant l'événement ;
- les messages de **Sortie de fichier quotidien**, les métadonnées d'**Analyse automatisée EML**, les résultats WebID et l'historique complet des **Adjudications consignées** (en ajout seul) ;
- preuves téléversées ou lues par QR ;
- analyse Web ID et choix d'isotopes facultatifs ;
- ID véhicule, notes, inspection secondaire et code ; et
- **Exporter en PDF**.

Utilisez **Copier l'UUID** pour permettre à un autre opérateur de rechercher le même événement. **Copier le lien direct** crée une URL qualifiée par le nœud qui ouvre l'événement sans répéter les filtres et conserve la vue opérationnelle active. Le lien fonctionne seulement tant que l'observation est conservée, que le nœud reste joignable et que la vue actuelle autorise cette voie. OSCAR rejette les identifiants d'un autre flux ou d'une voie hors périmètre.

**Retour** (ou **Retour aux événements filtrés** / **Tous les événements** depuis une recherche filtrée) revient à la liste sans reconstruire la requête. **CPS/NSIGMA** choisit la présentation gamma prise en charge; les flèches parcourent les vidéos. **Exporter en PDF** ouvre l'impression navigateur de la page rendue et inclut l'UUID visible. Pour un événement en direct qui vient d'arriver, les exports PDF et QR restent indisponibles pendant qu'OSCAR résout l'UUID enregistré ; si la résolution automatique échoue, sélectionnez **Réessayer** ou rouvrez l'événement depuis **Événements**. Cela diffère des rapports serveur de la section 10.

Les données disponibles dépendent du détecteur, de la caméra, de la conservation, de l'ancienneté, des droits et de WebID. Un panneau vide ne prouve pas l'absence de données: vérifiez voie, flux, caméra et conservation.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#13, 19.4s) : Examiner les 5 images conservées, le fichier quotidien et adjuger</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/13-event-details-adjudication-and-daily-file.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/13-event-details-adjudication-and-daily-file.vtt" srclang="en" label="English" default>
  </video>
</details>

#### Codes d'adjudication

| Groupe | Codes |
| --- | --- |
| Alarme réelle | 1 Contrebande trouvée ; 2 Autre |
| Alarme innocente | 3 Isotope médical ; 4 NORM ; 5 Expédition déclarée de matière radioactive |
| Fausse alarme | 6 Inspection physique négative ; 7 RIID/ASP indique uniquement le fond ; 8 Autre |
| Test/Maintenance | 9 Activité autorisée de test, maintenance ou formation |
| Sabotage/Défaut | 10 Activité non autorisée |
| Autre | 11 Autre |
| Automatisé | 9998 Alarme gamma d'occupation prolongée (généré par le système lorsqu'une occupation atteint ou dépasse le seuil d'occupation prolongée de la voie et se termine par une alarme gamma seule sans alarme neutron) ; 9999 Sans menace EML (généré par le système lorsqu'EML classe une alarme sans menace ; inclut probabilités, modèle/collimation et détail des sources dans **Détails de l'événement**) |

Choisissez selon la procédure du site, jamais par commodité.

### 8.5 Adjudication complète et preuves

![Preuves et adjudication complète](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/29-event-details-adjudication.png)

- **Résultats d'analyse WebID** affiche heure, nom/type d'isotope, confiance, taux, texte/nombre d'isotopes, avertissements, khi carré, DRF, erreur et dose estimée; **Lire plus** développe le texte.
- **Adjudications consignées** affiche occupation, heure, utilisateur, code, commentaire, isotopes, chemins, inspection secondaire et ID véhicule. Un nouvel envoi ajoute une ligne sans réécrire les anciennes.
- **Collecte de preuves** et le formulaire créent la nouvelle décision.

#### Preuves

1. **Téléverser des fichiers** ajoute un ou plusieurs fichiers. Aucun filtre de type navigateur: respectez la politique du site.
2. Pour analyser un spectre, activez **WebID**, choisissez une DRF fournie par Sandia Full Spectrum, **Premier plan** ou **Arrière-plan**, puis éventuellement **Synthétiser l'arrière-plan** pour un premier plan. Une paire premier/arrière-plan est envoyée ensemble.
3. **Scanner QR** utilise la caméra pour un texte spectroscopique. Autorisez-la, scannez/révisez/supprimez les codes, réglez WebID/DRF/type, puis **Terminé**. Les captures deviennent des preuves texte.
4. **Téléverser vers WebID** traite les preuves WebID non encore envoyées (`.n42`, `.spe`, `.spc` ou `.xml`) et reste désactivé sans élément éligible. L'accès au bucket est requis ; si Sandia Full Spectrum est hors ligne ou isolé, OSCAR bascule automatiquement sur l'identification locale des nucléides par photopics.
5. Dans **Preuve WebID**, sélectionnez des résultats puis **Utiliser le résultat sélectionné** pour appliquer leurs isotopes. L'opérateur reste responsable.

Supprimer avant envoi retire seulement l'élément en attente. Après téléversement, suivez le chemin consigné et la politique de conservation; le retrait du formulaire ne garantit pas la suppression serveur.

#### Formulaire

1. Saisissez l'ID véhicule si connu et exactement un code.
2. Choisissez zéro ou plusieurs isotopes. **Inconnu** exclut les isotopes nommés: Neptunium, Plutonium, Uranium-233/235/238, Américium, Baryum, Bismuth, Californium, Césium-134/137, Cobalt-57/60, Europium-152, Iridium, Manganèse, Sélénium, Sodium, Strontium, Fluor, Gallium, Iode-123/131, Indium, Palladium, Technétium, Xénon, Potassium, Radium et Thorium.
3. Ajoutez les notes et choisissez inspection **Aucune**, **Demandée** ou **Terminée** (présélectionnée automatiquement sur **Terminée** lorsqu'un fichier spectroscopique `.n42`/`.spe`/`.spc`/`.xml` ou une capture QR est joint, sauf modification manuelle, et comptabilisée comme inspection secondaire spectroscopique).
4. Sélectionnez **Envoyer**, relisez la confirmation complète puis **Confirmer et envoyer**. Vérifiez le succès et la nouvelle ligne. En cas d'échec, conservez le formulaire et corrigez nœud/flux de commande/téléversement avant de réessayer.

**Recherche par ID de véhicule, comparaison multi-passages et adjudication liée :** Lorsqu'un même véhicule repasse dans un portique de comptage global pour confirmer une alarme ou est dirigé d'une voie Rapiscan/Aspect (avec classification Enhanced Machine Learning `EML` facultative) vers une voie spectroscopique secondaire RS350 — sur le même nœud ou entre nœuds fédérés —, utilisez la barre de recherche **ID de véhicule** sur la page **Événements** (insensible à la casse, aux espaces et aux tirets sur les adjudications et les lectures `vehicleOcr`) ou la bannière **Passages associés du véhicule** dans **Détails de l'événement** pour ouvrir la **Comparaison des passages du véhicule**. Juste au-dessus de la liste des passages, un graphique interactif **UpSet de co-occurrence multi-passages** résume les intersections d'alarmes et catégories de sources (filtrant la liste au clic), tandis que le tableau regroupe chronologiquement chaque passage avec écarts temporels, pics gamma/neutrons, résultats EML et RS350/WebID, images clés et vignettes OCR, profils radiologiques superposés à la demande, **Adjudication liée** des alarmes ouvertes et export en un clic du **Rapport d'incident véhicule (`VEHICLE_INCIDENT`)**.

![Comparaison multi-passages du véhicule, profils radiologiques superposés et adjudication liée](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/35-vehicle-pass-comparison.png)

### 8.6 Transférer une alarme par code QR dans un environnement isolé

OSCAR peut placer un résumé compact d'alarme dans un seul QR sans service Internet. L'export contient le nœud et la voie d'origine, l'UUID de l'événement et l'ID d'occupation lisible, les heures, l'état et les maxima, les métadonnées compactes d'adjudication présentes, ainsi que les courbes gamma, neutron et seuil sous-échantillonnées. Il **n'inclut pas** la vidéo, les fichiers de preuve, les spectres ni tous les échantillons d'origine. Les nouveaux exports exigent un UUID résolu ; les anciens paquets version 1 qui n'en contiennent pas restent lisibles et sont signalés comme tels.

Pour exporter :

1. Ouvrez l'aperçu de l'alarme sur le tableau de bord ou **Détails de l'événement**, puis sélectionnez **Exporter l'alarme en QR**.
2. Attendez la lecture des observations de l'intervalle. La boîte indique le nombre de points gamma/neutron exportés et d'origine.
3. Faites scanner le code, utilisez **Télécharger l'image QR** pour un PNG, **Télécharger le fichier d'alarme** pour un `.oscar-alarm.json`, ou **Partager l'alarme**. Si le navigateur ne partage pas les fichiers, OSCAR le télécharge.

L'échantillonnage conserve les deux extrémités, minima/maxima globaux, points gamma de part et d'autre des franchissements de seuil et minima/maxima locaux. Les valeurs sont arrondies à trois décimales. Le paquet conserve l'empreinte SHA-256 des séries complètes pour une comparaison ultérieure avec la source, mais les échantillons omis ne sont pas reconstructibles depuis le QR.

Pour recevoir :

1. Ouvrez **Transfert d'alarme** dans la navigation du Viewer.
2. Utilisez **Démarrer le scan caméra**, **Scanner une image QR**, **Importer le fichier d'alarme**, ou collez le texte `OSCAR-ALARM:1:`. La caméra exige une autorisation et un contexte HTTPS sécurisé; l'image ou le fichier reste utilisable lorsque la caméra est interdite.
3. OSCAR décompresse, applique les limites de taille et de données, vérifie l'empreinte SHA-256 de transfert et redessine les courbes sous-échantillonnées.
4. Confirmez nœud, voie, UUID de l'événement, occupation, heures, état et courbes, puis téléchargez ou partagez le fichier reçu uniquement si vous y êtes autorisé. L'UUID ne retrouve l'original que sur un système OSCAR qui conserve encore cette observation et en autorise l'accès ; l'import du paquet ne crée pas un nouvel événement serveur.

> **Limite de sécurité.** Le paquet est comprimé et son intégrité est vérifiée, mais il n'est ni chiffré ni signé numériquement. L'empreinte détecte une corruption; elle n'identifie pas l'expéditeur, car une personne modifiant le contenu peut la recalculer. Considérez-le comme un aperçu portable, confirmez indépendamment sa source avant tout usage opérationnel et utilisez uniquement les supports et appareils autorisés.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#14, 25.2s) : Exporter un QR d’alarme signé et le vérifier hors ligne</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/14-airgapped-alarm-qr-transfer.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/14-airgapped-alarm-qr-transfer.vtt" srclang="en" label="English" default>
  </video>
</details>

### 8.7 Surveiller l'État de santé

Ouvrez **État de santé** avec l'icône de moniteur cardiaque dans la navigation ou accédez directement à `/health`. La page affiche une ligne pour chaque voie visible dans la portée actuelle ; `/health?view=<clé>` la limite à la vue opérationnelle affectée.

- **Connexions** indique le RPM et chaque caméra configurée comme **En ligne**, **Hors ligne** ou **En attente**. En attente signifie qu'aucune valeur de connexion exploitable n'est encore arrivée et n'est pas considérée comme saine.
- **État des défauts** affiche gamma élevé, gamma faible, neutron élevé, sabotage et occupation prolongée comme **Défaut**, **Normal** ou **Inconnu**. Une télémétrie absente ou en échec reste à l'état **Inconnu** ; elle n'est jamais considérée saine ni affichée comme normale.
- **Occupation actuelle** utilise le flux canonique `occupancyStatus` du Système de voie pour afficher une durée stable avec les RPM Rapiscan, Aspect et RS350.
- **Dernière mise à jour** utilise l'heure locale du navigateur pour la dernière mise à jour de connexion ou de défaut.

Chaque voie configure un **Seuil d'occupation prolongée (min)** côté serveur (`extendedOccupancyThresholdMinutes`, de `1` à `1440` minutes, défaut `1` minute) dans SensorHub Admin (**Lane Sensor System > General > Extended Occupancy Threshold (min)**). Lorsqu'un véhicule occupe une voie pendant au moins ce seuil, la voie publie `Fault - Extended Occupancy` sur `occupancyStatus`, active l'indicateur **Occupation prolongée** et incrémente le compteur de défauts `Ext-Occ` dans les Statistiques nationales et les Rapports de voie jusqu'à la libération de la voie. Si cette occupation prolongée se termine par une **alarme gamma seule** (`gammaAlarm = true`, `neutronAlarm = false`), OSCAR la masque de la file active du tableau de bord et l'adjuge automatiquement comme **Code 9998 : Alarme gamma d'occupation prolongée** (`username = "AUTO"`), tandis que toute occupation prolongée comportant une alarme neutron reste active pour adjudication par l'opérateur. Sur `/health`, vous pouvez également saisir de 1 à 1440 minutes dans **Seuil d'occupation prolongée** pour réévaluer l'affichage dans ce navigateur sans modifier les observations, adjudications ni rapports du serveur.

Les messages de sabotage Rapiscan signalent des transitions, et non un instantané répété périodiquement. Le [guide de maintenance Rapiscan du DOE/PNNL](https://d5.pnnl.gov/sites/default/files/2023-02/RPM_Maint_Guide_Drawings.pdf) précise que le RPM envoie `TT` une fois à l'entrée en sabotage et `TC` une fois au retour à la normale. Selon la politique OSCAR, chaque nouvelle connexion Rapiscan publie une valeur initiale **Normal** avant de traiter les messages de l'appareil. Un `TT` ultérieur la remplace immédiatement par **Défaut** et un `TC` ultérieur rétablit **Normal**. Un flux de sabotage absent ou illisible reste **Inconnu**.

Après la mise à niveau, confirmez que chaque Système de voie publie `occupancyStatus` et que chaque RPM et caméra FFmpeg publie son état de connexion. Arrêtez puis redémarrez une caméra de test autorisée et vérifiez Hors ligne puis En ligne. Confirmez qu'une télémétrie de défaut indisponible reste à l'état Inconnu ; testez gamma, neutron, sabotage et occupation longue uniquement selon les procédures matérielles approuvées du site.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#15, 10.2s) : Surveiller l’État de santé RPM/caméras et la Carte du site</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/15-status-of-health-and-site-map.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/15-status-of-health-and-site-map.vtt" srclang="en" label="English" default>
  </video>
</details>

### 8.8 Utiliser un poste avec vue opérationnelle

Ouvrez `https://<hote-oscar>/?view=<cle>` ou `https://<hote-oscar>/view/<cle>`. OSCAR conserve la clé pendant la navigation et limite les voies, événements, cartes, notifications, rapports, transferts d'alarme et l'État de santé aux voies affectées. L'URL racine non limitée charge toutes les voies. Les clés invalides et les vues sans voie échouent en mode fermé sans charger de données.

Les vues opérationnelles séparent la présentation des postes; elles ne constituent pas une limite d'autorisation. Utilisez l'authentification OSCAR et les contrôles réseau lorsque l'accès doit être restreint.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#10, 11.4s) : Restreindre les voies d’un poste avec les Vues Opérationnelles sécurisées</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/10-operational-views-scoping.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/10-operational-views-scoping.vtt" srclang="en" label="English" default>
  </video>
</details>

### 8.9 Afficher la vidéo en direct d'une voie

Ouvrez les **Détails de la voie** pour une voie dotée d'une caméra configurée. Le panneau affiche d'abord **Préparation de la vidéo en direct… en attente d’une image clé.** pendant qu'OSCAR crée une liste de lecture HLS décodable (amorcée immédiatement depuis un cache GOP aligné sur image clé lorsque le flux amont est déjà actif). Avec une caméra conforme, la lecture commence normalement dans l'intervalle entre images clés de la caméra — une à deux secondes — auquel s'ajoute la latence normale du réseau et du navigateur. À côté de la vidéo en direct, vous pouvez activer **Afficher les détecteurs individuels** pour combiner les détecteurs gamma (`G1`–`G4+`) sur un graphique et les détecteurs neutrons (`N1`–`N4+`) sur un second, ou activer **Graphiques séparés (Accessibilité)** pour séparer chaque détecteur.

La caméra doit fournir une vidéo H.264 avec un GOP fermé et une image IDR/clé toutes les une à deux secondes. Un GOP ouvert ou plus long peut retarder le démarrage ou empêcher le décodage indépendant du premier segment.

L'enregistrement d'une alarme applique la même exigence d'image clé. Une commande `OPEN` reste en attente pendant cinq secondes au maximum, jusqu'à ce que la génération d'enregistrement courante ait correctement multiplexé sa première image clé ; les paquets commençant au milieu d'un GOP sont rejetés. Une commande `CLOSE` pour une occupation très courte attend derrière cette commande `OPEN` et, si nécessaire, la décision limitée d'alarme/enregistrement d'OSCAR. Le délai de surveillance des commandes de chaque caméra ne commence qu'après ces deux attentes et l'envoi effectif de l'opération à la caméra ; aucune de ces périodes d'attente n'est considérée comme une panne de caméra. Si l'image clé n'arrive pas, ou si l'enregistreur s'arrête ou échoue, OSCAR annule cette génération et supprime son fichier partiel caché au lieu d'associer à l'événement un MP4 vide ou indécodable. Lorsqu'une alarme n'a pas de vidéo, vérifiez que l'intervalle d'images clés H.264 ne dépasse pas cinq secondes.

Si la récupération automatique limitée est épuisée, le panneau affiche **La vidéo en direct n’a pas pu être lue.** Vérifiez que la caméra et le réseau sont accessibles, puis sélectionnez **Réessayer**. Cette action arrête la tentative précédente et en lance une nouvelle ; elle ne nécessite ni réinitialisation ni redémarrage d'OSCAR, ni réimportation des voies, ni suppression de l'état de l'application ou des volumes Docker.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#11, 12.0s) : Inspecter le flux caméra en direct et les courbes gamma/neutrons</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/11-live-lane-video-and-charts.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/11-live-lane-video-and-charts.vtt" srclang="en" label="English" default>
  </video>
</details>

## 9. Statistiques nationales

![Statistiques nationales](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/30-national-statistics.png)

Une ligne par nœud donne **ID nœud**, alarmes Gamma, Neutron, combinées, occupations, sabotage, défauts Gamma, défauts Neutron et défauts totaux. Colonnes/Filtres/Densité ajustent la grille.

1. Choisissez **Tout le temps**, **30 derniers jours**, **7 derniers jours**, **24 dernières heures** ou **Plage personnalisée**.
2. Pour une plage, saisissez début et fin; la fin ne peut précéder le début.
3. **Actualiser les statistiques** commande chaque flux de contrôle du Service OSCAR. Les dates explicites ne sont envoyées que pour Personnalisée.
4. Attendez et vérifiez chaque nœud. Zéro ne prouve pas qu'il a répondu; examinez toute erreur de connexion/commande.

Les plages prédéfinies sont mises en cache et rechargées après actualisation. Les résultats dépendent du calendrier, des données conservées, des horloges et du réseau de chaque nœud.

## 10. Génération de rapports

![Générateur de rapports](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/31-report-generator.png)

Le **Générateur de rapports** demande au Service OSCAR du nœud sélectionné de générer un rapport actualisé dans le bucket `reports` (`PDF`, `CSV`, `JSON` ou `Excel (.xlsx)`) et affiche à droite soit le PDF intégré, soit un aperçu tabulaire interactif avec téléchargement direct.

1. Choisissez **Nœud** et **Portée du rapport** (**Nœud entier**, **Vue opérationnelle** ou **Voies spécifiques**).
2. Choisissez le **Type de rapport** :
   - **RDS Site (`RDS_SITE`)** : synthèse exécutive du site, taux d'occupation et d'alarmes, répartition des dispositions automatiques (`EML Suppressed (9999)`, `Extended Occupancy Gamma Alarm (9998)` et `Operator Review Required`), répartition des défauts, capacité de stockage et tableau récapitulatif par voie (sans tableaux redondants).
   - **Lane (`LANE`)** : indicateurs opérationnels par voie, taux d'alarmes et de dispositions automatiques (`9998`/`9999`), répartition des défauts et série temporelle quotidienne pour la portée choisie.
   - **Event (`EVENT`)** : choisissez **Alarmes et occupations**, **Alarmes** ou **État de santé** (`Gamma High`, `Gamma Low`, `Neutron High`, `Tamper`) pour produire les graphiques, tableaux quotidiens et matrices **UpSet** (plaçant d'abord les catégories peuplées et regroupant les intersections `SNM Present` rares dans une colonne cramoisie **`Other SNM`** afin d'égaler le total `SNM` exact).
   - **Adjudication (`ADJUDICATION`)** : commence par les tableaux cumulés **Toutes les voies sélectionnées (`ALL_LANES`)** (**Disposition** et **Résultats isotopiques**) pour l'ensemble des voies sélectionnées, suivis des sections **Disposition** et **Résultats isotopiques** propres à chaque voie.
   - **Adjudication Details Extract (`ADJUDICATION_DETAILS`)** : extraction d'audit ligne par ligne des décisions soumises (`Heure de soumission`, `Voie`, `Utilisateur`, `Compteur d'occupation`, `Code d'adjudication`, `Commentaires`, `Inspection secondaire`, `Isotopes`, `ID de véhicule`, `Chemins de fichiers` et `ID d'occupation`).
   - **Vehicle Incident Report (`VEHICLE_INCIDENT`)** : dossier consolidé multi-passages pour un ID de véhicule normalisé (conteneur ISO 6346 ou plaque dans les adjudications et les lectures `vehicleOcr`, y compris les passages facultatifs sur nœuds distants fédérés), réunissant indicateurs clés, comparaison chronologique entre comptage global et contrôle spectroscopique RS350, courbes superposées gamma/neutrons, résultats EML et WebID, images clés/vignettes OCR et historique complet des adjudications.
3. Choisissez le **Format d'exportation** : **PDF**, **CSV**, **JSON** ou **Excel (`.xlsx`)**.
4. Choisissez **24 heures**, **7 jours**, **30 jours**, **Ce mois** ou **Plage personnalisée** avec deux dates valides.
5. Cliquez sur **Générer** et gardez la page ouverte jusqu'à la fin. Consultez le PDF intégré ou l'aperçu tabulaire interactif (`CSV`, `JSON`, `Excel (.xlsx)`) et utilisez le bouton de téléchargement si nécessaire.

Chaque demande génère un nouveau fichier et remplace tout rapport antérieur portant les mêmes paramètres afin d'inclure immédiatement les dernières occupations ou adjudications. Seules les données conservées sont rapportées.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#16, 19.6s) : Analyser les statistiques nationales et générer des rapports multiformats</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/16-national-statistics-and-reports.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/16-national-statistics-and-reports.vtt" srclang="en" label="English" default>
  </video>
</details>

## 11. Fédération de nœuds

![Fédération de nœuds](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/32-node-federation.png)

**Nœuds** interroge plusieurs serveurs. Le nœud local dérivé du navigateur est par défaut et non supprimable; les distants peuvent être ajoutés, modifiés ou retirés.

1. Saisissez un **Nom** unique et une **Adresse** sans schéma.
2. Indiquez le **Port** (initial `8282`) et l'**Endpoint Connected Systems API** (défaut `/api`, ajouté à `/sensorhub`).
3. Saisissez utilisateur/mot de passe de lecture.
4. Activez **Connexion sécurisée** pour HTTPS/WSS/MQTTS avec certificat/nom valides.
5. Choisissez la **Méthode d'authentification** (ou cliquez sur **Utiliser un jeton de fédération (`oscar_fed_...`)**, ou ouvrez `/servers?auth=bearer` depuis **Utilisateurs et accès -> Jetons de fédération**) : **Session navigateur interactive** (par défaut, crée un cookie `HttpOnly` opaque), **Jeton de service de fédération (`oscar_fed_...`)** (port `443` HTTPS, créé dans **Utilisateurs et accès -> Jetons de fédération** sur le nœud distant pour authentifier HTTP `Bearer` et MQTT `svc-fed-<shortId>` sans mot de passe humain) ou **Nœud Basic uniquement** (secrets en mémoire jusqu'au rechargement/fermeture).
6. **Ajouter/Enregistrer** teste d'abord l'endpoint complet; inaccessible/non autorisé n'est pas sauvegardé. **Annuler** abandonne.

Nom, réseau, TLS/mode et carte sont enregistrés, jamais utilisateur/mot de passe; les anciennes entrées sont nettoyées. Les noms et couples adresse/port en doublon sont refusés. **Supprimer** retire seulement la configuration de ce navigateur, pas le serveur ni ses données.

Vérifiez voies/événements distants, ligne Nationale, rapport et événement avec média pour tester API, commandes, bucket et médias. Après rechargement, vérifiez la session et ressaisissez les secrets Basic. Le distant doit autoriser origine/CORS, chemins, WebSocket/MQTT, droits et confiance TLS.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#08, 16.9s) : Se connecter à OSCAR Viewer, changer la langue et gérer les nœuds</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/08-viewer-signin-language-and-nodes.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/08-viewer-signin-language-and-nodes.vtt" srclang="en" label="English" default>
  </video>
</details>

### 11.3 Recherche multi-nœuds de véhicules, rapports consolidés et révocation SSO

Une fois les portails distants connectés via des jetons `oscar_fed_...`, **Événements -> Comparer les passages** corrèle les conteneurs ISO 6346 et plaques sur tous les nœuds, et le **Générateur de rapports -> Rapport d'incident véhicule (`VEHICLE_INCIDENT`)** produit un rapport unifié (`PDF`, `CSV`, `JSON`, `XLSX`) indiquant le nœud et la voie de chaque passage. Pour révoquer complètement un utilisateur sur `ORNL/oscar-federation-hub`, supprimez le compte dans Authentik (**Directory -> Users -> Delete**), laissez `oscar-hub-sync` le retirer d'Octelium et supprimez toute ligne mise en cache dans **Utilisateurs et accès -> Utilisateurs et rôles**.

![Recherche et comparaison multi-nœuds de véhicules](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/41-multi-node-vehicle-search.png)

![Rapport consolidé multi-nœuds d'incident véhicule](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/42-multi-node-incident-report.png)

![Révocation complète des utilisateurs SSO](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/43-sso-user-revocation.png)

<details class="oscar-video-guide">
  <summary>Guide vidéo (#20, 28.4s) : Fédérer plusieurs sites, rechercher des véhicules multi-nœuds et générer des rapports</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/20-multi-node-federation-vehicle-search-and-report.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/20-multi-node-federation-vehicle-search-and-report.vtt" srclang="en" label="English" default>
  </video>
</details>

<details class="oscar-video-guide">
  <summary>Guide vidéo (#21, 24.0s) : Intégrer des utilisateurs SSO Passkey dans Authentik et révoquer tout accès</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/21-federated-sso-passkey-onboarding-and-revocation.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/21-federated-sso-passkey-onboarding-and-revocation.vtt" srclang="en" label="English" default>
  </video>
</details>

## 12. Conservation des données et stockage

Le profil standard 3.11.0 utilise l'architecture à plusieurs niveaux PostgreSQL + Apache Parquet en mode
`PARQUET_PRIMARY` (`OSCAR_TIER_STORAGE_MODE=PARQUET_PRIMARY`). Les flux de configuration statiques
(`sensorLocation`, `adjudicationCodes`, `siteInfo`) restent dans PostgreSQL `ONLY oscar.obs`, tandis que
les 22 sorties opérationnelles et probantes autorisées (`gammaCounts`, `gammaThreshold`, `neutronCounts`,
`setupGamma`, `setupNeutron`, `speed`, `backgroundReport`, `foregroundReport`, `status`, `emlContextual`,
`emlScanContextual`, `emlAnalysis`, `dailyFile`, `connectionStatus`, `occupancyStatus`, `tamper`,
`occupancy`, `adjudication`, `report`, `siteStatistics`, `nationalSummary`, `vehicleOcr`) sont écrites dans des
partitions de transit de 30 minutes (`osg_*`). Cinq minutes après la clôture de chaque fenêtre de
30 minutes, des tâches d'arrière-plan exportent la partition vers des fichiers Apache Parquet catalogués
et compressés en ZSTD (`archive/datasets/observations/`), alimentent `oscar_obs_latest`, les compteurs de
résumé de `oscar_archive_schema` et `oscar_event_locator`, vérifient la parité exacte des lignes/empreintes
et suppriment la partition de transit afin de garder PostgreSQL compact (et au démarrage, OSCAR réconcilie
les routes existantes et exporte rétroactivement vers Parquet toute ligne historique non archivée de la
table parente pour ces 22 sorties). Les décisions et ré-adjudications portant sur des occupations archivées
sont enregistrées dans `oscar_obs_overlay` + `oscar_obs_latest` sans réécrire Parquet, et chaque heure
OSCAR purge les commandes opérationnelles transitoires dans `cmd_commands`/`cmd_status` de plus de
24 heures (tout en conservant définitivement les commandes d'audit `%adjudicate%`). (Les sites configurés
explicitement en mode `HOT_RETENTION` conservent les lignes chaudes pendant la fenêtre configurée avant de
les retirer après vérification de parité.) La propriété de l'archive, l'intégrité des objets, la parité et
les conservations actives sont revérifiées avant toute suppression et échouent de façon sûre.

Chaque pilote RPM exige le journal direct des fichiers quotidiens. Il capture si possible
l'enregistrement source avant analyse, puis publie la journée terminée sous forme de CSV historique
immuable et de manifeste SHA-256. La conservation de la base ne doit jamais remplacer ce journal.
La maintenance des états de connexion et le nettoyage, contrôlé par manifeste, des observations
`dailyFile` historiques se poursuivent indépendamment.
Les segments privés scellés du journal sont conservés pendant 30 jours à compter du moment le plus
tardif entre la fermeture du jour et la publication réelle. Avec la valeur par défaut
`requireBackupBeforeSealedJournalDeletion=false`, la suppression exige aussi la validation du CSV
immuable, du manifeste et du registre, ainsi qu'une correspondance exacte entre la relecture des
segments scellés actuels et le CSV publié. Le site peut activer ce paramètre afin d'exiger en plus la
confirmation d'une sauvegarde vérifiée correspondante.

Chaque nouvel enregistrement d'occupation est lié à la voie, la caméra, l'horodatage d'occupation
et la génération immuable exacts. Les commandes caméra s'exécutent dans l'ordre, le nom source
porte un suffixe de génération et seul le chemin validé d'une fermeture réussie est joint à
l'alarme. Les enregistrements hors alarme, périmés, en échec, expirés ou tardifs non publiés, ainsi
que les fichiers partiels masqués, sont supprimés au lieu d'être réutilisés par un événement
ultérieur. Un chemin vide, dupliqué ou associé au mauvais événement ou à la mauvaise caméra, ou un
nouveau fichier orphelin, est un défaut d'intégrité bloquant la version ; ne corrigez pas la base en
devinant un nom de fichier.

Les vidéos d'alarme éligibles sont mises en file pour être converties de manière asynchrone en
exactement cinq JPEG décodables séparément (première image, quart, milieu, trois quarts et dernière
image) dès leur découverte. Après une publication réussie, la source est conservée pendant 24 heures
supplémentaires de délai de retour arrière ; sa suppression normale ne peut donc pas survenir avant
cette échéance relative à la publication. Tout échec de conversion et toute conservation applicable
préservent la source. Après 30 jours (`retainedSecondaryFramesDays = 30`), les images secondaires
(`frame-01.jpg`, `frame-02.jpg`, `frame-04.jpg`, `frame-05.jpg`) sont purgées tandis que l'image clé
centrale (`frame-03.jpg`) est conservée pendant toute la durée de rétention des médias. Les événements anciens ou non
convertis gardent la lecture historique. Le nettoyage sous pression ne peut enlever que des
ensembles de médias retenus complets et éligibles. Il ne supprime ni fichiers quotidiens, rapports,
décisions, vidéos historiques non cataloguées, ni archives Parquet.

La sauvegarde gérée par OSCAR est désactivée par défaut. Dans ce mode, `backup`, `backup-verify`,
`backup-prune` et `restore` sont refusées avant toute mutation. PostgreSQL s'exécute avec
`archive_mode=off`, sans commande d'archivage externe ni montage WAL de l'hôte. Un site qui a besoin
d'un point de reprise géré peut utiliser la mise à niveau validée pour choisir le mode `single` sur
un chemin de sauvegarde absolu sûr, choisi par l'opérateur et distinct des racines de la version et
de l'archive. Un disque physique séparé est recommandé pour isoler les défaillances, mais
n'est pas obligatoire. Chaque sauvegarde manuelle est un instantané autonome dont les WAL
nécessaires sont transférés dans la sauvegarde de base ; le nouvel instantané doit être vérifié avant
le changement de `CURRENT` et la suppression de l'ancien instantané géré. La commande historique
`backup-prune` est retirée.

Utilisez les commandes `archive status`, `archive verify`, de conservation, d'export portable et de
suppression avec simulation/jeton du lanceur, ainsi que `backup` et `backup-verify` uniquement en mode
`single`. Protégez séparément la clé de chiffrement et conservez une copie hors hôte si la politique
du site exige un autre domaine de défaillance. Ne supprimez jamais à la main les fichiers catalogués.
Une mise à niveau antérieure à 3.11.0 conserve l'ancienne base et configuration : elle ne crée pas tout
le profil et n'active pas les sommes de contrôle PostgreSQL. Initialisez un nouveau déploiement 3.11.0
ou appliquez une migration de production validée séparément. Les guides de stockage de
`documentation/` sont les procédures de référence.

Utilisez le lanceur de la version pour arrêter ou redémarrer tout le déploiement. Il arrête d'abord
la passerelle, laisse ensuite OSCAR vider ses files et enregistrer pendant que PostgreSQL reste
disponible, puis arrête PostgreSQL en dernier. N'exécutez pas
`docker compose stop` ou `docker compose restart` sans limiter les services : des signaux
simultanés peuvent retirer la base de données alors qu'OSCAR termine encore les écritures en file
et l'arrêt de ses modules.

Avant de modifier conservation, base, chemins, mode de sauvegarde ou **Supprimer
les données avec la voie**, confirmez les exigences de preuve, confidentialité, gel juridique,
reprise et destruction des documents.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#01, 11.2s) : Initialiser le déploiement OSCAR, les identifiants administrateur et TLS</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/01-deployment-init-and-credentials.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/01-deployment-init-and-credentials.vtt" srclang="en" label="English" default>
  </video>
</details>

<details class="oscar-video-guide">
  <summary>Guide vidéo (#02, 12.8s) : Mettre à niveau OSCAR dans un répertoire de version propre</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/02-clean-directory-upgrade.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/02-clean-directory-upgrade.vtt" srclang="en" label="English" default>
  </video>
</details>

<details class="oscar-video-guide">
  <summary>Guide vidéo (#03, 11.4s) : Vérifier les archives froides Parquet et la sauvegarde instantanée unique</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/03-archive-and-snapshot-backup.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/03-archive-and-snapshot-backup.vtt" srclang="en" label="English" default>
  </video>
</details>

## 13. Liste de validation

### Certificat et accès

- [ ] Le bon hôte s'ouvre sans avertissement et l'empreinte correspond.
- [ ] Le compte administrateur fonctionne sans mot de passe partagé/par défaut.
- [ ] La langue affiche les contrôles et l'aide correspondants.

### Service OSCAR

- [ ] ID de nœud unique ; base, Web ID, statistiques et démarrage automatique vérifiés.
- [ ] Conservation vidéo et pression examinées avant toute purge.
- [ ] Une base/configuration 3.11.0 neuve ou une migration approuvée est utilisée ; l'état et l'intégrité de l'archive sont valides.
- [ ] Le mode de sauvegarde est consigné. Le mode désactivé ne crée aucune sauvegarde gérée par OSCAR ; si `single` est activé, son instantané courant est vérifié, sa clé externe est séquestrée séparément et un exercice de restauration isolé a réussi.
- [ ] Une voie témoin publie CSV/manifeste depuis le journal ; les événements retenus montrent cinq JPEG et les anciens restent lisibles.
- [ ] Services de compartiments et OSCAR démarrés.

### Plan

- [ ] Image nord en haut, bien recadrée, PNG/JPG/JPEG.
- [ ] Limites valides, ordonnées et non nulles.
- [ ] Téléversement réussi, modifications appliquées, Enregistrer confirmé avec Oui et message d'enregistrement affiché.
- [ ] Viewer utilise l'étendue du plan et les marqueurs sont alignés.

### Voies et appareils

- [ ] Noms d'au plus 12 caractères et IDs uniques.
- [ ] Type/hôte/port/options RPM corrects.
- [ ] Type, hôte/port, identifiants, chemin/codec et tampon des caméras corrects.
- [ ] Enfants attendus démarrés ; une panne matérielle ne bloque pas la validation des autres voies.
- [ ] Enregistrement global confirmé avec Oui après import ou création ; le message d'enregistrement s'est affiché.

### Viewer

- [ ] États et marqueurs se mettent à jour.
- [ ] Une occupation de test affiche des valeurs plausibles.
- [ ] Vidéo en direct et enregistrée fonctionne, y compris après actualisation.
- [ ] Détails s'ouvre sans exception côté client.
- [ ] Une adjudication contrôlée peut être vérifiée et envoyée.
- [ ] Les filtres ET/OU imbriqués d'Événements, la sélection de tout le résultat filtré, une adjudication groupée contrôlée, l'actualisation Nationale et les rapports requis ont été testés.
- [ ] Un QR d'alarme contrôlée s'exporte, se scanne/importe, passe le contrôle d'intégrité et redessine des courbes gamma/neutron plausibles.
- [ ] **État de santé** répertorie toutes les voies attendues ; RPM et caméras affichent correctement En ligne/Hors ligne/En attente, les défauts affichent correctement Défaut/Normal/Inconnu et les tests autorisés gamma, neutron, sabotage et occupation prolongée d'une minute donnent l'état prévu.
- [ ] L'URL non limitée affiche toutes les voies, et chaque URL approuvée `?view=<clé>` ou `/view/<clé>` n'affiche que ses voies affectées dans Tableau de bord, Événements, rapports, Transfert d'alarme et État de santé.
- [ ] Chaque nœud fédéré a été revérifié après rechargement; aucun identifiant n'est stocké par le navigateur.

## 14. Dépannage

| Symptôme | Vérifications et correction |
| --- | --- |
| Avertissement TLS persistant | Vérifiez hôte/SAN, dates, magasin et portée utilisateur/machine, puis redémarrez le navigateur. Ne contournez jamais une empreinte différente. |
| Limites invalides | Ajoutez les deux objets, quatre nombres finis, puis vérifiez plage et ordre inférieur/supérieur. |
| Téléversement réussi mais plan absent | Appliquez et enregistrez, confirmez les services démarrés, rechargez Viewer et vérifiez l'objet du compartiment. |
| Plan trop petit | Recadrez plus serré et saisissez les coordonnées exactes des coins. |
| Marqueur absent | Vérifiez l'emplacement fixe enregistré et inclus dans les limites. Une épingle de la capture n'est pas un marqueur OSCAR. |
| Tuiles OSM 403/bloquées | N'utilisez pas directement des serveurs bénévoles contrairement à leur politique. Configurez un fournisseur/proxy OSM approuvé ou utilisez Esri pendant la correction. |
| CSV refusé | Vérifiez en-tête/ordre, groupes de six, index continus, nombre de cellules, nombres, cellules vides non citées et absence de virgules internes. |
| Voie manquante après CSV | Cherchez un UniqueID déjà chargé, un nom trop long, des erreurs d'enfants et attendez le chargement asynchrone. |
| **Capteurs** est vide après un redémarrage suivant le téléversement du CSV | Le téléversement a créé des modules en mémoire, mais la configuration complète n'a pas été persistée. Téléversez de nouveau si nécessaire, attendez toutes les voies et tous les enfants, utilisez l'action globale **Enregistrer**, redémarrez OSCAR et vérifiez encore **Capteurs**. Si un nœud 3.11.0 perd toujours les voies, consultez les journaux OSCAR et vérifiez que le propriétaire peut écrire dans `/var/lib/oscar/config.json`. |
| Vue opérationnelle vide ou clé rejetée | Vérifiez que les **Clés de vue opérationnelle** de la voie contiennent exactement la clé en minuscules, appliquez et enregistrez globalement, redémarrez la voie si nécessaire, puis utilisez `?view=<clé>` ou `/view/<clé>`. Une affectation vide ne correspond jamais à une vue limitée. |
| RPM ne démarre pas | Testez hôte, port, pare-feu, plage Aspect et disponibilité ; consultez l'erreur du pilote enfant. |
| Caméra ne démarre pas | Testez RTSP/identifiants, retirez `rtsp://` de l'hôte, évitez un port en double, vérifiez codec Axis ou chemin Custom depuis l'hôte OSCAR. |
| Vidéo perdue après actualisation | Vérifiez que la caméra et HLS restent démarrés et consultez les journaux. Actualiser ne doit pas imposer de recréer la voie. |
| Événement sans média | Vérifiez voie disponible, flux sur l'intervalle, conservation et droits. |
| Voie/appareil absent dans État de santé, ou connexion En attente ou Hors ligne | Vérifiez que la voie appartient à la vue actuelle, que la voie et l'appareil enfant sont démarrés, et que le RPM ou la caméra FFmpeg publie son état de connexion. En attente s'applique à la télémétrie de connexion et signifie qu'aucune valeur de connexion exploitable n'est encore arrivée ; testez la connexion et examinez les journaux de l'enfant. |
| Un défaut ou l'état de sabotage est Inconnu | Vérifiez que son flux de données existe et que sa dernière observation est lisible. Un pilote Rapiscan actuel publie une valeur initiale Normal à chaque connexion ; confirmez que le pilote enfant s'est reconnecté et que l'observation est lisible. Un `TT` ultérieur du matériel doit toujours faire passer l'état à Défaut. |
| L'occupation prolongée n'apparaît pas | Vérifiez que le Système de voie publie `occupancyStatus`, que l'entrée fichier quotidien/état du RPM signale l'entrée et la sortie, vérifiez le **Seuil d'occupation prolongée (min)** (`extendedOccupancyThresholdMinutes`, 1–1440 min) de la voie dans SensorHub Admin ainsi que le seuil de 1 à 1440 minutes dans ce navigateur, et attendez que la durée stable le dépasse. |
| L'adjudication groupée échoue partiellement | Gardez les lignes en échec sélectionnées, vérifiez la connexion voie/nœud et le flux de commande d'adjudication, puis choisissez **Réessayer les échecs**. Les succès ne sont pas renvoyés. |
| Échec d'adjudication | Choisissez un code; vérifiez nœud/voie, flux de commande et téléversement. Ne renvoyez pas avant de connaître le premier résultat. |
| WebID sans DRF/résultat | Vérifiez Full Spectrum, DRF, premier/arrière-plan et colonnes avertissement/erreur. La décision reste humaine. |
| Nationale vide/zéro | Actualisez la plage et vérifiez commande statistique, données conservées et authentification du nœud. |
| Rapport absent | Complétez nœud, portée, type de rapport, format (`PDF`, `CSV`, `JSON` ou `XLSX`), plage et voie/type d'événement ; vérifiez dates, commande et bucket `reports`. Chaque demande génère un nouveau fichier et remplace tout fichier antérieur équivalent. |
| Nœud distant refusé après rechargement | La session exige un cookie valide; Basic exige de ressaisir les secrets. Vérifiez TLS, CORS, chemins, port et droits. |
| QR d'alarme impossible à générer | Vérifiez la voie et les observations gamma/neutron conservées sur l'intervalle. Si un seul QR ne suffit pas, utilisez le fichier d'alarme téléchargeable. |
| QR impossible à scanner/importer | Augmentez la luminosité ou utilisez le PNG à sa taille d'origine; sinon importez `.oscar-alarm.json`. Un échec d'intégrité impose un nouvel export, jamais le contournement du contrôle. |
| Modifications perdues au redémarrage | Appliquez le formulaire, utilisez Enregistrer globalement, confirmez Oui et attendez le message d'enregistrement avant de redémarrer. |

Pour l'assistance technique, exportez une archive de diagnostic expurgée (`.zip`) :
- **Depuis la Visionneuse OSCAR (lorsque l'interface est accessible) :** Ouvrez **Aide (`?`)** dans la barre supérieure et sélectionnez **Exporter le pack d'assistance** (également disponible sous **Dépannage et FAQ**). OSCAR regroupe les avertissements/erreurs récents du navigateur, les requêtes API lentes ou en échec, l'état des modules et des voies, le résumé JVM et stockage, la configuration expurgée et les journaux serveur rotatifs (`oscar-support-<UTC>.zip`), ou télécharge une archive client seul (`oscar-client-support-<UTC>.zip`) si le serveur est injoignable.
- **Depuis la CLI de l'hôte (si OSCAR est dégradé ou ne démarre pas) :** Exécutez `sudo bash oscar.sh support-bundle` sous Ubuntu ou `.\oscar.bat support-bundle` sous Windows (options `--tail`/`-Tail` et `--output`/`-Destination`) pour collecter l'état hôte/Docker, les journaux de conteneurs, les métadonnées du certificat TLS et les journaux/configurations en lecture seule de `oscar_oscar_state` dans `oscar-host-support-<UTC>.zip`. Les mots de passe, jetons, témoins de session, courriels, `secrets/*` et clés privées TLS (`tls/server.key`) sont automatiquement masqués ou exclus ; vérifiez la politique du site avant tout partage externe.

![Guide rapide opérateur et exportation d'archive d'assistance expurgée](https://raw.githubusercontent.com/ORNL/osh-oakridge-modules/main/docs/oscar-operator-manual/images/34-viewer-help-support-bundle.png)

<details class="oscar-video-guide">
  <summary>Guide vidéo (#18, 10.9s) : Utiliser le Guide rapide (?) et exporter l’archive d’assistance masquée</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/18-operator-help-and-support-bundle.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/18-operator-help-and-support-bundle.vtt" srclang="en" label="English" default>
  </video>
</details>

## 15. Documentation associée

- [Guides vidéo pratiques OSCAR](https://github.com/ORNL/oscar-video-guides)
- [Index des guides vidéo OSCAR](https://github.com/ORNL/oscar/blob/main/docs/VIDEO_GUIDES.md)
- [Démarrage rapide OSCAR](https://github.com/ORNL/oscar/blob/main/dist/release/QUICKSTART.md)
- [Guide de déploiement](https://github.com/ORNL/oscar/blob/main/dist/release/DEPLOYMENT.md)
- [Guide du système de traduction](https://github.com/ORNL/oscar/blob/main/docs/TRANSLATION_SYSTEM.md)
- [Module Système de voie](https://github.com/ORNL/osh-oakridge-modules/tree/main/sensors/sensorhub-system-lane)
- [Module de service OSCAR](https://github.com/ORNL/osh-oakridge-modules/tree/main/services/sensorhub-service-oscar)

---

Référence du document : comportement du code OSCAR 3.11.0, revu le 2026-10-18. Si une version ultérieure change les champs ou procédures, mettez à jour ensemble le manuel canonique anglais et les trois traductions.
