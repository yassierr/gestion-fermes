package ma.ferme.dao;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ma.ferme.config.DatabaseConnection;
import ma.ferme.model.LigneVente;
import ma.ferme.model.Vente;
import ma.ferme.model.enums.StatutVente;

/**
 * Acces aux donnees des tables {@code vente} et {@code ligne_vente}.
 * La creation d'une livraison (vente + lignes + numero de bon) se fait dans
 * <b>une seule transaction</b> : tout est enregistre ou rien.
 *
 * @author Yassir Hikou
 * @version 1.0
 */
public class VenteDao implements GenericDao<Vente> {

    private static final String VENTE_COLS =
            "id, numero_bon, date_livraison, statut, date_paiement, id_client, id_admin";
    private static final String LIGNE_COLS = "id, quantite_kg, prix_kg, id_vente, id_ferme";

    /**
     * Equivalent de {@link #insertAvecLignes(Vente)}.
     *
     * @param v la livraison a enregistrer
     */
    @Override
    public void insert(Vente v) {
        insertAvecLignes(v);
    }

    /**
     * Enregistre une livraison complete dans une transaction : genere le numero
     * de bon {@code BL-AAAA-NNNN}, insere la vente puis toutes ses lignes.
     * En cas d'erreur (ex. ferme inexistante), aucune donnee n'est conservee.
     * Apres succes, l'identifiant, le numero de bon et les identifiants des lignes
     * sont recopies dans l'objet.
     *
     * @param v la livraison (date de livraison et au moins une ligne requises)
     * @throws DaoException si l'enregistrement echoue (la transaction est annulee)
     */
    public void insertAvecLignes(Vente v) {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (Statement st = c.createStatement()) {
                    st.execute("LOCK TABLE vente IN SHARE ROW EXCLUSIVE MODE");
                }
                String numero = prochainNumero(c, v.getDateLivraison().getYear());
                int idVente = insererVente(c, v, numero);
                List<Integer> idsLignes = insererLignes(c, v.getLignes(), idVente);
                c.commit();
                v.setId(idVente);
                v.setNumeroBon(numero);
                for (int i = 0; i < idsLignes.size(); i++) {
                    v.getLignes().get(i).setId(idsLignes.get(i));
                    v.getLignes().get(i).setIdVente(idVente);
                }
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException("Enregistrement de la livraison impossible : " + e.getMessage(), e);
        }
    }

    private String prochainNumero(Connection c, int annee) throws SQLException {
        String prefix = "BL-" + annee + "-";
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_bon FROM 9) AS INTEGER)), 0) + 1 "
                + "FROM vente WHERE numero_bon LIKE ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return prefix + String.format("%04d", rs.getInt(1));
                }
                return prefix + "0001";
            }
        }
    }

    private int insererVente(Connection c, Vente v, String numero) throws SQLException {
        String sql = "INSERT INTO vente(numero_bon, date_livraison, statut, date_paiement, id_client, id_admin) "
                + "VALUES (?, ?, ?::statut_vente, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, numero);
            ps.setDate(2, Date.valueOf(v.getDateLivraison()));
            ps.setString(3, v.getStatut().name());
            setDateOuNull(ps, 4, v.getDatePaiement());
            ps.setInt(5, v.getIdClient());
            ps.setInt(6, v.getIdAdmin());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new SQLException("Échec de la récupération de l'ID de la vente insérée.");
            }
        }
    }

    private List<Integer> insererLignes(Connection c, List<LigneVente> lignes, int idVente) throws SQLException {
        String sql = "INSERT INTO ligne_vente(quantite_kg, prix_kg, id_vente, id_ferme) VALUES (?, ?, ?, ?) RETURNING id";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (LigneVente l : lignes) {
                ps.setBigDecimal(1, l.getQuantiteKg());
                ps.setBigDecimal(2, l.getPrixKg());
                ps.setInt(3, idVente);
                ps.setInt(4, l.getIdFerme());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ids.add(rs.getInt(1));
                    } else {
                        throw new SQLException("Échec de la récupération de l'ID de la ligne de vente insérée.");
                    }
                }
            }
        }
        return ids;
    }

    /**
     * Recherche une livraison avec ses lignes.
     *
     * @param id identifiant de la vente
     * @return la vente, ou un Optional vide
     */
    @Override
    public Optional<Vente> findById(int id) {
        List<Vente> r = chargerVentes("SELECT " + VENTE_COLS + " FROM vente WHERE id = ?", id);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    /**
     * Retourne toutes les livraisons avec leurs lignes, les plus recentes d'abord.
     *
     * @return la liste des ventes
     */
    @Override
    public List<Vente> findAll() {
        return chargerVentes("SELECT " + VENTE_COLS + " FROM vente ORDER BY date_livraison DESC, id DESC", null);
    }

    /**
     * Retourne les livraisons contenant au moins une ligne de la ferme donnee
     * (utilise par l'espace Responsable).
     *
     * @param idFerme identifiant de la ferme
     * @return la liste des ventes concernees, avec toutes leurs lignes
     */
    public List<Vente> findByFerme(int idFerme) {
        String sql = "SELECT " + VENTE_COLS + " FROM vente WHERE id IN "
                + "(SELECT id_vente FROM ligne_vente WHERE id_ferme = ?) "
                + "ORDER BY date_livraison DESC, id DESC";
        return chargerVentes(sql, idFerme);
    }

    /**
     * Met a jour le statut, la date de paiement et la date de livraison d'une vente.
     * Les lignes ne sont pas modifiables.
     *
     * @param v la vente avec ses nouvelles valeurs
     * @throws DaoException si la mise a jour echoue
     */
    @Override
    public void update(Vente v) {
        String sql = "UPDATE vente SET statut = ?::statut_vente, date_paiement = ?, date_livraison = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.getStatut().name());
            setDateOuNull(ps, 2, v.getDatePaiement());
            ps.setDate(3, Date.valueOf(v.getDateLivraison()));
            ps.setInt(4, v.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Mise a jour de la vente impossible : " + e.getMessage(), e);
        }
    }

    /**
     * Supprime une vente et ses lignes (suppression en cascade).
     *
     * @param id identifiant de la vente
     */
    @Override
    public void delete(int id) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM vente WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Suppression de la vente impossible : " + e.getMessage(), e);
        }
    }

    /**
     * Calcule le stock disponible d'une ferme : somme des recoltes moins somme des
     * lignes de vente non annulees. Le stock n'est jamais stocke en base.
     *
     * @param idFerme identifiant de la ferme
     * @return le stock disponible en kg
     */
    public BigDecimal stockDisponible(int idFerme) {
        String sql = "SELECT COALESCE((SELECT SUM(poids_kg) FROM recolte WHERE id_ferme = ?), 0) "
                + "- COALESCE((SELECT SUM(lv.quantite_kg) FROM ligne_vente lv "
                + "JOIN vente v ON v.id = lv.id_vente WHERE lv.id_ferme = ? AND v.statut <> 'ANNULEE'), 0)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idFerme);
            ps.setInt(2, idFerme);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
                return BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new DaoException("Calcul du stock impossible : " + e.getMessage(), e);
        }
    }

    /**
     * Chiffre d'affaires : montant de toutes les livraisons non annulees.
     *
     * @return le CA en MAD
     */
    public BigDecimal chiffreAffaires() {
        return somme("v.statut <> 'ANNULEE'");
    }

    /**
     * Gains encaisses : montant des livraisons payees.
     *
     * @return les gains en MAD
     */
    public BigDecimal gainsEncaisses() {
        return somme("v.statut = 'PAYEE'");
    }

    /**
     * Creances : montant des livraisons livrees mais pas encore payees.
     *
     * @return les creances en MAD
     */
    public BigDecimal creances() {
        return somme("v.statut = 'LIVREE'");
    }

    private BigDecimal somme(String conditionStatut) {
        String sql = "SELECT COALESCE(SUM(lv.quantite_kg * lv.prix_kg), 0) FROM ligne_vente lv "
                + "JOIN vente v ON v.id = lv.id_vente WHERE " + conditionStatut;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
            return BigDecimal.ZERO;
        } catch (SQLException e) {
            throw new DaoException("Calcul du montant impossible : " + e.getMessage(), e);
        }
    }

    private List<Vente> chargerVentes(String sql, Object param) {
        try (Connection c = DatabaseConnection.getConnection()) {
            List<Vente> ventes = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                if (param != null) {
                    ps.setObject(1, param);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ventes.add(mapVente(rs));
                    }
                }
            }
            chargerLignes(c, ventes);
            return ventes;
        } catch (SQLException e) {
            throw new DaoException("Lecture des ventes impossible : " + e.getMessage(), e);
        }
    }

    private void chargerLignes(Connection c, List<Vente> ventes) throws SQLException {
        if (ventes.isEmpty()) {
            return;
        }
        Map<Integer, Vente> parId = new HashMap<>();
        for (Vente v : ventes) {
            parId.put(v.getId(), v);
        }
        Array ids = c.createArrayOf("integer", parId.keySet().toArray());
        String sql = "SELECT " + LIGNE_COLS + " FROM ligne_vente WHERE id_vente = ANY(?) ORDER BY id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setArray(1, ids);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LigneVente l = new LigneVente();
                    l.setId(rs.getInt("id"));
                    l.setQuantiteKg(rs.getBigDecimal("quantite_kg"));
                    l.setPrixKg(rs.getBigDecimal("prix_kg"));
                    l.setIdVente(rs.getInt("id_vente"));
                    l.setIdFerme(rs.getInt("id_ferme"));
                    parId.get(l.getIdVente()).ajouterLigne(l);
                }
            }
        }
    }

    private Vente mapVente(ResultSet rs) throws SQLException {
        Vente v = new Vente();
        v.setId(rs.getInt("id"));
        v.setNumeroBon(rs.getString("numero_bon"));
        v.setDateLivraison(rs.getDate("date_livraison").toLocalDate());
        v.setStatut(StatutVente.valueOf(rs.getString("statut")));
        Date dp = rs.getDate("date_paiement");
        v.setDatePaiement(dp == null ? null : dp.toLocalDate());
        v.setIdClient(rs.getInt("id_client"));
        v.setIdAdmin(rs.getInt("id_admin"));
        return v;
    }

    private void setDateOuNull(PreparedStatement ps, int index, java.time.LocalDate d) throws SQLException {
        if (d == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(d));
        }
    }
}