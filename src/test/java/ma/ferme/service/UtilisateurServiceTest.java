package ma.ferme.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import ma.ferme.TestSupport;
import ma.ferme.config.DatabaseConnection;
import ma.ferme.dao.UtilisateurDao;
import ma.ferme.model.Administrateur;
import ma.ferme.model.AgentRH;
import ma.ferme.model.ResponsableFerme;
import ma.ferme.model.Utilisateur;
import ma.ferme.model.enums.RoleUtilisateur;
import ma.ferme.security.PasswordUtil;
import ma.ferme.service.AuthService.AuthException;

/**
 * Tests de {@link UtilisateurService} avec la vraie base PostgreSQL : ignorés automatiquement
 * si elle n'est pas configurée. Chaque test crée deux fermes et ses comptes (logins préfixés
 * {@code test_svc_}) puis les supprime : {@code data.sql} n'est pas touché. Les compteurs sont
 * comparés avant / après, car la base contient d'autres comptes.
 */
class UtilisateurServiceTest {

    private static final String PREFIXE = "test_svc_";
    private static final String MOT_DE_PASSE = "secret123";

    /** BCrypt est lent : le hash est calculé une seule fois pour toute la classe. */
    private static String hash;

    private UtilisateurDao dao;
    private UtilisateurService service;
    private Administrateur admin;
    private Integer fermeA;
    private Integer fermeB;

    @BeforeAll
    static void calculerHash() {
        hash = PasswordUtil.hash(MOT_DE_PASSE);
    }

    @BeforeEach
    void preparer() throws SQLException {
        assumeTrue(TestSupport.baseDisponible(), "PostgreSQL n'est pas configurée : test ignoré.");
        dao = new UtilisateurDao();
        service = new UtilisateurService(dao);
        fermeA = creerFermeTest("A");
        fermeB = creerFermeTest("B");
        admin = new Administrateur("Karim Benjelloun", nouveauLogin("admin"), hash);
        dao.insert(admin);
    }

    @AfterEach
    void nettoyer() throws SQLException {
        if (dao == null) {
            return; // le test a été ignoré : rien n'a été créé
        }
        supprimerComptesDeTest();
        if (fermeA != null) {
            supprimerFermeTest(fermeA);
        }
        if (fermeB != null) {
            supprimerFermeTest(fermeB);
        }
    }

    // ------------------------------------------------------------------
    // Droits (ENF-02)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("un responsable ou un agent RH ne peut utiliser aucune action d'administration")
    void actionsReserveesAdmin() {
        Utilisateur responsable = inserer(new ResponsableFerme("Driss", nouveauLogin("driss"), hash, fermeA));
        Utilisateur agent = inserer(new AgentRH("Youssef", nouveauLogin("youssef"), hash, fermeA));

        for (Utilisateur acteur : new Utilisateur[] {responsable, agent, null}) {
            assertThrows(SecurityException.class, () -> service.lister(acteur));
            assertThrows(SecurityException.class, () -> service.rechercher(acteur, "x", null, null));
            assertThrows(SecurityException.class, () -> service.compterTous(acteur));
            assertThrows(SecurityException.class,
                    () -> service.compterParRole(acteur, RoleUtilisateur.AGENT_RH));
            assertThrows(SecurityException.class, () -> service.creerCompte(acteur, "Nom", "login",
                    MOT_DE_PASSE, RoleUtilisateur.RESPONSABLE, fermeB));
            assertThrows(SecurityException.class, () -> service.modifierCompte(acteur, 1, "Nom", "login", 1));
            assertThrows(SecurityException.class, () -> service.desactiver(acteur, 1));
            assertThrows(SecurityException.class, () -> service.activer(acteur, 1));
        }
    }

    // ------------------------------------------------------------------
    // Consultation
    // ------------------------------------------------------------------

    @Test
    @DisplayName("lister : contient les comptes créés")
    void lister() {
        Utilisateur responsable = inserer(new ResponsableFerme("Driss", nouveauLogin("driss"), hash, fermeA));

        List<Utilisateur> comptes = service.lister(admin);

        assertTrue(comptes.stream().anyMatch(u -> u.getId() == admin.getId()));
        assertTrue(comptes.stream().anyMatch(u -> u.getId() == responsable.getId()));
    }

    @Test
    @DisplayName("rechercher : par nom ou login, avec filtres rôle et ferme")
    void rechercher() {
        String jeton = "tk" + System.nanoTime(); // présent dans les trois noms, absent du reste de la base
        inserer(new ResponsableFerme("Driss " + jeton, nouveauLogin("driss"), hash, fermeA));
        inserer(new AgentRH("Youssef " + jeton, nouveauLogin("youssef"), hash, fermeA));
        inserer(new ResponsableFerme("Mehdi " + jeton, nouveauLogin("mehdi"), hash, fermeB));

        assertEquals(3, service.rechercher(admin, jeton, null, null).size());
        assertEquals(2, service.rechercher(admin, jeton, RoleUtilisateur.RESPONSABLE, null).size());
        assertEquals(2, service.rechercher(admin, jeton, null, fermeA).size());
        assertEquals(1, service.rechercher(admin, "Mehdi " + jeton, RoleUtilisateur.RESPONSABLE, fermeB).size());
        assertTrue(service.rechercher(admin, jeton + "zzz", null, null).isEmpty());
    }

    @Test
    @DisplayName("compteurs : total et par rôle augmentent à chaque création")
    void compteurs() {
        long total = service.compterTous(admin);
        long responsables = service.compterParRole(admin, RoleUtilisateur.RESPONSABLE);
        long agents = service.compterParRole(admin, RoleUtilisateur.AGENT_RH);

        inserer(new ResponsableFerme("Driss", nouveauLogin("driss"), hash, fermeA));
        inserer(new AgentRH("Youssef", nouveauLogin("youssef"), hash, fermeA));
        inserer(new AgentRH("Salma", nouveauLogin("salma"), hash, fermeB));

        assertEquals(total + 3, service.compterTous(admin));
        assertEquals(responsables + 1, service.compterParRole(admin, RoleUtilisateur.RESPONSABLE));
        assertEquals(agents + 2, service.compterParRole(admin, RoleUtilisateur.AGENT_RH));
    }

    // ------------------------------------------------------------------
    // Création de compte
    // ------------------------------------------------------------------

    @Test
    @DisplayName("creerCompte : compte actif, enregistré en base, mot de passe haché")
    void creerCompte() {
        String login = nouveauLogin("driss");

        Utilisateur cree = service.creerCompte(admin, "  Driss El Amrani ", " " + login + " ", "motdepasse1",
                RoleUtilisateur.RESPONSABLE, fermeA);

        assertTrue(cree.getId() > 0);
        Utilisateur enBase = dao.findById(cree.getId()).orElseThrow();
        assertTrue(enBase instanceof ResponsableFerme);
        assertEquals("Driss El Amrani", enBase.getNomComplet());
        assertEquals(login, enBase.getLogin());
        assertEquals(fermeA, enBase.getIdFerme());
        assertTrue(enBase.isActif());
        assertNotEquals("motdepasse1", enBase.getMotDePasseHash());
        assertTrue(PasswordUtil.verify("motdepasse1", enBase.getMotDePasseHash()));
    }

    @Test
    @DisplayName("creerCompte : un responsable et un agent RH peuvent partager la même ferme")
    void creerResponsableEtAgentMemeFerme() {
        service.creerCompte(admin, "Driss", nouveauLogin("driss"), "motdepasse1",
                RoleUtilisateur.RESPONSABLE, fermeA);
        Utilisateur agent = service.creerCompte(admin, "Youssef", nouveauLogin("youssef"), "motdepasse1",
                RoleUtilisateur.AGENT_RH, fermeA);

        assertTrue(dao.findById(agent.getId()).orElseThrow() instanceof AgentRH);
    }

    @Test
    @DisplayName("creerCompte : refuse les saisies invalides et ne crée rien")
    void creerCompteSaisiesInvalides() {
        String login = nouveauLogin("driss");
        long avant = dao.count();

        assertRefuse(() -> service.creerCompte(admin, " ", login, "motdepasse1",
                RoleUtilisateur.RESPONSABLE, fermeA), "nom complet");
        assertRefuse(() -> service.creerCompte(admin, null, login, "motdepasse1",
                RoleUtilisateur.RESPONSABLE, fermeA), "nom complet");
        assertRefuse(() -> service.creerCompte(admin, "x".repeat(101), login, "motdepasse1",
                RoleUtilisateur.RESPONSABLE, fermeA), "nom complet");
        assertRefuse(() -> service.creerCompte(admin, "Driss", "", "motdepasse1",
                RoleUtilisateur.RESPONSABLE, fermeA), "login");
        assertRefuse(() -> service.creerCompte(admin, "Driss", "dri ss", "motdepasse1",
                RoleUtilisateur.RESPONSABLE, fermeA), "espace");
        assertRefuse(() -> service.creerCompte(admin, "Driss", "x".repeat(51), "motdepasse1",
                RoleUtilisateur.RESPONSABLE, fermeA), "login");
        assertRefuse(() -> service.creerCompte(admin, "Driss", login, "12345",
                RoleUtilisateur.RESPONSABLE, fermeA), "mot de passe");
        assertRefuse(() -> service.creerCompte(admin, "Driss", login, null,
                RoleUtilisateur.RESPONSABLE, fermeA), "mot de passe");
        assertRefuse(() -> service.creerCompte(admin, "Driss", login, "motdepasse1",
                null, fermeA), "rôle");
        assertRefuse(() -> service.creerCompte(admin, "Driss", login, "motdepasse1",
                RoleUtilisateur.ADMIN, null), "responsable");
        assertRefuse(() -> service.creerCompte(admin, "Driss", login, "motdepasse1",
                RoleUtilisateur.RESPONSABLE, null), "ferme");
        assertEquals(avant, dao.count(), "aucun compte ne doit avoir été créé");
    }

    @Test
    @DisplayName("creerCompte : login déjà utilisé")
    void creerCompteLoginPris() {
        String login = nouveauLogin("driss");
        service.creerCompte(admin, "Driss", login, "motdepasse1", RoleUtilisateur.RESPONSABLE, fermeA);

        assertRefuse(() -> service.creerCompte(admin, "Autre", login, "motdepasse1",
                RoleUtilisateur.AGENT_RH, fermeB), "login");
    }

    @Test
    @DisplayName("creerCompte : RG-10, un seul responsable par ferme")
    void creerCompteFermeOccupee() {
        service.creerCompte(admin, "Driss", nouveauLogin("driss"), "motdepasse1",
                RoleUtilisateur.RESPONSABLE, fermeA);

        assertRefuse(() -> service.creerCompte(admin, "Autre", nouveauLogin("autre"), "motdepasse1",
                RoleUtilisateur.RESPONSABLE, fermeA), "ferme");
    }

    // ------------------------------------------------------------------
    // Modification de compte
    // ------------------------------------------------------------------

    @Test
    @DisplayName("modifierCompte : change nom, login et ferme en base ; le rôle ne change pas")
    void modifierCompte() {
        Utilisateur resp = inserer(new ResponsableFerme("Ancien Nom", nouveauLogin("ancien"), hash, fermeA));
        String nouveauLogin = nouveauLogin("nouveau");

        service.modifierCompte(admin, resp.getId(), " Nouveau Nom ", " " + nouveauLogin + " ", fermeB);

        Utilisateur enBase = dao.findById(resp.getId()).orElseThrow();
        assertEquals("Nouveau Nom", enBase.getNomComplet());
        assertEquals(nouveauLogin, enBase.getLogin());
        assertEquals(fermeB, enBase.getIdFerme());
        assertEquals(RoleUtilisateur.RESPONSABLE, enBase.getRole());
    }

    @Test
    @DisplayName("modifierCompte : garder son propre login et sa propre ferme est autorisé")
    void modifierCompteSansChangerLoginNiFerme() {
        Utilisateur resp = inserer(new ResponsableFerme("Driss", nouveauLogin("driss"), hash, fermeA));

        service.modifierCompte(admin, resp.getId(), "Driss El Amrani", resp.getLogin(), fermeA);

        assertEquals("Driss El Amrani", dao.findById(resp.getId()).orElseThrow().getNomComplet());
    }

    @Test
    @DisplayName("modifierCompte : login pris par un autre compte")
    void modifierCompteLoginPris() {
        Utilisateur driss = inserer(new ResponsableFerme("Driss", nouveauLogin("driss"), hash, fermeA));
        Utilisateur mehdi = inserer(new ResponsableFerme("Mehdi", nouveauLogin("mehdi"), hash, fermeB));

        assertRefuse(() -> service.modifierCompte(admin, mehdi.getId(), "Mehdi", driss.getLogin(), fermeB), "login");
        assertEquals(mehdi.getLogin(), dao.findById(mehdi.getId()).orElseThrow().getLogin());
    }

    @Test
    @DisplayName("modifierCompte : RG-10, la nouvelle ferme a déjà un responsable")
    void modifierCompteFermeOccupee() {
        inserer(new ResponsableFerme("Driss", nouveauLogin("driss"), hash, fermeA));
        Utilisateur mehdi = inserer(new ResponsableFerme("Mehdi", nouveauLogin("mehdi"), hash, fermeB));

        assertRefuse(() -> service.modifierCompte(admin, mehdi.getId(), "Mehdi", mehdi.getLogin(), fermeA), "ferme");
        assertEquals(fermeB, dao.findById(mehdi.getId()).orElseThrow().getIdFerme());
    }

    @Test
    @DisplayName("modifierCompte : compte inconnu, nom vide, responsable sans ferme, admin avec ferme")
    void modifierCompteRefus() {
        Utilisateur resp = inserer(new ResponsableFerme("Driss", nouveauLogin("driss"), hash, fermeA));

        assertRefuse(() -> service.modifierCompte(admin, Integer.MAX_VALUE, "Nom", "login", fermeA), "introuvable");
        assertRefuse(() -> service.modifierCompte(admin, resp.getId(), " ", resp.getLogin(), fermeA), "nom complet");
        assertRefuse(() -> service.modifierCompte(admin, resp.getId(), "Driss", resp.getLogin(), null), "ferme");
        assertRefuse(() -> service.modifierCompte(admin, admin.getId(), "Karim", admin.getLogin(), fermeA), "ferme");
        assertEquals(fermeA, dao.findById(resp.getId()).orElseThrow().getIdFerme());
    }

    // ------------------------------------------------------------------
    // Activation / désactivation
    // ------------------------------------------------------------------

    @Test
    @DisplayName("desactiver puis activer un compte")
    void desactiverEtActiver() {
        Utilisateur resp = inserer(new ResponsableFerme("Driss", nouveauLogin("driss"), hash, fermeA));

        service.desactiver(admin, resp.getId());
        assertFalse(dao.findById(resp.getId()).orElseThrow().isActif());

        service.activer(admin, resp.getId());
        assertTrue(dao.findById(resp.getId()).orElseThrow().isActif());
    }

    @Test
    @DisplayName("desactiver : l'administrateur ne peut pas désactiver son propre compte")
    void desactiverSoiMeme() {
        assertRefuse(() -> service.desactiver(admin, admin.getId()), "propre compte");
        assertTrue(dao.findById(admin.getId()).orElseThrow().isActif());
    }

    @Test
    @DisplayName("desactiver / activer : compte inconnu")
    void etatCompteInconnu() {
        assertRefuse(() -> service.desactiver(admin, Integer.MAX_VALUE), "introuvable");
        assertRefuse(() -> service.activer(admin, Integer.MAX_VALUE), "introuvable");
    }

    // ------------------------------------------------------------------
    // Changement de mot de passe
    // ------------------------------------------------------------------

    @Test
    @DisplayName("changerMotDePasse : un agent RH change son mot de passe, enregistré en base")
    void changerMotDePasse() {
        Utilisateur agent = inserer(new AgentRH("Youssef", nouveauLogin("youssef"), hash, fermeA));

        service.changerMotDePasse(agent, MOT_DE_PASSE, "nouveau456");

        String hashEnBase = dao.findById(agent.getId()).orElseThrow().getMotDePasseHash();
        assertTrue(PasswordUtil.verify("nouveau456", hashEnBase));
        assertFalse(PasswordUtil.verify(MOT_DE_PASSE, hashEnBase));
        assertEquals(hashEnBase, agent.getMotDePasseHash(), "le hash de la session doit être à jour");
    }

    @Test
    @DisplayName("changerMotDePasse : ancien incorrect ou vide, le hash ne change pas")
    void changerMotDePasseAncienInvalide() {
        Utilisateur agent = inserer(new AgentRH("Youssef", nouveauLogin("youssef"), hash, fermeA));

        assertRefuse(() -> service.changerMotDePasse(agent, "mauvais", "nouveau456"), "actuel");
        assertRefuse(() -> service.changerMotDePasse(agent, "", "nouveau456"), "actuel");
        assertRefuse(() -> service.changerMotDePasse(agent, null, "nouveau456"), "actuel");
        assertEquals(hash, dao.findById(agent.getId()).orElseThrow().getMotDePasseHash());
    }

    @Test
    @DisplayName("changerMotDePasse : nouveau trop court ou identique à l'ancien")
    void changerMotDePasseNouveauInvalide() {
        Utilisateur agent = inserer(new AgentRH("Youssef", nouveauLogin("youssef"), hash, fermeA));

        assertRefuse(() -> service.changerMotDePasse(agent, MOT_DE_PASSE, "123"), "mot de passe");
        assertRefuse(() -> service.changerMotDePasse(agent, MOT_DE_PASSE, null), "mot de passe");
        assertRefuse(() -> service.changerMotDePasse(agent, MOT_DE_PASSE, MOT_DE_PASSE), "différent");
        assertEquals(hash, dao.findById(agent.getId()).orElseThrow().getMotDePasseHash());
    }

    @Test
    @DisplayName("changerMotDePasse : personne de connecté, ou compte absent de la base")
    void changerMotDePasseSansCompte() {
        assertThrows(SecurityException.class, () -> service.changerMotDePasse(null, "a", "bbbbbb"));

        Utilisateur fantome = new AgentRH("Fantôme", nouveauLogin("fantome"), hash, fermeA); // jamais enregistré
        assertRefuse(() -> service.changerMotDePasse(fantome, MOT_DE_PASSE, "nouveau456"), "introuvable");
    }

    // ------------------------------------------------------------------
    // Scénario complet avec AuthService
    // ------------------------------------------------------------------

    @Test
    @DisplayName("scénario : créer un compte, se connecter, changer le mot de passe, désactiver")
    void scenarioComplet() throws AuthException {
        AuthService auth = new AuthService(dao);
        String login = nouveauLogin("scenario");

        service.creerCompte(admin, "Salma Bennani", login, "motdepasse1", RoleUtilisateur.AGENT_RH, fermeA);
        Utilisateur connecte = auth.authentifier(login, "motdepasse1");
        assertTrue(connecte instanceof AgentRH);

        service.changerMotDePasse(connecte, "motdepasse1", "nouveau456");
        assertThrows(AuthException.class, () -> auth.authentifier(login, "motdepasse1"));
        assertEquals(login, auth.authentifier(login, "nouveau456").getLogin());

        service.desactiver(admin, connecte.getId());
        AuthException refus = assertThrows(AuthException.class, () -> auth.authentifier(login, "nouveau456"));
        assertTrue(refus.getMessage().contains("désactivé"));
    }

    // ------------------------------------------------------------------
    // Outils de test
    // ------------------------------------------------------------------

    private Utilisateur inserer(Utilisateur u) {
        dao.insert(u);
        return u;
    }

    /** Login unique et reconnaissable, pour ne jamais entrer en conflit avec data.sql. */
    private static String nouveauLogin(String suffixe) {
        return PREFIXE + suffixe + "_" + System.nanoTime();
    }

    /** Vérifie qu'une IllegalArgumentException est levée et que son message contient le fragment (sans casse). */
    private static void assertRefuse(Executable action, String fragment) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, action);
        assertTrue(e.getMessage().toLowerCase().contains(fragment.toLowerCase()),
                "Message inattendu : « " + e.getMessage() + " » (attendu : contient « " + fragment + " »)");
    }

    private static int creerFermeTest(String lettre) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO ferme (nom, localisation) VALUES (?, ?) RETURNING id")) {
            ps.setString(1, "Ferme test service " + lettre + " " + System.nanoTime());
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