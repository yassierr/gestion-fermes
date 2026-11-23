package ma.ferme.controller.admin;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import ma.ferme.dao.DaoException;
import ma.ferme.model.Ferme;
import ma.ferme.model.Utilisateur;
import ma.ferme.model.enums.RoleUtilisateur;
import ma.ferme.service.UtilisateurService;
import ma.ferme.util.AlertUtil;

/**
 * Contrôleur du formulaire {@code /view/admin/utilisateur-form.fxml}, ouvert en fenêtre modale par
 * {@link UtilisateurController}. Il sert à la fois à créer un compte (EF-A08) et à en modifier un (EF-A09).
 * <p>
 * Le formulaire ne contient aucune règle métier : il transmet la saisie à {@link UtilisateurService},
 * qui valide et lève {@link IllegalArgumentException} avec un message affiché sous les champs.
 * Le contrôleur parent doit appeler {@link #init} juste après le chargement du FXML.
 * </p>
 */
public class UtilisateurFormController {

    private static final Logger LOG = Logger.getLogger(UtilisateurFormController.class.getName());

    @FXML private Label lblTitre;
    @FXML private Label lblErreur;
    @FXML private TextField txtNomComplet;
    @FXML private TextField txtLogin;
    @FXML private VBox boxMotDePasse;
    @FXML private PasswordField pfMotDePasse;
    @FXML private TextField txtMotDePasseVisible;
    @FXML private Button btnAfficher;
    @FXML private ComboBox<RoleUtilisateur> cbRole;
    @FXML private ComboBox<Ferme> cbFerme;
    @FXML private Button btnValider;

    private UtilisateurService service;
    private Utilisateur acteur;
    private Utilisateur aModifier;

    /** Appelée par JavaFX après le chargement du FXML : configuration qui ne dépend pas du contexte. */
    @FXML
    private void initialize() {
        // Les deux champs de mot de passe partagent le même texte : il suffit d'échanger leur visibilité.
        txtMotDePasseVisible.textProperty().bindBidirectional(pfMotDePasse.textProperty());

        cbFerme.setConverter(new StringConverter<>() {
            @Override
            public String toString(Ferme ferme) {
                return ferme == null ? "" : ferme.getNom();
            }

            @Override
            public Ferme fromString(String texte) {
                return null; // liste non éditable
            }
        });
        masquerErreur();
    }

    /**
     * Prépare le formulaire. À appeler une seule fois, avant d'afficher la fenêtre.
     *
     * @param service   le service des comptes
     * @param acteur    l'administrateur connecté
     * @param fermes    les fermes proposées dans la liste « Ferme à affecter »
     * @param aModifier le compte à modifier, ou {@code null} pour en créer un
     */
    public void init(UtilisateurService service, Utilisateur acteur, List<Ferme> fermes, Utilisateur aModifier) {
        this.service = service;
        this.acteur = acteur;
        this.aModifier = aModifier;
        cbFerme.getItems().setAll(fermes);

        if (aModifier == null) {
            lblTitre.setText("Créer un compte utilisateur");
            btnValider.setText("Créer le compte");
            cbRole.getItems().setAll(RoleUtilisateur.RESPONSABLE, RoleUtilisateur.AGENT_RH);
            cbRole.setValue(RoleUtilisateur.RESPONSABLE);
        } else {
            lblTitre.setText("Modifier le compte");
            btnValider.setText("Enregistrer les modifications");
            txtNomComplet.setText(aModifier.getNomComplet());
            txtLogin.setText(aModifier.getLogin());
            // Le mot de passe et le rôle ne se modifient pas ici.
            boxMotDePasse.setVisible(false);
            boxMotDePasse.setManaged(false);
            cbRole.getItems().setAll(aModifier.getRole());
            cbRole.setValue(aModifier.getRole());
            cbRole.setDisable(true);
            if (aModifier.getRole().isRattacheAFerme()) {
                fermes.stream()
                        .filter(f -> aModifier.getIdFerme() != null && f.getId() == aModifier.getIdFerme())
                        .findFirst()
                        .ifPresent(cbFerme::setValue);
            } else {
                cbFerme.setDisable(true); // l'administrateur n'a pas de ferme (RG-10)
            }
        }
    }

    /** Clic sur « Créer le compte » / « Enregistrer les modifications ». */
    @FXML
    private void onValider() {
        masquerErreur();
        try {
            Ferme ferme = cbFerme.getValue();
            Integer idFerme = ferme == null ? null : ferme.getId();
            if (aModifier == null) {
                service.creerCompte(acteur, txtNomComplet.getText(), txtLogin.getText(),
                        pfMotDePasse.getText(), cbRole.getValue(), idFerme);
            } else {
                service.modifierCompte(acteur, aModifier.getId(), txtNomComplet.getText(),
                        txtLogin.getText(), idFerme);
            }
            fermer();
        } catch (IllegalArgumentException e) {
            afficherErreur(e.getMessage());
        } catch (SecurityException e) {
            AlertUtil.error(e.getMessage());
            fermer();
        } catch (DaoException e) {
            LOG.log(Level.SEVERE, "Erreur d'accès à la base pendant l'enregistrement d'un compte", e);
            afficherErreur("Erreur d'accès à la base de données : " + e.getMessage());
        }
    }

    /** Clic sur « Annuler » (ou touche Échap). */
    @FXML
    private void onAnnuler() {
        fermer();
    }

    /** Clic sur l'œil : bascule entre points et texte clair. */
    @FXML
    private void onBasculerMotDePasse() {
        boolean etaitVisible = txtMotDePasseVisible.isVisible();
        txtMotDePasseVisible.setVisible(!etaitVisible);
        txtMotDePasseVisible.setManaged(!etaitVisible);
        pfMotDePasse.setVisible(etaitVisible);
        pfMotDePasse.setManaged(etaitVisible);
        btnAfficher.setText(etaitVisible ? "Afficher" : "Masquer");
    }

    private void fermer() {
        ((Stage) btnValider.getScene().getWindow()).close();
    }

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