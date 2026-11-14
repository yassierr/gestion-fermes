package ma.ferme.model;

import java.math.BigDecimal;

/**
 * 
 * Lignes de Paiement: Cette classe détaille la ligne de calcul pour un ouvrier spécifique sur une période donnée.
 * 
 * @author JamelBoud
 * @version 1.0
 */

public class LignePaie {
    private int id;
    private int idRecuPaie;
    private int idOuvrier;
    private double joursPayes;
    private BigDecimal montant;

    // Constructeur 1 : Création avant enregistrement en base de données
    /**
     * Chaque Ligne de Payment peut avoir L'identificateur Ouvrier comme un clé secondaire
     * afin de vérifier l'ouvrier qui va avoir cette payment.
     * @param idOuvrier
     * @param joursPayes
     * @param montant
     */
    public LignePaie(int idOuvrier, double joursPayes, BigDecimal montant) {
        if (joursPayes < 0) {
            throw new IllegalArgumentException("Le nombre de jours payés ne peut pas être négatif");
        }
        if (montant == null || montant.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le montant ne peut pas être nul ou négatif");
        }
        this.idOuvrier = idOuvrier;
        this.joursPayes = joursPayes;
        this.montant = montant;
    }

    // Constructeur 2 : Lecture depuis la BDD (DAO)
    public LignePaie(int id, int idRecuPaie, int idOuvrier, double joursPayes, BigDecimal montant) {
        this(idOuvrier, joursPayes, montant);
        this.id = id;
        this.idRecuPaie = idRecuPaie;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdRecuPaie() { return idRecuPaie; }
    public void setIdRecuPaie(int idRecuPaie) { this.idRecuPaie = idRecuPaie; }

    public int getIdOuvrier() { return idOuvrier; }
    public void setIdOuvrier(int idOuvrier) { this.idOuvrier = idOuvrier; }

    public double getJoursPayes() { return joursPayes; }
    public void setJoursPayes(double joursPayes) {
        if (joursPayes < 0) {
            throw new IllegalArgumentException("Le nombre de jours payés ne peut pas être négatif");
        }
        this.joursPayes = joursPayes;
    }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le montant ne peut pas être nul ou négatif");
        }
        this.montant = montant;
    }
}