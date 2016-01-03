package edu.umd.cs.submitServer.servlets;

import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import edu.umd.cs.submitServer.GradeServerDatabaseProperties;
import edu.umd.cs.submitServer.WebConfigProperties;

public abstract class GradeServerInterfaceServlet extends SubmitServerServlet {

	private GradeServerDatabaseProperties gradeServerDatabaseProperties;

	@Override
	public void init() throws ServletException {
		super.init();
		ServletContext servletContext = getServletContext();

		if (WebConfigProperties.get().getProperty("grades.driver") != null)
			gradeServerDatabaseProperties = new GradeServerDatabaseProperties(
					servletContext);
	}

	/**
	 * Gets a connection to the database.
	 *
	 * @return a connection to the database.
	 * @throws SQLException
	 */
	protected Connection getGradesConnection() throws SQLException {
		return gradeServerDatabaseProperties.getConnection();
	}

	protected void releaseGradesConnection(Connection conn) {
		try {
			gradeServerDatabaseProperties.releaseConnection(conn);
		} catch (SQLException e) {
			getSubmitServerServletLog().warn("Unable to close connection", e);
		}
	}


}
