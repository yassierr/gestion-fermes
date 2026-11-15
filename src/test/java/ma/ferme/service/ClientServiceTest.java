package ma.ferme.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ma.ferme.TestSupport;
import ma.ferme.model.Client;
import ma.ferme.model.enums.TypeClient;
import org.junit.jupiter.api.Test;

/** Tests de {@link ClientService} : validation (sans base) et unicite du code (avec base). */
class ClientServiceTest {

    private final ClientService service = new ClientService();

    @Test
    void codeObligatoire() {
        Client c = new Client("", "Nom", TypeClient.GROSSISTE, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.ajouter(c));
    }

    @Test
    void nomObligatoire() {
        Client c = new Client("C1", " ", TypeClient.GROSSISTE, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.ajouter(c));
    }

    @Test
    void typeObligatoire() {
        Client c = new Client("C1", "Nom", null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.ajouter(c));
    }

    @Test
    void codeTropLongRefuse() {
        Client c = new Client("X".repeat(21), "Nom", TypeClient.GROSSISTE, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.ajouter(c));
    }

    @Test
    void codeDejaUtiliseRefuse() {
        assumeTrue(TestSupport.baseDisponible(), "Base non disponible : test ignore");
        Client c1 = new Client("S" + (System.nanoTime() % 1_000_000_000L), "Un", TypeClient.GROSSISTE, null, null);
        service.ajouter(c1);
        try {
            Client c2 = new Client(c1.getCode(), "Deux", TypeClient.EXPORTATEUR, null, null);
            assertThrows(IllegalArgumentException.class, () -> service.ajouter(c2));
        } finally {
            service.supprimer(c1.getId());
        }
    }
}