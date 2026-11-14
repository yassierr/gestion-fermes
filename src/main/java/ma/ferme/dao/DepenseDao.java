package ma.ferme.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.Depense;
import ma.ferme.model.enums.CategorieDepense;

/**
 * Accès à la table {@code depense}. La catégorie est un type énumérée PostgreSQL :
 * écriture avec {@code ?::categorie_depense}, lecture avec {@code valueOf}.
 * Requêtes paramétrées uniquement (ENF-03).
 *
 * @author Mohamed Amine Ait Maait
 * @version 1.0
 */
public class DepenseDao implements GenericDao<Depense> {

    private static final String COLONNES = "id, date_depense, categorie, montant, description, id_ferme";

    /**
     * Enregistre une dépense. L'identifiant généré par la base est recopié dans l'objet.
     *
     * @param depense la dépense à enregistrer (son identifiant est renseigné au retour)
     * @throws DaoException si l'insertion échoue
     */
    @Override
    public void insert(Depense depense) {
        String sql = "INSERT INTO depense (date_depense, categorie, montant, description, id_ferme) "
                + "VALUES (?, ?::categorie_depense, ?, ?, ?) RETURNING id";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(depense.getDateDepense()));
            ps.setString(2, depense.getCategorie().name());
            ps.setBigDecimal(3, depense.getMontant());
            ps.setString(4, depense.getDescription());
            ps.setInt(5, depense.getIdFerme());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                depense.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible d'enregistrer la dépense.", e);
        }
    }

    /**
     * Cherche une dépense par son identifiant.
     *
     * @param id identifiant de la dépense
     * @return la dépense, ou vide si elle n'existe pas
     * @throws DaoException en cas d'erreur SQL
     */
    @Override
    public Optional<Depense> findById(int id) {
        String sql = "SELECT " + COLONNES + " FROM depense WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(lire(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de lire la dépense " + id + ".", e);
        }
    }

    /**
     * Liste les dépenses de toutes les fermes, les plus récentes d'abord.
     *
     * @return la liste des dépenses
     * @throws DaoException en cas d'erreur SQL
     */
    @Override
    public List<Depense> findAll() {
        return lister("SELECT " + COLONNES + " FROM depense ORDER BY date_depense DESC, id DESC", null);
    }

    /**
     * Liste les dépenses d'une ferme, les plus récentes d'abord (EFR10).
     *
     * @param idFerme identifiant de la ferme
     * @return les dépenses de la ferme
     * @throws DaoException en cas d'erreur SQL
     */
    public List<Depense> findByFerme(int idFerme) {
        return lister("SELECT " + COLONNES + " FROM depense WHERE id_ferme = ? "
                + "ORDER BY date_depense DESC, id DESC", idFerme);
    }

    /**
     * Met à jour une dépense.
     *
     * @param depense la dépense avec ses nouvelles valeurs (identifiant obligatoire)
     * @throws DaoException si la dépense n'existe pas ou en cas d'erreur SQL
     */
    @Override
    public void update(Depense depense) {
        String sql = "UPDATE depense SET date_depense = ?, categorie = ?::categorie_depense, "
                + "montant = ?, description = ?, id_ferme = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(depense.getDateDepense()));
            ps.setString(2, depense.getCategorie().name());
            ps.setBigDecimal(3, depense.getMontant());
            ps.setString(4, depense.getDescription());
            ps.setInt(5, depense.getIdFerme());
            ps.setInt(6, depense.getId());
            if (ps.executeUpdate() == 0) {
                throw new DaoException("Dépense introuvable : " + depense.getId(), null);
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de modifier la dépense " + depense.getId() + ".", e);
        }
    }

    /**
     * Supprime une dépense.
     *
     * @param id identifiant de la dépense
     * @throws DaoException si la dépense n'existe pas ou en cas d'erreur SQL
     */
    @Override
    public void delete(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM depense WHERE id = ?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) {
                throw new DaoException("Dépense introuvable : " + id, null);
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de supprimer la dépense " + id + ".", e);
        }
    }

    /**
     * Total des dépenses d'une ferme entre deux dates incluses.
     * Une borne {@code null} signifie « sans limite de ce côté ».
     *
     * @param idFerme identifiant de la ferme
     * @param debut   première date prise en compte, ou {@code null}
     * @param fin     dernière date prise en compte, ou {@code null}
     * @return le total en MAD (zéro si aucune dépense)
     * @throws DaoException en cas d'erreur SQL
     */
    public BigDecimal total(int idFerme, LocalDate debut, LocalDate fin) {
        String sql = "SELECT COALESCE(SUM(montant), 0) FROM depense WHERE id_ferme = ? "
                + "AND (?::date IS NULL OR date_depense >= ?::date) "
                + "AND (?::date IS NULL OR date_depense <= ?::date)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFerme);
            lier(ps, 2, debut);
            lier(ps, 4, fin);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new DaoException("Impossible de calculer le total des dépenses.", e);
        }
    }

    /** Lie une date (ou NULL) à deux paramètres consécutifs. */
    private static void lier(PreparedStatement ps, int index, LocalDate date) throws SQLException {
        Date valeur = date == null ? null : Date.valueOf(date);
        ps.setDate(index, valeur);
        ps.setDate(index + 1, valeur);
    }

    private List<Depense> lister(String sql, Integer idFerme) {
        List<Depense> depenses = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (idFerme != null) {
                ps.setInt(1, idFerme);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    depenses.add(lire(rs));
                }
            }
            return depenses;
        } catch (SQLException e) {
            throw new DaoException("Impossible de lister les dépenses.", e);
        }
    }

    private Depense lire(ResultSet rs) throws SQLException {
        return new Depense(
                rs.getInt("id"),
                rs.getDate("date_depense").toLocalDate(),
                CategorieDepense.valueOf(rs.getString("categorie")),
                rs.getBigDecimal("montant"),
                rs.getString("description"),
                rs.getInt("id_ferme"));
    }
}