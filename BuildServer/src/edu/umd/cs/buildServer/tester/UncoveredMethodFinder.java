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

/**
 * Created on Feb 25, 2006
 *
 * @author jspacco
 */
package edu.umd.cs.buildServer.tester;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;
import org.apache.log4j.Logger;

import edu.umd.cs.buildServer.BuildServer;
import edu.umd.cs.marmoset.codeCoverage.CodeCoverageResults;
import edu.umd.cs.marmoset.codeCoverage.FileWithCoverage;

/**
 * MethodFinder
 *
 * @author jspacco
 */
public class UncoveredMethodFinder {

	private List<File> classPathEntryList = new LinkedList<File>();

	private Logger log;

	private HashMap<String, SortedMap<Integer, MethodRef>> methodLinenumberMap = new HashMap<String, SortedMap<Integer, MethodRef>>();

	static class MethodRef {
		final String className, methodName, methodSignature;

		MethodRef(String className, String methodName, String methodSignature) {
			this.className = className;
			this.methodName = methodName;
			this.methodSignature = methodSignature;
		}
	}

	private Logger getLog() {
		if (log != null)
			return log;
		log = Logger.getLogger(BuildServer.class);
		return log;
	}

	public UncoveredMethodFinder() {
		// TODO Create a log that prints to stdout
	}

	public void setLog(Logger log) {
		this.log = log;
	}

	private CodeCoverageResults codeCoverageResults;

	/**
	 * Add a classpath entry. This will allow loading of the inspected class
	 * from precisely the codebase desired. Added entries should be specified in
	 * decreasing order of priority (i.e., the first one added has highest
	 * precedence), and will have precedence over system classpath entries.
	 *
	 * @param entry
	 *            classpath entry; filename of a jar or zip file, or the name of
	 *            a directory
	 */
	public void addClassPathEntry(File entry) {
		classPathEntryList.add(entry);
	}

	public void addClassPathEntry(String entry) {
		for (String s : entry.split(File.pathSeparator)) {
			addClassPathEntry(new File(s));
		}
	}

	/**
	 * Set class to inspect.
	 *
	 * @param classFile
	 *            class file containing the class to inspect
	 * @throws ClassFormatException
	 * @throws IOException
	 */
	public void inspectClass(File classFile) throws ClassFormatException,
			IOException {
		getLog().trace(
				"Inspecting class " + classFile.getAbsolutePath()
						+ " for uncovered methods");
		JavaClass inspectedClass = new ClassParser(classFile.getPath()).parse();
		Repository.addClass(inspectedClass);
		inspectClass(inspectedClass);
	}

	/**
	 * Set class to inspect.
	 *
	 * @param classFile
	 *            class file containing the class to inspect
	 * @throws ClassFormatException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void inspectClass(String className) throws ClassFormatException,
			IOException, ClassNotFoundException {
		JavaClass inspectedClass = Repository.lookupClass(className);
		inspectClass(inspectedClass);
	}

	/**
	 * Set class to inspect.
	 *
	 * @param classFile
	 *            class file containing the class to inspect
	 * @throws ClassFormatException
	 * @throws IOException
	 */
	public void inspectClass(JavaClass inspectedClass)
			throws ClassFormatException, IOException {

		SortedMap<Integer, MethodRef> methodMap = methodLinenumberMap
				.get(inspectedClass.getSourceFileName());
		if (methodMap == null) {
			methodMap = new TreeMap<Integer, MethodRef>();
			methodLinenumberMap.put(inspectedClass.getSourceFileName(),
					methodMap);
		}
		for (Method method : inspectedClass.getMethods())
			if (method.getLineNumberTable() != null) {
				for (LineNumber num : method.getLineNumberTable()
						.getLineNumberTable())
					methodMap.put(num.getLineNumber(), new MethodRef(
							inspectedClass.getClassName(), method.getName(),
							method.getSignature()));
			}
	}

	public void initRepository() {
		StringBuffer reposClassPath = new StringBuffer();

		// Add user-specified classpath entries
		for (File f : classPathEntryList) {
			if (reposClassPath.length() > 0)
				reposClassPath.append(File.pathSeparatorChar);
			reposClassPath.append(f.getPath());
		}

		// Add system classpath entries
		if (reposClassPath.length() > 0) {
			reposClassPath.append(File.pathSeparatorChar);
		}
		reposClassPath.append(ClassPath.getClassPath());

		// Create new SyntheticRepository, and make it current
		ClassPath classPath = new ClassPath(reposClassPath.toString());
		// XXX VERY IMPORTANT:
		// We need to clear the cache because the Repository caches classfiles
		// and the BuildServer runs in a loop in the same JVM invocation.
		Repository.clearCache();
		SyntheticRepository repos = SyntheticRepository.getInstance(classPath);
		Repository.setRepository(repos);
	}

	public List<StackTraceElement> findUncoveredMethods() {
		List<StackTraceElement> result = new LinkedList<StackTraceElement>();
		methodLinenumberMap.forEach(( sourceFile,methodMap ) -> {
		
			FileWithCoverage fileWithCoverage = codeCoverageResults
					.getFileWithCoverage(sourceFile);
			// If for some reason we can't find coverage for the classfile
			if (fileWithCoverage == null)
				return;

			// Skip source files that have zero coverage
			if (!fileWithCoverage.isAnythingCovered())
				return;

			SortedSet<Integer> uncoveredMethods = fileWithCoverage
					.getUncoveredMethods();

			for (Integer uncoveredMethodLineNumber : uncoveredMethods) {
				SortedMap<Integer, MethodRef> tailMap = methodMap
						.tailMap(uncoveredMethodLineNumber);
				if (tailMap.isEmpty()) {
					getLog().trace(
							"uncovered line tail map thing is empty for "
									+ " uncoveredMethodLineNumber = "
									+ uncoveredMethodLineNumber
									+ "; no idea how to figure out the filename as well");
					continue;
				}
				Integer nextLineNumber = tailMap.firstKey();
				MethodRef unconveredMethod = methodMap.get(nextLineNumber);
				result.add(new StackTraceElement(unconveredMethod.className,
						unconveredMethod.methodName, sourceFile,
						uncoveredMethodLineNumber));
			}
		});
		return result;
	}

	/**
	 * @return Returns the codeCoverageResults.
	 */
	public CodeCoverageResults getCodeCoverageResults() {
		return codeCoverageResults;
	}

	/**
	 * @param codeCoverageResults
	 *            The codeCoverageResults to set.
	 */
	public void setCodeCoverageResults(
			@Nonnull CodeCoverageResults codeCoverageResults) {
		this.codeCoverageResults = codeCoverageResults;
	}

	public static void main(String[] args) throws Exception {
		UncoveredMethodFinder finder = new UncoveredMethodFinder();
		finder.addClassPathEntry(new File("coverage1.xml"));
		String coverageFile = "coverage2.xml";
		CodeCoverageResults codeCoverageResults = CodeCoverageResults
				.parseFile(coverageFile);

		finder.setCodeCoverageResults(codeCoverageResults);
		finder.inspectClass("somecourse/p1/WebServer$1");

		for (StackTraceElement e : finder.findUncoveredMethods()) {
			System.out.println(e);
		}
	}

}
