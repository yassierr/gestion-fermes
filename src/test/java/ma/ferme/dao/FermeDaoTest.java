
package ma.ferme.dao;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.Ferme;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FermeDaoTest {

    private final FermeDao fermeDao = new FermeDao();

    private Ferme ferme;

    @BeforeEach
    void setUp() {
        assumeTrue(
                DatabaseConnection.isConfigured(),
                "Configuration database absente"
        );

        ferme = new Ferme(
                "Test Ferme " + System.nanoTime(),
                "Agadir"
        );

        fermeDao.insert(ferme);

        assertTrue(ferme.getId() > 0);
    }

    @AfterEach
    void tearDown() {
        if (ferme != null && ferme.getId() > 0) {
            fermeDao.delete(ferme.getId());
        }
    }

    @Test
    void testInsert() {
        // Insertion is already performed in setUp().
        Optional<Ferme> result = fermeDao.findById(ferme.getId());

        assertTrue(result.isPresent());
        assertEquals(ferme.getNom(), result.get().getNom());
        assertEquals(ferme.getLocalisation(), result.get().getLocalisation());
    }

    @Test
    void testFindById() {
        Optional<Ferme> result = fermeDao.findById(ferme.getId());

        assertTrue(result.isPresent());

        Ferme found = result.get();

        assertEquals(ferme.getId(), found.getId());
        assertEquals(ferme.getNom(), found.getNom());
        assertEquals("Agadir", found.getLocalisation());
    }

    @Test
    void testFindByIdNotFound() {
        Optional<Ferme> result = fermeDao.findById(-1);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAll() {
        List<Ferme> fermes = fermeDao.findAll();

        assertNotNull(fermes);

        assertTrue(
                fermes.stream().anyMatch(
                        f -> f.getId() == ferme.getId()
                )
        );
    }

    @Test
    void testUpdate() {
        ferme.setNom("Ferme Updated");
        ferme.setLocalisation("Taroudant");

        fermeDao.update(ferme);

        Optional<Ferme> result = fermeDao.findById(ferme.getId());

        assertTrue(result.isPresent());
        assertEquals("Ferme Updated", result.get().getNom());
        assertEquals("Taroudant", result.get().getLocalisation());
    }

    @Test
    void testUpdateNotFound() {
        Ferme nonexistent = new Ferme(
                -1,
                "Unknown Farm",
                "Unknown Location"
        );

        assertThrows(
                DaoException.class,
                () -> fermeDao.update(nonexistent)
        );
    }

    @Test
    void testDelete() {
        Ferme temporary = new Ferme(
                "Temporary Farm " + System.nanoTime(),
                "Marrakech"
        );

        fermeDao.insert(temporary);

        int id = temporary.getId();

        fermeDao.delete(id);

        Optional<Ferme> result = fermeDao.findById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteNotFound() {
        assertThrows(
                DaoException.class,
                () -> fermeDao.delete(-1)
        );
    }
}
