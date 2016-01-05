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
 * Created on Aug 30, 2004
 */
package edu.umd.cs.buildServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.SimpleLayout;

import edu.umd.cs.buildServer.builder.BuilderAndTesterFactory;
import edu.umd.cs.buildServer.util.LoadAverage;
import edu.umd.cs.buildServer.util.ServletAppender;
import edu.umd.cs.buildServer.util.jni.ProcessKiller;
import edu.umd.cs.marmoset.modelClasses.TestOutcome;
import edu.umd.cs.marmoset.modelClasses.TestOutcome.OutcomeType;
import edu.umd.cs.marmoset.modelClasses.TestOutcome.TestType;
import edu.umd.cs.marmoset.modelClasses.TestProperties;
import edu.umd.cs.marmoset.utilities.MarmosetUtilities;
import edu.umd.cs.marmoset.utilities.SystemInfo;
import edu.umd.cs.marmoset.utilities.TestPropertiesExtractor;
import edu.umd.cs.marmoset.utilities.ZipExtractorException;

/**
 * A BuildServer obtains a project submission zipfile and a project jarfile,
 * builds the submission, tests the submission (if the build was successful),
 * and reports the test results.
 *
 * <p>
 * Subclasses implement how submissions and project jarfiles are obtained, and
 * how test results are reported.
 * </p>
 *
 * @author David Hovemeyer
 */
public abstract class BuildServer implements ConfigurationKeys {
	static BuildServer instance;

	// Name of build directory subdirectory where compiled class
	// files are generated.
	public static final String BUILD_OUTPUT_DIR = "obj";
	public static final String SOURCE_DIR = "src";
	public static final String INSTRUMENTED_SOURCE_DIR = "inst-src";

	private static final String LOG4J_FILE_CONFIG = "edu/umd/cs/buildServer/log4j-file.properties";
	private static final String LOG4J_CONSOLE_CONFIG = "edu/umd/cs/buildServer/log4j-console.properties";

	// Status codes from doOneRequest()
	static enum RequestStatus { NO_WORK, SUCCESS, COMPILE_FAILURE, BUILD_FAILURE, ERROR };


	/**
	 * We will sleep at most 2^MAX_SLEEP seconds as the poll interval. We sleep
	 * for shorter periods when there has been work to do recently, longer
	 * periods if there hasn't been for a while.
	 */
	private static final int MAX_SLEEP = 4;

	/** Properties indicating how to connect to the submit server. */
	private Configuration config;

	/** Number of server loop iterations. */
	protected int numServerLoopIterations;

	/** Number of times submit server was polled and there was no work. */
	private int noWorkCount;

	/**
	 * Configuration information for the buildserver (what classes should be
	 * built, for what semester, how many iterations of the server loop, where
	 * is the logfile located, should we keep looping, etc). This class is
	 * designed to encapsulate all the useful configuration information about a
	 * buildServer as an MBean to manage buildServers while they are running.
	 * This class should replace the Configuration class.
	 */
	protected BuildServerConfiguration buildServerConfiguration;

	private Logger log;
	
	private boolean deletedPidFile = false;

	/**
	 * Constructor.
	 */
	public BuildServer() {
		this.config = new Configuration();
		this.noWorkCount = 0;

		if (BuildServer.instance != null)
			throw new IllegalStateException();
		BuildServer.instance = this;
	}

	/**
	 * Get the BuildServer instance.
	 *
	 * @return the BuildServer instance
	 */
	public static BuildServer instance() {
		return instance;
	}

	/**
	 * @return Returns the config.
	 */
	public Configuration getConfig() {
		return config;
	}

	/**
	 * @return Returns the log.
	 */
	public Logger getLog() {
		return log;
	}

	/**
	 * @return The BuildServerConfiguration.
	 */
	public BuildServerConfiguration getBuildServerConfiguration() {
		return buildServerConfiguration;
	}

	private final static Logger buildServerLog = Logger
			.getLogger("edu.umd.cs.buildServer.BuildServer");

	/**
	 * Static getter for the buildServer log.
	 *
	 * @return
	 */
	public static Logger getBuildServerLog() {
		return buildServerLog;
	}

	/**
	 * Write a URI of an HttpMethod to the Log.
	 *
	 * @param log
	 *            the Log
	 * @param method
	 *            the HttpMethod
	 */
	public static void printURI(Logger log, HttpMethod method) {
		try {
			log.trace("URI=" + method.getURI());
		} catch (URIException e) {
			log.error("Could not print URI for HttpMethod", e);
		}
	}

	/**
	 * Write a URI of an HttpMethod to the Log.
	 *
	 * @param method
	 *            the HttpMethod
	 */
	protected void printURI(HttpMethod method) {
		printURI(getLog(), method);
	}

	/**
	 * Populate the Properties object containing the configuration for the build
	 * server. Subclasses must override this to change the way the config
	 * properties are found.
	 *
	 * @param config
	 *            the Properties object containing the configuration
	 * @throws IOException
	 */
	public abstract void initConfig() throws IOException;

	protected void configureBuildServerForMBeanManagement() {
		buildServerConfiguration = new BuildServerConfiguration();
		// Try to configure a BuildServerConfiguration object
		try {
			buildServerConfiguration.loadAllProperties(getConfig());
			// Get MBeanServer
			MBeanServer platformMBeanserver = ManagementFactory
					.getPlatformMBeanServer();
			// Register the BuildServerMBean
			ObjectName buildServerName = new ObjectName(
					"edu.umd.cs.buildServer:id=BuildServerManager");
			platformMBeanserver.registerMBean(buildServerConfiguration,
					buildServerName);
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException(e);
		} catch (MBeanRegistrationException e) {
			throw new RuntimeException(e);
		} catch (NotCompliantMBeanException e) {
			throw new RuntimeException(e);
		} catch (InstanceAlreadyExistsException e) {
			throw new RuntimeException(e);
		} catch (MissingConfigurationPropertyException e) {
			// getLog().warn("Unable to configure (experimental) BuildServerConfiguration object");
			if (!isQuiet()) {
			    System.out.println(e);
			    e.printStackTrace();
			}
			
		}
	}

	/**
	 * Return whether or not a servlet appender should be created when the
	 * server loop executes. By default, this returns true. Subclasses may
	 * override, for example when testing the BuildServer on the command line
	 * there is no server to append messages to.
	 *
	 * @return true if a servlet appender should be used
	 */
	protected boolean useServletAppender() {
		return true;
	}

	/**
	 * Execute the build server server loop.
	 *
	 * @throws IOException
	 * @throws SecurityException
	 */
	public void executeServerLoop() throws Exception {
	    configureBuildServerForMBeanManagement();
        if (alreadyRunning())
          	return;
        try {
        markPid();
		createWorkingDirectories();
		this.log = createLog(getBuildServerConfiguration(), useServletAppender());
		log.info("BuildServer starting with pid " + MarmosetUtilities.getPid());
		log.warn("Alpha test 2015-12-16 21:33");
	       
		prepareToExecute();

		String supportedCourseList = getBuildServerConfiguration().getSupportedCourses();
		getLog().debug(
				"Executing server loop; can build " + supportedCourseList);
		
		doWelcome();
		int overloadCount = 0;
		while (continueServerLoop()) {

			if (LoadAverage.isOverloaded()) {
				overloadCount++;
				getLog().warn(
						"Build server overloaded, weighted load average" +
						" is " + LoadAverage.getWeightedLoadAverage());
				if (overloadCount < 4)
					continue;
			}
			overloadCount = 0;

			RequestStatus rc = doOneRequest();
			log.trace("Done with request");

			// Run GC, encourage finalizers to run
			if (rc == RequestStatus.ERROR) {
				ProcessKiller.killProcessGroup(MarmosetUtilities.getPid(), ProcessKiller.Signal.TERMINATION);
				// we shouldn't reach this point
				return;
			}
			
			System.gc();
			String load = SystemInfo.getSystemLoad();
			if (!SystemInfo.isGood(load))
				log.warn(load);
			// If there was no work, or if the project could
			// not be built due to an internal error,
			// sleep for a while. This will help avoid
			// thrashing the build server when nothing useful
			// can be done.
			if (rc == RequestStatus.NO_WORK || rc == RequestStatus.BUILD_FAILURE) {
				sleep();
			} else {
				noWorkCount = 0;
			}

			++numServerLoopIterations;
		}

		getLog().debug("Server loop finished");
        } catch (Exception e) {
            getLog().error("Server loop terminating due to exception", e);
        } catch (Error e) {
            getLog().error("Server loop terminating due to error", e);
        } finally {
            clearMyPidFile();
		}
	}

	/**
	 * Called before each iteration of the server loop to determine whether or
	 * not the loop should continue. Subclasses may override.
	 *
	 * @return true if the server loop should continue, false if not
	 */
	protected boolean continueServerLoop() {
		File pleaseShutdownFile = getPleaseShutdownFile();
		if (pleaseShutdownFile.exists()) {
			log.fatal("Shutdown requested at " + new Date(pleaseShutdownFile.lastModified()) + " by creation of " + pleaseShutdownFile.getAbsolutePath());
			return false;
		}
		int pidFileContents = getPidFileContents(true);
		if (pidFileContents != MarmosetUtilities.getPid()) {
		    log.fatal("Pid file contains " + pidFileContents 
		            + ", doesn't match our pid of " + MarmosetUtilities.getPid());
            return false;
        }

		if (config.getDebugProperty(DEBUG_DO_NOT_LOOP)
				|| config.getOptionalProperty(DEBUG_SPECIFIC_SUBMISSION) != null)
			return numServerLoopIterations == 0;
		else
			return true;
	}

	/**
	 * Create build, testfiles, jarcache, and log directories if they don't
	 * exist already.
	 *
	 * @throws MissingConfigurationPropertyException
	 * @throws IOException
	 *             if one of the directories couldn't be created
	 */
	private void createWorkingDirectories()
			throws MissingConfigurationPropertyException, IOException {
	    if (buildServerConfiguration == null)
	        throw new IllegalStateException("buildServerConfiguration not initialized");
		createDirectory(buildServerConfiguration.getBuildServerWorkingDir());
		makeDirectoryWorldReadable(buildServerConfiguration.getBuildServerWorkingDir());
		createDirectory(buildServerConfiguration.getTestFilesDirectory());
		createDirectory(buildServerConfiguration.getBuildDirectory());
		createDirectory(buildServerConfiguration.getJarCacheDirectory());
		createDirectory(buildServerConfiguration.getLogDirectory());
	}

	/**
	 * Create given directory if it doesn't already exist.
	 *
	 * @param dirName
	 *            name of the directory to create
	 * @throws IOException
	 *             if the directory couldn't be created
	 */
	private void createDirectory(@CheckForNull File dir) throws IOException {
	    if (dir == null)
	        return;
	    String dirName = dir.getAbsolutePath();
		if (dir.exists()) {
			if (dir.isFile()) {
				throw new IOException("Directory " + dirName
						+ " cannot be created because "
						+ "a file with the same name already exists");
			} else if (!dir.isDirectory()) {
				throw new IOException("Path " + dirName
						+ " exists, but does not seem to be a file "
						+ " or a directory!");
			}
		} else {
			if (!dir.mkdirs())
				throw new IOException("Directory " + dirName
						+ " does not exist, and couldn't create it");

			if (!dir.exists())
				throw new IOException("WTF? " + dir.toString());
		}
	}

	/**
	 * Makes a directory world-readable.
	 * <p>
	 * This will not work on Windows!
	 *
	 * @param dirName
	 *            The directory to make world-readable.
	 * @throws IOException
	 */
	private void makeDirectoryWorldReadable(File dir) throws IOException {
		// TODO Make sure that this is a non-Windows machine.
		
        if (dir.isDirectory()) {
            dir.setReadable(true, false);
            dir.setWritable(true, false);
            dir.setExecutable(true, false);
		}
	}

	/**
	 * Prepare to execute the server loop.
	 */
	protected abstract void prepareToExecute()
			throws MissingConfigurationPropertyException;

	/**
	 * Create the Log object.
	 *
	 * @param config
	 *            the build server Configuration
	 * @param useServletAppender
	 *            if true, add a ServletAppender
	 * @return the Log object
	 * @throws MissingConfigurationPropertyException
	 * @throws IOException
	 */
	public static Logger createLog(BuildServerConfiguration config,
			boolean useServletAppender)
			throws MissingConfigurationPropertyException, IOException {

		@CheckForNull File logDir = config.getLogDirectory();
		String configResource;

		if (logDir == null) {
			configResource = LOG4J_CONSOLE_CONFIG;
		} else {
			System.setProperty("buildserver.log.dir", logDir.getAbsolutePath());
			configResource = LOG4J_FILE_CONFIG;
		}

		Properties log4jProperties = new Properties();
		InputStream in = BuildServer.class.getClassLoader()
				.getResourceAsStream(configResource);
		if (in == null)
			throw new IOException("Could not read resource " + configResource);
		try {
			log4jProperties.load(in);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				// Ignore
			}
		}

		String threshold = config.getConfig().getOptionalProperty(ConfigurationKeys.LOG4J_THRESHOLD);
		if (threshold != null)
		    log4jProperties.setProperty("log4j.appender.fileAppender.Threshold", threshold);
		PropertyConfigurator.configure(log4jProperties);

		Logger logger = Logger.getLogger(BuildServer.class.getName());

		if (useServletAppender) {
			// need to pass the config object to retrieve the SubmitServer
			// password
			ServletAppender servletAppender = new ServletAppender();
			servletAppender.setLayout(new SimpleLayout());
			servletAppender.setThreshold(Level.INFO);
			servletAppender.setName("servletAppender");
			servletAppender.setConfig(config);
			logger.addAppender(servletAppender);
		}

		return logger;
	}

    /**
     * Get a single project from the submit server, and try to build and test
     * it.
     * 
     * @return a status code: NO_WORK if there was no work available, SUCCESS if
     *         we downloaded, built, and tested a project successfully,
     *         COMPILE_ERROR if the project failed to compile, BUILD_ERROR if
     *         the project could not be built due to an internal error
     */
    private RequestStatus doOneRequest() throws MissingConfigurationPropertyException {

        ProjectSubmission<?> projectSubmission = null;
        try {
            // Get a ProjectSubmission to build and test
            projectSubmission = getProjectSubmission();
            if (projectSubmission == null)
                return RequestStatus.NO_WORK;

            long start = System.currentTimeMillis();
            RequestStatus result;
            try {
            	log.trace("Build configuration");
            	log.trace(getConfig());
                if (getConfig().getBooleanProperty(DEBUG_SKIP_DOWNLOAD)) {
                    log.warn("Skipping download");
                } else {
                    cleanWorkingDirectories();

                    log.trace("About to download project");

                    // Read the zip file from the response stream.
                    downloadSubmissionZipFile(projectSubmission);

                    log.trace("Done downloading project");

                    log.warn("Preparing to  process submission "
                            + projectSubmission.getSubmissionPK()
                            + " for test setup "
                            + projectSubmission.getTestSetupPK());

                    // Get the project jar file containing the provided classes
                    // and the secret tests.
                    downloadProjectJarFile(projectSubmission);
                    // log.warn
                }
               

                log.trace("starting build and test");
                long started = System.currentTimeMillis();
                // Now we have the project and the testing jarfile.
                // Build and test it.
                try {
                    buildAndTestProject(projectSubmission);

                    if (getDownloadOnly()) 
                    	return RequestStatus.NO_WORK;
                    // Building and testing was successful.
                    // ProjectSubmission should have had its public, release,
                    // secret and student
                    // TestOutcomes added.
                    addBuildTestResult(projectSubmission, TestOutcome.PASSED,
                            "", started);
                    result = RequestStatus.SUCCESS;
                } catch (BuilderException e) {
                    // treat as compile error
                    getLog().info(
                            "Submission " + projectSubmission.getSubmissionPK()
                                    + " for test setup "
                                    + projectSubmission.getTestSetupPK()
                                    + " did not build", e);

                    // Add build test outcome
                    String compilerOutput = "Building threw builder exception: " + toString(e);

                    getLog().warn(
                            "Marking all classes of tests 'could_not_run' for "
                                    + projectSubmission.getSubmissionPK()
                                    + " and test-setup "
                                    + projectSubmission.getTestSetupPK());
                    
                    buildFailed(projectSubmission, started, compilerOutput);

                    result = RequestStatus.COMPILE_FAILURE;
                } catch (CompileFailureException e) {
                    // If we couldn't compile, report special testOutcome
                    // stating this fact
                    log.info(
                            "Submission " + projectSubmission.getSubmissionPK()
                                    + " did not compile", e);

                    // Add build test outcome
                    String compilerOutput = e.toString() + "\n"
                            + e.getCompilerOutput();
                    buildFailed(projectSubmission, started, compilerOutput);

                    result = RequestStatus.COMPILE_FAILURE;

                } catch (Throwable e) {
                    // Got a throwable
                    log.info(
                            "Submission " + projectSubmission.getSubmissionPK()
                                    + " threw unexpected exception", e);

                    // Add build test outcome
                    String compilerOutput = "Building threw unexpected exception: " + toString(e);
                    buildFailed(projectSubmission, started, compilerOutput);

                    result = RequestStatus.ERROR;
                }
            } finally {
                // Make sure the zip file is cleaned up.
                if (!getConfig().getDebugProperty(
                        DEBUG_PRESERVE_SUBMISSION_ZIPFILES)
                        && !projectSubmission.getZipFile().delete()) {
                    log.error("Could not delete submission zipfile "
                            + projectSubmission.getZipFile());
                }
            }

            writeToCurrentFile("Testing completed ");
            // Send the test results back to the submit server
            long total = System.currentTimeMillis() - start;
            log.info("submissionPK " + projectSubmission.getSubmissionPK()
                    + " took " + (total / 1000) + " seconds to process");
            if (total > Integer.MAX_VALUE)
                log.error("submissionPK %d took %d millisecons to process");
            else
                projectSubmission.setTestDurationMillis((int) total);
            reportTestResults(projectSubmission);

             return result;

        } catch (HttpException e) {
            log.error("Internal error: BuildServer got HttpException", e);
            // Assume this wasn't our fault
            return RequestStatus.NO_WORK;
        } catch(java.net.ConnectException e) {
        	 	log.info("Unable to connect to submit server at " +  getBuildServerConfiguration().getSubmitServerURL());
             return RequestStatus.NO_WORK;
        } catch (IOException e) {
            log.error("Internal error: BuildServer got IOException", e);
            // Assume this is an internal error
            return RequestStatus.BUILD_FAILURE;
        } catch (BuilderException e) {
            log.error("Internal error: BuildServer got BuilderException", e);
            // This is a build failure
            return RequestStatus.BUILD_FAILURE;
        } finally {
            if (projectSubmission != null) {
                releaseConnection(projectSubmission);
            }
            getCurrentFile().delete();
        }
    }
    
    String toString(Throwable e) {
        StringWriter w = new StringWriter();
        PrintWriter pw = new PrintWriter(w);
        e.printStackTrace(pw);
        pw.close();
        return w.toString();
    }

    /**
     * @param projectSubmission
     * @param started
     * @param compilerOutput
     */
    public void buildFailed(ProjectSubmission<?> projectSubmission,
            long started, String compilerOutput) {
        addBuildTestResult(projectSubmission, TestOutcome.FAILED,
                compilerOutput, started);
        
        // Add "cannot build submission" test outcomes for
        // the dynamic test types
        for (TestType testType : TestType.DYNAMIC_TEST_TYPES)
            addSpecialFailureTestOutcome(projectSubmission,
                    testType, "Compiler output:\n" + compilerOutput);
    }

	/**
	 * Ensure build and testfiles directories are completely empty before we
	 * commence building an testing a submission.
	 *
	 * @throws MissingConfigurationPropertyException
	 */
	private void cleanWorkingDirectories()
			throws MissingConfigurationPropertyException {
		cleanUpDirectory(getBuildServerConfiguration().getBuildDirectory());
		cleanUpDirectory(getBuildServerConfiguration().getTestFilesDirectory());
	}

	protected abstract void doWelcome() throws MissingConfigurationPropertyException, IOException;
	/**
	 * Get a ProjectSubmission object representing the submission to be built
	 * and tested.
	 *
	 * @return a ProjectSubmission, or null if the response didn't include all
	 *         of the required information
	 * @throws MissingConfigurationPropertyException
	 * @throws IOException
	 */
	protected abstract ProjectSubmission<?> getProjectSubmission()
			throws MissingConfigurationPropertyException, IOException;

	/**
	 * Download the submission zipfile into the build directory.
	 *
	 * @param projectSubmission
	 *            the ProjectSubmission
	 * @throws IOException
	 */
	protected abstract void downloadSubmissionZipFile(
			ProjectSubmission<?> projectSubmission) throws IOException;

	/**
	 * Release the connection used to contact the submit server.
	 *
	 * @param projectSubmission
	 *            the current ProjectSubmission
	 */
	protected abstract void releaseConnection(
			ProjectSubmission<?> projectSubmission);

	/**
	 * Download the project jarfile into the jarcache directory.
	 *
	 * @param projectSubmission
	 *            the current ProjectSubmission
	 * @throws MissingConfigurationPropertyException
	 * @throws HttpException
	 * @throws IOException
	 * @throws BuilderException
	 */
	protected abstract void downloadProjectJarFile(
			ProjectSubmission<?> projectSubmission)
			throws MissingConfigurationPropertyException, HttpException,
			IOException, BuilderException;

	/**
	 * Report test outcomes for a submission to the submit server.
	 *
	 * @param testOutcomeCollection
	 *            collection of test outcomes to report
	 * @param submissionPK
	 *            the PK of the submission
	 */
	protected abstract void reportTestResults(
			ProjectSubmission<?> projectSubmission)
			throws MissingConfigurationPropertyException;

	/**
	 * Add the outcome of the build test to the given test outcome collection.
	 *
	 * @param projectSubmission
	 *            the ProjectSubmission for which we're adding a build test
	 *            result
	 * @param passed
	 *            pass/fail status
	 * @param longDescription
	 *            compiler error messages (if any)
	 * @param started TODO
	 */
	private void addBuildTestResult(ProjectSubmission<?> projectSubmission,
			@OutcomeType String passed, String longDescription, long started) {
		TestOutcome outcome = new TestOutcome();
		outcome.setTestType(TestOutcome.TestType.BUILD);
		outcome.setTestName("Build Test");
		outcome.setOutcome(passed);
		outcome.setShortTestResult("Build test " + passed);
		outcome.setDetails(null);
		outcome.setLongTestResultCompressIfNeeded(longDescription);
		outcome.setTestNumber("0");
		outcome.setExceptionClassName("");

		outcome.setExecutionTimeMillis(System.currentTimeMillis() - started);

		projectSubmission.getTestOutcomeCollection().add(outcome);
	}

	/**
	 * Add a special failure test outcome indicating that a particular type of
	 * test could not be run because the project did not compile.
	 *
	 * @param projectSubmission
	 *            the ProjectSubmission
	 * @param testType
	 *            the test type
	 * @param longTestResult
	 *            compiler output from the failed build
	 */
	private void addSpecialFailureTestOutcome(
			ProjectSubmission<?> projectSubmission, TestType testType,
			String longTestResult) {
		TestOutcome outcome = new TestOutcome();

		outcome.setTestType(testType);
		outcome.setTestName("All " + testType + " tests");
		outcome.setOutcome(TestOutcome.COULD_NOT_RUN);
		outcome.setShortTestResult(testType
				+ " tests could not be run because the project did not compile");
		outcome.setLongTestResultCompressIfNeeded(longTestResult);
		outcome.setTestNumber("0");

		projectSubmission.getTestOutcomeCollection().add(outcome);

	}

	/**
	 * Build and run tests on given project submission.
	 *
	 * @param projectSubmission
	 *            the ProjectSubmission
	 * @throws CompileFailureException
	 *             if the project can't be compiled
	 * @throws BuilderException
	 * @throws IOException
	 */
	private <T extends TestProperties> void buildAndTestProject(ProjectSubmission<T> projectSubmission)
			throws CompileFailureException,
			MissingConfigurationPropertyException, IOException,
			BuilderException {
		// FIXME Should throw InternalBuildServerException instead of
		// BuilderException
		// Need to differentiate between problems with test-setup and bugs in my
		// servers
		File buildDirectory = getBuildServerConfiguration().getBuildDirectory();

		// Extract test properties and security policy files into build
		// directory
		TestPropertiesExtractor testPropertiesExtractor = null;
		try {
			testPropertiesExtractor = new TestPropertiesExtractor(
					projectSubmission.getTestSetup());
			testPropertiesExtractor.extract(buildDirectory);
		} catch (ZipExtractorException e) {
			throw new BuilderException(e);
		}

        // We absolutely have to have test.properties
        if (!testPropertiesExtractor.extractedTestProperties())
            throw new BuilderException(
                    "Test setup did not contain test.properties");

        T testProperties;
        try {
            // Load test.properties
            File testPropertiesFile = new File(buildDirectory,
                    "test.properties");
            testProperties = (T) TestProperties.load(testPropertiesFile);
        } catch (Exception e) {
            throw new BuilderException(e.getMessage(), e);
        }

		// Set test properties in the ProjectSubmission.
        projectSubmission.setTestProperties(testProperties);

		// validate required files
		Set<String> requiredFiles = testProperties.getRequiredFiles();
		Set<String> providedFiles = projectSubmission.getFilesInSubmission();
		
		requiredFiles.removeAll(providedFiles);
		if (!requiredFiles.isEmpty()) {
		    if (requiredFiles.size() == 1) {
		        String missingFile = requiredFiles.iterator().next();
		        throw new CompileFailureException("Missing required file " + missingFile, 
	                    "");
		    }
		        
		    throw new CompileFailureException("Missing required files", 
		            requiredFiles.toString());
		}
		
		// Create a BuilderAndTesterFactory, based on the language specified
		// in the test properties file
		BuilderAndTesterFactory<T> builderAndTesterFactory = projectSubmission
				.createBuilderAndTesterFactory();

		 if (getDownloadOnly()) {
        	 log.error("Download only; skipping build and test");
        	 builderAndTesterFactory.setDownloadOnly();
        } 
		builderAndTesterFactory.buildAndTest(buildDirectory, testPropertiesExtractor);
	}

 
	/**
	 * Delete all files in given directory.
	 *
	 * @param dir
	 *            the directory
	 */
	protected static void cleanUpDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] contents = dir.listFiles();
			if (contents != null) {
				for (int i = 0; i < contents.length; ++i) {
					deleteRecursive(contents[i]);
				}
			}
		}
	}

	/**
	 * Recursively delete file and all subdirectories (if any). This is done on
	 * a best-effort basis: no errors are reported if we can't delete something.
	 *
	 * @param file
	 *            the file or directory to delete
	 */
	private static void deleteRecursive(File file) {
		if (file.isDirectory()) {
			File[] contents = file.listFiles();
			if (contents != null) {
				for (int i = 0; i < contents.length; ++i)
					deleteRecursive(contents[i]);
			}
		}
		if (!file.delete())
			getBuildServerLog().warn(
					"Unable to delete " + file.getAbsolutePath());
	}

	Random random = new Random();
	/**
	 * Sleep for a while.
	 */
	private void sleep() throws InterruptedException {
		// Exponential decay: increase sleep time by a factor of
		// two each time submit server is polled and there is
		// no work (but capped at 2^MAX_SLEEP seconds).

		// TODO: We might want to add a random factor in here,
		// to prevent multiple build servers from getting
		// in lockstep.

		if (noWorkCount > MAX_SLEEP)
			noWorkCount = MAX_SLEEP;
		int sleepTime = (1 << noWorkCount) * 1000;
		log.trace("Sleeping for " + sleepTime + " milliseconds");
		System.gc();
		Thread.sleep(sleepTime + random.nextInt(sleepTime));

		++noWorkCount;
	}

	/**
	 * Sleeps for a given number of a millis. Logs an error if interrupted
	 * exception happens but doesn't throw the exception since it should never
	 * happen.
	 *
	 * @param millis
	 *            number of millis to sleep for
	 */
	protected void pause(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			log.error("Someone interrupted the pause() method", e);
		}
	}

	/*
	 * This is here because BuildServer used to be a concrete class. Now the
	 * daemon-mode functionality is in BuildServerDaemon.
	 */
	public static void main(String[] args) throws Exception {
		BuildServerDaemon.main(args);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.umd.cs.buildServer.BuildServerMBean#getNumServerLoopIterations()
	 */
	public int getNumServerLoopIterations() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @return Returns the shutdownRequested.
	 */
	public boolean getDoNotLoop() {
		return config.getDebugProperty(DEBUG_DO_NOT_LOOP);
	}

	/**
	 * @param shutdownRequested
	 *            The shutdownRequested to set.
	 */
	public void setDoNotLoop(boolean shutdownRequested) {
		config.setProperty(DEBUG_DO_NOT_LOOP, "true");
	}
	public boolean getDownloadOnly() {
		boolean result = config.getDebugProperty(DOWNLOAD_ONLY);
		return result;
	}
	public boolean isQuiet() {
        return config.getOptionalBooleanProperty(SERVER_QUIET);
    }
	
	public void setQuiet(boolean quiet) {
	    config.setProperty(SERVER_QUIET, Boolean.toString(quiet));
    }
	public void setDownloadOnly(boolean downloadOnly) {
		config.setProperty(DOWNLOAD_ONLY, Boolean.toString(downloadOnly));
	}

	public void markPid() throws Exception {
		File pidFile = getPidFile();
		PrintWriter out = new PrintWriter(new FileWriter(pidFile));
		int pid = MarmosetUtilities.getPid();
        out.println(pid);
		out.close();
		Thread.sleep(2000);
		if (!pidFile.exists())
		    getLog().log(Level.ERROR, "Pid file not created: " + pidFile);
		else if (!pidFile.canRead())
		    getLog().log(Level.ERROR, "Pid file cannot be read: " + pidFile);
		    
		int pidFromFile = getPidFileContents(true);
		if (pidFromFile != pid) {
		    getLog().log(Level.ERROR, "After writing " + pid + " to " + pidFile + " got " + pidFromFile);
		    throw new RuntimeException("Could not write pid file");
		}
		File pleaseShutdownFile = getPleaseShutdownFile();
		if (pleaseShutdownFile.exists())
			pleaseShutdownFile.delete();
		
	}


	 public File getFileInWorkingDir(String name) {
	        return new File(
	                buildServerConfiguration.getBuildServerWorkingDir(), name);
	    }
	
    public File getPleaseShutdownFile() {
        return getFileInWorkingDir("pleaseShutdown");
    }

    public File getPidFile() {
        return getFileInWorkingDir("pid");
    }

    public File getCurrentFile() {
        return getFileInWorkingDir("current");
    }
    
    public void clearMyPidFile() {
        if (!pidFileExists()) {
            if (!deletedPidFile)
                getLog().warn("Pid file missing, can't delete");
        } else if ( getPidFileContents(true) ==  MarmosetUtilities.getPid()) {
            getLog().error("Deleting my pid file");
            getPidFile().delete();
            deletedPidFile = true;
        }
    }
    
    public void writeToCurrentFile(String msg) {
        PrintWriter out = null;
        try {
        out = new PrintWriter(new FileWriter(getCurrentFile(), true));
        out.println(msg);
        } catch (Exception e) {
            getLog().error("Error writing " + msg + " to current file", e);
        } finally {
          if (out != null)
              out.close();
        }
        
        
    }
    
    public boolean pidFileExists() {
        File pidFile = getPidFile();
        return pidFile.exists() && pidFile.canRead();
    }
    public int getPidFileContents(boolean shouldExist) {
        File pidFile = getPidFile();
        if (!pidFile.exists()) {
            if (shouldExist) 
                getLog().log(Level.ERROR, "pid file doesn't exist");
            return -1;
        }
        try {
        BufferedReader r = new BufferedReader(new FileReader(pidFile));
        int oldPid = Integer.parseInt(r.readLine());
        r.close();
        return oldPid;
        } catch (Exception e) {
            getLog().log(Level.ERROR, "Unable to get pid file contents", e);
            return -2;
        }
        
    }
	public boolean alreadyRunning() throws Exception {
	    int oldPid = getPidFileContents(false);
	    if (oldPid < 0)
	        return false;
		ProcessBuilder b = new ProcessBuilder(new String[] { "/bin/ps", "xww",
				"-o", "pid,lstart,user,state,pcpu,cputime,args" });
		String user = System.getProperty("user.name");
	
		Process p = b.start();
		try {
		Scanner s = new Scanner(p.getInputStream());
		String header = s.nextLine();
		// log.trace("ps header: " + header);
		while (s.hasNext()) {
			String txt = s.nextLine();
			if (!txt.contains(user))
				continue;
	
			int pid = Integer.parseInt(txt.substring(0, 5).trim());
			if (pid == oldPid) {
			    if (!isQuiet()) {
			        System.out.println("BuildServer is already running");
			        System.out.println(txt);
			    }
				return true;
			} 
		}
		s.close();
		} finally {
		    p.destroy();
		}
        long pidLastModified = getPidFile().lastModified();
        String msg = "Previous buildserver pid " + oldPid + " died; had started at " + new Date(pidLastModified);
        
	    System.out.println(msg);
	    File currentFile = getCurrentFile();
	    long currentFileLastModified = currentFile.lastModified();
        if (currentFile.exists() && currentFileLastModified >= pidLastModified) {
	       Scanner scanner = new Scanner(currentFile);
	       int submissionPK = scanner.nextInt();
	       int testSetupPK = scanner.nextInt();
	       scanner.nextLine(); // skip EOL
	       String kind = scanner.nextLine().trim();
	       String load = scanner.nextLine().trim();
	       scanner.close();
	       reportBuildServerDeath(submissionPK, testSetupPK, currentFileLastModified, kind, load); 
	    }
        currentFile.delete();
        getPidFile().delete();
        System.out.println("Deleting old pid file");
		return false;
	
	}



abstract protected void reportBuildServerDeath(
        int submissionPK, int testSetupPK, long lastModified, String kind, String load)
      ;

}
// vim:ts=4
