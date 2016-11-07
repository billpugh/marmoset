package edu.umd.cs.marmoset.utilities;

public class PotentiallyLeakyMessageException extends SecurityException {

    
    static boolean needsSanitization(Throwable t) {
        
//        try (PrintWriter out =  new PrintWriter(
//                new FileWriter("/tmp/sanitationLog.txt", true))) {
        String s = t.getMessage();
        if (s.isEmpty()) return false;
//        t.printStackTrace(out);
//        out.println(s);
        
        char firstChar = s.charAt(0);
        if (!Character.isAlphabetic(firstChar) && !Character.isDefined(firstChar))
            return true;
        int alphabeticCharacters = 0;
        int digitCharacters = 0;
        int numbers = 0;
        int spaceCharacters = 0;
        int nonSpaceWhiteSpace = 0;
        char prevChar = ' ';
       
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') spaceCharacters++;
            else if (Character.isAlphabetic(c)) alphabeticCharacters++;
            else if (Character.isDigit(c)) {
                if (!Character.isDigit(prevChar)) numbers++;
                digitCharacters++;
            }
            else if (Character.isWhitespace(c)
                    || Character.isISOControl(c)) nonSpaceWhiteSpace++;
            prevChar = c;
        }
        
//        out.printf("%d %d %d %d %d%n", s.length(), 
//                nonSpaceWhiteSpace, numbers, digitCharacters, spaceCharacters);
        if (nonSpaceWhiteSpace > 2) 
            return true;
//        out.println("OK1");
        if (numbers > 10)
            return true; 
//        out.println("OK2");
        if (digitCharacters > s.length()/2)
            return true;
//        out.println("OK3");
        if (spaceCharacters == 0)
            return true;
//        out.println("OK4");
        if (alphabeticCharacters - nonSpaceWhiteSpace*4 < s.length() * 3 / 4)
            return true;
//        out.println("OK5");
        return false;
//        } catch (IOException e){
//        return false;
//    }
    }
    
    public static Throwable sanitize(Throwable original) {
        Throwable t = original;
        while (t != null) {
            if (needsSanitization(t))
                return new PotentiallyLeakyMessageException(t);
            t = t.getCause();
        }
        return original;
    }

    private static final long serialVersionUID = 1L;
    
    
    private  PotentiallyLeakyMessageException(Throwable t) {
        super(t.getClass().getSimpleName() + " which had potentially leaky message");
        setStackTrace(t.getStackTrace());
    }
    

}
