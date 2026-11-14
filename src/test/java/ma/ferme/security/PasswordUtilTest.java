package ma.ferme.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Tests de {@link PasswordUtil}. */
class PasswordUtilTest {

    @Test
    void hashNeContientPasLeMotDePasseEnClair() {
        String h = PasswordUtil.hash("admin123");
        assertNotEquals("admin123", h);
        assertTrue(h.startsWith("$2"));
    }

    @Test
    void verifyAccepteLeBonMotDePasse() {
        assertTrue(PasswordUtil.verify("admin123", PasswordUtil.hash("admin123")));
    }

    @Test
    void verifyRefuseUnMauvaisMotDePasse() {
        assertFalse(PasswordUtil.verify("autre", PasswordUtil.hash("admin123")));
    }

    @Test
    void verifyAccepteLeHashDeData() {
        // hash de data.sql : mot de passe "admin123"
        assertTrue(PasswordUtil.verify("admin123",
                "$2a$10$oB6/7AAIOhWwSKkpZ6LSMenAz5gMJSPSm6KicbTtRIroW29ERTzQi"));
    }

    @Test
    void verifyRefuseLesValeursNullesOuInvalides() {
        assertFalse(PasswordUtil.verify(null, "x"));
        assertFalse(PasswordUtil.verify("x", "pas-un-hash"));
    }
}