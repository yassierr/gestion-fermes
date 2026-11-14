package ma.ferme.dao;

import ma.ferme.model.Ouvrier;
import ma.ferme.model.Pointage;
import ma.ferme.model.enums.StatutPointage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PointageDaoTest {

    private PointageDao pointageDao;
    private OuvrierDao ouvrierDao;
    private int idOuvrierTest;

    @BeforeEach
    void setUp() {
        pointageDao = new PointageDao();
        ouvrierDao = new OuvrierDao();

        Ouvrier ouvrier = new Ouvrier(1, "TestPointage", "Ouvrier", "TEST999", "0699999999", "Biougra", "Test", new BigDecimal("150.00"));
        ouvrierDao.insert(ouvrier);
        idOuvrierTest = ouvrier.getId();
    }

    @AfterEach
    void tearDown() {
        // Supprime d'abord les pointages liés (clé étrangère), puis l'ouvrier de test
        for (Pointage p : pointageDao.findByOuvrier(idOuvrierTest)) {
            pointageDao.delete(p.getId());
        }
        ouvrierDao.delete(idOuvrierTest);
    }

    @Test
    @DisplayName("Devrait insérer un pointage")
    void testInsert() {
        Pointage pointage = new Pointage(idOuvrierTest, LocalDate.now(), StatutPointage.PRESENT);
        pointageDao.insert(pointage);

        assertTrue(pointage.getId() > 0);
    }

    @Test
    @DisplayName("Devrait rechercher un pointage par ID")
    void testFindById() {
        Pointage pointage = new Pointage(idOuvrierTest, LocalDate.now(), StatutPointage.DEMI_JOURNEE);
        pointageDao.insert(pointage);

        Optional<Pointage> trouve = pointageDao.findById(pointage.getId());

        assertTrue(trouve.isPresent());
        assertEquals(StatutPointage.DEMI_JOURNEE, trouve.get().getStatut());
    }

    @Test
    @DisplayName("Devrait filtrer les pointages par ouvrier")
    void testFindByOuvrier() {
        Pointage p1 = new Pointage(idOuvrierTest, LocalDate.now().minusDays(1), StatutPointage.PRESENT);
        Pointage p2 = new Pointage(idOuvrierTest, LocalDate.now(), StatutPointage.ABSENT);
        pointageDao.insert(p1);
        pointageDao.insert(p2);

        List<Pointage> liste = pointageDao.findByOuvrier(idOuvrierTest);

        assertEquals(2, liste.size());
    }

    @Test
    @DisplayName("Devrait mettre à jour le statut d'un pointage")
    void testUpdate() {
        Pointage pointage = new Pointage(idOuvrierTest, LocalDate.now(), StatutPointage.ABSENT);
        pointageDao.insert(pointage);

        pointage.setStatut(StatutPointage.PRESENT);
        pointageDao.update(pointage);

        Optional<Pointage> maj = pointageDao.findById(pointage.getId());
        assertTrue(maj.isPresent());
        assertEquals(StatutPointage.PRESENT, maj.get().getStatut());
    }

    @Test
    @DisplayName("Devrait supprimer un pointage")
    void testDelete() {
        Pointage pointage = new Pointage(idOuvrierTest, LocalDate.now(), StatutPointage.PRESENT);
        pointageDao.insert(pointage);

        int id = pointage.getId();
        pointageDao.delete(id);

        Optional<Pointage> supprime = pointageDao.findById(id);
        assertFalse(supprime.isPresent());
    }
}