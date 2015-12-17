package edu.umd.cs.buildServer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Scanner;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.umd.cs.marmoset.utilities.MarmosetUtilities;

public class ListProcesses {

	interface AnalyzePS {
		public void started();
		public void process(int pid, int ppid, int pgrp, char state, Date started, String txt); 
	
	}

	
	public static void main(String args[]) throws Exception {
		Logger log = Logger.getRootLogger();
		log.setLevel(Level.ALL);
		System.out.printf("%6s %6s %6s %1s %s%n", "pid", "ppid", "pgrp", "S", "txt");;
		listProcesses(new AnalyzePS() {

			@Override
			public void started() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void process(int pid, int ppid, int pgrp, char state, Date started, String txt) {
				System.out.printf("%6d %6d %6d %c %s\n", pid, ppid, pgrp, state, txt);
				
			}}, log);
	}
	public static void listProcesses(ListProcesses.AnalyzePS callback, Logger log) throws IOException {
		File p = new File("/proc");
		if (p.exists() && p.isDirectory())
			listProcessesUsingProc(callback, log);
		else
			listProcessesUsingPS(callback, log);
	}
	public static void listProcessesUsingPS(ListProcesses.AnalyzePS callback, Logger log) throws IOException {
	    ProcessBuilder b = new ProcessBuilder(
	            new String[] { "/bin/ps", "xww", "-o",
	                    "pid,ppid,pgid,lstart,user,state,pcpu,cputime,args" });
	
	    String thisUser = System.getProperty("user.name");
	    Process p = null;
	    try {
	        try {
	            p = b.start();
	        } catch (RuntimeException t) {
	            log.fatal("Unable to start ps", t);
	            throw t;
	        } catch (Error t) {
	            log.fatal("Unable to start ps", t);
	            throw t;
	        }
	        
	        int psPid = MarmosetUtilities.getPid(p);
	        p.getOutputStream().close();
	        Scanner s = new Scanner(p.getInputStream());
	        log.warn("Starting ps");
	        String header = s.nextLine();
	        log.warn("ps header: " + header);
	        callback.started();
	
	        while (s.hasNext()) {
	            String txt = s.nextLine();
	            log.warn(txt);
	            try {
	            	int pid = Integer.parseInt(txt.substring(0, 5).trim());
	                int ppid = Integer.parseInt(txt.substring(6, 11).trim());
	                int pgrp = Integer.parseInt(txt.substring(12, 17).trim());
	                 Date started = ProcessTree.DATE_FORMAT.get().parse(
	                        txt.substring(18, 42));
	                 String user = txt.substring(43,51).trim();
	                 char state = txt.charAt(52);
	                 if (!user.equals(thisUser)) 
	                	 continue;
	                 
	                if (psPid == pid)
	                    continue;
	                callback.process(pid, ppid, pgrp, state, started, txt);
	            } catch (Exception e) {
	                log.error("Error while building process treee, parsing "
	                        + txt, e);
	            }
	
	        }
	        s.close();
	        s = new Scanner(p.getErrorStream());
	        while (s.hasNext()) {
	            log.error(s.nextLine());
	        }
	        s.close();
	        log.warn("Finished ps");

	    } finally {
	    	if (p != null)
	    		p.destroy();
	    }
	}

	public static String getLine(File f) {
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			return br.readLine();
		} catch (IOException e) {
			return "";
		}
	}

	public static void listProcessesUsingProc(ListProcesses.AnalyzePS callback, Logger log) throws IOException {
		File proc = new File("/proc");
		if (!proc.isDirectory()) 
			throw new IOException("/proc not available");
		int myPid = MarmosetUtilities.getPid();
		String myUserId = getLoginUID(new File(proc, Integer.toString(myPid)));
		Date now = new Date();
		callback.started();
		for(File p : proc.listFiles()) {
			String name = p.getName();
			int pid;
			try {
				 pid = Integer.parseInt(name);
			} catch (NumberFormatException e) {
				continue;
			}
			String userid = getLoginUID(p);
			if (!myUserId.equals(userid))
				continue;
			String contents = getStat(p);
			String fields[] = contents.split(" ");
			int pid0 = Integer.parseInt(fields[0]);
			String filename = fields[1];
			char state = fields[2].charAt(0);
			int ppid =  Integer.parseInt(fields[3]);
			int pgrp =  Integer.parseInt(fields[4]);

			
			callback.process(pid0, ppid, pgrp, state, now, contents);
			
			
		}
	    ProcessBuilder b = new ProcessBuilder(
	            new String[] { "/bin/ps", "xww", "-o",
	                    "pid,ppid,pgrp,lstart,user,state,pcpu,cputime,args" });
	
	    Process p = null;
	    try {
	
	        try {
	            p = b.start();
	        } catch (RuntimeException t) {
	            log.fatal("Unable to start ps", t);
	            throw t;
	        } catch (Error t) {
	            log.fatal("Unable to start ps", t);
	            throw t;
	        }
	        
	        int psPid = MarmosetUtilities.getPid(p);
	        p.getOutputStream().close();
	        Scanner s = new Scanner(p.getInputStream());
	        log.warn("Starting ps");
	        String header = s.nextLine();
	        log.warn("ps header: " + header);
	        callback.started();
	
	        while (s.hasNext()) {
	            String txt = s.nextLine();
	            log.warn(txt);
	            try {
	            	int pid = Integer.parseInt(txt.substring(0, 5).trim());
	                int ppid = Integer.parseInt(txt.substring(6, 11).trim());
	                int pgrp = Integer.parseInt(txt.substring(12, 17).trim());
	                 Date started = ProcessTree.DATE_FORMAT.get().parse(
	                        txt.substring(18, 42));
	                 String user = txt.substring(43,51).trim();
	                 char state = txt.charAt(52);
	                 
	                if (psPid == pid)
	                    continue;
	                callback.process(pid,  ppid, pgrp, state, started, txt);
	            } catch (Exception e) {
	                log.error("Error while building process treee, parsing "
	                        + txt, e);
	            }
	
	        }
	        s.close();
	        s = new Scanner(p.getErrorStream());
	        while (s.hasNext()) {
	            log.error(s.nextLine());
	        }
	        s.close();
	        log.warn("Finished ps");
	    } catch (IOException e) {
	        throw new RuntimeException(e);
	    } finally {
	    	if (p != null)
	    		p.destroy();
	    }
	}

	private static String getStat(File p) {
		return getLine(new File(p,"stat"));
	}

	private static String getLoginUID(File p) {
		return getLine(new File(p, "loginuid"));
	}

}
