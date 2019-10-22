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
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jasig.cas.client.authentication.AttributePrincipal;

import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration;
import edu.umd.cs.marmoset.utilities.Charsets;
import edu.umd.cs.submitServer.BadPasswordException;
import edu.umd.cs.submitServer.CanNotFindDirectoryIDException;
import edu.umd.cs.submitServer.ClientRequestException;
import edu.umd.cs.submitServer.ILDAPAuthenticationService;
import edu.umd.cs.submitServer.RequestParser;
import edu.umd.cs.submitServer.UserSession;
import edu.umd.cs.submitServer.WebConfigProperties;

public class PerformCASLogin extends SubmitServerServlet {
 private static final WebConfigProperties webProperties = WebConfigProperties.get();

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	  
	  
		RequestParser parser = new RequestParser(request,
				getSubmitServerServletLog(), strictParameterChecking());
	 	
		HttpSession session = request.getSession(false);
	   UserSession userSession = session == null ? null : (UserSession) session.getAttribute(USER_SESSION);

	   if (session == null || session.isNew() || userSession == null) {
	        AttributePrincipal principal = (AttributePrincipal) request.getUserPrincipal();
	        if (principal != null) {
	          String ldapName = principal.getName();
	          if (ldapName != null) {
	            session = request.getSession(true);
	            try (Connection conn = getConnection()) {
	              Student student = Student.lookupByLoginName(ldapName, conn);
	              if (student != null) {
	                setUserSession(session, student, true, conn);
	                request.setAttribute(STUDENT, student);
	                request.setAttribute("authType", "cas");
	              } else
	                throw new ServletException("No student found for " + ldapName);
	            } catch (SQLException e) {
                throw new ServletException(e);
              }
	          } else
	            throw new ServletException("No name provided for CAS principle");
	        }
	   }
	                
		
			// check to see if user tried to view a page before logging in
			String target = parser.getStringParameter("target");

			if (target != null && !target.equals("")) {
			  System.out.println("Redirecting to " + target);
				//target = Charsets.decodeURL(target);
				response.sendRedirect(target);
				return;
			}

			// otherwise redirect to the main view page
			response.sendRedirect(request.getContextPath() + "/view/index.jsp");
		
	}


	public static void setUserSession(HttpSession session, Student student,
			boolean isCAS, Connection conn) throws SQLException {
		// look up list of student registrations for this studentPK
		List<StudentRegistration> collection = StudentRegistration
				.lookupAllByStudentPK(student.getStudentPK(), conn);

		UserSession userSession = new UserSession();
		userSession.setCasLogin(isCAS);

		// set studentPK and superUser
		userSession.setStudentPK(student.getStudentPK());
		userSession.setSuperUser(student.isSuperUser());
		// has this user returned a consent form?
		// we don't care if they've consented or not, just that they've returned
		// a form
		userSession.setGivenConsent(student.getGivenConsent());
		
		if (student.getCanImportCourses() && student.isNormalAccount()) {
			Student superuser = student.lookupAdminAccount(conn);
			if (superuser != null) {
				userSession.setSuperuserPK(superuser.getStudentPK());
			}

			Student shadow = student.lookupPseudoAccount(conn);
			if (shadow != null) {
				userSession.setShadowAccountPK(shadow.getStudentPK());
			}
		}

		if (student.isSuperUser()) {
			for(Course course : Course.lookupAll(conn))  {
				userSession.addInstructorActionCapability(course.getCoursePK());
			}

		}
		
		for (StudentRegistration registration : collection) {
			Course course = Course.lookupByStudentRegistrationPK(
					registration.getStudentRegistrationPK(), conn);

			// in my current database implementation, I give the modify
			// capability
			// to mean that someone has both modify and read-only capability
			// it might be a better idea to specifically give someone both
			// abilities
			// but it doesn't make any sense to have modify capability without
			// read-only
			// for this software.
			if (StudentRegistration.MODIFY_CAPABILITY.equals(registration
					.getInstructorCapability())) {
				userSession.addInstructorActionCapability(course.getCoursePK());
			}
			else if (StudentRegistration.READ_ONLY_CAPABILITY.equals(registration
					.getInstructorCapability())) {
				// read-only capability does not necessarily imply any other
				// privileges
				userSession.addInstructorCapability(course.getCoursePK());
			}
		}
		// set background data
		session.setAttribute(USER_SESSION, userSession);
	}

	/**
	 * Authenticate a student using the appropriate methods. Team accounts
	 * authenticate differently from normal accounts. Normal Accounts with a
	 * password in the `password` field will use the
	 * GenericAuthenticationService, while others will used the passed in
	 * authenticationService
	 *
	 * @param conn
	 * @param campusUID
	 * @param uidPassword
	 * @param skipLDAP
	 * @param authenticationService
	 *            - the service to use for normal students if the password is
	 *            null
	 * @return
	 * @throws SQLException
	 * @throws NamingException
	 * @throws ClientRequestException
	 */
	public static Student authenticateStudent(Connection conn,
			String campusUID, String uidPassword, boolean skipLDAP,
			ILDAPAuthenticationService authenticationService) throws SQLException,
			NamingException, ClientRequestException {
		// [NAT P001]
		// Lookup campusUID
		Student student = Student.lookupByLoginName(campusUID, conn);
		if (student == null)
			throw new ClientRequestException("Cannot find user " + campusUID);


			// Note: this is a read-only query.
			// So, we do not start a transaction here.

			student = authenticationService.authenticateLDAP(campusUID,
					uidPassword, conn, skipLDAP);
		
		// [end NAT P001]
		return student;
	}
}
