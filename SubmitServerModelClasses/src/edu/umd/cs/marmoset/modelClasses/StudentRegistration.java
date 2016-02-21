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

package edu.umd.cs.marmoset.modelClasses;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.marmoset.utilities.SqlUtilities;

/**
 * Object to represent a row of the student_registration table.
 * TODO Refactor getFromPreparedStatement().
 * @author daveho
 */
public class StudentRegistration implements Comparable<StudentRegistration> {

	@Documented
	@TypeQualifier(applicableTo = Integer.class)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface PK {}

	public static @PK int asPK( int pk) {
		return pk;
	}
	public static @PK Integer asPK( Integer pk) {
		return pk;
	}

    @Override
	public int hashCode() {
    		if (studentRegistrationPK != 0)
    			return studentRegistrationPK;
    		return getLastname().hashCode() + 31*getFirstname().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof StudentRegistration))
			return false;
		StudentRegistration other = (StudentRegistration) obj;
		if (studentRegistrationPK == 0) {
			if (other.studentRegistrationPK != 0)
				return false;
			return getLastname().equals(other.getLastname()) && getFirstname().equals(other.getFirstname());
		} else
			return studentRegistrationPK == other.studentRegistrationPK;
	}

	public static final Comparator<StudentRegistration> classAccountComparator = new Comparator<StudentRegistration> () {

        @Override
		public int compare(StudentRegistration s1, StudentRegistration s2) {

            int result = compareInstructorStatus(s1, s2);
            if (result != 0) return result;
            result = compareAccountNames(s1, s2);
            if (result != 0) return result;
            result = compareNames(s1, s2);
            return result;

        }

    };
    public static final Comparator<StudentRegistration> nameComparator = new Comparator<StudentRegistration> () {

        @Override
		public int compare(StudentRegistration s1, StudentRegistration s2) {

            int result = compareInstructorStatus(s1, s2);
            if (result != 0) return result;
            result = compareNames(s1, s2);
            if (result != 0) return result;
            result = compareAccountNames(s1, s2);
            return result;

        }

    };

    public static @CheckForNull Comparator<StudentRegistration> getComparator(String sortKey) {
        if (sortKey == null) return nameComparator;
        if ("account".equals(sortKey)) return classAccountComparator;
        if ("section".equals(sortKey))
            return sectionComparator;
        if ("name".equals(sortKey))  return nameComparator;
        return null;
    }
    
    public static final <T extends Comparable<T>> Comparator<StudentRegistration> getSubmissionViaMappedValuesComparator(final Map<Integer, T> values) {
        return  new Comparator<StudentRegistration>() {


        @Override
		public int compare(StudentRegistration s1, StudentRegistration s2) {

				T o1 = values.get(s1.getStudentRegistrationPK());
				T o2 = values.get(s2.getStudentRegistrationPK());

				int result = compareValues(o1, o2);
				if (result != 0)
					return result;
				return nameComparator.compare(s1, s2);
			}
		};

    };

    public static final Comparator<StudentRegistration> getSubmissionViaTimestampComparator(final Map<Integer, Submission> values) {
        return  new Comparator<StudentRegistration>() {


        @Override
		public int compare(StudentRegistration s1, StudentRegistration s2) {

            Submission o1 =  values.get(s1.getStudentRegistrationPK());
            Submission o2 =  values.get(s2.getStudentRegistrationPK());
            int result =  compareNulls(o1, o2);
            if (result != 0) return result;
            if (o1 == null) return nameComparator.compare(s1, s2);
            result =  - compareValues(o1.getSubmissionTimestamp(), o2.getSubmissionTimestamp());
            if (result != 0) return result;
            return nameComparator.compare(s1, s2);
        }
        };

    };
    
    public static final Comparator<StudentRegistration> sectionComparator =  new Comparator<StudentRegistration>() {

        @Override
        public int compare(StudentRegistration s1, StudentRegistration s2) {

            int result = compareSection(s1,s2);
            if (result != 0) return result;
            result = compareInstructorStatus(s1, s2);
            if (result != 0) return result;
            result = compareNames(s1, s2);
            if (result != 0) return result;
            result = compareAccountNames(s1, s2);
            return result;
        }
        };

    private static int compareInstructorStatus(StudentRegistration s1, StudentRegistration s2) {
        return -(s1.getInstructorLevel() - s2.getInstructorLevel());
    }
    private static int compareSection(StudentRegistration s1, StudentRegistration s2) {
        return nullSafeCompare(s1.getSection(), s2.getSection());
    }
    private static int compareNames(StudentRegistration s1, StudentRegistration s2) {
        int result = s1.getLastname().compareTo(s2.getLastname());
        if (result != 0) return result;
        return s1.getFirstname().compareTo(s2.getFirstname());
    }
    private static int compareAccountNames(StudentRegistration s1, StudentRegistration s2) {
        return s1.getClassAccount().compareTo(s2.getClassAccount());
    }
    private static <T extends Comparable<T>> 
    int nullSafeCompare(T t1, T t2) {
        if (t1== null) {
            if (t2 == null) return 0;
            return 1;
        }
        if (t2 == null) return -1;
        return t1.compareTo(t2);
            
    }
    
    @SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    public StudentRegistration() {
    	
    }
	public static final String TABLE_NAME = "student_registration";
    private @StudentRegistration.PK int studentRegistrationPK; // autoincrement
	private @Course.PK int coursePK; 
	private @Student.PK int studentPK; 
	private @Nonnull String classAccount;
	private @CheckForNull @Capability String instructorCapability;
    private @Nonnull String firstname;
    private @Nonnull  String lastname;
    private boolean inactive = false;
    private boolean dropped = false;
    private @Nonnull String course;
    private @Nonnull String section;
    private int courseID;

    @Override
    public String toString() {
        return studentRegistrationPK + " : " + getFullname();
    }

	/**
	 * List of all attributes of student_registration table.
	 */
	  static final String[] ATTRIBUTE_NAME_LIST = {
		"student_registration_pk",
		"course_pk",
		"student_pk",
		"class_account",
        "instructor_capability",
        "firstname",
        "lastname",
        "inactive",
        "dropped",
        "course",
        "section",
        "courseID"
	};

	/**
	 * Fully-qualified attributes for student_registration table.
	 */
	public static final String ATTRIBUTES =
	    Queries.getAttributeList(TABLE_NAME, ATTRIBUTE_NAME_LIST);

	 @Documented
	    @TypeQualifier(applicableTo = String.class)
	    @Retention(RetentionPolicy.RUNTIME)
	    public @interface Capability {}

	public static @Capability String asCapability(String c) {
		return c;
	}

	public static @Capability final String PSEUDO_STUDENT_CAPABILITY = "pseudo-student";
	public static @Capability final String READ_ONLY_CAPABILITY = "read-only";
    public static @Capability final String MODIFY_CAPABILITY = "modify";

    @Documented
    @TypeQualifier(applicableTo = Integer.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CapabilityLevel {}

    public static @CapabilityLevel final int READ_ONLY_CAPABILITY_LEVEL = 1;
    public static @CapabilityLevel  final int MODIFY_CAPABILITY_LEVEL = 2;
    public static @CapabilityLevel  final int STUDENT_CAPABILITY_LEVEL = 0;
    public static final String ADD_PERMISSION = "add";
    public static final String REMOVE_PERMISSION = "remove";

	/**
	 * @return Returns the coursePK.
	 */
	public @Course.PK int getCoursePK() {
		return coursePK;
	}
	/**
	 * @param coursePK The coursePK to set.
	 */
	public void setCoursePK(@Course.PK int coursePK) {
		this.coursePK = coursePK;
	}
	/**
	 * @return Returns the classAccount.
	 */
	public String getClassAccount() {
	    if (Student.FAKE_NAMES)
	        return FakeNames.getAccount(studentPK);
		return classAccount;
	}
	/**
	 * @param classAccount The classAccount to set.
	 */
	public void setClassAccount(String classAccount) {
	    if (Student.FAKE_NAMES)
	        return;
		this.classAccount = classAccount;
	}
    public String getSection() {
        if (section == null || section.length() == 0)
            return "";
        if (course != null && course.length() > 0)
            return course + "-" + section;
		return section;
	}

	public void setSection(String section) {
	    if (section == null)
	        this.section = "";
	    else
	        this.section = section;
	}

	public String getCourse() {
		return course;
	}

	public void setCourse(String course) {
	    if (course == null)
	        this.course = "";
	    else 
	        this.course = course;
	}

	public int getCourseID() {
		return courseID;
	}

	public void setCourseID(int courseID) {
		this.courseID = courseID;
	}

	/**
     * @return Returns the instructorCapability.
     */
    public @CheckForNull @Capability String getInstructorCapability()
    {
        return instructorCapability;
    }
    /**
   
   /** Has either modify or read_only instructor capability */
    public boolean isInstructor() {
    		return getInstructorLevel() > 0;
    }
    public boolean isPseudoStudent() {
        return PSEUDO_STUDENT_CAPABILITY.equals(instructorCapability);
}
    public boolean isNormalStudent() {
    		return instructorCapability == null && isActive();
    }
    
    public boolean isInactiveStudent() {
        return instructorCapability == null && !isActive();
}
    
    /** Has either modify or read_only instructor capability */
    public boolean isInstructorModifiy() {
    		return getInstructorLevel() >= MODIFY_CAPABILITY_LEVEL;
    }

     /** Instructor level is defined at:<br>
     * 2: modify capability (can create/edit projects, and make other modifications backed
     * by a write to the database.<br>
     * 1: read-only capability (can access read-only instructor capabilities)
     * 0: no instructor privileges, can only access student resources.
     * @return Returns the instructorLevel.
     */

    public @CapabilityLevel int getInstructorLevel()
    {
    	if (MODIFY_CAPABILITY.equals(instructorCapability)) return MODIFY_CAPABILITY_LEVEL;
        if (READ_ONLY_CAPABILITY.equals(instructorCapability)) return READ_ONLY_CAPABILITY_LEVEL;
        return STUDENT_CAPABILITY_LEVEL;
    }



    /**
     * @param instructorCapability The instructorCapability to set.
     */
    public void setInstructorCapability(@CheckForNull @Capability String instructorCapability)
    {
        this.instructorCapability = instructorCapability;
    }
	/**
	 * @return Returns the studentPK.
	 */
	public @Student.PK int getStudentPK() {
		return studentPK;
	}
	/**
	 * @param studentPK The studentPK to set.
	 */
	public void setStudentPK(@Student.PK int studentPK) {
		this.studentPK = studentPK;
	}
	/**
	 * @return Returns the studentRegistrationPK.
	 */
	public @StudentRegistration.PK  Integer getStudentRegistrationPK() {
		return studentRegistrationPK;
	}
	/**
	 * @param studentRegistrationPK The studentRegistrationPK to set.
	 */
	public void setStudentRegistrationPK(@StudentRegistration.PK  Integer studentRegistrationPK) {
		this.studentRegistrationPK = studentRegistrationPK;
	}

    /**
     * @return Returns the firstname.
     */
    public String getFirstname() {
    	    if (Student.FAKE_NAMES)
			return FakeNames.getFirstname(studentPK);
        return firstname;
    }
    /**
     * @param firstname The firstname to set.
     */
    public void setFirstname(String firstname) {
     	if (Student.FAKE_NAMES)
     		return;
        this.firstname = firstname;
    }
    /**
     * @return Returns the lastname.
     */
    public String getLastname() {
    	if (Student.FAKE_NAMES)
			return FakeNames.getLastname(studentPK);
        
        return lastname;
    }
    /**
     * @param lastname The lastname to set.
     */
    public void setLastname(String lastname) {
    	if (Student.FAKE_NAMES)
     		return;
        this.lastname = lastname;
    }


    public String getFullname() {
        if (Student.FAKE_NAMES)
            return FakeNames.getFullname(studentPK);
        String name = firstname + " " + lastname;
        if (classAccount != null && classAccount.length() > 0)
            name += " (" + classAccount + ")";
        return name;
    }

	public boolean isActive() {
		return !inactive && !dropped;
	}

	public boolean isInactive() {
		return inactive;
	}
	
	public void setInactive(boolean inactive) {
		this.inactive = inactive;
	}
	public void setDropped(boolean dropped) {
		this.dropped = dropped;
	}

	public boolean isDropped() {
		return dropped;
	}

	public int fetchValues(ResultSet resultSet, int startingFrom) throws SQLException {
		setStudentRegistrationPK(asPK(resultSet.getInt(startingFrom++)));
		setCoursePK(Course.asPK(resultSet.getInt(startingFrom++)));
		setStudentPK(Student.asPK(resultSet.getInt(startingFrom++)));
		setClassAccount(resultSet.getString(startingFrom++));
		setInstructorCapability(asCapability(resultSet.getString(startingFrom++)));
        setFirstname(resultSet.getString(startingFrom++));
        setLastname(resultSet.getString(startingFrom++));
        inactive = resultSet.getBoolean(startingFrom++);
        dropped = resultSet.getBoolean(startingFrom++);
        setCourse(resultSet.getString(startingFrom++));
        setSection(resultSet.getString(startingFrom++));
        setCourseID(resultSet.getInt(startingFrom++));
		return startingFrom;
	}

	/**
	 * Insert a new row into the student_registration database using
	 * the provided connection.
	 *
	 * THIS DOES NOT CHECK FOR DUPLICATES.
	 *
	 * @param conn
	 * @throws SQLException
	 */
	public void insert(Connection conn)
	throws SQLException
	{
	    String query = Queries.makeInsertStatementUsingSetSyntax(ATTRIBUTE_NAME_LIST, TABLE_NAME, true);


	    PreparedStatement stmt = null;
	    try {
	        stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
	        putValues(stmt, 1);
	        stmt.executeUpdate();
	        setStudentRegistrationPK(asPK(Queries.getGeneratedPrimaryKey(stmt)));
	    } finally {
	        Queries.closeStatement(stmt);
	    }
	}

	/**
	 * Update a StudentRegistration record in the database.
	 * @param conn The connection to the database.
	 * @throws SQLException
	 */
	public void update(Connection conn)
	throws SQLException
	{
	    String update = Queries.makeUpdateStatementWithWhereClause(
	            ATTRIBUTE_NAME_LIST,
	            TABLE_NAME,
	            " WHERE student_registration_pk = ? ");

	    PreparedStatement stmt = null;
	    try {
	        stmt = conn.prepareStatement(update);

	        int index = putValues(stmt, 1);
	        SqlUtilities.setInteger(stmt, index, getStudentRegistrationPK());

	        stmt.executeUpdate();

	    } finally {
	        Queries.closeStatement(stmt);
	    }
	}

	/**
	 * Puts the fields of this object (but not the first field, which is the primary key)
	 * into a prepared statement starting at a given index.
	 * @param stmt the preparedStatement
	 * @param index the index to start at
	 * @return the index of the next open slot in the prepared statement
	 * @throws SQLException
	 */
	public int putValues(PreparedStatement stmt, int index)
	throws SQLException
	{
	    stmt.setInt(index++, coursePK);
	    stmt.setInt(index++, studentPK);
	    stmt.setString(index++, classAccount);
	    stmt.setString(index++, instructorCapability);
	    stmt.setString(index++, firstname);
	    stmt.setString(index++, lastname);
	    stmt.setBoolean(index++, inactive);
	    stmt.setBoolean(index++, dropped);
	    stmt.setString(index++, course);
	    stmt.setString(index++, section);
	    stmt.setInt(index++, courseID);
	    return index;
	}



	/**
	 * Looks up a student registration row based on the cvs account and course PK.
	 *
	 * @param classAccount the cvs account
	 * @param coursePK the PK of the course
	 * @param conn the connection to the database
	 * @return the StudentRegistration object if it's found; null otherwise
	 * @throws SQLException
	 */
	public static StudentRegistration lookupByCvsAccountAndCoursePK(String classAccount, Integer coursePK, Connection conn)
	throws SQLException
	{
	    String query = queryString("") +
        " AND student_registration.class_account = ? "+
	    " AND student_registration.course_pk = ?";

	    PreparedStatement stmt = conn.prepareStatement(query);
	    stmt.setString(1, classAccount);
	    SqlUtilities.setInteger(stmt, 2, coursePK);

	    return getFromPreparedStatement(stmt);
	}

	public Student getCorrespondingStudent(Connection conn)
	throws SQLException
	{
		Student student = Student.lookupByStudentPK(getStudentPK(), conn);
		if (student==null) {
        	throw new SQLException("Database is corrupted; studentRegistrationPK " +
        			getStudentPK()+ " exists, but I can't find " +
        			"corresponding student record with studentPK = "+getStudentPK());
        }
		return student;
	}


	public static List<StudentRegistration> lookupAllByCoursePKAndSection(int coursePK, String section, Connection conn)
	        throws SQLException
	        {
	            String query =
	                " SELECT " +ATTRIBUTES+
	                " FROM student_registration " +
	                " WHERE course_pk = ? ";

	            PreparedStatement stmt = null;
	            try {
	                stmt = conn.prepareStatement(query);
	                SqlUtilities.setInteger(stmt, 1, coursePK);
	                ResultSet rs = stmt.executeQuery();
                    
                    List<StudentRegistration> collection = new LinkedList<StudentRegistration>();
                    while (rs.next())
                    {
                        StudentRegistration registration = new StudentRegistration();
                        registration.fetchValues(rs, 1);
                        if (section.equals(registration.getSection()))
                          collection.add(registration);
                    }
                    return collection;
	            } finally {
	                Queries.closeStatement(stmt);
	            }
	        }

	public static List<StudentRegistration> lookupAllByCoursePK(int coursePK, Connection conn)
	throws SQLException
	{
	    String query =
	        " SELECT " +ATTRIBUTES+
	        " FROM student_registration " +
	        " WHERE course_pk = ? ";

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(query);
            SqlUtilities.setInteger(stmt, 1, coursePK);
            return getAllFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
	}

	public static List<StudentRegistration> lookupAllByCourseAndCapability(Course course, 
			@Capability String capability,
			Connection conn)
	throws SQLException
	{
	    String query =
	        " SELECT " +ATTRIBUTES+
	        " FROM student_registration " +
	        " WHERE course_pk = ?  and instructor_capability = ?";

        PreparedStatement stmt = null;
        try {
            stmt = Queries.setStatement( conn, query, course.getCoursePK(), capability);
            return getAllFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
	}
	/**
	 * Get a list of all the team registrations that are in a given course. <br>
	 * Could be empty if no teams are registered
	 */
	public static List<StudentRegistration> lookupAllTeamsByCoursePK(Integer coursePK, Connection conn)
	throws SQLException
	{
	    String query =
	        " SELECT " +ATTRIBUTES+
	        " FROM student_registration,students " +
	        " WHERE student_registration.student_pk = students.student_pk "   +
	        " AND students.account_type = '"  + Student.TEAM_ACCOUNT + "' " +
	        " AND course_pk = ? ";

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(query);
            SqlUtilities.setInteger(stmt, 1, coursePK);
            return getAllFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
	}

	/**
	 * Fetches all studentRegistration records that have at least one submission
	 * for the project with the given projectPK.
	 * @param projectPK Primary Key of the project.
	 * @param conn Connection to the database.
	 * @return A list of studentRegistration records that have at least one submission
	 * 		for the given project.
	 * @throws SQLException
	 */
	public static List<StudentRegistration> lookupAllWithAtLeastOneSubmissionByProjectPK(
			Integer projectPK,
			Connection conn)
	throws SQLException
	{
	    String query =
	        " SELECT " +ATTRIBUTES+
	        " FROM student_registration, student_submit_status " +
            " WHERE student_registration.student_registration_pk = student_submit_status.student_registration_pk " +
            " AND student_submit_status.project_pk = ? " +
            " AND student_submit_status.number_submissions > 0";

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(query);
            SqlUtilities.setInteger(stmt, 1, projectPK);
            return getAllFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
	}

	public static List<StudentRegistration> lookupAllInstructorsByCoursePK(Integer coursePK, Connection conn)
	throws SQLException
	{
	    String query =
	        " SELECT " +ATTRIBUTES+
	        " FROM student_registration " +
	        " WHERE course_pk = ? " +
	        " AND instructor_capability IS NOT NULL" +
	        " AND instructor_capability != '" + PSEUDO_STUDENT_CAPABILITY + "'";

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(query);
            SqlUtilities.setInteger(stmt, 1, coursePK);
            return getAllFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
	}
	// [NAT]

	public static List<StudentRegistration> lookupAllByStudentPK(
			@Student.PK Integer studentPK, Connection conn)
	throws SQLException
	{
	    String query =
	        " SELECT " +ATTRIBUTES+
	        " FROM student_registration " +
	        " WHERE student_pk = ? ";

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(query);
            SqlUtilities.setInteger(stmt, 1, studentPK);
            return getAllFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
	}

	private static List<StudentRegistration> getAllFromPreparedStatement(PreparedStatement stmt)
    throws SQLException
    {
	    ResultSet rs = stmt.executeQuery();

	    List<StudentRegistration> collection = new LinkedList<StudentRegistration>();
	    while (rs.next())
	    {
	        StudentRegistration registration = new StudentRegistration();
	        registration.fetchValues(rs, 1);
	        collection.add(registration);
	    }
	    return collection;
    }

	public static StudentRegistration lookupBySubmissionPK(
			 @Submission.PK int submissionPK, Connection conn)
	throws SQLException
	{
	    String query = queryString(", " + Submission.TABLE_NAME) +
	        " AND student_registration.student_registration_pk = submissions.student_registration_pk " +
	        " AND submissions.submission_pk = ? ";

	    PreparedStatement stmt = conn.prepareStatement(query);
	    SqlUtilities.setInteger(stmt, 1, submissionPK);

	    return getFromPreparedStatement(stmt);
	}

	public static StudentRegistration lookupByStudentRegistrationPK(Integer studentRegistrationPK, Connection conn)
	throws SQLException
	{
	    String query = queryString("") + " AND student_registration_pk = ? ";

	    PreparedStatement stmt = conn.prepareStatement(query);
	    SqlUtilities.setInteger(stmt, 1, studentRegistrationPK);

	    return getFromPreparedStatement(stmt);
	}

	public boolean delete(Connection conn) throws SQLException {
		if (!dropped) {
			// Only remove someone the second time we synchronize
			dropped = true;
			this.update(conn);
			return false;
		}
		if (Submission.countSubmissions(this, conn) > 0 
				|| CodeReviewer.countActiveReviews(this, conn) > 0) {
			return false;
		}

		String delete = "DELETE FROM " + TABLE_NAME + " WHERE student_registration_pk = ?";
		System.out.println("Deleting " + getFullname() + " from " + course);
		PreparedStatement stmt = Queries.setStatement(conn, delete, studentRegistrationPK);
		stmt.executeUpdate();
		return true;
	}

	/**
	 * Looks up a student Registration based on the studentPK and the name of the course.
	 *
	 * @param studentPK the PK of the student
	 * @param courseName the name of the course
	 * @param conn the connection to the database
	 * @return the StudentRegistration object if it's found; null otherwise
	 * @throws SQLException
	 */
	public static StudentRegistration lookupByStudentPKAndCourseName(
			@Student.PK Integer studentPK, String courseName, Connection conn)
		throws SQLException
	{
        String query = queryString(", courses") +
        " AND student_registration.course_pk = courses.course_pk  "+
		" AND student_registration.student_pk = ? "+
		" AND courses.coursename= ?";

		PreparedStatement stmt = conn.prepareStatement(query);
		SqlUtilities.setInteger(stmt, 1, studentPK);
		stmt.setString(2, courseName);

		return getFromPreparedStatement(stmt);
	}

    static String queryString(String additionalTables) {
        return   "SELECT " +ATTRIBUTES+ "," + Student.TABLE_NAME + ".firstname, " + Student.TABLE_NAME + ".lastname" + " "+
        "FROM "+
        TABLE_NAME+", "+Student.TABLE_NAME + additionalTables +
        " WHERE " +
            TABLE_NAME + ".student_pk = " + Student.TABLE_NAME + ".student_pk ";

    }
	/**
	 * Finds a student registration based on the student's PK and the course's PK.
	 *
	 * @param studentPK the PK of the student
	 * @param coursePK the PK of the course
	 * @param conn the connection to the database
	 * @return the StudentRegistration object if it's found; null otherwise
	 * @throws SQLException
	 */
	public static StudentRegistration lookupByStudentPKAndCoursePK(
			@Student.PK Integer studentPK,
			Integer coursePK,
			Connection conn)
		throws SQLException
	{
		String query = queryString("") +
		" AND student_registration.student_pk = ? "+
		" AND student_registration.course_pk = ?";

		PreparedStatement stmt = conn.prepareStatement(query);
		SqlUtilities.setInteger(stmt, 1, studentPK);
        SqlUtilities.setInteger(stmt, 2, coursePK);

		//Debug.print("lookupStudentRegistration...()" + stmt.toString());
		return getFromPreparedStatement(stmt);
	}

	public static StudentRegistration lookupByCvsAccountAndProjectPKAndOneTimePassword(
        String classAccount,
        String oneTimePassword,
        Integer projectPK,
        Connection conn)
    throws SQLException
    {
    	String query = queryString(",student_submit_status") +
        " AND student_registration.class_account = ? " +
        " AND student_submit_status.one_time_password = ? " +
        " AND student_registration.student_registration_pk = student_submit_status.student_registration_pk " +
        " AND student_submit_status.project_pk = ? ";

    	PreparedStatement stmt = conn.prepareStatement(query);

    	stmt.setString(1, classAccount);
    	stmt.setString(2, oneTimePassword);
    	SqlUtilities.setInteger(stmt, 3, projectPK);

    	return getFromPreparedStatement(stmt);
    }

	/**
	 * Private helper method that executes a prepared statement, reads the results
	 * into a StudentRegistration object and then returns it.  Returns null if it can't
	 * find the StudentRegistration.  Closes the prepared statement if it's not null.
	 *
	 * @param stmt the prepared statement
	 * @return the StudentRegistration object if it's found; null otherwise
	 * @throws SQLException
	 */
	private static StudentRegistration getFromPreparedStatement(PreparedStatement stmt)
	throws SQLException
	{
	    try {
	        ResultSet rs = stmt.executeQuery();

			if (rs.next())
			{
				StudentRegistration studentRegistration = new StudentRegistration();
				studentRegistration.fetchValues(rs, 1);
				return studentRegistration;
			}
			return null;
		} finally {
			Queries.closeStatement(stmt);
	    }
	}
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
	public int compareTo(StudentRegistration that) {
        return this.getClassAccount().compareTo((that).getClassAccount());
    }
    /**
     * @param o1
     * @param o2
     * @return
     */
    static <T extends Comparable<? super T>> int compareValues(T o1, T  o2) {
        int result = compareNulls(o1, o2);
        if (result != 0) return result;
        if (o1 == o2) return 0;
        return o1.compareTo(o2);

    }
    static int compareNulls(Object o1, Object o2) {
        if (o1 == null && o2 == null) return 0;
        if (o1 == null) return 1;
        if (o2 == null) return -1;
        return 0;

    }

    public static Map<Integer, String> lookupStudentRegistrationMapByProjectPK(
        Integer projectPK,
        Connection conn)
    throws SQLException
    {
        String query=
            " SELECT " +ATTRIBUTES+
            " FROM student_registration, projects " +
            " WHERE projects.project_pk = ? " +
            " AND student_registration.course_pk = projects.course_pk ";
        Map<Integer,String> map=new HashMap<Integer,String>();
        PreparedStatement stmt=null;
        try {
            stmt=conn.prepareStatement(query);
            SqlUtilities.setInteger(stmt, 1, projectPK);
            ResultSet rs=stmt.executeQuery();
            while (rs.next()) {
                StudentRegistration registration=new StudentRegistration();
                registration.fetchValues(rs, 1);
                map.put(registration.getStudentRegistrationPK(), registration.getClassAccount());
            }
            return map;
        } finally {
            Queries.closeStatement(stmt);
        }
    }
}
