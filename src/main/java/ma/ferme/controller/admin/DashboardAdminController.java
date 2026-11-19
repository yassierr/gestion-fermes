package ma.ferme.controller.admin;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import ma.ferme.dao.DaoException;
import ma.ferme.dao.FermeDao;
import ma.ferme.model.Ferme;
import ma.ferme.security.Session;
import ma.ferme.service.FermeService;
import ma.ferme.util.AlertUtil;

/**
 * Contrôleur du tableau de bord global ({@code /view/admin/dashboard-admin.fxml}), EF-A03 à EF-A06.
 * <p>
 * L'affichage est complet : sept cartes, graphique du chiffre d'affaires par ferme, graphique de
 * production par variété, tableau « Performances et rentabilité par ferme » avec ligne de total,
 * recherche et pagination. Une valeur absente ({@code null}) s'affiche « — », jamais « 0 ».
 * </p>
 * <p>
 * Données : pour l'instant seules les fermes viennent d'un service ({@link FermeService}). Le reste
 * est à brancher dans {@link #chargerDonnees()} quand les services correspondants seront livrés
 * (chiffre d'affaires et stock : ventes ; surface : serres ; production : récoltes ; dépenses ;
 * effectif : ouvriers ; masse salariale : reçus de paie ; responsable : comptes). Le bénéfice se calcule
 * avec RG-19 : chiffre d'affaires − dépenses − reçus de paie.
 * </p>
 */
public class DashboardAdminController {

    private static final Logger LOG = Logger.getLogger(DashboardAdminController.class.getName());

    private static final int TAILLE_PAGE = 10;
    private static final String VIDE = "—";

    /**
     * Une ligne du tableau de performances.
     *
     * @param idFerme         identifiant de la ferme (0 pour la ligne de total)
     * @param ferme           nom de la ferme
     * @param responsable     nom du responsable ({@code null} si inconnu)
     * @param surface         surface totale en m²
     * @param production      production totale en kg
     * @param stock           stock disponible en kg
     * @param depenses        dépenses en MAD
     * @param benefice        bénéfice en MAD
     * @param chiffreAffaires chiffre d'affaires en MAD
     * @param effectif        nombre d'ouvriers
     * @param total           {@code true} pour la ligne de total
     */
    record LigneFerme(int idFerme, String ferme, String responsable, BigDecimal surface, BigDecimal production,
                      BigDecimal stock, BigDecimal depenses, BigDecimal benefice, BigDecimal chiffreAffaires,
                      Integer effectif, boolean total) {
    }

    /**
     * Toutes les données affichées par le tableau de bord.
     *
     * @param chiffreAffaires    chiffre d'affaires global
     * @param benefice           bénéfice net global
     * @param masseSalariale     masse salariale globale
     * @param nbFermes           nombre de fermes
     * @param surface            surface totale
     * @param stock              stock disponible total
     * @param effectif           effectif total des ouvriers
     * @param lignes             une ligne par ferme
     * @param productionParVariete production en kg par variété
     */
    record Donnees(BigDecimal chiffreAffaires, BigDecimal benefice, BigDecimal masseSalariale, int nbFermes,
                   BigDecimal surface, BigDecimal stock, Integer effectif, List<LigneFerme> lignes,
                   Map<String, BigDecimal> productionParVariete) {
    }

    @FXML private Label lblChiffreAffaires;
    @FXML private Label lblBenefice;
    @FXML private Label lblMasseSalariale;
    @FXML private Label lblNbFermes;
    @FXML private Label lblSurface;
    @FXML private Label lblStock;
    @FXML private Label lblEffectif;
    @FXML private Label lblTotalCa;
    @FXML private VBox boxCaParFerme;
    @FXML private Label lblTotalProduction;
    @FXML private BarChart<String, Number> chartProduction;
    @FXML private TextField txtRecherche;
    @FXML private TableView<LigneFerme> table;
    @FXML private TableColumn<LigneFerme, String> colFerme;
    @FXML private TableColumn<LigneFerme, String> colResponsable;
    @FXML private TableColumn<LigneFerme, String> colSurface;
    @FXML private TableColumn<LigneFerme, String> colProduction;
    @FXML private TableColumn<LigneFerme, String> colStock;
    @FXML private TableColumn<LigneFerme, String> colDepenses;
    @FXML private TableColumn<LigneFerme, String> colBenefice;
    @FXML private TableColumn<LigneFerme, String> colEffectif;
    @FXML private TableColumn<LigneFerme, LigneFerme> colAction;
    @FXML private Label lblPagination;
    @FXML private Button btnPrecedent;
    @FXML private Label lblPage;
    @FXML private Button btnSuivant;

    private final FermeService fermeService;
    private final PauseTransition attenteRecherche = new PauseTransition(Duration.millis(250));

    private Donnees donnees;
    private List<LigneFerme> lignesFiltrees = List.of();
    private int page;

    /** Constructeur sans argument, requis par FXMLLoader. */
    public DashboardAdminController() {
        // FermeService n'a pas encore de constructeur par défaut (contrat du projet) : à simplifier ensuite.
        this(new FermeService(new FermeDao()));
    }

    /**
     * Constructeur avec service injecté (utile pour les tests).
     *
     * @param fermeService le service des fermes
     */
    public DashboardAdminController(FermeService fermeService) {
        this.fermeService = fermeService;
    }

    /** Appelée par JavaFX après le chargement du FXML. */
    @FXML
    private void initialize() {
        configurerTable();
        attenteRecherche.setOnFinished(e -> {
            page = 0;
            filtrer();
        });
        txtRecherche.textProperty().addListener((obs, ancien, nouveau) -> attenteRecherche.playFromStart());

        if (!Session.estConnecte() || !"ADMIN".equals(Session.getRole())) {
            table.setPlaceholder(new Label("Accès réservé à l'administrateur."));
            txtRecherche.setDisable(true);
            return;
        }
        try {
            donnees = chargerDonnees();
            afficherCartes();
            afficherChiffreAffairesParFerme();
            afficherProduction();
            page = 0;
            filtrer();
        } catch (DaoException e) {
            LOG.log(Level.SEVERE, "Erreur d'accès à la base", e);
            Platform.runLater(() -> AlertUtil.error("Erreur d'accès à la base de données : " + e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    /** Clic sur « Exporter le tableau de bord (PDF) » (EF-A06). */
    @FXML
    private void onExporterPdf() {
        AlertUtil.info("L'export PDF sera disponible quand PdfUtil sera livré (itération 3).");
    }

    @FXML
    private void onPagePrecedente() {
        if (page > 0) {
            page--;
            afficherTable();
        }
    }

    @FXML
    private void onPageSuivante() {
        if ((page + 1) * TAILLE_PAGE < lignesFiltrees.size()) {
            page++;
            afficherTable();
        }
    }

    /** Clic sur « Consulter » : doit ouvrir la fiche détaillée de la ferme (écran de Brahim). */
    private void onConsulter(LigneFerme ligne) {
        AlertUtil.info("Fiche détaillée de « " + ligne.ferme()
                + " » : à relier à /view/admin/ferme-detail.fxml.");
    }

    // ------------------------------------------------------------------
    // Données
    // ------------------------------------------------------------------

    /**
     * Rassemble les données du tableau de bord.
     * <p>
     * Pour l'instant : la liste et le nombre de fermes. Chaque {@code null} ci-dessous est à remplacer
     * par la valeur du service concerné ; l'affichage et les totaux s'adaptent seuls.
     * </p>
     */
    private Donnees chargerDonnees() {
        List<Ferme> fermes = fermeService.obtenirToutesLesFermes();
        List<LigneFerme> lignes = new ArrayList<>();
        for (Ferme ferme : fermes) {
            lignes.add(new LigneFerme(ferme.getId(), ferme.getNom(),
                    null,   // responsable : comptes
                    null,   // surface : serres
                    null,   // production : récoltes
                    null,   // stock disponible : récoltes − lignes de vente non annulées (RG-01)
                    null,   // dépenses : dépenses
                    null,   // bénéfice : RG-19
                    null,   // chiffre d'affaires : lignes de vente livrées (RG-08)
                    null,   // effectif : ouvriers actifs
                    false));
        }
        return new Donnees(
                somme(lignes, LigneFerme::chiffreAffaires),
                somme(lignes, LigneFerme::benefice),
                null,       // masse salariale : reçus de paie (RG-18)
                fermes.size(),
                somme(lignes, LigneFerme::surface),
                somme(lignes, LigneFerme::stock),
                sommeEntiers(lignes, LigneFerme::effectif),
                lignes,
                new LinkedHashMap<>()); // production par variété : récoltes
    }

    // ------------------------------------------------------------------
    // Affichage
    // ------------------------------------------------------------------

    private void afficherCartes() {
        lblChiffreAffaires.setText(montant(donnees.chiffreAffaires()));
        lblBenefice.setText(montant(donnees.benefice()));
        lblMasseSalariale.setText(montant(donnees.masseSalariale()));
        lblNbFermes.setText(donnees.nbFermes() + (donnees.nbFermes() > 1 ? " fermes" : " ferme"));
        lblSurface.setText(quantite(donnees.surface(), "m²"));
        lblStock.setText(quantite(donnees.stock(), "kg"));
        lblEffectif.setText(donnees.effectif() == null ? VIDE
                : donnees.effectif() + (donnees.effectif() > 1 ? " ouvriers" : " ouvrier"));
    }

    private void afficherChiffreAffairesParFerme() {
        boxCaParFerme.getChildren().clear();
        lblTotalCa.setText("Total : " + montant(donnees.chiffreAffaires()));

        List<LigneFerme> avecCa = donnees.lignes().stream()
                .filter(l -> l.chiffreAffaires() != null)
                .sorted((a, b) -> b.chiffreAffaires().compareTo(a.chiffreAffaires()))
                .toList();
        if (avecCa.isEmpty()) {
            boxCaParFerme.getChildren().add(vide("Chiffre d'affaires non disponible pour le moment."));
            return;
        }
        BigDecimal max = avecCa.get(0).chiffreAffaires();
        for (int i = 0; i < avecCa.size(); i++) {
            LigneFerme ligne = avecCa.get(i);
            Label nom = new Label(ligne.ferme());
            nom.getStyleClass().add("dash-bar-name");
            Region espace = new Region();
            HBox.setHgrow(espace, Priority.ALWAYS);
            Label valeur = new Label(montant(ligne.chiffreAffaires()));
            valeur.getStyleClass().add("dash-bar-amount");

            ProgressBar barre = new ProgressBar(max.signum() == 0 ? 0
                    : ligne.chiffreAffaires().divide(max, 4, java.math.RoundingMode.HALF_UP).doubleValue());
            barre.setMaxWidth(Double.MAX_VALUE);
            barre.getStyleClass().addAll("dash-bar", classeCouleur(i));

            boxCaParFerme.getChildren().add(new VBox(5, new HBox(nom, espace, valeur), barre));
        }
    }

    private void afficherProduction() {
        chartProduction.getData().clear();
        Map<String, BigDecimal> production = donnees.productionParVariete();
        BigDecimal total = production.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalProduction.setText(production.isEmpty() ? "Total : " + VIDE : "Total : " + quantite(total, "kg"));

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        int rang = 0;
        for (Map.Entry<String, BigDecimal> entree : production.entrySet()) {
            XYChart.Data<String, Number> point = new XYChart.Data<>(entree.getKey(), entree.getValue());
            String couleur = couleurBarre(rang++);
            point.nodeProperty().addListener((obs, ancien, noeud) -> colorier(noeud, couleur));
            serie.getData().add(point);
        }
        chartProduction.getData().add(serie);
        serie.getData().forEach(p -> colorier(p.getNode(), couleurBarre(serie.getData().indexOf(p))));
    }

    private void filtrer() {
        String terme = txtRecherche.getText() == null ? "" : txtRecherche.getText().trim().toLowerCase();
        lignesFiltrees = donnees.lignes().stream()
                .filter(l -> terme.isEmpty()
                        || l.ferme().toLowerCase().contains(terme)
                        || (l.responsable() != null && l.responsable().toLowerCase().contains(terme)))
                .toList();
        afficherTable();
    }

    private void afficherTable() {
        int total = lignesFiltrees.size();
        int nbPages = Math.max(1, (total + TAILLE_PAGE - 1) / TAILLE_PAGE);
        page = Math.min(page, nbPages - 1);
        int debut = page * TAILLE_PAGE;
        int fin = Math.min(debut + TAILLE_PAGE, total);

        List<LigneFerme> affichees = new ArrayList<>(lignesFiltrees.subList(debut, fin));
        if (total > 0) {
            affichees.add(ligneTotal(lignesFiltrees));
        }
        // setAll (et non setItems) : la hauteur de la table est liée à cette liste précise.
        table.getItems().setAll(affichees);

        lblPagination.setText(total == 0
                ? "Aucune ferme"
                : "Affichage de " + (debut + 1) + " à " + fin + " sur " + total + (total > 1 ? " fermes" : " ferme"));
        lblPage.setText("Page " + (page + 1) + " / " + nbPages);
        btnPrecedent.setDisable(page == 0);
        btnSuivant.setDisable(page >= nbPages - 1);
    }

    private LigneFerme ligneTotal(List<LigneFerme> lignes) {
        return new LigneFerme(0, "TOTAL : " + lignes.size() + (lignes.size() > 1 ? " FERMES" : " FERME"), "-",
                somme(lignes, LigneFerme::surface), somme(lignes, LigneFerme::production),
                somme(lignes, LigneFerme::stock), somme(lignes, LigneFerme::depenses),
                somme(lignes, LigneFerme::benefice), somme(lignes, LigneFerme::chiffreAffaires),
                sommeEntiers(lignes, LigneFerme::effectif), true);
    }

    private void configurerTable() {
        table.setFixedCellSize(52);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        // La table prend exactement la hauteur de ses lignes (pas de défilement interne).
        table.prefHeightProperty().bind(table.fixedCellSizeProperty()
                .multiply(Bindings.size(table.getItems()).add(1.15)));
        table.setPlaceholder(new Label("Aucune ferme ne correspond à la recherche."));

        colFerme.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().ferme()));
        colResponsable.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().responsable() == null ? VIDE : cd.getValue().responsable()));
        colSurface.setCellValueFactory(cd -> new ReadOnlyStringWrapper(quantite(cd.getValue().surface(), "m²")));
        colProduction.setCellValueFactory(cd -> new ReadOnlyStringWrapper(quantite(cd.getValue().production(), "kg")));
        colStock.setCellValueFactory(cd -> new ReadOnlyStringWrapper(quantite(cd.getValue().stock(), "kg")));
        colDepenses.setCellValueFactory(cd -> new ReadOnlyStringWrapper(montant(cd.getValue().depenses())));
        colBenefice.setCellValueFactory(cd -> new ReadOnlyStringWrapper(montant(cd.getValue().benefice())));
        colEffectif.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().effectif() == null ? VIDE
                : cd.getValue().effectif() + (cd.getValue().effectif() > 1 ? " ouvriers" : " ouvrier")));
        colAction.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button bouton = new Button("Consulter");

            {
                bouton.getStyleClass().add("dash-btn-action");
                bouton.setOnAction(e -> onConsulter(getItem()));
            }

            @Override
            protected void updateItem(LigneFerme ligne, boolean empty) {
                super.updateItem(ligne, empty);
                setGraphic(empty || ligne == null || ligne.total() ? null : bouton);
            }
        });

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(LigneFerme ligne, boolean empty) {
                super.updateItem(ligne, empty);
                getStyleClass().remove("dash-total-row");
                if (!empty && ligne != null && ligne.total()) {
                    getStyleClass().add("dash-total-row");
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    /** Somme des valeurs présentes ; {@code null} si aucune ferme n'a de valeur (on n'affiche jamais un faux 0). */
    private static BigDecimal somme(List<LigneFerme> lignes, Function<LigneFerme, BigDecimal> valeur) {
        BigDecimal somme = null;
        for (LigneFerme ligne : lignes) {
            BigDecimal v = valeur.apply(ligne);
            if (v != null) {
                somme = (somme == null) ? v : somme.add(v);
            }
        }
        return somme;
    }

    private static Integer sommeEntiers(List<LigneFerme> lignes, Function<LigneFerme, Integer> valeur) {
        Integer somme = null;
        for (LigneFerme ligne : lignes) {
            Integer v = valeur.apply(ligne);
            if (v != null) {
                somme = (somme == null) ? v : somme + v;
            }
        }
        return somme;
    }

    private static String montant(BigDecimal valeur) {
        return valeur == null ? VIDE : nombre(valeur) + " MAD";
    }

    private static String quantite(BigDecimal valeur, String unite) {
        return valeur == null ? VIDE : nombre(valeur) + " " + unite;
    }

    /** Nombre avec espaces comme séparateurs de milliers (ex. 17 550 000). */
    private static String nombre(BigDecimal valeur) {
        return NumberFormat.getIntegerInstance(Locale.FRANCE).format(valeur)
                .replace('\u202f', ' ').replace('\u00a0', ' ');
    }

    private static Label vide(String message) {
        Label etiquette = new Label(message);
        etiquette.getStyleClass().add("dash-empty");
        return etiquette;
    }

    /** Vert pour les deux premiers, gris foncé pour les deux suivants, gris clair ensuite (comme la maquette). */
    private static String classeCouleur(int rang) {
        return rang < 2 ? "dash-bar-green" : rang < 4 ? "dash-bar-dark" : "dash-bar-gray";
    }

    private static String couleurBarre(int rang) {
        return rang < 2 ? "#166534" : rang < 4 ? "#374151" : "#9CA3AF";
    }

    private static void colorier(Node noeud, String couleur) {
        if (noeud != null) {
            noeud.setStyle("-fx-bar-fill: " + couleur + ";");
        }
    }
}
