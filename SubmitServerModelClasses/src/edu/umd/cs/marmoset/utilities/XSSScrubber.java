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

/**
 * Created on Jan 21, 2005
 *
 * @author langmead
 */

package edu.umd.cs.marmoset.utilities;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import edu.umd.cs.marmoset.modelClasses.HTML;

/**
 * A utility class for escaping HTML elements and removing CR and LF
 * characters from a string.  This is handy for scrubbing data that
 * will eventually be written into an HTML document or sent in an HTTP
 * response so that it can't trigger a cross-site or response-splitting
 * vulnerability.
 */
public final class XSSScrubber {
    
    public static @HTML <T extends CharSequence> T asHTML(T s) {
        return s;
    }

    
    public static String urlEncodePath(String s) {
    	try {
    	if (s.contains("%2F") || s.contains("$2f"))
    		s = URLDecoder.decode(s, "UTF-8");
    	return URLEncoder.encode(s, "UTF-8");
    	} catch (UnsupportedEncodingException e) {
    		throw new RuntimeException(e);
    	}
    }

    /**
     * 
     * Return an XSS-scrubbed (escaped) version of s as a String.
     * @param s string to scrub
     * @return String version of s after scrubbing
     */
    public static @HTML String scrubbedStr(CharSequence s) {
    	if(s == null) return null;
        return asHTML(scrubbed(s).toString());
    }

    /**
     * Overloaded: Return an XSS-scrubbed (escaped) version of s as a String.
     * @param s string to scrub
     * @param crlf boolean, true to strip line feeds
     * @return String version of s after scrubbing
     */
    public static @HTML  String scrubbedStr(CharSequence s, Boolean crlf) {
      if(s == null) return null;
      return asHTML(scrubbed(s, crlf).toString());
    }

    /**
     * Return an XSS-scrubbed (escaped) version of s as a StringBuffer.
     * Note: remove line feeds by default
     * @param s string to scrub
     * @return StringBuffer version of s after scrubbing
     */
    public static @HTML  StringBuffer scrubbed(CharSequence s) {
      return scrubbed(s, true);
    }

    /**
     * Return an XSS-scrubbed (escaped) version of s as a StringBuffer.
     * @param s string to scrub
     * @param crlf boolean, true to strip line feeds
     * @return StringBuffer version of s after scrubbing
     */
    public static @HTML StringBuffer scrubbed(CharSequence s, boolean crlf) {
        if(s == null) return null;
        StringBuffer sb = new StringBuffer(s.length() + 16);
        for(int i = 0; i < s.length(); i++) {
            if(s.charAt(i) == '<') {
                sb.append("&lt;");
            } else if(s.charAt(i) == '>') {
                sb.append("&gt;");
            } else if(s.charAt(i) == '&') {
                sb.append("&amp;");
            } else if((crlf) &&
                      (s.charAt(i) == '\n' || s.charAt(i) == '\r'))
            {
                // Do nothing; remove it
            } else {
                sb.append(s.charAt(i));
            }
        }
        return asHTML(sb);
    }
}
