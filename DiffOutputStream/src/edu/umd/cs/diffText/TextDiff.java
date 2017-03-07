package edu.umd.cs.diffText;

import static junit.framework.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextDiff extends StringsWriter {

    /**
     * Invoke the main method of a class, redirecting System.in and System.out,
     * and passing the supplied arguments to main
     */
    public static void invokeMain(Class<?> c, InputStream input, OutputStream out, String... args) throws Exception {
        PrintStream oStream = new PrintStream(out);
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        try {
            Method main;

            try {
                main = c.getMethod("main", String[].class);

                setSystemInAndOut(input, oStream);

            } catch (Throwable t) {
                // If an exception happens before we execute student code, throw
                // a TestInfrastructureException
                throw new TestInfrastructureException(t);
            }

            main.invoke(null, (Object) args);
            oStream.close();
            input.close();
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t instanceof Exception)
                throw (Exception) t;
            if (t instanceof Error)
                throw (Error) t;
            throw e;
        } finally {
            setSystemInAndOut(oldIn, oldOut);

        }
    }

    private static void setSystemInAndOut(final InputStream input, final PrintStream oStream) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                System.setIn(input);
                System.setOut(oStream);
                return null;
            }
        });
    }

    public static Builder withOptions() {
        return new Builder();
    }

    public static Builder withOptions(EnumMap<Option, String> options) {
        return new Builder(options);
    }

    public enum Option {
        TRIM, IGNORE_CASE, IGNORE_WHITESPACE_CHANGE, IGNORE_BLANK_LINES, IGNORE_WHITESPACE, WAIT_FOR;
        public static Option valueOfAnyCase(String name) {
            name = name.toUpperCase().replace(' ', '_');
            if (name.startsWith("IGNORES"))
                name = "IGNORE" + name.substring(6);
            if (name.equals("WAITFOR") )
                return WAIT_FOR;
            if (name.equals("IGNORECASE") )
                return IGNORE_CASE;
            if (name.equals("IGNOREWHITESPACECHANGE"))
                return IGNORE_WHITESPACE_CHANGE;
            if (name.equals("IGNOREWHITESPACE"))
                return IGNORE_WHITESPACE;
             if (name.equals("IGNOREBLANKLINES"))
                return IGNORE_BLANK_LINES;
            return valueOf(name);
        }
    }

    public static class Builder implements Cloneable {
        final EnumMap<Option,String> options;

        final ArrayDeque<Object> expect = new ArrayDeque<Object>();

        public Builder() {
            options = new EnumMap<Option, String>(Option.class);
        }

        public Builder(EnumMap<Option, String>options) {
            this.options = new EnumMap<Option,String>(options);
        }

        @Override
        protected Builder clone() {
            try {
                return (Builder) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }

        public Builder copy() {
            return this.clone();
        }

        public Builder set(Option... options) {

            for (Option o : options) {
                this.options.put(o, null);

            }
            return this;
        }
        public Builder set(Option option, String value) {

            this.options.put(option, value);

            return this;
        }

        public Builder trim() {
            set(Option.TRIM);
            return this;
        }

        public Builder ignoreCase() {
            set(Option.IGNORE_CASE);
            return this;
        }

        public Builder ignoreWhitespaceChange() {
            set(Option.IGNORE_WHITESPACE_CHANGE);
            return this;
        }
        public Builder ignoreWhitespace() {
            set(Option.IGNORE_WHITESPACE);
            return this;
        }

        public Builder ignoreBlankLines() {
            set(Option.IGNORE_BLANK_LINES);
            return this;
        }

        public Builder expect(String s) {
            expect.add(s);
            return this;
        }

        public Builder expect(String... strings) {
            for (String s : strings)
                expect.add(s);
            return this;
        }

        public Builder expect(Iterable<String> strings) {
            for (String s : strings)
                expect.add(s);
            return this;
        }

        public Builder expect(Pattern p) {
            expect.add(p);
            return this;
        }

        public Builder expect(File f) {
            expect.add(f);
            return this;
        }

        public TextDiff build() {
            return new TextDiff(new ArrayDeque<Object>(expect),  new EnumMap<Option,String>(options));
        }

        /**
         * Check that the output of the testedMain method matches that of the
         * referenceMain method on the supplied input file.
         * 
         * @param testedMain
         *            The class of the main method to be tested.
         * @param referenceMain
         *            The class of the reference main method
         * @param inputFile
         *            the input file
         * @throws Exception
         *             - if if any exception is thrown by the tested code.
         */
        public void check(Class<?> testedMain, Class<?> referenceMain, String inputFile) throws Exception {
            File f;
            Builder b = copy();
            try {
                StringListWriter expectedWriter = new StringListWriter();
                f = new File(inputFile);

                invokeMain(referenceMain, new FileInputStream(f), expectedWriter);
                b.expect(expectedWriter.getStrings());
            } catch (Throwable t) {
                throw new TestInfrastructureException(t);
            }

            invokeMain(testedMain, new FileInputStream(f), b.build());
        }

        /**
         * Check that the output of the testedMain method matches that of the
         * referenceMain method on the supplied input file.
         * 
         * @param testedMain
         *            The class of the main method to be tested.
         * @param referenceMain
         *            The class of the reference main method
         * @param inputFile
         *            the input file
         * @throws Exception
         *             - if if any exception is thrown by the tested code.
         */
        public void check(Class<?> testedMain, String inputFile, String expectedOutputFile) throws Exception {
            Builder b = copy();
            try {
                b.expect(new File(expectedOutputFile));
            } catch (Throwable t) {
                throw new TestInfrastructureException(t);
            }
            invokeMain(testedMain, new FileInputStream(new File(inputFile)), b.build());
        }
    }

    public static class TestInfrastructureException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        TestInfrastructureException(Throwable t) {
            super(t);
        }

        TestInfrastructureException(String msg) {
            super(msg);
        }
    }

    final EnumMap<Option, String> options;
    final ArrayDeque<Object> expect;
    boolean gotExpectedWaitFor;
    boolean gotActualWaitFor;

    TextDiff(ArrayDeque<Object> expect, EnumMap<Option, String> options) {
        this.expect = expect;
        this.options = new EnumMap<Option, String>(options);
        this.gotExpectedWaitFor = this.gotActualWaitFor
                = !options.containsKey(Option.WAIT_FOR);
        
    }
    
    

     static String normalize( Set<Option> options, String s) {
        if (options.contains(Option.IGNORE_CASE))
            s = s.toLowerCase();
        if (options.contains(Option.IGNORE_WHITESPACE))
            s = s.replaceAll("\\s+", "");
        else if (options.contains(Option.IGNORE_WHITESPACE_CHANGE))
            s = s.replaceAll("\\s+", " ").replaceAll(" $","");
        else if (options.contains(Option.TRIM))
            s = s.trim();
        return s;
    }
    
    private String normalize(String s) {
        return normalize(options.keySet(), s);
       
    }

    private   Object getNextExpected() {
        while (true) {
            Object o = getNextExpected0();
            if (o == null) return null;
            if (options.containsKey(Option.IGNORE_BLANK_LINES) && o instanceof String && ((String) o).trim().isEmpty()) 
                continue;
            if (!gotExpectedWaitFor) {
                if (! isWhatWeAreWaitingFor((String) o))
                    continue;
                gotExpectedWaitFor = true;
            }
            return o;
        }
    }
    
    private Object getNextExpected0() {
        try {
            while (true) {
                Object o = expect.pollFirst();
                if (o == null)
                    return null;
                Class<?> c = o.getClass();
                if (c == String.class) {
                    return o;
                } else if (c == Pattern.class) {
                    return c;
                } else if (c == BufferedReader.class) {
                    BufferedReader r = (BufferedReader) o;
                    String txt = r.readLine();
                    if (txt == null)
                        continue;
                    expect.addFirst(r);
                    return txt;

                } else if (c == File.class) {
                    BufferedReader r = new BufferedReader(new FileReader((File) o));
                    expect.addFirst(r);
                } else {
                    throw new AssertionError("Did not expect " + c);
                }
            }
        } catch (Throwable t) {
            throw new TestInfrastructureException(t);
        }

        
    }
    
  public static boolean differentBy(Option option, String normalizedExpected, String normalizedActual) {
        
      Set<Option> options = EnumSet.of(option);
       return normalize(options,normalizedExpected).equals(normalize(options, normalizedActual));
    }
  
    public Option diagnoseDifference(String normalizedExpected, String normalizedActual) {
        
        for(Option o : Option.values())
            if (differentBy(o, normalizedExpected, normalizedActual))
                return o;
        return null;
        
    }

    private String actual;
    private Object expected;

    public String getActual() {
        return actual;
    }

    public Object getExpected() {
        return expected;
    }

    @Override
    protected void got(String txt) {
        if (txt == null)
            throw new NullPointerException();
        actual = txt;

        int line = getLine();
        if (!gotActualWaitFor) {
            if (! isWhatWeAreWaitingFor(txt) ) 
                return;
            gotActualWaitFor = true;
        }
        boolean ignoreBlankLines = options.containsKey(Option.IGNORE_BLANK_LINES);
        if (ignoreBlankLines && txt.trim().isEmpty())
            return;
        Object o= getNextExpected();
        expected = o;
        if (o == null) {
            fail(String.format("On line %d, expected no more output but saw '%s'", line, txt));
        } else {
            String normalizedActual = normalize(txt);
            if (o instanceof String) {
                String normalizedExpected = normalize((String) o);
                if (!normalizedExpected.equals(normalizedActual)) {
                    Option diff = diagnoseDifference(normalizedExpected, normalizedActual);
                    if (diff != null) switch(diff) {
                    case TRIM: 
                        fail(String.format("On line %d, incorrect leading or trailing whitespace: expected '%s' but saw '%s'", line, o, txt));
                        break;
                    case IGNORE_CASE: 
                        fail(String.format("On line %d, incorrect case: expected '%s' but saw '%s'", line, o, txt));
                        break;
                    case IGNORE_WHITESPACE_CHANGE:
                       fail(String.format("On line %d, whitespace doesn't match: expected '%s' but saw '%s'", line, o, txt));
                        break;
                    case IGNORE_WHITESPACE:
                        fail(String.format("On line %d, missing or unexpected whitespace: expected '%s' but saw '%s'", line, o, txt));
                         break;
                    default:
                        
                    }
                    fail(String.format("On line %d, expected '%s' but saw '%s'", line, o, txt)); 
                }

            } else if (o instanceof Pattern) {
                Pattern p = (Pattern) o;
                Matcher m = p.matcher(normalizedActual);
                if (!m.matches())
                    fail(String.format("On line %d, /%s/ doesn't match '%s'", line, p, txt));

            } else {
                fail("Got " + o.getClass());
            }
        }

    }

    /**
     * @param txt
     * @return
     */
    public boolean isWhatWeAreWaitingFor(String txt) {
        String waitWeAreWaitingFor = options.get(Option.WAIT_FOR);
        return waitWeAreWaitingFor != null && normalize(txt).contains(waitWeAreWaitingFor);
    }

    @Override
    public void close() {
        super.close();
        if (!gotActualWaitFor) {
            fail(String.format("output ended at line %d but didn't see signal %s for start of expected output", getLine(), options.get(Option.WAIT_FOR)));
            
        }
        Object o = getNextExpected();
        if (o == null)
            return;
        if (o instanceof Pattern)
            fail(String.format("On line %d, expected line matching /%s/ but output ended", getLine() + 1, o));
        else
            fail(String.format("On line %d, expected '%s' but output ended", getLine() + 1, o));

    }

    public static FutureTask<Void> copyTask(final String name, final InputStream from, final OutputStream to) {
        return copyTask(name, from, to, Integer.MAX_VALUE);
    }

    public static FutureTask<Void> copyTask(final String name, final InputStream from, final OutputStream to,
            final int maxDrain) {
        Callable<Void> doCheck = new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                int capacity = maxDrain;
                byte[] buf = new byte[1024];
                try {
                    while (true) {
                        int sz;
                        try {
                            sz = from.read(buf);
                        } catch (Exception e) {
                            // ignore exceptions that happen while reading, but
                            // treat them as EOF
                            System.err.println("Error in " + name);
                            e.printStackTrace();
                            sz = -1;
                        }
                        if (sz < 0) {
                            to.close();
                            return null;
                        }
                        if (sz > capacity)
                            sz = capacity;
                        to.write(buf, 0, sz);
                        capacity -= sz;
                    }
                } finally {
                    try {
                        from.close();
                        to.close();
                    } catch (Exception e) {
                        System.err.println("Error in " + name);
                        e.printStackTrace();
                        assert true;
                    }

                }
            }
        };

        return new FutureTask<Void>(doCheck);
    }

    public static FutureTask<Void> copyTask(final String name, final Reader from, final Writer to) {
        return copyTask(name, from, to, Integer.MAX_VALUE);
    }

    public static FutureTask<Void> copyTask(final String name, final Reader from, final Writer to, final int maxDrain) {
        Callable<Void> doCheck = new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                int capacity = maxDrain;
                char[] buf = new char[1024];
                try {
                    while (true) {
                        int sz;
                        try {
                            sz = from.read(buf);
                        } catch (Exception e) {
                            // ignore exceptions that happen while reading, but
                            // treat them as EOF
                            sz = -1;
                        }
                        if (sz < 0) {
                            to.close();
                            return null;
                        }
                        if (sz > capacity)
                            sz = capacity;
                        to.write(buf, 0, sz);
                        capacity -= sz;
                    }
                } finally {
                    try {
                        from.close();
                        to.close();
                    } catch (Exception e) {
                        System.err.println("Error in " + name);
                        e.printStackTrace();
                        assert true;
                    }

                }
            }
        };

        return new FutureTask<Void>(doCheck);
    }

    public FutureTask<Void> check(final InputStream actual) {
        return copyTask("TextDiffCheck", actual, this);
    }

}
