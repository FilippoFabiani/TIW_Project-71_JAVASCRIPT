package it.polimi.tiw.project.dao;

import java.sql.Connection;

public abstract class DAO {
	protected Connection connection;
	
	
	public DAO(Connection connection) {
		//TODO metttetr qui la connessine con credenziali?
        this.connection = connection;	
	}
}
