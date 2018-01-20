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

import org.apache.log4j.Logger;

import edu.umd.cs.buildServer.BuilderException;
import edu.umd.cs.buildServer.MissingConfigurationPropertyException;
import edu.umd.cs.buildServer.ProjectSubmission;
import edu.umd.cs.buildServer.tester.CTester;
import edu.umd.cs.marmoset.modelClasses.MakeTestProperties;
import edu.umd.cs.marmoset.utilities.ZipExtractorException;

/**
 * Factory for producing Builder and Tester objects for C, OCaml and Ruby
 * ProjectSubmissions.
 * <p>
 * <b>NOTE:</b> "CBuilderAndTester" is a legacy name. We use the same
 * infrastructure for building and testing C, OCaml and Ruby code because the
 * process is exactly the same. For more details see {@see CBuilder}.
 *
 * @author David Hovemeyer
 * @author jspacco
 */
public class CBuilderAndTesterFactory extends BuilderAndTesterFactory<MakeTestProperties> {

	public CBuilderAndTesterFactory(
            ProjectSubmission<MakeTestProperties> projectSubmission,
            MakeTestProperties testProperties, Logger log) throws MissingConfigurationPropertyException {
        super(projectSubmission, testProperties, new CDirectoryFinder(projectSubmission.getConfig()), log);
    }


	@Override
	public CBuilder createBuilder()
			throws BuilderException, MissingConfigurationPropertyException,
			ZipExtractorException {
		CSubmissionExtractor submissionExtractor = new CSubmissionExtractor(
				projectSubmission,
				directoryFinder.getBuildDirectory());

		CBuilder builder = new CBuilder(testProperties, projectSubmission,
				directoryFinder, submissionExtractor);
		builder.addExpectedFile(projectSubmission.getZipFile().getName());
		builder.addExpectedFile("test.properties");

		return builder;

	}

	@Override
	public CTester createTester()
			throws MissingConfigurationPropertyException {

		CTester tester = new CTester(testProperties,
				projectSubmission, directoryFinder);

		return tester;
	}

}
