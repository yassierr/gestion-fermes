package ma.ferme;

import javafx.application.Application;
import javafx.stage.Stage;
import ma.ferme.util.SceneNavigator;

/**
 * Point d'entree de l'application JavaFX.
 * Initialise la fenetre principale puis affiche l'ecran de connexion.
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public class MainApp extends Application {

    /**
     * Demarre l'interface : memorise la fenetre principale et ouvre le login.
     *
     * @param stage la fenetre principale fournie par JavaFX
     */
    @Override
    public void start(Stage stage) {
        SceneNavigator.setStage(stage);
        SceneNavigator.goTo("/view/login.fxml", "Gestion des fermes - Connexion");
    }

    /**
     * Lance l'application.
     *
     * @param args arguments de la ligne de commande (non utilises)
     */
    public static void main(String[] args) {
        launch(args);
    }
}