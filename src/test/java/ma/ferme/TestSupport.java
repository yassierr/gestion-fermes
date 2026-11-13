package ma.ferme;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import ma.ferme.config.DatabaseConnection;

/**
 * Outils communs aux tests qui utilisent PostgreSQL. {@link #baseDisponible()}
 * permet d'ignorer ces tests quand la base n'est pas configuree sur le poste.
 */
public final class TestSupport {

    private TestSupport() {
    }

    /** @return {@code true} si config.properties existe et la base repond */
    public static boolean baseDisponible() {
        if (!DatabaseConnection.isConfigured()) {
            return false;
        }
        try (Connection c = DatabaseConnection.getConnection()) {
            return c.isValid(2);
        } catch (SQLException | RuntimeException e) {
            return false;
        }
    }

    /** @return l'identifiant d'un administrateur (data.sql) */
    public static int idAdmin() {
        return premierId("SELECT id FROM utilisateur WHERE role = 'ADMIN' ORDER BY id LIMIT 1");
    }

    /** @return l'identifiant d'une ferme avec du stock (data.sql) */
    public static int idFerme() {
        return premierId("SELECT id FROM ferme ORDER BY id LIMIT 1");
    }

    /**
     * Execute une requete et retourne la premiere valeur entiere.
     *
     * @param sql requete SELECT
     * @return la premiere valeur de la premiere ligne
     */
    public static int premierId(String sql) {
        try (Connection c = DatabaseConnection.getConnection();
             ResultSet rs = c.createStatement().executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Execute une instruction SQL (preparation / nettoyage des tests).
     *
     * @param sql instruction SQL
     */
    public static void executer(String sql) {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.createStatement().execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}