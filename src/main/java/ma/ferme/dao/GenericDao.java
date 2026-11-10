package ma.ferme.dao;

import java.util.List;
import java.util.Optional;

/**
 * Contrat commun a tous les DAO : operations CRUD de base sur une entite.
 * Chaque implementation (UtilisateurDao, FermeDao...) ecrit son propre SQL
 * avec des {@code PreparedStatement}. Les erreurs SQL sont converties en {@link DaoException}.
 *
 * @param <T> le type de l'entite geree (classe du package {@code model})
 * @author Yassir Hikou
 * @version 1.0
 */
public interface GenericDao<T> {

    /**
     * Insere une nouvelle entite. L'identifiant genere par la base est
     * recopie dans l'objet passe en parametre.
     *
     * @param entity l'entite a enregistrer
     */
    void insert(T entity);

    /**
     * Recherche une entite par son identifiant.
     *
     * @param id identifiant de l'entite
     * @return l'entite si elle existe, sinon un {@code Optional} vide
     */
    Optional<T> findById(int id);

    /**
     * Retourne toutes les entites de la table.
     *
     * @return la liste des entites (vide si la table est vide)
     */
    List<T> findAll();

    /**
     * Met a jour une entite existante (selon son identifiant).
     *
     * @param entity l'entite avec ses nouvelles valeurs
     */
    void update(T entity);

    /**
     * Supprime l'entite portant cet identifiant.
     *
     * @param id identifiant de l'entite a supprimer
     */
    void delete(int id);
}