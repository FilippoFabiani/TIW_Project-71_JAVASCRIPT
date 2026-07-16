package it.polimi.tiw.project.beans;

public class User {
	
	private String username;
	private String photoPath;
	private int id;
	private Position position;
	private String name;
	private String surname;
	

	public void setPosition(Position position) {
		this.position = position;
	}
	public void setId(int id) {
		this.id = id;
	}
	public void setPhotoPath(String path) {
		this.photoPath = path;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setSurname(String surname) {
		this.surname = surname;
	}
	
	
	public Position getPosition() {
		return position;
	}
	public int getId() {
		return id;
	}
	public String getPhotoPath() {
		return photoPath;
	}
	public String getUsername() {
		return username;
	}
	public String getName() {
		return name;
	}
	public String getSurname() {
		return surname;
	}
}