package it.polimi.tiw.project.beans;

import java.util.List;

public class Project {
	private String title;
	private int term;
	private String status;
	private List<Integer> collaborators;
	private List<WorkPackage> wp;
	private int manager;
	private int admin;

	public String getTitle() {
		return title;
	}
	public int getTerm() {
		return term;
	}
	public String getStatus() {
		return status;
	}
	public List<Integer> getCollaborators() {
		return collaborators;
	}
	public List<WorkPackage> getWp() {
		return wp;
	}
	public int getManager() {
		return manager;
	}
	public int getAdmin() {
		return admin;
	}
	
	public void setTitolo(String title) {
		this.title = title;
	}
	public void setDurata(int term) {
		this.term = term;
	}
	public void setStato(String status) {
		this.status = status;
	}
	public void setCollaborators(List<Integer> collaborators) {
		this.collaborators = collaborators;
	}
	public void setWp(List<WorkPackage> wp) {
		this.wp = wp;
	}

	public void setManager(int manager) {
        this.manager = manager;
    }

	public void setAdmin(int admin) {
        this.admin = admin;
    }
	
}
