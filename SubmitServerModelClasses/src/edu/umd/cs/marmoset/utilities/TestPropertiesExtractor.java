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

package edu.umd.cs.marmoset.utilities;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @author David Hovemeyer
 * @author Jaime Spacco
 */
public class TestPropertiesExtractor extends ZipExtractor {
	private final Set<String> extractedSet = new HashSet<String>();
	
	public TestPropertiesExtractor(File testSetupFile) throws ZipExtractorException{
		super(testSetupFile);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.buildServer.ZipExtractor#shouldExtract(java.lang.String)
	 */
	@Override
	public boolean shouldExtract(String entryName) {
		return entryName.equals("test.properties") || entryName.equals("security.policy");
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.buildServer.ZipExtractor#successfulFileExtraction(java.lang.String, java.lang.String)
	 */
	@Override
	protected void successfulFileExtraction(String entryName, String filename) {
		extractedSet.add(entryName);
	}
	
	public boolean extractedTestProperties() {
		return extractedSet.contains("test.properties");
	}
	
	public boolean extractedSecurityPolicyFile() {
		return extractedSet.contains("security.policy");
	}
}
