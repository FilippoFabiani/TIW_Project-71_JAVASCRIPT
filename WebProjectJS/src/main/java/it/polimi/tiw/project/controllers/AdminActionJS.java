package it.polimi.tiw.project.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import it.polimi.tiw.project.beans.AdminDtos.SaveProjectRequest;
import it.polimi.tiw.project.beans.AdminDtos.TaskReq;
import it.polimi.tiw.project.beans.AdminDtos.WpReq;
import it.polimi.tiw.project.beans.Position;
import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.dao.AdminDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Endpoint di AZIONE (JSON) dell'interfaccia JavaScript dell'AMMINISTRATORE.
 *
 * <p>Riceve nel corpo (Content-Type application/json) l'intera struttura del progetto
 * costruita lato client e, con {@code action="saveProject"}, la registra in modo
 * permanente. La validita' e' ricontrollata lato server: se qualcosa non va, nessuna
 * scrittura avviene e i dati restano nel client (che non li perde).</p>
 *
 * <p>Formato risposta, in linea col resto del progetto:
 * {@code {"success":true}} oppure {@code {"success":false,"error":"..."}}, e per i
 * problemi di validazione {@code {"success":false,"problems":[...]}}.</p>
 */
@WebServlet("/AdminActionJS")
public class AdminActionJS extends MyServlet {

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
        if (!Position.ADMIN.equals(user.getPosition())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeError(response, "Accesso negato");
            return;
        }

        SaveProjectRequest req = readBody(request);
        if (req == null || req.action == null || !req.action.equals("saveProject")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(response, "Richiesta non valida");
            return;
        }

        /* validazione strutturale lato server */
        List<String> problems = validate(req);
        if (!problems.isEmpty()) {
            writeProblems(response, problems);
            return;
        }

        String titolo = req.titolo.trim();

        try (Connection connection = getConnection()) {
            AdminDAO dao = new AdminDAO(connection);

            if (dao.projectExists(titolo)) {
                writeError(response, "Esiste gia' un progetto con questo titolo.");
                return;
            }
            if (!dao.isTecnico(req.responsabile)) {
                writeError(response, "Il responsabile selezionato non e' personale tecnico valido.");
                return;
            }

            req.titolo = titolo;   // titolo normalizzato
            dao.createProject(user.getId(), req);
            writeSuccess(response);
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(response, "Errore durante il salvataggio nel database.");
        }
    }

    /* Deserializza il corpo JSON; ritorna null se assente o malformato. */
    private SaveProjectRequest readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        try {
            return gson.fromJson(sb.toString(), SaveProjectRequest.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    /*
     * Controlli di validita' (gli stessi vincoli del client):
     *  - titolo non vuoto, durata intero positivo, responsabile presente;
     *  - almeno un WP e ogni WP con almeno un task;
     *  - mesi del WP entro [1..durata], mesi del task entro l'intervallo del WP.
     */
    private List<String> validate(SaveProjectRequest req) {
        List<String> problems = new ArrayList<>();

        if (req.titolo == null || req.titolo.trim().isEmpty()) {
            problems.add("Titolo del progetto mancante.");
        }
        if (req.durata == null || req.durata < 1) {
            problems.add("Durata del progetto non valida.");
        }
        if (req.responsabile == null) {
            problems.add("Responsabile non selezionato.");
        }
        if (req.wps == null || req.wps.isEmpty()) {
            problems.add("Il progetto deve contenere almeno un WP.");
            return problems;   // senza WP non ha senso proseguire
        }
        int durata = (req.durata != null) ? req.durata : 0;

        for (int i = 0; i < req.wps.size(); i++) {
            WpReq wp = req.wps.get(i);
            String wpLabel = "WP" + (i + 1);

            if (wp.titolo == null || wp.titolo.trim().isEmpty()) {
                problems.add(wpLabel + ": titolo mancante.");
            }
            if (wp.meseInizio == null || wp.meseFine == null
                    || wp.meseInizio < 1 || wp.meseFine > durata || wp.meseInizio > wp.meseFine) {
                problems.add(wpLabel + ": intervallo mesi non valido (deve stare in 1.." + durata + ").");
            }
            if (wp.tasks == null || wp.tasks.isEmpty()) {
                problems.add(wpLabel + ": deve contenere almeno un task.");
                continue;
            }
            for (int j = 0; j < wp.tasks.size(); j++) {
                TaskReq t = wp.tasks.get(j);
                String tLabel = "T" + (i + 1) + "." + (j + 1);
                if (t.titolo == null || t.titolo.trim().isEmpty()) {
                    problems.add(tLabel + ": titolo mancante.");
                }
                if (t.meseInizio == null || t.meseFine == null || t.meseInizio > t.meseFine) {
                    problems.add(tLabel + ": intervallo mesi non valido.");
                } else if (wp.meseInizio != null && wp.meseFine != null
                        && (t.meseInizio < wp.meseInizio || t.meseFine > wp.meseFine)) {
                    problems.add(tLabel + ": i mesi devono stare nell'intervallo del " + wpLabel + ".");
                }
            }
        }
        return problems;
    }

    private void writeSuccess(HttpServletResponse response) throws IOException {
        JsonObject ok = new JsonObject();
        ok.addProperty("success", true);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(ok));
    }

    private void writeProblems(HttpServletResponse response, List<String> problems) throws IOException {
        JsonObject out = new JsonObject();
        out.addProperty("success", false);
        out.add("problems", gson.toJsonTree(problems));
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(gson.toJson(out));
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        JsonObject err = new JsonObject();
        err.addProperty("success", false);
        err.addProperty("error", message);
        response.getWriter().write(gson.toJson(err));
    }
}
