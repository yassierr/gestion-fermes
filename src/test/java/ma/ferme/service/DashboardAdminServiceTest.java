package ma.ferme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import ma.ferme.dao.DaoException;
import ma.ferme.dao.DepenseDao;
import ma.ferme.dao.FermeDao;
import ma.ferme.dao.OuvrierDao;
import ma.ferme.dao.RecolteDao;
import ma.ferme.dao.SerreDao;
import ma.ferme.dao.UtilisateurDao;
import ma.ferme.dao.VenteDao;
import ma.ferme.model.Depense;
import ma.ferme.model.Ferme;
import ma.ferme.model.Ouvrier;
import ma.ferme.model.Recolte;
import ma.ferme.model.Serre;
import ma.ferme.model.enums.CategorieDepense;

import org.junit.jupiter.api.Test;

class DashboardAdminServiceTest {

    @Test
    void calculeLesIndicateursGlobauxEtParFerme() {
        FakeVenteDao ventes = new FakeVenteDao();
        ventes.ca = Map.of(1, new BigDecimal("1200.00"), 2, new BigDecimal("300.00"));
        ventes.stock = Map.of(1, new BigDecimal("11.50"), 2, new BigDecimal("8.50"));

        FakeSerreService serres = new FakeSerreService(List.of(
                new Serre("A1", 100.0, "Rouge", "Tunnel", "Goutte", 1),
                new Serre("A2", 25.5, "Jaune", "Tunnel", "Goutte", 1),
                new Serre("B1", 50.0, "Rouge", "Canarienne", "Goutte", 2)));

        FakeRecolteDao recoltes = new FakeRecolteDao(
                List.of(new Recolte(1, new BigDecimal("7"), "Rouge"),
                        new Recolte(1, new BigDecimal("5"), "Jaune"),
                        new Recolte(2, new BigDecimal("8"), "Rouge")),
                Map.of("Rouge", new BigDecimal("15"), "Jaune", new BigDecimal("5")));

        FakeDepenseDao depenses = new FakeDepenseDao(List.of(
                new Depense(1, LocalDate.of(2026, 1, 1), CategorieDepense.AUTRE,
                        new BigDecimal("200"), "Achat"),
                new Depense(2, LocalDate.of(2026, 1, 2), CategorieDepense.AUTRE,
                        new BigDecimal("50"), "Réparation")));

        DashboardAdminService service = service(
                List.of(new Ferme(1, "Ferme A", "Agadir"), new Ferme(2, "Ferme B", "Biougra")),
                ventes,
                serres,
                recoltes,
                depenses,
                Map.of(1, new BigDecimal("75"), 2, new BigDecimal("25")),
                Map.of(1, 2, 2, 1),
                Map.of(1, "Responsable A"));

        DashboardAdminService.Donnees donnees = service.chargerDonnees();

        assertMontant("1500.00", donnees.chiffreAffaires());
        assertMontant("1150.00", donnees.benefice());
        assertMontant("100", donnees.masseSalariale());
        assertMontant("175.5", donnees.surface());
        assertMontant("20.00", donnees.stock());
        assertEquals(3, donnees.effectif());
        assertEquals(2, donnees.nbFermes());
        assertMontant("20", donnees.productionParVariete().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        DashboardAdminService.LigneFerme fermeA = donnees.lignes().get(0);
        assertEquals("Responsable A", fermeA.responsable());
        assertMontant("125.5", fermeA.surface());
        assertMontant("12", fermeA.production());
        assertMontant("11.50", fermeA.stock());
        assertMontant("200.00", fermeA.depenses());
        assertMontant("925.00", fermeA.benefice());
        assertEquals(2, fermeA.effectif());
    }

    @Test
    void retourneDesTotauxNulsEtAucunDetailQuandLaBaseEstVide() {
        DashboardAdminService.Donnees donnees = service(
                List.of(),
                new FakeVenteDao(),
                new FakeSerreService(List.of()),
                new FakeRecolteDao(List.of(), Map.of()),
                new FakeDepenseDao(List.of()),
                Map.of(),
                Map.of(),
                Map.of()).chargerDonnees();

        assertMontant("0", donnees.chiffreAffaires());
        assertMontant("0", donnees.benefice());
        assertMontant("0", donnees.masseSalariale());
        assertMontant("0", donnees.surface());
        assertMontant("0", donnees.stock());
        assertEquals(0, donnees.effectif());
        assertEquals(0, donnees.nbFermes());
        assertEquals(List.of(), donnees.lignes());
        assertEquals(Map.of(), donnees.productionParVariete());
    }

    @Test
    void propageLesErreursDaoSansLesTransformerEnValeursDeSecours() {
        FakeVenteDao ventes = new FakeVenteDao();
        ventes.exception = new DaoException("Erreur SQL attendue.", null);
        DashboardAdminService service = service(
                List.of(new Ferme(1, "Ferme A", "Agadir")),
                ventes,
                new FakeSerreService(List.of()),
                new FakeRecolteDao(List.of(), Map.of()),
                new FakeDepenseDao(List.of()),
                Map.of(),
                Map.of(),
                Map.of());

        assertThrows(DaoException.class, service::chargerDonnees);
    }

    private static DashboardAdminService service(
            List<Ferme> fermes,
            FakeVenteDao ventes,
            FakeSerreService serres,
            FakeRecolteDao recoltes,
            FakeDepenseDao depenses,
            Map<Integer, BigDecimal> paies,
            Map<Integer, Integer> effectifs,
            Map<Integer, String> responsables) {
        FermeService fermeService = new FermeService(new FermeDao()) {
            @Override
            public List<Ferme> obtenirToutesLesFermes() {
                return fermes;
            }
        };
        RecuPaieService recuPaieService = new RecuPaieService() {
            @Override
            public Map<Integer, BigDecimal> totalPayeParFerme() {
                return paies;
            }
        };
        OuvrierDao ouvriers = new OuvrierDao() {
            @Override
            public Map<Integer, Integer> compterActifsParFerme() {
                return effectifs;
            }
        };
        UtilisateurDao utilisateurs = new UtilisateurDao() {
            @Override
            public Map<Integer, String> nomsResponsablesParFerme() {
                return responsables;
            }
        };
        return new DashboardAdminService(fermeService, serres, ventes, recoltes, depenses,
                recuPaieService, ouvriers, utilisateurs);
    }

    private static void assertMontant(String attendu, BigDecimal obtenu) {
        assertEquals(0, new BigDecimal(attendu).compareTo(obtenu));
    }

    private static final class FakeVenteDao extends VenteDao {
        private Map<Integer, BigDecimal> ca = Map.of();
        private Map<Integer, BigDecimal> stock = Map.of();
        private DaoException exception;

        @Override
        public Map<Integer, BigDecimal> chiffreAffairesParFerme() {
            if (exception != null) {
                throw exception;
            }
            return ca;
        }

        @Override
        public BigDecimal stockDisponible(int idFerme) {
            return stock.getOrDefault(idFerme, BigDecimal.ZERO);
        }
    }

    private static final class FakeSerreService extends SerreService {
        private final List<Serre> serres;

        private FakeSerreService(List<Serre> serres) {
            super(new SerreDao(), new FermeDao());
            this.serres = serres;
        }

        @Override
        public List<Serre> obtenirToutesLesSerres() {
            return serres;
        }
    }

    private static final class FakeRecolteDao extends RecolteDao {
        private final List<Recolte> recoltes;
        private final Map<String, BigDecimal> production;

        private FakeRecolteDao(List<Recolte> recoltes, Map<String, BigDecimal> production) {
            this.recoltes = recoltes;
            this.production = production;
        }

        @Override
        public List<Recolte> findAll() {
            return recoltes;
        }

        @Override
        public Map<String, BigDecimal> productionParVariete() {
            return production;
        }
    }

    private static final class FakeDepenseDao extends DepenseDao {
        private final List<Depense> depenses;

        private FakeDepenseDao(List<Depense> depenses) {
            this.depenses = depenses;
        }

        @Override
        public List<Depense> findAll() {
            return depenses;
        }
    }
}
