package ma.ferme.service;

import ma.ferme.dao.RecuPaieDao;
import ma.ferme.model.LignePaie;
import ma.ferme.model.RecuPaie;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service métier de consultation et d'analyse des reçus de paie.
 */
public class RecuPaieService {

    private final RecuPaieDao recuPaieDao;

    public RecuPaieService() {
        this(new RecuPaieDao());
    }

    public RecuPaieService(RecuPaieDao recuPaieDao) {
        this.recuPaieDao = recuPaieDao;
    }

    /**
     * Récupère un reçu complet (avec ses lignes de détail) par son ID.
     */
    public Optional<RecuPaie> trouverParId(int idRecu) {
        if (idRecu <= 0) {
            throw new IllegalArgumentException("L'identifiant du reçu doit être positif.");
        }
        return recuPaieDao.findById(idRecu);
    }

    /**
     * Liste tous les reçus de paie d'une ferme donnée.
     */
    public List<RecuPaie> listerParFerme(int idFerme) {
        if (idFerme <= 0) {
            throw new IllegalArgumentException("L'identifiant de la ferme doit être positif.");
        }
        return recuPaieDao.findByFerme(idFerme);
    }

    /**
     * Liste tous les reçus de paie émis par un Agent RH.
     */
    public List<RecuPaie> listerParAgentRh(int idAgentRh) {
        if (idAgentRh <= 0) {
            throw new IllegalArgumentException("L'identifiant de l'agent RH doit être positif.");
        }
        return recuPaieDao.findByAgentRh(idAgentRh);
    }

    /**
     * Liste tous les reçus de paie toutes fermes confondues.
     */
    public List<RecuPaie> listerTous() {
        return recuPaieDao.findAll();
    }

    /**
     * Récupère les lignes de détail d'un reçu donné.
     */
    public List<LignePaie> listerLignesDuRecu(int idRecu) {
        if (idRecu <= 0) {
            throw new IllegalArgumentException("L'identifiant du reçu doit être positif.");
        }
        return recuPaieDao.findLignesByRecu(idRecu);
    }

    /**
     * Calcule la masse salariale totale payée sur une ferme sur une période de paiement donnée.
     */
    public BigDecimal totalPayeParFerme(int idFerme, LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null || fin.isBefore(debut)) {
            throw new IllegalArgumentException("La période de recherche est invalide.");
        }
        return listerParFerme(idFerme).stream()
                .filter(r -> !r.getDatePaiement().isBefore(debut) && !r.getDatePaiement().isAfter(fin))
                .map(RecuPaie::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}