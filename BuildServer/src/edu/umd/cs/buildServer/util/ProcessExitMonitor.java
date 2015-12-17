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

/*
 * Created on Feb 10, 2005
 */
package edu.umd.cs.buildServer.util;

import org.apache.log4j.Logger;

import edu.umd.cs.marmoset.utilities.MarmosetUtilities;

/**
 * Wait a fixed amount of time for a process to exit.
 * 
 * @author David Hovemeyer
 */
public final class ProcessExitMonitor implements Runnable {
    
    private final Logger log;
	private final Process process;
	private volatile boolean exited = false;
	private volatile boolean shutdown = false;
	private volatile int exitCode;
	private final ProcessTree tree;
	private final Thread thread;
	private int processCount = 0;
	

	public int getProcessCount() {
		return processCount;
	}


	public ProcessExitMonitor(Process process, Logger logger, long startTime) {
	    thread = new Thread(this, "Process exit monitor");
		thread.setDaemon(true);
		this.process = process;
		this.log = logger;
		
		log.debug("Starting exit monitor for pid " + MarmosetUtilities.getPid(process));
		
		this.tree = new ProcessTree(process, logger, startTime);
		thread.start();
	}


	public Process getProcess() {
		return process;
	}
	@Override
	public void run() {
		try {
			this.exitCode = process.waitFor();
			this.exited = true;
		} catch (InterruptedException e) {
		    log.warn("process exit monitor interrupted");
		    assert true;
		} finally {
		    // no matter how we exit, kill process tree
		    log.debug("Killing process tree");
		    processCount = tree.destroyProcessTree();
		    this.shutdown = true;
		    synchronized(this) {
		        notifyAll();
		    }
		}
		
	}

	/**
	 * Wait given number of milliseconds for the process to exit. If it does
	 * not finish within that amount of time, kill the process;
	 * 
	 * @param millis
	 *            number of milliseconds to wait for the process to exit
	 * @return true if the process exited in the given amount of time, false
	 *         otherwise
	 * @throws InterruptedException
	 */
    public boolean waitForProcessToExit(long millis) {
        try {
            synchronized (this) {
                if (!exited && !shutdown) {
                    log.debug("waiting " + millis + " ms");
                    this.wait(millis);
                    log.debug("waiting done");
                }

            }
            if (!exited)
                thread.interrupt();

            synchronized (this) {
                while (!shutdown) {
                    log.warn("waiting for shutdown");
                    this.wait();
                }
            }
        } catch (InterruptedException e) {
            log.warn("waitForProcessToExit interrupted");
            Thread.currentThread().interrupt();
        }

        log.warn("waitForProcessToExit returning " + exited);

        return exited;
    }
    
    

	/**
	 * @return Returns the exitCode.
	 */
	public int getExitCode() {
	    if (!exited)
	        throw new IllegalStateException();
		return exitCode;
	}
}
