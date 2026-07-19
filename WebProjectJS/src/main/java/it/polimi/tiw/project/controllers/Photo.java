package it.polimi.tiw.project.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.polimi.tiw.project.beans.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Serve le fotografie dei tecnici a partire da una cartella locale esterna alla
 * webapp. Il file dell'immagine si chiama {@code <idTecnico>.jpg}.
 *
 * <p>Il client non riceve mai i byte dell'immagine dentro il JSON: gli endpoint dati
 * inseriscono solo un URL leggero (es. {@code photo?id=3}) e il browser scarica
 * l'immagine con una richiesta separata a questo servlet.</p>
 *
 * <p>La cartella e' configurabile tramite il context-param {@code photosDirectory}
 * in {@code web.xml}; in mancanza si usa il valore di default indicato sotto.</p>
 */
@WebServlet("/photo")
public class Photo extends MyServlet {

    private static final long serialVersionUID = 1L;

    /** Cartella di default (sovrascrivibile con il context-param photosDirectory). */
    private static final String DEFAULT_DIR =
            "/Users/Federico/Desktop/lezioni/Tecnologie per il web/PROGETTO/immagini";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // solo utenti autenticati possono vedere le fotografie
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // l'id deve essere composto di sole cifre: evita ogni path traversal
        String id = request.getParameter("id");
        if (id == null || !id.matches("\\d+")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String dir = getServletContext().getInitParameter("photosDirectory");
        if (dir == null || dir.trim().isEmpty()) {
            dir = DEFAULT_DIR;
        }

        File file = new File(dir, id + ".jpg");
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("image/jpeg");
        response.setContentLengthLong(file.length());
        byte[] buffer = new byte[8192];
        try (InputStream in = new FileInputStream(file);
             OutputStream out = response.getOutputStream()) {
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        }
    }
}
