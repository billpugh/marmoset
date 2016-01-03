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
 * Created on Jan 11, 2005
 *
 * @author jspacco
 */

package edu.umd.cs.submitServer.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.umd.cs.marmoset.modelClasses.BrowserEditing;
import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.submitServer.RequestParser;
import edu.umd.cs.submitServer.UserSession;

public class UpdateCourse extends SubmitServerServlet {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession();
		UserSession userSession = (UserSession) session
				.getAttribute(USER_SESSION);

		Course course = (Course) request.getAttribute("course");
		RequestParser parser = new RequestParser(request,
				getSubmitServerServletLog(), strictParameterChecking());
		String section  = parser.getOptionalCheckedParameter("section");
		String description = parser.getOptionalCheckedParameter("description");
		String url =  parser.getOptionalCheckedParameter("url");
		BrowserEditing browserEditing = BrowserEditing.valueOfAnyCase(
		        parser.getCheckedParameter("browserEditing"));
		boolean download = parser.getCheckbox("download");
		boolean helpRequests = parser.getCheckbox("helpRequests");
		boolean useDefaultBuildservers = parser.getCheckbox("useDefaultBuildservers");

		Connection conn = null;
		try {
			conn = getConnection();
			if (description != null)
				course.setDescription(description);
			if (url != null)
				course.setUrl(url);
			if (section != null)
			    course.setSection(section);
			course.setAllowsBaselineDownload(download);
			course.setBrowserEditing(browserEditing);
			course.setAllowsHelpRequests(helpRequests);
			course.setUseDefaultBuildservers(useDefaultBuildservers);
			
			// insert the course
			course.update(conn);
			String redirectUrl = request.getContextPath()
					+ "/view/instructor/course.jsp?coursePK="
					+ course.getCoursePK();
			response.sendRedirect(redirectUrl);

		} catch (SQLException e) {
			handleSQLException(e);
			throw new ServletException(e);
		}
	}

}
