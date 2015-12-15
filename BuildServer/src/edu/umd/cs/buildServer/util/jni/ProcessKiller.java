package edu.umd.cs.buildServer.util.jni;

public class ProcessKiller {

	public static native int kill(int pid, int sig);

	public static  int kill(int pid, Signal signal) {
		return kill(pid, signal.value);
	}

	
	public enum Signal {
		HUP("hang up",1),
		INTERRUPT("interrupt",2),
		QUIT("quit",3), 
		ABORT("abort",6),
		KILL("non-catchable, non-ignorable kill",9),    
		ALARM("alarm clock",14),
		TERMINATION("software termination signal",15);

		final String msg;
		final int value;
		Signal(String msg, int value) {
			this.msg = msg;
			this.value = value;
		}
		
	}
	static {
		System.loadLibrary("ProcessKiller");
	}

}