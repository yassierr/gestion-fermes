package ma.ferme.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ma.ferme.dao.DepenseDao;
import ma.ferme.dao.FermeDao;
import ma.ferme.dao.OuvrierDao;
import ma.ferme.dao.RecolteDao;
import ma.ferme.dao.SerreDao;
import ma.ferme.dao.UtilisateurDao;
import ma.ferme.dao.VenteDao;
import ma.ferme.model.Depense;
import ma.ferme.model.Ferme;
import ma.ferme.model.Recolte;
import ma.ferme.model.Serre;

/** Agrège les indicateurs globaux et par ferme du tableau de bord administrateur. */
public class DashboardAdminService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    public record LigneFerme(int idFerme, String ferme, String responsable, BigDecimal surface,
                             BigDecimal production, BigDecimal stock, BigDecimal depenses,
                             BigDecimal benefice, BigDecimal chiffreAffaires, Integer effectif,
                             boolean total) {
    }

    public record Donnees(BigDecimal chiffreAffaires, BigDecimal benefice, BigDecimal masseSalariale,
                          int nbFermes, BigDecimal surface, BigDecimal stock, Integer effectif,
                          List<LigneFerme> lignes, Map<String, BigDecimal> productionParVariete) {
        public Donnees {
            lignes = List.copyOf(lignes);
            productionParVariete = Collections.unmodifiableMap(new LinkedHashMap<>(productionParVariete));
        }
    }

    private final FermeService fermeService;
    private final SerreService serreService;
    private final VenteDao venteDao;
    private final RecolteDao recolteDao;
    private final DepenseDao depenseDao;
    private final RecuPaieService recuPaieService;
    private final OuvrierDao ouvrierDao;
    private final UtilisateurDao utilisateurDao;

    public DashboardAdminService() {
        this(
                new FermeService(new FermeDao()),
                new SerreService(new SerreDao(), new FermeDao()),
                new VenteDao(),
                new RecolteDao(),
                new DepenseDao(),
                new RecuPaieService(),
                new OuvrierDao(),
                new UtilisateurDao());
    }

    public DashboardAdminService(FermeService fermeService, SerreService serreService, VenteDao venteDao,
                                 RecolteDao recolteDao, DepenseDao depenseDao, RecuPaieService recuPaieService,
                                 OuvrierDao ouvrierDao, UtilisateurDao utilisateurDao) {
        this.fermeService = Objects.requireNonNull(fermeService);
        this.serreService = Objects.requireNonNull(serreService);
        this.venteDao = Objects.requireNonNull(venteDao);
        this.recolteDao = Objects.requireNonNull(recolteDao);
        this.depenseDao = Objects.requireNonNull(depenseDao);
        this.recuPaieService = Objects.requireNonNull(recuPaieService);
        this.ouvrierDao = Objects.requireNonNull(ouvrierDao);
        this.utilisateurDao = Objects.requireNonNull(utilisateurDao);
    }

    /**
     * Charge tous les indicateurs sur la même période : l'ensemble de l'historique disponible.
     * Le bénéfice suit RG-19 : chiffre d'affaires moins dépenses et masse salariale.
     */
    public Donnees chargerDonnees() {
        List<Ferme> fermes = fermeService.obtenirToutesLesFermes();
        Map<Integer, BigDecimal> chiffreAffairesParFerme = venteDao.chiffreAffairesParFerme();
        Map<Integer, BigDecimal> surfaceParFerme = new LinkedHashMap<>();
        for (Serre serre : serreService.obtenirToutesLesSerres()) {
            surfaceParFerme.merge(serre.getIdFerme(), BigDecimal.valueOf(serre.getSurfaceM2()),
                    BigDecimal::add);
        }

        List<Recolte> recoltes = recolteDao.findAll();
        Map<Integer, BigDecimal> productionParFerme = new LinkedHashMap<>();
        for (Recolte recolte : recoltes) {
            productionParFerme.merge(recolte.getIdFerme(), recolte.getPoidsKg(), BigDecimal::add);
        }

        Map<Integer, BigDecimal> depensesParFerme = new LinkedHashMap<>();
        for (Depense depense : depenseDao.findAll()) {
            depensesParFerme.merge(depense.getIdFerme(), depense.getMontant(), BigDecimal::add);
        }

        Map<Integer, BigDecimal> paieParFerme = recuPaieService.totalPayeParFerme();
        Map<Integer, Integer> effectifParFerme = ouvrierDao.compterActifsParFerme();
        Map<Integer, String> responsablesParFerme = utilisateurDao.nomsResponsablesParFerme();
        Map<String, BigDecimal> productionParVariete = recolteDao.productionParVariete();

        List<LigneFerme> lignes = new ArrayList<>();
        for (Ferme ferme : fermes) {
            BigDecimal ca = valeur(chiffreAffairesParFerme, ferme.getId());
            BigDecimal depenses = valeur(depensesParFerme, ferme.getId());
            BigDecimal paie = valeur(paieParFerme, ferme.getId());
            lignes.add(new LigneFerme(
                    ferme.getId(),
                    ferme.getNom(),
                    responsablesParFerme.get(ferme.getId()),
                    valeur(surfaceParFerme, ferme.getId()),
                    valeur(productionParFerme, ferme.getId()),
                    venteDao.stockDisponible(ferme.getId()),
                    depenses,
                    ca.subtract(depenses).subtract(paie),
                    ca,
                    effectifParFerme.getOrDefault(ferme.getId(), 0),
                    false));
        }

        BigDecimal chiffreAffaires = total(chiffreAffairesParFerme);
        BigDecimal depenses = total(depensesParFerme);
        BigDecimal masseSalariale = total(paieParFerme);
        return new Donnees(
                chiffreAffaires,
                chiffreAffaires.subtract(depenses).subtract(masseSalariale),
                masseSalariale,
                fermes.size(),
                total(surfaceParFerme),
                lignes.stream().map(LigneFerme::stock).reduce(ZERO, BigDecimal::add),
                effectifParFerme.values().stream().mapToInt(Integer::intValue).sum(),
                lignes,
                productionParVariete);
    }

    /** Résout la ferme à afficher dans sa fiche détaillée. */
    public Optional<Ferme> obtenirFermeParId(int id) {
        return fermeService.obtenirFermeParId(id);
    }

    private static BigDecimal valeur(Map<Integer, BigDecimal> valeurs, int idFerme) {
        return valeurs.getOrDefault(idFerme, ZERO);
    }

    private static BigDecimal total(Map<Integer, BigDecimal> valeurs) {
        return valeurs.values().stream().reduce(ZERO, BigDecimal::add);
    }

}
