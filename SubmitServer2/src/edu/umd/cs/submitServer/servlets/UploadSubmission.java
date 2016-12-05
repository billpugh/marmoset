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
 * Created on Jan 13, 2005
 *
 * @author jspacco
 */
package edu.umd.cs.submitServer.servlets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import metadata.identification.FormatDescription;
import metadata.identification.FormatIdentification;
import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;

import edu.umd.cs.marmoset.modelClasses.Project;
import edu.umd.cs.marmoset.modelClasses.StudentRegistration;
import edu.umd.cs.marmoset.modelClasses.Submission;
import edu.umd.cs.marmoset.utilities.FixZip;
import edu.umd.cs.submitServer.MultipartRequest;
import edu.umd.cs.submitServer.SubmitServerDatabaseProperties;
import edu.umd.cs.submitServer.util.WaitingBuildServer;

/**
 * @author jspacco
 * 
 */
public class UploadSubmission extends SubmitServerServlet {

  
  private static final  Logger LOGGER = Logger
      .getLogger(SUBMISSION_LOG);

  
    enum Kind {
        UNKNOWN, SINGLE_FILE, SPECIAL_ZIP_FILE, MULTIFILE_UPLOAD, ZIP_UPLOAD, TAR_UPLOAD, ZIP_UPLOAD2, FIXED_ZIP_UPLOAD, CODEMIRROR
    }

    private static Object UPLOAD_LOCK = new Object();

    static final Pattern OfficeFileName = Pattern.compile(".*.(pptx|docx|.xlsx)");
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        long now = System.currentTimeMillis();
        Timestamp submissionTimestamp = new Timestamp(now);

        // these are set by filters or previous servlets
        Project project = (Project) request.getAttribute(PROJECT);
        StudentRegistration studentRegistration = (StudentRegistration) request.getAttribute(STUDENT_REGISTRATION);
        MultipartRequest multipartRequest = (MultipartRequest) request.getAttribute(MULTIPART_REQUEST);
        boolean webBasedUpload = ((Boolean) request.getAttribute("webBasedUpload")).booleanValue();
        String clientTool = multipartRequest.getCheckedParameter("submitClientTool");
        String clientVersion = multipartRequest.getOptionalCheckedParameter("submitClientVersion");
        String cvsTimestamp = multipartRequest.getOptionalCheckedParameter("cvstagTimestamp");

      
        Collection<FileItem> files = multipartRequest.getFileItems();
        Kind kind;

        byte[] zipOutput = null; // zipped version of bytesForUpload
        boolean fixedZip = false;
        try {

            if (files.size() > 1) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(bos);
                for (FileItem item : files) {
                    String name = item.getName();
                    if (name == null || name.length() == 0)
                        continue;
                    byte[] bytes = item.get();
                    ZipEntry zentry = new ZipEntry(name);
                    zentry.setSize(bytes.length);
                    zentry.setTime(now);
                    zos.putNextEntry(zentry);
                    zos.write(bytes);
                    zos.closeEntry();
                }
                zos.flush();
                zos.close();
                zipOutput = bos.toByteArray();
                kind = Kind.MULTIFILE_UPLOAD;

            } else {
                FileItem fileItem = multipartRequest.getFileItem();
                if (fileItem == null) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "There was a problem processing your submission. "
                            + "No files were found in your submission");
                    return;
                }
                // get size in bytes
                long sizeInBytes = fileItem.getSize();
                if (sizeInBytes == 0 || sizeInBytes > Integer.MAX_VALUE) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Trying upload file of size " + sizeInBytes);
                    return;
                }

                // copy the fileItem into a byte array
                byte[] bytesForUpload = fileItem.get();
                String fileName = fileItem.getName();

                 
                boolean isSpecialSingleFile = OfficeFileName.matcher(fileName).matches();

                
                
                FormatDescription desc = FormatIdentification.identify(bytesForUpload);
                if (!isSpecialSingleFile && desc != null && desc.getMimeType().equals("application/zip")) {
                    fixedZip = FixZip.hasProblem(bytesForUpload);
                    kind = Kind.ZIP_UPLOAD;
                    if (fixedZip) {
                        bytesForUpload = FixZip.fixProblem(bytesForUpload, studentRegistration.getStudentRegistrationPK());
                        kind = Kind.FIXED_ZIP_UPLOAD;
                    }
                    zipOutput = bytesForUpload;

                } else {

                    // ==========================================================================================
                    // [NAT] [Buffer to ZIP Part]
                    // Check the type of the upload and convert to zip format if
                    // possible
                    // NOTE: I use both MagicMatch and FormatDescription (above)
                    // because MagicMatch was having
                    // some trouble identifying all zips

                    String mime = URLConnection.getFileNameMap().getContentTypeFor(fileName);

                    if (!isSpecialSingleFile && mime == null)
                        try {
                            MagicMatch match = Magic.getMagicMatch(bytesForUpload, true);
                            if (match != null)
                                mime = match.getMimeType();
                        } catch (Exception e) {
                            // leave mime as null
                        }

                    if (!isSpecialSingleFile && "application/zip".equalsIgnoreCase(mime)) {
                        zipOutput = bytesForUpload;
                        kind = Kind.ZIP_UPLOAD2;
                    } else {
                        InputStream ins = new ByteArrayInputStream(bytesForUpload);
                        if ("application/x-gzip".equalsIgnoreCase(mime)) {
                            ins = new GZIPInputStream(ins);
                        }

                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ZipOutputStream zos = new ZipOutputStream(bos);

                        if (!isSpecialSingleFile && ("application/x-gzip".equalsIgnoreCase(mime) || "application/x-tar".equalsIgnoreCase(mime))) {

                            kind = Kind.TAR_UPLOAD;

                            TarInputStream tins = new TarInputStream(ins);
                            TarEntry tarEntry = null;
                            while ((tarEntry = tins.getNextEntry()) != null) {
                                zos.putNextEntry(new ZipEntry(tarEntry.getName()));
                                tins.copyEntryContents(zos);
                                zos.closeEntry();
                            }
                            tins.close();
                        } else {
                            // Non-archive file type
                            if (isSpecialSingleFile)
                              kind = Kind.SPECIAL_ZIP_FILE;
                            else 
                              kind = Kind.SINGLE_FILE;
                            // Write bytes to a zip file
                            ZipEntry zentry = new ZipEntry(fileName);
                            zos.putNextEntry(zentry);
                            zos.write(bytesForUpload);
                            zos.closeEntry();
                        }
                        zos.flush();
                        zos.close();
                        zipOutput = bos.toByteArray();
                    }

                    // [END Buffer to ZIP Part]
                    // ==========================================================================================

                }
            }

        } catch (NullPointerException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "There was a problem processing your submission. "
                    + "You should submit files that are either zipped or jarred");
            return;
        } finally {
            for (FileItem fItem : files)
                fItem.delete();
        }
        
        if (webBasedUpload) {
            clientTool = "web";
            clientVersion = kind.toString();
        }
        
        Submission  
            submission = uploadSubmission(project, studentRegistration, zipOutput, request, submissionTimestamp, clientTool,
                    clientVersion, cvsTimestamp, getDatabaseProps(), getSubmitServerServletLog());
       
       request.setAttribute("submission", submission);

       

        if (!webBasedUpload) {

            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();
            out.println("Successful submission #" + submission.getSubmissionNumber() + " received for project "
                    + project.getProjectNumber());

            out.flush();
            out.close();
            return;
        }
        boolean instructorUpload = ((Boolean) request.getAttribute("instructorViewOfStudent")).booleanValue();
        // boolean
        // isCanonicalSubmission="true".equals(request.getParameter("isCanonicalSubmission"));
        // set the successful submission as a request attribute
        String redirectUrl;

        if (fixedZip) {
            redirectUrl = request.getContextPath() + "/view/fixedSubmissionUpload.jsp?submissionPK="
                    + submission.getSubmissionPK();
        }
        if (project.getCanonicalStudentRegistrationPK() == studentRegistration.getStudentRegistrationPK()) {
            redirectUrl = request.getContextPath() + "/view/instructor/projectUtilities.jsp?projectPK=" + project.getProjectPK();
        } else if (instructorUpload) {
            redirectUrl = request.getContextPath() + "/view/instructor/project.jsp?projectPK=" + project.getProjectPK();
        } else {
            redirectUrl = request.getContextPath() + "/view/project.jsp?projectPK=" + project.getProjectPK();
        }

        response.sendRedirect(redirectUrl);

    }

    /**
     * @param studentRegistration
     * @param zipOutput
     * @param submission
     */
    public static void logSubmission(StudentRegistration studentRegistration, byte[] zipOutput, Submission submission) {
      String msg = "Uploaded submission " + submission.getSubmissionPK() 
          + " of " + zipOutput.length + " bytes" 
          + " with archive PK " + submission.getArchivePK()
          + " from student PK " + studentRegistration.getStudentPK()
          + " for project PK " + submission.getProjectPK();
      LOGGER.log(Level.INFO, msg);
    }

    public static Submission uploadSubmission(Project project, StudentRegistration studentRegistration, byte[] zipOutput,
            HttpServletRequest request, Timestamp submissionTimestamp, String clientTool, String clientVersion, String cvsTimestamp,
            SubmitServerDatabaseProperties db,Logger log) throws ServletException, IOException {
       
        Connection conn;
        try {
            conn =  db.getConnection();
        } catch (SQLException e) {
           throw new ServletException(e);
        }
        Submission submission = null;
        boolean transactionSuccess = false;
        
        try {
            Integer baselinePK = project.getArchivePK();
            int testSetupPK = project.getTestSetupPK();
            byte baseLineSubmission[] = null;
            if (baselinePK != null && baselinePK.intValue() != 0) {
                 baseLineSubmission = project.getBaselineZip(conn); 
            } else if (testSetupPK != 0){
                baseLineSubmission = Submission.lookupCanonicalSubmissionArchive(project.getProjectPK(), conn);
            }
            zipOutput = FixZip.adjustZipNames(baseLineSubmission, zipOutput);
            
            int archivePK = Submission.uploadSubmissionArchive(zipOutput, conn);
            
            
            synchronized (UPLOAD_LOCK) {
                final int NUMBER_OF_ATTEMPTS = 2;
                int attempt = 1;
                while (true) {
                    try {
                        conn.setAutoCommit(false);
                        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                       
                        submission = Submission.submit(archivePK, studentRegistration, project, cvsTimestamp, clientTool,
                                clientVersion, submissionTimestamp, conn);

                        conn.commit();
                        transactionSuccess = true;
                        break;
                    } catch (SQLException e) {
                        conn.rollback();
                        
                        if (attempt++ >= NUMBER_OF_ATTEMPTS) {
                            Submission.deleteAbortedSubmissionArchive(archivePK, conn);
                            throw e;
                        }

                    }
                }
            }
          
        } catch (SQLException e) {
            throw new ServletException(e);
        } finally {
            rollbackIfUnsuccessfulAndAlwaysReleaseConnection(transactionSuccess, 
                    request, conn, db, log);
        }
        logSubmission(studentRegistration, zipOutput, submission);
        
        if (submission.getBuildStatus() == Submission.BuildStatus.NEW)
          WaitingBuildServer.offerSubmission(project, submission);
        return submission;
    }

}
