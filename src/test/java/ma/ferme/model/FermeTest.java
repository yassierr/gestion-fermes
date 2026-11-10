package ma.ferme.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FermeTest {

    @Test
    void testDefaultConstructor() {
        Ferme ferme = new Ferme();

        assertEquals(0, ferme.getId());
        assertNull(ferme.getNom());
        assertNull(ferme.getLocalisation());
    }

    @Test
    void testParameterizedConstructor() {
        Ferme ferme = new Ferme("Ferme A", "Agadir");

        assertEquals(0, ferme.getId());
        assertEquals("Ferme A", ferme.getNom());
        assertEquals("Agadir", ferme.getLocalisation());
    }

    @Test
    void testParameterizedConstructorWithId() {
        Ferme ferme = new Ferme(10, "Ferme B", "Taroudant");

        assertEquals(10, ferme.getId());
        assertEquals("Ferme B", ferme.getNom());
        assertEquals("Taroudant", ferme.getLocalisation());
    }

    @Test
    void testSetId() {
        Ferme ferme = new Ferme();
        ferme.setId(5);

        assertEquals(5, ferme.getId());
    }

    @Test
    void testSetNom() {
        Ferme ferme = new Ferme();
        ferme.setNom("Ferme C");

        assertEquals("Ferme C", ferme.getNom());
    }

    @Test
    void testSetLocalisation() {
        Ferme ferme = new Ferme();
        ferme.setLocalisation("Marrakech");

        assertEquals("Marrakech", ferme.getLocalisation());
    }
}