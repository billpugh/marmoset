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
 * Created on Jan 20, 2005
 */
package edu.umd.cs.buildServer.builder;

import static edu.umd.cs.buildServer.ConfigurationKeys.RUN_STUDENT_TESTS;
import static edu.umd.cs.buildServer.ConfigurationKeys.SKIP_TESTS;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import edu.umd.cs.buildServer.BuildServerConfiguration;
import edu.umd.cs.buildServer.BuilderException;
import edu.umd.cs.buildServer.CompileFailureException;
import edu.umd.cs.buildServer.ConfigurationKeys;
import edu.umd.cs.buildServer.MissingConfigurationPropertyException;
import edu.umd.cs.buildServer.ProjectSubmission;
import edu.umd.cs.buildServer.inspection.ISubmissionInspectionStep;
import edu.umd.cs.buildServer.tester.Tester;
import edu.umd.cs.buildServer.util.BuildServerUtilities;
import edu.umd.cs.marmoset.modelClasses.TestProperties;
import edu.umd.cs.marmoset.utilities.ZipExtractorException;

/**
 * Factory for creating Builder and Tester objects for a ProjectSubmission.
 * 
 * @author David Hovemeyer
 */
public abstract class BuilderAndTesterFactory<T extends TestProperties> implements Serializable {
    protected final ProjectSubmission<T> projectSubmission;
	protected final DirectoryFinder directoryFinder;
	protected final T testProperties;
	protected final transient Logger log;
	protected boolean downloadOnly = false;
	protected Logger getLog() {
	    return log;
	}

    public BuildServerConfiguration getConfig() {
        return projectSubmission.getConfig();
    }

    public BuilderAndTesterFactory(ProjectSubmission<T> projectSubmission,
            T testProperties, DirectoryFinder directoryFinder, Logger log) {
        super();
        this.projectSubmission = projectSubmission;
        this.testProperties = testProperties;
        this.directoryFinder = directoryFinder;
        this.log = log;
    }
    public  DirectoryFinder getDirectoryFinder()  {
        return directoryFinder;
    }
    
    public void setDownloadOnly() {
    		downloadOnly = true;
    }
    

	public abstract Builder<? super T> createBuilder()
			throws BuilderException, MissingConfigurationPropertyException,
			ZipExtractorException;

	public abstract Tester<? super T> createTester()
			throws MissingConfigurationPropertyException;
	
    public void buildAndTest(File buildDirectory)
            throws BuilderException, MissingConfigurationPropertyException,
            CompileFailureException {
        // Build the submission
        Builder<? super T> builder = null;
        try {
            builder = createBuilder();
        } catch (ZipExtractorException e) {
            throw new BuilderException(e);
        }

        List<File> files = BuildServerUtilities.listDirContents(buildDirectory);
        log.trace("Pristine environment before the first compilation attempt:");
        for (File file : files) {
            log.trace(file.getAbsolutePath());
        }

        log.debug("Extracting submission and test setup");
        builder.extract();
        
        if (downloadOnly)
        	  return;

        if (builder.doesInspectSubmission()) {

            try {
                // This compilation is for the inspection step for md5sums of
                // the
                // classfiles
                // and should *NOT* include any fancy things like code coverage
                builder.setInspectionStepCompilation(true);
                log.debug("Performing first build");
                builder.execute();
                // retrieve auxiliary information (if any) about the build
                projectSubmission.setCodeMetrics(builder.getCodeMetrics());
                log.debug("Inspection-step compile successful!");
            } catch (CompileFailureException e) {
                log.warn("Inspection-step compile failure: " + e.toString());
                log.warn(builder.getCompilerOutput());
                throw e;
            }

            // Run submission inspection steps
            runSubmissionInspectionSteps();

            // TODO Clean up build directory
            // Delete everything that ended up in there from the inspection
            // compilation
            // Possibly re-copying the submission and test-setup files
            // Start by listing out the contents:
            List<File> afterInspectionState = BuildServerUtilities
                    .listDirContents(buildDirectory);
            log.trace("After inspection");
            for (File file : afterInspectionState) {
                log.trace(file.getAbsolutePath());
            }
        }

        try {
            // This compilation is for the inspection step and should *NOT*
            // include any fancy things like code coverage
            builder.setInspectionStepCompilation(false);
            builder.execute();
            log.debug("Compile successful!");
        } catch (CompileFailureException e) {
            log.warn("Compile failure: " + e.toString());
            log.warn(builder.getCompilerOutput());
            throw e;
        }

        if (getConfig().getConfig().getOptionalBooleanProperty(SKIP_TESTS)) {
            log.info("Skipping unit tests");
            return;
        }

        // Test the submission
        Tester<? super T> tester = createTester();
        if (getConfig().getConfig().getOptionalBooleanProperty(
                RUN_STUDENT_TESTS)) {
            log.debug("Enabling execution of student tests for submission "
                    + projectSubmission.getSubmissionPK());
            tester.setExecuteStudentTests(true);
        }
        log.trace("Are we running student tests? "
                + tester.executeStudentTests());
        log.trace("Testing project...");
        // Test the project
        tester.execute();
        log.trace("done with test");

        // Add test outcomes to main collection
        projectSubmission.getTestOutcomeCollection().addAll(
                tester.getTestOutcomeCollection().getAllOutcomes());
    }

	   private void runSubmissionInspectionSteps() throws BuilderException {
	        String lang = projectSubmission.getTestProperties().getLanguage();
	        String steps = getConfig().getConfig().getOptionalProperty(
	                ConfigurationKeys.INSPECTION_TOOLS_PFX + lang);

	        if (steps != null) {
	            getLog().info("Attempting submission inspection steps: " + steps);
	            StringTokenizer tokenizer = new StringTokenizer(steps, ",");
	            while (tokenizer.hasMoreTokens()) {
	                String stepName = tokenizer.nextToken().trim();
	                if (stepName.equals(""))
	                    continue;
	                inspectSubmission(stepName);
	            }
	        }
	    }

	    private void inspectSubmission(String stepName) throws BuilderException {
	        Class<?> inspectionClass = null;

	        try {
	            inspectionClass = Class.forName(stepName);
	        } catch (ClassNotFoundException e) {
	            // Ignore
	        }

	        if (inspectionClass == null) {
	            try {
	                inspectionClass = Class.forName("edu.umd.cs.buildServer.inspection."
	                        + stepName);
	            } catch (ClassNotFoundException e) {
	                getLog().warn(
	                        "Could not load submission inspection step \""
	                                + stepName + "\"");
	                return;
	            }
	        }

	        Object inspectionObj = null;
	        try {
	            inspectionObj = inspectionClass.newInstance();
	        } catch (InstantiationException e) {
	            getLog().warn(
	                    "Could not create submission inspection step \"" + stepName
	                            + "\"");
	            return;
	        } catch (IllegalAccessException e) {
	            getLog().warn(
	                    "Could not create submission inspection step \"" + stepName
	                            + "\"");
	            return;
	        }

	        if (!(inspectionObj instanceof ISubmissionInspectionStep)) {
	            getLog().warn(
	                    "Class " + inspectionClass.getName()
	                            + " does not implement "
	                            + "ISubmissionInspectionStep");
	            return;
	        }
	        @SuppressWarnings("unchecked")
			ISubmissionInspectionStep<T> inspectionStep = (ISubmissionInspectionStep<T>) inspectionObj;
	        inspectionStep.setProjectSubmission(projectSubmission);
	        inspectionStep.execute();
	        projectSubmission.getTestOutcomeCollection().addAll(
	                inspectionStep.getTestOutcomeCollection().getAllOutcomes());
	    }

	   

}
