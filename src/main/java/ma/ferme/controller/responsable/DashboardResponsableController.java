package ma.ferme.controller.responsable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import ma.ferme.dao.DaoException;
import ma.ferme.dao.FermeDao;
import ma.ferme.dao.SerreDao;
import ma.ferme.model.LigneVente;
import ma.ferme.model.Recolte;
import ma.ferme.model.ResponsableFerme;
import ma.ferme.model.Serre;
import ma.ferme.model.Vente;
import ma.ferme.model.enums.StatutVente;
import ma.ferme.service.DepenseService;
import ma.ferme.service.RecolteService;
import ma.ferme.service.SerreService;
import ma.ferme.service.VenteService;
import ma.ferme.util.AlertUtil;
import ma.ferme.util.Formats;
import ma.ferme.util.SceneNavigator;

/**
 * Tableau de bord du responsable de ferme (EF-R12 à EF-R15), limité à sa ferme (EF-T04) :
 * chiffre d'affaires du mois, dépenses du mois, coût de main-d'œuvre, bénéfice (RG-15), stock
 * disponible (RG-01), rendement en kg/m² (RG-16) et courbe de production hebdomadaire.
 * <p>
 * <b>Deux points ne sont pas encore branchés :</b>
 * </p>
 * <ul>
 *   <li>le coût de main-d'œuvre : il faut lire les reçus de paie de la ferme, et
 *       {@code RecuPaieService} (it3, Jamel) n'existe pas encore. Tant que
 *       {@link #masseSalarialeDuMois(ResponsableFerme, YearMonth)} renvoie zéro, un avertissement
 *       est affiché car le bénéfice est alors trop élevé ;</li>
 *   <li>les alertes d'intrants (EF-R14) : aucune classe d'intrant n'existe dans l'architecture.</li>
 * </ul>
 * Le contrôleur passe uniquement par les services.
 *
 * @author Mohamed Amine Ait Maait
 * @version 1.0
 */
public class DashboardResponsableController {

    /** Passer à {@code true} quand {@link #masseSalarialeDuMois} lit les vrais reçus de paie. */
    private static final boolean MAIN_OEUVRE_BRANCHEE = false;

    private static final String LOGIN = "/view/login.fxml";
    private static final int NB_SEMAINES = 8;
    private static final DateTimeFormatter JOUR_MOIS = DateTimeFormatter.ofPattern("dd/MM");

    private final RecolteService recolteService = new RecolteService();
    private final DepenseService depenseService = new DepenseService();
    private final VenteService venteService = new VenteService();
    private final SerreService serreService = new SerreService(new SerreDao(), new FermeDao());

    @FXML private Label lblMois;
    @FXML private Label lblAvertissement;
    @FXML private Label lblChiffreAffaires;
    @FXML private Label lblDepenses;
    @FXML private Label lblMainOeuvre;
    @FXML private Label lblBenefice;
    @FXML private Label lblStock;
    @FXML private Label lblRendement;
    @FXML private LineChart<String, Number> chartProduction;

    /** Vérifie la session, puis calcule et affiche tous les indicateurs. */
    @FXML
    private void initialize() {
        ResponsableFerme acteur;
        try {
            acteur = ResponsableMainController.acteurConnecte();
        } catch (SecurityException e) {
            Platform.runLater(() -> SceneNavigator.goTo(LOGIN, "Gestion des fermes - Connexion"));
            return;
        }
        lblAvertissement.setText("Le coût de main-d'œuvre n'est pas encore calculé (reçus de paie non branchés) : "
                + "le bénéfice affiché ne le déduit pas.");
        lblAvertissement.setVisible(!MAIN_OEUVRE_BRANCHEE);
        lblAvertissement.setManaged(!MAIN_OEUVRE_BRANCHEE);
        chartProduction.setAnimated(false);
        chartProduction.setLegendVisible(false);
        try {
            afficher(acteur);
        } catch (DaoException e) {
            AlertUtil.error("Impossible de charger le tableau de bord : " + e.getMessage());
        }
    }

    private void afficher(ResponsableFerme acteur) {
        YearMonth mois = YearMonth.now();
        lblMois.setText(mois.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + mois.getYear());

        BigDecimal chiffreAffaires = chiffreAffairesDuMois(acteur, mois);
        BigDecimal depenses = depenseService.totalDuMois(acteur, mois);
        BigDecimal mainOeuvre = masseSalarialeDuMois(acteur, mois);
        BigDecimal benefice = chiffreAffaires.subtract(depenses).subtract(mainOeuvre);

        lblChiffreAffaires.setText(Formats.mad(chiffreAffaires));
        lblDepenses.setText(Formats.mad(depenses));
        lblMainOeuvre.setText(Formats.mad(mainOeuvre));
        lblBenefice.setText(Formats.mad(benefice));
        lblBenefice.getStyleClass().removeAll("valeur-positive", "valeur-negative");
        lblBenefice.getStyleClass().add(benefice.signum() < 0 ? "valeur-negative" : "valeur-positive");
        lblStock.setText(Formats.nombre(recolteService.stockDisponible(acteur)) + " kg");

        List<Recolte> recoltes = recolteService.lister(acteur);
        lblRendement.setText(Formats.nombre(rendement(acteur, recoltes)) + " kg/m²");
        afficherCourbe(recoltes);
    }

    /** Chiffre d'affaires : lignes de livraison de la ferme, non annulées, livrées pendant le mois (RG-15). */
    private BigDecimal chiffreAffairesDuMois(ResponsableFerme acteur, YearMonth mois) {
        BigDecimal total = BigDecimal.ZERO;
        for (Vente vente : venteService.listerParFerme(acteur.getIdFerme())) {
            if (vente.getStatut() == StatutVente.ANNULEE || !YearMonth.from(vente.getDateLivraison()).equals(mois)) {
                continue;
            }
            for (LigneVente ligne : vente.getLignes()) {
                if (ligne.getIdFerme() == acteur.getIdFerme()) {
                    total = total.add(ligne.getMontant());
                }
            }
        }
        return total;
    }

    /**
     * POINT À BRANCHER : somme des reçus de paie de la ferme payés pendant le mois (EF-R11).
     * À remplacer par un appel à {@code RecuPaieService} dès qu'il existe, puis passer
     * {@link #MAIN_OEUVRE_BRANCHEE} à {@code true}.
     *
     * @param acteur le responsable connecté
     * @param mois   le mois voulu
     * @return zéro tant que les reçus de paie ne sont pas branchés
     */
    private BigDecimal masseSalarialeDuMois(ResponsableFerme acteur, YearMonth mois) {
        return BigDecimal.ZERO;
    }

    /** RG-16 : récoltes totales (kg) divisées par la surface totale des serres de la ferme. */
    private BigDecimal rendement(ResponsableFerme acteur, List<Recolte> recoltes) {
        double surface = 0;
        for (Serre serre : serreService.obtenirToutesLesSerres()) {
            if (serre.getIdFerme() == acteur.getIdFerme()) {
                surface += serre.getSurfaceM2();
            }
        }
        if (surface <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal kg = recoltes.stream().map(Recolte::getPoidsKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        return kg.divide(BigDecimal.valueOf(surface), 2, RoundingMode.HALF_UP);
    }

    /** Courbe des kg récoltés par semaine (EF-R15) sur les {@value #NB_SEMAINES} dernières semaines. */
    private void afficherCourbe(List<Recolte> recoltes) {
        LocalDate lundiCourant = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Récoltes (kg)");
        for (int i = NB_SEMAINES - 1; i >= 0; i--) {
            LocalDate debut = lundiCourant.minusWeeks(i);
            LocalDate fin = debut.plusDays(6);
            BigDecimal kg = BigDecimal.ZERO;
            for (Recolte r : recoltes) {
                LocalDate jour = r.getDateHeure().toLocalDate();
                if (!jour.isBefore(debut) && !jour.isAfter(fin)) {
                    kg = kg.add(r.getPoidsKg());
                }
            }
            serie.getData().add(new XYChart.Data<>(debut.format(JOUR_MOIS), kg));
        }
        chartProduction.getData().setAll(List.of(serie));
    }
}