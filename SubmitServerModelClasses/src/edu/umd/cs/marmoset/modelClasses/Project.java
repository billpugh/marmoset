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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifier;

import org.apache.commons.io.CopyUtils;
import org.apache.log4j.Logger;

import edu.umd.cs.marmoset.modelClasses.Archive.UploadResult;
import edu.umd.cs.marmoset.modelClasses.Submission.BuildStatus;
import edu.umd.cs.marmoset.utilities.DisplayProperties;
import edu.umd.cs.marmoset.utilities.EditDistance;
import edu.umd.cs.marmoset.utilities.SqlUtilities;
import edu.umd.cs.marmoset.utilities.TextUtilities;
import edu.umd.cs.submitServer.policy.ChooseLastSubmissionPolicy;

/**
 * Object to represent a row in the projects table.
 * @author daveho
 * @author jspacco
 */
public class Project implements Serializable, Cloneable {
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


    public static final String TABLE_NAME = "projects";
	private static final String PROJECT_STARTER_FILE_ARCHIVES = "submission_archives";
    private @Project.PK int projectPK; //  autoincrement
	private @Course.PK int coursePK;
	private int testSetupPK = 0; 
	private @Project.PK int diffAgainst; // 0 to diff against canonical submission (if any), otherwise project to diff against
	private String projectNumber;
	private Timestamp ontime;
	private Timestamp late;
	private String title;
	private String url;
	private String description;
	private int releaseTokens;
	private int regenerationTime;
	private boolean isTested;
	private boolean isPair;
	private boolean visibleToStudents;
	private String postDeadlineOutcomeVisibility=POST_DEADLINE_OUTCOME_VISIBILITY_NOTHING;
	private String kindOfLatePenalty;
	private double lateMultiplier;
	private int lateConstant;
	private @StudentRegistration.PK int canonicalStudentRegistrationPK;
    private String bestSubmissionPolicy;
    private String releasePolicy;
    private String stackTracePolicy;
    private int numReleaseTestsRevealed;
    private @CheckForNull Integer archivePK; 
    private BrowserEditing browserEditing = BrowserEditing.DISCOURAGED;
    

    private transient byte[] cachedArchive;
    private transient Map<String, byte[]>  cachedContents;
    
    private static final long serialVersionUID = 1;
    private static final int serialMinorVersion = 1;

	public static final String ACCEPTED = "accepted";
	public static final String NEW = "new";
	public static final String CONSTANT = "constant";
	public static final String MULTIPLIER = "multiplier";

	public static final String JAVA = "java";
	public static final String OTHER = "other";

	public static final String POST_DEADLINE_OUTCOME_VISIBILITY_NOTHING="nothing";
	public static final String POST_DEADLINE_OUTCOME_VISIBILITY_EVERYTHING="everything";

    public static final String AFTER_PUBLIC="after_public";
    public static final String ANYTIME="anytime";
    public static final String TEST_NAME_ONLY="test_name_only";
    public static final String EXCEPTION_LOCATION="exception_location";
    public static final String RESTRICTED_EXCEPTION_LOCATION="restricted_exception_location";
    public static final String FULL_STACK_TRACE="full_stack_trace";

    public static final int UNLIMITED_RELEASE_TESTS=-1;


    static Logger getLogger() {
    		return Logger.getLogger(Project.class.getName());
    }
	/**
	 * List of all attributes of projects table.
	 */
	 final static String[] ATTRIBUTE_NAME_LIST = {
		"project_pk",
		"course_pk",
		"test_setup_pk",
		"diff_against",
		"project_number",
		"ontime",
		"late",
		"title",
		"URL",
		"description",
		"release_tokens",
		"regeneration_time",
		"is_tested",
		"is_pair",
		"visible_to_students",
		"post_deadline_outcome_visibility",
		"kind_of_late_penalty",
		"late_multiplier",
		"late_constant",
		"canonical_student_registration_pk",
        "best_submission_policy",
        "release_policy",
        "stack_trace_policy",
        "num_release_tests_revealed",
        "archive_pk",
        "browser_editing"
	};

	/**
	 * Fully-qualified attributes for projects table.
	 */
	 public static final String ATTRIBUTES =
		Queries.getAttributeList(TABLE_NAME, ATTRIBUTE_NAME_LIST);

	/**
	 * Constructor.  All fields will have default values.
	 */
	public Project() {
	}

	@Override
	public int hashCode() {
		return projectPK;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Project))
			return false;
		Project other = (Project) obj;
		return this.projectPK == other.projectPK;
	}

	/**
	 * Is this project configured for testing?
	 *
	 * @return true if the project is configured for testing, false otherwise
	 */
	public boolean isTested()
	{
	    return isTested;
	}

	/**
	 * Set whethet this project configured for testing
	 *
	 * @param isTested true if the project is configured for testing, false otherwise
	 */
	public void setIsTested(boolean isTested)
	{
	     this.isTested = isTested;
	}

	public boolean isAfterLateDeadline() {
	    return late.before(new Date());
	}

	public boolean isPair() {
		return isPair;
	}

	public void setPair(boolean isPair) {
		this.isPair = isPair;
	}

	/**
	 * @return Returns the regenerationTime.
	 */
	public int getRegenerationTime() {
		return regenerationTime;
	}
	/**
	 * @param regenerationTime The regenerationTime to set.
	 */
	public void setRegenerationTime(int regenerationTime) {
		this.regenerationTime = regenerationTime;
	}
	/**
	 * @return Returns the tokens.
	 */
	public int getReleaseTokens() {
		return releaseTokens;
	}
	/**
	 * @param tokens The tokens to set.
	 */
	public void setReleaseTokens(int tokens) {
		this.releaseTokens = tokens;
	}
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
	 * @return Returns the description.
	 */
	public @CheckForNull String getDescription() {
		return description;
	}
	/**
	 * @return Returns the description.
	 */
	public String getNonnullDescription() {
		if (description != null)
			return description;
		return "Project " + getProjectNumber();
	}
	
	public String getFullTitle() {
	    if (title == null)
	        return getProjectNumber();
	    return  getProjectNumber() + ": " + title;
	}
	/**
	 * @param description The description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return Returns the late.
	 */
	public Timestamp getLate() {
		return late;
	}
	/**
	 * @return The late deadline in utc millis.
	 */
	public long getLateMillis() {
		return late.getTime();
	}
	/**
	 * @param late The late to set.
	 */
	public void setLate(Timestamp late) {
		this.late = late;
	}
	/**
	 * @return Returns the projectNumber.
	 */
	public String getProjectNumber() {
		return projectNumber;
	}
	/**
	 * @param projectNumber The projectNumber to set.
	 */
	public void setProjectNumber(String projectNumber) {
		this.projectNumber = projectNumber;
	}

	/**
	 * @return Returns the projectPK.
	 */
	public @PK int getProjectPK() {
		return projectPK;
	}
	/**
	 * @param projectPK The projectPK to set.
	 */
	public void setProjectPK(@PK int projectPK) {
		this.projectPK = projectPK;
	}
	public @PK int getDiffAgainst() {
        return diffAgainst;
    }
    public void setDiffAgainst(@PK Integer diffAgainst) {
        if (diffAgainst == null)
            this.diffAgainst = 0;
        else
            this.diffAgainst = diffAgainst;
    }
    /**
	 * @return Returns the title.
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title The title to set.
	 */
	public void setTitle(String title) {
		this.title = title;
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

	/**
	 * @return Returns the initialBuildStatus.
	 */
	public Submission.BuildStatus getInitialBuildStatus() {
		if (isTested)
			return BuildStatus.NEW;
		return BuildStatus.ACCEPTED;
	}


    /**
     * @return Returns the kindOfLatePenalty. null for projects that are not tested.
     */
    public String getKindOfLatePenalty() {
        return kindOfLatePenalty;
    }
    /**
     * @param kindOfLatePenalty The kindOfLatePenalty to set.
     */
    public void setKindOfLatePenalty(String kindOfLatePenalty) {
        this.kindOfLatePenalty = kindOfLatePenalty;
    }
    /**
     * @return Returns the lateConstant.
     */
    public int getLateConstant() {
        return lateConstant;
    }
    /**
     * @param lateConstant The lateConstant to set.
     */
    public void setLateConstant(int lateConstant) {
        this.lateConstant = lateConstant;
    }
    /**
     * @return Returns the lateMultiplier.
     */
    public double getLateMultiplier() {
        return lateMultiplier;
    }
    /**
     * @param lateMultiplier The lateMultiplier to set.
     */
    public void setLateMultiplier(double lateMultiplier) {
        this.lateMultiplier = lateMultiplier;
    }
    /**
     * @return Returns the visibleToStudents.
     */
    public boolean getVisibleToStudents() {
        return visibleToStudents;
    }
    /**
     * @param visibleToStudents The visibleToStudents to set.
     */
    public void setVisibleToStudents(boolean visibleToStudents) {
        this.visibleToStudents = visibleToStudents;
    }
    /**
	 * @return Returns the postMortemRevelationLevel.
	 */
	public String getPostDeadlineOutcomeVisibility() {
		return postDeadlineOutcomeVisibility;
	}
	/**
	 * @param postMortemRevelationLevel The postMortemRevelationLevel to set.
	 */
	public void setPostDeadlineOutcomeVisibility(String postMortemRevelationLevel) {
		this.postDeadlineOutcomeVisibility = postMortemRevelationLevel;
	}

	/**
     * @return Returns the ontime.
     */
    public Timestamp getOntime() {
        return ontime;
    }

    /**
     * @return The ontime deadline in utc millis.
     */
    public long getOntimeMillis() {
    	return ontime.getTime();
    }
    /**
     * @param ontime The ontime to set.
     */
    public void setOntime(Timestamp ontime) {
        this.ontime = ontime;
    }

    /**
     * @return Returns the canonicalStudentRegistrationPK.
     */
    public @StudentRegistration.PK int getCanonicalStudentRegistrationPK()
    {
        return canonicalStudentRegistrationPK;
    }
    /**
     * @param canonicalStudentRegistrationPK The canonicalStudentRegistrationPK to set.
     */
    public void setCanonicalStudentRegistrationPK(
            @StudentRegistration.PK int canonicalStudentRegistrationPK)
    {
        this.canonicalStudentRegistrationPK = canonicalStudentRegistrationPK;
    }
    /**
     * @return Returns the bestSubmissionPolicy.
     */
    public String getBestSubmissionPolicy() {
        return bestSubmissionPolicy;
    }
    /**
     * @param bestSubmissionPolicy The bestSubmissionPolicy to set.
     */
    public void setBestSubmissionPolicy(String bestSubmissionPolicy) {
        this.bestSubmissionPolicy = bestSubmissionPolicy;
    }
    /**
     * @return Returns the numReleaseTestsRevealed.
     */
    public int getNumReleaseTestsRevealed() {
        return numReleaseTestsRevealed;
    }
    /**
     * @param numReleaseTestsRevealed The numReleaseTestsRevealed to set.
     */
    public void setNumReleaseTestsRevealed(int numReleaseTestsRevealed) {
        this.numReleaseTestsRevealed = numReleaseTestsRevealed;
    }
    /**
     * @return Returns the releasePolicy.
     */
    public String getReleasePolicy() {
        return releasePolicy;
    }
    /**
     * @param releasePolicy The releasePolicy to set.
     */
    public void setReleasePolicy(String releasePolicy) {
        this.releasePolicy = releasePolicy;
    }
    /**
     * @return Returns the stackTracePolicy.
     */
    public String getStackTracePolicy() {
        return stackTracePolicy;
    }
    /**
     * @param stackTracePolicy The stackTracePolicy to set.
     */
    public void setStackTracePolicy(String stackTracePolicy) {
        this.stackTracePolicy = stackTracePolicy;
    }
    /**
     * @return Returns the archivePK.
     */
    public @CheckForNull Integer getArchivePK()
    {
        return archivePK;
    }

    /**
     * @param archivePK The archivePK to set.
     */
    public void setArchivePK(Integer archivePK)
    {
        this.archivePK = archivePK;
    }

    public BrowserEditing getBrowserEditing() {
        return browserEditing;
    }
    public void setBrowserEditing(BrowserEditing browserEditing) {
        this.browserEditing = browserEditing;
    }
    public String checkOnTime(Timestamp ts)
    {
        if (!ts.after(getOntime()))
            return "on-time";
        if (ts.after(getOntime()) && !ts.after(late))
            return "late";
        return "very late";
    }
	/**
	 * Populate a Submission from a ResultSet that is positioned
	 * at a row of the submissions table.
	 *
	 * @param resultSet the ResultSet containing the row data
	 * @param startingFrom index specifying where to start fetching attributes from;
	 *   useful if the row contains attributes from multiple tables
	 */
	public void fetchValues(ResultSet resultSet, int startingFrom) throws SQLException {
		setProjectPK(Project.asPK(SqlUtilities.getInteger(resultSet, startingFrom++)));
		setCoursePK(Course.asPK(resultSet.getInt(startingFrom++)));
		setTestSetupPK(resultSet.getInt(startingFrom++));
		setDiffAgainst(Project.asPK(resultSet.getInt(startingFrom++)));
		setProjectNumber(resultSet.getString(startingFrom++));
		setOntime(resultSet.getTimestamp(startingFrom++));
		setLate(resultSet.getTimestamp(startingFrom++));
		setTitle(resultSet.getString(startingFrom++));
		setUrl(resultSet.getString(startingFrom++));
		setDescription(resultSet.getString(startingFrom++));
		setReleaseTokens(resultSet.getInt(startingFrom++));
		setRegenerationTime(resultSet.getInt(startingFrom++));
		setIsTested(resultSet.getBoolean(startingFrom++));
		setPair(resultSet.getBoolean(startingFrom++));
		setVisibleToStudents(resultSet.getBoolean(startingFrom++));
		setPostDeadlineOutcomeVisibility(resultSet.getString(startingFrom++));
		setKindOfLatePenalty(resultSet.getString(startingFrom++));
		setLateMultiplier(resultSet.getDouble(startingFrom++));
		setLateConstant(resultSet.getInt(startingFrom++));
		setCanonicalStudentRegistrationPK(StudentRegistration.asPK(resultSet.getInt(startingFrom++)));
        setBestSubmissionPolicy(resultSet.getString(startingFrom++));
        setReleasePolicy(resultSet.getString(startingFrom++));
        setStackTracePolicy(resultSet.getString(startingFrom++));
        // Using -1 to represent infinity
        int num=resultSet.getInt(startingFrom++);
        if (num==-1)
            num=Integer.MAX_VALUE;
        setNumReleaseTestsRevealed(num);
        setArchivePK(SqlUtilities.getInteger(resultSet, startingFrom++));
        setBrowserEditing(BrowserEditing.valueOfAnyCase(resultSet.getString(startingFrom++)));
	}

	public void insert(Connection conn)
	throws SQLException
	{
	    String insert = Queries.makeInsertStatement(
	            ATTRIBUTE_NAME_LIST.length,
	            ATTRIBUTES,
	            TABLE_NAME);

	    PreparedStatement stmt=null;
	    try {
	        stmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);

	        int index=1;
	        putValues(stmt, index);

	        stmt.executeUpdate();

	       setProjectPK(Project.asPK(Queries.getGeneratedPrimaryKey(stmt)));

	    } finally {
	        Queries.closeStatement(stmt);
	    }

	    // [NAT] make sure all existing teams in the course do not have
	    //       release test access access to this project
	    StudentSubmitStatus.banExistingTeamsFromProject(conn, getCoursePK(), getProjectPK());
	}

	private int putValues(PreparedStatement stmt, int index)
	throws SQLException
	{
	    stmt.setInt(index++, Course.asPK(getCoursePK()));
		stmt.setInt(index++, getTestSetupPK());
		stmt.setInt(index++, getDiffAgainst());
		stmt.setString(index++, getProjectNumber());
		stmt.setTimestamp(index++, getOntime());
		stmt.setTimestamp(index++, getLate());
		stmt.setString(index++, getTitle());
		stmt.setString(index++, getUrl());
		stmt.setString(index++, getDescription());
		stmt.setInt(index++, getReleaseTokens());
		stmt.setInt(index++, getRegenerationTime());
		stmt.setBoolean(index++, isTested());
		stmt.setBoolean(index++, isPair());
		stmt.setBoolean(index++, getVisibleToStudents());
		stmt.setString(index++, getPostDeadlineOutcomeVisibility());
		stmt.setString(index++, getKindOfLatePenalty());
		stmt.setDouble(index++, getLateMultiplier());
		stmt.setInt(index++, getLateConstant());
		stmt.setInt(index++, getCanonicalStudentRegistrationPK());
        stmt.setString(index++, getBestSubmissionPolicy());
        stmt.setString(index++, getReleasePolicy());
        stmt.setString(index++, getStackTracePolicy());
        // Using -1 to represent infinity in the database
        if (getNumReleaseTestsRevealed()==Integer.MAX_VALUE)
            stmt.setInt(index++, -1);
        else
            stmt.setInt(index++, getNumReleaseTestsRevealed());
        SqlUtilities.setInteger(stmt, index++, getArchivePK());
        stmt.setString(index++, browserEditing.name().toLowerCase());
		return index;
	}
	
	public boolean setHidden(boolean newValue, Connection conn) throws SQLException {
		String update = "UPDATE projects set hidden=? where project_pk=?";
		 PreparedStatement stmt = null;
		   
		try {
			stmt = Queries.setStatement(conn,  update, newValue, projectPK);
			stmt.execute();
			return stmt.getUpdateCount() > 0;
		} finally {
	        Queries.closeStatement(stmt);
	    }
	}

	public void update(Connection conn)
	throws SQLException
	{
	    String whereClause = " WHERE project_pk = ? ";

	    String update = Queries.makeUpdateStatementWithWhereClause(
	            ATTRIBUTE_NAME_LIST,
	            TABLE_NAME,
	            whereClause);

	    PreparedStatement stmt = null;
	    try {
	        stmt = conn.prepareStatement(update);
	        int index=1;
	        index = putValues(stmt, index);
	        SqlUtilities.setInteger(stmt, index, getProjectPK());

	        stmt.executeUpdate();
	    } finally {
	        Queries.closeStatement(stmt);
	    }
	}

	/**
	 * Gets a project based on its projectPK.  This method looks for a project that should exist
	 * because it is referenced from someplace else within the database.
	 *
	 * @param projectPK the PK of the project
	 * @param conn the connection to the database.
	 * @return returns the project object.  Will never return null but rather throw an exception
	 * if the project is not found.
	 * @throws SQLException if the project is not found, throws an exception and also logs
	 * that the internal database state is corrupt.
	 */
	public static @Nonnull Project getByProjectPK(int projectPK, Connection conn)
	throws SQLException
	{
	    Project project = lookupByProjectPK(projectPK, conn);
	    if (project == null)
	    {
	        throw new IllegalArgumentException("No project with PK: " + projectPK);
	    }
	    return project;
	}

	/**
	 * Gets a project based on a submissionPK.  This method looks for a project referenced
	 * by a submission in our database.  If the project is not found, this represents an
	 * internal database integrity problem, and we throw an SQLException stating this.
	 *
	 * @param submissionPK the submission PK
	 * @param conn the connection to the database
	 * @return the project object if it is found.  This method cannot return null; an exception
	 * will be thrown if the project is not found.
	 * @throws SQLException
	 */
	public static Project getBySubmissionPK( @Submission.PK int submissionPK, Connection conn)
	throws SQLException
	{
	    Project project = lookupBySubmissionPK(submissionPK, conn);
	    if (project == null)
	    {
	        throw new SQLException("Unable to find project referenced by submission with PK: " +submissionPK);
	    }
	    return project;
	}

	public static Project lookupBySubmissionPK( @Submission.PK
			int submissionPK, Connection conn)
	throws SQLException
	{
	    String query = "SELECT " +ATTRIBUTES+ " " +
	    "FROM projects, submissions " +
	    "WHERE submissions.submission_pk = ? " +
	    "AND projects.project_pk = submissions.project_pk ";

	    PreparedStatement stmt = conn.prepareStatement(query);
	    SqlUtilities.setInteger(stmt, 1, submissionPK);

	    return getFromPreparedStatement(stmt);
	}

	public static Project lookupByProjectPK(int projectPK, Connection conn)
	throws SQLException
	{
		String query = " SELECT " +ATTRIBUTES+ " "+
		" FROM " +
		" projects " +
		" WHERE projects.project_pk = ? ";

		PreparedStatement stmt = conn.prepareStatement(query);
		SqlUtilities.setInteger(stmt, 1, projectPK);

		return getFromPreparedStatement(stmt);
	}
	

    public static Project lookupByCourseProjectSemester(String courseName, String section, String projectNumber, String semester,
            Connection conn) throws SQLException {
        String query = "SELECT " + ATTRIBUTES + " FROM " + " projects, courses " + " WHERE courses.coursename = ? "
                + " AND courses.semester = ? " + " AND courses.section = ? " + " AND courses.course_pk = projects.course_pk "
                + " AND projects.project_number = ? ";

        if (section == null || section.isEmpty())
            return lookupByCourseProjectSemester(courseName, projectNumber, semester, conn);
        PreparedStatement stmt = null;

        stmt = conn.prepareStatement(query);
        stmt.setString(1, courseName);
        stmt.setString(2, section);
        stmt.setString(3, semester);
        stmt.setString(4, projectNumber);

        Project result = getFromPreparedStatement(stmt);
        if (result == null) {
            return lookupByCourseProjectSemester(courseName, projectNumber, semester, conn);
        }
        return result;
    }
    public static Project lookupByCourseAndProjectNumber(@Course.PK int coursePK, String projectNumber,
            Connection conn) throws SQLException {
        String query = "SELECT " + ATTRIBUTES + " FROM " + " projects " 
               + " WHERE course_pk = ? "
                + " AND project_number = ? ";

        PreparedStatement stmt = conn.prepareStatement(query);

        stmt.setInt(1, coursePK);
        stmt.setString(2, projectNumber);
        return  getFromPreparedStatement(stmt);
    }


	public static Project lookupByCourseProjectSemester(
        String courseName,
        String projectNumber,
        String semester,
        Connection conn)
	throws SQLException
	{
		String query = "SELECT " +ATTRIBUTES+
		" FROM "+
		" projects, courses "+
		" WHERE courses.coursename = ? "+
		" AND courses.semester = ? "+
		" AND courses.course_pk = projects.course_pk "+
		" AND projects.project_number = ? ";

		PreparedStatement stmt = null;

		stmt = conn.prepareStatement(query);
		stmt.setString(1, courseName);
		stmt.setString(2, semester);
		stmt.setString(3, projectNumber);
		//Debug.print("lookupProjectByCourseProjectSemesterSection()" + stmt.toString());

		return getFromPreparedStatement(stmt);
	}

	/**
	 * Helper method that uses a prepared statement to fetch a project from the DB.
	 * This method automatically closes the prepared statement.  Note that no code other
	 * than set...() methods should be called on the statement before it is passed to this
	 * method, or a resource leak could happen.
	 *
	 * @param stmt the prepared statement to execute
	 * @return the project if it's found; null otherwise
	 * @throws SQLException
	 */
	private static Project getFromPreparedStatement(PreparedStatement stmt)
	throws SQLException
	{
		try {
			ResultSet rs = stmt.executeQuery();

			if (rs.next())
			{
				Project project = new Project();
				project.fetchValues(rs, 1);
				if (rs.next())
				    throw new SQLException("project not uniquely identified by " + stmt);
				return project;
			}
			return null;
		} finally {
			Queries.closeStatement(stmt);
		}
	}

    /**
     * @return True if this submission collection is eligible for release or build/quick tests
     * false otherwise.
     */
    public static boolean isTestingRequired(int projectPK, Connection conn)
    throws SQLException
    {
        // We cannot rely on the build_status of the submissions because we might
        // have an empty set of submissions, in which case we won't know whether
        // testing was required or not.
        // So we have to fetch the project record from the database.
        Project project = getByProjectPK(projectPK, conn);
        return project.isTested();
    }
    /**
     * @return Returns the testSetupPK.
     */
    public int getTestSetupPK() {
        return testSetupPK;
    }
    /**
     * @param testSetupPK The testSetupPK to set.
     */
    public void setTestSetupPK(int testSetupPK) {
        this.testSetupPK = testSetupPK;
    }


	@Override
	public String toString()
    {
        StringBuffer buf=new StringBuffer();
        buf.append("projectPK ="+projectPK+"\n");
        buf.append("coursePK ="+coursePK+"\n");
        buf.append("testSetupPK ="+testSetupPK+"\n");
        buf.append("diffAgainst ="+diffAgainst+"\n");
        buf.append("projectNumber ="+projectNumber+"\n");
        buf.append("ontime ="+ontime+"\n");
        buf.append("late ="+late+"\n");
        buf.append("title ="+title+"\n");
        buf.append("url ="+url+"\n");
        buf.append("description ="+description+"\n");
        buf.append("releaseTokens ="+releaseTokens+"\n");
        buf.append("regenerationTime ="+regenerationTime+"\n");
        buf.append("isTested ="+isTested+"\n");
        buf.append("visibleToStudents ="+visibleToStudents+"\n");
        buf.append("postDeadlineOutcomeVisibility="+postDeadlineOutcomeVisibility+"\n");
        buf.append("kindOfLatePenalty ="+kindOfLatePenalty+"\n");
        buf.append("lateMultiplier ="+lateMultiplier+"\n");
        buf.append("lateConstant ="+lateConstant+"\n");
        buf.append("canonicalStudentRegistrationPK ="+canonicalStudentRegistrationPK+"\n");
        buf.append("bestSubmissionPolicy ="+bestSubmissionPolicy+"\n");
        buf.append("releasePolicy ="+releasePolicy+"\n");
        buf.append("stackTracePolicy ="+stackTracePolicy+"\n");
        buf.append("numReleaseTestsRevealed ="+numReleaseTestsRevealed+"\n");
        return buf.toString();
    }

    public Course getCorrespondingCourse(Connection conn)
    {
        try {
            Course course = Course.getByCoursePK(getCoursePK(), conn);
            if (course==null)
                throw new SQLException();
            return course;
        } catch (SQLException e) {
            throw new IllegalStateException("Internal database is corrupted!  I cannot " +
                    " find coursePK=" +getCoursePK()+
                    " that corresponds to projectPK=" +getProjectPK(), e);
        }
    }

    /**
     * Uploads the bytes of a cached archive to the database.
     * @param conn the connection to the database
     * @return the archivePK of the newly uploaded archive
     * @throws SQLException
     */
    public Integer uploadCachedArchive(Connection conn)
    throws SQLException
    {
        UploadResult uploadBytesToArchive = Archive.uploadBytesToArchive(PROJECT_STARTER_FILE_ARCHIVES, cachedArchive, conn);
        setArchivePK(uploadBytesToArchive.archive_pk);
        return getArchivePK();
    }


    public void updateCachedArchive(byte[] bytes, Connection conn)
    throws SQLException {
		if (archivePK == null)
			throw new NullPointerException("archivePK not known yet");
		cachedArchive = bytes;
		Archive.updateBytesInArchive(PROJECT_STARTER_FILE_ARCHIVES, archivePK,
				cachedArchive, conn);
	}
    /**
     * Does this project have an archive cached as bytes ready for upload to the database?
     * @return true if this project has a cached archive of starter files, false otherwise
     */
    public boolean getHasCachedArchive()
    {
        return cachedArchive != null;
    }
    /**
     * Sets the byte array of the archive for upload to the database.
     * @param bytes array of bytes of the cached archive
     */
    public void setArchiveForUpload(byte[] bytes)
    {
        cachedArchive = bytes;
    }
    /**
     * Downloads the bytes of the archive from the database and returns them directly.
     * @param conn the connection to the database
     * @return an array of bytes of the cached archive, null if none
     * @throws SQLException
     */
    public @CheckForNull byte[] getBaselineZip(Connection conn)
    throws SQLException
    {
    		if (cachedArchive != null) return cachedArchive;
    		Integer archivePK = getArchivePK();
    		if (archivePK == null) return null;
    		cachedArchive = Archive.downloadBytesFromArchive(PROJECT_STARTER_FILE_ARCHIVES, archivePK, conn);
    		return cachedArchive;
    }

    public byte[] getBaselineZip(int archivePK, Connection conn) throws SQLException {
        Integer projectArchivePK = getArchivePK();
        if (projectArchivePK != null && archivePK == projectArchivePK)
            return getBaselineZip(conn);
        return Archive.downloadBytesFromArchive(PROJECT_STARTER_FILE_ARCHIVES, archivePK, conn);
    }

    public Map<String, byte[]> getBaselineContents(Connection conn)
    throws SQLException, IOException
    {
            if (cachedContents == null)
                cachedContents = Archive.getContents(PROJECT_STARTER_FILE_ARCHIVES, getArchivePK(), conn);
            return cachedContents;
    }

    public Map<String, byte[]> getBaselineContents(int archivePK, Connection conn) throws SQLException, IOException {
        Integer projectArchivePK = getArchivePK();
        if (projectArchivePK != null && archivePK == projectArchivePK)
            return getBaselineContents(conn);
        return Archive.getContents(PROJECT_STARTER_FILE_ARCHIVES, archivePK, conn);
    }
    
	@Override
	protected Project clone() {
		Project result;
		try {
			result = (Project) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
		result.projectPK = 0;
		return result;
	}

	public Project fork(Connection conn) throws SQLException, IOException {
		Project fork = this.clone();
		fork.setVisibleToStudents(false);
		fork.setTitle("Fork of " + this.getTitle());
		fork.setProjectNumber(this.getProjectNumber() + "-fork");
		fork.insert(conn);
		TestSetup currentTestSetup = TestSetup.lookupByTestSetupPK(getTestSetupPK(),
				conn);
		
		Collection<Submission> canonicalSubmissions = Submission
				.lookupAllByStudentRegistrationPKAndProjectPK(
						this.getCanonicalStudentRegistrationPK(),
						this.getProjectPK(), conn);
		int number = 1;
		for (Submission s : canonicalSubmissions) {
			if (s.getBuildStatus() == BuildStatus.BROKEN)
				continue;
			if (s.isCurrent(currentTestSetup) || s.isBaseline(fork)) {
				Submission f = s.fork();
				f.setSubmissionNumber(number++);
				f.setProjectPK(fork.getProjectPK());
				f.insert(conn);
			}
		}
		if (currentTestSetup != null) {
		TestSetup testSetup = currentTestSetup.clone();
		testSetup.setProjectPK(fork.getProjectPK());
		testSetup.insert(conn);
		fork.setTestSetupPK(testSetup.getTestSetupPK());
		}

		fork.update(conn);

		return fork;
	}
	public void exportProject(Connection conn, OutputStream out)
    throws SQLException, IOException
    {
        ZipOutputStream zipOutputStream=new ZipOutputStream(out);

        TestSetup testSetup=TestSetup.lookupByTestSetupPK(getTestSetupPK(),conn);
        if (testSetup != null) {

            // Test-setup
            zipOutputStream.putNextEntry(new ZipEntry(getProjectNumber() + "-test-setup.zip"));
            zipOutputStream.write(testSetup.downloadArchive(conn));

            // Canonical
            Submission canonical = Submission.lookupBySubmissionPK(
                    (TestRun.lookupByTestRunPK(testSetup.getTestRunPK(), conn)).getSubmissionPK(), conn);
            zipOutputStream.putNextEntry(new ZipEntry(getProjectNumber() + "-canonical.zip"));
            zipOutputStream.write(canonical.downloadArchive(conn));
        }

        // Serialize the project object itself and include it
        zipOutputStream.putNextEntry(new ZipEntry(getProjectNumber() +"-project.out"));
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream=new ObjectOutputStream(baos);
        objectOutputStream.writeObject(this);
        objectOutputStream.flush();
        objectOutputStream.close();
        zipOutputStream.write(baos.toByteArray());

        // project starter files, if any
        if (getArchivePK() != null) {
            zipOutputStream.putNextEntry(new ZipEntry(getProjectNumber() +"-project-starter-files.zip"));
            zipOutputStream.write(getBaselineZip(conn));
        }

        zipOutputStream.close();
    }

    private void readObject(ObjectInputStream stream)
    throws IOException, ClassNotFoundException
    {
        int thisMinorVersion = stream.readInt();
        if (thisMinorVersion != serialMinorVersion) throw new IOException("Illegal minor version " + thisMinorVersion + ", expecting minor version " + serialMinorVersion);
        stream.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream stream)
    throws IOException
    {
        stream.writeInt(serialMinorVersion);
        stream.defaultWriteObject();
    }


    public @CheckForNull Map<String,List<String>> getBaselineText(Connection conn) throws IOException, SQLException {
      return getBaselineText(conn, null);
    }
    
    public @CheckForNull Map<String,List<String>> getBaselineText(Connection conn,
           @CheckForNull DisplayProperties fileProperties) throws IOException, SQLException {
        Integer baselinePK = this.getArchivePK();
        if (baselinePK == null)
            return null;
        Map<String, List<String>> baselineText
        = TextUtilities.scanTextFiles(this.getBaselineContents(baselinePK, conn), fileProperties);
        return baselineText;

       
    }
    public  @Nonnull Map<String, BitSet> computeDiff(Connection conn,
    		Submission submission, 
    		Map<String, List<String>> current,
    		@CheckForNull Map<String, List<String>> baselineText, DisplayProperties fileProperties)
			throws IOException, SQLException {
		Map<String, BitSet> changed =  new HashMap<String,BitSet>();
        int baselinePK = 0;
        Integer submissionArchivePK = submission.getArchivePK();
		assert submissionArchivePK != null;
        Integer tmp = this.getArchivePK();
        if (tmp != null)
            baselinePK = tmp;
        if (diffAgainst != 0) {
	        Project projectToDiffAgainst = Project.getByProjectPK(diffAgainst, conn);
	        ChooseLastSubmissionPolicy policy = new ChooseLastSubmissionPolicy();
	        Submission compareTo = policy.lookupChosenOntimeOrLateSubmission(projectToDiffAgainst, submission.getStudentRegistrationPK() , conn);
	        if (compareTo != null) {
	            Integer compareToArchivePK = compareTo.getArchivePK();
	            assert compareToArchivePK != null;
				baselinePK = compareToArchivePK;
	        }
	    }
	        
	   if (baselinePK != 0 && baselinePK != submissionArchivePK) {
	        if (baselineText == null)
	            baselineText = TextUtilities.scanTextFiles(this.getBaselineContents(baselinePK, conn), fileProperties);
	        for(Entry<String, List<String>> e : current.entrySet()) {
	            String file = e.getKey();
	            if (!baselineText.containsKey(file))
	                continue;
	            BitSet set = EditDistance.SOURCE_CODE_DIFF.whichAreNew(baselineText.get(file), e.getValue());
	            changed.put(file, set);
	        }

	    }
		return changed;
	}
    
    public static List<Project> lookupAllByCoursePK(@Course.PK int coursePK,
    		Connection conn) throws SQLException
    		{
    		return lookupAllByCoursePK(coursePK, false, conn);
    		}
    		

	public static List<Project> lookupAllByCoursePK(@Course.PK int coursePK, boolean hidden, 
    		Connection conn) throws SQLException
    {
    	String query = "SELECT " +ATTRIBUTES+
    	" FROM projects "+
    	" WHERE projects.course_pk = ? " + 
    	" AND projects.hidden = ? " + 
        " ORDER BY ontime ASC ";

    	PreparedStatement stmt = null;
    	try {
    	     stmt = Queries.setStatement(conn, query, coursePK, hidden);
    	    
            return Project.getProjectsFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
    }
	
	public static List<Project> lookupAllUpcoming(Timestamp time,
            Connection conn) throws SQLException
    {
        String query = "SELECT " +ATTRIBUTES+
        " FROM projects "+
        " WHERE late > ? "+
        " AND hidden = ? " + 
        " ORDER BY ontime ASC ";

        PreparedStatement stmt = null;
        try {
            stmt = Queries.setStatement(conn, query, time, false);
            return Project.getProjectsFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
    }

	public static List<Project> lookupAll(
            Connection conn) throws SQLException
    {
        String query = "SELECT " +ATTRIBUTES+
        " FROM projects "+
        " WHERE hidden = ? " + 
        " ORDER BY ontime ASC ";

        PreparedStatement stmt = null;
        try {
        	 stmt = Queries.setStatement(conn, query, false);
            return Project.getProjectsFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
    }

    public static List<Project> lookupAllByStudentPKAndCoursePK(
    		@Student.PK Integer studentPK,
    		@Course.PK  Integer coursePK, Connection conn)
    throws SQLException
    {
    	String query = " SELECT " +ATTRIBUTES+
    	" FROM "+
    	" projects, student_registration "+
    	" WHERE student_registration.student_pk = ? "+
    	" AND student_registration.course_pk = projects.course_pk "+
    	" AND projects.course_pk = ?";

    	PreparedStatement stmt = null;
    	try {
    	    stmt = conn.prepareStatement(query);
    	    SqlUtilities.setInteger(stmt, 1, studentPK);
            SqlUtilities.setInteger(stmt, 2, coursePK);
            return getProjectsFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
    }

    private static List<Project> getProjectsFromPreparedStatement(
            PreparedStatement stmt) throws SQLException {
        List<Project> projects = new LinkedList<Project>();
        try {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Project project = new Project();
                project.fetchValues(rs, 1);
                projects.add(project);
            }
            return projects;
        } finally {
            stmt.close();
        }
    }

    public static Project importProject(InputStream in,
        Course course,
        StudentRegistration canonicalStudentRegistration,
        Connection conn)
    throws SQLException, IOException, ClassNotFoundException
    {
        Project project=new Project();
        ZipInputStream zipIn=new ZipInputStream(in);

        // Start transaction
        conn.setAutoCommit(false);
        conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

        byte[] canonicalBytes=null;
        byte[] testSetupBytes=null;
        byte[] projectStarterFileBytes=null;

        while (true) {
            ZipEntry entry=zipIn.getNextEntry();
            if (entry==null) break;
            if (entry.getName().contains("project.out")) {
                // Found the serialized project!
                ObjectInputStream objectInputStream=new ObjectInputStream(zipIn);

                project=(Project)objectInputStream.readObject();

                // Set the PKs to null, the values that get serialized are actually from
                // a different database with a different set of keys
                project.setProjectPK(0);
                project.setTestSetupPK(0);
                project.setArchivePK(null);
                project.setVisibleToStudents(false);

                // These two PKs need to be passed in when we import/create the project
                project.setCoursePK(course.getCoursePK());
                project.setCanonicalStudentRegistrationPK(canonicalStudentRegistration.getStudentRegistrationPK());

                // Insert the project so that we have a projectPK for other methods
                project.insert(conn);

            } else if (entry.getName().contains("canonical")) {
                // Found the canonical submission...
                ByteArrayOutputStream baos=new ByteArrayOutputStream();
                CopyUtils.copy(zipIn, baos);
                canonicalBytes=baos.toByteArray();
            } else if (entry.getName().contains("test-setup")) {
                // Found the test-setup!
                ByteArrayOutputStream baos=new ByteArrayOutputStream();
                CopyUtils.copy(zipIn, baos);
                testSetupBytes=baos.toByteArray();
            } else if (entry.getName().contains("project-starter-files")) {
                // Found project starter files
                ByteArrayOutputStream baos=new ByteArrayOutputStream();
                CopyUtils.copy(zipIn, baos);
                projectStarterFileBytes=baos.toByteArray();
            }
        }

        Timestamp submissionTimestamp=new Timestamp(System.currentTimeMillis());

        // Now "upload" bytes as an archive for the project starter files, if it exists
        if (projectStarterFileBytes!=null) {
            project.setArchiveForUpload(projectStarterFileBytes);
            project.uploadCachedArchive(conn);
        }

        // Now "submit" these bytes as a canonical submission
        // TODO read the submissionTimestamp from the serialized project in the archive
        Submission submission=Submission.submit(
            canonicalBytes,
            canonicalStudentRegistration,
            project,
            "t" + submissionTimestamp.getTime(),
            "ProjectImportTool, serialMinorVersion",
            Integer.toString(serialMinorVersion,100),
            submissionTimestamp,
            conn);

        // Now "upload" the test-setup bytes as an archive
        String comment="Project Import Tool uploaded at " +submissionTimestamp;
        TestSetup testSetup=TestSetup.submit(testSetupBytes, project, comment, conn);
        project.setTestSetupPK(testSetup.getTestSetupPK());
        testSetup.setTestRunPK(submission.getCurrentTestRunPK());


        testSetup.update(conn);

        return project;
    }
    
	public Map<String, Integer> getBuildStatusCount(Connection c)
			throws SQLException {

		PreparedStatement stmt = c
				.prepareStatement("SELECT build_status, count(*) FROM "
						+ Submission.TABLE_NAME 
						+ " WHERE project_pk=? "
						+ " GROUP BY build_status "
						
						);
		stmt.setInt(1, getProjectPK());
		Map<String, Integer> result = new LinkedHashMap<String, Integer>();
		int done = 0;
		int notDone = 0;
		try {
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				String status = rs.getString(1);
				BuildStatus bs = BuildStatus.valueOfAnyCase(status);
				int count = rs.getInt(2);
				if (bs.isDone())
				    done += count;
				else
				    notDone += count;
				result.put(status, count);

			}
		} finally {
			Queries.closeStatement(stmt);
		}
		if (done > 0)
		    result.put("done", done);
		if (notDone > 0)
		    result.put("notDone", notDone);
		return result;

	}
    /**
     * @param project
     * @param displayProperties
     * @param conn
     * @return
     * @throws IOException
     * @throws SQLException
     */
    public  Map<String, List<String>> getBaselineText(DisplayProperties displayProperties,
        Connection conn) throws IOException, SQLException {
      return TextUtilities.scanTextFiles(this.getBaselineContents(conn), displayProperties);
    }
}
