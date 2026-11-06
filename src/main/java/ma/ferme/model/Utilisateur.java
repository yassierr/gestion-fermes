package ma.ferme.model;

import java.util.List;
import java.util.Objects;

import ma.ferme.model.enums.RoleUtilisateur;

/**
 * Classe abstraite représentant un utilisateur de l'application.
 * <p>
 * Les trois rôles ({@link Administrateur}, {@link ResponsableFerme}, {@link AgentRH})
 * partagent les mêmes données ; en base ils sont stockés dans une seule table
 * {@code utilisateur} avec une colonne {@code role}.
 * </p>
 * <p>
 * Cette classe est un simple objet du domaine : elle ne fait aucun accès à la base
 * et n'appelle ni DAO ni service. L'authentification est faite par {@code AuthService}
 * et la persistance par {@code UtilisateurDao}.
 * </p>
 * Règles portées par la classe :
 * <ul>
 *   <li>RG-10 : l'administrateur n'est rattaché à aucune ferme ; un responsable ou
 *       un agent RH est rattaché à exactement une ferme ;</li>
 *   <li>ENF-01 : un compte désactivé ne peut pas se connecter ;</li>
 *   <li>EF-T05 : seul le hash du mot de passe est conservé, jamais le mot de passe en clair.</li>
 * </ul>
 */
public abstract class Utilisateur {

    /** Identifiant en base ; 0 tant que l'utilisateur n'est pas enregistré. */
    private int id;
    private String nomComplet;
    private String login;
    /** Hash BCrypt du mot de passe (colonne {@code mot_de_passe_hash}). */
    private String motDePasseHash;
    private final RoleUtilisateur role;
    private boolean actif;
    /** Identifiant de la ferme ; {@code null} pour l'administrateur. */
    private Integer idFerme;

    /**
     * Constructeur complet, utilisé par les sous-classes.
     *
     * @param id             identifiant en base (0 si nouvel utilisateur)
     * @param nomComplet     nom complet, non vide
     * @param login          identifiant de connexion, non vide
     * @param motDePasseHash hash du mot de passe
     * @param role           rôle de l'utilisateur, non nul
     * @param actif          {@code true} si le compte est actif
     * @param idFerme        ferme de rattachement ({@code null} pour ADMIN, obligatoire sinon)
     * @throws IllegalArgumentException si une donnée est invalide ou si la règle RG-10 n'est pas respectée
     */
    protected Utilisateur(int id, String nomComplet, String login, String motDePasseHash,
                          RoleUtilisateur role, boolean actif, Integer idFerme) {
        this.role = Objects.requireNonNull(role, "Le rôle est obligatoire.");
        this.id = id;
        this.actif = actif;
        setNomComplet(nomComplet);
        setLogin(login);
        this.motDePasseHash = motDePasseHash;
        verifierRattachement(role, idFerme);
        this.idFerme = idFerme;
    }

    /**
     * Fabrique un utilisateur du bon type à partir d'un rôle.
     * Pratique pour {@code UtilisateurDao} lors de la lecture d'une ligne de la table.
     *
     * @param id             identifiant en base
     * @param nomComplet     nom complet
     * @param login          login
     * @param motDePasseHash hash du mot de passe
     * @param role           rôle lu en base
     * @param actif          état du compte
     * @param idFerme        ferme de rattachement ({@code null} pour ADMIN)
     * @return une instance de {@link Administrateur}, {@link ResponsableFerme} ou {@link AgentRH}
     * @throws IllegalArgumentException si les données sont invalides
     */
    public static Utilisateur creer(int id, String nomComplet, String login, String motDePasseHash,
                                    RoleUtilisateur role, boolean actif, Integer idFerme) {
        Objects.requireNonNull(role, "Le rôle est obligatoire.");
        return switch (role) {
            case ADMIN -> new Administrateur(id, nomComplet, login, motDePasseHash, actif);
            case RESPONSABLE -> new ResponsableFerme(id, nomComplet, login, motDePasseHash, actif,
                    exigerFerme(idFerme));
            case AGENT_RH -> new AgentRH(id, nomComplet, login, motDePasseHash, actif,
                    exigerFerme(idFerme));
        };
    }

    // ------------------------------------------------------------------
    // Comportement propre à chaque rôle
    // ------------------------------------------------------------------

    /**
     * Libellés du menu latéral du rôle, dans l'ordre d'affichage des maquettes.
     * Le contrôleur s'en sert pour construire la barre latérale après la connexion (EF-T01).
     *
     * @return la liste non modifiable des entrées du menu
     */
    public abstract List<String> afficherMenu();

    // ------------------------------------------------------------------
    // Comportement commun
    // ------------------------------------------------------------------

    /**
     * Indique si le compte peut ouvrir une session (ENF-01).
     *
     * @return {@code true} si le compte est actif
     */
    public boolean peutSeConnecter() {
        return actif;
    }

    /**
     * Indique si l'utilisateur peut consulter les données de toutes les fermes (EF-T04).
     *
     * @return {@code true} uniquement pour l'administrateur
     */
    public boolean voitToutesLesFermes() {
        return !role.isRattacheAFerme();
    }

    // ------------------------------------------------------------------
    // Getters / setters
    // ------------------------------------------------------------------

    /** @return l'identifiant en base (0 si non enregistré) */
    public int getId() {
        return id;
    }

    /** @param id identifiant attribué par la base après l'insertion */
    public void setId(int id) {
        this.id = id;
    }

    /** @return le nom complet */
    public String getNomComplet() {
        return nomComplet;
    }

    /**
     * @param nomComplet nouveau nom complet
     * @throws IllegalArgumentException s'il est vide
     */
    public void setNomComplet(String nomComplet) {
        if (nomComplet == null || nomComplet.isBlank()) {
            throw new IllegalArgumentException("Le nom complet est obligatoire.");
        }
        this.nomComplet = nomComplet.trim();
    }

    /** @return le login */
    public String getLogin() {
        return login;
    }

    /**
     * @param login nouveau login
     * @throws IllegalArgumentException s'il est vide
     */
    public void setLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Le login est obligatoire.");
        }
        this.login = login.trim();
    }

    /** @return le hash BCrypt du mot de passe */
    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    /** @param motDePasseHash nouveau hash (jamais un mot de passe en clair) */
    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    /** @return le rôle (non modifiable après création) */
    public RoleUtilisateur getRole() {
        return role;
    }

    /** @return {@code true} si le compte est actif */
    public boolean isActif() {
        return actif;
    }

    /** @param actif {@code true} pour activer le compte, {@code false} pour le désactiver */
    public void setActif(boolean actif) {
        this.actif = actif;
    }

    /** @return l'identifiant de la ferme de rattachement, ou {@code null} pour l'administrateur */
    public Integer getIdFerme() {
        return idFerme;
    }

    /**
     * Change la ferme de rattachement.
     *
     * @param idFerme nouvelle ferme ({@code null} uniquement pour l'administrateur)
     * @throws IllegalArgumentException si la règle RG-10 n'est pas respectée
     */
    public void setIdFerme(Integer idFerme) {
        verifierRattachement(role, idFerme);
        this.idFerme = idFerme;
    }

    // ------------------------------------------------------------------
    // Outils internes
    // ------------------------------------------------------------------

    private static void verifierRattachement(RoleUtilisateur role, Integer idFerme) {
        if (role.isRattacheAFerme() && idFerme == null) {
            throw new IllegalArgumentException(
                    "Un " + role.getLibelle().toLowerCase() + " doit être rattaché à une ferme.");
        }
        if (!role.isRattacheAFerme() && idFerme != null) {
            throw new IllegalArgumentException(
                    "L'administrateur ne peut pas être rattaché à une ferme.");
        }
    }

    private static int exigerFerme(Integer idFerme) {
        if (idFerme == null) {
            throw new IllegalArgumentException("La ferme de rattachement est obligatoire.");
        }
        return idFerme;
    }

    // ------------------------------------------------------------------
    // Object
    // ------------------------------------------------------------------

    /** Deux utilisateurs sont égaux s'ils ont le même login (unique en base). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Utilisateur autre)) {
            return false;
        }
        return Objects.equals(login, autre.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login);
    }

    /** Ne contient volontairement pas le hash du mot de passe. */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id
                + ", nomComplet='" + nomComplet + '\''
                + ", login='" + login + '\''
                + ", role=" + role
                + ", actif=" + actif
                + ", idFerme=" + idFerme + '}';
    }
}