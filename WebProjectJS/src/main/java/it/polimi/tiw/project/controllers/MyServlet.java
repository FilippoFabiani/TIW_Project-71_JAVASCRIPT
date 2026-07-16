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
    protected TemplateEngine templateEngine;
    protected JakartaServletWebApplication application;


    @Override
    public void init() throws ServletException {
        super.init();
        this.templateEngine = (TemplateEngine) getServletContext().getAttribute("templateEngine");
    	this.application = JakartaServletWebApplication.buildApplication(getServletContext());

    }

    // Ogni servlet chiama questo dentro doGet/doPost e la chiude in un finally
    protected Connection getConnection() throws SQLException {
        return DatabaseManager.getConnection();
    }
}