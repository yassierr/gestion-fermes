package ma.ferme.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.Client;
import ma.ferme.model.enums.TypeClient;

/**
 * Acces aux donnees de la table {@code client}.
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public class ClientDao implements GenericDao<Client> {

    private static final String COLS = "id, code, nom, type, telephone, ville, actif";

    /**
     * Insere un client. L'identifiant genere est recopie dans l'objet.
     *
     * @param c le client a enregistrer
     * @throws DaoException si l'insertion echoue (ex. code deja utilise)
     */
    @Override
    public void insert(Client c) {
        String sql = "INSERT INTO client(code, nom, type, telephone, ville, actif) "
                + "VALUES (?, ?, ?::type_client, ?, ?, ?) RETURNING id";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, c.getCode());
            ps.setString(2, c.getNom());
            ps.setString(3, c.getType().name());
            ps.setString(4, c.getTelephone());
            ps.setString(5, c.getVille());
            ps.setBoolean(6, c.isActif());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                c.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new DaoException("Insertion du client impossible : " + e.getMessage(), e);
        }
    }

    /**
     * Recherche un client par identifiant.
     *
     * @param id identifiant du client
     * @return le client, ou un Optional vide
     */
    @Override
    public Optional<Client> findById(int id) {
        return queryOne("SELECT " + COLS + " FROM client WHERE id = ?", id);
    }

    /**
     * Recherche un client par son code unique.
     *
     * @param code le code du client
     * @return le client, ou un Optional vide
     */
    public Optional<Client> findByCode(String code) {
        return queryOne("SELECT " + COLS + " FROM client WHERE code = ?", code);
    }

    /**
     * Retourne tous les clients, tries par nom.
     *
     * @return la liste des clients
     */
    @Override
    public List<Client> findAll() {
        return queryList("SELECT " + COLS + " FROM client ORDER BY nom", null);
    }

    /**
     * Recherche des clients dont le code, le nom ou la ville contient le texte.
     *
     * @param texte texte recherche (insensible a la casse)
     * @return la liste des clients correspondants
     */
    public List<Client> rechercher(String texte) {
        String sql = "SELECT " + COLS + " FROM client "
                + "WHERE code ILIKE ? OR nom ILIKE ? OR COALESCE(ville,'') ILIKE ? ORDER BY nom";
        return queryList(sql, "%" + texte + "%");
    }

    /**
     * Met a jour un client existant.
     *
     * @param c le client avec ses nouvelles valeurs
     * @throws DaoException si la mise a jour echoue
     */
    @Override
    public void update(Client c) {
        String sql = "UPDATE client SET code=?, nom=?, type=?::type_client, telephone=?, ville=?, actif=? WHERE id=?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, c.getCode());
            ps.setString(2, c.getNom());
            ps.setString(3, c.getType().name());
            ps.setString(4, c.getTelephone());
            ps.setString(5, c.getVille());
            ps.setBoolean(6, c.isActif());
            ps.setInt(7, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Mise a jour du client impossible : " + e.getMessage(), e);
        }
    }

    /**
     * Supprime un client.
     *
     * @param id identifiant du client
     * @throws DaoException si le client est reference par une livraison
     */
    @Override
    public void delete(int id) {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM client WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Suppression du client impossible : " + e.getMessage(), e);
        }
    }

    private Optional<Client> queryOne(String sql, Object param) {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Lecture du client impossible : " + e.getMessage(), e);
        }
    }

    private List<Client> queryList(String sql, String likeParam) {
        List<Client> out = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            if (likeParam != null) {
                ps.setString(1, likeParam);
                ps.setString(2, likeParam);
                ps.setString(3, likeParam);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Lecture des clients impossible : " + e.getMessage(), e);
        }
        return out;
    }

    private Client map(ResultSet rs) throws SQLException {
        Client c = new Client();
        c.setId(rs.getInt("id"));
        c.setCode(rs.getString("code"));
        c.setNom(rs.getString("nom"));
        c.setType(TypeClient.valueOf(rs.getString("type")));
        c.setTelephone(rs.getString("telephone"));
        c.setVille(rs.getString("ville"));
        c.setActif(rs.getBoolean("actif"));
        return c;
    }
}