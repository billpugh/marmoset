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

import org.apache.log4j.Logger;

import edu.umd.cs.buildServer.BuilderException;
import edu.umd.cs.buildServer.ConfigurationKeys;
import edu.umd.cs.buildServer.MissingConfigurationPropertyException;
import edu.umd.cs.buildServer.ProjectSubmission;
import edu.umd.cs.buildServer.tester.JavaTester;
import edu.umd.cs.buildServer.util.ArgumentParser;
import edu.umd.cs.marmoset.modelClasses.JUnitTestProperties;
import edu.umd.cs.marmoset.utilities.ZipExtractorException;

/**
 * Factory for creating Builder and Tester objects for Java ProjectSubmissions.
 * 
 * @author David Hovemeyer
 */
public class JavaBuilderAndTesterFactory extends BuilderAndTesterFactory<JUnitTestProperties>  {


	public JavaBuilderAndTesterFactory(
            ProjectSubmission<JUnitTestProperties> projectSubmission,
            JUnitTestProperties testProperties, Logger log) throws MissingConfigurationPropertyException {
        super(projectSubmission, testProperties, new JavaDirectoryFinder(projectSubmission.getConfig()), log);
        
    }

    @Override
	public JavaBuilder createBuilder()
			throws MissingConfigurationPropertyException, BuilderException,
			ZipExtractorException {
        JavaSubmissionExtractor submissionExtractor = new JavaSubmissionExtractor(
				projectSubmission.getZipFile(),
				directoryFinder.getBuildDirectory(), projectSubmission.getLog());
		// If the buildserver configuration specifies source files to exclude,
		// add them to the SubmissionExtractor
		String excludedSourceFileList = projectSubmission.getConfig().getConfig()
				.getOptionalProperty(
						ConfigurationKeys.EXCLUDED_SOURCE_FILE_LIST);
		if (excludedSourceFileList != null) {
			ArgumentParser parser = new ArgumentParser(excludedSourceFileList);
			while (parser.hasNext()) {
				submissionExtractor.addExcludedSourceFilePattern(parser.next());
			}
		}

		JavaBuilder builder = new JavaBuilder(testProperties, projectSubmission,
				directoryFinder, submissionExtractor);

		builder.addExpectedFile(projectSubmission.getZipFile().getName());
		builder.addExpectedFile(projectSubmission.getTestSetup().getName());
		
		builder.addExpectedFile("security.policy");

		return builder;
	}

	@Override
	public JavaTester createTester()
			throws MissingConfigurationPropertyException {
		return new JavaTester(testProperties,
				projectSubmission, directoryFinder);
	}
}
