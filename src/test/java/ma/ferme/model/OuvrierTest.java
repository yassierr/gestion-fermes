package ma.ferme.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OuvrierTest {

    @Test
    @DisplayName("Devrait créer un ouvrier avec le constructeur sans ID et initialiser actif à true")
    void testConstructeurCreation() {
        BigDecimal salaire = new BigDecimal("150.00");
        Ouvrier ouvrier = new Ouvrier(1, "Amghar", "Brahim", "JA18294", "0661448912", "Douar Tin Mansour", "Cueillette Serre B2", salaire);

        assertEquals(1, ouvrier.getIdFerme());
        assertEquals("Amghar", ouvrier.getNom());
        assertEquals("Brahim", ouvrier.getPrenom());
        assertEquals("JA18294", ouvrier.getCin());
        assertEquals("0661448912", ouvrier.getTelephone());
        assertEquals("Douar Tin Mansour", ouvrier.getAdresse());
        assertEquals("Cueillette Serre B2", ouvrier.getPoste());
        assertEquals(salaire, ouvrier.getSalaireJournalier());
        assertTrue(ouvrier.isActif());
    }

    @Test
    @DisplayName("Devrait générer le matricule au format AS-XXXX lors du setId")
    void testGenererMatricule() {
        Ouvrier ouvrier = new Ouvrier(1, "Amghar", "Brahim", "JA18294", "0661448912", "Tin Mansour", "Serre B2", new BigDecimal("150.00"));
        
        ouvrier.setId(42);
        
        assertEquals(42, ouvrier.getId());
        assertEquals("AS-0042", ouvrier.getMatricule());
    }

    @Test
    @DisplayName("Devrait créer un ouvrier complet depuis la base de données (Constructeur DAO)")
    void testConstructeurDao() {
        BigDecimal salaire = new BigDecimal("180.00");
        Ouvrier ouvrier = new Ouvrier(10, "AS-0010", 2, "Idoubihi", "Fatima", "JE29941", "0672153308", "Biougra", "Cueillette Serre B1", salaire, false);

        assertEquals(10, ouvrier.getId());
        assertEquals("AS-0010", ouvrier.getMatricule());
        assertEquals(2, ouvrier.getIdFerme());
        assertEquals("Idoubihi", ouvrier.getNom());
        assertFalse(ouvrier.isActif());
    }

    @Test
    @DisplayName("Devrait calculer correctement le montant pour un nombre de jours travaillés")
    void testCalculerMontant_CasNormal() {
        Ouvrier ouvrier = new Ouvrier(1, "Amghar", "Brahim", "JA18294", "0600000000", "Biougra", "Serre B2", new BigDecimal("150.00"));

        // 2.5 jours x 150.00 = 375.00
        BigDecimal result = ouvrier.calculerMontant(2.5);

        assertEquals(0, new BigDecimal("375.00").compareTo(result));
    }

    @Test
    @DisplayName("Devrait retourner 0.00 si 0 jour travaillé")
    void testCalculerMontant_ZeroJours() {
        Ouvrier ouvrier = new Ouvrier(1, "Amghar", "Brahim", "JA18294", "0600000000", "Biougra", "Serre B2", new BigDecimal("150.00"));

        BigDecimal result = ouvrier.calculerMontant(0);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    @DisplayName("Devrait lever une exception si le nombre de jours est négatif")
    void testCalculerMontant_JoursNegatifs_LanceException() {
        Ouvrier ouvrier = new Ouvrier(1, "Amghar", "Brahim", "JA18294", "0600000000", "Biougra", "Serre B2", new BigDecimal("150.00"));

        assertThrows(IllegalArgumentException.class, () -> {
            ouvrier.calculerMontant(-1.5);
        });
    }

    @Test
    @DisplayName("Devrait permettre la mise à jour des données via les setters")
    void testSetters() {
        Ouvrier ouvrier = new Ouvrier(1, "NomInitial", "PrenomInitial", "CIN1", "0606060606", "Adresse1", "Poste1", new BigDecimal("100.00"));

        ouvrier.setNom("NouveauNom");
        ouvrier.setPrenom("NouveauPrenom");
        ouvrier.setCin("CIN2");
        ouvrier.setTelephone("0707070707");
        ouvrier.setAdresse("Adresse2");
        ouvrier.setPoste("Poste2");
        ouvrier.setSalaireJournalier(new BigDecimal("200.00"));
        ouvrier.setActif(false);

        assertEquals("NouveauNom", ouvrier.getNom());
        assertEquals("NouveauPrenom", ouvrier.getPrenom());
        assertEquals("CIN2", ouvrier.getCin());
        assertEquals("0707070707", ouvrier.getTelephone());
        assertEquals("Adresse2", ouvrier.getAdresse());
        assertEquals("Poste2", ouvrier.getPoste());
        assertEquals(new BigDecimal("200.00"), ouvrier.getSalaireJournalier());
        assertFalse(ouvrier.isActif());
    }
}