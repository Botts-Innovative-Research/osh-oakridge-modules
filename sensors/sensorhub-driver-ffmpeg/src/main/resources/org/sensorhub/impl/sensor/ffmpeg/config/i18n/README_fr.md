# Pilote vidéo FFmpeg

Le **Pilote vidéo FFmpeg** se connecte au flux d'une caméra IP (RTSP/HTTP) ou à un fichier vidéo, diffuse la vidéo en direct à faible latence vers le Visualiseur OSCAR et extrait les images JPEG de preuve conservées (`frame-01.jpg` à `frame-05.jpg`) lors d'une alarme d'occupation.

## Termes expliqués simplement

| Terme | Signification |
| --- | --- |
| **Image clé / Intervalle GOP (`1–2 s`)** | Une image vidéo complète de référence (`I-frame`). Configurez les caméras pour envoyer une image clé toutes les `1 à 2 secondes` (`GOP 30–60` à `30 ips`) afin d'assurer un démarrage instantané du direct et un découpage précis lors des alarmes. |
| **Médias conservés à 5 images** | Pour chaque occupation avec alarme, OSCAR extrait 5 images JPEG réparties uniformément. Après 30 jours (`retainedSecondaryFramesDays = 30`), les images secondaires (`01`, `02`, `04`, `05`) sont purgées tandis que l'image centrale (`frame-03.jpg`) est conservée. |
| **Relais RTSP local (`mediamtx`)** | Lorsque le mode de diffusion en direct du Service OSCAR est réglé sur `RTSP_PROXY` ou `HYBRID`, OSCAR publie le flux H.264 vers le serveur local `mediamtx` (`127.0.0.1:8554`) afin que plusieurs opérateurs puissent visionner la caméra sans multiplier les connexions vers la caméra physique. |

## Connexion

- **Chaîne de connexion (`Connection String`)** / **Chemin du fichier (`File Path`)** : Renseignez la chaîne de connexion pour un flux RTSP/HTTP en direct ou le chemin d'un fichier vidéo enregistré.
- **Utiliser TCP (`Use TCP`)** : Active le transport TCP pour les flux RTSP lorsque le réseau l'exige.
- **Ignorer les horodatages des données (`Ignore Data Timestamps`)** : Activez cette option lorsque l'horloge de la caméra n'est pas synchronisée afin qu'OSCAR utilise l'horloge du serveur.

## Sorties

- **Images vidéo (`Video Frames`)** : Publie les images vidéo H.264 utilisées pour le direct et l'extraction des 5 images d'alarme.
- **HLS** : Publie une sortie HLS lorsque nécessaire.
- Configurez l'identifiant du flux, le délai de connexion et le comportement de reconnexion selon les besoins.

<details class="oscar-video-guide">
  <summary>Guide vidéo (#11, 12.0s) : Inspecter le flux caméra en direct et les courbes gamma/neutrons</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/11-live-lane-video-and-charts.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/11-live-lane-video-and-charts.vtt" srclang="en" label="English" default>
  </video>
</details>

<details class="oscar-video-guide">
  <summary>Guide vidéo (#13, 19.4s) : Examiner les 5 images conservées, le fichier quotidien et adjuger</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/13-event-details-adjudication-and-daily-file.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/13-event-details-adjudication-and-daily-file.vtt" srclang="en" label="English" default>
  </video>
</details>
