package ma.ferme.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fournit les connexions JDBC a la base PostgreSQL.
 * Les parametres (url, utilisateur, mot de passe) sont lus dans le fichier
 * {@code config.properties} situe a la racine du projet. Ce fichier est local
 * a chaque membre et n'est jamais pousse sur GitHub (voir {@code config.properties.example}).
 * <p>
 * Pour utiliser un autre fichier (ex. base de test), lancer la JVM avec
 * {@code -Dferme.config=config.test.properties}.
 *
 * @author Yassir 
 * @version 1.0
 */
public final class DatabaseConnection {

    private static final String DEFAULT_CONFIG = "config.properties";
    private static Properties props;

    private DatabaseConnection() {
    }

    /**
     * Ouvre une nouvelle connexion a la base. L'appelant doit la fermer
     * (utiliser un try-with-resources).
     *
     * @return une connexion JDBC ouverte
     * @throws SQLException si la connexion echoue (base arretee, mauvais mot de passe...)
     */
    public static Connection getConnection() throws SQLException {
        Properties p = load();
        return DriverManager.getConnection(
                p.getProperty("db.url"),
                p.getProperty("db.user"),
                p.getProperty("db.password"));
    }

    /**
     * Indique si le fichier de configuration est present.
     *
     * @return {@code true} si le fichier de configuration existe
     */
    public static boolean isConfigured() {
        return Files.exists(Path.of(System.getProperty("ferme.config", DEFAULT_CONFIG)));
    }

    private static synchronized Properties load() {
        if (props != null) {
            return props;
        }
        Path file = Path.of(System.getProperty("ferme.config", DEFAULT_CONFIG));
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Fichier de configuration introuvable : " + file.toAbsolutePath()
                    + " (copier config.properties.example en config.properties)", e);
        }
        props = p;
        return props;
    }
}
