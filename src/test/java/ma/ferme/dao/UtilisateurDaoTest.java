// package ma.ferme.dao;

// import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertNull;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.junit.jupiter.api.Assertions.assertTrue;
// import static org.junit.jupiter.api.Assumptions.assumeTrue;

// import java.sql.Connection;
// import java.sql.PreparedStatement;
// import java.sql.ResultSet;
// import java.sql.SQLException;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Optional;

// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;

// import ma.ferme.TestSupport;
// import ma.ferme.config.DatabaseConnection;
// import ma.ferme.model.Administrateur;
// import ma.ferme.model.AgentRH;
// import ma.ferme.model.ResponsableFerme;
// import ma.ferme.model.Utilisateur;
// import ma.ferme.model.enums.RoleUtilisateur;

// /**
//  * Tests de {@link UtilisateurDao}. Nécessitent la base PostgreSQL locale : ils sont ignorés
//  * automatiquement si elle n'est pas configurée.
//  * <p>
//  * Pour ne pas dépendre de {@code data.sql} (une ferme n'a qu'un responsable et un agent RH),
//  * chaque test crée sa propre ferme de test et la supprime à la fin, ainsi que tous les
//  * utilisateurs qu'il a créés.
//  * </p>
//  */
// class UtilisateurDaoTest {

//     private static final String PREFIXE = "test_dao_";
//     private static final String HASH = "hash_test_dao";

//     private UtilisateurDao dao;
//     private Integer idFermeTest;
//     private final List<Integer> idsCrees = new ArrayList<>();

//     @BeforeEach
//     void preparer() throws SQLException {
//         assumeTrue(TestSupport.baseDisponible(), "PostgreSQL n'est pas configurée : test ignoré.");
//         dao = new UtilisateurDao();
//         idFermeTest = creerFermeTest();
//     }

//     @AfterEach
//     void nettoyer() throws SQLException {
//         if (dao != null) {
//             for (int id : idsCrees) {
//                 dao.delete(id);
//             }
//         }
//         idsCrees.clear();
//         if (idFermeTest != null) {
//             supprimerFermeTest(idFermeTest);
//         }
//     }

//     // ------------------------------------------------------------------
//     // insert / findById
//     // ------------------------------------------------------------------

//     @Test
//     @DisplayName("insert : un administrateur reçoit un id et n'a pas de ferme")
//     void insertAdministrateur() {
//         Utilisateur admin = enregistrer(new Administrateur("Admin Test", nouveauLogin("admin"), HASH));

//         assertTrue(admin.getId() > 0);
//         Utilisateur relu = dao.findById(admin.getId()).orElseThrow();
//         assertTrue(relu instanceof Administrateur);
//         assertEquals(RoleUtilisateur.ADMIN, relu.getRole());
//         assertNull(relu.getIdFerme());
//         assertTrue(relu.isActif());
//     }

//     @Test
//     @DisplayName("insert + findById : un responsable est relu avec tous ses champs")
//     void insertResponsableEtRelecture() {
//         Utilisateur resp = enregistrer(
//                 new ResponsableFerme("Responsable Test", nouveauLogin("resp"), HASH, idFermeTest));

//         Utilisateur relu = dao.findById(resp.getId()).orElseThrow();
//         assertTrue(relu instanceof ResponsableFerme);
//         assertEquals("Responsable Test", relu.getNomComplet());
//         assertEquals(resp.getLogin(), relu.getLogin());
//         assertEquals(HASH, relu.getMotDePasseHash());
//         assertEquals(RoleUtilisateur.RESPONSABLE, relu.getRole());
//         assertEquals(idFermeTest, relu.getIdFerme());
//     }

//     @Test
//     @DisplayName("insert : un agent RH est relu comme AgentRH")
//     void insertAgentRh() {
//         Utilisateur agent = enregistrer(new AgentRH("Agent Test", nouveauLogin("rh"), HASH, idFermeTest));

//         assertTrue(dao.findById(agent.getId()).orElseThrow() instanceof AgentRH);
//     }

//     @Test
//     @DisplayName("findById : id inconnu → Optional vide")
//     void findByIdInconnu() {
//         assertTrue(dao.findById(Integer.MAX_VALUE).isEmpty());
//     }

//     // ------------------------------------------------------------------
//     // Contraintes
//     // ------------------------------------------------------------------

//     @Test
//     @DisplayName("insert : login déjà utilisé → DaoException")
//     void insertLoginEnDouble() {
//         String login = nouveauLogin("doublon");
//         enregistrer(new Administrateur("Premier", login, HASH));

//         assertThrows(DaoException.class, () -> dao.insert(new Administrateur("Second", login, HASH)));
//     }

//     @Test
//     @DisplayName("insert : deuxième responsable dans la même ferme (RG-10) → DaoException")
//     void insertDeuxiemeResponsableMemeFerme() {
//         enregistrer(new ResponsableFerme("Premier resp", nouveauLogin("resp1"), HASH, idFermeTest));
//         Utilisateur second = new ResponsableFerme("Second resp", nouveauLogin("resp2"), HASH, idFermeTest);

//         assertThrows(DaoException.class, () -> dao.insert(second));
//     }

//     @Test
//     @DisplayName("insert : ferme inexistante (clé étrangère) → DaoException")
//     void insertFermeInexistante() {
//         Utilisateur resp = new ResponsableFerme("Sans ferme", nouveauLogin("fk"), HASH, Integer.MAX_VALUE);

//         assertThrows(DaoException.class, () -> dao.insert(resp));
//     }

//     // ------------------------------------------------------------------
//     // findByLogin / existsByLogin / findByFermeAndRole
//     // ------------------------------------------------------------------

//     @Test
//     @DisplayName("findByLogin : trouve un compte, y compris désactivé ; inconnu → vide")
//     void findByLogin() {
//         Utilisateur resp = enregistrer(
//                 new ResponsableFerme("Login Test", nouveauLogin("login"), HASH, idFermeTest));
//         dao.setActif(resp.getId(), false);

//         Optional<Utilisateur> trouve = dao.findByLogin(resp.getLogin());
//         assertTrue(trouve.isPresent());
//         assertFalse(trouve.get().isActif());
//         assertTrue(dao.findByLogin("login_qui_n_existe_pas_" + System.nanoTime()).isEmpty());
//     }

//     @Test
//     @DisplayName("existsByLogin : vrai pour un login pris, faux sinon")
//     void existsByLogin() {
//         Utilisateur admin = enregistrer(new Administrateur("Exists Test", nouveauLogin("exists"), HASH));

//         assertTrue(dao.existsByLogin(admin.getLogin()));
//         assertFalse(dao.existsByLogin("login_libre_" + System.nanoTime()));
//     }

//     @Test
//     @DisplayName("findByFermeAndRole : retourne le responsable ou l'agent RH de la ferme")
//     void findByFermeAndRole() {
//         Utilisateur resp = enregistrer(
//                 new ResponsableFerme("Resp Ferme", nouveauLogin("fresp"), HASH, idFermeTest));
//         Utilisateur agent = enregistrer(new AgentRH("Agent Ferme", nouveauLogin("fagent"), HASH, idFermeTest));

//         assertEquals(resp.getId(), dao.findByFermeAndRole(idFermeTest, RoleUtilisateur.RESPONSABLE)
//                 .orElseThrow().getId());
//         assertEquals(agent.getId(), dao.findByFermeAndRole(idFermeTest, RoleUtilisateur.AGENT_RH)
//                 .orElseThrow().getId());
//         assertTrue(dao.findByFermeAndRole(Integer.MAX_VALUE, RoleUtilisateur.RESPONSABLE).isEmpty());
//     }

//     // ------------------------------------------------------------------
//     // findAll / search / count
//     // ------------------------------------------------------------------

//     @Test
//     @DisplayName("findAll : contient l'utilisateur inséré")
//     void findAll() {
//         Utilisateur admin = enregistrer(new Administrateur("FindAll Test", nouveauLogin("all"), HASH));

//         assertTrue(dao.findAll().stream().anyMatch(u -> u.getId() == admin.getId()));
//     }

//     @Test
//     @DisplayName("search : par nom ou login, avec filtres rôle et ferme")
//     void search() {
//         Utilisateur resp = enregistrer(
//                 new ResponsableFerme("Zzxq Recherche", nouveauLogin("zzxq"), HASH, idFermeTest));

//         List<Utilisateur> parNom = dao.search("zzxq", null, null);
//         assertEquals(1, parNom.size());
//         assertEquals(resp.getId(), parNom.get(0).getId());

//         assertEquals(1, dao.search(resp.getLogin(), RoleUtilisateur.RESPONSABLE, idFermeTest).size());
//         assertTrue(dao.search("zzxq", RoleUtilisateur.AGENT_RH, null).isEmpty());
//         assertTrue(dao.search("zzxq", null, Integer.MAX_VALUE).isEmpty());
//         assertTrue(dao.search("zzxq_introuvable", null, null).isEmpty());
//     }

//     @Test
//     @DisplayName("count / countByRole : augmentent de 1 après un insert")
//     void compteurs() {
//         long total = dao.count();
//         long agents = dao.countByRole(RoleUtilisateur.AGENT_RH);
//         long responsables = dao.countByRole(RoleUtilisateur.RESPONSABLE);

//         enregistrer(new AgentRH("Compteur Test", nouveauLogin("cpt"), HASH, idFermeTest));

//         assertEquals(total + 1, dao.count());
//         assertEquals(agents + 1, dao.countByRole(RoleUtilisateur.AGENT_RH));
//         assertEquals(responsables, dao.countByRole(RoleUtilisateur.RESPONSABLE));
//     }

//     // ------------------------------------------------------------------
//     // update / updateMotDePasse / setActif / delete
//     // ------------------------------------------------------------------

//     @Test
//     @DisplayName("update : modifie le nom et l'état, le rôle reste inchangé")
//     void update() {
//         Utilisateur agent = enregistrer(new AgentRH("Avant", nouveauLogin("maj"), HASH, idFermeTest));

//         agent.setNomComplet("Après");
//         agent.setActif(false);

//         dao.update(agent);

//         Utilisateur relu = dao.findById(agent.getId()).orElseThrow();
//         assertEquals("Après", relu.getNomComplet());
//         assertFalse(relu.isActif());
//         assertEquals(RoleUtilisateur.AGENT_RH, relu.getRole());
//     }

//     @Test
//     @DisplayName("update : id inconnu → DaoException")
//     void updateInconnu() {
//         Utilisateur fantome = new Administrateur(Integer.MAX_VALUE, "Fantôme", nouveauLogin("fantome"), HASH, true);

//         assertThrows(DaoException.class, () -> dao.update(fantome));
//     }

//     @Test
//     @DisplayName("updateMotDePasse : remplace le hash")
//     void updateMotDePasse() {
//         Utilisateur admin = enregistrer(new Administrateur("Mdp Test", nouveauLogin("mdp"), HASH));

//         assertTrue(dao.updateMotDePasse(admin.getId(), "nouveau_hash_test"));
//         assertEquals("nouveau_hash_test", dao.findById(admin.getId()).orElseThrow().getMotDePasseHash());
//         assertFalse(dao.updateMotDePasse(Integer.MAX_VALUE, "x"));
//     }

//     @Test
//     @DisplayName("setActif : désactive puis réactive un compte")
//     void setActif() {
//         Utilisateur admin = enregistrer(new Administrateur("Actif Test", nouveauLogin("actif"), HASH));

//         assertTrue(dao.setActif(admin.getId(), false));
//         assertFalse(dao.findById(admin.getId()).orElseThrow().isActif());
//         assertTrue(dao.setActif(admin.getId(), true));
//         assertTrue(dao.findById(admin.getId()).orElseThrow().isActif());
//     }

//     @Test
//     @DisplayName("delete : supprime le compte ; un second appel ne lève pas d'erreur")
//     void delete() {
//         Utilisateur admin = enregistrer(new Administrateur("Suppr Test", nouveauLogin("suppr"), HASH));

//         dao.delete(admin.getId());

//         assertTrue(dao.findById(admin.getId()).isEmpty());
//         assertDoesNotThrow(() -> dao.delete(admin.getId()));
//     }

//     // ------------------------------------------------------------------
//     // Outils de test
//     // ------------------------------------------------------------------

//     /** Insère l'utilisateur et mémorise son id pour le nettoyage. */
//     private Utilisateur enregistrer(Utilisateur u) {
//         dao.insert(u);
//         assertTrue(u.getId() > 0, "insert doit renseigner l'id généré");
//         idsCrees.add(u.getId());
//         return u;
//     }

//     /** Login unique et reconnaissable, pour ne jamais entrer en conflit avec data.sql. */
//     private static String nouveauLogin(String suffixe) {
//         return PREFIXE + suffixe + "_" + System.nanoTime();
//     }

//     private static int creerFermeTest() throws SQLException {
//         try (Connection c = DatabaseConnection.getConnection();
//              PreparedStatement ps = c.prepareStatement(
//                      "INSERT INTO ferme (nom, localisation) VALUES (?, ?) RETURNING id")) {
//             ps.setString(1, "Ferme test DAO " + System.nanoTime());
//             ps.setString(2, "Localisation test");
//             try (ResultSet rs = ps.executeQuery()) {
//                 rs.next();
//                 return rs.getInt(1);
//             }
//         }
//     }

//     private static void supprimerFermeTest(int idFerme) throws SQLException {
//         try (Connection c = DatabaseConnection.getConnection();
//              PreparedStatement ps = c.prepareStatement("DELETE FROM ferme WHERE id = ?")) {
//             ps.setInt(1, idFerme);
//             ps.executeUpdate();
//         }
//     }
// }