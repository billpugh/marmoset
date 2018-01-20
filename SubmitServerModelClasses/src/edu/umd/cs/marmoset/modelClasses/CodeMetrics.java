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

package edu.umd.cs.marmoset.modelClasses;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.commons.httpclient.methods.MultipartPostMethod;

import edu.umd.cs.marmoset.utilities.Checksums;

/**
 * CodeMetrics
 *
 * @author jspacco
 */
public class CodeMetrics implements Serializable {
	private int testRunPK; // non-NULL
	private String md5sumSourcefiles;
	private String md5sumClassfiles;
	private int codeSegmentSize;

	public static final String[] ATTRIBUTE_NAME_LIST = { "test_run_pk",
			"checksum_sourcefiles", "checksum_classfiles", "code_segment_size" };

	/** Name of this table in the database */
	public static final String TABLE_NAME = "code_metrics";

	/** Fully-qualified attributes for test_setups table. */
	public static final String ATTRIBUTES = Queries.getAttributeList(
			TABLE_NAME, ATTRIBUTE_NAME_LIST);

	public CodeMetrics() {
	}

	public int fetchValues(ResultSet rs, int startingFrom) throws SQLException {
		setTestRunPK(rs.getInt(startingFrom++));
		setMd5sumSourcefiles(rs.getString(startingFrom++));
		setMd5sumClassfiles(rs.getString(startingFrom++));
		setCodeSegmentSize(rs.getInt(startingFrom++));
		return startingFrom;
	}

	private int putValues(PreparedStatement stmt, int index)
			throws SQLException {
		stmt.setInt(index++, getTestRunPK());
		stmt.setString(index++, getMd5sumSourcefiles());
		stmt.setString(index++, getMd5sumClassfiles());
		stmt.setInt(index++, getCodeSegmentSize());
		return index;
	}

	public void insert(Connection conn) throws SQLException {
		String insert = Queries.makeInsertStatementUsingSetSyntax(
				ATTRIBUTE_NAME_LIST, TABLE_NAME, false);
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(insert);

			putValues(stmt, 1);

			stmt.executeUpdate();
		} finally {
			Queries.closeStatement(stmt);
		}
	}

	/**
	 * @return Returns the md5sumClassfiles.
	 */
	public String getMd5sumClassfiles() {
		return md5sumClassfiles;
	}

	/**
	 * @param md5sumClassfiles
	 *            The md5sumClassfiles to set.
	 */
	public void setMd5sumClassfiles(String md5sumClassfiles) {
		this.md5sumClassfiles = md5sumClassfiles;
	}

	/**
	 * @return Returns the md5sumSourcefiles.
	 */
	public String getMd5sumSourcefiles() {
		return md5sumSourcefiles;
	}

	/**
	 * @param md5sumSourcefiles
	 *            The md5sumSourcefiles to set.
	 */
	public void setMd5sumSourcefiles(String md5sumSourcefiles) {
		this.md5sumSourcefiles = md5sumSourcefiles;
	}

	/**
	 * @return Returns the codeSize.
	 */
	public int getCodeSegmentSize() {
		return codeSegmentSize;
	}

	/**
	 * @param codeSize
	 *            The codeSize to set.
	 */
	public void setCodeSegmentSize(int codeSize) {
		this.codeSegmentSize = codeSize;
	}

	public void mapIntoHttpHeader(MultipartPostMethod method) {
		method.addParameter("md5sumClassfiles", getMd5sumClassfiles());
		method.addParameter("md5sumSourcefiles", getMd5sumSourcefiles());
		method.addParameter("codeSegmentSize",
				Integer.toString(getCodeSegmentSize()));
	}

	/**
	 * @return Returns the codeMetricsPK.
	 */
	public int getTestRunPK() {
		return testRunPK;
	}

	/**
	 * @param codeMetricsPK
	 *            The codeMetricsPK to set.
	 */
	public void setTestRunPK(int testRunPK) {
		this.testRunPK = testRunPK;
	}

	public void setMd5sumClassfiles(Collection<File> fileList)
			throws NoSuchAlgorithmException, IOException {
		setMd5sumClassfiles(Checksums.checksumAsHexString(fileList));
	}

	public void setMd5sumSourcefiles(Collection<File> fileList)
			throws NoSuchAlgorithmException, IOException {
		setMd5sumSourcefiles(Checksums.checksumAsHexString(fileList));
	}

	@Override
	public String toString() {
		return "md5sum of classfiles: " + getMd5sumClassfiles() + "\n"
				+ "\tmd5sum of sourcefiles: " + getMd5sumSourcefiles() + "\n"
				+ "\tsize of code segments: " + getCodeSegmentSize();
	}

	public static void main(String args[]) {
		System.out.println("Hello");
	}
}
