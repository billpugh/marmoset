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
package edu.umd.cs.buildServer.tester;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.EnumMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import edu.umd.cs.buildServer.BuilderException;
import edu.umd.cs.buildServer.ProjectSubmission;
import edu.umd.cs.buildServer.builder.DirectoryFinder;
import edu.umd.cs.buildServer.util.DevNullInputStream;
import edu.umd.cs.buildServer.util.DevNullOutputStream;
import edu.umd.cs.buildServer.util.LoggingWriter;
import edu.umd.cs.buildServer.util.ProcessExitMonitor;
import edu.umd.cs.buildServer.util.Untrusted;
import edu.umd.cs.diffText.StringListWriter;
import edu.umd.cs.diffText.TextDiff;
import edu.umd.cs.diffText.TextDiff.Option;
import edu.umd.cs.marmoset.modelClasses.ExecutableTestCase;
import edu.umd.cs.marmoset.modelClasses.ExecutableTestCase.ExpectedKind;
import edu.umd.cs.marmoset.modelClasses.ScriptTestProperties;
import edu.umd.cs.marmoset.modelClasses.TestOutcome;

/**
 * Tester for C, OCaml and Ruby submissions.
 * <p>
 * <b>NOTE:</b> "CTester" is a legacy name. We use the same infrastructure for
 * building and testing C, OCaml and Ruby code because the process is exactly
 * the same. For more details see {@see CBuilder}.
 * 
 * @author David Hovemeyer
 * @author jspacco
 */
public class CTester extends Tester<ScriptTestProperties> {

    /**
     * Constructor.
     * 
     * @param testProperties
     *            TestProperties loaded from the project jarfile's
     *            test.properties
     * @param haveSecurityPolicyFile
     *            true if there is a security.policy file in the project jarfile
     * @param projectSubmission
     *            the ProjectSubmission
     * @param directoryFinder
     *            DirectoryFinder to locate build and testfiles directories
     */
    public CTester(
            ScriptTestProperties testProperties,
            ProjectSubmission<? extends ScriptTestProperties> projectSubmission,
            DirectoryFinder directoryFinder) {
        super(testProperties, projectSubmission, directoryFinder);
    }

    @Override
    protected void loadTestProperties() throws BuilderException {
        super.loadTestProperties();
    }

    @Override
    protected void executeTests() throws BuilderException {
        loadTestProperties();
        for (ExecutableTestCase e : getTestProperties()
                .getExecutableTestCases()) {
            getTestOutcomeCollection().add(executeTest(e));
        }

        testsCompleted();
    }

    ExecutorService executor = Executors
            .newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true)
                    .build());

    static class WhoClosedMeInputStream extends FilterInputStream {

        protected WhoClosedMeInputStream(InputStream arg0) {
            super(arg0);
        }

        @Override
        public void close() throws IOException {
            new RuntimeException("input stream closed at").printStackTrace();
            super.close();
        }

    }

    private TestOutcome executeTest(ExecutableTestCase testCase) {
        
        TestOutcome testOutcome = new TestOutcome();
        testOutcome.setTestNumber(Integer.toString(testCase.getNumber()));
        testOutcome.setTestName(testCase.getName());
        testOutcome.setTestType(testCase.getTestType());

        try {
            // Record a test outcome.
            int testTimeoutInSeconds = getTestProperties()
                    .getTestTimeoutInSeconds();

            getLog().debug("Running test " + testCase.getName());
            long processTimeoutMillis = testTimeoutInSeconds * 1000L;

            String[] exec = getExecLine(testCase,
                    ExecutableTestCase.Property.EXEC);
            File buildDirectory = getDirectoryFinder().getBuildDirectory();
            String options = testCase
                    .getProperty(ExecutableTestCase.Property.OPTIONS);
            EnumMap<Option,String> optionsForDiffing = new EnumMap<Option, String>(Option.class);
            if (options != null) {
                for (String oName : options.split("[\\s,]+")) {
                    int colon = oName.indexOf(':');
                    String value = null;
                    if (colon != -1) {
                        value = oName.substring(colon+1);
                        oName = oName.substring(0, colon);
                    }
                        
                    Option o = Option.valueOfAnyCase(oName);
                    optionsForDiffing.put(o, value);
                }

            }

            InputStream in;
            String inputProperty = testCase.getProperty(ExecutableTestCase.Property.INPUT);
            switch (testCase.getInputKind()) {
            case NONE:
                in = new DevNullInputStream();
                break;
            case FILE:
                boolean optional = false;
                if (inputProperty.endsWith("?")) {
                    optional = true;
                    inputProperty = inputProperty.substring(0, inputProperty.length()-1);
                }
                File f = new File(buildDirectory,
                        inputProperty);
                if (!readableFile(f)) {
                    if (optional) {
                        getLog().debug("Optional input file " + f + " not present, using /dev/null for input");
                        in = new DevNullInputStream();
                        break;
                    }
                    throw new IllegalStateException("No input file " + f);
                }

                in = new FileInputStream(f);
                break;
            case STRING:
                String input = inputProperty + "\n";
                in = new ByteArrayInputStream(input.getBytes());
                break;
            default:
                throw new AssertionError();
            }

            ExecutableTestCase.ExpectedKind outputKind = testCase.getOutputKind();
            TextDiff output = null;
            long startTime = System.currentTimeMillis();
            if (outputKind == ExpectedKind.NONE) {
                output = null;
            } else {
                TextDiff.Builder builder = TextDiff
                        .withOptions(optionsForDiffing);
                switch (outputKind) {
                case STRING:
                    String waitFor = optionsForDiffing.get(Option.WAIT_FOR);
                    if (waitFor != null)
                        builder.expect(waitFor);
                    builder.expect(testCase
                            .getProperty(ExecutableTestCase.Property.EXPECTED));
                    break;
                case FILE:
                    File f = new File(
                            buildDirectory,
                            testCase.getProperty(ExecutableTestCase.Property.EXPECTED));
                    if (!readableFile(f))
                        throw new IllegalStateException("No file for expected output" + f);
                    getLog().debug("Expecting output in file " + f);
                    
                    builder.expect(f);
                    break;
                case REFERENCE_IMPL:
                    String[] refExec = getExecLine(testCase,
                            ExecutableTestCase.Property.REFERENCE_EXEC);
                    Process refProcess = Untrusted.execute(buildDirectory,
                            refExec);
                    FutureTask<Void> copyInput = TextDiff.copyTask(
                            "copy input", in, refProcess.getOutputStream());
                    StringListWriter expectedOutput = new StringListWriter();
                    FutureTask<Void> saveOutput = TextDiff.copyTask(
                            "capture expected output",
                            refProcess.getInputStream(), expectedOutput,
                            getTestProperties().getMaxDrainOutputInBytes());
                    FutureTask<Void> drainErr = TextDiff.copyTask(
                            "drain expected err", refProcess.getErrorStream(),
                            new DevNullOutputStream());

                    executor.submit(copyInput);
                    executor.submit(saveOutput);
                    executor.submit(drainErr);
                    ProcessExitMonitor referenceExitMonitor = new ProcessExitMonitor(
                            refProcess, getLog(), startTime);
                    boolean done = referenceExitMonitor
                            .waitForProcessToExit(processTimeoutMillis);
                    if (!done)
                        throw new BuilderException(
                                "Capture of reference output for "
                                        + testCase.getName() + " timed out");
                    saveOutput.get();
                    copyInput.cancel(true);
                    drainErr.cancel(true);

                    builder.expect(expectedOutput.getStrings());

                }

                output = builder.build();

            }

            long started = System.currentTimeMillis();
            Process process;
            if (output == null)
                process = Untrusted
                        .executeCombiningOutput(buildDirectory, exec);
            else
                process = Untrusted.execute(buildDirectory, exec);

            FutureTask<Void> copyInput = TextDiff.copyTask("copy input", in,
                    process.getOutputStream());
            FutureTask<Void> checkOutput = null;

            StringWriter err = new StringWriter();

            Writer captureError = err;
            if (false) 
                captureError = new OutputStreamWriter(new LoggingWriter(new PrintWriter(err), getLog()));
            if (output != null)
                checkOutput = output.check((process.getInputStream()));

            FutureTask<Void> copyError;
            copyError = TextDiff.copyTask(
                    "copy error",
                    new InputStreamReader(output != null ? process
                            .getErrorStream() : process.getInputStream()), captureError,
                    getTestProperties().getMaxDrainOutputInBytes());
          

            executor.submit(copyInput);
            executor.submit(copyError);
            if (checkOutput != null)
                executor.submit(checkOutput);

            ProcessExitMonitor exitMonitor = new ProcessExitMonitor(process,
                    getLog(), startTime);

            boolean done = exitMonitor
                    .waitForProcessToExit(processTimeoutMillis);
            testOutcome.setExecutionTimeMillis(System.currentTimeMillis()
                    - started);

            boolean outcomeSet = false;

            if (!done) {
                outcomeSet = true;
                getLog().debug("Test timed out");
                // didn't terminate
                int processCount = exitMonitor.getProcessCount();
                if (processCount > 100) {
                	testOutcome.setOutcome(TestOutcome.FORK_BOMB);
                    testOutcome.setShortTestResult("Fork bomb:  "
                	+ processCount + " processes killed after "
                            + TimeUnit.SECONDS.convert(
                                    testOutcome.getExecutionTimeMillis(),
                                    TimeUnit.MILLISECONDS) + " secs");
                } else {
                testOutcome.setOutcome(TestOutcome.TIMEOUT);
                testOutcome.setShortTestResult("Timed out at "
                        + TimeUnit.SECONDS.convert(
                                testOutcome.getExecutionTimeMillis(),
                                TimeUnit.MILLISECONDS) + " secs");
                }
            } else if (checkOutput != null)
                try {
                    checkOutput.get(50, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    checkOutput.cancel(true);
                    if (done)
                        throw new AssertionError("done but output not ready");
                } catch (InterruptedException e) {
                    checkOutput.cancel(true);
                    if (done)
                        throw new AssertionError("done but output not ready");
                } catch (ExecutionException e) {
                    Throwable t = e.getCause();
                    if (t instanceof AssertionError) {
                        outcomeSet = true;
                        getLog().debug(
                                "Test didn't generate expected output: "
                                        + testOutcome.getShortTestResult());
                        testOutcome.setOutcome(TestOutcome.FAILED);
                        String msg = t.getMessage();
                        int i = msg.indexOf(" at junit.framework.Assert.fail(");
                        if (i >= 0)
                            msg = msg.substring(0, i);
                        testOutcome.setShortTestResult(msg);
                    }
                }

            err.flush();
            copyInput.cancel(true);
            copyError.cancel(true);
            err.flush();

            String errOutput = err.toString();
            if (errOutput.length() > 1000) {
                getLog().debug(
                        "Error output length is "
                                + errOutput.length()
                                + ", max drain is "
                                + getTestProperties()
                                        .getMaxDrainOutputInBytes());
            }
            testOutcome.setLongTestResultCompressIfNeeded(errOutput);
            if (testOutcome.isLongTestResultCompressed())
                getLog().debug("testOutcome.longTestResults is compressed");

            if (!outcomeSet) {
                int exitCode = exitMonitor.getExitCode();
                getLog().debug("Process exited with exit code: " + exitCode);
                if (exitCode == 0) {
                    testOutcome.setOutcome(TestOutcome.PASSED);
                    testOutcome.setLongTestResult("");
                } else {
                    testOutcome.setOutcome(TestOutcome.ERROR);
                    testOutcome.setShortTestResult("Test failed");
                }
            }

        } catch (Throwable t) {
            getLog().error("CTester error", t);
            testOutcome.setOutcome(TestOutcome.ERROR);
            testOutcome.setShortTestResult("Build server failure");
            testOutcome.setLongTestResultCompressIfNeeded(TestRunner
                    .toString(t));
        }

        if (testOutcome.getOutcome().equals(TestOutcome.PASSED))
            getLog().info(testOutcome.getOutcome());
        else
            getLog().info(
                    testOutcome.getOutcome() + " : "
                            + testOutcome.getShortTestResult());

        return testOutcome;
    }

    /**
     * @param testCase
     * @return
     */
    public String[] getExecLine(ExecutableTestCase testCase,
            ExecutableTestCase.Property property) {
        String exec[] = testCase.getStrings(property);

        exec[0] = checkTestExe(exec[0]);
        return exec;
    }

    private static boolean readableFile(File f) {
        return f.exists() && f.canRead() && !f.isDirectory();
    }

    /**
     * Check if a test executable really exists in the build directory. Right
     * now we just emit log messages if it doesn't.
     * 
     * @param exeName
     *            name of the test executable.
     */
    private String checkTestExe(String exeName) {
        File buildDirectory = getDirectoryFinder().getBuildDirectory();
        File exeFile = new File(buildDirectory, exeName);
        try {
            if (readableFile(exeFile)
                    && exeFile.getCanonicalPath().startsWith(
                            buildDirectory.getCanonicalPath())) {
                // OK, this looks like an executable
                if (!exeFile.canExecute())
                    exeFile.setExecutable(true);
                return "./" + exeName;
            }
        } catch (IOException e) {
            getLog().warn(
                    "Could not check executable " + exeFile
                            + " in build directory " + buildDirectory);
        }

        return exeName;

    }
}
