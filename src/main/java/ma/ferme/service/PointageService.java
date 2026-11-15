package ma.ferme.service;

import ma.ferme.dao.OuvrierDao;
import ma.ferme.dao.PointageDao;
import ma.ferme.model.Ouvrier;
import ma.ferme.model.Pointage;
import ma.ferme.model.enums.StatutPointage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PointageService {

    private final PointageDao pointageDao;
    private final OuvrierDao ouvrierDao;

    // Constructeur par défaut (utilisé par les Controllers JavaFX)
    public PointageService() {
        this(new PointageDao(), new OuvrierDao());
    }

    // Constructeur avec injection (utilisé pour les tests unitaires)
    public PointageService(PointageDao pointageDao, OuvrierDao ouvrierDao) {
        this.pointageDao = pointageDao;
        this.ouvrierDao = ouvrierDao;
    }

    /**
     * Enregistre ou met à jour le pointage d'un ouvrier pour une date donnée.
     */
    public Pointage pointerOuvrier(int idOuvrier, LocalDate date, StatutPointage statut) {
        if (date == null) {
            throw new IllegalArgumentException("La date de pointage ne peut pas être nulle.");
        }
        if (statut == null) {
            throw new IllegalArgumentException("Le statut de pointage ne peut pas être nul.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Impossible d'enregistrer un pointage dans le futur.");
        }

        Ouvrier ouvrier = ouvrierDao.findById(idOuvrier)
                .orElseThrow(() -> new IllegalArgumentException("Ouvrier introuvable (ID: " + idOuvrier + ")"));

        if (!ouvrier.isActif()) {
            throw new IllegalStateException("Impossible de pointer un ouvrier inactif (" + ouvrier.getNomEtPrenom() + ").");
        }

        // Vérifie si un pointage existe déjà pour cet ouvrier à cette date (contrainte ux_pointage_jour)
        Optional<Pointage> existant = pointageDao.findByOuvrier(idOuvrier).stream()
                .filter(p -> p.getDatePointage().equals(date))
                .findFirst();

        if (existant.isPresent()) {
            Pointage p = existant.get();
            p.setStatut(statut);
            pointageDao.update(p);
            return p;
        } else {
            Pointage nouveau = new Pointage(idOuvrier, date, statut);
            pointageDao.insert(nouveau);
            return nouveau;
        }
    }

    /**
     * Récupère tous les pointages d'un ouvrier sur une période donnée (inclusive).
     */
    public List<Pointage> listerPointagesPeriode(int idOuvrier, LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null || debut.isAfter(fin)) {
            throw new IllegalArgumentException("Période invalide : la date de début doit précéder ou égaler la date de fin.");
        }

        return pointageDao.findByOuvrier(idOuvrier).stream()
                .filter(p -> !p.getDatePointage().isBefore(debut) && !p.getDatePointage().isAfter(fin))
                .collect(Collectors.toList());
    }

    /**
     * Calcule le nombre total de jours travaillés (PRESENT = 1.0, ABSENT = 0.0) sur une période.
     */
    public double calculerJoursTravailles(int idOuvrier, LocalDate debut, LocalDate fin) {
        return listerPointagesPeriode(idOuvrier, debut, fin).stream()
                .mapToDouble(Pointage::getValeurJournee)
                .sum();
    }

    /**
     * Calcule le montant total dû à un ouvrier sur une période donnée (en BigDecimal).
     */
    public BigDecimal calculerMontantPeriode(int idOuvrier, LocalDate debut, LocalDate fin) {
        Ouvrier ouvrier = ouvrierDao.findById(idOuvrier)
                .orElseThrow(() -> new IllegalArgumentException("Ouvrier introuvable (ID: " + idOuvrier + ")"));

        double jours = calculerJoursTravailles(idOuvrier, debut, fin);
        return ouvrier.calculerMontant(jours);
    }

    public List<Pointage> listerParOuvrier(int idOuvrier) {
        return pointageDao.findByOuvrier(idOuvrier);
    }

    public void supprimerPointage(int idPointage) {
        pointageDao.delete(idPointage);
    }
}