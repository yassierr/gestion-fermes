package ma.ferme.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ma.ferme.TestSupport;
import ma.ferme.model.Client;
import ma.ferme.model.enums.TypeClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests CRUD de {@link ClientDao} (necessitent la base PostgreSQL locale). */
class ClientDaoTest {

    private final ClientDao dao = new ClientDao();
    private Client client;

    @BeforeEach
    void preparer() {
        assumeTrue(TestSupport.baseDisponible(), "Base non disponible : test ignore");
        client = new Client("T" + (System.nanoTime() % 1_000_000_000L), "Client Test",
                TypeClient.GROSSISTE, "0611111111", "Casablanca");
    }

    @AfterEach
    void nettoyer() {
        if (client != null && client.getId() > 0) {
            dao.delete(client.getId());
        }
    }

    @Test
    void insertGenereUnIdentifiantEtFindByIdRelit() {
        dao.insert(client);
        assertTrue(client.getId() > 0);
        Client relu = dao.findById(client.getId()).orElseThrow();
        assertEquals(client.getCode(), relu.getCode());
        assertEquals(TypeClient.GROSSISTE, relu.getType());
    }

    @Test
    void findByCodeRetrouveLeClient() {
        dao.insert(client);
        assertTrue(dao.findByCode(client.getCode()).isPresent());
        assertTrue(dao.findByCode("INEXISTANT-XYZ").isEmpty());
    }

    @Test
    void updateModifieLesChamps() {
        dao.insert(client);
        client.setNom("Nom modifié");
        client.setType(TypeClient.EXPORTATEUR);
        dao.update(client);
        Client relu = dao.findById(client.getId()).orElseThrow();
        assertEquals("Nom modifié", relu.getNom());
        assertEquals(TypeClient.EXPORTATEUR, relu.getType());
    }

    @Test
    void deleteSupprimeLeClient() {
        dao.insert(client);
        int id = client.getId();
        dao.delete(id);
        assertTrue(dao.findById(id).isEmpty());
        client = null;
    }

    @Test
    void codeEnDoubleEstRefuse() {
        dao.insert(client);
        Client doublon = new Client(client.getCode(), "Autre", TypeClient.MARCHE_GROS, null, null);
        assertThrows(DaoException.class, () -> dao.insert(doublon));
    }

    @Test
    void rechercherTrouveParNom() {
        dao.insert(client);
        assertTrue(dao.rechercher("Client Test").stream().anyMatch(c -> c.getId() == client.getId()));
    }
}