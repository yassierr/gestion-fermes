package ma.ferme.model;

import ma.ferme.model.enums.StatutPointage;
import java.time.LocalDate;

/**
 * 
 * Pointage des ouvriers: Cette classe permet L'agent RH de pointer le présence ou l'absence
 *  d'un ouvrier dans une ferme/serre certaine.
 * 
 * @author JamelBoud
 * @version 1.1
 */

public class Pointage {
    private int id;
    private int idOuvrier;
    private LocalDate datePointage;
    private StatutPointage statut;

    // Constructeur 1 : Utilisé lors de la saisie par l'Agent RH (avant sauvegarde en BDD)
    /**
     * La classe Pointage importe:
     * @param idOuvrier
     * Comme un clé étranger. mais Ila son propre ID : @param id
     * @param datePointage
     * @param statut
     */
    public Pointage(int idOuvrier, LocalDate datePointage, StatutPointage statut) {
        if (datePointage == null) {
            throw new IllegalArgumentException("La date de pointage ne peut pas être nulle");
        }
        if (statut == null) {
            throw new IllegalArgumentException("Le statut de pointage ne peut pas être nul");
        }
        this.idOuvrier = idOuvrier;
        this.datePointage = datePointage;
        this.statut = statut;
    }

    // Constructeur 2 : Utilisé par le PointageDao (lecture depuis la BDD)
    public Pointage(int id, int idOuvrier, LocalDate datePointage, StatutPointage statut) {
        this(idOuvrier, datePointage, statut);
        this.id = id;
    }

    // Méthode métier : Calcule le coefficient de jour travaillé pour le solde/paie
    public double getValeurJournee() {
        switch (statut) {
            case PRESENT:
                return 1.0;
            case DEMI_JOURNEE:
                return 0.5;
            case ABSENT:
            default:
                return 0.0;
        }
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdOuvrier() { return idOuvrier; }
    public void setIdOuvrier(int idOuvrier) { this.idOuvrier = idOuvrier; }

    public LocalDate getDatePointage() { return datePointage; }
    public void setDatePointage(LocalDate datePointage) {
        if (datePointage == null) {
            throw new IllegalArgumentException("La date de pointage ne peut pas être nulle");
        }
        this.datePointage = datePointage;
    }

    public StatutPointage getStatut() { return statut; }
    public void setStatut(StatutPointage statut) {
        if (statut == null) {
            throw new IllegalArgumentException("Le statut de pointage ne peut pas être nul");
        }
        this.statut = statut;
    }
}