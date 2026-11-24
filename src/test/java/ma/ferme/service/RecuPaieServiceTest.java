package ma.ferme.service;

import ma.ferme.TestSupport;
import ma.ferme.dao.RecuPaieDao;
import ma.ferme.model.LignePaie;
import ma.ferme.model.RecuPaie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RecuPaieServiceTest {

    private RecuPaieService service;
    private RecuPaieDao dao;
    private String suffixe;
    private int idFerme;
    private int idAgent;
    private int idOuvrier;

    @BeforeEach
    void setUp() {
        dao = new RecuPaieDao();
        service = new RecuPaieService(dao);

        if (!TestSupport.baseDisponible()) return;
        suffixe = String.valueOf(System.nanoTime() % 1_000_000_000L);
        idFerme = TestSupport.premierId("INSERT INTO ferme(nom, localisation) VALUES ('Ferme RecuSrv " + suffixe + "', 'Biougra') RETURNING id");
        idAgent = TestSupport.premierId("INSERT INTO utilisateur(nom_complet, login, mot_de_passe_hash, role, id_ferme) "
                + "VALUES ('RH Srv', 'rh_srv_" + suffixe + "', 'hash', 'AGENT_RH', " + idFerme + ") RETURNING id");
        idOuvrier = TestSupport.premierId("INSERT INTO ouvrier(matricule, cin, nom_prenom, salaire_journalier, id_ferme) "
                + "VALUES ('MS" + suffixe + "', 'CS" + suffixe + "', 'Ouvrier Srv', 150, " + idFerme + ") RETURNING id");
    }

    @AfterEach
    void tearDown() {
        if (suffixe == null) return;
        TestSupport.executer("DELETE FROM ligne_paie WHERE id_recu IN (SELECT id FROM recu_paie WHERE id_ferme = " + idFerme + ")");
        TestSupport.executer("DELETE FROM recu_paie WHERE id_ferme = " + idFerme);
        TestSupport.executer("DELETE FROM ouvrier WHERE id = " + idOuvrier);
        TestSupport.executer("DELETE FROM utilisateur WHERE id = " + idAgent);
        TestSupport.executer("DELETE FROM ferme WHERE id = " + idFerme);
    }

    @Test
    @DisplayName("Devrait refuser des paramètres invalides sans base de données")
    void testValidationsSansBase() {
        assertThrows(IllegalArgumentException.class, () -> service.trouverParId(0));
        assertThrows(IllegalArgumentException.class, () -> service.listerParFerme(-1));
        assertThrows(IllegalArgumentException.class, () -> service.listerParAgentRh(0));
        assertThrows(IllegalArgumentException.class, () ->
                service.totalPayeParFerme(1, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)));
    }

    @Test
    @DisplayName("Devrait calculer la masse salariale totale payée sur une période")
    void testTotalPayeParFerme() {
        assumeTrue(suffixe != null, "Base PostgreSQL indisponible : test ignoré");

        RecuPaie r1 = new RecuPaie(idAgent, LocalDate.of(2026, 11, 5),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5));
        r1.ajouterLigne(new LignePaie(idOuvrier, 4.0, new BigDecimal("600.00")));
        dao.insert(r1);

        RecuPaie r2 = new RecuPaie(idAgent, LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 20));
        r2.ajouterLigne(new LignePaie(idOuvrier, 6.0, new BigDecimal("900.00")));
        dao.insert(r2);

        BigDecimal total = service.totalPayeParFerme(idFerme,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertEquals(0, new BigDecimal("1500.00").compareTo(total));
        assertEquals(2, service.listerParAgentRh(idAgent).size());
    }
}