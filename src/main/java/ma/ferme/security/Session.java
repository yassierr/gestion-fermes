package ma.ferme.security;

/**
 * Memorise l'utilisateur actuellement connecte pendant l'execution de l'application.
 * Les controllers l'utilisent pour connaitre le role et la ferme de l'utilisateur.
 * Le role est conserve sous forme de texte (ADMIN, RESPONSABLE, AGENT_RH).
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public final class Session {

    private static int idUtilisateur;
    private static String nomComplet;
    private static String role;
    private static Integer idFerme;
    private static boolean connecte;

    private Session() {
    }

    /**
     * Ouvre une session apres une connexion reussie.
     *
     * @param id       identifiant de l'utilisateur
     * @param nom      nom complet affiche dans l'interface
     * @param roleName role : ADMIN, RESPONSABLE ou AGENT_RH
     * @param ferme    identifiant de la ferme (null pour un administrateur)
     */
    public static void ouvrir(int id, String nom, String roleName, Integer ferme) {
        idUtilisateur = id;
        nomComplet = nom;
        role = roleName;
        idFerme = ferme;
        connecte = true;
    }

    /** Ferme la session (deconnexion). */
    public static void fermer() {
        idUtilisateur = 0;
        nomComplet = null;
        role = null;
        idFerme = null;
        connecte = false;
    }

    /** @return {@code true} si un utilisateur est connecte */
    public static boolean estConnecte() {
        return connecte;
    }

    /** @return l'identifiant de l'utilisateur connecte */
    public static int getIdUtilisateur() {
        return idUtilisateur;
    }

    /** @return le nom complet de l'utilisateur connecte */
    public static String getNomComplet() {
        return nomComplet;
    }

    /** @return le role : ADMIN, RESPONSABLE ou AGENT_RH */
    public static String getRole() {
        return role;
    }

    /** @return l'identifiant de la ferme de l'utilisateur, ou null pour un administrateur */
    public static Integer getIdFerme() {
        return idFerme;
    }
}