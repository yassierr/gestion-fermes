package ma.ferme.service;

import java.util.List;
import java.util.Optional;
import ma.ferme.dao.ClientDao;
import ma.ferme.model.Client;

/**
 * Regles metier des clients : validation des champs et unicite du code.
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public class ClientService {

    private final ClientDao dao;

    /** Cree le service avec son DAO par defaut. */
    public ClientService() {
        this(new ClientDao());
    }

    /**
     * Cree le service avec un DAO donne (utile pour les tests).
     *
     * @param dao le DAO a utiliser
     */
    public ClientService(ClientDao dao) {
        this.dao = dao;
    }

    /**
     * Ajoute un client apres validation.
     *
     * @param c le client a ajouter
     * @throws IllegalArgumentException si un champ est invalide ou si le code existe deja
     */
    public void ajouter(Client c) {
        valider(c);
        if (dao.findByCode(c.getCode()).isPresent()) {
            throw new IllegalArgumentException("Ce code client est déjà utilisé : " + c.getCode());
        }
        dao.insert(c);
    }

    /**
     * Modifie un client existant apres validation.
     *
     * @param c le client avec ses nouvelles valeurs
     * @throws IllegalArgumentException si un champ est invalide ou si le code appartient a un autre client
     */
    public void modifier(Client c) {
        valider(c);
        Optional<Client> existant = dao.findByCode(c.getCode());
        if (existant.isPresent() && existant.get().getId() != c.getId()) {
            throw new IllegalArgumentException("Ce code client est déjà utilisé : " + c.getCode());
        }
        dao.update(c);
    }

    /**
     * Supprime un client.
     *
     * @param id identifiant du client
     */
    public void supprimer(int id) {
        dao.delete(id);
    }

    /**
     * Liste tous les clients.
     *
     * @return la liste des clients tries par nom
     */
    public List<Client> lister() {
        return dao.findAll();
    }

    /**
     * Recherche par code, nom ou ville.
     *
     * @param texte le texte recherche (vide = tous les clients)
     * @return les clients correspondants
     */
    public List<Client> rechercher(String texte) {
        if (texte == null || texte.isBlank()) {
            return dao.findAll();
        }
        return dao.rechercher(texte.trim());
    }

    /**
     * Recherche un client par identifiant.
     *
     * @param id identifiant du client
     * @return le client, ou un Optional vide
     */
    public Optional<Client> trouver(int id) {
        return dao.findById(id);
    }

    private void valider(Client c) {
        if (c == null) {
            throw new IllegalArgumentException("Client absent");
        }
        if (c.getCode() == null || c.getCode().isBlank()) {
            throw new IllegalArgumentException("Le code client est obligatoire");
        }
        if (c.getCode().length() > 20) {
            throw new IllegalArgumentException("Le code client ne doit pas dépasser 20 caractères");
        }
        if (c.getNom() == null || c.getNom().isBlank()) {
            throw new IllegalArgumentException("Le nom du client est obligatoire");
        }
        if (c.getType() == null) {
            throw new IllegalArgumentException("Le type de client est obligatoire");
        }
    }
}