package edu.umd.cs.buildServer;

import edu.umd.cs.buildServer.util.jni.ProcessKiller;

public class TestProcessKiller {

	public static void main(String args[]) {
		ProcessKiller.kill(49760, ProcessKiller.Signal.QUIT);
	}
}
