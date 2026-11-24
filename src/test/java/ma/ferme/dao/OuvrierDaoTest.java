package ma.ferme.dao;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.TestSupport;
import ma.ferme.model.Ouvrier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OuvrierDaoTest {

    private OuvrierDao ouvrierDao;
    private int idFermeTest;
    private final List<Integer> idsCrees = new ArrayList<>();

    @BeforeEach
    void setUp() throws SQLException {
        assumeTrue(TestSupport.baseDisponible(), "Base de données PostgreSQL indisponible");
        ouvrierDao = new OuvrierDao();
        idFermeTest = creerFermeTest();
    }

    @AfterEach
    void tearDown() throws SQLException {
        // 1. Supprimer les ouvriers créés
        for (Integer id : idsCrees) {
            try {
                ouvrierDao.delete(id);
            } catch (Exception ignored) {
            }
        }
        idsCrees.clear();

        // 2. Supprimer la ferme de test (ordre inverse des clés étrangères)
        supprimerFermeTest(idFermeTest);
    }

    private int creerFermeTest() throws SQLException {
        String sql = "INSERT INTO ferme (nom, localisation) VALUES (?, ?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "Ferme OuvrierDaoTest " + System.nanoTime());
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

    private String uniqueCin() {
        return "C" + (System.nanoTime() % 1_000_000_000L);
    }

    @Test
    @DisplayName("Devrait insérer un ouvrier et lui attribuer un ID généré par la BDD")
    void testInsert() {
        Ouvrier ouvrier = new Ouvrier(idFermeTest, "Bennani Youssef", uniqueCin(), "0612345678", "Biougra", "Serre A", new BigDecimal("160.00"));

        ouvrierDao.insert(ouvrier);
        idsCrees.add(ouvrier.getId());

        assertTrue(ouvrier.getId() > 0, "L'ID généré devrait être supérieur à 0");
        assertTrue(ouvrier.getMatricule().startsWith("AS-"));
    }

    @Test
    @DisplayName("Devrait rechercher un ouvrier par son ID")
    void testFindById() {
        Ouvrier ouvrier = new Ouvrier(idFermeTest, "El Amrani Hassan", uniqueCin(), "0687654321", "Ait Melloul", "Serre B", new BigDecimal("170.00"));
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
        Ouvrier o1 = new Ouvrier(idFermeTest, "Tazi Omar", uniqueCin(), "0600000001", "Biougra", "Serre A", new BigDecimal("150.00"));
        ouvrierDao.insert(o1);
        idsCrees.add(o1.getId());

        List<Ouvrier> listeFerme = ouvrierDao.findByFerme(idFermeTest);
        assertEquals(1, listeFerme.size());
        assertEquals(o1.getId(), listeFerme.get(0).getId());
    }

    @Test
    @DisplayName("compterActifsParFerme : ignore les ouvriers désactivés")
    void compterActifsParFerme() {
        Ouvrier actif = new Ouvrier(idFermeTest, "Ouvrier Actif", uniqueCin(),
                "0600000003", "Biougra", "Serre A", new BigDecimal("150.00"));
        Ouvrier inactif = new Ouvrier(idFermeTest, "Ouvrier Inactif", uniqueCin(),
                "0600000004", "Biougra", "Serre A", new BigDecimal("150.00"));
        inactif.setActif(false);
        ouvrierDao.insert(actif);
        ouvrierDao.insert(inactif);
        idsCrees.add(actif.getId());
        idsCrees.add(inactif.getId());

        assertEquals(1, ouvrierDao.compterActifsParFerme().get(idFermeTest));
    }

    @Test
    @DisplayName("Devrait mettre à jour un ouvrier")
    void testUpdate() {
        Ouvrier ouvrier = new Ouvrier(idFermeTest, "Chraib Anass", uniqueCin(), "0600000002", "Taroudant", "Cueillette", new BigDecimal("150.00"));
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
        Ouvrier ouvrier = new Ouvrier(idFermeTest, "Alaoui Saad", uniqueCin(), "0600000003", "Agadir", "Serre C", new BigDecimal("150.00"));
        ouvrierDao.insert(ouvrier);

        int id = ouvrier.getId();
        ouvrierDao.delete(id);

        Optional<Ouvrier> supprime = ouvrierDao.findById(id);
        assertFalse(supprime.isPresent());
    }
}