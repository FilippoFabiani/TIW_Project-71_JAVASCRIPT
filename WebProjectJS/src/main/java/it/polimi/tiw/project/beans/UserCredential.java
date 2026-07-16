package it.polimi.tiw.project.beans;

public class UserCredential {
	private String username;
	private String password;
	
	public UserCredential() {
		this.username = null;
		this.password = null;
	}
	public UserCredential(String usrn, String pwd) {
		this.username = usrn;
		this.password = pwd;
	}
	
	
	public void setUsername(String username) {
		this.username = username;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getUsername() {
		return username;
	}
	public String getPassword() {
		return password;
	}
}
