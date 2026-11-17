package ma.ferme.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ma.ferme.dao.VenteDao;
import ma.ferme.model.LigneVente;
import ma.ferme.model.Vente;
import ma.ferme.model.enums.StatutVente;

/**
 * Regles metier des livraisons : validation, controle du stock par ferme,
 * passage a l'etat PAYEE ou ANNULEE, indicateurs financiers.
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public class VenteService {

    private final VenteDao dao;

    /** Cree le service avec son DAO par defaut. */
    public VenteService() {
        this(new VenteDao());
    }

    /**
     * Cree le service avec un DAO donne (utile pour les tests).
     *
     * @param dao le DAO a utiliser
     */
    public VenteService(VenteDao dao) {
        this.dao = dao;
    }

    /**
     * Cree une livraison : valide les lignes, verifie que chaque ferme a assez de
     * stock, puis enregistre la vente et ses lignes dans une transaction.
     * La vente est creee au statut LIVREE ; la date du jour est utilisee si aucune
     * date de livraison n'est fournie.
     *
     * @param v       la livraison (client et au moins une ligne)
     * @param idAdmin identifiant de l'administrateur qui cree la livraison
     * @return la livraison enregistree (avec son numero de bon)
     * @throws IllegalArgumentException si la livraison est invalide ou si le stock est insuffisant
     */
    public Vente creerLivraison(Vente v, int idAdmin) {
        validerStructure(v);
        verifierStock(v);
        v.setIdAdmin(idAdmin);
        v.setStatut(StatutVente.LIVREE);
        v.setDatePaiement(null);
        if (v.getDateLivraison() == null) {
            v.setDateLivraison(LocalDate.now());
        }
        dao.insertAvecLignes(v);
        return v;
    }

    /**
     * Marque une livraison comme payee.
     *
     * @param idVente     identifiant de la vente
     * @param datePaiement date du paiement (date du jour si null)
     * @throws IllegalArgumentException si la vente n'existe pas
     * @throws IllegalStateException    si la vente n'est pas au statut LIVREE
     */
    public void marquerPayee(int idVente, LocalDate datePaiement) {
        Vente v = chargerOuErreur(idVente);
        if (v.getStatut() != StatutVente.LIVREE) {
            throw new IllegalStateException("Seule une livraison au statut Livrée peut être marquée payée");
        }
        v.setStatut(StatutVente.PAYEE);
        v.setDatePaiement(datePaiement == null ? LocalDate.now() : datePaiement);
        dao.update(v);
    }

    /**
     * Annule une livraison (le stock vendu est alors restitue a la ferme).
     *
     * @param idVente identifiant de la vente
     * @throws IllegalArgumentException si la vente n'existe pas
     * @throws IllegalStateException    si la vente n'est pas au statut LIVREE
     */
    public void annuler(int idVente) {
        Vente v = chargerOuErreur(idVente);
        if (v.getStatut() != StatutVente.LIVREE) {
            throw new IllegalStateException("Seule une livraison au statut Livrée peut être annulée");
        }
        v.setStatut(StatutVente.ANNULEE);
        dao.update(v);
    }

    /**
     * Liste toutes les livraisons.
     *
     * @return les ventes, les plus recentes d'abord
     */
    public List<Vente> lister() {
        return dao.findAll();
    }

    /**
     * Liste les livraisons contenant des lignes d'une ferme.
     *
     * @param idFerme identifiant de la ferme
     * @return les ventes concernees
     */
    public List<Vente> listerParFerme(int idFerme) {
        return dao.findByFerme(idFerme);
    }

    /**
     * Recherche une livraison.
     *
     * @param id identifiant de la vente
     * @return la vente, ou un Optional vide
     */
    public Optional<Vente> trouver(int id) {
        return dao.findById(id);
    }

    /**
     * Stock disponible d'une ferme (recoltes moins ventes non annulees).
     *
     * @param idFerme identifiant de la ferme
     * @return le stock en kg
     */
    public BigDecimal stockDisponible(int idFerme) {
        return dao.stockDisponible(idFerme);
    }

    /** @return le chiffre d'affaires (livraisons non annulees), en MAD */
    public BigDecimal chiffreAffaires() {
        return dao.chiffreAffaires();
    }

    /** @return les gains encaisses (livraisons payees), en MAD */
    public BigDecimal gainsEncaisses() {
        return dao.gainsEncaisses();
    }

    /** @return les creances (livraisons non payees), en MAD */
    public BigDecimal creances() {
        return dao.creances();
    }

    private Vente chargerOuErreur(int idVente) {
        return dao.findById(idVente)
                .orElseThrow(() -> new IllegalArgumentException("Livraison introuvable : " + idVente));
    }

    private void validerStructure(Vente v) {
        if (v == null) {
            throw new IllegalArgumentException("Livraison absente");
        }
        if (v.getIdClient() <= 0) {
            throw new IllegalArgumentException("Le client est obligatoire");
        }
        if (v.getLignes() == null || v.getLignes().isEmpty()) {
            throw new IllegalArgumentException("La livraison doit contenir au moins une ligne");
        }
        for (LigneVente l : v.getLignes()) {
            if (l.getIdFerme() <= 0) {
                throw new IllegalArgumentException("Chaque ligne doit indiquer une ferme");
            }
            if (l.getQuantiteKg() == null || l.getQuantiteKg().signum() <= 0) {
                throw new IllegalArgumentException("La quantité doit être supérieure à 0");
            }
            if (l.getPrixKg() == null || l.getPrixKg().signum() <= 0) {
                throw new IllegalArgumentException("Le prix par kg doit être supérieur à 0");
            }
        }
    }

    private void verifierStock(Vente v) {
        Map<Integer, BigDecimal> demande = new HashMap<>();
        for (LigneVente l : v.getLignes()) {
            demande.merge(l.getIdFerme(), l.getQuantiteKg(), BigDecimal::add);
        }
        for (Map.Entry<Integer, BigDecimal> e : demande.entrySet()) {
            BigDecimal stock = dao.stockDisponible(e.getKey());
            if (e.getValue().compareTo(stock) > 0) {
                throw new IllegalArgumentException("Stock insuffisant pour la ferme " + e.getKey()
                        + " : demandé " + e.getValue() + " kg, disponible " + stock + " kg");
            }
        }
    }
}