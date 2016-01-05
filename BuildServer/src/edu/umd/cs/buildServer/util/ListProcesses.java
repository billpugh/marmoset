package edu.umd.cs.buildServer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Scanner;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

import edu.umd.cs.marmoset.utilities.MarmosetUtilities;
import edu.umd.cs.marmoset.utilities.SystemInfo;

public class ListProcesses {

	interface AnalyzePS {
		public void started();

		public void process(int pid, int ppid, int pgrp, char state, Date started, String txt);

	}

	public static String getRealPath(Path p)  {
		try {
		return p.toRealPath().toString();
		} catch (Exception e) {
			return "Exception " + p.toString();
		}
	}
	public static  Multiset<String> openFiles() throws IOException {
		Multiset<String> result =  TreeMultiset.create();
		int myPid = MarmosetUtilities.getPid();
		for(File f :  new File("/proc/" + myPid + "/fd").listFiles()) 
			result.add(f.getCanonicalPath());
		
		return result;
		
	}
	public static void main(String args[]) throws Exception {
		Logger log = Logger.getRootLogger();
		log.setLevel(Level.ALL);
		System.out.printf("%6s %6s %6s %1s %s%n", "pid", "ppid", "pgrp", "S", "txt");
		
		listProcesses(new AnalyzePS() {

			@Override
			public void started() {
				// TODO Auto-generated method stub

			}

			@Override
			public void process(int pid, int ppid, int pgrp, char state, Date started, String txt) {
				System.out.printf("%6d %6d %6d %c %s\n", pid, ppid, pgrp, state, txt);

			}
		}, log);
	}

	public static void listProcesses(ListProcesses.AnalyzePS callback, Logger log) throws IOException {
		File p = new File("/proc");
		if (p.exists() && p.isDirectory())
			listProcessesUsingProc(callback, log);
		else
			listProcessesUsingPS(callback, log);
	}

	private static void listProcessesUsingPS(ListProcesses.AnalyzePS callback, Logger log) throws IOException {
		ProcessBuilder b = new ProcessBuilder(
				new String[] { "/bin/ps", "xww", "-o", "pid,ppid,pgid,lstart,user,state,pcpu,cputime,args" });
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
				try {
					int pid = Integer.parseInt(txt.substring(0, 5).trim());
					int ppid = Integer.parseInt(txt.substring(6, 11).trim());
					int pgrp = Integer.parseInt(txt.substring(12, 17).trim());
					Date started = ProcessTree.DATE_FORMAT.get().parse(txt.substring(18, 42));
					String user = txt.substring(43, 51).trim();
					char state = txt.charAt(52);
					if (!user.equals(thisUser))
						continue;

					if (psPid == pid)
						continue;
					callback.process(pid, ppid, pgrp, state, started, txt);
				} catch (Exception e) {
					log.error("Error while building process treee, parsing " + txt, e);
				}

			}
			s.close();
			try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
				r.lines().forEach(txt -> log.error(txt));
			}
			
			log.warn("Finished ps");

		} finally {
			if (p != null)
				p.destroy();
		}
	}

	public static String getLine(Path p) throws IOException {
		try (BufferedReader r = Files.newBufferedReader(p)) {
			return r.readLine();
		}
	}
	public static String getLine(File f) throws IOException {
		try (BufferedReader r = new BufferedReader(new FileReader(f))) {
			return r.readLine();
		}
	}

	public static boolean isProcess(Path p) {
		try {
			getPidFromProcDirectory(p);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}

	}

	public static boolean isProcess(File file) {
		try {
			Integer.parseInt(file.getName());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	private static int getPidFromProcDirectory(Path p) {
		return Integer.parseInt(p.getName(1).toString());
	}

	private static int getPidFromProcDirectory(File p) {
		return Integer.parseInt(p.getName());
	}
	private static void listProcessesUsingProc(ListProcesses.AnalyzePS callback, Logger log) throws IOException {
		Path proc = Paths.get("/proc");
		
		if (!Files.isDirectory(proc))
			throw new IOException("/proc not available");

		int myPid = MarmosetUtilities.getPid();
		log.info("My pid: " + myPid);
		String myUserId = getLoginUID(proc.resolve(Integer.toString(myPid)));
		log.info("My userID: " + myUserId);
		Date now = new Date();
		callback.started();
		long count0 = SystemInfo.getOpenFD();
		// Multiset<String> initiallyOpen = openFiles();
		 File[] processDirs = new File("/proc").listFiles(ListProcesses::isProcess);
		 long count1 = SystemInfo.getOpenFD();
		 for(File p : processDirs) {
					
			try {
				
				String userid = getLoginUID(p);
				if (!myUserId.equals(userid))
					return;
				int pid = getPidFromProcDirectory(p);
				String contents = getStat(p);
				String fields[] = contents.split(" ");
				int pid0 = Integer.parseInt(fields[0]);
				String filename = fields[1];
				char state = fields[2].charAt(0);
				int ppid = Integer.parseInt(fields[3]);
				int pgrp = Integer.parseInt(fields[4]);
				if (pid != pid0) {
					log.error("Pid " + pid + " doesn't match " + contents);
				}
				log.info("proc: " + userid + " :: " + contents);
				callback.process(pid, ppid, pgrp, state, now, filename);
			} catch (IOException e) {
				// must have died before we could examine it
			} catch (Exception e) {
				log.error("Error examining " + p, e);

			}

		};
		long count2 = SystemInfo.getOpenFD();
		 Multiset<String> openAtEnd = openFiles();
			
		if (count2 > 10 && (count2 > count0 || count2 > SystemInfo.getMaxFD()/2)) {
			log.warn(String.format("Open file descriptors: %d -> %d -. %d (max %d)",
					count0, count1, count2, SystemInfo.getMaxFD()));
			log.warn(openAtEnd.size() + " file descriptors");
			openAtEnd.entrySet().forEach(log::warn);
		}
		
		
	}

	private static String getStat(Path p) throws IOException {
		return getLine(p.resolve("stat"));
	}

	private static String getStat(File p) throws IOException {
		return getLine(new File(p, "stat"));
	}
	private static String getLoginUID(Path p) throws IOException {
		try (BufferedReader r = Files.newBufferedReader(p.resolve("status"))) {
			while(true) {
				String s = r.readLine();
				if (s == null)
					throw new IOException("Did not find user id for proc " + p);
				if (s.startsWith("Uid:"))
					return s.split("\t")[1];
			}
		}
	}
	private static String getLoginUID(File p) throws IOException {
		try (BufferedReader r =new BufferedReader(new FileReader(new File(p, "status")))) {
			while(true) {
				String s = r.readLine();
				if (s == null)
					throw new IOException("Did not find user id for proc " + p);
				if (s.startsWith("Uid:"))
					return s.split("\t")[1];
			}
		}
	}

}
