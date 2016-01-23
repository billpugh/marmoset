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
 * Created on Jun 8, 2005
 */
package edu.umd.cs.submitServer.filters;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.umd.cs.marmoset.modelClasses.Project;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration;
import edu.umd.cs.marmoset.modelClasses.StudentSubmitStatus;

public class SubmitStatusFilter extends SubmitServerFilter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        Project project = (Project) request.getAttribute("project");
        StudentRegistration studentRegistration = (StudentRegistration) request.getAttribute(STUDENT_REGISTRATION);
        StudentSubmitStatus submitStatus = (StudentSubmitStatus) request.getAttribute(STUDENT_SUBMIT_STATUS);
        Connection conn = null;
        if (studentRegistration == null || project == null)
            throw new NullPointerException();
       
        if (submitStatus == null) {
            boolean transactionSuccess = false;
            try {
                conn = getConnection();
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

                submitStatus = StudentSubmitStatus.findOrCreate(project.getProjectPK(),
                        studentRegistration.getStudentRegistrationPK(), conn);
                conn.commit();
                transactionSuccess = true;
            } catch (SQLException e) {
                throw new ServletException(e);
            } finally {
                super.rollbackIfUnsuccessfulAndAlwaysReleaseConnection(transactionSuccess, request, conn);
            }
        
        request.setAttribute("submitStatus", submitStatus);
        }
        String code = studentRegistration.getClassAccount()+";" + submitStatus.getOneTimePassword();
        int hash = code.hashCode() & 0xf;

        String checkSum = Integer.toHexString(hash);
        if (checkSum.length() != 1)
            throw new IllegalStateException();
        code = code + checkSum;
        request.setAttribute("submitStatusCode", code);
        
        chain.doFilter(request, response);
    }

}
