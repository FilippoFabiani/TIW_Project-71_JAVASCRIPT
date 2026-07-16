package it.polimi.tiw.project.controllers;

import java.io.IOException;

import org.thymeleaf.context.WebContext;

import it.polimi.tiw.project.beans.Position;
import it.polimi.tiw.project.beans.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Versione JavaScript (RIA) della HOME COLLABORATORE.
 *
 * <p>Serve l'unica pagina dell'interfaccia (il "guscio" HTML + lo script). Tutti i
 * dati veri e propri (progetti, WP, task, ore lavorate) sono caricati in modo
 * asincrono, una sola volta, da {@link GetCollaboratorData}; i salvataggi passano
 * per {@link UpdateWorkedHoursJS}. Questo servlet non inietta dati nel modello: si
 * limita a rendere il template.</p>
 */
@WebServlet("/homeCollaboratorJS")
public class HomeCollaboratorJS extends MyServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute("user") == null) {
			response.sendRedirect(request.getContextPath() + "/login");
			return;
		}

		User user = (User) session.getAttribute("user");
		Position position = user.getPosition();
		if (!Position.COLLABORATOR.equals(position) && !Position.TECHNICIAN.equals(position)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato: profilo di collaboratore richiesto.");
			return;
		}

		WebContext ctx = new WebContext(application.buildExchange(request, response), request.getLocale());
		response.setContentType("text/html;charset=UTF-8");
		templateEngine.process("homeCollaboratorJS", ctx, response.getWriter());
	}
}
