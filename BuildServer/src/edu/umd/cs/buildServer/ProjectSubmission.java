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
 * Created on Jan 19, 2005
 */
package edu.umd.cs.buildServer;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.CopyUtils;
import org.apache.log4j.Logger;

import edu.umd.cs.buildServer.BuildServer.RequestStatus;
import edu.umd.cs.buildServer.builder.BuilderAndTesterFactory;
import edu.umd.cs.buildServer.builder.CBuilderAndTesterFactory;
import edu.umd.cs.buildServer.builder.JavaBuilderAndTesterFactory;
import edu.umd.cs.buildServer.builder.ScriptBuilderAndTesterFactory;
import edu.umd.cs.marmoset.modelClasses.CodeMetrics;
import edu.umd.cs.marmoset.modelClasses.JUnitTestProperties;
import edu.umd.cs.marmoset.modelClasses.MakeTestProperties;
import edu.umd.cs.marmoset.modelClasses.MissingRequiredTestPropertyException;
import edu.umd.cs.marmoset.modelClasses.ScriptTestProperties;
import edu.umd.cs.marmoset.modelClasses.TestOutcomeCollection;
import edu.umd.cs.marmoset.modelClasses.TestProperties;
import edu.umd.cs.marmoset.modelClasses.TestPropertyKeys;
import edu.umd.cs.marmoset.utilities.ZipExtractor;

/**
 * A project submission to be compiled and tested. This object stores all of the
 * state information about a submission to avoid having to pass the state as
 * parameters to the various BuildServer subsystems.
 *
 * @author David Hovemeyer
 */
public class ProjectSubmission<T extends TestProperties> implements Serializable, ConfigurationKeys, TestPropertyKeys {
	
    private static SecureRandom random = new SecureRandom();
    public static long nextRandomLong() {
        synchronized (random) {
            return random.nextLong();
        }
    }
    private final long randomId = nextRandomLong();
    private transient BuildServerConfiguration config;
	private transient Logger log;
	private final String submissionPK;
	private final String testSetupPK;
	private final String isNewTestSetup;
	private final String isBackgroundRetest;
	
	
	/** Called only after ProjectSubmission is read via object serialization */
	public void restore(BuildServerConfiguration config, Logger log) {
	    this.config = config;
	    this.log = log;
	}
	/**
	 * Auxiliary information about the source to be built, such as an md5sum of
	 * the classfiles and/or the names of student-written tests. So far we don't
	 * compute any auxiliary information for C builds.
	 */
	private CodeMetrics codeMetrics;

	private File zipFile;
	private File testSetup;
	
	private byte[] testSetupBytes;
	private TreeSet<String> testSetupFiles = new TreeSet<String>();
	
	private int testDurationMillis;

	private final TestOutcomeCollection testOutcomeCollection;

	private transient HttpMethod method;
	private T testProperties;
	private BuilderAndTesterFactory<T> builderAndTesterFactory;
	private final String kind;
	private RequestStatus requestStatus = RequestStatus.IN_PROGRESS;

	/**
	 * Constructor.
	 *
	 * @param config
	 *            the BuildServer's Configuration
	 * @param log
	 *            the BuildServer's Log
	 * @param submissionPK
	 *            the submission PK
	 * @param projectJarfilePK
	 *            the project jarfile PK
	 * @param isNewTestSetup
	 *            boolean: whether or not the submission is for a new project
	 *            jarfile being tested with the canonical project solution
	 * @param kind TODO
	 * @throws MissingConfigurationPropertyException
	 */
	public ProjectSubmission(BuildServerConfiguration config, Logger log,
			String submissionPK, String projectJarfilePK,
			String isNewTestSetup, String isBackgroundRetest, String kind)
			throws MissingConfigurationPropertyException {
		this.config = config;
		this.log = log;
		this.submissionPK = submissionPK;
		this.testSetupPK = projectJarfilePK;
		this.isNewTestSetup = isNewTestSetup;
		this.isBackgroundRetest = isBackgroundRetest;
		this.kind = kind;

		// Choose a name for the zip file based on
		// the build directory and the submission PK.
		File zipFileName = new File(config.getBuildDirectory(),
		        "submission_" + getSubmissionPK() + ".zip");
		this.setZipFile(zipFileName);

		// Choose a name for the project jar file based
		// on the build directory and the project PK.
		File projectJarFileName = new File(
				config.getJarCacheDirectory(), "proj_"
						+ getTestSetupPK() + ".jar");
		this.setTestSetup(projectJarFileName);

		this.testOutcomeCollection = new TestOutcomeCollection();
	}

	/**
	 * @return Returns the config.
	 */
	public BuildServerConfiguration getConfig() {
		return config;
	}

	/**
	 * @return Returns the log.
	 */
	public Logger getLog() {
		return log;
	}

	public String getKind() {
	    return kind;
	}
	public int getTestDurationMillis() {
        return testDurationMillis;
    }

    public void setTestDurationMillis(int testDurationMillis) {
        this.testDurationMillis = testDurationMillis;
    }

    /**
	 * @return Returns the isNewTestSetup value.
	 */
	public String getIsNewTestSetup() {
		return isNewTestSetup;
	}

	/**
	 * @return Returns the projectJarfilePK.
	 */
	public String getTestSetupPK() {
		return testSetupPK;
	}

	/**
	 * @return Returns the submissionPK.
	 */
	public String getSubmissionPK() {
		return submissionPK;
	}

	/**
	 * Get File storing submission zip file.
	 *
	 * @return the File storing the submission zip file
	 */
	public File getZipFile() {
		return zipFile;
	}

	
    public Set<String> getFilesInSubmission() throws IOException {
        HashSet<String> result = new HashSet<String>();
        
        try (ZipInputStream z = new ZipInputStream(new FileInputStream(getZipFile()))) {
            while (true) {
                ZipEntry entry = z.getNextEntry();
                if (entry == null) break;
                result.add(entry.getName());
            }
        }
        return result;
    }
    
    public boolean downloadTestSetup(InputStream source) throws IOException, MissingRequiredTestPropertyException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (BufferedInputStream in = new BufferedInputStream(source)) {
            CopyUtils.copy(in, out);
        }
        testSetupBytes = out.toByteArray();
        testSetupFiles = new TreeSet<String>();
        
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(testSetupBytes))) {
            while (true) {
                ZipEntry entry = z.getNextEntry();
                if (entry == null) break;
                String name = entry.getName();
                testSetupFiles.add(name);
                if (name.equals("test.properties")) {
                    testProperties = (T) TestProperties.initializeTestProperties(z);
                    
                    
                }
                    
            }
        }
        return testProperties != null;
        
    }
    
    public boolean extractFromTestSetup(File directory, String name) throws IOException {
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(testSetupBytes))) {
            while (true) {
                ZipEntry entry = z.getNextEntry();
                if (entry == null) break;
                String thisName = entry.getName();
                if (thisName.equals(name)) {
                    try (FileOutputStream os = new FileOutputStream(new File(directory, name))) {
                      CopyUtils.copy(z, os);
                       os.close();
                       return true;
                    }
                }   
            }
        }
        return false;     
    }
    
    public Set<String> extractAllFromTestSetup(File directory) throws IOException {
        ZipExtractor extractor = new ZipExtractor( zipFile);
        Set<String> extracted = new TreeSet<String>();
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(testSetupBytes))) {
            while (true) {
                ZipEntry entry = z.getNextEntry();
                if (entry == null) break;
                String thisName = entry.getName();
                if (!thisName.equals("test.properties") 
                    && extractor.shouldExtract(thisName)) {
                    extracted.add(thisName);
                    
                    File file = new File(directory, thisName);
                    if (thisName.endsWith("/")) 
                      file.mkdir();
                    else try (FileOutputStream os = new FileOutputStream(file)) {
                      CopyUtils.copy(z, os);
                    }
                }   
            }
        }
        return extracted;
   
        
    }
	/**
	 * Get the File storing the project jar file.
	 *
	 * @return the File storing the project jar file
	 */
	public File getTestSetup() {
		return testSetup;
	}

	/**
	 * @return Returns the testOutcomeCollection.
	 */
	public TestOutcomeCollection getTestOutcomeCollection() {
		return testOutcomeCollection;
	}

	/**
	 * @param method
	 *            The method to set.
	 */
	public void setMethod(HttpMethod method) {
		this.method = method;
	}

	/**
	 * @return Returns the method.
	 */
	public HttpMethod getMethod() {
		return method;
	}


	/**
	 * Get the TestProperties.
	 *
	 * @return the TestProperties
	 */
	public T getTestProperties() {
		return testProperties;
	}

	/**
	 * Based on the language specified in test.properties, create a
	 * BuilderAndTesterFactory.
	 *
	 * @return a BuilderAndTesterFactory to be used to build and test the
	 *         submission
	 * @throws BuilderException
	 */
	@SuppressWarnings("unchecked")
	public BuilderAndTesterFactory<T> createBuilderAndTesterFactory()
			throws BuilderException {
		try {
		    switch(testProperties.getFramework()) {
		    case JUNIT:
		        this.builderAndTesterFactory = (BuilderAndTesterFactory<T>) 
		        new JavaBuilderAndTesterFactory((ProjectSubmission<JUnitTestProperties>) this, 
		                (JUnitTestProperties) testProperties,
		                getLog());
		        break;
		    case SCRIPT:
		    	 this.builderAndTesterFactory  = (BuilderAndTesterFactory<T>) 
			        new ScriptBuilderAndTesterFactory(
			                (ProjectSubmission<ScriptTestProperties>) this, 
	                        (ScriptTestProperties) testProperties, getLog());
			        break;
		    case MAKE:
		        this.builderAndTesterFactory  = (BuilderAndTesterFactory<T>) 
		        new CBuilderAndTesterFactory(
		                (ProjectSubmission<MakeTestProperties>) this, 
                        (MakeTestProperties) testProperties, getLog());
		        break;

		        default:
		            throw new AssertionError();
		    }
			
		} catch (MissingConfigurationPropertyException e) {
			throw new BuilderException(
					"Could not create builder/tester factory for submission", e);
		}
		return builderAndTesterFactory;
	}

	/**
	 * Get the BuilderAndTesterFactory.
	 *
	 * @return the BuilderAndTesterFactory
	 */
	public BuilderAndTesterFactory<T> getBuilderAndTesterFactory() {
		return builderAndTesterFactory;
	}

	

	/**
	 * Get the build output directory. It is not legal to call this method until
	 * the BuilderAndTesterFactory has been created.
	 *
	 * @return the File specifying the build output directory
	 */
	public File getBuildOutputDirectory() {
		return new File(getBuilderAndTesterFactory().getDirectoryFinder()
				.getBuildDirectory(), BuildServer.BUILD_OUTPUT_DIR);
	}

	public File getSrcDirectory() {
		return new File(getBuilderAndTesterFactory().getDirectoryFinder()
				.getBuildDirectory(), BuildServer.SOURCE_DIR);
	}

	public File getInstSrcDirectory() {
		return new File(getBuilderAndTesterFactory().getDirectoryFinder()
				.getBuildDirectory(), BuildServer.INSTRUMENTED_SOURCE_DIR);
	}


	public String getIsBackgroundRetest() {
		return isBackgroundRetest;
	}

	public void setCodeMetrics(CodeMetrics codeMetrics) {
		this.codeMetrics = codeMetrics;
	}

	public CodeMetrics getCodeMetrics() {
		return codeMetrics;
	}

	public void setTestSetup(File testSetup) {
		this.testSetup = testSetup;
	}

	public void setZipFile(File zipFile) {
		this.zipFile = zipFile;
	}

    public RequestStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    public void validateRandomId(ProjectSubmission<T> other) {
        if (this.randomId != other.randomId) 
            throw new RuntimeException("Can't validated project submission random id");
    }
}
