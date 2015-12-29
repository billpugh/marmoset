package edu.umd.cs.marmoset.modelClasses;

import static edu.umd.cs.marmoset.modelClasses.TestPropertyKeys.LD_LIBRARY_PATH;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;

import edu.umd.cs.marmoset.modelClasses.TestOutcome.TestType;

public class ScriptTestProperties extends TestProperties {
   
    
    final Map<String, ExecutableTestCase> testCases;
	private String ldLibraryPath;
    public ScriptTestProperties(Properties testProperties) {
        this(Framework.SCRIPT, testProperties);
    }
    protected ScriptTestProperties(Framework framework, Properties testProperties) {
        super(framework, testProperties);
        testCases = ExecutableTestCase.parse(this);
    }
    public Collection<ExecutableTestCase> getExecutableTestCases() {
        return testCases.values();
    }

    public EnumSet<TestType> getDynamicTestKinds() {
    	EnumSet<TestType> result =  EnumSet.noneOf(TestType.class);
    
    	for (TestType testType : TestType.DYNAMIC_TEST_TYPES) {
    		if (getTestNames(testType) != null)
    			result.add(testType);
    	}
    	return result;
    	       
    }
    
    public Iterable<String> getTestNames(TestType testType) {
       String names =  getOptionalStringProperty(TestPropertyKeys.TESTCASES_PREFIX + testType.toString());
       if (names == null)
           names =  getOptionalStringProperty(TestPropertyKeys.TESTCLASS_PREFIX + testType.toString());
       if (names == null)
           return null;
       names = names.trim();
       if (names.isEmpty())
           return null;
       return Arrays.asList(names.split("[,\\s]+"));
           
    }
	public String getLdLibraryPath() {
	    return ldLibraryPath;
	}
	protected void setLdLibraryPath(String ldLibraryPath) {
	    this.ldLibraryPath = ldLibraryPath;
	    setProperty(LD_LIBRARY_PATH, this.ldLibraryPath);
	}
   
}
