package it.polimi.tiw.project.beans;

import java.util.List;

/**
 * Modello dati (serializzato in JSON con Gson) per la versione JavaScript della
 * HOME COLLABORATORE.
 *
 * <p>Contiene, per il collaboratore corrente, tutti i dati necessari a costruire
 * lato client l'intera interfaccia in un'unica chiamata: l'elenco dei progetti in
 * cui &egrave; assegnato ad almeno un task e, per ciascun progetto, la tabella
 * "ore lavorate" (mese per mese, per WP e task).</p>
 *
 * <p>I dati vengono caricati una sola volta all'accesso alla pagina: la selezione di
 * un progetto avviene interamente lato client, senza nuove richieste al server.</p>
 *
 * <pre>
 *   CollaboratorBoard
 *     collaboratorName
 *     projects[]
 *        ProjectBoard.title, duration, months[]=[1..duration]
 *        ProjectBoard.wps[]
 *           WpBoard.label
 *           WpBoard.tasks[]
 *              TaskBoard.wp, task, label
 *              TaskBoard.cells[] = una cella per mese
 *                 Cell.month, worked (ore lavorate), editable
 * </pre>
 */
public class CollaboratorBoard {

	private String collaboratorName;
	private List<ProjectBoard> projects;

	public String getCollaboratorName() {
		return collaboratorName;
	}

	public void setCollaboratorName(String collaboratorName) {
		this.collaboratorName = collaboratorName;
	}

	public List<ProjectBoard> getProjects() {
		return projects;
	}

	public void setProjects(List<ProjectBoard> projects) {
		this.projects = projects;
	}

	/** una tabella per un singolo progetto */
	public static class ProjectBoard {
		private String title;
		private int duration;
		private List<Integer> months;
		private List<WpBoard> wps;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public int getDuration() {
			return duration;
		}

		public void setDuration(int duration) {
			this.duration = duration;
		}

		public List<Integer> getMonths() {
			return months;
		}

		public void setMonths(List<Integer> months) {
			this.months = months;
		}

		public List<WpBoard> getWps() {
			return wps;
		}

		public void setWps(List<WpBoard> wps) {
			this.wps = wps;
		}
	}

	/** un WP con le sue righe-task */
	public static class WpBoard {
		private String label;
		private List<TaskBoard> tasks;

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public List<TaskBoard> getTasks() {
			return tasks;
		}

		public void setTasks(List<TaskBoard> tasks) {
			this.tasks = tasks;
		}
	}

	/** una riga (task) con una cella per ogni mese */
	public static class TaskBoard {
		private int wp;
		private int task;
		private String label;
		private List<Cell> cells;

		public int getWp() {
			return wp;
		}

		public void setWp(int wp) {
			this.wp = wp;
		}

		public int getTask() {
			return task;
		}

		public void setTask(int task) {
			this.task = task;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public List<Cell> getCells() {
			return cells;
		}

		public void setCells(List<Cell> cells) {
			this.cells = cells;
		}
	}

	/**
	 * Una cella della tabella.
	 * <ul>
	 *   <li>{@code worked}: ore lavorate dal collaboratore nel mese (0 di default);
	 *       {@code null} quando la cella non &egrave; modificabile.</li>
	 *   <li>{@code editable}: true se il collaboratore &egrave; assegnato al task in quel
	 *       mese (esiste una riga in {@code Task_ha_Tecnico}) e pu&ograve; quindi editarne
	 *       le ore.</li>
	 * </ul>
	 */
	public static class Cell {
		private int month;
		private Integer worked;
		private boolean editable;

		public Cell(int month, Integer worked, boolean editable) {
			this.month = month;
			this.worked = worked;
			this.editable = editable;
		}

		public int getMonth() {
			return month;
		}

		public Integer getWorked() {
			return worked;
		}

		public boolean isEditable() {
			return editable;
		}
	}
}
