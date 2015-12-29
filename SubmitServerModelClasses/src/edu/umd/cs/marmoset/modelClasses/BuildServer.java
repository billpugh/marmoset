package edu.umd.cs.marmoset.modelClasses;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;

public class BuildServer implements Comparable<BuildServer> {

	int buildserverPK;
	String name;
	@CheckForNull
	String courses;
	String remoteHost;
	String javaVersion;
	Timestamp lastRequest;
	@Submission.PK final int lastRequestSubmissionPK;
	@CheckForNull
	Timestamp lastSuccess;
	@CheckForNull
	Timestamp lastJob;
	String load;
	final TestRun.Kind kind;
	@Project.PK final int lastRequestProjectPK;
	@Course.PK final int lastRequestCoursePK;
	public static final String TABLE_NAME = "buildservers";

	static final String[] ATTRIBUTE_NAME_LIST = { "buildserver_pk", "name",
			"remote_host", "courses", "java_version", "last_request", "last_request_submission_pk", "last_job", "last_success", "system_load", "kind" };

	public static final String ATTRIBUTES = Queries.getAttributeList(
			TABLE_NAME, ATTRIBUTE_NAME_LIST);

	public BuildServer(ResultSet rs, int startingFrom) throws SQLException {
		buildserverPK = rs.getInt(startingFrom++);
		name = rs.getString(startingFrom++);
		remoteHost = rs.getString(startingFrom++);
		courses = rs.getString(startingFrom++);
		javaVersion = rs.getString(startingFrom++);
		lastRequest = rs.getTimestamp(startingFrom++);
		lastRequestSubmissionPK = Submission.asPK(rs.getInt(startingFrom++));
		lastJob = rs.getTimestamp(startingFrom++);
		lastSuccess = rs.getTimestamp(startingFrom++);
		load = rs.getString(startingFrom++);
		kind = TestRun.Kind.valueOfAnyCase(rs.getString(startingFrom++));
		boolean hasLastRequest = lastRequestSubmissionPK != 0;
		lastRequestProjectPK = hasLastRequest ? Project.asPK(rs.getInt(startingFrom++)) : 0;
		lastRequestCoursePK = hasLastRequest ? Course.asPK(rs.getInt(startingFrom++)) : 0;
	}

	public boolean canBuild(Course course, @CheckForNull String universalBuilderserverKey) {
	    if (courses == null || course == null) return false;
	    String buildserverKey = course.getBuildserverKey();
        if (universalBuilderserverKey != null && courses.startsWith(universalBuilderserverKey +"-"))
	        return !courses.substring(universalBuilderserverKey.length()+1).contains(buildserverKey);
		return  courses.contains(buildserverKey);
	}
	public String getName() {
		return name;
	}

	public @CheckForNull String getCourses() {
		return courses;
	}

	public String getJavaVersion() {
		return javaVersion;
	}
	
	public String getRemoteHost() {
		return remoteHost;
	}

	public Timestamp getLastRequest() {
		return lastRequest;
	}
	public @Submission.PK int getLastRequestSubmissionPK() {
        return lastRequestSubmissionPK;
    }
	public @Project.PK int getLastRequestProjectPK() {
		return lastRequestProjectPK;
	}

	public @Course.PK int getLastRequestCoursePK() {
		return lastRequestCoursePK;
	}

	public Timestamp getLastJob() {
		return lastJob;
	}

	public Timestamp getLastSuccess() {
		return lastSuccess;
	}

	
	public String getLoad() {
		return load;
	}
	
	public TestRun.Kind getKind() {
		return kind;
	}

	public static void submissionRequestedNoneAvailable(Connection conn, String name,
			String remoteHost, @CheckForNull String courses, String javaVersion,
			Timestamp lastRequest, String load) throws SQLException {
		String query = Queries.makeInsertOrUpdateStatement(new String[] {
				"name", "remote_host", "courses", "java_version", "last_request", 
				"last_request_submission_pk", "system_load", "kind"},
				TABLE_NAME);
		PreparedStatement stmt = conn.prepareStatement(query);
		try {
			Queries.setStatement(stmt, name, remoteHost, courses, javaVersion, lastRequest, 0,
					load, TestRun.Kind.UNKNOWN, remoteHost, courses, javaVersion, lastRequest, 0,
					load, TestRun.Kind.UNKNOWN);
			stmt.executeUpdate();
		} finally {
			Queries.closeStatement(stmt);
		}
	}
	public static void submissionRequestedAndProvided(Connection conn, String name,
			String remoteHost, @CheckForNull String courses, String javaVersion,
			Timestamp now, String load, Submission submission, TestRun.Kind kind) throws SQLException {
		String query = Queries.makeInsertOrUpdateStatement(new String[] {
				"name", "remote_host", "courses", "java_version", "last_request", "last_request_submission_pk", "last_job", "system_load", "kind" },
				TABLE_NAME);
		PreparedStatement stmt = conn.prepareStatement(query);
		try {
			Queries.setStatement(stmt, name, remoteHost, courses, javaVersion, now,submission.getSubmissionPK(), now,load, kind,
					remoteHost, courses, javaVersion, now, submission.getSubmissionPK(), now,load, kind);
			stmt.executeUpdate();
		} finally {
			Queries.closeStatement(stmt);
		}
	}

	public static void insertOrUpdateSuccess(Connection conn, String name,
			String remoteHost, 
			Timestamp now, String load, Submission submission)
			throws SQLException {
		String query = Queries.makeInsertOrUpdateStatement(
				new String[] {"name", "remote_host","last_success" ,"last_built_submission_pk", "last_request_submission_pk", "system_load", "kind", "courses", "last_request" }, 
				new String[] {"remote_host", "last_success","last_built_submission_pk", "last_request_submission_pk", "system_load", "kind"}, 
				TABLE_NAME);
		@Submission.PK int submissionPK = submission.getSubmissionPK();
		PreparedStatement stmt = conn.prepareStatement(query);
		try {
			Queries.setStatement(stmt,
					name, remoteHost, now, submissionPK, 0, load, TestRun.Kind.UNKNOWN, "", now,
					 remoteHost, now, submissionPK,0, load, TestRun.Kind.UNKNOWN);
			stmt.executeUpdate();
		} finally {
			Queries.closeStatement(stmt);
		}
	}
	
	   public static void updateLastTestRun(Connection conn, String testMachine, TestRun testRun) throws SQLException {
	       
	       String query = "UPDATE " + TABLE_NAME + " SET last_testrun_pk = ? WHERE name= ?";
	       PreparedStatement stmt =  Queries.setStatement(conn, query, testRun.getTestRunPK(), testMachine);
	        try {
	            stmt.executeUpdate();
	        } finally {
	            Queries.closeStatement(stmt);
	        }
	   }
	        
    public static Collection<BuildServer> getAll(Connection conn)
            throws SQLException {
        String query = " SELECT "
                + ATTRIBUTES
                + ", submissions.project_pk, projects.course_pk FROM "
                + TABLE_NAME
                + ","
                + Submission.TABLE_NAME
                + ","
                + Project.TABLE_NAME
                + " WHERE buildservers.last_request_submission_pk = submissions.submission_pk "
                + " AND submissions.project_pk = projects.project_pk ";
        PreparedStatement stmt = conn.prepareStatement(query);
        Collection<BuildServer> collection = new TreeSet<BuildServer>();
        long recent = System.currentTimeMillis()
                - TimeUnit.MILLISECONDS.convert(40, TimeUnit.MINUTES);

        addRecentBuildServers(stmt, collection, recent);
        query = " SELECT " + ATTRIBUTES + " FROM " + TABLE_NAME
                + " WHERE buildservers.last_request_submission_pk = ? ";
        stmt = conn.prepareStatement(query);
        stmt.setInt(1, 0);
        addRecentBuildServers(stmt, collection, recent);

        return collection;
    }

    private static void addRecentBuildServers(PreparedStatement stmt,
            Collection<BuildServer> collection, long recent)
            throws SQLException {
        ResultSet rs = stmt.executeQuery();
		
		while (rs.next()) {
        	BuildServer bs = new BuildServer(rs, 1);
        	Timestamp lastRequest = bs.getLastRequest();
        	Timestamp lastSuccess = bs.getLastSuccess();
        	if (lastRequest.getTime() > recent || lastSuccess != null && lastSuccess.getTime() > recent)
        	  collection.add(bs);
        }
        rs.close();
		stmt.close();
    }

    public static Collection<BuildServer> getAll(Course course, String universalBuilderserverKey, Connection conn)
			throws SQLException {
		Collection<BuildServer> collection = getAll(conn);
		Collection<BuildServer> result = new TreeSet<BuildServer>();
		
		for (BuildServer bs : collection) {
		    if (bs.canBuild(course, universalBuilderserverKey))
				result.add(bs);
		}
		return result;

	}

	@Override
	public int compareTo(BuildServer that) {
		int result = that.lastRequest.compareTo(this.lastRequest);
		if (result != 0)
			return result;
		result = this.name.compareTo(that.name);
		if (result != 0)
			return result;
		return this.buildserverPK - that.buildserverPK;

	}

 
}
