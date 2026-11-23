package ma.ferme.controller;

import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import ma.ferme.dao.DaoException;
import ma.ferme.model.Utilisateur;
import ma.ferme.security.Session;
import ma.ferme.service.AuthService;
import ma.ferme.service.AuthService.AuthException;
import ma.ferme.util.AlertUtil;
import ma.ferme.util.SceneNavigator;

/**
 * Contrôleur de l'écran de connexion ({@code /view/login.fxml}).
 * <p>
 * Flux : FXML → {@code LoginController} → {@link AuthService} → DAO. Le contrôleur ne touche
 * jamais au DAO. Après une connexion réussie, il ouvre la {@link Session} puis affiche l'écran
 * principal du rôle avec {@link SceneNavigator#goTo(String, String)}.
 * </p>
 */
public class LoginController {

    private static final Logger LOG = Logger.getLogger(LoginController.class.getName());

    private static final String FXML_ADMIN = "/view/admin/admin-main.fxml";
    private static final String FXML_RESPONSABLE = "/view/responsable/responsable-main.fxml";
    private static final String FXML_RH = "/view/rh/rh-main.fxml";

    @FXML private TextField txtLogin;
    @FXML private PasswordField pfMotDePasse;
    @FXML private TextField txtMotDePasseVisible;
    @FXML private Button btnAfficher;
    @FXML private CheckBox chkSouvenir;
    @FXML private Hyperlink lienOublie;
    @FXML private Label lblErreur;
    @FXML private Button btnConnexion;

    private final AuthService authService;

    /** Constructeur sans argument, requis par FXMLLoader. */
    public LoginController() {
        this(new AuthService());
    }

    /**
     * Constructeur avec service injecté (utile pour les tests).
     *
     * @param authService le service d'authentification
     */
    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    /** Appelée par JavaFX après le chargement du FXML. */
    @FXML
    private void initialize() {
        // Les deux champs de mot de passe partagent le même texte : il suffit d'échanger leur visibilité.
        txtMotDePasseVisible.textProperty().bindBidirectional(pfMotDePasse.textProperty());

        // L'erreur disparaît dès que l'utilisateur corrige sa saisie.
        txtLogin.textProperty().addListener((obs, ancien, nouveau) -> masquerErreur());
        pfMotDePasse.textProperty().addListener((obs, ancien, nouveau) -> masquerErreur());

        Platform.runLater(() -> txtLogin.requestFocus());
    }

    /** Clic sur « Se connecter » (ou touche Entrée : le bouton est le bouton par défaut). */
    @FXML
    private void onConnexion() {
        masquerErreur();
        btnConnexion.setDisable(true);
        try {
            Utilisateur utilisateur = authService.authentifier(txtLogin.getText(), pfMotDePasse.getText());
            ouvrirEcranPrincipal(utilisateur);
        } catch (AuthException e) {
            // clear() d'abord : il déclenche le listener qui masque l'erreur.
            pfMotDePasse.clear();
            afficherErreur(e.getMessage());
            pfMotDePasse.requestFocus();
        } catch (DaoException e) {
            LOG.log(Level.SEVERE, "Erreur d'accès à la base pendant la connexion", e);
            afficherErreur("Impossible de joindre la base de données. Vérifiez qu'elle est démarrée.");
        } finally {
            btnConnexion.setDisable(false);
        }
    }

    /** Clic sur « Afficher » / « Masquer » : bascule entre points et texte clair. */
    @FXML
    private void onBasculerMotDePasse() {
        boolean etaitVisible = txtMotDePasseVisible.isVisible();

        txtMotDePasseVisible.setVisible(!etaitVisible);
        txtMotDePasseVisible.setManaged(!etaitVisible);
        pfMotDePasse.setVisible(etaitVisible);
        pfMotDePasse.setManaged(etaitVisible);
        btnAfficher.setText(etaitVisible ? "Afficher" : "Masquer");

        TextField actif = etaitVisible ? pfMotDePasse : txtMotDePasseVisible;
        actif.requestFocus();
        actif.positionCaret(actif.getText().length());
    }

    /** Clic sur « Mot de passe oublié ? » : pas de réinitialisation en libre-service dans le cahier des charges. */
    @FXML
    private void onMotDePasseOublie() {
        AlertUtil.info("Contactez l'administrateur de l'entreprise pour réinitialiser votre mot de passe.");
    }

    // ------------------------------------------------------------------
    // Navigation après connexion
    // ------------------------------------------------------------------

    /**
     * Ouvre la session puis remplace l'écran de connexion par l'écran principal du rôle.
     * Si cet écran est introuvable (module pas encore livré) ou ne se charge pas, la session
     * est refermée et l'utilisateur reste sur la page de connexion avec un message.
     */
    private void ouvrirEcranPrincipal(Utilisateur utilisateur) {
        String chemin;
        String titre;
        switch (utilisateur.getRole()) {
            case ADMIN -> {
                chemin = FXML_ADMIN;
                titre = "Gestion des fermes - Administrateur";
            }
            case RESPONSABLE -> {
                chemin = FXML_RESPONSABLE;
                titre = "Gestion des fermes - Responsable de ferme";
            }
            case AGENT_RH -> {
                chemin = FXML_RH;
                titre = "Gestion des fermes - Agent RH";
            }
            default -> throw new IllegalStateException("Rôle inconnu : " + utilisateur.getRole());
        }

        // La session doit exister avant le chargement : les contrôleurs des écrans suivants la lisent.
        Session.ouvrir(utilisateur.getId(), utilisateur.getNomComplet(),
                utilisateur.getRole().name(), utilisateur.getIdFerme());
        try {
            SceneNavigator.goTo(chemin, titre);
        } catch (IllegalStateException e) {
            // SceneNavigator.load lève IllegalStateException si le FXML manque ou ne se charge pas.
            Session.fermer();
            LOG.log(Level.SEVERE, "Impossible d'ouvrir " + chemin, e);
            afficherErreur("L'écran « " + utilisateur.getRole().getLibelle()
                    + " » n'est pas disponible pour le moment (" + chemin + ").");
        }
    }

    // ------------------------------------------------------------------
    // Message d'erreur
    // ------------------------------------------------------------------

    private void afficherErreur(String message) {
        lblErreur.setText(message);
        lblErreur.setVisible(true);
        lblErreur.setManaged(true);
    }

    private void masquerErreur() {
        lblErreur.setVisible(false);
        lblErreur.setManaged(false);
    }
}
