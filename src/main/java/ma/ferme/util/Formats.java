package ma.ferme.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatage commun des nombres et des dates pour l'affichage dans les ecrans.
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public final class Formats {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formats() {
    }

    /**
     * Formate un nombre avec 2 decimales et un espace comme separateur de milliers.
     *
     * @param valeur la valeur (null donne 0.00)
     * @return le texte, ex. {@code 12 345.50}
     */
    public static String nombre(BigDecimal valeur) {
        BigDecimal v = valeur == null ? BigDecimal.ZERO : valeur;
        return String.format(Locale.US, "%,.2f", v).replace(',', ' ');
    }

    /**
     * Formate un montant en dirhams.
     *
     * @param valeur le montant
     * @return le texte, ex. {@code 12 345.50 MAD}
     */
    public static String mad(BigDecimal valeur) {
        return nombre(valeur) + " MAD";
    }

    /**
     * Formate une date au format jour/mois/annee.
     *
     * @param date la date (null donne une chaine vide)
     * @return le texte, ex. {@code 05/11/2026}
     */
    public static String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    /**
     * Convertit un texte saisi (virgule ou point decimal) en nombre.
     *
     * @param texte le texte saisi
     * @return le nombre
     * @throws IllegalArgumentException si le texte n'est pas un nombre valide
     */
    public static BigDecimal lireNombre(String texte) {
        if (texte == null || texte.isBlank()) {
            throw new IllegalArgumentException("Nombre manquant");
        }
        try {
            return new BigDecimal(texte.trim().replace(" ", "").replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nombre invalide : " + texte);
        }
    }
}