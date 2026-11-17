package ma.ferme.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import ma.ferme.config.DatabaseConnection;
import ma.ferme.dao.DaoException;

/**
 * Paiement des ouvriers d'une ferme. Le paiement est une operation
 * <b>atomique</b> : dans une seule transaction, il cree le recu de paie,
 * ses lignes (une par ouvrier) et marque les pointages PRESENT comme payes.
 * Si une etape echoue, rien n'est enregistre.
 * <p>
 * Regle : montant d'un ouvrier = nombre de jours PRESENT non payes x salaire journalier
 * (pas de demi-journee).
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public class PaieService {

    /**
     * Ligne d'un recu de paie.
     *
     * @param idOuvrier        identifiant de l'ouvrier
     * @param matricule        matricule de l'ouvrier
     * @param nomPrenom        nom et prenom
     * @param jours            nombre de jours payes
     * @param salaireJournalier salaire journalier en MAD
     * @param montant          jours x salaire journalier
     */
    public record LigneRecu(int idOuvrier, String matricule, String nomPrenom, int jours,
                            BigDecimal salaireJournalier, BigDecimal montant) {
    }

    /**
     * Resultat d'un paiement : le recu cree et ses lignes.
     *
     * @param idRecu       identifiant du recu
     * @param numero       numero du recu (RP-AAAA-NNNN)
     * @param datePaiement date du paiement
     * @param periodeDebut debut de la periode payee
     * @param periodeFin   fin de la periode payee
     * @param idFerme      identifiant de la ferme
     * @param total        total paye en MAD
     * @param lignes       detail par ouvrier
     */
    public record ResultatPaie(int idRecu, String numero, LocalDate datePaiement, LocalDate periodeDebut,
                               LocalDate periodeFin, int idFerme, BigDecimal total, List<LigneRecu> lignes) {
    }

    /**
     * Paie tous les jours PRESENT non payes d'une ferme sur une periode.
     *
     * @param idFerme    identifiant de la ferme
     * @param debut      premier jour de la periode (inclus)
     * @param fin        dernier jour de la periode (inclus)
     * @param idAgentRh  identifiant de l'agent RH qui effectue le paiement
     * @return le recu cree avec son detail
     * @throws IllegalArgumentException si la periode est invalide
     * @throws IllegalStateException    s'il n'y a aucun jour a payer
     * @throws DaoException             si une erreur SQL survient (la transaction est annulee)
     */
    public ResultatPaie payerFerme(int idFerme, LocalDate debut, LocalDate fin, int idAgentRh) {
        if (debut == null || fin == null) {
            throw new IllegalArgumentException("La période est obligatoire");
        }
        if (fin.isBefore(debut)) {
            throw new IllegalArgumentException("La date de fin doit être après la date de début");
        }
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (Statement st = c.createStatement()) {
                    st.execute("LOCK TABLE recu_paie IN SHARE ROW EXCLUSIVE MODE");
                }
                List<LigneRecu> lignes = calculerLignes(c, idFerme, debut, fin);
                if (lignes.isEmpty()) {
                    throw new IllegalStateException("Aucun jour à payer sur cette période");
                }
                BigDecimal total = lignes.stream().map(LigneRecu::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
                LocalDate aujourdhui = LocalDate.now();
                String numero = prochainNumero(c, aujourdhui.getYear());
                int idRecu = insererRecu(c, numero, aujourdhui, debut, fin, total, idFerme, idAgentRh);
                insererLignes(c, idRecu, lignes);
                marquerPointages(c, idRecu, idFerme, debut, fin);
                c.commit();
                return new ResultatPaie(idRecu, numero, aujourdhui, debut, fin, idFerme, total, lignes);
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException("Paiement impossible : " + e.getMessage(), e);
        }
    }

    /**
     * Calcule le solde actuel d'un ouvrier : jours PRESENT non payes x salaire journalier.
     *
     * @param idOuvrier identifiant de l'ouvrier
     * @return le solde en MAD (0 si aucun jour a payer)
     */
    public BigDecimal soldeOuvrier(int idOuvrier) {
        String sql = "SELECT COUNT(p.id) * o.salaire_journalier FROM ouvrier o "
                + "LEFT JOIN pointage p ON p.id_ouvrier = o.id AND p.statut = 'PRESENT' AND p.id_recu IS NULL "
                + "WHERE o.id = ? GROUP BY o.salaire_journalier";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idOuvrier);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new DaoException("Calcul du solde impossible : " + e.getMessage(), e);
        }
    }

    private List<LigneRecu> calculerLignes(Connection c, int idFerme, LocalDate debut, LocalDate fin)
            throws SQLException {
        String sql = "SELECT o.id, o.matricule, o.nom_prenom, o.salaire_journalier, COUNT(p.id) AS jours "
                + "FROM ouvrier o JOIN pointage p ON p.id_ouvrier = o.id "
                + "AND p.statut = 'PRESENT' AND p.id_recu IS NULL AND p.date_pointage BETWEEN ? AND ? "
                + "WHERE o.id_ferme = ? "
                + "GROUP BY o.id, o.matricule, o.nom_prenom, o.salaire_journalier "
                + "HAVING COUNT(p.id) > 0 ORDER BY o.matricule";
        List<LigneRecu> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(debut));
            ps.setDate(2, Date.valueOf(fin));
            ps.setInt(3, idFerme);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int jours = rs.getInt("jours");
                    BigDecimal salaire = rs.getBigDecimal("salaire_journalier");
                    out.add(new LigneRecu(rs.getInt("id"), rs.getString("matricule"),
                            rs.getString("nom_prenom"), jours, salaire,
                            salaire.multiply(BigDecimal.valueOf(jours))));
                }
            }
        }
        return out;
    }

    private String prochainNumero(Connection c, int annee) throws SQLException {
        String prefix = "RP-" + annee + "-";
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero FROM 9) AS INTEGER)), 0) + 1 "
                + "FROM recu_paie WHERE numero LIKE ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return prefix + String.format("%04d", rs.getInt(1));
            }
        }
    }

    private int insererRecu(Connection c, String numero, LocalDate datePaiement, LocalDate debut, LocalDate fin,
                            BigDecimal total, int idFerme, int idAgentRh) throws SQLException {
        String sql = "INSERT INTO recu_paie(numero, date_paiement, periode_debut, periode_fin, total, id_ferme, id_agent_rh) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, numero);
            ps.setDate(2, Date.valueOf(datePaiement));
            ps.setDate(3, Date.valueOf(debut));
            ps.setDate(4, Date.valueOf(fin));
            ps.setBigDecimal(5, total);
            ps.setInt(6, idFerme);
            ps.setInt(7, idAgentRh);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void insererLignes(Connection c, int idRecu, List<LigneRecu> lignes) throws SQLException {
        String sql = "INSERT INTO ligne_paie(jours_payes, salaire_journalier, montant, id_recu, id_ouvrier) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (LigneRecu l : lignes) {
                ps.setInt(1, l.jours());
                ps.setBigDecimal(2, l.salaireJournalier());
                ps.setBigDecimal(3, l.montant());
                ps.setInt(4, idRecu);
                ps.setInt(5, l.idOuvrier());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void marquerPointages(Connection c, int idRecu, int idFerme, LocalDate debut, LocalDate fin)
            throws SQLException {
        String sql = "UPDATE pointage SET id_recu = ? WHERE statut = 'PRESENT' AND id_recu IS NULL "
                + "AND date_pointage BETWEEN ? AND ? "
                + "AND id_ouvrier IN (SELECT id FROM ouvrier WHERE id_ferme = ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idRecu);
            ps.setDate(2, Date.valueOf(debut));
            ps.setDate(3, Date.valueOf(fin));
            ps.setInt(4, idFerme);
            ps.executeUpdate();
        }
    }
}