package it.polimi.tiw.project.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import it.polimi.tiw.project.beans.Position;
import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.dao.ManagerDAO;
import it.polimi.tiw.project.utils.ServletUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Endpoint di AZIONE (JSON) dell'interfaccia JavaScript del responsabile.
 *
 * <p>Il parametro {@code action} seleziona l'operazione: {@code save} (SALVA),
 * {@code assign} (ASSEGNA), {@code conclude} (CONCLUDI). Le risposte seguono lo
 * stile del resto del progetto: {@code {"success":true,...}} oppure
 * {@code {"success":false,"error":"..."}} (per l'ASSEGNA fallito, anche
 * {@code {"success":false,"problems":[...]}}).</p>
 *
 * <p>Se l'ASSEGNA fallisce i dati inseriti non si perdono: restano nel DOM lato
 * client, il server si limita a segnalare i problemi.</p>
 */
@WebServlet("/ManagerActionJS")
public class ManagerActionJS extends MyServlet {

	private static final long serialVersionUID = 1L;
	private final Gson gson = new Gson();

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		HttpSession session = request.getSession(false);
		User user = (session != null) ? (User) session.getAttribute("user") : null;
		if (user == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			writeError(response, "Utente non autenticato");
			return;
		}
		Position position = user.getPosition();
		if (!Position.MANAGER.equals(position) && !Position.TECHNICIAN.equals(position)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			writeError(response, "Accesso negato");
			return;
		}

		String action = request.getParameter("action");
		String progetto = ServletUtils.trimToNull(request.getParameter("progetto"));
		if (action == null || progetto == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			writeError(response, "Parametri 'action'/'progetto' mancanti");
			return;
		}

		try (Connection connection = getConnection()) {
			ManagerDAO dao = new ManagerDAO(connection);

			if (!dao.isManagerOf(user.getId(), progetto)) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				writeError(response, "Non sei il responsabile di questo progetto");
				return;
			}

			switch (action) {

			case "save": {
				Integer wp = ServletUtils.parseIntOrNull(request.getParameter("wp"));
				Integer task = ServletUtils.parseIntOrNull(request.getParameter("task"));
				if (wp == null || task == null) {
					writeError(response, "Selezione di WP/task mancante");
					return;
				}
				String status = dao.findProjectStatus(progetto);
				if (status == null || !status.equalsIgnoreCase("creato")) {
					writeError(response, "Il progetto non e' piu' modificabile (stato: " + status + ")");
					return;
				}

				List<Integer> months = dao.findTaskMonths(progetto, wp, task);
				Map<Integer, Integer> hours = new LinkedHashMap<>();
				for (Integer m : months) {
					String raw = ServletUtils.trimToNull(request.getParameter("ore_" + m));
					if (raw == null)
						continue;
					Integer value = ServletUtils.parseIntOrNull(raw);
					if (value == null || value < 0) {
						writeError(response, "Ore non valide nel mese M" + m);
						return;
					}
					hours.put(m, value);
				}

				List<Integer> collaborators = new ArrayList<>();
				String[] raw = request.getParameterValues("collaboratore");
				if (raw != null) {
					for (String s : raw) {
						Integer id = ServletUtils.parseIntOrNull(s);
						if (id != null && id != user.getId() && !collaborators.contains(id))
							collaborators.add(id);
					}
				}

				dao.saveAssignment(progetto, wp, task, months, hours, collaborators);
				writeSuccess(response);
				return;
			}

			case "assign": {
				String status = dao.findProjectStatus(progetto);
				if (status == null || !status.equalsIgnoreCase("creato")) {
					writeError(response, "Il progetto non e' nello stato 'creato'");
					return;
				}
				List<String> problems = dao.validateForAssignment(progetto);
				if (problems.isEmpty()) {
					dao.setProjectAssigned(progetto, user.getId());
					writeSuccess(response);
				} else {
					JsonObject out = new JsonObject();
					out.addProperty("success", false);
					out.add("problems", gson.toJsonTree(problems));
					response.setStatus(HttpServletResponse.SC_OK);
					response.getWriter().write(gson.toJson(out));
				}
				return;
			}

			case "conclude": {
				if (!dao.getProjectMonitoringBoard(progetto).isConcludable()) {
					writeError(response, "Non tutti i task hanno ore lavorate sufficienti");
					return;
				}
				boolean done = dao.setProjectConcluded(progetto, user.getId());
				if (done)
					writeSuccess(response);
				else
					writeError(response, "Il progetto non e' nello stato 'assegnato'");
				return;
			}

			default:
				writeError(response, "Azione sconosciuta");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			writeError(response, "Errore durante l'operazione");
		}
	}

	private void writeSuccess(HttpServletResponse response) throws IOException {
		JsonObject ok = new JsonObject();
		ok.addProperty("success", true);
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().write(gson.toJson(ok));
	}

	private void writeError(HttpServletResponse response, String message) throws IOException {
		JsonObject err = new JsonObject();
		err.addProperty("success", false);
		err.addProperty("error", message);
		response.getWriter().write(gson.toJson(err));
	}
}
