package ma.ferme.model;

import java.util.List;

import ma.ferme.model.enums.RoleUtilisateur;

/**
 * Responsable de ferme : gère les serres, les récoltes et les dépenses de sa ferme,
 * consulte ses livraisons, l'effectif et les reçus de paie.
 * Il est rattaché à exactement une ferme (RG-10) et toutes ses requêtes sont
 * filtrées sur cette ferme (EF-T04).
 * <p>
 * L'ajout d'une serre ou d'une dépense est réalisé par {@code SerreService} et
 * {@code DepenseService}.
 * </p>
 */
public class ResponsableFerme extends Utilisateur {

    /**
     * Crée un responsable déjà enregistré en base.
     *
     * @param id             identifiant en base
     * @param nomComplet     nom complet
     * @param login          login
     * @param motDePasseHash hash du mot de passe
     * @param actif          état du compte
     * @param idFerme        ferme gérée (obligatoire)
     */
    public ResponsableFerme(int id, String nomComplet, String login, String motDePasseHash,
                            boolean actif, int idFerme) {
        super(id, nomComplet, login, motDePasseHash, RoleUtilisateur.RESPONSABLE, actif, idFerme);
    }

    /**
     * Crée un nouveau responsable actif, pas encore enregistré.
     *
     * @param nomComplet     nom complet
     * @param login          login
     * @param motDePasseHash hash du mot de passe
     * @param idFerme        ferme gérée
     */
    public ResponsableFerme(String nomComplet, String login, String motDePasseHash, int idFerme) {
        this(0, nomComplet, login, motDePasseHash, true, idFerme);
    }

    /**
     * Menu du responsable de ferme.
     *
     * @return Tableau de bord, Serres, Récoltes, Dépenses, Livraisons &amp; Ouvriers
     */
    @Override
    public List<String> afficherMenu() {
        return List.of("Tableau de bord", "Serres", "Récoltes", "Dépenses", "Livraisons & Ouvriers");
    }
}