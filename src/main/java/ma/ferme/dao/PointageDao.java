package ma.ferme.dao;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.Pointage;
import ma.ferme.model.enums.StatutPointage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PointageDao implements GenericDao<Pointage> {

    @Override
    public void insert(Pointage pointage) {
        String sql = "INSERT INTO pointage (id_ouvrier, date_pointage, statut) VALUES (?, ?, ?::statut_pointage) RETURNING id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, pointage.getIdOuvrier());
            stmt.setDate(2, Date.valueOf(pointage.getDatePointage()));
            stmt.setString(3, pointage.getStatut().name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    pointage.setId(rs.getInt("id"));
                }
            }

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de l'insertion du pointage pour l'ouvrier ID: " + pointage.getIdOuvrier(), e);
        }
    }

    @Override
    public Optional<Pointage> findById(int id) {
        String sql = "SELECT * FROM pointage WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPointage(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la recherche du pointage ID: " + id, e);
        }
    }

    @Override
    public List<Pointage> findAll() {
        String sql = "SELECT * FROM pointage ORDER BY date_pointage DESC";
        List<Pointage> liste = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                liste.add(mapResultSetToPointage(rs));
            }
            return liste;

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la récupération de la liste des pointages", e);
        }
    }

    // Récupère les pointages d'un ouvrier
    public List<Pointage> findByOuvrier(int idOuvrier) {
        String sql = "SELECT * FROM pointage WHERE id_ouvrier = ? ORDER BY date_pointage DESC";
        List<Pointage> liste = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idOuvrier);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapResultSetToPointage(rs));
                }
            }
            return liste;

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la récupération des pointages de l'ouvrier ID: " + idOuvrier, e);
        }
    }

    @Override
    public void update(Pointage pointage) {
        String sql = "UPDATE pointage SET id_ouvrier = ?, date_pointage = ?, statut = ?::statut_pointage WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, pointage.getIdOuvrier());
            stmt.setDate(2, Date.valueOf(pointage.getDatePointage()));
            stmt.setString(3, pointage.getStatut().name());
            stmt.setInt(4, pointage.getId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la mise à jour du pointage ID: " + pointage.getId(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM pointage WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la suppression du pointage ID: " + id, e);
        }
    }

    private Pointage mapResultSetToPointage(ResultSet rs) throws SQLException {
        return new Pointage(
            rs.getInt("id"),
            rs.getInt("id_ouvrier"),
            rs.getDate("date_pointage").toLocalDate(),
            StatutPointage.valueOf(rs.getString("statut"))
        );
    }
}