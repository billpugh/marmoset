/**
 * Marmoset: an automated snapshot, submission and testing system
 * Copyright (C) 2005, University of Maryland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Email: marmoset-discuss@cs.umd.edu
 * 
 * Snail mail:
 * The Marmoset Project
 * Attention: Dr. Bill Pugh
 * Dept. of Computer Science
 * Univ. of Maryland
 * College Park, MD 20742
 */

/*
 * Created on Jan 24, 2005
 */
package edu.umd.cs.submit;

import java.util.regex.Pattern;

/**
 * @author pugh
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class FilesToIgnore {

    private static final String baseIgnoredPatterns = "RCS SCCS CVS .git CVS.adm RCSLOG cvslog.* tags TAGS .make.state .nse_depinfo"
            + " *~ #* .#* ,* _$* *$ *.old *.bak *.BAK *.orig *.rej .del-* *.a *.olb *.o *.obj *.so *.exe *.Z *.elc *.ln *.class core";

    StringBuffer patternBuffer;

    public FilesToIgnore() {
        patternBuffer = new StringBuffer(baseIgnoredPatterns.replace(' ', '|'));

    }

    public void addPattern(String p) {
        patternBuffer.append('|');
        patternBuffer.append(p);
    }

    public Pattern getPattern() {
        String patternString = patternBuffer.toString();
        // System.out.println(patternString);
        patternString = patternString.replaceAll("\\$", "\\\\\\$");
        patternString = patternString.replaceAll("\\.", "\\\\.");
        patternString = patternString.replaceAll("\\*", "\\.\\*");
        patternString = patternString.replaceAll("\\?", "\\.");
        return Pattern.compile(patternString);
    }

    public static void main(String[] args) {
        FilesToIgnore tmp = new FilesToIgnore();
        tmp.addPattern("foo");
        tmp.addPattern("foo?");
        tmp.addPattern("foo$");
        tmp.addPattern("foo.bar");
        tmp.addPattern("foo.*");
        String[] pp = tmp.getPattern().pattern().split("\\|");
        for (int i = 0; i < pp.length; i++)
            System.out.println(pp[i]);

    }
}