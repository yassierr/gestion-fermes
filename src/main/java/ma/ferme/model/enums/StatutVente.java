package ma.ferme.model.enums;

/**
 * Statut d'une livraison (vente). Les valeurs correspondent a l'enumeration
 * PostgreSQL {@code statut_vente}.
 * <ul>
 *   <li>{@code LIVREE} : livree, pas encore payee (creance)</li>
 *   <li>{@code PAYEE} : payee par le client (gain encaisse)</li>
 *   <li>{@code ANNULEE} : annulee, exclue du stock vendu et du chiffre d'affaires</li>
 * </ul>
 *
 * @author Yassir 
 * @version 1.0
 */
public enum StatutVente {
    LIVREE("Livrée"),
    PAYEE("Payée"),
    ANNULEE("Annulée");

    private final String libelle;

    StatutVente(String libelle) {
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