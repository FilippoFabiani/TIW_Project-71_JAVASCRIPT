package it.polimi.tiw.project.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import it.polimi.tiw.project.beans.CollaboratorBoard;
import it.polimi.tiw.project.beans.Position;
import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.dao.CollaboratorDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Endpoint JSON che restituisce, in un'unica risposta, tutti i dati della HOME
 * COLLABORATORE (versione JavaScript): nome del collaboratore e, per ogni progetto
 * in cui &egrave; assegnato, la tabella delle ore lavorate.
 *
 * <p>&Egrave; invocato una sola volta all'accesso alla pagina; la successiva selezione
 * di un progetto avviene interamente lato client, senza ulteriori richieste.</p>
 */
@WebServlet("/GetCollaboratorData")
public class GetCollaboratorData extends MyServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute("user") == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().write("{\"error\":\"Utente non autenticato\"}");
			return;
		}

		User user = (User) session.getAttribute("user");
		Position position = user.getPosition();
		if (!Position.COLLABORATOR.equals(position) && !Position.TECHNICIAN.equals(position)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.getWriter().write("{\"error\":\"Accesso negato\"}");
			return;
		}

		try (Connection connection = getConnection()) {
			CollaboratorDAO dao = new CollaboratorDAO(connection);
			CollaboratorBoard board = dao.findBoard(user.getId());

			String json = new Gson().toJson(board);
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write(json);
		} catch (SQLException e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("{\"error\":\"Errore nel recupero dei dati\"}");
		}
	}
}
