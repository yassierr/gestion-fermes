package ma.ferme.model.enums;

/**
 * Rôles possibles d'un utilisateur de l'application.
 * <p>
 * Les noms des constantes correspondent exactement aux valeurs de l'énumération
 * PostgreSQL {@code role_utilisateur} : on peut donc les relire avec
 * {@code RoleUtilisateur.valueOf(rs.getString("role"))} et les écrire avec
 * {@code ?::role_utilisateur}.
 * </p>
 */
public enum RoleUtilisateur {

    /** Supervise toutes les fermes ; n'est rattaché à aucune ferme (RG-10). */
    ADMIN("Administrateur", false),

    /** Gère les serres, récoltes et dépenses d'une seule ferme (RG-10). */
    RESPONSABLE("Responsable de ferme", true),

    /** Gère les ouvriers, le pointage et la paie d'une seule ferme (RG-10). */
    AGENT_RH("Agent RH", true);

    private final String libelle;
    private final boolean rattacheAFerme;

    RoleUtilisateur(String libelle, boolean rattacheAFerme) {
        this.libelle = libelle;
        this.rattacheAFerme = rattacheAFerme;
    }

    /**
     * Libellé lisible, destiné à l'interface.
     *
     * @return le libellé en français du rôle
     */
    public String getLibelle() {
        return libelle;
    }

    /**
     * Indique si ce rôle doit obligatoirement être rattaché à une ferme.
     *
     * @return {@code true} pour RESPONSABLE et AGENT_RH, {@code false} pour ADMIN
     */
    public boolean isRattacheAFerme() {
        return rattacheAFerme;
    }

    @Override
    public String toString() {
        return libelle;
    }
}