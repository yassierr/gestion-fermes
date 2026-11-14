package ma.ferme.service;

import java.util.Objects;

import ma.ferme.dao.UtilisateurDao;
import ma.ferme.model.Utilisateur;
import ma.ferme.security.PasswordUtil;

/**
 * Service d'authentification : vérifie les identifiants saisis à l'écran de connexion
 * (voir le diagramme de séquence « Connexion et ouverture du menu selon le rôle »).
 * <p>
 * Règles appliquées :
 * </p>
 * <ul>
 *   <li>login et mot de passe obligatoires (EF-T02) ;</li>
 *   <li>mot de passe comparé au hash BCrypt, jamais en clair (EF-T05) ;</li>
 *   <li>un compte désactivé ne peut pas se connecter (ENF-01, EF-R01) ;</li>
 *   <li>un login inconnu et un mauvais mot de passe donnent le même message, pour ne pas
 *       révéler quels logins existent.</li>
 * </ul>
 */
public class AuthService {

    /** Message commun au login inconnu et au mauvais mot de passe. */
    static final String MSG_IDENTIFIANTS_INCORRECTS = "Identifiants incorrects.";

    private final UtilisateurDao utilisateurDao;

    /** Crée le service avec le DAO par défaut. */
    public AuthService() {
        this(new UtilisateurDao());
    }

    /**
     * Crée le service avec un DAO donné (utile pour les tests).
     *
     * @param utilisateurDao le DAO des utilisateurs
     */
    public AuthService(UtilisateurDao utilisateurDao) {
        this.utilisateurDao = Objects.requireNonNull(utilisateurDao);
    }

    /**
     * Vérifie un couple login / mot de passe.
     *
     * @param login      identifiant saisi
     * @param motDePasse mot de passe saisi, en clair
     * @return l'utilisateur connecté ({@code Administrateur}, {@code ResponsableFerme} ou {@code AgentRH})
     * @throws AuthException                  si un champ est vide, si les identifiants sont incorrects
     *                                        ou si le compte est désactivé
     * @throws ma.ferme.dao.DaoException      en cas d'erreur d'accès à la base
     */
    public Utilisateur authentifier(String login, String motDePasse) throws AuthException {
        if (login == null || login.isBlank()) {
            throw new AuthException("Veuillez saisir votre identifiant.");
        }
        if (motDePasse == null || motDePasse.isEmpty()) {
            throw new AuthException("Veuillez saisir votre mot de passe.");
        }

        Utilisateur utilisateur = utilisateurDao.findByLogin(login.trim()).orElse(null);
        if (utilisateur == null || !motDePasseCorrect(motDePasse, utilisateur.getMotDePasseHash())) {
            throw new AuthException(MSG_IDENTIFIANTS_INCORRECTS);
        }
        // Vérifié après le mot de passe : on ne révèle l'état d'un compte qu'à son propriétaire.
        if (!utilisateur.peutSeConnecter()) {
            throw new AuthException("Ce compte est désactivé. Contactez l'administrateur.");
        }
        return utilisateur;
    }

    /**
     * Compare un mot de passe en clair à un hash BCrypt. Un hash absent ou mal formé
     * (par exemple une valeur de test comme « dummy_hash » dans data.sql) donne {@code false}
     * au lieu de lever une exception.
     * <p>
     * Seul endroit qui appelle {@code PasswordUtil.verifier} : à adapter si le nom de la méthode diffère.
     * </p>
     *
     * @param motDePasseClair le mot de passe saisi
     * @param hash            le hash stocké en base
     * @return {@code true} si le mot de passe correspond au hash
     */
    static boolean motDePasseCorrect(String motDePasseClair, String hash) {
        if (hash == null || motDePasseClair == null) {
            return false;
        }
        try {
            return PasswordUtil.verifier(motDePasseClair, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}