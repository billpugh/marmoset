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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration;
import edu.umd.cs.submitServer.RequestParser;

public class TurnStudentIntoTA extends SubmitServerServlet {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Connection conn = null;
		boolean transactionSuccess = false;
		try {
			conn = getConnection();
			
			RequestParser parser = new RequestParser(request,
					getSubmitServerServletLog(), strictParameterChecking());
			
		  String capability = parser
          .getOptionalCheckedParameter("capability");

			Course course = (Course) request.getAttribute("course");
			StudentRegistration studentRegistration = (StudentRegistration) request.getAttribute("studentRegistration");
			if (studentRegistration.getCoursePK() != course.getCoursePK())
        throw new IllegalArgumentException();
    
			studentRegistration.setInstructorCapability( StudentRegistration.asCapability(capability));
			studentRegistration.update(conn);
			
			String redirectUrl =  request.getContextPath()
					+ "/view/instructor/course.jsp?coursePK="
					+ course.getCoursePK();
			    
			response.sendRedirect(redirectUrl);
		} catch (SQLException e) {
			throw new ServletException(e);
		} finally {
			releaseConnection(conn);
		}
	}

}
