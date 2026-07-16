package it.polimi.tiw.project.beans;

import java.util.List;

public class Task {
	private int idOrder;
	private String title;
	private String description;
	private String startMonth;
	private String endMonth;
	
	public int getIdOrder() {
		return idOrder;
	}
	public String getTitle() {
		return title;
	}
	public String getDescription() {
		return description;
	}
	public String getStartMonth() {
		return startMonth;
	}
	public String getEndMonth() {
		return endMonth;
	}
	public String getIdOrderAsString(int wp) {
		return "T" + Integer.toString(wp) + "." + Integer.toString(idOrder);
	}

	public void setIdOrder(int idOrder) {
		this.idOrder=idOrder;
	}
	public void setTitle(String title) {
		this.title=title;
	}
	public void setDescription(String description) {
		this.description=description;
	}
	public void setStartMonth(String startMonth) {
		this.startMonth=startMonth;
	}
	public void setEndMonth(String endMonth) {
		this.endMonth=endMonth;
	}

	public void setStartMonth(int startMonth) {
		String monthString = "M";
		this.startMonth = monthString + Integer.toString(startMonth);
	}

	public void setEndMonth(int endMonth) {
		String monthString = "M";
		this.endMonth = monthString + Integer.toString(endMonth);
	}
	
	public static List<Integer> parseTaskId(String taskId) throws IllegalArgumentException {
		if (taskId == null || !taskId.matches("T\\d+\\.\\d+")) {
			throw new IllegalArgumentException("Invalid task ID format");
		}

		String[] parts = taskId.substring(1).split("\\.");
		int wp = Integer.parseInt(parts[0]);
		int task = Integer.parseInt(parts[1]);

		return List.of(wp, task);
	}
}
