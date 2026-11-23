package ma.ferme.controller.rh;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import ma.ferme.dao.FermeDao;
import ma.ferme.model.Ferme;
import ma.ferme.security.Session;
import ma.ferme.service.FermeService;
import ma.ferme.util.AlertUtil;
import ma.ferme.util.SceneNavigator;

/**
 * Contrôleur principal de l'espace Agent RH ({@code /view/rh/rh-main.fxml}).
 * Gère l'en-tête de session, la navigation latérale et le chargement des vues RH.
 */
public class RhMainController {

    private static final String VIEW_DASHBOARD = "/view/rh/dashboard-rh.fxml";
    private static final String VIEW_OUVRIERS = "/view/rh/ouvriers.fxml";
    private static final String VIEW_PAIEMENT = "/view/rh/paiement.fxml";

    @FXML private StackPane rootContentStack;
    @FXML private Label pageTitle;
    @FXML private Label lblFerme;
    @FXML private Label lblAgent;

    @FXML private Button navDashboard;
    @FXML private Button navOuvriers;
    @FXML private Button navPaiement;

    private final FermeService fermeService = new FermeService(new FermeDao());

    @FXML
    public void initialize() {
        if (!Session.estConnecte() || !"AGENT_RH".equals(Session.getRole())) {
            AlertUtil.error("Accès réservé à l'Agent RH.");
            SceneNavigator.goTo("/view/login.fxml", "Gestion des fermes - Connexion");
            return;
        }

        lblAgent.setText(Session.getNomComplet());
        Integer idFerme = Session.getIdFerme();
        if (idFerme != null) {
            String nomFerme = fermeService.obtenirToutesLesFermes().stream()
                    .filter(f -> f.getId() == idFerme)
                    .map(Ferme::getNom)
                    .findFirst()
                    .orElse("Ferme #" + idFerme);
            lblFerme.setText("Ferme : " + nomFerme);
        }

        showDashboard();
    }

    @FXML
    public void showDashboard() {
        setActiveNav(navDashboard);
        pageTitle.setText("Tableau de bord RH");
        chargerVue(VIEW_DASHBOARD);
    }

    @FXML
    public void showOuvriers() {
        setActiveNav(navOuvriers);
        pageTitle.setText("Gestion des ouvriers et pointage");
        chargerVue(VIEW_OUVRIERS);
    }

    @FXML
    public void showPaiement() {
        setActiveNav(navPaiement);
        pageTitle.setText("Paiement des salaires et reçus");
        chargerVue(VIEW_PAIEMENT);
    }

    @FXML
    public void logout() {
        Session.fermer();
        SceneNavigator.goTo("/view/login.fxml", "Gestion des fermes - Connexion");
    }

    private void chargerVue(String cheminFxml) {
        try {
            Node vue = SceneNavigator.load(cheminFxml);
            if (rootContentStack != null) {
                rootContentStack.getChildren().setAll(vue);
            }
        } catch (Exception e) {
            AlertUtil.error("Impossible de charger la vue " + cheminFxml + " : " + e.getMessage());
        }
    }

    private void setActiveNav(Button activeBtn) {
        Button[] navs = {navDashboard, navOuvriers, navPaiement};
        for (Button btn : navs) {
            if (btn != null) {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4b5563; -fx-font-size: 13px; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 0 12;");
            }
        }
        if (activeBtn != null) {
            activeBtn.setStyle("-fx-background-color: #1b5e20; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 0 12;");
        }
    }
}