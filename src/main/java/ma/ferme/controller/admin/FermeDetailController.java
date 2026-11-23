package ma.ferme.controller.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import ma.ferme.dao.FermeDao;
import ma.ferme.dao.SerreDao;
import ma.ferme.model.Ferme;
import ma.ferme.model.Serre;
import ma.ferme.service.SerreService;
import ma.ferme.util.SceneNavigator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur de la fiche détaillée d'une ferme (FermeDetail).
 * Affiche les statistiques (Iteration 3) et les onglets (Serres, Récoltes, Dépenses, Ouvriers, Reçus de paie).
 *
 * @author Brahim
 * @version 1.0
 */
public class FermeDetailController {

    @FXML private Label lblBreadcrumbName;
    @FXML private Label lblNomFerme;
    @FXML private Label lblLocalisation;
    @FXML private Label lblSurface;

    @FXML private Label lblStatStock;
    @FXML private Label lblStatCa;
    @FXML private Label lblStatBenefice;
    @FXML private Label lblStatEffectif;
    @FXML private Label lblStatMasseSalariale;

    @FXML private TextField txtSearchSerre;
    @FXML private Label lblSerreCount;
    @FXML private TableView<Serre> tableSerres;
    @FXML private TableColumn<Serre, String> colNomSerre;
    @FXML private TableColumn<Serre, String> colSurfaceSerre;
    @FXML private TableColumn<Serre, String> colTypeAbri;
    @FXML private TableColumn<Serre, String> colIrrigation;

    private Ferme ferme;
    private final SerreService serreService = new SerreService(new SerreDao(), new FermeDao());
    private ObservableList<Serre> serreList;

    @FXML
    public void initialize() {
        if (colNomSerre != null) {
            colNomSerre.setCellValueFactory(new PropertyValueFactory<>("nom"));
            colSurfaceSerre.setCellValueFactory(cell ->
                    new SimpleStringProperty(String.format("%.0f m²", cell.getValue().getSurfaceM2()))
            );
            colTypeAbri.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getTypeAbri() != null ? cell.getValue().getTypeAbri() : "-")
            );
            colIrrigation.setCellValueFactory(cell ->
                    new SimpleStringProperty(cell.getValue().getIrrigation() != null ? cell.getValue().getIrrigation() : "-")
            );
        }
    }

    public void initData(Ferme ferme) {
        this.ferme = ferme;
        if (ferme != null) {
            lblBreadcrumbName.setText(ferme.getNom());
            lblNomFerme.setText(ferme.getNom());
            lblLocalisation.setText("📍 " + ferme.getLocalisation());
            lblStatStock.setText(String.format("%.0f kg", ferme.getStockDisponible()));
            loadSerresForFerme();
        }
    }

    private void loadSerresForFerme() {
        try {
            if (ferme != null) {
                List<Serre> fermeSerres = serreService.obtenirToutesLesSerres().stream()
                        .filter(s -> s.getIdFerme() == ferme.getId())
                        .collect(Collectors.toList());

                serreList = FXCollections.observableArrayList(fermeSerres);

                double totalSurface = fermeSerres.stream()
                        .mapToDouble(Serre::getSurfaceM2)
                        .sum();

                if (lblSurface != null) {
                    lblSurface.setText(String.format("%.0f m²", totalSurface));
                }
                if (lblSerreCount != null) {
                    lblSerreCount.setText("Total : " + fermeSerres.size() + " serres répertoriées");
                }

                setupSerreSearch();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (lblSerreCount != null) {
                lblSerreCount.setText("Total : 0 serres répertoriées");
            }
        }
    }

    private void setupSerreSearch() {
        if (serreList == null) return;

        FilteredList<Serre> filteredData = new FilteredList<>(serreList, s -> true);
        if (txtSearchSerre != null) {
            txtSearchSerre.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredData.setPredicate(serre -> {
                    if (newVal == null || newVal.isBlank()) {
                        return true;
                    }
                    String filter = newVal.toLowerCase();
                    return serre.getNom().toLowerCase().contains(filter)
                            || (serre.getTypeAbri() != null && serre.getTypeAbri().toLowerCase().contains(filter))
                            || (serre.getIrrigation() != null && serre.getIrrigation().toLowerCase().contains(filter));
                });
            });
        }
        tableSerres.setItems(filteredData);
    }

    @FXML
    private void handleRetourFermes() {
        try {
            Node fermesView = SceneNavigator.load("/view/admin/fermes.fxml");
            StackPane contentStack = (StackPane) lblNomFerme.getScene().lookup("#rootContentStack");
            if (contentStack != null) {
                contentStack.getChildren().setAll(fermesView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}