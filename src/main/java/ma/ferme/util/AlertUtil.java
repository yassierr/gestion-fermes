package ma.ferme.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Affiche les boites de dialogue standard (information, erreur, confirmation)
 * pour garder un comportement uniforme dans toute l'application.
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public final class AlertUtil {

    private AlertUtil() {
    }

    /**
     * Affiche un message d'information.
     *
     * @param message le texte a afficher
     */
    public static void info(String message) {
        show(Alert.AlertType.INFORMATION, "Information", message);
    }

    /**
     * Affiche un message d'erreur.
     *
     * @param message le texte de l'erreur
     */
    public static void error(String message) {
        show(Alert.AlertType.ERROR, "Erreur", message);
    }

    /**
     * Demande une confirmation a l'utilisateur (ex. avant une suppression).
     *
     * @param message la question posee
     * @return {@code true} si l'utilisateur clique sur OK
     */
    public static boolean confirm(String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Confirmation");
        a.setHeaderText(null);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type, message, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}