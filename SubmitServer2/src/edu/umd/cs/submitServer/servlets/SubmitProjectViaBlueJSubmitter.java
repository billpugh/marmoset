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

package edu.umd.cs.submitServer.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.marmoset.modelClasses.Project;
import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration;
import edu.umd.cs.submitServer.ClientRequestException;
import edu.umd.cs.submitServer.MultipartRequest;

public class SubmitProjectViaBlueJSubmitter extends SubmitServerServlet {

	/**
	 * The doPost method of the servlet. <br>
	 * 
	 * This method is called when a form has its tag value method equals to
	 * post.
	 * 
	 * @param request
	 *            the request send by the client to the server
	 * @param response
	 *            the response send by the server to the client
	 * @throws ServletException
	 *             if an error occurred
	 * @throws IOException
	 *             if an error occurred
	 */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		MultipartRequest multipartRequest = (MultipartRequest) request
				.getAttribute(MULTIPART_REQUEST);
		String campusUID = multipartRequest
				.getOptionalCheckedParameter("campusUID");
		String password = multipartRequest.getOptionalPasswordParameter();
		String courseName = multipartRequest
				.getOptionalCheckedParameter("courseName");
		String semester = multipartRequest
				.getOptionalCheckedParameter("semester");
		String projectNumber = multipartRequest
				.getOptionalCheckedParameter("projectNumber");
		Connection conn = null;
		try {
			conn = getConnection();

			// Authenticate student against the database/LDAP system
			Student student = getIAuthenticationService(true).authenticateLDAP(
					campusUID, password, conn, false);
			if (student == null)
				throw new ServletException("Password doesn't match username "
						+ campusUID);

			// Lookup the project
			Project project = Project.lookupByCourseProjectSemester(courseName,
					projectNumber, semester, conn);
			if (project == null)
				throw new ServletException("Could not find record for project "
						+ projectNumber + " in " + courseName + ", " + semester);
			// Get corresponding course.
			Course course = project.getCorrespondingCourse(conn);

			// Get studentRegistration
			StudentRegistration studentRegistration = StudentRegistration
					.lookupByStudentPKAndCoursePK(student.getStudentPK(),
							project.getCoursePK(), conn);
			if (studentRegistration == null)
				throw new ServletException(
						student.getFirstname()
								+ " "
								+ student.getLastname()
								+ " is not registered for this course.  "
								+ " If you changed your DirectoryID, please notify your instructor so that we can get the system upated");

			request.setAttribute("course", course);
			request.setAttribute("studentRegistration", studentRegistration);
			request.setAttribute("user", student);
			request.setAttribute("project", project);
			request.setAttribute("webBasedUpload", Boolean.FALSE);
			// forward to the UploadSubmission servlet for the heavy lifting
			String uploadSubmission = "/action/UploadSubmission";
			RequestDispatcher dispatcher = request
					.getRequestDispatcher(uploadSubmission);
			dispatcher.forward(request, response);
		} catch (NamingException e) {
			throw new ServletException(e);
		} catch (ClientRequestException e) {
			throw new ServletException(e);
		} catch (SQLException e) {
			throw new ServletException(e);
		} finally {
			releaseConnection(conn);
		}
	}
}
