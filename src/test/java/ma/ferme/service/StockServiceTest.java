package ma.ferme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ma.ferme.TestSupport;
import ma.ferme.dao.ClientDao;
import ma.ferme.dao.VenteDao;
import ma.ferme.model.Administrateur;
import ma.ferme.model.Client;
import ma.ferme.model.LigneVente;
import ma.ferme.model.ResponsableFerme;
import ma.ferme.model.Vente;
import ma.ferme.model.enums.TypeClient;

/**
 * Tests de {@link StockService} (RG-01 : stock = récoltes − livraisons non annulées).
 * Les tests de droits n'ont pas besoin de la base ; les calculs utilisent PostgreSQL
 * et sont ignorés si elle n'est pas configurée. Chaque test crée sa propre ferme et la supprime.
 */
class StockServiceTest {

    private final StockService service = new StockService();
    private final VenteDao venteDao = new VenteDao();
    private final ClientDao clientDao = new ClientDao();

    private final List<Integer> fermes = new ArrayList<>();
    private final List<Integer> ventes = new ArrayList<>();
    private final List<Integer> clients = new ArrayList<>();
    private final List<Integer> admins = new ArrayList<>();

    @AfterEach
    void nettoyer() {
        if (!TestSupport.baseDisponible()) {
            return;
        }
        ventes.forEach(id -> TestSupport.executer("DELETE FROM vente WHERE id = " + id));
        fermes.forEach(id -> TestSupport.executer("DELETE FROM recolte WHERE id_ferme = " + id));
        clients.forEach(id -> TestSupport.executer("DELETE FROM client WHERE id = " + id));
        admins.forEach(id -> TestSupport.executer("DELETE FROM utilisateur WHERE id = " + id));
        fermes.forEach(id -> TestSupport.executer("DELETE FROM ferme WHERE id = " + id));
    }

    // ---------- sans base de données ----------

    @Test
    void unAdministrateurNaPasDeStockDeFerme() {
        Administrateur admin = new Administrateur("Admin", "admin", "hash");
        assertThrows(SecurityException.class, () -> service.stockDisponible(admin));
    }

    @Test
    void unResponsableInactifEstRefuse() {
        ResponsableFerme inactif = new ResponsableFerme(1, "Resp", "resp", "hash", false, 1);
        assertThrows(SecurityException.class, () -> service.stockDisponible(inactif));
    }

    @Test
    void quantiteAbsenteNestJamaisCouverte() {
        assertFalse(service.estSuffisant(1, null));
    }

    // ---------- avec la base ----------

    @Test
    void stockEgalRecoltesMoinsLivraisonsNonAnnulees() {
        int ferme = preparerFerme();
        assertEquals(0, BigDecimal.ZERO.compareTo(service.stockDisponible(ferme)));

        TestSupport.executer("INSERT INTO recolte (poids_kg, variete, id_ferme) VALUES (100, 'Cerise', " + ferme + ")");
        assertEquals(0, new BigDecimal("100").compareTo(service.stockDisponible(ferme)));

        int vente = livrer(ferme, "30");
        assertEquals(0, new BigDecimal("70").compareTo(service.stockDisponible(ferme)));

        TestSupport.executer("UPDATE vente SET statut = 'ANNULEE' WHERE id = " + vente);
        assertEquals(0, new BigDecimal("100").compareTo(service.stockDisponible(ferme)),
                "annuler une livraison remet les kg en stock (RG-05)");
    }

    @Test
    void stockDuResponsableCorrespondASaFerme() {
        int ferme = preparerFerme();
        TestSupport.executer("INSERT INTO recolte (poids_kg, variete, id_ferme) VALUES (42.5, 'Cerise', " + ferme + ")");
        ResponsableFerme acteur = new ResponsableFerme(1, "Resp", "resp", "hash", true, ferme);
        assertEquals(0, new BigDecimal("42.5").compareTo(service.stockDisponible(acteur)));
    }

    @Test
    void estSuffisantComparaisonAuStockExact() {
        int ferme = preparerFerme();
        TestSupport.executer("INSERT INTO recolte (poids_kg, variete, id_ferme) VALUES (70, 'Cerise', " + ferme + ")");
        assertTrue(service.estSuffisant(ferme, new BigDecimal("70")));
        assertTrue(service.estSuffisant(ferme, new BigDecimal("10")));
        assertFalse(service.estSuffisant(ferme, new BigDecimal("70.01")));
    }

    // ---------- outils ----------

    /** Crée une ferme vide, ignorant le test si la base n'est pas disponible. */
    private int preparerFerme() {
        assumeTrue(TestSupport.baseDisponible(), "PostgreSQL non configuré : test ignoré");
        int id = TestSupport.premierId("INSERT INTO ferme (nom, localisation) VALUES ('Ferme test stock "
                + System.nanoTime() + "', 'Agadir') RETURNING id");
        fermes.add(id);
        return id;
    }

    /** Crée une livraison de {@code kg} kg prélevés sur la ferme et retourne son identifiant. */
    private int livrer(int ferme, String kg) {
        long suffixe = System.nanoTime() % 1_000_000_000L;
        int admin = TestSupport.premierId("INSERT INTO utilisateur (nom_complet, login, mot_de_passe_hash, role) "
                + "VALUES ('Admin test', 'adm" + suffixe + "', 'hash', 'ADMIN') RETURNING id");
        admins.add(admin);
        Client client = new Client("S" + suffixe, "Client test", TypeClient.STATION_CONDITIONNEMENT, null, "Agadir");
        clientDao.insert(client);
        clients.add(client.getId());

        Vente vente = new Vente();
        vente.setIdClient(client.getId());
        vente.setIdAdmin(admin);
        vente.setDateLivraison(LocalDate.now());
        vente.ajouterLigne(new LigneVente(ferme, new BigDecimal(kg), new BigDecimal("12.50")));
        venteDao.insertAvecLignes(vente);
        ventes.add(vente.getId());
        return vente.getId();
    }
}