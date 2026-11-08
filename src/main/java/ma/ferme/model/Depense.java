package ma.ferme.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

import ma.ferme.model.enums.CategorieDepense;

/**
 * Dépense d'exploitation d'une ferme : catégorie, montant en MAD, date et description.
 * <p>
 * La description contient aussi le fournisseur (champ unique « Description &amp; fournisseur »
 * de la maquette, colonne {@code description VARCHAR(255)}).
 * </p>
 * Objet du domaine : aucun accès à la base. Les droits (responsable de la ferme) sont
 * vérifiés par {@code DepenseService}.
 *
 * @author Mohamed Amine Ait Maait
 * @version 1.0
 */
public class Depense {

    /** Taille maximale de la description (colonne {@code description VARCHAR(255)}). */
    public static final int DESCRIPTION_MAX = 255;

    private int id;
    private LocalDate dateDepense;
    private CategorieDepense categorie;
    private BigDecimal montant;
    private String description;
    private int idFerme;

    /**
     * Crée une nouvelle dépense pas encore enregistrée.
     *
     * @param idFerme     ferme concernée
     * @param dateDepense date de la dépense ; aujourd'hui si {@code null}
     * @param categorie   catégorie, non nulle
     * @param montant     montant en MAD, strictement positif
     * @param description description et fournisseur, facultative
     * @throws IllegalArgumentException si une donnée est invalide
     */
    public Depense(int idFerme, LocalDate dateDepense, CategorieDepense categorie,
                   BigDecimal montant, String description) {
        this(0, dateDepense == null ? LocalDate.now() : dateDepense, categorie, montant, description, idFerme);
    }

    /**
     * Constructeur complet, utilisé par {@code DepenseDao} à la lecture de la base.
     *
     * @param id          identifiant en base (0 si nouvelle dépense)
     * @param dateDepense date de la dépense
     * @param categorie   catégorie
     * @param montant     montant en MAD, strictement positif
     * @param description description et fournisseur, facultative
     * @param idFerme     ferme concernée
     * @throws IllegalArgumentException si une donnée est invalide
     */
    public Depense(int id, LocalDate dateDepense, CategorieDepense categorie,
                   BigDecimal montant, String description, int idFerme) {
        this.id = id;
        setDateDepense(dateDepense);
        setCategorie(categorie);
        setMontant(montant);
        setDescription(description);
        setIdFerme(idFerme);
    }

    /** @return l'identifiant en base (0 si non enregistrée) */
    public int getId() {
        return id;
    }

    /** @param id l'identifiant généré par la base */
    public void setId(int id) {
        this.id = id;
    }

    /** @return la date de la dépense */
    public LocalDate getDateDepense() {
        return dateDepense;
    }

    /**
     * @param dateDepense date de la dépense, non nulle
     * @throws IllegalArgumentException si la date est absente
     */
    public void setDateDepense(LocalDate dateDepense) {
        if (dateDepense == null) {
            throw new IllegalArgumentException("La date de la dépense est obligatoire.");
        }
        this.dateDepense = dateDepense;
    }

    /** @return la catégorie */
    public CategorieDepense getCategorie() {
        return categorie;
    }

    /**
     * @param categorie catégorie, non nulle
     * @throws IllegalArgumentException si la catégorie est absente
     */
    public void setCategorie(CategorieDepense categorie) {
        if (categorie == null) {
            throw new IllegalArgumentException("La catégorie de la dépense est obligatoire.");
        }
        this.categorie = categorie;
    }

    /** @return le montant en MAD (2 décimales) */
    public BigDecimal getMontant() {
        return montant;
    }

    /**
     * @param montant montant en MAD, strictement positif (arrondi à 2 décimales)
     * @throws IllegalArgumentException si le montant est absent ou n'est pas strictement positif
     */
    public void setMontant(BigDecimal montant) {
        if (montant == null || montant.signum() <= 0) {
            throw new IllegalArgumentException("Le montant doit être un nombre strictement positif (MAD).");
        }
        this.montant = montant.setScale(2, RoundingMode.HALF_UP);
        if (this.montant.signum() <= 0) {
            throw new IllegalArgumentException("Le montant doit être au moins 0,01 MAD.");
        }
    }

    /** @return la description et le fournisseur (jamais {@code null}) */
    public String getDescription() {
        return description;
    }

    /**
     * @param description texte libre ; {@code null} devient une chaîne vide
     * @throws IllegalArgumentException si le texte dépasse {@value #DESCRIPTION_MAX} caractères
     */
    public void setDescription(String description) {
        String d = description == null ? "" : description.trim();
        if (d.length() > DESCRIPTION_MAX) {
            throw new IllegalArgumentException(
                    "La description ne doit pas dépasser " + DESCRIPTION_MAX + " caractères.");
        }
        this.description = d;
    }

    /** @return l'identifiant de la ferme */
    public int getIdFerme() {
        return idFerme;
    }

    /**
     * @param idFerme identifiant de la ferme, strictement positif
     * @throws IllegalArgumentException si l'identifiant n'est pas valide
     */
    public void setIdFerme(int idFerme) {
        if (idFerme <= 0) {
            throw new IllegalArgumentException("La ferme de la dépense est obligatoire.");
        }
        this.idFerme = idFerme;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Depense autre)) {
            return false;
        }
        return id != 0 && id == autre.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Depense{id=" + id + ", date=" + dateDepense + ", categorie=" + categorie
                + ", montant=" + montant + ", idFerme=" + idFerme + '}';
    }
}