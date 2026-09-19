# Service de communication Z-Wave

Ce service gère la connexion série à un contrôleur Z-Wave et partage les nœuds détectés avec les pilotes de capteur.

## Configuration

- Sélectionnez le port série USB connecté au contrôleur.
- Définissez le débit en bauds du contrôleur ; la valeur par défaut est 115200.
- Activez **Contrôleur principal** lorsque ce périphérique est le contrôleur Z-Wave principal.
- Saisissez l'ID du contrôleur et sa position physique.
- **Pilotes de capteur abonnés** répertorie les nœuds Z-Wave actuellement associés au service.
- Démarrez ce service avant les pilotes de capteur qui en dépendent.
