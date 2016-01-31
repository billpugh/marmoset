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

/**
 * Created on Nov 21, 2005
 *
 * @author jspacco
 */
package edu.umd.cs.submitServer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import edu.umd.cs.marmoset.modelClasses.Student;

/**
 * GenericLDAPAuthenticationService
 * 
 */
public class GenericLDAPAuthenticationService implements ILDAPAuthenticationService {
	private static final WebConfigProperties webProperties = WebConfigProperties.get();
    protected boolean useSSL = true;

    String ldapURL = "";
    String authMechanism = "simple";
    String principleFormat = "";

    @Override
    public void initialize(ServletContext context) {
        // assume SSL is enabled by default
        String skipLDAP = webProperties.getProperty(SubmitServerConstants.LDAP_SSL_OFF);
        boolean skipSSL = "true".equalsIgnoreCase(skipLDAP);
        useSSL = !skipSSL;
        ldapURL = webProperties.getProperty(SubmitServerConstants.LDAP_URL);
        authMechanism = webProperties.getProperty(SubmitServerConstants.LDAP_AUTH_MECHANISM);
        principleFormat = webProperties.getProperty(SubmitServerConstants.LDAP_PRINCIPAL_FORMAT);
    }

    @Override
    public Student authenticateLDAP(String campusUID, String uidPassword, Connection conn, boolean skipLDAP) throws SQLException,
            NamingException, ClientRequestException {
        //
        // LDAP Authentication
        //
        if (campusUID == null) {
            String msg = "campusUID or uidPassword null";
            throw new CanNotFindDirectoryIDException(HttpServletResponse.SC_BAD_REQUEST, msg);
        }
        //
        // Perform reverse-LDAP lookup
        //
        Student student = Student.lookupByLoginName(campusUID, conn);
        if (student == null) {
            String msg = "Cannot find directoryID: " + campusUID;
            throw new CanNotFindDirectoryIDException(HttpServletResponse.SC_UNAUTHORIZED, msg);
        }
        // Use campus uid instead of employee number
        if (skipLDAP || authenticateViaLDAP(campusUID, uidPassword)) {
            return student;
        }
        String msg = "Password incorrect for directoryID: " + campusUID + ", campus UID: " + campusUID;
        throw new BadPasswordException(HttpServletResponse.SC_UNAUTHORIZED, msg);
    }

    private boolean authenticateViaLDAP(String campusUID, String password) throws NamingException {
        try {
            Hashtable<String, String> env = new Hashtable<String, String>(11);
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapURL);
            env.put(Context.SECURITY_AUTHENTICATION, authMechanism);

            LdapName principalDn;
            try {
                principalDn = new LdapName(String.format(principleFormat, Rdn.escapeValue(campusUID)));
            } catch (InvalidNameException ine) {
                throw new NamingException("Invalid name error: " + ine.toString());
            }
            String principal = principalDn.toString();

            env.put(Context.SECURITY_PRINCIPAL, principal);
            env.put(Context.SECURITY_CREDENTIALS, password);
            if (useSSL)
                env.put(Context.SECURITY_PROTOCOL, "ssl");
            DirContext ctx = new InitialDirContext(env);
            ctx.close();
        } catch (AuthenticationException e) {
            return false;
        } catch (NamingException e) {
            if (e.getMessage().indexOf("Operations Error") != -1 || e.getExplanation().indexOf("Operations Error") != -1) {
                return false;
            } else {
                throw e;
            }
        }
        return true;
    }

  

}
