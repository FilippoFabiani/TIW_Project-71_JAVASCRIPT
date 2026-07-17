package it.polimi.tiw.project.beans;

import java.util.List;

/**
 * Modello dati (serializzato in JSON con Gson) per la pagina MONITORAGGIO
 * PROGETTI della versione JavaScript del responsabile.
 *
 * <p>Rappresenta la tabella "ore previste / ore lavorate" di un progetto, mese per
 * mese, raggruppata per Work Package e task, pi&ugrave; il flag {@code concludable}
 * (per ogni task le ore lavorate da tutti i collaboratori sono &ge; alle previste).</p>
 *
 * <pre>
 *   ProjectMonitoringBoard
 *     projectTitle, status, concludable
 *     months[] = [1..durata]
 *     wpGroups[]
 *        WpGroup.label
 *        WpGroup.tasks[]
 *           TaskRow.label
 *           TaskRow.cells[] = una cella per mese (previste + lavorate)
 * </pre>
 */
public class ProjectMonitoringBoard {

	private String projectTitle;
	private String status;
	private List<Integer> months;
	private List<WpGroup> wpGroups;
	private boolean concludable;

	public String getProjectTitle() {
		return projectTitle;
	}

	public void setProjectTitle(String projectTitle) {
		this.projectTitle = projectTitle;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public boolean isConcludable() {
		return concludable;
	}

	public void setConcludable(boolean concludable) {
		this.concludable = concludable;
	}

	/** un WP con le sue righe-task */
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

	/** una riga (task) con una cella per ogni mese */
	public static class TaskRow {
		private String label;
		private List<Cell> cells;

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

	/** una cella: ore previste e ore lavorate (somma di tutti i collaboratori) in un mese */
	public static class Cell {
		private int previste;
		private int lavorate;

		public Cell(int previste, int lavorate) {
			this.previste = previste;
			this.lavorate = lavorate;
		}

		public int getPreviste() {
			return previste;
		}

		public int getLavorate() {
			return lavorate;
		}
	}
}
