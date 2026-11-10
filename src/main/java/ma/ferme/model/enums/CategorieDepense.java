package ma.ferme.model.enums;

/**
 * Catégorie d'une dépense d'exploitation (EF-R09).
 * <p>
 * Les noms des constantes correspondent exactement aux valeurs du type
 * PostgreSQL {@code categorie_depense} (voir {@code database/schema.sql}) :
 * on les relit avec {@code valueOf(rs.getString("categorie"))} et on les écrit
 * avec {@code ?::categorie_depense}.
 * </p>
 *
 * @author Mohamed Amine Ait Maait
 * @version 1.0
 */
public enum CategorieDepense {

    /** Plants et semences. */
    PLANTS("Plants"),
    /** Engrais et solutions nutritives. */
    ENGRAIS("Engrais"),
    /** Eau et électricité. */
    EAU_ENERGIE("Eau-Énergie"),
    /** Caisses, cartons, emballages. */
    EMBALLAGE("Emballage"),
    /** Toute autre dépense. */
    AUTRE("Autre");

    private final String libelle;

    CategorieDepense(String libelle) {
        this.libelle = libelle;
    }

    /**
     * Libellé affiché dans l'interface.
     *
     * @return le libellé en français
     */
    public String getLibelle() {
        return libelle;
    }

    /**
     * Retrouve une catégorie à partir de son libellé affiché (sans tenir compte de la casse).
     *
     * @param texte libellé ou nom de la constante
     * @return la catégorie correspondante
     * @throws IllegalArgumentException si le texte ne correspond à aucune catégorie
     */
    public static CategorieDepense depuisLibelle(String texte) {
        if (texte != null) {
            for (CategorieDepense c : values()) {
                if (c.libelle.equalsIgnoreCase(texte.trim()) || c.name().equalsIgnoreCase(texte.trim())) {
                    return c;
                }
            }
        }
        throw new IllegalArgumentException("Catégorie de dépense inconnue : " + texte);
    }

    /**
     * Le libellé sert d'affichage dans les listes déroulantes.
     *
     * @return le libellé
     */
    @Override
    public String toString() {
        return libelle;
    }
}