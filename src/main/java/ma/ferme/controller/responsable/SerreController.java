package ma.ferme.controller.responsable;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import ma.ferme.dao.FermeDao;
import ma.ferme.dao.SerreDao;
import ma.ferme.model.Ferme;
import ma.ferme.model.Serre;
import ma.ferme.service.FermeService;
import ma.ferme.service.SerreService;
import ma.ferme.util.SceneNavigator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Contrôleur de gestion des serres dans l'espace Responsable de ferme.
 * Gère l'affichage, les indicateurs KPI, la recherche, l'ajout, la modification et la suppression des serres.
 *
 * @author Brahim
 * @version 1.0
 */
public class SerreController {

    @FXML private Label lblFermeContext;
    @FXML private Label lblTotalSerres;
    @FXML private Label lblTotalSurface;
    @FXML private Label lblTotalVarietes;
    @FXML private Label lblTableSummary;
    @FXML private TextField txtSearch;

    @FXML private TableView<Serre> tableSerres;
    @FXML private TableColumn<Serre, String> colNom;
    @FXML private TableColumn<Serre, String> colSurface;
    @FXML private TableColumn<Serre, String> colVariete;
    @FXML private TableColumn<Serre, String> colTypeAbri;
    @FXML private TableColumn<Serre, String> colIrrigation;
    @FXML private TableColumn<Serre, Void> colActions;

    private final FermeDao fermeDao = new FermeDao();
    private final SerreService serreService = new SerreService(new SerreDao(), fermeDao);
    private final FermeService fermeService = new FermeService(fermeDao);

    private ObservableList<Serre> serreList;
    private int currentFermeId = 1; // Default to first farm

    @FXML
    public void initialize() {
        resolveCurrentFerme();
        configureTable();
        loadSerresData();
    }

    private void resolveCurrentFerme() {
        List<Ferme> fermes = fermeService.obtenirToutesLesFermes();
        if (!fermes.isEmpty()) {
            Ferme first = fermes.get(0);
            this.currentFermeId = first.getId();
            if (lblFermeContext != null) {
                lblFermeContext.setText("Ferme : " + first.getNom() + " (" + first.getLocalisation() + ")");
            }
        }
    }

    private void configureTable() {
        colNom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNom()));
        
        colSurface.setCellValueFactory(cell ->
                new SimpleStringProperty(String.format("%,.0f m²", cell.getValue().getSurfaceM2()))
        );

        // Custom cell to render green badge for variety/structure style matching mockup
        colVariete.setCellValueFactory(cell -> {
            String variete = cell.getValue().getVariete();
            String abri = cell.getValue().getTypeAbri();
            String display = (variete != null ? variete : "Sweetelle") + (abri != "null" && abri != null ? " (" + abri + ")" : " (Grappe)");
            return new SimpleStringProperty(display);
        });
        
        colVariete.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label("●  " + item);
                    badge.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #f0fdf4; -fx-background-radius: 12; -fx-padding: 3 8;");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final HBox actionBox = new HBox(12);
            private final Button btnEdit = new Button("✎");
            private final Button btnDelete = new Button("🗑");

            {
                btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #6b7280; -fx-cursor: hand; -fx-font-size: 14px;");
                btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #dc2626; -fx-cursor: hand; -fx-font-size: 14px;");

                btnEdit.setOnAction(e -> {
                    Serre serre = getTableView().getItems().get(getIndex());
                    handleModifier(serre);
                });

                btnDelete.setOnAction(e -> {
                    Serre serre = getTableView().getItems().get(getIndex());
                    handleSupprimer(serre);
                });

                actionBox.getChildren().addAll(btnEdit, btnDelete);
                actionBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionBox);
            }
        });
    }
    private void loadSerresData() {
        try {
            List<Serre> allSerres = serreService.obtenirToutesLesSerres();
            List<Serre> filteredByFerme = allSerres.stream()
                    .filter(s -> s.getIdFerme() == currentFermeId)
                    .collect(Collectors.toList());

            serreList = FXCollections.observableArrayList(filteredByFerme);

            int totalCount = serreList.size();
            double totalArea = serreList.stream().mapToDouble(Serre::getSurfaceM2).sum();
            long distinctVarieties = serreList.stream()
                    .map(Serre::getVariete)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .distinct()
                    .count();

            if (lblTotalSerres != null) lblTotalSerres.setText(totalCount + " serres");
            if (lblTotalSurface != null) lblTotalSurface.setText(String.format("%.0f m²", totalArea));
            if (lblTotalVarietes != null) lblTotalVarietes.setText(distinctVarieties + " variétés");
            if (lblTableSummary != null) lblTableSummary.setText("Affichage de " + totalCount + " sur " + totalCount + " serres");

            setupSearchFilter();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la liste des serres : " + e.getMessage());
        }
    }

    private void setupSearchFilter() {
        if (serreList == null) return;

        FilteredList<Serre> filteredData = new FilteredList<>(serreList, s -> true);
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(serre -> {
                    if (newValue == null || newValue.isBlank()) {
                        return true;
                    }
                    String filter = newValue.toLowerCase();
                    return serre.getNom().toLowerCase().contains(filter)
                            || (serre.getVariete() != null && serre.getVariete().toLowerCase().contains(filter))
                            || (serre.getTypeAbri() != null && serre.getTypeAbri().toLowerCase().contains(filter));
                });
                if (lblTableSummary != null) {
                    lblTableSummary.setText("Affichage de " + filteredData.size() + " sur " + serreList.size() + " serres");
                }
            });
        }
        tableSerres.setItems(filteredData);
    }

    @FXML
    private void handleAjouterSerre() {
        if (currentFermeId <= 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucune ferme associée trouvée pour ajouter une serre.");
            return;
        }
        openSerreFormModal(null);
    }

    private void handleModifier(Serre serre) {
        openSerreFormModal(serre);
    }

    private void openSerreFormModal(Serre serreToEdit) {
        SceneNavigator.openModal(
                "/view/responsable/serre-form.fxml",
                serreToEdit == null ? "Ajouter une serre" : "Modifier la serre",
                (SerreFormController controller) -> {
                    controller.setSerreService(serreService);
                    controller.setIdFerme(currentFermeId);
                    controller.setSerre(serreToEdit);
                }
        );
        loadSerresData();
    }

    private void handleSupprimer(Serre serre) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer la serre : " + serre.getNom());
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette serre ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                serreService.supprimerSerre(serre.getId());
                loadSerresData();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Serre supprimée avec succès.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}