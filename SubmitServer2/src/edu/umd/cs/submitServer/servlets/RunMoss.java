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
	 *            the request send by the client to the server
	 * @param response
	 *            the response send by the server to the client
	 * @throws ServletException
	 *             if an error occurred
	 * @throws IOException
	 *             if an error occurred
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Connection conn = null;
		SocketClient socketClient = new SocketClient();

    // set your MOSS user ID
    socketClient.setUserID("166942184");
    // socketClient.setOpt...

   
		try {
			conn = getConnection();
			 // set the programming language of all student source codes
	    socketClient.setLanguage("c");
	    // initialize connection and send parameters
	    socketClient.run();
	    System.out.println("talking to moss server");

			// get the project and all the student registrations
			Map<Integer, Submission> lastSubmissionMap = (Map<Integer, Submission>) request
					.getAttribute("lastSubmission");
		

	      System.out.println("best submissions: " + lastSubmissionMap.keySet());
	  	Project project = (Project) request.getAttribute("project");
			
			Set<StudentRegistration> registrationSet = (Set<StudentRegistration>) request
					.getAttribute("studentRegistrationSet");
			
			for (StudentRegistration registration : registrationSet) {
			  Submission submission = lastSubmissionMap.get(registration
						.getStudentRegistrationPK());
					if (submission == null) {
				  System.out.println("No submission for #" + 
				registration.getStudentRegistrationPK() + " " + registration.getClassAccount());
				} else {
				  System.out.println("Uploading files for " + registration.getClassAccount());
	        
					try {
						byte[] bytes = submission.downloadArchive(conn);
						HashSet<String> seen = new HashSet<>();
						Map<String, List<String>> files 
						  = TextUtilities.scanTextFilesInZip(new ByteArrayInputStream(bytes));
					  
						for(Map.Entry<String,List<String>> e : files.entrySet()) {
						  String name = e.getKey();
						  name = name.substring(name.lastIndexOf('/')+1);
						  if (name.endsWith(".c") && seen.add(name)) {
						    name = registration.getClassAccount() +"/" + name;
						    System.out.println("Sending " + name);
						    socketClient.uploadFile(name, 
						        e.getValue(), false);
						  }
						}
					} catch (Exception e) {
					  e.printStackTrace();
					}
				}
			}

		  // finished uploading, tell server to check files
	    socketClient.sendQuery();
	    URL results = socketClient.getResultURL();
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

}
