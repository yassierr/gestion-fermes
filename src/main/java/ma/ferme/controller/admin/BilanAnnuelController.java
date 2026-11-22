package ma.ferme.controller.admin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ma.ferme.model.enums.RoleUtilisateur;
import ma.ferme.security.Session;
import ma.ferme.util.Formats;
import ma.ferme.util.SceneNavigator;

/**
 * Écran « Bilan annuel » de l'administrateur : pour une année choisie, chiffre d'affaires, gains
 * encaissés, créances, dépenses, masse salariale et bénéfice (RG-19), puis un tableau par ferme
 * avec le rendement (RG-20).
 * <p>
 * <b>État : l'affichage est prêt, les données ne sont pas encore branchées.</b> Les deux méthodes
 * {@link #anneesDisponibles()} et {@link #calculerBilan(int)} sont les seuls points à compléter :
 * elles doivent appeler la couche service/DAO dès que les requêtes par année et par ferme existent
 * (aujourd'hui {@code VenteDao} ne donne que le chiffre d'affaires, les gains et les créances
 * globaux, sans année ni ferme). Tant que {@link #DONNEES_BRANCHEES} vaut {@code false}, l'écran
 * affiche un avertissement et des zéros plutôt que de faux chiffres.
 * </p>
 *
 * @author Mohamed Amine Ait Maait
 * @version 1.0
 */
public class BilanAnnuelController {

    /** Passer à {@code true} quand {@link #calculerBilan(int)} lit de vraies données. */
    private static final boolean DONNEES_BRANCHEES = false;

    private static final String LOGIN = "/view/login.fxml";

    /**
     * Résultat d'une ferme pour l'année.
     *
     * @param nom        nom de la ferme
     * @param surfaceM2  surface totale des serres, en m²
     * @param recoltesKg récoltes de l'année, en kg
     * @param gains      gains encaissés, en MAD
     * @param depenses   dépenses + masse salariale, en MAD
     */
    public record LigneFerme(String nom, BigDecimal surfaceM2, BigDecimal recoltesKg,
                             BigDecimal gains, BigDecimal depenses) {

        /** @return le résultat de la ferme : gains − dépenses */
        public BigDecimal resultat() {
            return gains.subtract(depenses);
        }

        /** @return le rendement en kg/m² (RG-20), zéro si la ferme n'a pas de serre */
        public BigDecimal rendement() {
            if (surfaceM2.signum() == 0) {
                return BigDecimal.ZERO;
            }
            return recoltesKg.divide(surfaceM2, 2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Bilan d'une année pour toutes les fermes.
     *
     * @param chiffreAffaires livraisons livrées ou payées (RG-08)
     * @param gains           livraisons payées (RG-08)
     * @param creances        livraisons livrées mais non payées (RG-08)
     * @param depenses        dépenses des fermes
     * @param masseSalariale  reçus de paie
     * @param fermes          détail par ferme
     */
    public record Bilan(BigDecimal chiffreAffaires, BigDecimal gains, BigDecimal creances,
                        BigDecimal depenses, BigDecimal masseSalariale, List<LigneFerme> fermes) {

        /** @return le bénéfice : chiffre d'affaires − dépenses − masse salariale (RG-19) */
        public BigDecimal benefice() {
            return chiffreAffaires.subtract(depenses).subtract(masseSalariale);
        }
    }

    @FXML private ComboBox<Integer> cmbAnnee;
    @FXML private Label lblAvertissement;
    @FXML private Label lblChiffreAffaires;
    @FXML private Label lblGains;
    @FXML private Label lblCreances;
    @FXML private Label lblDepenses;
    @FXML private Label lblMasseSalariale;
    @FXML private Label lblBenefice;
    @FXML private TableView<LigneFerme> tableFermes;
    @FXML private TableColumn<LigneFerme, String> colFerme;
    @FXML private TableColumn<LigneFerme, String> colSurface;
    @FXML private TableColumn<LigneFerme, String> colRecoltes;
    @FXML private TableColumn<LigneFerme, String> colGains;
    @FXML private TableColumn<LigneFerme, String> colDepenses;
    @FXML private TableColumn<LigneFerme, String> colResultat;
    @FXML private TableColumn<LigneFerme, String> colRendement;

    /** Vérifie le rôle, prépare le tableau, puis affiche le bilan de l'année la plus récente. */
    @FXML
    private void initialize() {
        if (!Session.estConnecte() || !RoleUtilisateur.ADMIN.name().equals(Session.getRole())) {
            Platform.runLater(() -> SceneNavigator.goTo(LOGIN, "Gestion des fermes - Connexion"));
            return;
        }
        colFerme.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nom()));
        colSurface.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formats.nombre(c.getValue().surfaceM2()) + " m²"));
        colRecoltes.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formats.nombre(c.getValue().recoltesKg()) + " kg"));
        colGains.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formats.mad(c.getValue().gains())));
        colDepenses.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formats.mad(c.getValue().depenses())));
        colResultat.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formats.mad(c.getValue().resultat())));
        colRendement.setCellValueFactory(c -> new ReadOnlyStringWrapper(Formats.nombre(c.getValue().rendement()) + " kg/m²"));
        for (TableColumn<LigneFerme, String> colonne : List.of(colSurface, colRecoltes, colGains, colDepenses,
                colResultat, colRendement)) {
            colonne.setStyle("-fx-alignment: CENTER-RIGHT;");
        }

        lblAvertissement.setText("Les chiffres ne sont pas encore calculés : en attente des requêtes par année et par ferme.");
        lblAvertissement.setVisible(!DONNEES_BRANCHEES);
        lblAvertissement.setManaged(!DONNEES_BRANCHEES);

        cmbAnnee.setItems(FXCollections.observableArrayList(anneesDisponibles()));
        cmbAnnee.valueProperty().addListener((obs, ancienne, nouvelle) -> {
            if (nouvelle != null) {
                afficher(calculerBilan(nouvelle));
            }
        });
        if (!cmbAnnee.getItems().isEmpty()) {
            cmbAnnee.setValue(cmbAnnee.getItems().get(0));
        }
    }

    /**
     * POINT À BRANCHER : années ayant des données (récoltes, livraisons, dépenses ou paies),
     * de la plus récente à la plus ancienne.
     *
     * @return les années proposées dans la liste déroulante
     */
    private List<Integer> anneesDisponibles() {
        return List.of(Year.now().getValue());
    }

    /**
     * POINT À BRANCHER : calcule le bilan de l'année pour toutes les fermes (administrateur).
     * Doit passer par un service, jamais directement par un DAO.
     *
     * @param annee l'année choisie
     * @return le bilan ; ici des zéros tant que les données ne sont pas branchées
     */
    private Bilan calculerBilan(int annee) {
        return new Bilan(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, List.of());
    }

    private void afficher(Bilan bilan) {
        lblChiffreAffaires.setText(Formats.mad(bilan.chiffreAffaires()));
        lblGains.setText(Formats.mad(bilan.gains()));
        lblCreances.setText(Formats.mad(bilan.creances()));
        lblDepenses.setText(Formats.mad(bilan.depenses()));
        lblMasseSalariale.setText(Formats.mad(bilan.masseSalariale()));
        lblBenefice.setText(Formats.mad(bilan.benefice()));
        tableFermes.setItems(FXCollections.observableArrayList(bilan.fermes()));
    }
}