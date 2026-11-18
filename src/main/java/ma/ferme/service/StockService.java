package ma.ferme.service;

import java.math.BigDecimal;

import ma.ferme.dao.VenteDao;
import ma.ferme.model.Utilisateur;
import ma.ferme.model.enums.RoleUtilisateur;

/**
 * Calcul du stock disponible d'une ferme (EF-A09, EFR08).
 * <p>
 * Règle RG-01 : stock = somme des récoltes − somme des lignes de livraison non annulées.
 * Le stock n'est jamais saisi ni stocké (ENF-08) : il est recalculé à chaque appel
 * par {@link VenteDao#stockDisponible(int)}. Annuler une livraison remet donc
 * automatiquement les kg en stock (RG-05).
 * </p>
 *
 * @author Mohamed Amine Ait Maait
 * @version 1.0
 */
public class StockService {

    private final VenteDao dao;

    /** Crée le service avec son DAO par défaut. */
    public StockService() {
        this(new VenteDao());
    }

    /**
     * Crée le service avec un DAO donné (utile pour les tests).
     *
     * @param dao le DAO des ventes, qui calcule le stock
     */
    public StockService(VenteDao dao) {
        this.dao = dao;
    }

    /**
     * Stock disponible d'une ferme.
     *
     * @param idFerme identifiant de la ferme
     * @return le stock en kg
     */
    public BigDecimal stockDisponible(int idFerme) {
        return dao.stockDisponible(idFerme);
    }

    /**
     * Stock disponible de la ferme du responsable connecté.
     *
     * @param acteur le responsable de ferme connecté
     * @return le stock en kg
     * @throws SecurityException si l'acteur n'est pas un responsable actif
     */
    public BigDecimal stockDisponible(Utilisateur acteur) {
        return dao.stockDisponible(fermeDuResponsable(acteur));
    }

    /**
     * Indique si une ferme peut fournir une quantité (RG-02).
     *
     * @param idFerme    identifiant de la ferme
     * @param quantiteKg quantité demandée en kg
     * @return {@code true} si le stock disponible couvre la quantité
     */
    public boolean estSuffisant(int idFerme, BigDecimal quantiteKg) {
        return quantiteKg != null && stockDisponible(idFerme).compareTo(quantiteKg) >= 0;
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
}