# Détecteur de proximité inductif

Ce pilote Raspberry Pi surveille un détecteur de proximité inductif au moyen des entrées GPIO.

## Câblage et configuration

- Saisissez un numéro de série unique pour le détecteur.
- Définissez **Broche normalement fermée** sur le GPIO relié au contact NC blanc lorsqu'il est câblé.
- Définissez **Broche normalement ouverte** sur le GPIO relié au contact NO noir lorsqu'il est câblé.
- Sélectionnez **Non défini** pour un contact non connecté. Un seul contact ou les deux peuvent être utilisés.
- Vérifiez la numérotation GPIO et la compatibilité électrique avant de démarrer le pilote.
- Activez **Démarrage automatique** pour surveiller le détecteur au démarrage du nœud.
