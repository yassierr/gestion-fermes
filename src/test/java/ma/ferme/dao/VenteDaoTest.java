package ma.ferme.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import ma.ferme.TestSupport;
import ma.ferme.model.Client;
import ma.ferme.model.LigneVente;
import ma.ferme.model.Vente;
import ma.ferme.model.enums.StatutVente;
import ma.ferme.model.enums.TypeClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests de {@link VenteDao}, dont la transaction (necessitent la base PostgreSQL locale). */
class VenteDaoTest {

    private final VenteDao dao = new VenteDao();
    private final ClientDao clientDao = new ClientDao();
    private Client client;
    private Vente vente;

    @BeforeEach
    void preparer() {
        assumeTrue(TestSupport.baseDisponible(), "Base non disponible : test ignore");
        client = new Client("V" + (System.nanoTime() % 1_000_000_000L), "Client Vente",
                TypeClient.STATION_CONDITIONNEMENT, null, "Agadir");
        clientDao.insert(client);
    }

    @AfterEach
    void nettoyer() {
        if (vente != null && vente.getId() > 0) {
            dao.delete(vente.getId());
        }
        if (client != null && client.getId() > 0) {
            clientDao.delete(client.getId());
        }
    }

    private Vente nouvelleVente(int idFerme, String kg) {
        Vente v = new Vente();
        v.setIdClient(client.getId());
        v.setIdAdmin(TestSupport.idAdmin());
        v.setDateLivraison(LocalDate.now());
        v.ajouterLigne(new LigneVente(idFerme, new BigDecimal(kg), new BigDecimal("12.50")));
        return v;
    }

    @Test
    void insertAvecLignesGenereNumeroEtIdentifiants() {
        vente = nouvelleVente(TestSupport.idFerme(), "10");
        dao.insertAvecLignes(vente);
        assertTrue(vente.getId() > 0);
        assertTrue(vente.getNumeroBon().matches("BL-\\d{4}-\\d{4}"));
        Vente relue = dao.findById(vente.getId()).orElseThrow();
        assertEquals(1, relue.getLignes().size());
        assertEquals(0, new BigDecimal("125.00").compareTo(relue.getTotal()));
        assertEquals(StatutVente.LIVREE, relue.getStatut());
    }

    @Test
    void deuxVentesOntDesNumerosDifferents() {
        vente = nouvelleVente(TestSupport.idFerme(), "1");
        Vente autre = nouvelleVente(TestSupport.idFerme(), "1");
        dao.insertAvecLignes(vente);
        dao.insertAvecLignes(autre);
        try {
            assertNotEquals(vente.getNumeroBon(), autre.getNumeroBon());
        } finally {
            dao.delete(autre.getId());
        }
    }

    @Test
    void transactionAnnuleToutEnCasDErreur() {
        int avant = dao.findAll().size();
        vente = nouvelleVente(TestSupport.idFerme(), "5");
        vente.ajouterLigne(new LigneVente(999999, new BigDecimal("5"), new BigDecimal("10"))); // ferme inexistante
        assertThrows(DaoException.class, () -> dao.insertAvecLignes(vente));
        assertEquals(avant, dao.findAll().size(), "la vente ne doit pas etre conservee");
        assertEquals(0, vente.getId());
    }

    @Test
    void updateChangeLeStatutEtLaDateDePaiement() {
        vente = nouvelleVente(TestSupport.idFerme(), "2");
        dao.insertAvecLignes(vente);
        vente.setStatut(StatutVente.PAYEE);
        vente.setDatePaiement(LocalDate.now());
        dao.update(vente);
        Vente relue = dao.findById(vente.getId()).orElseThrow();
        assertEquals(StatutVente.PAYEE, relue.getStatut());
        assertNotNull(relue.getDatePaiement());
    }

    @Test
    void ventePayeeSansDateEstRefuseParLaBase() {
        vente = nouvelleVente(TestSupport.idFerme(), "2");
        vente.setStatut(StatutVente.PAYEE);
        assertThrows(DaoException.class, () -> dao.insertAvecLignes(vente));
        vente = null;
    }

    @Test
    void annulerRestitueLeStock() {
        int ferme = TestSupport.idFerme();
        BigDecimal avant = dao.stockDisponible(ferme);
        vente = nouvelleVente(ferme, "20");
        dao.insertAvecLignes(vente);
        assertEquals(0, avant.subtract(new BigDecimal("20")).compareTo(dao.stockDisponible(ferme)));
        vente.setStatut(StatutVente.ANNULEE);
        dao.update(vente);
        assertEquals(0, avant.compareTo(dao.stockDisponible(ferme)));
    }

    @Test
    void indicateursFinanciersAugmententAvecUneLivraison() {
        BigDecimal caAvant = dao.chiffreAffaires();
        BigDecimal creancesAvant = dao.creances();
        vente = nouvelleVente(TestSupport.idFerme(), "10"); // 125.00 MAD
        dao.insertAvecLignes(vente);
        assertEquals(0, caAvant.add(new BigDecimal("125.00")).compareTo(dao.chiffreAffaires()));
        assertEquals(0, creancesAvant.add(new BigDecimal("125.00")).compareTo(dao.creances()));
    }

    @Test
    void findByFermeRetrouveLaVente() {
        int ferme = TestSupport.idFerme();
        vente = nouvelleVente(ferme, "3");
        dao.insertAvecLignes(vente);
        assertTrue(dao.findByFerme(ferme).stream().anyMatch(v -> v.getId() == vente.getId()));
    }
}