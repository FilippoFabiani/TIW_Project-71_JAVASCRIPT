package it.polimi.tiw.project.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.text.StringEscapeUtils;

import com.google.gson.JsonObject;

import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.beans.UserCredential;
import it.polimi.tiw.project.dao.UserCredentialDAO;
import it.polimi.tiw.project.exceptions.BusinessException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/CheckLogin")
@MultipartConfig
public class CheckLogin extends MyServlet{

		private static final long serialVersionUID = 1L;

		public CheckLogin() {
			super();
		}

		@Override
		protected void doPost(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			// obtain and escape params
			String usrn = null;
			String pwd = null;
			User user = null;
			UserCredentialDAO userDAO = null;
			UserCredential credential = null;
			
			usrn = StringEscapeUtils.escapeJava(request.getParameter("username"));
			pwd = StringEscapeUtils.escapeJava(request.getParameter("pwd"));
			if (usrn == null || pwd == null || usrn.isEmpty() || pwd.isEmpty() ) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("Credentials must be not null");
				return;
			}
			// query db to authenticate for user
			//autoclosable connection, if an exception is thrown the connection is closed and the exception is propagated
			try (Connection connection = getConnection()) {
				userDAO = new UserCredentialDAO(connection);
				credential = new UserCredential(usrn, pwd);
				user = userDAO.checkCredential(credential);
				
			} catch (SQLException e) {
				// TODO manage exception, creating an handling servlet
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().println("Not Possible to check credentials");
				e.printStackTrace();
				return;
				} catch (BusinessException e){
					//TODO manage business exception, creating an handling servlet 
					// maybe each page can have a error message to show the user
				}
			
			if(user == null) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.getWriter().println("Incorrect credentials");
			} else {
				request.getSession().setAttribute("user", user);
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				
				JsonObject jsonResponse = new JsonObject();
				jsonResponse.addProperty("position", user.getPosition().toString());
				jsonResponse.addProperty("username", user.getUsername());
				
				
				response.getWriter().println(jsonResponse.toString());
			}
			
			
			
			
		}

}
		
