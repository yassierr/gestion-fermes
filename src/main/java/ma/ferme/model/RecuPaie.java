package ma.ferme.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
/**
 * 
 * Reçu de Paiement : Cette classe gère le reçu global émis 
 * lors d'une session de paiement par l'Agent RH, 
 * regroupant plusieurs LignePaie et le total
 * 
 * @author JamelBoud
 * @version 1.0
 */
public class RecuPaie {
    private int id;
    private int idAgentRh;
    private LocalDate datePaiement;
    private LocalDate periodeDebut;
    private LocalDate periodeFin;
    private BigDecimal total;
    private List<LignePaie> lignes;

    // Constructeur 1 : Création lors du paiement (avant BDD)
    /**
     * Chaque Reçu de Paiement prend @param idAgentRh comme un clé secondaire 
     * afin de vérifier L'agent RH qui va suivre et vérifier cette payment.
     * Aussi des autres paramètres :
     * @param datePaiement
     * @param periodeDebut
     * @param periodeFin
     */
    public RecuPaie(int idAgentRh, LocalDate datePaiement, LocalDate periodeDebut, LocalDate periodeFin) {
        if (datePaiement == null || periodeDebut == null || periodeFin == null) {
            throw new IllegalArgumentException("Les dates du reçu de paie ne peuvent pas être nulles");
        }
        if (periodeDebut.isAfter(periodeFin)) {
            throw new IllegalArgumentException("La date de début de période ne peut pas être après la date de fin");
        }
        this.idAgentRh = idAgentRh;
        this.datePaiement = datePaiement;
        this.periodeDebut = periodeDebut;
        this.periodeFin = periodeFin;
        this.total = BigDecimal.ZERO;
        this.lignes = new ArrayList<>();
    }

    // Constructeur 2 : Lecture depuis la BDD (DAO)
    public RecuPaie(int id, int idAgentRh, LocalDate datePaiement, LocalDate periodeDebut, LocalDate periodeFin, BigDecimal total) {
        this(idAgentRh, datePaiement, periodeDebut, periodeFin);
        this.id = id;
        this.total = (total != null) ? total : BigDecimal.ZERO;
    }

    // Méthode métier : Ajouter une ligne de paie et recalculer automatiquement le total
    public void ajouterLigne(LignePaie ligne) {
        if (ligne == null) {
            throw new IllegalArgumentException("La ligne de paie ne peut pas être nulle");
        }
        this.lignes.add(ligne);
        recalculerTotal();
    }

    // Recalcule le montant total cumulé du reçu
    /**
     * recalculerTotal(): c'est une méthode qui va reconfirmer le montant que l'ouvrier doit avoir.
     * Elle utilise la méthode getMontant() de la classe LignePaie.
     */
    public void recalculerTotal() {
        BigDecimal somme = BigDecimal.ZERO;
        for (LignePaie l : lignes) {
            if (l.getMontant() != null) {
                somme = somme.add(l.getMontant());
            }
        }
        this.total = somme;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdAgentRh() { return idAgentRh; }
    public void setIdAgentRh(int idAgentRh) { this.idAgentRh = idAgentRh; }

    public LocalDate getDatePaiement() { return datePaiement; }
    public void setDatePaiement(LocalDate datePaiement) { this.datePaiement = datePaiement; }

    public LocalDate getPeriodeDebut() { return periodeDebut; }
    public void setPeriodeDebut(LocalDate periodeDebut) { this.periodeDebut = periodeDebut; }

    public LocalDate getPeriodeFin() { return periodeFin; }
    public void setPeriodeFin(LocalDate periodeFin) { this.periodeFin = periodeFin; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public List<LignePaie> getLignes() { return lignes; }
    public void setLignes(List<LignePaie> lignes) {
        this.lignes = (lignes != null) ? lignes : new ArrayList<>();
        recalculerTotal();
    }
}