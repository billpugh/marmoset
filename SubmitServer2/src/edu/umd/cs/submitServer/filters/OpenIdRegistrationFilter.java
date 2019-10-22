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

import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.submitServer.servlets.PerformLogin;
import edu.umd.cs.submitServer.servlets.Util;

/**
 * Filter that checks if an OpenID identifier needs to be registered. If not, it redirects to the
 * target URL, otherwise it proceeds on to {@code /openid/register.jsp}, where the user is asked to
 * fill in information like name, email, etc.
 * 
 * @author rwsims
 * 
 */
public class OpenIdRegistrationFilter extends SubmitServerFilter {

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
	    throws IOException, ServletException {
		String uid = req.getParameter("uid");
		String target = req.getParameter("target");
		
		Connection conn = null;
		try {
	    conn = getConnection();
	    Student student = Student.lookupByCampusUID(uid, conn);
	    if (student != null) {
	    	// Login user and redirect.
	    	PerformLogin.setUserSession(((HttpServletRequest) req).getSession(), student, false, conn);
		    ((HttpServletResponse) resp).sendRedirect(Util.urlDecode(target));
		    return;
	    }
	    chain.doFilter(req, resp);
    } catch (SQLException e) {
	    throw new ServletException(e);
    } finally {
    	releaseConnection(conn);
    }
	}
}
