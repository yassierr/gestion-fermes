-- ============================================================
-- Gestion de fermes de tomates cerises - schéma PostgreSQL
-- 12 tables, 5 types énumérés.
-- Utilisation : psql -U postgres -d gestion_fermes -f schema.sql
-- (créer d'abord la base : CREATE DATABASE gestion_fermes;)
-- ============================================================

DROP TABLE IF EXISTS pointage, ligne_paie, recu_paie, ouvrier, ligne_vente,
                     vente, client, depense, recolte, serre, utilisateur, ferme CASCADE;
DROP TYPE  IF EXISTS role_utilisateur, statut_vente, statut_pointage,
                     type_client, categorie_depense CASCADE;

-- ---------- Types énumérés ----------
CREATE TYPE role_utilisateur  AS ENUM ('ADMIN', 'RESPONSABLE', 'AGENT_RH');
CREATE TYPE statut_vente      AS ENUM ('LIVREE', 'PAYEE', 'ANNULEE');
CREATE TYPE statut_pointage   AS ENUM ('PRESENT', 'ABSENT');
CREATE TYPE type_client       AS ENUM ('STATION_CONDITIONNEMENT', 'EXPORTATEUR', 'GROSSISTE', 'MARCHE_GROS');
CREATE TYPE categorie_depense AS ENUM ('PLANTS', 'ENGRAIS', 'EAU_ENERGIE', 'EMBALLAGE', 'AUTRE');

-- ---------- Fermes et utilisateurs ----------
CREATE TABLE ferme (
    id            SERIAL PRIMARY KEY,
    nom           VARCHAR(100) NOT NULL UNIQUE,
    localisation  VARCHAR(150) NOT NULL
);

CREATE TABLE utilisateur (
    id                 SERIAL PRIMARY KEY,
    nom_complet        VARCHAR(100) NOT NULL,
    login              VARCHAR(50)  NOT NULL UNIQUE,
    mot_de_passe_hash  VARCHAR(100) NOT NULL,
    role               role_utilisateur NOT NULL,
    actif              BOOLEAN NOT NULL DEFAULT TRUE,
    id_ferme           INT REFERENCES ferme(id),
    -- l'administrateur n'a pas de ferme ; les autres rôles en ont une
    CONSTRAINT ck_utilisateur_ferme CHECK (
        (role = 'ADMIN' AND id_ferme IS NULL) OR (role <> 'ADMIN' AND id_ferme IS NOT NULL))
);
-- une ferme a au plus un responsable et un agent RH
CREATE UNIQUE INDEX ux_utilisateur_ferme_role ON utilisateur (id_ferme, role) WHERE role <> 'ADMIN';

-- ---------- Production ----------
CREATE TABLE serre (
    id          SERIAL PRIMARY KEY,
    nom         VARCHAR(100) NOT NULL,
    surface_m2  NUMERIC(10,2) NOT NULL CHECK (surface_m2 > 0),
    structure   VARCHAR(50),
    irrigation  VARCHAR(50),
    variete     VARCHAR(50),
    id_ferme    INT NOT NULL REFERENCES ferme(id),
    CONSTRAINT ux_serre_ferme_nom UNIQUE (id_ferme, nom)
);

CREATE TABLE recolte (
    id          SERIAL PRIMARY KEY,
    date_heure  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    poids_kg    NUMERIC(10,2) NOT NULL CHECK (poids_kg > 0),
    variete     VARCHAR(50) NOT NULL,
    id_ferme    INT NOT NULL REFERENCES ferme(id)
);

CREATE TABLE depense (
    id            SERIAL PRIMARY KEY,
    date_depense  DATE NOT NULL DEFAULT CURRENT_DATE,
    categorie     categorie_depense NOT NULL,
    montant       NUMERIC(12,2) NOT NULL CHECK (montant > 0),
    description   VARCHAR(255),
    id_ferme      INT NOT NULL REFERENCES ferme(id)
);

-- ---------- Clients et livraisons ----------
CREATE TABLE client (
    id         SERIAL PRIMARY KEY,
    code       VARCHAR(20)  NOT NULL UNIQUE,
    nom        VARCHAR(100) NOT NULL,
    type       type_client  NOT NULL,
    telephone  VARCHAR(30),
    ville      VARCHAR(80),
    actif      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE vente (
    id             SERIAL PRIMARY KEY,
    numero_bon     VARCHAR(20) NOT NULL UNIQUE,
    date_livraison DATE NOT NULL DEFAULT CURRENT_DATE,
    statut         statut_vente NOT NULL DEFAULT 'LIVREE',
    date_paiement  DATE,
    id_client      INT NOT NULL REFERENCES client(id),
    id_admin       INT NOT NULL REFERENCES utilisateur(id),
    CONSTRAINT ck_vente_paiement CHECK (statut <> 'PAYEE' OR date_paiement IS NOT NULL)
);

CREATE TABLE ligne_vente (
    id           SERIAL PRIMARY KEY,
    quantite_kg  NUMERIC(10,2) NOT NULL CHECK (quantite_kg > 0),
    prix_kg      NUMERIC(10,2) NOT NULL CHECK (prix_kg > 0),
    id_vente     INT NOT NULL REFERENCES vente(id) ON DELETE CASCADE,
    id_ferme     INT NOT NULL REFERENCES ferme(id)
);

-- ---------- Ouvriers, paie et pointage ----------
CREATE TABLE ouvrier (
    id                  SERIAL PRIMARY KEY,
    matricule           VARCHAR(20)  NOT NULL UNIQUE,
    cin                 VARCHAR(20)  NOT NULL UNIQUE,
    nom_prenom          VARCHAR(100) NOT NULL,
    telephone           VARCHAR(30),
    adresse             VARCHAR(150),
    poste               VARCHAR(50),
    salaire_journalier  NUMERIC(8,2) NOT NULL DEFAULT 150 CHECK (salaire_journalier >= 0),
    actif               BOOLEAN NOT NULL DEFAULT TRUE,
    id_serre            INT REFERENCES serre(id) ON DELETE SET NULL,
    id_ferme            INT NOT NULL REFERENCES ferme(id)
);

CREATE TABLE recu_paie (
    id            SERIAL PRIMARY KEY,
    numero        VARCHAR(20) NOT NULL UNIQUE,
    date_paiement DATE NOT NULL DEFAULT CURRENT_DATE,
    periode_debut DATE NOT NULL,
    periode_fin   DATE NOT NULL,
    total         NUMERIC(12,2) NOT NULL CHECK (total >= 0),
    id_ferme      INT NOT NULL REFERENCES ferme(id),
    id_agent_rh   INT NOT NULL REFERENCES utilisateur(id),
    CONSTRAINT ck_recu_periode CHECK (periode_fin >= periode_debut)
);

CREATE TABLE ligne_paie (
    id                  SERIAL PRIMARY KEY,
    jours_payes         INT NOT NULL CHECK (jours_payes > 0),
    salaire_journalier  NUMERIC(8,2) NOT NULL CHECK (salaire_journalier >= 0),
    montant             NUMERIC(12,2) NOT NULL CHECK (montant >= 0),
    id_recu             INT NOT NULL REFERENCES recu_paie(id) ON DELETE CASCADE,
    id_ouvrier          INT NOT NULL REFERENCES ouvrier(id),
    CONSTRAINT ux_ligne_paie UNIQUE (id_recu, id_ouvrier)
);

CREATE TABLE pointage (
    id             SERIAL PRIMARY KEY,
    date_pointage  DATE NOT NULL,
    statut         statut_pointage NOT NULL,
    id_ouvrier     INT NOT NULL REFERENCES ouvrier(id),
    id_recu        INT REFERENCES recu_paie(id),   -- vide tant que le jour n'est pas payé
    CONSTRAINT ux_pointage_jour UNIQUE (id_ouvrier, date_pointage)
);

-- ---------- Index sur les clés étrangères ----------
CREATE INDEX ix_utilisateur_ferme   ON utilisateur (id_ferme);
CREATE INDEX ix_recolte_ferme_date  ON recolte (id_ferme, date_heure);
CREATE INDEX ix_depense_ferme_date  ON depense (id_ferme, date_depense);
CREATE INDEX ix_vente_client        ON vente (id_client);
CREATE INDEX ix_ligne_vente_vente   ON ligne_vente (id_vente);
CREATE INDEX ix_ligne_vente_ferme   ON ligne_vente (id_ferme);
CREATE INDEX ix_ouvrier_ferme       ON ouvrier (id_ferme);
CREATE INDEX ix_pointage_recu       ON pointage (id_recu);
