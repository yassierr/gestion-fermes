package ma.ferme.controller.rh;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ma.ferme.model.Ouvrier;
import ma.ferme.model.Pointage;
import ma.ferme.model.enums.StatutPointage;
import ma.ferme.security.Session;
import ma.ferme.service.OuvrierService;
import ma.ferme.service.PointageService;
import ma.ferme.util.AlertUtil;
import ma.ferme.util.Formats;
import ma.ferme.util.SceneNavigator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur de l'écran Ouvriers & Pointage ({@code /view/rh/ouvriers.fxml})
 * et du formulaire modal ({@code /view/rh/ouvrier-form.fxml}).
 */
public class OuvrierController {

    // Composants de ouvriers.fxml
    @FXML private TextField txtRecherche;
    @FXML private CheckBox chkActifsUniquement;
    @FXML private Label lblCompteur;
    @FXML private DatePicker dpDatePointage;
    @FXML private ComboBox<StatutPointage> cbStatutPointage;

    @FXML private TableView<Ouvrier> tableOuvriers;
    @FXML private TableColumn<Ouvrier, String> colMatricule;
    @FXML private TableColumn<Ouvrier, String> colCin;
    @FXML private TableColumn<Ouvrier, String> colNomPrenom;
    @FXML private TableColumn<Ouvrier, String> colTelephone;
    @FXML private TableColumn<Ouvrier, String> colSalaire;
    @FXML private TableColumn<Ouvrier, String> colPointageDate;
    @FXML private TableColumn<Ouvrier, String> colStatut;

    // Composants de ouvrier-form.fxml
    @FXML private TextField tfFormNomPrenom;
    @FXML private TextField tfFormCin;
    @FXML private TextField tfFormTelephone;
    @FXML private TextField tfFormAdresse;
    @FXML private TextField tfFormPoste;
    @FXML private TextField tfFormSalaire;

    private final OuvrierService ouvrierService = new OuvrierService();
    private final PointageService pointageService = new PointageService();

    private Integer idFerme;
    private List<Ouvrier> ouvriersCourants = List.of();
    private final Map<Integer, StatutPointage> pointagesDateCourante = new HashMap<>();

    @FXML
    public void initialize() {
        idFerme = Session.getIdFerme();
        if (idFerme == null) {
            AlertUtil.error("Aucune ferme associée à votre session.");
            return;
        }

        // Si le FXML chargé est ouvrier-form.fxml, les colonnes de tableOuvriers sont absentes
        if (tableOuvriers == null) {
            return;
        }

        dpDatePointage.setValue(LocalDate.now());
        cbStatutPointage.setItems(FXCollections.observableArrayList(StatutPointage.values()));
        cbStatutPointage.setValue(StatutPointage.PRESENT);

        configurerColonnes();

        chkActifsUniquement.selectedProperty().addListener((obs, a, b) -> chargerOuvriers());
        dpDatePointage.valueProperty().addListener((obs, a, b) -> chargerPointagesDate());

        chargerOuvriers();
    }

    private void configurerColonnes() {
        colMatricule.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMatricule()));
        colCin.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCin()));
        colNomPrenom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNomEtPrenom()));
        colTelephone.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTelephone() != null ? c.getValue().getTelephone() : "—"));
        colSalaire.setCellValueFactory(c -> new SimpleStringProperty(
                Formats.nombre(c.getValue().getSalaireJournalier())));
        colPointageDate.setCellValueFactory(c -> {
            StatutPointage st = pointagesDateCourante.get(c.getValue().getId());
            return new SimpleStringProperty(st == null ? "Non pointé"
                    : (st == StatutPointage.PRESENT ? "Présent" : "Absent"));
        });
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isActif() ? "Actif" : "Inactif"));
    }

    private void chargerOuvriers() {
        try {
            ouvriersCourants = chkActifsUniquement.isSelected()
                    ? ouvrierService.listerActifsParFerme(idFerme)
                    : ouvrierService.listerParFerme(idFerme);

            FilteredList<Ouvrier> filtree = new FilteredList<>(FXCollections.observableArrayList(ouvriersCourants), o -> true);
            txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
                String filtre = newVal == null ? "" : newVal.trim().toLowerCase();
                filtree.setPredicate(o -> filtre.isEmpty()
                        || o.getNomEtPrenom().toLowerCase().contains(filtre)
                        || o.getCin().toLowerCase().contains(filtre)
                        || (o.getMatricule() != null && o.getMatricule().toLowerCase().contains(filtre)));
                lblCompteur.setText(filtree.size() + (filtree.size() > 1 ? " ouvriers" : " ouvrier"));
            });

            tableOuvriers.setItems(filtree);
            lblCompteur.setText(filtree.size() + (filtree.size() > 1 ? " ouvriers" : " ouvrier"));
            chargerPointagesDate();
        } catch (RuntimeException e) {
            AlertUtil.error("Erreur lors du chargement des ouvriers : " + e.getMessage());
        }
    }

    private void chargerPointagesDate() {
        LocalDate date = dpDatePointage.getValue();
        pointagesDateCourante.clear();
        if (date != null) {
            for (Ouvrier o : ouvriersCourants) {
                List<Pointage> liste = pointageService.listerPointagesPeriode(o.getId(), date, date);
                if (!liste.isEmpty()) {
                    pointagesDateCourante.put(o.getId(), liste.get(0).getStatut());
                }
            }
        }
        tableOuvriers.refresh();
    }

    @FXML
    private void onAjouterOuvrier() {
        SceneNavigator.openModal("/view/rh/ouvrier-form.fxml", "Ajouter un ouvrier");
        chargerOuvriers();
    }

    @FXML
    private void onEnregistrerPointage() {
        Ouvrier selection = tableOuvriers.getSelectionModel().getSelectedItem();
        if (selection == null) {
            AlertUtil.info("Sélectionnez d'abord un ouvrier dans le tableau.");
            return;
        }
        LocalDate date = dpDatePointage.getValue();
        StatutPointage statut = cbStatutPointage.getValue();
        if (date == null || statut == null) {
            AlertUtil.error("Veuillez choisir une date et un statut de pointage.");
            return;
        }
        try {
            pointageService.pointerOuvrier(selection.getId(), date, statut);
            chargerPointagesDate();
            AlertUtil.info("Pointage enregistré pour " + selection.getNomEtPrenom() + ".");
        } catch (RuntimeException e) {
            AlertUtil.error(e.getMessage());
        }
    }

    @FXML
    private void onBasculerStatut() {
        Ouvrier selection = tableOuvriers.getSelectionModel().getSelectedItem();
        if (selection == null) {
            AlertUtil.info("Sélectionnez d'abord un ouvrier.");
            return;
        }
        try {
            if (selection.isActif()) {
                ouvrierService.desactiverOuvrier(selection.getId());
            } else {
                ouvrierService.reactiverOuvrier(selection.getId());
            }
            chargerOuvriers();
        } catch (RuntimeException e) {
            AlertUtil.error(e.getMessage());
        }
    }

    @FXML
    private void onSupprimerOuvrier() {
        Ouvrier selection = tableOuvriers.getSelectionModel().getSelectedItem();
        if (selection == null) {
            AlertUtil.info("Sélectionnez d'abord un ouvrier.");
            return;
        }
        if (!AlertUtil.confirm("Supprimer l'ouvrier « " + selection.getNomEtPrenom() + " » ?")) {
            return;
        }
        try {
            ouvrierService.supprimerOuvrier(selection.getId());
            chargerOuvriers();
        } catch (RuntimeException e) {
            AlertUtil.error(e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Actions du formulaire modal (ouvrier-form.fxml)
    // ------------------------------------------------------------------

    @FXML
    private void onValiderCreationOuvrier() {
        try {
            BigDecimal salaire = Formats.lireNombre(tfFormSalaire.getText());
            String adresse = (tfFormAdresse != null && !tfFormAdresse.getText().isBlank())
                    ? tfFormAdresse.getText().trim() : "Non renseignée";
            String poste = (tfFormPoste != null && !tfFormPoste.getText().isBlank())
                    ? tfFormPoste.getText().trim() : "Ouvrier agricole";

            Ouvrier nouveau = new Ouvrier(
                    Session.getIdFerme(),
                    tfFormNomPrenom.getText(),
                    tfFormCin.getText(),
                    tfFormTelephone.getText(),
                    adresse,
                    poste,
                    salaire
            );
            ouvrierService.ajouterOuvrier(nouveau);
            fermerModal();
        } catch (RuntimeException e) {
            AlertUtil.error(e.getMessage());
        }
    }

    @FXML
    private void onAnnulerModal() {
        fermerModal();
    }

    private void fermerModal() {
        ((Stage) tfFormNomPrenom.getScene().getWindow()).close();
    }
}