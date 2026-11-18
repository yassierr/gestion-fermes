package ma.ferme.controller.responsable;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ma.ferme.model.Serre;
import ma.ferme.service.SerreService;

/**
 * Contrôleur du formulaire modal d'ajout ou de modification d'une serre.
 *
 * @author Brahim
 * @version 1.0
 */
public class SerreFormController {

    @FXML private Label lblFormTitle;
    @FXML private TextField txtNom;
    @FXML private TextField txtSurface;
    @FXML private ComboBox<String> comboVariete;
    @FXML private ComboBox<String> comboTypeAbri;
    @FXML private ComboBox<String> comboIrrigation;

    private SerreService serreService;
    private Serre currentSerre;
    private int idFerme;
    private boolean saved = false;

    @FXML
    public void initialize() {
        comboVariete.getItems().addAll("Sweetelle", "Angelle", "Bamcelo", "Datterino", "San Marzano", "Cœur de Pigeon");
        comboTypeAbri.getItems().addAll("Serre plastique", "Serre verre", "Serre canarienne", "Multichapelle");
        comboIrrigation.getItems().addAll("Goutte-à-goutte", "Aspersion", "Micro-aspersion", "Hydroponie");
    }

    public void setSerreService(SerreService serreService) {
        this.serreService = serreService;
    }

    public void setIdFerme(int idFerme) {
        this.idFerme = idFerme;
    }

    public void setSerre(Serre serre) {
        this.currentSerre = serre;
        if (serre != null) {
            this.idFerme = serre.getIdFerme();
            lblFormTitle.setText("Modifier la serre");
            txtNom.setText(serre.getNom());
            txtSurface.setText(String.valueOf(serre.getSurfaceM2()));
            comboVariete.setValue(serre.getVariete());
            comboTypeAbri.setValue(serre.getTypeAbri());
            comboIrrigation.setValue(serre.getIrrigation());
        } else {
            lblFormTitle.setText("Ajouter une serre");
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleEnregistrer() {
        try {
            String nom = txtNom.getText();
            double surface = parseSurface(txtSurface.getText());
            String variete = comboVariete.getEditor().getText();
            String typeAbri = comboTypeAbri.getEditor().getText();
            String irrigation = comboIrrigation.getEditor().getText();

            if (currentSerre == null) {
                currentSerre = serreService.creerSerre(nom, surface, variete, typeAbri, irrigation, idFerme);
            } else {
                currentSerre.setNom(nom);
                currentSerre.setSurfaceM2(surface);
                currentSerre.setVariete(variete);
                currentSerre.setTypeAbri(typeAbri);
                currentSerre.setIrrigation(irrigation);
                serreService.modifierSerre(currentSerre);
            }
            saved = true;
            closeStage();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation", "La surface doit être un nombre valide (ex: 7500).");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private double parseSurface(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("La surface de la serre est obligatoire.");
        }
        return Double.parseDouble(raw.trim().replace(",", ".").replace(" ", ""));
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}