package edu.umd.cs.marmoset.utilities;

import static org.junit.Assert.*;

import org.junit.Test;

public class PotentiallyLeakyMessageExceptionTest {

    @Test
    public void testArrayIndexOutOfBounds() {
        int a[] = {1, 2};
    
        try {
           System.out.println(a[2]);
        } catch (ArrayIndexOutOfBoundsException e) {
            assertFalse(PotentiallyLeakyMessageException.needsSanitization(e));
        }
        
    }

}
