package edu.umd.cs.buildServer.util;

import java.io.File;

public class TestingGround {
	
	public static void main(String args[]) throws Exception {
		File[] processDirs = new File("/Users/pugh/test").listFiles(ListProcesses::fileIsProcess);
		for(File f : processDirs)
			System.out.println(f);;
	}

}
