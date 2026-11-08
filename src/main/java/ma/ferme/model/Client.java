package ma.ferme.model;

import ma.ferme.model.enums.TypeClient;

/**
 * Client de l'entreprise (station, exportateur, grossiste...). Les clients sont
 * partages par toutes les fermes : une livraison peut contenir des lignes de
 * plusieurs fermes pour un meme client.
 *
 * @author Yassir 
 * @version 1.0
 */
public class Client {

    private int id;
    private String code;
    private String nom;
    private TypeClient type;
    private String telephone;
    private String ville;
    private boolean actif = true;

    /** Constructeur vide (utilise par les DAO et les controllers). */
    public Client() {
    }

    /**
     * Cree un client sans identifiant (avant insertion en base).
     *
     * @param code      code unique du client (ex. CL-001)
     * @param nom       nom du client
     * @param type      type de client
     * @param telephone telephone (peut etre null)
     * @param ville     ville (peut etre null)
     */
    public Client(String code, String nom, TypeClient type, String telephone, String ville) {
        this.code = code;
        this.nom = nom;
        this.type = type;
        this.telephone = telephone;
        this.ville = ville;
    }

    /** @return l'identifiant (0 tant que le client n'est pas enregistre) */
    public int getId() { return id; }

    /** @param id l'identifiant genere par la base */
    public void setId(int id) { this.id = id; }

    /** @return le code unique du client */
    public String getCode() { return code; }

    /** @param code le code unique du client */
    public void setCode(String code) { this.code = code; }

    /** @return le nom du client */
    public String getNom() { return nom; }

    /** @param nom le nom du client */
    public void setNom(String nom) { this.nom = nom; }

    /** @return le type de client */
    public TypeClient getType() { return type; }

    /** @param type le type de client */
    public void setType(TypeClient type) { this.type = type; }

    /** @return le telephone (peut etre null) */
    public String getTelephone() { return telephone; }

    /** @param telephone le telephone */
    public void setTelephone(String telephone) { this.telephone = telephone; }

    /** @return la ville (peut etre null) */
    public String getVille() { return ville; }

    /** @param ville la ville */
    public void setVille(String ville) { this.ville = ville; }

    /** @return {@code true} si le client est actif */
    public boolean isActif() { return actif; }

    /** @param actif {@code true} si le client est actif */
    public void setActif(boolean actif) { this.actif = actif; }

    @Override
    public String toString() {
        return nom;
    }
}