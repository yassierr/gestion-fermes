
package ma.ferme.model;

/**
 * Represente une serre appartenant a une ferme.
 */
public class Serre {

    private int id;
    private String nom;
    private double surfaceM2;
    private String variete;
    private String typeAbri;
    private String irrigation;
    private int idFerme;

    public Serre() {
    }

    /**
     * Constructeur pour creer une nouvelle serre.
     */
    public Serre(String nom, double surfaceM2, String variete,
                 String typeAbri, String irrigation, int idFerme) {
        this.nom = nom;
        this.surfaceM2 = surfaceM2;
        this.variete = variete;
        this.typeAbri = typeAbri;
        this.irrigation = irrigation;
        this.idFerme = idFerme;
    }

    /**
     * Constructeur utilise lors de la recuperation depuis la base.
     */
    public Serre(int id, String nom, double surfaceM2, String variete,
                 String typeAbri, String irrigation, int idFerme) {
        this.id = id;
        this.nom = nom;
        this.surfaceM2 = surfaceM2;
        this.variete = variete;
        this.typeAbri = typeAbri;
        this.irrigation = irrigation;
        this.idFerme = idFerme;
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

    public double getSurfaceM2() {
        return surfaceM2;
    }

    public void setSurfaceM2(double surfaceM2) {
        this.surfaceM2 = surfaceM2;
    }

    public String getVariete() {
        return variete;
    }

    public void setVariete(String variete) {
        this.variete = variete;
    }

    public String getTypeAbri() {
        return typeAbri;
    }

    public void setTypeAbri(String typeAbri) {
        this.typeAbri = typeAbri;
    }

    public String getIrrigation() {
        return irrigation;
    }

    public void setIrrigation(String irrigation) {
        this.irrigation = irrigation;
    }

    public int getIdFerme() {
        return idFerme;
    }

    public void setIdFerme(int idFerme) {
        this.idFerme = idFerme;
    }
}
