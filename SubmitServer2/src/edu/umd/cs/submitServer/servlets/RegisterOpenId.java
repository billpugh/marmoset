package edu.umd.cs.submitServer.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.marmoset.utilities.Charsets;
import edu.umd.cs.submitServer.SubmitServerConstants;

/**
 * Complete registration of a new OpenID identifier. This servlet handles the form post from
 * {@code /openid/register.jsp}.
 * 
 * @author rwsims
 * 
 */
public class RegisterOpenId extends SubmitServerServlet {
	private static final Logger logger = Logger.getLogger(RegisterOpenId.class);
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
		req.getSession().removeAttribute(SubmitServerConstants.OPENID_AX_MAP);
		String uid = req.getParameter("uid");
		logger.info("Registering uid " + uid);
		String firstname = req.getParameter("firstname");
		String lastname = req.getParameter("lastname");
		String loginName = req.getParameter("login");
		String email = req.getParameter("email");
		
		Connection conn = null;
		try {
			conn = getConnection();
			boolean emptyDatabase = !Student.existAny(conn);
			
			// Create normal user account.
			
			if (emptyDatabase) {
			    Student student = makeStudent(uid, firstname, lastname, loginName, email);
	            student.setCanImportCourses(true);
	            student.insert(conn);
				Student admin = makeStudent(uid, firstname, lastname, student.getLoginNameForAdminAccount(), email);
				admin.setSuperUser(true);
				admin.insert(conn);
				PerformLogin.setUserSession(req.getSession(), admin, false, conn);
			} else {
			    Student student = Student.insertOrUpdateByUID(uid, firstname, lastname, loginName, email, conn);
	            
				PerformLogin.setUserSession(req.getSession(), student, false, conn);
			}
		} catch (SQLException e) {
			throw new ServletException(e);
		} finally {
			releaseConnection(conn);
		}
		  // check to see if user tried to view a page before logging in
        String target = req.getParameter("target");

        if (target != null && !target.equals("")) {
            target = Charsets.decodeURL(target);
            resp.sendRedirect(target);
            return;
        }

		
	}

    /**
     * @param uid
     * @param firstname
     * @param lastname
     * @param loginName
     * @param email
     * @return
     */
    public Student makeStudent(String uid, String firstname, String lastname, String loginName, String email) {
        Student student = new Student();
        student.setLoginName(loginName);
        student.setCampusUID(uid);
        student.setFirstname(firstname);
        student.setLastname(lastname);
        if (email != null) {
          student.setEmail(email);
        }
        return student;
    }
}
