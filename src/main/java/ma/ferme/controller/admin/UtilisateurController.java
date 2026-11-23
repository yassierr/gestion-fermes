package ma.ferme.controller.admin;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import ma.ferme.dao.DaoException;
import ma.ferme.dao.FermeDao;
import ma.ferme.model.Ferme;
import ma.ferme.model.Utilisateur;
import ma.ferme.model.enums.RoleUtilisateur;
import ma.ferme.security.Session;
import ma.ferme.service.FermeService;
import ma.ferme.service.UtilisateurService;
import ma.ferme.util.AlertUtil;

/**
 * Contrôleur de l'écran « Gestion des Utilisateurs » ({@code /view/admin/utilisateurs.fxml}).
 * <p>
 * L'écran ne contient que la zone de contenu : la barre latérale et la barre du haut appartiennent à
 * {@code admin-main.fxml}, qui charge cet écran dans sa zone centrale.
 * </p>
 * Fonctions (EF-A07 à EF-A09, EF-T06) : compteurs, recherche par nom ou login, filtres rôle et ferme,
 * pagination, création et modification de compte (formulaire modal), activation et désactivation.
 * Toutes les règles et les droits sont dans {@link UtilisateurService} ; ce contrôleur ne fait que
 * l'affichage et affiche les messages d'erreur du service.
 */
public class UtilisateurController {

    private static final Logger LOG = Logger.getLogger(UtilisateurController.class.getName());

    private static final String FXML_FORMULAIRE = "/view/admin/utilisateur-form.fxml";
    private static final int TAILLE_PAGE = 10;
    private static final String TOUS_LES_ROLES = "Tous les rôles";
    /** Élément « neutre » de la liste des fermes (identifiant 0 = pas de filtre). */
    private static final Ferme TOUTES_LES_FERMES = new Ferme(0, "Toutes les fermes", "");

    @FXML private Button btnCreer;
    @FXML private Label lblTotal;
    @FXML private Label lblResponsables;
    @FXML private Label lblAgents;
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> cbRole;
    @FXML private ComboBox<Ferme> cbFerme;
    @FXML private Button btnReinitialiser;
    @FXML private TableView<Utilisateur> table;
    @FXML private TableColumn<Utilisateur, String> colNom;
    @FXML private TableColumn<Utilisateur, String> colLogin;
    @FXML private TableColumn<Utilisateur, Utilisateur> colRole;
    @FXML private TableColumn<Utilisateur, String> colFerme;
    @FXML private TableColumn<Utilisateur, Utilisateur> colStatut;
    @FXML private TableColumn<Utilisateur, Utilisateur> colActions;
    @FXML private Label lblPagination;
    @FXML private Button btnPrecedent;
    @FXML private Label lblPage;
    @FXML private Button btnSuivant;

    private final UtilisateurService utilisateurService;
    private final FermeService fermeService;
    private final PauseTransition attenteRecherche = new PauseTransition(Duration.millis(250));

    private Utilisateur acteur;
    private List<Ferme> fermes = List.of();
    private final Map<Integer, String> nomsFermes = new HashMap<>();
    private List<Utilisateur> resultat = List.of();
    private int page;
    /** Vrai pendant qu'on modifie les filtres par code : évite un rechargement par changement. */
    private boolean filtresEnCoursDeModification;

    /** Constructeur sans argument, requis par FXMLLoader. */
    public UtilisateurController() {
        // FermeService n'a pas encore de constructeur par défaut (contrat du projet) : à simplifier ensuite.
        this(new UtilisateurService(), new FermeService(new FermeDao()));
    }

    /**
     * Constructeur avec services injectés (utile pour les tests).
     *
     * @param utilisateurService le service des comptes
     * @param fermeService       le service des fermes
     */
    public UtilisateurController(UtilisateurService utilisateurService, FermeService fermeService) {
        this.utilisateurService = utilisateurService;
        this.fermeService = fermeService;
    }

    /** Appelée par JavaFX après le chargement du FXML. */
    @FXML
    private void initialize() {
        configurerFiltres();
        configurerTable();

        if (!Session.estConnecte() || !"ADMIN".equals(Session.getRole())) {
            // ENF-02 : le service refuserait de toute façon ; on évite aussi d'afficher l'écran.
            table.setPlaceholder(new Label("Accès réservé à l'administrateur."));
            btnCreer.setDisable(true);
            txtRecherche.setDisable(true);
            cbRole.setDisable(true);
            cbFerme.setDisable(true);
            btnReinitialiser.setDisable(true);
            return;
        }
        acteur = acteurCourant();
        gerer(() -> {
            chargerFermes();
            recharger();
        });
    }

    // ------------------------------------------------------------------
    // Actions de l'écran
    // ------------------------------------------------------------------

    /** Clic sur « + Créer un compte ». */
    @FXML
    private void onCreerCompte() {
        ouvrirFormulaire(null);
    }

    /** Clic sur le bouton de réinitialisation des filtres. */
    @FXML
    private void onReinitialiser() {
        filtresEnCoursDeModification = true;
        txtRecherche.clear();
        cbRole.getSelectionModel().selectFirst();
        cbFerme.getSelectionModel().selectFirst();
        filtresEnCoursDeModification = false;
        page = 0;
        gerer(this::recharger);
    }

    @FXML
    private void onPagePrecedente() {
        if (page > 0) {
            page--;
            afficherPage();
        }
    }

    @FXML
    private void onPageSuivante() {
        if ((page + 1) * TAILLE_PAGE < resultat.size()) {
            page++;
            afficherPage();
        }
    }

    private void onModifier(Utilisateur utilisateur) {
        ouvrirFormulaire(utilisateur);
    }

    private void onChangerEtat(Utilisateur utilisateur) {
        boolean desactiver = utilisateur.isActif();
        String question = desactiver
                ? "Désactiver le compte de « " + utilisateur.getNomComplet()
                        + " » ? Cette personne ne pourra plus se connecter."
                : "Réactiver le compte de « " + utilisateur.getNomComplet() + " » ?";
        if (!AlertUtil.confirm(question)) {
            return;
        }
        gerer(() -> {
            if (desactiver) {
                utilisateurService.desactiver(acteur, utilisateur.getId());
            } else {
                utilisateurService.activer(acteur, utilisateur.getId());
            }
            recharger();
        });
    }

    // ------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------

    private void configurerFiltres() {
        cbRole.getItems().setAll(TOUS_LES_ROLES, RoleUtilisateur.RESPONSABLE.name(), RoleUtilisateur.AGENT_RH.name());
        cbRole.getSelectionModel().selectFirst();

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

        // Recherche avec un court délai : une requête par pause de frappe, pas une par lettre.
        attenteRecherche.setOnFinished(e -> {
            page = 0;
            gerer(this::recharger);
        });
        txtRecherche.textProperty().addListener((obs, ancien, nouveau) -> {
            if (!filtresEnCoursDeModification) {
                attenteRecherche.playFromStart();
            }
        });
        cbRole.valueProperty().addListener((obs, ancien, nouveau) -> filtreModifie());
        cbFerme.valueProperty().addListener((obs, ancien, nouveau) -> filtreModifie());
    }

    private void filtreModifie() {
        if (!filtresEnCoursDeModification && acteur != null) {
            page = 0;
            gerer(this::recharger);
        }
    }

    private void configurerTable() {
        table.setFixedCellSize(52);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        // La table prend exactement la hauteur de ses lignes (pas de défilement interne).
        table.prefHeightProperty().bind(table.fixedCellSizeProperty()
                .multiply(Bindings.size(table.getItems()).add(1.15)));
        table.setPlaceholder(new Label("Aucun compte ne correspond à la recherche."));

        colNom.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getNomComplet()));
        colLogin.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getLogin()));
        colFerme.setCellValueFactory(cd -> new ReadOnlyStringWrapper(nomFerme(cd.getValue())));
        colRole.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        colStatut.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        colActions.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));

        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Utilisateur u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(badge(u.getRole().name(), classeBadgeRole(u.getRole())));
            }
        });

        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Utilisateur u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(u.isActif()
                        ? badge("Actif", "usr-badge-actif")
                        : badge("Désactivé", "usr-badge-inactif"));
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnModifier = new Button("Modifier");
            private final Button btnEtat = new Button();
            private final HBox boite = new HBox(8, btnModifier, btnEtat);

            {
                boite.setAlignment(Pos.CENTER_RIGHT);
                btnModifier.getStyleClass().add("usr-btn-action");
                btnModifier.setOnAction(e -> onModifier(getItem()));
                btnEtat.setOnAction(e -> onChangerEtat(getItem()));
            }

            @Override
            protected void updateItem(Utilisateur u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setGraphic(null);
                    return;
                }
                btnEtat.setText(u.isActif() ? "Désactiver" : "Activer");
                btnEtat.getStyleClass().setAll("button", u.isActif() ? "usr-btn-danger" : "usr-btn-ok");
                // L'administrateur ne peut pas désactiver son propre compte (règle du service).
                boolean propreCompte = acteur != null && u.getId() == acteur.getId();
                btnEtat.setVisible(!propreCompte);
                btnEtat.setManaged(!propreCompte);
                setGraphic(boite);
            }
        });
    }

    // ------------------------------------------------------------------
    // Chargement et affichage
    // ------------------------------------------------------------------

    private void chargerFermes() {
        fermes = fermeService.obtenirToutesLesFermes();
        nomsFermes.clear();
        fermes.forEach(f -> nomsFermes.put(f.getId(), f.getNom()));

        filtresEnCoursDeModification = true;
        cbFerme.getItems().setAll(TOUTES_LES_FERMES);
        cbFerme.getItems().addAll(fermes);
        cbFerme.getSelectionModel().selectFirst();
        filtresEnCoursDeModification = false;
    }

    private void recharger() {
        resultat = utilisateurService.rechercher(acteur, txtRecherche.getText(), roleFiltre(), idFermeFiltre());

        long total = utilisateurService.compterTous(acteur);
        long responsables = utilisateurService.compterParRole(acteur, RoleUtilisateur.RESPONSABLE);
        long agents = utilisateurService.compterParRole(acteur, RoleUtilisateur.AGENT_RH);
        lblTotal.setText(total + (total > 1 ? " comptes" : " compte"));
        lblResponsables.setText(responsables + (responsables > 1 ? " responsables" : " responsable"));
        lblAgents.setText(agents + (agents > 1 ? " agents RH" : " agent RH"));

        afficherPage();
    }

    private void afficherPage() {
        int total = resultat.size();
        int nbPages = Math.max(1, (total + TAILLE_PAGE - 1) / TAILLE_PAGE);
        page = Math.min(page, nbPages - 1);
        int debut = page * TAILLE_PAGE;
        int fin = Math.min(debut + TAILLE_PAGE, total);

        // setAll (et non setItems) : la hauteur de la table est liée à cette liste précise.
        table.getItems().setAll(resultat.subList(debut, fin));
        lblPagination.setText(total == 0
                ? "Aucun compte"
                : "Affichage de " + (debut + 1) + " à " + fin + " sur " + total + " comptes");
        lblPage.setText("Page " + (page + 1) + " / " + nbPages);
        btnPrecedent.setDisable(page == 0);
        btnSuivant.setDisable(page >= nbPages - 1);
    }

    private RoleUtilisateur roleFiltre() {
        String valeur = cbRole.getValue();
        return (valeur == null || TOUS_LES_ROLES.equals(valeur)) ? null : RoleUtilisateur.valueOf(valeur);
    }

    private Integer idFermeFiltre() {
        Ferme ferme = cbFerme.getValue();
        return (ferme == null || ferme.getId() == 0) ? null : ferme.getId();
    }

    private String nomFerme(Utilisateur utilisateur) {
        Integer idFerme = utilisateur.getIdFerme();
        if (idFerme == null) {
            return "—";
        }
        return nomsFermes.getOrDefault(idFerme, "Ferme n° " + idFerme);
    }

    private static Label badge(String texte, String classe) {
        Label etiquette = new Label(texte);
        etiquette.getStyleClass().addAll("usr-badge", classe);
        return etiquette;
    }

    private static String classeBadgeRole(RoleUtilisateur role) {
        return switch (role) {
            case ADMIN -> "usr-badge-admin";
            case RESPONSABLE -> "usr-badge-resp";
            case AGENT_RH -> "usr-badge-rh";
        };
    }

    // ------------------------------------------------------------------
    // Formulaire modal
    // ------------------------------------------------------------------

    /**
     * Ouvre le formulaire en fenêtre modale, puis recharge la liste à sa fermeture.
     * On charge le FXML avec un FXMLLoader (et non {@code SceneNavigator.openModal}) pour pouvoir
     * transmettre les données au contrôleur du formulaire ; la fenêtre est configurée de la même façon.
     */
    private void ouvrirFormulaire(Utilisateur aModifier) {
        URL fxml = getClass().getResource(FXML_FORMULAIRE);
        if (fxml == null) {
            AlertUtil.error("Formulaire introuvable : " + FXML_FORMULAIRE);
            return;
        }
        try {
            FXMLLoader chargeur = new FXMLLoader(fxml);
            Parent racine = chargeur.load();
            UtilisateurFormController formulaire = chargeur.getController();
            formulaire.init(utilisateurService, acteur, fermes, aModifier);

            Stage fenetre = new Stage();
            fenetre.initOwner(table.getScene().getWindow());
            fenetre.initModality(Modality.APPLICATION_MODAL);
            fenetre.setTitle(aModifier == null ? "Créer un compte utilisateur" : "Modifier le compte");
            fenetre.setResizable(false);
            fenetre.setScene(new Scene(racine));
            fenetre.showAndWait();

            gerer(this::recharger);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Impossible d'ouvrir " + FXML_FORMULAIRE, e);
            AlertUtil.error("Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    /**
     * Reconstruit l'utilisateur connecté à partir de la {@link Session}, qui ne conserve que son
     * identifiant, son nom, son rôle et sa ferme. Suffisant pour les droits (rôle) et pour la règle
     * « on ne désactive pas son propre compte » (identifiant).
     */
    private static Utilisateur acteurCourant() {
        return Utilisateur.creer(Session.getIdUtilisateur(), Session.getNomComplet(), "session", null,
                RoleUtilisateur.valueOf(Session.getRole()), true, Session.getIdFerme());
    }

    /** Exécute une action et affiche proprement les erreurs du service ou de la base. */
    private void gerer(Runnable action) {
        try {
            action.run();
        } catch (SecurityException | IllegalArgumentException e) {
            erreur(e.getMessage());
        } catch (DaoException e) {
            LOG.log(Level.SEVERE, "Erreur d'accès à la base", e);
            erreur("Erreur d'accès à la base de données : " + e.getMessage());
        }
    }

    /** Affichage différé : évite d'ouvrir une boîte de dialogue pendant le chargement du FXML. */
    private static void erreur(String message) {
        Platform.runLater(() -> AlertUtil.error(message));
    }
}