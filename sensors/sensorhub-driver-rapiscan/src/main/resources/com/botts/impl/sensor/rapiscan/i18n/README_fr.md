# Portique de détection radiologique Rapiscan

Le pilote Rapiscan se connecte à un RPM par TCP et publie les données gamma, neutroniques, d'occupation et les données associées.

## Configuration

- Saisissez un numéro de série unique et l'ID de la voie.
- Dans **Paramètres de communication**, ajoutez un fournisseur TCP et saisissez l'hôte et le port du RPM.
- Configurez les délais, les nouvelles tentatives et la vérification d'accessibilité.
- Saisissez la position du capteur lorsqu'elle n'est pas héritée d'un système de voie.
- Utilisez **Configuration gamma du matériel RPM** et **Configuration neutronique du matériel RPM** pour les paramètres matériels gamma et neutroniques ; lorsque le RPM publie ses messages quotidiens de configuration (`SG1`–`SG3` et `SN1`–`SN2`) dans le flux de fichiers quotidiens, le pilote met automatiquement à jour ces paramètres en mémoire et regroupe tout changement dans une mise à jour asynchrone de `config.json` sans bloquer le flux en temps réel.
- Activez **Analyse EML** uniquement pour une voie EML VM250 après avoir téléversé l'archive externe du module EML dans **Services > Module de service OSCAR > Chemin du module EML**, puis saisissez son état de collimation et sa largeur. Les alarmes sans menace EML (`RELEASE`) sont masquées du tableau de bord et adjugées automatiquement comme **Sans menace EML** (code `9999`) avec métadonnées interrogeables.
- Activez **Démarrage automatique** pour démarrer le pilote avec le nœud.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#07, 11.8s) : Charger le module externe EML VM-250 et activer l’EML par voie</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/07-admin-eml-plugin-setup.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/07-admin-eml-plugin-setup.vtt" srclang="en" label="English" default>
  </video>
</details>
