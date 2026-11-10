
package ma.ferme.dao;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.Serre;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO responsable des operations CRUD sur les serres.
 */
public class SerreDao implements GenericDao<Serre> {

    @Override
    public void insert(Serre serre) {
        String sql = """
                INSERT INTO serre
                (nom, surface_m2, structure, irrigation, variete, id_ferme)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, serre.getNom());
            ps.setDouble(2, serre.getSurfaceM2());
            ps.setString(3, serre.getTypeAbri());
            ps.setString(4, serre.getIrrigation());
            ps.setString(5, serre.getVariete());
            ps.setInt(6, serre.getIdFerme());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    serre.setId(rs.getInt(1));
                } else {
                    throw new DaoException(
                            "Impossible de recuperer l'ID de la serre", null);
                }
            }

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de l'insertion de la serre", e);
        }
    }

    @Override
    public Optional<Serre> findById(int id) {
        String sql = """
                SELECT id, nom, surface_m2, structure,
                       irrigation, variete, id_ferme
                FROM serre
                WHERE id = ?
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapSerre(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la recherche de la serre", e);
        }
    }

    @Override
    public List<Serre> findAll() {
        String sql = """
                SELECT id, nom, surface_m2, structure,
                       irrigation, variete, id_ferme
                FROM serre
                ORDER BY id
                """;

        List<Serre> serres = new ArrayList<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                serres.add(mapSerre(rs));
            }

            return serres;

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la recuperation des serres", e);
        }
    }

    @Override
    public void update(Serre serre) {
        String sql = """
                UPDATE serre
                SET nom = ?, surface_m2 = ?, structure = ?,
                    irrigation = ?, variete = ?, id_ferme = ?
                WHERE id = ?
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, serre.getNom());
            ps.setDouble(2, serre.getSurfaceM2());
            ps.setString(3, serre.getTypeAbri());
            ps.setString(4, serre.getIrrigation());
            ps.setString(5, serre.getVariete());
            ps.setInt(6, serre.getIdFerme());
            ps.setInt(7, serre.getId());

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new DaoException(
                        "Aucune serre trouvee avec l'ID " + serre.getId(), null);
            }

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la modification de la serre", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM serre WHERE id = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new DaoException(
                        "Aucune serre trouvee avec l'ID " + id, null);
            }

        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la suppression de la serre", e);
        }
    }

    /**
     * Convertit une ligne SQL en objet Serre.
     */
    private Serre mapSerre(ResultSet rs) throws SQLException {
        return new Serre(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getDouble("surface_m2"),
                rs.getString("variete"),
                rs.getString("structure"),
                rs.getString("irrigation"),
                rs.getInt("id_ferme")
        );
    }
}
