package ma.ferme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import ma.ferme.model.Recolte;
import ma.ferme.model.ResponsableFerme;
import ma.ferme.model.Vente;
import ma.ferme.model.enums.TypeClient;

/**
 * Tests de {@link RecolteService} : validation, droits (EF-T04) et règle RG-07
 * (modifier ou supprimer une récolte est refusé si le stock deviendrait négatif).
 * Les tests de validation n'ont pas besoin de la base ; les autres utilisent PostgreSQL
 * et sont ignorés si elle n'est pas configurée. Chaque test crée ses fermes et les supprime.
 */
class RecolteServiceTest {

    private final RecolteService service = new RecolteService();
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
    void seulUnResponsableActifPeutAgir() {
        Administrateur admin = new Administrateur("Admin", "admin", "hash");
        ResponsableFerme inactif = new ResponsableFerme(1, "Resp", "resp", "hash", false, 1);
        BigDecimal poids = BigDecimal.TEN;
        assertThrows(SecurityException.class, () -> service.enregistrer(admin, poids, "Cerise"));
        assertThrows(SecurityException.class, () -> service.enregistrer(inactif, poids, "Cerise"));
        assertThrows(SecurityException.class, () -> service.enregistrer(null, poids, "Cerise"));
        assertThrows(SecurityException.class, () -> service.modifier(admin, 1, poids, "Cerise"));
        assertThrows(SecurityException.class, () -> service.supprimer(admin, 1));
        assertThrows(SecurityException.class, () -> service.lister(admin));
        assertThrows(SecurityException.class, () -> service.stockDisponible(admin));
    }

    @Test
    void poidsOuVarieteInvalidesSontRefuses() {
        ResponsableFerme acteur = new ResponsableFerme(1, "Resp", "resp", "hash", true, 1);
        assertThrows(IllegalArgumentException.class, () -> service.enregistrer(acteur, BigDecimal.ZERO, "Cerise"));
        assertThrows(IllegalArgumentException.class, () -> service.enregistrer(acteur, new BigDecimal("-5"), "Cerise"));
        assertThrows(IllegalArgumentException.class, () -> service.enregistrer(acteur, null, "Cerise"));
        assertThrows(IllegalArgumentException.class, () -> service.enregistrer(acteur, BigDecimal.TEN, " "));
    }

    // ---------- avec la base ----------

    @Test
    void enregistrerListerModifierSupprimer() {
        int ferme = preparerFerme();
        ResponsableFerme acteur = responsable(ferme);

        Recolte r = service.enregistrer(acteur, new BigDecimal("850"), "  Sweetelle ");
        assertTrue(r.getId() > 0);
        assertEquals("Sweetelle", r.getVariete());
        assertEquals(1, service.lister(acteur).size());
        assertEquals(0, new BigDecimal("850").compareTo(service.stockDisponible(acteur)));

        service.modifier(acteur, r.getId(), new BigDecimal("900.5"), "Sungold");
        Recolte relue = service.lister(acteur).get(0);
        assertEquals(0, new BigDecimal("900.5").compareTo(relue.getPoidsKg()));
        assertEquals("Sungold", relue.getVariete());

        service.supprimer(acteur, r.getId());
        assertTrue(service.lister(acteur).isEmpty());
    }

    @Test
    void recolteInexistanteEstRefusee() {
        int ferme = preparerFerme();
        ResponsableFerme acteur = responsable(ferme);
        assertThrows(IllegalArgumentException.class, () -> service.modifier(acteur, -1, BigDecimal.TEN, "A"));
        assertThrows(IllegalArgumentException.class, () -> service.supprimer(acteur, -1));
    }

    @Test
    void unResponsableNeToucheQuALaRecolteDeSaFerme() {
        ResponsableFerme a = responsable(preparerFerme());
        ResponsableFerme b = responsable(preparerFerme());
        Recolte deA = service.enregistrer(a, new BigDecimal("100"), "Cerise");

        assertThrows(SecurityException.class, () -> service.modifier(b, deA.getId(), BigDecimal.TEN, "Cerise"));
        assertThrows(SecurityException.class, () -> service.supprimer(b, deA.getId()));
        assertTrue(service.lister(b).isEmpty());
        assertEquals(1, service.lister(a).size());
    }

    @Test
    void rg07ModifierOuSupprimerRefuseSiLeStockDevientNegatif() {
        int ferme = preparerFerme();
        ResponsableFerme acteur = responsable(ferme);
        Recolte r = service.enregistrer(acteur, new BigDecimal("100"), "Cerise");
        livrer(ferme, "60");   // stock = 40

        // 100 -> 50 : stock = 50 - 60 = -10 : refusé
        assertThrows(IllegalStateException.class, () -> service.modifier(acteur, r.getId(), new BigDecimal("50"), "Cerise"));
        // 100 -> 60 : stock = 0 : accepté (limite)
        service.modifier(acteur, r.getId(), new BigDecimal("60"), "Cerise");
        assertEquals(0, BigDecimal.ZERO.compareTo(service.stockDisponible(acteur)));
        // supprimer : stock = -60 : refusé
        assertThrows(IllegalStateException.class, () -> service.supprimer(acteur, r.getId()));
        assertEquals(1, service.lister(acteur).size());
    }

    @Test
    void rg07LAugmentationDuPoidsEstToujoursPermise() {
        int ferme = preparerFerme();
        ResponsableFerme acteur = responsable(ferme);
        Recolte r = service.enregistrer(acteur, new BigDecimal("100"), "Cerise");
        livrer(ferme, "100");  // stock = 0
        service.modifier(acteur, r.getId(), new BigDecimal("150"), "Cerise");
        assertEquals(0, new BigDecimal("50").compareTo(service.stockDisponible(acteur)));
    }

    @Test
    void varietesProposeesVientDesRecoltesDeLaFerme() {
        ResponsableFerme acteur = responsable(preparerFerme());
        service.enregistrer(acteur, BigDecimal.TEN, "Sungold");
        service.enregistrer(acteur, BigDecimal.TEN, "Cerise rouge");
        assertTrue(service.varietes(acteur).containsAll(List.of("Sungold", "Cerise rouge")));
    }

    // ---------- outils ----------

    private ResponsableFerme responsable(int ferme) {
        return new ResponsableFerme(1, "Resp", "resp", "hash", true, ferme);
    }

    /** Crée une ferme vide, ignorant le test si la base n'est pas disponible. */
    private int preparerFerme() {
        assumeTrue(TestSupport.baseDisponible(), "PostgreSQL non configuré : test ignoré");
        int id = TestSupport.premierId("INSERT INTO ferme (nom, localisation) VALUES ('Ferme test recolte "
                + System.nanoTime() + "', 'Agadir') RETURNING id");
        fermes.add(id);
        return id;
    }

    /** Crée une livraison de {@code kg} kg prélevés sur la ferme. */
    private void livrer(int ferme, String kg) {
        long suffixe = System.nanoTime() % 1_000_000_000L;
        int admin = TestSupport.premierId("INSERT INTO utilisateur (nom_complet, login, mot_de_passe_hash, role) "
                + "VALUES ('Admin test', 'adm" + suffixe + "', 'hash', 'ADMIN') RETURNING id");
        admins.add(admin);
        Client client = new Client("R" + suffixe, "Client test", TypeClient.STATION_CONDITIONNEMENT, null, "Agadir");
        clientDao.insert(client);
        clients.add(client.getId());

        Vente vente = new Vente();
        vente.setIdClient(client.getId());
        vente.setIdAdmin(admin);
        vente.setDateLivraison(LocalDate.now());
        vente.ajouterLigne(new LigneVente(ferme, new BigDecimal(kg), new BigDecimal("12.50")));
        venteDao.insertAvecLignes(vente);
        ventes.add(vente.getId());
    }
}