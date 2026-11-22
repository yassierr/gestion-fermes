package ma.ferme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ma.ferme.TestSupport;
import ma.ferme.model.Administrateur;
import ma.ferme.model.Depense;
import ma.ferme.model.ResponsableFerme;
import ma.ferme.model.enums.CategorieDepense;

/**
 * Tests de {@link DepenseService} : validation, droits (EF-T04), recherche et total mensuel.
 * Les tests de validation n'ont pas besoin de la base ; les autres utilisent PostgreSQL
 * et sont ignorés si elle n'est pas configurée. Chaque test crée ses fermes et les supprime.
 */
class DepenseServiceTest {

    private final DepenseService service = new DepenseService();
    private final List<Integer> fermes = new ArrayList<>();

    @AfterEach
    void nettoyer() {
        if (!TestSupport.baseDisponible()) {
            return;
        }
        fermes.forEach(id -> TestSupport.executer("DELETE FROM depense WHERE id_ferme = " + id));
        fermes.forEach(id -> TestSupport.executer("DELETE FROM ferme WHERE id = " + id));
    }

    // ---------- sans base de données ----------

    @Test
    void seulUnResponsableActifPeutAgir() {
        Administrateur admin = new Administrateur("Admin", "admin", "hash");
        ResponsableFerme inactif = new ResponsableFerme(1, "Resp", "resp", "hash", false, 1);
        LocalDate jour = LocalDate.now();
        assertThrows(SecurityException.class,
                () -> service.ajouter(admin, jour, CategorieDepense.ENGRAIS, BigDecimal.TEN, ""));
        assertThrows(SecurityException.class,
                () -> service.ajouter(inactif, jour, CategorieDepense.ENGRAIS, BigDecimal.TEN, ""));
        assertThrows(SecurityException.class,
                () -> service.modifier(admin, 1, jour, CategorieDepense.ENGRAIS, BigDecimal.TEN, ""));
        assertThrows(SecurityException.class, () -> service.supprimer(admin, 1));
        assertThrows(SecurityException.class, () -> service.lister(admin));
        assertThrows(SecurityException.class, () -> service.rechercher(admin, "", null, null, null));
        assertThrows(SecurityException.class, () -> service.totalDuMois(admin, YearMonth.now()));
    }

    @Test
    void donneesInvalidesSontRefusees() {
        ResponsableFerme acteur = new ResponsableFerme(1, "Resp", "resp", "hash", true, 1);
        LocalDate jour = LocalDate.now();
        assertThrows(IllegalArgumentException.class,
                () -> service.ajouter(acteur, jour, CategorieDepense.AUTRE, BigDecimal.ZERO, ""));
        assertThrows(IllegalArgumentException.class,
                () -> service.ajouter(acteur, jour, CategorieDepense.AUTRE, new BigDecimal("-1"), ""));
        assertThrows(IllegalArgumentException.class,
                () -> service.ajouter(acteur, jour, CategorieDepense.AUTRE, null, ""));
        assertThrows(IllegalArgumentException.class,
                () -> service.ajouter(acteur, jour, null, BigDecimal.TEN, ""));
        assertThrows(IllegalArgumentException.class,
                () -> service.ajouter(acteur, jour, CategorieDepense.AUTRE, BigDecimal.TEN,
                        "x".repeat(Depense.DESCRIPTION_MAX + 1)));
    }

    // ---------- avec la base ----------

    @Test
    void ajouterListerModifierSupprimer() {
        ResponsableFerme acteur = responsable(preparerFerme());
        Depense d = service.ajouter(acteur, LocalDate.of(2025, 3, 10), CategorieDepense.ENGRAIS,
                new BigDecimal("450"), "Nitrate - Agri-Sud");
        assertTrue(d.getId() > 0);
        assertEquals(1, service.lister(acteur).size());

        service.modifier(acteur, d.getId(), LocalDate.of(2025, 3, 11), CategorieDepense.EMBALLAGE,
                new BigDecimal("500.5"), "Cagettes");
        Depense relue = service.lister(acteur).get(0);
        assertEquals(CategorieDepense.EMBALLAGE, relue.getCategorie());
        assertEquals(0, new BigDecimal("500.5").compareTo(relue.getMontant()));
        assertEquals("Cagettes", relue.getDescription());
        assertEquals(LocalDate.of(2025, 3, 11), relue.getDateDepense());

        service.supprimer(acteur, d.getId());
        assertTrue(service.lister(acteur).isEmpty());
    }

    @Test
    void dateAbsenteDevientAujourdhui() {
        ResponsableFerme acteur = responsable(preparerFerme());
        Depense d = service.ajouter(acteur, null, CategorieDepense.AUTRE, BigDecimal.TEN, "");
        assertEquals(LocalDate.now(), d.getDateDepense());
    }

    @Test
    void depenseInexistanteEstRefusee() {
        ResponsableFerme acteur = responsable(preparerFerme());
        assertThrows(IllegalArgumentException.class, () -> service.modifier(acteur, -1, LocalDate.now(),
                CategorieDepense.AUTRE, BigDecimal.TEN, ""));
        assertThrows(IllegalArgumentException.class, () -> service.supprimer(acteur, -1));
    }

    @Test
    void unResponsableNeToucheQuAuxDepensesDeSaFerme() {
        ResponsableFerme a = responsable(preparerFerme());
        ResponsableFerme b = responsable(preparerFerme());
        Depense deA = service.ajouter(a, LocalDate.now(), CategorieDepense.PLANTS, BigDecimal.TEN, "");

        assertThrows(SecurityException.class, () -> service.modifier(b, deA.getId(), LocalDate.now(),
                CategorieDepense.PLANTS, BigDecimal.ONE, ""));
        assertThrows(SecurityException.class, () -> service.supprimer(b, deA.getId()));
        assertTrue(service.lister(b).isEmpty());
        assertEquals(1, service.lister(a).size());
    }

    @Test
    void listeDuPlusRecentAuPlusAncien() {
        ResponsableFerme acteur = responsable(preparerFerme());
        service.ajouter(acteur, LocalDate.of(2025, 1, 5), CategorieDepense.AUTRE, BigDecimal.ONE, "ancienne");
        service.ajouter(acteur, LocalDate.of(2025, 6, 5), CategorieDepense.AUTRE, BigDecimal.ONE, "recente");
        assertEquals("recente", service.lister(acteur).get(0).getDescription());
    }

    @Test
    void rechercheParTexteCategorieEtPeriode() {
        ResponsableFerme acteur = responsable(preparerFerme());
        service.ajouter(acteur, LocalDate.of(2025, 3, 15), CategorieDepense.ENGRAIS, new BigDecimal("450"), "Nitrate Agri-Sud");
        service.ajouter(acteur, LocalDate.of(2025, 4, 2), CategorieDepense.EAU_ENERGIE, new BigDecimal("1200"), "Facture eau");
        service.ajouter(acteur, LocalDate.of(2025, 5, 20), CategorieDepense.PLANTS, new BigDecimal("300"), "Plants Sungold");

        assertEquals(3, service.rechercher(acteur, "", null, null, null).size());
        assertEquals(3, service.rechercher(acteur, null, null, null, null).size());
        assertEquals(1, service.rechercher(acteur, "NITRATE", null, null, null).size(), "casse ignorée");
        assertEquals(1, service.rechercher(acteur, "engrais", null, null, null).size(), "libellé de catégorie");
        assertEquals(1, service.rechercher(acteur, "1200", null, null, null).size(), "montant");
        assertEquals(1, service.rechercher(acteur, "02/04/2025", null, null, null).size(), "date jj/mm/aaaa");
        assertEquals(1, service.rechercher(acteur, "", CategorieDepense.PLANTS, null, null).size());
        assertEquals(2, service.rechercher(acteur, "", null, LocalDate.of(2025, 4, 1), null).size());
        assertEquals(2, service.rechercher(acteur, "", null, null, LocalDate.of(2025, 4, 30)).size());
        assertEquals(1, service.rechercher(acteur, "", null, LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 30)).size());
        assertTrue(service.rechercher(acteur, "introuvable", null, null, null).isEmpty());
    }

    @Test
    void totalDuMoisNeCompteQueCeMois() {
        ResponsableFerme acteur = responsable(preparerFerme());
        service.ajouter(acteur, LocalDate.of(2025, 3, 1), CategorieDepense.AUTRE, new BigDecimal("100.50"), "");
        service.ajouter(acteur, LocalDate.of(2025, 3, 31), CategorieDepense.AUTRE, new BigDecimal("200"), "");
        service.ajouter(acteur, LocalDate.of(2025, 4, 1), CategorieDepense.AUTRE, new BigDecimal("50"), "");

        assertEquals(0, new BigDecimal("300.50").compareTo(service.totalDuMois(acteur, YearMonth.of(2025, 3))));
        assertEquals(0, new BigDecimal("50").compareTo(service.totalDuMois(acteur, YearMonth.of(2025, 4))));
        assertEquals(0, BigDecimal.ZERO.compareTo(service.totalDuMois(acteur, YearMonth.of(2025, 5))));
    }

    // ---------- outils ----------

    private ResponsableFerme responsable(int ferme) {
        return new ResponsableFerme(1, "Resp", "resp", "hash", true, ferme);
    }

    /** Crée une ferme vide, ignorant le test si la base n'est pas disponible. */
    private int preparerFerme() {
        assumeTrue(TestSupport.baseDisponible(), "PostgreSQL non configuré : test ignoré");
        int id = TestSupport.premierId("INSERT INTO ferme (nom, localisation) VALUES ('Ferme test depense "
                + System.nanoTime() + "', 'Agadir') RETURNING id");
        fermes.add(id);
        return id;
    }
}