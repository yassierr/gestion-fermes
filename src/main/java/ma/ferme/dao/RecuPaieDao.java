package ma.ferme.dao;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.LignePaie;
import ma.ferme.model.RecuPaie;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO d'accès aux reçus de paie ({@code recu_paie}) et à leurs lignes ({@code ligne_paie}).
 */
public class RecuPaieDao implements GenericDao<RecuPaie> {

    @Override
    public void insert(RecuPaie recu) {
        String sqlFerme = "SELECT id_ferme FROM utilisateur WHERE id = ?";
        String sqlInsertRecu = "INSERT INTO recu_paie (numero, date_paiement, periode_debut, periode_fin, total, id_ferme, id_agent_rh) "
                             + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        String sqlInsertLigne = "INSERT INTO ligne_paie (jours_payes, salaire_journalier, montant, id_recu, id_ouvrier) "
                              + "VALUES (?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int idFerme = recupererIdFermeAgent(conn, sqlFerme, recu.getIdAgentRh());
                String numero = "RP-" + recu.getDatePaiement().getYear() + "-" + (System.nanoTime() % 1_000_000L);

                try (PreparedStatement stmt = conn.prepareStatement(sqlInsertRecu)) {
                    stmt.setString(1, numero);
                    stmt.setDate(2, Date.valueOf(recu.getDatePaiement()));
                    stmt.setDate(3, Date.valueOf(recu.getPeriodeDebut()));
                    stmt.setDate(4, Date.valueOf(recu.getPeriodeFin()));
                    stmt.setBigDecimal(5, recu.getTotal());
                    stmt.setInt(6, idFerme);
                    stmt.setInt(7, recu.getIdAgentRh());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            recu.setId(rs.getInt("id"));
                        }
                    }
                }

                // Insertion des lignes de paie éventuelles attachées au reçu
                if (recu.getLignes() != null && !recu.getLignes().isEmpty()) {
                    try (PreparedStatement stmtLigne = conn.prepareStatement(sqlInsertLigne)) {
                        for (LignePaie ligne : recu.getLignes()) {
                            int jours = (int) Math.round(ligne.getJoursPayes());
                            BigDecimal salaireJour = jours > 0
                                    ? ligne.getMontant().divide(BigDecimal.valueOf(jours), 2, RoundingMode.HALF_UP)
                                    : BigDecimal.ZERO;

                            ligne.setIdRecuPaie(recu.getId());
                            stmtLigne.setInt(1, Math.max(jours, 1));
                            stmtLigne.setBigDecimal(2, salaireJour);
                            stmtLigne.setBigDecimal(3, ligne.getMontant());
                            stmtLigne.setInt(4, recu.getId());
                            stmtLigne.setInt(5, ligne.getIdOuvrier());

                            try (ResultSet rsL = stmtLigne.executeQuery()) {
                                if (rsL.next()) {
                                    ligne.setId(rsL.getInt("id"));
                                }
                            }
                        }
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de l'insertion du reçu de paie", e);
        }
    }

    @Override
    public Optional<RecuPaie> findById(int id) {
        String sql = "SELECT * FROM recu_paie WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    RecuPaie recu = mapResultSetToRecu(rs);
                    recu.setLignes(findLignesByRecu(conn, recu.getId()));
                    return Optional.of(recu);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la recherche du reçu ID : " + id, e);
        }
    }

    @Override
    public List<RecuPaie> findAll() {
        String sql = "SELECT * FROM recu_paie ORDER BY date_paiement DESC, id DESC";
        List<RecuPaie> liste = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                liste.add(mapResultSetToRecu(rs));
            }
            return liste;
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la récupération de tous les reçus de paie", e);
        }
    }

    /**
     * Récupère tous les reçus de paie d'une ferme donnée.
     */
    public List<RecuPaie> findByFerme(int idFerme) {
        String sql = "SELECT * FROM recu_paie WHERE id_ferme = ? ORDER BY date_paiement DESC, id DESC";
        List<RecuPaie> liste = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idFerme);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapResultSetToRecu(rs));
                }
            }
            return liste;
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la récupération des reçus de la ferme ID : " + idFerme, e);
        }
    }

    /**
     * Récupère tous les reçus émis par un Agent RH donné.
     */
    public List<RecuPaie> findByAgentRh(int idAgentRh) {
        String sql = "SELECT * FROM recu_paie WHERE id_agent_rh = ? ORDER BY date_paiement DESC, id DESC";
        List<RecuPaie> liste = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idAgentRh);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapResultSetToRecu(rs));
                }
            }
            return liste;
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la récupération des reçus de l'agent RH ID : " + idAgentRh, e);
        }
    }

    /**
     * Récupère les lignes de détail d'un reçu de paie donné.
     */
    public List<LignePaie> findLignesByRecu(int idRecu) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findLignesByRecu(conn, idRecu);
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la récupération des lignes du reçu ID : " + idRecu, e);
        }
    }

    @Override
    public void update(RecuPaie recu) {
        String sql = "UPDATE recu_paie SET date_paiement = ?, periode_debut = ?, periode_fin = ?, "
                   + "total = ?, id_agent_rh = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(recu.getDatePaiement()));
            stmt.setDate(2, Date.valueOf(recu.getPeriodeDebut()));
            stmt.setDate(3, Date.valueOf(recu.getPeriodeFin()));
            stmt.setBigDecimal(4, recu.getTotal());
            stmt.setInt(5, recu.getIdAgentRh());
            stmt.setInt(6, recu.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la mise à jour du reçu ID : " + recu.getId(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM recu_paie WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la suppression du reçu ID : " + id, e);
        }
    }

    private int recupererIdFermeAgent(Connection conn, String sql, int idAgentRh) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idAgentRh);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getObject("id_ferme") != null) {
                    return rs.getInt("id_ferme");
                }
            }
        }
        throw new SQLException("Agent RH introuvable ou sans ferme associée (ID : " + idAgentRh + ")");
    }

    private List<LignePaie> findLignesByRecu(Connection conn, int idRecu) throws SQLException {
        String sql = "SELECT * FROM ligne_paie WHERE id_recu = ? ORDER BY id ASC";
        List<LignePaie> lignes = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idRecu);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lignes.add(new LignePaie(
                        rs.getInt("id"),
                        rs.getInt("id_recu"),
                        rs.getInt("id_ouvrier"),
                        rs.getInt("jours_payes"),
                        rs.getBigDecimal("montant")
                    ));
                }
            }
        }
        return lignes;
    }

    private RecuPaie mapResultSetToRecu(ResultSet rs) throws SQLException {
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