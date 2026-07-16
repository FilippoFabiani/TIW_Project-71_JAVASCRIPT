package it.polimi.tiw.project.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import it.polimi.tiw.project.beans.Position;
import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.utils.*;
import it.polimi.tiw.project.dao.CollaboratorDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Salvataggio asincrono di una singola cella (versione JavaScript).
 *
 * <p>Riceve progetto, wp, task, mese e ore; registra le ore lavorate nel database in
 * sostituzione del valore precedente (default 0) e risponde in JSON con l'esito. La
 * validazione (intero non negativo, mese valido per il collaboratore) &egrave; fatta anche
 * lato server: in caso di errore la risposta ha {@code success=false} e nessuna
 * scrittura viene effettuata.</p>
 */
@WebServlet("/UpdateWorkedHoursJS")
public class UpdateWorkedHoursJS extends MyServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute("user") == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			writeError(response, "Utente non autenticato");
			return;
		}

		User user = (User) session.getAttribute("user");
		Position position = user.getPosition();
		if (!Position.COLLABORATOR.equals(position) && !Position.TECHNICIAN.equals(position)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			writeError(response, "Accesso negato");
			return;
		}

		String progetto = ServletUtils.trimToNull(request.getParameter("progetto"));
		Integer wp = ServletUtils.parseIntOrNull(request.getParameter("wp"));
		Integer task = ServletUtils.parseIntOrNull(request.getParameter("task"));
		Integer mese = ServletUtils.parseIntOrNull(request.getParameter("mese"));

		if (progetto == null || wp == null || task == null || mese == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			writeError(response, "Parametri mancanti o non validi");
			return;
		}

		// validazione: le ore devono essere un intero non negativo
		Integer ore = ServletUtils.parseIntOrNull(request.getParameter("ore"));
		if (ore == null || ore < 0) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			writeError(response, "Le ore lavorate devono essere un numero intero non negativo.");
			return;
		}

		try (Connection connection = getConnection()) {
			CollaboratorDAO dao = new CollaboratorDAO(connection);

			// autorizzazione: il collaboratore deve essere assegnato a questo task
			if (!dao.isAssignedToTask(user.getId(), progetto, wp, task)) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				writeError(response, "Non sei assegnato a questo task.");
				return;
			}

			boolean updated = dao.updateWorkedHours(user.getId(), progetto, wp, task, mese, ore);
			if (!updated) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				writeError(response, "Il mese selezionato non e' valido per questo task.");
				return;
			}

			JsonObject ok = new JsonObject();
			ok.addProperty("success", true);
			ok.addProperty("worked", ore);
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write(new Gson().toJson(ok));
		} catch (SQLException e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			writeError(response, "Errore durante il salvataggio nel database.");
		}
	}

	private void writeError(HttpServletResponse response, String message) throws IOException {
		JsonObject err = new JsonObject();
		err.addProperty("success", false);
		err.addProperty("error", message);
		response.getWriter().write(new Gson().toJson(err));
	}
}
