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
import java.io.Serializable;

import edu.umd.cs.buildServer.BuildServerConfiguration;
import edu.umd.cs.buildServer.ConfigurationKeys;
import edu.umd.cs.buildServer.MissingConfigurationPropertyException;

/**
 * Interface which can locate the build and testfiles directories. Depending on
 * what kind of submission we're building and testing, these might be different
 * directories, or they might be the same directory.
 * 
 * @author David Hovemeyer
 */
public abstract class DirectoryFinder implements Serializable, ConfigurationKeys {
	protected File buildServerRoot;

	protected DirectoryFinder(BuildServerConfiguration config)
			throws MissingConfigurationPropertyException {
		this.buildServerRoot = config.getBuildServerRoot();
	}

	/**
	 * Get the build directory.
	 * 
	 * @return the build directory
	 */
	public abstract File getBuildDirectory();

	/**
	 * Get the testfiles directory.
	 * 
	 * @return the testfiles directory
	 */
	public abstract File getTestFilesDirectory();

	/**
	 * Get the buildServer root directory.
	 * 
	 * @return the buildServer root directory
	 */
	public File getBuildServerRoot() {
		return buildServerRoot;
	}
}
