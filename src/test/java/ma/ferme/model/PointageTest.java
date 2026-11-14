package ma.ferme.model;

import ma.ferme.model.enums.StatutPointage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PointageTest {

    @Test
    @DisplayName("Devrait créer un pointage valide avec le constructeur de création")
    void testConstructeurCreation() {
        LocalDate aujourdhui = LocalDate.now();
        Pointage pointage = new Pointage(10, aujourdhui, StatutPointage.PRESENT);

        assertEquals(10, pointage.getIdOuvrier());
        assertEquals(aujourdhui, pointage.getDatePointage());
        assertEquals(StatutPointage.PRESENT, pointage.getStatut());
    }

    @Test
    @DisplayName("Devrait calculer 1.0 jour pour un statut PRESENT")
    void testValeurJournee_Present() {
        Pointage pointage = new Pointage(1, LocalDate.now(), StatutPointage.PRESENT);
        assertEquals(1.0, pointage.getValeurJournee());
    }

    @Test
    @DisplayName("Devrait calculer 0.0 jour pour un statut ABSENT")
    void testValeurJournee_Absent() {
        Pointage pointage = new Pointage(1, LocalDate.now(), StatutPointage.ABSENT);
        assertEquals(0.0, pointage.getValeurJournee());
    }

    @Test
    @DisplayName("Devrait lever une exception si la date ou le statut est nul")
    void testValidationsContraintes() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Pointage(1, null, StatutPointage.PRESENT);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new Pointage(1, LocalDate.now(), null);
        });
    }
}