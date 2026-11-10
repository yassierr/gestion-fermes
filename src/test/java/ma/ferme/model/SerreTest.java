
package ma.ferme.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SerreTest {

    @Test
    void testDefaultConstructor() {
        Serre serre = new Serre();

        assertEquals(0, serre.getId());
        assertNull(serre.getNom());
        assertEquals(0.0, serre.getSurfaceM2());
        assertNull(serre.getVariete());
        assertNull(serre.getTypeAbri());
        assertNull(serre.getIrrigation());
        assertEquals(0, serre.getIdFerme());
    }

    @Test
    void testParameterizedConstructor() {
        Serre serre = new Serre(
                "Serre A",
                500.0,
                "Cherry Tomato",
                "Tunnel",
                "Goutte-à-goutte",
                1
        );

        assertEquals(0, serre.getId());
        assertEquals("Serre A", serre.getNom());
        assertEquals(500.0, serre.getSurfaceM2());
        assertEquals("Cherry Tomato", serre.getVariete());
        assertEquals("Tunnel", serre.getTypeAbri());
        assertEquals("Goutte-à-goutte", serre.getIrrigation());
        assertEquals(1, serre.getIdFerme());
    }

    @Test
    void testParameterizedConstructorWithId() {
        Serre serre = new Serre(
                10,
                "Serre B",
                750.0,
                "Cherry Tomato",
                "Tunnel",
                "Goutte-à-goutte",
                2
        );

        assertEquals(10, serre.getId());
        assertEquals("Serre B", serre.getNom());
        assertEquals(750.0, serre.getSurfaceM2());
        assertEquals("Cherry Tomato", serre.getVariete());
        assertEquals("Tunnel", serre.getTypeAbri());
        assertEquals("Goutte-à-goutte", serre.getIrrigation());
        assertEquals(2, serre.getIdFerme());
    }

    @Test
    void testSetId() {
        Serre serre = new Serre();

        serre.setId(5);

        assertEquals(5, serre.getId());
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
    void testSetIdFerme() {
        Serre serre = new Serre();
        serre.setIdFerme(3);

        assertEquals(3, serre.getIdFerme());
    }
}
