
package ma.ferme.model;

/**
 * Represente une ferme dans l'application.
 */
public class Ferme {

    private int id;
    private String nom;
    private String localisation;

    /**
     * Constructeur sans identifiant.
     */
    public Ferme() {
    }

    /**
     * Constructeur pour creer une nouvelle ferme.
     *
     * @param nom nom de la ferme
     * @param localisation localisation de la ferme
     */
    public Ferme(String nom, String localisation) {
        this.nom = nom;
        this.localisation = localisation;
    }

    /**
     * Constructeur utilise lors de la recuperation depuis la base.
     *
     * @param id identifiant de la ferme
     * @param nom nom de la ferme
     * @param localisation localisation de la ferme
     */
    public Ferme(int id, String nom, String localisation) {
        this.id = id;
        this.nom = nom;
        this.localisation = localisation;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    /**
     * Retourne le stock disponible.
     * A implementer dans une future evolution.
     *
     * @return stock disponible
     */
    public double getStockDisponible() {
        return 0.0;
    }
}
