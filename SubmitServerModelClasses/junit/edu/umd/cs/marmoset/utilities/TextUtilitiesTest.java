package edu.umd.cs.marmoset.utilities;

import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class TextUtilitiesTest {

  @Test
  public void test() throws FileNotFoundException, IOException {
    DisplayProperties displayProperties = new DisplayProperties();
    Map<String, List<String>> files = TextUtilities.scanTextFilesInZip(new FileInputStream("/Users/pugh/Documents/workspace/cmsc131-p7/submission.zip")
        , displayProperties);
    System.out.println(displayProperties.entries);
    System.out.println(files.keySet());
    
  }

}
