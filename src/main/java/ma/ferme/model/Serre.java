package ma.ferme.model;
public class Serre {
    private static int counter = 1; // Counter to generate automatic IDs
    
    private final int id; // Read-only ID
    private String nom;
    private double surfaceM2;
    private String variete;
    private String typeAbri;
    private String irrigation;

    /**
     * Default constructor. Automatically assigns a unique ID.
     */
    public Serre() {
        this.id = counter++;
    }

    /**
     * Parameterized constructor (without ID, as it is automatic).
     */
    public Serre(String nom, double surfaceM2, String variete, String typeAbri, String irrigation) {
        this.id = counter++;
        this.nom = nom;
        this.surfaceM2 = surfaceM2;
        this.variete = variete;
        this.typeAbri = typeAbri;
        this.irrigation = irrigation;
    }

    // Getter for ID (No setter, so it cannot be changed)
    public int getId() {
        return id;
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
}