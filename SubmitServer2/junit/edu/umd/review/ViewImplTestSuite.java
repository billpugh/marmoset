package edu.umd.review;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.google.gwt.junit.tools.GWTTestSuite;

import edu.umd.review.gwt.view.impl.DraftViewImplTest;
import edu.umd.review.gwt.view.impl.ThreadViewImplTest;

public class ViewImplTestSuite extends GWTTestSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite("View tests");
    suite.addTestSuite(DraftViewImplTest.class);
    suite.addTestSuite(ThreadViewImplTest.class);
    return suite;
  }
}
