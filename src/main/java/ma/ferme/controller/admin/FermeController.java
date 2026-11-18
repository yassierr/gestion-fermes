package ma.ferme.controller.admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ma.ferme.dao.FermeDao;
import ma.ferme.model.Ferme;
import ma.ferme.service.FermeService;

import java.util.Optional;

/**
 * Contrôleur de gestion des fermes.
 * Gère l'affichage, la recherche, l'ajout, la modification et la suppression des fermes.
 *
 * @author Brahim
 * @version 1.0
 */
public class FermeController {

    @FXML private Label lblTotalFarms;
    @FXML private Label lblTotalArea;
    @FXML private Label lblTotalStock;
    @FXML private Label lblTableSummary;
    @FXML private TextField txtSearch;

    @FXML private TableView<Ferme> tableFermes;
    @FXML private TableColumn<Ferme, String> colNom;
    @FXML private TableColumn<Ferme, String> colLocalisation;
    @FXML private TableColumn<Ferme, String> colResponsable;
    @FXML private TableColumn<Ferme, String> colAgentRh;
    @FXML private TableColumn<Ferme, String> colStock;
    @FXML private TableColumn<Ferme, Void> colActions;

    private final FermeService fermeService = new FermeService(new FermeDao());
    private ObservableList<Ferme> fermeList;

    @FXML
    public void initialize() {
        configureTable();
        loadFermesData();
        setupSearchFilter();
    }

    private void configureTable() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colLocalisation.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        
        colResponsable.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty("Driss El Amrani"));
        colAgentRh.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty("Youssef Alaoui"));
        colStock.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStockDisponible() + " kg"));

        colActions.setCellFactory(param -> new TableCell<>() {
            private final MenuButton menuButton = new MenuButton("⋮");
            {
                menuButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 14px;");
                
                MenuItem itemConsult = new MenuItem("👁 Consulter");
                itemConsult.setOnAction(e -> {
                    Ferme ferme = getTableView().getItems().get(getIndex());
                    handleConsulter(ferme);
                });

                MenuItem itemEdit = new MenuItem("✏ Modifier");
                itemEdit.setOnAction(e -> {
                    Ferme ferme = getTableView().getItems().get(getIndex());
                    handleModifier(ferme);
                });

                MenuItem itemDelete = new MenuItem("🗑 Supprimer");
                itemDelete.setStyle("-fx-text-fill: #dc2626;");
                itemDelete.setOnAction(e -> {
                    Ferme ferme = getTableView().getItems().get(getIndex());
                    handleSupprimer(ferme);
                });

                menuButton.getItems().addAll(itemConsult, itemEdit, itemDelete);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : menuButton);
            }
        });
    }

    private void loadFermesData() {
        try {
            fermeList = FXCollections.observableArrayList(fermeService.obtenirToutesLesFermes());
            tableFermes.setItems(fermeList);

            int totalFarms = fermeList.size();
            if (lblTotalFarms != null) lblTotalFarms.setText(totalFarms + " fermes");
            if (lblTotalArea != null) lblTotalArea.setText("185 000 m²");
            if (lblTotalStock != null) lblTotalStock.setText("42 850 kg");
            if (lblTableSummary != null) lblTableSummary.setText("Affichage de " + totalFarms + " sur " + totalFarms + " fermes");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la liste des fermes depuis la base de données.");
        }
    }

    private void setupSearchFilter() {
        if (txtSearch != null && fermeList != null) {
            FilteredList<Ferme> filteredData = new FilteredList<>(fermeList, b -> true);
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(ferme -> {
                    if (newValue == null || newValue.isBlank()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();
                    return ferme.getNom().toLowerCase().contains(lowerCaseFilter) ||
                           ferme.getLocalisation().toLowerCase().contains(lowerCaseFilter);
                });
            });
            tableFermes.setItems(filteredData);
        }
    }

    @FXML
    private void handleAjouterFerme() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Ajouter une ferme");
        dialog.setHeaderText("Création d'une nouvelle ferme");
        dialog.setContentText("Nom de la ferme :");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nom -> {
            TextInputDialog locDialog = new TextInputDialog();
            locDialog.setTitle("Localisation");
            locDialog.setHeaderText("Localisation de la ferme");
            locDialog.setContentText("Adresse / Ville :");
            Optional<String> locResult = locDialog.showAndWait();
            
            locResult.ifPresent(localisation -> {
                try {
                    fermeService.creerFerme(nom, localisation);
                    loadFermesData();
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "La ferme a été ajoutée avec succès.");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
                }
            });
        });
    }

    private void handleConsulter(Ferme ferme) {
        showAlert(Alert.AlertType.INFORMATION, "Détails de la ferme", "Nom : " + ferme.getNom() + "\nLocalisation : " + ferme.getLocalisation() + "\nStock disponible : " + ferme.getStockDisponible() + " kg");
    }

    private void handleModifier(Ferme ferme) {
        TextInputDialog dialog = new TextInputDialog(ferme.getNom());
        dialog.setTitle("Modifier la ferme");
        dialog.setHeaderText("Mise à jour des informations");
        dialog.setContentText("Nouveau nom :");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nouveauNom -> {
            ferme.setNom(nouveauNom);
            try {
                fermeService.modifierFerme(ferme);
                loadFermesData();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Ferme modifiée avec succès.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        });
    }

    private void handleSupprimer(Ferme ferme) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer la ferme : " + ferme.getNom());
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette ferme ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                fermeService.supprimerFerme(ferme.getId());
                loadFermesData();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Ferme supprimée avec succès.");
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