
package ma.ferme.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import ma.ferme.TestSupport;
import ma.ferme.model.Administrateur;
import ma.ferme.model.Client;
import ma.ferme.model.Ferme;
import ma.ferme.model.LigneVente;
import ma.ferme.model.Recolte;
import ma.ferme.model.Vente;
import ma.ferme.model.enums.StatutVente;
import ma.ferme.model.enums.TypeClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VenteDaoTest {

    private final VenteDao dao = new VenteDao();
    private final ClientDao clientDao = new ClientDao();
    private final FermeDao fermeDao = new FermeDao();
    private final UtilisateurDao utilisateurDao = new UtilisateurDao();
    private final RecolteDao recolteDao = new RecolteDao();

    private Ferme ferme;
    private Administrateur admin;
    private Client client;
    private Recolte recolte;
    private Vente vente;

    @BeforeEach
    void preparer() {
        assumeTrue(
                TestSupport.baseDisponible(),
                "Base non disponible : test ignore"
        );

        // Creer une ferme de test
        ferme = new Ferme(
                "Ferme Test " + System.nanoTime(),
                "Agadir"
        );
        fermeDao.insert(ferme);

        // Creer un administrateur de test
        admin = new Administrateur(
                "Administrateur Test",
                "admin_test_" + System.nanoTime(),
                "test_hash"
        );
        utilisateurDao.insert(admin);

        // Creer un client de test
        client = new Client(
                "V" + System.nanoTime(),
                "Client Vente",
                TypeClient.STATION_CONDITIONNEMENT,
                null,
                "Agadir"
        );
        clientDao.insert(client);
    }

    @AfterEach
    void nettoyer() {

        // Supprimer la vente
        if (vente != null && vente.getId() > 0) {
            dao.delete(vente.getId());
        }

        // Supprimer le client
        if (client != null && client.getId() > 0) {
            clientDao.delete(client.getId());
        }

        // Supprimer la recolte
        if (recolte != null && recolte.getId() > 0) {
            recolteDao.delete(recolte.getId());
        }

        // Supprimer l'administrateur
        if (admin != null && admin.getId() > 0) {
            utilisateurDao.delete(admin.getId());
        }

        // Supprimer la ferme
        if (ferme != null && ferme.getId() > 0) {
            fermeDao.delete(ferme.getId());
        }
    }

    private Vente nouvelleVente(int idFerme, String kg) {
        Vente v = new Vente();

        v.setIdClient(client.getId());
        v.setIdAdmin(admin.getId());
        v.setDateLivraison(LocalDate.now());

        v.ajouterLigne(
                new LigneVente(
                        idFerme,
                        new BigDecimal(kg),
                        new BigDecimal("12.50")
                )
        );

        return v;
    }

    @Test
    void insertAvecLignesGenereNumeroEtIdentifiants() {
        vente = nouvelleVente(ferme.getId(), "10");

        dao.insertAvecLignes(vente);

        assertTrue(vente.getId() > 0);
        assertTrue(vente.getNumeroBon().matches("BL-\\d{4}-\\d{4}"));

        Vente relue = dao.findById(vente.getId()).orElseThrow();

        assertEquals(1, relue.getLignes().size());
        assertEquals(
                0,
                new BigDecimal("125.00").compareTo(relue.getTotal())
        );
        assertEquals(StatutVente.LIVREE, relue.getStatut());
    }

    @Test
    void deuxVentesOntDesNumerosDifferents() {
        vente = nouvelleVente(ferme.getId(), "1");

        Vente autre = nouvelleVente(ferme.getId(), "1");

        dao.insertAvecLignes(vente);
        dao.insertAvecLignes(autre);

        try {
            assertNotEquals(
                    vente.getNumeroBon(),
                    autre.getNumeroBon()
            );
        } finally {
            dao.delete(autre.getId());
        }
    }

    @Test
    void transactionAnnuleToutEnCasDErreur() {
        int avant = dao.findAll().size();

        vente = nouvelleVente(ferme.getId(), "5");

        // Ajouter une ligne avec une ferme inexistante
        vente.ajouterLigne(
                new LigneVente(
                        999999,
                        new BigDecimal("5"),
                        new BigDecimal("10")
                )
        );

        assertThrows(
                DaoException.class,
                () -> dao.insertAvecLignes(vente)
        );

        assertEquals(
                avant,
                dao.findAll().size(),
                "La vente ne doit pas etre conservee"
        );

        assertEquals(0, vente.getId());
    }

    @Test
    void updateChangeLeStatutEtLaDateDePaiement() {
        vente = nouvelleVente(ferme.getId(), "2");

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
        vente = nouvelleVente(ferme.getId(), "2");

        vente.setStatut(StatutVente.PAYEE);

        assertThrows(
                DaoException.class,
                () -> dao.insertAvecLignes(vente)
        );
    }

    @Test
    void annulerRestitueLeStock() {

        // Creer une recolte de 100 kg
        recolte = new Recolte(
                ferme.getId(),
                new BigDecimal("100"),
                "Cerise Test"
        );

        recolteDao.insert(recolte);

        // Stock initial
        BigDecimal avant = dao.stockDisponible(ferme.getId());

        // Vendre 20 kg
        vente = nouvelleVente(ferme.getId(), "20");

        dao.insertAvecLignes(vente);

        // Verifier la diminution du stock
        assertEquals(
                0,
                avant.subtract(new BigDecimal("20"))
                        .compareTo(dao.stockDisponible(ferme.getId()))
        );

        // Annuler la vente
        vente.setStatut(StatutVente.ANNULEE);
        dao.update(vente);

        // Verifier la restitution du stock
        assertEquals(
                0,
                avant.compareTo(dao.stockDisponible(ferme.getId()))
        );
    }

    @Test
    void indicateursFinanciersAugmententAvecUneLivraison() {
        BigDecimal caAvant = dao.chiffreAffaires();
        BigDecimal creancesAvant = dao.creances();

        vente = nouvelleVente(ferme.getId(), "10");

        dao.insertAvecLignes(vente);

        assertEquals(
                0,
                caAvant.add(new BigDecimal("125.00"))
                        .compareTo(dao.chiffreAffaires())
        );

        assertEquals(
                0,
                creancesAvant.add(new BigDecimal("125.00"))
                        .compareTo(dao.creances())
        );
    }

    @Test
    void chiffreAffairesParFermeInclutLesLivraisonsNonAnnulees() {
        vente = nouvelleVente(ferme.getId(), "10");
        dao.insertAvecLignes(vente);

        assertEquals(0, new BigDecimal("125.00").compareTo(dao.chiffreAffairesParFerme().get(ferme.getId())));
    }

    @Test
    void findByFermeRetrouveLaVente() {
        vente = nouvelleVente(ferme.getId(), "3");

        dao.insertAvecLignes(vente);

        assertTrue(
                dao.findByFerme(ferme.getId())
                        .stream()
                        .anyMatch(v -> v.getId() == vente.getId())
        );
    }
}
