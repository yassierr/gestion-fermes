package ma.ferme.controller.admin;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ma.ferme.model.Client;
import ma.ferme.model.enums.TypeClient;
import ma.ferme.service.ClientService;

/**
 * Controller du formulaire modal d'ajout / modification d'un client.
 * Appeler {@link #setClient(Client)} avant l'affichage : {@code null} pour un
 * nouveau client, un client existant pour le modifier.
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public class ClientFormController {

    private final ClientService service = new ClientService();
    private Client client;

    @FXML private Label lblTitre;
    @FXML private Label lblErreur;
    @FXML private TextField tfCode;
    @FXML private TextField tfNom;
    @FXML private ComboBox<TypeClient> cbType;
    @FXML private TextField tfTelephone;
    @FXML private TextField tfVille;

    /** Remplit la liste des types de client. */
    @FXML
    private void initialize() {
        cbType.setItems(FXCollections.observableArrayList(TypeClient.values()));
    }

    /**
     * Prepare le formulaire.
     *
     * @param aModifier le client a modifier, ou {@code null} pour en creer un nouveau
     */
    public void setClient(Client aModifier) {
        this.client = aModifier;
        if (aModifier != null) {
            lblTitre.setText("Modifier le client");
            tfCode.setText(aModifier.getCode());
            tfNom.setText(aModifier.getNom());
            cbType.setValue(aModifier.getType());
            tfTelephone.setText(aModifier.getTelephone());
            tfVille.setText(aModifier.getVille());
        }
    }

    /** Valide la saisie, enregistre le client et ferme la fenetre. */
    @FXML
    private void onEnregistrer() {
        try {
            Client c = client == null ? new Client() : client;
            c.setCode(tfCode.getText() == null ? null : tfCode.getText().trim());
            c.setNom(tfNom.getText() == null ? null : tfNom.getText().trim());
            c.setType(cbType.getValue());
            c.setTelephone(vide(tfTelephone.getText()));
            c.setVille(vide(tfVille.getText()));
            if (client == null) {
                service.ajouter(c);
            } else {
                service.modifier(c);
            }
            fermer();
        } catch (RuntimeException e) {
            lblErreur.setText(e.getMessage());
        }
    }

    /** Ferme la fenetre sans enregistrer. */
    @FXML
    private void onAnnuler() {
        fermer();
    }

    private void fermer() {
        ((Stage) tfCode.getScene().getWindow()).close();
    }

    private static String vide(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}