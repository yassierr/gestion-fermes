
package ma.ferme.service;

import ma.ferme.dao.FermeDao;
import ma.ferme.dao.SerreDao;
import ma.ferme.model.Ferme;
import ma.ferme.model.Serre;

import java.util.List;
import java.util.Optional;

public class SerreService {

    private final SerreDao serreDao;
    private final FermeDao fermeDao;

    // Constructor injection
    public SerreService(SerreDao serreDao, FermeDao fermeDao) {
        if (serreDao == null || fermeDao == null) {
            throw new IllegalArgumentException("Les DAO ne peuvent pas etre null");
        }

        this.serreDao = serreDao;
        this.fermeDao = fermeDao;
    }

    // Create a greenhouse
    public Serre creerSerre(
            String nom,
            double surfaceM2,
            String variete,
            String typeAbri,
            String irrigation,
            int idFerme
    ) {
        validerInformations(nom, surfaceM2, idFerme);

        verifierExistenceFerme(idFerme);

        nom = nom.trim();
        variete = nettoyerTexte(variete);
        typeAbri = nettoyerTexte(typeAbri);
        irrigation = nettoyerTexte(irrigation);

        if (nomExisteDeja(nom, idFerme, 0)) {
            throw new IllegalStateException(
                    "Une serre avec ce nom existe deja dans cette ferme"
            );
        }

        Serre serre = new Serre(
                nom,
                surfaceM2,
                variete,
                typeAbri,
                irrigation,
                idFerme
        );

        serreDao.insert(serre);

        return serre;
    }

    // Find a greenhouse by ID
    public Optional<Serre> obtenirSerreParId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de serre invalide");
        }

        return serreDao.findById(id);
    }

    // Get all greenhouses
    public List<Serre> obtenirToutesLesSerres() {
        return serreDao.findAll();
    }

    // Update a greenhouse
    public void modifierSerre(Serre serre) {
        if (serre == null) {
            throw new IllegalArgumentException("La serre ne peut pas etre null");
        }

        if (serre.getId() <= 0) {
            throw new IllegalArgumentException("ID de serre invalide");
        }

        validerInformations(
                serre.getNom(),
                serre.getSurfaceM2(),
                serre.getIdFerme()
        );

        verifierExistenceFerme(serre.getIdFerme());

        serre.setNom(serre.getNom().trim());
        serre.setVariete(nettoyerTexte(serre.getVariete()));
        serre.setTypeAbri(nettoyerTexte(serre.getTypeAbri()));
        serre.setIrrigation(nettoyerTexte(serre.getIrrigation()));

        if (nomExisteDeja(
                serre.getNom(),
                serre.getIdFerme(),
                serre.getId()
        )) {
            throw new IllegalStateException(
                    "Une autre serre utilise deja ce nom dans cette ferme"
            );
        }

        serreDao.update(serre);
    }

    // Delete a greenhouse
    public void supprimerSerre(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de serre invalide");
        }

        serreDao.delete(id);
    }

    // Validate required information
    private void validerInformations(
            String nom,
            double surfaceM2,
            int idFerme
    ) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom de la serre est obligatoire");
        }

        if (nom.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "Le nom ne doit pas depasser 100 caracteres"
            );
        }

        if (!Double.isFinite(surfaceM2) || surfaceM2 <= 0) {
            throw new IllegalArgumentException(
                    "La surface doit etre un nombre positif et valide"
            );
        }

        if (idFerme <= 0) {
            throw new IllegalArgumentException("ID de ferme invalide");
        }
    }

    // Check that the parent farm exists
    private void verifierExistenceFerme(int idFerme) {
        Optional<Ferme> ferme = fermeDao.findById(idFerme);

        if (ferme.isEmpty()) {
            throw new IllegalArgumentException(
                    "La ferme associee n'existe pas"
            );
        }
    }

    // Check duplicate names inside the same farm
    private boolean nomExisteDeja(
            String nom,
            int idFerme,
            int idSerreExclu
    ) {
        return serreDao.findAll().stream()
                .anyMatch(serre ->
                        serre.getId() != idSerreExclu
                                && serre.getIdFerme() == idFerme
                                && serre.getNom().equalsIgnoreCase(nom)
                );
    }

    // Trim optional text fields; keep null values as null
    private String nettoyerTexte(String valeur) {
        if (valeur == null) {
            return null;
        }

        String resultat = valeur.trim();
        return resultat.isEmpty() ? null : resultat;
    }
}
