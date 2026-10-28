# Gestion de fermes de tomates cerises

Application de bureau Java (JavaFX + PostgreSQL) de gestion de fermes de
tomates cerises. Projet du module Programmation orientée objet avancée,
GI S5 2026/2027.

## Équipe
Yassir Hikou (chef de projet), Abdellatif Kharbat, Brahim Ait Oubraim,
Mohamed Amine Ait Maait, Jamel Boud.

## Livrables de la phase 1 (conception)
- [Cahier des charges](docs/cahier-des-charges.pdf)
- [Diagramme de classes](docs/diagrammes/classes.png)
- [Diagramme de cas d'utilisation](docs/diagrammes/cas-utilisation.png)
- Diagrammes de séquence : [connexion](docs/diagrammes/sequence-connexion.png),
  [livraison](docs/diagrammes/sequence-livraison.png),
  [paiement](docs/diagrammes/sequence-paiement.png)
- [Architecture](docs/diagrammes/architecture.png)
- [Schéma de la base de données](docs/base-de-donnees/schema-base.pdf)
- Maquettes : [administrateur](docs/maquettes/administrateur.pdf),
  [responsable](docs/maquettes/responsable.pdf),
  [agent RH](docs/maquettes/agent-rh.pdf)
- [Démarche](docs/demarche.pdf) et [synthèse](docs/synthese.pdf)

## Technologies
Java 17, JavaFX, PostgreSQL (JDBC), Maven, BCrypt, OpenPDF, JUnit 5, Javadoc.

## Configuration (phase de réalisation)
Copier `config.properties.example` en `config.properties` et renseigner
les identifiants PostgreSQL. Le fichier `config.properties` n'est jamais
envoyé sur GitHub.