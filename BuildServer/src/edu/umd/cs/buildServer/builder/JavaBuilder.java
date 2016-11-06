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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.dom4j.DocumentException;

import com.google.common.base.Strings;

import edu.umd.cs.buildServer.BuilderException;
import edu.umd.cs.buildServer.CompileFailureException;
import edu.umd.cs.buildServer.ConfigurationKeys;
import edu.umd.cs.buildServer.MissingConfigurationPropertyException;
import edu.umd.cs.buildServer.ProjectSubmission;
import edu.umd.cs.buildServer.inspection.CodeMetricsComputation;
import edu.umd.cs.buildServer.tester.TestRunner;
import edu.umd.cs.buildServer.util.BuildServerUtilities;
import edu.umd.cs.buildServer.util.CombinedStreamMonitor;
import edu.umd.cs.buildServer.util.IO;
import edu.umd.cs.buildServer.util.Untrusted;
import edu.umd.cs.diffText.TextDiff;
import edu.umd.cs.marmoset.modelClasses.CodeMetrics;
import edu.umd.cs.marmoset.modelClasses.JUnitTestProperties;
import edu.umd.cs.marmoset.modelClasses.TestOutcome;
import edu.umd.cs.marmoset.modelClasses.TestPropertyKeys;

/**
 * Build a Java submission.
 *
 * @author David Hovemeyer
 */
public class JavaBuilder extends Builder<JUnitTestProperties> implements TestPropertyKeys {
    
    private static final String SECURITY_POLICY_PATH = "edu/umd/cs/buildServer/security.policy";
    
	/**
	 * Constructor.
	 *
	 * @param testProperties
	 *            TestProperties loaded from project jarfile's test.properties
	 * @param projectSubmission
	 *            the ProjectSubmission to build
	 * @param directoryFinder
	 *            DirectoryFinder used to locate build and testfiles directories
	 */
	public JavaBuilder(JUnitTestProperties testProperties,
			ProjectSubmission<JUnitTestProperties> projectSubmission,
			DirectoryFinder directoryFinder,
			JavaSubmissionExtractor submissionExtractor) {
		super(testProperties, projectSubmission, directoryFinder,
				submissionExtractor);
	}

	
	/*
	 * Get the directory prefix leading to the Java project. Returns an empty
	 * string if the project is in the root directory of the submission zipfile.
	 *
	 * Right now, the only thing we do is to look for an Eclipse ".project"
	 * file. If we find it, that is where the project is.
	 */
	@Override
	protected String getProjectPathPrefix() throws IOException {
		String prefix = "";
		ZipFile z = new ZipFile(getProjectSubmission().getZipFile());
		try {
			Enumeration<? extends ZipEntry> e = z.entries();
			while (e.hasMoreElements()) {
				ZipEntry entry = e.nextElement();
				String entryName = entry.getName();
				// XXX Note that we're only looking for something that ends with
				// .project
				// so it would be really easy for this algorithm to mess up!
				// Also, note that the order in which files come out of the
				// zipfile is
				// extremely important! E.g. if the order is
				//
				// Images/.project
				// .project
				//
				// Then we will get the wrong prefix ("Images") instead of an
				// empty path.
				if (entryName.endsWith(".project")) {
					prefix = entryName.substring(0, entryName.length()
							- ".project".length());
					break;
				}
			}
		} finally {
			try {
				z.close();
			} catch (IOException ignore) {
				// Ignore
			}
		}
		return prefix;
	}

	
	@Override
	public void extract() throws BuilderException {
	    super.extract();
	    addBuildServerPermissionsToSecurityPolicyFile(
                    getDirectoryFinder().getTestFilesDirectory());
	}
	
	 /**
     * Extract the default security.policy file into the testfiles directory.
     *
     * @param testFilesDirectory
     *            the testfiles directory
     * @throws BuilderException
     */
    private void addBuildServerPermissionsToSecurityPolicyFile(File testFilesDirectory)
            throws BuilderException {
        InputStream in = getClass().getClassLoader().getResourceAsStream(
                SECURITY_POLICY_PATH);
        if (in == null)
            throw new BuilderException("Could not find default security policy");
        OutputStream out = null;
        try {
            File file = new File(
                    testFilesDirectory, "security.policy");
            out = new BufferedOutputStream(new FileOutputStream(file, true));
            IO.copyStream(in, out);
            out.flush();
        } catch (IOException e) {
            throw new BuilderException(
                    "Could not create/update security.policy file", e);
        } finally {
            IO.closeSilently(in, out);
        }
    }
    
    
    @Override
    protected boolean doesInspectSubmission() {
        return true;
    }
    

	@Override
	protected CodeMetrics inspectSubmission() throws BuilderException,
			CompileFailureException {
		if (getProjectSubmission().getConfig().getConfig().getOptionalBooleanProperty(
				ConfigurationKeys.SKIP_BUILD_INFO)) {
			getLog().info("Skipping build information step");
			return null;
		}

		// perform the compile with debugging turned off so that we don't have a
		// linenumber or variable map and the md5sum of the classfiles will be the same.
		doCompile(false, "-g:none" );

		// Now get a list of all the class files
		File outputDir = getProjectSubmission().getBuildOutputDirectory();

		// get a list of the classfiles
		List<File> classFileList = BuildServerUtilities
				.listClassFilesInDirectory(outputDir, getLog());

		for (File file : classFileList) {
			getLog().trace("classfile to inspect: " + file);
		}

		// Now get a list of the source files
		Set<File> sourceFileList = new LinkedHashSet<File>();
		// convert from Strings to files
		for (Iterator<String> ii = getSourceFiles().iterator(); ii.hasNext();) {
			String filePath = ii.next();
			File file = new File(getDirectoryFinder().getBuildDirectory(),
					filePath);
			sourceFileList.add(file);
		}

		try {
			CodeMetrics codeMetrics = new CodeMetrics();
			codeMetrics.setMd5sumClassfiles(classFileList);
			codeMetrics.setMd5sumSourcefiles(sourceFileList);
			int sz = CodeMetricsComputation.computeCodeSegmentSize(
					outputDir, classFileList,
					outputDir.getAbsolutePath());
			codeMetrics.setCodeSegmentSize(sz);
			return codeMetrics;
		} catch (IOException e) {
			getLog().error("Unable to compute md5sum due to IOException!", e);
		} catch (NoSuchAlgorithmException e) {
			getLog().error("md5 algorithm not found!", e);
		} catch (ClassNotFoundException e) {
			getLog().error(
					"Unable to find and load one of the classes in "
							+ outputDir, e);
		}
		return null;
	}

	Collection<File> findJavaSourceFiles(@Nonnull File dir) throws IOException {
	    if (dir == null)
	        throw new NullPointerException("Null dir");
	    Collection<File> javaFiles = new TreeSet<File>();
	    Collection<String> all = new HashSet<String>();
	    String root = dir.getCanonicalPath();

	    findJavaSourceFiles(root, dir, javaFiles, all);
	    return javaFiles;
	}
	    
	void findJavaSourceFiles(String root, File dir, Collection<File> javaFiles, Collection<String> all) {
	    if (dir == null)
            throw new NullPointerException("Null dir");
        File[] listFiles = dir.listFiles();
        if (listFiles == null) {
            getLog().log(Level.ERROR, "Unable to list files in " + dir +", exists = " + dir.exists() 
                    + ", readable = " + dir.canRead()
                    + ", is dir = " + dir.isDirectory());
            return;
        }
        for(File f : listFiles) {
	        String fullpath;
	        try {
	            fullpath = f.getCanonicalPath();
	        } catch (IOException e) {
	            continue;
	        }
	        if (fullpath == null) {
	            getLog().warn("Got null canonical path for " + f);
	            continue;
	        }
	        
	        if (!all.add(fullpath))
	            continue;
	        if (!fullpath.startsWith(root)) 
	            continue;
	        
	        if (f.isDirectory()) 
	            findJavaSourceFiles(root, f, javaFiles, all);
	        else if (fullpath.endsWith(".java")) {
	            javaFiles.add(f);
	        }
	    }
	}
	/**
	 * Compile the project.
	 * @param generateCodeCoverage
	 *            If true, then compile in the "inst-src" directory containing
	 *            classfiles instrumented by Clover rather than the raw
	 *            classfiles extracted from the projectSubmission. <b>NOTE:</b>
	 *            It's now <b>REQUIRED</b> that all source files are rooted in a
	 *            "src" directory!
	 * @param options
	 *            Additional options passed to javac, such as -g:none that keeps
	 *            debugging information out of the classfile.
	 *
	 * @throws BuilderException
	 *             thrown when the compile fails for unexpected reasons (i.e.
	 *             IOException)
	 * @throws CompileFailureException
	 *             thrown when the compile fails for expected reasons (i.e.
	 *             syntax errors, etc.)
	 */
	private void doCompile(boolean generateCodeCoverage, String... options)
			throws BuilderException, CompileFailureException {
	    
	    // Determine Java -source value to use.
        @CheckForNull String javaSourceVersion = getTestProperties().getJavaSourceVersion();
        if (javaSourceVersion != null && javaSourceVersion.length() == 0)
            javaSourceVersion = null;
        
        
		// CODE COVERAGE:
		// Use the programmic interface to Clover to instrument code for
		// coverage
		@Nonnull File instSrcDirectory = getProjectSubmission().getInstSrcDirectory();
        if (generateCodeCoverage && Clover.isAvailable()) {
			// TODO Put this clover database in the student's build directory
			// TODO Also clean up this file when we're done with it!
			String cloverDBPath;
			try {
				cloverDBPath = getProjectSubmission().getConfig().getConfig()
						.getRequiredProperty(CLOVER_DB);
			} catch (MissingConfigurationPropertyException e) {
				throw new BuilderException(e);
			}

			
			File cloverDB = new File(cloverDBPath);
			if (cloverDB.exists()) {
				if (!cloverDB.delete())
					getLog().warn(
							"Unable to delete old clover DB at " + cloverDBPath);
			}
		
			String[] cliArgs;
			if (javaSourceVersion == null)
			    cliArgs = new String[] {
					"-i",
					cloverDBPath,
					"-s",
					getProjectSubmission().getSrcDirectory().getAbsolutePath(),
					"-d",
					instSrcDirectory
							.getAbsolutePath() };
			else  cliArgs = new String[] {
		                    "-source",
		                    javaSourceVersion,
		                    "-i",
		                    cloverDBPath,
		                    "-s",
		                    getProjectSubmission().getSrcDirectory().getAbsolutePath(),
		                    "-d",
		                    instSrcDirectory
		                            .getAbsolutePath(),
		                            "--recordTestResults", "true",
		                            "-p", "threaded",
		                            "-f", "10",
		                            "-v"};
			String coverageMarkupCmd = " ";
			for (int ii = 0; ii < cliArgs.length; ii++) {
				coverageMarkupCmd += cliArgs[ii] + " ";
			}
			getLog().trace("Clover instrumentation args: " + coverageMarkupCmd);
			int result = Clover.cloverInstrMainImpl(cliArgs);
			if (result != 0) {
				throw new BuilderException(
						"Clover was unable to instrument the source code in "
								+ getProjectSubmission().getSrcDirectory()
										.getAbsolutePath());
			}
		}

		if (getSourceFiles().isEmpty())
			throw new CompileFailureException("Submission "
					+ getProjectSubmission().getSubmissionPK()
					+ " contains no source files", "");

		// Create compiler output directory
		File outputDir = getProjectSubmission().getBuildOutputDirectory();
		if (!outputDir.isDirectory() && !outputDir.mkdir()) {
			throw new BuilderException(
					"Could not create compiler output directory ");
		}

		
		// Determine the classpath to be used for compiling.
		StringBuffer cp = new StringBuffer();
		cp.append(getProjectSubmission().getTestSetup().getAbsolutePath());
		appendJUnitToClassPath(cp);
		appendLibrariesToClassPath(cp);
		if (generateCodeCoverage)
			appendCloverToClassPath(cp);

		// Specify javac command line arguments.
		ArrayList<String> args = new ArrayList<String>();
		args.add("javac");
		args.add("-encoding");
		args.add("UTF-8");
		// Specify classpath
		args.add("-classpath");
		args.add(cp.toString());
		// Generate compiled class files in the output directory
		args.add("-d");
		args.add(outputDir.getAbsolutePath());
		if (javaSourceVersion != null) {
		    // Specify Java source version.
		    args.add("-source");
		    args.add(javaSourceVersion);
		}
		// add optional args
		if (options != null) {
			args.addAll(Arrays.asList(options));
		}
		
		// Compile all source files found in submission

		// XXX Code now MUST be in a "src" directory!
		if (generateCodeCoverage && Clover.isAvailable() ) {
		    try {
                for(File j : findJavaSourceFiles(instSrcDirectory)) {
                    args.add(j.getPath());
                }
            } catch (IOException e) {
                throw new BuilderException(e);
            }
			
		} else {
			// TODO rewrite the source files into the appropriate directory
			// anyway
			args.addAll(getSourceFiles());
		}

		if (getLog().isEnabledFor(Level.DEBUG)) {
			StringBuffer buf = new StringBuffer();
			for (Iterator<String> i = args.iterator(); i.hasNext();) {
				buf.append(i.next() + " ");
			}
			getLog().debug("Javac command: " + buf.toString());
		}

		
		try {
			Process javac = Untrusted.execute(
					getDirectoryFinder().getBuildDirectory(), args.toArray(new String[args.size()]));

			// Capture stdout and stderr from the process
			CombinedStreamMonitor monitor = new CombinedStreamMonitor(
					javac.getInputStream(), javac.getErrorStream());
			monitor.start();

			// Wait for process to execute, and for all process output
			// to be read
			int exitCode = javac.waitFor();
			monitor.join();

			// If compile failed, collect output messages
			// and throw a CompileFailureException
			if (exitCode != 0) {
				setCompilerOutput(monitor.getCombinedOutput());

				throw new CompileFailureException("Compile failed",
						this.getCompilerOutput());
			}

			// Looks like compilation succeeded.
			// Sleep for a few seconds to try to workaround some of
			// the mysterious "file not found" problems we've been
			// seeing when trying to execute the project.
			// (These may be NFS-related.)
			pause(10);
			

		} catch (IOException e) {
			throw new BuilderException("Couldn't invoke java", e);
		} catch (InterruptedException e) {
			throw new BuilderException("Javac wait was interrupted", e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.umd.cs.buildServer.Builder#compileProject()
	 */
	@Override
	protected void compileProject() throws BuilderException,
			CompileFailureException {

		// Don't use instrumented source directories when this compilation is
		// for the
		// inspection step. Otherwise FindBugs reports lots of things introduced
		// by Clover.
		if (isInspectionStepCompilation())
			doCompile(false, "-g");
		else
			doCompile(isPerformCodeCoverage(), "-g");
	}

	/**
     * Should we use the directory with src code instrumented for code coverage?
     *
     * TODO Instrumented source is specific to Clover; other code coverage tools
     * (such as Emma) don't have instrument the source and so make this step
     * unnecessary. It's still not clear how to integrate everything together.
     *
     * @return True if we should use the src directory instrumented for code
     *         coverage; false otherwise.
     *         <p>
     *         TODO If the buildServer's configuration asks for code coverage,
     *         but we notice that we don't have permission to read and write the
     *         directory where the code coverage data is being written, then we
     *         need to either:
     *         <ul>
     *         <li>over-ride the code coverage setting or else all the test
     *         outcomes will fail.
     *         <li>add the necessary permissions to the security policy file.
     *         </ul>
     *         This would be easy if there were some way to ask a
     *         security.policy file what permissions it is granting. I don't
     *         know if this is possible or how to do so. Future work.
     *
     */
    public boolean isPerformCodeCoverage() {
        boolean performCodeCoverage = getTestProperties().isPerformCodeCoverage();
		boolean available = Clover.isAvailable();
		return performCodeCoverage
                &&  available;
    }

	public static void appendJUnitToClassPath(StringBuffer buf) {
		File f = getJUnitJar();
		addFileToClasspath(buf, f);
	}

	public static void appendLibrariesToClassPath(StringBuffer buf) {
        addFileToClasspath(buf, getGuavaJar());
        addFileToClasspath(buf, getTextDiffJar());
    }


	private static void addFileToClasspath(StringBuffer buf, File f) {
		if (f == null) return;
		buf.append(File.pathSeparatorChar);
		buf.append(f.getAbsolutePath());
	}
	public static void appendCloverToClassPath(StringBuffer buf) {
		File f = getCloverJar();
		addFileToClasspath(buf, f);
	}
	public static void appendBuildServerToClasspath(StringBuffer buf) {
		addFileToClasspath(buf, getBuildServerJar());
		addFileToClasspath(buf, getSubmitServerModelClasses());
		addFileToClasspath(buf, getLog4jJar());
		addFileToClasspath(buf, getDom4jJar());
	}
	public static File getJUnitJar() {
		return getCodeBase(TestCase.class);
	}
	public static File getGuavaJar() {
        return getCodeBase(Strings.class);
    }
	public static File getTextDiffJar() {
        return getCodeBase(TextDiff.class);
    }
	public static File getBuildServerJar() {
		return getCodeBase(TestRunner.class);
	}
	public static File getCloverJar() {
		return Clover.getCloverJar();
	}

	public static File getLog4jJar() {
		return getCodeBase(org.apache.log4j.Logger.class);
	}
	public static File getDom4jJar() {
		return getCodeBase(DocumentException.class);
	}
	public static File getSubmitServerModelClasses() {
		return getCodeBase(TestOutcome.class);
	}

	public static File getCodeBase(Class<?> c) {
		ClassLoader cl = c.getClassLoader();
		String classFileName = c.getName().replace('.', '/') + ".class";
		URL u = cl.getResource(classFileName);
		try {
			String path = u.toString();
			if (path.startsWith("jar:")) {
				path = u.getPath();
				int i = path.lastIndexOf("!/" + classFileName);
				if (i >= 0)
					return new File(new URL(path.substring(0, i)).toURI());
			} else {
				int i = path.lastIndexOf("/" + classFileName);
				if (i >= 0)
					return new File(new URL(path.substring(0, i)).toURI());
			}
		} catch (MalformedURLException e) {
			assert true;

		} catch (URISyntaxException e) {
			assert true;
		}
		return null;
	}

}
