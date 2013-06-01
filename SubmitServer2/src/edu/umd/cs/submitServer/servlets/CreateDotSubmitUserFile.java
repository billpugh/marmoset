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

package edu.umd.cs.submitServer.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.marmoset.modelClasses.Project;
import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration;
import edu.umd.cs.marmoset.modelClasses.StudentSubmitStatus;

public class CreateDotSubmitUserFile extends SubmitServerServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Course course = (Course) request.getAttribute(COURSE);
        Project project = (Project) request.getAttribute(PROJECT);
        StudentRegistration studentRegistration = (StudentRegistration) request.getAttribute(STUDENT_REGISTRATION);
        Student student = (Student) request.getAttribute(STUDENT);

        Connection conn = null;
        boolean transactionSuccess = false;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            StudentSubmitStatus submitStatus = StudentSubmitStatus.findOrCreate(project.getProjectPK(),
                    studentRegistration.getStudentRegistrationPK(), conn);
            transactionSuccess = true;
            NegotiateOneTimePassword.generateSubmitUser(response, student,
                    studentRegistration, course.getCourseName(), project, submitStatus);
        } catch (SQLException e) {
            throw new ServletException(e);
        } finally {
            rollbackIfUnsuccessfulAndAlwaysReleaseConnection(transactionSuccess, request, conn);
        }
    }

}
