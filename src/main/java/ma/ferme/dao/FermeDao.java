
package ma.ferme.dao;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.Ferme;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO responsable des operations CRUD sur les fermes.
 */
public class FermeDao implements GenericDao<Ferme> {

    @Override
    public void insert(Ferme ferme) {
        String sql = """
                INSERT INTO ferme (nom, localisation)
                VALUES (?, ?)
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, ferme.getNom());
            ps.setString(2, ferme.getLocalisation());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    ferme.setId(rs.getInt(1));
                } else {
                    throw new DaoException(
                            "Impossible de recuperer l'ID de la ferme", null);
                }
            }

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de l'insertion de la ferme", e);
        }
    }

    @Override
    public Optional<Ferme> findById(int id) {
        String sql = """
                SELECT id, nom, localisation
                FROM ferme
                WHERE id = ?
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapFerme(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la recherche de la ferme", e);
        }
    }

    @Override
    public List<Ferme> findAll() {
        String sql = """
                SELECT id, nom, localisation
                FROM ferme
                ORDER BY id
                """;

        List<Ferme> fermes = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                fermes.add(mapFerme(rs));
            }

            return fermes;

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la recuperation des fermes", e);
        }
    }

    @Override
    public void update(Ferme ferme) {
        String sql = """
                UPDATE ferme
                SET nom = ?, localisation = ?
                WHERE id = ?
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, ferme.getNom());
            ps.setString(2, ferme.getLocalisation());
            ps.setInt(3, ferme.getId());

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new DaoException(
                        "Aucune ferme trouvee avec l'ID " + ferme.getId(), null);
            }

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la modification de la ferme", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM ferme WHERE id = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new DaoException(
                        "Aucune ferme trouvee avec l'ID " + id, null);
            }

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la suppression de la ferme", e);
        }
    }

    /**
     * Convertit une ligne SQL en objet Ferme.
     */
    private Ferme mapFerme(ResultSet rs) throws SQLException {
        return new Ferme(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("localisation")
        );
    }
}
