package ma.ferme.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** Tests de {@link Recolte} (sans base de données). */
class RecolteTest {

    @Test
    void nouvelleRecolteEstDateeDeMaintenantEtSansId() {
        LocalDateTime avant = LocalDateTime.now().minusSeconds(2);
        Recolte r = new Recolte(3, new BigDecimal("850"), "Sweetelle");
        assertEquals(0, r.getId());
        assertEquals(3, r.getIdFerme());
        assertTrue(r.getDateHeure().isAfter(avant));
    }

    @Test
    void lePoidsEstArrondiADeuxDecimalesEtLaVarieteEstNettoyee() {
        Recolte r = new Recolte(1, new BigDecimal("12.345"), "  Sungold  ");
        assertEquals(new BigDecimal("12.35"), r.getPoidsKg());
        assertEquals("Sungold", r.getVariete());
    }

    @Test
    void poidsNulNegatifOuAbsentEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> new Recolte(1, BigDecimal.ZERO, "A"));
        assertThrows(IllegalArgumentException.class, () -> new Recolte(1, new BigDecimal("-5"), "A"));
        assertThrows(IllegalArgumentException.class, () -> new Recolte(1, null, "A"));
        assertThrows(IllegalArgumentException.class, () -> new Recolte(1, new BigDecimal("0.001"), "A"));
    }

    @Test
    void varieteVideOuTropLongueEstRefusee() {
        assertThrows(IllegalArgumentException.class, () -> new Recolte(1, BigDecimal.TEN, " "));
        assertThrows(IllegalArgumentException.class, () -> new Recolte(1, BigDecimal.TEN, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Recolte(1, BigDecimal.TEN, "x".repeat(Recolte.VARIETE_MAX + 1)));
    }

    @Test
    void fermeInvalideEstRefusee() {
        assertThrows(IllegalArgumentException.class, () -> new Recolte(0, BigDecimal.TEN, "A"));
    }

    @Test
    void egaliteParIdentifiantSeulementQuandEnregistree() {
        Recolte a = new Recolte(5, LocalDateTime.now(), BigDecimal.ONE, "A", 1);
        Recolte b = new Recolte(5, LocalDateTime.now().minusDays(1), BigDecimal.TEN, "B", 1);
        Recolte nouvelleA = new Recolte(1, BigDecimal.ONE, "A");
        Recolte nouvelleB = new Recolte(1, BigDecimal.ONE, "A");
        assertEquals(a, b);
        assertNotEquals(nouvelleA, nouvelleB);
    }
}