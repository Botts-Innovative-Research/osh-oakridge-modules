# Service de journal durable des fichiers quotidiens OSCAR

Le **Service de journal durable des fichiers quotidiens OSCAR** enregistre les messages bruts des portiques de détection de rayonnement (RPM) dans un journal binaire récupérable après incident et publie des fichiers CSV quotidiens vérifiés (`dailyfiles/`) accompagnés de manifestes d'intégrité cryptographique (`SHA-256`).

## Termes expliqués simplement

| Terme | Signification |
| --- | --- |
| **Fichier quotidien (Daily File)** | Un journal CSV standard contenant une journée civile de messages RPM bruts pour une voie : bruit de fond (`GB`, `NB`), début/fin d'occupation (`GS`, `GX`), comptages gamma/neutrons (`GC`, `NC`) et alarmes (`GA`, `NA`, `NS`). |
| **Segment de journal (Journal Segment)** | Un fichier binaire en ajout seul qui enregistre chaque ligne RPM entrante avec une somme de contrôle `CRC32C` avant la publication CSV. En cas de coupure de courant, OSCAR relit ce journal au démarrage afin qu'aucun message ne soit perdu. |
| **Manifeste et grand livre (Manifest & Ledger)** | Fichiers de vérification écrits à la clôture d'une journée. Ils enregistrent le nombre exact de lignes, la taille en octets et l'empreinte `SHA-256` du fichier CSV publié. |
| **Fsync** | Écriture forcée des mémoires tampons sur le disque physique afin que les messages RPM acceptés survivent à un redémarrage inattendu. |

## Référence de configuration

- **Racine de stockage (`Storage Root`)** : Dossier de base contenant le journal binaire privé et les fichiers CSV quotidiens publiés.
- **Répertoire du journal (`Journal Directory`)** : Sous-dossier contenant les segments binaires actifs et scellés ainsi que les grands livres quotidiens.
- **Répertoire de publication (`Publication Directory`)** : Sous-dossier recevant les fichiers CSV quotidiens et leurs fichiers `.manifest.json`.
- **Fuseau horaire du fichier quotidien (`Daily File Time Zone`)** : Fuseau horaire IANA (par exemple `UTC` ou `America/New_York`) déterminant le changement de jour à minuit.
- **Mode d'observation des fichiers quotidiens (`Daily File Observation Mode`)** :
  - `NONE` *(recommandé avec `PARQUET_PRIMARY`)* : Écrit directement le journal binaire et les fichiers CSV sans dupliquer les observations `dailyFile` dans la base de données.
  - `LATEST` / `FULL` : Publie des observations de compatibilité dans la base de données et nettoie automatiquement les journées terminées et validées par manifeste.
- **Intervalle Fsync (ms)**, **Enregistrements max. non forcés** et **Octets max. non forcés** : Déterminent la fréquence de synchronisation sur le disque physique.
- **Octets max. par segment** et **Âge max. d'un segment (secondes)** : Déterminent la rotation des segments actifs.
- **Octets max. par enregistrement** : Taille maximale autorisée pour un message RPM brut en octets UTF-8.
- **Rétention du journal scellé (jours)** : Nombre de jours de conservation des segments binaires scellés après clôture et publication vérifiée (`0` conserve indéfiniment).
- **Exiger une sauvegarde avant suppression du journal scellé** : Lorsque cette option est activée, une confirmation de sauvegarde externe vérifiée est requise avant de supprimer une journée scellée expirée.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#13, 19.4s) : Examiner les 5 images conservées, le fichier quotidien et adjuger</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/13-event-details-adjudication-and-daily-file.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/13-event-details-adjudication-and-daily-file.vtt" srclang="en" label="English" default>
  </video>
</details>

<details class="oscar-video-guide">
  <summary>Guide vidéo (#03, 11.4s) : Vérifier les archives froides Parquet et la sauvegarde instantanée unique</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/03-archive-and-snapshot-backup.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/03-archive-and-snapshot-backup.vtt" srclang="en" label="English" default>
  </video>
</details>
