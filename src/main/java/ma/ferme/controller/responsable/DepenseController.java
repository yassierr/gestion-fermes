package ma.ferme.controller.responsable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ma.ferme.dao.DaoException;
import ma.ferme.model.Depense;
import ma.ferme.model.ResponsableFerme;
import ma.ferme.model.enums.CategorieDepense;
import ma.ferme.service.DepenseService;
import ma.ferme.util.AlertUtil;
import ma.ferme.util.Formats;
import ma.ferme.util.SceneNavigator;

public class DepenseController {

    private static final String FORMULAIRE = "/view/responsable/depense-form.fxml";
    private static Depense aModifier;

    private final DepenseService service = new DepenseService();

    // ----- liste (depenses.fxml) -----
    @FXML private TableView<Depense> tableDepenses;
    @FXML private TableColumn<Depense, String> colDate;
    @FXML private TableColumn<Depense, String> colCategorie;
    @FXML private TableColumn<Depense, String> colDescription;
    @FXML private TableColumn<Depense, String> colMontant;
    @FXML private TableColumn<Depense, Void> colActions;
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<CategorieDepense> cmbFiltreCategorie;
    @FXML private DatePicker dpDebut;
    @FXML private DatePicker dpFin;
    @FXML private Label lblTotalMois;
    @FXML private Label lblTotalFiltre;
    @FXML private Label lblNombre;

    // ----- modale (depense-form.fxml) -----
    @FXML private Label lblTitreFormulaire;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<CategorieDepense> cmbCategorie;
    @FXML private TextField txtMontant;
    @FXML private TextArea txtDescription;
    @FXML private Button btnEnregistrer;

    private Depense enEdition;

    @FXML
    private void initialize() {
        if (tableDepenses != null) {
            initialiserListe();
        }
        if (cmbCategorie != null) {
            initialiserFormulaire();
        }
    }

    // ================= liste =================

    private void initialiserListe() {
        colDate.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formats.date(c.getValue().getDateDepense())));
        colCategorie.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCategorie().getLibelle()));
        colDescription.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDescription()));
        colMontant.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formats.mad(c.getValue().getMontant())));
        colMontant.setStyle("-fx-alignment: CENTER-RIGHT;");
        configurerColonneActions();

        if (cmbFiltreCategorie != null) {
            cmbFiltreCategorie.getItems().add(null);
            cmbFiltreCategorie.getItems().addAll(CategorieDepense.values());
            cmbFiltreCategorie.setConverter(new StringConverter<>() {
                @Override
                public String toString(CategorieDepense c) {
                    return c == null ? "Toutes les catégories" : c.getLibelle();
                }

                @Override
                public CategorieDepense fromString(String texte) {
                    return null;
                }
            });
            cmbFiltreCategorie.valueProperty().addListener((obs, ancien, nouveau) -> filtrer());
        }
        if (txtRecherche != null) {
            txtRecherche.textProperty().addListener((obs, ancien, nouveau) -> filtrer());
        }
        if (dpDebut != null) {
            dpDebut.valueProperty().addListener((obs, ancien, nouveau) -> filtrer());
        }
        if (dpFin != null) {
            dpFin.valueProperty().addListener((obs, ancien, nouveau) -> filtrer());
        }
        filtrer();
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

    private void filtrer() {
        try {
            ResponsableFerme acteur = ResponsableMainController.acteurConnecte();
            String recherche = txtRecherche != null ? txtRecherche.getText() : null;
            CategorieDepense categorie = cmbFiltreCategorie != null ? cmbFiltreCategorie.getValue() : null;
            LocalDate debut = dpDebut != null ? dpDebut.getValue() : null;
            LocalDate fin = dpFin != null ? dpFin.getValue() : null;

            List<Depense> depenses = service.rechercher(acteur, recherche, categorie, debut, fin);
            tableDepenses.setItems(FXCollections.observableArrayList(depenses));
            if (lblNombre != null) {
                lblNombre.setText(depenses.size() + (depenses.size() > 1 ? " dépenses" : " dépense"));
            }
            if (lblTotalFiltre != null) {
                BigDecimal total = depenses.stream().map(Depense::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
                lblTotalFiltre.setText(Formats.mad(total));
            }
            if (lblTotalMois != null) {
                lblTotalMois.setText(Formats.nombre(service.totalDuMois(acteur, YearMonth.now())));
            }
        } catch (SecurityException | DaoException e) {
            AlertUtil.error(e.getMessage());
        }
    }

    @FXML
    private void onNouvelle() {
        aModifier = null;
        SceneNavigator.openModal(FORMULAIRE, "Nouvelle dépense");
        filtrer();
    }

    @FXML
    private void onModifier() {
        Depense choisie = tableDepenses.getSelectionModel().getSelectedItem();
        if (choisie == null) {
            AlertUtil.info("Sélectionnez d'abord une dépense dans la liste.");
            return;
        }
        modifierLigne(choisie);
    }

    private void modifierLigne(Depense choisie) {
        aModifier = choisie;
        try {
            SceneNavigator.openModal(FORMULAIRE, "Modifier la dépense");
        } finally {
            aModifier = null;
        }
        filtrer();
    }

    @FXML
    private void onSupprimer() {
        Depense choisie = tableDepenses.getSelectionModel().getSelectedItem();
        if (choisie == null) {
            AlertUtil.info("Sélectionnez d'abord une dépense dans la liste.");
            return;
        }
        supprimerLigne(choisie);
    }

    private void supprimerLigne(Depense choisie) {
        if (!AlertUtil.confirm("Supprimer la dépense « " + choisie.getCategorie().getLibelle() + " » de "
                + Formats.mad(choisie.getMontant()) + " ?")) {
            return;
        }
        try {
            service.supprimer(ResponsableMainController.acteurConnecte(), choisie.getId());
            filtrer();
        } catch (IllegalArgumentException | SecurityException | DaoException e) {
            AlertUtil.error(e.getMessage());
        }
    }

    @FXML
    private void onReinitialiser() {
        if (txtRecherche != null) txtRecherche.clear();
        if (cmbFiltreCategorie != null) cmbFiltreCategorie.setValue(null);
        if (dpDebut != null) dpDebut.setValue(null);
        if (dpFin != null) dpFin.setValue(null);
    }

    // ================= modale =================

    private void initialiserFormulaire() {
        cmbCategorie.getItems().setAll(CategorieDepense.values());
        enEdition = aModifier;
        if (enEdition == null) {
            lblTitreFormulaire.setText("Nouvelle dépense");
            btnEnregistrer.setText("Enregistrer");
            dpDate.setValue(LocalDate.now());
        } else {
            lblTitreFormulaire.setText("Modifier la dépense");
            btnEnregistrer.setText("Mettre à jour");
            dpDate.setValue(enEdition.getDateDepense());
            cmbCategorie.setValue(enEdition.getCategorie());
            txtMontant.setText(enEdition.getMontant().toPlainString());
            txtDescription.setText(enEdition.getDescription());
        }
    }

    @FXML
    private void onEnregistrer() {
        try {
            ResponsableFerme acteur = ResponsableMainController.acteurConnecte();
            BigDecimal montant = Formats.lireNombre(txtMontant.getText());
            if (enEdition == null) {
                service.ajouter(acteur, dpDate.getValue(), cmbCategorie.getValue(), montant,
                        txtDescription.getText());
            } else {
                service.modifier(acteur, enEdition.getId(), dpDate.getValue(), cmbCategorie.getValue(),
                        montant, txtDescription.getText());
            }
            fermer();
        } catch (IllegalArgumentException | SecurityException | DaoException e) {
            AlertUtil.error(e.getMessage());
        }
    }

    @FXML
    private void onAnnuler() {
        fermer();
    }

    private void fermer() {
        ((Stage) btnEnregistrer.getScene().getWindow()).close();
    }
}