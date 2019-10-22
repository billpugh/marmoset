package edu.umd.cs.submitServer.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.marmoset.modelClasses.StudentPicture;
import edu.umd.cs.marmoset.utilities.Charsets;
import edu.umd.cs.submitServer.WebConfigProperties;

/**
 * Servlet implementation class GoogleSignin
 */
public class GoogleSignin extends SubmitServerServlet {
  private static final long serialVersionUID = 1L;
  private static final WebConfigProperties webProperties = WebConfigProperties.get();

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doPost(request, response);
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String queryString = request.getQueryString();
    String uri = request.getRequestURI();
    String url = request.getRequestURL().toString();

    String authCode = request.getParameter("authCode");
    String path = request.getContextPath();
    Logger log = getSubmitServerServletLog();
    log.warn("Authorization code: " + authCode);

    GoogleAuthorizationCodeTokenRequest authorizationRequest = new GoogleAuthorizationCodeTokenRequest(
        new NetHttpTransport(), JacksonFactory.getDefaultInstance(), "https://www.googleapis.com/oauth2/v4/token",
        webProperties.getRequiredProperty("googleSignin.clientID"),
        webProperties.getRequiredProperty("googleSignin.secret"), authCode, "postmessage");
    HttpResponse executeUnparsed = authorizationRequest.executeUnparsed();

    GoogleTokenResponse tokenResponse = executeUnparsed.parseAs(GoogleTokenResponse.class);
    log.warn("authorization request successful");

    // Get profile info from ID token
    GoogleIdToken idToken = tokenResponse.parseIdToken();
    GoogleIdToken.Payload payload = idToken.getPayload();
    String userId = "GoogleSignin-" + payload.getSubject(); // Use this value as
                                                            // a key to identify
    // a user.
    String email = payload.getEmail();
    boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());

    String name = (String) payload.get("name");
    String pictureUrl = (String) payload.get("picture");
    String locale = (String) payload.get("locale");
    String familyName = (String) payload.get("family_name");
    String givenName = (String) payload.get("given_name");

    Connection conn = null;
    try {
      conn = getConnection();
      boolean emptyDatabase = !Student.existAny(conn);

      Student student = Student.lookupByCampusUID(userId, conn);
      if (student == null) {
        // register student
        student = new Student();
        student.setCampusUID(userId);
        student.setFirstname(givenName);
        student.setLastname(familyName);
        student.setEmail(email);
        student.setLoginName(email);
        if (emptyDatabase)
          student.setCanImportCourses(true);
        student.setHasPicture(pictureUrl != null && !pictureUrl.isEmpty());
        student.insert(conn);
        try {
          StudentPicture.insertOrUpdate(conn, student, pictureUrl);
        } catch (Exception e) {
          getSubmitServerServletLog().warn("Unable to load picture for " + email + " from " + pictureUrl);
        }
        
        if (emptyDatabase) {
          student =  student.lookupOrCreateAdminAccount(conn);
        }
      }
      PerformLogin.setUserSession(request.getSession(), student, false, conn);

    } catch (SQLException e) {
      throw new ServletException(e);
    } finally {
      releaseConnection(conn);
    }
    // check to see if user tried to view a page before logging in
    String target = request.getParameter("target");

    if (target != null && !target.equals("")) {
      target = Charsets.decodeURL(target);
      response.sendRedirect(target);
      return;
    }
    response.sendRedirect(request.getContextPath() + "/view/index.jsp");

  }

}
