package it.polimi.tiw.project.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.polimi.tiw.project.beans.AdminDtos;
import it.polimi.tiw.project.beans.Project;
import it.polimi.tiw.project.beans.User;

/**
 * DAO dedicato all'interfaccia dell'AMMINISTRATORE (versione JavaScript).
 *
 * <p>Copre: elenco del personale tecnico assegnabile come responsabile, elenco dei
 * progetti creati dall'amministratore, struttura gerarchica di un progetto per la
 * pagina VERIFICA PROGETTI e salvataggio transazionale dell'intero progetto
 * (progetto + WP + task) creato lato client.</p>
 *
 * <p>La numerazione di WP e task e' delegata ai trigger del database
 * ({@code trg_id_wp}, {@code trg_id_task}), che assegnano {@code numero_ordine}
 * come MAX+1: inserendo gli elementi in ordine dentro un'unica transazione, i WP di
 * un progetto nuovo ricevono 1..N e i task di ogni WP 1..M.</p>
 */
public class AdminDAO extends DAO {

    public AdminDAO(Connection connection) {
        super(connection);
    }

    /* ===================================================================== */
    /*  LETTURE                                                               */
    /* ===================================================================== */

    /** personale tecnico assegnabile come responsabile (id, nome, cognome) */
    public List<User> findTechnicalStaff() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT id, nome, cognome FROM Tecnico ORDER BY cognome, nome";
        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("nome"));
                u.setSurname(rs.getString("cognome"));
                users.add(u);
            }
        }
        return users;
    }

    /** true se l'id corrisponde a un utente del personale tecnico */
    public boolean isTecnico(int id) throws SQLException {
        String query = "SELECT 1 FROM Tecnico WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.isBeforeFirst();
            }
        }
    }

    /** true se esiste gia' un progetto con quel titolo (titolo e' PK) */
    public boolean projectExists(String titolo) throws SQLException {
        String query = "SELECT 1 FROM Progetto WHERE titolo = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, titolo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.isBeforeFirst();
            }
        }
    }

    /** progetti creati da questo amministratore (titolo, durata, stato) */
    public List<Project> findAdminProjects(int adminId) throws SQLException {
        List<Project> projects = new ArrayList<>();
        String query = "SELECT titolo, durata, stato FROM Progetto WHERE amministratore = ? ORDER BY titolo";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Project p = new Project();
                    p.setTitolo(rs.getString("titolo"));
                    p.setDurata(rs.getInt("durata"));
                    p.setStato(rs.getString("stato"));
                    p.setAdmin(adminId);
                    projects.add(p);
                }
            }
        }
        return projects;
    }

    /** true se il progetto esiste ed e' stato creato da questo amministratore */
    public boolean isAdminOf(int adminId, String titolo) throws SQLException {
        String query = "SELECT 1 FROM Progetto WHERE titolo = ? AND amministratore = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, titolo);
            ps.setInt(2, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.isBeforeFirst();
            }
        }
    }

    /* piccolo holder interno per materializzare le righe prima delle query annidate */
    private static class Row {
        int ord;
        String titolo;
        int mi;
        int mf;
    }

    /**
     * Costruisce la struttura gerarchica del progetto per VERIFICA PROGETTI:
     * per ogni WP i suoi task con mese inizio/fine e i totali (su tutti i mesi e,
     * per le ore lavorate, su tutti i collaboratori).
     */
    public AdminDtos.Structure getProjectStructure(String progetto) throws SQLException {
        AdminDtos.Structure structure = new AdminDtos.Structure();
        structure.title = progetto;
        structure.wps = new ArrayList<>();

        List<Row> wps = new ArrayList<>();
        String wpQuery = "SELECT numero_ordine, titolo, Mese_inizio, Mese_fine "
                + "FROM Work_Package WHERE progetto = ? ORDER BY numero_ordine";
        try (PreparedStatement ps = connection.prepareStatement(wpQuery)) {
            ps.setString(1, progetto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Row r = new Row();
                    r.ord = rs.getInt("numero_ordine");
                    r.titolo = rs.getString("titolo");
                    r.mi = rs.getInt("Mese_inizio");
                    r.mf = rs.getInt("Mese_fine");
                    wps.add(r);
                }
            }
        }

        for (Row wp : wps) {
            AdminDtos.Wp w = new AdminDtos.Wp();
            w.label = "WP" + wp.ord + ": " + wp.titolo;
            w.meseInizio = wp.mi;
            w.meseFine = wp.mf;
            w.tasks = new ArrayList<>();

            List<Row> tasks = new ArrayList<>();
            String taskQuery = "SELECT numero_ordine, titolo, Mese_inizio, Mese_fine "
                    + "FROM Task WHERE progetto = ? AND wp = ? ORDER BY numero_ordine";
            try (PreparedStatement ps = connection.prepareStatement(taskQuery)) {
                ps.setString(1, progetto);
                ps.setInt(2, wp.ord);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Row r = new Row();
                        r.ord = rs.getInt("numero_ordine");
                        r.titolo = rs.getString("titolo");
                        r.mi = rs.getInt("Mese_inizio");
                        r.mf = rs.getInt("Mese_fine");
                        tasks.add(r);
                    }
                }
            }

            for (Row t : tasks) {
                AdminDtos.TaskItem ti = new AdminDtos.TaskItem();
                ti.label = "T" + wp.ord + "." + t.ord + ": " + t.titolo;
                ti.meseInizio = t.mi;
                ti.meseFine = t.mf;
                ti.orePreviste = sumPlannedHours(progetto, wp.ord, t.ord);
                ti.oreLavorate = sumWorkedHours(progetto, wp.ord, t.ord);
                w.tasks.add(ti);
            }
            structure.wps.add(w);
        }
        return structure;
    }

    /** somma delle ore previste del task su tutti i mesi (Task_has_Mese) */
    private int sumPlannedHours(String progetto, int wp, int task) throws SQLException {
        String query = "SELECT COALESCE(SUM(ore_previste), 0) AS s FROM Task_has_Mese "
                + "WHERE progetto = ? AND wp = ? AND task = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, progetto);
            ps.setInt(2, wp);
            ps.setInt(3, task);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("s") : 0;
            }
        }
    }

    /** somma delle ore lavorate del task su tutti i collaboratori e mesi (Task_ha_Tecnico) */
    private int sumWorkedHours(String progetto, int wp, int task) throws SQLException {
        String query = "SELECT COALESCE(SUM(ore_effettive), 0) AS s FROM Task_ha_Tecnico "
                + "WHERE progetto = ? AND wp = ? AND task = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, progetto);
            ps.setInt(2, wp);
            ps.setInt(3, task);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("s") : 0;
            }
        }
    }

    /* ===================================================================== */
    /*  SALVATAGGIO DELL'INTERO PROGETTO                                      */
    /* ===================================================================== */

    /**
     * Inserisce in un'unica transazione il progetto (stato CREATO), i suoi WP e i
     * relativi task. La numerazione di WP/task e' assegnata dai trigger del DB. Il
     * responsabile viene collegato dal trigger {@code trg_progetto_insert}
     * (aggiornamento di {@code Tecnico.isManager}).
     *
     * <p>Presuppone che i dati siano gia' stati validati a monte; qualunque errore SQL
     * provoca il rollback completo (nessun progetto parziale resta nel database).</p>
     */
    public void createProject(int adminId, AdminDtos.SaveProjectRequest req) throws SQLException {
        String insProject = "INSERT INTO Progetto (titolo, durata, stato, responsabile, amministratore) "
                + "VALUES (?, ?, 'CREATO', ?, ?)";
        String insWp = "INSERT INTO Work_Package (numero_ordine, titolo, progetto, Mese_inizio, Mese_fine) "
                + "VALUES (?, ?, ?, ?, ?)";
        String insTask = "INSERT INTO Task (numero_ordine, titolo, descrizione, wp, progetto, Mese_inizio, Mese_fine) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement(insProject)) {
                ps.setString(1, req.titolo);
                ps.setInt(2, req.durata);
                ps.setInt(3, req.responsabile);
                ps.setInt(4, adminId);
                ps.executeUpdate();
            }

            for (int i = 0; i < req.wps.size(); i++) {
                AdminDtos.WpReq wp = req.wps.get(i);
                int wpOrder = i + 1;   // coerente con la numerazione assegnata dal trigger

                try (PreparedStatement ps = connection.prepareStatement(insWp)) {
                    ps.setInt(1, wpOrder);            // valore riscritto dal trigger trg_id_wp
                    ps.setString(2, wp.titolo);
                    ps.setString(3, req.titolo);
                    ps.setInt(4, wp.meseInizio);
                    ps.setInt(5, wp.meseFine);
                    ps.executeUpdate();
                }

                for (int j = 0; j < wp.tasks.size(); j++) {
                    AdminDtos.TaskReq t = wp.tasks.get(j);
                    int taskOrder = j + 1;   // coerente con trg_id_task
                    String descr = (t.descrizione != null) ? t.descrizione : "";

                    try (PreparedStatement ps = connection.prepareStatement(insTask)) {
                        ps.setInt(1, taskOrder);      // valore riscritto dal trigger trg_id_task
                        ps.setString(2, t.titolo);
                        ps.setString(3, descr);
                        ps.setInt(4, wpOrder);
                        ps.setString(5, req.titolo);
                        ps.setInt(6, t.meseInizio);
                        ps.setInt(7, t.meseFine);
                        ps.executeUpdate();
                    }
                }
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
}
