package ma.ferme.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ligne d'une livraison : une quantite de tomates provenant d'une ferme,
 * vendue a un prix par kilo propre a cette ligne.
 *
 * @author Yassir 
 * @version 1.0
 */
public class LigneVente {

    private int id;
    private BigDecimal quantiteKg;
    private BigDecimal prixKg;
    private int idVente;
    private int idFerme;

    /** Constructeur vide. */
    public LigneVente() {
    }

    /**
     * Cree une ligne pour une ferme donnee.
     *
     * @param idFerme    identifiant de la ferme d'origine
     * @param quantiteKg quantite en kg (doit etre &gt; 0)
     * @param prixKg     prix par kg en MAD (doit etre &gt; 0)
     */
    public LigneVente(int idFerme, BigDecimal quantiteKg, BigDecimal prixKg) {
        this.idFerme = idFerme;
        this.quantiteKg = quantiteKg;
        this.prixKg = prixKg;
    }

    /**
     * Calcule le montant de la ligne (quantite x prix), arrondi a 2 decimales.
     *
     * @return le montant en MAD, ou 0 si quantite ou prix sont absents
     */
    public BigDecimal getMontant() {
        if (quantiteKg == null || prixKg == null) {
            return BigDecimal.ZERO;
        }
        return quantiteKg.multiply(prixKg).setScale(2, RoundingMode.HALF_UP);
    }

    /** @return l'identifiant de la ligne */
    public int getId() { return id; }

    /** @param id l'identifiant genere par la base */
    public void setId(int id) { this.id = id; }

    /** @return la quantite en kg */
    public BigDecimal getQuantiteKg() { return quantiteKg; }

    /** @param quantiteKg la quantite en kg */
    public void setQuantiteKg(BigDecimal quantiteKg) { this.quantiteKg = quantiteKg; }

    /** @return le prix par kg en MAD */
    public BigDecimal getPrixKg() { return prixKg; }

    /** @param prixKg le prix par kg en MAD */
    public void setPrixKg(BigDecimal prixKg) { this.prixKg = prixKg; }

    /** @return l'identifiant de la vente parente */
    public int getIdVente() { return idVente; }

    /** @param idVente l'identifiant de la vente parente */
    public void setIdVente(int idVente) { this.idVente = idVente; }

    /** @return l'identifiant de la ferme d'origine */
    public int getIdFerme() { return idFerme; }

    /** @param idFerme l'identifiant de la ferme d'origine */
    public void setIdFerme(int idFerme) { this.idFerme = idFerme; }
}