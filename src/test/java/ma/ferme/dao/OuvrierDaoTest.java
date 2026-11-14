package ma.ferme.dao;

import ma.ferme.model.Ouvrier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OuvrierDaoTest {

    private OuvrierDao ouvrierDao;
    private final List<Integer> idsCrees = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ouvrierDao = new OuvrierDao();
    }

    @AfterEach
    void tearDown() {
        for (Integer id : idsCrees) {
            try {
                ouvrierDao.delete(id);
            } catch (Exception ignored) {
            }
        }
        idsCrees.clear();
    }

    @Test
    @DisplayName("Devrait insérer un ouvrier et lui attribuer un ID généré par la BDD")
    void testInsert() {
        Ouvrier ouvrier = new Ouvrier(1, "Bennani Youssef", "K123456", "0612345678", "Biougra", "Serre A", new BigDecimal("160.00"));

        ouvrierDao.insert(ouvrier);
        idsCrees.add(ouvrier.getId());

        assertTrue(ouvrier.getId() > 0, "L'ID généré devrait être supérieur à 0");
        assertTrue(ouvrier.getMatricule().startsWith("AS-"));
    }

    @Test
    @DisplayName("Devrait rechercher un ouvrier par son ID")
    void testFindById() {
        Ouvrier ouvrier = new Ouvrier(1, "El Amrani Hassan", "K654321", "0687654321", "Ait Melloul", "Serre B", new BigDecimal("170.00"));
        ouvrierDao.insert(ouvrier);
        idsCrees.add(ouvrier.getId());

        Optional<Ouvrier> trouve = ouvrierDao.findById(ouvrier.getId());

        assertTrue(trouve.isPresent());
        assertEquals("El Amrani Hassan", trouve.get().getNomEtPrenom());
        assertEquals(0, new BigDecimal("170.00").compareTo(trouve.get().getSalaireJournalier()));
    }

    @Test
    @DisplayName("Devrait récupérer tous les ouvriers de la base")
    void testFindAll() {
        List<Ouvrier> liste = ouvrierDao.findAll();
        assertNotNull(liste);
    }

    @Test
    @DisplayName("Devrait filtrer les ouvriers par ferme")
    void testFindByFerme() {
        Ouvrier o1 = new Ouvrier(1, "Tazi Omar", "K111111", "0600000001", "Biougra", "Serre A", new BigDecimal("150.00"));
        ouvrierDao.insert(o1);
        idsCrees.add(o1.getId());

        List<Ouvrier> listeFerme1 = ouvrierDao.findByFerme(1);
        assertFalse(listeFerme1.isEmpty());
        assertTrue(listeFerme1.stream().anyMatch(o -> o.getId() == o1.getId()));
    }

    @Test
    @DisplayName("Devrait mettre à jour un ouvrier")
    void testUpdate() {
        Ouvrier ouvrier = new Ouvrier(1, "Chraib Anass", "K222222", "0600000002", "Taroudant", "Cueillette", new BigDecimal("150.00"));
        ouvrierDao.insert(ouvrier);
        idsCrees.add(ouvrier.getId());

        ouvrier.setNomEtPrenom("Chraibi Modifie");
        ouvrier.setSalaireJournalier(new BigDecimal("200.00"));
        ouvrierDao.update(ouvrier);

        Optional<Ouvrier> maj = ouvrierDao.findById(ouvrier.getId());
        assertTrue(maj.isPresent());
        assertEquals("Chraibi Modifie", maj.get().getNomEtPrenom());
        assertEquals(0, new BigDecimal("200.00").compareTo(maj.get().getSalaireJournalier()));
    }

    @Test
    @DisplayName("Devrait supprimer un ouvrier")
    void testDelete() {
        Ouvrier ouvrier = new Ouvrier(1, "Alaoui Saad", "K333333", "0600000003", "Agadir", "Serre C", new BigDecimal("150.00"));
        ouvrierDao.insert(ouvrier);

        int id = ouvrier.getId();
        ouvrierDao.delete(id);

        Optional<Ouvrier> supprime = ouvrierDao.findById(id);
        assertFalse(supprime.isPresent());
    }
}