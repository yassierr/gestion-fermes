
package ma.ferme.service;

import ma.ferme.dao.FermeDao;
import ma.ferme.model.Ferme;

import java.util.List;
import java.util.Optional;

public class FermeService {

    private final FermeDao fermeDao;

    // Constructor injection
    public FermeService(FermeDao fermeDao) {
        if (fermeDao == null) {
            throw new IllegalArgumentException("FermeDao ne peut pas etre null");
        }
        this.fermeDao = fermeDao;
    }

    // Create a farm
    public Ferme creerFerme(String nom, String localisation) {

        validerInformations(nom, localisation);

        nom = nom.trim();
        localisation = localisation.trim();

        // Check for duplicate farm names
        if (nomExisteDeja(nom, 0)) {
            throw new IllegalStateException(
                    "Une ferme avec ce nom existe deja"
            );
        }

        Ferme ferme = new Ferme(nom, localisation);

        fermeDao.insert(ferme);

        return ferme;
    }

    // Find a farm by ID
    public Optional<Ferme> obtenirFermeParId(int id) {

        if (id <= 0) {
            throw new IllegalArgumentException("ID invalide");
        }

        return fermeDao.findById(id);
    }

    // Get all farms
    public List<Ferme> obtenirToutesLesFermes() {
        return fermeDao.findAll();
    }

    // Update an existing farm
    public void modifierFerme(Ferme ferme) {

        if (ferme == null) {
            throw new IllegalArgumentException("La ferme ne peut pas etre null");
        }

        if (ferme.getId() <= 0) {
            throw new IllegalArgumentException("ID de ferme invalide");
        }

        validerInformations(
                ferme.getNom(),
                ferme.getLocalisation()
        );

        ferme.setNom(ferme.getNom().trim());
        ferme.setLocalisation(ferme.getLocalisation().trim());

        if (nomExisteDeja(ferme.getNom(), ferme.getId())) {
            throw new IllegalStateException(
                    "Une autre ferme utilise deja ce nom"
            );
        }

        fermeDao.update(ferme);
    }

    // Delete a farm
    public void supprimerFerme(int id) {

        if (id <= 0) {
            throw new IllegalArgumentException("ID invalide");
        }

        fermeDao.delete(id);
    }

    // Validate farm information
    private void validerInformations(String nom, String localisation) {

        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException(
                    "Le nom de la ferme est obligatoire"
            );
        }

        if (localisation == null || localisation.isBlank()) {
            throw new IllegalArgumentException(
                    "La localisation est obligatoire"
            );
        }

        if (nom.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "Le nom ne doit pas depasser 100 caracteres"
            );
        }

        if (localisation.trim().length() > 150) {
            throw new IllegalArgumentException(
                    "La localisation ne doit pas depasser 150 caracteres"
            );
        }
    }

    // Check whether another farm already has this name
    private boolean nomExisteDeja(String nom, int idExclu) {

        return fermeDao.findAll().stream()
                .anyMatch(ferme ->
                        ferme.getId() != idExclu
                                && ferme.getNom().equalsIgnoreCase(nom)
                );
    }
}
