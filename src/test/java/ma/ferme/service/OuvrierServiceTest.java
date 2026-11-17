package ma.ferme.service;

import ma.ferme.TestSupport;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OuvrierServiceTest {

    private OuvrierService ouvrierService;
    private OuvrierDao ouvrierDao;
    private PointageDao pointageDao;
    private int idFermeTest;
    private final List<Integer> idsCrees = new ArrayList<>();

    @BeforeEach
    void setUp() {
        assumeTrue(TestSupport.baseDisponible(), "Base non disponible : test ignoré");
        ouvrierDao = new OuvrierDao();
        pointageDao = new PointageDao();
        ouvrierService = new OuvrierService(ouvrierDao, pointageDao);

        idFermeTest = TestSupport.premierId(
                "INSERT INTO ferme (nom, localisation) VALUES ('Ferme OuvrierServiceTest " + System.nanoTime() + "', 'Biougra') RETURNING id"
        );
    }

    @AfterEach
    void tearDown() {
        if (!TestSupport.baseDisponible()) return;
        for (Integer id : idsCrees) {
            try {
                for (Pointage p : pointageDao.findByOuvrier(id)) {
                    pointageDao.delete(p.getId());
                }
                ouvrierDao.delete(id);
            } catch (Exception ignored) {
            }
        }
        idsCrees.clear();
        if (idFermeTest > 0) {
            TestSupport.executer("DELETE FROM ferme WHERE id = " + idFermeTest);
        }
    }

    private String uniqueCin() {
        return "S" + (System.nanoTime() % 1_000_000_000L);
    }

    @Test
    @DisplayName("Devrait ajouter un ouvrier valide et lui générer un ID et un matricule")
    void testAjouterOuvrierValide() {
        Ouvrier ouvrier = new Ouvrier(idFermeTest, "Bennani Youssef", uniqueCin(), "0612345678", "Biougra", "Serre A", new BigDecimal("160.00"));

        ouvrierService.ajouterOuvrier(ouvrier);
        idsCrees.add(ouvrier.getId());

        assertTrue(ouvrier.getId() > 0);
        assertTrue(ouvrier.getMatricule().startsWith("AS-"));
    }

    @Test
    @DisplayName("Devrait refuser l'ajout d'un ouvrier avec un CIN déjà existant")
    void testAjouterOuvrierCinDoublon() {
        String cin = uniqueCin();
        Ouvrier o1 = new Ouvrier(idFermeTest, "Ouvrier Un", cin, "0611111111", "Biougra", "Serre A", new BigDecimal("150.00"));
        ouvrierService.ajouterOuvrier(o1);
        idsCrees.add(o1.getId());

        Ouvrier o2 = new Ouvrier(idFermeTest, "Ouvrier Deux", cin.toLowerCase(), "0622222222", "Agadir", "Serre B", new BigDecimal("150.00"));

        assertThrows(IllegalArgumentException.class, () -> ouvrierService.ajouterOuvrier(o2));
    }

    @Test
    @DisplayName("Devrait refuser un salaire négatif")
    void testAjouterOuvrierSalaireNegatif() {
        Ouvrier ouvrier = new Ouvrier(idFermeTest, "Test Salaire", uniqueCin(), "0633333333", "Biougra", "Serre A", new BigDecimal("-50.00"));

        assertThrows(IllegalArgumentException.class, () -> ouvrierService.ajouterOuvrier(ouvrier));
    }

    @Test
    @DisplayName("Devrait désactiver et réactiver un ouvrier (soft delete)")
    void testDesactiverEtReactiverOuvrier() {
        Ouvrier ouvrier = new Ouvrier(idFermeTest, "El Amrani Hassan", uniqueCin(), "0644444444", "Ait Melloul", "Serre B", new BigDecimal("170.00"));
        ouvrierService.ajouterOuvrier(ouvrier);
        idsCrees.add(ouvrier.getId());

        ouvrierService.desactiverOuvrier(ouvrier.getId());
        assertFalse(ouvrierService.trouverParId(ouvrier.getId()).orElseThrow().isActif());

        ouvrierService.reactiverOuvrier(ouvrier.getId());
        assertTrue(ouvrierService.trouverParId(ouvrier.getId()).orElseThrow().isActif());
    }

    @Test
    @DisplayName("Devrait bloquer la suppression définitive si l'ouvrier a des pointages")
    void testSupprimerOuvrierAvecPointagesBloque() {
        Ouvrier ouvrier = new Ouvrier(idFermeTest, "Tazi Omar", uniqueCin(), "0655555555", "Biougra", "Serre A", new BigDecimal("150.00"));
        ouvrierService.ajouterOuvrier(ouvrier);
        idsCrees.add(ouvrier.getId());

        pointageDao.insert(new Pointage(ouvrier.getId(), LocalDate.now(), StatutPointage.PRESENT));

        assertThrows(IllegalStateException.class, () -> ouvrierService.supprimerOuvrier(ouvrier.getId()));
    }

    @Test
    @DisplayName("Devrait autoriser la suppression définitive si l'ouvrier n'a aucun pointage")
    void testSupprimerOuvrierSansPointageReussit() {
        Ouvrier ouvrier = new Ouvrier(idFermeTest, "Alaoui Saad", uniqueCin(), "0666666666", "Agadir", "Serre C", new BigDecimal("150.00"));
        ouvrierService.ajouterOuvrier(ouvrier);

        ouvrierService.supprimerOuvrier(ouvrier.getId());

        assertTrue(ouvrierService.trouverParId(ouvrier.getId()).isEmpty());
    }
}