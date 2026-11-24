package ma.ferme.controller.responsable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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

/**
 * Écran « Dépenses » du responsable (EFR09, EFR10) : liste avec recherche, filtre par catégorie et
 * par période, total du mois, et fenêtre modale « Nouvelle dépense ».
 * <p>
 * Ce contrôleur sert deux fichiers FXML : {@code depenses.fxml} (la liste) et
 * {@code depense-form.fxml} (la modale d'ajout / de modification). Seuls les champs présents dans
 * le FXML chargé sont renseignés par JavaFX : {@link #initialize()} détecte le mode.
 * La dépense à modifier est transmise à la modale par {@link #aModifier}.
 * </p>
 *
 * @author Mohamed Amine Ait Maait
 * @version 1.0
 */
public class DepenseController {

    private static final String FORMULAIRE = "/view/responsable/depense-form.fxml";

    /** Dépense transmise à la modale pour la modifier ; {@code null} pour une nouvelle dépense. */
    private static Depense aModifier;

    private final DepenseService service = new DepenseService();

    // ----- liste (depenses.fxml) -----
    @FXML private TableView<Depense> tableDepenses;
    @FXML private TableColumn<Depense, String> colDate;
    @FXML private TableColumn<Depense, String> colCategorie;
    @FXML private TableColumn<Depense, String> colDescription;
    @FXML private TableColumn<Depense, String> colMontant;
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

    /** Dépense en cours de modification dans la modale, ou {@code null}. */
    private Depense enEdition;

    /** Détecte le FXML chargé (liste ou modale) et prépare ses champs. */
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

        // premier élément null = « toutes les catégories »
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

        txtRecherche.textProperty().addListener((obs, ancien, nouveau) -> filtrer());
        cmbFiltreCategorie.valueProperty().addListener((obs, ancien, nouveau) -> filtrer());
        dpDebut.valueProperty().addListener((obs, ancien, nouveau) -> filtrer());
        dpFin.valueProperty().addListener((obs, ancien, nouveau) -> filtrer());
        filtrer();
    }

    /** Recharge la liste selon la recherche, la catégorie et la période, et met à jour les totaux. */
    private void filtrer() {
        try {
            ResponsableFerme acteur = ResponsableMainController.acteurConnecte();
            List<Depense> depenses = service.rechercher(acteur, txtRecherche.getText(),
                    cmbFiltreCategorie.getValue(), dpDebut.getValue(), dpFin.getValue());
            tableDepenses.setItems(FXCollections.observableArrayList(depenses));
            lblNombre.setText(depenses.size() + (depenses.size() > 1 ? " dépenses" : " dépense"));
            BigDecimal total = depenses.stream().map(Depense::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
            lblTotalFiltre.setText(Formats.mad(total));
            lblTotalMois.setText(Formats.mad(service.totalDuMois(acteur, YearMonth.now())));
        } catch (SecurityException | DaoException e) {
            AlertUtil.error(e.getMessage());
        }
    }

    /** Ouvre la modale pour saisir une nouvelle dépense. */
    @FXML
    private void onNouvelle() {
        aModifier = null;
        SceneNavigator.openModal(FORMULAIRE, "Nouvelle dépense");
        filtrer();
    }

    /** Ouvre la modale pour modifier la dépense sélectionnée. */
    @FXML
    private void onModifier() {
        Depense choisie = tableDepenses.getSelectionModel().getSelectedItem();
        if (choisie == null) {
            AlertUtil.info("Sélectionnez d'abord une dépense dans la liste.");
            return;
        }
        aModifier = choisie;
        try {
            SceneNavigator.openModal(FORMULAIRE, "Modifier la dépense");
        } finally {
            aModifier = null;
        }
        filtrer();
    }

    /** Supprime la dépense sélectionnée après confirmation. */
    @FXML
    private void onSupprimer() {
        Depense choisie = tableDepenses.getSelectionModel().getSelectedItem();
        if (choisie == null) {
            AlertUtil.info("Sélectionnez d'abord une dépense dans la liste.");
            return;
        }
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

    /** Efface la recherche et les filtres. */
    @FXML
    private void onReinitialiser() {
        txtRecherche.clear();
        cmbFiltreCategorie.setValue(null);
        dpDebut.setValue(null);
        dpFin.setValue(null);
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

    /** Enregistre (ou met à jour) la dépense saisie, puis ferme la modale. */
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

    /** Ferme la modale sans rien enregistrer. */
    @FXML
    private void onAnnuler() {
        fermer();
    }

    private void fermer() {
        ((Stage) btnEnregistrer.getScene().getWindow()).close();
    }
}