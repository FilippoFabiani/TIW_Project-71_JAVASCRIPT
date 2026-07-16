package it.polimi.tiw.project.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import it.polimi.tiw.project.beans.User;
import it.polimi.tiw.project.beans.UserCredential;
import it.polimi.tiw.project.exceptions.BusinessException;
import it.polimi.tiw.project.exceptions.ErrorCode;
import it.polimi.tiw.project.beans.Position;
import java.sql.Connection;

public class UserCredentialDAO extends DAO {



		public UserCredentialDAO(Connection connection) {
			super(connection);
		}

		/**
		 * checks whether the credentials are valid and translates into the {@link User}
		 * bean.
		 */
		public User checkCredential(UserCredential credential) throws SQLException {

			String query = """

					SELECT id, position
					FROM User
					WHERE username = ? AND password = ?

					""";

			try (PreparedStatement pstatement = connection.prepareStatement(query)) {
				pstatement.setString(1, credential.getUsername());
				pstatement.setString(2, credential.getPassword());

				try (ResultSet result = pstatement.executeQuery()) {
					if (!result.isBeforeFirst())
						// this credentials are not valid
						// TODO gestire con codice di errore ma senza eccezione (?)
						return null;
					else {
						result.next();

						User user = new User();
						int id = result.getInt("id");
						Position position = Position.parsePosition(result.getString("position"));

						user.setId(id);
						user.setPosition(setIfTechProfile(id, position));
						return user;
					}
				}
			}
		}

		/**
		 * This method is used to determine which tech profile the user has, based on
		 * the user ID. Wheter the user is admin, tech profile is unchecked. It returns
		 * a Position enum value corresponding to the user's role in the system.
		 */
		private Position setIfTechProfile(int userId, Position position) throws SQLException {

			boolean isManager = false;
			boolean isCollaborator = false;

			if (position.equals(Position.ADMIN))
				return position;

			String query = """

					SELECT isManager, isCollaborator
					FROM Tecnico
					WHERE id = ?

					""";

			try (PreparedStatement pstatement = connection.prepareStatement(query)) {
				pstatement.setInt(1, userId);

				try (ResultSet result = pstatement.executeQuery()) {
					if (!result.isBeforeFirst())
						// the user must exists at this phase
						throw new BusinessException(ErrorCode.USER_NOT_FOUND);
					else {
						result.next();
						if (result.getBoolean("isManager"))
							isManager = true;
						if (result.getBoolean("isCollaborator"))
							isCollaborator = true;
					}
				}

			}

			Position techPosition;

			if (isManager && isCollaborator)
				techPosition = Position.TECHNICIAN;
			else if (isManager && !isCollaborator)
				techPosition = Position.MANAGER;
			else if (!isManager && isCollaborator)
				techPosition = Position.COLLABORATOR;
			else
				throw new BusinessException(ErrorCode.INCONSISTENT_DB_DATA);

			return techPosition;
		}

	

}
