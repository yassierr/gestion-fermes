package ma.ferme.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import ma.ferme.dao.DepenseDao;
import ma.ferme.model.Depense;
import ma.ferme.model.Utilisateur;
import ma.ferme.model.enums.CategorieDepense;

/**
 * Règles métier des dépenses (EFR09, EFR10), réservées au responsable de la ferme (EF-T04).
 * Les erreurs de saisie lèvent {@link IllegalArgumentException} ; un accès interdit lève
 * {@link SecurityException}.
 *
 * @author Mohamed Amine Ait Maait
 * @version 1.0
 */
public class DepenseService {

    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DepenseDao dao;

    /** Crée le service avec son DAO par défaut. */
    public DepenseService() {
        this(new DepenseDao());
    }

    /**
     * Crée le service avec un DAO donné (utile pour les tests).
     *
     * @param dao le DAO des dépenses
     */
    public DepenseService(DepenseDao dao) {
        this.dao = dao;
    }

    /**
     * Ajoute une dépense à la ferme du responsable (EFR09).
     *
     * @param acteur      le responsable de ferme connecté
     * @param date        date de la dépense ; aujourd'hui si {@code null}
     * @param categorie   catégorie, non nulle
     * @param montant     montant en MAD, strictement positif
     * @param description description et fournisseur, facultative
     * @return la dépense enregistrée
     * @throws SecurityException        si l'acteur n'est pas un responsable actif
     * @throws IllegalArgumentException si une donnée est invalide
     */
    public Depense ajouter(Utilisateur acteur, LocalDate date, CategorieDepense categorie,
                           BigDecimal montant, String description) {
        int idFerme = Droits.fermeDuResponsable(acteur);
        Depense depense = new Depense(idFerme, date, categorie, montant, description);
        dao.insert(depense);
        return depense;
    }

    /**
     * Modifie une dépense de la ferme (EFR10).
     *
     * @param acteur      le responsable de ferme connecté
     * @param idDepense   identifiant de la dépense
     * @param date        nouvelle date
     * @param categorie   nouvelle catégorie
     * @param montant     nouveau montant en MAD
     * @param description nouvelle description
     * @throws SecurityException        si la dépense est d'une autre ferme
     * @throws IllegalArgumentException si la dépense est introuvable ou si une valeur est invalide
     */
    public void modifier(Utilisateur acteur, int idDepense, LocalDate date, CategorieDepense categorie,
                         BigDecimal montant, String description) {
        int idFerme = Droits.fermeDuResponsable(acteur);
        Depense depense = trouver(idDepense, idFerme);
        depense.setDateDepense(date);
        depense.setCategorie(categorie);
        depense.setMontant(montant);
        depense.setDescription(description);
        dao.update(depense);
    }

    /**
     * Supprime une dépense de la ferme (EFR10).
     *
     * @param acteur    le responsable de ferme connecté
     * @param idDepense identifiant de la dépense
     * @throws SecurityException        si la dépense est d'une autre ferme
     * @throws IllegalArgumentException si la dépense est introuvable
     */
    public void supprimer(Utilisateur acteur, int idDepense) {
        int idFerme = Droits.fermeDuResponsable(acteur);
        trouver(idDepense, idFerme);
        dao.delete(idDepense);
    }

    /**
     * Liste les dépenses de la ferme, les plus récentes d'abord.
     *
     * @param acteur le responsable de ferme connecté
     * @return les dépenses de sa ferme
     */
    public List<Depense> lister(Utilisateur acteur) {
        return dao.findByFerme(Droits.fermeDuResponsable(acteur));
    }

    /**
     * Recherche dans les dépenses de la ferme (EFR10). Le texte est comparé, sans tenir compte
     * de la casse, à la description, au libellé de la catégorie, au montant et à la date (jj/mm/aaaa).
     *
     * @param acteur    le responsable de ferme connecté
     * @param texte     texte recherché ; vide pour tout lister
     * @param categorie catégorie à garder, ou {@code null} pour toutes
     * @param debut     première date à garder, ou {@code null}
     * @param fin       dernière date à garder, ou {@code null}
     * @return les dépenses qui correspondent
     */
    public List<Depense> rechercher(Utilisateur acteur, String texte, CategorieDepense categorie,
                                    LocalDate debut, LocalDate fin) {
        String t = texte == null ? "" : texte.trim().toLowerCase(Locale.ROOT);
        return lister(acteur).stream()
                .filter(d -> categorie == null || d.getCategorie() == categorie)
                .filter(d -> debut == null || !d.getDateDepense().isBefore(debut))
                .filter(d -> fin == null || !d.getDateDepense().isAfter(fin))
                .filter(d -> t.isEmpty()
                        || d.getDescription().toLowerCase(Locale.ROOT).contains(t)
                        || d.getCategorie().getLibelle().toLowerCase(Locale.ROOT).contains(t)
                        || d.getMontant().toPlainString().contains(t)
                        || JJ_MM_AAAA.format(d.getDateDepense()).contains(t))
                .toList();
    }

    /**
     * Total des dépenses d'un mois (EFR10).
     *
     * @param acteur le responsable de ferme connecté
     * @param mois   le mois voulu
     * @return le total en MAD
     */
    public BigDecimal totalDuMois(Utilisateur acteur, YearMonth mois) {
        return dao.total(Droits.fermeDuResponsable(acteur), mois.atDay(1), mois.atEndOfMonth());
    }

    private Depense trouver(int idDepense, int idFerme) {
        Depense depense = dao.findById(idDepense)
                .orElseThrow(() -> new IllegalArgumentException("Cette dépense n'existe plus."));
        Droits.exigerMemeFerme(depense.getIdFerme(), idFerme);
        return depense;
    }
}