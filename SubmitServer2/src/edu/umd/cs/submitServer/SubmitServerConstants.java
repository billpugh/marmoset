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
 * Created on Feb 14, 2005
 */

package edu.umd.cs.submitServer;

import edu.umd.cs.submitServer.policy.ChooseLastSubmissionPolicy;

/**
 * @author jspacco
 *
 */
public interface SubmitServerConstants {
	public static final String SNAPSHOT_PASSWORD = "snapshot.password";
	public static final String AUTHENTICATION_LOG = "edu.umd.cs.submitServer.logging.authenticationLog";
	
	public static final String SUBMISSION_LOG = "edu.umd.cs.submitServer.logging.submissionLog";
	public static final String AUTHENTICATION_LDAP_SERVICE = "authentication.ldap.service";

	// SSL enviroment setup keys -- need to explicitly specify cacerts path
	public static final String AUTHENTICATION_KEYSTORE_PATH = "authentication.keystore.path";
	public static final String AUTHENTICATION_KEYSTORE_PASSWORD = "authentication.keystore.password";
	public static final String LDAP_SSL_OFF = "authentication.ldap.SSL.disable";

	// Keys for various LDAP-related parameters
	public static final String LDAP_URL = "authentication.ldap.provider_url";
	public static final String LDAP_AUTH_MECHANISM = "authentication.ldap.authentication_mechanism";
	public static final String LDAP_PRINCIPAL_FORMAT = "authentication.ldap.principal.format";
	public static final String AUTHENTICATION_TYPE = "authentication.type";

	public static final String SKIP_AUTHENTICATION = "authentication.skip";
	public static final String INITIALIZATION_NEEDED = "initializationNeeded";


	// Keys used for setting global properties about the submit server's
	// environment
	public static final String ADMIN_EMAIL = "admin.email";
	public static final String EMAIL_RETURN_ADDRESS = "admin.return_address";
	public static final String SMTP_HOST = "admin.smtp";

	// Keys used for setting/getting http request attributes
	public static final String STUDENT_REGISTRATION_SET = "studentRegistrationSet";
	public static final String STAFF_STUDENT_REGISTRATION_SET = "staffStudentRegistrationSet";
	public static final String JUST_STUDENT_REGISTRATION_SET = "justStudentRegistrationSet";
	public static final String INACTIVE_STUDENT_REGISTRATION_SET = "inactiveStudentRegistrationSet";
  
	public static final String SECTION_MAP = "sectionMap";
	public static final String SECTIONS = "sections";
	public static final String SECTION = "section";
	   
    
	public static final String TEST_PROPERTIES = "testProperties";
	public static final String PROJECT = "project";
	public static final String PROJECT_LIST = "projectList";
	 public static final String UPCOMING_CODE_REVIEW_ASSIGNMENTS = "upcomingCodeReviewAssignments";
	public static final String COURSE = "course";
	public static final String REVIEWER = "reviewer";
	public static final String COURSE_INSTRUCTORS = "courseInstructors";
	public static final String COURSE_IDS = "courseIds";
	public static final String PSEUDO_STUDENT_REGISTRATION = "pseudoStudentRegistration";

	public static final String USER = "user";
	public static final String USER_SESSION = "userSession";
	public static final String STUDENT = "student";
	public static final String TEST_OUTCOME_COLLECTION = "testOutcomeCollection";
	public static final String TEST_RUN = "testRun";
	public static final String SUBMISSION = "submission";
	public static final String TEST_SETUP = "testSetup";


	public static final String REVIEW_ASSIGNMENTS_FOR_PROJECT = "reviewAssignmentsForProject";


	public static final String LAST_LATE = "lastLate";

	public static final String ECLIPSE_SUBMIT_PATH = "/eclipse/SubmitProjectViaEclipse";

	public static final String DEFAULT_BEST_SUBMISSION_POLICY = ChooseLastSubmissionPolicy.class.getName();
	public static final String PUBLIC_STUDENT = "public-student";
	public static final String RELEASE_UNIQUE = "release-unique";
	public static final String CARDINAL = "cardinal";
	public static final String ALL_TESTS = "all";
  public static final String FAILING_ONLY = "failing-only";
	public static final String HYBRID_TEST_TYPE = "hybridTestType";
	public static final String TEST_NUMBER = "testNumber";
	public static final String TEST_TYPE = "testType";
	public static final String TEST_TYPE_STRING = "testTypeString";

	public static final String MULTIPART_REQUEST = "multipartRequest";
	public static final String STUDENT_REGISTRATION = "studentRegistration";
	public static final String STUDENT_SUBMIT_STATUS = "studentSubmitStatus";
	public static final String COURSE_PK = "coursePK";
	public static final String PROJECT_PK = "projectPK";
	public static final String COURSE_KEY = "courseKey";
	public static final String SORT_KEY = "sortKey";
	public static final String SOURCE_FILE_LIST = "sourceFileList";
	public static final String SUBMISSION_LIST = "submissionList";
	public static final String TEST_RUN_LIST = "testRunList";
	public static final String TEST_OUTCOMES_MAP = "testOutcomesMap";
	public static final String INSTRUCTOR_CAPABILITY = "instructorCapability";
	public static final String INSTRUCTOR_ACTION_CAPABILITY = "instructorActionCapability";
	public static final String COURSE_LIST = "courseList";
	/** All current courses that a student is not yet registered for. */
	public static final String OPEN_COURSES = "openCourses";
	public static final String COURSE_MAP = "courseMap";
	public static final String PROJECT_MAP = "projectMap";

	public static final String COURSE_STUDENTREG_LIST = "courseStudentRegList";
	public static final String UPCOMING_PROJECTS = "upcomingProjects";
	public static final String PROJECT_BUILD_STATUS_COUNT = "buildStatusCount";
	public static final String THIS_PROJECT_BUILD_STATUS_COUNT = "projectBuildStatusCount";
    public static final String CODE_REVIEW_ASSIGNMENT = "codeReviewAssignment";
    public static final String NEXT_CODE_REVIEW = "nextCodeReview";
    

  /** Key for accessing OpenID discovery information in a user session. */
  public static final String OPENID_DISCOVERED = "openid.discovered";
  /** Map of AX attributes received from OpenID provider. */
  public static final String OPENID_AX_MAP = "openidAx";
}
