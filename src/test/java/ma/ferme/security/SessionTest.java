package ma.ferme.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Tests de {@link Session}. */
class SessionTest {

    @AfterEach
    void nettoyer() {
        Session.fermer();
    }

    @Test
    void ouvrirMemoriseLUtilisateur() {
        Session.ouvrir(2, "Driss El Amrani", "RESPONSABLE", 1);
        assertTrue(Session.estConnecte());
        assertEquals("RESPONSABLE", Session.getRole());
        assertEquals(1, Session.getIdFerme());
    }

    @Test
    void fermerEfface() {
        Session.ouvrir(1, "Admin", "ADMIN", null);
        Session.fermer();
        assertFalse(Session.estConnecte());
        assertNull(Session.getRole());
    }
}