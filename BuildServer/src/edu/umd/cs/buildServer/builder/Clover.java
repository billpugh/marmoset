package edu.umd.cs.buildServer.builder;

import java.io.File;
import java.lang.reflect.Method;

public class Clover {

    static Class<?> cloverInstr;
    static Class<?> cloverRuntime;
    static Method cloverInstrMainImpl, xmlReporterRunReport, cloverUtilsScrubCoverageData;
    static Method cloverGlobalFlush;
    static {
        boolean a = false;

        try {
            cloverInstr = Class.forName("com.atlassian.clover.CloverInstr");
            cloverRuntime = Class.forName("com_atlassian_clover.Clover");
            cloverInstrMainImpl = cloverInstr.getDeclaredMethod("mainImpl", String[].class);
            Class<?> cloverUtils = Class.forName("com.atlassian.clover.util.CloverUtils");
            cloverUtilsScrubCoverageData = cloverUtils.getDeclaredMethod("scrubCoverageData", String.class, Boolean.TYPE);
            Class<?> xmlReporter = Class.forName("com.atlassian.clover.reporters.xml.XMLReporter");
            xmlReporterRunReport = xmlReporter.getDeclaredMethod("runReport", String[].class);
            // com_atlassian_clover.Clover.globalFlush();
            cloverGlobalFlush = cloverRuntime.getDeclaredMethod("globalFlush");
            a = true;
        } catch (Exception e) {
//            System.err.println("Unable to load clover");
//        	e.printStackTrace();
            
        }
        available = a;

    }
    static final boolean available;

    public static boolean isAvailable() {
        return available;

    }

    public static File getCloverJar() {
        return JavaBuilder.getCodeBase(cloverInstr);
    }

    public static void globalFlush() {
        if (!available) return;
        try {
            System.out.println("Flushing clover data");
            cloverGlobalFlush.invoke(null);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static int cloverInstrMainImpl(String[] cliArgs) {
        try {
            return (Integer) cloverInstrMainImpl.invoke(null, (Object) cliArgs);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int xmlReporterRunReport(String[] cliArgs) {
        try {
            return (Integer) xmlReporterRunReport.invoke(null, (Object) cliArgs);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void cloverUtilsScrubCoverageData(String requiredProperty, boolean b) {
        try {
            cloverUtilsScrubCoverageData.invoke(null, requiredProperty, b);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
