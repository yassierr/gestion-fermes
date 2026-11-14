package ma.ferme.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.Recolte;

/**
 * Accès à la table {@code recolte} (CRUD et requêtes filtrées sur une ferme).
 * Toutes les requêtes sont paramétrées (ENF-03). Aucune règle métier ici :
 * le stock (RG-07) est vérifié par {@code RecolteService}.
 *
 * @author Mohamed Amine Ait Maait
 * @version 1.0
 */
public class RecolteDao implements GenericDao<Recolte> {

    private static final String COLONNES = "id, date_heure, poids_kg, variete, id_ferme";

    /**
     * Enregistre une récolte. L'identifiant généré par la base est recopié dans l'objet.
     *
     * @param recolte la récolte à enregistrer (son identifiant est renseigné au retour)
     * @throws DaoException si l'insertion échoue (ferme inexistante, erreur SQL...)
     */
    @Override
    public void insert(Recolte recolte) {
        String sql = "INSERT INTO recolte (date_heure, poids_kg, variete, id_ferme) "
                + "VALUES (?, ?, ?, ?) RETURNING id";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(recolte.getDateHeure()));
            ps.setBigDecimal(2, recolte.getPoidsKg());
            ps.setString(3, recolte.getVariete());
            ps.setInt(4, recolte.getIdFerme());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                recolte.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible d'enregistrer la récolte.", e);
        }
    }

    /**
     * Cherche une récolte par son identifiant.
     *
     * @param id identifiant de la récolte
     * @return la récolte, ou vide si elle n'existe pas
     * @throws DaoException en cas d'erreur SQL
     */
    @Override
    public Optional<Recolte> findById(int id) {
        String sql = "SELECT " + COLONNES + " FROM recolte WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(lire(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de lire la récolte " + id + ".", e);
        }
    }

    /**
     * Liste les récoltes de toutes les fermes, les plus récentes d'abord.
     *
     * @return la liste des récoltes
     * @throws DaoException en cas d'erreur SQL
     */
    @Override
    public List<Recolte> findAll() {
        return lister("SELECT " + COLONNES + " FROM recolte ORDER BY date_heure DESC, id DESC", null);
    }

    /**
     * Liste les récoltes d'une ferme, les plus récentes d'abord (EFR07).
     *
     * @param idFerme identifiant de la ferme
     * @return les récoltes de la ferme
     * @throws DaoException en cas d'erreur SQL
     */
    public List<Recolte> findByFerme(int idFerme) {
        return lister("SELECT " + COLONNES + " FROM recolte WHERE id_ferme = ? "
                + "ORDER BY date_heure DESC, id DESC", idFerme);
    }

    /**
     * Met à jour la date, le poids, la variété et la ferme d'une récolte.
     *
     * @param recolte la récolte avec ses nouvelles valeurs (identifiant obligatoire)
     * @throws DaoException si la récolte n'existe pas ou en cas d'erreur SQL
     */
    @Override
    public void update(Recolte recolte) {
        String sql = "UPDATE recolte SET date_heure = ?, poids_kg = ?, variete = ?, id_ferme = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(recolte.getDateHeure()));
            ps.setBigDecimal(2, recolte.getPoidsKg());
            ps.setString(3, recolte.getVariete());
            ps.setInt(4, recolte.getIdFerme());
            ps.setInt(5, recolte.getId());
            if (ps.executeUpdate() == 0) {
                throw new DaoException("Récolte introuvable : " + recolte.getId(), null);
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de modifier la récolte " + recolte.getId() + ".", e);
        }
    }

    /**
     * Supprime une récolte.
     *
     * @param id identifiant de la récolte
     * @throws DaoException si la récolte n'existe pas ou en cas d'erreur SQL
     */
    @Override
    public void delete(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM recolte WHERE id = ?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) {
                throw new DaoException("Récolte introuvable : " + id, null);
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de supprimer la récolte " + id + ".", e);
        }
    }

    /**
     * Somme des poids récoltés par une ferme (zéro si aucune récolte).
     *
     * @param idFerme identifiant de la ferme
     * @return le total en kg
     * @throws DaoException en cas d'erreur SQL
     */
    public BigDecimal totalKg(int idFerme) {
        String sql = "SELECT COALESCE(SUM(poids_kg), 0) FROM recolte WHERE id_ferme = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFerme);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de calculer le total récolté.", e);
        }
    }

    /**
     * Variétés connues d'une ferme : celles de ses récoltes et celles de ses serres,
     * sans doublon, par ordre alphabétique. Alimente la liste déroulante du formulaire.
     *
     * @param idFerme identifiant de la ferme
     * @return les variétés connues
     * @throws DaoException en cas d'erreur SQL
     */
    public List<String> varietes(int idFerme) {
        String sql = "SELECT variete FROM recolte WHERE id_ferme = ? "
                + "UNION SELECT variete FROM serre WHERE id_ferme = ? AND variete IS NOT NULL AND variete <> '' "
                + "ORDER BY 1";
        List<String> varietes = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFerme);
            ps.setInt(2, idFerme);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    varietes.add(rs.getString(1));
                }
            }
            return varietes;
        } catch (SQLException e) {
            throw new DaoException("Impossible de lire les variétés.", e);
        }
    }

    private List<Recolte> lister(String sql, Integer idFerme) {
        List<Recolte> recoltes = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (idFerme != null) {
                ps.setInt(1, idFerme);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    recoltes.add(lire(rs));
                }
            }
            return recoltes;
        } catch (SQLException e) {
            throw new DaoException("Impossible de lister les récoltes.", e);
        }
    }

    private Recolte lire(ResultSet rs) throws SQLException {
        return new Recolte(
                rs.getInt("id"),
                rs.getTimestamp("date_heure").toLocalDateTime(),
                rs.getBigDecimal("poids_kg"),
                rs.getString("variete"),
                rs.getInt("id_ferme"));
    }
}