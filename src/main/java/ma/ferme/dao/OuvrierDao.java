package ma.ferme.dao;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.Ouvrier;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OuvrierDao implements GenericDao<Ouvrier> {

    @Override
    public void insert(Ouvrier ouvrier) {
        String sqlInsert = "INSERT INTO ouvrier (matricule, id_ferme, nom, prenom, cin, telephone, adresse, poste, salaire_journalier, actif) "
                         + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        String sqlUpdateMat = "UPDATE ouvrier SET matricule = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Matricule temporaire unique pour satisfaire la contrainte NOT NULL UNIQUE avant d'obtenir l'ID
            String tempMatricule = (ouvrier.getMatricule() != null && !ouvrier.getMatricule().isEmpty())
                    ? ouvrier.getMatricule()
                    : "TMP-" + System.nanoTime();

            try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                stmt.setString(1, tempMatricule);
                stmt.setInt(2, ouvrier.getIdFerme());
                stmt.setString(3, ouvrier.getNom());
                stmt.setString(4, ouvrier.getPrenom());
                stmt.setString(5, ouvrier.getCin());
                stmt.setString(6, ouvrier.getTelephone());
                stmt.setString(7, ouvrier.getAdresse());
                stmt.setString(8, ouvrier.getPoste());
                stmt.setBigDecimal(9, ouvrier.getSalaireJournalier());
                stmt.setBoolean(10, ouvrier.isActif());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt("id");
                        ouvrier.setId(generatedId); // Génère aussi "AS-XXXX" dans l'objet Java
                    }
                }
            }

            // Met à jour le matricule définitif ("AS-XXXX") en base de données
            try (PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdateMat)) {
                stmtUpdate.setString(1, ouvrier.getMatricule());
                stmtUpdate.setInt(2, ouvrier.getId());
                stmtUpdate.executeUpdate();
            }

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de l'insertion de l'ouvrier: " + ouvrier.getNom(), e);
        }
    }

    @Override
    public Optional<Ouvrier> findById(int id) {
        String sql = "SELECT * FROM ouvrier WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToOuvrier(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la recherche de l'ouvrier avec l'ID: " + id, e);
        }
    }

    @Override
    public List<Ouvrier> findAll() {
        String sql = "SELECT * FROM ouvrier ORDER BY id DESC";
        List<Ouvrier> liste = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                liste.add(mapResultSetToOuvrier(rs));
            }
            return liste;

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la récupération de tous les ouvriers", e);
        }
    }

    public List<Ouvrier> findByFerme(int idFerme) {
        String sql = "SELECT * FROM ouvrier WHERE id_ferme = ? ORDER BY id DESC";
        List<Ouvrier> liste = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idFerme);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapResultSetToOuvrier(rs));
                }
            }
            return liste;

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la récupération des ouvriers de la ferme: " + idFerme, e);
        }
    }

    @Override
    public void update(Ouvrier ouvrier) {
        String sql = "UPDATE ouvrier SET id_ferme = ?, nom = ?, prenom = ?, cin = ?, telephone = ?, adresse = ?, poste = ?, salaire_journalier = ?, actif = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ouvrier.getIdFerme());
            stmt.setString(2, ouvrier.getNom());
            stmt.setString(3, ouvrier.getPrenom());
            stmt.setString(4, ouvrier.getCin());
            stmt.setString(5, ouvrier.getTelephone());
            stmt.setString(6, ouvrier.getAdresse());
            stmt.setString(7, ouvrier.getPoste());
            stmt.setBigDecimal(8, ouvrier.getSalaireJournalier());
            stmt.setBoolean(9, ouvrier.isActif());
            stmt.setInt(10, ouvrier.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la mise à jour de l'ouvrier ID: " + ouvrier.getId(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM ouvrier WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la suppression de l'ouvrier ID: " + id, e);
        }
    }

    private Ouvrier mapResultSetToOuvrier(ResultSet rs) throws SQLException {
        return new Ouvrier(
            rs.getInt("id"),
            rs.getString("matricule"),
            rs.getInt("id_ferme"),
            rs.getString("nom"),
            rs.getString("prenom"),
            rs.getString("cin"),
            rs.getString("telephone"),
            rs.getString("adresse"),
            rs.getString("poste"),
            rs.getBigDecimal("salaire_journalier"),
            rs.getBoolean("actif")
        );
    }
}