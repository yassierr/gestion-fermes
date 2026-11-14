package ma.ferme.model;

import java.util.List;

import ma.ferme.model.enums.RoleUtilisateur;

/**
 * Administrateur : supervise toutes les fermes (comptes, fermes, clients, livraisons,
 * bilan annuel). Il n'est rattaché à aucune ferme (RG-10), donc son
 * {@link #getIdFerme()} vaut toujours {@code null}.
 * <p>
 * Les actions de l'administrateur (créer un compte, une ferme, un client, une livraison)
 * sont réalisées par les services correspondants ({@code UtilisateurService},
 * {@code FermeService}, {@code ClientService}, {@code VenteService}), pas par cette classe.
 * </p>
 */
public class Administrateur extends Utilisateur {

    /**
     * Crée un administrateur déjà enregistré en base.
     *
     * @param id             identifiant en base
     * @param nomComplet     nom complet
     * @param login          login
     * @param motDePasseHash hash du mot de passe
     * @param actif          état du compte
     */
    public Administrateur(int id, String nomComplet, String login, String motDePasseHash, boolean actif) {
        super(id, nomComplet, login, motDePasseHash, RoleUtilisateur.ADMIN, actif, null);
    }

    /**
     * Crée un nouvel administrateur actif, pas encore enregistré.
     *
     * @param nomComplet     nom complet
     * @param login          login
     * @param motDePasseHash hash du mot de passe
     */
    public Administrateur(String nomComplet, String login, String motDePasseHash) {
        this(0, nomComplet, login, motDePasseHash, true);
    }

    /**
     * Menu de l'administrateur.
     *
     * @return Tableau de bord, Utilisateurs, Fermes, Clients &amp; Livraisons, Bilan annuel
     */
    @Override
    public List<String> afficherMenu() {
        return List.of("Tableau de bord", "Utilisateurs", "Fermes", "Clients & Livraisons", "Bilan annuel");
    }
}