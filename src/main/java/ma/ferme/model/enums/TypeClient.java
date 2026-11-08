package ma.ferme.model.enums;

/**
 * Types de clients de l'entreprise. Les valeurs correspondent exactement
 * a l'enumeration PostgreSQL {@code type_client}.
 *
 * @author Yassir 
 * @version 1.0
 */
public enum TypeClient {
    STATION_CONDITIONNEMENT("Station de conditionnement"),
    EXPORTATEUR("Exportateur"),
    GROSSISTE("Grossiste"),
    MARCHE_GROS("Marché de gros");

    private final String libelle;

    TypeClient(String libelle) {
        this.libelle = libelle;
    }

    /**
     * Retourne le texte affiche dans l'interface.
     *
     * @return le libelle en francais
     */
    public String getLibelle() {
        return libelle;
    }

    @Override
    public String toString() {
        return libelle;
    }
}