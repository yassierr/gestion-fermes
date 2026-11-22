
package ma.ferme.controller.responsable;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

import ma.ferme.dao.FermeDao;
import ma.ferme.model.Client;
import ma.ferme.model.Ferme;
import ma.ferme.model.LigneVente;
import ma.ferme.model.Vente;
import ma.ferme.model.enums.StatutVente;
import ma.ferme.security.Session;
import ma.ferme.service.ClientService;
import ma.ferme.service.FermeService;
import ma.ferme.service.PaieService;
import ma.ferme.service.PaieService.ResumeRecu;
import ma.ferme.service.VenteService;
import ma.ferme.util.AlertUtil;
import ma.ferme.util.Formats;
import ma.ferme.util.PdfUtil;

/**
 * Controller de l'écran Livraisons & Ouvriers.
 *
 * Le responsable ne voit que les livraisons et les reçus
 * de paie concernant sa ferme.
 */
public class LivraisonOuvrierController {

    /**
     * Résumé d'une livraison pour la ferme du responsable.
     */
    private record LivraisonFerme(
            Vente vente,
            BigDecimal quantite,
            BigDecimal montant
    ) {

        /**
         * Calcule le prix moyen par kilogramme.
         */
        BigDecimal prixMoyen() {

            if (quantite.signum() == 0) {
                return BigDecimal.ZERO;
            }

            return montant.divide(
                    quantite,
                    2,
                    RoundingMode.HALF_UP
            );
        }
    }

    // Services

    private final VenteService venteService = new VenteService();

    private final ClientService clientService = new ClientService();

    private final PaieService paieService = new PaieService();

    private final FermeService fermeService =
            new FermeService(new FermeDao());


    // Labels

    @FXML
    private Label lblFerme;

    @FXML
    private Label lblVolume;

    @FXML
    private Label lblMontant;

    @FXML
    private Label lblPrixMoyen;

    @FXML
    private Label lblEffectif;

    @FXML
    private Label lblTotalPaies;


    // Tableau des livraisons

    @FXML
    private TableView<LivraisonFerme> tableLivraisons;

    @FXML
    private TableColumn<LivraisonFerme, String> colBon;

    @FXML
    private TableColumn<LivraisonFerme, String> colDate;

    @FXML
    private TableColumn<LivraisonFerme, String> colClient;

    @FXML
    private TableColumn<LivraisonFerme, String> colQuantite;

    @FXML
    private TableColumn<LivraisonFerme, String> colPrix;

    @FXML
    private TableColumn<LivraisonFerme, String> colMontantLigne;

    @FXML
    private TableColumn<LivraisonFerme, String> colStatut;


    // Tableau des reçus de paie

    @FXML
    private TableView<ResumeRecu> tableRecus;

    @FXML
    private TableColumn<ResumeRecu, String> colNumero;

    @FXML
    private TableColumn<ResumeRecu, String> colDatePaie;

    @FXML
    private TableColumn<ResumeRecu, String> colPeriode;

    @FXML
    private TableColumn<ResumeRecu, String> colNbOuvriers;

    @FXML
    private TableColumn<ResumeRecu, String> colTotal;


    // Données

    private Integer idFerme;

    private Map<Integer, String> nomsFermes = new HashMap<>();

    private Map<Integer, Client> clientsParId = new HashMap<>();


    /**
     * Configure les colonnes et charge les données
     * de la ferme du responsable connecté.
     */
    @FXML
    private void initialize() {

        // =========================================
        // CONFIGURATION TABLEAU LIVRAISONS
        // =========================================

        colBon.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().vente().getNumeroBon()
                )
        );

        colDate.setCellValueFactory(c ->
                new SimpleStringProperty(
                        Formats.date(
                                c.getValue().vente().getDateLivraison()
                        )
                )
        );

        colClient.setCellValueFactory(c -> {

            Client client = clientsParId.get(
                    c.getValue().vente().getIdClient()
            );

            return new SimpleStringProperty(
                    client == null ? "?" : client.getNom()
            );
        });

        colQuantite.setCellValueFactory(c ->
                new SimpleStringProperty(
                        Formats.nombre(
                                c.getValue().quantite()
                        )
                )
        );

        colPrix.setCellValueFactory(c ->
                new SimpleStringProperty(
                        Formats.nombre(
                                c.getValue().prixMoyen()
                        )
                )
        );

        colMontantLigne.setCellValueFactory(c ->
                new SimpleStringProperty(
                        Formats.nombre(
                                c.getValue().montant()
                        )
                )
        );

        colStatut.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue()
                                .vente()
                                .getStatut()
                                .getLibelle()
                )
        );


        // =========================================
        // CONFIGURATION TABLEAU REÇUS DE PAIE
        // =========================================

        colNumero.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().numero()
                )
        );

        colDatePaie.setCellValueFactory(c ->
                new SimpleStringProperty(
                        Formats.date(
                                c.getValue().datePaiement()
                        )
                )
        );

        colPeriode.setCellValueFactory(c ->
                new SimpleStringProperty(
                        Formats.date(
                                c.getValue().periodeDebut()
                        )
                        + " au "
                        + Formats.date(
                                c.getValue().periodeFin()
                        )
                )
        );

        colNbOuvriers.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.valueOf(
                                c.getValue().nbOuvriers()
                        )
                )
        );

        colTotal.setCellValueFactory(c ->
                new SimpleStringProperty(
                        Formats.nombre(
                                c.getValue().total()
                        )
                )
        );


        // =========================================
        // FERME DU RESPONSABLE CONNECTÉ
        // =========================================

        idFerme = Session.getIdFerme();

        if (idFerme == null) {

            AlertUtil.error(
                    "Aucune ferme n'est associée à votre compte."
            );

            return;
        }

        charger();
    }


    /**
     * Charge les informations de la ferme.
     */
    private void charger() {

        try {

            // Chargement des fermes

            nomsFermes = fermeService
                    .obtenirToutesLesFermes()
                    .stream()
                    .collect(Collectors.toMap(
                            Ferme::getId,
                            Ferme::getNom
                    ));


            // Chargement des clients

            clientsParId = clientService
                    .lister()
                    .stream()
                    .collect(Collectors.toMap(
                            Client::getId,
                            c -> c
                    ));


            // Affichage du nom de la ferme

            lblFerme.setText(
                    "Ferme : "
                            + nomsFermes.getOrDefault(
                                    idFerme,
                                    "Ferme " + idFerme
                            )
            );


            // Chargement des livraisons

            chargerLivraisons();


            // Chargement des reçus de paie

            chargerRecus();

        } catch (RuntimeException e) {

            AlertUtil.error(
                    "Chargement impossible : "
                            + e.getMessage()
            );
        }
    }


    /**
     * Charge les livraisons concernant la ferme.
     */
    private void chargerLivraisons() {

        List<LivraisonFerme> lignes =
                venteService
                        .listerParFerme(idFerme)
                        .stream()
                        .map(this::resumerPourFerme)
                        .collect(Collectors.toList());


        tableLivraisons.setItems(
                FXCollections.observableArrayList(lignes)
        );


        // Exclusion des ventes annulées des statistiques

        List<LivraisonFerme> actives = lignes
                .stream()
                .filter(l ->
                        l.vente().getStatut()
                                != StatutVente.ANNULEE
                )
                .collect(Collectors.toList());


        // Calcul du volume total

        BigDecimal volume = actives
                .stream()
                .map(LivraisonFerme::quantite)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );


        // Calcul du montant total

        BigDecimal montant = actives
                .stream()
                .map(LivraisonFerme::montant)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );


        // Affichage du volume

        lblVolume.setText(
                Formats.nombre(volume) + " kg"
        );


        // Affichage du montant

        lblMontant.setText(
                Formats.mad(montant)
        );


        // Calcul et affichage du prix moyen

        lblPrixMoyen.setText(
                Formats.mad(
                        volume.signum() == 0
                                ? BigDecimal.ZERO
                                : montant.divide(
                                        volume,
                                        2,
                                        RoundingMode.HALF_UP
                                )
                )
        );
    }


    /**
     * Résume une livraison pour la ferme du responsable.
     */
    private LivraisonFerme resumerPourFerme(Vente v) {

        List<LigneVente> siennes = v
                .getLignes()
                .stream()
                .filter(l ->
                        l.getIdFerme() == idFerme
                )
                .collect(Collectors.toList());


        // Quantité vendue par la ferme

        BigDecimal quantite = siennes
                .stream()
                .map(LigneVente::getQuantiteKg)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );


        // Montant vendu par la ferme

        BigDecimal montant = siennes
                .stream()
                .map(LigneVente::getMontant)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );


        return new LivraisonFerme(
                v,
                quantite,
                montant
        );
    }


    /**
     * Charge les reçus de paie de la ferme.
     */
    private void chargerRecus() {

        // Récupération des reçus depuis le service

        List<ResumeRecu> recus =
                paieService.listerRecus(idFerme);


        // Affichage dans le tableau

        tableRecus.setItems(
                FXCollections.observableArrayList(recus)
        );


        // Effectif actif

        lblEffectif.setText(
                paieService.effectifActif(idFerme)
                        + " ouvriers"
        );


        // Calcul du total des paiements

        BigDecimal totalPaies = recus
                .stream()
                .map(ResumeRecu::total)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );


        // Affichage du total

        lblTotalPaies.setText(
                Formats.mad(totalPaies)
        );
    }


    /**
     * Génère le PDF du reçu de paie sélectionné.
     */
    @FXML
    private void onRecuPdf() {

        // Récupération du reçu sélectionné

        ResumeRecu selection =
                tableRecus
                        .getSelectionModel()
                        .getSelectedItem();


        // Vérification de la sélection

        if (selection == null) {

            AlertUtil.info(
                    "Sélectionnez d'abord un reçu."
            );

            return;
        }


        // =========================================
        // CHOIX DU FICHIER PDF
        // =========================================

        FileChooser chooser = new FileChooser();

        chooser.setTitle(
                "Enregistrer le reçu de paie"
        );

        chooser.setInitialFileName(
                selection.numero() + ".pdf"
        );

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Fichier PDF",
                        "*.pdf"
                )
        );


        File fichier = chooser.showSaveDialog(
                tableRecus.getScene().getWindow()
        );


        if (fichier == null) {
            return;
        }


        // =========================================
        // GÉNÉRATION DU PDF
        // =========================================

        try {

            /*
             * IMPORTANT :
             *
             * chargerRecuPdf() retourne un ResultatPaie
             * contenant les détails du reçu et ses lignes.
             *
             * PdfUtil.recuPaie() attend ce type de résultat.
             */

            var recu = paieService
                    .chargerRecuPdf(selection.idRecu())
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Reçu introuvable."
                            )
                    );


            // Génération du fichier PDF

            PdfUtil.recuPaie(
                    recu,
                    nomsFermes.getOrDefault(
                            idFerme,
                            "Ferme " + idFerme
                    ),
                    fichier.toPath()
            );


            // Confirmation

            AlertUtil.info(
                    "Reçu enregistré :\n"
                            + fichier.getAbsolutePath()
            );

        } catch (RuntimeException e) {

            AlertUtil.error(
                    "Erreur lors de la génération du reçu : "
                            + e.getMessage()
            );
        }
    }
}
