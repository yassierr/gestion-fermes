package ma.ferme.controller.admin;

import java.io.File;
import java.math.BigDecimal;
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
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import ma.ferme.model.Client;
import ma.ferme.model.LigneVente;
import ma.ferme.model.Vente;
import ma.ferme.model.enums.StatutVente;
import ma.ferme.service.ClientService;
import ma.ferme.service.VenteService;
import ma.ferme.util.AlertUtil;
import ma.ferme.util.Formats;
import ma.ferme.util.PdfUtil;
import ma.ferme.util.SceneNavigator;
import ma.ferme.model.Ferme;
import ma.ferme.service.FermeService;
import ma.ferme.dao.FermeDao;

/**
 * Controller de l'ecran « Clients &amp; Livraisons » (espace Administrateur) :
 * annuaire des clients, registre des livraisons, indicateurs, actions
 * (ajouter/modifier/supprimer un client, creer une livraison, la marquer payee ou
 * l'annuler, generer le bon de livraison en PDF).
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public class ClientLivraisonController {

    private final ClientService clientService = new ClientService();
    private final VenteService venteService = new VenteService();
    private final FermeService fermeService = new FermeService(new FermeDao());

    @FXML private Label lblTotalClients;
    @FXML private Label lblNbLivraisons;
    @FXML private Label lblVolume;
    @FXML private Label lblCreances;
    @FXML private TextField rechercheClient;
    @FXML private TextField rechercheLivraison;

    @FXML private TableView<Client> tableClients;
    @FXML private TableColumn<Client, String> colCode;
    @FXML private TableColumn<Client, String> colNom;
    @FXML private TableColumn<Client, String> colType;
    @FXML private TableColumn<Client, String> colTelephone;
    @FXML private TableColumn<Client, String> colVille;

    @FXML private TableView<Vente> tableVentes;
    @FXML private TableColumn<Vente, String> colBon;
    @FXML private TableColumn<Vente, String> colDate;
    @FXML private TableColumn<Vente, String> colClientVente;
    @FXML private TableColumn<Vente, String> colFermes;
    @FXML private TableColumn<Vente, String> colVolume;
    @FXML private TableColumn<Vente, String> colMontant;
    @FXML private TableColumn<Vente, String> colStatut;

    private Map<Integer, Client> clientsParId = new HashMap<>();
    private Map<Integer, String> nomsFermes = new HashMap<>();
    private List<Vente> toutesLesVentes = List.of();

    /** Configure les colonnes, les champs de recherche et charge les donnees. */
    @FXML
    private void initialize() {
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCode()));
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().getLibelle()));
        colTelephone.setCellValueFactory(c -> new SimpleStringProperty(nonNull(c.getValue().getTelephone())));
        colVille.setCellValueFactory(c -> new SimpleStringProperty(nonNull(c.getValue().getVille())));

        colBon.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroBon()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(Formats.date(c.getValue().getDateLivraison())));
        colClientVente.setCellValueFactory(c -> new SimpleStringProperty(nomClient(c.getValue())));
        colFermes.setCellValueFactory(c -> new SimpleStringProperty(fermesDe(c.getValue())));
        colVolume.setCellValueFactory(c -> new SimpleStringProperty(Formats.nombre(c.getValue().getQuantiteTotale())));
        colMontant.setCellValueFactory(c -> new SimpleStringProperty(Formats.nombre(c.getValue().getTotal())));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut().getLibelle()));

        rechercheClient.textProperty().addListener((obs, ancien, nouveau) -> rafraichirClients());
        rechercheLivraison.textProperty().addListener((obs, ancien, nouveau) -> afficherVentes());
        rafraichir();
    }

    /** Recharge les clients, les livraisons et les indicateurs depuis la base. */
    private void rafraichir() {
        try {
            rafraichirClients();
            toutesLesVentes = venteService.lister();
            nomsFermes = fermeService.obtenirToutesLesFermes().stream().collect(Collectors.toMap(Ferme::getId, Ferme::getNom));
            afficherVentes();
            majIndicateurs();
        } catch (RuntimeException e) {
            AlertUtil.error("Chargement impossible : " + e.getMessage());
        }
    }

    private void rafraichirClients() {
        List<Client> tous = clientService.lister();
        clientsParId = tous.stream().collect(Collectors.toMap(Client::getId, c -> c));
        lblTotalClients.setText(String.valueOf(tous.size()));
        tableClients.setItems(FXCollections.observableArrayList(clientService.rechercher(rechercheClient.getText())));
    }

    private void afficherVentes() {
        String filtre = rechercheLivraison.getText() == null ? "" : rechercheLivraison.getText().trim().toLowerCase();
        List<Vente> filtrees = toutesLesVentes.stream()
                .filter(v -> filtre.isEmpty()
                        || v.getNumeroBon().toLowerCase().contains(filtre)
                        || nomClient(v).toLowerCase().contains(filtre))
                .collect(Collectors.toList());
        tableVentes.setItems(FXCollections.observableArrayList(filtrees));
    }

    private void majIndicateurs() {
        List<Vente> actives = toutesLesVentes.stream()
                .filter(v -> v.getStatut() != StatutVente.ANNULEE).collect(Collectors.toList());
        BigDecimal volume = actives.stream().map(Vente::getQuantiteTotale).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblNbLivraisons.setText(String.valueOf(actives.size()));
        lblVolume.setText(Formats.nombre(volume) + " kg");
        lblCreances.setText(Formats.mad(venteService.creances()));
    }

    // ---------------- Actions sur les clients ----------------

    /** Ouvre le formulaire d'ajout d'un client. */
    @FXML
    private void onAjouterClient() {
        SceneNavigator.<ClientFormController>openModal("/view/admin/client-form.fxml", "Ajouter un client",
                form -> form.setClient(null));
        rafraichir();
    }

    /** Ouvre le formulaire de modification du client selectionne. */
    @FXML
    private void onModifierClient() {
        Client selection = tableClients.getSelectionModel().getSelectedItem();
        if (selection == null) {
            AlertUtil.info("Sélectionnez d'abord un client.");
            return;
        }
        SceneNavigator.<ClientFormController>openModal("/view/admin/client-form.fxml", "Modifier le client",
                form -> form.setClient(selection));
        rafraichir();
    }

    /** Supprime le client selectionne apres confirmation. */
    @FXML
    private void onSupprimerClient() {
        Client selection = tableClients.getSelectionModel().getSelectedItem();
        if (selection == null) {
            AlertUtil.info("Sélectionnez d'abord un client.");
            return;
        }
        if (!AlertUtil.confirm("Supprimer le client « " + selection.getNom() + " » ?")) {
            return;
        }
        try {
            clientService.supprimer(selection.getId());
            rafraichir();
        } catch (RuntimeException e) {
            AlertUtil.error("Suppression impossible (le client a peut-être des livraisons).");
        }
    }

    // ---------------- Actions sur les livraisons ----------------

    /** Ouvre le formulaire de creation d'une livraison. */
    @FXML
    private void onCreerLivraison() {
        SceneNavigator.openModal("/view/admin/livraison-form.fxml", "Créer une livraison");
        rafraichir();
    }

    /** Marque la livraison selectionnee comme payee. */
    @FXML
    private void onMarquerPayee() {
        Vente selection = tableVentes.getSelectionModel().getSelectedItem();
        if (selection == null) {
            AlertUtil.info("Sélectionnez d'abord une livraison.");
            return;
        }
        try {
            venteService.marquerPayee(selection.getId(), null);
            rafraichir();
        } catch (RuntimeException e) {
            AlertUtil.error(e.getMessage());
        }
    }

    /** Annule la livraison selectionnee apres confirmation. */
    @FXML
    private void onAnnulerLivraison() {
        Vente selection = tableVentes.getSelectionModel().getSelectedItem();
        if (selection == null) {
            AlertUtil.info("Sélectionnez d'abord une livraison.");
            return;
        }
        if (!AlertUtil.confirm("Annuler la livraison " + selection.getNumeroBon() + " ?")) {
            return;
        }
        try {
            venteService.annuler(selection.getId());
            rafraichir();
        } catch (RuntimeException e) {
            AlertUtil.error(e.getMessage());
        }
    }

    /** Genere le bon de livraison PDF de la livraison selectionnee. */
    @FXML
    private void onBonPdf() {
        Vente selection = tableVentes.getSelectionModel().getSelectedItem();
        if (selection == null) {
            AlertUtil.info("Sélectionnez d'abord une livraison.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(selection.getNumeroBon() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File fichier = chooser.showSaveDialog(tableVentes.getScene().getWindow());
        if (fichier == null) {
            return;
        }
        try {
            PdfUtil.bonLivraison(selection, clientsParId.get(selection.getIdClient()), nomsFermes, fichier.toPath());
            AlertUtil.info("Bon de livraison enregistré :\n" + fichier.getAbsolutePath());
        } catch (RuntimeException e) {
            AlertUtil.error(e.getMessage());
        }
    }

    // ---------------- Utilitaires d'affichage ----------------

    private String nomClient(Vente v) {
        Client c = clientsParId.get(v.getIdClient());
        return c == null ? "?" : c.getNom();
    }

    private String fermesDe(Vente v) {
        return v.getLignes().stream()
                .map(LigneVente::getIdFerme).distinct()
                .map(id -> nomsFermes.getOrDefault(id, "Ferme " + id))
                .collect(Collectors.joining(", "));
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }
}