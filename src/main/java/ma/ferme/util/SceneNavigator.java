package ma.ferme.util;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Gere la navigation entre les ecrans FXML : changement de fenetre principale
 * et ouverture de fenetres modales (formulaires d'ajout/modification).
 * Les chemins FXML sont relatifs a {@code src/main/resources}, ex. {@code /view/login.fxml}.
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public final class SceneNavigator {

    private static final String CSS = "/css/style.css";
    private static Stage mainStage;

    private SceneNavigator() {
    }

    /**
     * Enregistre la fenetre principale (appele une fois par {@code MainApp}).
     *
     * @param stage la fenetre principale
     */
    public static void setStage(Stage stage) {
        mainStage = stage;
    }

    /**
     * Remplace le contenu de la fenetre principale par l'ecran demande.
     *
     * @param fxmlPath chemin du fichier FXML (ex. {@code /view/admin/admin-main.fxml})
     * @param title    titre de la fenetre
     */
    public static void goTo(String fxmlPath, String title) {
        Parent root = load(fxmlPath);
        Scene scene = new Scene(root);
        addCss(scene);
        mainStage.setTitle(title);
        mainStage.setScene(scene);
        mainStage.show();
    }

    /**
     * Charge un FXML et retourne son noeud racine (pour l'inserer dans une zone
     * de la fenetre, par exemple le contenu central d'un BorderPane).
     *
     * @param fxmlPath chemin du fichier FXML
     * @return le noeud racine charge
     */
    public static Parent load(String fxmlPath) {
        try {
            return FXMLLoader.load(SceneNavigator.class.getResource(fxmlPath));
        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException("Impossible de charger " + fxmlPath, e);
        }
    }

    /**
     * Ouvre un FXML dans une fenetre modale et attend sa fermeture.
     *
     * @param fxmlPath chemin du fichier FXML du formulaire
     * @param title    titre de la fenetre modale
     */
    public static void openModal(String fxmlPath, String title) {
        Stage modal = new Stage();
        modal.initOwner(mainStage);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(title);
        Scene scene = new Scene(load(fxmlPath));
        addCss(scene);
        modal.setScene(scene);
        modal.showAndWait();
    }

    private static void addCss(Scene scene) {
        var css = SceneNavigator.class.getResource(CSS);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }
}