package ma.ferme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ma.ferme.TestSupport;
import ma.ferme.config.DatabaseConnection;
import ma.ferme.dao.UtilisateurDao;
import ma.ferme.model.Administrateur;
import ma.ferme.model.AgentRH;
import ma.ferme.model.ResponsableFerme;
import ma.ferme.model.Utilisateur;
import ma.ferme.security.PasswordUtil;
import ma.ferme.service.AuthService.AuthException;

/**
 * Tests de {@link AuthService} avec la vraie base PostgreSQL : ignorés automatiquement si
 * elle n'est pas configurée. Chaque test crée sa propre ferme et ses propres comptes
 * (logins préfixés {@code test_auth_}) et les supprime à la fin : {@code data.sql} n'est pas touché.
 */
class AuthServiceTest {

    private static final String PREFIXE = "test_auth_";
    private static final String MOT_DE_PASSE = "secret123";

    /** BCrypt est lent : le hash est calculé une seule fois pour toute la classe. */
    private static String hash;

    private UtilisateurDao dao;
    private AuthService service;
    private Integer idFerme;
    private String loginResponsable;

    @BeforeAll
    static void calculerHash() {
        hash = PasswordUtil.hash(MOT_DE_PASSE);
    }

    @BeforeEach
    void preparer() throws SQLException {
        assumeTrue(TestSupport.baseDisponible(), "PostgreSQL n'est pas configurée : test ignoré.");
        dao = new UtilisateurDao();
        service = new AuthService(dao);
        idFerme = creerFermeTest();
        loginResponsable = nouveauLogin("driss");
        dao.insert(new ResponsableFerme("Driss El Amrani", loginResponsable, hash, idFerme));
    }

    @AfterEach
    void nettoyer() throws SQLException {
        if (dao == null) {
            return; // le test a été ignoré : rien n'a été créé
        }
        supprimerComptesDeTest();
        if (idFerme != null) {
            supprimerFermeTest(idFerme);
        }
    }

    // ------------------------------------------------------------------
    // Connexion réussie
    // ------------------------------------------------------------------

    @Test
    @DisplayName("bons identifiants : retourne l'utilisateur du bon type")
    void connexionReussie() throws AuthException {
        Utilisateur u = service.authentifier(loginResponsable, MOT_DE_PASSE);

        assertTrue(u instanceof ResponsableFerme);
        assertEquals(loginResponsable, u.getLogin());
        assertEquals(idFerme, u.getIdFerme());
    }

    @Test
    @DisplayName("le login est nettoyé des espaces autour")
    void loginAvecEspaces() throws AuthException {
        assertEquals(loginResponsable, service.authentifier("  " + loginResponsable + "  ", MOT_DE_PASSE).getLogin());
    }

    @Test
    @DisplayName("un administrateur et un agent RH se connectent aussi")
    void connexionAutresRoles() throws AuthException {
        String loginAdmin = nouveauLogin("admin");
        String loginAgent = nouveauLogin("agent");
        dao.insert(new Administrateur("Karim Benjelloun", loginAdmin, hash));
        dao.insert(new AgentRH("Youssef Alaoui", loginAgent, hash, idFerme));

        assertTrue(service.authentifier(loginAdmin, MOT_DE_PASSE) instanceof Administrateur);
        assertTrue(service.authentifier(loginAgent, MOT_DE_PASSE) instanceof AgentRH);
    }

    // ------------------------------------------------------------------
    // Refus
    // ------------------------------------------------------------------

    @Test
    @DisplayName("login vide ou nul → message dédié")
    void loginVide() {
        AuthException e = assertThrows(AuthException.class, () -> service.authentifier("  ", MOT_DE_PASSE));
        assertEquals("Veuillez saisir votre identifiant.", e.getMessage());
        assertThrows(AuthException.class, () -> service.authentifier(null, MOT_DE_PASSE));
    }

    @Test
    @DisplayName("mot de passe vide ou nul → message dédié")
    void motDePasseVide() {
        AuthException e = assertThrows(AuthException.class, () -> service.authentifier(loginResponsable, ""));
        assertEquals("Veuillez saisir votre mot de passe.", e.getMessage());
        assertThrows(AuthException.class, () -> service.authentifier(loginResponsable, null));
    }

    @Test
    @DisplayName("login inconnu → « Identifiants incorrects. »")
    void loginInconnu() {
        AuthException e = assertThrows(AuthException.class,
                () -> service.authentifier(nouveauLogin("inconnu"), MOT_DE_PASSE));
        assertEquals(AuthService.MSG_IDENTIFIANTS_INCORRECTS, e.getMessage());
    }

    @Test
    @DisplayName("mauvais mot de passe → même message que login inconnu")
    void mauvaisMotDePasse() {
        AuthException e = assertThrows(AuthException.class, () -> service.authentifier(loginResponsable, "faux"));
        assertEquals(AuthService.MSG_IDENTIFIANTS_INCORRECTS, e.getMessage());
    }

    @Test
    @DisplayName("la casse du mot de passe compte")
    void motDePasseSensibleALaCasse() {
        assertThrows(AuthException.class,
                () -> service.authentifier(loginResponsable, MOT_DE_PASSE.toUpperCase()));
    }

    // ------------------------------------------------------------------
    // Compte désactivé (ENF-01)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("compte désactivé + bon mot de passe → refus avec message dédié")
    void compteDesactive() {
        desactiverResponsable();

        AuthException e = assertThrows(AuthException.class,
                () -> service.authentifier(loginResponsable, MOT_DE_PASSE));
        assertTrue(e.getMessage().contains("désactivé"));
    }

    @Test
    @DisplayName("compte désactivé + mauvais mot de passe → message générique (rien n'est révélé)")
    void compteDesactiveMauvaisMotDePasse() {
        desactiverResponsable();

        AuthException e = assertThrows(AuthException.class, () -> service.authentifier(loginResponsable, "faux"));
        assertEquals(AuthService.MSG_IDENTIFIANTS_INCORRECTS, e.getMessage());
    }

    // ------------------------------------------------------------------
    // Robustesse
    // ------------------------------------------------------------------

    @Test
    @DisplayName("hash invalide en base (ex. « dummy_hash ») → AuthException, pas de plantage")
    void hashInvalide() {
        String login = nouveauLogin("dummy");
        dao.insert(new AgentRH("Compte de test", login, "dummy_hash", idFerme));

        AuthException e = assertThrows(AuthException.class, () -> service.authentifier(login, "dummy_hash"));
        assertEquals(AuthService.MSG_IDENTIFIANTS_INCORRECTS, e.getMessage());
    }

    // ------------------------------------------------------------------
    // Outils de test
    // ------------------------------------------------------------------

    private void desactiverResponsable() {
        Utilisateur responsable = dao.findByLogin(loginResponsable).orElseThrow();
        dao.setActif(responsable.getId(), false);
    }

    /** Login unique et reconnaissable, pour ne jamais entrer en conflit avec data.sql. */
    private static String nouveauLogin(String suffixe) {
        return PREFIXE + suffixe + "_" + System.nanoTime();
    }

    private static int creerFermeTest() throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO ferme (nom, localisation) VALUES (?, ?) RETURNING id")) {
            ps.setString(1, "Ferme test auth " + System.nanoTime());
            ps.setString(2, "Localisation test");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void supprimerComptesDeTest() throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM utilisateur WHERE login LIKE ?")) {
            ps.setString(1, PREFIXE + "%");
            ps.executeUpdate();
        }
    }

    private static void supprimerFermeTest(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM ferme WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}