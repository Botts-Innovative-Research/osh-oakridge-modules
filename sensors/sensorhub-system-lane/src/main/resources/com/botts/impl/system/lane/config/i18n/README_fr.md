# Système de capteurs de voie

Le Système de voie crée un système parent pour un portique de détection radiologique (RPM) et une ou plusieurs caméras de voie.

## Configuration générale

- Donnez à la voie un nom de module et un ID uniques.
- Saisissez sa latitude et sa longitude fixes lorsqu'elles sont connues.
- Activez **Supprimer les données avec la voie** uniquement si la suppression d'une voie doit également supprimer ses enregistrements stockés.

## Options de la voie

- Dans **Configuration RPM initiale**, sélectionnez Aspect, Rapiscan ou RS-350 et saisissez l'hôte et le port du périphérique. Aspect nécessite également une plage d'adresses Modbus.
- Dans **Configuration initiale des caméras**, ajoutez chaque caméra Sony, Axis ou personnalisée et saisissez son hôte, son nom d'utilisateur et son mot de passe si nécessaire.
- Pour les caméras Axis, sélectionnez le codec du flux. Pour les caméras personnalisées, saisissez le chemin qui suit l'hôte et le port de la caméra.
- Vérifiez tous les modules enfants générés avant d'enregistrer la voie.
