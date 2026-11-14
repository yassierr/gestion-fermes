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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.Recolte;

/** Tests CRUD de {@link RecolteDao} (nécessitent la base PostgreSQL locale). */
class RecolteDaoTest {

    private final RecolteDao dao = new RecolteDao();
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

    /** Crée une serre jetable dans la ferme (supprimée avec elle par {@link #nettoyer()}). */
    private void creerSerre(int ferme, int surfaceM2, String variete) {
        String sql = "INSERT INTO serre (nom, surface_m2, variete, id_ferme) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "S-" + System.nanoTime());
            ps.setInt(2, surfaceM2);
            ps.setString(3, variete);
            ps.setInt(4, ferme);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de créer la serre de test.", e);
        }
    }

    @Test
    void insertPuisFindById() {
        Recolte r = new Recolte(idFerme, new BigDecimal("1450.50"), "Sweet Million");
        dao.insert(r);
        assertTrue(r.getId() > 0);
        Recolte lue = dao.findById(r.getId()).orElseThrow();
        assertEquals(new BigDecimal("1450.50"), lue.getPoidsKg());
        assertEquals("Sweet Million", lue.getVariete());
        assertEquals(idFerme, lue.getIdFerme());
        assertEquals(r.getDateHeure(), lue.getDateHeure());
    }

    @Test
    void findByIdInconnuRetourneVide() {
        assertTrue(dao.findById(-1).isEmpty());
    }

    @Test
    void findByFermeEstTrieDuPlusRecentAuPlusAncien() {
        LocalDateTime t = LocalDateTime.of(2024, 10, 23, 8, 0);
        dao.insert(new Recolte(0, t, new BigDecimal("10"), "A", idFerme));
        dao.insert(new Recolte(0, t.plusDays(1), new BigDecimal("20"), "B", idFerme));
        dao.insert(new Recolte(0, t.plusDays(2), new BigDecimal("30"), "C", idFerme));
        List<Recolte> liste = dao.findByFerme(idFerme);
        assertEquals(3, liste.size());
        assertEquals("C", liste.get(0).getVariete());
        assertEquals("A", liste.get(2).getVariete());
    }

    @Test
    void findByFermeNeRetournePasLesRecoltesDUneAutreFerme() {
        int autre = creerFerme();
        dao.insert(new Recolte(idFerme, BigDecimal.TEN, "A"));
        dao.insert(new Recolte(autre, BigDecimal.TEN, "B"));
        assertEquals(1, dao.findByFerme(idFerme).size());
        assertTrue(dao.findAll().size() >= 2);
    }

    @Test
    void updateModifieLesChamps() {
        Recolte r = new Recolte(idFerme, new BigDecimal("100"), "A");
        dao.insert(r);
        r.setPoidsKg(new BigDecimal("120.25"));
        r.setVariete("Sungold");
        dao.update(r);
        Recolte lue = dao.findById(r.getId()).orElseThrow();
        assertEquals(new BigDecimal("120.25"), lue.getPoidsKg());
        assertEquals("Sungold", lue.getVariete());
    }

    @Test
    void updateDUneRecolteInexistanteEchoue() {
        Recolte fantome = new Recolte(999999999, LocalDateTime.now(), BigDecimal.ONE, "A", idFerme);
        assertThrows(DaoException.class, () -> dao.update(fantome));
    }

    @Test
    void deleteSupprimeLaRecolte() {
        Recolte r = new Recolte(idFerme, BigDecimal.TEN, "A");
        dao.insert(r);
        dao.delete(r.getId());
        assertFalse(dao.findById(r.getId()).isPresent());
        assertThrows(DaoException.class, () -> dao.delete(r.getId()));
    }

    @Test
    void totalKgFaitLaSommeDeLaFerme() {
        assertEquals(0, dao.totalKg(idFerme).signum());
        dao.insert(new Recolte(idFerme, new BigDecimal("100.10"), "A"));
        dao.insert(new Recolte(idFerme, new BigDecimal("50.15"), "B"));
        assertEquals(new BigDecimal("150.25"), dao.totalKg(idFerme));
    }

    @Test
    void fermeInexistanteViolaLaCleEtrangere() {
        Recolte r = new Recolte(999999999, BigDecimal.TEN, "A");
        assertThrows(DaoException.class, () -> dao.insert(r));
    }

    @Test
    void varietesReunitRecoltesEtSerresSansDoublon() {
        creerSerre(idFerme, 1000, "Angelle");
        creerSerre(idFerme, 1000, null);
        dao.insert(new Recolte(idFerme, BigDecimal.TEN, "Sweetelle"));
        dao.insert(new Recolte(idFerme, BigDecimal.TEN, "Sweetelle"));
        dao.insert(new Recolte(idFerme, BigDecimal.TEN, "Angelle"));
        assertEquals(List.of("Angelle", "Sweetelle"), dao.varietes(idFerme));
    }
}