package ma.ferme.model;

import static org.junit.jupiter.api.Assertions.*;

import ma.ferme.model.enums.TypeClient;
import org.junit.jupiter.api.Test;

/** Tests du modele {@link Client}. */
class ClientTest {

    @Test
    void constructeurRenseigneLesChamps() {
        Client c = new Client("CL-001", "Station Atlas", TypeClient.STATION_CONDITIONNEMENT, "0600000000", "Agadir");
        assertEquals("CL-001", c.getCode());
        assertEquals("Station Atlas", c.getNom());
        assertEquals(TypeClient.STATION_CONDITIONNEMENT, c.getType());
        assertTrue(c.isActif());
        assertEquals(0, c.getId());
    }

    @Test
    void toStringRetourneLeNom() {
        assertEquals("Atlas", new Client("C", "Atlas", TypeClient.GROSSISTE, null, null).toString());
    }

    @Test
    void libellesDesTypes() {
        assertEquals("Exportateur", TypeClient.EXPORTATEUR.getLibelle());
    }
}