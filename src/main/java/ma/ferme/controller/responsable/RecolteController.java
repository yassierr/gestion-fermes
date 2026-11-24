package ma.ferme.controller.responsable;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import ma.ferme.dao.DaoException;
import ma.ferme.model.Recolte;
import ma.ferme.model.ResponsableFerme;
import ma.ferme.service.RecolteService;
import ma.ferme.util.AlertUtil;
import ma.ferme.util.Formats;

public class RecolteController {

    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RecolteService service = new RecolteService();

    @FXML private Label lblTitreFormulaire;
    @FXML private TextField txtPoids;
    @FXML private ComboBox<String> cmbVariete;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnAnnuler;
    @FXML private Label lblStock;
    @FXML private Label lblTotal;
    @FXML private TableView<Recolte> tableRecoltes;
    @FXML private TableColumn<Recolte, String> colDate;
    @FXML private TableColumn<Recolte, String> colVariete;
    @FXML private TableColumn<Recolte, String> colPoids;
    @FXML private TableColumn<Recolte, Void> colActions;

    private Recolte enEdition;

    @FXML
    private void initialize() {
        colDate.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDateHeure().format(DATE_HEURE)));
        colVariete.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getVariete()));
        colPoids.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formats.nombre(c.getValue().getPoidsKg()) + " kg"));
        colPoids.setStyle("-fx-alignment: CENTER-RIGHT;");
        configurerColonneActions();
        cmbVariete.setEditable(true);
        passerEnModeAjout();
        rafraichir();
    }

    private void configurerColonneActions() {
        if (colActions == null) {
            return;
        }
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✎");
            private final Button btnDel = new Button("🗑");
            private final HBox box = new HBox(8, btnEdit, btnDel);

            {
                box.setAlignment(Pos.CENTER_RIGHT);
                btnEdit.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #374151;");
                btnDel.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #dc2626;");
                btnEdit.setOnAction(e -> modifierLigne(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> supprimerLigne(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    @FXML
    private void onEnregistrer() {
        try {
            ResponsableFerme acteur = ResponsableMainController.acteurConnecte();
            BigDecimal poids = Formats.lireNombre(txtPoids.getText());
            String variete = cmbVariete.getEditor().getText();
            if (enEdition == null) {
                service.enregistrer(acteur, poids, variete);
            } else {
                service.modifier(acteur, enEdition.getId(), poids, variete);
            }
            passerEnModeAjout();
            rafraichir();
        } catch (IllegalArgumentException | IllegalStateException | SecurityException | DaoException e) {
            AlertUtil.error(e.getMessage());
        }
    }

    @FXML
    private void onModifier() {
        Recolte choisie = tableRecoltes.getSelectionModel().getSelectedItem();
        if (choisie == null) {
            AlertUtil.info("Sélectionnez d'abord une récolte dans l'historique.");
            return;
        }
        modifierLigne(choisie);
    }

    private void modifierLigne(Recolte choisie) {
        enEdition = choisie;
        lblTitreFormulaire.setText("Modifier la récolte");
        btnEnregistrer.setText("Mettre à jour");
        btnAnnuler.setVisible(true);
        btnAnnuler.setManaged(true);
        txtPoids.setText(choisie.getPoidsKg().toPlainString());
        cmbVariete.getEditor().setText(choisie.getVariete());
    }

    @FXML
    private void onSupprimer() {
        Recolte choisie = tableRecoltes.getSelectionModel().getSelectedItem();
        if (choisie == null) {
            AlertUtil.info("Sélectionnez d'abord une récolte dans l'historique.");
            return;
        }
        supprimerLigne(choisie);
    }

    private void supprimerLigne(Recolte choisie) {
        if (!AlertUtil.confirm("Supprimer la récolte de " + Formats.nombre(choisie.getPoidsKg())
                + " kg (" + choisie.getVariete() + ") ?")) {
            return;
        }
        try {
            service.supprimer(ResponsableMainController.acteurConnecte(), choisie.getId());
            passerEnModeAjout();
            rafraichir();
        } catch (IllegalArgumentException | IllegalStateException | SecurityException | DaoException e) {
            AlertUtil.error(e.getMessage());
        }
    }

    @FXML
    private void onAnnuler() {
        passerEnModeAjout();
    }

    private void passerEnModeAjout() {
        enEdition = null;
        lblTitreFormulaire.setText("Nouvelle récolte");
        btnEnregistrer.setText("Enregistrer");
        btnAnnuler.setVisible(false);
        btnAnnuler.setManaged(false);
        txtPoids.clear();
        cmbVariete.getEditor().clear();
        cmbVariete.setValue(null);
        tableRecoltes.getSelectionModel().clearSelection();
    }

    private void rafraichir() {
        try {
            ResponsableFerme acteur = ResponsableMainController.acteurConnecte();
            List<Recolte> recoltes = service.lister(acteur);
            tableRecoltes.setItems(FXCollections.observableArrayList(recoltes));
            if (lblStock != null) {
                lblStock.setText(Formats.nombre(service.stockDisponible(acteur)));
            }
            if (lblTotal != null) {
                BigDecimal total = recoltes.stream().map(Recolte::getPoidsKg).reduce(BigDecimal.ZERO, BigDecimal::add);
                lblTotal.setText(Formats.nombre(total) + " kg");
            }
            cmbVariete.setItems(FXCollections.observableArrayList(service.varietes(acteur)));
        } catch (SecurityException | DaoException e) {
            AlertUtil.error(e.getMessage());
        }
    }
}