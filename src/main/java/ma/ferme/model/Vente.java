package ma.ferme.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import ma.ferme.model.enums.StatutVente;

/**
 * Livraison de tomates a un client (bon de livraison {@code BL-AAAA-NNNN}).
 * Une vente regroupe une ou plusieurs {@link LigneVente}, une par ferme d'origine.
 *
 * @author Yassir 
 * @version 1.0
 */
public class Vente {

    private int id;
    private String numeroBon;
    private LocalDate dateLivraison;
    private StatutVente statut = StatutVente.LIVREE;
    private LocalDate datePaiement;
    private int idClient;
    private int idAdmin;
    private List<LigneVente> lignes = new ArrayList<>();

    /** Constructeur vide. */
    public Vente() {
    }

    /**
     * Ajoute une ligne a la livraison.
     *
     * @param ligne la ligne a ajouter
     */
    public void ajouterLigne(LigneVente ligne) {
        lignes.add(ligne);
    }

    /**
     * Calcule le montant total de la livraison.
     *
     * @return la somme des montants des lignes, en MAD
     */
    public BigDecimal getTotal() {
        return lignes.stream().map(LigneVente::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcule la quantite totale livree.
     *
     * @return la somme des quantites des lignes, en kg
     */
    public BigDecimal getQuantiteTotale() {
        return lignes.stream().map(LigneVente::getQuantiteKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** @return l'identifiant de la vente */
    public int getId() { return id; }

    /** @param id l'identifiant genere par la base */
    public void setId(int id) { this.id = id; }

    /** @return le numero du bon de livraison (ex. BL-2026-0001) */
    public String getNumeroBon() { return numeroBon; }

    /** @param numeroBon le numero du bon de livraison */
    public void setNumeroBon(String numeroBon) { this.numeroBon = numeroBon; }

    /** @return la date de livraison */
    public LocalDate getDateLivraison() { return dateLivraison; }

    /** @param dateLivraison la date de livraison */
    public void setDateLivraison(LocalDate dateLivraison) { this.dateLivraison = dateLivraison; }

    /** @return le statut de la livraison */
    public StatutVente getStatut() { return statut; }

    /** @param statut le statut de la livraison */
    public void setStatut(StatutVente statut) { this.statut = statut; }

    /** @return la date de paiement (null tant que non payee) */
    public LocalDate getDatePaiement() { return datePaiement; }

    /** @param datePaiement la date de paiement */
    public void setDatePaiement(LocalDate datePaiement) { this.datePaiement = datePaiement; }

    /** @return l'identifiant du client */
    public int getIdClient() { return idClient; }

    /** @param idClient l'identifiant du client */
    public void setIdClient(int idClient) { this.idClient = idClient; }

    /** @return l'identifiant de l'administrateur qui a cree la livraison */
    public int getIdAdmin() { return idAdmin; }

    /** @param idAdmin l'identifiant de l'administrateur */
    public void setIdAdmin(int idAdmin) { this.idAdmin = idAdmin; }

    /** @return la liste des lignes de la livraison */
    public List<LigneVente> getLignes() { return lignes; }

    /** @param lignes la liste des lignes de la livraison */
    public void setLignes(List<LigneVente> lignes) { this.lignes = lignes; }
}