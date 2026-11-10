package ma.ferme.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LignePaieTest {

    @Test
    @DisplayName("Devrait créer une ligne de paie valide avec le constructeur de création")
    void testConstructeurCreation() {
        BigDecimal montant = new BigDecimal("375.00");
        LignePaie ligne = new LignePaie(5, 2.5, montant);

        assertEquals(5, ligne.getIdOuvrier());
        assertEquals(2.5, ligne.getJoursPayes());
        assertEquals(0, montant.compareTo(ligne.getMontant()));
    }

    @Test
    @DisplayName("Devrait créer une ligne de paie complète (Constructeur DAO)")
    void testConstructeurDao() {
        BigDecimal montant = new BigDecimal("600.00");
        LignePaie ligne = new LignePaie(1, 10, 5, 4.0, montant);

        assertEquals(1, ligne.getId());
        assertEquals(10, ligne.getIdRecuPaie());
        assertEquals(5, ligne.getIdOuvrier());
        assertEquals(4.0, ligne.getJoursPayes());
        assertEquals(0, montant.compareTo(ligne.getMontant()));
    }

    @Test
    @DisplayName("Devrait lever une exception si les jours payés ou le montant sont négatifs")
    void testValidationsContraintes() {
        BigDecimal montantValide = new BigDecimal("150.00");

        // Test jours négatifs
        assertThrows(IllegalArgumentException.class, () -> {
            new LignePaie(1, -1.0, montantValide);
        });

        // Test montant négatif
        assertThrows(IllegalArgumentException.class, () -> {
            new LignePaie(1, 2.0, new BigDecimal("-50.00"));
        });

        // Test montant nul
        assertThrows(IllegalArgumentException.class, () -> {
            new LignePaie(1, 2.0, null);
        });
    }

    @Test
    @DisplayName("Devrait modifier correctement les valeurs via les setters")
    void testSetters() {
        LignePaie ligne = new LignePaie(1, 1.0, new BigDecimal("100.00"));

        ligne.setId(10);
        ligne.setIdRecuPaie(20);
        ligne.setIdOuvrier(2);
        ligne.setJoursPayes(3.5);
        ligne.setMontant(new BigDecimal("350.00"));

        assertEquals(10, ligne.getId());
        assertEquals(20, ligne.getIdRecuPaie());
        assertEquals(2, ligne.getIdOuvrier());
        assertEquals(3.5, ligne.getJoursPayes());
        assertEquals(0, new BigDecimal("350.00").compareTo(ligne.getMontant()));
    }
}