# Portique de détection radiologique Aspect

Le pilote Aspect connecte un portique de détection radiologique (RPM) Aspect à OpenSensorHub.

## Configuration

- Donnez un nom clair au module et saisissez le numéro de série du capteur.
- Dans **Paramètres de communication**, ajoutez le **Pilote de communication Modbus TCP**.
- Saisissez l'hôte, le port et la plage d'adresses Modbus du RPM. La plage habituelle va de 1 à 32.
- Configurez les délais, les nouvelles tentatives et la vérification d'accessibilité selon les besoins.
- Saisissez l'ID de la voie et, si elles sont connues, la position et l'orientation du capteur.
- Activez **Démarrage automatique** pour démarrer le pilote avec le nœud OpenSensorHub.
