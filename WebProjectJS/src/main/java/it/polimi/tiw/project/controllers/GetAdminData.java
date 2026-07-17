package it.polimi.tiw.project.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import it.polimi.tiw.project.beans.AdminDtos;
import it.polimi.tiw.project.beans.Position;
import it.polimi.tiw.project.beans.Project;
import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.dao.AdminDAO;
import it.polimi.tiw.project.utils.ServletUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Endpoint di LETTURA (JSON) dell'interfaccia JavaScript dell'AMMINISTRATORE.
 *
 * <p>Il parametro {@code resource} seleziona il dato richiesto:</p>
 * <ul>
 *   <li>{@code managers} - personale tecnico assegnabile come responsabile;</li>
 *   <li>{@code projects} - progetti creati dall'amministratore corrente;</li>
 *   <li>{@code projectStructure} (con {@code progetto=...}) - gerarchia WP/task con
 *       i totali delle ore previste e lavorate.</li>
 * </ul>
 *
 * <p>Un amministratore vede e ispeziona solo i progetti da lui creati.</p>
 */
@WebServlet("/GetAdminData")
public class GetAdminData extends MyServlet {

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
        if (!Position.ADMIN.equals(user.getPosition())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Accesso negato\"}");
            return;
        }

        String resource = request.getParameter("resource");
        if (resource == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Parametro 'resource' mancante");
            return;
        }
        String progetto = ServletUtils.trimToNull(request.getParameter("progetto"));

        try (Connection connection = getConnection()) {
            AdminDAO dao = new AdminDAO(connection);

            switch (resource) {

            case "managers": {
                List<AdminDtos.ManagerOption> out = new ArrayList<>();
                for (User u : dao.findTechnicalStaff())
                    out.add(new AdminDtos.ManagerOption(u.getId(), u.getName(), u.getSurname()));
                writeOk(response, out);
                return;
            }

            case "projects": {
                List<AdminDtos.ProjectItem> out = new ArrayList<>();
                for (Project p : dao.findAdminProjects(user.getId()))
                    out.add(new AdminDtos.ProjectItem(p.getTitle(), p.getStatus()));
                writeOk(response, out);
                return;
            }

            case "projectStructure": {
                if (progetto == null) {
                    writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Parametro 'progetto' mancante");
                    return;
                }
                if (!dao.isAdminOf(user.getId(), progetto)) {
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, "Progetto non tuo o inesistente");
                    return;
                }
                writeOk(response, dao.getProjectStructure(progetto));
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

    private void writeOk(HttpServletResponse response, Object payload) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(payload));
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
