package ma.ferme.dao;

/**
 * Exception non verifiee levee quand une operation d'acces aux donnees echoue.
 * Elle encapsule l'{@link java.sql.SQLException} d'origine afin que les couches
 * service et controller n'aient pas a gerer d'exceptions SQL.
 *
 * @author Yassir 
 * @version 1.0
 */
public class DaoException extends RuntimeException {

    /**
     * Cree une exception avec un message et sa cause.
     *
     * @param message description de l'erreur
     * @param cause   exception d'origine (souvent une SQLException)
     */
    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }
}