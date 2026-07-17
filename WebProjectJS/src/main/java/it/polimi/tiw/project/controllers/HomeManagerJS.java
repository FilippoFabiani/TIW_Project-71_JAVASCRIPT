package it.polimi.tiw.project.controllers;

import java.io.IOException;

import it.polimi.tiw.project.beans.Position;
import it.polimi.tiw.project.beans.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Versione JavaScript (RIA) dell'interfaccia del RESPONSABILE.
 *
 * <p>Serve l'unica pagina (il "guscio" HTML + lo script). Non inietta dati: si limita
 * a controllare sessione/ruolo e a inoltrare al template statico. Tutti i dati sono
 * poi richiesti in modo asincrono da {@code /js/manager.js} agli endpoint
 * {@link GetManagerData} (letture) e {@link ManagerActionJS} (SALVA/ASSEGNA/CONCLUDI).</p>
 *
 * <p>&Egrave; mappato su {@code /homeManager}, l'URL a cui il login (client) reindirizza il
 * responsabile.</p>
 */
@WebServlet("/homeManager")
public class HomeManagerJS extends MyServlet {

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
		if (!Position.MANAGER.equals(position) && !Position.TECHNICIAN.equals(position)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato: profilo di responsabile richiesto.");
			return;
		}

		response.setContentType("text/html;charset=UTF-8");
		request.getRequestDispatcher("/WEB-INF/templates/homeManager.html").forward(request, response);
	}
}
