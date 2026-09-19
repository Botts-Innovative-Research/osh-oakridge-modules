# Pilote vidéo FFmpeg

Ce pilote publie une vidéo à partir d'un fichier ou d'un flux réseau compatible avec FFmpeg.

## Connexion

- Définissez **Chemin du fichier** pour une vidéo enregistrée ou **Chaîne de connexion** pour un flux en direct.
- Pour la lecture d'un fichier, configurez la fréquence d'images, la taille du tampon et la lecture en boucle facultative.
- Pour un flux réseau, sélectionnez TCP si nécessaire ; sinon, le pilote utilise UDP.
- Activez **Ignorer les horodatages** lorsque la source ne contient pas d'horodatages utilisables.

## Sorties

- Activez **HLS** pour publier une sortie de chemin de fichier HLS.
- Activez **Images vidéo** pour publier les images vidéo binaires.
- Configurez l'ID du flux, la position de la source, le délai et la reconnexion selon les besoins.
