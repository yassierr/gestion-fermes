package ma.ferme.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ma.ferme.model.Ferme;
import ma.ferme.service.FermeService;

/**
 * Contrôleur du formulaire modal d'ajout/modification d'une ferme.
 *
 * @author Brahim
 * @version 1.0
 */
public class FermeFormController {

    @FXML private Label lblFormTitle;
    @FXML private TextField txtNom;
    @FXML private TextField txtLocalisation;
    @FXML private ComboBox<String> comboResponsable;
    @FXML private ComboBox<String> comboAgentRh;
    @FXML private TextField txtStock;

    private FermeService fermeService;
    private Ferme currentFerme;
    private boolean saved = false;

    @FXML
    public void initialize() {
        comboResponsable.getItems().addAll("Driss El Amrani", "Mehdi Chraibi", "Tariq Tazi");
        comboAgentRh.getItems().addAll("Youssef Alaoui", "Salma Bennani", "Houda Mansouri");
    }

    public void setFermeService(FermeService fermeService) {
        this.fermeService = fermeService;
    }

    public void setFerme(Ferme ferme) {
        this.currentFerme = ferme;
        if (ferme != null) {
            lblFormTitle.setText("Modifier la ferme");
            txtNom.setText(ferme.getNom());
            txtLocalisation.setText(ferme.getLocalisation());
            txtStock.setText(String.valueOf(ferme.getStockDisponible()));
        } else {
            lblFormTitle.setText("Ajouter une ferme");
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleEnregistrer() {
        try {
            String nom = txtNom.getText();
            String loc = txtLocalisation.getText();

            if (currentFerme == null) {
                currentFerme = fermeService.creerFerme(nom, loc);
            } else {
                currentFerme.setNom(nom);
                currentFerme.setLocalisation(loc);
                fermeService.modifierFerme(currentFerme);
            }
            saved = true;
            closeStage();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de validation");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleAnnuler() {
        closeStage();
    }

    @FXML
    private void handleClose() {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) txtNom.getScene().getWindow();
        stage.close();
    }
}