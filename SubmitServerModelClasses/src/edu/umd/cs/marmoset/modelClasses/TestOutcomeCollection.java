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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

import edu.umd.cs.marmoset.codeCoverage.CodeCoverageResults;
import edu.umd.cs.marmoset.codeCoverage.FileWithCoverage;
import edu.umd.cs.marmoset.modelClasses.TestOutcome.OutcomeType;
import edu.umd.cs.marmoset.modelClasses.TestOutcome.TestType;
import edu.umd.cs.marmoset.utilities.Objects;
import edu.umd.cs.marmoset.utilities.SqlUtilities;

/**
 * A collection of TestOutcomes. These are associated with a specific
 * testRunPK; this class should not be used to store test outcomes from
 * different testRuns.  There is no check for this specifically, though.
 *
 * @author David Hovemeyer
 * @author jspacco
 */

public class TestOutcomeCollection implements ITestSummary<TestOutcomeCollection>, Iterable<TestOutcome> {
    private List<TestOutcome> testOutcomes = new ArrayList<TestOutcome>();
    private Map<String, TestOutcome> testMap = new HashMap<String, TestOutcome>();
    private boolean compileSuccessful = false;
    private Integer testRunPK = null;



	public static String formattedColumnHeaders(int numCols, TestOutcomeCollection canonicalResults)
    {
    	if (canonicalResults == null) return "";
		StringBuffer buf = new StringBuffer();
		List<TestOutcome> publicOutcomes = canonicalResults.getPublicOutcomes();
		List<TestOutcome> releaseOutcomes = canonicalResults.getReleaseOutcomes();
		List<TestOutcome> secretOutcomes = canonicalResults.getSecretOutcomes();

		for (int ii=0; ii<numCols; ii++) {
			buf.append("<col>");
		}

		// col.right adds a dark line to the right border of the cells that divide the different
		// categories of tests
		if (!publicOutcomes.isEmpty()) {
			for (int ii=0; ii<publicOutcomes.size()-1;ii++) {
				buf.append("<col>");
			}
			buf.append("<col class=\"right\">");
		}
		if ( !releaseOutcomes.isEmpty()) {
			for (int ii=0; ii<releaseOutcomes.size()-1;ii++) {
				buf.append("<col>");
			}
			buf.append("<col class=\"right\">");
		}
		if (!secretOutcomes.isEmpty()) {
			for (int ii=0; ii<secretOutcomes.size()-1;ii++) {
				buf.append("<col>");
			}
		}
		return buf.toString();
    }


	public static String formattedTestHeaderTop(TestOutcomeCollection canonicalResults, boolean instructorView)
    {
        if (canonicalResults == null) return "";
		StringBuffer buf = new StringBuffer();
		List<TestOutcome> publicOutcomes = canonicalResults.getPublicOutcomes();
		List<TestOutcome> releaseOutcomes = canonicalResults.getReleaseOutcomes();
		List<TestOutcome> secretOutcomes = canonicalResults.getSecretOutcomes();

		if (!publicOutcomes.isEmpty()) {
			buf.append("<th colspan=\""
					+ publicOutcomes.size()
					+ "\">Public</th>");
		}
		if (!releaseOutcomes.isEmpty()) {
			buf.append("<th colspan=\""
					+ releaseOutcomes.size()
					+ "\">Release</th>");
		}
		if (instructorView && !secretOutcomes.isEmpty()) {
			buf.append("<th colspan=\""
					+ secretOutcomes.size()
					+ "\">Secret</th>");
		}
		return buf.toString();
	}

	public static String formattedTestHeader(
	TestOutcomeCollection canonicalResults) {
		return formattedTestHeader(canonicalResults, true);
	}

	public static String formattedTestHeader(
			TestOutcomeCollection canonicalResults, boolean instructorView) {
	    if (canonicalResults == null) return "";
		StringBuffer buf = new StringBuffer();

		if (!canonicalResults.getPublicOutcomes().isEmpty()) {
			for (Iterator<TestOutcome> i = canonicalResults.getPublicOutcomes().iterator(); i
					.hasNext(); ) {
				TestOutcome test = i.next();
				buf.append("<th><a title=\"" + test.getShortTestName()+"\">" + test.getTestNumber() + "</a></th>");
			}
		}

		if (!canonicalResults.getReleaseOutcomes().isEmpty()) {
			for (Iterator<TestOutcome> i = canonicalResults.getReleaseOutcomes().iterator(); i
					.hasNext(); ) {
				TestOutcome test = i.next();
				if (instructorView) 
                buf.append("<th><a title=\"" + test.getShortTestName()+"\">" + test.getTestNumber() + "</a></th>");
				else
					buf.append("<th>" + test.getTestNumber() + "</th>");
			}
		}

		if (instructorView && !canonicalResults.getSecretOutcomes().isEmpty()) {
			for (Iterator<TestOutcome> i = canonicalResults.getSecretOutcomes().iterator(); i
					.hasNext(); ) {
				TestOutcome test = i.next();
                buf.append("<th><a title=\"" + test.getShortTestName()+"\">" + test.getTestNumber() + "</a></th>");
			}
		}
		return buf.toString();
	}

	/**
	 * Returns HTML formatted for a table using a cascading style sheet.
	 * If results == null then the test results are not ready, and we color
	 * the table background the same if the tests could_not_run.
	 * @param canonicalResults
	 * @param results
	 * @return
	 */
	public static String formattedTestResults(TestOutcomeCollection canonicalResults,
	        TestOutcomeCollection results) {
				return formattedTestResults(canonicalResults, results,
						true, false);
			}

	public static String formattedTestResults(TestOutcomeCollection canonicalResults,
            TestOutcomeCollection results,  boolean releaseTested) {
		return formattedTestResults(canonicalResults, results,
				false, releaseTested);
	}
 
	/**
	 * Returns HTML formatted for a table using a cascading style sheet.
	 * If results == null then the test results are not ready, and we color
	 * the table background the same if the tests could_not_run.
	 * @param canonicalResults
	 * @param results
	 * @param instructorView TODO
	 * @param releaseTested TODO
	 * @return
	 */
	public static String formattedTestResults(TestOutcomeCollection canonicalResults,
            TestOutcomeCollection results, boolean instructorView, boolean releaseTested) {
        if (canonicalResults == null)
            return "";
        StringBuffer buf = new StringBuffer();

        if (!canonicalResults.getPublicOutcomes().isEmpty()) {
            if (results == null || results.getCouldNotRunPublicTests()) {
                buf.append("<td class=\"could_not_run\" colspan=\""
                        + canonicalResults.getPublicOutcomes().size() + "\">&nbsp;</td>");
            } else {
                List<TestOutcome> tests = canonicalResults.getPublicOutcomes();
                formatTestResults(results, buf, tests);
            }
        }

        if (!canonicalResults.getReleaseOutcomes().isEmpty()) {
            if (results == null || results.getCouldNotRunReleaseTests()) {
                buf
                        .append("<td class=\"could_not_run\" colspan=\""
                                + canonicalResults.getReleaseOutcomes().size()
                                + "\">&nbsp;</td>");
            } else if (instructorView || releaseTested) {
                List<TestOutcome> tests = canonicalResults.getReleaseOutcomes();
                formatTestResults(results, buf, tests);
            } else 
            	   buf
                   .append("<td  colspan=\""
                           + canonicalResults.getReleaseOutcomes().size()
                           + "\">Not release tested</td>");
        }

        if (instructorView && !canonicalResults.getSecretOutcomes().isEmpty()) {
            if (results == null || results.getCouldNotRunSecretTests()) {
                buf.append("<td class=\"could_not_run\" colspan=\""
                        + canonicalResults.getSecretOutcomes().size() + "\">&nbsp;</td>");
            } else {
                List<TestOutcome> tests = canonicalResults.getSecretOutcomes();
                formatTestResults(results, buf, tests);
            }
        }

        return buf.toString();
    }

    /**
     * @param results
     * @param buf
     * @param tests
     */
    private static void formatTestResults(TestOutcomeCollection results, StringBuffer buf, List<TestOutcome> tests) {
        for (Iterator<TestOutcome> i = tests.iterator(); i.hasNext();) {
            TestOutcome canonicalResult = i.next();
            TestOutcome test = results.getTest(canonicalResult.getTestName());
            if (test != null)
                buf.append("<td class=\"" + test.getOutcome() + "\"><a title=\""
                        + test.getShortTestName() + "\">&nbsp;</a></td>");
            else {
                buf.append("<td class=\"could_not_run\"><a title=\""
                        + canonicalResult.getShortTestName() + "\">&nbsp;</td>");
            }
        }
    }
    /**
	 * Checks if the collection is empty.
	 *
	 * @return true if the collection is empty; false otherwise.
	 */
    public boolean isEmpty() {
        return testOutcomes.isEmpty();
    }

    /**
     * Returns a compact summary of the public, release and secret test scores
     * suitable to be displayed on a web-page.
     * @return A summary of the public, release and secret scores for this project.
     */
    public String getCompactOutcomesSummary() {
        return getValuePublicTestsPassed() +" / " +
            getValueReleaseTestsPassed() +" / " +
            getValueSecretTestsPassed();
    }
    /**
     * Returns a more detailed summary consisting of the public, release and secret scores,
     * along with the number of FindBugs warnings and student-written tests, to be displayed
     * on a web page.  Currenly only Java supports FindBugs warnings and student-written JUnit
     * tests.
     * @return A detailed summary of the scores that includes the number of FindBugs warnings
     * and the number of student-written tests.
     */
    public String getJavaOutcomesSummary() {
        return getCompactOutcomesSummary() +" / "+
            getNumFindBugsWarnings() +" / "+
            getNumStudentWrittenTests();
    }

    /* (non-Javadoc)
     * @see edu.umd.cs.marmoset.modelClasses.ITestSummary#getValuePassedOverall()
     */
    @Override
	public int getValuePassedOverall() {
        // if the build_test returned COULD_NOT_RUN, then return 0 for the
        // overall
        if (!isCompileSuccessful())
            return 0;
        int publicValue = getValuePublicTestsPassed();
        int releaseValue = getValueReleaseTestsPassed();
        int secretValue = getValueSecretTestsPassed();
        if (publicValue < 0) publicValue=0;
        if (releaseValue < 0) releaseValue=0;
        if (secretValue < 0) secretValue=0;
        return publicValue + releaseValue + secretValue;
    }

    /**
     * Get number of release tests in this collection.
     *
     * @return number of release tests in this collection
     */
    public int getValueReleaseTests() {
        return scoreOutcomes(getReleaseOutcomes());
    }

    /**
     * Get number of public tests for this collection.
     *
     * @return number of public tests for this collection.
     */
    public int getValuePublicTests() {
        return scoreOutcomes(getOutcomesForTestType(TestOutcome.TestType.PUBLIC));
    }



    /**
     * Get number of failed tests.
     * @return number of failed tests
     */
    public int getValueFailedOverall() {
        return scoreNonPassedOutcomes();
    }

    /**
     * Get number of passed tests.
     * @return number of passed tests
     */
    public int getNumPassedOverall() {
        int numPassed=0;
        for (TestOutcome outcome : getAllScoredOutcomes()) {
            if (outcome.isPassed())
                numPassed++;
        }
        return numPassed;
    }

    public int getValueSecretTests() {
        return scoreOutcomes(getOutcomesForTestType(TestOutcome.TestType.SECRET));
    }

    /* (non-Javadoc)
     * @see edu.umd.cs.marmoset.modelClasses.ITestSummary#getValueSecretTestsPassed()
     */
    @Override
	public int getValueSecretTestsPassed() {
        List<TestOutcome> secretTests = getSecretOutcomes();
        return scoreOutcomes(TestOutcome.PASSED, secretTests);
    }

    /* (non-Javadoc)
     * @see edu.umd.cs.marmoset.modelClasses.ITestSummary#getValueReleaseTestsPassed()
     */
    @Override
	public int getValueReleaseTestsPassed() {
        List<TestOutcome> releaseTests = getReleaseOutcomes();
        return scoreOutcomes(TestOutcome.PASSED, releaseTests);
    }


    /* (non-Javadoc)
     * @see edu.umd.cs.marmoset.modelClasses.ITestSummary#getValuePublicTestsPassed()
     */
    @Override
	public int getValuePublicTestsPassed() {
        List<TestOutcome> publicTests = getOutcomesForTestType(TestOutcome.TestType.PUBLIC);
        return scoreOutcomes(TestOutcome.PASSED, publicTests);
    }

    /**
     * @return True if this collection has passed all the public tests;
     * false otherwise.
     */
    public boolean getPassedAllPublicTests() {
        List<TestOutcome> publicTests = getOutcomesForTestType(TestOutcome.TestType.PUBLIC);
        return (publicTests.size() == countOutcomes(TestOutcome.PASSED, getPublicOutcomes()));
    }

    /**
     * @return True if this collection has passed all the release tests;
     * false otherwise.
     */
    public boolean getPassedAllReleaseTests() {
        List<TestOutcome> releaseTests = getOutcomesForTestType(TestOutcome.TestType.RELEASE);
        return (releaseTests.size() == countOutcomes(TestOutcome.PASSED, getReleaseOutcomes()));
    }

    /* (non-Javadoc)
     * @see edu.umd.cs.marmoset.modelClasses.ITestSummary#isCompileSuccessful()
     */
    @Override
	public boolean isCompileSuccessful() {
        return compileSuccessful;
    }

    /**
     * Get number of failed tests.
     * @return number of failed tests
     */
    public int getNumFailedOverall() {
        return countNonPassedOutcomes();
    }

    /**
     * Get the number of student-written tests.
     * @return The number of student-written tests.
     */
    public int getNumStudentWrittenTests() {
        return getStudentOutcomes().size();
    }

    /**
     * Return number of TestOutcomes with given outcome.
     *
     * @param outcome
     *            the outcome (PASSED or FAILED/ERROR/HUH)
     * @return number of TestOutcomes with given outcome
     */
    public int countOutcomes(@OutcomeType String outcome) {
        int count = 0;
        for (TestOutcome testOutcome : testOutcomes) {
            if (testOutcome.getOutcome().equals(outcome))
                ++count;
        }
        return count;
    }

    public int countCardinalOutcomes(@OutcomeType String outcome) {
        int count = 0;
        for (TestOutcome testOutcome : getAllScoredOutcomes()) {
            if (testOutcome.getOutcome().equals(outcome))
                ++count;
        }
        return count;
    }

    /**
     * Count the number of outcomes with the given outcome in a given list of testOutcomes
     * @param outcome the outcome to count
     * @param outcomeList the list of testOutcomes
     * @return the count of the number of instances of the given outcome
     */
    int countOutcomes(@OutcomeType String outcome, List<TestOutcome> outcomeList)
    {
        int count=0;
        for (Iterator<TestOutcome> i = outcomeList.iterator(); i.hasNext();) {
            TestOutcome testOutcome = i.next();
            if (testOutcome.getOutcome().equals(outcome))
                ++count;
        }
        return count;
    }

    private int countNonPassedOutcomes()
    {
        int nonPassed=0;
        for (TestOutcome outcome : getAllScoredOutcomes()) {
            if (outcome.isFailed())
                nonPassed++;
        }
        return nonPassed;
    }

    private int scoreNonPassedOutcomes()
    {
        int nonPassedScore=0;
        for (Iterator<TestOutcome> ii=testOutcomes.iterator(); ii.hasNext();)
        {
            TestOutcome outcome = ii.next();
            if (outcome.getTestType().equals(TestOutcome.TestType.FINDBUGS))
                continue;
            if (!outcome.getOutcome().equals(TestOutcome.PASSED))
                nonPassedScore += outcome.getPointValue();
        }
        return nonPassedScore;
    }


    private int scoreOutcomes(@OutcomeType String outcome, List<TestOutcome> list) {
        int count = 0;
        for (Iterator<TestOutcome> i = list.iterator(); i.hasNext();) {
            TestOutcome testOutcome = i.next();
            if (testOutcome.getTestType().equals(TestOutcome.TestType.FINDBUGS))
                continue;
            if (testOutcome.getOutcome().equals(outcome))
                count += testOutcome.getPointValue();
        }
        return count;
    }
    private int scoreOutcomes(List<TestOutcome> list) {
        int count = 0;
        for (Iterator<TestOutcome> i = list.iterator(); i.hasNext();) {
            TestOutcome testOutcome = i.next();

                count += testOutcome.getPointValue();
        }
        return count;
    }

    @Deprecated
    public List<TestOutcome> getAllTestOutcomes() {
    	return getAllScoredOutcomes();
    }

    /**
     * Get a list of the scored test results
     *
     */
	public List<TestOutcome> getAllScoredOutcomes() {

		ArrayList<TestOutcome> outcomes = new ArrayList<TestOutcome>();
		for (TestOutcome outcome : getAllOutcomes())
			if (outcome.getTestType().isScored())
				outcomes.add(outcome);

		return outcomes;
	}

    /**
     * Gets only the build and quick outcomes
     *
     * @return a List of the build and quick outcomes.
     */
    public TestOutcome getBuildOutcome() {
        List<TestOutcome> buildOutcomes = getOutcomesForTestType(TestOutcome.TestType.BUILD);
        return buildOutcomes.get(0);
    }


    /**
     * Gets only the public outcomes
     * @return a List of the build and quick outcomes.
     */
    public List<TestOutcome> getPublicOutcomes() {
        List<TestOutcome> publicOutcomes = getOutcomesForTestType(TestOutcome.TestType.PUBLIC);
        return publicOutcomes;
    }

    public boolean getCouldNotRunPublicTests() {
        List<TestOutcome> publicOutcomes = getOutcomesForTestType(TestOutcome.TestType.PUBLIC);
        if (publicOutcomes.size() != 1) return false;
        return (publicOutcomes.get(0)).getOutcome().equals(TestOutcome.COULD_NOT_RUN);
    }

    public boolean getCouldNotRunReleaseTests() {
        List<TestOutcome> releaseOutcomes = getOutcomesForTestType(TestOutcome.TestType.RELEASE);
        if (releaseOutcomes.size() != 1) return false;
        return (releaseOutcomes.get(0)).getOutcome().equals(TestOutcome.COULD_NOT_RUN);
    }

    public boolean getCouldNotRunSecretTests() {
        List<TestOutcome> secretOutcomes = getOutcomesForTestType(TestOutcome.TestType.SECRET);
        if (secretOutcomes.size() != 1) return false;
        return (secretOutcomes.get(0)).getOutcome().equals(TestOutcome.COULD_NOT_RUN);
    }

    public boolean getCouldNotRunAnyCardinalTests() {
    	return getCouldNotRunPublicTests() || getCouldNotRunReleaseTests() ||
    		getCouldNotRunSecretTests();
    }

    /**
     * Gets a list of release test outcomes.
     *
     * @return A list of all release test outcomes contained in this collection.
     */
    public List<TestOutcome> getReleaseOutcomes() {
        return getOutcomesForTestType(TestOutcome.TestType.RELEASE);
    }

    /**
     * Gets a list of the secret test outcomes.
     *
     * @return A list of the secret test outcomes in this collection.
     */
    public List<TestOutcome> getSecretOutcomes() {
        return getOutcomesForTestType(TestOutcome.TestType.SECRET);
    }
    /**
     * Gets a list of student test outcomes.
     * @return A list of all student test outcomes in this collection.
     */
    public List<TestOutcome> getStudentOutcomes() {
    	return getOutcomesForTestType(TestOutcome.TestType.STUDENT);
    }

    /**
     * Gets only the findbugs outcomes.
     * @return a List of the findbugs outcomes.
     */
    public List<TestOutcome> getFindBugsOutcomes() {
        return getOutcomesForTestType(TestOutcome.TestType.FINDBUGS);
    }

    /**
     * Gets only the PMD outcomes.
     * @return a List of the pmd outcomes.
     */
    public List<TestOutcome> getPmdOutcomes() {
        return getOutcomesForTestType(TestOutcome.TestType.PMD);
    }

    /**
     * Checks if the submission that produced this collection of test outcomes
     * is release eligible. A submission is release eligible if all of the build
     * and quick test have passed.
     *
     * @return true if all of the build and quick tests passed; false if any of
     *         them failed.
     */
    public boolean isReleaseEligible() {
        if (!isCompileSuccessful())
            return false;
        for (Iterator<TestOutcome> it = getPublicOutcomes().iterator(); it.hasNext();) {
            TestOutcome outcome = it.next();

            if (!outcome.getStudentOutcome().equals(TestOutcome.PASSED))
                return false;
        }
        return true;
    }

    /**
     * Constructor.
     */
    public TestOutcomeCollection() {
    }

	public static TestOutcomeCollection deserialize(byte[] data) throws IOException {
		TestOutcomeCollection testOutcomeCollection = new TestOutcomeCollection();
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new ByteArrayInputStream(data));
			testOutcomeCollection.read(in);
		} finally {
			if (in != null)
				in.close();
		}

		// Make sure test outcome collection is not empty
		if (testOutcomeCollection.isEmpty()) {
			throw new IllegalArgumentException("Empty test outcome collection");
		}
		return testOutcomeCollection;
	}
    
    	 
    /**
     * Add a TestOutcome.
     *
     * @param outcome
     *            the TestOutcome to add
     */
    public void add(TestOutcome outcome) {
        if (testRunPK == null)
            testRunPK = outcome.getTestRunPK();
        if (outcome.getTestType().equals(TestOutcome.TestType.BUILD))
            compileSuccessful = outcome.getOutcome().equals(TestOutcome.PASSED);
        testOutcomes.add(outcome);
        testMap.put(outcome.getTestName(), outcome);
    }

    public TestOutcome getTest(String name) {
        return testMap.get(name);
    }

    
    public @CheckForNull TestOutcome getTest(TestType type, String number) {
        for(TestOutcome t : testOutcomes)
            if (type.equals(t.getTestType()) && number.equals(t.getTestNumber()))
                return t;
        return null;
    }
    public Map<String,TestOutcome> getTestMap() {
        return testMap;
    }

    public void addAll(Collection<TestOutcome> collection) {
        for(Iterator<TestOutcome> i = collection.iterator(); i.hasNext(); ) {
            TestOutcome outcome = i.next();
            add(outcome);
        }
    }

    public Iterator<TestOutcome> cardinalTestTypesIterator()
    {
    	return iterator(TestOutcome.TestType.PUBLIC,
    	        TestOutcome.TestType.RELEASE,
    	        TestOutcome.TestType.SECRET);
    }

    public Iterator<TestOutcome> findBugsIterator() {
    	return iterator(TestOutcome.TestType.FINDBUGS);
    }

    public Iterable<TestOutcome> getIterableForCardinalTestTypes()
    {
    	return new Iterable<TestOutcome>() {

			/* (non-Javadoc)
			 * @see java.lang.Iterable#iterator()
			 */
			@Override
			public Iterator<TestOutcome> iterator()
			{
				return cardinalTestTypesIterator();
			}

    	};
    }

    private Iterator<TestOutcome> iterator(final TestType... testTypes) {
        return new Iterator<TestOutcome>() {
            private Iterator<TestOutcome> ii = testOutcomes.iterator();

            private TestOutcome next;

            @Override
			public boolean hasNext() {
                while (ii.hasNext()) {
                    TestOutcome testOutcome = ii.next();
                    for (int jj = 0; jj < testTypes.length; jj++) {
                        if (testOutcome.getTestType().equals(testTypes[jj])) {
                            next = testOutcome;
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
			public TestOutcome next() {
                if (next == null)
                    throw new NoSuchElementException(
                            "No elements remaining in this iterator");
                return next;
            }

            @Override
			public void remove() {
                throw new UnsupportedOperationException("cannot call remove here");
            }
        };
    }

    /**
     * Returns a list of the outcomes mapped to the given test type.
     *
     * @param testType the type of test outcomes we want
     * @return a List of the test outcomes of the given type that are contained
     *         in this collection.
     */
    private List<TestOutcome> getOutcomesForTestType(TestType testType) {
        List<TestOutcome> result = new ArrayList<TestOutcome>();
        for (Iterator<TestOutcome> ii = iterator(testType); ii.hasNext();) {
            result.add((TestOutcome)ii.next());
        }
        return result;
    }

    /**
     * Return a Collection of all the TestOutcomes.
     *
     * @return a Collection of all the TestOutcomes
     */
    public List<TestOutcome> getAllOutcomes() {
        return testOutcomes;
    }

    /**
     * Get an Iterator over all the TestOutcomes.
     *
     * @return Iterator over all the TestOutcomes
     */
    @Override
	public Iterator<TestOutcome> iterator() {
        return testOutcomes.iterator();
    }

    public void dump(PrintStream out) {
        for (Iterator<TestOutcome> i = iterator(); i.hasNext();) {
            TestOutcome outcome = i.next();
            out.println(outcome.toString());
        }
    }

    /**
     * Returns the number of test outcomes contained in this collection.
     *
     * @return The number of test outcomes contained in this collection.
     */
    public int size() {
        return testOutcomes.size();
    }

    /**
     * Read collection from given ObjectInputSTream source. TestOutcomes are added
     * until EOF is reached.
     *
     * @param in the ObjectInputStream source
     */
    public void read(ObjectInputStream in) throws IOException {
        try {
            @SuppressWarnings("unchecked")
			List <TestOutcome>outcomes = (List<TestOutcome>) in.readObject();
            for (TestOutcome to : outcomes)
                add(to);

        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Write collection to given ObjectOutputStream sink.
     *
     * @param out
     *            the ObjectOutputStream sink
     * @throws IOException
     */
    public void write(ObjectOutputStream out) throws IOException {
            out.writeObject(testOutcomes);
    }

    List<String> getHeaders() {
        List<String> headers = new ArrayList<String>();
        for (Iterator<TestOutcome> ii = getOutcomesForTestType(TestOutcome.TestType.BUILD).iterator(); ii
                .hasNext();) {
            TestOutcome outcome = (TestOutcome) ii.next();

            headers.add("b" + Formats.twoDigitInt.format(outcome.getTestNumber()));
        }
        for (Iterator<TestOutcome> ii = getOutcomesForTestType(TestOutcome.TestType.PUBLIC).iterator(); ii
                .hasNext();) {
            TestOutcome outcome = (TestOutcome) ii.next();

            headers.add("q" + Formats.twoDigitInt.format(outcome.getTestNumber()));
        }
        for (Iterator<TestOutcome> ii = getOutcomesForTestType(TestOutcome.TestType.RELEASE).iterator(); ii
                .hasNext();) {
            TestOutcome outcome = (TestOutcome) ii.next();

            headers.add("r" + Formats.twoDigitInt.format(outcome.getTestNumber()));
        }
        return headers;
    }

    /* (non-Javadoc)
     * @see edu.umd.cs.marmoset.modelClasses.ITestSummary#getNumFindBugsWarnings()
     */
    @Override
	public int getNumFindBugsWarnings() {
        //return countOutcomes(TestOutcome.WARNING);
        int count = 0;
        for (TestOutcome testOutcome : testOutcomes) {
            if (testOutcome.getTestType().equals(TestOutcome.TestType.FINDBUGS))
                ++count;
        }
        return count;
    }




    /**
     * Private helper method that executes the prepared statement and returns a
     * collection of test outcomes.
     *
     * @param stmt
     *            the statement to execute
     * @return a collection of the test outcomes returned by the given prepared
     *         statement. The collection will never be null, though it might be
     *         empty.
     * @throws SQLException
     */
    private static TestOutcomeCollection getAllFromPreparedStatement(
            PreparedStatement stmt) throws SQLException {
        ResultSet rs = stmt.executeQuery();

        TestOutcomeCollection testOutcomes = new TestOutcomeCollection();

        while (rs.next()) {
            TestOutcome outcome = new TestOutcome(rs, 1);
            testOutcomes.add(outcome);
        }
        return testOutcomes;
    }

    /**
     * @param testRunPK
     * @param conn
     * @return
     */
    public static TestOutcomeCollection lookupByTestRunPK(Integer testRunPK,
            Connection conn) throws SQLException {
        String query = " SELECT " + TestOutcome.ATTRIBUTES + " FROM test_outcomes "
                + " WHERE test_run_pk = ? ";

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(query);
            SqlUtilities.setInteger(stmt, 1, testRunPK);
            return getAllFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
    }

    public static TestOutcomeCollection lookupBySubmissionPK(
    		 @Submission.PK int submissionPK,
            Connection conn)
    throws SQLException
    {
        String query =
            " SELECT " +TestOutcome.ATTRIBUTES+
            " FROM test_outcomes, submissions " +
            " WHERE submissions.current_test_run_pk = test_outcomes.test_run_pk" +
            " AND submissions.submission_pk = ? ";
        PreparedStatement stmt=null;
        try {
            stmt = conn.prepareStatement(query);
            SqlUtilities.setInteger(stmt, 1, submissionPK);
            return getAllFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
    }

    /**
     * Sets the testRunPKs to the given value. This method is used by the
     * SubmitServer to set the testRunPK of all the testOutcomes in a collection
     * returned by the BuildServer. The BuildServer leaves this field empty
     * because it is unknown until the SubmitServer inserts a new testRun
     * object, therefore generating the testRunPK.
     *
     * @param testRunPK
     *            the testRunPK to set
     */
    public void updateTestRunPK(Integer testRunPK) {
        for (Iterator<TestOutcome> ii = iterator(); ii.hasNext();) {
            TestOutcome testOutcome = ii.next();
            testOutcome.setTestRunPK(testRunPK);
        }
    }


	private final static Logger log = Logger
			.getLogger(TestOutcomeCollection.class);

    /**
     * Insert the contents of a TestOutcomeCollection into the test_outcomes
     * table.
     *
     * TODO consider making this an instance method of TestOutcomeCollection
     *
     * @param conn
     *            the database connection
     * @param testOutcomeCollection
     *            the collection of test outcomes to be inserted
     * @throws SQLException
     */
    public void insert(Connection conn) throws SQLException {
        String query = Queries.makeInsertStatementUsingSetSyntax(TestOutcome.ATTRIBUTE_NAME_LIST, TestOutcome.TABLE_NAME, false);

        PreparedStatement stmt = conn.prepareStatement(query);

        try {
            // Put values into statement (preparing for bulk insert)
            List<String> kinds = new ArrayList<String>();
            for (TestOutcome outcome : testOutcomes) {
                kinds.add( outcome.getTestType() + "-" + outcome.getTestNumber());
                outcome.putValues(stmt, 1);
                stmt.addBatch();
            }

            // Insert the values!
            stmt.executeBatch();
        } finally {
            Queries.closeStatement(stmt);
        }
    }

    /**
     * Performs a batch update of the point values of each test outcome in this collection
     * after an AssignPoints operation.
     * For efficiency, this batch update only affects the point values and does not try
     * to update any other fields.
     * @param conn
     * @throws SQLException
     */
    public void batchUpdatePointValues(Connection conn)
    throws SQLException
    {
        String update =
            " UPDATE " + TestOutcome.TABLE_NAME +
            " SET " +
            " point_value = ? " +
            " WHERE test_run_pk = ? " +
            " AND test_type = ? " +
            " AND test_number = ? ";

        PreparedStatement stmt=null;
        try {
            stmt = conn.prepareStatement(update);

            for (TestOutcome outcome : testOutcomes) {
                int index=1;
                stmt.setInt(index++, outcome.getPointValue());
                stmt.setInt(index++, outcome.getTestRunPK());
                stmt.setString(index++, outcome.getTestType().name());
                stmt.setString(index++, outcome.getTestNumber());
                stmt.addBatch();
            }

            stmt.executeBatch();
        } finally {
            Queries.closeStatement(stmt);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
	public int compareTo(TestOutcomeCollection that) {
    	if (this == that)
    		return 0;
        int result = getValuePassedOverall() - that.getValuePassedOverall();
        if (result != 0)
        	return result;
        return Objects.identityCompareTo(this, that); 
         
    }

	/**
     * Gets all of the test outcomes from the canonical run for a given project.
     * @param projectPK the projectPK
     * @param conn the connection to the database
     * @return a collection of test outcomes from the canonical run for a given project;
     * returns an empty collection if there is no valid canonical run for the project.
     */
    public static TestOutcomeCollection lookupCanonicalOutcomesByProjectPK(Integer projectPK, Connection conn)
    throws SQLException
    {
        String query =
            " SELECT " +TestOutcome.ATTRIBUTES+
            " FROM test_outcomes, projects, test_setups " +
            " WHERE projects.project_pk = ? " +
            " AND projects.test_setup_pk = test_setups.test_setup_pk " +
            " AND test_setups.test_run_pk = test_outcomes.test_run_pk ";

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(query);
            SqlUtilities.setInteger(stmt, 1, projectPK);
            return getAllFromPreparedStatement(stmt);
        } finally {
            Queries.closeStatement(stmt);
        }
    }

    /**
     * Returns the number of outcomes for each test type in the given array
     * @param outcomeArr
     * @return number of outcomes for each test type in the given array
     */
    private int countOutcomes(String[] outcomeArr)
    {
        int count=0;
        for (int ii=0; ii < outcomeArr.length; ii++)
        {
            count += countOutcomes(TestOutcome.asOutcomeType(outcomeArr[ii]));
        }
        return count;
    }

    /**
     * Get the number of tests that failed due to a fault (HUH, ERROR, FAILED) rather than
     * being unimplemented.
     * @return the number of tests that failed due to a fault (HUH, ERROR, FAILED).
     */
    public int countFaults()
    {
        return countOutcomes(new String[] {
                TestOutcome.HUH,
                TestOutcome.ERROR,
                TestOutcome.FAILED,
                TestOutcome.MISSING_COMPONENT,
                TestOutcome.TIMEOUT
        });
    }

    private CodeCoverageResults getCodeCoverageResultsOfGivenType(TestType... types)
    throws IOException
    {
    	CodeCoverageResults results = new CodeCoverageResults();
    	List<TestType> typesList = Arrays.asList(types);
    	for (TestOutcome outcome : testOutcomes) {
    		// Note that timeouts don't produce any coverage output
            // but still shouldn't cause everything else to fail!
            if (typesList.contains(outcome.getTestType()) && outcome.isCoverageType() && !outcome.getOutcome().equals(TestOutcome.TIMEOUT)) {
                //System.out.println("Adding test of type " +outcome.getTestType());
    			CodeCoverageResults currentCoverageResults = outcome.getCodeCoverageResults();
    			results.union(currentCoverageResults);
    		}
    	}
    	return results;
    }

    /**
     * Get the overall code coverage results for the public, release and secret tests.
     * @return CodeCoverageResults object representing the aggregate results of code coverage.
     * @throws IOException
     */
    public CodeCoverageResults getOverallCoverageResultsForCardinalTests()
    throws IOException
    {
    	return getCodeCoverageResultsOfGivenType(
    			TestOutcome.TestType.PUBLIC,
    			TestOutcome.TestType.RELEASE,
    			TestOutcome.TestType.SECRET);
    }

    public CodeCoverageResults getOverallCoverageResultsForPublicTests()
    throws IOException
    {
    	return getCodeCoverageResultsOfGivenType(TestOutcome.TestType.PUBLIC);
    }

    public CodeCoverageResults getOverallCoverageResultsForReleaseTests()
    throws IOException
    {
    	return getCodeCoverageResultsOfGivenType(TestOutcome.TestType.RELEASE);
    }

    public CodeCoverageResults getOverallCoverageResultsForSecretTests()
    throws IOException
    {
    	return getCodeCoverageResultsOfGivenType(TestOutcome.TestType.SECRET);
    }

    public CodeCoverageResults getOverallCoverageResultsForStudentTests()
    throws IOException
    {
    	return getCodeCoverageResultsOfGivenType(TestOutcome.TestType.STUDENT);
    }

    public CodeCoverageResults getOverallCoverageResultsForPublicAndStudentTests()
    throws IOException
    {
    	return getCodeCoverageResultsOfGivenType(TestOutcome.TestType.PUBLIC, TestOutcome.TestType.STUDENT);
    }
    
    public CodeCoverageResults getOverallCoverageResultsForAllTests()
    	    throws IOException
    	    {
    	    	return getCodeCoverageResultsOfGivenType(TestOutcome.TestType.PUBLIC, TestOutcome.TestType.STUDENT,
    	    			TestOutcome.TestType.RELEASE, TestOutcome.TestType.SECRET);
    	    }
    /**
     * Get the code coverage results divided up by packages.
     * @param packageNameList
     * @return A map of the package names to maps of the different "interesting" categories
     * 		mapped to their corresponding coverage results.
     * @throws IOException
     */
    public Map<String,Map<String,CodeCoverageResults>> getCoverageResultsByPackageMap(
    	Iterable<String> packageNameList)
    throws IOException
    {
    	Map<String,Map<String,CodeCoverageResults>> resultMap= new HashMap<String,Map<String,CodeCoverageResults>>();
    	// map from package-names to a map of "interesting" coverage results categories
    	// (i.e. public, student, public + student, etc) to their corresponding results
    	Map<String,CodeCoverageResults> coverageMap = getCoverageResultsMap();

    	for (String packageName : packageNameList) {
    		// This map will map code coverage category (public, student, etc) to the results
    		// filtered for the package name
    		Map<String,CodeCoverageResults> mapForPackageName = new HashMap<String,CodeCoverageResults>();
    		for (Entry<String,CodeCoverageResults> entry : coverageMap.entrySet()) {
    			CodeCoverageResults results = entry.getValue();
    			CodeCoverageResults resultsForPackage = results.getCodeCoverageResultsForPackage(packageName);
    			mapForPackageName.put(entry.getKey(), resultsForPackage);
    		}
    		resultMap.put(packageName, mapForPackageName);
    	}
    	return resultMap;
    }

    public List<TestOutcome> getUncoveredMethods()
    {
        return getOutcomesForTestType(TestOutcome.TestType.UNCOVERED_METHOD);
    }

    /**
     * Gets a map of the different "interesting" categories of coverage (public, student,
     * public + student, cardinal, public + student intersect cardinal) to their corresponding
     * coverage results.
     * TODO return null if coverage information is not available?
     * @return
     * @throws IOException
     */
    public Map<String,CodeCoverageResults> getCoverageResultsMap()
	throws IOException
	{
		Map<String,CodeCoverageResults> codeCoverageResultsMap = new HashMap<String,CodeCoverageResults>();

		CodeCoverageResults publicCoverageResults = getOverallCoverageResultsForPublicTests();
		//CodeCoverageResults releaseCoverageResults = getOverallCoverageResultsForReleaseTests();
		CodeCoverageResults studentCoverageResults = getOverallCoverageResultsForStudentTests();
		CodeCoverageResults cardinalCoverageResults = getOverallCoverageResultsForCardinalTests();

		CodeCoverageResults publicAndStudentCoverageResults = new CodeCoverageResults(publicCoverageResults);
		publicAndStudentCoverageResults.union(studentCoverageResults);

		CodeCoverageResults intersectionCoverageResults = new CodeCoverageResults(studentCoverageResults);
		intersectionCoverageResults.union(publicAndStudentCoverageResults);
		intersectionCoverageResults.intersect(cardinalCoverageResults);

		codeCoverageResultsMap.put("public",publicCoverageResults);
		codeCoverageResultsMap.put("student", studentCoverageResults);
		codeCoverageResultsMap.put("cardinal",cardinalCoverageResults);
		codeCoverageResultsMap.put("public_and_student",publicAndStudentCoverageResults);
		codeCoverageResultsMap.put("public_and_student_intersect_cardinal",intersectionCoverageResults);
		return codeCoverageResultsMap;
	}

    public List<TestOutcome> getTestOutcomesWithStackTraceAtLine(FileNameLineNumberPair pair)
    {
        return getTestOutcomesWithStackTraceAtLine(pair.getFileName(), pair.getLineNumber());
    }

    List<TestOutcome> getTestOutcomesWithStackTraceAtLine(String filename, int lineNumber)
    {
        List<TestOutcome> result = new LinkedList<TestOutcome>();
        for (Iterator<TestOutcome> ii=iterator(); ii.hasNext();) {
            TestOutcome outcome = ii.next();
            if (outcome.isStackTraceAtLineForFile(filename, lineNumber))
                result.add(outcome);
        }
        return result;
    }

    /**
     * Returns a collection of the testOutcomes that cover the given file at
     * the given line number.  This method (obviously) only returns public, release,
     * secret, and student-written tests.
     *
     * @param filename the name of the file
     * @param lineNumber the number of the line of the file
     * @return a testOutcomeCollection of the testOutcomes that cover the given source line of
     * the given source file.
     */
    public TestOutcomeCollection getTestOutcomesCoveringFileAtLine(String filename, int lineNumber)
    throws IOException
    {
    	TestOutcomeCollection results = new TestOutcomeCollection();
    	for (TestOutcome outcome : testOutcomes) {
    		if (outcome.isCoverageType() && outcome.coversFileAtLineNumber(filename, lineNumber)) {
    			results.add(outcome);
    		}
    	}
    	return results;
    }

    /**
     * Gets a list of cardinal tests (public, release or secret) that fail due to
     * the given (runtime) exception.
     * @param exceptionClassName The fully-qualified classname of the exception.
     * @return A List&lt;TestOutcome&gt; of test outcomes that fail due to the
     * given runtime exception.
     */
    public List<TestOutcome> getFailingCardinalOutcomesDueToException(@Nonnull String exceptionClassName)
    {
        List<TestOutcome> list=new LinkedList<TestOutcome>();
        for (TestOutcome outcome : getAllScoredOutcomes()) {
            if (outcome.isError() && exceptionClassName.equals(outcome.getExceptionClassName())) {
                list.add(outcome);
            }
        }
        return list;
    }

    /**
     * Gets the testOutcomes that cover the given findbugs warning.
     * @param findbugsWarning The findbugs warning
     * @return The collection of testOutcomes that cover the given FindBugs warning.
     * @throws DocumentException
     * @throws IOException
     */
    public TestOutcomeCollection getTestOutcomesCoveredByFindbugsWarning(
    		TestOutcome findbugsWarning)
    throws IOException
    {
        if (!findbugsWarning.isFindBugsWarning())
            throw new IllegalArgumentException("This method requires a FindBugs test outcome");

        FileNameLineNumberPair pair = findbugsWarning.getFileNameLineNumberPair();
        // If the given FindBugs warning doesn't contain any line numbers; return an empty collection
        if (pair == null || pair.getLineNumber() == -1)
            return new TestOutcomeCollection();
        return getTestOutcomesCoveringFileAtLine(pair.getFileName(), pair.getLineNumber());
    }

	public TestOutcome getOutcomeByTestTypeAndTestNumber(TestType testType, String testNumber)
	{
		for (TestOutcome outcome : testOutcomes) {
			if (outcome.getTestType().equals(testType) && outcome.getTestNumber().equals(testNumber))
				return outcome;
		}
		return null;
	}

    public static class TarantulaScore
    {
        public final double score;
        public final double intensity;
        /**
		 * @deprecated Use {@link #TarantulaScore(double,double)} instead
		 */
		public TarantulaScore(double score, double intensity, StringBuffer buf) {
			this(score, intensity);
		}
		public TarantulaScore(double score, double intensity) {
            this.score=score;
            this.intensity=intensity;
        }
    }

    public TarantulaScore getTarantulaScoreForFileAtLine(String filename, int lineNo)
    throws IOException
    {
//      FIXME: Should handle any selected test type, not only all cardinal test types
        StringBuffer buf=new StringBuffer();
        double score=-1.0;
        double intensity=-1.0;
        Iterable<TestOutcome> iterable= getIterableForCardinalTestTypes();

        boolean isLineExecutable=false;

        int totalTests=0;
        int passed=0;
        int totalPassed=0;
        int failed=0;
        int totalFailed=0;
        int countByOutcome=0;
        for (TestOutcome outcome : iterable) {
            totalTests++;
            FileWithCoverage coverageForGivenOutcome = outcome.getCodeCoverageResults().getFileWithCoverage(filename);

            countByOutcome = coverageForGivenOutcome.getStmtCoverageCount(lineNo);
            if (outcome.getOutcome().equals(TestOutcome.PASSED))
                totalPassed++;
            else
                totalFailed++;

            if (countByOutcome > 0) {
                isLineExecutable=true;
                if (outcome.getOutcome().equals(TestOutcome.PASSED)) {
                    // failed outcome
                    buf.append("<td class=\"passed\">"+countByOutcome+"</td>\n");
                    passed++;
                } else {
                    buf.append("<td class=\"failed\">"+countByOutcome+"</td>\n");
                    failed++;
                }
            } else if (countByOutcome < 0){
                buf.append("<td></td>\n");
            } else {
                buf.append("<td></td>\n");
            }
        }
        if (isLineExecutable) {
            double passedPct = totalPassed>0?(double)passed / (double)totalPassed:0.0;
            double failedPct = totalFailed>0?(double)failed / (double)totalFailed:0.0;

            intensity = Math.max(passedPct, failedPct);
            score=passedPct/(passedPct+failedPct);

//            System.out.println(fileWithCoverage.getShortFileName() +
//                ", lineNumber = "+lineNo+
//                ", executable? "+isLineExecutable+
//                ", countByOutcome = "+countByOutcome+
//                ", totalPassed = "+totalPassed+
//                ", totalFailed = "+totalFailed+
//                ", passed = " +passed+
//                ", failed = " +failed+
//                ", passedPct = "+passedPct+
//                ", failedPct = "+failedPct+
//                ", score = "+score);
        }
        return new TarantulaScore(score, intensity);
    }

    /**
     * Look up a list of all of the the TestOutcomeCollections with a given submissionPK and
     * testSetupPK.  The additional TestOutcomeCollections will be due to explicit retests
     * or background retests.
     * TODO invalidate test runs.
     * @param submissionPK
     * @param testSetupPK
     * @param conn
     * @return A list of all the TestOutcomeCollections for the given submissionPK
     *  and testSetupPK.
     * @throws SQLException
     */
    public static List<TestOutcomeCollection> lookupAllBySubmissionPKAndTestSetupPK(
    		@Submission.PK int submissionPK,
        Integer testSetupPK,
        Connection conn)
    throws SQLException
    {
        String query=
            " SELECT " +TestOutcome.ATTRIBUTES+ " " +
            " FROM test_outcomes, test_runs " +
            " WHERE submission_pk = ? " +
            " AND test_runs.test_setup_pk = ? " +
            " AND test_runs.test_run_pk = test_outcomes.test_run_pk " +
            " ORDER BY test_outcomes.test_run_pk ";
        PreparedStatement stmt=null;
        try {
            stmt=conn.prepareStatement(query);
            SqlUtilities.setInteger(stmt, 1, submissionPK);
            SqlUtilities.setInteger(stmt, 2, testSetupPK);
            ResultSet rs=stmt.executeQuery();
            // Could also use a map to store results
            List<TestOutcomeCollection> list=new LinkedList<TestOutcomeCollection>();
            TestOutcome previous=null;
            TestOutcomeCollection currentCollection=new TestOutcomeCollection();
            while (rs.next()) {
                TestOutcome outcome=new TestOutcome(rs, 1);
                // If these two outcomes have different testRunPK, then they represent
                // the boundary between different testOutcomeCollections.
                // So add the currentCollection the list of results, and start the next collection.
                if (previous!=null && previous.getTestRunPK() != outcome.getTestRunPK()) {
                    list.add(currentCollection);
                    currentCollection=new TestOutcomeCollection();
                }
                previous=outcome;
                currentCollection.add(outcome);
            }
            // Add outcomes in the last collection, if any.
            if (currentCollection.size() > 0)
                list.add(currentCollection);
            return list;
        } finally {
            Queries.closeStatement(stmt);
        }
    }

    public CodeCoverageResults getOverallCoverageResultsForAllPassingTests()
    {
        CodeCoverageResults results=new CodeCoverageResults();

        for (TestOutcome outcome : testOutcomes) {

            if (outcome.getTestType().isDynamic() && outcome.getOutcome().equals(TestOutcome.PASSED)) 
                results.union(outcome.getCodeCoverageResults());

        }
        return results;
    }

    public CodeCoverageResults getOverallCoverageResultsForReleaseUnique()
    throws IOException
    {
        CodeCoverageResults codeCoverageResults=getOverallCoverageResultsForReleaseTests();

        CodeCoverageResults nonReleaseResults=getOverallCoverageResultsForPublicAndStudentTests();
        nonReleaseResults.union(getOverallCoverageResultsForSecretTests());
        codeCoverageResults.excluding(nonReleaseResults);
        return codeCoverageResults;
    }

    public static boolean isApproximatelyCovered(TestOutcomeCollection collection,
        TestOutcome outcome)
    throws IOException
    {
        return collection.isExceptionSourceApproximatelyCovered(outcome, 3);
    }

    /**
     * Check if the coverage of a given release test that failed due to a run-time
     * exception has public/student tests that cover the source of the exception
     * or range lines before it
     * @param releaseTest
     * @param range the number of lines before the exception source that needs to be covered
     *  to constitute "approximate" coverage
     * @return True if the public/student tests approximately cover the source of the exception;
     *  false otherwise
     * @throws IOException
     */
    boolean isExceptionSourceApproximatelyCovered(TestOutcome releaseTest, int range)
    throws IOException
    {
        // Doesn't make sense to mix test outcomes from different test runs
        assert releaseTest.getTestRunPK() == testRunPK.intValue();
        return releaseTest.isExceptionSourceApproximatelyCoveredElsewhere(getOverallCoverageResultsForPublicAndStudentTests(), range);
    }

    public List<TestOutcome> getReleaseAndSecretOutcomes()
    {
        List<TestOutcome> releaseList=getReleaseOutcomes();
        List<TestOutcome> secretList=getSecretOutcomes();
        releaseList.addAll(secretList);
        return releaseList;
    }

    /**
     * Returns a list of all FindBugs outcomes (warnings) in this collection
     * that start with the given bug-code prefix.
     * @param warningPrefix Prefix of bug-code warning that result list of
     *  bug-codes must begin with.
     * @return List of all FindBugs outcomes (warnings) contained in this collection
     *  that start with the given bug-code prefix.
     */
    public List<TestOutcome> getFindBugsOutcomesWithWarningPrefix(String warningPrefix)
    {
        List<TestOutcome> list=new LinkedList<TestOutcome>();
        for (TestOutcome outcome : getFindBugsOutcomes()) {
            if (outcome.getTestName().startsWith(warningPrefix)) {
                list.add(outcome);
            }
        }
        return list;
    }


    /**
     * @param outcome
     * @return
     * @throws IOException
     */
    public  int getCoverageScore() throws IOException {
        CodeCoverageResults codeCoverageResults = 
                getOverallCoverageResultsForCardinalTests();
        CodeCoverageResults publicStudentCoverage = 
                getOverallCoverageResultsForPublicAndStudentTests();
        codeCoverageResults
                .excluding(publicStudentCoverage);
        return  codeCoverageResults.getOverallCoverageStats().getIntScore();
    }
}
