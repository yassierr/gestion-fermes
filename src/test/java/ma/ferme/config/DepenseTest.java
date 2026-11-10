package ma.ferme.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ma.ferme.model.enums.CategorieDepense;

/** Tests de {@link Depense} et {@link CategorieDepense} (sans base de données). */
class DepenseTest {

    @Test
    void dateAbsenteDevientAujourdhui() {
        Depense d = new Depense(1, null, CategorieDepense.ENGRAIS, new BigDecimal("45000"), "Nitrate");
        assertEquals(LocalDate.now(), d.getDateDepense());
    }

    @Test
    void montantEstArrondiEtDescriptionNulleDevientVide() {
        Depense d = new Depense(1, LocalDate.of(2024, 10, 24), CategorieDepense.PLANTS, new BigDecimal("385000.555"), null);
        assertEquals(new BigDecimal("385000.56"), d.getMontant());
        assertEquals("", d.getDescription());
    }

    @Test
    void montantNulNegatifOuAbsentEstRefuse() {
        LocalDate j = LocalDate.now();
        assertThrows(IllegalArgumentException.class, () -> new Depense(1, j, CategorieDepense.AUTRE, BigDecimal.ZERO, ""));
        assertThrows(IllegalArgumentException.class, () -> new Depense(1, j, CategorieDepense.AUTRE, new BigDecimal("-1"), ""));
        assertThrows(IllegalArgumentException.class, () -> new Depense(1, j, CategorieDepense.AUTRE, null, ""));
    }

    @Test
    void categorieAbsenteEstRefusee() {
        assertThrows(IllegalArgumentException.class,
                () -> new Depense(1, LocalDate.now(), null, BigDecimal.TEN, ""));
    }

    @Test
    void descriptionTropLongueEstRefusee() {
        assertThrows(IllegalArgumentException.class, () -> new Depense(1, LocalDate.now(),
                CategorieDepense.AUTRE, BigDecimal.TEN, "x".repeat(Depense.DESCRIPTION_MAX + 1)));
    }

    @Test
    void fermeInvalideEstRefusee() {
        assertThrows(IllegalArgumentException.class,
                () -> new Depense(0, LocalDate.now(), CategorieDepense.AUTRE, BigDecimal.TEN, ""));
    }

    @Test
    void categorieSeRetrouveParLibelleOuParNom() {
        assertEquals(CategorieDepense.EAU_ENERGIE, CategorieDepense.depuisLibelle("Eau-Énergie"));
        assertEquals(CategorieDepense.EAU_ENERGIE, CategorieDepense.depuisLibelle("eau_energie"));
        assertEquals("Emballage", CategorieDepense.EMBALLAGE.toString());
        assertThrows(IllegalArgumentException.class, () -> CategorieDepense.depuisLibelle("Salaires"));
        assertThrows(IllegalArgumentException.class, () -> CategorieDepense.depuisLibelle(null));
    }

    @Test
    void lesCinqCategoriesDuSchemaExistent() {
        assertEquals(5, CategorieDepense.values().length);
    }
}