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
 * Created on Jan 21, 2005
 */
package edu.umd.cs.buildServer.builder;

import java.io.File;
import java.util.List;

import edu.umd.cs.buildServer.BuilderException;
import edu.umd.cs.buildServer.ProjectSubmission;
import edu.umd.cs.marmoset.modelClasses.TestProperties;
import edu.umd.cs.marmoset.utilities.ZipExtractorException;

/**
 * SubmissionExtractor for C, OCaml and Ruby submissions.
 * <p>
 * <b>NOTE:</b> "CSubmissionExtractor" is a legacy name. We use the same
 * infrastructure for for building and testing C, OCaml and Ruby code because
 * the process is exactly the same. For more details see {@see CBuilder}.
 * 
 * @author David Hovemeyer
 */
public class CSubmissionExtractor extends SubmissionExtractor {

	/**
	 * Constructor.
	 * 
	 * @param zipFile
	 *            the submission zipfile
	 * @param directory
	 *            directory to extract submission into
	 * @param buildServerLog
	 *            the buildserver's Log
	 * @throws BuilderException
	 */
	public CSubmissionExtractor(ProjectSubmission<? extends TestProperties> submission, File directory) throws ZipExtractorException {
		super(submission, directory);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.buildServer.SubmissionExtractor#isSourceFile(java.lang.String)
	 */
	@Override
	protected boolean isSourceFile(String fileName) {
		return !fileName.endsWith(".o");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.buildServer.SubmissionExtractor#pruneSourceFileList(java.util
	 * .List)
	 */
	@Override
	protected void pruneSourceFileList(List<String> sourceFileList) {
		// We don't do any source file pruning
	}

}
