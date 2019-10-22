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

/*
 * Created on Jan 9, 2005
 *
 * @author jspacco
 */
package edu.umd.cs.submitServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.marmoset.modelClasses.Course;
import edu.umd.cs.marmoset.modelClasses.Student;
import edu.umd.cs.marmoset.utilities.MarmosetUtilities;

/**
 * @author jspacco
 *
 */
public class UserSession {
	private @Student.PK Integer studentPK;
	private boolean superUser;
	 private boolean casLogin;
	private boolean capabilitiesActivated = true;
	private Set<Integer> instructorCapabilitySet = new HashSet<Integer>();
	private Set<Integer> instructorActionCapabilitySet = new HashSet<Integer>();
	private boolean backgroundDataComplete = false;
	private String givenConsent;
	private @Course.PK Integer onlyCoursePK;
	
	/* These are only set if the user actually has these identities. */
	private @Student.PK Integer superuserPK = null;
	private @Student.PK Integer shadowAccountPK = null;

	public boolean isCasLogin() {
    return casLogin;
  }

  public void setCasLogin(boolean casLogin) {
    this.casLogin = casLogin;
  }

  public void addInstructorCapability(Integer coursePK) {
		instructorCapabilitySet.add(coursePK);
	}

	public void addInstructorActionCapability(Integer coursePK) {
		instructorCapabilitySet.add(coursePK);
		instructorActionCapabilitySet.add(coursePK);
	}

	public boolean hasInstructorActionCapability(Integer coursePK) {
		return capabilitiesActivated
				&& instructorActionCapabilitySet.contains(coursePK)  || superUser;
	}

	public Map<Integer,Boolean> getInstructorStatus() {
		if (!capabilitiesActivated)
			return Collections.emptyMap();
		return MarmosetUtilities.setAsMap(instructorCapabilitySet);
	}
   public List<Integer> getInstructorCourses() {
	        if (!capabilitiesActivated)
	            return Collections.emptyList();
	        return new ArrayList<Integer>(instructorActionCapabilitySet);
	    }


	public boolean hasInstructorCapability(Integer coursePK) {
		return capabilitiesActivated
				&& instructorCapabilitySet.contains(coursePK) || superUser;
	}

	public boolean canActivateCapabilities() {
		return !instructorCapabilitySet.isEmpty() ||  superUser;
	}

	public boolean getCapabilitiesActivated() {
		return capabilitiesActivated || superUser;
	}

	public void setCapabilitiesActivated(boolean newValue) {
		capabilitiesActivated = newValue;
	}

	/**
	 * @param studentPK
	 *            The studentPK to set.
	 */
	public void setStudentPK(@Student.PK Integer studentPK) {
		this.studentPK = studentPK;
	}

	/**
	 * @return Returns the studentPK.
	 */
	public @Student.PK Integer getStudentPK() {
		return studentPK;
	}

	/**
	 * @return Returns the superUser.
	 */
	public boolean isSuperUser() {
		return superUser;
	}

	/**
	 * @param superUser
	 *            The superUser to set.
	 */
	public void setSuperUser(boolean superUser) {
		this.superUser = superUser;
	}

	/**
	 * @return Returns the backgroundDataComplete.
	 */
	public boolean isBackgroundDataComplete() {
		return backgroundDataComplete;
	}

	/**
	 * @param backgroundDataComplete
	 *            The backgroundDataComplete to set.
	 */
	public void setBackgroundDataComplete(boolean backgroundDataComplete) {
		this.backgroundDataComplete = backgroundDataComplete;
	}

	public String getGivenConsent() {
		return givenConsent;
	}

	public void setGivenConsent(String returnedConsentForm) {
		this.givenConsent = returnedConsentForm;
	}

    public @Course.PK Integer getOnlyCoursePK() {
        return onlyCoursePK;
    }

    public void setOnlyCoursePK(@Course.PK Integer onlyCoursePK) {
        this.onlyCoursePK = onlyCoursePK;
    }
    
    public void setSuperuserPK(@Nonnull @Student.PK Integer superuserPK) {
	    this.superuserPK = superuserPK;
    }
    
    public void setShadowAccountPK(@Nonnull @Student.PK Integer shadowAccountPK) {
	    this.shadowAccountPK = shadowAccountPK;
    }
    
    public @Nullable Integer getShadowAccountPK() {
	    return shadowAccountPK;
    }
    
    public @Nullable Integer getSuperuserPK() {
	    return superuserPK;
    }
}
