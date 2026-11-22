package ma.ferme.util;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ma.ferme.model.Client;
import ma.ferme.model.LigneVente;
import ma.ferme.model.Vente;
import ma.ferme.service.PaieService.LigneRecu;
import ma.ferme.service.PaieService.ResultatPaie;

/**
 * Generation des documents PDF (bibliotheque OpenPDF) : bon de livraison,
 * recu de paie et export d'un tableau (tableau de bord).
 * Chaque methode ecrit le fichier demande et leve une {@link IllegalStateException}
 * si la generation echoue.
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public final class PdfUtil {

    private static final Color VERT = new Color(0x1F, 0x5F, 0x2B);
    private static final Font TITRE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, VERT);
    private static final Font GRAS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 11);
    private static final Font ENTETE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

    private PdfUtil() {
    }

    /**
     * Genere le bon de livraison d'une vente.
     *
     * @param vente       la livraison (avec ses lignes et son numero de bon)
     * @param client      le client livre
     * @param nomsFermes  noms des fermes par identifiant (pour afficher l'origine de chaque ligne)
     * @param sortie      fichier PDF a creer
     */
    public static void bonLivraison(Vente vente, Client client, Map<Integer, String> nomsFermes, Path sortie) {
        ecrire(sortie, doc -> {
            doc.add(new Paragraph("BON DE LIVRAISON", TITRE));
            doc.add(new Paragraph("N° " + vente.getNumeroBon(), GRAS));
            doc.add(new Paragraph("Date de livraison : " + vente.getDateLivraison(), NORMAL));
            doc.add(new Paragraph("Statut : " + vente.getStatut().getLibelle(), NORMAL));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Client : " + client.getNom() + " (" + client.getCode() + ")", GRAS));
            doc.add(new Paragraph(client.getType().getLibelle()
                    + (client.getVille() == null ? "" : " - " + client.getVille()), NORMAL));
            doc.add(new Paragraph(" "));

            PdfPTable t = tableau(new String[] {"Ferme d'origine", "Quantité (kg)", "Prix / kg (MAD)", "Montant (MAD)"});
            for (LigneVente l : vente.getLignes()) {
                String ferme = nomsFermes.getOrDefault(l.getIdFerme(), "Ferme " + l.getIdFerme());
                cellule(t, ferme, Element.ALIGN_LEFT);
                cellule(t, nombre(l.getQuantiteKg()), Element.ALIGN_RIGHT);
                cellule(t, nombre(l.getPrixKg()), Element.ALIGN_RIGHT);
                cellule(t, nombre(l.getMontant()), Element.ALIGN_RIGHT);
            }
            doc.add(t);
            doc.add(new Paragraph(" "));
            Paragraph total = new Paragraph("Quantité totale : " + nombre(vente.getQuantiteTotale())
                    + " kg      TOTAL : " + nombre(vente.getTotal()) + " MAD", GRAS);
            total.setAlignment(Element.ALIGN_RIGHT);
            doc.add(total);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Signature du livreur : ____________        Signature du client : ____________", NORMAL));
        });
    }

    /**
     * Genere le recu de paie d'une ferme.
     *
     * @param recu      le resultat du paiement (recu et lignes)
     * @param nomFerme  nom de la ferme
     * @param sortie    fichier PDF a creer
     */
    public static void recuPaie(ResultatPaie recu, String nomFerme, Path sortie) {
        ecrire(sortie, doc -> {
            doc.add(new Paragraph("REÇU DE PAIE", TITRE));
            doc.add(new Paragraph("N° " + recu.numero(), GRAS));
            doc.add(new Paragraph("Ferme : " + nomFerme, NORMAL));
            doc.add(new Paragraph("Période : du " + recu.periodeDebut() + " au " + recu.periodeFin(), NORMAL));
            doc.add(new Paragraph("Date de paiement : " + recu.datePaiement(), NORMAL));
            doc.add(new Paragraph(" "));

            PdfPTable t = tableau(new String[] {"Matricule", "Ouvrier", "Jours payés", "Salaire / jour (MAD)", "Montant (MAD)"});
            for (LigneRecu l : recu.lignes()) {
                cellule(t, l.matricule(), Element.ALIGN_LEFT);
                cellule(t, l.nomPrenom(), Element.ALIGN_LEFT);
                cellule(t, String.valueOf(l.jours()), Element.ALIGN_RIGHT);
                cellule(t, nombre(l.salaireJournalier()), Element.ALIGN_RIGHT);
                cellule(t, nombre(l.montant()), Element.ALIGN_RIGHT);
            }
            doc.add(t);
            doc.add(new Paragraph(" "));
            Paragraph total = new Paragraph("TOTAL PAYÉ : " + nombre(recu.total()) + " MAD", GRAS);
            total.setAlignment(Element.ALIGN_RIGHT);
            doc.add(total);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Signature de l'agent RH : ____________", NORMAL));
        });
    }

    /**
     * Exporte un tableau quelconque en PDF (ex. tableau de bord).
     *
     * @param titre    titre du document
     * @param entetes  en-tetes de colonnes
     * @param lignes   lignes de donnees (chaque tableau a autant de cases que d'en-tetes)
     * @param sortie   fichier PDF a creer
     */
    public static void exportTableau(String titre, String[] entetes, List<String[]> lignes, Path sortie) {
        ecrire(sortie, doc -> {
            doc.add(new Paragraph(titre, TITRE));
            doc.add(new Paragraph("Édité le " + java.time.LocalDate.now(), NORMAL));
            doc.add(new Paragraph(" "));
            PdfPTable t = tableau(entetes);
            for (String[] ligne : lignes) {
                for (String valeur : ligne) {
                    cellule(t, valeur, Element.ALIGN_LEFT);
                }
            }
            doc.add(t);
        });
    }

    /**
     * Formate un montant avec 2 decimales et un espace comme separateur de milliers.
     *
     * @param valeur la valeur a formater
     * @return le texte (ex. 12 345.50)
     */
    static String nombre(BigDecimal valeur) {
        return String.format(Locale.US, "%,.2f", valeur).replace(',', ' ');
    }

    @FunctionalInterface
    private interface Contenu {
        void remplir(Document doc) throws Exception;
    }

    private static void ecrire(Path sortie, Contenu contenu) {
        try (OutputStream out = Files.newOutputStream(sortie)) {
            Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();
            contenu.remplir(doc);
            doc.close();
        } catch (Exception e) {
            throw new IllegalStateException("Génération du PDF impossible : " + e.getMessage(), e);
        }
    }

    private static PdfPTable tableau(String[] entetes) {
        PdfPTable t = new PdfPTable(entetes.length);
        t.setWidthPercentage(100);
        for (String e : entetes) {
            PdfPCell c = new PdfPCell(new Phrase(e, ENTETE));
            c.setBackgroundColor(VERT);
            c.setPadding(6);
            t.addCell(c);
        }
        return t;
    }

    private static void cellule(PdfPTable t, String texte, int alignement) {
        PdfPCell c = new PdfPCell(new Phrase(texte == null ? "" : texte, NORMAL));
        c.setHorizontalAlignment(alignement);
        c.setPadding(5);
        t.addCell(c);
    }
}