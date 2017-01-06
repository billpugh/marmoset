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
 * Created on Aug 24, 2004
 */
package edu.umd.cs.buildServer.tester;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;

import edu.umd.cs.buildServer.BuildServer;
import edu.umd.cs.buildServer.BuilderException;
import edu.umd.cs.buildServer.builder.Clover;
import edu.umd.cs.marmoset.modelClasses.TestOutcome;
import edu.umd.cs.marmoset.modelClasses.TestOutcome.TestType;
import edu.umd.cs.marmoset.modelClasses.TestOutcomeCollection;
import junit.framework.AssertionFailedError;
import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.runner.BaseTestRunner;

/**
 * Run some JUnit tests and record the outcomes.
 *
 * @author Bill Pugh
 * @author David Hovemeyer
 * @author Nat Ayewah
 */
public class TestRunner extends BaseTestRunner {
    /**
     * An input stream that does nothing but return EOF.
     */
    private static class DevNullInputStream extends InputStream {
        /*
         * (non-Javadoc)
         *
         * @see java.io.InputStream#read()
         */
        @Override
        public int read() throws IOException {
            return -1;
        }
    }

    private static final int DEFAULT_TEST_TIMEOUT_IN_SECONDS = 30;

    private TestType testType;
    private int testTimeoutInSeconds;
    private TestOutcomeCollection outcomeCollection;
    /**
     * If nonnull, the named test method will be the only test case executed.
     */
    private String testMethod;
    /** Where to start numbering recorded test outcomes. */
    private int nextTestNumber;

    // Transient state
    private Class<?> suiteClass;
    private TestOutcome currentTestOutcome;
    private long currentTestStarted;
    private static Logger log;

    private static Logger getLog() {
        if (log == null) {
            log = Logger.getLogger(BuildServer.class);
        }
        return log;
    }

    /**
     * Constructor
     * 
     * @param testType
     *            type of test being performed
     */
    public TestRunner(TestType testType, int testTimeoutInSeconds) {
        this.testType = testType;
        this.testTimeoutInSeconds = testTimeoutInSeconds;
        this.outcomeCollection = new TestOutcomeCollection();

        this.nextTestNumber = TestOutcome.FIRST_TEST_NUMBER;
        this.currentTestOutcome = null;
    }

    /**
     * Set the single test method to execute. By default, all test methods in
     * the test suite class will be executed.
     *
     * @param testMethod
     *            the name of the single test method to execute
     */
    public void setTestMethod(String testMethod) {
        this.testMethod = testMethod;
    }

    /**
     * Set number of first test case to be recorded.
     *
     * @param nextTestNumber
     */
    public void setNextTestNumber(int nextTestNumber) {
        this.nextTestNumber = nextTestNumber;
    }

    /**
     * Get Collection containing all TestOutcomes.
     *
     * @return the Collection of TestOutcomes
     */
    public Collection<TestOutcome> getTestOutcomes() {
        return outcomeCollection.getAllOutcomes();
    }

    @Override
    public void testStarted(String testName) {
        // Create a new (incomplete) TestOutcome to represent
        // the outcome of this test.

        currentTestOutcome = new TestOutcome();
        currentTestOutcome.setTestType(testType);
        currentTestOutcome.setTestName(testName);
        currentTestOutcome.setTestNumber(Integer.toString(nextTestNumber++));
        currentTestStarted = System.currentTimeMillis();
    }

    @Override
    public void testEnded(String testName) {
        if (currentTestOutcome.getOutcome() == null) {

            // The test didn't fail, so it must have succeeded.
            currentTestOutcome.setOutcome(TestOutcome.PASSED);
            currentTestOutcome.setShortTestResult("PASSED");
            currentTestOutcome.setLongTestResult("");
            // since this didn't fail, these can be empty
            currentTestOutcome.setExceptionClassName("");
            currentTestOutcome.setDetails(null);
            currentTestOutcome.setExecutionTimeMillis(System.currentTimeMillis() - currentTestStarted);
        }
        outcomeCollection.add(currentTestOutcome);
    }

    @Override
    public void testFailed(int status, Test test, Throwable t) {

        Throwable original = t;

        Throwable cause = t.getCause();
        while (cause != null) {
            t = cause;
            cause = t.getCause();
        }

        // determine finer-grained cause of failure
        if (notYetImplemented(t)) {
            currentTestOutcome.setOutcome(TestOutcome.NOT_IMPLEMENTED);
        } else if (t instanceof TestTimeoutError) {
            currentTestOutcome.setOutcome(TestOutcome.TIMEOUT);
        } else if (t instanceof SecurityException) {
            currentTestOutcome.setOutcome(TestOutcome.HUH);
        } else if (t instanceof NoClassDefFoundError || t instanceof ClassNotFoundException
                || t instanceof NoSuchFieldError || t instanceof NoSuchFieldException || t instanceof NoSuchMethodError
                || t instanceof NoSuchMethodException) {
            currentTestOutcome.setOutcome(TestOutcome.MISSING_COMPONENT);
        } else if (t instanceof AssertionFailedError) {
            currentTestOutcome.setOutcome(TestOutcome.FAILED);
        } else if (isThrownFromTestCode(t)) {
            // We assume that any exception thrown from test code
            // is the student's fault. E.g., a method which was
            // supposed to return a non-null value returned null,
            // and the test code dereferenced it.
            currentTestOutcome.setOutcome(TestOutcome.FAILED);
        } else {
            currentTestOutcome.setOutcome(TestOutcome.ERROR);
        }
        currentTestOutcome.setExecutionTimeMillis(System.currentTimeMillis() - currentTestStarted);
        currentTestOutcome.setShortTestResult(t.toString() + formatShortExceptionMessage(t));
        currentTestOutcome.setLongTestResult(toString(original));
        String exceptionName = t.getClass().getName();
        currentTestOutcome.setExceptionClassName(exceptionName);
    }

    /**
     * Return whether or not the given exception was thrown from test code.
     *
     * @param t
     *            the exception
     * @return true if the exception was thrown from test code, false otherwise
     */
    private boolean isThrownFromTestCode(Throwable t) {
        StackTraceElement[] trace = t.getStackTrace();

        if (trace.length < 1)
            return false;

        return trace[0].getClassName().contains(suiteClass.getName());
    }

    /**
     * Checks if the functionality this test case exercises has not been
     * implemented. This allows us to distinguish between a method throwing
     * UnsupportedOperationException because it hasn't been implemented from a
     * test cause that fails because of another type of exception (such as
     * AssertionFailedException).
     *
     * @param t
     *            the throwable
     * @return true if this test case failed because the necessary functionality
     *         has not yet been implemented; false otherwise
     */
    private static boolean notYetImplemented(Throwable t) {
        if (t instanceof UnsupportedOperationException || t instanceof NoSuchMethodException
                || t instanceof ClassNotFoundException)
            return true;
        if (t.getCause() instanceof UnsupportedOperationException)
            return true;
        return false;
    }

    /**
     * Format exception to describe (briefly) where the exception occurred.
     *
     * @param t
     *            the exception
     * @return where the exception occurred
     */
    private static String formatShortExceptionMessage(Throwable t) {
        StackTraceElement[] trace = t.getStackTrace();
        if (trace.length == 0)
            return " at unknown source line";
        else
            return " at " + trace[0].toString() + "...";
    }

    /**
     * Format an exception object to store in the long_test_result field of the
     * test_outcomes table.
     *
     * @param t
     *            the exception
     * @return the long description string for the exception
     */
    public static String toString(Throwable t) {
        StringWriter out = new StringWriter();
        PrintWriter pw = new PrintWriter(out);
        t.printStackTrace(pw);
        pw.close();
        return out.toString();
    }

    @Override
    protected void runFailed(String message) {
        getLog().debug("Run failed: " + message);
    }

    /**
     * Execute a single test specified by testMethod in its own thread. Kill
     * threads for tests that exceed the timeout value
     */
    public void runTests(String testClassName) throws BuilderException {
        Thread t = new Thread("suicideThread") {
            @Override
            public void run() {
                try {
                    Thread.sleep(testTimeoutInSeconds * 1000 + 15 * 60 * 1000);
                    System.exit(1);
                } catch (InterruptedException ignore) {
                    // ignore
                }
            }
        };
        t.setDaemon(true);
        t.start();

        getBuildServerLog().trace("Running tests for class " + testClassName);

        // Return a TestSuite with a single test
        Test suite = getTest(testClassName);
        if (suite == null) {
            getBuildServerLog().fatal("Could not load test " + testClassName);
            throw new BuilderException("Could not load test " + testClassName);
        }
        TestResult result = new TestResult();
        result.addListener(this);

        InputStream sysIn = System.in;
        PrintStream sysOut = System.out;
        PrintStream sysErr = System.err;
        try {
            suite.run(result);
        } finally {
            System.out.flush();
            System.err.flush();
            setSystemInOutAndErr(sysIn, sysOut, sysErr);
        }
    }

    private static void setSystemInOutAndErr(final InputStream input, final PrintStream oStream,
            final PrintStream err) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                System.setIn(input);
                System.setOut(oStream);
                System.setErr(err);
                return null;
            }
        });
    }

    /**
     * Get a single test derived from combination of testClassName and
     * {@link #testMethod}. If the test class extends TestCase, then this is
     * treated as a JUnit 3 test (use TestSuite.createTest), otherwise this is a
     * JUnit 4 test (use JUnit4TestAdapter).
     *
     * @see junit.runner.BaseTestRunner#getTest(java.lang.String)
     */
    @Override
    public Test getTest(String testClassName) {

        // -- try JUnit 3 approach
        try {
            Class<?> suiteClass = loadSuiteClass(testClassName);

            // As a side-effect, store the test suite Class
            this.suiteClass = suiteClass;

            TestSuite ts = new TestSuite();
            ts.addTest(TestSuite.createTest(suiteClass, testMethod));
            return ts;
        } catch (ClassCastException e) {
            // not a TestCase
        } catch (ClassNotFoundException e) {
            // not a TestCase
        }

        // -- try JUnit 4 approach
        try {
            Class<?> suiteClass = Class.forName(testClassName);

            Test returnTest = filterAdapter(new JUnit4TestAdapter(suiteClass), testMethod);

            // add adapter to test suite, so it can be run
            TestSuite ts = new TestSuite();
            ts.addTest(returnTest);
            return ts;

        } catch (ClassNotFoundException e) {
            runFailed("Could not load test class " + testClassName + ": " + e.toString());
            return null;
        }

    }

    /**
     * If the methodName is not null, try to filter adapter to test only the
     * specified test
     */
    private Test filterAdapter(JUnit4TestAdapter adapter, final String methodName) {

        // if testMethod is not null, create a filter to select the method
        if (methodName != null) {
            Filter f = new Filter() {
                @Override
                public String describe() {
                    return "filter runs " + methodName;
                }

                @Override
                public boolean shouldRun(Description description) {
                    if (description.isSuite()) {
                        for (Description child : description.getChildren()) {
                            if (shouldRun(child))
                                return true;
                        }
                    } else {
                        String displayName = description.getDisplayName();
                        if (displayName.startsWith(methodName + "("))
                            return true;
                    }
                    return false;
                }
            };
            try {
                adapter.filter(f);
                return adapter;
            } catch (NoTestsRemainException e) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter writer = new PrintWriter(stringWriter);
                e.printStackTrace(writer);

                return TestSuite.warning("Cannot find test: " + methodName + " (" + stringWriter.toString() + ")");
            }
        } else {
            // fail if methodName is null
            return TestSuite.warning("Cannot have a null method name for class: " + adapter);
        }
    }

    private static Logger buildServerLog;

    private static Logger getBuildServerLog() {
        if (buildServerLog == null) {
            buildServerLog = Logger.getLogger("edu.umd.cs.buildServer.BuildServer");
        }
        return buildServerLog;
    }

    public static void main(String[] args) {

        int startTestNumber = -1;

        int argCount = 0;
        while (argCount < args.length) {
            String opt = args[argCount];
            if (!opt.startsWith("-"))
                break;
            if (opt.equals("-startTestNumber")) {
                ++argCount;
                if (argCount >= args.length)
                    throw new IllegalArgumentException("-startTestNumber option requires argument");
                startTestNumber = Integer.parseInt(args[argCount]);
            } else {
                throw new IllegalArgumentException("Unknown option " + opt);
            }

            ++argCount;
        }
        if (argCount > 0) {
            String[] remainingArgs = new String[args.length - argCount];
            System.arraycopy(args, argCount, remainingArgs, 0, remainingArgs.length);
            args = remainingArgs;
        }

        if (args.length < 4 || args.length > 6) {
            getBuildServerLog().fatal("Usage: " + TestRunner.class.getName()
                    + " [options] <submission_pk> <test_type> <test classname> <output file> "
                    + "[<test timeout in seconds>] [<test method>]");
            getBuildServerLog().fatal("Options:");
            getBuildServerLog().fatal("-startTestNumber <n>   Start numbering test outcomes at <n>");
            System.exit(1);
        }

        String submissionPK = args[0];
        TestType testType = TestType.valueOfAnyCase(args[1]);
        String testClassname = args[2];
        String outputFile = args[3];

        int testTimeoutInSeconds = DEFAULT_TEST_TIMEOUT_IN_SECONDS;
        if (args.length >= 5) {
            // The JavaTester will pass -1 if the test timeout was not
            // explicitly
            // specified.
            int argVal = Integer.parseInt(args[4]);
            if (argVal > 0)
                testTimeoutInSeconds = argVal;
        }

        String testMethod = null;
        if (args.length >= 6) {
            testMethod = args[5];
        }

        // Redirect reads from System.in so that they always return EOF
        System.setIn(new DevNullInputStream());

        // Execute the tests
        TestRunner r = new TestRunner(testType, testTimeoutInSeconds);
        if (testMethod != null) {
            r.setTestMethod(testMethod);
        }
        if (startTestNumber >= 0) {
            r.setNextTestNumber(startTestNumber);
        }

        // Save test results to a file
        try {
            r.runTests(testClassname);

            ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));

            Collection<TestOutcome> testOutcomes = r.getTestOutcomes();

            out.writeObject(testOutcomes);
            out.close();

            // If we are using Clover, flush output before we call
            // System.exit(0);
            Clover.globalFlush();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Shutdown the process.
            // There may be non-daemon threads running which would
            // keep the process alive if we just fell off the
            // end of main().
            // System.exit(0);
        } catch (BuilderException e) {
            getBuildServerLog().fatal("runTests() failed", e);
            Clover.globalFlush();
            System.exit(1);
        } catch (IOException e) {
            getBuildServerLog().fatal("TestRunner raised an IOException", e);
            Clover.globalFlush();
            System.exit(1);
        } catch (LinkageError e) {
            getBuildServerLog().fatal("TestRunner raised a LinkageError", e);

            e.printStackTrace();
            Clover.globalFlush();
            System.exit(2);
        }
    }
}
