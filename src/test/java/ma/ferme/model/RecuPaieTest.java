package ma.ferme.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RecuPaieTest {

    @Test
    @DisplayName("Devrait créer un reçu de paie avec un total initial à zéro et une liste vide")
    void testConstructeurCreation() {
        LocalDate debut = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 15);
        LocalDate paiement = LocalDate.of(2026, 11, 16);

        RecuPaie recu = new RecuPaie(3, paiement, debut, fin);

        assertEquals(3, recu.getIdAgentRh());
        assertEquals(paiement, recu.getDatePaiement());
        assertEquals(debut, recu.getPeriodeDebut());
        assertEquals(fin, recu.getPeriodeFin());
        assertEquals(0, BigDecimal.ZERO.compareTo(recu.getTotal()));
        assertTrue(recu.getLignes().isEmpty());
    }

    @Test
    @DisplayName("Devrait recalculer automatiquement le total lors de l'ajout de lignes de paie")
    void testAjouterLigneEtRecalculerTotal() {
        RecuPaie recu = new RecuPaie(3, LocalDate.now(), LocalDate.now().minusDays(10), LocalDate.now());

        LignePaie ligne1 = new LignePaie(1, 2.0, new BigDecimal("300.00"));
        LignePaie ligne2 = new LignePaie(2, 3.5, new BigDecimal("525.00"));

        recu.ajouterLigne(ligne1);
        assertEquals(0, new BigDecimal("300.00").compareTo(recu.getTotal()));

        recu.ajouterLigne(ligne2);
        // Total attendu : 300.00 + 525.00 = 825.00
        assertEquals(0, new BigDecimal("825.00").compareTo(recu.getTotal()));
        assertEquals(2, recu.getLignes().size());
    }

    @Test
    @DisplayName("Devrait lever une exception si la date de début est après la date de fin")
    void testValidationIncoherenceDates() {
        LocalDate debut = LocalDate.of(2026, 11, 15);
        LocalDate fin = LocalDate.of(2026, 11, 1); // Invalide

        assertThrows(IllegalArgumentException.class, () -> {
            new RecuPaie(1, LocalDate.now(), debut, fin);
        });
    }

    @Test
    @DisplayName("Devrait lever une exception si une date obligatoire est nulle")
    void testValidationDatesNulles() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RecuPaie(1, null, LocalDate.now(), LocalDate.now());
        });
    }
}