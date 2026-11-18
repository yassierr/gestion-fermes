
package ma.ferme.util;

import java.io.IOException;
import java.util.function.Consumer;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class SceneNavigator {

    private static final String CSS = "/css/style.css";
    private static Stage mainStage;

    private SceneNavigator() {
    }

    public static void setStage(Stage stage) {
        mainStage = stage;
    }

    public static void goTo(String fxmlPath, String title) {
        Parent root = load(fxmlPath);
        Scene scene = new Scene(root);
        addCss(scene);
        mainStage.setTitle(title);
        mainStage.setScene(scene);
        mainStage.show();
    }

    public static Parent load(String fxmlPath) {
        try {
            return FXMLLoader.load(SceneNavigator.class.getResource(fxmlPath));
        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException("Impossible de charger " + fxmlPath, e);
        }
    }

    // Ouverture d'une fenêtre modale sans initialisation du contrôleur
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

    // Ouverture d'une fenêtre modale avec initialisation du contrôleur
    public static <T> void openModal(
            String fxmlPath,
            String title,
            Consumer<T> initializer) {

        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneNavigator.class.getResource(fxmlPath)
            );

            Parent root = loader.load();

            @SuppressWarnings("unchecked")
            T controller = (T) loader.getController();

            initializer.accept(controller);

            Stage modal = new Stage();
            modal.initOwner(mainStage);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle(title);

            Scene scene = new Scene(root);
            addCss(scene);

            modal.setScene(scene);
            modal.showAndWait();

        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException(
                    "Impossible de charger " + fxmlPath, e
            );
        }
    }

    private static void addCss(Scene scene) {
        var css = SceneNavigator.class.getResource(CSS);

        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }
}
