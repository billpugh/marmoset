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
 * Created on Aug 31, 2004
 */
package edu.umd.cs.buildServer.builder;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.umd.cs.buildServer.BuilderException;
import edu.umd.cs.buildServer.CompileFailureException;
import edu.umd.cs.buildServer.ConfigurationKeys;
import edu.umd.cs.buildServer.ProjectSubmission;
import edu.umd.cs.marmoset.modelClasses.CodeMetrics;
import edu.umd.cs.marmoset.modelClasses.TestProperties;
import edu.umd.cs.marmoset.utilities.ZipExtractorException;

/**
 * Build a student project submission. This base class handles the work of
 * extracting the submission into the build directory and invoking the
 * compilation. Subclasses (e.g., JavaBuilder) implement the work of actually
 * building the submission for whatever language/environment is required.
 *
 * @author David Hovemeyer
 */
public abstract class Builder<T extends TestProperties> implements ConfigurationKeys {
	private ProjectSubmission<T> projectSubmission;
	protected T testProperties;
	private DirectoryFinder directoryFinder;
	private SubmissionExtractor submissionExtractor;
	private Set<String> expectedFileSet;
	protected Set<String> filesExtractedFromTestSetup;
	private LinkedHashSet<String> sourceFileList;
	private String compilerOutput;
	/** Auxiliary information (i.e. student written tests, md5sum of classfiles) */
	private CodeMetrics codeMetrics;

	/**
	 * Constructor.
	 *
	 * @param testProperties
	 *            TestProperties loaded from test.properties in the project jar
	 *            file
	 * @param projectSubmisison
	 *            the ProjectSubmission to build
	 */
	protected Builder(T testProperties,
			ProjectSubmission<T> projectSubmission,
			DirectoryFinder directoryFinder,
			SubmissionExtractor submissionExtractor) {
		this.projectSubmission = projectSubmission;
		this.testProperties = testProperties;
		this.directoryFinder = directoryFinder;
		this.submissionExtractor = submissionExtractor;
		this.expectedFileSet = new HashSet<String>();
		this.sourceFileList = new LinkedHashSet<String>();
		this.compilerOutput = "";
	}

	/**
	 * @return Returns the projectSubmission.
	 */
	public ProjectSubmission<T> getProjectSubmission() {
		return projectSubmission;
	}

	/**
	 * @return Returns the sourceFileList.
	 */
	public Collection<String> getSourceFiles() {
		return sourceFileList;
	}

	/**
	 * @return Returns the testProperties.
	 */
	public T getTestProperties() {
		return testProperties;
	}

	/**
	 * @return Returns the log.
	 */
	public Logger getLog() {
		return projectSubmission.getLog();
	}

	/**
	 * @param compilerOutput
	 *            The compilerOutput to set.
	 */
	public void setCompilerOutput(String compilerOutput) {
		this.compilerOutput = compilerOutput;
	}

	/**
	 * Add a file which is expected to exist in the build directory before the
	 * builder is invoked.
	 *
	 * @param fileName
	 *            the name of the expected file
	 */
	public void addExpectedFile(String fileName) {
		expectedFileSet.add(fileName);
	}

	public void extract() throws BuilderException {
		String msg = "extracting submission " + projectSubmission.getSubmissionPK()
				+ " for test setup "
				+ projectSubmission.getTestSetupPK();
		getLog().trace(msg);

		try {
			// Extract submission contents into the build directory
			extractSubmission();

		} catch (IOException e) {
			String message = e .getClass().getSimpleName() + " while " + msg + "; " + e.getMessage();
			getLog().error(message, e);
			throw new BuilderException(message, e);
		}

		msg = "extracting " 
				+ " test setup "+ projectSubmission.getTestSetupPK()
								+ " for "
				+ " submission " + projectSubmission.getSubmissionPK();
		try {
			// Extract submission contents into the build directory
			extractTestFiles();

		} catch (IOException e) {
			String message = e .getClass().getSimpleName() + " while " + msg + "; " + e.getMessage();
			getLog().error(message, e);
			throw new BuilderException(message, e);
		}
		
		// pause to ensure that the unzipped files are stable before
		// compiling
		pause(100);

	}
	/**
	 * Build the project. Throws an exception if the build fails.
	 *
	 * @throws BuilderException
	 *             if the build fails for an unforseen reason: such failures are
	 *             likely to indicate an internal error
	 * @throws CompileFailureException
	 *             if the project does not compile
	 */
	public void execute() throws BuilderException, CompileFailureException  {

		getLog().trace(
				"Preparing to build submission "
						+ projectSubmission.getSubmissionPK()
						+ " for test setup "
						+ projectSubmission.getTestSetupPK());

		// perform any necessary inspections of the source or classfiles
		// this may require compilation, but we will delete the contents of
		// the
		// directory afterwards so that it may be recompile with appropriate
		// flags
		// for regular building and unit testing
		// TODO refactor inspectSubmission() into an
		// ISubmissionInspectionStep
		// XXX this will do nothing for C code

		if (isInspectionStepCompilation()) {
			codeMetrics = inspectSubmission();
			getLog().trace(
					"Code metrics after call to inspectSubmission() are: "
							+ codeMetrics);
		}

		compileProject();
		getLog().info(
				"Submission " + projectSubmission.getSubmissionPK()
						+ " for test setup "
						+ projectSubmission.getTestSetupPK()
						+ " built successfully");
	}

	/**
	 * Extract test files.
	 *
	 * @throws BuilderException
	 * @throws IOException
	 */
	protected void extractTestFiles() throws IOException, ZipExtractorException {
	    filesExtractedFromTestSetup = projectSubmission.extractAllFromTestSetup(directoryFinder.getTestFilesDirectory());
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
			getLog().error("Someone interrupted the pause() method: " + e);
		}
	}

	/**
	 * Get the compiler output, if any. You can call this method to get the
	 * compiler error output after execute() throws a CompileFailureException.
	 *
	 * @throws BuilderException
	 */
	public String getCompilerOutput() {
		return compilerOutput;
	}

	/**
	 * Make sure the directory contains only expected files.
	 */
	private void ensureDirectoryHasExpectedContents() throws BuilderException {
		File buildDirectory = getDirectoryFinder().getBuildDirectory();

		File[] contents = buildDirectory.listFiles();

		if (contents == null) {
			// TODO should I try again here if the directory looks empty
			getLog().error(
					"Build directory " + buildDirectory.toString()
							+ " not readable");
			throw new BuilderException("Build directory "
					+ buildDirectory.toString() + " not readable");
		}

		for (int i = 0; i < contents.length; ++i) {
			File file = contents[i];

			// Ignore .nfs files
			if (file.getName().startsWith(".nfs"))
				continue;

			if (!expectedFileSet.contains(file.getName()))
				throw new BuilderException("Unexpected file " + file.getName()
						+ " in build directory");
		}
	}

	/**
	 * Unzip the submission zipfile. Take care not to write any paths outside
	 * the build directory.
	 */
	protected void extractSubmission() throws BuilderException, IOException,
			ZipExtractorException {
		// Make sure there aren't any unexpected files in the build directory
		// TODO Currently this breaks when I compile things twice (for
		// inspection and for real)
		// Please fix this.
		// ensureDirectoryHasExpectedContents();

		// Subclass may examine the project zipfile to see if there
		// are leading directories that should be ignored.
		final String projectRoot = getProjectPathPrefix();

		// Extract the files into the build directory
		submissionExtractor.setProjectRoot(projectRoot);
		submissionExtractor.extract();

		// another pause to make sure files are set after extraction
		pause(30);

		// Add all source files found in submission
		List<String> files = submissionExtractor.getSourceFileList();
		for (String f : files) {
			if (sourceFileList.contains(f))
				getLog().error("sourceFileList already contains " + f);
		}
		sourceFileList.addAll(files);
	}

	/**
	 * Get the DirectoryFinder.
	 *
	 * @return the DirectoryFinder
	 */
	protected DirectoryFinder getDirectoryFinder() {
		return directoryFinder;
	}

	/**
	 * Examine the submission zipfile to determine if there is a directory
	 * prefix leading to the project base directory.
	 *
	 * @return the project path prefix, or an empty string if the project is in
	 *         the base directory of the submission zipfile
	 * @throws IOException
	 */
	protected abstract String getProjectPathPrefix() throws IOException;

	/**
	 * Compile the source files in the project. If a CompileFailureException is
	 * thrown, getCompilerOutput() may be called to get the error messages from
	 * the compiler.
	 *
	 * @throws BuilderException
	 *             if the compile fails for unexpected reasons (I/O errors,
	 *             etc.)
	 * @throws CompileFailureException
	 *             if the compile fails for expected reasons: e.g., syntax
	 *             errors, linking problems, etc.
	 */
	protected abstract void compileProject() throws BuilderException,
			CompileFailureException;

	/**
	 * Compute auxiliary metrics, such as md5sum of the classfile with debugging
	 * hooks stripped out.
	 * <p>
	 * Note that the inspectSubmission() method is not related to the
	 * ISubmissionInspectStep interface even through they share similar names.
	 *
	 * @throws BuilderException
	 *             if the compile fails for unexpected reasons (I/O errors,
	 *             etc.)
	 * @throws CompileFailureException
	 *             if the compile fails for expectedr reasons: e.g., syntax
	 *             errors, linking problems, etc.
	 * @return a CodeMetrics object representing auxiliary metrics about the
	 *         code, such as an md5sum of the classfiles compiled with -g:none.
	 *         Returns null if the additional information is not needed or
	 *         cannot be computed.
	 */
	protected CodeMetrics inspectSubmission() throws BuilderException,
			CompileFailureException {
		return null;
	}
	
	protected boolean doesInspectSubmission() {
	    return false;
	}

	public CodeMetrics getCodeMetrics() {
		return codeMetrics;
	}

	private boolean inspectionStepCompilcation = false;

	public boolean isInspectionStepCompilation() {
		return inspectionStepCompilcation;
	}

	public void setInspectionStepCompilation(boolean inspectionStepCompilcation) {
		this.inspectionStepCompilcation = inspectionStepCompilcation;
	}
}
