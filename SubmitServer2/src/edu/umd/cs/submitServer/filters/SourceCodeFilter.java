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
 * Created on Apr 8, 2005
 */
package edu.umd.cs.submitServer.filters;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.CheckForNull;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.umd.cs.marmoset.codeCoverage.CodeCoverageResults;
import edu.umd.cs.marmoset.modelClasses.JUnitTestProperties;
import edu.umd.cs.marmoset.modelClasses.TestOutcome;
import edu.umd.cs.marmoset.modelClasses.TestOutcome.TestType;
import edu.umd.cs.marmoset.modelClasses.TestOutcomeCollection;
import edu.umd.cs.marmoset.modelClasses.TestProperties;
import edu.umd.cs.submitServer.RequestParser;
import edu.umd.cs.submitServer.SubmitServerConstants;
import edu.umd.cs.submitServer.UserSession;

/**
 * @author jspacco
 * 
 */
public class SourceCodeFilter extends SubmitServerFilter {

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 * javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		HttpSession session = request.getSession();
		UserSession userSession = (UserSession) session
				.getAttribute(USER_SESSION);
		Connection conn = null;
		try {
			conn = getConnection();

			RequestParser parser = new RequestParser(request,
					getSubmitServerFilterLog(), strictParameterChecking());
			String sourceFileName = parser
					.getOptionalCheckedParameter("sourceFileName");

			Integer startHighlight = parser
					.getOptionalInteger("startHighlight");
			Integer numToHighlight = parser
					.getOptionalInteger("numToHighlight");
			Integer numContext = parser.getOptionalInteger("numContext");

			request.setAttribute("sourceFileName", sourceFileName);
			request.setAttribute("startHighlight", startHighlight);
			request.setAttribute("numToHighlight", numToHighlight);
			request.setAttribute("numContext", numContext);

			// NAT Get tab width parameter
			Integer tabWidth = parser.getOptionalInteger("tabWidth");
			// Make tab width persist unless it is overwritten by the value
			// above
			if (false) {
				Object tabWidthObj = request.getAttribute("tabWidth");
				if (tabWidthObj != null && (tabWidth == null || tabWidth < 1)) {
					try {
						tabWidth = (Integer) tabWidthObj;
					} catch (RuntimeException e) {
					} // do nothing
				}
			}
			request.setAttribute("tabWidth", tabWidth);

			TestProperties testProperties = (TestProperties) request
					.getAttribute("testProperties");

			// Instructors can see code coverage results for any class of tests
			// Students can only see coverage results for student/public tests

			String testTypeString =  parser.getOptionalCheckedParameter(TEST_TYPE);
			 
			@CheckForNull TestType testType = TestType.valueOfAnyCaseOrNull(testTypeString);
			       
			String testNumber = parser.getOptionalCheckedParameter(TEST_NUMBER);
			String hybridTestType = parser
					.getOptionalCheckedParameter(HYBRID_TEST_TYPE);
			// Setting test type and testNumber as request attributes so that
			// JSPs can use them
			// more easily
			if (testType != null) 
			    request.setAttribute(TEST_TYPE, testType);
			request.setAttribute(TEST_TYPE_STRING, testTypeString);
			request.setAttribute(TEST_NUMBER, testNumber);
			request.setAttribute(HYBRID_TEST_TYPE, hybridTestType);
			TestOutcomeCollection currentTestOutcomes = (TestOutcomeCollection) request
					.getAttribute("testOutcomeCollection");
			CodeCoverageResults codeCoverageResults = null;

			if (testType != null )
			try {
				// Instructor's view of coverage
				if (testProperties != null && testProperties.isJava()
						&& userSession.canActivateCapabilities()
						&& ((JUnitTestProperties)testProperties).isPerformCodeCoverage()) {
					if (TestOutcome.TestType.FINDBUGS.equals(testType)
							|| TestOutcome.TestType.UNCOVERED_METHOD.equals(testType)) {
						// display union of public and student tests
						codeCoverageResults = currentTestOutcomes
								.getOverallCoverageResultsForPublicAndStudentTests();
					} else if ("all".equals(testNumber) || testNumber == null) {
						if (TestOutcome.TestType.STUDENT.equals(testType)) {
							// Get coverage for all the student tests
							codeCoverageResults = currentTestOutcomes
									.getOverallCoverageResultsForStudentTests();
							getSubmitServerFilterLog().trace(
									"instructor " + testType + ", "
											+ testNumber);
						} else if (TestOutcome.TestType.PUBLIC.equals(testType)) {
							// Get coverage for all the public tests.
							codeCoverageResults = currentTestOutcomes
									.getOverallCoverageResultsForPublicTests();
							getSubmitServerFilterLog().trace(
									"instructor " + testType + ", "
											+ testNumber);
						} else if (TestOutcome.TestType.RELEASE.equals(testType)) {
							// Get coverage for all the release tests.
							codeCoverageResults = currentTestOutcomes
									.getOverallCoverageResultsForReleaseTests();
							getSubmitServerFilterLog().trace(
									"instructor " + testType + ", "
											+ testNumber);
						} else if (TestOutcome.TestType.SECRET.equals(testType)) {
							// Get coverage for all the secret tests.
							codeCoverageResults = currentTestOutcomes
									.getOverallCoverageResultsForSecretTests();
							getSubmitServerFilterLog().trace(
									"instructor " + testType + ", "
											+ testNumber);
						} else if (SubmitServerConstants.RELEASE_UNIQUE
								.equals(testTypeString)) {
							// Get coverage for all the release tests
							// excluding anything covered by a public or student
							// test
							codeCoverageResults = currentTestOutcomes
									.getOverallCoverageResultsForCardinalTests();
							CodeCoverageResults publicStudentCoverage = currentTestOutcomes
									.getOverallCoverageResultsForPublicAndStudentTests();
							codeCoverageResults
									.excluding(publicStudentCoverage);
							getSubmitServerFilterLog().trace(
									"instructor " + testType + ", "
											+ testNumber);
						} else if (SubmitServerConstants.PUBLIC_STUDENT
								.equals(testTypeString)) {
							codeCoverageResults = currentTestOutcomes
									.getOverallCoverageResultsForPublicAndStudentTests();
							getSubmitServerFilterLog().trace(
									"instructor " + testType + ", "
											+ testNumber);
						} else if (SubmitServerConstants.CARDINAL
								.equals(testTypeString)) {
							codeCoverageResults = currentTestOutcomes
									.getOverallCoverageResultsForCardinalTests();
							request.setAttribute(
									SubmitServerConstants.TEST_TYPE,
									"public/release/secret");
							getSubmitServerFilterLog().trace(
									"instructor " + testType + ", "
											+ testNumber);
						} else {
							// TODO Create a link so instructors can get all
							// test results
							// TODO Handle combinations of test cases
							codeCoverageResults = currentTestOutcomes
									.getOverallCoverageResultsForCardinalTests();
							request.setAttribute(
									SubmitServerConstants.TEST_TYPE,
									"public/release/secret");
							getSubmitServerFilterLog().trace(
									"instructor " + testType + ", "
											+ testNumber);
						}
					} else {
						if (testNumber != null && testType != null) {
							if (RELEASE_UNIQUE.equals(hybridTestType)) {
								// // For release-unique, exclude coverage for
								// all public/student written tests
								codeCoverageResults = currentTestOutcomes
										.getOutcomeByTestTypeAndTestNumber(
												TestOutcome.TestType.RELEASE,
												testNumber)
										.getCodeCoverageResults();
								codeCoverageResults
										.excluding(currentTestOutcomes
												.getOverallCoverageResultsForPublicAndStudentTests());
							} else if (FAILING_ONLY.equals(hybridTestType)) {
								// For failing-only, exclude coverage by all
								// passing tests
								codeCoverageResults = currentTestOutcomes
										.getOutcomeByTestTypeAndTestNumber(
												testType, testNumber)
										.getCodeCoverageResults();
								codeCoverageResults
										.excluding(currentTestOutcomes
												.getOverallCoverageResultsForAllPassingTests());
							} else {
								// otherwise just get coverage for the request
								// test
								codeCoverageResults = currentTestOutcomes
										.getOutcomeByTestTypeAndTestNumber(
												testType, testNumber)
										.getCodeCoverageResults();
							}
							getSubmitServerFilterLog().trace(
									"instructor specific " + testType + ", "
											+ testNumber);
						}
					}
				} else if (testProperties != null && testProperties.isJava()
						&& !userSession.canActivateCapabilities()
						&& testProperties.isPerformCodeCoverage()) {

					getSubmitServerFilterLog().trace(
							"student: " + testType + ", " + testNumber);

					// TODO students should be allowed to see the combination of
					// student/public tests
					if (TestOutcome.TestType.FINDBUGS.equals(testType)
							|| TestOutcome.TestType.UNCOVERED_METHOD.equals(testType)) {
						// for FindBugs or UncoveredMethod, show coverage for
						// all public/student tests
						codeCoverageResults = currentTestOutcomes
								.getOverallCoverageResultsForPublicAndStudentTests();
					} else if ("all".equals(testNumber)) {
						if (TestOutcome.TestType.STUDENT.equals(testType)) {
							// Get coverage for all the student tests
							codeCoverageResults = currentTestOutcomes
									.getOverallCoverageResultsForStudentTests();
							getSubmitServerFilterLog().trace(
									"student " + testType + ", " + testNumber);
						} else if (TestOutcome.TestType.PUBLIC.equals(testType)) {
							// Get coverage for all the public tests
							codeCoverageResults = currentTestOutcomes
									.getOverallCoverageResultsForPublicTests();
							getSubmitServerFilterLog().trace(
									"public " + testType + ", " + testNumber);
						} else if (SubmitServerConstants.PUBLIC_STUDENT
								.equals(testTypeString)) {
							// Get coverage for all the public tests
							codeCoverageResults = currentTestOutcomes
									.getOverallCoverageResultsForPublicAndStudentTests();
							getSubmitServerFilterLog().trace(
									"public-student " + testType + ", "
											+ testNumber);
						} else {
							getSubmitServerFilterLog().error(
									"student: bad testType: " + testType);
						}
					} else {
						if (TestOutcome.TestType.STUDENT.equals(testType)
								|| TestOutcome.TestType.PUBLIC.equals(testType)) {
							codeCoverageResults = currentTestOutcomes
									.getOutcomeByTestTypeAndTestNumber(
											testType, testNumber)
									.getCodeCoverageResults();
							getSubmitServerFilterLog().trace(
									"specific student " + testType + ", "
											+ testNumber);
						}
					}
				}
			} catch (Exception e) {
				String testNumberStr = (testNumber == null ? "null"
						: testNumber);
				String testTypeStr = (testType == null ? "null" : testType.toString());
				String instructor = Boolean.toString(userSession
						.canActivateCapabilities());
				getSubmitServerFilterLog().error(
						"Error getting code coverage (testType:\""
								+ testTypeStr + "\", testNumber:\""
								+ testNumberStr + "\", instructor:\""
								+ instructor + "\")", e);
			}
			if (codeCoverageResults != null)
				request.setAttribute("codeCoverageResults", codeCoverageResults);
		} catch (SQLException e) {
			throw new ServletException(e);
		} finally {
			releaseConnection(conn);
		}
		chain.doFilter(request, response);
	}

}
