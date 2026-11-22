<<<<<<< HEAD
package ma.ferme.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import ma.ferme.TestSupport;
import ma.ferme.dao.DaoException;
import ma.ferme.service.PaieService.ResultatPaie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests de {@link PaieService}. Les tests avec base creent leur propre ferme,
 * agent RH et ouvriers (sans toucher aux donnees de data.sql) et nettoient apres.
 */
class PaieServiceTest {

    private final PaieService service = new PaieService();
    private String suffixe;
    private int idFerme;
    private int idAgent;
    private int idOuvrier1;
    private int idOuvrier2;

    @Test
    void periodeObligatoire() {
        assertThrows(IllegalArgumentException.class, () -> service.payerFerme(1, null, LocalDate.now(), 1));
    }

    @Test
    void finAvantDebutRefusee() {
        assertThrows(IllegalArgumentException.class,
                () -> service.payerFerme(1, LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 1), 1));
    }

    @BeforeEach
    void preparerBase() {
        if (!TestSupport.baseDisponible()) {
            return;
        }
        suffixe = String.valueOf(System.nanoTime() % 1_000_000_000L);
        TestSupport.executer("INSERT INTO ferme(nom, localisation) VALUES ('Ferme PaieTest " + suffixe + "', 'Test')");
        idFerme = TestSupport.premierId("SELECT id FROM ferme WHERE nom = 'Ferme PaieTest " + suffixe + "'");
        TestSupport.executer("INSERT INTO utilisateur(nom_complet, login, mot_de_passe_hash, role, id_ferme) "
                + "VALUES ('Agent Test', 'rh" + suffixe + "', 'x', 'AGENT_RH', " + idFerme + ")");
        idAgent = TestSupport.premierId("SELECT id FROM utilisateur WHERE login = 'rh" + suffixe + "'");
        TestSupport.executer("INSERT INTO ouvrier(matricule, cin, nom_prenom, salaire_journalier, id_ferme) VALUES "
                + "('M1" + suffixe + "', 'C1" + suffixe + "', 'Ouvrier Un', 150, " + idFerme + "), "
                + "('M2" + suffixe + "', 'C2" + suffixe + "', 'Ouvrier Deux', 200, " + idFerme + ")");
        idOuvrier1 = TestSupport.premierId("SELECT id FROM ouvrier WHERE matricule = 'M1" + suffixe + "'");
        idOuvrier2 = TestSupport.premierId("SELECT id FROM ouvrier WHERE matricule = 'M2" + suffixe + "'");
        // Ouvrier 1 : 3 jours presents + 1 absent ; Ouvrier 2 : 2 jours presents
        TestSupport.executer("INSERT INTO pointage(date_pointage, statut, id_ouvrier) VALUES "
                + "('2026-03-01','PRESENT'," + idOuvrier1 + "), ('2026-03-02','PRESENT'," + idOuvrier1 + "), "
                + "('2026-03-03','PRESENT'," + idOuvrier1 + "), ('2026-03-04','ABSENT'," + idOuvrier1 + "), "
                + "('2026-03-01','PRESENT'," + idOuvrier2 + "), ('2026-03-02','PRESENT'," + idOuvrier2 + ")");
=======

package ma.ferme.service;

import ma.ferme.dao.DaoException;
import ma.ferme.dao.RecuPaieDao;

import ma.ferme.TestSupport;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PaieServiceTest {

    private PaieService service;

    private int idFerme;
    private int idRh;
    private int idOuvrier1;
    private int idOuvrier2;

    private static final LocalDate DEBUT =
            LocalDate.of(2026, 3, 1);

    private static final LocalDate FIN =
            LocalDate.of(2026, 3, 31);

    @BeforeEach
    void preparerBase() {

        if (!TestSupport.baseDisponible()) {
            return;
        }

        long suffix = System.nanoTime();

        idFerme = TestSupport.premierId(
                "INSERT INTO ferme(nom, localisation) " +
                "VALUES ('Ferme PaieTest " + suffix +
                "', 'Test') RETURNING id"
        );

        idRh = TestSupport.premierId(
                "INSERT INTO utilisateur(" +
                "nom_complet, login, mot_de_passe_hash, role, actif, id_ferme" +
                ") VALUES (" +
                "'RH Test', 'rh" + suffix +
                "', 'hash', 'AGENT_RH', TRUE, " + idFerme +
                ") RETURNING id"
        );

        idOuvrier1 = TestSupport.premierId(
                "INSERT INTO ouvrier(" +
                "matricule, cin, nom_prenom, salaire_journalier, actif, id_ferme" +
                ") VALUES (" +
                "'M1" + suffix +
                "', 'C1" + suffix +
                "', 'Ouvrier Test 1', 150, TRUE, " + idFerme +
                ") RETURNING id"
        );

        idOuvrier2 = TestSupport.premierId(
                "INSERT INTO ouvrier(" +
                "matricule, cin, nom_prenom, salaire_journalier, actif, id_ferme" +
                ") VALUES (" +
                "'M2" + suffix +
                "', 'C2" + suffix +
                "', 'Ouvrier Test 2', 200, TRUE, " + idFerme +
                ") RETURNING id"
        );

        // Ouvrier 1 : 3 jours présents + 1 jour absent

        ajouterPointage(idOuvrier1, LocalDate.of(2026, 3, 1), "PRESENT");
        ajouterPointage(idOuvrier1, LocalDate.of(2026, 3, 2), "PRESENT");
        ajouterPointage(idOuvrier1, LocalDate.of(2026, 3, 3), "PRESENT");
        ajouterPointage(idOuvrier1, LocalDate.of(2026, 3, 4), "ABSENT");

        // Ouvrier 2 : 2 jours présents

        ajouterPointage(idOuvrier2, LocalDate.of(2026, 3, 1), "PRESENT");
        ajouterPointage(idOuvrier2, LocalDate.of(2026, 3, 2), "PRESENT");

        service = new PaieService(new RecuPaieDao());
>>>>>>> main
    }

    @AfterEach
    void nettoyer() {
<<<<<<< HEAD
        if (suffixe == null) {
            return;
        }
        TestSupport.executer("UPDATE pointage SET id_recu = NULL WHERE id_ouvrier IN (" + idOuvrier1 + "," + idOuvrier2 + ")");
        TestSupport.executer("DELETE FROM recu_paie WHERE id_ferme = " + idFerme); // supprime aussi ligne_paie (cascade)
        TestSupport.executer("DELETE FROM pointage WHERE id_ouvrier IN (" + idOuvrier1 + "," + idOuvrier2 + ")");
        TestSupport.executer("DELETE FROM ouvrier WHERE id_ferme = " + idFerme);
        TestSupport.executer("DELETE FROM utilisateur WHERE id = " + idAgent);
        TestSupport.executer("DELETE FROM ferme WHERE id = " + idFerme);
    }

    @Test
    void soldeEstJoursPresentsFoisSalaire() {
        assumeTrue(suffixe != null, "Base non disponible : test ignore");
        assertEquals(0, new BigDecimal("450").compareTo(service.soldeOuvrier(idOuvrier1))); // 3 x 150
        assertEquals(0, new BigDecimal("400").compareTo(service.soldeOuvrier(idOuvrier2))); // 2 x 200
    }

    @Test
    void payerCreeLeRecuEtMarqueLesPointages() {
        assumeTrue(suffixe != null, "Base non disponible : test ignore");
        ResultatPaie r = service.payerFerme(idFerme, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), idAgent);
        assertTrue(r.numero().matches("RP-\\d{4}-\\d{4}"));
        assertEquals(2, r.lignes().size());
        assertEquals(0, new BigDecimal("850").compareTo(r.total())); // 450 + 400
        // les soldes sont remis a zero
        assertEquals(0, BigDecimal.ZERO.compareTo(service.soldeOuvrier(idOuvrier1)));
        assertEquals(0, BigDecimal.ZERO.compareTo(service.soldeOuvrier(idOuvrier2)));
    }

    @Test
    void ondoitPasPayerDeuxFoisLesMemesJours() {
        assumeTrue(suffixe != null, "Base non disponible : test ignore");
        service.payerFerme(idFerme, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), idAgent);
        assertThrows(IllegalStateException.class,
                () -> service.payerFerme(idFerme, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), idAgent));
    }

    @Test
    void periodeSansJourAPayerRefusee() {
        assumeTrue(suffixe != null, "Base non disponible : test ignore");
        assertThrows(IllegalStateException.class,
                () -> service.payerFerme(idFerme, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), idAgent));
    }

    @Test
    void erreurPendantLePaiementAnnuleTout() {
        assumeTrue(suffixe != null, "Base non disponible : test ignore");
        // agent RH inexistant -> la creation du recu echoue -> rien ne doit etre marque paye
        assertThrows(DaoException.class,
                () -> service.payerFerme(idFerme, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 999999));
        assertEquals(0, new BigDecimal("450").compareTo(service.soldeOuvrier(idOuvrier1)));
    }
}
=======

        if (!TestSupport.baseDisponible() || idFerme == 0) {
            return;
        }

        TestSupport.executer(
                "UPDATE pointage SET id_recu = NULL " +
                "WHERE id_ouvrier IN (" +
                idOuvrier1 + ", " + idOuvrier2 + ")"
        );

        TestSupport.executer(
                "DELETE FROM recu_paie WHERE id_ferme = " + idFerme
        );

        TestSupport.executer(
                "DELETE FROM pointage WHERE id_ouvrier IN (" +
                idOuvrier1 + ", " + idOuvrier2 + ")"
        );

        TestSupport.executer(
                "DELETE FROM ouvrier WHERE id IN (" +
                idOuvrier1 + ", " + idOuvrier2 + ")"
        );

        TestSupport.executer(
                "DELETE FROM utilisateur WHERE id = " + idRh
        );

        TestSupport.executer(
                "DELETE FROM ferme WHERE id = " + idFerme
        );
    }

    // =========================================================
    // AJOUT POINTAGE
    // =========================================================

    private void ajouterPointage(
            int idOuvrier,
            LocalDate date,
            String statut
    ) {

        TestSupport.executer(
                "INSERT INTO pointage(" +
                "date_pointage, statut, id_ouvrier" +
                ") VALUES (" +
                "'" + date + "', '" + statut + "', " + idOuvrier +
                ")"
        );
    }

    // =========================================================
    // TESTS VALIDATION PERIODE
    // =========================================================

    @Test
    void periodeObligatoire() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.payerFerme(
                        idFerme,
                        null,
                        FIN,
                        idRh
                )
        );
    }

    @Test
    void finAvantDebutRefusee() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.payerFerme(
                        idFerme,
                        FIN,
                        DEBUT,
                        idRh
                )
        );
    }

    // =========================================================
    // TEST SOLDE OUVRIER
    // =========================================================

    @Test
    void soldeEstJoursPresentsFoisSalaire() {

        BigDecimal solde1 = service.soldeOuvrier(idOuvrier1);

        BigDecimal solde2 = service.soldeOuvrier(idOuvrier2);

        assertEquals(
                new BigDecimal("450.00"),
                solde1
        );

        assertEquals(
                new BigDecimal("400.00"),
                solde2
        );
    }

    // =========================================================
    // TEST PAIEMENT
    // =========================================================

    @Test
    void payerCreeLeRecuEtMarqueLesPointages() {

        PaieService.ResultatPaie resultat =
                service.payerFerme(
                        idFerme,
                        DEBUT,
                        FIN,
                        idRh
                );

        // Le reçu doit exister

        assertNotNull(resultat);

        assertTrue(resultat.idRecu() > 0);

        assertNotNull(resultat.numero());

        // Format numéro : RP-2026-000001

        assertTrue(
                resultat.numero().matches("RP-\\d{4}-\\d{6}"),
                "Format du numéro incorrect : " + resultat.numero()
        );

        // Deux ouvriers doivent être payés

        assertEquals(2, resultat.lignes().size());

        // Vérification du nombre de jours par ouvrier
        // IMPORTANT : on ne dépend pas de l'ordre des lignes

        PaieService.LigneRecu ligne1 =
                resultat.lignes()
                        .stream()
                        .filter(l -> l.idOuvrier() == idOuvrier1)
                        .findFirst()
                        .orElseThrow();

        PaieService.LigneRecu ligne2 =
                resultat.lignes()
                        .stream()
                        .filter(l -> l.idOuvrier() == idOuvrier2)
                        .findFirst()
                        .orElseThrow();

        assertEquals(3, ligne1.jours());

        assertEquals(2, ligne2.jours());

        // Vérification des salaires journaliers

        assertEquals(
                new BigDecimal("150.00"),
                ligne1.salaireJournalier()
        );

        assertEquals(
                new BigDecimal("200.00"),
                ligne2.salaireJournalier()
        );

        // Vérification des montants individuels

        assertEquals(
                new BigDecimal("450.00"),
                ligne1.montant()
        );

        assertEquals(
                new BigDecimal("400.00"),
                ligne2.montant()
        );

        // Vérification du montant total

        assertEquals(
                new BigDecimal("850.00"),
                resultat.total()
        );

        // Vérification du nombre de lignes en base

        int nombreLignes = TestSupport.premierId(
                "SELECT COUNT(*) FROM ligne_paie " +
                "WHERE id_recu = " + resultat.idRecu()
        );

        assertEquals(2, nombreLignes);

        // Vérification de l'association des pointages
        // au reçu de paiement

        int pointagesPayes = TestSupport.premierId(
                "SELECT COUNT(*) FROM pointage " +
                "WHERE id_ouvrier IN (" +
                idOuvrier1 + ", " + idOuvrier2 +
                ") AND id_recu = " + resultat.idRecu()
        );

        assertEquals(5, pointagesPayes);

        // Aucun pointage PRESENT ne doit rester impayé
        // dans cette période

        int pointagesPresentNonPayes = TestSupport.premierId(
                "SELECT COUNT(*) FROM pointage " +
                "WHERE id_ouvrier IN (" +
                idOuvrier1 + ", " + idOuvrier2 +
                ") AND statut = 'PRESENT' " +
                "AND id_recu IS NULL " +
                "AND date_pointage BETWEEN '" + DEBUT +
                "' AND '" + FIN + "'"
        );

        assertEquals(0, pointagesPresentNonPayes);

        // Le pointage ABSENT ne doit pas être payé

        int absentsPayes = TestSupport.premierId(
                "SELECT COUNT(*) FROM pointage " +
                "WHERE id_ouvrier = " + idOuvrier1 +
                " AND statut = 'ABSENT' " +
                "AND id_recu = " + resultat.idRecu()
        );

        assertEquals(0, absentsPayes);

        // Les soldes doivent être à zéro après paiement

        assertEquals(
                BigDecimal.ZERO.setScale(2),
                service.soldeOuvrier(idOuvrier1)
        );

        assertEquals(
                BigDecimal.ZERO.setScale(2),
                service.soldeOuvrier(idOuvrier2)
        );
    }

    // =========================================================
    // TEST DOUBLE PAIEMENT
    // =========================================================

    @Test
    void onNeDoitPasPayerDeuxFoisLesMemesJours() {

        service.payerFerme(
                idFerme,
                DEBUT,
                FIN,
                idRh
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.payerFerme(
                        idFerme,
                        DEBUT,
                        FIN,
                        idRh
                )
        );
    }

    // =========================================================
    // TEST PERIODE SANS POINTAGE A PAYER
    // =========================================================

    @Test
    void periodeSansJourAPayerRefusee() {

        LocalDate debutAvril =
                LocalDate.of(2026, 4, 1);

        LocalDate finAvril =
                LocalDate.of(2026, 4, 30);

        assertThrows(
                IllegalStateException.class,
                () -> service.payerFerme(
                        idFerme,
                        debutAvril,
                        finAvril,
                        idRh
                )
        );
    }

    // =========================================================
    // TEST ROLLBACK
    // =========================================================

    @Test
    void erreurPendantLePaiementAnnuleTout() {

        BigDecimal solde1Avant =
                service.soldeOuvrier(idOuvrier1);

        BigDecimal solde2Avant =
                service.soldeOuvrier(idOuvrier2);

        assertThrows(
                DaoException.class,
                () -> service.payerFerme(
                        idFerme,
                        DEBUT,
                        FIN,
                        999999
                )
        );

        // Les soldes doivent rester identiques

        assertEquals(
                solde1Avant,
                service.soldeOuvrier(idOuvrier1)
        );

        assertEquals(
                solde2Avant,
                service.soldeOuvrier(idOuvrier2)
        );

        // Aucun reçu ne doit avoir été créé

        int nombreRecus = TestSupport.premierId(
                "SELECT COUNT(*) FROM recu_paie " +
                "WHERE id_ferme = " + idFerme
        );

        assertEquals(0, nombreRecus);

        // Aucun pointage ne doit être associé à un reçu

        int pointagesPayes = TestSupport.premierId(
                "SELECT COUNT(*) FROM pointage " +
                "WHERE id_ouvrier IN (" +
                idOuvrier1 + ", " + idOuvrier2 +
                ") AND id_recu IS NOT NULL"
        );

        assertEquals(0, pointagesPayes);
    }
}
>>>>>>> main
