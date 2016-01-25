package edu.umd.cs.buildServer.util.jni;

import java.io.File;
import java.io.IOException;

import edu.umd.cs.buildServer.BuildServerConfiguration;

public class ProcessKiller {

	public static void main(String args[]) {
		System.out.println("nativeLibraryLoaded: " + nativeLibraryLoaded);
	}

	public static native int kill(int pid, int sig);

	public static int killFallback(int pid, int sig) {
		return -1;
	}

	public static int kill(int pid, Signal signal) {
		if (nativeLibraryLoaded)
			return kill(pid, signal.value);
		else
			return kill(pid, signal.value);
	}

	public static int killProcessGroup(int pid, Signal signal) {
		return kill(-pid, signal);
	}

	static final boolean nativeLibraryLoaded;

	public static boolean getNativeLibraryLoaded() {
		return nativeLibraryLoaded;
	}

	public enum Signal {
		HUP("hang up", 1), INTERRUPT("interrupt", 2), QUIT("quit", 3), ABORT("abort", 6), KILL(
				"non-catchable, non-ignorable kill",
				9), ALARM("alarm clock", 14), TERMINATION("software termination signal", 15), STOP("stop", 19);

		final String msg;
		final int value;

		Signal(String msg, int value) {
			this.msg = msg;
			this.value = value;
		}

	}

	static File getFile(File f, String... components) {
		for (String c : components) {
			f = new File(f, c);
		}
		return f;
	}

	static {
		File root = BuildServerConfiguration.getBuildServerRootFromCodeSource();
		String osName = System.getProperty("os.name");
		System.out.println(osName);
		String bitSize = System.getProperty("sun.arch.data.model");
		System.out.println(bitSize);

		switch (osName) {
		case "Mac OS X":
			osName = "Darwin";
			break;
		}
		boolean success;
		File library = getFile(root, "lib", osName, bitSize, "libProcessKiller.so");
		String libraryPath = library.getAbsolutePath();
		try {
			if (!library.exists() || !library.canRead())
				throw new IOException(libraryPath + " doesn't exist or isn't readable");
			System.load(libraryPath);
			success = true;
		} catch (Throwable t) {
			System.err.println("Unable to load native library for process killing from " + libraryPath);
			t.printStackTrace();
			success = false;
		}
		nativeLibraryLoaded = success;
	}

}