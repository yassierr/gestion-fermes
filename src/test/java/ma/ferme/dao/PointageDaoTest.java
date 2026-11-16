package ma.ferme.dao;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.TestSupport;
import ma.ferme.model.Ouvrier;
import ma.ferme.model.Pointage;
import ma.ferme.model.enums.StatutPointage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PointageDaoTest {

    private PointageDao pointageDao;
    private OuvrierDao ouvrierDao;
    private int idFermeTest;
    private int idOuvrierTest;

    @BeforeEach
    void setUp() throws SQLException {
        assumeTrue(TestSupport.baseDisponible(), "Base de données PostgreSQL indisponible");
        pointageDao = new PointageDao();
        ouvrierDao = new OuvrierDao();

        idFermeTest = creerFermeTest();

        String cinUnique = "P" + (System.nanoTime() % 1_000_000_000L);
        Ouvrier ouvrier = new Ouvrier(idFermeTest, "TestPointage", cinUnique, "0699999999", "Biougra", "Test", new BigDecimal("150.00"));
        ouvrierDao.insert(ouvrier);
        idOuvrierTest = ouvrier.getId();
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Ordre inverse des clés étrangères : pointage -> ouvrier -> ferme
        for (Pointage p : pointageDao.findByOuvrier(idOuvrierTest)) {
            pointageDao.delete(p.getId());
        }
        ouvrierDao.delete(idOuvrierTest);
        supprimerFermeTest(idFermeTest);
    }

    private int creerFermeTest() throws SQLException {
        String sql = "INSERT INTO ferme (nom, localisation) VALUES (?, ?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "Ferme PointageDaoTest " + System.nanoTime());
            stmt.setString(2, "Biougra");
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt("id");
            }
        }
    }

    private void supprimerFermeTest(int idFerme) throws SQLException {
        String sql = "DELETE FROM ferme WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idFerme);
            stmt.executeUpdate();
        }
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
        Pointage pointage = new Pointage(idOuvrierTest, LocalDate.now(), StatutPointage.ABSENT);
        pointageDao.insert(pointage);

        Optional<Pointage> trouve = pointageDao.findById(pointage.getId());

        assertTrue(trouve.isPresent());
        assertEquals(StatutPointage.ABSENT, trouve.get().getStatut());
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