package edu.umd.cs.submitServer.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.submitServer.SubmitServerConstants;
import edu.umd.cs.submitServer.UserSession;

/**
 * Servlet to allow changing personas to become an admin or shadow (fake student) account.
 * 
 * @author rwsims
 *
 */
public class BecomePersona extends SubmitServerServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	  UserSession userSession = (UserSession) req.getSession().getAttribute(SubmitServerConstants.USER_SESSION);
	  String suffix = req.getParameter("persona");
	  Preconditions.checkArgument(!Strings.isNullOrEmpty(suffix), "Must specify an account type to become");
	  int becomePK;
	  if (suffix.equals("admin")) {
	  	becomePK = Preconditions.checkNotNull(userSession.getSuperuserPK());
	  } else if (suffix.equals("student")) {
	  	becomePK = Preconditions.checkNotNull(userSession.getShadowAccountPK());
	  } else {
	  	throw new IllegalArgumentException("Invalid persona suffix given");
	  }
	  
	  Connection conn = null;
	  try {
	  	conn = getConnection();
	  	Student become = Student.getByStudentPK(Student.asPK(becomePK), conn);
	  	PerformLogin.setUserSession(req.getSession(), become, false, conn);
	  } catch (SQLException e) {
	  	throw new ServletException(e);
    } finally {
	  	releaseConnection(conn);
	  }
	  
	  resp.sendRedirect(req.getContextPath() + "/view/index.jsp");
	}
}
