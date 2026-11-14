package ma.ferme.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.Depense;
import ma.ferme.model.enums.CategorieDepense;

/** Tests CRUD de {@link DepenseDao} (nécessitent la base PostgreSQL locale). */
class DepenseDaoTest {

    private final DepenseDao dao = new DepenseDao();
    private final List<Integer> fermesCreees = new ArrayList<>();
    private int idFerme;

    @BeforeEach
    void preparer() {
        assumeTrue(baseDisponible(), "PostgreSQL non configuré : test ignoré");
        idFerme = creerFerme();
    }

    /** Supprime tout ce que le test a créé : d'abord les enfants de la ferme, puis la ferme. */
    @AfterEach
    void nettoyer() {
        for (int id : fermesCreees) {
            for (String table : new String[] {"recolte", "depense", "serre"}) {
                executer("DELETE FROM " + table + " WHERE id_ferme = ?", id);
            }
            executer("DELETE FROM ferme WHERE id = ?", id);
        }
    }

    private static boolean baseDisponible() {
        if (!DatabaseConnection.isConfigured()) {
            return false;
        }
        try (Connection c = DatabaseConnection.getConnection()) {
            return c.isValid(2);
        } catch (SQLException | RuntimeException e) {
            return false;
        }
    }

    /** Crée une ferme jetable (nom unique) et la mémorise pour la supprimer après le test. */
    private int creerFerme() {
        String sql = "INSERT INTO ferme (nom, localisation) VALUES (?, 'Test') RETURNING id";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "TestFerme-" + System.nanoTime() + "-" + fermesCreees.size());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int id = rs.getInt(1);
                fermesCreees.add(id);
                return id;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de créer la ferme de test.", e);
        }
    }

    private static void executer(String sql, int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Nettoyage impossible : " + sql, e);
        }
    }

    private Depense depense(LocalDate date, CategorieDepense cat, String montant) {
        return new Depense(idFerme, date, cat, new BigDecimal(montant), "Test");
    }

    @Test
    void insertPuisFindByIdConserveTousLesChamps() {
        Depense d = new Depense(idFerme, LocalDate.of(2024, 10, 22), CategorieDepense.EAU_ENERGIE,
                new BigDecimal("95200"), "Facture ONEE - fournisseur ONEE");
        dao.insert(d);
        Depense lue = dao.findById(d.getId()).orElseThrow();
        assertEquals(LocalDate.of(2024, 10, 22), lue.getDateDepense());
        assertEquals(CategorieDepense.EAU_ENERGIE, lue.getCategorie());
        assertEquals(new BigDecimal("95200.00"), lue.getMontant());
        assertEquals("Facture ONEE - fournisseur ONEE", lue.getDescription());
        assertEquals(idFerme, lue.getIdFerme());
    }

    @Test
    void chaqueCategorieEstAcceptee() {
        for (CategorieDepense c : CategorieDepense.values()) {
            Depense d = depense(LocalDate.now(), c, "10");
            dao.insert(d);
            assertEquals(c, dao.findById(d.getId()).orElseThrow().getCategorie());
        }
    }

    @Test
    void findByFermeEstTrieEtIsole() {
        int autre = creerFerme();
        dao.insert(depense(LocalDate.of(2024, 10, 1), CategorieDepense.PLANTS, "10"));
        dao.insert(depense(LocalDate.of(2024, 10, 20), CategorieDepense.ENGRAIS, "20"));
        dao.insert(new Depense(autre, LocalDate.now(), CategorieDepense.AUTRE, BigDecimal.TEN, ""));
        List<Depense> liste = dao.findByFerme(idFerme);
        assertEquals(2, liste.size());
        assertEquals(CategorieDepense.ENGRAIS, liste.get(0).getCategorie());
        assertTrue(dao.findAll().size() >= 3);
    }

    @Test
    void updateModifieLesChamps() {
        Depense d = depense(LocalDate.of(2024, 10, 1), CategorieDepense.PLANTS, "100");
        dao.insert(d);
        d.setCategorie(CategorieDepense.EMBALLAGE);
        d.setMontant(new BigDecimal("48600"));
        d.setDescription("5 000 caisses");
        d.setDateDepense(LocalDate.of(2024, 10, 18));
        dao.update(d);
        Depense lue = dao.findById(d.getId()).orElseThrow();
        assertEquals(CategorieDepense.EMBALLAGE, lue.getCategorie());
        assertEquals(new BigDecimal("48600.00"), lue.getMontant());
        assertEquals("5 000 caisses", lue.getDescription());
        assertEquals(LocalDate.of(2024, 10, 18), lue.getDateDepense());
    }

    @Test
    void updateEtDeleteInexistantsEchouent() {
        Depense fantome = new Depense(999999999, LocalDate.now(), CategorieDepense.AUTRE, BigDecimal.ONE, "", idFerme);
        assertThrows(DaoException.class, () -> dao.update(fantome));
        assertThrows(DaoException.class, () -> dao.delete(999999999));
    }

    @Test
    void deleteSupprimeLaDepense() {
        Depense d = depense(LocalDate.now(), CategorieDepense.AUTRE, "10");
        dao.insert(d);
        dao.delete(d.getId());
        assertFalse(dao.findById(d.getId()).isPresent());
    }

    @Test
    void totalRespecteLesBornesDeDates() {
        dao.insert(depense(LocalDate.of(2024, 9, 30), CategorieDepense.PLANTS, "100"));
        dao.insert(depense(LocalDate.of(2024, 10, 1), CategorieDepense.PLANTS, "200"));
        dao.insert(depense(LocalDate.of(2024, 10, 31), CategorieDepense.PLANTS, "300"));
        dao.insert(depense(LocalDate.of(2024, 11, 1), CategorieDepense.PLANTS, "400"));
        LocalDate debut = LocalDate.of(2024, 10, 1);
        LocalDate fin = LocalDate.of(2024, 10, 31);
        assertEquals(0, new BigDecimal("500").compareTo(dao.total(idFerme, debut, fin)));
        assertEquals(0, new BigDecimal("1000").compareTo(dao.total(idFerme, null, null)));
        assertEquals(0, new BigDecimal("900").compareTo(dao.total(idFerme, debut, null)));
        assertEquals(0, new BigDecimal("600").compareTo(dao.total(idFerme, null, fin)));
    }

    @Test
    void fermeInexistanteViolaLaCleEtrangere() {
        Depense d = new Depense(999999999, LocalDate.now(), CategorieDepense.AUTRE, BigDecimal.TEN, "");
        assertThrows(DaoException.class, () -> dao.insert(d));
    }
}