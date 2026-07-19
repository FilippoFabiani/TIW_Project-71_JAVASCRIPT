package it.polimi.tiw.project.dao;

import it.polimi.tiw.project.beans.CollaboratorMonitoringBoard;
import it.polimi.tiw.project.beans.Project;
import it.polimi.tiw.project.beans.ProjectMonitoringBoard;
import it.polimi.tiw.project.beans.Task;
import it.polimi.tiw.project.beans.User;
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
 * DAO dedicato all'interfaccia del RESPONSABILE (HOME RESPONSABILE, MONITORAGGIO
 * PROGETTI, MONITORAGGIO COLLABORATORI). &Egrave; autosufficiente: contiene tutte le
 * query di cui ha bisogno, senza dipendere da altri DAO.
 *
 * <p>Riepilogo dello schema usato:</p>
 * <ul>
 *   <li>{@code Progetto(titolo PK, durata, stato, responsabile, amministratore)}</li>
 *   <li>{@code Work_Package(numero_ordine, progetto) PK, titolo}</li>
 *   <li>{@code Task(numero_ordine, wp, progetto) PK, titolo, descrizione, Mese_inizio, Mese_fine}</li>
 *   <li>{@code Task_has_Mese(mese, task, wp, progetto) PK, ore_previste} &rarr; ore PREVISTE per task/mese</li>
 *   <li>{@code Task_ha_Tecnico(task, wp, progetto, collaboratore, mese) PK, ore_effettive}
 *       &rarr; legame collaboratore&harr;task per mese; {@code ore_effettive} = ore LAVORATE</li>
 *   <li>{@code Tecnico(id PK, nome, cognome, isManager, isCollaborator)}, {@code User(id PK, ...)}</li>
 * </ul>
 */
public class ManagerDAO extends DAO {

	public ManagerDAO(Connection connection) {
		super(connection);
	}

	/* ===================================================================== */
	/*  LETTURE DI BASE                                                       */
	/* ===================================================================== */

	/** progetti di cui l'utente &egrave; responsabile (titolo, durata, stato) */
	public List<Project> findManagerProjects(int managerId) throws SQLException {
		List<Project> projects = new ArrayList<>();
		String query = "SELECT titolo, durata, stato FROM Progetto WHERE responsabile = ? ORDER BY titolo";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, managerId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Project p = new Project();
					p.setTitolo(rs.getString("titolo"));
					p.setDurata(rs.getInt("durata"));
					p.setStato(rs.getString("stato"));
					p.setManager(managerId);
					projects.add(p);
				}
			}
		}
		return projects;
	}

	/** profilo (nome, cognome) del tecnico corrente, per il saluto della HOME; null se non trovato */
	public User findTechnician(int id) throws SQLException {
		String query = "SELECT nome, cognome FROM Tecnico WHERE id = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next())
					return null;
				User u = new User();
				u.setId(id);
				u.setName(rs.getString("nome"));
				u.setSurname(rs.getString("cognome"));
				return u;
			}
		}
	}

	/** true se il progetto esiste ed appartiene (come responsabile) all'utente dato */
	public boolean isManagerOf(int managerId, String progetto) throws SQLException {
		String query = "SELECT 1 FROM Progetto WHERE titolo = ? AND responsabile = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			ps.setInt(2, managerId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.isBeforeFirst();
			}
		}
	}

	/** stato del progetto (es. "creato", "assegnato", "concluso"); null se non esiste */
	public String findProjectStatus(String progetto) throws SQLException {
		String query = "SELECT stato FROM Progetto WHERE titolo = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getString("stato") : null;
			}
		}
	}

	/** durata (numero di mesi) del progetto; 0 se non trovato */
	public int findProjectDuration(String progetto) throws SQLException {
		String query = "SELECT durata FROM Progetto WHERE titolo = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt("durata") : 0;
			}
		}
	}

	/** WP del progetto, ordinati per numero d'ordine */
	public List<WorkPackage> findWorkPackages(String progetto) throws SQLException {
		List<WorkPackage> wps = new ArrayList<>();
		String query = "SELECT numero_ordine, titolo FROM Work_Package WHERE progetto = ? ORDER BY numero_ordine";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
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

	/** task di un WP, ordinati per numero d'ordine */
	public List<Task> findTasks(String progetto, int wp) throws SQLException {
		List<Task> tasks = new ArrayList<>();
		String query = "SELECT numero_ordine, titolo FROM Task WHERE progetto = ? AND wp = ? ORDER BY numero_ordine";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			ps.setInt(2, wp);
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

	/** singolo task (titolo/descrizione); null se non trovato */
	public Task findTask(String progetto, int wp, int task) throws SQLException {
		String query = "SELECT numero_ordine, titolo, descrizione FROM Task "
				+ "WHERE progetto = ? AND wp = ? AND numero_ordine = ?";
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
				return t;
			}
		}
	}

	/**
	 * Mesi in cui il task &egrave; attivo, come lista ordinata [inizio..fine]. Gestisce in
	 * modo difensivo gli estremi NULL: se manca la fine usa solo l'inizio; se mancano
	 * entrambi torna lista vuota.
	 */
	public List<Integer> findTaskMonths(String progetto, int wp, int task) throws SQLException {
		String query = "SELECT Mese_inizio, Mese_fine FROM Task WHERE progetto = ? AND wp = ? AND numero_ordine = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			ps.setInt(2, wp);
			ps.setInt(3, task);
			try (ResultSet rs = ps.executeQuery()) {
				List<Integer> months = new ArrayList<>();
				if (!rs.next())
					return months;
				int start = rs.getInt("Mese_inizio");
				boolean startNull = rs.wasNull();
				int end = rs.getInt("Mese_fine");
				boolean endNull = rs.wasNull();
				if (startNull)
					return months;
				if (endNull)
					end = start;
				for (int m = start; m <= end; m++)
					months.add(m);
				return months;
			}
		}
	}

	/** mese &rarr; ore_previste per un task (da Task_has_Mese) */
	public Map<Integer, Integer> findPlannedHours(String progetto, int wp, int task) throws SQLException {
		Map<Integer, Integer> hours = new LinkedHashMap<>();
		String query = "SELECT mese, ore_previste FROM Task_has_Mese WHERE progetto = ? AND wp = ? AND task = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			ps.setInt(2, wp);
			ps.setInt(3, task);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int mese = rs.getInt("mese");
					int ore = rs.getInt("ore_previste");
					if (!rs.wasNull())
						hours.put(mese, ore);
				}
			}
		}
		return hours;
	}

	/** id (distinti) dei collaboratori attualmente assegnati al task */
	public List<Integer> findAssignedCollaboratorIds(String progetto, int wp, int task) throws SQLException {
		List<Integer> ids = new ArrayList<>();
		String query = "SELECT DISTINCT collaboratore FROM Task_ha_Tecnico WHERE progetto = ? AND wp = ? AND task = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			ps.setInt(2, wp);
			ps.setInt(3, task);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					ids.add(rs.getInt("collaboratore"));
			}
		}
		return ids;
	}

	/**
	 * Collaboratori assegnabili: tecnici con {@code isCollaborator = 1}, escluso
	 * l'utente stesso (l'autoassegnazione non &egrave; permessa).
	 */
	public List<User> findAssignableCollaborators(int managerId) throws SQLException {
		List<User> users = new ArrayList<>();
		String query = "SELECT User.id AS id, Tecnico.nome AS nome, Tecnico.cognome AS cognome "
				+ "FROM Tecnico JOIN User ON Tecnico.id = User.id "
				+ "WHERE Tecnico.isCollaborator = 1 AND User.id <> ? "
				+ "ORDER BY Tecnico.cognome, Tecnico.nome";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, managerId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					User u = new User();
					u.setId(rs.getInt("id"));
					u.setName(rs.getString("nome"));
					u.setSurname(rs.getString("cognome"));
					users.add(u);
				}
			}
		}
		return users;
	}

	/* ===================================================================== */
	/*  SALVA                                                                 */
	/* ===================================================================== */

	/**
	 * Salva l'assegnamento di UN task (SALVA), in transazione: garantisce l'esistenza
	 * dei mesi in {@code Mese}, fa l'upsert delle ore previste in {@code Task_has_Mese}
	 * e riallinea i collaboratori in {@code Task_ha_Tecnico} (una riga per collaboratore
	 * &times; mese del task, {@code ore_effettive} NULL). Sicuro perch&eacute; l'assegnamento
	 * avviene quando il progetto &egrave; ancora "creato".
	 */
	public void saveAssignment(String progetto, int wp, int task, List<Integer> months,
			Map<Integer, Integer> hoursByMonth, List<Integer> collaboratorIds) throws SQLException {

		String ensureMonth = "INSERT IGNORE INTO Mese (mese) VALUES (?)";
		String upsertHours = "INSERT INTO Task_has_Mese (mese, ore_previste, task, wp, progetto) "
				+ "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE ore_previste = VALUES(ore_previste)";
		String deleteCollaborators = "DELETE FROM Task_ha_Tecnico WHERE progetto = ? AND wp = ? AND task = ?";
		String insertCollaborator = "INSERT INTO Task_ha_Tecnico (task, wp, progetto, collaboratore, mese) "
				+ "VALUES (?, ?, ?, ?, ?)";

		try {
			connection.setAutoCommit(false);

			try (PreparedStatement ps = connection.prepareStatement(ensureMonth)) {
				for (Integer m : months) {
					ps.setInt(1, m);
					ps.addBatch();
				}
				ps.executeBatch();
			}

			try (PreparedStatement ps = connection.prepareStatement(upsertHours)) {
				for (Map.Entry<Integer, Integer> e : hoursByMonth.entrySet()) {
					ps.setInt(1, e.getKey());
					ps.setInt(2, e.getValue());
					ps.setInt(3, task);
					ps.setInt(4, wp);
					ps.setString(5, progetto);
					ps.addBatch();
				}
				ps.executeBatch();
			}

			try (PreparedStatement ps = connection.prepareStatement(deleteCollaborators)) {
				ps.setString(1, progetto);
				ps.setInt(2, wp);
				ps.setInt(3, task);
				ps.executeUpdate();
			}

			if (!collaboratorIds.isEmpty() && !months.isEmpty()) {
				try (PreparedStatement ps = connection.prepareStatement(insertCollaborator)) {
					for (Integer collabId : collaboratorIds) {
						for (Integer m : months) {
							ps.setInt(1, task);
							ps.setInt(2, wp);
							ps.setString(3, progetto);
							ps.setInt(4, collabId);
							ps.setInt(5, m);
							ps.addBatch();
						}
					}
					ps.executeBatch();
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

	/* ===================================================================== */
	/*  ASSEGNA                                                               */
	/* ===================================================================== */

	/**
	 * Controlla se il progetto pu&ograve; essere assegnato: per OGNI task servono almeno un
	 * collaboratore e le ore previste in OGNI mese di attivit&agrave;. Ritorna la lista dei
	 * problemi (vuota &rarr; assegnabile).
	 */
	public List<String> validateForAssignment(String progetto) throws SQLException {
		List<String> problems = new ArrayList<>();
		String tasksQuery = "SELECT wp, numero_ordine, Mese_inizio, Mese_fine "
				+ "FROM Task WHERE progetto = ? ORDER BY wp, numero_ordine";

		List<int[]> tasks = new ArrayList<>();
		try (PreparedStatement ps = connection.prepareStatement(tasksQuery)) {
			ps.setString(1, progetto);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int wp = rs.getInt("wp");
					int ord = rs.getInt("numero_ordine");
					int mi = rs.getInt("Mese_inizio");
					int miNull = rs.wasNull() ? 1 : 0;
					int mf = rs.getInt("Mese_fine");
					int mfNull = rs.wasNull() ? 1 : 0;
					tasks.add(new int[] { wp, ord, mi, mf, miNull, mfNull });
				}
			}
		}

		if (tasks.isEmpty()) {
			problems.add("Il progetto non contiene alcun task.");
			return problems;
		}

		for (int[] t : tasks) {
			int wp = t[0];
			int ord = t[1];
			String label = "Task T" + wp + "." + ord;

			if (countCollaborators(progetto, wp, ord) == 0)
				problems.add(label + ": nessun collaboratore assegnato.");

			List<Integer> months = new ArrayList<>();
			if (t[4] == 0) {
				int start = t[2];
				int end = (t[5] == 0) ? t[3] : start;
				for (int m = start; m <= end; m++)
					months.add(m);
			}
			for (Integer m : months) {
				if (!hasPlannedHours(progetto, wp, ord, m))
					problems.add(label + ": ore previste mancanti nel mese M" + m + ".");
			}
		}
		return problems;
	}

	private int countCollaborators(String progetto, int wp, int task) throws SQLException {
		String query = "SELECT COUNT(DISTINCT collaboratore) AS n FROM Task_ha_Tecnico "
				+ "WHERE progetto = ? AND wp = ? AND task = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			ps.setInt(2, wp);
			ps.setInt(3, task);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getInt("n");
			}
		}
	}

	private boolean hasPlannedHours(String progetto, int wp, int task, int mese) throws SQLException {
		String query = "SELECT ore_previste FROM Task_has_Mese "
				+ "WHERE progetto = ? AND wp = ? AND task = ? AND mese = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			ps.setInt(2, wp);
			ps.setInt(3, task);
			ps.setInt(4, mese);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next())
					return false;
				int ore = rs.getInt("ore_previste");
				return !rs.wasNull() && ore > 0;
			}
		}
	}

	/** porta il progetto nello stato "assegnato" (solo se del responsabile e "creato") */
	public boolean setProjectAssigned(String progetto, int managerId) throws SQLException {
		String query = "UPDATE Progetto SET stato = 'assegnato' "
				+ "WHERE titolo = ? AND responsabile = ? AND LOWER(stato) = 'creato'";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			ps.setInt(2, managerId);
			return ps.executeUpdate() > 0;
		}
	}

	/* ===================================================================== */
	/*  MONITORAGGIO PROGETTI                                                 */
	/* ===================================================================== */

	/** mese &rarr; somma ore lavorate da TUTTI i collaboratori, per un task */
	private Map<Integer, Integer> findWorkedHoursByMonth(String progetto, int wp, int task) throws SQLException {
		Map<Integer, Integer> worked = new LinkedHashMap<>();
		String query = "SELECT mese, SUM(ore_effettive) AS ore FROM Task_ha_Tecnico "
				+ "WHERE progetto = ? AND wp = ? AND task = ? GROUP BY mese";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			ps.setInt(2, wp);
			ps.setInt(3, task);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int mese = rs.getInt("mese");
					int ore = rs.getInt("ore");
					worked.put(mese, rs.wasNull() ? 0 : ore);
				}
			}
		}
		return worked;
	}

	/**
	 * Costruisce il modello della tabella MONITORAGGIO PROGETTI e calcola se il
	 * progetto pu&ograve; essere concluso (per OGNI task: totale ore lavorate &ge; previste).
	 */
	public ProjectMonitoringBoard getProjectMonitoringBoard(String progetto) throws SQLException {
		ProjectMonitoringBoard board = new ProjectMonitoringBoard();
		board.setProjectTitle(progetto);
		board.setStatus(findProjectStatus(progetto));

		int durata = findProjectDuration(progetto);
		List<Integer> months = new ArrayList<>();
		for (int m = 1; m <= durata; m++)
			months.add(m);
		board.setMonths(months);

		List<ProjectMonitoringBoard.WpGroup> wpGroups = new ArrayList<>();
		boolean hasTasks = false;
		boolean allComplete = true;

		for (WorkPackage wp : findWorkPackages(progetto)) {
			ProjectMonitoringBoard.WpGroup group = new ProjectMonitoringBoard.WpGroup();
			group.setLabel(wp.getWpAsString() + " - " + wp.getTitle());

			List<ProjectMonitoringBoard.TaskRow> rows = new ArrayList<>();
			for (Task t : findTasks(progetto, wp.getIdOrder())) {
				hasTasks = true;
				Map<Integer, Integer> planned = findPlannedHours(progetto, wp.getIdOrder(), t.getIdOrder());
				Map<Integer, Integer> worked = findWorkedHoursByMonth(progetto, wp.getIdOrder(), t.getIdOrder());

				ProjectMonitoringBoard.TaskRow row = new ProjectMonitoringBoard.TaskRow();
				row.setLabel("T" + wp.getIdOrder() + "." + t.getIdOrder() + " - " + t.getTitle());

				List<ProjectMonitoringBoard.Cell> cells = new ArrayList<>();
				int sumPlanned = 0;
				int sumWorked = 0;
				for (Integer m : months) {
					int p = planned.getOrDefault(m, 0);
					int w = worked.getOrDefault(m, 0);
					sumPlanned += p;
					sumWorked += w;
					cells.add(new ProjectMonitoringBoard.Cell(p, w));
				}
				row.setCells(cells);
				rows.add(row);

				if (sumWorked < sumPlanned)
					allComplete = false;
			}
			group.setTasks(rows);
			wpGroups.add(group);
		}
		board.setWpGroups(wpGroups);
		board.setConcludable(hasTasks && allComplete);
		return board;
	}

	/** porta il progetto nello stato "concluso" (solo se del responsabile e "assegnato") */
	public boolean setProjectConcluded(String progetto, int managerId) throws SQLException {
		String query = "UPDATE Progetto SET stato = 'concluso' "
				+ "WHERE titolo = ? AND responsabile = ? AND LOWER(stato) = 'assegnato'";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			ps.setInt(2, managerId);
			return ps.executeUpdate() > 0;
		}
	}

	/* ===================================================================== */
	/*  MONITORAGGIO COLLABORATORI                                            */
	/* ===================================================================== */

	/** collaboratori che lavorano in almeno un progetto del responsabile */
	public List<User> findCollaboratorsOfManagerProjects(int managerId) throws SQLException {
		List<User> users = new ArrayList<>();
		String query = "SELECT DISTINCT tht.collaboratore AS id, tec.nome AS nome, tec.cognome AS cognome "
				+ "FROM Task_ha_Tecnico tht "
				+ "JOIN Progetto p ON tht.progetto = p.titolo "
				+ "JOIN Tecnico tec ON tht.collaboratore = tec.id "
				+ "WHERE p.responsabile = ? "
				+ "ORDER BY tec.cognome, tec.nome";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setInt(1, managerId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					User u = new User();
					u.setId(rs.getInt("id"));
					u.setName(rs.getString("nome"));
					u.setSurname(rs.getString("cognome"));
					users.add(u);
				}
			}
		}
		return users;
	}

	/** mese &rarr; ore lavorate dal singolo collaboratore in un task */
	private Map<Integer, Integer> findWorkedHoursForCollaborator(String progetto, int wp, int task, int collabId)
			throws SQLException {
		Map<Integer, Integer> worked = new LinkedHashMap<>();
		String query = "SELECT mese, ore_effettive FROM Task_ha_Tecnico "
				+ "WHERE progetto = ? AND wp = ? AND task = ? AND collaboratore = ?";
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, progetto);
			ps.setInt(2, wp);
			ps.setInt(3, task);
			ps.setInt(4, collabId);
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

	/**
	 * Costruisce il modello della pagina MONITORAGGIO COLLABORATORI per un dato
	 * collaboratore: una tabella per ogni progetto DEL RESPONSABILE CORRENTE in cui il
	 * collaboratore lavora (i progetti di altri responsabili sono esclusi).
	 */
	public CollaboratorMonitoringBoard getCollaboratorMonitoringBoard(int managerId, int collabId) throws SQLException {
		CollaboratorMonitoringBoard board = new CollaboratorMonitoringBoard();

		String nameQuery = "SELECT nome, cognome FROM Tecnico WHERE id = ?";
		try (PreparedStatement ps = connection.prepareStatement(nameQuery)) {
			ps.setInt(1, collabId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					board.setCollaboratorName(rs.getString("nome") + " " + rs.getString("cognome"));
			}
		}

		List<String> projectTitles = new ArrayList<>();
		String projQuery = "SELECT DISTINCT p.titolo AS titolo FROM Task_ha_Tecnico tht "
				+ "JOIN Progetto p ON tht.progetto = p.titolo "
				+ "WHERE p.responsabile = ? AND tht.collaboratore = ? ORDER BY p.titolo";
		try (PreparedStatement ps = connection.prepareStatement(projQuery)) {
			ps.setInt(1, managerId);
			ps.setInt(2, collabId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					projectTitles.add(rs.getString("titolo"));
			}
		}

		List<CollaboratorMonitoringBoard.ProjectTable> tables = new ArrayList<>();
		for (String progetto : projectTitles) {
			CollaboratorMonitoringBoard.ProjectTable table = new CollaboratorMonitoringBoard.ProjectTable();
			table.setTitle(progetto);

			int durata = findProjectDuration(progetto);
			List<Integer> months = new ArrayList<>();
			for (int m = 1; m <= durata; m++)
				months.add(m);
			table.setMonths(months);

			List<CollaboratorMonitoringBoard.WpGroup> wpGroups = new ArrayList<>();
			for (WorkPackage wp : findWorkPackages(progetto)) {
				CollaboratorMonitoringBoard.WpGroup group = new CollaboratorMonitoringBoard.WpGroup();
				group.setLabel(wp.getWpAsString() + " - " + wp.getTitle());

				List<CollaboratorMonitoringBoard.TaskRow> rows = new ArrayList<>();
				for (Task t : findTasks(progetto, wp.getIdOrder())) {
					Map<Integer, Integer> worked =
							findWorkedHoursForCollaborator(progetto, wp.getIdOrder(), t.getIdOrder(), collabId);

					CollaboratorMonitoringBoard.TaskRow row = new CollaboratorMonitoringBoard.TaskRow();
					row.setLabel("T" + wp.getIdOrder() + "." + t.getIdOrder() + " - " + t.getTitle());

					List<Integer> workedList = new ArrayList<>();
					for (Integer m : months)
						workedList.add(worked.getOrDefault(m, 0));
					row.setWorked(workedList);
					rows.add(row);
				}
				group.setTasks(rows);
				wpGroups.add(group);
			}
			table.setWpGroups(wpGroups);
			tables.add(table);
		}
		board.setProjects(tables);
		return board;
	}
}
