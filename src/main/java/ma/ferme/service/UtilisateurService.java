package ma.ferme.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import ma.ferme.dao.UtilisateurDao;
import ma.ferme.model.Utilisateur;
import ma.ferme.model.enums.RoleUtilisateur;
import ma.ferme.security.PasswordUtil;

/**
 * Logique métier des comptes utilisateurs : écran « Utilisateurs » de l'administrateur
 * (EF-A07 à EF-A09) et changement de mot de passe de n'importe quel rôle (EF-A02).
 * <p>
 * Conventions de la classe :
 * </p>
 * <ul>
 *   <li>les droits sont vérifiés ici, pas seulement par le menu (ENF-02) : chaque méthode
 *       reçoit l'utilisateur qui agit, et lève {@link SecurityException} s'il n'a pas le droit ;</li>
 *   <li>une saisie invalide ou une règle métier violée lève {@link IllegalArgumentException}
 *       avec un message en français, affichable tel quel dans l'interface (EF-T02) ;</li>
 *   <li>les erreurs d'accès à la base restent des {@code DaoException} ;</li>
 *   <li>les mots de passe sont hachés avec {@link PasswordUtil}, jamais stockés en clair (EF-T05).</li>
 * </ul>
 * Règle RG-10 : une ferme a au plus un responsable et un agent RH ; l'administrateur n'a pas de ferme.
 * L'existence de la ferme elle-même est garantie par la clé étrangère de la base.
 */
public class UtilisateurService {

    /** Longueur minimale d'un mot de passe. */
    static final int LONGUEUR_MIN_MOT_DE_PASSE = 6;
    /** Taille de la colonne {@code login}. */
    static final int LONGUEUR_MAX_LOGIN = 50;
    /** Taille de la colonne {@code nom_complet}. */
    static final int LONGUEUR_MAX_NOM = 100;

    private final UtilisateurDao utilisateurDao;

    /** Crée le service avec le DAO par défaut. */
    public UtilisateurService() {
        this(new UtilisateurDao());
    }

    /**
     * Crée le service avec un DAO donné (utile pour les tests).
     *
     * @param utilisateurDao le DAO des utilisateurs
     */
    public UtilisateurService(UtilisateurDao utilisateurDao) {
        this.utilisateurDao = Objects.requireNonNull(utilisateurDao);
    }

    // ------------------------------------------------------------------
    // Consultation (administrateur)
    // ------------------------------------------------------------------

    /**
     * Liste tous les comptes.
     *
     * @param acteur l'utilisateur connecté (doit être administrateur)
     * @return tous les comptes, triés par identifiant
     * @throws SecurityException si l'acteur n'est pas administrateur
     */
    public List<Utilisateur> lister(Utilisateur acteur) {
        exigerAdmin(acteur);
        return utilisateurDao.findAll();
    }

    /**
     * Recherche des comptes par nom ou login, avec filtres facultatifs (EF-A07).
     *
     * @param acteur  l'utilisateur connecté (doit être administrateur)
     * @param terme   texte cherché dans le nom ou le login ({@code null} ou vide = pas de filtre)
     * @param role    filtre sur le rôle ({@code null} = tous)
     * @param idFerme filtre sur la ferme ({@code null} = toutes)
     * @return les comptes correspondants
     * @throws SecurityException si l'acteur n'est pas administrateur
     */
    public List<Utilisateur> rechercher(Utilisateur acteur, String terme, RoleUtilisateur role, Integer idFerme) {
        exigerAdmin(acteur);
        return utilisateurDao.search(terme, role, idFerme);
    }

    /**
     * Nombre total de comptes (compteur « Total des comptes »).
     *
     * @param acteur l'utilisateur connecté (doit être administrateur)
     * @return le nombre de comptes
     * @throws SecurityException si l'acteur n'est pas administrateur
     */
    public long compterTous(Utilisateur acteur) {
        exigerAdmin(acteur);
        return utilisateurDao.count();
    }

    /**
     * Nombre de comptes d'un rôle (compteurs « Responsables » et « Agents RH »).
     *
     * @param acteur l'utilisateur connecté (doit être administrateur)
     * @param role   le rôle compté
     * @return le nombre de comptes de ce rôle
     * @throws SecurityException si l'acteur n'est pas administrateur
     */
    public long compterParRole(Utilisateur acteur, RoleUtilisateur role) {
        exigerAdmin(acteur);
        return utilisateurDao.countByRole(Objects.requireNonNull(role));
    }

    // ------------------------------------------------------------------
    // Création et modification (administrateur)
    // ------------------------------------------------------------------

    /**
     * Crée un compte de responsable de ferme ou d'agent RH (EF-A08). Le compte est créé actif.
     *
     * @param acteur     l'utilisateur connecté (doit être administrateur)
     * @param nomComplet nom complet, obligatoire
     * @param login      identifiant de connexion, unique, sans espace
     * @param motDePasse mot de passe en clair (6 caractères minimum) ; il est haché avant l'enregistrement
     * @param role       RESPONSABLE ou AGENT_RH
     * @param idFerme    ferme à affecter, obligatoire
     * @return le compte créé, avec son identifiant
     * @throws SecurityException        si l'acteur n'est pas administrateur
     * @throws IllegalArgumentException si une saisie est invalide, si le login est déjà pris ou si la
     *                                  ferme a déjà un utilisateur de ce rôle (RG-10)
     */
    public Utilisateur creerCompte(Utilisateur acteur, String nomComplet, String login, String motDePasse,
                                   RoleUtilisateur role, Integer idFerme) {
        exigerAdmin(acteur);
        String nom = validerNom(nomComplet);
        String loginValide = validerLogin(login);
        validerMotDePasse(motDePasse);
        if (role == null) {
            throw new IllegalArgumentException("Le rôle est obligatoire.");
        }
        if (!role.isRattacheAFerme()) {
            throw new IllegalArgumentException(
                    "Seuls un responsable de ferme ou un agent RH peuvent être créés ici.");
        }
        if (idFerme == null) {
            throw new IllegalArgumentException("La ferme à affecter est obligatoire.");
        }
        if (utilisateurDao.existsByLogin(loginValide)) {
            throw new IllegalArgumentException("Ce login est déjà utilisé.");
        }
        if (utilisateurDao.findByFermeAndRole(idFerme, role).isPresent()) {
            throw messagePosteOccupe(role);
        }

        Utilisateur utilisateur = Utilisateur.creer(0, nom, loginValide, PasswordUtil.hash(motDePasse),
                role, true, idFerme);
        utilisateurDao.insert(utilisateur);
        return utilisateur;
    }

    /**
     * Modifie le nom, le login et la ferme d'un compte (EF-A09). Le rôle et le mot de passe ne changent pas.
     *
     * @param acteur     l'utilisateur connecté (doit être administrateur)
     * @param id         identifiant du compte à modifier
     * @param nomComplet nouveau nom complet
     * @param login      nouveau login (peut rester le même)
     * @param idFerme    nouvelle ferme ({@code null} uniquement pour un administrateur)
     * @return le compte modifié
     * @throws SecurityException        si l'acteur n'est pas administrateur
     * @throws IllegalArgumentException si le compte n'existe pas, si une saisie est invalide, si le login
     *                                  est pris par un autre compte ou si la ferme a déjà un utilisateur de ce rôle
     */
    public Utilisateur modifierCompte(Utilisateur acteur, int id, String nomComplet, String login, Integer idFerme) {
        exigerAdmin(acteur);
        Utilisateur utilisateur = utilisateurDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable."));
        String nom = validerNom(nomComplet);
        String loginValide = validerLogin(login);

        Optional<Utilisateur> autreAvecCeLogin = utilisateurDao.findByLogin(loginValide);
        if (autreAvecCeLogin.isPresent() && autreAvecCeLogin.get().getId() != id) {
            throw new IllegalArgumentException("Ce login est déjà utilisé.");
        }
        if (idFerme != null && utilisateur.getRole().isRattacheAFerme()) {
            boolean occupe = utilisateurDao.findByFermeAndRole(idFerme, utilisateur.getRole())
                    .filter(autre -> autre.getId() != id)
                    .isPresent();
            if (occupe) {
                throw messagePosteOccupe(utilisateur.getRole());
            }
        }

        utilisateur.setIdFerme(idFerme); // vérifie RG-10 dans le modèle
        utilisateur.setNomComplet(nom);
        utilisateur.setLogin(loginValide);
        utilisateurDao.update(utilisateur);
        return utilisateur;
    }

    /**
     * Désactive un compte : il ne pourra plus se connecter (ENF-01). Le compte n'est jamais supprimé.
     *
     * @param acteur l'utilisateur connecté (doit être administrateur)
     * @param id     identifiant du compte à désactiver
     * @throws SecurityException        si l'acteur n'est pas administrateur
     * @throws IllegalArgumentException si le compte n'existe pas ou si l'administrateur vise son propre compte
     */
    public void desactiver(Utilisateur acteur, int id) {
        exigerAdmin(acteur);
        if (acteur.getId() == id) {
            throw new IllegalArgumentException("Vous ne pouvez pas désactiver votre propre compte.");
        }
        changerEtat(id, false);
    }

    /**
     * Réactive un compte désactivé.
     *
     * @param acteur l'utilisateur connecté (doit être administrateur)
     * @param id     identifiant du compte à activer
     * @throws SecurityException        si l'acteur n'est pas administrateur
     * @throws IllegalArgumentException si le compte n'existe pas
     */
    public void activer(Utilisateur acteur, int id) {
        exigerAdmin(acteur);
        changerEtat(id, true);
    }

    // ------------------------------------------------------------------
    // Mot de passe (tous les rôles)
    // ------------------------------------------------------------------

    /**
     * Change le mot de passe de l'utilisateur connecté (EF-A02). Disponible pour les trois rôles.
     *
     * @param utilisateur l'utilisateur connecté ; son hash est mis à jour en mémoire
     * @param ancien      mot de passe actuel, en clair
     * @param nouveau     nouveau mot de passe, en clair (6 caractères minimum, différent de l'ancien)
     * @throws SecurityException        si aucun utilisateur n'est connecté
     * @throws IllegalArgumentException si l'ancien mot de passe est faux ou le nouveau invalide
     */
    public void changerMotDePasse(Utilisateur utilisateur, String ancien, String nouveau) {
        if (utilisateur == null) {
            throw new SecurityException("Aucun utilisateur connecté.");
        }
        if (ancien == null || ancien.isEmpty()) {
            throw new IllegalArgumentException("Veuillez saisir votre mot de passe actuel.");
        }
        if (!AuthService.motDePasseCorrect(ancien, utilisateur.getMotDePasseHash())) {
            throw new IllegalArgumentException("Le mot de passe actuel est incorrect.");
        }
        validerMotDePasse(nouveau);
        if (nouveau.equals(ancien)) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit être différent de l'ancien.");
        }

        String hash = PasswordUtil.hash(nouveau);
        if (!utilisateurDao.updateMotDePasse(utilisateur.getId(), hash)) {
            throw new IllegalArgumentException("Compte introuvable.");
        }
        utilisateur.setMotDePasseHash(hash);
    }

    // ------------------------------------------------------------------
    // Outils internes
    // ------------------------------------------------------------------

    private void changerEtat(int id, boolean actif) {
        if (!utilisateurDao.setActif(id, actif)) {
            throw new IllegalArgumentException("Compte introuvable.");
        }
    }

    private static void exigerAdmin(Utilisateur acteur) {
        if (acteur == null || acteur.getRole() != RoleUtilisateur.ADMIN) {
            throw new SecurityException("Action réservée à l'administrateur.");
        }
    }

    private static IllegalArgumentException messagePosteOccupe(RoleUtilisateur role) {
        return new IllegalArgumentException("Cette ferme a déjà un utilisateur avec le rôle « "
                + role.getLibelle() + " ».");
    }

    private static String validerNom(String nomComplet) {
        if (nomComplet == null || nomComplet.isBlank()) {
            throw new IllegalArgumentException("Le nom complet est obligatoire.");
        }
        String nom = nomComplet.trim();
        if (nom.length() > LONGUEUR_MAX_NOM) {
            throw new IllegalArgumentException("Le nom complet ne doit pas dépasser " + LONGUEUR_MAX_NOM
                    + " caractères.");
        }
        return nom;
    }

    private static String validerLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Le login est obligatoire.");
        }
        String valide = login.trim();
        if (valide.length() > LONGUEUR_MAX_LOGIN) {
            throw new IllegalArgumentException("Le login ne doit pas dépasser " + LONGUEUR_MAX_LOGIN
                    + " caractères.");
        }
        if (valide.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Le login ne doit pas contenir d'espace.");
        }
        return valide;
    }

    private static void validerMotDePasse(String motDePasse) {
        if (motDePasse == null || motDePasse.length() < LONGUEUR_MIN_MOT_DE_PASSE) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins "
                    + LONGUEUR_MIN_MOT_DE_PASSE + " caractères.");
        }
    }
}