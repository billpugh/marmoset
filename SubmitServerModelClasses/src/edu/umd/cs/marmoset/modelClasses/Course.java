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
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.CheckForNull;
import javax.annotation.meta.TypeQualifier;

import com.google.common.base.Joiner;

import edu.umd.cs.marmoset.utilities.SqlUtilities;

/**
 * @author jspacco
 *
 */
public class Course {

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

    public static final String TABLE_NAME = "courses";

	public static String defaultSemester;

	public static void setDefaultSemester(String defaultSemester) {
	    if (defaultSemester == null)
	        return;
	    if (Course.defaultSemester != null
	            && !Course.defaultSemester.equals(defaultSemester))
	        throw new IllegalArgumentException("Bad attempt to change default semester from "
	                + Course.defaultSemester + " to " + defaultSemester);
	    Course.defaultSemester = defaultSemester;
	}

	private static SecureRandom random = new SecureRandom();

	private static String generateRandomKey() {
	    String result = Long.toHexString(random.nextLong());
	    while (result.length() < 16)
	        result = "0" + result;
	    return result;

	}
	/**
	 * List of all attributes for courses table.
	 */
     static final String[] ATTRIBUTE_NAME_LIST = {
            "course_pk",
            "semester",
            "coursename",
            "section",
            "description",
            "url",
            "course_ids",
            "allows_baseline_download",
            "buildserver_key",
            "submit_key",
            "browser_editing",
            "sections",
            "help_requests_allowed",
            "use_default_buildservers"
            
	};

	/**
	 * Fully-qualified attributes for courses table.
	 */
	 public static final String ATTRIBUTES = Queries.getAttributeList(TABLE_NAME,
			ATTRIBUTE_NAME_LIST);

	private @PK Integer coursePK; // non-NULL, autoincrement
	private String semester;
	private String courseName;
	private String section;
	private String description;
	private String url;
	private String courseIDs;
	private String buildserverKey;
	private String submitKey;
	private String sections;
	private BrowserEditing browserEditing = BrowserEditing.DISCOURAGED;
	private boolean allowsBaselineDownload;
	private boolean allowsHelpRequests;
	private boolean useDefaultBuildservers;

	public Course() {

	}


	public boolean getAllowsBaselineDownload() {
		return allowsBaselineDownload;
	}
	public void setAllowsBaselineDownload(boolean allowsBaselineDownload) {
		this.allowsBaselineDownload = allowsBaselineDownload;
	}
	/**
	 * @return Returns the courseName.
	 */
	public String getCourseName() {
		return courseName;
	}
	public StringBuilder getFullNameBuilder() {
	    StringBuilder b = new StringBuilder(courseName);
	    if (section != null && section.length() > 0) {
	        b.append("-");
	        b.append(section);
	    }
	    if (semester != null && semester.length() > 0
	            && !semester.equals(defaultSemester)) {
            b.append(", ");
            b.append(semester);
        }
	    return b;
	}
	public String getFullName() {
	    return getFullNameBuilder().toString();
    }
	public String getFullname() {
       return getFullName();
    }
	public String getFullDescription() {
        StringBuilder b = getFullNameBuilder();
        if (description != null && description.length() > 0) {
            b.append(": ");
            b.append(description);
        }
        return b.toString();
    }
	public String getBuildserverKey() {
	    return buildserverKey;
	}
	private void setBuildserverKey(String buildserverKey) {
        this.buildserverKey = buildserverKey;
    }
	public String getSubmitKey() {
        return submitKey;
    }
	public String getCourseKey() {
        return submitKey;
    }

    private void setSubmitKey(String submitKey) {
        this.submitKey = submitKey;
    }

    /**
	 * @param courseName The courseName to set.
	 */
	public void setCourseName(String courseName) {
		this.courseName = courseName;
	}
	/**
	 * @return Returns the coursePK.
	 */
	public @PK Integer getCoursePK() {
		return coursePK;
	}
	/**
	 * @param coursePK The coursePK to set.
	 */
	public void setCoursePK(@PK Integer coursePK) {
		this.coursePK = coursePK;
	}
	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description The description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return Returns the section.
	 */
	public String getSection() {
		return section;
	}
	/**
	 * @param section The section to set.
	 */
	public void setSection(String section) {
		this.section = section;
	}
	/**
	 * @return Returns the semester.
	 */
	public String getSemester() {
		return semester;
	}
	/**
	 * @param semester The semester to set.
	 */
	public void setSemester(String semester) {
		this.semester = semester;
	}
	/**
	 * @return Returns the url.
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * @param url The url to set.
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	public String getCourseIDs() {
		return courseIDs;
	}
	public void setCourseIDs(String courseIDs) {
		this.courseIDs = courseIDs;
	}
	public String[] getSections() {
        return (sections == null) ? null : sections.split(",");
    }

    public void setSections(String [] sections) {
        for(String s : sections) {
            if (s.indexOf('.') >= 0)
                throw new IllegalArgumentException(s);
            if (s.indexOf(' ') >= 0)
                throw new IllegalArgumentException(s);
        }
        this.sections = Joiner.on(",").join(sections);
    }

    public BrowserEditing getBrowserEditing() {
        return browserEditing;
    }


    public void setBrowserEditing(BrowserEditing browserEditing) {
        this.browserEditing = browserEditing;
    }


    public boolean isAllowsHelpRequests() {
        return allowsHelpRequests;
    }


    public void setAllowsHelpRequests(boolean allowsHelpRequests) {
        this.allowsHelpRequests = allowsHelpRequests;
    }


    public boolean isUseDefaultBuildservers() {
		return useDefaultBuildservers;
	}
	public void setUseDefaultBuildservers(boolean useDefaultBuildservers) {
		this.useDefaultBuildservers = useDefaultBuildservers;
	}
	public void insert(Connection conn)
	throws SQLException
	{
	    if (buildserverKey != null)
	        throw new IllegalStateException();
	    buildserverKey = generateRandomKey();
	    if (submitKey != null)
	        throw new IllegalStateException();
	    submitKey = generateRandomKey();

	    String insert = Queries.makeInsertStatementUsingSetSyntax(ATTRIBUTE_NAME_LIST, TABLE_NAME, true);

	    PreparedStatement stmt = null;
	    try {
	        stmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
	        putValues(stmt,1);
	        stmt.executeUpdate();
	        setCoursePK(asPK(Queries.getGeneratedPrimaryKey(stmt)));
	    } finally {
	        Queries.closeStatement(stmt);
	    }
	}
	/**
	 * @param stmt
	 * @throws SQLException
	 */
	private int putValues(PreparedStatement stmt, int col) throws SQLException {
		stmt.setString(col++, getSemester());
		stmt.setString(col++, getCourseName());
		stmt.setString(col++, getSection());
		stmt.setString(col++, getDescription());
		stmt.setString(col++, getUrl());
		stmt.setString(col++, getCourseIDs());
		stmt.setBoolean(col++, getAllowsBaselineDownload());
		stmt.setString(col++, getBuildserverKey());
		stmt.setString(col++, getSubmitKey());
		stmt.setString(col++, browserEditing.name().toLowerCase());
		stmt.setString(col++, sections);
		stmt.setBoolean(col++, isAllowsHelpRequests());
		stmt.setBoolean(col++, isUseDefaultBuildservers());
		return col;
	}

	public void update(Connection conn)
	throws SQLException
	{
	    String whereClause = " WHERE course_pk = ? ";

	    String update = Queries.makeUpdateStatementWithWhereClause(
	            ATTRIBUTE_NAME_LIST,
	            TABLE_NAME,
	            whereClause);

	    PreparedStatement stmt = null;
	    try {
	        stmt = conn.prepareStatement(update);
	        int col = putValues(stmt, 1);
	        SqlUtilities.setInteger(stmt, col, getCoursePK());

	        stmt.executeUpdate();
	    } finally {
	        Queries.closeStatement(stmt);
	    }
	}


	public int fetchValues(ResultSet resultSet, int startingFrom) throws SQLException
	{
		setCoursePK(asPK(resultSet.getInt(startingFrom++)));
		setSemester(resultSet.getString(startingFrom++));
		setCourseName(resultSet.getString(startingFrom++));
		setSection(resultSet.getString(startingFrom++));
		setDescription(resultSet.getString(startingFrom++));
		setUrl(resultSet.getString(startingFrom++));
		setCourseIDs(resultSet.getString(startingFrom++));
		setAllowsBaselineDownload(resultSet.getBoolean(startingFrom++));
		setBuildserverKey(resultSet.getString(startingFrom++));
		setSubmitKey(resultSet.getString(startingFrom++));
		setBrowserEditing(BrowserEditing.valueOfAnyCase(resultSet.getString(startingFrom++)));
		this.sections = resultSet.getString(startingFrom++);
		setAllowsHelpRequests(resultSet.getBoolean(startingFrom++));
		setUseDefaultBuildservers(resultSet.getBoolean(startingFrom++));
		return startingFrom;
	}

	@Override
	public String toString() {
		return String.format("Course %s", getDescription());
	}

	@Override
	public boolean equals(Object obj) {
	  if (!(obj instanceof Course)) {
	  	return false;
	  }
	  Course that = (Course) obj;
	  if (this.coursePK == null || that.coursePK == null) {
	  	return this.courseName.equals(that.courseName);
	  }
	  return this.coursePK.equals(that.coursePK);
	}

	@Override
	public int hashCode() {
	  int hash = 71;
	  hash += 31 * (coursePK == null ? 0 : coursePK);
	  hash += 31 * courseName.hashCode();
	  return hash;
	}

	/**
	 * Finds a course in the database based in the name of the course.
	 * @param courseName
	 * @param conn
	 * @return the Course object representing the row found in the DB; null if it can't be found
	 * @throws SQLException
	 */
	public static Collection<Course> lookupCourseByCourseNameSemester(String courseName,
			String semester,
			Connection conn) throws SQLException
	{
		String query = "SELECT " +ATTRIBUTES+ " " +
				"FROM courses " +
				"WHERE courses.coursename = ? " +
				"AND courses.semester = ? ";

		PreparedStatement stmt = null;

		stmt = conn.prepareStatement(query);
		stmt.setString(1, courseName);
		stmt.setString(2, semester);

		return getAllFromPreparedStatement(stmt);
	}

	   /**
     * Finds a cousre in the database based in the name of the course.
     * @param courseName
     * @param conn
     * @return the Course object representing the row found in the DB; null if it can't be found
     * @throws SQLException
     */
    public static Course lookupByCourseKey(String courseKey,
            Connection conn) throws SQLException
    {
        String query = "SELECT " +ATTRIBUTES+ " " +
                "FROM courses " +
                "WHERE courses.submit_key = ? " ;

        PreparedStatement stmt = null;

        stmt = conn.prepareStatement(query);
        stmt.setString(1, courseKey);

        return getCourseFromPreparedStatement(stmt);
    }
	/**
	 * Finds a course based on its course PK.
	 *
	 * @param coursePK the PK of the course
	 * @param conn the connection to the database
	 * @return the course if it's found.  If it's not found, SQLException will be
	 * thrown.  This method never returns null.
	 * @throws SQLException if an error occurrs or the course is not found
	 */
	public static Course getByCoursePK(@PK Integer coursePK, Connection conn)
	throws SQLException
	{
	    Course course = lookupByCoursePK(coursePK, conn);
	    if (course == null)
	        throw new SQLException("Course with PK " +coursePK+ " does not exist!");
	    return course;
	}

	/**
	 * Gets the course for which a particular project has been assigned.
	 *
	 * @param projectPK the PK of the project
	 * @param conn the connection to the database
	 * @return the course if it's found; if not SQLException is thrown.  Will never
	 * return null.
	 * @throws SQLException
	 */
	public static Course getByProjectPK(@Project.PK Integer projectPK, Connection conn)
	throws SQLException
	{
	    String query = " SELECT " +ATTRIBUTES+ " " +
	    " FROM courses, projects " +
	    " WHERE coures.course_pk = projects.project_pk " +
	    " AND projects.project_pk = ? ";

	    PreparedStatement stmt = conn.prepareStatement(query);
	    SqlUtilities.setInteger(stmt, 1, projectPK);

	    Course course = getCourseFromPreparedStatement(stmt);
	    if (course == null)
	        throw new SQLException("Cannot find course referenced by project with PK " +projectPK);
	    return course;
	}

	/**
	 * Looks up a a course based on the coursePK.
	 *
	 * @param coursePK the course PK
	 * @param conn the connection to the database
	 * @return the Course if the coursePK is found; null otherwise
	 * @throws SQLException
	 */
	public static Course lookupByCoursePK(@PK Integer coursePK, Connection conn)
	throws SQLException
	{
		String query = "SELECT " +ATTRIBUTES+ " " +
		"FROM courses " +
		"WHERE courses.course_pk = ?";

		PreparedStatement stmt = null;

		stmt = conn.prepareStatement(query);
		SqlUtilities.setInteger(stmt, 1, coursePK);

		return getCourseFromPreparedStatement(stmt);
	}

	private static Course getCourseFromPreparedStatement(PreparedStatement stmt)
	throws SQLException
	{
		try {
			ResultSet rs = stmt.executeQuery();

			if (rs.next())
			{
				Course course = new Course();
				course.fetchValues(rs, 1);
				return course;
			}
			return null;
		}
		finally {
			Queries.closeStatement(stmt);
		}
	}
    /**
     * Gets a course based on the studentRegistrationPK of someone registered for the course.
     * @param studentRegistrationPK
     * @param conn the connection to the database.
     * @return the Course if it is found; null if it doesn't exist.
     */
    public static Course lookupByStudentRegistrationPK(@StudentRegistration.PK Integer studentRegistrationPK, Connection conn)
    throws SQLException
    {
        String query =
            " SELECT " +ATTRIBUTES+
            " FROM courses, student_registration " +
            " WHERE student_registration.course_pk = courses.course_pk " +
            " AND student_registration.student_registration_pk = ? ";

        PreparedStatement stmt = conn.prepareStatement(query);
        SqlUtilities.setInteger(stmt, 1, studentRegistrationPK);
        return getCourseFromPreparedStatement(stmt);
    }
    /**
     * Gets a course from the databse by its courseName.
     * @param courseName The name of the course.
     * @param conn The connection to the database.
     * @return The course object; null if it can't be found.
     * @throws SQLException
     */
    public static Course lookupByCourseName(String courseName, Connection conn)
    throws SQLException
    {
        String query=
            " SELECT " +ATTRIBUTES+
            " FROM courses " +
            " WHERE coursename = ? ";
        PreparedStatement stmt=conn.prepareStatement(query);
        stmt.setString(1,courseName);
        return getCourseFromPreparedStatement(stmt);
    }

    public static List<Integer> lookupAllPKButByBuildserverKey(Connection conn, String excluded)
            throws SQLException
            {
               
                excluded = excluded.trim();
                String k[] = excluded.split("[ ,]");

                PreparedStatement stmt = queryAllButByBuildserverKey(conn, "course_pk", k);
                ArrayList<Integer> result = new ArrayList<Integer>();

                try {
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        result.add(rs.getInt(1));
                    }
                    return result;
                } finally {
                    Queries.closeStatement(stmt);
                }
            }

   
            private static PreparedStatement queryAllButByBuildserverKey(Connection conn, String attributes, String k[]) throws SQLException {
                PreparedStatement stmt = null;
                String query = " SELECT " + attributes + "  FROM courses" 
                + " WHERE use_default_buildservers = ?";
                for (int i = 0; i < k.length; i++) {
                    query += " AND buildserver_key != ? ";
                }

                stmt = conn.prepareStatement(query);
                stmt.setBoolean(1, true);
                for (int i = 0; i < k.length; i++) {
                    stmt.setString(i + 2, k[i]);
                }

                return stmt;
            }

    public static List<Integer> lookupAllPKByBuildserverKey(Connection conn, String keys)
    throws SQLException
    {
       
        keys = keys.trim();
        String k[] = keys.split("[ ,]");

        PreparedStatement stmt = queryByBuildserverKey(conn, "course_pk", k);
        ArrayList<Integer> result = new ArrayList<Integer>();

        try {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getInt(1));
            }
            return result;
        } finally {
            Queries.closeStatement(stmt);
        }
    }


    private static PreparedStatement queryByBuildserverKey(Connection conn, String attributes, String k[]) throws SQLException {
        PreparedStatement stmt = null;
        String query = " SELECT " + attributes + "  FROM courses WHERE ";
        for (int i = 0; i < k.length; i++) {
            if (i > 0)
                query += " OR ";
            query += " buildserver_key = ? ";
        }

        stmt = conn.prepareStatement(query);
        for (int i = 0; i < k.length; i++) {
            stmt.setString(i + 1, k[i]);
        }

        return stmt;
    }

    public static Map<String, Course> lookupAllButByBuildserverKey(Connection conn, String keys) throws SQLException {
        String k[] = keys.trim().split("[ ,]");
        PreparedStatement stmt = queryAllButByBuildserverKey(conn, ATTRIBUTES,  k);

        Map<String, Course> result = new TreeMap<String, Course>();

        try {

            for (Course c : getAllFromPreparedStatement(stmt)) {
                result.put(c.getBuildserverKey(), c);
            }
            return result;
        } finally {
            Queries.closeStatement(stmt);
        }
    }
    
    public static Map<String, Course> lookupAllByBuildserverKey(Connection conn, String keys) throws SQLException {
        String k[] = keys.trim().split("[ ,]");
        PreparedStatement stmt = queryByBuildserverKey(conn, ATTRIBUTES,  k);

        Map<String, Course> result = new TreeMap<String, Course>();

        for(String kk : k)
            result.put(kk, null);

        try {

            for (Course c : getAllFromPreparedStatement(stmt)) {
                result.put(c.getBuildserverKey(), c);
            }
            return result;
        } finally {
            Queries.closeStatement(stmt);
        }
    }

    public static List<Course> lookupAll(Connection conn)
    throws SQLException
    {
        String query =
            " SELECT " +ATTRIBUTES+
            " FROM courses " +
            " ORDER BY semester DESC, coursename ASC, section ASC";
        PreparedStatement stmt = null;
        try {
            stmt=conn.prepareStatement(query);
            return getAllFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
    }

    private static List<Course> getAllFromPreparedStatement(PreparedStatement stmt)
    throws SQLException
    {
        ResultSet rs = stmt.executeQuery();

        List<Course> list=new LinkedList<Course>();
        while (rs.next())
        {
            Course course = new Course();
            course.fetchValues(rs, 1);
            list.add(course);
        }
        return list;
    }

    public static List<Course> lookupAllByStudentPK(
    		@Student.PK Integer studentPK, Connection conn)
    throws SQLException
    {
        String query = " SELECT " +ATTRIBUTES+ " "+
        " FROM courses, student_registration "+
        " WHERE student_registration.student_pk = ? "+
        " AND student_registration.course_pk = courses.course_pk " +
        " ORDER BY courses.semester DESC, courses.coursename ASC, courses.section ASC";

        PreparedStatement stmt = null;
        try {
            stmt=conn.prepareStatement(query);
            SqlUtilities.setInteger(stmt, 1, studentPK);
            return getAllFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
    }


    public static @CheckForNull Course lookupByCourseIdsAndSemester(String courseIds, String semester, Connection conn)
    throws SQLException
    {
        String query =
            " SELECT " +Course.ATTRIBUTES+ " " +
            " FROM courses " +
            " WHERE semester = ? " +
            " AND course_ids = ? " ;

        PreparedStatement stmt = null;
        try {
            stmt=conn.prepareStatement(query);
            Queries.setStatement(stmt, semester, courseIds);
            return getCourseFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
    }

    public static List<Course> lookupallWithReadOnlyCapability(
    		@Student.PK Integer studentPK, Connection conn)
    throws SQLException
    {
        String query =
            " SELECT " +Course.ATTRIBUTES+ " " +
            " FROM courses, student_registration " +
            " WHERE student_registration.student_pk = ? " +
            " AND student_registration.course_pk = courses.course_pk " +
            " AND ( student_registration.instructor_capability = ? OR " +
            "       student_registration.instructor_capability = ?) " +
            " ORDER BY courses.semester DESC, courses.coursename ASC, courses.section ASC";

        PreparedStatement stmt = null;
        try {
            stmt=conn.prepareStatement(query);
            SqlUtilities.setInteger(stmt, 1, studentPK);
            stmt.setString(2, StudentRegistration.READ_ONLY_CAPABILITY);
            stmt.setString(3, StudentRegistration.MODIFY_CAPABILITY);
            return getAllFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
    }
}
