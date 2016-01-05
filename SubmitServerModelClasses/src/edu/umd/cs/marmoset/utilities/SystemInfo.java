package edu.umd.cs.marmoset.utilities;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;

import javax.annotation.CheckForNull;

import com.sun.management.UnixOperatingSystemMXBean;

public class SystemInfo {

	private static final String GOOD = " good ";

    static final int MEGABYTE = 1024 * 1024;

	static OperatingSystemMXBean osBean = ManagementFactory
			.getOperatingSystemMXBean();
	static @CheckForNull
	com.sun.management.OperatingSystemMXBean sunOsBean = osBean instanceof com.sun.management.OperatingSystemMXBean ? ((com.sun.management.OperatingSystemMXBean) osBean)
			: null;
	static @CheckForNull com.sun.management.UnixOperatingSystemMXBean unixBean =
	       (sunOsBean instanceof UnixOperatingSystemMXBean ?   (UnixOperatingSystemMXBean) sunOsBean : null);

	public static double getLoadAverage() {
		return osBean.getSystemLoadAverage();
	}

	public static boolean hasExtended() {
		return sunOsBean != null;
	}

	public static long getSystemMemory() {
		if (sunOsBean != null)
			return sunOsBean.getTotalPhysicalMemorySize();
		return 0;
	}

	   public static String getSystemLoad() {
	       return getSystemLoad(false);
	   }
	   
	static int mb(long bytes) {
	    long result = bytes/MEGABYTE;
	    if (result > Integer.MAX_VALUE)
	        throw new IllegalArgumentException();
	    return (int) result;
	}
	
	final static long Mbyte = 1024*1024;
	final static long Gbyte = 1024*Mbyte;
	
	public static void getFreeDiskSpace(PrintWriter out, boolean verbose) {
	    for(File f :File.listRoots() ) {
	        long freeSpace = f.getUsableSpace();
	        long totalSpace = f.getTotalSpace();
	        if (totalSpace > 10 * Gbyte && (freeSpace < 2 * Gbyte || verbose))
	            out.printf("%,d MBytes free on %s, ", freeSpace / Mbyte, f);
	    }
	}
	
	public static String getSystemLoad(boolean verbose) {
		Runtime runtime = Runtime.getRuntime();
		
		StringWriter w = new StringWriter();
		PrintWriter out = new PrintWriter(w);
		
		double loadAverage = getLoadAverage();
		if (loadAverage > 2.0)
		    out.printf("Load average %.1f, ", loadAverage);
		
        int freeMemory = mb(runtime.freeMemory());
        int totalMemory = mb(runtime.totalMemory());
        int maxMemory = mb(runtime.maxMemory() );
        if (verbose || totalMemory - freeMemory > maxMemory/2)
            out.printf("memory %d/%d/%d, ", freeMemory, totalMemory, maxMemory);
        
        if (unixBean != null) {
            long openFD = getOpenFD();
            long maxFD = getMaxFD() ;
            if (verbose || openFD > maxFD/2 || openFD > 450)
                out.printf("fd %d/%d, ", openFD, maxFD);
        }
        
        List<MemoryPoolMXBean> mlist = ManagementFactory.getMemoryPoolMXBeans();
        for(MemoryPoolMXBean mb : mlist) {
        	String name = mb.getName();
			if (name.equals("Metaspace"))
				continue;
            MemoryUsage usage = mb.getUsage();
            int current = mb(usage.getUsed());
            int max = mb(usage.getMax());
            
            if (verbose || current > max/2+4) {
				out.printf("%s %d/%d, ", name, current, max);
			}
        }   
        
        getFreeDiskSpace(out, verbose);
        out.close();
        String s =  w.toString();
        if (s.isEmpty())
            return GOOD;
        return s.substring(0, s.length() -2);
	}

	public static long getMaxFD() {
		if (unixBean == null) return -1;
		return unixBean.getMaxFileDescriptorCount();
	}

	public static long getOpenFD() {
		if (unixBean == null) return -1;
		return unixBean.getOpenFileDescriptorCount();
	}
	
	public static boolean isGood(String load) {
	    return load.equals(GOOD);
	}
	
	public static void main(String args[]) {
	    System.out.println(getSystemLoad(false));
        
	    System.out.println(getSystemLoad(true));
		  List<MemoryPoolMXBean> mlist = ManagementFactory.getMemoryPoolMXBeans();

		for(MemoryPoolMXBean mb : mlist) {
			System.out.println(mb.getName());
			System.out.println(mb.getType());
			MemoryUsage usage = mb.getUsage();
            System.out.println(usage);			
		}
	}

}
