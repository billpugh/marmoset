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

package edu.umd.cs.marmoset.codeCoverage;

public enum CoverageLevel {
	METHOD, STATEMENT, BRANCH, NONE;
	public CoverageLevel max(CoverageLevel other) {
		if (this.compareTo(other) < 0)
			return other;
		return this;
	}

    public static CoverageLevel fromString(String s) {
        if ("method".equals(s))
            return METHOD;
        if ("statement".equals(s))
            return STATEMENT;
        if ("branch".equals(s))
            return BRANCH;
        return NONE;
    }

	public CoverageLevel min(CoverageLevel other) {
		if (this.compareTo(other) > 0)
			return other;
		return this;
	}

}
