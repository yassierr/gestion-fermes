package ma.ferme.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Outils de hachage des mots de passe avec BCrypt (jBCrypt).
 * Les mots de passe ne sont jamais stockes en clair : seule l'empreinte
 * (format {@code $2a$...}) est enregistree dans la colonne {@code mot_de_passe}.
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    /**
     * Calcule l'empreinte BCrypt d'un mot de passe.
     *
     * @param plain le mot de passe en clair
     * @return l'empreinte a stocker en base
     */
    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(10));
    }

    /**
     * Verifie un mot de passe saisi par rapport a l'empreinte stockee.
     *
     * @param plain  le mot de passe saisi
     * @param hashed l'empreinte lue en base
     * @return {@code true} si le mot de passe correspond
     */
    public static boolean verify(String plain, String hashed) {
        if (plain == null || hashed == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plain, hashed);
        } catch (IllegalArgumentException e) {
            return false; // empreinte invalide
        }
    }
}