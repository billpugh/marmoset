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
 * Created on Jan 6, 2005
 *
 */
package edu.umd.cs.submitServer.filters;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jasig.cas.client.authentication.AttributePrincipal;

import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.submitServer.UserSession;
import edu.umd.cs.submitServer.WebConfigProperties;
import edu.umd.cs.submitServer.servlets.PerformLogin;

/**
 * If the user is not logged in, redirect to an appropriate login url, set in
 * the {@code authentication.redirect} system property.
 * 
 * @author pugh
 * 
 */
public class CASAuthenticationFilter extends SubmitServerFilter {
  private static final WebConfigProperties webProperties = WebConfigProperties.get();

  private static int getPort(URL u) throws ServletException {
    int port = u.getPort();
    if (port > 0)
      return port;
    if (u.getProtocol().equals("http"))
      return 80;
    if (u.getProtocol().equals("https"))
      return 443;
    throw new ServletException("Unhandled protocol: " + u.getProtocol());

  }

  public static void checkReferer(HttpServletRequest request) throws ServletException {

    String referer = request.getHeader("referer");
    String requestURLString = request.getRequestURL().toString();

    if (referer == null)
      throw new ServletException("No referer for " + request.getMethod() + " of " + requestURLString);
    try {
      URL refererURL = new URL(referer);
      URL requestURL = new URL(requestURLString);

      if (!requestURL.getProtocol().equals(refererURL.getProtocol())
          || !requestURL.getHost().equals(refererURL.getHost()) || getPort(requestURL) != getPort(refererURL))

        throw new ServletException(String.format("referer %s doesn't match %s", refererURL, requestURL));

    } catch (MalformedURLException e) {
      throw new ServletException("Bad referer " + referer, e);

    }
  }

  private String authType;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    super.init(filterConfig);
    ServletContext ctx = filterConfig.getServletContext();
    authType = webProperties.getRequiredProperty("authentication.type", "cas");
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;
    HttpSession session = request.getSession(false);
    UserSession userSession = session == null ? null : (UserSession) session.getAttribute(USER_SESSION);

    String method = request.getMethod();
    if (!method.equals("GET") && !method.equals("HEAD"))
      checkReferer(request);
    needLogin: if (session == null || session.isNew() || userSession == null) {
      try {
        AttributePrincipal principal = (AttributePrincipal) request.getUserPrincipal();
        if (principal != null) {
          String ldapName = principal.getName();
          if (ldapName != null) {
            session = request.getSession(true);
            try (Connection conn = getConnection()) {
              Student student = Student.lookupByLoginName(ldapName, conn);
              if (student != null) {
                PerformLogin.setUserSession(session, student, true, conn);
                request.setAttribute(STUDENT, student);
                request.setAttribute("authType", authType);
                chain.doFilter(req, resp);
                return;
              } else
                throw new ServletException("No student found for " + ldapName);

            }
          } else
            throw new ServletException("No name provided for CAS principle");
        }

      } catch (Exception e) {
        e.printStackTrace();
      }

          String login = String.format("%s/authenticate/%s/PerformLogin", request.getContextPath(), authType);

        // if the request is "get", save the target for a later
        // re-direct
        // after authentication
        if (request.getMethod().equals("GET")) {
          String target = request.getRequestURI();
          if (request.getQueryString() != null)
            target += "?" + request.getQueryString();
          target = URLEncoder.encode(target, "UTF-8");
          login = login + "?target=" + target;
          System.out.println("CAS authentication filter sending redirect to " + login);

        }

        response.sendRedirect(login);
      
    } else {
      request.setAttribute("authType", authType);
      Connection conn = null;
      try {
        conn = getConnection();
        Student student = Student.getByStudentPK(userSession.getStudentPK(), conn);
        request.setAttribute(STUDENT, student);

      } catch (SQLException e) {
        handleSQLException(e);
        throw new ServletException(e);
      } finally {
        releaseConnection(conn);
      }
      chain.doFilter(req, resp);
    }
  }
}
