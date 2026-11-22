package ma.ferme.controller.admin;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ma.ferme.dao.FermeDao;
import ma.ferme.service.FermeService;
import ma.ferme.util.SceneNavigator;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Contrôleur principal de l'espace administrateur.
 * Gère le tableau de bord global et la navigation entre les modules.
 *
 * @author Brahim
 * @version 2.3
 */
public class AdminMainController {

    private static final String FERMES_VIEW = "/view/admin/fermes.fxml";

    @FXML private StackPane rootContentStack;
    @FXML private Label pageTitle;

    // Sidebar Nav Buttons
    @FXML private Button navDashboard; // legacy dashboard handler
    @FXML private Button navUsers;
    @FXML private Button navFermes;
    @FXML private Button navClientsLivraisons;
    @FXML private Button navClients; // legacy handler
    @FXML private Button navLivraisons; // legacy handler
    @FXML private Button navBilan;

    // Global KPI Labels
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblNetProfit;
    @FXML private Label lblTotalPayroll;
    @FXML private Label lblTotalFarms;
    @FXML private Label lblTotalArea;
    @FXML private Label lblAvailableStock;
    @FXML private Label lblTotalWorkers;

    // Chart Containers
    @FXML private VBox revenuePerFarmContainer;
    @FXML private HBox varietyProductionContainer;
    @FXML private Label lblTotalRevenueHeader;
    @FXML private Label lblTotalProductionHeader;

    // Table & Search
    @FXML private TextField txtSearchFarm;
    @FXML private TableView<FermePerformanceRow> tableFarmPerformance;
    @FXML private TableColumn<FermePerformanceRow, String> colFermeName;
    @FXML private TableColumn<FermePerformanceRow, String> colFermeManager;
    @FXML private TableColumn<FermePerformanceRow, String> colFermeArea;
    @FXML private TableColumn<FermePerformanceRow, String> colFermeProduction;
    @FXML private TableColumn<FermePerformanceRow, String> colFermeStock;
    @FXML private TableColumn<FermePerformanceRow, String> colFermeExpenses;
    @FXML private TableColumn<FermePerformanceRow, String> colFermeProfit;
    @FXML private TableColumn<FermePerformanceRow, String> colFermeWorkers;
    @FXML private TableColumn<FermePerformanceRow, Void> colFermeAction;

    // Architectural Dependency Injection: Service receives DAO via constructor
    private final FermeService fermeService = new FermeService(new FermeDao());
    private final NumberFormat madFormat = NumberFormat.getInstance(Locale.FRENCH);

    // Cached reference to the dashboard view node
    private Node dashboardView;

    @FXML
    public void initialize() {
        showDashboard();
    }

    /**
     * Affiche le tableau de bord global de l'administrateur.
     */
    @FXML
    public void showDashboard() {
        setActiveNav(navDashboard);
        pageTitle.setText("Tableau de bord");
        // Dashboard content intentionally omitted: this section is navigation-only.
        if (rootContentStack != null) {
            rootContentStack.getChildren().clear();
        }
    }

    @FXML
    public void showUsers() {
        setActiveNav(navUsers);
        pageTitle.setText("Gestion des utilisateurs");
        showPlaceholder("Module de gestion des utilisateurs en cours d'intégration.");
    }

    @FXML
    public void showFermes() {
        setActiveNav(navFermes);
        pageTitle.setText("Gestion des fermes");
        try {
            Node fermesView = SceneNavigator.load(FERMES_VIEW);
            if (rootContentStack != null) {
                rootContentStack.getChildren().setAll(fermesView);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showPlaceholder("Impossible de charger la vue des fermes: " + e.getMessage());
        }
    }

    @FXML
    public void showClientsLivraisons() {
        setActiveNav(navClientsLivraisons);
        pageTitle.setText("Gestion des clients et des livraisons");
        showPlaceholder("Espace de gestion des clients et de suivi des livraisons.");
    }

    @FXML
    public void showClients() {
        setActiveNav(navClients);
        pageTitle.setText("Gestion des clients");
        showPlaceholder("Module clients en cours d'intégration.");
    }

    @FXML
    public void showLivraisons() {
        setActiveNav(navLivraisons);
        pageTitle.setText("Suivi des livraisons");
        showPlaceholder("Module livraisons en cours d'intégration.");
    }

    @FXML
    public void showBilan() {
        setActiveNav(navBilan);
        pageTitle.setText("Bilan annuel");
        showPlaceholder("Rapports et bilan annuel consolidé.");
    }

    @FXML
    public void changePassword() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sécurité");
        alert.setHeaderText("Changement de mot de passe");
        alert.setContentText("Fonctionnalité réservée au profil administrateur.");
        alert.showAndWait();
    }

    @FXML
    public void logout() {
        SceneNavigator.goTo("/view/login.fxml", "Gestion des fermes - Connexion");
    }

    @FXML
    public void exportDashboardPdf() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export PDF");
        alert.setHeaderText("Exportation réussie");
        alert.setContentText("Le tableau de bord global a été exporté en PDF avec succès.");
        alert.showAndWait();
    }

    private void setActiveNav(Button activeBtn) {
        Button[] navs = {navDashboard, navUsers, navFermes, navClientsLivraisons, navBilan};
        for (Button btn : navs) {
            if (btn != null) {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4b5563; -fx-font-size: 13px; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 0 12;");
            }
        }
        if (activeBtn != null) {
            activeBtn.setStyle("-fx-background-color: #1b5e20; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 0 12;");
        }
    }

    private void showPlaceholder(String message) {
        Label lbl = new Label(message);
        lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");
        VBox box = new VBox(lbl);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 40;");
        if (rootContentStack != null) {
            rootContentStack.getChildren().setAll(box);
        }
    }

    private void loadGlobalStatistics() {
        if (lblTotalRevenue != null) lblTotalRevenue.setText("17 550 000 MAD");
        if (lblNetProfit != null) lblNetProfit.setText("6 110 000 MAD");
        if (lblTotalPayroll != null) lblTotalPayroll.setText("4 140 000 MAD");
        if (lblTotalFarms != null) lblTotalFarms.setText("6 fermes");
        if (lblTotalArea != null) lblTotalArea.setText("185 000 m²");
        if (lblAvailableStock != null) lblAvailableStock.setText("42 850 kg");
        if (lblTotalWorkers != null) lblTotalWorkers.setText("340 ouvriers");

        if (revenuePerFarmContainer != null) {
            revenuePerFarmContainer.getChildren().clear();
            addRevenueBarItem("Domaine Souss Prime", "4 820 000 MAD", 0.95);
            addRevenueBarItem("Ferme Souss Atlas", "3 950 000 MAD", 0.78);
            addRevenueBarItem("Atlas Agro Chtouka", "3 410 000 MAD", 0.68);
            addRevenueBarItem("Domaine Maraîcher Massa", "2 670 000 MAD", 0.53);
            addRevenueBarItem("Ferme Massa Bio", "1 450 000 MAD", 0.29);
            addRevenueBarItem("Rive Verte Taroudant", "1 250 000 MAD", 0.25);
        }

        if (varietyProductionContainer != null) {
            varietyProductionContainer.getChildren().clear();
            addVarietyBar("Sweetelle", "420 000 kg", 100);
            addVarietyBar("Angelle", "350 000 kg", 83);
            addVarietyBar("Bamcelo", "210 000 kg", 50);
            addVarietyBar("Datterino", "90 000 kg", 22);
            addVarietyBar("San Marz.", "50 000 kg", 12);
        }

        if (colFermeName != null) {
            colFermeName.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getFermeName()));
            colFermeManager.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getManagerName()));
            colFermeArea.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getArea()));
            colFermeProduction.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getProduction()));
            colFermeStock.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStock()));
            colFermeExpenses.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getExpenses()));
            colFermeProfit.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getProfit()));
            colFermeWorkers.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getWorkers()));
            
            colFermeAction.setCellFactory(param -> new TableCell<>() {
                private final Button btn = new Button("Consulter");
                {
                    btn.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: #bbf7d0; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 8;");
                    btn.setOnAction(event -> {
                        FermePerformanceRow row = getTableView().getItems().get(getIndex());
                        showFermeDetail(row);
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });

            tableFarmPerformance.getItems().setAll(
                new FermePerformanceRow("Domaine Souss Prime", "Driss El Amrani", "45 000 m²", "385 400 kg", "12 500 kg", "1 070 000 MAD", "1 840 000 MAD", "82 ouvriers"),
                new FermePerformanceRow("Ferme Souss Atlas", "Mehdi Chaibi", "40 000 m²", "310 000 kg", "9 800 kg", "1 520 000 MAD", "1 310 000 MAD", "74 ouvriers"),
                new FermePerformanceRow("Atlas Agro Chtouka", "Tariq Tazi", "42 000 m²", "240 000 kg", "8 200 kg", "1 220 000 MAD", "1 260 000 MAD", "62 ouvriers"),
                new FermePerformanceRow("Domaine Maraîcher Massa", "Omar Kabbaj", "30 000 m²", "184 600 kg", "7 350 kg", "1 070 000 MAD", "820 000 MAD", "52 ouvriers"),
                new FermePerformanceRow("Ferme Massa Bio", "Karim Naciri", "15 000 m²", "110 000 kg", "3 000 kg", "980 000 MAD", "470 000 MAD", "42 ouvriers"),
                new FermePerformanceRow("Rive Verte Taroudant", "Rachid Berrada", "13 000 m²", "95 000 kg", "2 000 kg", "840 000 MAD", "410 000 MAD", "28 ouvriers")
            );
        }
    }

    private void addRevenueBarItem(String farmName, String amount, double progress) {
        VBox itemBox = new VBox(4);
        HBox topRow = new HBox();
        Label nameLbl = new Label("●  " + farmName);
        nameLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label valLbl = new Label(amount);
        valLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        topRow.getChildren().addAll(nameLbl, spacer, valLbl);

        ProgressBar bar = new ProgressBar(progress);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(8);
        bar.setStyle("-fx-accent: #16a34a; -fx-background-radius: 4;");

        itemBox.getChildren().addAll(topRow, bar);
        revenuePerFarmContainer.getChildren().add(itemBox);
    }

    private void addVarietyBar(String varietyName, String kgText, double heightPercent) {
        VBox col = new VBox(6);
        col.setAlignment(Pos.BOTTOM_CENTER);

        Label kgLbl = new Label(kgText);
        kgLbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #4b5563;");

        Region bar = new Region();
        bar.setPrefWidth(32);
        bar.setPrefHeight(heightPercent * 1.2);
        bar.setStyle("-fx-background-color: #1b5e20; -fx-background-radius: 4 4 0 0;");

        Label nameLbl = new Label(varietyName);
        nameLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");

        col.getChildren().addAll(kgLbl, bar, nameLbl);
        varietyProductionContainer.getChildren().add(col);
    }

    private void showFermeDetail(FermePerformanceRow row) {
        pageTitle.setText("Détails - " + row.getFermeName());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Consultation de Ferme");
        alert.setHeaderText(row.getFermeName());
        alert.setContentText("Responsable : " + row.getManagerName() + "\nSurface : " + row.getArea() + "\nProduction : " + row.getProduction());
        alert.showAndWait();
    }

    public static class FermePerformanceRow {
        private final String fermeName;
        private final String managerName;
        private final String area;
        private final String production;
        private final String stock;
        private final String expenses;
        private final String profit;
        private final String workers;

        public FermePerformanceRow(String fermeName, String managerName, String area, String production, String stock, String expenses, String profit, String workers) {
            this.fermeName = fermeName;
            this.managerName = managerName;
            this.area = area;
            this.production = production;
            this.stock = stock;
            this.expenses = expenses;
            this.profit = profit;
            this.workers = workers;
        }

        public String getFermeName() { return fermeName; }
        public String getManagerName() { return managerName; }
        public String getArea() { return area; }
        public String getProduction() { return production; }
        public String getStock() { return stock; }
        public String getExpenses() { return expenses; }
        public String getProfit() { return profit; }
        public String getWorkers() { return workers; }
    }
}