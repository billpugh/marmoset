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
 * Created on Feb 2, 2005
 */
package edu.umd.cs.submitServer.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.umd.cs.marmoset.modelClasses.Project;
import edu.umd.cs.marmoset.modelClasses.TestSetup;
import edu.umd.cs.submitServer.ClientRequestException;
import edu.umd.cs.submitServer.RequestParser;

/**
 * @author jspacco
 * 
 */
public class ChangeTestSetupStatus extends SubmitServerServlet {


	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO make this a post method and make the link to it a form button
		Connection conn = null;
		boolean transactionSuccess = false;
		try {
			conn = getConnection();
			TestSetup testSetup = (TestSetup) request.getAttribute("testSetup");
			Project project = (Project) request.getAttribute("project");
			RequestParser parser = new RequestParser(request,
					getSubmitServerServletLog(), strictParameterChecking());
			TestSetup.Status status = TestSetup.Status.valueOfAnyCase(parser.getCheckedParameter("status"));

			conn.setAutoCommit(false);
			/*
			 * 20090608: changed TRANSACTION_READ_COMMITTED to
			 * TRANSACTION_REPEATABLE_READ to make transaction compatible with
			 * innodb in MySQL 5.1, which defines READ_COMMITTED as unsafe for
			 * use with standard binary logging. For more information, see:
			 * <http
			 * ://dev.mysql.com/doc/refman/5.1/en/set-transaction.html#isolevel_read
			 * -committed>
			 */
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			if (status.equals(TestSetup.Status.INACTIVE)) {
				// Can only mark something INACTIVE that was previously ACTIVE
				if (testSetup.hasStatus(TestSetup.Status.ACTIVE)) {
					testSetup.setStatus(TestSetup.Status.INACTIVE);
					testSetup.update(conn);
					project.setTestSetupPK(0);
					project.update(conn);
				}
			} else if (status.equals(TestSetup.Status.BROKEN)
					&& testSetup.hasStatus(TestSetup.Status.FAILED, TestSetup.Status.TESTED, TestSetup.Status.INACTIVE, 
					        TestSetup.Status.PENDING, TestSetup.Status.NEW)) {
				// Can mark broken only if current state is:
				// FAILED, TESTED, INACTIVE, or PENDING
				// If you mark something BROKEN that was pending, it might
				// re-appear
				// if the buildserver was stalled.
				testSetup.setStatus(status);
				testSetup.update(conn);
			} else if (status.equals(TestSetup.Status.NEW)
                    && testSetup.hasStatus(TestSetup.Status.FAILED, TestSetup.Status.TESTED, TestSetup.Status.PENDING)) {
                testSetup.setStatus(status);
                testSetup.update(conn);
            }

			conn.commit();
			transactionSuccess = true;
			String url = request.getContextPath()
					+ "/view/instructor/projectUtilities.jsp?projectPK="
					+ testSetup.getProjectPK();
			response.sendRedirect(url);
		} catch (ClientRequestException e) {
			response.sendError(e.getErrorCode(), e.getMessage());
			return;
		} catch (SQLException e) {
			handleSQLException(e);
			throw new ServletException(e);
		} finally {
			rollbackIfUnsuccessfulAndAlwaysReleaseConnection(
					transactionSuccess, request, conn);
		}
	}
}
