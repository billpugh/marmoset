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
 * Created on Jan 21, 2005
 *
 * @author jspacco
 */
package edu.umd.cs.submitServer.filters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.marmoset.modelClasses.ServerError;
import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration;
import edu.umd.cs.submitServer.MultipartRequest;
import edu.umd.cs.submitServer.StudentForUpload;
import edu.umd.cs.submitServer.WebConfigProperties;

/**
 * @author jspacco
 * 
 */
public class RegisterStudentsFilter extends SubmitServerFilter {
	private static final WebConfigProperties webProperties = WebConfigProperties.get();

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        if (!request.getMethod().equals("POST")) {
            throw new ServletException("Only POST accepted");
        }
        Connection conn = null;
        BufferedReader reader = null;
        FileItem fileItem = null;
        TreeSet<StudentRegistration> registeredStudents = new TreeSet<StudentRegistration>();
        List<String> errors = new ArrayList<String>();
        try {
            conn = getConnection();

            // MultipartRequestFilter is required
            MultipartRequest multipartRequest = (MultipartRequest) request.getAttribute(MULTIPART_REQUEST);

            Course course = (Course) request.getAttribute("course");

            // open the uploaded file
            fileItem = multipartRequest.getFileItem();
            reader = new BufferedReader(new InputStreamReader(fileItem.getInputStream()));

            int lineNumber = 1;

            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break;
                lineNumber++;

                // hard-coded skip of first two lines for Maryland-specific
                // format
                if (line.startsWith("Last,First,UID,section,ClassAcct,DirectoryID"))
                    continue;
                if (line.startsWith(",,,,,")) {
                  if (line.equals(",,,,,"))
                    continue;
                  String ldap = line.substring(5);
                  Student student = Student.lookupByLoginName(ldap, conn);
                  if (student != null) {
                  StudentRegistration sr = StudentForUpload.registerStudent(course,
                          student, "", ldap,  null, conn);
                  registeredStudents.add(sr);
                  } else 
                    errors.add("Did not find " + ldap);
                  
                  continue;
          
                }
                if (line.startsWith("#"))
                    continue;

                // skip blank lines
                if (line.trim().equals(""))
                    continue;

                try {
                    StudentForUpload s = new StudentForUpload(line, delimiter);

                    Student student = s.lookupOrInsert(conn);
                    StudentRegistration sr = StudentForUpload.registerStudent(course, student, s.section, s.classAccount, null, conn);
                    registeredStudents.add(sr);

                } catch (IllegalStateException e) {
                    errors.add(e.getMessage());
                    ServletExceptionFilter.logError(conn, ServerError.Kind.EXCEPTION, request, "error while registering " + line,
                            null, e);

                } catch (Exception e1) {
                    errors.add("Problem processing line: '" + line + "' at line number: " + lineNumber);
                    ServletExceptionFilter.logError(conn, ServerError.Kind.EXCEPTION, request, "error while registering " + line,
                            null, e1);
                }
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        } finally {
            releaseConnection(conn);
            if (reader != null)
                reader.close();
            if (fileItem != null)
                fileItem.delete();
        }
        request.setAttribute("registeredStudents", registeredStudents);
        request.setAttribute("errors", errors);
        chain.doFilter(request, response);
    }

    /**
     * The delimiter used in the uploaded files of students for registration.
     */
    String delimiter = ",";

    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        String delimiter = webProperties.getProperty("register.students.delimiter");
        if (delimiter != null)
            this.delimiter = delimiter;
    }

}
