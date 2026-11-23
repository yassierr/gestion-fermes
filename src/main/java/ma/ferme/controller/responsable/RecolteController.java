package ma.ferme.controller.responsable;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ma.ferme.dao.DaoException;
import ma.ferme.model.Recolte;
import ma.ferme.model.ResponsableFerme;
import ma.ferme.service.RecolteService;
import ma.ferme.util.AlertUtil;
import ma.ferme.util.Formats;

/**
 * Écran « Récoltes » du responsable (EFR06 à EFR08) : formulaire d'enregistrement, historique
 * des récoltes de la ferme et stock disponible. Une ligne de l'historique peut être modifiée
 * (le formulaire passe en mode édition) ou supprimée. Les règles (droits, RG-07 : stock jamais
 * négatif) sont dans {@link RecolteService} ; ce contrôleur ne fait qu'afficher et relayer.
 *
 * @author Mohamed Amine Ait Maait
 * @version 1.0
 */
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

    /** Récolte en cours de modification, ou {@code null} en mode ajout. */
    private Recolte enEdition;

    /** Prépare le tableau et le formulaire, puis charge les données. */
    @FXML
    private void initialize() {
        colDate.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDateHeure().format(DATE_HEURE)));
        colVariete.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getVariete()));
        colPoids.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formats.nombre(c.getValue().getPoidsKg()) + " kg"));
        colPoids.setStyle("-fx-alignment: CENTER-RIGHT;");
        cmbVariete.setEditable(true);
        passerEnModeAjout();
        rafraichir();
    }

    /** Enregistre une nouvelle récolte, ou applique la modification en cours. */
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

    /** Charge la récolte sélectionnée dans le formulaire pour la modifier. */
    @FXML
    private void onModifier() {
        Recolte choisie = tableRecoltes.getSelectionModel().getSelectedItem();
        if (choisie == null) {
            AlertUtil.info("Sélectionnez d'abord une récolte dans l'historique.");
            return;
        }
        enEdition = choisie;
        lblTitreFormulaire.setText("Modifier la récolte");
        btnEnregistrer.setText("Mettre à jour");
        btnAnnuler.setVisible(true);
        btnAnnuler.setManaged(true);
        txtPoids.setText(choisie.getPoidsKg().toPlainString());
        cmbVariete.getEditor().setText(choisie.getVariete());
    }

    /** Supprime la récolte sélectionnée après confirmation (refusé si le stock devient négatif). */
    @FXML
    private void onSupprimer() {
        Recolte choisie = tableRecoltes.getSelectionModel().getSelectedItem();
        if (choisie == null) {
            AlertUtil.info("Sélectionnez d'abord une récolte dans l'historique.");
            return;
        }
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

    /** Abandonne la modification et vide le formulaire. */
    @FXML
    private void onAnnuler() {
        passerEnModeAjout();
    }

    private void passerEnModeAjout() {
        enEdition = null;
        lblTitreFormulaire.setText("Enregistrer une récolte");
        btnEnregistrer.setText("Enregistrer");
        btnAnnuler.setVisible(false);
        btnAnnuler.setManaged(false);
        txtPoids.clear();
        cmbVariete.getEditor().clear();
        cmbVariete.setValue(null);
        tableRecoltes.getSelectionModel().clearSelection();
    }

    /** Recharge l'historique, le stock, le total et les variétés proposées. */
    private void rafraichir() {
        try {
            ResponsableFerme acteur = ResponsableMainController.acteurConnecte();
            List<Recolte> recoltes = service.lister(acteur);
            tableRecoltes.setItems(FXCollections.observableArrayList(recoltes));
            lblStock.setText(Formats.nombre(service.stockDisponible(acteur)) + " kg");
            BigDecimal total = recoltes.stream().map(Recolte::getPoidsKg).reduce(BigDecimal.ZERO, BigDecimal::add);
            lblTotal.setText(Formats.nombre(total) + " kg");
            cmbVariete.setItems(FXCollections.observableArrayList(service.varietes(acteur)));
        } catch (SecurityException | DaoException e) {
            AlertUtil.error(e.getMessage());
        }
    }
}