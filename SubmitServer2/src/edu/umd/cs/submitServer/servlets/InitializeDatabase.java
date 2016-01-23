/**
 * Marmoset: a student project snapshot, submission, testing and code review
 * system developed by the Univ. of Maryland, College Park
 * 
 * Developed as part of Jaime Spacco's Ph.D. thesis work, continuing effort led
 * by William Pugh. See http://marmoset.cs.umd.edu/
 * 
 * Copyright 2005 - 2011, Univ. of Maryland
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */

/*
 * Created on Jan 9, 2005
 *
 * @author jspacco
 */
package edu.umd.cs.submitServer.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.submitServer.RequestParser;
import edu.umd.cs.submitServer.WebConfigProperties;

/**
 * This servlet creates an admin user in the database if its empty, otherwise throws an exception.
 * 
 * @author jspacco
 *
 */
public class InitializeDatabase extends GradeServerInterfaceServlet {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		RequestParser parser = new RequestParser(request,
				getSubmitServerServletLog(), strictParameterChecking());
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		session = request.getSession(true);
		String loginName = parser.getOptionalCheckedParameter("loginName");
		if (loginName == null || loginName.trim().equals("")) {
			request.setAttribute("missingIDOrPasswordException", Boolean.TRUE);
			request.getRequestDispatcher("/index.jsp").forward(request,
					response);
			return;
		}

		Connection conn = null;
		Connection gradesConn = null;
		try {
			conn = getConnection();

			if (Student.existAny(conn))
				throw new ServletException("Submit server already initialized");
			WebConfigProperties webProperties = WebConfigProperties.get();
			boolean skipAuthentication = "true".equals(webProperties.getProperty(SKIP_AUTHENTICATION));

      Student s = new Student();
      if (skipAuthentication) {
        s.setLoginName(loginName);
        s.setLastname("Doe");
        s.setFirstname(loginName);
        s.setCampusUID("");
      } else {
        gradesConn = getGradesConnection();

        String query = "SELECT lastName, firstName, uid" + " FROM submitexport " + " WHERE directoryId = ?"
            + " LIMIT 1";
        PreparedStatement stmt = gradesConn.prepareStatement(query);
        stmt.setString(1, loginName);
        ResultSet rs = stmt.executeQuery();

        if (!rs.next())
          throw new ServletException("Can't import that user from grades");

        // TODO(rwsims): this information should be populated by the
        // authentication mechanism.
        int col = 1;
        s.setLastname(rs.getString(col++));
        s.setFirstname(rs.getString(col++));
        s.setCampusUID(rs.getString(col++));
        s.setLoginName(loginName);
        rs.close();
        stmt.close();
      }
      s.setCanImportCourses(true);
			s = s.insertOrUpdateCheckingLoginNameAndCampusUID(conn);

			Student superuser = s.lookupOrCreateAdminAccount(conn);

			// Sets required information in the user's session.
			PerformLogin.setUserSession(session, superuser, conn);


			response.sendRedirect(request.getContextPath() + "/view/admin/index.jsp");
		} catch (SQLException e) {
			handleSQLException(e);
			throw new ServletException(e);
		} finally {
			releaseConnection(conn);
		}
	}


    

}
