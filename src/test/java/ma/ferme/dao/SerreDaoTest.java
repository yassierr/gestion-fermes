
package ma.ferme.dao;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.Serre;

import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SerreDaoTest {

    private final SerreDao serreDao = new SerreDao();

    private int fermeId;
    private int serreId;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(
                DatabaseConnection.isConfigured(),
                "Configuration database absente"
        );

        // Create a temporary farm for the foreign key.
        String sql = """
                INSERT INTO ferme (nom, localisation)
                VALUES (?, ?)
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, "Test Ferme " + System.nanoTime());
            ps.setString(2, "Agadir");

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                assertTrue(rs.next());
                fermeId = rs.getInt(1);
            }
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (fermeId == 0) {
            return;
        }

        try (Connection c = DatabaseConnection.getConnection()) {

            // Delete any greenhouses belonging to the temporary farm.
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM serre WHERE id_ferme = ?")) {
                ps.setInt(1, fermeId);
                ps.executeUpdate();
            }

            // Delete the temporary farm.
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM ferme WHERE id = ?")) {
                ps.setInt(1, fermeId);
                ps.executeUpdate();
            }
        }
    }

    private Serre createSerre() {
        return new Serre(
                "Serre Test",
                500.0,
                "Cherry Tomato",
                "Tunnel",
                "Goutte-à-goutte",
                fermeId
        );
    }

    @Test
    void testInsert() {
        Serre serre = createSerre();

        serreDao.insert(serre);

        assertTrue(serre.getId() > 0);

        serreId = serre.getId();

        Optional<Serre> result = serreDao.findById(serreId);

        assertTrue(result.isPresent());
        assertEquals("Serre Test", result.get().getNom());
    }

    @Test
    void testFindById() {
        Serre serre = createSerre();
        serreDao.insert(serre);

        Optional<Serre> result = serreDao.findById(serre.getId());

        assertTrue(result.isPresent());

        Serre found = result.get();

        assertEquals(serre.getId(), found.getId());
        assertEquals("Serre Test", found.getNom());
        assertEquals(500.0, found.getSurfaceM2());
        assertEquals("Cherry Tomato", found.getVariete());
        assertEquals("Tunnel", found.getTypeAbri());
        assertEquals("Goutte-à-goutte", found.getIrrigation());
        assertEquals(fermeId, found.getIdFerme());
    }

    @Test
    void testFindByIdNotFound() {
        Optional<Serre> result = serreDao.findById(-1);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAll() {
        Serre serre = createSerre();
        serreDao.insert(serre);

        List<Serre> serres = serreDao.findAll();

        assertNotNull(serres);
        assertTrue(
                serres.stream().anyMatch(s -> s.getId() == serre.getId())
        );
    }

    @Test
    void testUpdate() {
        Serre serre = createSerre();
        serreDao.insert(serre);

        serre.setNom("Serre Updated");
        serre.setSurfaceM2(750.0);
        serre.setVariete("Cherry Tomato Updated");

        serreDao.update(serre);

        Optional<Serre> result = serreDao.findById(serre.getId());

        assertTrue(result.isPresent());
        assertEquals("Serre Updated", result.get().getNom());
        assertEquals(750.0, result.get().getSurfaceM2());
        assertEquals("Cherry Tomato Updated", result.get().getVariete());
    }

    @Test
    void testUpdateNotFound() {
        Serre serre = createSerre();
        serre.setId(-1);

        assertThrows(
                DaoException.class,
                () -> serreDao.update(serre)
        );
    }

    @Test
    void testDelete() {
        Serre serre = createSerre();
        serreDao.insert(serre);

        int id = serre.getId();

        serreDao.delete(id);

        Optional<Serre> result = serreDao.findById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteNotFound() {
        assertThrows(
                DaoException.class,
                () -> serreDao.delete(-1)
        );
    }

    @Test
    void testInsertWithInvalidFarmId() {
        Serre serre = new Serre(
                "Invalid Serre",
                500.0,
                "Cherry Tomato",
                "Tunnel",
                "Goutte-à-goutte",
                -1
        );

        assertThrows(
                DaoException.class,
                () -> serreDao.insert(serre)
        );
    }
}
