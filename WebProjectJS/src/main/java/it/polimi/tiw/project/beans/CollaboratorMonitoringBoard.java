package it.polimi.tiw.project.beans;

import java.util.List;

/**
 * Modello dati (serializzato in JSON con Gson) per la pagina MONITORAGGIO
 * COLLABORATORI della versione JavaScript del responsabile.
 *
 * <p>Per il collaboratore selezionato contiene una tabella per ogni progetto
 * <b>del responsabile corrente</b> in cui il collaboratore lavora; ogni tabella
 * riporta, mese per mese, le ore lavorate dal collaboratore in ciascun task.</p>
 *
 * <pre>
 *   CollaboratorMonitoringBoard
 *     collaboratorName
 *     projects[]
 *        ProjectTable.title, months[]=[1..durata]
 *        ProjectTable.wpGroups[]
 *           WpGroup.label
 *           WpGroup.tasks[]
 *              TaskRow.label, worked[] = ore lavorate, una per mese
 * </pre>
 */
public class CollaboratorMonitoringBoard {

	private String collaboratorName;
	private List<ProjectTable> projects;

	public String getCollaboratorName() {
		return collaboratorName;
	}

	public void setCollaboratorName(String collaboratorName) {
		this.collaboratorName = collaboratorName;
	}

	public List<ProjectTable> getProjects() {
		return projects;
	}

	public void setProjects(List<ProjectTable> projects) {
		this.projects = projects;
	}

	/** una tabella per un singolo progetto */
	public static class ProjectTable {
		private String title;
		private List<Integer> months;
		private List<WpGroup> wpGroups;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public List<Integer> getMonths() {
			return months;
		}

		public void setMonths(List<Integer> months) {
			this.months = months;
		}

		public List<WpGroup> getWpGroups() {
			return wpGroups;
		}

		public void setWpGroups(List<WpGroup> wpGroups) {
			this.wpGroups = wpGroups;
		}
	}

	public static class WpGroup {
		private String label;
		private List<TaskRow> tasks;

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public List<TaskRow> getTasks() {
			return tasks;
		}

		public void setTasks(List<TaskRow> tasks) {
			this.tasks = tasks;
		}
	}

	/** una riga (task) con le ore lavorate del collaboratore, una per mese */
	public static class TaskRow {
		private String label;
		private List<Integer> worked;

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public List<Integer> getWorked() {
			return worked;
		}

		public void setWorked(List<Integer> worked) {
			this.worked = worked;
		}
	}
}
