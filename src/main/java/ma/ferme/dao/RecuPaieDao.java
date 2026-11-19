package ma.ferme.dao;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.LignePaie;
import ma.ferme.model.RecuPaie;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO de gestion des reçus de paie.
 *
 * Tables utilisées :
 * - utilisateur
 * - ouvrier
 * - pointage
 * - recu_paie
 * - ligne_paie
 *
 * Compatible avec le schéma PostgreSQL de Gestion de fermes.
 *
 * Règles :
 * - seuls les pointages PRESENT sont rémunérés ;
 * - un pointage est non payé si id_recu IS NULL ;
 * - le paiement est enregistré par pointage.id_recu ;
 * - le salaire journalier est récupéré depuis ouvrier ;
 * - les opérations importantes utilisent des transactions.
 */
public class RecuPaieDao implements GenericDao<RecuPaie> {

    // =========================================================
    // RECORDS UTILISES PAR LE SERVICE
    // =========================================================

    /**
     * Ligne utilisée pour calculer une paie.
     */
    public record LignePaiement(
            int idOuvrier,
            String matricule,
            String nomPrenom,
            int jours,
            BigDecimal salaireJournalier,
            BigDecimal montant
    ) {
    }

    /**
     * Résumé d'un reçu pour l'affichage dans un tableau.
     */
    public record ResumeRecu(
            int idRecu,
            String numero,
            LocalDate datePaiement,
            LocalDate periodeDebut,
            LocalDate periodeFin,
            int nbOuvriers,
            BigDecimal total
    ) {
    }

    /**
     * Ligne utilisée pour la génération du PDF.
     */
    public record LigneRecuPdf(
            int idOuvrier,
            String matricule,
            String nomPrenom,
            int jours,
            BigDecimal salaireJournalier,
            BigDecimal montant
    ) {
    }

    /**
     * Informations complètes d'un reçu pour PDF.
     */
    public record DetailsRecuPdf(
            int idRecu,
            String numero,
            LocalDate datePaiement,
            LocalDate periodeDebut,
            LocalDate periodeFin,
            int idFerme,
            BigDecimal total,
            List<LigneRecuPdf> lignes
    ) {
    }


    // =========================================================
    // INSERTION CLASSIQUE
    // =========================================================

    /**
     * Insère un reçu et ses lignes.
     *
     * Cette méthode est utilisée par le GenericDao.
     */
    @Override
    public void insert(RecuPaie recu) {

        if (recu == null) {
            throw new IllegalArgumentException(
                    "Le reçu ne peut pas être null."
            );
        }

        if (recu.getLignes() == null || recu.getLignes().isEmpty()) {
            throw new IllegalArgumentException(
                    "Un reçu doit contenir au moins une ligne de paie."
            );
        }

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                int idFerme = recupererIdFermeAgent(
                        conn,
                        recu.getIdAgentRh()
                );

                String numero = prochainNumero(conn);

                int idRecu = insererRecu(
                        conn,
                        numero,
                        recu.getDatePaiement(),
                        recu.getPeriodeDebut(),
                        recu.getPeriodeFin(),
                        recu.getTotal(),
                        idFerme,
                        recu.getIdAgentRh()
                );

                recu.setId(idRecu);

                insererLignesModel(
                        conn,
                        idRecu,
                        recu.getLignes()
                );

                conn.commit();

            } catch (Exception e) {

                conn.rollback();

                throw e;

            } finally {

                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {

            throw new DaoException(
                    "Erreur lors de l'insertion du reçu de paie.",
                    e
            );
        }
    }


    // =========================================================
    // INSERTION DU RECU DANS UNE TRANSACTION
    // =========================================================

    /**
     * Insère uniquement le reçu.
     *
     * Cette méthode permet au PaieService de gérer lui-même
     * la transaction globale.
     */
    public int insererRecu(
            Connection conn,
            String numero,
            LocalDate datePaiement,
            LocalDate periodeDebut,
            LocalDate periodeFin,
            BigDecimal total,
            int idFerme,
            int idAgentRh
    ) throws SQLException {

        String sql = """
                INSERT INTO recu_paie (
                    numero,
                    date_paiement,
                    periode_debut,
                    periode_fin,
                    total,
                    id_ferme,
                    id_agent_rh
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, numero);

            stmt.setDate(
                    2,
                    Date.valueOf(datePaiement)
            );

            stmt.setDate(
                    3,
                    Date.valueOf(periodeDebut)
            );

            stmt.setDate(
                    4,
                    Date.valueOf(periodeFin)
            );

            stmt.setBigDecimal(5, total);

            stmt.setInt(6, idFerme);

            stmt.setInt(7, idAgentRh);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        throw new SQLException(
                "Impossible de récupérer l'identifiant du reçu."
        );
    }


    // =========================================================
    // INSERTION DES LIGNES DEPUIS LE MODELE
    // =========================================================

    /**
     * Insère les lignes venant du modèle LignePaie.
     */
    public void insererLignesModel(
            Connection conn,
            int idRecu,
            List<LignePaie> lignes
    ) throws SQLException {

        if (lignes == null || lignes.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO ligne_paie (
                    jours_payes,
                    salaire_journalier,
                    montant,
                    id_recu,
                    id_ouvrier
                )
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (LignePaie ligne : lignes) {

                int jours = (int) Math.round(
                        ligne.getJoursPayes()
                );

                if (jours <= 0) {
                    throw new SQLException(
                            "Le nombre de jours payés doit être positif."
                    );
                }

                BigDecimal montant = ligne.getMontant();

                BigDecimal salaireJournalier = montant.divide(
                        BigDecimal.valueOf(jours),
                        2,
                        RoundingMode.HALF_UP
                );

                ligne.setIdRecuPaie(idRecu);

                stmt.setInt(1, jours);

                stmt.setBigDecimal(
                        2,
                        salaireJournalier
                );

                stmt.setBigDecimal(
                        3,
                        montant
                );

                stmt.setInt(4, idRecu);

                stmt.setInt(
                        5,
                        ligne.getIdOuvrier()
                );

                try (ResultSet rs = stmt.executeQuery()) {

                    if (rs.next()) {
                        ligne.setId(
                                rs.getInt("id")
                        );
                    }
                }
            }
        }
    }


    // =========================================================
    // INSERTION DES LIGNES CALCULEES PAR LE SERVICE
    // =========================================================

    /**
     * Insère les lignes de paiement calculées par PaieService.
     */
    public void insererLignes(
            Connection conn,
            int idRecu,
            List<LignePaiement> lignes
    ) throws SQLException {

        if (lignes == null || lignes.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO ligne_paie (
                    jours_payes,
                    salaire_journalier,
                    montant,
                    id_recu,
                    id_ouvrier
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (LignePaiement ligne : lignes) {

                if (ligne.jours() <= 0) {
                    throw new SQLException(
                            "Nombre de jours invalide pour l'ouvrier "
                                    + ligne.idOuvrier()
                    );
                }

                stmt.setInt(
                        1,
                        ligne.jours()
                );

                stmt.setBigDecimal(
                        2,
                        ligne.salaireJournalier()
                );

                stmt.setBigDecimal(
                        3,
                        ligne.montant()
                );

                stmt.setInt(
                        4,
                        idRecu
                );

                stmt.setInt(
                        5,
                        ligne.idOuvrier()
                );

                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }


    // =========================================================
    // CALCUL DES LIGNES DE PAIE
    // =========================================================

    /**
     * Calcule les paies des ouvriers pour une période donnée.
     *
     * Seuls les pointages :
     * - PRESENT
     * - non encore payés (id_recu IS NULL)
     * - appartenant à la ferme
     * - compris dans la période
     *
     * sont pris en compte.
     */
    public List<LignePaiement> calculerLignesPaiement(
            Connection conn,
            int idFerme,
            LocalDate periodeDebut,
            LocalDate periodeFin
    ) throws SQLException {

        String sql = """
                SELECT
                    o.id AS id_ouvrier,
                    o.matricule,
                    o.nom_prenom,
                    o.salaire_journalier,
                    COUNT(p.id) AS jours
                FROM ouvrier o
                INNER JOIN pointage p
                    ON p.id_ouvrier = o.id
                WHERE o.id_ferme = ?
                  AND p.statut = 'PRESENT'
                  AND p.id_recu IS NULL
                  AND p.date_pointage BETWEEN ? AND ?
                GROUP BY
                    o.id,
                    o.matricule,
                    o.nom_prenom,
                    o.salaire_journalier
                HAVING COUNT(p.id) > 0
                ORDER BY o.nom_prenom
                """;

        List<LignePaiement> lignes = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(
                    1,
                    idFerme
            );

            stmt.setDate(
                    2,
                    Date.valueOf(periodeDebut)
            );

            stmt.setDate(
                    3,
                    Date.valueOf(periodeFin)
            );

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    int jours = rs.getInt("jours");

                    BigDecimal salaireJournalier =
                            rs.getBigDecimal("salaire_journalier");

                    BigDecimal montant = salaireJournalier
                            .multiply(BigDecimal.valueOf(jours))
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

                    lignes.add(
                            new LignePaiement(
                                    rs.getInt("id_ouvrier"),
                                    rs.getString("matricule"),
                                    rs.getString("nom_prenom"),
                                    jours,
                                    salaireJournalier,
                                    montant
                            )
                    );
                }
            }
        }

        return lignes;
    }


    // =========================================================
    // GENERATION DU NUMERO DU RECU
    // =========================================================

    /**
     * Génère un numéro de reçu unique (séquence globale par année).
     *
     * Format : RP-2026-000001
     *
     * La contrainte d'unicité de recu_paie.numero est globale :
     * le numéro ne doit donc PAS dépendre de la ferme.
     * L'appelant doit avoir verrouillé recu_paie (SHARE ROW EXCLUSIVE)
     * pour éviter les doublons en cas de paiements simultanés.
     */
    public String prochainNumero(Connection conn) throws SQLException {

        String sql = """
                SELECT numero
                FROM recu_paie
                WHERE numero LIKE ?
                ORDER BY numero DESC
                LIMIT 1
                """;

        int annee = LocalDate.now().getYear();

        String prefixe = "RP-" + annee + "-";

        int prochain = 1;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, prefixe + "%");

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    String numero = rs.getString("numero");

                    prochain = Integer.parseInt(
                            numero.substring(prefixe.length())
                    ) + 1;
                }
            }
        }

        return prefixe + String.format("%06d", prochain);
    }


    // =========================================================
    // MARQUER LES POINTAGES COMME PAYES
    // =========================================================

    /**
     * Associe les pointages présents et non payés au reçu.
     *
     * IMPORTANT :
     * Cette méthode doit être exécutée dans la même transaction
     * que la création du reçu.
     */
    public int marquerPointagesPayes(
            Connection conn,
            int idRecu,
            int idOuvrier,
            LocalDate periodeDebut,
            LocalDate periodeFin
    ) throws SQLException {

        String sql = """
                UPDATE pointage
                SET id_recu = ?
                WHERE id_ouvrier = ?
                  AND statut = 'PRESENT'
                  AND id_recu IS NULL
                  AND date_pointage BETWEEN ? AND ?
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(
                    1,
                    idRecu
            );

            stmt.setInt(
                    2,
                    idOuvrier
            );

            stmt.setDate(
                    3,
                    Date.valueOf(periodeDebut)
            );

            stmt.setDate(
                    4,
                    Date.valueOf(periodeFin)
            );

            return stmt.executeUpdate();
        }
    }


    /**
     * Marque tous les pointages correspondant aux lignes de paiement.
     */
    public void marquerPointagesPayes(
            Connection conn,
            int idRecu,
            List<LignePaiement> lignes,
            LocalDate periodeDebut,
            LocalDate periodeFin
    ) throws SQLException {

        if (lignes == null || lignes.isEmpty()) {
            return;
        }

        for (LignePaiement ligne : lignes) {

            int nbPointages = marquerPointagesPayes(
                    conn,
                    idRecu,
                    ligne.idOuvrier(),
                    periodeDebut,
                    periodeFin
            );

            /*
             * Protection contre un décalage entre :
             * le calcul des jours et les pointages réellement modifiés.
             */
            if (nbPointages != ligne.jours()) {

                throw new SQLException(
                        "Le nombre de pointages modifiés ne correspond "
                                + "pas au nombre de jours calculés pour "
                                + ligne.nomPrenom()
                                + ". Calculés : "
                                + ligne.jours()
                                + ", modifiés : "
                                + nbPointages
                );
            }
        }
    }


    // =========================================================
    // RECHERCHE PAR ID
    // =========================================================

    @Override
    public Optional<RecuPaie> findById(int id) {

        String sql = """
                SELECT *
                FROM recu_paie
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(
                    1,
                    id
            );

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    RecuPaie recu = mapResultSetToRecu(rs);

                    recu.setLignes(
                            findLignesByRecu(
                                    conn,
                                    recu.getId()
                            )
                    );

                    return Optional.of(recu);
                }
            }

            return Optional.empty();

        } catch (SQLException e) {

            throw new DaoException(
                    "Erreur lors de la recherche du reçu ID : " + id,
                    e
            );
        }
    }


    // =========================================================
    // TOUS LES REÇUS
    // =========================================================

    @Override
    public List<RecuPaie> findAll() {

        String sql = """
                SELECT *
                FROM recu_paie
                ORDER BY date_paiement DESC, id DESC
                """;

        List<RecuPaie> liste = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                liste.add(
                        mapResultSetToRecu(rs)
                );
            }

            return liste;

        } catch (SQLException e) {

            throw new DaoException(
                    "Erreur lors de la récupération des reçus.",
                    e
            );
        }
    }


    // =========================================================
    // REÇUS PAR FERME
    // =========================================================

    /**
     * Récupère tous les reçus d'une ferme.
     */
    public List<RecuPaie> findByFerme(int idFerme) {

        String sql = """
                SELECT *
                FROM recu_paie
                WHERE id_ferme = ?
                ORDER BY date_paiement DESC, id DESC
                """;

        List<RecuPaie> liste = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(
                    1,
                    idFerme
            );

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    liste.add(
                            mapResultSetToRecu(rs)
                    );
                }
            }

            return liste;

        } catch (SQLException e) {

            throw new DaoException(
                    "Erreur lors de la récupération des reçus de la ferme "
                            + idFerme,
                    e
            );
        }
    }


    // =========================================================
    // REÇUS PAR AGENT RH
    // =========================================================

    /**
     * Récupère les reçus générés par un Agent RH.
     */
    public List<RecuPaie> findByAgentRh(int idAgentRh) {

        String sql = """
                SELECT *
                FROM recu_paie
                WHERE id_agent_rh = ?
                ORDER BY date_paiement DESC, id DESC
                """;

        List<RecuPaie> liste = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(
                    1,
                    idAgentRh
            );

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    liste.add(
                            mapResultSetToRecu(rs)
                    );
                }
            }

            return liste;

        } catch (SQLException e) {

            throw new DaoException(
                    "Erreur lors de la récupération des reçus de l'Agent RH "
                            + idAgentRh,
                    e
            );
        }
    }


    // =========================================================
    // LIGNES D'UN REÇU
    // =========================================================

    /**
     * Récupère les lignes de détail d'un reçu.
     */
    public List<LignePaie> findLignesByRecu(int idRecu) {

        try (Connection conn = DatabaseConnection.getConnection()) {

            return findLignesByRecu(
                    conn,
                    idRecu
            );

        } catch (SQLException e) {

            throw new DaoException(
                    "Erreur lors de la récupération des lignes du reçu "
                            + idRecu,
                    e
            );
        }
    }


    /**
     * Version utilisant une connexion existante.
     */
    private List<LignePaie> findLignesByRecu(
            Connection conn,
            int idRecu
    ) throws SQLException {

        String sql = """
                SELECT *
                FROM ligne_paie
                WHERE id_recu = ?
                ORDER BY id ASC
                """;

        List<LignePaie> lignes = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(
                    1,
                    idRecu
            );

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    lignes.add(
                            new LignePaie(
                                    rs.getInt("id"),
                                    rs.getInt("id_recu"),
                                    rs.getInt("id_ouvrier"),
                                    rs.getInt("jours_payes"),
                                    rs.getBigDecimal("montant")
                            )
                    );
                }
            }
        }

        return lignes;
    }


    // =========================================================
    // CALCUL DU SOLDE NON PAYE D'UN OUVRIER
    // =========================================================

    /**
     * Retourne le montant total des journées PRESENT non payées
     * pour un ouvrier.
     */
    public BigDecimal calculerSoldeOuvrier(int idOuvrier) {

        String sql = """
                SELECT
                    COALESCE(
                        SUM(o.salaire_journalier),
                        0
                    ) AS solde
                FROM ouvrier o
                INNER JOIN pointage p
                    ON p.id_ouvrier = o.id
                WHERE o.id = ?
                  AND p.statut = 'PRESENT'
                  AND p.id_recu IS NULL
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(
                    1,
                    idOuvrier
            );

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    BigDecimal solde = rs.getBigDecimal("solde");

                    // COALESCE(..., 0) renvoie un 0 sans décimales :
                    // on normalise toujours à 2 décimales.
                    return (solde == null ? BigDecimal.ZERO : solde)
                            .setScale(2, RoundingMode.HALF_UP);
                }
            }

            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        } catch (SQLException e) {

            throw new DaoException(
                    "Erreur lors du calcul du solde de l'ouvrier "
                            + idOuvrier,
                    e
            );
        }
    }


    // =========================================================
    // EFFECTIF ACTIF
    // =========================================================

    /**
     * Compte le nombre d'ouvriers actifs dans une ferme.
     */
    public int compterOuvriersActifs(int idFerme) {

        String sql = """
                SELECT COUNT(*)
                FROM ouvrier
                WHERE id_ferme = ?
                  AND actif = TRUE
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(
                    1,
                    idFerme
            );

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            return 0;

        } catch (SQLException e) {

            throw new DaoException(
                    "Erreur lors du calcul de l'effectif actif.",
                    e
            );
        }
    }


    // =========================================================
    // HISTORIQUE DES REÇUS
    // =========================================================

    /**
     * Retourne les résumés des reçus pour affichage.
     */
    public List<ResumeRecu> listerResumeRecus(int idFerme) {

        String sql = """
                SELECT
                    r.id,
                    r.numero,
                    r.date_paiement,
                    r.periode_debut,
                    r.periode_fin,
                    r.total,
                    COUNT(lp.id) AS nb_ouvriers
                FROM recu_paie r
                LEFT JOIN ligne_paie lp
                    ON lp.id_recu = r.id
                WHERE r.id_ferme = ?
                GROUP BY
                    r.id,
                    r.numero,
                    r.date_paiement,
                    r.periode_debut,
                    r.periode_fin,
                    r.total
                ORDER BY
                    r.date_paiement DESC,
                    r.id DESC
                """;

        List<ResumeRecu> liste = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(
                    1,
                    idFerme
            );

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    liste.add(
                            new ResumeRecu(
                                    rs.getInt("id"),
                                    rs.getString("numero"),
                                    rs.getDate("date_paiement").toLocalDate(),
                                    rs.getDate("periode_debut").toLocalDate(),
                                    rs.getDate("periode_fin").toLocalDate(),
                                    rs.getInt("nb_ouvriers"),
                                    rs.getBigDecimal("total")
                            )
                    );
                }
            }

            return liste;

        } catch (SQLException e) {

            throw new DaoException(
                    "Erreur lors du chargement de l'historique des paies.",
                    e
            );
        }
    }


    // =========================================================
    // DETAILS COMPLETS POUR LE PDF
    // =========================================================

    /**
     * Charge un reçu complet avec les informations des ouvriers.
     */
    public Optional<DetailsRecuPdf> chargerDetailsPdf(int idRecu) {

        String sqlRecu = """
                SELECT
                    id,
                    numero,
                    date_paiement,
                    periode_debut,
                    periode_fin,
                    id_ferme,
                    total
                FROM recu_paie
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlRecu)) {

            stmt.setInt(
                    1,
                    idRecu
            );

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    return Optional.empty();
                }

                List<LigneRecuPdf> lignes =
                        findLignesRecuPdf(
                                conn,
                                idRecu
                        );

                DetailsRecuPdf details =
                        new DetailsRecuPdf(
                                rs.getInt("id"),
                                rs.getString("numero"),
                                rs.getDate("date_paiement").toLocalDate(),
                                rs.getDate("periode_debut").toLocalDate(),
                                rs.getDate("periode_fin").toLocalDate(),
                                rs.getInt("id_ferme"),
                                rs.getBigDecimal("total"),
                                lignes
                        );

                return Optional.of(details);
            }

        } catch (SQLException e) {

            throw new DaoException(
                    "Erreur lors du chargement du reçu PDF "
                            + idRecu,
                    e
            );
        }
    }


    /**
     * Charge les lignes PDF avec les informations des ouvriers.
     */
    private List<LigneRecuPdf> findLignesRecuPdf(
            Connection conn,
            int idRecu
    ) throws SQLException {

        String sql = """
                SELECT
                    lp.id_ouvrier,
                    o.matricule,
                    o.nom_prenom,
                    lp.jours_payes,
                    lp.salaire_journalier,
                    lp.montant
                FROM ligne_paie lp
                INNER JOIN ouvrier o
                    ON o.id = lp.id_ouvrier
                WHERE lp.id_recu = ?
                ORDER BY o.nom_prenom
                """;

        List<LigneRecuPdf> lignes = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(
                    1,
                    idRecu
            );

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    lignes.add(
                            new LigneRecuPdf(
                                    rs.getInt("id_ouvrier"),
                                    rs.getString("matricule"),
                                    rs.getString("nom_prenom"),
                                    rs.getInt("jours_payes"),
                                    rs.getBigDecimal("salaire_journalier"),
                                    rs.getBigDecimal("montant")
                            )
                    );
                }
            }
        }

        return lignes;
    }


    // =========================================================
    // UPDATE
    // =========================================================

    /**
     * Mise à jour d'un reçu.
     *
     * Le numéro, la ferme et les lignes ne sont pas modifiés ici.
     */
    @Override
    public void update(RecuPaie recu) {

        String sql = """
                UPDATE recu_paie
                SET
                    date_paiement = ?,
                    periode_debut = ?,
                    periode_fin = ?,
                    total = ?,
                    id_agent_rh = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(
                    1,
                    Date.valueOf(recu.getDatePaiement())
            );

            stmt.setDate(
                    2,
                    Date.valueOf(recu.getPeriodeDebut())
            );

            stmt.setDate(
                    3,
                    Date.valueOf(recu.getPeriodeFin())
            );

            stmt.setBigDecimal(
                    4,
                    recu.getTotal()
            );

            stmt.setInt(
                    5,
                    recu.getIdAgentRh()
            );

            stmt.setInt(
                    6,
                    recu.getId()
            );

            stmt.executeUpdate();

        } catch (SQLException e) {

            throw new DaoException(
                    "Erreur lors de la mise à jour du reçu "
                            + recu.getId(),
                    e
            );
        }
    }


    // =========================================================
    // DELETE
    // =========================================================

    /**
     * Supprime un reçu.
     *
     * ATTENTION :
     * les lignes_paie sont supprimées automatiquement grâce à
     * ON DELETE CASCADE.
     *
     * Les pointages associés ne sont PAS automatiquement libérés
     * dans ce DAO. Cette opération doit être gérée par le Service
     * si la suppression d'un reçu est autorisée.
     */
    @Override
    public void delete(int id) {

        String sql = """
                DELETE FROM recu_paie
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(
                    1,
                    id
            );

            stmt.executeUpdate();

        } catch (SQLException e) {

            throw new DaoException(
                    "Erreur lors de la suppression du reçu "
                            + id,
                    e
            );
        }
    }


    // =========================================================
    // RECUPERATION DE LA FERME DE L'AGENT RH
    // =========================================================

    /**
     * Récupère l'identifiant de la ferme de l'Agent RH.
     */
    public int recupererIdFermeAgent(
            Connection conn,
            int idAgentRh
    ) throws SQLException {

        String sql = """
                SELECT id_ferme
                FROM utilisateur
                WHERE id = ?
                  AND role = 'AGENT_RH'
                  AND actif = TRUE
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(
                    1,
                    idAgentRh
            );

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    Object idFerme = rs.getObject("id_ferme");

                    if (idFerme != null) {
                        return rs.getInt("id_ferme");
                    }
                }
            }
        }

        throw new SQLException(
                "Agent RH introuvable, inactif ou sans ferme associée. "
                        + "ID : " + idAgentRh
        );
    }


    // =========================================================
    // MAPPING RESULTSET -> MODELE
    // =========================================================

    /**
     * Convertit une ligne SQL en objet RecuPaie.
     */
    private RecuPaie mapResultSetToRecu(
            ResultSet rs
    ) throws SQLException {

        return new RecuPaie(
                rs.getInt("id"),
                rs.getInt("id_agent_rh"),
                rs.getDate("date_paiement").toLocalDate(),
                rs.getDate("periode_debut").toLocalDate(),
                rs.getDate("periode_fin").toLocalDate(),
                rs.getBigDecimal("total")
        );
    }
}