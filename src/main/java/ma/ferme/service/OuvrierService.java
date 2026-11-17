package ma.ferme.service;

import ma.ferme.dao.OuvrierDao;
import ma.ferme.dao.PointageDao;
import ma.ferme.model.Ouvrier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OuvrierService {

    private final OuvrierDao ouvrierDao;
    private final PointageDao pointageDao;

    // Constructeur par défaut (utilisé par les Controllers JavaFX)
    public OuvrierService() {
        this(new OuvrierDao(), new PointageDao());
    }

    // Constructeur avec injection (utilisé pour les tests unitaires)
    public OuvrierService(OuvrierDao ouvrierDao, PointageDao pointageDao) {
        this.ouvrierDao = ouvrierDao;
        this.pointageDao = pointageDao;
    }

    /**
     * Ajoute un nouvel ouvrier après vérification des règles métier.
     */
    public void ajouterOuvrier(Ouvrier ouvrier) {
        if (ouvrier == null) {
            throw new IllegalArgumentException("L'ouvrier ne peut pas être nul.");
        }
        validerSalaire(ouvrier.getSalaireJournalier());

        // Vérifie l'unicité du CIN avant insertion en base
        boolean cinExiste = ouvrierDao.findAll().stream()
                .anyMatch(o -> o.getCin().equalsIgnoreCase(ouvrier.getCin().trim()));
        if (cinExiste) {
            throw new IllegalArgumentException("Un ouvrier avec le CIN '" + ouvrier.getCin() + "' existe déjà.");
        }

        ouvrierDao.insert(ouvrier);
    }

    /**
     * Met à jour les informations d'un ouvrier existant.
     */
    public void modifierOuvrier(Ouvrier ouvrier) {
        if (ouvrier == null || ouvrier.getId() <= 0) {
            throw new IllegalArgumentException("Ouvrier invalide pour la mise à jour.");
        }
        validerSalaire(ouvrier.getSalaireJournalier());

        Optional<Ouvrier> existant = ouvrierDao.findById(ouvrier.getId());
        if (existant.isEmpty()) {
            throw new IllegalArgumentException("Aucun ouvrier trouvé avec l'ID : " + ouvrier.getId());
        }

        // Vérifie que le CIN n'est pas déjà pris par un AUTRE ouvrier
        boolean cinPrisParAutre = ouvrierDao.findAll().stream()
                .anyMatch(o -> o.getId() != ouvrier.getId()
                        && o.getCin().equalsIgnoreCase(ouvrier.getCin().trim()));
        if (cinPrisParAutre) {
            throw new IllegalArgumentException("Le CIN '" + ouvrier.getCin() + "' est déjà attribué à un autre ouvrier.");
        }

        ouvrierDao.update(ouvrier);
    }

    /**
     * Désactive un ouvrier (soft delete) pour conserver son historique de pointages et de paie.
     */
    public void desactiverOuvrier(int idOuvrier) {
        Ouvrier ouvrier = ouvrierDao.findById(idOuvrier)
                .orElseThrow(() -> new IllegalArgumentException("Ouvrier introuvable (ID: " + idOuvrier + ")"));

        ouvrier.setActif(false);
        ouvrierDao.update(ouvrier);
    }

    /**
     * Réactive un ouvrier.
     */
    public void reactiverOuvrier(int idOuvrier) {
        Ouvrier ouvrier = ouvrierDao.findById(idOuvrier)
                .orElseThrow(() -> new IllegalArgumentException("Ouvrier introuvable (ID: " + idOuvrier + ")"));

        ouvrier.setActif(true);
        ouvrierDao.update(ouvrier);
    }

    /**
     * Supprime définitivement un ouvrier uniquement s'il n'a aucun historique de pointage.
     */
    public void supprimerOuvrier(int idOuvrier) {
        if (ouvrierDao.findById(idOuvrier).isEmpty()) {
            throw new IllegalArgumentException("Ouvrier introuvable (ID: " + idOuvrier + ")");
        }

        if (!pointageDao.findByOuvrier(idOuvrier).isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer cet ouvrier car il possède un historique de pointages. Désactivez-le plutôt.");
        }

        ouvrierDao.delete(idOuvrier);
    }

    public Optional<Ouvrier> trouverParId(int id) {
        return ouvrierDao.findById(id);
    }

    public List<Ouvrier> listerTous() {
        return ouvrierDao.findAll();
    }

    public List<Ouvrier> listerParFerme(int idFerme) {
        return ouvrierDao.findByFerme(idFerme);
    }

    public List<Ouvrier> listerActifsParFerme(int idFerme) {
        return ouvrierDao.findByFerme(idFerme).stream()
                .filter(Ouvrier::isActif)
                .collect(Collectors.toList());
    }

    private void validerSalaire(BigDecimal salaire) {
        if (salaire == null || salaire.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le salaire journalier ne peut pas être nul ou négatif.");
        }
    }
}