package it.polimi.tiw.project.controllers;

import java.io.IOException;

import it.polimi.tiw.project.beans.Position;
import it.polimi.tiw.project.beans.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/HomeAdmin")
@MultipartConfig
public class HomeAdmin extends MyServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/index.html");   // redirect di sicurezza
            return;
        }
        User user = (User) session.getAttribute("user");
        Position p = user.getPosition();
        if (!p.equals(Position.ADMIN)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato: profilo di amministratore richiesto.");
            return;
        }
        response.setContentType("text/html;charset=UTF-8");
        request.getRequestDispatcher("/WEB-INF/templates/homeAdmin.html").forward(request, response);
    }
	

}
