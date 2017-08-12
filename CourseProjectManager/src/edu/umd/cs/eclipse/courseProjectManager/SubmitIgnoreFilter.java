package edu.umd.cs.eclipse.courseProjectManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

class SubmitIgnoreFilter {
        private final Collection<String> filters = new LinkedHashSet<String>();

        private static final String[] DEFAULTS = { ".", "..", "core", "RCSLOG", "tags", "TAGS", "RCS", "SCCS",
                ".make.state", ".nse_depinfo", "#*", ".#*", "cvslog.*", ",*", ".git", "CVS", "CVS.adm", ".del-*", "*.a",
                "*.olb", "*.o", "*.obj", "*.so", "*.Z", "*~", "*.old", "*.elc", "*.ln", "*.bak", "*.BAK", "*.orig",
                "*.rej", "*.exe", "*.dll", "*.pdb", "*.lib", "*.ncb", "*.ilk", "*.exp", "*.suo", ".DS_Store", "_$*",
                "*$", "*.lo", "*.pch", "*.idb", "*.class", "~*" };
//        private static final String[] DEFAULTS1 = {  "*.class" };

        SubmitIgnoreFilter() {
            for(String p : DEFAULTS) {
                addFilter(p);
            }
        }
        /**
         * Adds a new filter represented by the given string.
         * 
         * @param filterString
         *            the String representing the types of files to filter
         */
        void addFilter(String filterString) {
            
            filterString = filterString.replace("$", "\\$");
            filterString = filterString.replace(".", "\\.");
            filterString = filterString.replace("*", ".*");
            filterString = "^(.*/)*" + filterString;
            filters.add(filterString);
        }

        /**
         * Checks if a filename should be filtered out because it matches one of
         * the rules in the SubmitIgnore object (which was created from a
         * .submitIgnore file).
         * 
         * @param filename
         *            the filename to try to match
         * @return true if the file should be filtered, false otherwise
         */
        boolean matches(String filename) {
            for (String regexp : filters) {
                if (filename.matches(regexp))
                    return true;
            }
            return false;
        }

        public String toString() {
            StringBuffer result = new StringBuffer();
            for (Iterator<String> ii = filters.iterator(); ii.hasNext();) {
                result.append(ii.next() + "\n");
            }
            return result.toString();
        }

        /**
         * Gets an iterator over the filter strings.
         * 
         * @return an iterator over the filter strings
         */
        Iterator<String> iterator() {
            return filters.iterator();
        }

        /**
         * Static factory method that creates a SubmitIgnoreFilter from a file.
         * 
         * @param filename
         *            the name of the file to use to create the
         *            SubmitIgnoreFilter.
         * @return a submitIgnoreFilter
         * @throws IOException
         *             if there is an error reading the file
         */
        static SubmitIgnoreFilter createSubmitIgnoreFilterFromFile(String filename) throws IOException {
            SubmitIgnoreFilter submitIgnoreFilter = new SubmitIgnoreFilter();

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(filename));

                String filterString;
                while ((filterString = TurninProjectAction.readLine(reader)) != null) {
                    submitIgnoreFilter.addFilter(filterString);
                }
                return submitIgnoreFilter;
            } finally {
                if (reader != null)
                    reader.close();
            }
        }
    }