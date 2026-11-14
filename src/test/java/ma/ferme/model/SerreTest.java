
package ma.ferme.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SerreTest {

    @Test
    void testDefaultConstructor() {
        Serre serre = new Serre();

        assertTrue(serre.getId() > 0);
        assertNull(serre.getNom());
        assertEquals(0.0, serre.getSurfaceM2());
        assertNull(serre.getVariete());
        assertNull(serre.getTypeAbri());
        assertNull(serre.getIrrigation());
    }

    @Test
    void testParameterizedConstructor() {
        Serre serre = new Serre(
            "Serre A",
            500.0,
            "Cherry Tomato",
            "Tunnel",
            "Goutte-à-goutte"
        );

        assertTrue(serre.getId() > 0);
        assertEquals("Serre A", serre.getNom());
        assertEquals(500.0, serre.getSurfaceM2());
        assertEquals("Cherry Tomato", serre.getVariete());
        assertEquals("Tunnel", serre.getTypeAbri());
        assertEquals("Goutte-à-goutte", serre.getIrrigation());
    }

    @Test
    void testSetNom() {
        Serre serre = new Serre();

        serre.setNom("Serre B");

        assertEquals("Serre B", serre.getNom());
    }

    @Test
    void testSetSurfaceM2() {
        Serre serre = new Serre();

        serre.setSurfaceM2(750.0);

        assertEquals(750.0, serre.getSurfaceM2());
    }

    @Test
    void testSetVariete() {
        Serre serre = new Serre();

        serre.setVariete("Cherry Tomato");

        assertEquals("Cherry Tomato", serre.getVariete());
    }

    @Test
    void testSetTypeAbri() {
        Serre serre = new Serre();

        serre.setTypeAbri("Tunnel");

        assertEquals("Tunnel", serre.getTypeAbri());
    }

    @Test
    void testSetIrrigation() {
        Serre serre = new Serre();

        serre.setIrrigation("Goutte-à-goutte");

        assertEquals("Goutte-à-goutte", serre.getIrrigation());
    }

    @Test
    void testAutomaticIdGeneration() {
        Serre serre1 = new Serre();
        Serre serre2 = new Serre();

        assertTrue(serre2.getId() > serre1.getId());
    }
}
