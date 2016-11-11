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

package edu.umd.cs.buildServer.tester;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import edu.umd.cs.buildServer.builder.JavaBuilder;


/**
 * Find trusted code bases on the system classpath.
 */
final class TrustedCodeBaseFinder {
	private final JavaTester tester;


	TrustedCodeBaseFinder(JavaTester tester) {
		this.tester = tester;
	}

	private Set<String> defined = new HashSet<String>();
	private List<TrustedCodeBase> trustedCodeBaseList = new LinkedList<TrustedCodeBase>();

	/**
	 * Build the list of trusted code bases.
	 */
	public void execute() {
	    File junit = JavaBuilder.getCodeBase(TestCase.class);
//	    tester.getLog().debug("junit at: " + junit);
	    addTrustedCodeBase("buildserver.junit.jar.file", junit.getAbsolutePath());
	    File hamcrest = JavaBuilder.getCodeBase(org.hamcrest.Condition.class);
//	     tester.getLog().debug("hamcrest at: " + hamcrest);
	    addTrustedCodeBase("buildserver.hamcrest.jar.file", hamcrest.getAbsolutePath());
	       
	    File buildserverRoot =tester.getDirectoryFinder().getBuildServerRoot();
//        tester.getLog().debug("buildserver at: " + buildserverRoot);
        addTrustedCodeBase("buildserver.tester.codebase", buildserverRoot.getAbsolutePath());
    	}

	/**
	 * Get collection of trusted codebase objects.
	 *
	 * @return
	 */
	public Collection<TrustedCodeBase> getCollection() {
		return trustedCodeBaseList;
	}

	private void addTrustedCodeBase(String property, String value) {
	    if (defined.contains(property))
	        throw new IllegalArgumentException("already defined " + property);
//		this.tester.getLog().debug(
//				"Trusted code base: " + property + "=" + value);
		trustedCodeBaseList.add(new TrustedCodeBase(property, value));
	}
}
