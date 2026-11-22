package ma.ferme.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import ma.ferme.TestSupport;
import ma.ferme.dao.ClientDao;
import ma.ferme.model.Client;
import ma.ferme.model.LigneVente;
import ma.ferme.model.Vente;
import ma.ferme.model.enums.StatutVente;
import ma.ferme.model.enums.TypeClient;
import org.junit.jupiter.api.Test;

/** Tests de {@link VenteService} : validation (sans base) et scenario complet (avec base). */
class VenteServiceTest {

    private final VenteService service = new VenteService();

    private Vente vente(int idClient, LigneVente... lignes) {
        Vente v = new Vente();
        v.setIdClient(idClient);
        for (LigneVente l : lignes) {
            v.ajouterLigne(l);
        }
        return v;
    }

    @Test
    void clientObligatoire() {
        Vente v = vente(0, new LigneVente(1, BigDecimal.TEN, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> service.creerLivraison(v, 1));
    }

    @Test
    void auMoinsUneLigne() {
        assertThrows(IllegalArgumentException.class, () -> service.creerLivraison(vente(1), 1));
    }

    @Test
    void quantiteDoitEtrePositive() {
        Vente v = vente(1, new LigneVente(1, BigDecimal.ZERO, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> service.creerLivraison(v, 1));
    }

    @Test
    void prixDoitEtrePositif() {
        Vente v = vente(1, new LigneVente(1, BigDecimal.TEN, new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> service.creerLivraison(v, 1));
    }

    @Test
    void fermeObligatoireSurChaqueLigne() {
        Vente v = vente(1, new LigneVente(0, BigDecimal.TEN, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> service.creerLivraison(v, 1));
    }

    @Test
    void scenarioLivrerPayerEtStockInsuffisant() {
        assumeTrue(TestSupport.baseDisponible(), "Base non disponible : test ignore");
        ClientDao clientDao = new ClientDao();
        Client client = new Client("VS" + (System.nanoTime() % 1_000_000_000L), "Client Service",
                TypeClient.EXPORTATEUR, null, "Agadir");
        clientDao.insert(client);
        int ferme = TestSupport.idFerme();
        Vente v = null;
        try {
            // 1. stock insuffisant refuse
            BigDecimal stock = service.stockDisponible(ferme);
            Vente trop = vente(client.getId(), new LigneVente(ferme, stock.add(BigDecimal.TEN), BigDecimal.TEN));
            assertThrows(IllegalArgumentException.class, () -> service.creerLivraison(trop, TestSupport.idAdmin()));

            // 2. livraison valide
            v = service.creerLivraison(vente(client.getId(), new LigneVente(ferme, BigDecimal.TEN, BigDecimal.TEN)),
                    TestSupport.idAdmin());
            assertEquals(StatutVente.LIVREE, v.getStatut());
            assertNotNull(v.getNumeroBon());

            // 3. paiement
            service.marquerPayee(v.getId(), LocalDate.now());
            assertEquals(StatutVente.PAYEE, service.trouver(v.getId()).orElseThrow().getStatut());

            // 4. on ne peut ni repayer ni annuler une vente payee
            int id = v.getId();
            assertThrows(IllegalStateException.class, () -> service.marquerPayee(id, null));
            assertThrows(IllegalStateException.class, () -> service.annuler(id));
        } finally {
            if (v != null) {
                new ma.ferme.dao.VenteDao().delete(v.getId());
            }
            clientDao.delete(client.getId());
        }
    }
}