package ma.ferme.model;

import java.util.List;

import ma.ferme.model.enums.RoleUtilisateur;

/**
 * Agent RH : gère les ouvriers, le pointage quotidien et la paie de sa ferme.
 * Il est rattaché à exactement une ferme (RG-10) et ne voit ni les ventes,
 * ni les dépenses, ni les récoltes.
 * <p>
 * L'ajout d'un ouvrier, le pointage et le paiement groupé sont réalisés par
 * {@code OuvrierService}, {@code PointageService} et {@code PaieService}.
 * </p>
 */
public class AgentRH extends Utilisateur {

    /**
     * Crée un agent RH déjà enregistré en base.
     *
     * @param id             identifiant en base
     * @param nomComplet     nom complet
     * @param login          login
     * @param motDePasseHash hash du mot de passe
     * @param actif          état du compte
     * @param idFerme        ferme où il travaille (obligatoire)
     */
    public AgentRH(int id, String nomComplet, String login, String motDePasseHash,
                   boolean actif, int idFerme) {
        super(id, nomComplet, login, motDePasseHash, RoleUtilisateur.AGENT_RH, actif, idFerme);
    }

    /**
     * Crée un nouvel agent RH actif, pas encore enregistré.
     *
     * @param nomComplet     nom complet
     * @param login          login
     * @param motDePasseHash hash du mot de passe
     * @param idFerme        ferme où il travaille
     */
    public AgentRH(String nomComplet, String login, String motDePasseHash, int idFerme) {
        this(0, nomComplet, login, motDePasseHash, true, idFerme);
    }

    /**
     * Menu de l'agent RH.
     *
     * @return Tableau de bord, Ouvriers
     */
    @Override
    public List<String> afficherMenu() {
        return List.of("Tableau de bord", "Ouvriers");
    }
}