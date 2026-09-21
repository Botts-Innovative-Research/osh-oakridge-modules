# Manuel d'administration et d'exploitation d'OSCAR 3.8.3

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

- d'un déploiement OSCAR 3.8.3 installé et démarré ;
- de l'URL OSCAR, généralement `https://oscar.local/` sauf si un autre hôte a été choisi ;
- d'un compte administrateur pour `/sensorhub/admin` ;
- de l'image de site approuvée et des coordonnées des coins inférieur gauche et supérieur droit ;
- de l'adresse IP ou DNS et du port de chaque RPM ;
- de l'adresse IP/DNS, des identifiants, du fabricant et des informations de flux de chaque caméra ; et
- d'un navigateur pris en charge sur un poste capable de résoudre et de joindre l'hôte OSCAR.

Consultez le [Guide de démarrage rapide](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/dist/release/QUICKSTART.md) pour l'installation. Le [Guide de déploiement](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/dist/release/DEPLOYMENT.md) couvre l'initialisation, le DNS, les certificats, les commandes de cycle de vie, les contrôles de sécurité et les mises à niveau. OSCAR ne fournit aucun mot de passe administrateur par défaut ; il est défini pendant `oscar init`.

### Conventions de coordonnées et de réseau

- Latitude et longitude sont exprimées en degrés décimaux WGS 84. La latitude est positive au nord et négative au sud ; la longitude est positive à l'est et négative à l'ouest.
- Un rectangle valide exige `latitude inférieur gauche < latitude supérieur droit` et `longitude inférieur gauche < longitude supérieur droit`. La latitude doit être comprise entre -90 et 90, la longitude entre -180 et 180.
- Pour un RPM, hôte et port ont des champs séparés. La caméra n'a pas de champ de port séparé ; ajoutez un port non standard à l'hôte, par exemple `192.0.2.25:8554`.
- N'ajoutez pas `rtsp://` à l'hôte d'une caméra de voie. Le Système de voie génère l'URL RTSP.

## 2. Approuver le certificat OSCAR

OSCAR utilise HTTPS. Un déploiement muni de son certificat autosigné affiche un avertissement de confidentialité jusqu'à ce que le poste lui fasse confiance. Un certificat émis par une autorité publique ou d'entreprise déjà approuvée ne nécessite pas cette procédure.

![Avertissement de certificat dans Chrome](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/01-certificate-warning.png)

### 2.1 Vérifier avant d'approuver

1. Vérifiez que la barre d'adresse contient exactement le nom d'hôte OSCAR communiqué par l'administrateur.
2. Demandez l'empreinte SHA-256 du certificat OSCAR ou de son autorité émettrice.
3. Ouvrez les détails du certificat dans le navigateur et comparez l'empreinte. Arrêtez si elle ne correspond pas.

Les captures utilisent Chrome sous Windows ; le libellé peut varier selon la version.

### 2.2 Exporter depuis Chrome sous Windows

1. Ouvrez `https://<hote-oscar>/sensorhub/admin`.
2. À l'avertissement, utilisez **Paramètres avancés** uniquement après avoir confirmé le serveur attendu.
3. Ouvrez les informations du site à côté de l'adresse et sélectionnez **Détails du certificat**.

![Ouvrir les détails du certificat](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/02-certificate-details.png)

4. Dans la visionneuse, sélectionnez le certificat, ouvrez **Détails**, puis **Exporter**.

![Exporter le certificat](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/03-certificate-export.png)

5. Enregistrez-le au format X.509 codé Base-64, de préférence avec l'extension `.cer` ou `.crt`. L'extension `.download` de l'exemple peut être importée si Windows la reconnaît, mais `.cer` est plus explicite.

![Enregistrer le certificat](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/04-certificate-save.png)

Si une autorité privée est utilisée, importez le **certificat d'AC** fourni par l'administrateur, et non un certificat de serveur obtenu sur une page non vérifiée.

### 2.3 Importer dans le magasin de confiance Windows

1. Ouvrez le certificat et sélectionnez **Installer le certificat**, ou exécutez `certmgr.msc`, ouvrez **Autorités de certification racines de confiance > Certificats**, puis **Importer**.
2. Choisissez **Utilisateur actuel** pour ce seul utilisateur. Choisissez **Ordinateur local** uniquement si la politique exige une confiance globale et que vous êtes autorisé à l'administrer.
3. Sélectionnez **Placer tous les certificats dans le magasin suivant**, puis **Autorités de certification racines de confiance**.
4. Sélectionnez le fichier exporté et terminez l'assistant.

![Choisir le certificat à importer](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/05-certificate-import.png)

5. Fermez toutes les fenêtres du navigateur, relancez-le et rechargez OSCAR. L'avertissement doit avoir disparu et le certificat doit être valide pour l'hôte.

Sur un poste administré, distribuez la confiance par le mécanisme de l'organisation. Sous macOS, importez le certificat ou l'AC vérifiée dans Trousseaux d'accès. Sous Linux, utilisez le magasin du navigateur ou du système prévu par la distribution. Le Guide de déploiement reste la référence pour créer ou remplacer le certificat côté serveur.

## 3. Se connecter et comprendre l'enregistrement

Ouvrez `https://<hote-oscar>/sensorhub/admin` et saisissez les identifiants créés lors du déploiement.

![Connexion à l'administration OSCAR](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/06-login.png)

### 3.1 Langue

Utilisez le sélecteur de langue en haut de l'administration ou d'OSCAR Viewer. Anglais, espagnol, français et grec sont pris en charge. Le choix est partagé entre les deux interfaces et s'applique au chargement ou rechargement. Cet onglet README ouvre automatiquement le manuel localisé.

### 3.2 Trois actions différentes

- **Téléverser** envoie immédiatement le fichier ; pour le CSV, cela démarre l'importation des voies.
- **Appliquer les modifications** valide le formulaire du module affiché et met à jour sa configuration en cours d'exécution.
- **Enregistrer** dans l'en-tête écrit toute la configuration afin qu'elle survive à un redémarrage.

Après toute configuration, utilisez **Appliquer les modifications** lorsqu'il est disponible, puis **Enregistrer**. Un message de téléversement réussi prouve que le fichier a été accepté, pas que tous les appareils se connectent ni que tout est persistant.

## 4. Configurer le module de service OSCAR

Dans la navigation de gauche, ouvrez **Services > Module de service OSCAR**.

![Configuration générale du service OSCAR](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/14-service-general.png)

### 4.1 Onglet Général

| Champ | Signification et recommandations |
| --- | --- |
| Classe du module | Implémentation Java choisie ; laissez-la gérée par le système. |
| Nom du module | Nom lisible affiché dans l'arborescence. |
| ID du module | Identifiant local unique. Conservez celui généré sauf migration contrôlée. |
| Description | Description administrative facultative. |
| Chemin de configuration de feuille de calcul | Téléverse immédiatement un CSV de voies. **Télécharger** exporte les voies chargées dans `config.csv` ; indisponible s'il n'y a aucune voie. |
| ID du nœud | Identifiant obligatoire et unique de ce nœud OSCAR. Ne le réutilisez pas sur un autre nœud. |
| ID de base de données | Base d'observations utilisée. Vide, les statistiques utilisent la base fédérée ; les tâches de purge/export propres à une base ne démarrent qu'avec une sélection explicite. |
| Racine de l'API WebID | URL de Sandia Full Spectrum Web ID. Valeur par défaut : `https://full-spectrum.sandia.gov/api/v1`. Utilisez un service local approuvé en environnement isolé ou laissez vide pour ne pas créer le client Web ID. |
| Fréquence des statistiques (min) | Intervalle de publication, 60 minutes par défaut. |
| Démarrage automatique | Démarre le service au chargement du nœud. À activer normalement en exploitation. |

Le service requiert un Service de stockage par compartiments démarré. S'il est introuvable, démarrez et vérifiez **Services > Bucket Storage Service**, puis réinitialisez ou redémarrez le service OSCAR.

### 4.2 Onglet Configuration du plan de site

Il associe une image PNG/JPG/JPEG à ses limites inférieur gauche et supérieur droit. La section 5 détaille la procédure.

### 4.3 Onglet Conservation vidéo

| Champ | Défaut | Comportement |
| --- | ---: | --- |
| Délai avant conservation/suppression d'images clés (jours) | 7 | Âge auquel une vidéo d'occupation est traitée. |
| Période de requête vidéo (minutes) | 1 | Intervalle de recherche des vidéos éligibles. Une valeur plus grande regroupe davantage d'enregistrements. |
| Activer la conservation d'images | activé | Activé : réduit les anciens clips au nombre d'images indiqué. Désactivé : supprime les clips éligibles. |
| Nombre d'images clés conservées | 5 | Images conservées ; sans effet si la conservation est désactivée. |

Ces paramètres concernent la vidéo historique d'occupation, pas le direct. Choisissez-les selon la politique de conservation des preuves.

### 4.4 Onglet Conservation sous pression de stockage

| Champ | Défaut | Comportement |
| --- | ---: | --- |
| Seuil de déclenchement (%) | 85 | Déclenche la purge à ce taux. Doit être supérieur à la cible et au plus égal à 100. |
| Utilisation cible (%) | 80 | La purge continue jusqu'à cette cible ou jusqu'à épuisement des fichiers éligibles. |
| Période de contrôle (minutes) | 1 | Fréquence des contrôles ; doit être positive. |
| Âge minimal de l'objet (minutes) | 10 | Protège les fichiers plus récents ; ne peut pas être négatif. |
| Chemin de stockage | `files` | Système de fichiers dont l'usage commande la purge. Doit viser le volume de fichiers OSCAR réel. |

La purge sous pression ne supprime que les objets éligibles du compartiment `videos`, en privilégiant les plus anciens ; les CSV quotidiens restent protégés. Surveillez aussi le stockage au niveau du système d'exploitation : ce mécanisme d'urgence ne remplace pas les sauvegardes.

## 5. Créer et téléverser un plan géoréférencé

Le plan est une image rectangulaire orientée au nord, interpolée linéairement entre deux coins géographiques. OSCAR affiche OSM par défaut lorsqu'il est joignable et qu'aucun plan n'est configuré. Avec un plan valide, Viewer ajuste l'étendue à ses limites, place le plan comme couche raster supérieure et conserve les marqueurs de voie au-dessus.

### 5.1 Ouvrir le formulaire

1. Ouvrez **Services > Module de service OSCAR > Configuration du plan de site**.
2. Si la configuration facultative n'existe pas, sélectionnez **Ajouter**.

![Configuration vide du plan](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/07-site-diagram-empty.png)

Le formulaire contient le téléversement et deux paires de coordonnées.

![Champs du plan](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/08-site-diagram-fields.png)

### 5.2 Préparer l'image

1. Ouvrez une source cartographique ou un plan approuvé. Si vous utilisez Google Maps, respectez ses conditions et la politique de l'organisation.
2. Utilisez une vue nord en haut, sans rotation ni inclinaison ; le modèle OSCAR est un rectangle aligné sur les axes.
3. Cadrez le plus petit rectangle contenant la zone d'exploitation et les voies. Un cadrage serré améliore le zoom initial.
4. Capturez uniquement ce rectangle et enregistrez-le en `.png`, `.jpg` ou `.jpeg`.

![Carte source cadrée](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/09-source-map.png)

![Plan recadré](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/10-cropped-site-diagram.png)

Évitez les menus, le curseur, les contrôles du navigateur et les grandes marges. Les étiquettes ou épingles déjà présentes sont des pixels de l'image, pas des marqueurs OSCAR.

### 5.3 Relever les limites

Notez les coordonnées exactes des coins de l'image :

- **Limite inférieur gauche** : coin sud-ouest (`latitude`, `longitude`).
- **Limite supérieur droit** : coin nord-est (`latitude`, `longitude`).

Dans Google Maps, cliquez avec le bouton droit sur un point pour afficher et copier ses coordonnées décimales. Répétez aux deux coins opposés du recadrage.

![Copier les coordonnées d'un coin](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/11-corner-coordinate.png)

Vérifiez que :

- la latitude supérieure droite est supérieure à la latitude inférieure gauche ;
- la longitude supérieure droite est supérieure (plus à l'est) ; sur le site américain illustré, elle est moins négative ;
- aucune paire n'est `0, 0` ; et
- le rectangle n'est pas si large que le site n'en occupe qu'une petite partie.

### 5.4 Téléverser et rendre persistant

1. Choisissez l'image avec **Choisir un fichier**.
2. Saisissez les quatre limites avant **Téléverser**.

![Limites saisies](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/12-site-bounds-entered.png)

3. Sélectionnez **Téléverser** et attendez le message vert.

![Téléversement réussi](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/13-site-upload-success.png)

4. Sélectionnez **Appliquer les modifications**, puis **Enregistrer** globalement.
5. Ouvrez ou rechargez Viewer et vérifiez l'alignement ainsi que l'étendue initiale.

Un nouveau téléversement valide remplace la référence active sans modifier les coordonnées des voies. Si les limites sont invalides, corrigez leur ordre et leur plage.

## 6. Importer ou exporter les voies avec `config.csv`

L'importation en masse est recommandée pour de nombreuses voies. Sur un site existant, utilisez d'abord **Télécharger** afin d'obtenir un modèle exact. Le fichier est sensible : les noms d'utilisateur et mots de passe des caméras sont exportés en clair.

### 6.1 Téléverser le CSV

1. Ouvrez **Services > Module de service OSCAR > Général**.
2. Sous **Chemin de configuration de feuille de calcul**, choisissez un `.csv`.

![Choisir un CSV de voies](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/15-csv-selected.png)

3. Sélectionnez **Téléverser** et attendez la confirmation.

![CSV téléversé](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/16-csv-upload-success.png)

4. Ouvrez **Capteurs** et inspectez chaque voie et ses sous-modules RPM/caméra. Le chargement est asynchrone ; attendez l'apparition de toutes les lignes.
5. Corrigez les valeurs par voie, appliquez les changements, puis enregistrez après validation.

![Voies importées](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/17-sensors-overview.png)

Une ligne est ignorée si son `UniqueID` appartient déjà à un Système de voie chargé ; les voies existantes ne sont pas écrasées. Assurez aussi l'unicité dans le fichier lui-même. L'analyse précède le chargement, mais l'initialisation des enfants est asynchrone : la panne d'un appareil peut toucher une voie sans annuler les autres déjà acceptées.

### 6.2 Schéma exact

Les 13 premiers en-têtes sont obligatoires, sensibles à la casse et doivent être dans cet ordre exact :

```csv
Name,UniqueID,AutoStart,Latitude,Longitude,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth
```

Ajoutez ensuite un groupe de six colonnes par caméra, à partir de 0 et sans saut :

```csv
CameraType0,CameraHost0,CameraPath0,Codec0,Username0,Password0
CameraType1,CameraHost1,CameraPath1,Codec1,Username1,Password1
```

Le code n'impose pas de nombre maximal de groupes séquentiels, mais le déploiement doit être dimensionné pour tous les flux. Chaque ligne doit comporter exactement autant de cellules que l'en-tête. L'analyseur sépare simplement sur les virgules : n'insérez ni virgule ni saut de ligne dans une valeur, même entre guillemets. Utilisez des cellules vides non citées.

### 6.3 Colonnes de voie et RPM

| Colonne | Obligatoire | Valeur et effet |
| --- | --- | --- |
| `Name` | oui | Nom affiché, limité à 12 caractères. |
| `UniqueID` | oui | Identifiant stable non réutilisable. Un suffixe simple devient une URN de voie. |
| `AutoStart` | oui | `true` ou `false` ; seul `true`, sans distinction de casse, vaut vrai. |
| `Latitude` / `Longitude` | ensemble | Degrés WGS 84. Laissez les deux vides pour omettre l'emplacement fixe. |
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
Name,UniqueID,AutoStart,Latitude,Longitude,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth,CameraType0,CameraHost0,CameraPath0,Codec0,Username0,Password0
Lane01,lane01,true,35.8858,-84.2121,Rapiscan,192.0.2.10,1601,,,false,false,4.82,Axis,192.0.2.20,,H264,operator,replace-me
```

Une voie Aspect avec deux caméras :

```csv
Name,UniqueID,AutoStart,Latitude,Longitude,RPMConfigType,RPMHost,RPMPort,AspectAddressStart,AspectAddressEnd,EMLEnabled,EMLCollimated,LaneWidth,CameraType0,CameraHost0,CameraPath0,Codec0,Username0,Password0,CameraType1,CameraHost1,CameraPath1,Codec1,Username1,Password1
Lane02,lane02,true,35.8859,-84.2119,Aspect,192.0.2.11,502,1,32,,,,Sony,192.0.2.21,,,operator,replace-me,Custom,192.0.2.22:8554,/stream1,,operator,replace-me
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

![Menu contextuel de capteur](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/20-lane-context-menu.png)

Sélectionnez **Système de voie**.

![Sélecteur de type de module](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/21-module-picker.png)

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

![Champs généraux d'une voie](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/22-lane-general.png)

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
| Supprimer les données avec la voie | Activé par défaut. La suppression de la voie efface ses données. Désactivez-le si l'historique doit être conservé. |
| Informations de source de données | Métadonnées héritées facultatives, seulement si le modèle SensorML du site les exige. |

### 7.3 Onglet Emplacement fixe

Saisissez latitude, longitude et altitude facultative (hauteur au-dessus de l'ellipsoïde WGS 84, en mètres).

Si un plan valide existe, il apparaît dans le formulaire. Cliquez à l'emplacement précis de la voie : OSCAR calcule latitude et longitude depuis les limites du plan. Vérifiez les valeurs avant d'enregistrer. Vous pouvez aussi les saisir directement.

![Choisir l'emplacement sur le plan](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/23-lane-location.png)

### 7.4 Onglet Orientation fixe

L'orientation utilise le repère local Nord-Est-Bas :

- **Cap** (lacet) : rotation autour de Z, en degrés ;
- **Tangage** : rotation autour de Y ;
- **Roulis** : rotation autour de X.

Laissez-la non définie si le site ne l'utilise pas. Elle ne remplace pas l'emplacement et ne géoréférence pas le plan.

### 7.5 Onglet Options de la voie

Sélectionnez **Ajouter** sous **Configuration RPM initiale** et le bouton plus/**Ajouter** sous **Configuration initiale de caméra**.

![Options initiales vides](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/24-lane-options-empty.png)

Ces options génèrent les modules enfants à l'initialisation. Elles ne constituent pas une seconde copie dynamique de tous leurs réglages. Ensuite, inspectez et gérez les enfants RPM/caméra dans l'arborescence Capteurs.

#### Types de RPM

Tous exigent **Hôte distant** et **Port distant**. Options supplémentaires :

| Type | Champs supplémentaires |
| --- | --- |
| Aspect | **Rechercher l'appareil dans la plage — De/À**. Plage Modbus inclusive obligatoire, 1 à 32 par défaut. |
| Rapiscan | **Activer l'analyse EML** uniquement pour VM250/EML ; **Est collimaté** ; **Largeur de voie (m)**, 4.82 par défaut. |
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

![RPM et caméras configurés](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/25-lane-options-filled.png)

### 7.6 Terminer et vérifier

1. Vérifiez chaque onglet.
2. Ajoutez le module ou appliquez les modifications d'une voie existante.
3. Attendez l'initialisation de la voie et des enfants.
4. Développez la voie : confirmez un RPM, toutes les caméras et, pour RS350, le processus d'occupation.
5. Corrigez les enfants si nécessaire et confirmez l'état **Démarré**.
6. Sélectionnez **Enregistrer** globalement.

Consultez la documentation [Rapiscan](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-rapiscan), [Aspect](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-aspect), [RS-350](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-rs350) et [FFmpeg](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-driver-ffmpeg).

## 8. Utiliser OSCAR Viewer

Ouvrez `https://<hote-oscar>/`. Le tableau de bord doit afficher l'état des voies, la table des événements et la carte. Avec un plan configuré, il est l'image supérieure et l'étendue initiale correspond à ses limites. Les marqueurs OSCAR restent interactifs au-dessus ; une épingle dessinée dans la capture n'est qu'une partie de l'image.

![Tableau de bord OSCAR Viewer](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/18-viewer-dashboard.png)

### 8.1 Tableau de bord et file d'alarmes

1. Confirmez toutes les voies dans **État des voies**.
2. Vérifiez la position de chaque marqueur.
3. Utilisez les couches pour comparer le plan à OSM ou Esri.
4. Vérifiez que les nouvelles occupations arrivent et que heures, gamma, neutrons et état sont plausibles.
5. La table d'alarmes contient les occupations en alarme non encore adjugées. Sélectionnez une ligne pour ouvrir/fermer l'aperçu; l'état colore Gamma, Neutron ou Gamma et Neutron.
6. Vérifiez les onglets CPS/NSIGMA et chaque vidéo; les flèches changent de média.
7. Agrandissez ou double-cliquez la ligne pour ouvrir **Détails de l'événement**.

### 8.2 Adjudication rapide depuis le tableau de bord

![Adjudication dans le tableau de bord](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/26-dashboard-adjudication.png)

Utilisez-la lorsque graphiques et vidéo suffisent, sans preuve ni WebID:

1. Saisissez l'**ID véhicule** s'il est connu.
2. Choisissez un code **Adjuger** dans la liste groupée de 8.5.
3. Réglez **Inspection secondaire** sur **Aucune**, **Demandée** ou **Terminée**.
4. Ajoutez des **Notes** justifiant la décision.
5. Sélectionnez **Envoyer**. Le succès identifie l'occupation et retire l'alarme de la file. **Réinitialiser** efface le formulaire local sans envoi.

L'envoi ajoute un enregistrement sans modifier l'observation d'origine. Pour preuves, isotopes, QR, WebID ou historique, ouvrez les Détails.

### 8.3 Page Événements

![Liste des événements](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/27-event-list.png)

**Événements** est l'historique de tous les nœuds locaux et fédérés configurés, pas seulement des alarmes en attente. Il affiche voie/nœud, ID d'occupation, début/fin, maxima gamma/neutron, état et présence d'une adjudication.

- **Colonnes** masque/affiche les champs, **Filtres** ouvre les filtres serveur et **Densité** règle l'espacement.
- Début/fin acceptent **après** et **avant**. État accepte **Aucun**, **Gamma**, **Neutron**, **Gamma et Neutron**. Adjugé accepte **Oui/Non**. Un filtre revient à la première page.
- Résultats du plus récent, par pages de 15. Sélection = aperçu; double-clic ou **Détails** = page complète.
- Un nœud fédéré indisponible peut rendre lignes/comptes incomplets; vérifiez la connexion avant d'interpréter zéro.

### 8.4 Détails de l'événement

![Résumé, graphiques et vidéo de l'événement](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/28-event-details-summary.png)

La page peut contenir :

- voie, occupation, heures, alarme, vitesse et autres champs ;
- graphiques gamma, seuil, neutrons et RS-350 pris en charge ;
- vidéo enregistrée pendant l'événement ;
- données diverses et adjudications précédentes ;
- preuves téléversées ou lues par QR ;
- analyse Web ID et choix d'isotopes facultatifs ;
- ID véhicule, notes, inspection secondaire et code ; et
- **Exporter en PDF**.

**Retour** revient à la liste. **CPS/NSIGMA** choisit la présentation gamma prise en charge; les flèches parcourent les vidéos. **Exporter en PDF** ouvre l'impression navigateur de la page rendue; cela diffère des rapports serveur de la section 10.

Les données disponibles dépendent du détecteur, de la caméra, de la conservation, de l'ancienneté, des droits et de WebID. Un panneau vide ne prouve pas l'absence de données: vérifiez voie, flux, caméra et conservation.

#### Codes d'adjudication

| Groupe | Codes |
| --- | --- |
| Alarme réelle | 1 Contrebande trouvée ; 2 Autre |
| Alarme innocente | 3 Isotope médical ; 4 NORM ; 5 Expédition déclarée de matière radioactive |
| Fausse alarme | 6 Inspection physique négative ; 7 RIID/ASP indique uniquement le fond ; 8 Autre |
| Test/Maintenance | 9 Activité autorisée de test, maintenance ou formation |
| Sabotage/Défaut | 10 Activité non autorisée |
| Autre | 11 Autre |

Choisissez selon la procédure du site, jamais par commodité.

### 8.5 Adjudication complète et preuves

![Preuves et adjudication complète](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/29-event-details-adjudication.png)

- **Résultats d'analyse WebID** affiche heure, nom/type d'isotope, confiance, taux, texte/nombre d'isotopes, avertissements, khi carré, DRF, erreur et dose estimée; **Lire plus** développe le texte.
- **Adjudications consignées** affiche occupation, heure, utilisateur, code, commentaire, isotopes, chemins, inspection secondaire et ID véhicule. Un nouvel envoi ajoute une ligne sans réécrire les anciennes.
- **Collecte de preuves** et le formulaire créent la nouvelle décision.

#### Preuves

1. **Téléverser des fichiers** ajoute un ou plusieurs fichiers. Aucun filtre de type navigateur: respectez la politique du site.
2. Pour analyser un spectre, activez **WebID**, choisissez une DRF fournie par Sandia Full Spectrum, **Premier plan** ou **Arrière-plan**, puis éventuellement **Synthétiser l'arrière-plan** pour un premier plan. Une paire premier/arrière-plan est envoyée ensemble.
3. **Scanner QR** utilise la caméra pour un texte spectroscopique. Autorisez-la, scannez/révisez/supprimez les codes, réglez WebID/DRF/type, puis **Terminé**. Les captures deviennent des preuves texte.
4. **Téléverser vers WebID** traite les preuves WebID non encore envoyées et reste désactivé sans élément éligible. Accès au bucket et au service Full Spectrum requis.
5. Dans **Preuve WebID**, sélectionnez des résultats puis **Utiliser le résultat sélectionné** pour appliquer leurs isotopes. L'opérateur reste responsable.

Supprimer avant envoi retire seulement l'élément en attente. Après téléversement, suivez le chemin consigné et la politique de conservation; le retrait du formulaire ne garantit pas la suppression serveur.

#### Formulaire

1. Saisissez l'ID véhicule si connu et exactement un code.
2. Choisissez zéro ou plusieurs isotopes. **Inconnu** exclut les isotopes nommés: Neptunium, Plutonium, Uranium-233/235/238, Américium, Baryum, Bismuth, Californium, Césium-134/137, Cobalt-57/60, Europium-152, Iridium, Manganèse, Sélénium, Sodium, Strontium, Fluor, Gallium, Iode-123/131, Indium, Palladium, Technétium, Xénon, Potassium, Radium et Thorium.
3. Ajoutez les notes et choisissez inspection **Aucune**, **Demandée** ou **Terminée**.
4. Sélectionnez **Envoyer**, relisez la confirmation complète puis **Confirmer et envoyer**. Vérifiez le succès et la nouvelle ligne. En cas d'échec, conservez le formulaire et corrigez nœud/flux de commande/téléversement avant de réessayer.

## 9. Statistiques nationales

![Statistiques nationales](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/30-national-statistics.png)

Une ligne par nœud donne **ID nœud**, alarmes Gamma, Neutron, combinées, occupations, sabotage, défauts Gamma, défauts Neutron et défauts totaux. Colonnes/Filtres/Densité ajustent la grille.

1. Choisissez **Tout le temps**, **30 derniers jours**, **7 derniers jours**, **24 dernières heures** ou **Plage personnalisée**.
2. Pour une plage, saisissez début et fin; la fin ne peut précéder le début.
3. **Actualiser les statistiques** commande chaque flux de contrôle du Service OSCAR. Les dates explicites ne sont envoyées que pour Personnalisée.
4. Attendez et vérifiez chaque nœud. Zéro ne prouve pas qu'il a répondu; examinez toute erreur de connexion/commande.

Les plages prédéfinies sont mises en cache et rechargées après actualisation. Les résultats dépendent du calendrier, des données conservées, des horloges et du réseau de chaque nœud.

## 10. Génération de rapports

![Générateur de rapports](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/31-report-generator.png)

Le générateur crée un PDF dans le bucket `reports` du nœud et l'affiche à droite.

1. Choisissez **Nœud** et type:
   - **RDS Site**: alarmes/taux, EML supprimées/taux, défauts et capacité libre/utilisable/totale.
   - **Lane**: statistiques d'alarmes et défauts d'une ou plusieurs voies.
   - **Adjudication**: répartitions/pourcentages, isotopes et détails par voie.
   - **Event**: **Alarmes et occupations**, **Alarmes** ou **État de santé**; ce dernier inclut gamma haut/bas, neutron haut et sabotage.
2. Pour Lane/Adjudication, choisissez une ou plusieurs voies. **Tout sélectionner** bascule tout/aucun.
3. Choisissez **24 heures**, **7 jours**, **30 jours**, **Ce mois** ou **Plage personnalisée** avec deux dates valides.
4. **Générer** peut rester accepté/en attente; gardez la page ouverte. Vérifiez le PDF et utilisez zoom, recherche, téléchargement ou impression. Le formulaire se réinitialise, pas l'aperçu.

Le serveur réessaie jusqu'à trois fois. Le nom encode nœud/type/début/fin; une demande identique peut réutiliser le fichier. Seules les données conservées sont rapportées.

## 11. Fédération de nœuds

![Fédération de nœuds](https://raw.githubusercontent.com/Botts-Innovative-Research/osh-oakridge-modules/main/docs/oscar-operator-manual/images/32-node-federation.png)

**Nœuds** interroge plusieurs serveurs. Le nœud local dérivé du navigateur est par défaut et non supprimable; les distants peuvent être ajoutés, modifiés ou retirés.

1. Saisissez un **Nom** unique et une **Adresse** sans schéma.
2. Indiquez le **Port** (initial `8282`) et l'**Endpoint Connected Systems API** (défaut `/api`, ajouté à `/sensorhub`).
3. Saisissez utilisateur/mot de passe de lecture.
4. Activez **Connexion sécurisée** pour HTTPS/WSS/MQTTS avec certificat/nom valides.
5. Laissez **Nœud Basic uniquement** décoché pour une session: les identifiants créent un cookie HttpOnly opaque puis le mot de passe est supprimé. Cochez seulement sans connexion de session: secrets en mémoire jusqu'au rechargement/fermeture.
6. **Ajouter/Enregistrer** teste d'abord l'endpoint complet; inaccessible/non autorisé n'est pas sauvegardé. **Annuler** abandonne.

Nom, réseau, TLS/mode et carte sont enregistrés, jamais utilisateur/mot de passe; les anciennes entrées sont nettoyées. Les noms et couples adresse/port en doublon sont refusés. **Supprimer** retire seulement la configuration de ce navigateur, pas le serveur ni ses données.

Vérifiez voies/événements distants, ligne Nationale, rapport et événement avec média pour tester API, commandes, bucket et médias. Après rechargement, vérifiez la session et ressaisissez les secrets Basic. Le distant doit autoriser origine/CORS, chemins, WebSocket/MQTT, droits et confiance TLS.

## 12. Conservation des données et stockage

Avec un **ID de base de données** explicite, le service démarre :

- chaque heure, la suppression des états de connexion de plus d'une heure et des mesures volumineuses hors des fenêtres d'occupation tamponnées ;
- à minuit UTC, l'export du `dailyFile` de la veille pour chaque voie dans `dailyfiles`, puis la suppression des observations exportées.

S'il n'existe aucune fenêtre d'occupation, la purge hors occupation est ignorée afin d'éviter une perte involontaire. Une marge récente est conservée et un tampon de cinq secondes entoure chaque occupation.

Le service de compartiments gère plans, CSV, vidéos, rapports et exports. Les données de base et les fichiers nécessitent des sauvegardes distinctes. Conservation par âge et par pression sont indépendantes : la première réduit/supprime les vidéos d'occupation ; la seconde réagit au disque et supprime actuellement uniquement les objets éligibles de `videos`.

Avant de modifier conservation, base, chemin ou **Supprimer les données avec la voie**, confirmez les obligations de preuve et de sauvegarde.

## 13. Liste de validation

### Certificat et accès

- [ ] Le bon hôte s'ouvre sans avertissement et l'empreinte correspond.
- [ ] Le compte administrateur fonctionne sans mot de passe partagé/par défaut.
- [ ] La langue affiche les contrôles et l'aide correspondants.

### Service OSCAR

- [ ] ID de nœud unique ; base, Web ID, statistiques et démarrage automatique vérifiés.
- [ ] Conservation vidéo et pression examinées avant toute purge.
- [ ] Services de compartiments et OSCAR démarrés.

### Plan

- [ ] Image nord en haut, bien recadrée, PNG/JPG/JPEG.
- [ ] Limites valides, ordonnées et non nulles.
- [ ] Téléversement réussi, modifications appliquées et configuration enregistrée.
- [ ] Viewer utilise l'étendue du plan et les marqueurs sont alignés.

### Voies et appareils

- [ ] Noms d'au plus 12 caractères et IDs uniques.
- [ ] Type/hôte/port/options RPM corrects.
- [ ] Type, hôte/port, identifiants, chemin/codec et tampon des caméras corrects.
- [ ] Enfants attendus démarrés ; une panne matérielle ne bloque pas la validation des autres voies.
- [ ] Enregistrement global après import ou création.

### Viewer

- [ ] États et marqueurs se mettent à jour.
- [ ] Une occupation de test affiche des valeurs plausibles.
- [ ] Vidéo en direct et enregistrée fonctionne, y compris après actualisation.
- [ ] Détails s'ouvre sans exception côté client.
- [ ] Une adjudication contrôlée peut être vérifiée et envoyée.
- [ ] Les filtres Événements, l'actualisation Nationale et les rapports requis ont été testés.
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
| RPM ne démarre pas | Testez hôte, port, pare-feu, plage Aspect et disponibilité ; consultez l'erreur du pilote enfant. |
| Caméra ne démarre pas | Testez RTSP/identifiants, retirez `rtsp://` de l'hôte, évitez un port en double, vérifiez codec Axis ou chemin Custom depuis l'hôte OSCAR. |
| Vidéo perdue après actualisation | Vérifiez que la caméra et HLS restent démarrés et consultez les journaux. Actualiser ne doit pas imposer de recréer la voie. |
| Événement sans média | Vérifiez voie disponible, flux sur l'intervalle, conservation et droits. |
| Échec d'adjudication | Choisissez un code; vérifiez nœud/voie, flux de commande et téléversement. Ne renvoyez pas avant de connaître le premier résultat. |
| WebID sans DRF/résultat | Vérifiez Full Spectrum, DRF, premier/arrière-plan et colonnes avertissement/erreur. La décision reste humaine. |
| Nationale vide/zéro | Actualisez la plage et vérifiez commande statistique, données conservées et authentification du nœud. |
| Rapport absent | Complétez nœud/type/plage et voie/type d'événement; vérifiez dates, commande et bucket `reports`. Une demande identique peut réutiliser un fichier. |
| Nœud distant refusé après rechargement | La session exige un cookie valide; Basic exige de ressaisir les secrets. Vérifiez TLS, CORS, chemins, port et droits. |
| Modifications perdues au redémarrage | Appliquez le formulaire, puis utilisez Enregistrer globalement. |

Pour l'assistance, relevez version OSCAR, navigateur, voie/occupation, heure et fuseau, états et journaux nettoyés. Retirez mots de passe, jetons, clés privées et preuves sensibles.

## 15. Documentation associée

- [Démarrage rapide OSCAR](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/dist/release/QUICKSTART.md)
- [Guide de déploiement](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/dist/release/DEPLOYMENT.md)
- [Guide du système de traduction](https://github.com/Botts-Innovative-Research/osh-oakridge-buildnode/blob/main/docs/TRANSLATION_SYSTEM.md)
- [Module Système de voie](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/sensors/sensorhub-system-lane)
- [Module de service OSCAR](https://github.com/Botts-Innovative-Research/osh-oakridge-modules/tree/main/services/sensorhub-service-oscar)

---

Référence du document : comportement du code OSCAR 3.8.3, revu le 2026-09-21. Si une version ultérieure change les champs ou procédures, mettez à jour ensemble le manuel canonique anglais et les trois traductions.
