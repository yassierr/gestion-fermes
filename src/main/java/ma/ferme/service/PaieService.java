package ma.ferme.service;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.dao.DaoException;
import ma.ferme.dao.RecuPaieDao;
import ma.ferme.dao.RecuPaieDao.LignePaiement;
import ma.ferme.model.RecuPaie;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service métier de gestion des paiements des ouvriers.
 *
 * Les règles métier sont centralisées ici.
 * Les requêtes SQL sont déléguées à RecuPaieDao.
 */
public class PaieService {

    private final RecuPaieDao recuPaieDao;

    public PaieService() {
        this.recuPaieDao = new RecuPaieDao();
    }

    public PaieService(RecuPaieDao recuPaieDao) {
        this.recuPaieDao = recuPaieDao;
    }

    // =========================================================
    // RECORDS MÉTIER
    // =========================================================

    /**
     * Ligne d'un reçu de paie.
     */
    public record LigneRecu(
            int idOuvrier,
            String matricule,
            String nomPrenom,
            int jours,
            BigDecimal salaireJournalier,
            BigDecimal montant
    ) {
    }

    /**
     * Résultat complet d'un paiement.
     */
    public record ResultatPaie(
            int idRecu,
            String numero,
            LocalDate datePaiement,
            LocalDate periodeDebut,
            LocalDate periodeFin,
            int idFerme,
            BigDecimal total,
            List<LigneRecu> lignes
    ) {
    }

    /**
     * Résumé d'un reçu pour affichage.
     */
    public record ResumeRecu(
            int idRecu,
            String numero,
            LocalDate datePaiement,
            LocalDate periodeDebut,
            LocalDate periodeFin,
            int nbOuvriers,
            BigDecimal total
    ) {
    }

    // =========================================================
    // PAIEMENT D'UNE FERME
    // =========================================================

    /**
     * Paie tous les jours PRESENT non payés d'une ferme
     * sur la période demandée.
     *
     * Le paiement est atomique :
     * - création du reçu ;
     * - insertion des lignes ;
     * - marquage des pointages ;
     * - validation de la transaction.
     *
     * Si une opération échoue, la transaction est annulée.
     */
    public ResultatPaie payerFerme(
            int idFerme,
            LocalDate debut,
            LocalDate fin,
            int idAgentRh
    ) {

        // Validation métier
        validerPeriode(debut, fin);

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                // Empêcher les paiements concurrents
                try (Statement stmt = conn.createStatement()) {

                    stmt.execute(
                            "LOCK TABLE recu_paie "
                                    + "IN SHARE ROW EXCLUSIVE MODE"
                    );
                }

                // Récupérer les jours PRESENT non payés
                List<LignePaiement> lignesPaiement =
                        recuPaieDao.calculerLignesPaiement(
                                conn,
                                idFerme,
                                debut,
                                fin
                        );

                // Vérification métier
                if (lignesPaiement.isEmpty()) {

                    throw new IllegalStateException(
                            "Aucun jour à payer sur cette période."
                    );
                }

                // Calcul du total
                BigDecimal total = calculerTotal(lignesPaiement);

                // Date du paiement
                LocalDate datePaiement = LocalDate.now();

                // Génération du numéro
                String numero = recuPaieDao.prochainNumero(conn);

                // Création du reçu
                int idRecu = recuPaieDao.insererRecu(
                        conn,
                        numero,
                        datePaiement,
                        debut,
                        fin,
                        total,
                        idFerme,
                        idAgentRh
                );

                // Insertion des lignes de paiement
                recuPaieDao.insererLignes(
                        conn,
                        idRecu,
                        lignesPaiement
                );

                // Marquer les pointages comme payés
                recuPaieDao.marquerPointagesPayes(conn, idRecu, lignesPaiement, debut, fin);

                // Validation transactionnelle
                conn.commit();

                // Conversion vers le résultat métier
                List<LigneRecu> lignes = convertirLignes(lignesPaiement);

                return new ResultatPaie(
                        idRecu,
                        numero,
                        datePaiement,
                        debut,
                        fin,
                        idFerme,
                        total,
                        lignes
                );

            } catch (SQLException | RuntimeException e) {

                conn.rollback();
                throw e;

            } finally {

                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {

            throw new DaoException(
                    "Paiement impossible : " + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // VALIDATION MÉTIER
    // =========================================================

    private void validerPeriode(
            LocalDate debut,
            LocalDate fin
    ) {

        if (debut == null || fin == null) {

            throw new IllegalArgumentException(
                    "La période est obligatoire."
            );
        }

        if (fin.isBefore(debut)) {

            throw new IllegalArgumentException(
                    "La date de fin doit être après la date de début."
            );
        }
    }

    // =========================================================
    // CALCUL DU TOTAL
    // =========================================================

    private BigDecimal calculerTotal(List<LignePaiement> lignes) {

        return lignes.stream()
                .map(LignePaiement::montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // =========================================================
    // CONVERSION DES LIGNES
    // =========================================================

    private List<LigneRecu> convertirLignes(
            List<LignePaiement> lignes
    ) {

        List<LigneRecu> resultat = new ArrayList<>();

        for (LignePaiement ligne : lignes) {

            resultat.add(new LigneRecu(
                    ligne.idOuvrier(),
                    ligne.matricule(),
                    ligne.nomPrenom(),
                    ligne.jours(),
                    ligne.salaireJournalier(),
                    ligne.montant()
            ));
        }

        return resultat;
    }

    // =========================================================
    // SOLDE D'UN OUVRIER
    // =========================================================

    /**
     * Calcule le solde des jours PRESENT non payés d'un ouvrier.
     */
    public BigDecimal soldeOuvrier(int idOuvrier) {

        if (idOuvrier <= 0) {

            throw new IllegalArgumentException(
                    "Identifiant ouvrier invalide."
            );
        }

        return recuPaieDao.calculerSoldeOuvrier(idOuvrier);
    }

    // =========================================================
    // LISTE DES REÇUS D'UNE FERME
    // =========================================================

    /**
     * Retourne les reçus d'une ferme pour affichage.
     */
    public List<ResumeRecu> listerRecus(int idFerme) {

        if (idFerme <= 0) {

            throw new IllegalArgumentException(
                    "Identifiant ferme invalide."
            );
        }

        return recuPaieDao.listerResumeRecus(idFerme)
                .stream()
                .map(r -> new ResumeRecu(
                        r.idRecu(),
                        r.numero(),
                        r.datePaiement(),
                        r.periodeDebut(),
                        r.periodeFin(),
                        r.nbOuvriers(),
                        r.total()
                ))
                .toList();
    }

    // =========================================================
    // EFFECTIF ACTIF
    // =========================================================

    /**
     * Retourne le nombre d'ouvriers actifs d'une ferme.
     */
    public int effectifActif(int idFerme) {

        if (idFerme <= 0) {

            throw new IllegalArgumentException(
                    "Identifiant ferme invalide."
            );
        }

        return recuPaieDao.compterOuvriersActifs(idFerme);
    }

    // =========================================================
    // CHARGEMENT D'UN REÇU
    // =========================================================

    /**
     * Charge un reçu de paie.
     */
    public Optional<RecuPaie> chargerRecu(int idRecu) {

        if (idRecu <= 0) {

            throw new IllegalArgumentException(
                    "Identifiant reçu invalide."
            );
        }

        return recuPaieDao.findById(idRecu);
    }

    // =========================================================
    // CHARGEMENT POUR PDF
    // =========================================================

    /**
     * Charge les données nécessaires à la génération PDF.
     */
    public Optional<ResultatPaie> chargerRecuPdf(int idRecu) {

        if (idRecu <= 0) {

            throw new IllegalArgumentException(
                    "Identifiant reçu invalide."
            );
        }

        return recuPaieDao.chargerDetailsPdf(idRecu)
                .map(details -> {

                    List<LigneRecu> lignes = details.lignes()
                            .stream()
                            .map(ligne -> new LigneRecu(
                                    ligne.idOuvrier(),
                                    ligne.matricule(),
                                    ligne.nomPrenom(),
                                    ligne.jours(),
                                    ligne.salaireJournalier(),
                                    ligne.montant()
                            ))
                            .toList();

                    return new ResultatPaie(
                            details.idRecu(),
                            details.numero(),
                            details.datePaiement(),
                            details.periodeDebut(),
                            details.periodeFin(),
                            details.idFerme(),
                            details.total(),
                            lignes
                    );
                });
    }
}