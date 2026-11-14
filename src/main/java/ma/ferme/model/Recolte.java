package ma.ferme.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Pesée de tomates cerises récoltées dans une ferme : poids net en kg et variété.
 * La date et l'heure sont posées automatiquement à l'enregistrement (EFR06).
 * <p>
 * Objet du domaine : aucun accès à la base, aucun appel de DAO ou de service.
 * Les règles de stock (RG-01, RG-07) sont vérifiées par {@code RecolteService}.
 * </p>
 *
 * @author Mohamed Amine Ait Maait
 * @version 1.0
 */
public class Recolte {

    /** Taille maximale de la variété (colonne {@code variete VARCHAR(50)}). */
    public static final int VARIETE_MAX = 50;

    private int id;
    private LocalDateTime dateHeure;
    private BigDecimal poidsKg;
    private String variete;
    private int idFerme;

    /**
     * Crée une nouvelle récolte pas encore enregistrée, datée de maintenant.
     *
     * @param idFerme  ferme où la récolte a été faite
     * @param poidsKg  poids net en kg, strictement positif
     * @param variete  variété de tomate cerise, non vide
     * @throws IllegalArgumentException si une donnée est invalide
     */
    public Recolte(int idFerme, BigDecimal poidsKg, String variete) {
        this(0, LocalDateTime.now().withNano(0), poidsKg, variete, idFerme);
    }

    /**
     * Constructeur complet, utilisé par {@code RecolteDao} à la lecture de la base.
     *
     * @param id         identifiant en base (0 si nouvelle récolte)
     * @param dateHeure  date et heure de la pesée
     * @param poidsKg    poids net en kg, strictement positif
     * @param variete    variété, non vide
     * @param idFerme    ferme concernée
     * @throws IllegalArgumentException si une donnée est invalide
     */
    public Recolte(int id, LocalDateTime dateHeure, BigDecimal poidsKg, String variete, int idFerme) {
        this.id = id;
        setDateHeure(dateHeure);
        setPoidsKg(poidsKg);
        setVariete(variete);
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

    /** @return la date et l'heure de la pesée */
    public LocalDateTime getDateHeure() {
        return dateHeure;
    }

    /**
     * @param dateHeure date et heure de la pesée, non nulle
     * @throws IllegalArgumentException si la valeur est nulle
     */
    public void setDateHeure(LocalDateTime dateHeure) {
        if (dateHeure == null) {
            throw new IllegalArgumentException("La date de la récolte est obligatoire.");
        }
        this.dateHeure = dateHeure;
    }

    /** @return le poids net en kg (2 décimales) */
    public BigDecimal getPoidsKg() {
        return poidsKg;
    }

    /**
     * @param poidsKg poids net en kg, strictement positif (arrondi à 2 décimales)
     * @throws IllegalArgumentException si le poids est absent ou n'est pas strictement positif
     */
    public void setPoidsKg(BigDecimal poidsKg) {
        if (poidsKg == null || poidsKg.signum() <= 0) {
            throw new IllegalArgumentException("Le poids doit être un nombre strictement positif (kg).");
        }
        this.poidsKg = poidsKg.setScale(2, RoundingMode.HALF_UP);
        if (this.poidsKg.signum() <= 0) {
            throw new IllegalArgumentException("Le poids doit être au moins 0,01 kg.");
        }
    }

    /** @return la variété de tomate cerise */
    public String getVariete() {
        return variete;
    }

    /**
     * @param variete variété, non vide, {@value #VARIETE_MAX} caractères au maximum
     * @throws IllegalArgumentException si la variété est vide ou trop longue
     */
    public void setVariete(String variete) {
        if (variete == null || variete.isBlank()) {
            throw new IllegalArgumentException("La variété est obligatoire.");
        }
        String v = variete.trim();
        if (v.length() > VARIETE_MAX) {
            throw new IllegalArgumentException(
                    "La variété ne doit pas dépasser " + VARIETE_MAX + " caractères.");
        }
        this.variete = v;
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
            throw new IllegalArgumentException("La ferme de la récolte est obligatoire.");
        }
        this.idFerme = idFerme;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Recolte autre)) {
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
        return "Recolte{id=" + id + ", dateHeure=" + dateHeure + ", poidsKg=" + poidsKg
                + ", variete='" + variete + "', idFerme=" + idFerme + '}';
    }
}