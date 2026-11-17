package ma.ferme.service;

import ma.ferme.dao.OuvrierDao;
import ma.ferme.dao.PointageDao;
import ma.ferme.model.Ouvrier;
import ma.ferme.model.Pointage;
import ma.ferme.model.enums.StatutPointage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PointageServiceTest {

    private PointageService pointageService;
    private OuvrierService ouvrierService;
    private PointageDao pointageDao;
    private OuvrierDao ouvrierDao;
    private int idOuvrierTest;

    @BeforeEach
    void setUp() {
        pointageDao = new PointageDao();
        ouvrierDao = new OuvrierDao();
        pointageService = new PointageService(pointageDao, ouvrierDao);
        ouvrierService = new OuvrierService(ouvrierDao, pointageDao);

        Ouvrier ouvrier = new Ouvrier(1, "Ouvrier Service Pointage", "PTG999", "0699999999", "Biougra", "Serre A", new BigDecimal("150.00"));
        ouvrierService.ajouterOuvrier(ouvrier);
        idOuvrierTest = ouvrier.getId();
    }

    @AfterEach
    void tearDown() {
        for (Pointage p : pointageDao.findByOuvrier(idOuvrierTest)) {
            pointageDao.delete(p.getId());
        }
        ouvrierDao.delete(idOuvrierTest);
    }

    @Test
    @DisplayName("Devrait enregistrer un pointage puis le mettre à jour si pointé à nouveau le même jour")
    void testPointerOuvrierCreationEtMiseAJourMemeJour() {
        LocalDate aujourdhui = LocalDate.now();

        Pointage p1 = pointageService.pointerOuvrier(idOuvrierTest, aujourdhui, StatutPointage.ABSENT);
        assertEquals(StatutPointage.ABSENT, p1.getStatut());

        // Deuxième pointage le même jour : doit mettre à jour sans créer de doublon
        Pointage p2 = pointageService.pointerOuvrier(idOuvrierTest, aujourdhui, StatutPointage.PRESENT);
        assertEquals(p1.getId(), p2.getId());
        assertEquals(StatutPointage.PRESENT, p2.getStatut());
        assertEquals(1, pointageService.listerParOuvrier(idOuvrierTest).size());
    }

    @Test
    @DisplayName("Devrait refuser le pointage d'un ouvrier inactif")
    void testPointerOuvrierInactifBloque() {
        ouvrierService.desactiverOuvrier(idOuvrierTest);

        assertThrows(IllegalStateException.class, () ->
                pointageService.pointerOuvrier(idOuvrierTest, LocalDate.now(), StatutPointage.PRESENT)
        );
    }

    @Test
    @DisplayName("Devrait refuser un pointage dans le futur")
    void testPointerDateFutureBloque() {
        LocalDate demain = LocalDate.now().plusDays(1);

        assertThrows(IllegalArgumentException.class, () ->
                pointageService.pointerOuvrier(idOuvrierTest, demain, StatutPointage.PRESENT)
        );
    }

    @Test
    @DisplayName("Devrait calculer correctement les jours travaillés et le montant total sur une période")
    void testCalculerJoursEtMontantPeriode() {
        LocalDate jMinus2 = LocalDate.now().minusDays(2);
        LocalDate jMinus1 = LocalDate.now().minusDays(1);
        LocalDate aujourdhui = LocalDate.now();

        pointageService.pointerOuvrier(idOuvrierTest, jMinus2, StatutPointage.PRESENT);  // +1.0
        pointageService.pointerOuvrier(idOuvrierTest, jMinus1, StatutPointage.ABSENT);   // +0.0
        pointageService.pointerOuvrier(idOuvrierTest, aujourdhui, StatutPointage.PRESENT); // +1.0

        double jours = pointageService.calculerJoursTravailles(idOuvrierTest, jMinus2, aujourdhui);
        BigDecimal montant = pointageService.calculerMontantPeriode(idOuvrierTest, jMinus2, aujourdhui);

        assertEquals(2.0, jours);
        // 2.0 jours * 150.00 = 300.00
        assertEquals(0, new BigDecimal("300.00").compareTo(montant));
    }
}