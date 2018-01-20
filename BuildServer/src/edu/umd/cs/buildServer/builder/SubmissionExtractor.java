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
 * Created on Oct 6, 2004
 */
package edu.umd.cs.buildServer.builder;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.umd.cs.buildServer.BuilderException;
import edu.umd.cs.buildServer.ProjectSubmission;
import edu.umd.cs.marmoset.modelClasses.TestProperties;
import edu.umd.cs.marmoset.utilities.ZipExtractor;
import edu.umd.cs.marmoset.utilities.ZipExtractorException;

/**
 * Extract a project submission into the build directory.
 * 
 * @author David Hovemeyer
 */
public abstract class SubmissionExtractor extends ZipExtractor {

	// Fields
	private String projectRoot;
	private List<String> sourceFileList;
	private Logger log;
	private boolean prunedSourceFileList;

	private final File directory;
	/**
	 * Constructor.
	 * 
	 * @param zipFile
	 *            the submission zipfile
	 * @param directory
	 *            directory to extract the submission into
	 * @param buildServerLog
	 *            BuildServer's Log
	 * @throws BuilderException
	 */
	public SubmissionExtractor(ProjectSubmission<? extends TestProperties> submission, File directory) throws ZipExtractorException {
		this(submission.getZipFile(), directory, submission.getLog());
	}

	
	/**
     * Constructor.
     * 
     * @param zipFile
     *            the submission zipfile
     * @param directory
     *            directory to extract the submission into
     * @param buildServerLog
     *            BuildServer's Log
	 * @throws ZipExtractorException 
     * @throws BuilderException
     */
	public SubmissionExtractor(File zipFile, File directory, Logger buildServerLog) throws ZipExtractorException {
	    super(zipFile);
        this.projectRoot = "";
        this.directory = directory;
        this.log = buildServerLog;
        this.sourceFileList = new LinkedList<String>();
        this.prunedSourceFileList = false;
    }


    File getDirectory() {
	    return directory;
	}
	/**
	 * Set the project root directory inside the submission zipfile. Only files
	 * inside this directory will be extracted from the zipfile.
	 * 
	 * @param projectRoot
	 *            the project root directory
	 */
	public void setProjectRoot(String projectRoot) {
		this.projectRoot = projectRoot;
	}

	/**
	 * Get the list of source files that should be compiled.
	 * 
	 * @return List of source files (Strings)
	 */
	public List<String> getSourceFileList() {
		if (!prunedSourceFileList) {
			pruneSourceFileList(sourceFileList);
			this.prunedSourceFileList = true;
		}

		return sourceFileList;
	}

	/**
	 * @return Returns the log.
	 */
	protected Logger getLog() {
		return log;
	}

	@Override
	public boolean shouldExtract(String entryName) {
		// FIXME: we really should report an error if
		// an entry doesn't begin with the project root
		return entryName.startsWith(projectRoot);
	}

	@Override
	protected String transformFileName(String entryName) {
		return entryName.substring(projectRoot.length());
	}

	@Override
	protected void successfulFileExtraction(String entryName, String fileName) {
		if (isSourceFile(fileName)) {
			this.sourceFileList.add(fileName);
		}
	}

	
	public void extract() throws ZipExtractorException, IOException {
	    extract(directory);
	}
	/**
	 * Return whether or not the file whose name is given is a source file.
	 * 
	 * @param fileName
	 *            the file name
	 * @return true if the file is a source file, false if not
	 */
	protected abstract boolean isSourceFile(String fileName);

	/**
	 * Prune the source file list by removing those that should not be compiled.
	 * A no-op implementation of this method is acceptable.
	 * 
	 * @param sourceFileList
	 *            List of source file names (Strings)
	 */
	protected abstract void pruneSourceFileList(List<String> sourceFileList);
}
