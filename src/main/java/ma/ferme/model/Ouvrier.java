package ma.ferme.model;

import java.math.BigDecimal;

/**
 * Ouvrier : cette classe permet l'agent RH de créer un identificateur d'un ouvrier dans le système,
 * concernant ses informations et confirmant son état du travail.
 * 
 * @author JamelBoud
 * @version 1.1
 */
public class Ouvrier {
    private int id;
    private String matricule;
    private int idFerme;
    private String nom_prenom;
    private String cin;
    private String telephone;
    private String adresse;
    private String poste;
    private BigDecimal salaireJournalier;
    private boolean actif;

    // Constructeur 1 : Utilisé par le Formulaire FXML (Avant sauvegarde en base de données)
    /**
     * Un ouvrier peut avoir des parametres extérieurs comme 
     * @param idFerme
     * sinon tous ces paramètres représente ses informations personnelles.
     * @param nom_prenom
     * @param cin
     * @param telephone
     * @param adresse
     * @param poste
     * @param salaireJournalier
     */
    public Ouvrier(int idFerme, String nom_prenom, String cin, String telephone, String adresse, String poste, BigDecimal salaireJournalier) {

        this.idFerme = idFerme;
        setNomEtPrenom(nom_prenom);
        setCin(cin);
        setTelephone(telephone);
        setAdresse(adresse);
        this.poste = poste;
        this.salaireJournalier = salaireJournalier;
        this.actif = true;
    }

    // Constructeur 2 : Utilisé par le OuvrierDao lors de la lecture depuis PostgreSQL
    public Ouvrier(int id, String matricule, int idFerme, String nom_prenom, String cin, String telephone, String adresse, String poste, BigDecimal salaireJournalier, boolean actif) {
        this.id = id;
        this.matricule = matricule;
        this.idFerme = idFerme;
        this.nom_prenom = nom_prenom;
        this.cin = cin;
        this.telephone = telephone;
        this.adresse = adresse;
        this.poste = poste;
        this.salaireJournalier = salaireJournalier;
        this.actif = actif;
    }

    // Méthode de génération du matricule si besoin
    /**
     * Chaque ouvrier est identifié par un identificateur dans le système,
     * mais aussi avoir une matricule crée selon leur ID comme son propre ID personnel
     */
    public void genererMatricule() {
        if (this.id > 0) {
            this.matricule = "AS-" + String.format("%04d", this.id);
        }
    }

    // Calcul métier
    public BigDecimal calculerMontant(double joursTravailles) {
        if (joursTravailles < 0) {
            throw new IllegalArgumentException("Le nombre de jours ne peut pas être négatif");
        }
        return salaireJournalier.multiply(BigDecimal.valueOf(joursTravailles));
    }

    // Getters
    public int getId() { return id; }
    public String getMatricule() { return matricule; }
    public int getIdFerme() { return idFerme; }
    public String getNomEtPrenom() { return nom_prenom; }
    public String getCin() { return cin; }
    public String getTelephone() { return telephone; }
    public String getAdresse() { return adresse; }
    public String getPoste() { return poste; }
    public BigDecimal getSalaireJournalier() { return salaireJournalier; }
    public boolean isActif() { return actif; }

    // Setters (Uniquement pour les données modifiables)
    public void setId(int id) { 
        this.id = id; 
        genererMatricule(); 
    }
    public void setIdFerme(int idFerme) { this.idFerme = idFerme; }
    public void setNomEtPrenom(String nom_prenom) {
        if (nom_prenom == null || nom_prenom.isBlank()){
            throw new IllegalArgumentException("le nom/prenom ne doît pas être vide.");
        }
        else this.nom_prenom = nom_prenom;
    }
    public void setCin(String cin) {
        if (cin == null || cin.isBlank()){
            throw new IllegalArgumentException("l'identité nationale ne doît pas être vide.");
        }
        else this.cin = cin;
    }
    public void setTelephone(String telephone) {
        if ((telephone == null || telephone.isBlank()) || (telephone.length() != 10)){
            throw new IllegalArgumentException("le numèro du téléphone doît être 10 caractères.");
        }
        else this.telephone = telephone;
    }
    public void setAdresse(String adresse) {
        if (adresse == null || adresse.isBlank()){
            throw new IllegalArgumentException("l'adresse ne doît pas être vide.");
        }
        else this.adresse = adresse;
    }
    public void setPoste(String poste) { this.poste = poste; }
    public void setSalaireJournalier(BigDecimal salaireJournalier) { this.salaireJournalier = salaireJournalier; }
    public void setActif(boolean actif) { this.actif = actif; }
}