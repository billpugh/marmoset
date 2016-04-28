<%--

 Marmoset: a student project snapshot, submission, testing and code review
 system developed by the Univ. of Maryland, College Park
 
 Developed as part of Jaime Spacco's Ph.D. thesis work, continuing effort led
 by William Pugh. See http://marmoset.cs.umd.edu/
 
 Copyright 2005 - 2011, Univ. of Maryland
 
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain a copy of
 the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 License for the specific language governing permissions and limitations under
 the License.

--%>

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="ss" uri="http://www.cs.umd.edu/marmoset/ss"%>


<!DOCTYPE HTML>
<html>
<head>
<ss:headContent title="Create new project for ${course.courseName}" />
<c:url var="jsBase" value="/js" />

<link
	href="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.7/themes/base/jquery-ui.css"
	rel="stylesheet" type="text/css" />
<script
	src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.7/jquery-ui.min.js"
	type="text/javascript"></script>

 <link href="${jsBase}/timePicker.css"  rel="stylesheet" type="text/css" />
<script src="${jsBase}/jquery.timePicker.js" type="text/javascript"></script>


<script type="text/javascript">
$(function() {
	 $( "#ontime-date" ).datepicker({
	        defaultDate: +10,
	        minDate: -1,
	        dateFormat: "yy-mm-dd",
	        onSelect: function( selectedDate ) {
	            var instance = $( this ).data( "datepicker" ),
	                date = $.datepicker.parseDate(
	                    instance.settings.dateFormat ||
	                    $.datepicker._defaults.dateFormat,
	                    selectedDate, instance.settings );
	            $( "#late-date" ).datepicker( "option", "minDate", date );
	            $( "#ontime-time").focus();
	        }
	    });
	   $( "#late-date" ).datepicker({
           defaultDate: +10,
           minDate: -1,
           dateFormat: "yy-mm-dd",
           onSelect: function( selectedDate ) {
               var instance = $( this ).data( "datepicker" ),
                   date = $.datepicker.parseDate(
                       instance.settings.dateFormat ||
                       $.datepicker._defaults.dateFormat,
                       selectedDate, instance.settings );
               $( "#ontime-date" ).datepicker( "option", "maxDate", date );
               $( "#late-time").focus();
           }
       });
   
  
});


function changedIsTested(isTested)
{
	if (isTested) {
		showItem("tested");
	} else {
		hideItem("tested");
	}
}

  $(function() {
    
    $("#ontime-time").timePicker({
    	  show24Hours: false,
    	  step: 60,
    	  defaultTime: "06:00 PM"});
    $("#late-time").timePicker({
    	  show24Hours: false,
    	  step: 60,
    	  defaultTime: "06:00 PM"});
    
  });
</script>
</head>

<body>
<ss:header />
<ss:instructorBreadCrumb />

<div class="sectionTitle">
	<h1>New Project</h1>

	<p class="sectionDescription">Fill out the following form to create
	a new project for ${course.courseName}</p>
</div>

<p>

 <%--
	Previous projects names in this course:
	<c:forEach var="project" items="${projectList}">
	${project.projectNumber}
	<p>
	</c:forEach>
	--%>

<form class="form"
	action="<c:url value="/action/instructor/CreateProject"/>"
	method="POST" name="createProjectForm">

	<input type="hidden" name="coursePK" value="${course.coursePK}">
	<input type="hidden" name="postDeadlineOutcomeVisibility" value="nothing"/>
	<input type="hidden" name="visible" value="no">


<table class="form">

<tfoot>
    <tr  class="submit">
        <td colspan=2><input type=submit value="Create Project"></td>
    </tr>
    </tfoot>
<tbody>
	<tr title="should be very short; doesn't have to be a number, probably shouldn't have spaces">
		<td class="label">project Number</td>
		<td class="input"><INPUT TYPE="text" NAME="projectNumber" size="8" required></td>
	</tr>
	<tr>
		<td class="label"><label for="ontime">on-time deadline</label></td>
		<td class="input"><INPUT TYPE="text" id="ontime-date" NAME="ontime-date" PLACEHOLDER="yyyy-mm-dd" size="12" required>
		<INPUT TYPE="text" id="ontime-time" NAME="ontime-time" PLACEHOLDER="hh:mm aa" size="12"  ></td>
	</tr>
	<tr title="leave both blank for no late deadline">
		<td class="label"><label for="late">late deadline</label></td>
		<td class="input"><INPUT TYPE="text" id="late-date" NAME="late-date" PLACEHOLDER="yyyy-mm-dd" size="12" >
        <INPUT TYPE="text" id="late-time" NAME="late-time" PLACEHOLDER="hh:mm aa" size="12" title="blank for one second before midnight"> </td>
	</tr>
	<tr>
		<td class="label">project title</td>
		<td class="input"><INPUT TYPE="text" NAME="title" size="60" required="required"></td>
	</tr>
	<tr>
		<td class="label" >URL</td>
		<td class="input"><INPUT TYPE="url" NAME="url" size="60"></td>
	</tr>
	<tr>
		<td class="label">description</td>
		<td class="input"><INPUT TYPE="text" NAME="description" size="90"></td>
	</tr>
    <tr>
    <td class="label">Submission editing in browser</td>
    <td class="input">
         <select name="browserEditing">
            <option value="prohibited"  ${ss:selected(course.browserEditing, 'PROHIBITED')}>Prohibited</option>
            <option value="discouraged"  ${ss:selected(course.browserEditing, 'DISCOURAGED')}>Discouraged</option>
            <option value="allowed"  ${ss:selected(course.browserEditing, 'ALLOWED')}>Allowed</option>
            </select>
            </td>
    </tr>
<tr 
    title="Submissions from the canoonical account are used to check test setups and to provide baseline/starter snapshots of projects">
    
        <td class="label">canonical account</td>
        <td class="input"><select name="canonicalStudentRegistrationPK">
            <c:set var="first" value="true"/>
            <c:forEach var="studentRegistration" items="${courseInstructors}">
                <c:choose>
                   <c:when test="${studentRegistration.studentRegistrationPK == studentRegistrationPK || first}">
                    <option value="${studentRegistration.studentRegistrationPK}" selected="selected">
                   <c:out value="${studentRegistration.fullname}"/> </option>
                   <c:set var="first" value="false"/>
                    </c:when>
                    <c:otherwise>
                    <option value="${studentRegistration.studentRegistrationPK}">
                   <c:out value="${studentRegistration.fullname}"/> </option>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
        </select>
        </td>
    </tr>
    <tr title="Student submissions are compared against either the baseline submission for this project, or against the students last submission from another project. The comparison is used to highlight changes text, and elide long sections of unchanged files. ">
    <td class="label">Diff against
        </td>
        <td class="input">
        <select>
        <option value="0" selected="selected">Baseline submission (if any)</option>
        <c:forEach var="p" items="${projectList}">
        <c:if test="${p.visibleToStudents}">
        <option value="${p.projectPK}"><c:out value="${p.projectNumber}"/>:
<c:out value="${p.title}"/></option>
        </c:if>
        </c:forEach>
        </select>
	<tr>
		<td class="label">Pair project
		</td>
		<td class="input"><INPUT TYPE="CHECKBOX" NAME="pair" ></td>
	</tr>

	<tr>
		<td class="label">Tested on server<br>
		</td>
		<td class="input"><INPUT TYPE="CHECKBOX" NAME="tested" CHECKED
		  ONCHANGE="changedIsTested(this.checked)"
		></td>
	</tr>
</tbody>

</table>

<div id="tested">

<table class="form">
<thead>
  <tr  class="submit">
        <th colspan=2>Test options</th>
    </tr>
    </thead>

<tbody>	<tr>
		<td class="label">kind of late penalty:</td>
		<td class="input"><select name="kindOfLatePenalty">
			<option selected>constant</option>
			<option>multiplier</option>
		</select></td>
	</tr>

	<tr>
		<td class="label" title="How many points	to subtract
			from a late submission">
			Late Constant
		</td>
		<td class="input"><input type="text" name="lateConstant" value="20" size="5"/></td>
	</tr>

	<tr>
		<td class="label" title="Fraction by which to multiply
			a late submission">
			Late Multiplier
		</td>
		<td class="input"><input type="text" name="lateMultiplier" value="0.8" size="5"/></td>
	</tr>

	<tr title="Set to 'everything' if you want  students to see all outcomes (public, release and secret)  after the deadline passes.">
		<td class="label">
		Post-Deadline Outcome Visibility
		</td>
		<td class="input">
		<input type="radio" name="postDeadlineOutcomeVisibility" value="everything" ${ss:checkedOrNull(project.postDeadlineOutcomeVisibility,'everything')}> Everything<br>
		<input type="radio" name="postDeadlineOutcomeVisibility" value="nothing" ${ss:checked(project.postDeadlineOutcomeVisibility,'nothing')}> Nothing<br>
		</td>
	</tr>

	<tr title="How much information to reveal for a release test">
		<td class="label" >release test stack trace policy<br>
		</td>
		<td class="input">
		<input type="radio" name="stackTracePolicy" value="test_name_only" checked="checked">
		name of test only (default)
		<br>
		<input type="radio" name="stackTracePolicy" value="exception_location">
		the line number in student's code where exception happens
		<br>
		<input type="radio" name="stackTracePolicy" value="restricted_exception_location">
		exception line number, if it's covered by a student or public test
		<br>
		<input type="radio" name="stackTracePolicy" value="full_stack_trace">
		the entire stack trace for Java or full output printed to stdout for C
		<br>
		</td>
	</tr>
	<tr title="which submissions can be release tested">
		<td class="label">release test policy
		</td>
		<td class="input">
			<select name="releasePolicy">
			<option value="after_public">submissions that pass all public tests</option>
			<option value="anytime">any submission</option>
			</select>
	</tr>

	<tr>
		<td class="label" title="Determines how the submission to be graded is chosen from each category (on-time and late)">best submission policy</td>
		<td class="input">
		<input type="radio" name="bestSubmissionPolicy" value="edu.umd.cs.submitServer.policy.ChooseLastSubmissionPolicy" checked="checked">
		Last compilable submission.
		<br>
		<input type="radio" name="bestSubmissionPolicy" value="edu.umd.cs.submitServer.policy.ChooseHighScoreSubmissionPolicy">
		Highest scoring submission
		<br>
		<input type="radio" name="bestSubmissionPolicy" value="edu.umd.cs.submitServer.policy.ReleaseTestAwareSubmissionPolicy">
		Either last compilable submission, or highest scoring release tested submission, whichever is higher
		</td>
	</tr>
	<tr  title="How many release tests to reveal the students when they use a token">
		<td class="label"># release tests to reveal<br>
		</td>
		<td class="input">

			<select name="numReleaseTestsRevealed">
			<OPTION>1</OPTION>
			<OPTION selected value="2">2</OPTION>
			<OPTION>3</OPTION>
			<OPTION>4</OPTION>
			<OPTION>5</OPTION>
			<OPTION>6</OPTION>
			<OPTION>7</OPTION>
			<OPTION>8</OPTION>
			<OPTION>9</OPTION>
			<OPTION>10</OPTION>
			<option value="-1">all</option>
			</select>
		</td>
	</tr>
	<tr>
		<td class="label">number of release tokens</td>
		<td class="input"><input name="releaseTokens"
         type="number"
         min="1"
         max="5"
         value="3" size="4"></td>
	</tr>

	<tr>
		<td class="label">regeneration time (hours)</td>
		<td class="input"><SELECT NAME="regenerationTime">
			<OPTION>12</OPTION>
			<OPTION SELECTED>24</OPTION>
			<OPTION>36</OPTION>
			<OPTION>48</OPTION>
		</SELECT></td>
	</tr>
	</tbody>
	</table>

</div>

</form>
<ss:footer />
</body>
</html>
