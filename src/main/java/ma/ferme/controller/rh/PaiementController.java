package ma.ferme.controller.rh;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import ma.ferme.dao.FermeDao;
import ma.ferme.model.Ferme;
import ma.ferme.model.Ouvrier;
import ma.ferme.security.Session;
import ma.ferme.service.FermeService;
import ma.ferme.service.OuvrierService;
import ma.ferme.service.PaieService;
import ma.ferme.service.PaieService.ResultatPaie;
import ma.ferme.service.PaieService.ResumeRecu;
import ma.ferme.util.AlertUtil;
import ma.ferme.util.Formats;
import ma.ferme.util.PdfUtil;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur de paiement des salaires et d'édition des reçus PDF ({@code /view/rh/paiement.fxml}).
 */
public class PaiementController {

    private record LigneApercuPaie(
            String matricule,
            String nomPrenom,
            BigDecimal salaireJour,
            int joursPayes,
            BigDecimal montant
    ) {}

    @FXML private DatePicker dpPeriodeDebut;
    @FXML private DatePicker dpPeriodeFin;
    @FXML private Label lblTotalAPayer;

    @FXML private TableView<LigneApercuPaie> tableApercu;
    @FXML private TableColumn<LigneApercuPaie, String> colApercuMatricule;
    @FXML private TableColumn<LigneApercuPaie, String> colApercuNom;
    @FXML private TableColumn<LigneApercuPaie, String> colApercuSalaire;
    @FXML private TableColumn<LigneApercuPaie, String> colApercuJours;
    @FXML private TableColumn<LigneApercuPaie, String> colApercuMontant;

    @FXML private TableView<ResumeRecu> tableRecus;
    @FXML private TableColumn<ResumeRecu, String> colRecuNumero;
    @FXML private TableColumn<ResumeRecu, String> colRecuDate;
    @FXML private TableColumn<ResumeRecu, String> colRecuPeriode;
    @FXML private TableColumn<ResumeRecu, String> colRecuOuvriers;
    @FXML private TableColumn<ResumeRecu, String> colRecuTotal;

    private final PaieService paieService = new PaieService();
    private final OuvrierService ouvrierService = new OuvrierService();
    private final FermeService fermeService = new FermeService(new FermeDao());

    private Integer idFerme;
    private String nomFerme = "Ferme";

    @FXML
    public void initialize() {
        idFerme = Session.getIdFerme();
        if (idFerme == null) {
            AlertUtil.error("Aucune ferme associée à votre session.");
            return;
        }

        nomFerme = fermeService.obtenirToutesLesFermes().stream()
                .filter(f -> f.getId() == idFerme)
                .map(Ferme::getNom)
                .findFirst()
                .orElse("Ferme #" + idFerme);

        dpPeriodeDebut.setValue(LocalDate.now().withDayOfMonth(1));
        dpPeriodeFin.setValue(LocalDate.now());

        configurerColonnes();
        chargerApercuSoldes();
        chargerHistoriqueRecus();
    }

    private void configurerColonnes() {
        colApercuMatricule.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().matricule()));
        colApercuNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nomPrenom()));
        colApercuSalaire.setCellValueFactory(c -> new SimpleStringProperty(Formats.nombre(c.getValue().salaireJour())));
        colApercuJours.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().joursPayes())));
        colApercuMontant.setCellValueFactory(c -> new SimpleStringProperty(Formats.nombre(c.getValue().montant())));

        colRecuNumero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().numero()));
        colRecuDate.setCellValueFactory(c -> new SimpleStringProperty(Formats.date(c.getValue().datePaiement())));
        colRecuPeriode.setCellValueFactory(c -> new SimpleStringProperty(
                Formats.date(c.getValue().periodeDebut()) + " au " + Formats.date(c.getValue().periodeFin())));
        colRecuOuvriers.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().nbOuvriers())));
        colRecuTotal.setCellValueFactory(c -> new SimpleStringProperty(Formats.nombre(c.getValue().total())));
    }

    @FXML
    private void onCalculerApercu() {
        chargerApercuSoldes();
    }

    private void chargerApercuSoldes() {
        try {
            List<Ouvrier> ouvriers = ouvrierService.listerParFerme(idFerme);
            List<LigneApercuPaie> lignes = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;

            for (Ouvrier o : ouvriers) {
                BigDecimal soldeDu = paieService.soldeOuvrier(o.getId());
                if (soldeDu.signum() > 0) {
                    int joursNonPayes = (o.getSalaireJournalier() != null && o.getSalaireJournalier().signum() > 0)
                            ? soldeDu.divide(o.getSalaireJournalier(), 0, RoundingMode.HALF_UP).intValue()
                            : 0;
                    lignes.add(new LigneApercuPaie(
                            o.getMatricule(),
                            o.getNomEtPrenom(),
                            o.getSalaireJournalier(),
                            joursNonPayes,
                            soldeDu
                    ));
                    total = total.add(soldeDu);
                }
            }

            tableApercu.setItems(FXCollections.observableArrayList(lignes));
            lblTotalAPayer.setText("Total à régler : " + Formats.mad(total));
        } catch (RuntimeException e) {
            AlertUtil.error("Erreur lors du calcul des soldes : " + e.getMessage());
        }
    }

    private void chargerHistoriqueRecus() {
        try {
            tableRecus.setItems(FXCollections.observableArrayList(paieService.listerRecus(idFerme)));
        } catch (RuntimeException e) {
            AlertUtil.error("Erreur lors du chargement des reçus : " + e.getMessage());
        }
    }

    @FXML
    private void onValiderPaiement() {
        LocalDate debut = dpPeriodeDebut.getValue();
        LocalDate fin = dpPeriodeFin.getValue();
        if (debut == null || fin == null) {
            AlertUtil.error("Veuillez renseigner la période de début et de fin.");
            return;
        }
        if (!AlertUtil.confirm("Confirmer le paiement des salaires pour la période du "
                + Formats.date(debut) + " au " + Formats.date(fin) + " ?")) {
            return;
        }

        try {
            ResultatPaie resultat = paieService.payerFerme(idFerme, debut, fin, Session.getIdUtilisateur());
            AlertUtil.info("Paiement validé ! Reçu n° " + resultat.numero()
                    + " (" + Formats.mad(resultat.total()) + ").");
            chargerApercuSoldes();
            chargerHistoriqueRecus();
        } catch (RuntimeException e) {
            AlertUtil.error(e.getMessage());
        }
    }

    @FXML
    private void onExporterRecuPdf() {
        ResumeRecu selection = tableRecus.getSelectionModel().getSelectedItem();
        if (selection == null) {
            AlertUtil.info("Sélectionnez d'abord un reçu dans l'historique.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le reçu de paie");
        chooser.setInitialFileName(selection.numero() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        File fichier = chooser.showSaveDialog(tableRecus.getScene().getWindow());
        if (fichier == null) {
            return;
        }

        try {
            ResultatPaie recuComplet = paieService.chargerRecuPdf(selection.idRecu())
                    .orElseThrow(() -> new IllegalStateException("Reçu introuvable."));
            PdfUtil.recuPaie(recuComplet, nomFerme, fichier.toPath());
            AlertUtil.info("Reçu PDF enregistré :\n" + fichier.getAbsolutePath());
        } catch (RuntimeException e) {
            AlertUtil.error("Erreur PDF : " + e.getMessage());
        }
    }
}