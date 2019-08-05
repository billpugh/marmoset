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
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.umd.cs.marmoset.modelClasses.CodeReviewer;
import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.marmoset.modelClasses.ServerError;
import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration;
import edu.umd.cs.marmoset.modelClasses.Submission;
import edu.umd.cs.submitServer.RequestParser;
import edu.umd.cs.submitServer.UserSession;
import edu.umd.cs.submitServer.WebConfigProperties;

public class ImportCourse extends GradeServerInterfaceServlet {
	private static final WebConfigProperties webProperties = WebConfigProperties.get();

	Pattern courseNamePattern = Pattern.compile("[\\w-]+");
	Pattern termPattern = Pattern.compile("\\d{6}");

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession();

		UserSession userSession = (UserSession) session
				.getAttribute(USER_SESSION);

		String term = request.getParameter("term");
		if (term == null)
			term = webProperties.getRequiredProperty("semester");

		if (!termPattern.matcher(term).matches())
			throw new ServletException("Invalid term");

		String[] courseIDs = request.getParameterValues("courseID");
		if (courseIDs == null || courseIDs.length == 0)
			throw new ServletException("No courses selected");
		Arrays.sort(courseIDs);
		StringBuilder buffer = new StringBuilder();

		for (String c : courseIDs) {
			buffer.append(",");
			buffer.append(c);
		}
		String combinedCourseIds = buffer.toString().substring(1);


		String section = request.getParameter("section");

		String courseName = request.getParameter("courseName");
		if (!courseNamePattern.matcher(courseName).matches())
			throw new ServletException("Invalid course name");

		Connection gradesConn = null;
		Connection conn = null;
		boolean transactionSuccess = false;
		try {
			conn = getConnection();

			Course course = Course.lookupByCourseIdsAndSemester(
					combinedCourseIds, term, conn);
			if (course != null)
				throw new ServletException("Already imported as "
						+ course.getCourseName());
			
			 RequestParser parser = new RequestParser(request,
	          getSubmitServerServletLog(), strictParameterChecking());
	      course = parser.getCourse();

	      
			course.setSemester(term);
			course.setSection(section);
			course.setCourseIDs(combinedCourseIds);

			response.setContentType("text/plain");

			PrintWriter out = response.getWriter();
			out.println("Created " + courseName);
			gradesConn = getGradesConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

			course.insert(conn);
			for (String c : courseIDs) {
				int courseID = Integer.parseInt(c);
				importStudents(out, term, course, courseID, false, gradesConn, conn);
			}
			conn.commit();

			userSession.addInstructorActionCapability(course.getCoursePK());

			transactionSuccess = true;

		} catch (SQLException e) {
			throw new ServletException(e);
		} finally {
			rollbackIfUnsuccessfulAndAlwaysReleaseConnection(
					transactionSuccess, request, conn);
			releaseGradesConnection(gradesConn);
		}

	}
	
	public static String getEffectiveFirstname(String firstname, String nickname) {
	    return firstname;
	}

	public static void importStudents(PrintWriter out, String term, Course course,
			int courseID, boolean updatePictures, Connection gradesConn, Connection conn) throws SQLException {
		String query = "SELECT lastName, firstName, nickname, uid, directoryID, email, classAccount, role, course, section, dropped, inactive"
				+ " FROM submitexport "
				+ " WHERE term = ?"
				+ " AND courseID = ?";
		out.printf("Importing students for %s from grade server course id %d%n",
				course.getCourseName(), courseID);
		PreparedStatement stmt = gradesConn.prepareStatement(query);
		stmt.setString(1, term);
		stmt.setInt(2, courseID);
		ResultSet rs = stmt.executeQuery();
		int count = 0;
		int activeCount = 0;
		int activeStudentCount = 0;

		studentLoop: while (rs.next()) {
			count++;
			int col = 1;
			String lastname = rs.getString(col++);
			String firstname = rs.getString(col++);
			String nickname = rs.getString(col++);
			firstname = getEffectiveFirstname(firstname, nickname);
			String campusUID = rs.getString(col++);
			String loginName = rs.getString(col++);
			String email = rs.getString(col++);
			String classAccount = rs.getString(col++);
			if (classAccount == null || classAccount.length() == 0)
				classAccount = loginName;
			String role = rs.getString(col++);
			String courseName = rs.getString(col++);
			String sectionName = rs.getString(col++);
			boolean dropped = rs.getBoolean(col++);
			boolean inactive = rs.getBoolean(col++);

			boolean active = !dropped && !inactive;
			if (active)
				activeCount++;

			Student s = Student.lookupByLoginNameAndCampusUID(loginName, campusUID,
					conn);
			boolean loginNameChanged = false;
			if (s == null) {
				s = Student.lookupByCampusUID(campusUID, conn);
				if (s != null) {
					out.printf("Changing login name from %s to %s%n", s.getLoginName(), loginName);
					loginNameChanged = true;
				}
			}

			if (dropped && s == null)
					continue;
			
		  try {
				s = Student.insertOrUpdateByUID(campusUID, firstname, lastname,
						loginName, email, conn);
				if (loginNameChanged)
				  out.printf("Updated record for %s: %s%n", s.getCampusUID(), s);
				} catch (SQLException e) {
					String msg = "Error trying to insert/update " + campusUID
						+ " " + loginName + ":" + e.getMessage();
					String msg2 = "For " + course.getCourseName() + " with courseID " + courseID;
					ServerError.insert(conn, ServerError.Kind.EXCEPTION, null, null, null, null, null, null, msg + "\n" + msg2, "", "", "", "", "", "", null, e);
					out.println(msg);
					out.println(msg2);
					e.printStackTrace(out);
					continue studentLoop;
				}

			StudentRegistration registration = StudentRegistration
					.lookupByStudentPKAndCoursePK(s.getStudentPK(),
							course.getCoursePK(), conn);

			boolean isNew = false;
			if (registration == null) {
				if (dropped || inactive)
					continue;
				out.printf("Registering %s %s%n", firstname, lastname);
				registration = new StudentRegistration();
				registration.setStudentPK(s.getStudentPK());
			  isNew = true;
			} else if (dropped) {
				if (registration.isDropped()) {
					if (registration.delete(conn)) {
					  out.printf("  Previously noted as dropped, deleting %s%n", s.getFullname());
					} else {
					  int submissions = Submission.countSubmissions(registration, conn);
					  int reviews = CodeReviewer.countActiveReviews(registration, conn);
					  
            if (reviews == 0)
              out.printf("  %s has %d submissions, leaving as dropped (not deleting)%n", s.getFullname(), submissions);
            else
              out.printf("  %s has %d submissions and %d code reviews, leaving as dropped (not deleting)%n",
                  s.getFullname(), submissions, reviews);
					}
				} else {
					out.printf("  marking %s as dropped%n", s.getFullname());
					registration.setDropped(true);
					registration.update(conn);
				}
				continue;
			} else if (inactive) {
				if (!registration.isInactive()) {
					out.printf("  marking %s as inactive%n", s.getFullname());
				}
			}
			
			assert !dropped;
			
			registration.setDropped(dropped);
			registration.setInactive(inactive);


			registration.setCoursePK(course.getCoursePK());
			registration.setClassAccount(classAccount);
			
			if ("Instructor".equals(role) || "TA".equals(role))
				registration
						.setInstructorCapability(StudentRegistration.MODIFY_CAPABILITY);
			else if ("Grader".equals(role))
				registration
						.setInstructorCapability(StudentRegistration.READ_ONLY_CAPABILITY);
			else {
				if (active && registration.getInstructorLevel() == StudentRegistration.STUDENT_CAPABILITY_LEVEL)
					activeStudentCount++;
			}
			registration.setFirstname(s.getFirstname());
			registration.setLastname(s.getLastname());
			registration.setCourse(courseName);
			registration.setSection(sectionName);
			registration.setCourseID(courseID);
			if (isNew)
				registration.insert(conn);
			else
				registration.update(conn);

		}
		out.printf("  got %d people, %d of them inactive, %d active students%n", count, (count- activeCount),
				activeStudentCount);

	}
}
