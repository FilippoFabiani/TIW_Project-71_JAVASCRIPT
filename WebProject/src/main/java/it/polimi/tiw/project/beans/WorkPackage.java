package it.polimi.tiw.project.beans;

import java.util.List;

public class WorkPackage {
	private int idOrder;
	private String title;
	private String startMonth;
	private String endMonth;
	private List<Task> tasks;
	
	public int getIdOrder() {
		return idOrder;
	}
	public String getTitle() {
		return title;
	}
	public String getStartMonth() {
		return startMonth;
	}
	public String getEndMonth() {
		return endMonth;
	}
	public List<Task> getTasks(){
		return tasks;
	}
	public String getWpAsString() {
		return "WP" + Integer.toString(idOrder);
	}

 	public void setIdOrder(int idOrder) {
		this.idOrder=idOrder;
	}
	public void setTitle(String title) {
		this.title=title;
	}
	public void setStartMonth(String startMonth) {
		this.startMonth=startMonth;
	}
	public void setEndMonth(String endMonth) {
		this.endMonth=endMonth;
	}
	public void setTasks(List<Task> tasks) {
		this.tasks=tasks;
	}

	public void setStartMonth(int startMonth) {
		String monthString = "M";
		this.startMonth = monthString + Integer.toString(startMonth);
	}

	public void setEndMonth(int endMonth) {
		String monthString = "M";
		this.endMonth = monthString + Integer.toString(endMonth);
	}
	
	public static List<Integer> parseWpId(String wpId) throws IllegalArgumentException {
        if (wpId == null || !wpId.matches("WP\\d+")) {
            throw new IllegalArgumentException("Invalid work package ID format");
        }
        String[] parts = wpId.split("WP");
        int idOrder = Integer.parseInt(parts[1]);
        return List.of(idOrder);
    }

}
