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
 * Created on Jan 22, 2005
 */
package edu.umd.cs.buildServer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.ConnectException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.MultipartPostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.umd.cs.buildServer.util.DevNullOutputStream;
import edu.umd.cs.buildServer.util.IO;
import edu.umd.cs.buildServer.util.ServletAppender;
import edu.umd.cs.marmoset.modelClasses.HttpHeaders;
import edu.umd.cs.marmoset.modelClasses.TestOutcome;
import edu.umd.cs.marmoset.modelClasses.TestOutcomeCollection;
import edu.umd.cs.marmoset.modelClasses.TestProperties;
import edu.umd.cs.marmoset.utilities.SystemInfo;

/**
 * Request projects to build from the SubmitServer, build them, perform quick
 * and release tests, and send the results back to the SubmitServer. This class
 * contains the main() method which runs the build server.
 *
 * @author David Hovemeyer
 */
public class BuildServerDaemon extends BuildServer implements ConfigurationKeys {
	private String configFile;

	/** Our HttpClient instance. */
	private HttpClient client;

	/**
	 * Constructor.
	 */
	public BuildServerDaemon() {
	}

	/**
	 * Set the name of the BuildServer configuration file.
	 *
	 * @param configFile
	 */
	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.umd.cs.buildServer.BuildServer#prepareToExecute()
	 */
	@Override
	protected void prepareToExecute() {
		this.client = new HttpClient();
		client.setConnectionTimeout(getConnectionTimeout());
	}

	/**
	 * @return
	 */
	protected int getConnectionTimeout() {
		return (int)TimeUnit.MILLISECONDS.convert(3, TimeUnit.MINUTES);
	}

	
	
	private HttpClient getClient() {
	    if (client == null) 
	        prepareToExecute();
	    return client;
	}
	/*
	 * (non-Javadoc)
	 *
	 * @see edu.umd.cs.buildServer.BuildServer#initConfig()
	 */
	@Override
	public void initConfig() throws IOException {
		// TODO: Verify that all the important parameters are set, and FAIL
		// EARLY is necessary
		// TODO: Make getter methods in Config for the required params
		// (build.directory, test.files.directory, etc)
        InputStream defaultConfig = BuildServerDaemon.class
                .getResourceAsStream("defaultConfig.properties");
        getConfig().load(defaultConfig);
        if (configFile == null) {
            File root = BuildServerConfiguration
                    .getBuildServerRootFromCodeSource();

            if (root == null) {
                throw new IllegalStateException(
                        "No config file specified and could not determine buildserver root");

            }
            File localConfig = new File(root, "config.properties");
            if (!localConfig.exists() || !localConfig.canRead())
                throw new IllegalStateException(
                        "No config file specified and not found at "
                                + localConfig);

            configFile = localConfig.getAbsolutePath();
        }

        getConfig().load(
                new BufferedInputStream(new FileInputStream(configFile)));

		// I'm setting the clover binary database in this method rather than in
		// the config.properties file because it always goes into /tmp and I
		// need
		// a unique name in case there are multiple buildServers on the same
		// host

		// TODO move the location of the Clover DB to the build directory.
		// NOTE: This requires changing the security.policy since Clover needs
		// to be able
		// to read, write and create files in the directory.
		//
		// String cloverDBPath =
		// getConfig().getRequiredProperty(BUILD_DIRECTORY) +"/myclover.db";
		String cloverDBPath = "/tmp/myclover.db."
				+ Long.toHexString(nextRandomLong());
		getConfig().setProperty(CLOVER_DB, cloverDBPath);
	}

	private String getWelcomeURL(){
        return getBuildServerConfiguration().getServletURL(SUBMIT_SERVER_WELCOME_PATH);
    }


	private String getRequestSubmissionURL(){
		return getBuildServerConfiguration().getServletURL(SUBMIT_SERVER_REQUESTSUBMISSION_PATH);
	}

	private String getTestSetupURL(){
		return getBuildServerConfiguration().getServletURL(SUBMIT_SERVER_GETTESTSETUP_PATH);
	}

	private String getReportTestResultsURL() {
		return getBuildServerConfiguration().getServletURL(SUBMIT_SERVER_REPORTTESTRESULTS_PATH);
	}
	private String getReportBuildServerDeathURL() {
        return getBuildServerConfiguration().getServletURL(SUBMIT_SERVER_REPORTBUILDSERVERDEATH_PATH);
    }


	/**
	 * Get a required header value. If the header value isn't specified in the
	 * server response, returns null.
	 *
	 * @param method
	 *            the HttpMethod representing the request/response
	 * @param headerName
	 *            the name of the header
	 * @return the value of the header, or null if the header isn't present
	 * @throws HttpException
	 */
	private String getRequiredHeaderValue(HttpMethod method, String headerName)
			throws HttpException {
		Header header = method.getResponseHeader(headerName);
		if (header == null || header.getValues().length != 1) {
			getLog().error(
					"Internal error: Missing header " + headerName
							+ " in submit server response");
			for(Header h : method.getResponseHeaders()) {
				getLog().error(
						"  have header " + h.getName());
			}
			return null;
		}
		return header.getValue();
	}
	/**
	 * Get a required header value. If the header value isn't specified in the
	 * server response, returns null.
	 *
	 * @param method
	 *            the HttpMethod representing the request/response
	 * @param headerName
	 *            the name of the header
	 * @return the value of the header, or null if the header isn't present
	 * @throws HttpException
	 */
	private String getRequiredHeaderValue(HttpMethod method, String headerName, String alternativeHeaderName)
			throws HttpException {
		Header header = method.getResponseHeader(headerName);
		if (header == null || header.getValues().length != 1) 
			header = method.getResponseHeader(alternativeHeaderName);
		if (header == null || header.getValues().length != 1) {
			getLog().error(
					"Internal error: Missing header " + headerName
							+ " in submit server response");
			for(Header h : method.getResponseHeaders()) {
				getLog().error(
						"  have header " + h.getName());
			}
			return null;
		}
		return header.getValue();
	}

    @Override
	protected void doWelcome() throws MissingConfigurationPropertyException, IOException {
        if (!isQuiet()) 
            System.out.println("Connecting to submit server");
        String url = getWelcomeURL();
        MultipartPostMethod method = new MultipartPostMethod(url);

       
        addCommonParameters(method);
      
        BuildServer.printURI(getLog(), method);
        int responseCode;
        try {
            responseCode = client.executeMethod(method);
        } catch (IOException e) {
            throw new IOException(
                    "Buildserver unable to connect to submitserver at " + url,
                    e);
        }
        if (!isQuiet())
            System.out.println(method.getResponseBodyAsString());

        if (responseCode != HttpStatus.SC_OK) {

            getLog().error(
                    "HTTP server returned non-OK response: " + responseCode
                            + ": " + method.getStatusText());
            getLog().error(" for URI: " + method.getURI());
            getLog().error("Full error message: " + method.getStatusText());
            throw new IOException(
                    "Buildserver unable to connect to submitserver at " + url
                            + ", Status " + responseCode + ": "
                            + method.getStatusText());
        }

    }
    
	
	/*
	 * (non-Javadoc)
	 *
	 * @see edu.umd.cs.buildServer.BuildServer#getProjectSubmission()
	 */
	@Override
	protected ProjectSubmission<?> getProjectSubmission()
			throws MissingConfigurationPropertyException, IOException {

		try {
		String url = getRequestSubmissionURL();
		MultipartPostMethod method = new MultipartPostMethod(
				url);		

		  String supportedCoursePKList 
		    = getBuildServerConfiguration().getSupportedCourses();

		  String specificProjectNum = getConfig().getOptionalProperty(
	                DEBUG_SPECIFIC_PROJECT);
		  String specificCourse = getConfig().getOptionalProperty(
                  DEBUG_SPECIFIC_COURSE);
		  if (specificCourse != null)
		      supportedCoursePKList = specificCourse;
		
		String specificSubmission = getConfig().getOptionalProperty(
				DEBUG_SPECIFIC_SUBMISSION);
		String specificTestSetup = getConfig().getOptionalProperty(
				DEBUG_SPECIFIC_TESTSETUP);

		if (specificSubmission != null) {
			method.addParameter("submissionPK", specificSubmission);
			if (!isQuiet())
			    System.out.printf("Requesting submissionPK %s%n", specificSubmission);
		}
		if (specificTestSetup != null) {
		    method.addParameter("testSetupPK", specificTestSetup);
		    if (!isQuiet())
		        System.out.printf("Requesting testSetupPK %s%n", specificTestSetup);
		}
		
		if (specificProjectNum != null) {
		    method.addParameter("projectNumber", specificProjectNum);
		}
		
		addCommonParameters(method);
		
		BuildServer.printURI(getLog(), method);

		int responseCode = client.executeMethod(method);
		if (responseCode != HttpStatus.SC_OK) {
			if (responseCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
				getLog().trace("Server returned 503 (no work)");
			} else {
			    String msg = "HTTP server returned non-OK response: " + responseCode
                        + ": " + method.getStatusText();
				getLog().error(msg);
				getLog().error(
						" for URI: " + method.getURI());
			
				getLog().error(
						"Full error message: "
								+ method.getResponseBodyAsString());
				if (responseCode == HttpStatus.SC_BAD_REQUEST) {
				    if (!isQuiet()) {
				        System.err.println(msg);
				        System.out.println(msg);
				    }
				    System.exit(1);
				}
			}
			return null;
		}

		getLog().debug(
				"content-type: " + method.getResponseHeader("Content-type"));
		getLog().debug(
				"content-length: " + method.getResponseHeader("content-length"));
		// Ensure we have a submission PK.
		String submissionPK = getRequiredHeaderValue(method,
				HttpHeaders.HTTP_SUBMISSION_PK_HEADER);
		if (submissionPK == null) {
			if (specificSubmission != null)
				getLog().error(
						"Server did not return submission " + specificSubmission);
			return null;
		}

		// Ensure we have a project PK.
		String testSetupPK = specificTestSetup != null ? specificTestSetup : getTestSetupPK(method);
		if (testSetupPK == null)
			return null;

		// This is a boolean value specifying whether the project jar file
		// is NEW, meaning that it needs to be tested against the
		// canonical project solution. The build server doesn't need
		// to do anything with this value except pass it back to
		// the submit server when reporting test outcomes.
		String isNewTestSetup = getIsNewTestSetup(method);
		if (isNewTestSetup == null)
			return null;

		// Opaque boolean value representing whether this was a
		// "background retest".
		// The BuildServer doesn't need to do anything with this except pass it
		// back to the SubmitServer.
		String isBackgroundRetest = getRequiredHeaderValue(method,
				HttpHeaders.HTTP_BACKGROUND_RETEST);
		if (isBackgroundRetest == null)
			isBackgroundRetest = "no";

		ServletAppender servletAppender = (ServletAppender) getLog()
				.getAppender("servletAppender");
		if (isBackgroundRetest.equals("yes"))
			servletAppender.setThreshold(Level.FATAL);
		else
			servletAppender.setThreshold(Level.INFO);

		String kind = method.getResponseHeader(HttpHeaders.HTTP_KIND_HEADER).getValue();
		String logMsg = "Got submission " +submissionPK +  ", testSetup " + testSetupPK + ", kind: " + kind;
		getLog().info(logMsg);
        
		ProjectSubmission<?> projectSubmission = new ProjectSubmission<TestProperties>(
				getBuildServerConfiguration(), getLog(), submissionPK, testSetupPK,
				isNewTestSetup, isBackgroundRetest, kind);

		projectSubmission.setMethod(method);

		getCurrentFile().delete();
		writeToCurrentFile(submissionPK + "\n" + testSetupPK + "\n" + kind + "\n" +  SystemInfo.getSystemLoad() + "\n" + logMsg);
		
		return projectSubmission;
		} catch (ConnectException e) {
			getLog().info("Unable to connect to " + getBuildServerConfiguration().getSubmitServerURL());
			return null;
		}
	}

	/**
	 * @param method
	 * @return
	 * @throws HttpException
	 */
	@SuppressWarnings("deprecation")
	private String getIsNewTestSetup(MultipartPostMethod method)
			throws HttpException {
		return getRequiredHeaderValue(method,
				HttpHeaders.HTTP_NEW_TEST_SETUP);
	}

	/**
	 * @param method
	 * @return
	 * @throws HttpException
	 */
	@SuppressWarnings("deprecation")
	private String getTestSetupPK(MultipartPostMethod method)
			throws HttpException {
		return getRequiredHeaderValue(method,
				HttpHeaders.HTTP_TEST_SETUP_PK_HEADER);

	}

	@Override
	protected void downloadSubmissionZipFile(ProjectSubmission<?> projectSubmission)
			throws IOException {
		IO.download(projectSubmission.getZipFile(),
				projectSubmission.getMethod());
	}

	@Override
	protected void downloadProjectJarFile(ProjectSubmission<?> projectSubmission)
			throws MissingConfigurationPropertyException, HttpException,
			IOException, BuilderException {
		// FIXME: We should cache these

		MultipartPostMethod method = new MultipartPostMethod(
				getTestSetupURL());
		method.addParameter("testSetupPK", projectSubmission.getTestSetupPK());
		method.addParameter("projectJarfilePK", projectSubmission.getTestSetupPK());
		String supportedCourses = getBuildServerConfiguration().getSupportedCourses();
        method.addParameter("courses", supportedCourses);
        
		BuildServer.printURI(getLog(), method);

		try {
			int responseCode = client.executeMethod(method);
			if (responseCode != HttpStatus.SC_OK) {
				throw new BuilderException(
						"Could not download project test setup from " + getTestSetupURL() + ": " + responseCode
								+ ": " + method.getStatusText());
			}

			getLog().trace("Downloading test setup file");
			IO.download(projectSubmission.getTestSetup(), method);

			// We're passing the project_jarfile_pk so we don't need to read it
			// from
			// the headers

			// wait for a while in case the files have not "settled"
			// TODO: Verify that this is still necessary; should be OK unless
			// run on NFS
			pause(10);

			getLog().trace("Done.");
		} finally {
			method.releaseConnection();
		}

	}

	@Override
	protected void releaseConnection(ProjectSubmission<?> projectSubmission) {
		projectSubmission.getMethod().releaseConnection();
	}

	private void dumpOutcomes(ProjectSubmission<?> projectSubmission) {
		try {
			// Can't dump outcomes if we don't have a test.properties file.
			if (projectSubmission.getTestProperties() == null)
				return;
			ObjectOutputStream out = null;
			File outputFile = new File(projectSubmission
					.getBuilderAndTesterFactory().getDirectoryFinder()
					.getBuildDirectory(), "daemonresults.out");
			try {
				out = new ObjectOutputStream(new FileOutputStream(outputFile));
				projectSubmission.getTestOutcomeCollection().write(out);
			} catch (IOException e) {
				System.err.println("Could not save test outcome collection in "
						+ outputFile.getPath());
				e.printStackTrace(); // OK, this is a command line app
			} finally {
				IOUtils.closeQuietly(out);
			}
		} catch (Exception e) {
			// XXX Prevent this from throwing exception
			getLog().warn(
					"Ignoring error in BuildServerDaemon.dumpOutcomes " + e);
		}
	}

	protected void addCommonParameters(MultipartPostMethod method) {
	    String hostname = getBuildServerConfiguration().getHostname();

	    method.addParameter("testMachine", hostname);
	    method.addParameter("hostname", hostname);
        
        method.addParameter("load", SystemInfo.getSystemLoad());
        String supportedCourses = getBuildServerConfiguration().getSupportedCourses();
        method.addParameter("courses", supportedCourses);
        method.addParameter("javaVersion", System.getProperty("java.version"));
        method.addParameter("serverTimestamp",
                Long.toString(getBuildServerConfiguration().getServerTimestamp()));
        method.addParameter("connectionTimeout", Integer.toString(getConnectionTimeout()));
	}
	
	
    @Override
    protected void reportBuildServerDeath(int submissionPK, int testSetupPK,
            long lastModified, String kind, String load) {
        
        MultipartPostMethod method = new MultipartPostMethod(
                getReportBuildServerDeathURL());

        method.addParameter("submissionPK",Integer.toString(submissionPK));
        method.addParameter("testSetupPK",Integer.toString(testSetupPK));
        
        addCommonParameters(method);
        method.addParameter("kind", kind);
        method.addParameter("lastModified", Long.toString(lastModified));

        try {
            int statusCode = getClient().executeMethod(method);

            if (statusCode != HttpStatus.SC_OK) {
                System.out.println(
                        "Error eporting build server death for submissionPK "
                                + submissionPK + ": status " + statusCode
                                + ": " + method.getStatusText());
            System.out.println(method.getResponseBodyAsString());
            }
        } catch (Exception e) {
           e.printStackTrace();
        }

    }
	       
	@Override
	protected void reportTestResults(ProjectSubmission<?> projectSubmission)
			throws MissingConfigurationPropertyException {

		dumpOutcomes(projectSubmission);

		getLog().info(
				"Test outcome collection for "
						+ projectSubmission.getSubmissionPK()
						+ " for test setup "
						+ projectSubmission.getTestSetupPK() + " contains "
						+ projectSubmission.getTestOutcomeCollection().size()
						+ " entries");

		// Format the test outcome collection as bytes in memory
		ByteArrayOutputStream sink = new ByteArrayOutputStream();
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(sink);
		} catch (IOException ignore) {
			getLog().error("IOException creating ObjectOutputStream");
		}

		TestOutcomeCollection c = projectSubmission.getTestOutcomeCollection();

		// Print some info about the size of the collection
		getLog().info("Got TestOutcomeCollection; size: " + c.size());
		for (TestOutcome to : c.getAllOutcomes()) {
			// Truncate to avoid OutOfMemories
			to.truncateLongTestResult();
			// Most important size to print is the longResult len - it
			// can be really long
			int length = to.getLongTestResult().length();
			getLog().info(
					"  Outcome " + to.getTestNumber() + ": " + to.getTestName() + " = " + to.getOutcome() 
					+ (length > 0 ?  ", longResult len: " +  length : ""));

		}

		try {
			c.write(out);
		} catch (IOException ignore) {
			getLog().error("IOException writing to ObjectOutputStream", ignore);
		} catch (Error e) {
			// Can happen if the long test output is really long; we
			// truncate down to 64K (also the limit imposed by the
			// MySQL 'text' type) in order to avoid this, but we should
			// note it.
			getLog().error("While writing, caught Error", e);
			getLog().error("Rethrowing...");
			throw (e);
		}

		try {
			out.close();
		} catch (IOException ignore) {
			getLog().error("IOException closing ObjectOutputStream");
		}

		byte[] testOutcomeData = sink.toByteArray();
		String subPK = projectSubmission.getSubmissionPK();
		String jarfilePK = projectSubmission.getTestSetupPK();
		int outcomes = projectSubmission.getTestOutcomeCollection().size();
		getLog().info(
				"Test data for submission " + subPK + " for test setup "
						+ jarfilePK + " contains " + testOutcomeData.length
						+ " bytes from " + outcomes + " test outcomes");

		String hostname = getBuildServerConfiguration().getHostname();

		MultipartPostMethod method = new MultipartPostMethod(
				getReportTestResultsURL());

		method.addParameter("submissionPK", projectSubmission.getSubmissionPK());
		method.addParameter("testSetupPK", projectSubmission.getTestSetupPK());
		method.addParameter("projectJarfilePK", projectSubmission.getTestSetupPK());
		method.addParameter("newTestSetup",
				projectSubmission.getIsNewTestSetup());
		
		method.addParameter("newProjectJarfile", projectSubmission.getIsNewTestSetup());
		method.addParameter("isBackgroundRetest",
				projectSubmission.getIsBackgroundRetest());
		method.addParameter("testMachine", hostname);
		method.addParameter("testDurationsMillis", Long.toString(projectSubmission.getTestDurationMillis()));
        
		addCommonParameters(method);
		    
        method.addParameter("kind", projectSubmission.getKind());
        

		
		// CodeMetrics
		if (projectSubmission.getCodeMetrics() != null) {
			getLog().debug(
					"Code Metrics: " + projectSubmission.getCodeMetrics());
			projectSubmission.getCodeMetrics().mapIntoHttpHeader(method);
		}
		method.addPart(new FilePart("testResults", new ByteArrayPartSource(
				"testresults.out", testOutcomeData)));
		printURI(method);

		try {
			getLog().debug(
					"Submitting test results for "
							+ projectSubmission.getSubmissionPK() + "...");

			int statusCode = client.executeMethod(method);
			if (statusCode == HttpStatus.SC_OK)
				getLog().debug(
						"Done submitting test results for submissionPK "
								+ projectSubmission.getSubmissionPK()
								+ "; statusCode=" + statusCode);
			else {

				getLog().error(
						"Error submitting test results for submissionPK "
								+ projectSubmission.getSubmissionPK() + ": "
								+ statusCode + ": " + method.getStatusText());
				getLog().error(method.getResponseBodyAsString());
				// TODO: Should we do anything else in case of an error?
			}
		} catch (HttpException e) {
			getLog().error(
					"Internal error: HttpException submitting test results", e);
			return;
		} catch (IOException e) {
			getLog().error(
					"Internal error: IOException submitting test results", e);
			return;
		} finally {
			getLog().trace("Releasing connection...");
			method.releaseConnection();
			getLog().trace("Done releasing connection");
		}
	}

	/**
	 * Command-line interface.
	 */
	
	@SuppressWarnings("static-access")
    public static Options getOptions() {
	    Options options = new Options();
        Option configFile = OptionBuilder.withArgName( "file" )
                .hasArg()
                .withDescription(  "use given config file" )
                .withLongOpt("config")
                .create( "c");
        Option courseKey = OptionBuilder.withArgName( "courseKey" )
                .hasArg()
                .withDescription(  "use given course key" )
                .create( "course");
     Option downloadOnly = OptionBuilder
                .withDescription(  "download only" )
                .withLongOpt("download")
                .create( "d");
       Option submission = OptionBuilder.withArgName( "submissionPK" )
                .hasArg()
                .withDescription(  "test the specified submission" )
                  .withLongOpt("submission")
                .create( "s");
       
       Option quiet = OptionBuilder.withDescription(  "no output; no warnings if already running" )
                 .withLongOpt("quiet")
               .create( "q");
     
        Option testSetup = OptionBuilder.withArgName( "testSetupPK" )
                .hasArg()
                .withDescription(  "use the specified test setup" )
                 .withLongOpt( "testSetup")
                .create( "t");
        
        Option projectNum =  OptionBuilder.withArgName( "projectNum" )
                .hasArg()
                .withDescription(  "exhaustively retest the specified project" )
                 .withLongOpt( "projectNum")
                .create( "p");
        Option skipDownload =  OptionBuilder.withDescription(  "don't download submission" )
                .create( "skipDownload");
      
        Option onceOption = new Option( "o", "once", false, "quit after handling one request" );
        Option logLevel = OptionBuilder.withArgName("logLevel")
                .hasArg().withDescription("Log4j log level")
                .withLongOpt("logLevel")
                .create( "l");
        Option help = new Option( "h", "help",false, "print this message" );
        options.addOption(help);
        options.addOption(configFile);
        options.addOption(submission);
        options.addOption(skipDownload);
        
        options.addOption(projectNum);
        options.addOption(courseKey);
        
        options.addOption(testSetup);
        options.addOption(onceOption);
        options.addOption(logLevel);
        options.addOption(quiet);
        options.addOption(downloadOnly);
        return options;
	}
	
	private static void printHelp(Options options) {
	    HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "java -jar buildserver.jar <options> ", options );
	}
	
	public static void main(String[] args) throws Exception {
		
		  CommandLineParser parser = new PosixParser();
	    Options  options = getOptions();
	    CommandLine line;
	    try { line = parser.parse( options, args );
	    
	    } catch (Exception e) {
	        printHelp(options);
	        return;
	    }
	    if (line.hasOption("help")) {
	        printHelp(options);
            return;
	    }
	    
	    String [] remainingArgs = line.getArgs();
	  
	   Protocol easyhttps = new Protocol("https",
                new EasySSLProtocolSocketFactory(), 443);
        Protocol.registerProtocol("easyhttps", easyhttps);

        BuildServerDaemon buildServer = new BuildServerDaemon();
        if (line.hasOption("config")) {
            String c = line.getOptionValue("config");
            buildServer.setConfigFile(c);
        } else if (remainingArgs.length == 1)
            buildServer.setConfigFile(remainingArgs[0]);
            
           
        boolean once = line.hasOption("once");

        if (line.hasOption("submission")) {
            once = true;
            buildServer.getConfig().setProperty(DEBUG_SPECIFIC_SUBMISSION,
                    line.getOptionValue("submission"));
            if (line.hasOption("testSetup"))
                buildServer.getConfig().setProperty(DEBUG_SPECIFIC_TESTSETUP,
                        line.getOptionValue("testSetup"));
            if (line.hasOption("skipDownload"))
                buildServer.getConfig().setProperty(DEBUG_SKIP_DOWNLOAD, "true");
            
        } else if (line.hasOption("testSetup")) {
            throw new IllegalArgumentException(
                    "You can only specify a specific test setup if you also specify a specific submission");
        }
        
        if (line.hasOption("projectNum")) {
            buildServer.getConfig().setProperty(DEBUG_SPECIFIC_PROJECT,
                    line.getOptionValue("projectNum"));
        }
        
        if (line.hasOption("quiet"))
            buildServer.setQuiet(true);
        
        if (line.hasOption("course")) {
            buildServer.getConfig().setProperty(DEBUG_SPECIFIC_COURSE,
                    line.getOptionValue("course"));
        }
            
    
        if (line.hasOption("logLevel"))
            buildServer.getConfig().setProperty(LOG4J_THRESHOLD,
                  line.getOptionValue("logLevel"));

        if (line.hasOption("downloadOnly")) {
        	System.out.println("Setting download only");
       	 buildServer.setDownloadOnly(true);
          once = true;
	     }
        if (once) {
            buildServer.getConfig().setProperty(LOG_DIRECTORY, "console");
            buildServer.setDoNotLoop(true);
            buildServer.getConfig().setProperty(
                    DEBUG_PRESERVE_SUBMISSION_ZIPFILES, "true");
        }
        
       
        buildServer.initConfig();
        Logger log = buildServer.getLog();
        
        /** Redirect standard out and err to dev null, since clover
         * writes to standard out and error */
        
        PrintStream systemOut = System.out;
        PrintStream systemErr = System.err;
        if (buildServer.isQuiet()) {
            System.setOut(new PrintStream(new DevNullOutputStream()));
            System.setErr(new PrintStream(new DevNullOutputStream()));
        }
        
        try {
            buildServer.executeServerLoop();
            if (log != null) 
				log.info("Shutting down");
            System.out.println("Shutting down");
            timedSystemExit0();
        } catch (Throwable e) {
            buildServer.clearMyPidFile();
            if (log != null)
                    log.fatal("BuildServerDaemon got fatal exception; waiting for cron to restart me: ",
                            e);
            e.printStackTrace(systemErr);
            System.exit(1);
        }
        if (buildServer.isQuiet()) {
            System.setOut(systemOut);
            System.setErr(systemErr);
        }
    }

	public static void timedSystemExit0() {
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					assert true;
				}
				System.exit(0);
			}
		}, "force shutdown thread");
		t.setDaemon(true);
		t.start();
	}

	private static SecureRandom rng = new SecureRandom();

	private static long nextRandomLong() {
		synchronized (rng) {
			return rng.nextLong();
		}
	}


}
