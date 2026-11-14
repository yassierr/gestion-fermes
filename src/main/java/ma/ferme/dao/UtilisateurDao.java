package ma.ferme.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.Utilisateur;
import ma.ferme.model.enums.RoleUtilisateur;

/**
 * Accès aux données de la table {@code utilisateur}.
 * <p>
 * Les trois rôles (administrateur, responsable, agent RH) sont dans la même table ;
 * chaque ligne est convertie en la bonne sous-classe par {@link Utilisateur#creer}.
 * </p>
 * Respecte le contrat commun du projet : uniquement {@code DatabaseConnection.getConnection()},
 * des {@link PreparedStatement} dans des try-with-resources (ENF-03), et aucune
 * {@link SQLException} ne remonte : tout est converti en {@link DaoException}.
 * <p>
 * Le mot de passe est toujours stocké sous forme de hash (colonne {@code mot_de_passe_hash}) ;
 * le hachage lui-même est fait par {@code PasswordUtil}, pas par ce DAO.
 * </p>
 * <p>
 * Le cahier des charges demande de <em>désactiver</em> un compte, pas de le supprimer
 * (EF-A09) : le service doit utiliser {@link #setActif(int, boolean)}. {@link #delete(int)}
 * existe pour respecter {@link GenericDao} et pour nettoyer les données de test.
 * </p>
 */
public class UtilisateurDao implements GenericDao<Utilisateur> {

    private static final String COLONNES =
            "id, nom_complet, login, mot_de_passe_hash, role, actif, id_ferme";

    private static final String SQL_INSERT =
            "INSERT INTO utilisateur (nom_complet, login, mot_de_passe_hash, role, actif, id_ferme) "
                    + "VALUES (?, ?, ?, ?::role_utilisateur, ?, ?) RETURNING id";

    // Le rôle n'est volontairement pas modifiable (il est final dans le modèle).
    private static final String SQL_UPDATE =
            "UPDATE utilisateur SET nom_complet = ?, login = ?, mot_de_passe_hash = ?, "
                    + "actif = ?, id_ferme = ? WHERE id = ?";

    private static final String SQL_SELECT = "SELECT " + COLONNES + " FROM utilisateur";

    // ------------------------------------------------------------------
    // GenericDao
    // ------------------------------------------------------------------

    /**
     * Enregistre un nouvel utilisateur et renseigne son identifiant généré.
     *
     * @param u l'utilisateur à enregistrer (le hash du mot de passe doit être déjà calculé) ;
     *          son {@code id} est renseigné avec la valeur attribuée par la base
     * @throws DaoException si le login existe déjà, si la ferme a déjà un responsable / agent RH
     *                      de ce rôle, si la ferme n'existe pas ou en cas d'erreur SQL
     */
    @Override
    public void insert(Utilisateur u) {
        Objects.requireNonNull(u, "L'utilisateur est obligatoire.");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            ps.setString(1, u.getNomComplet());
            ps.setString(2, u.getLogin());
            ps.setString(3, u.getMotDePasseHash());
            ps.setString(4, u.getRole().name());
            ps.setBoolean(5, u.isActif());
            lierIdFerme(ps, 6, u.getIdFerme());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                u.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw erreur("l'insertion de l'utilisateur « " + u.getLogin() + " »", e);
        }
    }

    /**
     * Cherche un utilisateur par son identifiant.
     *
     * @param id identifiant en base
     * @return l'utilisateur, ou {@link Optional#empty()} s'il n'existe pas
     * @throws DaoException en cas d'erreur SQL
     */
    @Override
    public Optional<Utilisateur> findById(int id) {
        return premier(SQL_SELECT + " WHERE id = ?", List.of(id), "la recherche par id");
    }

    /**
     * Liste tous les utilisateurs, triés par identifiant.
     *
     * @return la liste (vide si la table est vide)
     * @throws DaoException en cas d'erreur SQL
     */
    @Override
    public List<Utilisateur> findAll() {
        return lister(SQL_SELECT + " ORDER BY id", List.of(), "la lecture des utilisateurs");
    }

    /**
     * Met à jour le nom, le login, le hash, l'état actif et la ferme d'un utilisateur.
     * Le rôle ne change jamais.
     *
     * @param u l'utilisateur déjà enregistré (id &gt; 0)
     * @throws DaoException si l'id n'existe pas, si le nouveau login existe déjà, si la ferme
     *                      n'existe pas ou a déjà un utilisateur de ce rôle, ou en cas d'erreur SQL
     */
    @Override
    public void update(Utilisateur u) {
        Objects.requireNonNull(u, "L'utilisateur est obligatoire.");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, u.getNomComplet());
            ps.setString(2, u.getLogin());
            ps.setString(3, u.getMotDePasseHash());
            ps.setBoolean(4, u.isActif());
            lierIdFerme(ps, 5, u.getIdFerme());
            ps.setInt(6, u.getId());
            if (ps.executeUpdate() == 0) {
                throw new DaoException("Utilisateur introuvable (id=" + u.getId()
                        + ") : modification impossible.", null);
            }
        } catch (SQLException e) {
            throw erreur("la modification de l'utilisateur « " + u.getLogin() + " »", e);
        }
    }

    /**
     * Supprime définitivement un utilisateur. À éviter dans l'application (préférer
     * {@link #setActif(int, boolean)}) ; échoue si des ventes ou des reçus de paie le référencent.
     *
     * Ne fait rien si l'identifiant n'existe pas.
     *
     * @param id identifiant en base
     * @throws DaoException si l'utilisateur est référencé ailleurs ou en cas d'erreur SQL
     */
    @Override
    public void delete(int id) {
        modifier("DELETE FROM utilisateur WHERE id = ?", List.of(id),
                "la suppression de l'utilisateur " + id);
    }

    // ------------------------------------------------------------------
    // Méthodes propres aux utilisateurs
    // ------------------------------------------------------------------

    /**
     * Cherche un compte par login (utilisé par {@code AuthService} à la connexion).
     * Le résultat contient aussi les comptes désactivés : c'est au service de refuser la connexion.
     *
     * @param login identifiant de connexion
     * @return l'utilisateur, ou {@link Optional#empty()} si le login est inconnu
     * @throws DaoException en cas d'erreur SQL
     */
    public Optional<Utilisateur> findByLogin(String login) {
        return premier(SQL_SELECT + " WHERE login = ?", List.of(login), "la recherche par login");
    }

    /**
     * Indique si un login est déjà pris.
     *
     * @param login identifiant de connexion
     * @return {@code true} si un compte porte ce login
     * @throws DaoException en cas d'erreur SQL
     */
    public boolean existsByLogin(String login) {
        return findByLogin(login).isPresent();
    }

    /**
     * Recherche pour l'écran « Utilisateurs » (EF-A07) : par nom ou login, avec filtres
     * facultatifs sur le rôle et la ferme.
     *
     * @param terme   texte cherché dans le nom complet ou le login (ignoré s'il est vide ou nul)
     * @param role    filtre sur le rôle ({@code null} = tous)
     * @param idFerme filtre sur la ferme ({@code null} = toutes)
     * @return les utilisateurs correspondants, triés par identifiant
     * @throws DaoException en cas d'erreur SQL
     */
    public List<Utilisateur> search(String terme, RoleUtilisateur role, Integer idFerme) {
        StringBuilder sql = new StringBuilder(SQL_SELECT).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (role != null) {
            sql.append(" AND role = ?::role_utilisateur");
            params.add(role.name());
        }
        if (idFerme != null) {
            sql.append(" AND id_ferme = ?");
            params.add(idFerme);
        }
        if (terme != null && !terme.isBlank()) {
            sql.append(" AND (nom_complet ILIKE ? OR login ILIKE ?)");
            String motif = "%" + terme.trim() + "%";
            params.add(motif);
            params.add(motif);
        }
        sql.append(" ORDER BY id");
        return lister(sql.toString(), params, "la recherche d'utilisateurs");
    }

    /**
     * Cherche le responsable ou l'agent RH d'une ferme (RG-10 : un seul de chaque par ferme).
     *
     * @param idFerme identifiant de la ferme
     * @param role    RESPONSABLE ou AGENT_RH
     * @return l'utilisateur, ou {@link Optional#empty()} si le poste est vacant
     * @throws DaoException en cas d'erreur SQL
     */
    public Optional<Utilisateur> findByFermeAndRole(int idFerme, RoleUtilisateur role) {
        return premier(SQL_SELECT + " WHERE id_ferme = ? AND role = ?::role_utilisateur ORDER BY id LIMIT 1",
                List.of(idFerme, role.name()), "la recherche par ferme et rôle");
    }

    /**
     * Nombre total de comptes (compteur « Total des comptes »).
     *
     * @return le nombre de lignes de la table
     * @throws DaoException en cas d'erreur SQL
     */
    public long count() {
        return compter("SELECT COUNT(*) FROM utilisateur", List.of());
    }

    /**
     * Nombre de comptes d'un rôle (compteurs « Responsables » et « Agents RH »).
     *
     * @param role le rôle
     * @return le nombre de comptes de ce rôle
     * @throws DaoException en cas d'erreur SQL
     */
    public long countByRole(RoleUtilisateur role) {
        return compter("SELECT COUNT(*) FROM utilisateur WHERE role = ?::role_utilisateur",
                List.of(role.name()));
    }

    /**
     * Remplace le hash du mot de passe (changement de mot de passe, EF-A02).
     *
     * @param id   identifiant de l'utilisateur
     * @param hash nouveau hash, déjà calculé par {@code PasswordUtil}
     * @return {@code true} si le compte existe
     * @throws DaoException en cas d'erreur SQL
     */
    public boolean updateMotDePasse(int id, String hash) {
        return modifier("UPDATE utilisateur SET mot_de_passe_hash = ? WHERE id = ?", List.of(hash, id),
                "le changement de mot de passe de l'utilisateur " + id);
    }

    /**
     * Active ou désactive un compte (EF-A09, ENF-01).
     *
     * @param id    identifiant de l'utilisateur
     * @param actif {@code true} pour activer, {@code false} pour désactiver
     * @return {@code true} si le compte existe
     * @throws DaoException en cas d'erreur SQL
     */
    public boolean setActif(int id, boolean actif) {
        return modifier("UPDATE utilisateur SET actif = ? WHERE id = ?", List.of(actif, id),
                "le changement d'état de l'utilisateur " + id);
    }

    // ------------------------------------------------------------------
    // Outils internes
    // ------------------------------------------------------------------

    /** Convertit une ligne du ResultSet en Administrateur, ResponsableFerme ou AgentRH selon le rôle. */
    private Utilisateur mapper(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        try {
            return Utilisateur.creer(
                    id,
                    rs.getString("nom_complet"),
                    rs.getString("login"),
                    rs.getString("mot_de_passe_hash"),
                    RoleUtilisateur.valueOf(rs.getString("role")),
                    rs.getBoolean("actif"),
                    rs.getObject("id_ferme", Integer.class));
        } catch (IllegalArgumentException e) {
            throw new DaoException("Ligne incohérente dans la table utilisateur (id=" + id + ").", e);
        }
    }

    private List<Utilisateur> lister(String sql, List<Object> params, String contexte) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            lier(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Utilisateur> resultat = new ArrayList<>();
                while (rs.next()) {
                    resultat.add(mapper(rs));
                }
                return resultat;
            }
        } catch (SQLException e) {
            throw erreur(contexte, e);
        }
    }

    private Optional<Utilisateur> premier(String sql, List<Object> params, String contexte) {
        List<Utilisateur> liste = lister(sql, params, contexte);
        return liste.isEmpty() ? Optional.empty() : Optional.of(liste.get(0));
    }

    private long compter(String sql, List<Object> params) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            lier(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw erreur("le comptage des utilisateurs", e);
        }
    }

    private boolean modifier(String sql, List<Object> params, String contexte) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            lier(ps, params);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw erreur(contexte, e);
        }
    }

    private static void lier(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private static void lierIdFerme(PreparedStatement ps, int index, Integer idFerme) throws SQLException {
        if (idFerme == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, idFerme);
        }
    }

    /** Transforme une SQLException en DaoException avec un message compréhensible. */
    private static DaoException erreur(String contexte, SQLException e) {
        String etat = e.getSQLState();
        if ("23505".equals(etat)) {
            return new DaoException("Doublon interdit pendant " + contexte
                    + " : login déjà utilisé, ou la ferme a déjà un utilisateur de ce rôle.", e);
        }
        if ("23503".equals(etat)) {
            return new DaoException("Référence invalide (ferme inexistante ou utilisateur encore utilisé) "
                    + "pendant " + contexte + ".", e);
        }
        return new DaoException("Erreur SQL pendant " + contexte + ".", e);
    }
}