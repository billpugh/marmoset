package edu.umd.cs.buildServer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import edu.umd.cs.buildServer.util.jni.ProcessKiller;
import edu.umd.cs.buildServer.util.jni.ProcessKiller.Signal;
import edu.umd.cs.marmoset.utilities.MarmosetUtilities;

public final class ProcessTree {

    final Multimap<Integer, Integer> children = ArrayListMultimap.create();
    final Map<Integer, String> info = new HashMap<Integer, String>();

    final Set<Integer> live = new HashSet<Integer>();
    final Logger log;
    final Process process;
    final long startTime;

    public ProcessTree(Process process, Logger log, long startTime) {
        this.process = process;
        this.log = log;
      
        this.startTime = startTime
                - TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS);
        computeChildrenSwallowErrors();
    }

    // example date: Fri Apr 13 00:02:43 2012
    static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        public SimpleDateFormat initialValue() {
            return new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
        }
    };

    class MyAnalyzePS implements ListProcesses.AnalyzePS{

    	 final int rootPid = MarmosetUtilities.getPid();
         
		@Override
		public void started() {
			live.clear();
		}

		@Override
		public void process(int pid, int ppid, int pgrp, char state, Date started, String txt) {

			 if (started.getTime() < startTime) 
				 return;
			 if (rootPid == pid) 
				 return;
             live.add(pid);
             children.put(ppid, pid);
             info.put(pid, txt);
		}
    	
    	
    }
    private void computeChildrenSwallowErrors() {
    	 
    try {
        ListProcesses.listProcesses(new MyAnalyzePS(), log);
        } catch (Throwable e) {
        	log.debug("Exception while recomputing children after stop", e);
        }
    }
    private void findTree(Set<Integer> found, int pid) {
        if (!found.add(pid))
            return;
        for (int c : children.get(pid))
            findTree(found, c);
    }

    private Set<Integer> findTree(int rootPid) {
        Set<Integer> result = new LinkedHashSet<Integer>();
        findTree(result, rootPid);
        result.retainAll(live);
        return result;
    }

    private Set<Integer> findJvmSubtree() {
        int rootPid = MarmosetUtilities.getPid();
        Set<Integer> result = findTree(rootPid);
        result.remove(rootPid);
        return result;
    }

    private void pause(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            pause(20);
            Thread.currentThread().interrupt();
        }
    }

    public void destroyProcessTree() {
        int pid = MarmosetUtilities.getPid(process);
//        log.info("Killing process tree for pid " + pid);

          
        try {
        	computeChildrenSwallowErrors();
            this.killProcessTree(pid, Signal.KILL);
        } catch (Exception e) {
            log.warn("Error trying to kill process tree for " + pid, e);
        } finally {
            // call process.destroy() whether or not "kill -9 -<pid>" worked
            // in order to maintain proper internal state
            process.destroy();
        }
//        log.info("Done Killing process tree for " + pid);

    }

    private void logProcesses(String title, Collection<Integer> pids) {
        log.info(String.format("%-14s: %s", title, pids));
        for (Integer pid : pids)
            logProcess(pid);
    }

    private void logProcess(Integer pid) {
        String process = info.get(pid);
        if (process == null) {
            log.info("no info for pid " + pid);
            return;
        }
        log.info(process);
    }

    private int killProcessTree(int rootPid, Signal signal) throws IOException {
        Set<Integer> result = findTree(rootPid);
        Set<Integer> unrooted = findTree(1);

        Set<Integer> subtree = findJvmSubtree();
        boolean differ = !result.equals(subtree) || !unrooted.isEmpty();
        if (differ) {
            if (differ)
                log.info("process tree and JVM subtree not the same:");
            logProcesses("root pid", Collections.singleton(rootPid));
            logProcesses("process tree", result);
            logProcesses("unrooted", unrooted);
            logProcesses("JVM subtree", subtree);
            logProcesses("live", live);
        }

        result.addAll(unrooted);
        if (result.isEmpty())
            return 0;
        
        log.info("Halting process tree starting at " + rootPid + " which is "
                + result);
        while (true) {
            killProcesses(Signal.STOP, result);
            pause(100);
            computeChildrenSwallowErrors();
            Set<Integer> r = findTree(rootPid);
            Set<Integer> u = findTree(1);
            r.addAll(u);
            if (r.equals(result))
                break;
            result = r;
            log.info("process tree starting at " + rootPid + " changed to "
                    + result);
        }
        int resultSize = result.size();
        
        killProcesses(signal, result);
        pause(1000);
        log.debug("process tree should now be dead");
        computeChildrenSwallowErrors();
        result.retainAll(children.keySet());
        if (!result.isEmpty()) {
            log.error("Undead processes: " + result);
            killProcesses(signal, result);
            computeChildrenSwallowErrors();
            result.retainAll(children.keySet());
            if (!result.isEmpty())
                log.error("super zombie processes: " + result);
        }
        return resultSize;
    }

    /**
     * @param result
     * @throws IOException
     * @throws InterruptedException
     */
    private void killProcesses(Signal signal, Set<Integer> pids)
            throws IOException {
        if (pids.isEmpty()) {
            return;
        }
        for (Integer pid : pids) {
        	 int exitCode = ProcessKiller.kill(pid, signal);
        	 log.warn("exit code from killing " + pid + " is " + exitCode);
        }
   
    }

    private void drainToLog(final InputStream in) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader r = new BufferedReader(
                            new InputStreamReader(in));

                    while (true) {
                        String txt = r.readLine();
                        if (txt == null)
                            return;
                        log.debug("process generated: " + txt);
                    }
                } catch (IOException e) {
                    if (!e.getMessage().equals("Stream closed"))
                        log.warn("error while draining", e);

                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        assert false;
                    }
                }
            }
        });
        t.setDaemon(true);
        t.start();

    }

    private int execute(ProcessBuilder b) throws IOException {
        b.redirectErrorStream(true);
        Process p = b.start();
        p.getOutputStream().close();
        drainToLog(p.getInputStream());
        boolean isInterrupted = Thread.interrupted();
        int exitCode;
        while (true)
            try {
                exitCode = p.waitFor();
                break;
            } catch (InterruptedException e) {
                isInterrupted = true;
            }
        p.getInputStream().close();
        p.destroy();
        if (isInterrupted)
            Thread.currentThread().interrupt();
        return exitCode;
    }

}
