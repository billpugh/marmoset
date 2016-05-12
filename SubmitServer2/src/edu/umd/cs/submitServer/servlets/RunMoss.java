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
 * Created on Feb 8, 2005
 */
package edu.umd.cs.submitServer.servlets;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import edu.umd.cs.marmoset.modelClasses.IO;
import edu.umd.cs.marmoset.modelClasses.Project;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration;
import edu.umd.cs.marmoset.modelClasses.Submission;
import edu.umd.cs.marmoset.modelClasses.ZipFileAggregator;
import edu.umd.cs.marmoset.utilities.TextUtilities;
import edu.umd.cs.submitServer.IOUtilities;
import it.zielke.moji.MossException;
import it.zielke.moji.SocketClient;

/**
 * @author jspacco
 *
 */
public class RunMoss extends SubmitServerServlet {

  /**
   * The doGet method of the servlet. <br>
   *
   * This method is called when a form has its tag value method equals to get.
   *
   * @param request
   *          the request send by the client to the server
   * @param response
   *          the response send by the server to the client
   * @throws ServletException
   *           if an error occurred
   * @throws IOException
   *           if an error occurred
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Connection conn = null;
    SocketClient socketClient = new SocketClient();
    response.setContentType("text/plain");
    PrintWriter writer = response.getWriter();
    writer.println("Preparing to run moss");
    // set your MOSS user ID
    socketClient.setUserID("166942184");
    // socketClient.setOpt...
    String language = request.getParameter("language");
    HashSet<String> extensions = new HashSet<>();
    switch (language) {
    case "java": extensions.add(".java"); break;
    case "cc": extensions.add(".h"); extensions.add(".c"); 
    extensions.add(".cc"); extensions.add(".C"); extensions.add(".cxx"); 
    extensions.add(".c++"); break;
    
    case "c": extensions.add(".h"); extensions.add(".c"); break;
    case "prolog": extensions.add(".pl"); break;
    
    default: extensions.add(language);
    }
    try {
      conn = getConnection();
      // set the programming language of all student source codes

      
      socketClient.setLanguage(language);

      // initialize connection and send parameters
      socketClient.run();
      writer.println("Connection to moss server started");
     
      // get the project and all the student registrations
      Map<Integer, Submission> lastSubmissionMap = (Map<Integer, Submission>) request.getAttribute("lastSubmission");

      writer.println("best submissions: " + lastSubmissionMap.keySet());
      Project project = (Project) request.getAttribute("project");

      byte[] baseline = project.getBaselineZip(conn);
      if (baseline != null) {
        uploadSubmission(socketClient, "baseline", baseline, true, extensions);
      }
      Set<StudentRegistration> registrationSet = (Set<StudentRegistration>) request
          .getAttribute("studentRegistrationSet");

      for (StudentRegistration registration : registrationSet) {
        Submission submission = lastSubmissionMap.get(registration.getStudentRegistrationPK());
        String classAccount = registration.getClassAccount();
        if (submission == null) {
          writer.println("No submission for #" + registration.getStudentRegistrationPK() + " " + classAccount);
        } else {
          writer.println("Uploading files for " + classAccount);
          byte[] bytes = submission.downloadArchive(conn);

          uploadSubmission(writer, socketClient, classAccount, bytes, false, extensions);
        }
      }

      // finished uploading, tell server to check files
      socketClient.sendQuery(5000, () -> writer.println("waiting..."));
      URL results = socketClient.getResultURL();
      writer.println("Moss results at " + results);
      response.sendRedirect(results.toString());

    } catch (SQLException e) {
      handleSQLException(e);
      throw new ServletException(e);
    } catch (MossException e) {
      throw new ServletException(e);
    } finally {
      releaseConnection(conn);

    }
  }

 static private void uploadSubmission(PrintWriter writer, SocketClient socketClient, String classAccount, byte[] bytes, boolean baselline,
      HashSet<String> extensions) {
    try {
      HashSet<String> seen = new HashSet<>();
      Map<String, List<String>> files = TextUtilities.scanTextFilesInZip(new ByteArrayInputStream(bytes));

      for (Map.Entry<String, List<String>> e : files.entrySet()) {
        String name = e.getKey();
        name = name.substring(name.lastIndexOf('/') + 1);
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0 && extensions.contains(name.substring(lastDot))
            && seen.add(name)) {
          name = classAccount + "/" + name;
          writer.println("  Sending " + name);
          socketClient.uploadFile(name, e.getValue(), false);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
