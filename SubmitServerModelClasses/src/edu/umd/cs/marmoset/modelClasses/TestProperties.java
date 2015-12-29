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

import static edu.umd.cs.marmoset.modelClasses.TestPropertyKeys.ADDITIONAL_SOURCE_FILE_EXTENSIONS;
import static edu.umd.cs.marmoset.modelClasses.TestPropertyKeys.DEFAULT_MAX_DRAIN_OUTPUT_IN_BYTES;
import static edu.umd.cs.marmoset.modelClasses.TestPropertyKeys.DEFAULT_PROCESS_TIMEOUT;
import static edu.umd.cs.marmoset.modelClasses.TestPropertyKeys.JAVA;
import static edu.umd.cs.marmoset.modelClasses.TestPropertyKeys.MAX_DRAIN_OUTPUT_IN_BYTES;
import static edu.umd.cs.marmoset.modelClasses.TestPropertyKeys.TEST_TIMEOUT;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.CheckForNull;

import edu.umd.cs.marmoset.modelClasses.TestOutcome.TestType;
import edu.umd.cs.marmoset.utilities.FileNames;

/**
 * Test properties: loaded from the test.properties file in the project jarfile.
 * 
 * @author David Hovemeyer
 * @author Jaime Spacco
 */
public abstract class TestProperties {

    public enum Framework {
        JUNIT, MAKE, SCRIPT;
        static Framework get(String v) {
            v = v.trim().toLowerCase();
            if (v.equals("java") || v.equals("junit"))
                return JUNIT;
            if (v.equals("script"))
                return SCRIPT;
            return MAKE;
        }
    }

    protected final Framework framework;
    protected final Properties testProperties;
   
    // Shared
    private int testTimeoutInSeconds;
    private int buildTimeoutInSeconds;
    protected Set<String> requiredFiles;

    private int maxDrainOutputInBytes;
    private String additionalSourceFileExtensions;

    public abstract EnumSet<TestType> getDynamicTestKinds();
    
    public String getAdditionalSourceFileExtensions() {
        return additionalSourceFileExtensions;
    }

    protected void setAdditionalSourceFileExtensions(
            String additionalSourceFileExtensions) {
        this.additionalSourceFileExtensions = additionalSourceFileExtensions;
    }

    public static TestProperties load(ZipInputStream zipIn) throws IOException,
            MissingRequiredTestPropertyException {
        try {
            while (true) {
                ZipEntry entry = zipIn.getNextEntry();
                if (entry == null)
                    throw new IllegalArgumentException("No test properties");
                if (entry.getName().equals("test.properties")
                        || entry.getName().endsWith("/test.properties"))
                    return initializeTestProperties(zipIn);

            }
        } finally {
            zipIn.close();
        }
    }

    /**
     * Load test properties from a file.
     * 
     * @param file
     *            file storing test properties
     * @throws IOException
     * @throws FileNotFoundException
     * @throws MissingRequiredTestPropertyException
     */
    public static TestProperties load(File file) throws FileNotFoundException,
            IOException, MissingRequiredTestPropertyException {
        return initializeTestProperties(new BufferedInputStream(
                new FileInputStream(file)));
    }

    /**
     * Initialize the test properties, setting any mandatory test properties.
     * Throws an exception in mandatory test properties are missing.
     * 
     * @param is
     *            The input stream storing the test properties.
     * @throws IOException
     * @throws MissingRequiredTestPropertyException
     *             If any mandatory test properties are missing.
     */
    private static TestProperties initializeTestProperties(InputStream is)
            throws IOException, MissingRequiredTestPropertyException {
        Properties testProperties = new Properties();

        testProperties.load(is);
        
        String f = null;
        for(String p : new String[] { "test.framework", "build.language", "test.language", "build.framework"}) {
            f = testProperties.getProperty(p);
            if (f != null)
                break;
        }
        
        if (f == null)
            f = "junit";

        Framework framework = Framework.get(f);

        switch (framework) {
        case JUNIT:
            return new JUnitTestProperties(testProperties);
        case SCRIPT:
            return new ScriptTestProperties(testProperties);
        case MAKE:
            return new MakeTestProperties(testProperties);
        default:
            throw new AssertionError();
        }

    }

    protected TestProperties(Framework framework, Properties testProperties) {
        this.framework = framework;
        this.testProperties = testProperties;

        setMaxDrainOutputInBytes(getOptionalIntegerProperty(
                MAX_DRAIN_OUTPUT_IN_BYTES, DEFAULT_MAX_DRAIN_OUTPUT_IN_BYTES));
        setAdditionalSourceFileExtensions(getOptionalStringProperty(ADDITIONAL_SOURCE_FILE_EXTENSIONS));
        int timeout = getOptionalIntegerProperty(TEST_TIMEOUT,
                DEFAULT_PROCESS_TIMEOUT);
        int buildTimeout = getOptionalIntegerProperty(TestPropertyKeys.BUILD_TIMEOUT,
                timeout);
        setTestTimeoutInSeconds(timeout);
        setBuildTimeoutInSeconds(buildTimeout);
        setRequiredFiles(getOptionalStringProperty(TestPropertyKeys.REQUIRED_FILES));
    }

    protected void setProperty(String name, String value) {
        if (value != null)
            testProperties.setProperty(name, value);
        else
            testProperties.remove(name);
    }

    protected void setRequiredFiles(@CheckForNull String requiredFiles) {
        if (requiredFiles == null) {
            this.requiredFiles = Collections.emptySet();
            return;
        }
        this.requiredFiles = new HashSet<String>();
        for(String file : requiredFiles.split(",")) {
            this.requiredFiles.add(file.trim());
        }
    }
    
    public Set<String> getRequiredFiles() {
        if (this.requiredFiles.isEmpty())
            return Collections.emptySet();
        return new HashSet<String>(this.requiredFiles);
    }
    public boolean isJava() {
        return getLanguage().equalsIgnoreCase(JAVA);
    }

    public boolean isMakefileBased() {
        return !isJava();
    }

    public boolean isPerformCodeCoverage() {
        return false;
    }
    
    public Collection<? extends String> getPropertyNames() {
        return testProperties.stringPropertyNames();
    }
    /**
     * Get the project source language.
     * 
     * @return the project source language: "java", "c", etc.
     */
    public String getLanguage() {
        switch (framework) {
        case JUNIT:
            return "java";
        case MAKE:
            return "c";
        case SCRIPT:
            return "script";
        default:
            throw new AssertionError();
        }

    }

    public Framework getFramework() {
        return framework;
    }

    public int getTestTimeoutInSeconds() {
        return testTimeoutInSeconds;
    }

    public int getBuildTimeoutInSeconds() {
        return buildTimeoutInSeconds;
    }

    public void setBuildTimeoutInSeconds(int buildTimeout) {
        this.buildTimeoutInSeconds = buildTimeout;
        setProperty(TEST_TIMEOUT[0],
                Integer.toString(buildTimeout));
    }
    public void setTestTimeoutInSeconds(int testTimeout) {
        this.testTimeoutInSeconds = testTimeout;
        setProperty(TestPropertyKeys.BUILD_TIMEOUT[0],
                Integer.toString(this.testTimeoutInSeconds));
    }

    public int getMaxDrainOutputInBytes() {
        return maxDrainOutputInBytes;
    }

    protected void setMaxDrainOutputInBytes(int maxDrainOutputInBytes) {
        this.maxDrainOutputInBytes = maxDrainOutputInBytes;
        setProperty(MAX_DRAIN_OUTPUT_IN_BYTES[0],
                Integer.toString(this.maxDrainOutputInBytes));
    }

    public boolean isSourceFile(String name) {
        String simpleName = FileNames.trimSourceFileName(name);
        int lastDot = simpleName.lastIndexOf('.');
        if (lastDot > 0 && additionalSourceFileExtensions != null) {
            String suffix = simpleName.substring(lastDot + 1);
            String allowed[] = additionalSourceFileExtensions.split(" ,");
            if (Arrays.asList(allowed).contains(suffix))
                return true;
        }
        return false;
    }

    /**
     * Get an integer property from the loaded test properties.
     * 
     * @param property
     *            the property name
     * @param defaultValue
     *            default value
     * @return the property value
     */
    protected int getOptionalIntegerProperty(String property, int defaultValue) {
        try {
            String value = testProperties.getProperty(property);
            if (value == null)
                return defaultValue;
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get an integer property from the loaded test properties.
     * 
     * @param propertyNameList
     *            list of property names (this assumes all of the names mean the
     *            same thing)
     * @param defaultValue
     *            the default value
     * @return the property value
     */
    protected int getOptionalIntegerProperty(String[] propertyNameList,
            int defaultValue) {
        try {
            String value = getOptionalStringProperty(propertyNameList);
            if (value == null)
                return defaultValue;
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }

    }

    /**
     * Get a boolean property from the loaded test properties.
     * 
     * @param property
     *            name of the property
     * @param defaultValue
     *            value to return if property is not defined
     * @return boolean value of the property
     */
    protected boolean getOptionalBooleanProperty(String property,
            boolean defaultValue) {
        String value = getOptionalStringProperty(property);
        if (value != null)
            return value.equalsIgnoreCase("true")
                    || value.equalsIgnoreCase("yes");
        return defaultValue;
    }

    /**
     * Get a boolean property from the loaded test properties.
     * 
     * @param propertyNameList
     *            list of property names (this assumes all of the names mean the
     *            same thing)
     * @param defaultValue
     *            value to return if property is not defined
     * @return boolean value of the property
     */
    protected boolean getOptionalBooleanProperty(String[] propertyNameList,
            boolean defaultValue) {
        String value = getOptionalStringProperty(propertyNameList);
        if (value != null)
            return value.equalsIgnoreCase("true")
                    || value.equalsIgnoreCase("yes");
        return defaultValue;
    }

    /**
     * Get a required string-valued property from the loaded test properties.
     * 
     * @param property
     *            the property name
     * @return the value of the property
     */
    protected String getRequiredStringProperty(String property)
            throws MissingRequiredTestPropertyException {
        String value = testProperties.getProperty(property);
        if (value == null)
            throw new MissingRequiredTestPropertyException(
                    "test.properties is missing required property " + property);
        return value;
    }

    /**
     * Get a required string-valued property from the loaded test properties.
     * 
     * @param propertyNameList
     *            list of property names (this assumes all of the names mean the
     *            same thing)
     * @return the value of the property
     */
    protected String getRequiredStringProperty(String[] propertyNameList)
            throws MissingRequiredTestPropertyException {
        if (propertyNameList.length == 0)
            throw new IllegalArgumentException("empty property name list");
        String value = getOptionalStringProperty(propertyNameList);
        if (value == null)
            throw new MissingRequiredTestPropertyException(
                    "test.properties is missing required property "
                            + propertyNameList[0]);
        return value;
    }

    /**
     * Get an optional string-valued property from the loaded test propertes.
     * 
     * @param property
     *            the property name
     * @return the property value, or null if the property was not defined
     */
    public String getOptionalStringProperty(String property) {
        return testProperties.getProperty(property);
    }

    protected String getOptionalStringProperty(String property,
            String defaultValue) {
        String result = getOptionalStringProperty(property);
        if (result != null)
            return result;
        return defaultValue;
    }

    /**
     * Get an optional string-valued property from the loaded test propertes.
     * 
     * @param propertyNameList
     *            list of property names (this assumes all of the names mean the
     *            same thing)
     * @return the property value, or null if the property was not defined
     */
    protected String getOptionalStringProperty(String[] propertyNameList) {
        for (int i = 0; i < propertyNameList.length; ++i) {
            String value = getOptionalStringProperty(propertyNameList[i]);
            if (value != null)
                return value.trim();
        }
        return null;
    }

    protected String getOptionalStringProperty(String[] propertyNameList,
            String defaultValue) {
        String result = getOptionalStringProperty(propertyNameList);
        if (result != null)
            return result;
        return defaultValue;
    }
}
