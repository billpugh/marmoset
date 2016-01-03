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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="ss" uri="http://www.cs.umd.edu/marmoset/ss"%>

<!DOCTYPE HTML>
<html>
<head>
<ss:headContent title="Instructor view of course ${course.courseName}" />
<style>
tr.reject {background: #f33}
</style>
</head>
<body>
    <ss:header />
    <ss:instructorBreadCrumb />

    <div class="sectionTitle">
        <h1>
        <c:choose>
        <c:when test="${not empty course.url}">
            <a href="${course.url}"><c:out value="${course.fullDescription}"/></a>:
            </c:when>
            <c:otherwise>
            <c:out value="${course.fullDescription}"/>:
            </c:otherwise>
            </c:choose>
            <c:choose>
            <c:when test="${instructorActionCapability}">
             (Instructor View)
             </c:when>
             <c:otherwise>
             (Grader View)
             </c:otherwise>
            </c:choose>
           
        </h1>

        <p class="sectionDescription"><ss:hello/></p>
    </div>

    <div class="projectMenu">
        <a href="#projects">Projects</a> &nbsp;|&nbsp; <a href="#students">Students</a> &nbsp;|&nbsp; <a href="#staff">Staff</a>
        &nbsp;|&nbsp; <a href="#status">Status</a>
        <c:if test="${instructorActionCapability}"> &nbsp;|&nbsp; <a href="#update">Update</a></c:if>
    </div>

    <ss:codeReviews title="Pending Code reviews" />
    
    <c:if test="${not empty requestsForHelp}">
    <h2>Help requests</h2>
    <table>
    <tr>
    <th>Who<th>View<th>Accept<th>Sub #<th>requested
    <c:forEach var="submission" items="${requestsForHelp}"  varStatus="counter">
    <c:set var="studentRegistration" value="${studentRegistrationMap[submission.studentRegistrationPK]}" />
    <c:url var="viewLink" value="/view/instructor/submission.jsp">
    <c:param name="submissionPK" value="${submission.submissionPK}" />
    </c:url>
     <c:url var="codeReviewLink" value="/view/codeReview/index.jsp">
    <c:param name="submissionPK" value="${submission.submissionPK}" />
    </c:url>
    <c:choose>
                <c:when test="${submission.mostRecent}">
                    <c:set var="rowKind" value="r${counter.index % 2}" />
                </c:when>
                <c:otherwise>
                    <c:set var="rowKind" value="ir${counter.index % 2}" />
                </c:otherwise>
            </c:choose>
            <tr class="${rowKind}">
    <td><c:out value="${studentRegistration.fullname}"/>
    <td><a href="${viewLink}">view</a>
    <td><a href="${codeReviewLink}">accept</a>
   <td> ${submission.submissionNumber}
   <td> <fmt:formatDate value="${requestsForHelpTimestamp[submission]}"
                pattern="dd MMM h:mm a" />
            
    </c:forEach>
        </table>
    
    </c:if>

    <c:if test="${not empty pendingRegistrations}">
    <h2 id="requests">Registration Requests
    </h2>
    <c:url var="updateRegistrations" value="/action/instructor/UpdatePendingRegistrations" />
    <form action="${updateRegistrations}" method="POST">
    <input type="hidden" value="${course.coursePK}" name="course" />
    <table id="pending-table">
        <tr>
            <th>Name</th>
            <c:if test="${not empty course.sections}">
            <th>Section</th>
            </c:if>
            <th>Action</th>
        </tr>
        <c:forEach var="student" items="${pendingRegistrations}">
        <tr>
            <td><c:out value="${student.lastname}" />, <c:out value="${student.firstname}" /></td>
            <c:if test="${not empty course.sections}">
            <td>
	            <select name="section-${student.studentPK}" id="section-${student.studentPK}-dropdown">
	            	<c:set var="studentSection" value="${studentSections[student.studentPK]}" />
	            	<c:forEach var="section" items="${course.sections}">
	            	<c:choose>
		            	<c:when test="${studentSection == section}">
		            		<option value="${section}" selected="selected">${section}</option>
		            	</c:when>
		            	<c:otherwise>
		            		<option value="${section}">${section}</option>
		            	</c:otherwise>
	            	</c:choose>
	            	</c:forEach>
	            </select>
            </td>
            </c:if>
            <td>
                <c:set var="radioName" value="request-pk-${student.studentPK}" />
                <input type="radio" name="${radioName}" value="accept" id="accept-${radioName}"/>
                <label for="accept-${radioName}">accept</label>
                &nbsp;&nbsp;
                <input type="radio" name="${radioName}" value="reject" id="reject-${radioName}"/>
                <label for="reject-${radioName}">reject</label>
            </td>
        </tr>
        </c:forEach>
    </table>
    <button id="set-all-accept-button">Set all to "accept"</button>
    <button id="clear-all-button">Clear all</button>
    <input type="submit" value="Submit" />
    </form>
    </c:if>
    
    <h2>
        <a id="projects">Projects</a>
    </h2>
    <p></p>
    <table>
        <tr>
            <th rowspan="2">Project</th>
            <th rowspan="2">Overview</th>
            <th rowspan="2">testing<br> setup</th>
            <th colspan="5">build status</th>
            <th rowspan="2">Visible</th>
            <th rowspan="2">Due</th>
            <th  rowspan="2" class="description">Title</th>
        </tr>
           <tr>
            <th>new</th>
            <th>pending</th>
            <th>complete</th>
            <th>retest</th>
            <th>broken</th>
        </tr>

        <c:forEach var="project" items="${projectList}" varStatus="counter">
        <c:url var="projectLink" value="/view/instructor/project.jsp">
                    <c:param name="projectPK" value="${project.projectPK}" />
                </c:url>
                
        <c:set var="projectURL"><c:out value="${project.url}"/></c:set>
        
            <c:choose>
                <c:when test="${project.visibleToStudents}">
                    <c:set var="rowKind" value="r${counter.index % 2}" />
                </c:when>
                <c:otherwise>
                    <c:set var="rowKind" value="ir${counter.index % 2}" />
                </c:otherwise>
            </c:choose>
            <tr class="${rowKind}">

                <td><c:choose>
                    <c:when test="${not empty projectURL}">
                        <a href="${projectURL}"  title="Project description"><c:out value="${project.projectNumber}"/> </a>
                    </c:when>
                    <c:otherwise>
                        <a href="${projectLink}" title="Project page"><c:out value="${project.projectNumber}"/></a>
                    </c:otherwise>
                    </c:choose>
                </td>

                <td  title="Project page"><a href="${projectLink}"> view </a></td>

                <c:choose>
                    <c:when test="${project.tested}">
                        <c:set var="testedProjects" value="true" />
                        <td><c:choose>
                                <c:when test="${project.testSetupPK > 0}">active</c:when>
                                <c:otherwise>
                                    <c:url var="uploadTestSetupLink" value="/view/instructor/uploadTestSetup.jsp">
                                        <c:param name="projectPK" value="${project.projectPK}" />
                                    </c:url>
                                    <a href="${uploadTestSetupLink}"> upload </a>
                                </c:otherwise>
                            </c:choose></td>


                        <td><c:out value="${buildStatusCount[project]['new']}" /></td>
                        <td><c:out value="${buildStatusCount[project]['pending']}" /></td>
                        <td><c:out value="${buildStatusCount[project]['complete']}" /></td>
                        <td><c:out value="${buildStatusCount[project]['retest']}" /></td>
                        <td><c:out value="${buildStatusCount[project]['broken']}" /></td>
                    </c:when>
                         <c:otherwise>
                        <td colspan="6"><c:out value="${buildStatusCount[project]['accepted']}" /> accepted</td>
                    </c:otherwise>
                </c:choose>

                <td>${project.visibleToStudents}</td>
                <td><fmt:formatDate value="${project.ontime}" pattern="dd MMM, hh:mm a" /></td>
                <td class="description">
                 <c:choose>
                <c:when test="${not empty projectURL}">
                        <a href="${projectURL}"  title="Project description"><c:out value="${project.title}"/> </a>
                    </c:when>

                    <c:otherwise>
                        <a href="${projectLink}" title="Project page"> <c:out value="${project.title}"/></a>
                    </c:otherwise>
                   </c:choose>
                </td>
            </tr>
        </c:forEach>
    </table>

    <c:if test="${instructorActionCapability}">

        <ul>
            <li><c:url var="createProjectLink" value="/view/instructor/createProject.jsp">
                    <c:param name="coursePK" value="${course.coursePK}" />
                </c:url> <a href="${createProjectLink}"> create new project </a></li>
            <li>Import a project <c:url var="importProjectLink" value="/action/instructor/ImportProject" />

                <form id="importProjectForm" enctype="multipart/form-data" method="post" action="${importProjectLink}">
                    Canonical Account: <select name="canonicalStudentRegistrationPK">
                        <c:forEach var="studentRegistration" items="${courseInstructors}">
                            <c:choose>
                                <c:when test="${studentRegistration.studentRegistrationPK == studentRegistrationPK}">
                                    <option value="${studentRegistration.studentRegistrationPK}" selected="selected">
                                      <c:out value="${studentRegistration.classAccount}"/></option>
                                </c:when>
                                <c:otherwise>
                                    <option value="${studentRegistration.studentRegistrationPK}">
                                       <c:out value="${studentRegistration.classAccount}"/></option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </select> <input type="file" name="file" size="40" /> <input type="submit" value="Import project!">
                    <input type="hidden" name="coursePK" value="${course.coursePK}">
                </form>
                <script>
jQuery(document).ready(function ($) {
        $('#importProjectForm input:submit').attr('disabled',true);
        $('#importProjectForm input:file').change(
            function(){
                if ($(this).val()){
                    $('#importProjectForm input:submit').removeAttr('disabled'); 
                }
                else {
                    $('#importProjectForm input:submit').attr('disabled',true);
                }
            });
      
    });
</script></li>
        </ul>
    </c:if>


    <h2>
        <a href="javascript:toggle('studentList')" title="Click to toggle display of students" id="students"> <c:out
                value="${fn:length(justStudentRegistrationSet)}" /> Students
        </a>
    </h2>

    <c:if test="${ fn:length(courseIds) > 0}">
        <c:url var="syncCourseLink" value="/action/instructor/SyncCourse" />
        <p>
        <form id="syncCourseForm" method="post" action="${syncCourseLink}">
            <input type="hidden" name="coursePK" value="${course.coursePK}" /> <input type="submit" name="submit"
                value="Update students from grade server" />
        </form>
    </c:if>

    <div id="studentList" style="display: none">

        <table>
            <tr>
                 <th>Name</th>
                <th>class account</th>
            </tr>
            <c:choose>
            <c:when test="${not empty sections}">
             <c:forEach var="section" items="${sections}" >
             <c:set var="inSection" value="${sectionMap[section]}"/>
           <tr><td colspan="2">${fn:length(inSection)} Students in Section <c:out value="${section}"/></td></tr>
            <c:forEach var="studentRegistration" items="${inSection}" varStatus="counter">
                <tr class="r${counter.index % 2}">
                    <c:url var="studentLink" value="/view/instructor/student.jsp">
                        <c:param name="studentPK" value="${studentRegistration.studentPK}" />
                        <c:param name="coursePK" value="${course.coursePK}" />
                    </c:url>
                     <td class="description"><a href="${studentLink}">
                    <c:out value="${studentRegistration.fullname}"/></a></td>
                    <td><a href="${studentLink}">
                    <c:out value="${studentRegistration.classAccount}"/></a></td>
                </tr>
            </c:forEach>
            </c:forEach>
            </c:when>
            <c:otherwise>
            <c:forEach var="studentRegistration" items="${justStudentRegistrationSet}" varStatus="counter">
                <tr class="r${counter.index % 2}">
                    <c:url var="studentLink" value="/view/instructor/student.jsp">
                        <c:param name="studentPK" value="${studentRegistration.studentPK}" />
                        <c:param name="coursePK" value="${course.coursePK}" />
                    </c:url>
                    <td class="description"><a href="${studentLink}">
                    <c:out value="${studentRegistration.fullname}"/></a></td>
                    <td><a href="${studentLink}">
                    <c:out value="${studentRegistration.classAccount}"/></a></td>
                </tr>
            </c:forEach>
            </c:otherwise>
            </c:choose>
            
        </table>
    </div>


    <c:if test="${not empty inactiveStudentRegistrationSet }">
        <h3>
            <a href="javascript:toggle('inactiveStudentList')" title="Click to toggle display of inactive students">
                <c:out value="${fn:length(inactiveStudentRegistrationSet)}" /> Inactive Students
            </a>
        </h3>

        <div id="inactiveStudentList" style="display: none">

            <p>Registration status is controlled through grades.cs.umd.edu; dropped students will be deleted if they have no submissions or code reviews
            <table>
                <tr>
                    <th>Dropped</th>
                    <th>Inactive</th>
                    <th>Name</th>
                    <th>class account</th>
                    <c:if test="${not empty sections}">
                        <th>Section</th>
                    </c:if>

                </tr>

                <c:forEach var="studentRegistration" items="${inactiveStudentRegistrationSet}" varStatus="counter">
                    <tr class="r${counter.index % 2}">
                        <c:url var="studentLink" value="/view/instructor/student.jsp">
                            <c:param name="studentPK" value="${studentRegistration.studentPK}" />
                            <c:param name="coursePK" value="${course.coursePK}" />
                        </c:url>
                        <td title="registration status is controlled through grades.cs.umd.edu"><input
                            name="dropped" type="checkbox" ${ss:isChecked(studentRegistration.dropped)} disabled /></td>
                        <td title="registration status is controlled through grades.cs.umd.edu"><input
                            name="inactive" type="checkbox" ${ss:isChecked(studentRegistration.inactive)} disabled /></td>

                        <td class="description"><a href="${studentLink}"> <c:out
                                    value="${studentRegistration.fullname}" /></a></td>
                        <td><a href="${studentLink}"> <c:out value="${studentRegistration.classAccount}" /></a></td>
                        <c:if test="${not empty sections}">
                            <td><c:out value="${studentRegistration.section}" /></td>
                        </c:if>
                    </tr>
                </c:forEach>


            </table>
        </div>
    </c:if>



    <h2>
        <a id="staff">Staff</a>
    </h2>
    <table>
        <tr>
            <th>Active</th>
            <th>Name</th>
            <th>class account</th>
            <th>can update course</th>
        </tr>
        <c:forEach var="studentRegistration" items="${staffStudentRegistrationSet}" varStatus="counter">
            <tr class="r${counter.index % 2}">
                <c:url var="studentLink" value="/view/instructor/student.jsp">
                    <c:param name="studentPK" value="${studentRegistration.studentPK}" />
                    <c:param name="coursePK" value="${course.coursePK}" />
                </c:url>
                <td><input name="active" type="checkbox" ${ss:isChecked(studentRegistration.active)}  disabled />
                </td>
                <td class="description"><a href="${studentLink}">
                <c:out value="${studentRegistration.fullname}"/></a></td>
                <td><a href="${studentLink}">
                <c:out value="${studentRegistration.classAccount}"/></a></td>

                <c:set var="modifyPermission" value="${studentRegistration.instructorCapability == 'modify'}" />
                <td><input name="modify" type="checkbox" ${ss:isChecked(modifyPermission)}  disabled /></td>

            </tr>
        </c:forEach>
    </table>

<h2><a id="status">Status</a></h2>


     <c:url var="courseCalendarLink" value="/feed/CourseCalendar">
                            <c:param name="courseKey" value="${course.courseKey}" />
                        </c:url>
    <ul>
    <li><a href="${courseCalendarLink}">Course calendar link</a> (iCalendar .ics format: 
    subscribe in iCal, Google calendar, or Outlook)

    <li>Server load: ${systemLoad}
    
    <c:url var="createBuildserverConfig" value="/action/instructor/CreateBuildserverConfigFile">
                    <c:param name="buildserverCourse">${course.coursePK}</c:param>
                </c:url>
    <c:url var="zippedBuildServer" value="/resources/buildserver.zip"/>
                                  <li><a href="${createBuildserverConfig}">Generate buildserver config file to run your own
                        buildserver</a>
                <li><a href="${zippedBuildServer}">Zip archive of build server</a>
  <c:choose>
        <c:when test="${empty buildServers}">
            <li>
                <c:if test="${testedProjects}">
                    <b>WARNING</b>
                </c:if>
                There are no recent build servers for your course.
           
               
        </c:when>
        <c:otherwise>
            <li>
                There are ${fn:length(buildServers)} recent build servers that can build code for your course. (<a
                    href="javascript:toggle('buildServers')">details</a>)
                
            <div id="buildServers" style="display: none">
                <p>
                <table>
                    <tr>
                        <th>Host
                        <th>Last request
                        <th>Last success
                        <th>Load</th>
                    </tr>

                    <c:forEach var="buildServer" items="${buildServers}" varStatus="counter">
                        <tr class="r${counter.index % 2}">
                            <td><c:out value="${buildServer.name}" />
                            <td><fmt:formatDate value="${buildServer.lastRequest}" pattern="dd MMM, hh:mm a" /></td>
                            <td><fmt:formatDate value="${buildServer.lastSuccess}" pattern="dd MMM, hh:mm a" /></td>
                            <td><c:out value="${buildServer.load}" /></td>
                        </tr>
                    </c:forEach>
                </table>

               
            </div>
        </c:otherwise>
    </c:choose>
    </ul>

    <c:if test="${instructorActionCapability}">
        <h2>
            <a id="update" >Update</a>
        </h2>
        <c:url var="updateCourseLink" value="/action/instructor/UpdateCourse" />
        <c:set var="currentURL">
            <c:out value="${course.url}" />
        </c:set>
        <c:set var="currentDescription">
            <c:out value="${course.description}" />
        </c:set>
        <form action="${updateCourseLink}" method="post" name="updateCourseForm">
            <input type="hidden" name="coursePK" value="${course.coursePK}">

            <table class="form">
                <tr>
                    <th colspan="2">Update course info</th>
                </tr>
                <tr>
                    <td class="label">Course Name:</td>
                    <td class="input"><c:out value="${course.courseName}" /></td>
                </tr>
                <c:if test="${course.section}">
                 <tr>
                    <td class="label">Section:</td>
                    <td class="input"><c:out value="${course.section}" /></td>
                </tr>
                </c:if>
                <tr>
                    <td class="label">Description:</td>
                    <td class="input"><input type="text" name="description" size="60" value="${currentDescription}"
                        placeholder="course description (optional)" /></td>
                </tr>
                <tr>
                    <td class="label">URL:</td>
                    <td class="input"><input type="url" name="url" size="60" value="${currentURL}"
                        placeholder="course web page (optional)" /></td>
                </tr>
                <tr>
                    <td class="label">allows baseline/starter code download:</td>
                    <td class="input"><input name="download" type="checkbox" ${ss:isChecked(course.allowsBaselineDownload)}  />
                    </td>
                </tr>
                 <tr>
                    <td class="label">allows help requests:</td>
                    <td class="input"><input name="helpRequests" type="checkbox" ${ss:isChecked(course.allowsHelpRequests)}  />
                    </td>
                </tr>
                        <tr>
            <td class="label">Use default buildservers:</td>
            <td class="input"><input name="useDefaultBuildservers" type="checkbox"  ${ss:isChecked(course.useDefaultBuildservers)}  />
            </td>
        </tr>

                  <tr>
           <td class="label">Editing source code in browser:</td>
            <td class="input">
            <select name="browserEditing">
            <option value="prohibited"  ${ss:selected(course.browserEditing, 'PROHIBITED')}>Prohibited</option>
            <option value="discouraged"  ${ss:selected(course.browserEditing, 'DISCOURAGED')}>Discouraged</option>
            <option value="allowed"  ${ss:selected(course.browserEditing, 'ALLOWED')}>Allowed</option>
            </select>
            </td>
        </tr>
                <tr class="submit">
                    <td colspan="2"><input type="submit" value="Update course"></td>
                </tr>
            </table>

        </form>

        <ul>
            <li><c:url var="studentAccountForInstructorLink" value="/action/instructor/StudentAccountForInstructor" />
                <form action="${studentAccountForInstructorLink}" method="post" name="studentAccountForInstructorForm">
                    <input type="hidden" name="coursePK" value="${course.coursePK}" />
                    <c:choose>
                    <c:when test="${not empty pseudoStudentRegistration}">
                     Login as 
                     </c:when>
                     <c:otherwise>
                     Create and login as
                     </c:otherwise>
                     </c:choose>
                     pseudo-student
                    account for instructor
                    <c:out value="${studentRegistration.fullname}" />
                    <input type="submit" value="Go">
                </form></li>
            <c:if test="${gradesServer}">
            <li><c:url var="registerStudentsLink" value="/view/instructor/registerStudents.jsp">
                    <c:param name="coursePK" value="${course.coursePK}" />
                </c:url> <a href="${registerStudentsLink}"> Register students for this course by uploading a text file </a></li>
                </c:if>
                <c:if test="${instructorActionCapability}">
            <li><c:url var="registerPersonLink" value="/view/instructor/registerPerson.jsp">
                    <c:param name="coursePK" value="${course.coursePK}" />
                </c:url> <a href="${registerPersonLink}"> Register one person course using a web interface</a></li>
                </c:if>
        </ul>
    </c:if>

    <c:if test="${! empty hiddenProjects }">

        <h2>
            <a id="hiddenProjects">Hidden Projects</a>
        </h2>
        <p></p>
        <table>
            <tr>
                <th>Project</th>
                <th>Overview</th>
                <th>unhide
                <th class="description">Title</th>
            </tr>

            <c:forEach var="project" items="${hiddenProjects}" varStatus="counter">
                <c:choose>
                    <c:when test="${project.visibleToStudents}">
                        <c:set var="rowKind" value="ir${counter.index % 2}" />
                    </c:when>
                    <c:otherwise>
                        <c:set var="rowKind" value="r${counter.index % 2}" />
                    </c:otherwise>
                </c:choose>
                    <tr class="r${counter.index % 2}">

                    <td><c:out value="${project.projectNumber}"/></td>

                    <td><c:url var="projectLink" value="/view/instructor/project.jsp">
                            <c:param name="projectPK" value="${project.projectPK}" />
                        </c:url> <a href="${projectLink}"> view </a></td>

                    <td><c:url var="makeVisibleHidden" value="/action/instructor/MakeProjectHidden" />
                        <form method="post" action="${makeVisibleHidden}">
                            <input type="hidden" name="projectPK" value="${project.projectPK}" /> <input type="hidden"
                                name="newValue" value="false" /> <input type="submit" value="Unhide"
                                style="color: #003399" />
                        </form></td>

                    <td class="description">><c:out value="${project.title}"/></td>
                </tr>
            </c:forEach>
        </table>
    </c:if>
    
   
    
    <ss:footer />
	<script type="text/javascript">
    window.$marmoset = {
    	acceptRadios: $("#pending-table").find('input[type="radio"][value="accept"]'),
    	allRadios: $("#pending-table").find('input[type="radio"]'),
    	allRows: $("#pending-table tr")
    };
    
    $("#set-all-accept-button").click(function(event) {
    	$marmoset.acceptRadios.attr('checked', true);
    	$marmoset.allRows.removeClass("reject");
    	event.preventDefault();
    });
    
    $("#clear-all-button").click(function(event) {
    	$marmoset.allRadios.attr('checked', false);
    	$marmoset.allRows.removeClass("reject");
    	event.preventDefault();
    });
    
    $marmoset.allRadios.change(function(event) {
    	var $target = $(event.target);
    	$parents = $target.parents("tr");
    	if ($target.val() == 'reject') {
    		$parents.addClass('reject');
    	} else {
    		$parents.removeClass('reject');
    	}
    });
    </script>
</body>
</html>
