package it.polimi.tiw.project.controllers;

import java.sql.Connection;
import java.sql.SQLException;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.project.utils.DatabaseManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;

public abstract class MyServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    protected JakartaServletWebApplication application;



    // Ogni servlet chiama questo dentro doGet/doPost e la chiude in un finally
    protected Connection getConnection() throws SQLException {
        return DatabaseManager.getConnection();
    }
    
	public void destroy() {
		try {
			DatabaseManager.closeConnection(getConnection());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		}
	
	protected String loginRedirect(jakarta.servlet.http.HttpServletRequest request, String message) {
        String url = request.getContextPath() + "/index.html";
        if (message != null && !message.isEmpty()) {
            url += "?msg=" + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
        }
        return url;
    }
}