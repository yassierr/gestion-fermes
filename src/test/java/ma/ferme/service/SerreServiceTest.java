
package ma.ferme.service;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.dao.FermeDao;
import ma.ferme.dao.SerreDao;
import ma.ferme.model.Ferme;
import ma.ferme.model.Serre;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SerreServiceTest {

    private SerreService serreService;
    private SerreDao serreDao;
    private FermeDao fermeDao;

    private int testFarmId;
    private final List<Integer> createdSerreIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        serreDao = new SerreDao();
        fermeDao = new FermeDao();
        serreService = new SerreService(serreDao, fermeDao);

        assumeTrue(DatabaseConnection.isConfigured());

        Ferme ferme = new Ferme(
                "Test Farm " + System.nanoTime(),
                "Agadir"
        );

        fermeDao.insert(ferme);
        testFarmId = ferme.getId();
    }

    @AfterEach
    void tearDown() {
        for (int id : createdSerreIds) {
            if (serreDao.findById(id).isPresent()) {
                serreDao.delete(id);
            }
        }

        if (testFarmId > 0 && fermeDao.findById(testFarmId).isPresent()) {
            fermeDao.delete(testFarmId);
        }

        createdSerreIds.clear();
    }

    // Helper method
    private Serre createTestSerre(String nom) {
        Serre serre = serreService.creerSerre(
                nom,
                500.0,
                "Tomate cerise",
                "Plastique",
                "Goutte a goutte",
                testFarmId
        );

        createdSerreIds.add(serre.getId());

        return serre;
    }

    @Test
    void creerSerreAvecInformationsValides() {
        Serre serre = createTestSerre(
                "Serre A"
        );

        assertNotNull(serre);
        assertTrue(serre.getId() > 0);
        assertEquals(500.0, serre.getSurfaceM2());
        assertEquals(testFarmId, serre.getIdFerme());
    }

    @Test
    void refuserCreationAvecNomVide() {
        assertThrows(
                IllegalArgumentException.class,
                () -> serreService.creerSerre(
                        "",
                        500,
                        "Tomate",
                        "Plastique",
                        "Goutte a goutte",
                        testFarmId
                )
        );
    }

    @Test
    void refuserCreationAvecSurfaceInvalide() {
        assertThrows(
                IllegalArgumentException.class,
                () -> serreService.creerSerre(
                        "Serre A",
                        -100,
                        "Tomate",
                        "Plastique",
                        "Goutte a goutte",
                        testFarmId
                )
        );
    }

    @Test
    void refuserCreationAvecFermeInexistante() {
        assertThrows(
                IllegalArgumentException.class,
                () -> serreService.creerSerre(
                        "Serre A",
                        500,
                        "Tomate",
                        "Plastique",
                        "Goutte a goutte",
                        999999999
                )
        );
    }

    @Test
    void refuserCreationAvecNomDupliqueDansMemeFerme() {
        String nom = "Duplicate Serre";

        createTestSerre(nom);

        assertThrows(
                IllegalStateException.class,
                () -> serreService.creerSerre(
                        nom,
                        600,
                        "Tomate",
                        "Plastique",
                        "Goutte a goutte",
                        testFarmId
                )
        );
    }

    @Test
    void obtenirSerreParId() {
        Serre serre = createTestSerre("Find Serre");

        Optional<Serre> resultat =
                serreService.obtenirSerreParId(serre.getId());

        assertTrue(resultat.isPresent());
        assertEquals(serre.getNom(), resultat.get().getNom());
    }

    @Test
    void refuserRechercheAvecIdInvalide() {
        assertThrows(
                IllegalArgumentException.class,
                () -> serreService.obtenirSerreParId(0)
        );
    }

    @Test
    void obtenirToutesLesSerres() {
        createTestSerre("Serre A");
        createTestSerre("Serre B");

        List<Serre> serres = serreService.obtenirToutesLesSerres();

        assertNotNull(serres);
        assertTrue(serres.size() >= 2);
    }

    @Test
    void modifierSerre() {
        Serre serre = createTestSerre("Old Serre");

        serre.setNom("Updated Serre");
        serre.setSurfaceM2(750.0);

        serreService.modifierSerre(serre);

        Optional<Serre> resultat =
                serreDao.findById(serre.getId());

        assertTrue(resultat.isPresent());
        assertEquals("Updated Serre", resultat.get().getNom());
        assertEquals(750.0, resultat.get().getSurfaceM2());
    }

    @Test
    void refuserModificationAvecSurfaceInvalide() {
        Serre serre = createTestSerre("Invalid Update");

        serre.setSurfaceM2(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> serreService.modifierSerre(serre)
        );
    }

    @Test
    void supprimerSerre() {
        Serre serre = createTestSerre("Delete Serre");

        serreService.supprimerSerre(serre.getId());

        createdSerreIds.remove(Integer.valueOf(serre.getId()));

        assertTrue(
                serreDao.findById(serre.getId()).isEmpty()
        );
    }

    @Test
    void refuserSuppressionAvecIdInvalide() {
        assertThrows(
                IllegalArgumentException.class,
                () -> serreService.supprimerSerre(-1)
        );
    }
}
