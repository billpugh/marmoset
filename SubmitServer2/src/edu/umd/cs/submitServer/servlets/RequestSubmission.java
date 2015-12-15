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
 * Created on Jan 15, 2005
 *
 * @author jspacco
 */
package edu.umd.cs.submitServer.servlets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.annotation.CheckForNull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.umd.cs.marmoset.modelClasses.BuildServer;
import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.marmoset.modelClasses.HttpHeaders;
import edu.umd.cs.marmoset.modelClasses.IO;
import edu.umd.cs.marmoset.modelClasses.Project;
import edu.umd.cs.marmoset.modelClasses.Queries;
import edu.umd.cs.marmoset.modelClasses.StudentSubmitStatus;
import edu.umd.cs.marmoset.modelClasses.Submission;
import edu.umd.cs.marmoset.modelClasses.Submission.BuildStatus;
import edu.umd.cs.marmoset.modelClasses.TestRun.Kind;
import edu.umd.cs.marmoset.modelClasses.TestSetup;
import edu.umd.cs.submitServer.MultipartRequest;
import edu.umd.cs.submitServer.WebConfigProperties;
import edu.umd.cs.submitServer.filters.SubmitServerFilter;
import edu.umd.cs.submitServer.util.WaitingBuildServer;

/**
 * @author jspacco
 * 
 */
public class RequestSubmission extends SubmitServerServlet {
  private static final WebConfigProperties webProperties = WebConfigProperties.get();

  static HashSet<String> complainedAbout = new HashSet<String>();

  private static final int MAX_BUILD_DURATION_MINUTES = 3;

  private static Lock lock = new java.util.concurrent.locks.ReentrantLock();

  public static void timeDump(StringBuffer buf, Timestamp started, String f, Object... args) {
    long now = System.currentTimeMillis();
    long delay = now - started.getTime();
    buf.append(String.format("%4d ", delay));
    buf.append(String.format(f, args));

  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    throw new ServletException();

  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    Connection conn = null;
    // Create model objects to represent a submission and
    // auxiliary info
    Submission submission = new Submission();
    TestSetup testSetup = new TestSetup();
    MultipartRequest multipartRequest = (MultipartRequest) request.getAttribute(MULTIPART_REQUEST);
    Timestamp now = new Timestamp(System.currentTimeMillis());

    String submissionPK = multipartRequest.getOptionalCheckedParameter("submissionPK");
    String testSetupPK = multipartRequest.getOptionalCheckedParameter("testSetupPK");

    String hostname = multipartRequest.getOptionalStringParameter("hostname");
    String load = multipartRequest.getOptionalStringParameter("load");
    if (load == null)
      load = "unknown";
    String javaVersion = multipartRequest.getOptionalStringParameter("javaVersion");
    
    String projectNumber = multipartRequest.getOptionalStringParameter("projectNumber");

    Kind kind = Kind.UNKNOWN;
    Queries.RetestPriority foundPriority = null;
    String courseKey = multipartRequest.getStringParameter("courses").trim();
    String remoteHost = SubmitServerFilter.getRemoteHost(request);
    int connectionTimeout = multipartRequest.getOptionalIntParameter("connectionTimeout", 4000);

    try {

      /**
       * We are going to use Java static locking as well as database locking. If
       * requests from two build servers come in at the same time, we'd just
       * wind up trying to assign them the same submission and having to roll
       * back one. Better to just do them one at a time.
       */
      StringBuffer buf = new StringBuffer();

      boolean locked = lock.tryLock(100, TimeUnit.MILLISECONDS);
      if (!locked) {
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, SUBMIT_SERVER_BUSY);
        return;
      }
      try {
        conn = getConnection();
        Collection<Integer> allowedCourses = getCourses(conn, courseKey);

        if (allowedCourses.isEmpty()) {
          if (isUniversalBuildServer(courseKey)) {
            BuildServer.submissionRequestedNoneAvailable(conn, hostname, remoteHost, courseKey, javaVersion, now, load);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, NO_SUBMISSIONS_AVAILABLE_MESSASGE);
            return;
          }

          String msg = "host " + hostname + "; no courses match " + courseKey;
          getSubmitServerServletLog().warn(msg);
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No courses match " + courseKey);
          return;
        }

        if (submissionPK != null) {
          @Submission.PK
          Integer subPK = Submission.asPK(Integer.valueOf(submissionPK));
          submission = Submission.lookupBySubmissionPK(Submission.asPK(subPK), conn);
          Project project = Project.lookupByProjectPK(submission.getProjectPK(), conn);
          if (testSetupPK != null) {
            // test setup specified by buildserver
            testSetup = TestSetup.lookupByTestSetupPK(Integer.valueOf(testSetupPK), conn);
            if (testSetup != null && testSetup.getProjectPK() != submission.getProjectPK())
              throw new ServletException("Submission " + submissionPK + " and test setup " + testSetupPK
                  + " are for different projects");
          } else
            testSetup = TestSetup.lookupByTestSetupPK(project.getTestSetupPK(), conn);
          if (testSetup == null)
            testSetup = TestSetup.lookupRecentNonBrokenTestSetupForProject(conn, project.getProjectPK());
          if (testSetup != null) {
            if (project.getTestSetupPK() == 0)
              kind = Kind.SPECIFIC_REQUEST_NEW_TESTUP;
            else
              kind = Kind.SPECIFIC_REQUEST;
          } else {
            kind = Kind.SPECIFIC_REQUEST_NO_TESTSETUP;
          }
        } else if (projectNumber != null) {
          if (allowedCourses.size() != 1)
            throw new ServletException("Can only specify a single course when specifying a project number");
          Integer coursePK = Course.asPK(allowedCourses.iterator().next());
          Project project = Project.lookupByCourseAndProjectNumber(coursePK, projectNumber, conn);
          kind = Kind.PROJECT_RETEST;
          if (!Queries.lookupOldestSubmissionForProject(conn, submission, testSetup, project)) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, NO_SUBMISSIONS_AVAILABLE_MESSASGE);
            return;
          }
        } else {

          findSubmission: {

            if (logNewTestSetups(conn, submission, testSetup, allowedCourses, MAX_BUILD_DURATION_MINUTES)
                && Queries.lookupNewTestSetup(conn, submission, testSetup, allowedCourses, MAX_BUILD_DURATION_MINUTES)) {
              if (submission.getNumPendingBuildRequests() > 3) {
                submission.setBuildStatus(Submission.BuildStatus.BROKEN);
                submission.update(conn);
              } else {
                kind = Kind.NEW_TEST_SETUP;
                break findSubmission;
              }
            }
            for (Queries.RetestPriority priority : Queries.RetestPriority.values()) {

              timeDump(buf, now, "%s%n", priority);
              foundPriority = priority;
              // most_recent submission without any test run,
              // oldest first
              kind = Kind.BUILD_STATUS_NEW;
              if (Queries.lookupSubmission(conn, submission, testSetup, allowedCourses, BuildStatus.NEW, priority))
                break findSubmission;

              kind = Kind.EXPLICIT_RETESTS;
              // look for explicit retests
              if (Queries.lookupSubmission(conn, submission, testSetup, allowedCourses, BuildStatus.RETEST, priority))
                break findSubmission;

              kind = Kind.OUT_OF_DATE_TEST_SETUPS;
              // look for out of date test setups
              if (Queries
                  .lookupSubmissionWithOutdatedTestResults(conn, submission, testSetup, allowedCourses, priority))
                break findSubmission;

              kind = Kind.BUILD_STATUS_NEW;

              // now look for pending submissions
              if (Queries.lookupPendingSubmission(conn, submission, testSetup, allowedCourses,
                  MAX_BUILD_DURATION_MINUTES, priority)) {
                if (submission.getNumPendingBuildRequests() <= 3)
                  break findSubmission;

                // TODO: RSS Entry:
                submission.setBuildStatus(Submission.BuildStatus.BROKEN);
                submission.update(conn);
              }

            }

            // *) look for ambient background retest
            if (isBackgroundRetestingEnabled()
                && Queries.lookupSubmissionForBackgroundRetest(conn, submission, testSetup, allowedCourses)) {
              kind = Kind.BACKGROUND_RETEST;
              break findSubmission;
            }

            BuildServer.submissionRequestedNoneAvailable(conn, hostname, remoteHost, courseKey, javaVersion, now, load);
            int waitFor = connectionTimeout - 1000;
            if (waitFor > 1000) {

              // OK, want to pause and wait for something to come in.
              lock.unlock();
              locked = false;
              releaseConnection(conn);
              conn = null;
              submission = WaitingBuildServer.waitForSubmission(allowedCourses, waitFor);
              if (submission != null) {
                locked = lock.tryLock(500, TimeUnit.MILLISECONDS);
                if (!locked) {

                  getSubmitServerServletLog().trace(
                      "RequestSubmission: unable to reacquire lock to verify " + submission.getSubmissionPK()
                          + ", projectPK =  " + submission.getProjectPK());
                  submission = null;

                } else {
                  conn = getConnection();
                  submission = Submission.lookupBySubmissionPK(submission.getSubmissionPK(), conn);
                  Project project = Project.lookupByProjectPK(submission.getProjectPK(), conn);
                  boolean canonicalSubmission = submission.getStudentRegistrationPK() == project
                      .getCanonicalStudentRegistrationPK();
                  if (submission.getBuildStatus() != Submission.BuildStatus.BROKEN && canonicalSubmission) {
                    testSetup = TestSetup.lookupRecentNonBrokenTestSetupForProject(conn, project.getProjectPK());
                    if (testSetup != null
                        && (testSetup.getStatus() == TestSetup.Status.NEW || testSetup.getStatus() == TestSetup.Status.FAILED)) {
                      kind = Kind.NEW_TEST_SETUP;
                      break findSubmission;
                    }
                  }
                  if (submission.getBuildStatus() == Submission.BuildStatus.NEW) {
                    getSubmitServerServletLog().trace(
                        "RequestSubmission: used WaitingBuildServer to get submission pk = "
                            + submission.getSubmissionPK() + ", projectPK =  " + submission.getProjectPK());
                    kind = Kind.BUILD_STATUS_NEW;
                    testSetup = TestSetup.lookupCurrentTestSetupForProject(conn, submission.getProjectPK());
                    if (testSetup != null)
                      break findSubmission;

                  }
                }
              }
            }

            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, NO_SUBMISSIONS_AVAILABLE_MESSASGE);
            return;

          }

        }

        timeDump(buf, now, "%s %s%n", kind, foundPriority);
        getSubmitServerServletLog().warn(
            "RequestSubmission: got " + kind + " submission pk = " + submission.getSubmissionPK() + ", projectPK =  "
                + submission.getProjectPK());

        // at this point, either submission and testSetup are
        // non-null or we've already sent an error

        // Tell client not to cache this object.
        // See http://www.jguru.com/faq/view.jsp?EID=377
        Util.setNoCache(response);

        // Send submission PK in an HTTP header.
        response.setHeader(HttpHeaders.HTTP_SUBMISSION_PK_HEADER, submission.getSubmissionPK().toString());

        response.setHeader(HttpHeaders.HTTP_KIND_HEADER, kind.name());
        if (foundPriority != null)
          response.setHeader(HttpHeaders.HTTP_PRIORITY_HEADER, foundPriority.name());
        response.setHeader(HttpHeaders.HTTP_PREVIOUS_BUILD_STATUS_HEADER, submission.getBuildStatus().name());

        // Send project PK in an HTTP header.
        boolean hasTestSetup = testSetup != null && testSetup.getTestSetupPK() != null;
        if (hasTestSetup)
          response.setHeader(HttpHeaders.HTTP_TEST_SETUP_PK_HEADER, testSetup.getTestSetupPK().toString());
        else
          response.setHeader(
              HttpHeaders.HTTP_HUH_HEADER,
              String.format("submission %d, project %d, no test setup", submission.getSubmissionPK(),
                  submission.getProjectPK()));

        // if we found a new test setup, let the buildserver know
        if (hasTestSetup && (kind == Kind.NEW_TEST_SETUP || kind == Kind.SPECIFIC_REQUEST_NEW_TESTUP)) {
          response.setHeader(HttpHeaders.HTTP_NEW_TEST_SETUP, "yes");
          // set the status to 'pending' and set the current time for
          // datePosted
          // date_posted is a horrible name, this is the field I'm
          // using for timeouts
          testSetup.setStatus(TestSetup.Status.PENDING);
          testSetup.setDatePosted(new Timestamp(System.currentTimeMillis()));
          testSetup.update(conn);
        } else
          response.setHeader(HttpHeaders.HTTP_NEW_TEST_SETUP, "no");

        if (kind.isBackgroundRetest())
          response.setHeader(HttpHeaders.HTTP_BACKGROUND_RETEST, "yes");
        else {
          response.setHeader(HttpHeaders.HTTP_BACKGROUND_RETEST, "no");
          // If this is *NOT* a background re-test then
          // update build_status to pending

          submission.setBuildStatus(Submission.BuildStatus.PENDING);

        }
        // in any case, increment number of pending build requests
        if (submission.getNumPendingBuildRequests() > 0)
          getSubmitServerServletLog().warn(
              "submission " + submission.getSubmissionPK() + " already has " + submission.getNumPendingBuildRequests()
                  + " pending build requests");

        submission.incrementNumPendingBuildRequests();
        Timestamp buildRequestTimestamp = new Timestamp(System.currentTimeMillis());
        submission.setBuildRequestTimestamp(buildRequestTimestamp);

        submission.update(conn);
        if (!kind.isBackgroundRetest())
          StudentSubmitStatus.updateLastBuildRequest(submission.getStudentRegistrationPK(), 
            submission.getProjectPK(), 
            buildRequestTimestamp, conn);

      } finally {
        if (locked)
          lock.unlock();
      }
      // if communication with the buildserver fails after this
      // then we'll wait for things to time out
      // prepare byte array input stream of the submission
      byte[] bytes = submission.downloadArchive(conn);
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

      // Inform client of file length
      response.setContentLength(bytes.length);

      // Inform client of content type
      response.setContentType("application/x-zip");

      // TODO: maybe we should send the md5sum?

      OutputStream out = response.getOutputStream();
      IO.copyStream(bais, out);
      BuildServer.submissionRequestedAndProvided(conn, hostname, remoteHost, courseKey, javaVersion, now, load, submission, kind);
    } catch (SQLException e) {
      handleSQLException(e);
      throw new ServletException(e);
    } catch (InterruptedException e) {
      response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "request interrupted");
    } finally {
      releaseConnection(conn);
      response.getOutputStream().close();
    }
    getSubmitServerServletLog().info("Completed RequestSubmission");
  }

  public static boolean isUniversalBuildServer(String courses) {
    @CheckForNull
    String universalBuilderserver = webProperties.getProperty("buildserver.password.universal");

    return universalBuilderserver != null && courses.startsWith(universalBuilderserver + "-");
  }

  public static String courseKeysExcludedFromUniversal(String courseKey) {
    String universalBuilderserver = webProperties.getProperty("buildserver.password.universal");

    if (universalBuilderserver == null)
      throw new NullPointerException();

    return courseKey.substring(universalBuilderserver.length() + 1);
  }

  public static Collection<Integer> getCourses(Connection conn, String courses) throws SQLException {
    Collection<Integer> allowedCourses;

    if (isUniversalBuildServer(courses))
      allowedCourses = Course.lookupAllPKButByBuildserverKey(conn, courseKeysExcludedFromUniversal(courses));
    else
      allowedCourses = Course.lookupAllPKByBuildserverKey(conn, courses);
    return allowedCourses;
  }

  public static Map<String, Course> getCourseMap(Connection conn, String courses) throws SQLException {
    @CheckForNull
    String universalBuilderserver = webProperties.getProperty("buildserver.password.universal");

    if (universalBuilderserver != null && courses.startsWith(universalBuilderserver + "-"))
      return Course.lookupAllButByBuildserverKey(conn, courses.substring(universalBuilderserver.length() + 1));

    else
      return Course.lookupAllByBuildserverKey(conn, courses);

  }

  static final String NO_SUBMISSIONS_AVAILABLE_MESSASGE = "No submissions available";
  static final String SUBMIT_SERVER_BUSY = "Submit server busy with other build server";

  private boolean performBackgroundRetesting = false;

  /*
   * (non-Javadoc)
   * 
   * @see edu.umd.cs.submitServer.servlets.SubmitServerServlet#init()
   */
  @Override
  public void init() throws ServletException {
    // TODO Auto-generated method stub
    super.init();
    String performBackgroundRetestingStr = webProperties.getProperty("perform.background.retesting");
    if ("true".equals(performBackgroundRetestingStr))
      performBackgroundRetesting = true;
  }

  private boolean isBackgroundRetestingEnabled() {
    return performBackgroundRetesting;
  }

  public static boolean logNewTestSetups(Connection conn, Submission submission, TestSetup testSetup,
      Collection<Integer> allowedCourses, int maxBuildDurationInMinutes) throws SQLException {
    if (allowedCourses.isEmpty())
      throw new IllegalArgumentException();
    // SQL timestamp of build requests that have,
    // as of this moment, taken too long.
    Timestamp buildTimeout = new Timestamp(System.currentTimeMillis() - (maxBuildDurationInMinutes * 60L * 1000L));

    String courseRestrictions = Queries.makeCourseRestrictionsWhereClause(allowedCourses);

    String query = " SELECT "
        + "submissions.submission_pk, test_setups.test_setup_pk, projects.project_pk, projects.course_pk"
        + " FROM submissions, test_setups, projects " + " WHERE (" + "      (test_setups.jarfile_status = ?) "
        + "      OR (test_setups.jarfile_status = ? AND test_setups.date_posted < ?)" + "       ) "
        + courseRestrictions + " AND test_setups.project_pk = projects.project_pk "
        + " AND submissions.project_pk = projects.project_pk " + " AND submissions.most_recent = 1 "
        + " AND submissions.build_status != ? "
        + " AND submissions.student_registration_pk = projects.canonical_student_registration_pk "
        + " ORDER BY submissions.submission_number DESC ";
    PreparedStatement stmt = conn.prepareStatement(query);
    stmt.setString(1, TestSetup.Status.NEW.toString());
    stmt.setString(2, TestSetup.Status.PENDING.toString());
    stmt.setTimestamp(3, buildTimeout);
    stmt.setString(4, Submission.BuildStatus.BROKEN.toString());

    ResultSet rs = stmt.executeQuery();

    int count = 0;
    while (rs.next()) {
      int submissionPK = rs.getInt(1);
      int testSetupPK = rs.getInt(2);
      int projectPK = rs.getInt(3);
      int coursePK = rs.getInt(4);
      getSubmitServerServletLog().info(
          String.format("new test setup available: course %d, project %d, test setup %d, submission %d", coursePK,
              projectPK, testSetupPK, submissionPK));
      count++;

    }
    if (count > 1)
      getSubmitServerServletLog().warn(String.format("Total of %d new test setups available", count));
    return count > 0;

  }
}
