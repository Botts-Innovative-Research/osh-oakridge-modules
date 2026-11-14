# Système de capteurs de voie

Le **Système de capteurs de voie** crée un système parent regroupant un portique de détection de rayonnement (RPM), son processus d'occupation et jusqu'à quatre caméras de voie.

## Termes expliqués simplement

| Terme | Signification |
| --- | --- |
| **Clé de vue opérationnelle (`Operational View Key`)** | Étiquette facultative (par exemple `north-gate` ou `cargo-inbound`) qui regroupe cette voie dans un espace de travail opérateur nommé dans le Visualiseur OSCAR. |
| **Apprentissage automatique amélioré (`EML`, Enhanced Machine Learning)** | Algorithme Rapiscan facultatif comparant les comptages gamma de basse, moyenne et haute énergie pour distinguer la radioactivité naturelle du fret (**NORM**, auto-adjugée en `Code 9999: EML Non-Threat`) des sources menaçantes. |
| **OCR des véhicules (`ocrConfig`)** | Lecture optique facultative par voie (`Activer l'OCR des véhicules = false` par défaut) qui analyse de façon asynchrone les numéros de conteneur ISO 6346 (avec chiffre de contrôle) et les plaques d'immatriculation selon les ressources CPU et mémoire disponibles, publie des suggestions `vehicleOcr` (`PARQUET_PRIMARY`), des vignettes bornées (`files/ocr/crops`, plafond FIFO de `2 000` fichiers) et préremplit l'**ID véhicule** lors de l'adjudication. Lorsque `Alarmes uniquement` est décoché, les séquences hors alarme sont analysées de manière transitoire puis supprimées immédiatement tout en conservant `occupancy.videoPaths` vide (`[]`). |
| **Plage d'adresses Modbus** | Plage de registres (`De` et `À`) utilisée par les contrôleurs Aspect sur TCP/IP. |

## Configuration générale

- **Nom du module** et **ID unique** : Attribuez à la voie un nom unique et un identifiant unique.
- **Clé de vue opérationnelle** : Clé facultative associant la voie à une vue opérationnelle du Service OSCAR.
- **OCR des véhicules** : Réglages facultatifs par voie (`Activer l'OCR des véhicules`, `Alarmes uniquement`, `Répertoire des modèles`, `Lire les numéros de conteneur`, `Lire les plaques d'immatriculation`, `UID des caméras`, `Images par séquence`, `Confiance minimale conteneur`, `Confiance minimale plaque`, `Fraction supérieure ignorée`, `Fraction inférieure ignorée`). Désactivé par défaut sans surcharge lorsqu'il est inactif.
- **Emplacement** : Saisissez la latitude et la longitude fixes de la voie.
- **Supprimer les données lors du retrait de la voie** : Activez cette option **uniquement** si la suppression de la voie doit aussi effacer ses enregistrements historiques.

## Options de voie

- **Configuration initiale du RPM** :
  - Sélectionnez **Rapiscan**, **Aspect** ou **RS-350** et saisissez l'hôte et le port TCP.
  - Pour **Aspect**, renseignez la plage d'adresses Modbus.
  - Pour **Rapiscan**, configurez les options **EML** lorsqu'un module d'extension `.jar` EML est chargé sur le nœud.
- **Configuration initiale des caméras** :
  - Ajoutez jusqu'à **4** caméras **Sony**, **Axis** ou **Custom** par voie.
  - Pour **Axis**, sélectionnez le codec de flux (`H.264`). Pour **Custom**, saisissez le chemin du flux après l'hôte et le port.
- Cliquez sur **Appliquer** puis **Enregistrer** dans la barre d'outils supérieure pour générer et démarrer les modules enfants.
