package it.polimi.tiw.project.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import it.polimi.tiw.project.beans.ManagerDtos.OptionDTO;
import it.polimi.tiw.project.beans.ManagerDtos.PersonDTO;
import it.polimi.tiw.project.beans.ManagerDtos.ProjectDTO;
import it.polimi.tiw.project.beans.ManagerDtos.TaskDetailDTO;
import it.polimi.tiw.project.beans.Position;
import it.polimi.tiw.project.beans.Project;
import it.polimi.tiw.project.beans.Task;
import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.beans.WorkPackage;
import it.polimi.tiw.project.dao.ManagerDAO;
import it.polimi.tiw.project.utils.ServletUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Endpoint di LETTURA (JSON) dell'interfaccia JavaScript del responsabile.
 *
 * <p>Il parametro {@code resource} seleziona il dato richiesto:
 * {@code projects}, {@code wps}, {@code tasks}, {@code taskDetail},
 * {@code collaborators}, {@code projectMonitoring}, {@code collaboratorMonitoring}.
 * Riusa la logica di {@link ManagerDAO}.</p>
 */
@WebServlet("/GetManagerData")
public class GetManagerData extends MyServlet {

	private static final long serialVersionUID = 1L;
	private final Gson gson = new Gson();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		HttpSession session = request.getSession(false);
		User user = (session != null) ? (User) session.getAttribute("user") : null;
		if (user == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().write("{\"error\":\"Utente non autenticato\"}");
			return;
		}
		Position position = user.getPosition();
		if (!Position.MANAGER.equals(position) && !Position.TECHNICIAN.equals(position)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.getWriter().write("{\"error\":\"Accesso negato: profilo di responsabile richiesto.\",\"reauth\":true}");
			return;
		}

		String resource = request.getParameter("resource");
		if (resource == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().write("{\"error\":\"Parametro 'resource' mancante\"}");
			return;
		}

		String progetto = ServletUtils.trimToNull(request.getParameter("progetto"));
		Integer wp = ServletUtils.parseIntOrNull(request.getParameter("wp"));
		Integer task = ServletUtils.parseIntOrNull(request.getParameter("task"));
		Integer collab = ServletUtils.parseIntOrNull(request.getParameter("collaboratore"));

		try (Connection connection = getConnection()) {
			ManagerDAO dao = new ManagerDAO(connection);

			switch (resource) {

			case "me": {
				// profilo del responsabile per il saluto: nome, cognome e URL leggero della foto
				User me = dao.findTechnician(user.getId());
				String name = (me != null) ? me.getName() : null;
				String surname = (me != null) ? me.getSurname() : null;
				writeOk(response, new ProfileDTO(name, surname, "photo?id=" + user.getId()));
				return;
			}

			case "projects": {
				List<ProjectDTO> out = new ArrayList<>();
				for (Project p : dao.findManagerProjects(user.getId()))
					out.add(new ProjectDTO(p.getTitle(), p.getTerm(), p.getStatus()));
				writeOk(response, out);
				return;
			}

			case "wps": {
				if (!requireOwnedProject(dao, user, progetto, response))
					return;
				List<OptionDTO> out = new ArrayList<>();
				for (WorkPackage w : dao.findWorkPackages(progetto))
					out.add(new OptionDTO(w.getIdOrder(), w.getWpAsString() + " - " + w.getTitle()));
				writeOk(response, out);
				return;
			}

			case "tasks": {
				if (!requireOwnedProject(dao, user, progetto, response))
					return;
				if (wp == null) {
					writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Parametro 'wp' mancante");
					return;
				}
				List<OptionDTO> out = new ArrayList<>();
				for (Task t : dao.findTasks(progetto, wp))
					out.add(new OptionDTO(t.getIdOrder(), "T" + wp + "." + t.getIdOrder() + " - " + t.getTitle()));
				writeOk(response, out);
				return;
			}

			case "taskDetail": {
				if (!requireOwnedProject(dao, user, progetto, response))
					return;
				if (wp == null || task == null) {
					writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Parametri 'wp'/'task' mancanti");
					return;
				}
				List<PersonDTO> collaborators = new ArrayList<>();
				for (User u : dao.findAssignableCollaborators(user.getId()))
					collaborators.add(new PersonDTO(u.getId(), u.getName(), u.getSurname()));

				String status = dao.findProjectStatus(progetto);
				boolean editable = status != null && status.equalsIgnoreCase("creato");

				TaskDetailDTO detail = new TaskDetailDTO(
						dao.findTaskMonths(progetto, wp, task),
						dao.findPlannedHours(progetto, wp, task),
						collaborators,
						dao.findAssignedCollaboratorIds(progetto, wp, task),
						editable,
						status);
				writeOk(response, detail);
				return;
			}

			case "collaborators": {
				List<PersonDTO> out = new ArrayList<>();
				for (User u : dao.findCollaboratorsOfManagerProjects(user.getId()))
					out.add(new PersonDTO(u.getId(), u.getName(), u.getSurname()));
				writeOk(response, out);
				return;
			}

			case "projectMonitoring": {
				if (!requireOwnedProject(dao, user, progetto, response))
					return;
				writeOk(response, dao.getProjectMonitoringBoard(progetto));
				return;
			}

			case "collaboratorMonitoring": {
				if (collab == null) {
					writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Parametro 'collaboratore' mancante");
					return;
				}
				boolean allowed = dao.findCollaboratorsOfManagerProjects(user.getId())
						.stream().anyMatch(u -> u.getId() == collab);
				if (!allowed) {
					writeError(response, HttpServletResponse.SC_FORBIDDEN, "Collaboratore non consentito");
					return;
				}
				writeOk(response, dao.getCollaboratorMonitoringBoard(user.getId(), collab));
				return;
			}

			default:
				writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Risorsa sconosciuta");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore nel recupero dei dati");
		}
	}

	private boolean requireOwnedProject(ManagerDAO dao, User user, String progetto, HttpServletResponse response)
			throws SQLException, IOException {
		if (progetto == null) {
			writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Parametro 'progetto' mancante");
			return false;
		}
		if (!dao.isManagerOf(user.getId(), progetto)) {
			writeError(response, HttpServletResponse.SC_FORBIDDEN, "Non sei il responsabile di questo progetto");
			return false;
		}
		return true;
	}

	private void writeOk(HttpServletResponse response, Object payload) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().write(gson.toJson(payload));
	}

	private void writeError(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.getWriter().write("{\"error\":\"" + message + "\"}");
	}

	/** payload della risorsa "me": saluto personalizzato + URL leggero della foto */
	private static class ProfileDTO {
		@SuppressWarnings("unused") final String name;
		@SuppressWarnings("unused") final String surname;
		@SuppressWarnings("unused") final String photoUrl;

		ProfileDTO(String name, String surname, String photoUrl) {
			this.name = name;
			this.surname = surname;
			this.photoUrl = photoUrl;
		}
	}
}
