package it.polimi.tiw.project.dao;

import it.polimi.tiw.project.beans.CollaboratorBoard;
import it.polimi.tiw.project.beans.Project;
import it.polimi.tiw.project.beans.Task;
import it.polimi.tiw.project.beans.WorkPackage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO dedicato all'interfaccia del COLLABORATORE (HOME COLLABORATORE).
 *
 * <p>Le letture sono sempre filtrate sul collaboratore corrente: vengono mostrati
 * soltanto i progetti, i WP e i task in cui il collaboratore &egrave; assegnato ad almeno
 * un task (cio&egrave; ha almeno una riga in {@code Task_ha_Tecnico}).</p>
 *
 * <p>Riepilogo dello schema usato:</p>
 * <ul>
 *   <li>{@code Progetto(titolo PK, durata, stato, responsabile, amministratore)}</li>
 *   <li>{@code Work_Package(numero_ordine, progetto) PK, titolo}</li>
 *   <li>{@code Task(numero_ordine, wp, progetto) PK, titolo, descrizione, Mese_inizio, Mese_fine}</li>
 *   <li>{@code Task_has_Mese(mese, task, wp, progetto) PK, ore_previste} &rarr; ore PREVISTE per task/mese</li>
 *   <li>{@code Task_ha_Tecnico(task, wp, progetto, collaboratore, mese) PK, ore_effettive}
 *       &rarr; legame collaboratore&harr;task per mese; {@code ore_effettive} contiene le ore
 *       LAVORATE dal collaboratore in quel mese (NULL &rarr; trattato come 0)</li>
 *   <li>{@code Tecnico(id PK, nome, cognome, ...)}</li>
 * </ul>
 */
public class CollaboratorDAO extends DAO {

	public CollaboratorDAO(Connection connection) {
		super(connection);
	}

	/* ===================================================================== */
	/*  DATI DEL COLLABORATORE                                                */
	/* ===================================================================== */

	/** nome e cognome del collaboratore (per il saluto della HOME); null se non trovato */
	public String findCollaboratorFullName(int collabId) throws SQLException {
		String query = "SELECT nome, cognome FROM Tecnico WHERE id = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, collabId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getString("nome") + " " + rs.getString("cognome");
				return null;
			}
		}
	}

	/* ===================================================================== */
	/*  ELENCHI (SEMPRE FILTRATI SUL COLLABORATORE)                          */
	/* ===================================================================== */

	/**
	 * Progetti in cui il collaboratore &egrave; assegnato ad almeno un task
	 * (per popolare l'elenco della HOME COLLABORATORE).
	 */
	public List<Project> findCollaboratorProjects(int collabId) throws SQLException {
		List<Project> projects = new ArrayList<>();
		String query = "SELECT DISTINCT p.titolo AS titolo, p.durata AS durata, p.stato AS stato "
				+ "FROM Task_ha_Tecnico tht "
				+ "JOIN Progetto p ON tht.progetto = p.titolo "
				+ "WHERE tht.collaboratore = ? "
				+ "ORDER BY p.titolo";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, collabId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Project p = new Project();
					p.setTitolo(rs.getString("titolo"));
					p.setDurata(rs.getInt("durata"));
					p.setStato(rs.getString("stato"));
					projects.add(p);
				}
			}
		}
		return projects;
	}

	/** true se il collaboratore &egrave; assegnato ad almeno un task del progetto (autorizzazione) */
	public boolean isAssignedToProject(int collabId, String progetto) throws SQLException {
		String query = "SELECT 1 FROM Task_ha_Tecnico WHERE collaboratore = ? AND progetto = ? LIMIT 1";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, collabId);
			ps.setString(2, progetto);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.isBeforeFirst();
			}
		}
	}

	/**
	 * WP del progetto per cui il collaboratore &egrave; assegnato ad almeno un task.
	 */
	public List<WorkPackage> findCollaboratorWorkPackages(int collabId, String progetto) throws SQLException {
		List<WorkPackage> wps = new ArrayList<>();
		String query = "SELECT DISTINCT w.numero_ordine AS numero_ordine, w.titolo AS titolo "
				+ "FROM Task_ha_Tecnico tht "
				+ "JOIN Work_Package w ON tht.wp = w.numero_ordine AND tht.progetto = w.progetto "
				+ "WHERE tht.collaboratore = ? AND tht.progetto = ? "
				+ "ORDER BY w.numero_ordine";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, collabId);
			ps.setString(2, progetto);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					WorkPackage wp = new WorkPackage();
					wp.setIdOrder(rs.getInt("numero_ordine"));
					wp.setTitle(rs.getString("titolo"));
					wps.add(wp);
				}
			}
		}
		return wps;
	}

	/**
	 * Task del WP per cui il collaboratore &egrave; assegnato.
	 */
	public List<Task> findCollaboratorTasks(int collabId, String progetto, int wp) throws SQLException {
		List<Task> tasks = new ArrayList<>();
		String query = "SELECT DISTINCT t.numero_ordine AS numero_ordine, t.titolo AS titolo "
				+ "FROM Task_ha_Tecnico tht "
				+ "JOIN Task t ON tht.task = t.numero_ordine AND tht.wp = t.wp AND tht.progetto = t.progetto "
				+ "WHERE tht.collaboratore = ? AND tht.progetto = ? AND tht.wp = ? "
				+ "ORDER BY t.numero_ordine";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, collabId);
			ps.setString(2, progetto);
			ps.setInt(3, wp);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Task t = new Task();
					t.setIdOrder(rs.getInt("numero_ordine"));
					t.setTitle(rs.getString("titolo"));
					tasks.add(t);
				}
			}
		}
		return tasks;
	}

	/** true se il collaboratore &egrave; assegnato allo specifico task (autorizzazione) */
	public boolean isAssignedToTask(int collabId, String progetto, int wp, int task) throws SQLException {
		String query = "SELECT 1 FROM Task_ha_Tecnico "
				+ "WHERE collaboratore = ? AND progetto = ? AND wp = ? AND task = ? LIMIT 1";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, collabId);
			ps.setString(2, progetto);
			ps.setInt(3, wp);
			ps.setInt(4, task);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.isBeforeFirst();
			}
		}
	}

	/* ===================================================================== */
	/*  DETTAGLIO DEL TASK                                                    */
	/* ===================================================================== */

	/** singolo task (titolo, descrizione, mesi di inizio/fine); null se non trovato */
	public Task findTask(String progetto, int wp, int task) throws SQLException {
		String query = "SELECT numero_ordine, titolo, descrizione, Mese_inizio, Mese_fine "
				+ "FROM Task WHERE progetto = ? AND wp = ? AND numero_ordine = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			ps.setInt(2, wp);
			ps.setInt(3, task);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next())
					return null;
				Task t = new Task();
				t.setIdOrder(rs.getInt("numero_ordine"));
				t.setTitle(rs.getString("titolo"));
				t.setDescription(rs.getString("descrizione"));

				int start = rs.getInt("Mese_inizio");
				if (!rs.wasNull())
					t.setStartMonth(start);
				int end = rs.getInt("Mese_fine");
				if (!rs.wasNull())
					t.setEndMonth(end);
				return t;
			}
		}
	}

	/** somma delle ore PREVISTE (tutti i mesi) per il task; 0 se assenti */
	public int findTotalPlannedHours(String progetto, int wp, int task) throws SQLException {
		String query = "SELECT SUM(ore_previste) AS ore FROM Task_has_Mese "
				+ "WHERE progetto = ? AND wp = ? AND task = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			ps.setInt(2, wp);
			ps.setInt(3, task);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					int ore = rs.getInt("ore");
					return rs.wasNull() ? 0 : ore;
				}
				return 0;
			}
		}
	}

	/** somma delle ore LAVORATE dal collaboratore (tutti i mesi) per il task; 0 se assenti */
	public int findTotalWorkedHours(int collabId, String progetto, int wp, int task) throws SQLException {
		String query = "SELECT SUM(ore_effettive) AS ore FROM Task_ha_Tecnico "
				+ "WHERE collaboratore = ? AND progetto = ? AND wp = ? AND task = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, collabId);
			ps.setString(2, progetto);
			ps.setInt(3, wp);
			ps.setInt(4, task);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					int ore = rs.getInt("ore");
					return rs.wasNull() ? 0 : ore;
				}
				return 0;
			}
		}
	}

	/**
	 * Mesi in cui il collaboratore &egrave; assegnato al task: sono gli unici mesi per cui
	 * pu&ograve; registrare ore lavorate (una riga in {@code Task_ha_Tecnico} per mese).
	 * Alimenta il menu a tendina del form SALVA.
	 */
	public List<Integer> findAssignedMonths(int collabId, String progetto, int wp, int task) throws SQLException {
		List<Integer> months = new ArrayList<>();
		String query = "SELECT mese FROM Task_ha_Tecnico "
				+ "WHERE collaboratore = ? AND progetto = ? AND wp = ? AND task = ? "
				+ "ORDER BY mese";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, collabId);
			ps.setString(2, progetto);
			ps.setInt(3, wp);
			ps.setInt(4, task);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					months.add(rs.getInt("mese"));
			}
		}
		return months;
	}

	/**
	 * mese &rarr; ore lavorate dal collaboratore per il task (NULL trattato come 0).
	 * Usato per il riepilogo di sola lettura mostrato accanto al form.
	 */
	public Map<Integer, Integer> findWorkedHoursByMonth(int collabId, String progetto, int wp, int task)
			throws SQLException {
		Map<Integer, Integer> worked = new LinkedHashMap<>();
		String query = "SELECT mese, ore_effettive FROM Task_ha_Tecnico "
				+ "WHERE collaboratore = ? AND progetto = ? AND wp = ? AND task = ? "
				+ "ORDER BY mese";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, collabId);
			ps.setString(2, progetto);
			ps.setInt(3, wp);
			ps.setInt(4, task);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int mese = rs.getInt("mese");
					int ore = rs.getInt("ore_effettive");
					worked.put(mese, rs.wasNull() ? 0 : ore);
				}
			}
		}
		return worked;
	}

	/* ===================================================================== */
	/*  SALVA: REGISTRAZIONE DELLE ORE LAVORATE                              */
	/* ===================================================================== */

	/**
	 * Registra (in sostituzione del valore precedente, default 0) le ore lavorate dal
	 * collaboratore nel task, nel mese indicato. L'aggiornamento tocca solo la riga
	 * gi&agrave; esistente in {@code Task_ha_Tecnico}: se il collaboratore non &egrave; assegnato a
	 * quel mese nessuna riga viene aggiornata.
	 *
	 * @return true se una riga &egrave; stata aggiornata (mese valido per il collaboratore)
	 */
	public boolean updateWorkedHours(int collabId, String progetto, int wp, int task, int mese, int ore)
			throws SQLException {
		String query = "UPDATE Task_ha_Tecnico SET ore_effettive = ? "
				+ "WHERE collaboratore = ? AND progetto = ? AND wp = ? AND task = ? AND mese = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, ore);
			ps.setInt(2, collabId);
			ps.setString(3, progetto);
			ps.setInt(4, wp);
			ps.setInt(5, task);
			ps.setInt(6, mese);
			return ps.executeUpdate() > 0;
		}
	}

	/* ===================================================================== */
	/*  VERSIONE JAVASCRIPT: CARICAMENTO COMPLETO DELLA BOARD                */
	/* ===================================================================== */

	/**
	 * Costruisce, in un'unica struttura, tutti i dati necessari alla versione
	 * JavaScript della HOME COLLABORATORE: nome del collaboratore e, per ogni
	 * progetto in cui &egrave; assegnato ad almeno un task, la tabella delle ore
	 * lavorate (mese per mese, per WP e task).
	 *
	 * <p>Sono inclusi solo i WP e i task a cui il collaboratore &egrave; assegnato. In
	 * ciascuna riga-task, sono modificabili solo le celle dei mesi a cui il
	 * collaboratore &egrave; effettivamente assegnato (cio&egrave; con una riga in
	 * {@code Task_ha_Tecnico}); gli altri mesi risultano non modificabili.</p>
	 */
	public CollaboratorBoard findBoard(int collabId) throws SQLException {
		CollaboratorBoard board = new CollaboratorBoard();
		board.setCollaboratorName(findCollaboratorFullName(collabId));

		List<CollaboratorBoard.ProjectBoard> projectBoards = new ArrayList<>();

		for (Project project : findCollaboratorProjects(collabId)) {
			CollaboratorBoard.ProjectBoard pb = new CollaboratorBoard.ProjectBoard();
			pb.setTitle(project.getTitle());

			int duration = project.getTerm();
			pb.setDuration(duration);

			List<Integer> months = new ArrayList<>();
			for (int m = 1; m <= duration; m++)
				months.add(m);
			pb.setMonths(months);

			List<CollaboratorBoard.WpBoard> wpBoards = new ArrayList<>();
			for (WorkPackage wp : findCollaboratorWorkPackages(collabId, project.getTitle())) {
				CollaboratorBoard.WpBoard wb = new CollaboratorBoard.WpBoard();
				wb.setLabel(wp.getWpAsString() + " - " + wp.getTitle());

				List<CollaboratorBoard.TaskBoard> taskBoards = new ArrayList<>();
				for (Task t : findCollaboratorTasks(collabId, project.getTitle(), wp.getIdOrder())) {
					CollaboratorBoard.TaskBoard tb = new CollaboratorBoard.TaskBoard();
					tb.setWp(wp.getIdOrder());
					tb.setTask(t.getIdOrder());
					tb.setLabel("T" + wp.getIdOrder() + "." + t.getIdOrder() + " - " + t.getTitle());

					// mese -> ore lavorate; le chiavi presenti sono i mesi assegnati (modificabili)
					Map<Integer, Integer> worked =
							findWorkedHoursByMonth(collabId, project.getTitle(), wp.getIdOrder(), t.getIdOrder());

					List<CollaboratorBoard.Cell> cells = new ArrayList<>();
					for (Integer m : months) {
						if (worked.containsKey(m))
							cells.add(new CollaboratorBoard.Cell(m, worked.get(m), true));
						else
							cells.add(new CollaboratorBoard.Cell(m, null, false));
					}
					tb.setCells(cells);
					taskBoards.add(tb);
				}
				wb.setTasks(taskBoards);
				wpBoards.add(wb);
			}
			pb.setWps(wpBoards);
			projectBoards.add(pb);
		}

		board.setProjects(projectBoards);
		return board;
	}
}
