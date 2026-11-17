package ma.ferme.service;

import java.math.BigDecimal;
import java.util.List;

import ma.ferme.dao.RecolteDao;
import ma.ferme.model.Recolte;
import ma.ferme.model.Utilisateur;
import ma.ferme.model.enums.RoleUtilisateur;

/**
 * Règles métier des récoltes (EFR06 à EFR08), réservées au responsable de la ferme.
 * <ul>
 *   <li>EF-T04 : un responsable ne voit et ne modifie que les récoltes de sa ferme ;</li>
 *   <li>RG-07 : modifier ou supprimer une récolte est refusé si le stock deviendrait négatif ;</li>
 *   <li>RG-01 : le stock est calculé par {@link StockService}, jamais saisi.</li>
 * </ul>
 * Les erreurs de saisie lèvent {@link IllegalArgumentException} ; une règle métier violée lève
 * {@link IllegalStateException} ; un accès interdit lève {@link SecurityException}.
 *
 * @author Mohamed Amine Ait Maait
 * @version 1.0
 */
public class RecolteService {

    private final RecolteDao dao;
    private final StockService stockService;

    /** Crée le service avec ses dépendances par défaut. */
    public RecolteService() {
        this(new RecolteDao(), new StockService());
    }

    /**
     * Crée le service avec des dépendances données (utile pour les tests).
     *
     * @param dao          le DAO des récoltes
     * @param stockService le service de stock
     */
    public RecolteService(RecolteDao dao, StockService stockService) {
        this.dao = dao;
        this.stockService = stockService;
    }

    /**
     * Enregistre une récolte datée de maintenant (EFR06).
     *
     * @param acteur  le responsable de ferme connecté
     * @param poidsKg poids net en kg, strictement positif
     * @param variete variété, non vide
     * @return la récolte enregistrée
     * @throws SecurityException        si l'acteur n'est pas un responsable actif
     * @throws IllegalArgumentException si le poids ou la variété est invalide
     */
    public Recolte enregistrer(Utilisateur acteur, BigDecimal poidsKg, String variete) {
        int idFerme = fermeDuResponsable(acteur);
        Recolte recolte = new Recolte(idFerme, poidsKg, variete);
        dao.insert(recolte);
        return recolte;
    }

    /**
     * Modifie le poids et la variété d'une récolte (EFR07). La date reste inchangée.
     *
     * @param acteur    le responsable de ferme connecté
     * @param idRecolte identifiant de la récolte
     * @param poidsKg   nouveau poids net en kg
     * @param variete   nouvelle variété
     * @throws SecurityException        si la récolte est d'une autre ferme
     * @throws IllegalArgumentException si la récolte est introuvable ou si les valeurs sont invalides
     * @throws IllegalStateException    si le stock deviendrait négatif (RG-07)
     */
    public void modifier(Utilisateur acteur, int idRecolte, BigDecimal poidsKg, String variete) {
        int idFerme = fermeDuResponsable(acteur);
        Recolte recolte = trouver(idRecolte, idFerme);
        BigDecimal ancienPoids = recolte.getPoidsKg();
        recolte.setPoidsKg(poidsKg);
        recolte.setVariete(variete);
        verifierStock(idFerme, recolte.getPoidsKg().subtract(ancienPoids), "modifier");
        dao.update(recolte);
    }

    /**
     * Supprime une récolte (EFR07).
     *
     * @param acteur    le responsable de ferme connecté
     * @param idRecolte identifiant de la récolte
     * @throws SecurityException        si la récolte est d'une autre ferme
     * @throws IllegalArgumentException si la récolte est introuvable
     * @throws IllegalStateException    si le stock deviendrait négatif (RG-07)
     */
    public void supprimer(Utilisateur acteur, int idRecolte) {
        int idFerme = fermeDuResponsable(acteur);
        Recolte recolte = trouver(idRecolte, idFerme);
        verifierStock(idFerme, recolte.getPoidsKg().negate(), "supprimer");
        dao.delete(idRecolte);
    }

    /**
     * Historique des récoltes de la ferme, les plus récentes d'abord (EFR07).
     *
     * @param acteur le responsable de ferme connecté
     * @return les récoltes de sa ferme
     */
    public List<Recolte> lister(Utilisateur acteur) {
        return dao.findByFerme(fermeDuResponsable(acteur));
    }

    /**
     * Stock disponible de la ferme du responsable (EFR08).
     *
     * @param acteur le responsable de ferme connecté
     * @return le stock en kg
     */
    public BigDecimal stockDisponible(Utilisateur acteur) {
        return stockService.stockDisponible(acteur);
    }

    /**
     * Variétés proposées dans le formulaire : celles déjà récoltées ou cultivées dans la ferme.
     *
     * @param acteur le responsable de ferme connecté
     * @return les variétés connues, par ordre alphabétique
     */
    public List<String> varietes(Utilisateur acteur) {
        return dao.varietes(fermeDuResponsable(acteur));
    }

    private Recolte trouver(int idRecolte, int idFerme) {
        Recolte recolte = dao.findById(idRecolte)
                .orElseThrow(() -> new IllegalArgumentException("Cette récolte n'existe plus."));
        exigerMemeFerme(recolte.getIdFerme(), idFerme);
        return recolte;
    }

    /** RG-07 : le stock après changement ({@code variation} en kg) doit rester positif ou nul. */
    private void verifierStock(int idFerme, BigDecimal variation, String action) {
        BigDecimal stock = stockService.stockDisponible(idFerme);
        if (stock.add(variation).signum() < 0) {
            throw new IllegalStateException("Impossible de " + action + " cette récolte : le stock de la ferme "
                    + "(" + stock.toPlainString() + " kg) deviendrait négatif, car ces kg ont déjà été livrés.");
        }
    }

    /** Vérifie que l'acteur est un responsable actif et retourne sa ferme (ENF-02, EF-T04). */
    private static int fermeDuResponsable(Utilisateur acteur) {
        if (acteur == null || acteur.getRole() != RoleUtilisateur.RESPONSABLE || !acteur.isActif()) {
            throw new SecurityException("Action réservée au responsable de ferme.");
        }
        if (acteur.getIdFerme() == null) {
            throw new SecurityException("Ce responsable n'est rattaché à aucune ferme.");
        }
        return acteur.getIdFerme();
    }

    /** Refuse l'accès à un élément d'une autre ferme (EF-T04). */
    private static void exigerMemeFerme(int idFermeElement, int idFermeActeur) {
        if (idFermeElement != idFermeActeur) {
            throw new SecurityException("Cet élément appartient à une autre ferme.");
        }
    }
}