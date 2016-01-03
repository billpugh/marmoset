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
import javax.servlet.http.HttpSession;

import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration;
import edu.umd.cs.submitServer.ClientRequestException;
import edu.umd.cs.submitServer.RequestParser;
import edu.umd.cs.submitServer.StudentForUpload;
import edu.umd.cs.submitServer.UserSession;

public class RegisterOneStudent extends SubmitServerServlet {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Connection conn = null;
		boolean transactionSuccess = false;
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

			RequestParser parser = new RequestParser(request,
					getSubmitServerServletLog(), strictParameterChecking());
			@Student.PK Integer studentPK = 
					Student.asPK(parser.getOptionalInteger("existingStudentPK"));
			Student student;
			if (studentPK != null) {
				student = Student.lookupByStudentPK(studentPK, conn);
			} else {
				StudentForUpload studentForUpload = new StudentForUpload(parser);
				student = studentForUpload.lookupOrInsert(conn);
			}
			
			Course course = (Course) request.getAttribute("course");
			if (course != null) {
			    String classAccount = parser.getOptionalCheckedParameter("classAccount");
			    if (classAccount == null)
			        classAccount = student.getLoginName();
			    String capability = parser
			            .getOptionalCheckedParameter("capability");
			    String section = "";
	               
			    if (capability == null || "".equals(capability) || "student".equals("capability")) {
			        capability = null;
			        section = parser.getOptionalCheckedParameter("section");
			    }
			       

			    StudentForUpload.registerStudent(course,
			            student, section, classAccount,  StudentRegistration.asCapability(capability), conn);
			}

			conn.commit();
			transactionSuccess = true;

			HttpSession session = request.getSession();
			UserSession userSession = (UserSession) session
	        .getAttribute(USER_SESSION);
			String redirectUrl;
			if (course != null)
			    redirectUrl = request.getContextPath()
					+ "/view/instructor/course.jsp?coursePK="
					+ course.getCoursePK();
			else if (!userSession.isSuperUser()) 
			    redirectUrl = request.getContextPath()
                + "/view/admin/index.jsp";
			else redirectUrl = request.getContextPath()
          + "/view/index.jsp";
			  
			    
			response.sendRedirect(redirectUrl);
		} catch (ClientRequestException e) {
			throw new ServletException(e);
		} catch (SQLException e) {
			throw new ServletException(e);
		} finally {
			rollbackIfUnsuccessfulAndAlwaysReleaseConnection(
					transactionSuccess, request, conn);
		}
	}

}
