package ma.ferme.controller.responsable;

import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import ma.ferme.model.ResponsableFerme;
import ma.ferme.model.enums.RoleUtilisateur;
import ma.ferme.security.Session;
import ma.ferme.util.AlertUtil;
import ma.ferme.util.SceneNavigator;

/**
 * Fenêtre principale de l'espace Responsable de ferme : barre latérale (Tableau de bord, Serres,
 * Récoltes, Dépenses, Livraisons &amp; Ouvriers) et zone centrale où s'affiche l'écran choisi.
 * <p>
 * Le contrôle d'accès est refait ici (ENF-02) : sans session de responsable, on retourne au login.
 * Elle fournit aussi {@link #acteurConnecte()}, utilisé par les autres contrôleurs du responsable
 * pour appeler les services.
 * </p>
 *
 * @author Mohamed Amine Ait Maait
 * @version 1.0
 */
public class ResponsableMainController {

    private static final String LOGIN = "/view/login.fxml";
    private static final String DASHBOARD = "/view/responsable/dashboard-responsable.fxml";
    private static final String SERRES = "/view/responsable/serres.fxml";
    private static final String RECOLTES = "/view/responsable/recoltes.fxml";
    private static final String DEPENSES = "/view/responsable/depenses.fxml";
    private static final String LIVRAISONS = "/view/responsable/livraisons-ouvriers.fxml";
    private static final String STYLE_ACTIF = "nav-active";

    @FXML private StackPane zoneContenu;
    @FXML private Label lblNom;
    @FXML private Button btnDashboard;
    @FXML private Button btnSerres;
    @FXML private Button btnRecoltes;
    @FXML private Button btnDepenses;
    @FXML private Button btnLivraisons;

    /**
     * Construit l'utilisateur connecté à partir de la {@link Session}, pour les appels aux services.
     * Les services revérifient le rôle et la ferme (EF-T04) ; la session ne contient que
     * l'identifiant, le nom, le rôle et la ferme.
     *
     * @return le responsable de ferme connecté
     * @throws SecurityException si aucune session de responsable n'est ouverte
     */
    public static ResponsableFerme acteurConnecte() {
        if (!Session.estConnecte()
                || !RoleUtilisateur.RESPONSABLE.name().equals(Session.getRole())
                || Session.getIdFerme() == null) {
            throw new SecurityException("Session invalide : veuillez vous reconnecter.");
        }
        return new ResponsableFerme(Session.getIdUtilisateur(), Session.getNomComplet(),
                "session", null, true, Session.getIdFerme());
    }

    /** Vérifie la session, affiche le nom du responsable puis ouvre le premier écran. */
    @FXML
    private void initialize() {
        if (!Session.estConnecte() || !RoleUtilisateur.RESPONSABLE.name().equals(Session.getRole())) {
            // on ne change pas d'écran pendant le chargement de celui-ci
            Platform.runLater(() -> SceneNavigator.goTo(LOGIN, "Gestion des fermes - Connexion"));
            return;
        }
        lblNom.setText(Session.getNomComplet());
        if (!afficher(DASHBOARD, btnDashboard)) {
            afficher(RECOLTES, btnRecoltes);
        }
    }

    @FXML
    private void onDashboard() {
        ouvrir(DASHBOARD, btnDashboard);
    }

    @FXML
    private void onSerres() {
        ouvrir(SERRES, btnSerres);
    }

    @FXML
    private void onRecoltes() {
        ouvrir(RECOLTES, btnRecoltes);
    }

    @FXML
    private void onDepenses() {
        ouvrir(DEPENSES, btnDepenses);
    }

    @FXML
    private void onLivraisons() {
        ouvrir(LIVRAISONS, btnLivraisons);
    }

    /** Ferme la session et revient à l'écran de connexion. */
    @FXML
    private void onDeconnexion() {
        Session.fermer();
        SceneNavigator.goTo(LOGIN, "Gestion des fermes - Connexion");
    }

    /** Affiche un écran, ou prévient l'utilisateur s'il n'est pas encore disponible. */
    private void ouvrir(String fxml, Button bouton) {
        if (!afficher(fxml, bouton)) {
            AlertUtil.info("Cet écran n'est pas encore disponible.");
        }
    }

    /**
     * Charge un écran dans la zone centrale et met en évidence son bouton de menu.
     *
     * @return {@code false} si le fichier FXML est absent ou ne se charge pas
     */
    private boolean afficher(String fxml, Button bouton) {
        Parent ecran;
        try {
            ecran = SceneNavigator.load(fxml);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        }
        zoneContenu.getChildren().setAll(ecran);
        List<Button> boutons = List.of(btnDashboard, btnSerres, btnRecoltes, btnDepenses, btnLivraisons);
        boutons.forEach(b -> b.getStyleClass().remove(STYLE_ACTIF));
        bouton.getStyleClass().add(STYLE_ACTIF);
        return true;
    }
}