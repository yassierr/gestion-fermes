
package ma.ferme.model;

import ma.ferme.model.enums.RoleUtilisateur;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilisateurTest {

    // 1. Test des constructeurs

    @Test
    void testConstructeurAdministrateur() {

        Administrateur admin = new Administrateur(
                "Ahmed Ali",
                "ahmed",
                "hash123"
        );

        assertEquals("Ahmed Ali", admin.getNomComplet());
        assertEquals("ahmed", admin.getLogin());
        assertEquals(0, admin.getId());
        assertEquals(RoleUtilisateur.ADMIN, admin.getRole());
        assertTrue(admin.isActif());
        assertNull(admin.getIdFerme());
    }

    @Test
    void testConstructeurResponsableFerme() {

        ResponsableFerme responsable = new ResponsableFerme(
                "Youssef",
                "youssef",
                "hash456",
                10
        );

        assertEquals(RoleUtilisateur.RESPONSABLE, responsable.getRole());
        assertEquals(10, responsable.getIdFerme());
        assertTrue(responsable.isActif());
    }

    @Test
    void testConstructeurAgentRH() {

        AgentRH agent = new AgentRH(
                "Ali",
                "ali",
                "hash789",
                20
        );

        assertEquals(RoleUtilisateur.AGENT_RH, agent.getRole());
        assertEquals(20, agent.getIdFerme());
        assertTrue(agent.isActif());
    }


    // 2. Test Getters et Setters

    @Test
    void testGettersAndSetters() {

        Administrateur admin = new Administrateur(
                "Ahmed",
                "ahmed",
                "hash123"
        );

        admin.setId(5);
        admin.setNomComplet("Mohamed");
        admin.setLogin("mohamed");
        admin.setMotDePasseHash("newHash");
        admin.setActif(false);

        assertEquals(5, admin.getId());
        assertEquals("Mohamed", admin.getNomComplet());
        assertEquals("mohamed", admin.getLogin());
        assertEquals("newHash", admin.getMotDePasseHash());
        assertFalse(admin.isActif());
    }


    // 3. Test validation des données

    @Test
    void testNomCompletInvalide() {

        Administrateur admin = new Administrateur(
                "Ahmed",
                "ahmed",
                "hash"
        );

        assertThrows(IllegalArgumentException.class,
                () -> admin.setNomComplet(""));

        assertThrows(IllegalArgumentException.class,
                () -> admin.setNomComplet(null));
    }

    @Test
    void testLoginInvalide() {

        Administrateur admin = new Administrateur(
                "Ahmed",
                "ahmed",
                "hash"
        );

        assertThrows(IllegalArgumentException.class,
                () -> admin.setLogin(" "));

        assertThrows(IllegalArgumentException.class,
                () -> admin.setLogin(null));
    }

    @Test
    void testTrimDesDonnees() {

        Administrateur admin = new Administrateur(
                "  Ahmed Ali  ",
                "  ahmed  ",
                "hash"
        );

        assertEquals("Ahmed Ali", admin.getNomComplet());
        assertEquals("ahmed", admin.getLogin());
    }


    // 4. Test RG-10 : rattachement à une ferme

    @Test
    void testAdministrateurSansFerme() {

        Administrateur admin = new Administrateur(
                "Ahmed", "ahmed", "hash"
        );

        assertNull(admin.getIdFerme());
        assertTrue(admin.voitToutesLesFermes());
    }

    @Test
    void testResponsableAvecFerme() {

        ResponsableFerme responsable = new ResponsableFerme(
                "Youssef", "youssef", "hash", 10
        );

        assertEquals(10, responsable.getIdFerme());
        assertFalse(responsable.voitToutesLesFermes());
    }

    @Test
    void testAgentSansFermeInvalide() {

        assertThrows(IllegalArgumentException.class,
                () -> Utilisateur.creer(
                        1, "Ali", "ali", "hash",
                        RoleUtilisateur.AGENT_RH,
                        true, null
                ));
    }

   @Test
    void testAdministrateurAvecFermeInvalide() {

        assertThrows(IllegalArgumentException.class,
                () -> Utilisateur.creer(
                        1, "Ahmed", "ahmed", "hash",
                        RoleUtilisateur.ADMIN, true, 10
                ));
    }

    private Administrateur adminAvecFerme() {
        return new Administrateur(
                1, "Ahmed", "ahmed", "hash", true
        ) {
            // Administrateur n'accepte pas de ferme.
        };
    }


    // 5. Test Factory creer()

    @Test
    void testCreerAdministrateur() {

        Utilisateur user = Utilisateur.creer(
                1, "Ahmed", "ahmed", "hash",
                RoleUtilisateur.ADMIN, true, null
        );

        assertTrue(user instanceof Administrateur);
    }

    @Test
    void testCreerResponsable() {

        Utilisateur user = Utilisateur.creer(
                2, "Youssef", "youssef", "hash",
                RoleUtilisateur.RESPONSABLE, true, 10
        );

        assertTrue(user instanceof ResponsableFerme);
    }

    @Test
    void testCreerAgentRH() {

        Utilisateur user = Utilisateur.creer(
                3, "Ali", "ali", "hash",
                RoleUtilisateur.AGENT_RH, true, 20
        );

        assertTrue(user instanceof AgentRH);
    }


    // 6. Test afficherMenu()

    @Test
    void testMenuAdministrateur() {

        Administrateur admin = new Administrateur(
                "Ahmed", "ahmed", "hash"
        );

        assertEquals(5, admin.afficherMenu().size());
        assertTrue(admin.afficherMenu().contains("Utilisateurs"));
    }

    @Test
    void testMenuResponsable() {

        ResponsableFerme responsable = new ResponsableFerme(
                "Youssef", "youssef", "hash", 10
        );

        assertTrue(responsable.afficherMenu().contains("Serres"));
        assertTrue(responsable.afficherMenu().contains("Récoltes"));
    }

    @Test
    void testMenuAgentRH() {

        AgentRH agent = new AgentRH(
                "Ali", "ali", "hash", 20
        );

        assertTrue(agent.afficherMenu().contains("Ouvriers"));
    }


    // 7. Test authentification et compte actif

    @Test
    void testPeutSeConnecter() {

        Administrateur admin = new Administrateur(
                "Ahmed", "ahmed", "hash"
        );

        assertTrue(admin.peutSeConnecter());

        admin.setActif(false);

        assertFalse(admin.peutSeConnecter());
    }


    // 8. Test equals et hashCode

    @Test
    void testEquals() {

        Administrateur admin1 = new Administrateur(
                "Ahmed", "ahmed", "hash1"
        );

        AgentRH agent = new AgentRH(
                "Ali", "ahmed", "hash2", 10
        );

        assertEquals(admin1, agent);
        assertEquals(admin1.hashCode(), agent.hashCode());
    }


    // 9. Test toString : sécurité

    @Test
    void testToStringNeContientPasHash() {

        Administrateur admin = new Administrateur(
                "Ahmed", "ahmed", "secretHash123"
        );

        assertFalse(admin.toString().contains("secretHash123"));
    }


    // 10. Test Enum RoleUtilisateur

    @Test
    void testRoles() {

        assertEquals(3, RoleUtilisateur.values().length);

        assertEquals("Administrateur",
                RoleUtilisateur.ADMIN.getLibelle());

        assertEquals("Responsable de ferme",
                RoleUtilisateur.RESPONSABLE.getLibelle());

        assertEquals("Agent RH",
                RoleUtilisateur.AGENT_RH.getLibelle());
    }
}
