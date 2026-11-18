
package ma.ferme.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import ma.ferme.util.SceneNavigator;

/**
 * Controleur principal de l'espace administrateur.
 * Gere la navigation entre les differentes pages.
 *
 * @author Brahim
 * @version 1.0
 */
public class AdminMainController {

    private static final String FERMES_VIEW =
            "/view/admin/fermes.fxml";

    @FXML
    private StackPane contentArea;

    @FXML
    private Label pageTitle;

    /**
     * Initialise l'espace administrateur.
     */
    @FXML
    public void initialize() {
        showDashboard();
    }

    /**
     * Affiche la page d'accueil de l'administrateur.
     */
    @FXML
    private void showDashboard() {
        pageTitle.setText("Tableau de bord");

        Label welcome = new Label(
                "Bienvenue dans votre espace administrateur.\n"
                + "Selectionnez une rubrique dans le menu."
        );

        welcome.setStyle(
                "-fx-font-size: 18px;"
                + "-fx-text-fill: #64748b;"
        );

        contentArea.getChildren().setAll(welcome);
    }

    /**
     * Ouvre la page de gestion des fermes.
     */
    @FXML
    private void showFermes() {
        pageTitle.setText("Gestion des fermes");

        try {
            contentArea.getChildren().setAll(
                    SceneNavigator.load(FERMES_VIEW)
            );
        } catch (IllegalStateException e) {
            showError("Impossible de charger la page des fermes.");
        }
    }

    /**
     * Retourne a l'ecran de connexion.
     */
    @FXML
    private void logout() {
        SceneNavigator.goTo(
                "/view/login.fxml",
                "Gestion des fermes - Connexion"
        );
    }

    /**
     * Affiche un message d'erreur dans la zone principale.
     *
     * @param message message a afficher
     */
    private void showError(String message) {
        Label error = new Label(message);

        error.setStyle(
                "-fx-font-size: 15px;"
                + "-fx-text-fill: #dc2626;"
        );

        contentArea.getChildren().setAll(error);
    }
}
