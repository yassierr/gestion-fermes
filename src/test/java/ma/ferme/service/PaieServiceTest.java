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
    }

    @AfterEach
    void nettoyer() {
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