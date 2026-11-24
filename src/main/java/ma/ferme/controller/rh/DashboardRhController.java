package ma.ferme.controller.rh;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ma.ferme.model.Ouvrier;
import ma.ferme.model.Pointage;
import ma.ferme.model.enums.StatutPointage;
import ma.ferme.security.Session;
import ma.ferme.service.OuvrierService;
import ma.ferme.service.PaieService;
import ma.ferme.service.PaieService.ResumeRecu;
import ma.ferme.service.PointageService;
import ma.ferme.util.AlertUtil;
import ma.ferme.util.Formats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur du tableau de bord RH ({@code /view/rh/dashboard-rh.fxml}).
 */
public class DashboardRhController {

    private record LigneSuiviOuvrier(
            Ouvrier ouvrier,
            String statutJour,
            int joursImpayes,
            BigDecimal soldeDu
    ) {}

    @FXML private Label lblEffectifActif;
    @FXML private Label lblPresentsJour;
    @FXML private Label lblSoldeImpaye;
    @FXML private Label lblMasseSalariale;
    @FXML private Label lblDateJour;

    @FXML private TableView<LigneSuiviOuvrier> tablePointageJour;
    @FXML private TableColumn<LigneSuiviOuvrier, String> colMatricule;
    @FXML private TableColumn<LigneSuiviOuvrier, String> colNom;
    @FXML private TableColumn<LigneSuiviOuvrier, String> colStatutJour;
    @FXML private TableColumn<LigneSuiviOuvrier, String> colJoursImpayes;
    @FXML private TableColumn<LigneSuiviOuvrier, String> colSolde;

    @FXML private TableView<ResumeRecu> tableDerniersRecus;
    @FXML private TableColumn<ResumeRecu, String> colRecuNumero;
    @FXML private TableColumn<ResumeRecu, String> colRecuDate;
    @FXML private TableColumn<ResumeRecu, String> colRecuPeriode;
    @FXML private TableColumn<ResumeRecu, String> colRecuNbOuvriers;
    @FXML private TableColumn<ResumeRecu, String> colRecuTotal;

    private final OuvrierService ouvrierService = new OuvrierService();
    private final PointageService pointageService = new PointageService();
    private final PaieService paieService = new PaieService();

    private Integer idFerme;

    @FXML
    public void initialize() {
        idFerme = Session.getIdFerme();
        if (idFerme == null) {
            AlertUtil.error("Aucune ferme associée à votre session Agent RH.");
            return;
        }

        configurerColonnes();
        chargerDonnees();
    }

    @FXML
    private void onActualiser() {
        chargerDonnees();
    }

    private void configurerColonnes() {
        colMatricule.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().ouvrier().getMatricule()));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().ouvrier().getNomEtPrenom()));
        colStatutJour.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().statutJour()));
        colJoursImpayes.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().joursImpayes())));
        colSolde.setCellValueFactory(c -> new SimpleStringProperty(Formats.nombre(c.getValue().soldeDu())));

        colRecuNumero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().numero()));
        colRecuDate.setCellValueFactory(c -> new SimpleStringProperty(Formats.date(c.getValue().datePaiement())));
        colRecuPeriode.setCellValueFactory(c -> new SimpleStringProperty(
                Formats.date(c.getValue().periodeDebut()) + " au " + Formats.date(c.getValue().periodeFin())));
        colRecuNbOuvriers.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().nbOuvriers())));
        colRecuTotal.setCellValueFactory(c -> new SimpleStringProperty(Formats.nombre(c.getValue().total())));
    }

    private void chargerDonnees() {
        try {
            LocalDate aujourdHui = LocalDate.now();
            lblDateJour.setText("Suivi du jour (" + Formats.date(aujourdHui) + ")");

            List<Ouvrier> actifs = ouvrierService.listerActifsParFerme(idFerme);
            lblEffectifActif.setText(actifs.size() + (actifs.size() > 1 ? " ouvriers" : " ouvrier"));

            int presents = 0;
            BigDecimal totalImpaye = BigDecimal.ZERO;
            List<LigneSuiviOuvrier> lignes = new ArrayList<>();

            for (Ouvrier o : actifs) {
                List<Pointage> pointagesDuJour = pointageService.listerPointagesPeriode(o.getId(), aujourdHui, aujourdHui);
                StatutPointage st = pointagesDuJour.isEmpty() ? null : pointagesDuJour.get(0).getStatut();
                if (st == StatutPointage.PRESENT) {
                    presents++;
                }
                String libelleStatut = st == null ? "Non pointé"
                        : (st == StatutPointage.PRESENT ? "Présent" : "Absent");

                BigDecimal soldeDu = paieService.soldeOuvrier(o.getId());
                int joursImpayes = (o.getSalaireJournalier() != null && o.getSalaireJournalier().signum() > 0)
                        ? soldeDu.divide(o.getSalaireJournalier(), 0, RoundingMode.HALF_UP).intValue()
                        : 0;

                totalImpaye = totalImpaye.add(soldeDu);
                lignes.add(new LigneSuiviOuvrier(o, libelleStatut, joursImpayes, soldeDu));
            }

            lblPresentsJour.setText(presents + " / " + actifs.size());
            lblSoldeImpaye.setText(Formats.mad(totalImpaye));
            tablePointageJour.setItems(FXCollections.observableArrayList(lignes));

            List<ResumeRecu> recus = paieService.listerRecus(idFerme);
            BigDecimal masseSalariale = recus.stream()
                    .map(ResumeRecu::total)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            lblMasseSalariale.setText(Formats.mad(masseSalariale));
            tableDerniersRecus.setItems(FXCollections.observableArrayList(recus));

        } catch (RuntimeException e) {
            AlertUtil.error("Erreur lors du chargement du tableau de bord RH : " + e.getMessage());
        }
    }
}