package ma.ferme.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Outils de hachage et de vérification des mots de passe avec BCrypt.
 * Les mots de passe ne sont jamais stockés en clair.
 *
 * @author Yassir Hikou
 * @version 1.1
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    /**
     * Calcule l'empreinte BCrypt d'un mot de passe.
     *
     * @param plain le mot de passe en clair
     * @return l'empreinte BCrypt à stocker en base
     */
    public static String hash(String plain) {

        if (plain == null || plain.isEmpty()) {
            throw new IllegalArgumentException(
                    "Le mot de passe ne peut pas être vide."
            );
        }

        return BCrypt.withDefaults()
                .hashToString(10, plain.toCharArray());
    }

    /**
     * Vérifie un mot de passe saisi par rapport à l'empreinte stockée.
     *
     * @param plain  le mot de passe saisi
     * @param hashed l'empreinte lue en base
     * @return true si le mot de passe correspond
     */
    public static boolean verify(String plain, String hashed) {

        if (plain == null || hashed == null
                || plain.isEmpty() || hashed.isEmpty()) {
            return false;
        }

        try {
            return BCrypt.verifyer()
                    .verify(plain.toCharArray(), hashed)
                    .verified;

        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}