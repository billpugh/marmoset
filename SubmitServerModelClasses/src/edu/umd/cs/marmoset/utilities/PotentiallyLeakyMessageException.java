package edu.umd.cs.marmoset.utilities;

public class PotentiallyLeakyMessageException extends SecurityException {

    
    static boolean needsSanitization(Throwable t) {
        String s = t.getMessage();
        if (s.isEmpty()) return false;
        
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
            
        }
        if (nonSpaceWhiteSpace > 2) 
            return true;
        if (numbers > 8)
            return true; 
            
        if (digitCharacters > s.length()/2)
            return true;
        if (spaceCharacters == 0)
            return true;
        if (alphabeticCharacters - nonSpaceWhiteSpace*4 < s.length() * 3 / 4)
            return true;

        return false;
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
