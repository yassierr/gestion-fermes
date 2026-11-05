package ma.ferme.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import org.junit.jupiter.api.Test;

/**
 * Teste la connexion a PostgreSQL. Ignore automatiquement si
 * {@code config.properties} n'existe pas encore sur le poste.
 */
class DatabaseConnectionTest {

    @Test
    void connexionEtTablesPresentes() throws Exception {
        assumeTrue(DatabaseConnection.isConfigured(), "config.properties absent : test ignore");
        try (Connection c = DatabaseConnection.getConnection();
             var rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM utilisateur")) {
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) >= 0);
        }
    }
}