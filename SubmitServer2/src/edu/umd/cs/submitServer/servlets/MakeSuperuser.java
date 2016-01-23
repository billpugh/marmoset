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

import edu.umd.cs.marmoset.modelClasses.Student;

public class MakeSuperuser extends SubmitServerServlet {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Student student = (Student) request.getAttribute(STUDENT);
        Student user = (Student) request.getAttribute(USER);

        if (!user.isSuperUser()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not superuser");
            return;
        }
        if (!student.isNormalAccount()) {
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "student not normal account");
          return;
        }
        if (!student.getCanImportCourses()) {
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "student not instructor account");
          return;
        }

        Connection conn = null;
        try {
          conn = getConnection();
          student.lookupOrCreateAdminAccount(conn);
        } catch (SQLException e) {
          throw new ServletException(e);
        } finally {
          releaseConnection(conn);
        }

        String redirectUrl = request.getContextPath() + "/view/admin/";
        response.sendRedirect(redirectUrl);
    }

}
