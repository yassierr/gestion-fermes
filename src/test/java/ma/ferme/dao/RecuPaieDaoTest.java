package ma.ferme.dao;

import ma.ferme.TestSupport;
import ma.ferme.model.LignePaie;
import ma.ferme.model.RecuPaie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RecuPaieDaoTest {

    private RecuPaieDao dao;
    private String suffixe;
    private int idFerme;
    private int idAgent;
    private int idOuvrier;

    @BeforeEach
    void setUp() {
        assumeTrue(TestSupport.baseDisponible(), "Base PostgreSQL indisponible : test ignoré");
        dao = new RecuPaieDao();
        suffixe = String.valueOf(System.nanoTime() % 1_000_000_000L);

        idFerme = TestSupport.premierId("INSERT INTO ferme(nom, localisation) VALUES ('Ferme RecuDao " + suffixe + "', 'Biougra') RETURNING id");
        idAgent = TestSupport.premierId("INSERT INTO utilisateur(nom_complet, login, mot_de_passe_hash, role, id_ferme) "
                + "VALUES ('RH Recu', 'rh_recu_" + suffixe + "', 'hash', 'AGENT_RH', " + idFerme + ") RETURNING id");
        idOuvrier = TestSupport.premierId("INSERT INTO ouvrier(matricule, cin, nom_prenom, salaire_journalier, id_ferme) "
                + "VALUES ('MR" + suffixe + "', 'CR" + suffixe + "', 'Ouvrier Recu', 150, " + idFerme + ") RETURNING id");
    }

    @AfterEach
    void tearDown() {
        if (suffixe == null) return;
        TestSupport.executer("DELETE FROM recu_paie WHERE id_ferme = " + idFerme);
        TestSupport.executer("DELETE FROM ouvrier WHERE id = " + idOuvrier);
        TestSupport.executer("DELETE FROM utilisateur WHERE id = " + idAgent);
        TestSupport.executer("DELETE FROM ferme WHERE id = " + idFerme);
    }

    @Test
    @DisplayName("Devrait insérer un reçu avec ses lignes et le retrouver par ID et par ferme")
    void testCrudRecuEtLignes() {
        RecuPaie recu = new RecuPaie(idAgent, LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15));
        recu.ajouterLigne(new LignePaie(idOuvrier, 5.0, new BigDecimal("750.00")));

        dao.insert(recu);
        assertTrue(recu.getId() > 0);

        Optional<RecuPaie> trouve = dao.findById(recu.getId());
        assertTrue(trouve.isPresent());
        assertEquals(0, new BigDecimal("750.00").compareTo(trouve.get().getTotal()));
        assertEquals(1, trouve.get().getLignes().size());
        assertEquals(5.0, trouve.get().getLignes().get(0).getJoursPayes());

        List<RecuPaie> parFerme = dao.findByFerme(idFerme);
        assertEquals(1, parFerme.size());

        dao.delete(recu.getId());
        assertTrue(dao.findById(recu.getId()).isEmpty());
    }
}