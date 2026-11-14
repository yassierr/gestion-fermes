package ma.ferme.model;

public class Ferme {
    private static int counter = 1; // Counter to generate automatic IDs
    
    private final int id; // Read-only ID
    private String nom;
    private String localisation;

    /**
     * Default constructor. Automatically assigns a unique ID.
     */
    public Ferme() {
        this.id = counter++;
    }

    /**
     * Parameterized constructor (without ID, as it is automatic).
     */
    public Ferme(String nom, String localisation) {
        this.id = counter++;
        this.nom = nom;
        this.localisation = localisation;
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

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public double getStockDisponible() {
        // TODO: Implement business logic
        return 0.0;
    }
}