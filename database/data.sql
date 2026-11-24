-- ============================================================
-- Jeu de données de test - Gestion de fermes de tomates cerises
-- À exécuter APRÈS schema.sql :
--   psql -U postgres -d gestion_fermes -f schema.sql
--   psql -U postgres -d gestion_fermes -f data.sql
-- Rejouable : le script vide les tables et remet les séquences à 1.
-- Mot de passe de TOUS les utilisateurs de test : password123
-- (hash BCrypt ; adaptez-le si votre application utilise un autre algorithme)
-- ============================================================

BEGIN;

TRUNCATE TABLE pointage, ligne_paie, recu_paie, ouvrier, ligne_vente,
               vente, client, depense, recolte, serre, utilisateur, ferme
RESTART IDENTITY CASCADE;

-- ---------- Fermes ----------
INSERT INTO ferme (id, nom, localisation) VALUES
 (1, 'Ferme Al Baraka',   'Biougra'),
 (2, 'Ferme Souss Atlas', 'Ait Melloul'),
 (3, 'Ferme Chtouka',     'Ait Baha');

-- ---------- Utilisateurs ----------
-- ADMIN sans ferme ; RESPONSABLE et AGENT_RH : un seul de chaque par ferme
INSERT INTO utilisateur (id, nom_complet, login, mot_de_passe_hash, role, actif, id_ferme) VALUES
 (1, 'Administrateur Général', 'admin',   '$2a$12$.mEn176k2WtA91jK4TNL/OXz3N8cDJPokNvYfl7YJmqLDVSGb5mua', 'ADMIN',       TRUE, NULL),
 (2, 'Youssef Benali',         'resp1',   '$2a$10$Q4AyG.ECojLinvZpv6NdFuJRloi5jJeJLfL36CIVihGZNRfRXqfRC', 'RESPONSABLE', TRUE, 1),
 (3, 'Salma Idrissi',          'rh1',     '$2a$10$Q4AyG.ECojLinvZpv6NdFuJRloi5jJeJLfL36CIVihGZNRfRXqfRC', 'AGENT_RH',    TRUE, 1),
 (4, 'Karim El Amrani',        'resp2',   '$2a$10$Q4AyG.ECojLinvZpv6NdFuJRloi5jJeJLfL36CIVihGZNRfRXqfRC', 'RESPONSABLE', TRUE, 2),
 (5, 'Nadia Chraibi',          'rh2',     '$2a$10$Q4AyG.ECojLinvZpv6NdFuJRloi5jJeJLfL36CIVihGZNRfRXqfRC', 'AGENT_RH',    TRUE, 2),
 (6, 'Omar Tazi',              'resp3',   '$2a$10$Q4AyG.ECojLinvZpv6NdFuJRloi5jJeJLfL36CIVihGZNRfRXqfRC', 'RESPONSABLE', TRUE, 3),
 (7, 'Hind Alaoui',            'rh3',     '$2a$10$Q4AyG.ECojLinvZpv6NdFuJRloi5jJeJLfL36CIVihGZNRfRXqfRC', 'AGENT_RH',    FALSE, 3);

-- ---------- Serres ----------
INSERT INTO serre (id, nom, surface_m2, structure, irrigation, variete, id_ferme) VALUES
 (1, 'Serre A1', 5000.00, 'Canarienne',  'Goutte à goutte', 'Cerise rouge', 1),
 (2, 'Serre A2', 3500.50, 'Tunnel',      'Goutte à goutte', 'Cerise jaune', 1),
 (3, 'Serre B1', 4200.00, 'Canarienne',  'Goutte à goutte', 'Cerise rouge', 2),
 (4, 'Serre B2', 2800.00, 'Multichapelle','Hydroponique',   'Cerise grappe',2),
 (5, 'Serre C1', 6000.00, 'Canarienne',  'Goutte à goutte', 'Cerise rouge', 3),
 (6, 'Serre C2', 1500.00, 'Tunnel',      NULL,              NULL,           3);

-- ---------- Récoltes ----------
INSERT INTO recolte (id, date_heure, poids_kg, variete, id_ferme) VALUES
 (1,  '2026-10-01 07:30:00', 420.50, 'Cerise rouge',  1),
 (2,  '2026-10-02 07:45:00', 380.00, 'Cerise rouge',  1),
 (3,  '2026-10-03 08:00:00', 250.25, 'Cerise jaune',  1),
 (4,  '2026-10-05 07:15:00', 510.00, 'Cerise rouge',  2),
 (5,  '2026-10-06 07:20:00', 460.75, 'Cerise grappe', 2),
 (6,  '2026-10-08 08:10:00', 300.00, 'Cerise rouge',  2),
 (7,  '2026-10-10 06:50:00', 700.00, 'Cerise rouge',  3),
 (8,  '2026-10-12 07:00:00', 650.40, 'Cerise rouge',  3),
 (9,  '2026-11-03 07:30:00', 480.00, 'Cerise rouge',  1),
 (10, '2026-11-04 07:30:00', 355.60, 'Cerise jaune',  1),
 (11, '2026-11-09 07:40:00', 520.00, 'Cerise rouge',  2),
 (12, '2026-11-10 07:50:00', 610.00, 'Cerise rouge',  3);

-- ---------- Dépenses (toutes les catégories) ----------
INSERT INTO depense (id, date_depense, categorie, montant, description, id_ferme) VALUES
 (1,  '2026-09-01', 'PLANTS',      15000.00, 'Achat de plants de tomates cerises',   1),
 (2,  '2026-09-05', 'ENGRAIS',      4200.50, 'Engrais NPK 20-20-20',                 1),
 (3,  '2026-09-10', 'EAU_ENERGIE',  1800.00, 'Facture électricité pompage',          1),
 (4,  '2026-10-02', 'EMBALLAGE',    2500.00, 'Barquettes et cartons',                1),
 (5,  '2026-10-15', 'AUTRE',         950.00, 'Réparation du film plastique',         1),
 (6,  '2026-09-03', 'PLANTS',      12000.00, 'Achat de plants',                      2),
 (7,  '2026-09-12', 'ENGRAIS',      3100.00, 'Engrais potassique',                   2),
 (8,  '2026-10-05', 'EAU_ENERGIE',  2200.75, 'Eau d''irrigation',                    2),
 (9,  '2026-10-20', 'EMBALLAGE',    1900.00, 'Cagettes',                             2),
 (10, '2026-09-08', 'PLANTS',      18000.00, 'Achat de plants',                      3),
 (11, '2026-10-01', 'ENGRAIS',      5000.00, 'Engrais organique',                    3),
 (12, '2026-11-02', 'AUTRE',        1200.00, 'Maintenance du système de goutte à goutte', 3);

-- ---------- Clients (les 4 types, dont un inactif) ----------
INSERT INTO client (id, code, nom, type, telephone, ville, actif) VALUES
 (1, 'CLI-001', 'Station Agadir Pack',   'STATION_CONDITIONNEMENT', '0528123456', 'Agadir',   TRUE),
 (2, 'CLI-002', 'Export Maroc Fruits',   'EXPORTATEUR',             '0528654321', 'Inezgane', TRUE),
 (3, 'CLI-003', 'Grossiste Al Amal',     'GROSSISTE',               '0661001122', 'Taroudant',TRUE),
 (4, 'CLI-004', 'Marché de Gros Casablanca', 'MARCHE_GROS',         '0522334455', 'Casablanca',TRUE),
 (5, 'CLI-005', 'Ancien Client SARL',    'GROSSISTE',               NULL,         NULL,       FALSE);

-- ---------- Ventes (LIVREE, PAYEE, ANNULEE) ----------
-- PAYEE exige une date_paiement (contrainte ck_vente_paiement)
INSERT INTO vente (id, numero_bon, date_livraison, statut, date_paiement, id_client, id_admin) VALUES
 (1, 'BL-2026-001', '2026-10-05', 'PAYEE',   '2026-10-20', 1, 1),
 (2, 'BL-2026-002', '2026-10-12', 'PAYEE',   '2026-10-30', 2, 1),
 (3, 'BL-2026-003', '2026-10-20', 'LIVREE',  NULL,         3, 1),
 (4, 'BL-2026-004', '2026-11-02', 'LIVREE',  NULL,         4, 1),
 (5, 'BL-2026-005', '2026-11-08', 'ANNULEE', NULL,         1, 1),
 (6, 'BL-2026-006', '2026-11-12', 'LIVREE',  NULL,         2, 1);

INSERT INTO ligne_vente (id, quantite_kg, prix_kg, id_vente, id_ferme) VALUES
 (1,  500.00, 12.50, 1, 1),
 (2,  300.00, 12.00, 1, 2),
 (3,  800.00, 11.80, 2, 1),
 (4,  400.00, 12.20, 2, 3),
 (5,  600.00, 10.50, 3, 2),
 (6,  350.00, 13.00, 4, 3),
 (7,  250.00, 13.00, 4, 1),
 (8,  100.00, 12.00, 5, 1),
 (9,  450.00, 11.00, 6, 2),
 (10, 200.00, 11.50, 6, 3);

-- ---------- Ouvriers ----------
INSERT INTO ouvrier (id, matricule, cin, nom_prenom, telephone, adresse, poste, salaire_journalier, actif, id_serre, id_ferme) VALUES
 (1, 'OUV-001', 'JA100001', 'Ahmed Boukhari',  '0661000001', 'Douar Ait Yazza, Biougra', 'Cueilleur',    150.00, TRUE,  1,    1),
 (2, 'OUV-002', 'JA100002', 'Fatima Zahra Ait Lahcen', '0661000002', 'Biougra centre',   'Chef d''équipe',160.00, TRUE,  2,    1),
 (3, 'OUV-003', 'JA100003', 'Mohamed Ouali',   '0661000003', 'Douar Imi Ouaddar',        'Cueilleur',    150.00, FALSE, NULL, 1),
 (4, 'OUV-004', 'JB200001', 'Rachid Hamdaoui', '0662000001', 'Ait Melloul',              'Cueilleur',    150.00, TRUE,  3,    2),
 (5, 'OUV-005', 'JB200002', 'Khadija Mansouri','0662000002', 'Inezgane',                 'Technicien',   170.00, TRUE,  4,    2),
 (6, 'OUV-006', 'JB200003', 'Said Benhammou',  '0662000003', 'Ait Melloul',              'Cueilleur',    150.00, TRUE,  3,    2),
 (7, 'OUV-007', 'JC300001', 'Brahim Lachgar',  '0663000001', 'Ait Baha',                 'Cueilleur',    150.00, TRUE,  5,    3),
 (8, 'OUV-008', 'JC300002', 'Zineb Moutawakil','0663000002', 'Ait Baha',                 'Cueilleur',    150.00, TRUE,  6,    3),
 (9, 'OUV-009', 'JC300003', 'Hassan Elouazzani','0663000003','Tiznit',                   'Chef d''équipe',180.00, TRUE,  NULL, 3);

-- ---------- Reçus de paie ----------
-- Reçu 1 (ferme 1) : ouvrier 1 = 5 j x 150 = 750 ; ouvrier 2 = 6 j x 160 = 960 -> total 1710
-- Reçu 2 (ferme 2) : ouvrier 4 = 4 j x 150 = 600 ; ouvrier 5 = 3 j x 170 = 510 -> total 1110
INSERT INTO recu_paie (id, numero, date_paiement, periode_debut, periode_fin, total, id_ferme, id_agent_rh) VALUES
 (1, 'RP-2026-001', '2026-10-08', '2026-10-01', '2026-10-07', 1710.00, 1, 3),
 (2, 'RP-2026-002', '2026-10-08', '2026-10-01', '2026-10-07', 1110.00, 2, 5);

INSERT INTO ligne_paie (id, jours_payes, salaire_journalier, montant, id_recu, id_ouvrier) VALUES
 (1, 5, 150.00, 750.00, 1, 1),
 (2, 6, 160.00, 960.00, 1, 2),
 (3, 4, 150.00, 600.00, 2, 4),
 (4, 3, 170.00, 510.00, 2, 5);

-- ---------- Pointages ----------
-- Jours déjà payés (id_recu renseigné)
INSERT INTO pointage (date_pointage, statut, id_ouvrier, id_recu)
SELECT d::date, 'PRESENT', 1, 1 FROM generate_series('2026-10-01'::date, '2026-10-05'::date, '1 day') d;
INSERT INTO pointage (date_pointage, statut, id_ouvrier, id_recu)
SELECT d::date, 'ABSENT', 1, 1 FROM generate_series('2026-10-06'::date, '2026-10-07'::date, '1 day') d;

INSERT INTO pointage (date_pointage, statut, id_ouvrier, id_recu)
SELECT d::date, 'PRESENT', 2, 1 FROM generate_series('2026-10-01'::date, '2026-10-06'::date, '1 day') d;
INSERT INTO pointage (date_pointage, statut, id_ouvrier, id_recu)
VALUES ('2026-10-07', 'ABSENT', 2, 1);

INSERT INTO pointage (date_pointage, statut, id_ouvrier, id_recu)
SELECT d::date, 'PRESENT', 4, 2 FROM generate_series('2026-10-01'::date, '2026-10-04'::date, '1 day') d;
INSERT INTO pointage (date_pointage, statut, id_ouvrier, id_recu)
SELECT d::date, 'ABSENT', 4, 2 FROM generate_series('2026-10-05'::date, '2026-10-07'::date, '1 day') d;

INSERT INTO pointage (date_pointage, statut, id_ouvrier, id_recu)
SELECT d::date, 'PRESENT', 5, 2 FROM generate_series('2026-10-01'::date, '2026-10-03'::date, '1 day') d;
INSERT INTO pointage (date_pointage, statut, id_ouvrier, id_recu)
SELECT d::date, 'ABSENT', 5, 2 FROM generate_series('2026-10-04'::date, '2026-10-07'::date, '1 day') d;

-- Jours non encore payés (id_recu NULL) : semaine du 9 au 13 novembre 2026
INSERT INTO pointage (date_pointage, statut, id_ouvrier, id_recu)
SELECT d::date, 'PRESENT', o, NULL
FROM generate_series('2026-11-09'::date, '2026-11-13'::date, '1 day') d, (VALUES (1),(2),(4),(5),(6),(7),(8),(9)) AS t(o);

-- Quelques absences non payées
UPDATE pointage SET statut = 'ABSENT'
WHERE id_recu IS NULL AND ((id_ouvrier = 1 AND date_pointage = '2026-11-11')
                        OR (id_ouvrier = 6 AND date_pointage = '2026-11-12'));

-- ---------- Réalignement des séquences SERIAL ----------
-- (les id ont été insérés explicitement : sans ceci, les INSERT des tests échoueraient en doublon)
SELECT setval(pg_get_serial_sequence('ferme',       'id'), (SELECT MAX(id) FROM ferme));
SELECT setval(pg_get_serial_sequence('utilisateur', 'id'), (SELECT MAX(id) FROM utilisateur));
SELECT setval(pg_get_serial_sequence('serre',       'id'), (SELECT MAX(id) FROM serre));
SELECT setval(pg_get_serial_sequence('recolte',     'id'), (SELECT MAX(id) FROM recolte));
SELECT setval(pg_get_serial_sequence('depense',     'id'), (SELECT MAX(id) FROM depense));
SELECT setval(pg_get_serial_sequence('client',      'id'), (SELECT MAX(id) FROM client));
SELECT setval(pg_get_serial_sequence('vente',       'id'), (SELECT MAX(id) FROM vente));
SELECT setval(pg_get_serial_sequence('ligne_vente', 'id'), (SELECT MAX(id) FROM ligne_vente));
SELECT setval(pg_get_serial_sequence('ouvrier',     'id'), (SELECT MAX(id) FROM ouvrier));
SELECT setval(pg_get_serial_sequence('recu_paie',   'id'), (SELECT MAX(id) FROM recu_paie));
SELECT setval(pg_get_serial_sequence('ligne_paie',  'id'), (SELECT MAX(id) FROM ligne_paie));
SELECT setval(pg_get_serial_sequence('pointage',    'id'), (SELECT MAX(id) FROM pointage));

COMMIT;