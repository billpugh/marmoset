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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import edu.umd.cs.buildServer.BuilderException;
import edu.umd.cs.buildServer.ProjectSubmission;
import edu.umd.cs.marmoset.modelClasses.JUnitTestProperties;
import edu.umd.cs.marmoset.utilities.ZipExtractorException;

/**
 * SubmissionExtractor for Java submissions.
 * 
 * @author David Hovemeyer
 */
public class JavaSubmissionExtractor extends SubmissionExtractor {
	// Constants
	private static final String SRC_ENTRY_XPATH = "/classpath/classpathentry[@kind='src']";

	// Fields
	private String classpathFile; // Eclipse .classpath file
	private List<Pattern> excludedSourceFileList;

	 /**
     * Constructor.
     * 
     * @param zipFile
     *            the submission zipfile
     * @param directory
     *            directory to extract the submission into
     * @param buildServerLog
     *            the buildserver's Log
     * @throws BuilderException
     */
    public JavaSubmissionExtractor(File zipFile, File directory,
            Logger buildServerLog) throws ZipExtractorException {
        super(zipFile, directory, buildServerLog);
        this.classpathFile = null;
        this.excludedSourceFileList = new LinkedList<Pattern>();
    }
    
	/**
	 * Constructor.
	 * 
	 * @param zipFile
	 *            the submission zipfile
	 * @param directory
	 *            directory to extract the submission into
	 * @param buildServerLog
	 *            the buildserver's Log
	 * @throws BuilderException
	 */
	public JavaSubmissionExtractor(ProjectSubmission<JUnitTestProperties> submission,  File directory) 
	        throws ZipExtractorException {
		super(submission, directory);
		this.classpathFile = null;
		this.excludedSourceFileList = new LinkedList<Pattern>();
	}

	/**
	 * Add a regex pattern specifying source file(s) to exclude.
	 * 
	 * @param pattern
	 *            exclusion regex pattern
	 */
	public void addExcludedSourceFilePattern(String pattern) {
		try {
			Pattern regex = Pattern.compile(pattern);
			excludedSourceFileList.add(regex);
		} catch (PatternSyntaxException e) {
			getLog().warn(
					"Could not compile source file exclusion pattern "
							+ pattern, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.buildServer.ZipExtractor#successfulFileExtraction(java.lang
	 * .String, java.lang.String)
	 */
	@Override
	protected void successfulFileExtraction(String entryName, String fileName) {
		super.successfulFileExtraction(entryName, fileName);

		if (fileName.equals(".classpath")) {
			classpathFile = fileName;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.buildServer.SubmissionExtractor#isSourceFile(java.lang.String)
	 */
	@Override
	protected boolean isSourceFile(String fileName) {
		return fileName.endsWith(".java");
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
		// Apply exclusion patterns, if any
		if (excludedSourceFileList.size() > 0) {
			outer: for (Iterator<String> i = sourceFileList.iterator(); i
					.hasNext();) {
				String sourceFile = i.next();
				for (Iterator<Pattern> j = excludedSourceFileList.iterator(); j
						.hasNext();) {
					Pattern regex = j.next();
					Matcher m = regex.matcher(sourceFile);
					if (m.matches()) {
						i.remove();
						continue outer;
					}
				}
			}
		}

		if (classpathFile == null)
			return;

		// The submission contained a .classpath file.
		// Try to read it so we can find out the sourcepath
		// and remove all entries that aren't on the sourcepath
		// from the source files list.

		try {
			List<String> sourcePath = new LinkedList<String>();
			InputStream in = null;

			try {
				in = new BufferedInputStream(new FileInputStream(new File(
						getDirectory(), classpathFile)));

				SAXReader reader = new SAXReader();
				Document document = reader.read(in);

				for (Iterator<?> i = document.selectNodes(SRC_ENTRY_XPATH)
						.iterator(); i.hasNext();) {
					Node node = (Node) i.next();
					String srcPathEntry = node.valueOf("@path");
					if (!srcPathEntry.equals("") && !srcPathEntry.endsWith("/"))
						srcPathEntry += "/";
					sourcePath.add(srcPathEntry);
				}

				if (!allSourcePathsExist(sourcePath))
					return;

				// Remove all files that don't begin with a valid source path
				// element from the source file list
				for (Iterator<String> i = sourceFileList.iterator(); i
						.hasNext();) {
					String sourceFile = i.next();
					if (!isOnSourcePath(sourceFile, sourcePath)) {
						// Source file wasn't on the source path
						i.remove();
					}
				}
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException ignore) {
					// Ignore
				}
			}
		} catch (IOException e) {
			getLog().warn(
					"Could not read .classpath file in submission zipfile", e);
		} catch (DocumentException e) {
			getLog().warn(
					"Could not read .classpath file in submission zipfile", e);
		}

	}

	/**
	 * Ensures that all of the source paths exist. Strives to handle web-based
	 * submissions better, specifically when students submit their source
	 * outside of a "src" directory but include an Eclipse .classpath file that
	 * references the non-existent source file.
	 * 
	 * @param sourcePath
	 *            list of the source paths
	 * @return true if all of source paths in the given list exist; false if at
	 *         least one of the source paths does not exist
	 */
	private boolean allSourcePathsExist(List<String> sourcePath) {
		for (Iterator<String> ii = sourcePath.iterator(); ii.hasNext();) {
			String dirName = ii.next();
			File dir = new File(getDirectory().getAbsolutePath() + "/"
					+ dirName);
			if (!dir.exists())
				return false;
		}
		return true;
	}

	/**
	 * Determine whether given source file is on the source path.
	 * 
	 * @param sourceFile
	 *            the source file
	 * @param sourcePath
	 *            the source path
	 * @return true if source file is on the source path, false otherwise
	 */
	private boolean isOnSourcePath(String sourceFile, List<String> sourcePath) {
		for (Iterator<String> j = sourcePath.iterator(); j.hasNext();) {
			String srcPathEntry = j.next();
			if (sourceFile.startsWith(srcPathEntry))
				return true;
		}
		return false;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			System.err.println("Usage: "
					+ JavaSubmissionExtractor.class.getName()
					+ " <zipfile> <directory> <project root>");
			System.exit(1);
		}

		File zipFile = new File(args[0]).getAbsoluteFile();
		File directory = new File(args[1]).getAbsoluteFile();
		String projectRoot = args[2];
		Logger log = Logger.getLogger(SubmissionExtractor.class);

		SubmissionExtractor extractor = new JavaSubmissionExtractor(zipFile,
				directory, log);
		extractor.setProjectRoot(projectRoot);
		extractor.extract();

		List<String> sourceFileList = extractor.getSourceFileList();

		System.out.println("Source files:");
		for (Iterator<String> i = sourceFileList.iterator(); i.hasNext();) {
			System.out.println("\t" + i.next());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.buildServer.SubmissionExtractor#transformFileName(java.lang
	 * .String)
	 */
	@Override
	protected String transformFileName(String entryName) {
		String filename = super.transformFileName(entryName);
		// If a source file isn't in a src directory, put it into a src
		// directory!
		if (filename.endsWith(".java") && !filename.startsWith("src"))
			filename = "src/" + filename;
		return filename;
	}

	@Override
	public boolean shouldExtract(String entryName) {
		if (entryName.contains(".clover"))
			return false;
		return super.shouldExtract(entryName);
	}
}
