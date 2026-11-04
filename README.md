# Gestion de fermes de tomates cerises

Application de bureau Java (JavaFX + PostgreSQL) de gestion de plusieurs fermes
de tomates cerises : production, main-d'œuvre, livraisons aux stations de
conditionnement et résultats financiers.

Projet du module **Programmation orientée objet avancée**, GI S5 2026/2027.
Encadrant : Youssef ES-SAADY.

## Équipe

| Nom | Rôle | GitHub |
| --- | --- | --- |
| Yassir Hikou | Chef de projet | [yassierr](https://github.com/yassierr) |
| Abdellatif Kharbat | Membre | [abdellatif-kharbat](https://github.com/abdellatif-kharbat) |
| Brahim Ait Oubraim | Membre | [Oubraim-code](https://github.com/Oubraim-code) |
| Mohamed Amine Ait Maait | Membre | [MedoAmine1](https://github.com/MedoAmine1) |
| Jamel Boud | Membre | [JamelBoud](https://github.com/JamelBoud) |

## Livrables de la phase 1 (conception)

| Livrable | Fichier |
| --- | --- |
| Cahier des charges | [docs/cahier-des-charges.pdf](docs/cahier-des-charges.pdf) |
| Diagramme de cas d'utilisation | [docs/diagrammes/cas-utilisation.pdf](docs/diagrammes/cas-utilisation.pdf) |
| Diagramme de classes | [docs/diagrammes/classes.pdf](docs/diagrammes/classes.pdf) |
| Diagramme de séquence : connexion | [docs/diagrammes/sequence-connexion.pdf](docs/diagrammes/sequence-connexion.pdf) |
| Diagramme de séquence : livraison | [docs/diagrammes/sequence-livraison.pdf](docs/diagrammes/sequence-livraison.pdf) |
| Diagramme de séquence : paiement des ouvriers | [docs/diagrammes/sequence-paiement.pdf](docs/diagrammes/sequence-paiement.pdf) |
| Schéma de la base de données | [docs/base-de-donnees/schema-base.pdf](docs/base-de-donnees/schema-base.pdf) |
| Maquettes : administrateur | [docs/maquettes/administrateur.pdf](docs/maquettes/administrateur.pdf) |
| Maquettes : responsable de ferme | [docs/maquettes/responsable.pdf](docs/maquettes/responsable.pdf) |
| Maquettes : agent RH | [docs/maquettes/agent-rh.pdf](docs/maquettes/agent-rh.pdf) |
| Maquette : connexion | [docs/maquettes/login.jpeg](docs/maquettes/login.jpeg) |
| Démarche suivie, outils, fonctionnalités | [docs/demarche.pdf](docs/demarche.pdf) |
| Synthèse des travaux et choix de conception | [docs/synthese.pdf](docs/synthese.pdf) |

Les sources des diagrammes (PlantUML) et du schéma (DBML) sont dans
[docs/diagrammes/sources](docs/diagrammes/sources) et
[docs/base-de-donnees](docs/base-de-donnees).

## Structure du dépôt

```
gestion-fermes/
├── README.md
├── .gitignore
├── config.properties.example
├── database/                  scripts SQL (phase de réalisation)
└── docs/
    ├── cahier-des-charges.pdf
    ├── demarche.pdf
    ├── synthese.pdf
    ├── base-de-donnees/       schéma PDF et source DBML
    ├── diagrammes/            diagrammes PDF
    │   └── sources/           sources PlantUML
    └── maquettes/             maquettes de l'interface
```

## Technologies

Java 25, JavaFX, PostgreSQL (JDBC), Maven, BCrypt, OpenPDF, JUnit 5, Javadoc.

## Configuration (phase de réalisation)

Copier `config.properties.example` en `config.properties` et renseigner les
identifiants PostgreSQL. Le fichier `config.properties` n'est jamais envoyé sur
GitHub.
