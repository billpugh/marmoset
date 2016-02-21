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

/**
 * Created on Oct 17, 2005
 *
 * @author jspacco
 */
package edu.umd.cs.submitServer;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration.Capability;

/**
 * StudentForUpload is a convenience class for parsing the fields required to
 * create/update Student and studentRegistration records from a variety of
 * sources, such as uploaded files or http requests.
 * <p>
 * TODO Note that there is a strong coupling between the Maryland-specific
 * implementations of the student and studentRegistration classes and this class
 * that will need to be dealt with before shipping this code someplace else.
 * class.
 *
 * @author jspacco
 */
public class StudentForUpload {
	public final String campusUID;

	public final String loginName;
	public final String firstname;
	public final String lastname;
	public final String classAccount;
	public final String email;
	public final @Nonnull String section;
	public final boolean inactive;
	public final boolean dropped;

	/**
	 * Create a studentForUpload by entering the values directly
	 *
	 * @param loginName
	 * @param campusUID
	 * @param firstname
	 * @param lastname
	 * @param classAccount
	 * @param section
	 * @param password
	 */
	public StudentForUpload(String loginName, String campusUID,
	    String email,
			String firstname, String lastname, String classAccount,
			String section, boolean inactive, boolean dropped) {
		this.loginName = loginName;
		this.campusUID = campusUID;
		this.email = email;
		this.firstname = firstname;
		this.lastname = lastname;
		this.classAccount = classAccount;
		if (section == null)
		    section = "";
		this.section = section;

		this.inactive = inactive;
		this.dropped = dropped;
	}




	// Last,First,CampusUID,Section,ClassAcct,loginName,Overall,Grade,
	// Smith,Jane,123456789,0101,cs101035,jsmith,0,,

	/**
	 * @param line
	 *            - the line to parse
	 * @param delimiter
	 *            - the delimiter between tokens
	 * @param genPassword
	 *            - if true, then generate a password
	 * @throws IllegalStateException
	 */
	public StudentForUpload(String line, String delimiter) throws IllegalStateException {
		String tokens[] = line.split(delimiter);
		// Last,First,UID,section,ClassAcct,DirectoryID
		// remove leading/trailing whitespace
		lastname = tokens[0].replaceAll("^\\s+", "").replaceAll("\\s+$", "");
		if (lastname.equals(""))
			throw new IllegalStateException("lastname CANNOT be empty!");
		// remove leading/trailing whitespace
		firstname = tokens[1].replaceAll("^\\s+", "").replaceAll("\\s+$", "");
		
		if (firstname.equals(""))
			throw new IllegalStateException("firstname CANNOT be empty!");

		campusUID = tokens[2].replaceAll("\\s+", "");
		if (!campusUID.matches("\\d+"))
			throw new IllegalStateException(
					campusUID
							+ " doesn't look like an campus UID "
							+ " in the campus UID field.  Are you sure that you're uploading a file "
							+ " for the SubmitServer and not for another service?");
		// TODO add a column to studentRegistration record for the section
		// number
		section = tokens[3];
		loginName = tokens[5].replaceAll("\\s+", "");
        if (loginName.equals(""))
            throw new IllegalStateException("Campus UID CANNOT be empty!");

        String classAccount = tokens[4].replaceAll("\\s+", "");

		if (classAccount.equals(""))
		    classAccount = loginName;
		this.classAccount  = classAccount;
		this.email = null;
		
		inactive = false;
		dropped = false;
	}

	public StudentForUpload(RequestParser parser) throws ClientRequestException {
		lastname = parser.getCheckedParameter("lastname");
		firstname = parser.getCheckedParameter("firstname");
		campusUID = parser.getCheckedParameter("campusUID");
		email = parser.getOptionalCheckedParameter("email");
		classAccount = parser.getOptionalCheckedParameter("classAccount");
		loginName = parser.getCheckedParameter("loginName");
		section = parser.getOptionalCheckedParameter("section","");


		this.inactive  = "on".equals(parser.getParameter("inactive"));
		this.dropped  = "on".equals(parser.getParameter("dropped"));
	}

	@Override
	public String toString() {
		return "loginName: " + loginName + ", campusUID: " + campusUID
				+ ", firstname: " + firstname + ", lastname: " + lastname
				+ ", classAccount: " + classAccount;
	}


	public static void registerStudent(Course course, StudentForUpload s,
			@Capability String capability, Connection conn) throws SQLException {
		Student student = s.lookupOrInsert(conn);
		registerStudent( course,  student, "", s.classAccount, capability, conn);
	}
	
	public static StudentRegistration registerStudent(Course course, Student student,
			String section,
			String classAccount, @CheckForNull @Capability String capability, Connection conn) throws SQLException {
		
		StudentRegistration registration = StudentRegistration
				.lookupByStudentPKAndCoursePK(student.getStudentPK(),
						course.getCoursePK(), conn);

		if (registration == null) {
			registration = new StudentRegistration();
			registration.setCoursePK(course.getCoursePK());
			registration.setClassAccount(classAccount);
			registration.setStudentPK(student.getStudentPK());
			registration.setInstructorCapability(capability);
			registration.setFirstname(student.getFirstname());
			registration.setLastname(student.getLastname());
			registration.setSection(section);
			registration.insert(conn);
		} 
		return registration;
	}
	
	/**
	 * @param s
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public  Student lookupOrInsert(Connection conn)
			throws SQLException {
		StudentForUpload s = this;
		Student student = Student.insertOrUpdateByLoginNameAndCampusUID(
				s.campusUID, s.firstname, s.lastname, s.loginName, s.email, conn);
		return student;
	}
}
