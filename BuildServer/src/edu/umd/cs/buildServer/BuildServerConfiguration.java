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
 * Created on Nov 9, 2005
 *
 * @author jspacco
 */
package edu.umd.cs.buildServer;

import java.io.File;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.log4j.Logger;

/**
 * BuildServerConfiguration Contains all information passed to the BuildServer
 * through the config.properties file. Ultimately this class should be created
 * directly from the config.properties file, replacing the Configuration class
 * completely.
 * 
 * @author jspacco
 */
public class BuildServerConfiguration implements BuildServerConfigurationMBean {
	private static final String BUILD_SERVER_HOME = "build.server.home";
	private static final String FINDBUGS_HOME = "findbugs.home";
	private static final String PMD_HOME = "pmd.home";
	
	private Configuration configuration;

	private File javaHome;
	private File buildServerHome;
	private File buildServerRoot;
	private String hostname;
	private List<String> supportedCourseList;
	private String supportedCourses;

	private String submitServerURL;
	
	private static final String submitServerRequestprojectPath = "/buildServer/RequestSubmission";
	private static final String submitServerGetTestSetupPath = "/buildServer/GetTestSetup";
	private static final String submitServerReporttestresultsPath = "/buildServer/ReportTestOutcomes";
	private String submitServerHandlebuildserverlogmessagePath;
	
	private static final String START_DIRECTORY = new File(".").getAbsolutePath();

	private File buildDirectory;
	private File testFilesDirectory;
	private File jarCacheDirectory;
	private @CheckForNull File logDirectory;

	private boolean debugVerbose = true;
	private boolean doNotLoop = true;
	private boolean debugJavaSecurity = false;

	private int numServerLoopIterations;

	private String cloverDBPath;
	
	private long serverDate;

	public long getServerTimestamp() {
	    return serverDate;
	}
	public BuildServerConfiguration() {
	}
	
	
	public static @Nonnull File getBuildServerJarfile() {
	    URL location = BuildServerConfiguration.class.getProtectionDomain()
	            .getCodeSource().getLocation();
	    File f;
	    try {
	        f = new File(location.toURI());
	    } catch (URISyntaxException e) {
	        throw new AssertionError("URL syntax invalid: " + location);
	    }
	    return f;
	}
	
    public static @CheckForNull
    File getBuildServerRootFromCodeSource() {
        try {
           File f = getBuildServerJarfile();
           f =  f.getParentFile();
           File lib = new File(f, "lib");
           if (lib.exists()) return f;
           f = f.getParentFile();
           lib = new File(f, "lib");
           if (lib.exists()) return f;
           return null;
           
           
        } catch (Exception e) {
            return null;
        }
    }

    public @Nonnull File getBuildServerRoot(Configuration config)
            throws MissingConfigurationPropertyException {
        File f = getBuildServerRootFromCodeSource();
        if (f != null)
            return f;
        String root = config
                .getRequiredProperty(ConfigurationKeys.BUILDSERVER_ROOT);
        return new File(root);
    }

	public String getLocalHostName(Configuration config, Logger logger) {
	    String name = config.getOptionalProperty(ConfigurationKeys.HOSTNAME);
	    if (name != null)
	        return name;
	    
	    InetAddress localHost;
        try {
            localHost = InetAddress.getLocalHost();
            String username = System.getProperty("user.name");
            if (username == null)
                username = "";
            else username = "/" + username;
            return localHost.getHostName()
                    + username;
        } catch (Exception e) {
        	logger.warn("Unable to get local host name", e);
            return "unknown";
        }
        
	    
	}
	public void loadAllProperties(Configuration config, Logger logger)
			throws MissingConfigurationPropertyException {
		setJavaHome(config.getStringProperty("java.home", ""));
		setBuildServerWorkingDir(config.getStringProperty(BUILD_SERVER_HOME, START_DIRECTORY));
		setBuildServerRoot(getBuildServerRoot(config));
		this.configuration = config;

		// Semester and course
		setSupportedCourses(config
				.getOptionalProperty(ConfigurationKeys.SUPPORTED_COURSE_LIST));

		// Protocol, hostname, port and password
		setSubmitServerURL(config
				.getOptionalProperty(ConfigurationKeys.SUBMIT_SERVER_URL));
		  
		setHostname(getLocalHostName(config, logger));
		
		serverDate = getBuildServerJarfile().lastModified();
		
		// XXX These properties have been standardized for quite some time and
		// haven't changed.
		// Thus we use the defaults unless something else is requested.
				// If hanldeBuildServerLogMessages is empty, then we're not logging back
		// to the submitServer
		// So this property must be read; we can't just use the default
		
		setBuildDirectory(config
				.getStringProperty(ConfigurationKeys.BUILD_DIRECTORY, "build"));
		setTestFilesDirectory(config
				.getStringProperty(ConfigurationKeys.TEST_FILES_DIRECTORY, "testfiles"));
		setJarCacheDirectory(config
				.getStringProperty(ConfigurationKeys.TEST_SETUP_CACHE_DIRECTORY, "setupCache"));
		setLogDirectory(config
				.getStringProperty(ConfigurationKeys.LOG_DIRECTORY, "log"));

		setDebugVerbose(config
				.getOptionalBooleanProperty(ConfigurationKeys.DEBUG_VERBOSE));
		setDebugJavaSecurity(config
				.getOptionalBooleanProperty(ConfigurationKeys.DEBUG_SECURITY));
		setDoNotLoop(config
				.getBooleanProperty(ConfigurationKeys.DEBUG_DO_NOT_LOOP));
	}
	
	public Configuration getConfig() {
	    return configuration;
	}

	/**
	 * @return Returns the buildServerHome.
	 */
	public File getBuildServerWorkingDir() {
		return buildServerHome;
	}

	/**
	 * @param buildServerHome
	 *            The buildServerHome to set.
	 */
	public void setBuildServerWorkingDir(String buildServerHome) {
		this.buildServerHome = new File(buildServerHome);
	}

	/**
	 * @return Returns the buildServerRoot.
	 */
	public File getBuildServerRoot() {
	    if (buildServerRoot == null)
            throw new NullPointerException("null build server root");
		return buildServerRoot;
	}

	/**
	 * @param buildServerRoot
	 *            The buildServerRoot to set.
	 */
	public void setBuildServerRoot(File buildServerRoot) {
	    if (buildServerRoot == null)
	        throw new NullPointerException("null build server root");
		this.buildServerRoot = buildServerRoot;
	}

	/**
	 * @return Returns the javaHome.
	 */
	@Override
	public File getJavaHome() {
		return javaHome;
	}

	/**
	 * @param javaHome
	 *            The javaHome to set.
	 */
	public void setJavaHome(String javaHome) {
		this.javaHome = new File(javaHome);
	}

	/**
	 * @return Returns the buildDirectory.
	 */
	public File getBuildDirectory() {
		return buildDirectory;
	}

	/**
	 * @param buildDirectory
	 *            The buildDirectory to set.
	 */
	public void setBuildDirectory(String buildDirectory) {
		this.buildDirectory = new File(buildServerHome, buildDirectory);
	}

	public String getSubmitServerURL() {
        return submitServerURL;
    }

    public void setSubmitServerURL(String submitServerURL) {
        this.submitServerURL = submitServerURL;
    }

    /**
	 * @return Returns the debugDonotloop.
	 */
	@Override
	public boolean getDoNotLoop() {
		return doNotLoop;
	}

	/**
	 * @param debugDonotloop
	 *            The debugDonotloop to set.
	 */
	@Override
	public void setDoNotLoop(boolean debugDonotLoop) {
		this.doNotLoop = debugDonotLoop;
	}

	/**
	 * @return Returns the debugJavaSecurity.
	 */
	public boolean isDebugJavaSecurity() {
		return debugJavaSecurity;
	}

	/**
	 * @param debugJavaSecurity
	 *            The debugJavaSecurity to set.
	 */
	public void setDebugJavaSecurity(boolean debugJavaSecurity) {
		this.debugJavaSecurity = debugJavaSecurity;
	}

	/**
	 * @return Returns the debugVerbose.
	 */
	public boolean isDebugVerbose() {
		return debugVerbose;
	}

	/**
	 * @param debugVerbose
	 *            The debugVerbose to set.
	 */
	public void setDebugVerbose(boolean debugVerbose) {
		this.debugVerbose = debugVerbose;
	}

	/**
	 * @return Returns the hostname.
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * @param hostname
	 *            The hostname to set.
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * @return Returns the jarCacheDirectory.
	 */
	public File getJarCacheDirectory() {
		return jarCacheDirectory;
	}

	/**
	 * @param jarCacheDirectory
	 *            The jarCacheDirectory to set.
	 */
	public void setJarCacheDirectory(String jarCacheDirectory) {
		this.jarCacheDirectory =  new File(buildServerHome, jarCacheDirectory);
	}

	/**
	 * @return Returns the logDirectory.
	 */
	@Override
	public @CheckForNull File getLogDirectory() {
		return logDirectory;
	}

	/**
	 * @param logDirectory
	 *            The logDirectory to set.
	 */
	public void setLogDirectory(String logDirectory) {
	    if (logDirectory == null || logDirectory.equals("console"))
	        this.logDirectory = null;
	    else
	        this.logDirectory = new File(buildServerHome, logDirectory);
	}

	
    /**
	 * @return Returns the submitServerGetprojectjarPath.
	 */
	public String getSubmitServerGetprojectjarPath() {
		return submitServerGetTestSetupPath;
	}

	/**
	 * @return Returns the submitServerHandlebuildserverlogmessagePath.
	 */
	public String getSubmitServerHandlebuildserverlogmessagePath() {
		return submitServerHandlebuildserverlogmessagePath;
	}

	/**
	 * @param submitServerHandlebuildserverlogmessagePath
	 *            The submitServerHandlebuildserverlogmessagePath to set.
	 */
	public void setSubmitServerHandlebuildserverlogmessagePath(
			String submitServerHandlebuildserverlogmessagePath) {
		this.submitServerHandlebuildserverlogmessagePath = submitServerHandlebuildserverlogmessagePath;
	}

	
	/**
	 * @return Returns the submitServerReporttestresultsPath.
	 */
	public String getSubmitServerReporttestresultsPath() {
		return submitServerReporttestresultsPath;
	}

	
	/**
	 * @return Returns the submitServerRequestprojectPath.
	 */
	public String getSubmitServerRequestprojectPath() {
		return submitServerRequestprojectPath;
	}

		/**
	 * @return Returns the supportedCourses.
	 */
	@Override
	public String getSupportedCourses() {
		return supportedCourses;
	}

	/**
	 * @param supportedCourses
	 *            The supportedCourses to set.
	 */
	@Override
	public void setSupportedCourses(String supportedCourses) {
	    if (supportedCourses == null) return;
		supportedCourseList = new LinkedList<String>();
		this.supportedCourses = supportedCourses;
		StringTokenizer tokenizer = new StringTokenizer(supportedCourses);

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			supportedCourseList.add(token);
		}
	}

	public List<String> getSupportedCoursesList() {
		return supportedCourseList;
	}

	/**
	 * @return Returns the testFilesDirectory.
	 */
	public File getTestFilesDirectory() {
		return testFilesDirectory;
	}

	/**
	 * @param testFilesDirectory
	 *            The testFilesDirectory to set.
	 */
	public void setTestFilesDirectory(String testFilesDirectory) {
		this.testFilesDirectory = new File(buildServerHome, testFilesDirectory);
	}

	public String getCloverDBPath() {
		return cloverDBPath;
	}

	public void setCloverDBPath(String cloverDBPath) {
		this.cloverDBPath = cloverDBPath;
	}

	@Override
	public int getNumServerLoopIterations() {
		return numServerLoopIterations;
	}

	@Override
	public void setNumServerLoopIterations(int numServerLoopIterations) {
		this.numServerLoopIterations = numServerLoopIterations;
	}
	
    public String getServletURL(String path) {
        return getSubmitServerURL() + path;
    }
}
