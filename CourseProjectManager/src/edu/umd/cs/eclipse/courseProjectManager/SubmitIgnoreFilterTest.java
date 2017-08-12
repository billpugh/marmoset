package edu.umd.cs.eclipse.courseProjectManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SubmitIgnoreFilterTest extends SubmitIgnoreFilter {

    
    boolean deepMatches(String filename) {
        boolean  shallow = matches(filename);
        boolean deep = matches("foobar/"+filename);
       if (shallow != deep) throw new AssertionError("Inconsistent results for " + filename);
       return shallow;
    }
    @Test
    public void test() {
        assertFalse(deepMatches("Foo.java"));
        assertTrue(deepMatches("Foo.class"));
        assertFalse(deepMatches("Foo.c"));
        assertTrue(deepMatches("Foo.o"));
        assertTrue(deepMatches(".git"));
        assertTrue(deepMatches("core"));
        assertTrue(deepMatches("CVS"));
        assertFalse(deepMatches("notCVS"));
        assertTrue(deepMatches("Foo.exe"));
        assertTrue(deepMatches("Foo.BAK"));
        assertTrue(deepMatches("Foo.bak"));
        assertTrue(deepMatches("Foo.old"));
        assertTrue(deepMatches("Foo.java~"));
        
        
    }

}
