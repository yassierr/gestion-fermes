package ma.ferme.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ma.ferme.dao.FermeDao;
import ma.ferme.model.Client;
import ma.ferme.model.LigneVente;
import ma.ferme.model.Vente;
import ma.ferme.security.Session;
import ma.ferme.service.ClientService;
import ma.ferme.service.FermeService;
import ma.ferme.service.VenteService;
import ma.ferme.util.Formats;
import ma.ferme.model.Ferme;
import ma.ferme.service.FermeService;
import ma.ferme.dao.FermeDao;
import java.util.stream.Collectors;

/**
 * Controller du formulaire modal « Creer une livraison » : choix du client, de la date
 * et ajout de lignes (une par ferme, avec quantite et prix par kg). Le stock disponible
 * de la ferme choisie est affiche ; l'enregistrement est refuse si le stock est insuffisant.
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public class LivraisonFormController {

    /** Element de liste deroulante : une ferme (identifiant + nom). */
    private record FermeItem(int id, String nom) {
        @Override
        public String toString() {
            return nom;
        }
    }

    private final VenteService venteService = new VenteService();
    private final ClientService clientService = new ClientService();
    private final FermeService fermeService = new FermeService(new FermeDao());
    private final ObservableList<LigneVente> lignes = FXCollections.observableArrayList();
    private Map<Integer, String> nomsFermes;

    @FXML private ComboBox<Client> cbClient;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<FermeItem> cbFerme;
    @FXML private TextField tfQuantite;
    @FXML private TextField tfPrix;
    @FXML private Label lblStock;
    @FXML private Label lblTotal;
    @FXML private Label lblErreur;
    @FXML private TableView<LigneVente> tableLignes;
    @FXML private TableColumn<LigneVente, String> colFerme;
    @FXML private TableColumn<LigneVente, String> colQuantite;
    @FXML private TableColumn<LigneVente, String> colPrix;
    @FXML private TableColumn<LigneVente, String> colMontant;

    /** Charge les clients et les fermes, et configure le tableau des lignes. */
    @FXML
    private void initialize() {
        cbClient.setItems(FXCollections.observableArrayList(
                clientService.lister().stream().filter(Client::isActif).toList()));
        dpDate.setValue(LocalDate.now());

        nomsFermes = fermeService.obtenirToutesLesFermes().stream().collect(Collectors.toMap(
                Ferme::getId,
                Ferme::getNom
        ));
        nomsFermes.forEach((id, nom) -> cbFerme.getItems().add(new FermeItem(id, nom)));
        cbFerme.valueProperty().addListener((obs, ancien, nouveau) -> afficherStock(nouveau));

        colFerme.setCellValueFactory(c -> new SimpleStringProperty(
                nomsFermes.getOrDefault(c.getValue().getIdFerme(), "Ferme " + c.getValue().getIdFerme())));
        colQuantite.setCellValueFactory(c -> new SimpleStringProperty(Formats.nombre(c.getValue().getQuantiteKg())));
        colPrix.setCellValueFactory(c -> new SimpleStringProperty(Formats.nombre(c.getValue().getPrixKg())));
        colMontant.setCellValueFactory(c -> new SimpleStringProperty(Formats.nombre(c.getValue().getMontant())));
        tableLignes.setItems(lignes);
    }

    private void afficherStock(FermeItem ferme) {
        if (ferme == null) {
            lblStock.setText("");
            return;
        }
        lblStock.setText("Stock disponible de " + ferme.nom() + " : "
                + Formats.nombre(venteService.stockDisponible(ferme.id())) + " kg");
    }

    /** Ajoute une ligne (ferme, quantite, prix) a la livraison en cours de saisie. */
    @FXML
    private void onAjouterLigne() {
        try {
            lblErreur.setText("");
            FermeItem ferme = cbFerme.getValue();
            if (ferme == null) {
                throw new IllegalArgumentException("Choisissez une ferme");
            }
            BigDecimal quantite = Formats.lireNombre(tfQuantite.getText());
            BigDecimal prix = Formats.lireNombre(tfPrix.getText());
            if (quantite.signum() <= 0 || prix.signum() <= 0) {
                throw new IllegalArgumentException("Quantité et prix doivent être supérieurs à 0");
            }
            lignes.add(new LigneVente(ferme.id(), quantite, prix));
            tfQuantite.clear();
            tfPrix.clear();
            majTotal();
        } catch (RuntimeException e) {
            lblErreur.setText(e.getMessage());
        }
    }

    /** Retire la ligne selectionnee. */
    @FXML
    private void onRetirerLigne() {
        LigneVente selection = tableLignes.getSelectionModel().getSelectedItem();
        if (selection != null) {
            lignes.remove(selection);
            majTotal();
        }
    }

    private void majTotal() {
        BigDecimal total = lignes.stream().map(LigneVente::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotal.setText("Total : " + Formats.mad(total));
    }

    /** Valide et enregistre la livraison (transaction), puis ferme la fenetre. */
    @FXML
    private void onEnregistrer() {
        try {
            Client client = cbClient.getValue();
            if (client == null) {
                throw new IllegalArgumentException("Choisissez un client");
            }
            Vente vente = new Vente();
            vente.setIdClient(client.getId());
            vente.setDateLivraison(dpDate.getValue());
            vente.setLignes(new java.util.ArrayList<>(lignes));
            venteService.creerLivraison(vente, Session.getIdUtilisateur());
            fermer();
        } catch (RuntimeException e) {
            lblErreur.setText(e.getMessage());
        }
    }

    /** Ferme la fenetre sans enregistrer. */
    @FXML
    private void onAnnuler() {
        fermer();
    }

    private void fermer() {
        ((Stage) tableLignes.getScene().getWindow()).close();
    }
}