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

import static edu.umd.cs.marmoset.modelClasses.TestOutcome.TestType.FINDBUGS;
import static edu.umd.cs.marmoset.modelClasses.TestOutcome.TestType.PMD;
import static edu.umd.cs.marmoset.modelClasses.TestOutcome.TestType.PUBLIC;
import static edu.umd.cs.marmoset.modelClasses.TestOutcome.TestType.RELEASE;
import static edu.umd.cs.marmoset.modelClasses.TestOutcome.TestType.SECRET;
import static edu.umd.cs.marmoset.modelClasses.TestOutcome.TestType.STUDENT;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.annotation.CheckForNull;
import javax.annotation.CheckReturnValue;
import javax.annotation.meta.TypeQualifier;

import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentException;

import com.google.common.base.Strings;

import edu.umd.cs.marmoset.codeCoverage.CodeCoverageResults;
import edu.umd.cs.marmoset.codeCoverage.CoverageLevel;
import edu.umd.cs.marmoset.codeCoverage.FileWithCoverage;
import edu.umd.cs.marmoset.utilities.MarmosetUtilities;
import edu.umd.cs.marmoset.utilities.SqlUtilities;
import edu.umd.cs.marmoset.utilities.TextFileReader;
import edu.umd.cs.marmoset.utilities.XSSScrubber;
/**
 * Object to represent a single test outcome.
 *
 * This class is horribly over-loaded in the database in that we
 * store different things depending on how the class is being used.
 * For example, we store code coverage results, pass/fail test outcomes,
 * FindBugs results, and PMD results in the same database table.  Thus,
 * this class contains many getter methods that access the same fields
 * in order to make programming to this class a little easier.
 *
 * TODO Document the various ways in which this class is used.
 *
 * @author David Hovemeyer
 * @author Jaime Spacco
 */
public class TestOutcome implements Serializable {
	/**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 2L;
    private static final int serialMinorVersion = 1;


    public static final int FIRST_TEST_NUMBER = id(1);

    private static int id(int i) {
    	return i;
    }
    
    // Test types
    @Documented
     @TypeQualifier(applicableTo = String.class)
     @Retention(RetentionPolicy.RUNTIME)
     public @interface OutcomeType {
        
    }
    public static @OutcomeType String asOutcomeType(String s) {
        return s;
    }
	// Outcome types
    public static final @OutcomeType String FAILED = asOutcomeType("failed");
	public static final @OutcomeType String PASSED = asOutcomeType("passed");
	public static final @OutcomeType String COULD_NOT_RUN = asOutcomeType("could_not_run");
	public static final @OutcomeType String STATIC_ANALYSIS = asOutcomeType("warning");
    public static final @OutcomeType String NOT_IMPLEMENTED = asOutcomeType("not_implemented");
    public static final @OutcomeType String ERROR = asOutcomeType("error");
    public static final @OutcomeType String HUH = asOutcomeType("huh");
    public static final @OutcomeType String MISSING_COMPONENT = asOutcomeType("missing_component");
    public static final @OutcomeType String TIMEOUT = asOutcomeType("timeout");
    public static final @OutcomeType String FORK_BOMB = asOutcomeType("fork_bomb");
	public static final @OutcomeType String UNCOVERED_METHOD = asOutcomeType("uncovered_method");

	
    public static enum TestType {
        BUILD, PUBLIC, RELEASE, SECRET, STUDENT, FINDBUGS, PMD, UNCOVERED_METHOD;
        
        public static TestType valueOfAnyCase(String name) {
            return valueOf(name.toUpperCase());
        }
        public static @CheckForNull TestType valueOfAnyCaseOrNull(String name) {
            if (Strings.isNullOrEmpty(name))
                return null;
            try {
                return valueOfAnyCase(name);
            } catch (Exception e) {
                return null;
            }
        }
        
        /** Does this test have a standard score defined by the current test setup */
        public boolean isScored() {
            switch (this) {
            case PUBLIC:
            case RELEASE:
            case SECRET:
                return true;
            default:
                return false;
            }
        }
        public static TestOutcome.TestType[] DYNAMIC_TEST_TYPES = { PUBLIC, RELEASE, SECRET, STUDENT };
        @Override
        public String toString() {
            return name().toLowerCase();
        }
        public boolean isDynamic() {
            switch (this) {
            case PUBLIC:
            case RELEASE:
            case SECRET:
            case STUDENT:
                return true;
            default:
                return false;
            }
        }
    }


    // Granularity of coverage
    public static final String METHOD="METHOD";
    public static final String STATEMENT="STATEMENT";
    public static final String BRANCH="BRANCH";
    public static final String NONE="NONE";

		
	private int testRunPK = 0;
	private TestType  testType;
	private String testNumber;
	private @OutcomeType String outcome;
	private int pointValue;
	private int executionTimeMillis;
	private String testName;
	private String shortTestResult = "";
	private String longTestResult="";
	private /* @DottedClassName */ String exceptionClassName;
	private Object details;
    private CoverageLevel coarsestCoverageLevel=CoverageLevel.NONE;
    private boolean exceptionSourceCoveredElsewhere;
    /**
     * Cached copy of codeCoverageResults.  Field is initialized once which saves a bunch of
     * extra unzipping of the zipped XML coverage results.
     */
    private transient CodeCoverageResults codeCoverageResults=null;

	

	/**
	 * List of all attributes for test_outcomes <code>TEST_OUTCOMES_ATTRIBUTE_NAME_LIST</code>
	 */
	  static final String[] ATTRIBUTE_NAME_LIST = {
			"test_run_pk",
			"test_type",
			"test_number",
			"outcome",
			"point_value",
			"test_name",
			"short_test_result",
			"long_test_result",
			"exception_class_name",
            "coarsest_coverage_level",
            "exception_source_covered_elsewhere",
			"details",
			"execution_time_ms"
	};
	  static final String[] ATTRIBUTE_NICE_NAME_LIST = {
			"test_run_pk",
			"type",
			"test #",
			"outcome",
			"points",
			"name",
			"short result",
			"long result",
			"exception class name",
            "coarsest coverage level",
            "exception source covered elsewhere",
			"details",
	        "execution time ms"
	};

	 /** Name of this table in the database. */
	 public static final String TABLE_NAME =  "test_outcomes";

	/**
	 * Fully-qualified attributes for test_outcomes table.
	 */
	public static final String ATTRIBUTES =
	    Queries.getAttributeList(TABLE_NAME, ATTRIBUTE_NAME_LIST);


	public TestOutcome() {

	}

	public TestOutcome(ResultSet rs, int col) throws SQLException {
		fetchValues(rs, col);

	}
	@Override
	public int hashCode() {
		return testType.hashCode() +
			MarmosetUtilities.hashString(testName) +
			MarmosetUtilities.hashString(outcome) +
			MarmosetUtilities.hashString(shortTestResult) +
			MarmosetUtilities.hashString(longTestResult);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || this.getClass() != o.getClass())
			return false;
		TestOutcome other = (TestOutcome) o;
		return testType.equals(other.testType)
			&& MarmosetUtilities.stringEquals(testName, other.testName)
			&& MarmosetUtilities.stringEquals(outcome, other.outcome)
			&& MarmosetUtilities.stringEquals(shortTestResult, other.shortTestResult)
			&& MarmosetUtilities.stringEquals(longTestResult, other.longTestResult);
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(testRunPK);
		buf.append(',');
		buf.append(testType);
		buf.append(',');
		buf.append(testName);
		buf.append(',');
		buf.append(outcome);
		buf.append(',');
		buf.append(shortTestResult);
		buf.append(',');
		buf.append(longTestResult.substring(0,Math.min(longTestResult.length(),10000)).replace('\n', '|'));
		buf.append(',');
		buf.append(coarsestCoverageLevel);
		buf.append(',');
		buf.append(exceptionSourceCoveredElsewhere);
		return buf.toString();
	}

    public String toConciseString() {
        StringBuffer buf = new StringBuffer();
        buf.append(testRunPK);
        buf.append(',');
        buf.append(testType);
        buf.append(',');
        buf.append(testName);
        buf.append(',');
        buf.append(outcome);
        buf.append(',');
        return buf.toString();
    }

    public boolean isExceptionSourceApproximatelyCoveredElsewhere(CodeCoverageResults coverage, int range)
    throws IOException
    {
        if (!isError() || !RELEASE.equals(testType))
            return false;
        StackTraceElement stackTraceElement=getExceptionSourceFromLongTestResult();
        if (coversLineOrPreviousLines(stackTraceElement, range, coverage))
            return true;
        return false;
    }

    private boolean coversLineOrPreviousLines(StackTraceElement stackTraceElement, int range, CodeCoverageResults coverage)
    throws IOException
    {
        // If we weren't able to extract a stack trace for some reason, then simply return false
        if (stackTraceElement==null)
            return false;
        if (!isCoverageType())
            return false;
        FileWithCoverage fileWithCoverage=coverage.getFileWithCoverage(stackTraceElement.getFileName());
        if (fileWithCoverage==null) {
            // ERROR: Can't find the appropriate file in this set of coverage results
            System.err.println("No coverage for " +stackTraceElement.getFileName());
            return false;
        }
        for (int ii=stackTraceElement.getLineNumber(); ii>=stackTraceElement.getLineNumber()-range; ii--) {
            if (fileWithCoverage.isLineCovered(ii))
                return true;
        }
        return false;
    }

	/**
     * Java-only.
     * <p>
     * Is this outcome a fault?
     * <p>
     * A fault is any test case that fails but does not fail with the default
     * UnsupportedOperationException("You must implement this method") that skeleton or
     * stub implementation throw when they are not implemented.
     * <p>
     * A fault in a test case is any failure due to a normal unit test failure,
     * an exception being thrown in student code,
     * a timeout (where the buildserver kills a test case that's taking too long), or
     * a security manager exception.
	 * @return
	 */
	public boolean isFault()
	{
	    if (outcome.equals(FAILED) ||
	            outcome.equals(ERROR) ||
	            outcome.equals(HUH) ||
	            outcome.equals(MISSING_COMPONENT) ||
	            outcome.equals(TIMEOUT))
	        return true;
	    return false;
	}

    public boolean isError() {
        return outcome.equals(ERROR);
    }

    public boolean isPublicTest() {
        return getTestType().equals(PUBLIC);
    }

    public boolean isStudentTest() {
        return getTestType().equals(STUDENT);
    }

    public boolean isReleaseTest() {
        return getTestType().equals(RELEASE);
    }

    public boolean isSecretTest() {
        return getTestType().equals(SECRET);
    }

    public boolean isPassed()
    {
        return getOutcome().equals(TestOutcome.PASSED);
    }

	public boolean isFailed()
	{
	    if (isFault() || outcome.equals(NOT_IMPLEMENTED))
	        return true;
	    return false;
	}

	/**
	 * @return Returns the longTestResult.
	 */
	public @CheckReturnValue String getLongTrimmedTestResult() {
		String result = getLongTestResult();
		int i = result.indexOf("at sun.reflect.NativeMethodAccessorImpl.invoke0");
        if (i > 0) 
            result = result.substring(0,i).trim();
        
		if (result.length() >  MAX_LONG_TEST_RESULT_CHARS_TO_DISPLAY)
			result = result.substring(0,MAX_LONG_TEST_RESULT_CHARS_TO_DISPLAY)
			+ String.format("%n ... %d characters trimmed ...", result.length() - MAX_LONG_TEST_RESULT_CHARS_TO_DISPLAY);
		return result;
	}

	/**
	 * @return Returns the longTestResult.
	 */
	public String getLongTestResult() {
		if (isLongTestResultCompressed())
			return decompress((byte[]) details);
		return longTestResult;
	}

	/**
	 * @return
	 */
	public boolean isLongTestResultCompressed() {
		return LONG_TEST_RESULTS_ARE_COMPRESSED.equals(longTestResult) && details instanceof byte[];
	}

	/**
	 * @return Returns the longTestResult.
	 */
	public @HTML String getLongTestResultAsHtml() {
		StringBuffer builder = XSSScrubber.scrubbed(getLongTestResult(), false);
		return XSSScrubber.asHTML(builder.toString().replaceAll("\n","<br>"));
	}

	public static final int MAX_LONG_TEST_RESULT_CHARS_TO_DISPLAY = 5000;
	public static final int MAX_LONG_TEST_RESULT_CHARS = 60000;
    public static final String CARDINAL = "cardinal";

	/**
	 * Returns The String representation of the code coverage results in XML format.
	 *
	 * TODO Perhaps this should return the CodeCoverageResults object?
	 *
	 * @return Returns The String representation of the code coverage results in XML format.
	 * @throws IOException If the coverage results (currently zipped into a byte
	 * 	array and stored in the 'details' blob column) cannot be unzipped.
	 */
	public String getCodeCoverageXMLResultsAsString()
	throws IOException
	{
	    // In very early versions that supported code coverage, I stuck the entire XML
		// file into the longTestResult field
//		if (isCoverageType() && !"".equals(longTestResult))
//	        return longTestResult;

		// Unzip the code coverage results from the "details" field.
	    // We assume there will only be one entry in the zip archive.
	    ZipInputStream zip = null;
	    BufferedReader reader=null;
	    try {
	    	zip = new ZipInputStream(new ByteArrayInputStream((byte[])details));

	    	ZipEntry entry = zip.getNextEntry();
	    	if (entry == null)
	    		throw new IOException("This test outcome doesn't contain " +
	    		"any zipped entries in the 'details' field.");

	    	reader= new BufferedReader(new InputStreamReader(zip));

	    	StringBuffer result = new StringBuffer();
	    	while (true) {
	    		String line = reader.readLine();
	    		if (line == null) break;
	    		result.append(line + "\n");
	    	}
	    	return result.toString();
	    } finally {
	    	try {
	    		if (reader != null) reader.close();
	    	} finally {
	    		if (zip != null) zip.close();
	    	}
	    }
	}

	/**
     * Getter for the codeCoverageResults.  Will unzip and parse the results once and
     * then cache them in an instance variable.
	 * @return The codeCoverageResults for this testOutcome, if any are available.
	 * @throws IOException If there is an error unzipping or parsing the XML document containing
     *  the codeCoverageResults
     * @throws IllegalStateException If this test outcome lacks codeCoverageResults.
	 */
	public CodeCoverageResults getCodeCoverageResults() {
        if (codeCoverageResults!=null)
            return codeCoverageResults;
        if (TIMEOUT.equals(getOutcome())) {
            // Return empty coverage results for a timeout
            return new CodeCoverageResults();
        }
        // Can't have coverage if not correct type (must be public/release/secret/student)
        if (!isCoverageType())
            throw new IllegalStateException("Lacking code coverage information for test outcome "+this);
        // If we don't have any code coverage details available, return an empty set.
        // A test case may have timed out, which produces no coverage.  In general
        // we don't want to fail and throw an exception if we don't have coverage data for
        // some reason.
        if (getDetails()==null)
            return new CodeCoverageResults();

        try {
	    	// Haven't unzipped and parsed codeCoverageResults yet, so initialize them from
            // the details field.
            String xmlStringResults = getCodeCoverageXMLResultsAsString();
	    	return codeCoverageResults = CodeCoverageResults.parseString(xmlStringResults);
	    } catch (DocumentException e) {
            //throw new IOException("Unable to parse XML file containing CodeCoverageResults: "+e.getMessage());
	        // If we can't parse things, just return an empty
            // TODO replace println with a logger!
            System.err.println("Unable to parse XML file containing CodeCoverageResults for " +toConciseString()+
                ": "+e.getMessage());
            return new CodeCoverageResults();
	    } catch (IOException e) {
          System.err.println("Unable to parse XML file containing CodeCoverageResults for " +toConciseString()+
              ": "+e.getMessage());
          return new CodeCoverageResults();
      }
	}

	/**
	 * Zips the contents of a given file containing code coverage results in XML format.
	 * @param file The file containing the code coverage XML results.
	 * @throws IOException If there are any error reading the file.
	 */
	public void setCodeCoveralXMLResults(File file)
	throws IOException
	{
	    if (!file.exists() || !file.canRead())  {
	        return;
	    }
	    ByteArrayOutputStream actualData = new ByteArrayOutputStream();
	    ZipOutputStream zipOut = new ZipOutputStream(actualData);

	    ZipEntry entry = new ZipEntry("coverage");
	    zipOut.putNextEntry(entry);

	    BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
	    try {
	    int bytesRead=0;
	    byte[] bytes = new byte[2048];
	    while (true) {
	        int numBytes = in.read(bytes);
	        if (numBytes == -1) break;
	        zipOut.write(bytes, 0, numBytes);
	        bytesRead += numBytes;
	    }
	    zipOut.closeEntry();
	    zipOut.close();
	    byte[] outbytes = actualData.toByteArray();
//	    System.out.println("outbytes.length: "+outbytes.length+
//	            " and we read: " +bytesRead+ " total bytes");
	    setDetails(outbytes);
	    } finally { in.close(); }
	}

	public @CheckReturnValue String getCappedLongTestResult()
	{
		String result = getLongTestResult();
		return result.substring(0, Math.min(result.length(), MAX_LONG_TEST_RESULT_CHARS_TO_DISPLAY));
	}

	private static @CheckReturnValue String limitLength(String txt, int maxLength) {
		if (txt.length() <= maxLength)
			return txt;
		return txt.substring(0, maxLength);
	}
	
	public static byte[] compress(String txt) {
		byte[] rawBYtes = txt.getBytes();
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			GZIPOutputStream gzout =  new GZIPOutputStream(out);
			
			gzout.write(rawBYtes);
			gzout.close();
		} catch (IOException e) {
			e.printStackTrace();
			return rawBYtes;	
		}
		return out.toByteArray();
	}
	public static String  decompress(byte [] rawBytes ) {
		ByteArrayInputStream bytes = new ByteArrayInputStream(rawBytes);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			 GZIPInputStream gzin = new GZIPInputStream(bytes);
			
			IO.copyStream(gzin, out);
			out.close();
		} catch (IOException e) {
			return "  -- unable to decompress: " +  e.getMessage() + " -- ";	
		}
		return new String(out.toByteArray());
	}
	
	public static final String LONG_TEST_RESULTS_ARE_COMPRESSED = "Long test results are compressed (MVRnYVfnYvDotUpeNOHDaD31DlJYkgs)";
	public void setLongTestResultCompressIfNeeded(String longTestResult) {
		if (longTestResult.length() > MAX_LONG_TEST_RESULT_CHARS && details == null) {
			
			byte[] bytes = compress(longTestResult);
			if (bytes.length <= MAX_LONG_TEST_RESULT_CHARS)
				details = bytes;
				this.longTestResult = LONG_TEST_RESULTS_ARE_COMPRESSED;
				return;
		}
		setLongTestResult(longTestResult);
	}
	
	/**
	 * @param longTestResult The longTestResult to set.
	 */
	public void setLongTestResult(String longTestResult) {
		
			
		// Truncate long test results that are excessively long.  These
		// could cause an OutOfMemory down the line when we serialize
		// the test results for submission.
		//
		// 1 << 16 is the limit imposed by the MySQL text field anyway.
		//
		this.longTestResult = limitLength(longTestResult, MAX_LONG_TEST_RESULT_CHARS);
		}

	/**
	 * Truncates the longTestResult string to be at most 64K chars
	 * (which is the limit imposed by the MySQL test field anyway).
	 */
	public void truncateLongTestResult() {
		longTestResult = limitLength(longTestResult, MAX_LONG_TEST_RESULT_CHARS);
	}
	/**
	 * @return Returns the outcome.
	 */
	public @OutcomeType String getOutcome() {
		return outcome;
	}
	/**
	 * @param outcome The outcome to set.
	 */
	public void setOutcome(@OutcomeType String outcome) {
		this.outcome = outcome;
	}
	/**
	 * Returns the outcome suitable for viewing by students; basically, this means
	 * mapping ERROR and NOT_IMPLEMENTED to failed so that students always see
	 * outcomes as either PASSED or FAILED (or COULD_NOT_RUN if things time out).
	 * @return the outcome, with "error" and "not_implemented" mapped to "failed"
	 */
	public String getStudentOutcome() {
	    if (outcome.equals(HUH))
	        return TestOutcome.FAILED;
	    return outcome;
	}
    /**
     * @return Returns the pointValue.
     */
    public int getPointValue()
    {
        return pointValue;
    }
    /**
     * @param pointValue The pointValue to set.
     */
    public void setPointValue(int pointValue)
    {
        this.pointValue = pointValue;
    }
	/**
	 * @return Returns the shortTestResult.
	 */
	public String getShortTestResult() {
		return shortTestResult;
	}
	/**
	 * @param shortTestResult The shortTestResult to set.
	 */
	public void setShortTestResult(String shortTestResult) {
		this.shortTestResult = limitLength(shortTestResult, 300);
	}
    /**
     * @return Returns the testRunPK.
     */
    public int getTestRunPK() {
        return testRunPK;
    }
    /**
     * @param testRunPK The testRunPK to set.
     */
    public void setTestRunPK(int testRunPK) {
        this.testRunPK = testRunPK;
    }
	/**
	 * @return Returns the testName.
	 */
	public String getTestName() {
		return testName;
	}
    /**
     * Converts the &lt; and &gt; to & lt and & gt
     * @return Test name suitable for display as HTML
     */
    public @HTML String getHtmlTestName() {
        String htmlTestName=testName.replace("<","&lt;");
        htmlTestName=htmlTestName.replace(">","&gt;");
        return XSSScrubber.asHTML(htmlTestName);
    }
	/**
	 * @return Returns the short version of the testName.
	 */
	public String getShortTestName() {
		String s = testName;
		int i = s.indexOf('(');
		if (i > 0) s = s.substring(0,i);
		return s;
	}

	public static int numLines(String s) {
	    int count = 0;
	    int i = -1;
	    do {
	        i = s.indexOf('\n', i+1);
	        count++;
	    } while (i >= 0);
	    return count;
	}
	
	private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return URLEncoder.encode(s);
        }

	}
	public static @HTML String getHotlink(
			TestOutcome outcome, String viewLink) {
	    if (outcome.getOutcome().equals(NOT_IMPLEMENTED))
            return XSSScrubber.asHTML("");
       
	    String viewSourceLink = viewLink + "/sourceCode.jsp";
	    String fullResult  = outcome.getLongTestResult();
	    
	    int length = fullResult.length();
	    
	    @HTML String result =  outcome.getHotlink(viewSourceLink);
	    if (length > 5000) {
	        result = String.format(
	                "<b><a href=\"%s/PrintTestOutcome?testRunPK=%d&testType=%s&testNumber=%s\">full result is %d characters, %d lines long</a></b><br>%s",
	                viewLink,
	                outcome.getTestRunPK(),
	                urlEncode(outcome.getTestType().name()),
	                urlEncode(outcome.getTestNumber()),
	                length, 
	                numLines(fullResult),
	                result);
	    }
	    return XSSScrubber.asHTML(result);
	}

	private  @HTML String getHotlink(String viewSourceLink)
	{
	    if (testType.equals(FINDBUGS))
	        return getFindbugsHotlink(viewSourceLink);
	    else if (testType.equals(PMD))
	        return getPmdLocation(viewSourceLink);
	    else if (testType.isDynamic() ||
                testType.equals(TestType.UNCOVERED_METHOD))
	        return getStackTraceHotlinks(viewSourceLink);
	    return getLongTestResultAsHtml();
	}

    private static List<String> ignoreSet=new LinkedList<String>();
    static {
        ignoreSet.add("java.");
        ignoreSet.add("junit.");
        ignoreSet.add("\\s+sun\\.reflect");
        ignoreSet.add("edu.umd.cs.buildServer");
        ignoreSet.add("ReleaseTest");
        ignoreSet.add("PublicTest");
        ignoreSet.add("SecretTest");
        ignoreSet.add("SimpleTest");
        ignoreSet.add("TestAgainstFile");
        ignoreSet.add("SpiderTest");
    }

    /**
     * Checks if a string matches any regexp that is in the "ignoreSet".  The "ignoreSet" is
     * a hard-coded list of regexps that represent frames of a stack-trace that it doesn't
     * make sense to try to hotlink to because the source is not available; i.e. anything in
     * the java.* or junit.* packages, in any of the buildserver classes, etc.
     *
     * TODO the ignoreSet should dynamically incorporate the classfiles from the
     * test.properties file since that makes more sense.
     *
     * @param line The line of text to be searched for any of the regexps in the ignoreSet.
     * @return True if this line should be ignored; false otherwise.
     */
    private static boolean matchesIgnoreSet(String line) {
        for (String s : ignoreSet) {
            if (line.contains(s))
                return true;
        }
        return false;
    }

    public static String getExceptionLocation(TestOutcome outcome, String viewSourceLink) {
        return outcome.getExceptionLocation(viewSourceLink);
    }
    public static final Pattern FINDBUGS_LOCATION_REGEX = Pattern.compile("At (\\w+\\.java):\\[line (\\d+)\\]");

    public @HTML String getExceptionLocation(String viewSourceLink)
    {
        StringBuffer buf=new StringBuffer();
        if (isError()) {
        	    String testResult = getLongTestResult();
            if (Strings.isNullOrEmpty(testResult))	
                return "";
            BufferedReader reader = null;
            Pattern pattern = FINDBUGS_LOCATION_REGEX;
            try {
                reader=new BufferedReader(new StringReader(getLongTrimmedTestResult()));
                while (true) {
                    String line=reader.readLine();
                    if (line==null) break;
                    // System.out.println("line: " +line);
                    Matcher matcher = pattern.matcher(line);
                    if (!matchesIgnoreSet(line) &&
                            matcher.find() &&
                            matcher.groupCount() > 1)
                    {
                        String sourceFileName=matcher.group(1);
                        String startHighlight=matcher.group(2);
                        int numToHighlight=1;
                        int numContext=0;
                        buf.append(createSourceCodeLink(viewSourceLink, line, sourceFileName, startHighlight, numToHighlight, numContext));
                        buf.append("<br>");
                        // Return after we find the first hot-link-able stack frame
                        return XSSScrubber.asHTML(buf.toString());
                    } else {
                        buf.append(line + "<br>\n");
                    }
                }
            } catch (IOException ignore) {
                throw new RuntimeException("DAMMIT JIM!",ignore);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }
        return XSSScrubber.asHTML(buf.toString());
    }

	private @HTML String getFindbugsHotlink(String viewSourceLink)
	{
	    if (!testType.equals(FINDBUGS))
	        throw new IllegalArgumentException("is type " + testType + ", not findbugs");
	    if (shortTestResult == null || shortTestResult.equals(""))
	        return "";
	    // At ExtractHRefs.java:[line 64]
	    // TODO handle multi-line FB warnings
	    String p = "At (\\w+\\.java):\\[line (\\d+)\\]";
	    Pattern pattern = Pattern.compile(p);
	    Matcher matcher = pattern.matcher(shortTestResult);
	    if (matcher.matches() && matcher.groupCount() > 1)
	    {
	        String sourceFileName = matcher.group(1);
            String startHighlight = matcher.group(2);
            int numToHighlight=1;
            int numContext=0;
            return createSourceCodeLink(viewSourceLink, shortTestResult, sourceFileName, startHighlight, numToHighlight, numContext);
	    }
        return XSSScrubber.scrubbedStr(shortTestResult);
	}

    public StackTraceElement getExceptionSourceFromLongTestResult()
    {
        if (!isError())
            throw new IllegalStateException("Can ONLY get exception source from test outcome of type " +TestOutcome.ERROR);
        String testResult = getLongTestResult();
        if (Strings.isNullOrEmpty(testResult))	
            throw new IllegalStateException("No stack trace available in this outcome: " +outcome);

        BufferedReader reader = null;
        try {
            reader=new BufferedReader(new StringReader(getLongTrimmedTestResult()));
            Pattern pattern = Pattern.compile("\\((\\w+\\.java):(\\d+)\\)");
            while (true) {
                String line=reader.readLine();
                if (line==null) break;

                Matcher matcher = pattern.matcher(line);
                if (!matchesIgnoreSet(line) && matcher.find() && matcher.groupCount() > 1) {
                    return MarmosetUtilities.parseStackTrace(line);
                }
            }
        } catch (IOException ignore) {
            // Cannot really happen with a stringreader
        }
        return null;
    }




	private @HTML String getStackTraceHotlinks(String viewSourceLink) {
	   //  System.out.println("Calling getStackTraceHotlinks!");
		 String testResult = getLongTestResult();
	       
        if (Strings.isNullOrEmpty(testResult) || getOutcome().equals(NOT_IMPLEMENTED))
	        return "";
       
	    StringBuffer buf=new StringBuffer();
	    String txt = getLongTrimmedTestResult();
	    BufferedReader reader = null;

	    Pattern pattern = Pattern.compile("\\((\\w+\\.java):(\\d+)\\)");
	    try {
	        reader=new BufferedReader(new StringReader(txt));
	    	while (true) {
	    	    String line=reader.readLine();
                if (line==null) break;
                /** skip stacktracelements resulting from Clover */
                String trimmed = line.trim();
                if (trimmed.startsWith("at ")) {
                		if (line.contains(".__CLR3_0_")
                				|| trimmed.startsWith("at junit.framework.Assert")
                				|| trimmed.startsWith("at java.io")
                				|| trimmed.startsWith("at sun.")
                					|| trimmed.startsWith("at edu.umd.cs.diffText"))
                			continue;
                }
               
                line=line.replaceAll("&","&amp;");
                line=line.replaceAll("<","&lt;");
                line=line.replaceAll(">","&gt;");
                Matcher matcher = pattern.matcher(line);
	            if (!line.contains("java.") && !line.contains("junit.") &&
	                    !line.contains("\\s+sun\\.reflect") &&
	                    !line.contains("edu.umd.cs.buildServer") &&
	                    !line.contains("ReleaseTest") &&
	                    !line.contains("PublicTest") &&
	                    !line.contains("SecretTest") &&
	                    !line.contains("SimpleTest") &&
	                    !line.contains("TestAgainstFile") &&
	                    !line.contains("SpiderTest") &&
	                    matcher.find() && matcher.groupCount() > 1) {
	                String sourceFileName=matcher.group(1);
	                String startHighlight=matcher.group(2);
	                int numToHighlight=1;
	                int numContext=0;
	                buf.append(createSourceCodeLink(viewSourceLink, line, sourceFileName, startHighlight, numToHighlight, numContext));
	                buf.append("<br>");
	            } else {
	                buf.append(line + "<br>\n");
	            }
	        }
	    } catch (IOException ignore) {
	        throw new RuntimeException("DAMMIT JIM!",ignore);
	    } finally {
	    	IOUtils.closeQuietly(reader);
	    }
	    return XSSScrubber.asHTML( buf.toString());
	}

	/**
     * @param viewSourceLink TODO
	 * @param line
	 * @param sourceFileName
	 * @param startHighlight
	 * @param numToHighlight
	 * @param numContext
	 * @return
     */
    private @HTML String createSourceCodeLink(String viewSourceLink, String line, String sourceFileName, String startHighlight, int numToHighlight, int numContext)
    {
    	if (line.startsWith("\tat ")) 
    		line = "  at " + line.substring(4);
    	int leadingSpaces = 0;
    	while (leadingSpaces < line.length() &&( line.charAt(leadingSpaces) == ' ' || line.charAt(leadingSpaces) == '\t'))
    		leadingSpaces++;
    	
        return  XSSScrubber.asHTML(line.substring(0,leadingSpaces) + "<a href=\"" + viewSourceLink +
        		"?testRunPK=" +testRunPK+
                "&sourceFileName="+sourceFileName+
                "&testType="+(testType.equals(TestOutcome.TestType.UNCOVERED_METHOD)?"public-student":testType)+
                "&testNumber="+(testType.equals(TestOutcome.TestType.UNCOVERED_METHOD)?"all":testNumber)+
                "&testName="+testName+
                "&startHighlight="+startHighlight+
                "&numToHighlight="+numToHighlight+
                "&numContext="+numContext+
                "#codehighlight0\">" +line.substring(leadingSpaces)+
                "</a>\n");
    }

    /**
	 * @param viewSourceLink TODO
     * @return Returns the location in the file where the PMD warning occurs.
	 */
	private @HTML String getPmdLocation(String viewSourceLink) {
	    String pmdLocation = shortTestResult;
	    int i = pmdLocation.indexOf(':');
	    if (i > 0) pmdLocation = pmdLocation.substring(i+1);

	    if (pmdLocation == null || pmdLocation.equals(""))
	        return "";
	    //  src/oop2/searchTree/EmptyTree.java:19
	    Pattern pattern = Pattern.compile("([\\w/]+\\.java):(\\d+)");
	    Matcher matcher = pattern.matcher(pmdLocation);
	    if (matcher.matches() && matcher.groupCount() > 1)
	    {
	        String sourceFileName = matcher.group(1);
	        String startHighlight = matcher.group(2);
	        int numToHighlight=1;
	        int numContext=0;
	        return createSourceCodeLink(viewSourceLink, pmdLocation, sourceFileName, startHighlight, numToHighlight, numContext);
	    }
	    return XSSScrubber.scrubbedStr(pmdLocation);
	}
	/**
	 * @param testName The testName to set.
	 */
	public void setTestName(String testName) {
		this.testName = testName;
	}
	/**
	 * @return Returns the testType.
	 */
	public TestType getTestType() {
		return testType;
	}
	/**
	 * @param testType The testType to set.
	 */
	public void setTestType(TestType testType) {
		this.testType = testType;
	}

	/**
	 * @return Returns the testNumber.
	 */
	public String getTestNumber() {
		return testNumber;
	}
	/**
	 * @param testNumber The testNumber to set.
	 */
	public void setTestNumber(String testNumber) {
		this.testNumber = testNumber;
	}
	/**
	 * Returns a key suitable for inserting this TestOutcome into a map.
	 * Yes, I could just write a hashCode() method and put objects into a set,
	 * but I don't think sets work with JSPs yet.
	 * @return A key suitable for inserting this testOutcome into a map.
	 */
	public String getKey() {
		return testType +"-"+testNumber;
	}


    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int thisMinorVersion = stream.readInt();
        if (thisMinorVersion != serialMinorVersion) throw new IOException("Illegal minor version " + thisMinorVersion + ", expecting minor version " + serialMinorVersion);
        stream.defaultReadObject();
        truncateLongTestResult();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException{
    	truncateLongTestResult();
        stream.writeInt(serialMinorVersion);
        stream.defaultWriteObject();
    }

    public String getExceptionClassName()
    {
        return exceptionClassName;
    }
    public void setExceptionClassName(String exceptionClassName)
    {
        this.exceptionClassName = exceptionClassName;
    }

    public int getFindBugsRank() {
        if (!getTestType().equals(TestType.FINDBUGS))
            throw new IllegalStateException("Not a findbugs outcome");
        return Integer.parseInt(exceptionClassName);
    }
    public String getFindBugsRankDescription() {
        if (!getTestType().equals(FINDBUGS))
            throw new IllegalStateException("Not a findbugs outcome");
        int rank  = Integer.parseInt(exceptionClassName);
        if (rank <=4)
            return String.format("Scariest (%d)", rank);
        else if (rank <= 9)
            return String.format("Scary (%d)", rank);
        else if (rank <= 14)
            return String.format("Troubling (%d)", rank);
        return String.format("Of concern (%d)", rank);
    }

    /**
     * @return Returns the details.
     */
    public Object getDetails()
    {
        return details;
    }
    /**
     * @param details The details to set.
     */
    public void setDetails(Object details)
    {
        this.details = details;
    }
	public int getExecutionTimeMillis() {
        return executionTimeMillis;
    }

    public void setExecutionTimeMillis(long executionTimeMillis) {
        if (executionTimeMillis > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Execution time too long: " + executionTimeMillis);
        this.executionTimeMillis = (int) executionTimeMillis;
    }

    /**
     * @return Returns the coarsestPublicStudentCoverage.
     */
    public CoverageLevel getCoarsestCoverageLevel() {
        return coarsestCoverageLevel;
    }
    /**
     * @param coarsestPublicStudentCoverage The coarsestPublicStudentCoverage to set.
     */
    public void setCoarsestCoverageLevel(CoverageLevel coarsestPublicStudentCoverage) {
        this.coarsestCoverageLevel = coarsestPublicStudentCoverage;
    }
    /**
     * @return Returns the exceptionPointCoveredElsewhere.
     */
    public boolean getExceptionSourceCoveredElsewhere() {
        return exceptionSourceCoveredElsewhere;
    }
    /**
     * @param exceptionPointCoveredElsewhere The exceptionPointCoveredElsewhere to set.
     */
    public void setExceptionSourceCoveredElsewhere(boolean exceptionPointCoveredElsewhere) {
        this.exceptionSourceCoveredElsewhere = exceptionPointCoveredElsewhere;
    }
    private void limitSizes() {
    	truncateLongTestResult();
    }
    /**
	 * Populated a prepared statement starting at a given index with all of the fields
	 * of this model class.
	 * @param stmt the PreparedStatement
	 * @param index the starting index
	 * @return the index of the next open slot in the prepared statement
	 * @throws SQLException
	 */
	int putValues(PreparedStatement stmt, int index)
	throws SQLException
	{
		limitSizes();
	    stmt.setInt(index++, getTestRunPK());
	    stmt.setString(index++, getTestType().name());
	    stmt.setString(index++, getTestNumber());
	    stmt.setString(index++, getOutcome());
	    stmt.setInt(index++, getPointValue());
	    stmt.setString(index++, getTestName());
	    stmt.setString(index++, getShortTestResult());
	    stmt.setString(index++, longTestResult);
	    stmt.setString(index++, getExceptionClassName());
        stmt.setString(index++, (getCoarsestCoverageLevel()!=null)?getCoarsestCoverageLevel().toString():CoverageLevel.NONE.toString());
        stmt.setBoolean(index++, getExceptionSourceCoveredElsewhere());
	    stmt.setObject(index++, getDetails());
	    stmt.setInt(index++, getExecutionTimeMillis());
	    return index;
	}

	/**
	 * Updates a row of the database based on its compound primary key (test_run_pk,
	 * test_type, test_number) and updates it.
	 * @param conn the connection to the database
	 * @throws SQLException
	 */
	public void update(Connection conn)
	throws SQLException
	{
	    String update =
	        " UPDATE " + TABLE_NAME +
	        " SET " +
	        " test_run_pk = ?, " +
	        " test_type = ?," +
	        " test_number = ?, " +
	        " outcome = ?, " +
	        " point_value = ?, " +
	        " test_name = ?, " +
	        " short_test_result = ?, " +
	        " long_test_result = ?," +
	        " exception_class_name = ?, " +
            " coarsest_coverage_level = ?," +
            " exception_source_covered_elsewhere = ?, " +
	        " details = ? " +
            " execution_time_ms = ? " +
	        " WHERE test_run_pk = ? " +
	        " AND test_type = ? " +
	        " AND test_number = ? ";

	    limitSizes();
	    PreparedStatement stmt=null;
	    try {
	        stmt = conn.prepareStatement(update);

	        int index = putValues(stmt, 1);
	        stmt.setInt(index++, getTestRunPK());
	        stmt.setString(index++, getTestType().name());
	        stmt.setString(index++, getTestNumber());

	        //System.out.println(stmt);
            stmt.executeUpdate();
	    } finally {
	        Queries.closeStatement(stmt);
	    }

	}

	/**
	 * Populate a TestOutcome from a ResultSet that is positioned
	 * at a row of the test_outcomes table.
	 *
	 * @param rs the ResultSet returned by the database.
	 * @param startingFrom index specifying where to start fetching attributes from;
	 *   useful if the row contains attributes from multiple tables
	 * @throws SQLException
	 */
	public int fetchValues(ResultSet rs, int startingFrom) throws SQLException
	{
		setTestRunPK(rs.getInt(startingFrom++));
		setTestType(TestType.valueOfAnyCase(rs.getString(startingFrom++)));
		setTestNumber(rs.getString(startingFrom++));
		setOutcome(asOutcomeType(rs.getString(startingFrom++)));
		setPointValue(rs.getInt(startingFrom++));
		setTestName(rs.getString(startingFrom++));
		setShortTestResult(rs.getString(startingFrom++));
		setLongTestResult(rs.getString(startingFrom++));
		setExceptionClassName(rs.getString(startingFrom++));
        setCoarsestCoverageLevel(CoverageLevel.fromString(rs.getString(startingFrom++)));
        setExceptionSourceCoveredElsewhere(rs.getBoolean(startingFrom++));
		setDetails(rs.getObject(startingFrom++));
		setExecutionTimeMillis(rs.getInt(startingFrom++));
        
		limitSizes();
		return startingFrom++;
	}

    /**
     * Delete all test_outcomes that have the given test_run_pk.
     * @return the number of rows affected
     */
    public static int deleteByTestRunPK(Integer testRunPK, Connection conn)
    throws SQLException
    {
        String query =
            " DELETE FROM test_outcomes " +
            " WHERE test_run_pk = ? ";

        PreparedStatement stmt = null;
        try {
            stmt=conn.prepareStatement(query);
            SqlUtilities.setInteger(stmt, 1, testRunPK);

            return stmt.executeUpdate();
        } finally {
            Queries.closeStatement(stmt);
        }

    }

	/**
	 * Is the source of a failed test in the implementation or in the driver code?
	 * @return true if the source of the failure is in the implementation; false if it's in the driver
	 * or cannot be determined
	 */
	public boolean isExceptionSourceInTestDriver()
	{
	    if (! (testType.equals(PUBLIC) ||
	            testType.equals(RELEASE) ||
	            testType.equals(SECRET)))
	        throw new IllegalStateException("Cannot query source of failure for a testOutcome of type " +testType+
	                " because the source of a failure only makes sense for a public, release or secret test");
	    // doesn't really make sense to query this information for a passed test
	    // but I'm not sure I should throw an exception here
	    if (outcome.equals(PASSED))
	        return false;

	    // timeouts, security manager exceptions and normal failure due to assertion failed
	    // exceptions are definitely not interesting
	    if (outcome.equals(HUH)
	            || outcome.equals(FAILED)
	            || outcome.equals(MISSING_COMPONENT)
	            || outcome.equals(TIMEOUT)
	            || outcome.equals(NOT_IMPLEMENTED))
	        return false;

	    // now parse through the stack trace to see if this is exception originates in
	    // the student implementation code or in
	    BufferedReader reader=null;
	    try {
	    	reader = new BufferedReader(new StringReader(getLongTrimmedTestResult()));
	        // throw out the first line of the stacktrace (this is just the exception name)
	        String line=reader.readLine();
	        if (line == null) {
	            System.err.println("Missing first line of the stack trace for " +this.toString());
	            return false;
	        }
	        // read the second line of the stack trace-- this is the true source of the exception
	        line = reader.readLine();
	        if (line == null) {
	            System.err.println("Missing second line of the stack trace for " +this.toString());
	            return false;
	        }

	        // if the source of the stack trace exception contains a class that looks like
	        // a junit test driver classname, then the source of the exception is the driver
	        // TODO make sure instructors standardize on the names of the junit test suites
	        // i.e. always PublicTests, ReleaseTests, SecretTests
	        if (line.contains("ReleaseTest") ||
	                line.contains("PublicTest") ||
	                line.contains("SecretTest") ||
	                line.contains("SimpleTest") ||
	                line.contains("TestAgainstFile") ||
	                line.contains("SpiderTest"))
	        {
	            //System.err.println("matching line: " +line);
	            return true;
	        }
	        return false;
	    } catch (IOException e) {
	        throw new RuntimeException("A readLine() to a StringReader failed! ", e);
	    } finally {
	    	IOUtils.closeQuietly(reader);
	    }
	}

	/**
	 * Does this testOutcome have coverage information avaiable?
	 * <p>
	 * Must be a test type with a non-null details field containing zipped XML
	 * of the coverage results.
	 * @return True if this testOutcome contains coverage information; false otherwise.
	 */
	public boolean isCoverageType() {
	    return isCardinalTestType() || isStudentTestType();
	}

	/**
	 * Is this testOutcome one of the cardinal test types (public, release, secret)?
	 * @return True if this testOutcome is a test type (i.e. a public, release or secret
	 * outcome); false if it it some other type of testOutcome (i.e. a FindBugs warning
	 * or a student-written test).
	 */
	public boolean isCardinalTestType() {
	    return testType.isScored();
	}

	/**
	 * Is this testOutcome the result of executing a student-written test case?
	 * @return True if this testOutcome is a student-written test; false otherwise.
	 */
	public boolean isStudentTestType() {
		return testType.equals(STUDENT);
	}

	public boolean coversFileAtLineNumber(String fileName, int lineNumber)
	throws IOException
	{
		// Code coverage is stored as the zipped XML of the coverage results in
		// the details field of the TestOutcome object.  Yes, this is specific
		// to coverage tools that use XML (which seems to be most of them)
		// and perhaps I could do it a more robust way.  But that's how it is for now.

		// Can't have code coverage if this is not a test outcome with
		// coverage information available.
		if (!isCoverageType())
			throw new IllegalStateException("Cannot call this method on a testType lacking coverage information! ");

		CodeCoverageResults codeCoverageResults = getCodeCoverageResults();
        return codeCoverageResults.coversFileAtLineNumber(fileName,lineNumber);
	}

	/**
	 * @param requestedSourceFileName
	 * @param requestedLineNumber
	 * @return
	 */
	public boolean isStackTraceAtLineForFile(String requestedSourceFileName, int requestedLineNumber)
	{
	    if (!isCardinalTestType())
	        return false;
	    if (longTestResult == null || longTestResult.equals(""))
	        return false;

	    BufferedReader reader=null;
	    Pattern pattern = Pattern.compile("\\((\\w+\\.java):(\\d+)\\)");
	    try {
	    	reader = new BufferedReader(new StringReader(getLongTrimmedTestResult()));
	        while (true)
	        {
	            String line =reader.readLine();
	            if (line == null) break;
	            //System.out.println("line: " +line);
	            Matcher matcher = pattern.matcher(line);
	            if (!line.contains("java.") && !line.contains("junit.") &&
	                    !line.contains("\\s+sun\\.reflect") &&
	                    !line.contains("edu.umd.cs.buildServer") &&
	                    !line.contains("ReleaseTest") &&
	                    !line.contains("PublicTest") &&
	                    !line.contains("SecretTest") &&
	                    !line.contains("SimpleTest") &&
	                    !line.contains("TestAgainstFile") &&
	                    !line.contains("SpiderTest") &&
	                    matcher.find() && matcher.groupCount() > 1) {
	                String sourceFileName=matcher.group(1);
	                String lineNumber=matcher.group(2);

	                if (requestedSourceFileName.equals(sourceFileName) &&
	                        Integer.valueOf(lineNumber).intValue() == requestedLineNumber)
	                    return true;
	            }
	        }
	    } catch (IOException ignore) {
	        // cannot happen; we're reading from a String!
	    } finally {
            IOUtils.closeQuietly(reader);
	    }
        return false;
	}

	public boolean isFindBugsWarning()
	{
	    return testType.equals(FINDBUGS);
	}

	public boolean isFindBugsWarningAtLine(String requestedSourceFileName, int requestedLineNumber)
	{
	    if (!testType.equals(FINDBUGS))
	        return false;


	    // TODO handle multi-line FB warnings
	    Pattern pattern = Pattern.compile("At (\\w+\\.java):\\[line (\\d+)\\]");
	    Matcher matcher = pattern.matcher(shortTestResult);
	    if (matcher.matches() && matcher.groupCount() > 1)
	    {
	        String sourceFileName = matcher.group(1);
            String lineNumber = matcher.group(2);
            if (requestedSourceFileName.equals(sourceFileName) &&
                    requestedLineNumber == Integer.parseInt(lineNumber)) {
                return true;
            }
	    }
        return false;
	}

	public static FileNameLineNumberPair getFileNameLineNumberPair(String shortTestResult)
	{
	    // TODO handle multi-line FB warnings, and FB warnings w/ no line number
	    Pattern pattern = Pattern.compile("At (\\w+\\.java):\\[line (\\d+)\\]");
	    Matcher matcher = pattern.matcher(shortTestResult);
	    if (matcher.matches() && matcher.groupCount() > 1)
	    {
	        String sourceFileName = matcher.group(1);
	        String lineNumber = matcher.group(2);
	        return new FileNameLineNumberPair(sourceFileName, lineNumber);
	    }
	    return null;
	}

    public StackTraceElement getInnermostStackTraceElement()
    {
        if (isError()) {
            if (longTestResult == null || longTestResult.equals(""))
                return null;
            TextFileReader reader = null;
            try {
                reader=new TextFileReader(new StringReader(getLongTrimmedTestResult()));
                for (String line : reader) {
                    // System.out.println("line: " +line);
                    StackTraceElement element=MarmosetUtilities.parseStackTrace(line);
                    if (element!=null)
                        return element;
                }
            } catch (IOException ignore) {
                throw new RuntimeException("DAMMIT JIM!",ignore);
            } finally {
                try {
                    if (reader != null) reader.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
        return null;
    }

    public FileNameLineNumberPair getFileNameLineNumberPair()
    {
        if (getTestType().equals(FINDBUGS)) {
            return getFileNameLineNumberPair(getShortTestResult());
        } else if (getTestType().equals(PUBLIC) ||
                getTestType().equals(RELEASE) ||
                getTestType().equals(SECRET))
        {
            BufferedReader reader=null;
    	    Pattern pattern = Pattern.compile("\\((\\w+\\.java):(\\d+)\\)");
    	    try {
    	        reader = new BufferedReader(new StringReader(getLongTrimmedTestResult()));
    	    	while (true) {
    	            String line =reader.readLine();
    	            if (line == null) break;

    	            // skip over frames that we won't be able to link to
    	            Matcher matcher = pattern.matcher(line);
    	            if (!line.contains("java.") && !line.contains("junit.") &&
    	                    !line.contains("\\s+sun\\.reflect") &&
    	                    !line.contains("edu.umd.cs.buildServer") &&
    	                    !line.contains("ReleaseTest") &&
    	                    !line.contains("PublicTest") &&
    	                    !line.contains("SecretTest") &&
    	                    !line.contains("SimpleTest") &&
    	                    !line.contains("TestAgainstFile") &&
    	                    !line.contains("SpiderTest") &&
    	                    matcher.find() && matcher.groupCount() > 1) {
    	                String sourceFileName=matcher.group(1);
    	                String lineNumber=matcher.group(2);
    	                return new FileNameLineNumberPair(sourceFileName, lineNumber);
    	            }
    	        }
    	    } catch (IOException ignore) {
    	        // cannot happen; reading from a String!
    	    } finally {
    	    	IOUtils.closeQuietly(reader);
    	    }
	        // XXX Does it make sense here to return a singleton representing nothing?
    	    return null;
        } else {
            throw new IllegalStateException("You cannot get the filename and line number " +
            		" of a test outcome of type " +getTestType());
        }
    }
}
