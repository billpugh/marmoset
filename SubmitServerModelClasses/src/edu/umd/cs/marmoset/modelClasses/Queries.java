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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckReturnValue;
import javax.annotation.WillClose;

import edu.umd.cs.marmoset.modelClasses.Submission.BuildStatus;
import edu.umd.cs.marmoset.utilities.MarmosetPatterns;
import edu.umd.cs.marmoset.utilities.MarmosetUtilities;
import edu.umd.cs.marmoset.utilities.SqlUtilities;

/**
 * TODO refactor the lookup[New|Pending|ReTest]Submission...() methods so that
 * they return the submission, and move them into Submission. Then write another
 * static method that looks up the other information.
 *
 * Canned queries.
 */
public final class Queries {
	/**
	 * Max number of times a submission should be successfully retested. TODO
	 * Make this configurable in web.xml.
	 */
	public static final int MAX_SUCCESSFUL_RETESTS = 2;

	public static void close(@WillClose Connection conn) {
        try {
            if (conn != null)
                conn.close();
        } catch (SQLException ignore) {
            // ignore
        }
    }
	public static void closeStatement(@WillClose PreparedStatement stmt) {
		try {
			if (stmt != null)
				stmt.close();
		} catch (SQLException ignore) {
			// ignore
		}
	}

	/**
	 * Counts the number of submissions that need to be retested for a new test
	 * setup for a given projectPK. Called from JSP.
	 *
	 * @param projectPK
	 *            the projectPK of the project that needs to be retested
	 * @param conn
	 *            the connection to the database
	 * @return the number of submissions that need to be retested for a new test
	 *         setup.
	 * @throws SQLException
	 */
	public static int countNumSubmissionsForRetest(String projectPKStr,
			Connection conn) throws SQLException {
		Integer projectPK = MarmosetUtilities.toIntegerOrNull(projectPKStr);
		String query = " SELECT count(*) "
				+ " FROM submissions, projects, test_runs, test_setups "
				+ " WHERE submissions.project_pk = projects.project_pk "
				+ " AND submissions.current_test_run_pk = test_runs.test_run_pk "
				+ " AND test_runs.test_setup_pk != projects.test_setup_pk "
				+ " AND test_setups.test_setup_pk = projects.test_setup_pk "
				+ " AND projects.project_pk = ? ";
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(query);
			SqlUtilities.setInteger(stmt, 1, projectPK);

			ResultSet rs = stmt.executeQuery();
			rs.next();
			return rs.getInt(1);
		} finally {
			closeStatement(stmt);
		}
	}

	/**
	 * Count the number of submissions waiting to be tested. A submission is
	 * waiting to be tested when 1) it is not a canonical submission, and 2)
	 * submissions.current_test_run_pk is null, and 3) the build_status is not
	 * 'broken'. Called from JSP.
	 *
	 * @param projectPK
	 *            key for the project that needs to be queried
	 * @param conn
	 *            the connection to the database
	 * @return number of submissions waiting to be tested
	 * @throws SQLException
	 */
	public static int countNumSubmissionsToTest(String projectPKStr,
			Connection conn) throws SQLException {
		Integer projectPK = MarmosetUtilities.toIntegerOrNull(projectPKStr);
		if (projectPK == null)
			throw new IllegalArgumentException("Bad projectPK");
		String query = " SELECT count(*) "
				+ " FROM submissions "
				+ " WHERE  current_test_run_pk is null "
				+ " AND build_status != ? "
				+ " AND project_pk = ? ";
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(query);
			setStatement(stmt, BuildStatus.BROKEN, projectPK);

			ResultSet rs = stmt.executeQuery();
			rs.next();
			return rs.getInt(1);
		} finally {
			closeStatement(stmt);
		}
	}

	/**
	 * Format an array of attributes as a fully qualified attribute list,
	 * suitable for use in a SELECT query.
	 *
	 * @param table
	 *            name of table
	 * @param attrNameList
	 *            array of attribute names for table
	 * @return fully qualified attribute list for query
	 */
	public static String getAttributeList(String table, String[] attrNameList) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < attrNameList.length; ++i) {
			if (!MarmosetPatterns.isAttributeName(attrNameList[i])) {
				throw new IllegalArgumentException("Invalid attribute name");
			}
			buf.append(table);
			buf.append('.');
			buf.append(attrNameList[i]);
			if (i < attrNameList.length - 1)
				buf.append(", ");
		}
		return buf.toString();
	}


	public static int getGeneratedPrimaryKey(PreparedStatement stmt) throws SQLException {
		ResultSet rs = stmt.getGeneratedKeys();
		try {
	     if (!rs.next())
	     	  throw new SQLException("No primary key returned");

	     return rs.getInt(1);
		} finally {
			rs.close();
		}

	}

	/**
	 * Check if the current database specified by the given connection contains
	 * a table with the given name.
	 *
	 * @param tableName
	 *            The name of the table.
	 * @param conn
	 *            Connection to the database.
	 * @return True if the current database contains a table with the given
	 *         name; false otherwise.
	 * @throws SQLException
	 */
	public static boolean hasTable(String tableName, Connection conn)
			throws SQLException {
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(" show tables ");
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				if (tableName.equals(rs.getString(1)))
					return true;
			}
			return false;
		} finally {
			Queries.closeStatement(stmt);
		}
	}

	public static boolean hasTableAndColumn(String tableName,
			String columnName, Connection conn) throws SQLException {
		if (tableName == null)
			throw new IllegalArgumentException("tableName must not be null!");
		if (columnName == null)
			throw new IllegalArgumentException("columnName must not be null!");
		if (!hasTable(tableName, conn))
			return false;
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(" describe " + tableName);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				if (columnName.equals(rs.getString(1)))
					return true;
			}
			return false;
		} finally {
			Queries.closeStatement(stmt);
		}
	}

	public static enum RetestPriority {
		MOST_RECENT(" AND submissions.most_recent "), RELEASE_TESTED(
				" AND submissions.release_request IS NOT NULL "), ANY("");
		final  String query;

		private RetestPriority(String query) {
			this.query = query;
		}
	}

	/**
	 * Gets the oldest submission marked with the provided build status and most_recent flag
	 *
	 */
	public static boolean lookupSubmission(Connection conn,
			Submission submission, TestSetup testSetup,
			Collection<Integer> allowedCourses, BuildStatus buildStatus, RetestPriority priority) throws SQLException {

		String query = querySubmissionAndTestSetup(allowedCourses, priority);
		query +=  " AND submissions.build_status = ? "
				+ " ORDER BY submissions.submission_timestamp ASC "
				+ " LIMIT 1 LOCK IN SHARE MODE ";

		PreparedStatement stmt = conn.prepareStatement(query);
		setStatement(stmt, buildStatus);
		return getFromPreparedStatement(stmt, submission, testSetup);
	}

	
	   public static boolean lookupOldestSubmissionForProject(Connection conn,
	            Submission submission, TestSetup testSetup,
	             Project project)
	            throws SQLException {
	       
	       String query = " SELECT "
	                + Submission.ATTRIBUTES
	                + ", "
	                + TestSetup.ATTRIBUTES
	                + " "
	                + " FROM submissions, test_setups, projects "
	                + " WHERE "
	                + "     submissions.project_pk = projects.project_pk "
	                + " AND projects.test_setup_pk = test_setups.test_setup_pk "
	                + " AND projects.project_pk = ? "
                    + " AND submissions.build_status != ? "
	                + " ORDER BY submissions.build_request_timestamp  ASC "
	                + " LIMIT 1 " + " LOCK IN SHARE MODE ";

	        PreparedStatement stmt = conn.prepareStatement(query);
	        setStatement(stmt, project.getProjectPK(), BuildStatus.BROKEN);

	        return getFromPreparedStatement(stmt, submission, testSetup);
	    }
	   
	/**
	 * Gets the most recent submission that has timed out (i.e. been marked
	 * "pending" for too long)
	 *
	 * @param conn
	 * @param submission
	 * @param testSetup
	 * @param allowedCourses
	 * @param maxBuildDurationInMinutes
	 *            the amount of time in minutes a submission can be marked
	 *            "pending" before we send it out for testing again
	 * @param most_recent TODO
	 * @return
	 * @throws SQLException
	 */
	public static boolean lookupPendingSubmission(Connection conn,
			Submission submission, TestSetup testSetup,
			 Collection<Integer> allowedCourses, int maxBuildDurationInMinutes, RetestPriority priority)
			throws SQLException {
		// SQL timestamp of build requests that have,
		// as of this moment, taken too long.
	    
		Timestamp buildTimeout = new Timestamp(System.currentTimeMillis()
				- TimeUnit.MILLISECONDS.convert(maxBuildDurationInMinutes, TimeUnit.MINUTES));

		String query = querySubmissionAndTestSetup(allowedCourses, priority);

		query += " AND submissions.build_status = ? "
				+ " AND submissions.build_request_timestamp < ? "
				+ " ORDER BY submissions.submission_timestamp ASC "
				+ " LIMIT 1 " + " LOCK IN SHARE MODE ";

		PreparedStatement stmt = conn.prepareStatement(query);
		setStatement(stmt, BuildStatus.PENDING, buildTimeout);

		return getFromPreparedStatement(stmt, submission, testSetup);
	}

	/**
	 * Gets the most recent submission that has timed out (i.e. been marked
	 * "pending" for too long)
	 *
	 * @param conn
	 * @param submission
	 * @param testSetup
	 * @param allowedCourses
	 * @param maxBuildDurationInMinutes
	 *            the amount of time in minutes a submission can be marked
	 *            "pending" before we send it out for testing again
	 * @param most_recent TODO
	 * @return
	 * @throws SQLException
	 */
	public static boolean lookupSubmissionWithOutdatedTestResults(Connection conn,
			Submission submission, TestSetup testSetup,
			Collection<Integer> allowedCourses, RetestPriority priority)
			throws SQLException {

		String query = querySubmissionAndTestSetupForOutOfDateTestSetup(allowedCourses, priority);

		query += " AND submissions.build_status = ? "
				+ " ORDER BY submissions.submission_timestamp ASC "
				+ " LIMIT 1 " + " LOCK IN SHARE MODE ";

		PreparedStatement stmt = conn.prepareStatement(query);
		setStatement(stmt, BuildStatus.COMPLETE);

		return getFromPreparedStatement(stmt, submission, testSetup);
	}

	public static Set<Integer> lookupSubmissionsWithOutdatedTestResults(Connection conn,
	        Project project) throws SQLException {
	    String query = " SELECT "
                + " submissions.submission_pk "
                + " FROM submissions, test_runs "
                + " WHERE submissions.project_pk = ? "
                + " AND submissions.current_test_run_pk = test_runs.test_run_pk "
                + " AND test_runs.test_setup_pk != ? ";
	    PreparedStatement stmt = conn.prepareStatement(query);
        setStatement(stmt, project.getProjectPK(), project.getTestSetupPK());
        
        try {
            ResultSet rs = stmt.executeQuery();
            HashSet<Integer> result = new HashSet<Integer>();
            
            while (rs.next()) {
                result.add(rs.getInt(1));
            }
            return result;
        } finally {
            closeStatement(stmt);
        }

    
	    
	}

	/**
	 * If background re-testing is enabled, pick a random submission for one of
	 * the allowed courses and its current testSetup. Will only retest a
	 * submission 6 times against the current testSetup. If the first 2
	 * retests are "successful" (meaning that the results are exactly the same
	 * as the results for the current_test_run_pk) then no further retesting is
	 * performed. Submissions must compile to be eligible for background
	 * retesting.
	 *
	 * @param conn
	 * @param submission
	 * @param testSetup
	 * @param allowedCourses
	 * @return True if background re-testing is enabled and we found a
	 *         submission and jarfile; false otherwise.
	 * @throws SQLException
	 */
	public static boolean lookupSubmissionForBackgroundRetest(Connection conn,
			Submission submission, TestSetup testSetup,
			 Collection<Integer> allowedCourses)
			throws SQLException {

		String query = querySubmissionAndTestSetup(allowedCourses, RetestPriority.ANY);
		
		Timestamp buildTimeout = new Timestamp(System.currentTimeMillis()
                - TimeUnit.MILLISECONDS.convert(20, TimeUnit.MINUTES));

		query += " AND submissions.build_status = ? "
			    + " AND submissions.num_successful_background_retests < ? "
			    + " AND submissions.num_failed_background_retests < ? "
			    + " AND submissions.build_request_timestamp < ? "
			    + " AND submissions.num_pending_build_requests < ? "
				+ " ORDER BY rand()  "
				+ " LIMIT 1 " ;

		PreparedStatement stmt = conn.prepareStatement(query);
		setStatement(stmt, BuildStatus.COMPLETE, 2, 4, buildTimeout, 4);
		return getFromPreparedStatement(stmt, submission, testSetup);
	}

	/**
	 * Gets the most recent canonical submission along with a new project
	 * jarfile.
	 *
	 * @param conn
	 * @param submission
	 * @param info.project
	 * @param course
	 * @param studentRegistration
	 * @param testSetup
	 * @return
	 */
	public static boolean lookupNewTestSetup(Connection conn,
			Submission submission, TestSetup testSetup,
			Collection<Integer> allowedCourses, int maxBuildDurationInMinutes)
			throws SQLException {
	    if (allowedCourses.isEmpty())
	        throw new IllegalArgumentException();
		// SQL timestamp of build requests that have,
		// as of this moment, taken too long.
		Timestamp buildTimeout = new Timestamp(System.currentTimeMillis()
				- (maxBuildDurationInMinutes * 60L * 1000L));

		String courseRestrictions = makeCourseRestrictionsWhereClause(allowedCourses);

	    String query = " SELECT "
				+ Submission.ATTRIBUTES
				+ ", "
				+ TestSetup.ATTRIBUTES
				+ " "
				+ " FROM submissions, test_setups, projects "
				+ " WHERE ("
				+ "		 (test_setups.jarfile_status = ?) "
				+ "		 OR (test_setups.jarfile_status = ? AND test_setups.date_posted < ?)"
				+ "       ) "
				+ courseRestrictions
				+ " AND test_setups.project_pk = projects.project_pk "
				+ " AND submissions.project_pk = projects.project_pk "
			    + " AND submissions.most_recent = 1 "
			    + " AND submissions.build_status != ? "
				+ " AND submissions.student_registration_pk = projects.canonical_student_registration_pk "
				+ " ORDER BY submissions.submission_number DESC " + " LIMIT 1 ";

		PreparedStatement stmt = conn.prepareStatement(query);
		stmt.setString(1, TestSetup.Status.NEW.toString());
		stmt.setString(2, TestSetup.Status.PENDING.toString());
		stmt.setTimestamp(3, buildTimeout);
		stmt.setString(4,  Submission.BuildStatus.BROKEN.toString());


		return getFromPreparedStatement(stmt, submission, testSetup);
	}


	/**
	 *
	 * Construct basic equijoin of submissions, test_setups, projects
	 * @param allowedCourses
	 * @param most_recent TODO
	 * @return
	 */
	private static String querySubmissionAndTestSetup(Collection<Integer> allowedCourses, RetestPriority priority) {
		String courseRestrictions = makeCourseRestrictionsWhereClause(allowedCourses);

		String query = " SELECT "
				+ Submission.ATTRIBUTES
				+ ", "
				+ TestSetup.ATTRIBUTES
				+ " "
				+ " FROM submissions, test_setups, projects "
				+ " WHERE "
				+ "     submissions.project_pk = projects.project_pk "
				+ " AND projects.test_setup_pk = test_setups.test_setup_pk "
				+ courseRestrictions
				+ priority.query;
		return query;
	}


	/**
	 *
	 * Construct basic equijoin of submissions, test_setups, projects from those
	 * with an outdated test run
	 * @param allowedCourses
	 * @param priority which submissions are considered
	 * @return
	 */
	private static String querySubmissionAndTestSetupForOutOfDateTestSetup(
	        Collection<Integer> allowedCourses, RetestPriority priority) {
		String courseRestrictions = makeCourseRestrictionsWhereClause(allowedCourses);

		String query = " SELECT "
				+ Submission.ATTRIBUTES
				+ ", "
				+ TestSetup.ATTRIBUTES
				+ " FROM submissions, test_setups, test_runs, projects "
				+ " WHERE "
				+ "     submissions.project_pk = projects.project_pk "
				+ " AND projects.test_setup_pk = test_setups.test_setup_pk "
				+ " AND submissions.current_test_run_pk = test_runs.test_run_pk "
				+ " AND test_runs.test_setup_pk != projects.test_setup_pk "
				+ courseRestrictions
				+ priority.query;
		return query;
	}

	   
	
	public static String makeInsertStatement(String [] attributeNames,
		 String tableName) {
		return makeInsertStatement(attributeNames.length, 
				Queries.getAttributeList(tableName, attributeNames),
				tableName);
	}
	

	public static String makeInsertStatement(int numAttributes,
			String attributes, String tableName) {
		if (!MarmosetPatterns.isTableName(tableName)) {
			throw new IllegalArgumentException("Bad table name");
		}
		if (!MarmosetPatterns.isAttributeList(attributes)) {
			throw new IllegalArgumentException("Bad attribute list");
		}

		StringBuffer result = new StringBuffer();
		result.append(" INSERT INTO " + tableName + " \n");
		result.append(" ( " + attributes + " ) \n");
		result.append(" VALUES \n");
		result.append(" ( DEFAULT, \n");
		// leave a slot at the beginning for the primary key and one at the end
		// with no comma
		for (int ii = 0; ii < numAttributes - 2; ii++) {
			result.append(" ?, \n");
		}
		result.append(" ? ) \n");
		return result.toString();
	}
	
	public static String makeInsertStatementUsingSetSyntax(String[] attributes,
            String tableName, boolean skipFirstAttribute) {
        StringBuffer buf = new StringBuffer();
        buf.append(" INSERT INTO " + tableName + " \n");
        buf.append(" SET \n");
        // Skip primary key
        for (int ii = skipFirstAttribute ? 1 : 0; ii < attributes.length - 1; ii++) {
            buf.append(" " + attributes[ii] + " = ?, \n");
        }
        buf.append(" " + attributes[attributes.length - 1] + " = ? \n");
        return buf.toString();
    }

	public static String makeInsertOrUpdateStatement(String[] insert, String[] update,
			String tableName) {
		StringBuffer buf = new StringBuffer();
		buf.append(" INSERT INTO " + tableName + " \n");
		buf.append(" SET \n");
		for (int ii = 0; ii < insert.length - 1; ii++) {
			buf.append(" " + insert[ii] + " = ?, \n");
		}
		buf.append(" " + insert[insert.length - 1] + " = ? \n");
		buf.append(" ON DUPLICATE KEY UPDATE \n");
		// Skip primary key
		for (int ii = 0; ii < update.length - 1; ii++) {
			buf.append(" " + update[ii] + " = ?, \n");
		}
		buf.append(" " + update[update.length - 1] + " = ? \n");

		return buf.toString();
	}
	public static String makeInsertOrUpdateStatement(String[] attributes,
			String tableName) {
		StringBuffer buf = new StringBuffer();
		buf.append(" INSERT INTO " + tableName + " \n");
		buf.append(" SET \n");
		for (int ii = 0; ii < attributes.length - 1; ii++) {
			buf.append(" " + attributes[ii] + " = ?, \n");
		}
		buf.append(" " + attributes[attributes.length - 1] + " = ? \n");
		buf.append(" ON DUPLICATE KEY UPDATE \n");
		// Skip primary key
		for (int ii = 1; ii < attributes.length - 1; ii++) {
			buf.append(" " + attributes[ii] + " = ?, \n");
		}
		buf.append(" " + attributes[attributes.length - 1] + " = ? \n");

		return buf.toString();
	}

	public static String makeUpdateStatementWithWhereClause(
			String[] attributesList, String tableName, String whereClause) {
		StringBuffer update = makeGenericUpdateStatement(attributesList,
				tableName);
		update.append(whereClause);
		return update.toString();
	}

	public static void rollbackIfUnsuccessful(boolean transactionSuccess,
			Connection conn) {
		try {
			if (!transactionSuccess)
				conn.rollback();
		} catch (SQLException ignore) {
			// ignore
		}
	}

	public static @CheckReturnValue PreparedStatement setStatement(Connection conn, String query, Object... args) throws SQLException {
	    PreparedStatement stmt = conn.prepareStatement(query);
	    setStatement(stmt, args);
	    return stmt;
	}

	
	//makeInsertOrUpdateStatement
	
	public static void setStatement(PreparedStatement p, Object... args)
			throws SQLException {
		for (int i = 0; i < args.length; i++) {
			Object o = args[i];
			int col = i + 1;
				
			setPreparedStatementParameter(p, col, o);
		}
	}
	
	public static void setInsertOrUpdateStatement(PreparedStatement p, Object... args)
			throws SQLException {
		int col = 1;
		for (int i = 0; i < args.length; i++) {
				
			setPreparedStatementParameter(p, col++, args[i]);
		}
		// skip first argument, which is primary key
		for (int i = 1; i < args.length; i++) {
			setPreparedStatementParameter(p, col++, args[i]);
		}

	}
	
	
	private static void setPreparedStatementParameter(PreparedStatement p, int col, Object o) throws SQLException {
//		ParameterMetaData meta = p.getParameterMetaData();
//		String className = meta.getParameterClassName(col);
//		if (o != null && !o.getClass().getName().equals(className)) {
//			new SQLException(String.format("parameter %d of type %s doesn't match argument of class %s",
//					col, className, o.getClass().getName())).printStackTrace();
//		}

		if (o instanceof String)
			p.setString(col, (String) o);
		else if (o instanceof Enum<?>) 
		    p.setString(col, ((Enum<?>) o).name());
		else if (o instanceof Integer)
			p.setInt(col, (Integer) o);
		else if (o instanceof Long)
			p.setLong(col, (Long) o);
		else if (o instanceof Timestamp)
			p.setTimestamp(col, (Timestamp) o);
		else if (o instanceof Blob)
			p.setBlob(col, (Blob) o);
		else if (o instanceof Boolean)
			p.setBoolean(col, (Boolean) o);
		else if (o == null)
			p.setNull(col, Types.INTEGER);
		else
			throw new IllegalArgumentException(
					"Can handle argument of class "
							+ o.getClass().getSimpleName());
	}

	/**
	 * Move findbugs warnings from fromTestRunPK to toTestRunPK. Note that the
	 * 'outcome' should be different for both testruns (e.g. from should be
	 * 'warning-1.1', while to is 'warning'), Otherwise an error may occur as
	 * duplicate findbugs entries may emerge.
	 *
	 * @return the number of rows affected by this query
	 */
	public static int updateFindbugsTestRun(Integer fromTestRunPK,
			Integer toTestRunPK, Connection conn) throws SQLException {
		String query = " UPDATE test_outcomes " + " SET    test_run_pk = ? "
				+ " WHERE  test_run_pk = ? "
				+ " AND    test_type = 'findbugs' ";
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(query);
			SqlUtilities.setInteger(stmt, 1, toTestRunPK);
			SqlUtilities.setInteger(stmt, 2, fromTestRunPK);

			return stmt.executeUpdate();
		} finally {
			closeStatement(stmt);
		}
	}

	private static boolean getFromPreparedStatement(PreparedStatement stmt,
			Submission submission, TestSetup testSetup)
			throws SQLException {
		try {
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				int index = 1;
				index = submission.fetchValues(rs, index);
				index = testSetup.fetchValues(rs, index);
				return true;
			}
			return false;
		} catch (SQLException e) {
		    System.out.println("XXX SQL Error with " + stmt);
		    throw e;
		} finally {
			closeStatement(stmt);
		}
	}

	/**
	 * @param allowedCourses
	 * @return
	 */
    public static String makeCourseRestrictionsWhereClause(Collection<Integer> allowedCourses) {
        if (allowedCourses.isEmpty())
            throw new IllegalArgumentException("no courses");
        String whereClause = null;
        for (Integer coursePK : allowedCourses) {
            if (whereClause == null)
                whereClause = " AND ( ( projects.course_pk = " + coursePK + " ) ";
            else
                whereClause += " OR ( projects.course_pk = " + coursePK + " ) ";
        }
        if (whereClause == null)
            throw new AssertionError();
        whereClause += " ) ";
        return whereClause;
	}

	private static StringBuffer makeGenericUpdateStatement(
			String[] attributesList, String tableName) {
		StringBuffer result = new StringBuffer();
		result.append(" UPDATE " + tableName + " \n");
		result.append(" SET \n");
		// ignore first slot which is the PK
		// leave a [ col_name = ? ] at the end with no comma
		for (int ii = 1; ii < attributesList.length - 1; ii++) {
			result.append(attributesList[ii] + " = ?, \n");
		}
		result.append(attributesList[attributesList.length - 1] + " = ? \n");
		return result;
	}


	public static Blob serialize(Connection conn, Object o) {
		if (o == null)
			return null;
		try {
			Blob b = conn.createBlob();
			ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bOut);
		out.writeObject(o);
		out.close();
		b.setBytes(1, bOut.toByteArray());
		return b;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	public static Object deserialize(Blob bytes) {
		if (bytes == null)
			return null;
		try {
		ObjectInputStream in = new ObjectInputStream(bytes.getBinaryStream());
		Object result = in.readObject();
		in.close();
		return result;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	

	private Queries() {
		// block instantiation of this class
	}

}
