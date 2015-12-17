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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import edu.umd.cs.marmoset.modelClasses.BuildServer;

/**
 * Extract a zip file into a directory.
 * @author David Hovemeyer
 */
public class ZipExtractor {
	private File zipFile;
	private int numFilesExtacted;
	private Set<String> entriesExtractedFromZipArchive = new HashSet<String>();
	private Logger log;

	private Logger getLog()
	{
		if (log!=null)
			return log;
		 log = Logger.getLogger(BuildServer.class);
		return log;
	}

	/**
	 * Constructor.
	 * @param zipFile the zip file to extract
	 * @throws BuilderException
	 */
	public ZipExtractor(File zipFile) throws ZipExtractorException {
		this.zipFile = zipFile;
		this.numFilesExtacted = 0;

		// Paranoia
		if (!zipFile.isFile())
			throw new ZipExtractorException("File " + zipFile + " is not a file");
	}



	/**
	 * Extract the zip file.
	 * @throws IOException
	 * @throws BuilderException
	 */
	public void extract(File directory) throws IOException, ZipExtractorException {
		ZipFile z = new ZipFile(zipFile);
		Pattern badName =  Pattern.compile("[\\p{Cntrl}<>]");
		
		try {
			Enumeration<? extends ZipEntry> entries = z.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String entryName = entry.getName();
				
				if (!shouldExtract(entryName))
					continue;
				if (badName.matcher(entryName).find()) {
					if (entry.getSize() > 0) 
						getLog().debug("Skipped entry of length " + entry.getSize()
					+ " with bad file name " 
							+ java.net.URLEncoder.encode(entryName, "UTF-8"));
					continue;
				}
				try {
				// Get the filename to extract the entry into.
				// Subclasses may define this to be something other
				// than the entry name.
				String entryFileName = transformFileName(entryName);
				if (!entryFileName.equals(entryName)) {
					getLog().debug("Transformed zip entry name: " + entryName + " ==> " +
							entryFileName);
				}
				entriesExtractedFromZipArchive.add(entryFileName);

				File entryOutputFile = new File(directory, entryFileName).getAbsoluteFile();

				File parentDir = entryOutputFile.getParentFile();
				if (!parentDir.exists()) {
					if (!parentDir.mkdirs()) {
						throw new ZipExtractorException("Couldn't make directory for entry output file " +
							entryOutputFile.getPath());
					}
				}

				if (!parentDir.isDirectory()) {
					throw new ZipExtractorException("Parent directory for entry " +
							entryOutputFile.getPath() + " is not a directory");
				}

				// Make sure the entry output file lies within the build directory.
				// A malicious zip file might have ".." components in it.

				getLog().trace("entryOutputFile path: " +entryOutputFile.getCanonicalPath());
                if (!entryOutputFile.getCanonicalPath().startsWith(directory.getCanonicalPath() + "/")) {
                	
                	if (!entry.isDirectory()) 
         				getLog().warn("Zip entry " + entryName + " accesses a path " + entryOutputFile.getPath() +
							"outside the build directory " + directory.getPath());
                	continue;
				}
                
                if (entry.isDirectory()) {
					entryOutputFile.mkdir();
					continue;
                }


				// Extract the entry
				InputStream entryInputStream = null;
				OutputStream entryOutputStream = null;
				try {
					entryInputStream = z.getInputStream(entry);
					entryOutputStream =
						new BufferedOutputStream(new FileOutputStream(entryOutputFile));

					CopyUtils.copy(entryInputStream, entryOutputStream);
				} finally {
                    IOUtils.closeQuietly(entryInputStream);
                    IOUtils.closeQuietly(entryOutputStream);
				}

				// Hook for subclasses, to specify when entries are
				// successfully extracted.
				successfulFileExtraction(entryName, entryFileName);
				++numFilesExtacted;
				} catch (RuntimeException e) {
					getLog().error("Error extracting " + entryName, e);
					throw e;
				}
			}
		} finally {
			z.close();
		}

	}

	/**
	 * Get the number of files extracted.
	 * @return the number of files extracted
	 */
	public int getNumFilesExtracted() {
		return numFilesExtacted;
	}

	/**
	 * Called before we attempt to extract each entry.
	 * If it returns false, then we won't try to extract the entry.
	 * Subclasses may override.
	 *
	 * @param entryName name of the entry
	 * @return true if the entry should be extracted, false if not
	 */
	protected boolean shouldExtract(String entryName) {
		return true;
	}

	/**
	 * Called before extracting an entry, in order to transform
	 * the entry name into the actual filename which will be created.
	 * @param entryName name of the entry to be extracted
	 * @return actual filename to be created for the entry
	 */
	protected String transformFileName(String entryName) {
		return entryName;
	}

	/**
	 * Called when an entry has been successfully extracted.
	 *
	 * @param entryName the name of the zip entry extracted
	 * @param filename the filename of the extracted entry (relative
	 *   to the directory where the entry was extracted)
	 */
	protected void successfulFileExtraction(String entryName, String filename) {
        getLog().trace("Extracted zip entry " +entryName+ " to file " +filename);
	}

	/**
	 * @return Returns the entriesExtractedFromZipArchive.
	 */
	public Set<String> getEntriesExtractedFromZipArchive() {
		return entriesExtractedFromZipArchive;
	}
}
