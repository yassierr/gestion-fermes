package ma.ferme.model;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FermeTest {

    @Test
    void testDefaultConstructor() {
        Ferme ferme = new Ferme();

        assertTrue(ferme.getId() > 0);
        assertNull(ferme.getNom());
        assertNull(ferme.getLocalisation());
    }

    @Test
    void testParameterizedConstructor() {
        Ferme ferme = new Ferme("Ferme Atlas", "Taroudant");

        assertTrue(ferme.getId() > 0);
        assertEquals("Ferme Atlas", ferme.getNom());
        assertEquals("Taroudant", ferme.getLocalisation());
    }

    @Test
    void testSetNom() {
        Ferme ferme = new Ferme();

        ferme.setNom("Ferme Atlas");

        assertEquals("Ferme Atlas", ferme.getNom());
    }

    @Test
    void testSetLocalisation() {
        Ferme ferme = new Ferme();

        ferme.setLocalisation("Agadir");

        assertEquals("Agadir", ferme.getLocalisation());
    }

    @Test
    void testAutomaticIdGeneration() {
        Ferme ferme1 = new Ferme();
        Ferme ferme2 = new Ferme();

        assertTrue(ferme2.getId() > ferme1.getId());
    }

    @Test
    void testGetStockDisponible() {
        Ferme ferme = new Ferme();

        assertEquals(0.0, ferme.getStockDisponible());
    }
}

