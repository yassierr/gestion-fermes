package ma.ferme.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import ma.ferme.model.enums.StatutVente;
import org.junit.jupiter.api.Test;

/** Tests des modeles {@link Vente} et {@link LigneVente}. */
class VenteTest {

    private LigneVente ligne(int ferme, String kg, String prix) {
        return new LigneVente(ferme, new BigDecimal(kg), new BigDecimal(prix));
    }

    @Test
    void montantDeLigneEstQuantiteFoisPrix() {
        assertEquals(new BigDecimal("1250.00"), ligne(1, "100", "12.50").getMontant());
    }

    @Test
    void montantEstArrondiADeuxDecimales() {
        assertEquals(new BigDecimal("3.33"), ligne(1, "0.333", "10").getMontant());
    }

    @Test
    void totalEtQuantiteTotaleSommentLesLignes() {
        Vente v = new Vente();
        v.ajouterLigne(ligne(1, "100", "10"));
        v.ajouterLigne(ligne(2, "50", "20"));
        assertEquals(new BigDecimal("2000.00"), v.getTotal());
        assertEquals(new BigDecimal("150"), v.getQuantiteTotale());
    }

    @Test
    void uneNouvelleVenteEstLivreeEtSansLigne() {
        Vente v = new Vente();
        assertEquals(StatutVente.LIVREE, v.getStatut());
        assertTrue(v.getLignes().isEmpty());
        assertEquals(BigDecimal.ZERO, v.getTotal());
    }
}