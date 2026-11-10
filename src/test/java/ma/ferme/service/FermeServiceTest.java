package ma.ferme.service;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.dao.FermeDao;
import ma.ferme.model.Ferme;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FermeServiceTest {

    private FermeService fermeService;
    private FermeDao fermeDao;

    private final List<Integer> createdFarmIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        assumeTrue(DatabaseConnection.isConfigured());

        fermeDao = new FermeDao();
        fermeService = new FermeService(fermeDao);
    }

    @AfterEach
    void tearDown() {
        for (int id : createdFarmIds) {
            if (fermeDao.findById(id).isPresent()) {
                fermeDao.delete(id);
            }
        }

        createdFarmIds.clear();
    }

    // Helper method
    private Ferme createTestFarm(String nom) {
        Ferme ferme = fermeService.creerFerme(
                nom,
                "Agadir"
        );

        createdFarmIds.add(ferme.getId());

        return ferme;
    }

    @Test
    void creerFermeAvecInformationsValides() {

        Ferme ferme = createTestFarm(
                "Test Farm " + System.nanoTime()
        );

        assertNotNull(ferme);
        assertTrue(ferme.getId() > 0);
        assertEquals("Agadir", ferme.getLocalisation());
    }

    @Test
    void refuserCreationAvecNomVide() {

        assertThrows(
                IllegalArgumentException.class,
                () -> fermeService.creerFerme("", "Agadir")
        );
    }

    @Test
    void refuserCreationAvecLocalisationVide() {

        assertThrows(
                IllegalArgumentException.class,
                () -> fermeService.creerFerme("Test Farm", "")
        );
    }

    @Test
    void refuserCreationAvecNomDuplique() {

        String nom = "Duplicate Farm " + System.nanoTime();

        createTestFarm(nom);

        assertThrows(
                IllegalStateException.class,
                () -> fermeService.creerFerme(nom, "Agadir")
        );
    }

    @Test
    void obtenirFermeParId() {

        Ferme ferme = createTestFarm(
                "Find Farm " + System.nanoTime()
        );

        Optional<Ferme> resultat =
                fermeService.obtenirFermeParId(ferme.getId());

        assertTrue(resultat.isPresent());
        assertEquals(ferme.getNom(), resultat.get().getNom());
    }

    @Test
    void refuserRechercheAvecIdInvalide() {

        assertThrows(
                IllegalArgumentException.class,
                () -> fermeService.obtenirFermeParId(0)
        );
    }

    @Test
    void obtenirToutesLesFermes() {

        createTestFarm("Farm A " + System.nanoTime());
        createTestFarm("Farm B " + System.nanoTime());

        List<Ferme> fermes =
                fermeService.obtenirToutesLesFermes();

        assertNotNull(fermes);
        assertTrue(fermes.size() >= 2);
    }

    @Test
    void modifierFerme() {

        Ferme ferme = createTestFarm(
                "Old Name " + System.nanoTime()
        );

        ferme.setNom("Updated Farm");
        ferme.setLocalisation("Taroudant");

        fermeService.modifierFerme(ferme);

        Optional<Ferme> resultat =
                fermeDao.findById(ferme.getId());

        assertTrue(resultat.isPresent());
        assertEquals("Updated Farm", resultat.get().getNom());
        assertEquals("Taroudant", resultat.get().getLocalisation());
    }

    @Test
    void refuserModificationSansNom() {

        Ferme ferme = createTestFarm(
                "Invalid Update " + System.nanoTime()
        );

        ferme.setNom("");

        assertThrows(
                IllegalArgumentException.class,
                () -> fermeService.modifierFerme(ferme)
        );
    }

    @Test
    void supprimerFerme() {

        Ferme ferme = createTestFarm(
                "Delete Farm " + System.nanoTime()
        );

        fermeService.supprimerFerme(ferme.getId());

        createdFarmIds.remove(Integer.valueOf(ferme.getId()));

        assertTrue(
                fermeDao.findById(ferme.getId()).isEmpty()
        );
    }

    @Test
    void refuserSuppressionAvecIdInvalide() {

        assertThrows(
                IllegalArgumentException.class,
                () -> fermeService.supprimerFerme(-1)
        );
    }
}