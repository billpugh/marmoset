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
	<ss:head
		title="Project Utilities for Project ${project.projectNumber} in ${course.courseName}" />
	<body>
	<ss:header />
	<ss:instructorBreadCrumb />

	<ss:projectTitle/>
	<ss:projectMenu/>


<script>
jQuery(document).ready(function ($) {
        $('#uploadTestSetupForm input:submit').attr('disabled',true);
        $('#uploadTestSetupForm input:file').change(
            function(){
                if ($(this).val()){
                    $('#uploadTestSetupForm input:submit').removeAttr('disabled'); 
                }
                else {
                    $('#uploadTestSetupForm input:submit').attr('disabled',true);
                }
            });
        $('#submitProjectForm input:submit').attr('disabled',true);
        $('#submitProjectForm input:file').change(
            function(){
                if ($(this).val()){
                    $('#submitProjectForm input:submit').removeAttr('disabled'); 
                }
                else {
                    $('#submitProjectForm input:submit').attr('disabled',true);
                }
            });
    });
</script>

<h2>Instructor Utilities for Project 
<c:out value="${project.fullTitle}"/></h2>


<c:if test="${empty studentRegistrationSet}">
	<c:url var="createDotSubmitFileLink" value="/data/instructor/CreateDotSubmitFile">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>

<p>No submissions.
		<a href="${createDotSubmitFileLink}">Get .submit file for this project </a></p>

</c:if>
<c:url var="submitProjectLink"
                    value="/view/submitProject.jsp">
                    <c:param name="projectPK" value="${project.projectPK}" />
                    <c:param name="testSetupPK" value="${project.testSetupPK}" />
                </c:url>
<p> <a href="${submitProjectLink}"> web submission</a></p>


	<c:if test="${empty allTestSetups}">
			<p> <c:url
				var="uploadTestSetupLink" value="/action/instructor/UploadTestSetup" />
			<form id="uploadTestSetupForm" action="${uploadTestSetupLink}"
				enctype="multipart/form-data" method="POST"><input
				type="hidden" name="projectPK" value="${project.projectPK}">
		No test setups for this project. <input type="hidden" name="comment" value=""/>
		<input type="submit" value="Upload"><a title="zip/jar file to upload"><input type="file" name="file"
				size=40></a> </form>

</c:if>


<h4>Canonical Submissions</h4>
			<c:choose>
				<c:when test="${not empty canonicalSubmissions}">
					<form id="submitProjectForm" enctype="multipart/form-data"
						action="
	<c:url value="/action/SubmitProjectViaWeb"/>"
						method="POST">
						<input type="hidden" name="projectPK" value="${project.projectPK}" />


						<input type="hidden" name="studentRegistrationPK"
							value="${canonicalAccount.studentRegistrationPK}" /> <input
							type="hidden" name="studentPK"
							value="${canonicalAccount.studentPK}" /> <input type="hidden"
							name="submitClientTool" value="web" /> <input type="hidden"
							name="isCanonicalSubmission" value="true" />

						<table class="form">
							<tr>
								<th>#</th>
								<th># inconsistent<br>background<br>retests</th>
								<th>timestamp</th>
								<th>public</th>
								<th>release</th>
								<th>secret</th>
							</tr>
							<c:forEach var="submission" items="${canonicalSubmissions}"
								varStatus="counter">
								<tr class="r${counter.index % 2}">
									<c:url var="submissionLink"
										value="/view/instructor/submission.jsp">
										<c:param name="submissionPK"
											value="${submission.submissionPK}" />
									</c:url>
									<td>${submission.submissionNumber}</td>
									<td></td>
									<td><a href="${submissionLink}"> <fmt:formatDate
												value="${submission.submissionTimestamp}"
												pattern="dd MMM, hh:mm a" /> </a>
									</td>
                                    <c:choose>
                                    <c:when test="${submission.buildStatus == 'COMPLETE'}">
									<td>${submission.valuePublicTestsPassed}</td>
									<td>${submission.valueReleaseTestsPassed}</td>
									<td>${submission.valueSecretTestsPassed}</td>
                                    </c:when>
                                    <c:otherwise>
                                    <td colspan="3"><c:out value="${submission.buildStatus}"/></td>
                                    </c:otherwise>
                                    </c:choose>
								</tr>
							</c:forEach>


							<tr class="submit">
								<th class="label" colspan=2><a
									title="Any instructor can upload a canonical submission through this interface; you do not need to own the canonical account (and have the password) to make a canonical submission through this web interface.">Upload</a>
								</th>
								<td class="input" colspan="4">
                                <input type="submit" value="Submit" title="Any instructor can upload a canonical submission through this interface; you do not need to own the canonical account (and have the password) to make a canonical submission through this web interface.">
                                <input type="file" required="required"
										name="file" size=40 title="zip/jar file to upload"/>
								
								
								</td>
							</tr>

						</table>
					</form>

				</c:when>
				<c:otherwise>
					<c:url var="updateProjectLink"
						value="/view/instructor/updateProject.jsp">
						<c:param name="projectPK" value="${project.projectPK}" />
					</c:url>

					<p>
						No canonical submissions. The canonical account for this project
						is <c:out value="${canonicalAccount.fullname}" />.
						The canonical account can be changed via 
						<a href="${updateProjectLink}"> the project update screen</a>. New
						test setups are checked against the most recent submission from
						the canonical account for the project.
					</p>
				</c:otherwise>
			</c:choose>
			<c:if test="${not empty allTestSetups}">
				<h4>Testing setups</h4>
            <c:url var="uploadTestSetupLink"
                        value="/action/instructor/UploadTestSetup" />
            <form id="uploadTestSetupForm" action="${uploadTestSetupLink}"
                        enctype="multipart/form-data" method="POST">
                        <input type="hidden" name="projectPK" value="${project.projectPK}">
				<p>
					<table class="form">
					<tr>
				<th>Version</th>
				<th>Tested</th>
				<th>Comment</th>
				<th>status</th>

				<th>activate/<br>inactivate</th>

				<th title="Broken test-setups are never retested. Not usually necessary for test-setups marked 'failed'.">Mark test-setup<br> broken.</th>
                <th>Retest</th>
				<th>Download</th>
			</tr>
			<c:forEach var="testSetup" items="${allTestSetups}"
						varStatus="counter">
				<c:if test="${testSetup.status != 'BROKEN'}">
				<c:choose>
					<c:when test="${testSetup.status == 'ACTIVE'}">
					<tr class="highlight">
					
								</c:when>
					<c:otherwise>
					<tr class="r${counter.index % 2}">
					
								</c:otherwise>
				</c:choose>
					<td><c:if test="${testSetup.status == 'TESTED' || testSetup.status == 'INACTIVE'}">
					${testSetup.version}
					</c:if>
					</td>

					<td class="description"><fmt:formatDate
									value="${testSetup.datePosted}" pattern="dd MMM, hh:mm a" />
					</td>

					<td class="description">${testSetup.comment}</td>

					<td>
					<c:choose>
						<c:when test="${testSetup.status != 'NEW' && testSetup.status != 'PENDING'}">

							<c:url var="canonicalRunLink"
											value="/view/instructor/submission.jsp">
								<c:param name="testRunPK" value="${testSetup.testRunPK}" />
							</c:url>
							<a href="${canonicalRunLink}">${testSetup.status.toString()}</a>
						</c:when>
						<c:otherwise>${testSetup.status.toString()}</c:otherwise>
					</c:choose>
					</td>

					<td>
					<c:choose>
						<c:when test="${testSetup.status == 'TESTED'}">
							<c:url var="assignPointsLink"
											value="/view/instructor/assignPoints.jsp">
								<c:param name="testRunPK" value="${testSetup.testRunPK}" />
							</c:url>
							<a href="${assignPointsLink}">assign points</a>
						</c:when>
						<c:when test="${testSetup.status == 'ACTIVE' && instructorActionCapability}">
							<c:url var="deactivateLink"
											value="/action/instructor/ChangeTestSetupStatus">
								<c:param name="testSetupPK" value="${testSetup.testSetupPK}" />
								<c:param name="status" value="inactive" />
							</c:url>
							<a href="${deactivateLink}">inactivate</a>
						</c:when>
						<c:when test="${testSetup.status == 'FAILED'}">
                            <c:url var="assignPointsLink"
                                            value="/view/instructor/assignPoints.jsp">
                                <c:param name="testRunPK" value="${testSetup.testRunPK}" />
                            </c:url>
                            <a href="${assignPointsLink}">Activate and assign points to failed test setup</a>
                        </c:when>
						
						<%--
						<c:when test="${testSetup.status == 'INACTIVE'}">
							<c:url var="activateLink" value="/action/instructor/ChangeTestSetupStatus">
								<c:param name="testSetupPK" value="${testSetup.testSetupPK}"/>
								<c:param name="status" value="active"/>
							</c:url>
							<a href="${activateLink}">activate</a>
						</c:when>
						--%>
					</c:choose>
					</td>

					<td>
						<c:if test="${testSetup.status != 'ACTIVE' and testSetup.status != 'INACTIVE'}">
						<c:url var="markBrokenLink"
										value="/action/instructor/ChangeTestSetupStatus">
							<c:param name="testSetupPK" value="${testSetup.testSetupPK}" />
							<c:param name="status" value="broken" />
						</c:url>
						<a href="${markBrokenLink}"> mark broken </a>
						</c:if>
					</td>
                    <td>
                        <c:if test="${testSetup.status == 'FAILED' or testSetup.status == 'PENDING'}">
                        <c:url var="retestLink"
                                        value="/action/instructor/ChangeTestSetupStatus">
                            <c:param name="testSetupPK" value="${testSetup.testSetupPK}" />
                            <c:param name="status" value="new" />
                        </c:url>
                        <a href="${retestLink}"> retest </a>
                        </c:if>
                    </td>
					<td><c:url var="downloadTestSetupLink"
							value="/data/instructor/DownloadTestSetup">
							<c:param name="testSetupPK" value="${testSetup.testSetupPK}" />
						</c:url> <a href="${downloadTestSetupLink}"> download </a></td>
				
						</c:if>
			</c:forEach>


<c:if test="${instructorActionCapability }">
			<tr class="submit">
	<th colspan=2 class="label">Upload new test setup</th>
	<td class="input"><a title="Comment"><textarea cols="30" rows="1"
										name="comment" placeholder="Description/comment on test setup"></textarea>
							</a>
	
							<td class="input" colspan="5" >
                            <input type="submit" value="Upload"/>
                    <input type="file" required="required" name="file" title="zip/jar file to upload"
									size=40/>

							
</td>
						</tr>
                        </c:if>
				
		</table>
            </form>
	</c:if>

<c:choose>

<c:when test="${project.tested}">

<ss:projectStats/>

</c:when>
<c:otherwise>
<h3>Upload only project</h3>
<p>This project is set up to accept uploads only, and not perform
any server based compilation and testing
</c:otherwise>
</c:choose>

<h3>Project Maintenance</h3>

<ul>
		<li><p style="font-weight: bold">Project Visibility</p> <c:url
				var="makeVisibleLink" value="/action/instructor/MakeProjectVisible" />

			<c:choose>
				<c:when test="${project.visibleToStudents}">
					<p>
						<span class="statusmessage">Visible to Students</span>
					<form method="post" action="${makeVisibleLink}">
						<input type="hidden" name="projectPK" value="${project.projectPK}" />
						<input type="hidden" name="newValue" value="false" /> <input
							type="submit" value="Make Invisible" style="color: #CC3300" />
					</form>

				</c:when>

				<c:otherwise>
					<p>
						<span class="statusmessageNegative">Invisible to Students</span>
					
					<form method="post" action="${makeVisibleLink}">
						<input type="hidden" name="projectPK" value="${project.projectPK}" />

						<input type="hidden" name="newValue" value="true" /> <input
							type="submit" value="Make Visible" style="color: #003399" />
					</form>
					<c:url var="makeVisibleHidden"
						value="/action/instructor/MakeProjectHidden" />
					<form method="post" action="${makeVisibleHidden}">
						<input type="hidden" name="projectPK" value="${project.projectPK}" />
						<input type="hidden" name="newValue" value="true" /> <input
							title="Hide a botched project creation; only visible at bottom of instructor course screen"
							type="submit" value="Hide project" style="color: #003399" />
					</form>

				</c:otherwise>
			</c:choose></li>

		<c:if test="${project.tested && instructorActionCapability}">
	<li><p style="font-weight: bold">Test-Outcome Visibility</p>
		<c:url var="link" value="/action/instructor/UpdatePostDeadlineOutcomeVisibility" />

		<form id="updatePostDeadlineOutcomeVisibilityForm" action="${link}" method="POST">
			<p><span class="statusmessage">Visibility of Test-Outcomes after the Late Deadline is '${project.postDeadlineOutcomeVisibility}'</span></p>
			<input type="hidden" name="projectPK" value="${project.projectPK}"/>
			<c:choose>
				<c:when test="${project.postDeadlineOutcomeVisibility == 'everything'}">
					<p class="notemessage">
						<b>NOTE:</b> Please be careful with this option!<br>
						All test outcomes are visible to students after the late deadline.
					</p>
				</c:when>
				<c:otherwise>
					<p class="notemessage">
						<b>NOTE: </b>Currently, all test outcomes (public, release and
						secret) are hidden from students even after the late deadline has
						passed.
						<br>
						If you set the post-deadline outcome visibility to 'everything',
						students will see all test outcomes (public, release and secret)
						once the project deadline has passed.
					</p>
				</c:otherwise>
			</c:choose>
			<p>
			<input type="radio" name="newPostDeadlineOutcomeVisibility" value="everything" ${ss:checked(project.postDeadlineOutcomeVisibility,'everything')}> Everything
			<input type="radio" name="newPostDeadlineOutcomeVisibility" value="nothing" ${ss:checked(project.postDeadlineOutcomeVisibility,'nothing')}> Nothing
			</p>
			<input type="submit" value="change post-deadline visibility of outcomes">
		</form>
	</li>
	</c:if>

<c:if test="${instructorActionCapability}">
	<li><p>
		<c:url var="updateProjectLink" value="/view/instructor/updateProject.jsp">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>
		<a href="${updateProjectLink}"> Update this project </a></p></li>
</c:if>

	<li><p>
		<c:url var="exportProjectLink" value="/data/instructor/ExportProject">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>
		<a href="${exportProjectLink}"> Export a project to a durable format
		(includes the currently active test-setup, canonical, the project starter files (if any),
		and the project settings).</a></p></li>

<c:url var="forkProjectLink" value="/action/instructor/ForkProject"/>
	<li><p>
<form id="forkProjectForm" action="${forkProjectLink}"
				 method="POST"><input
				type="hidden" name="projectPK" value="${project.projectPK}">
		Fork project - create a new project starting from information for this project
		 <input type="submit" value="Fork"></form>
		</li>

	
	<li><p>
		<c:url var="createDotSubmitFileLink" value="/data/instructor/CreateDotSubmitFile">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>
		<a href="${createDotSubmitFileLink}"> Get a .submit file for this project </a></p></li>
</ul>

<h3>Student Submissions</h3>
<ul>

    <li><p>	<c:url var="runMossLink" value="/action/instructor/RunMoss">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>
		<a href="${runMossLink}">
		Run Moss against all students' <b>last</b> submissions
		</a></p></li>
		
	 <c:if test="${project.tested}">
   <li><p>	<c:url var="downloadBestSubmissionsLink" value="/data/instructor/DownloadBestSubmissions">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>
		<a href="${downloadBestSubmissionsLink}">
		Download all students' <b>best</b> submissions
		</a> (according to this project's best submission policy)</p></li>

	<li><p> <c:url var="downloadBestOnTimeAndLateLink" value="/data/instructor/DownloadMostRecentOnTimeAndLateSubmissions">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>
		<a href="${downloadBestOnTimeAndLateLink}">
		Download all students' <b>best</b> on-time and late submissions
		</a> (according to this project's best submission policy)</p></li>
	</c:if>
	<li><p> <c:url var="downloadMostRecentOnTimeAndLateLink" value="/data/instructor/DownloadMostRecentOnTimeAndLateSubmissions">
			<c:param name="projectPK" value="${project.projectPK}"/>
			<c:param name="useDefault" value="true"/>
		</c:url>
		<a href="${downloadMostRecentOnTimeAndLateLink}">
		Download all students' <b>most recent</b> on-time and late submissions
		</a> (compiling submissions only; ignores this project's best submission policy)</p></li>

	<li><p> <c:url var="downloadAllSubmissionsLink" value="/data/instructor/DownloadAllSubmissions">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>
		<a href="${downloadAllSubmissionsLink}">
		Download all submissions for all students for this project
		</a></p></li>

	<li><p> <c:url var="downloadRobotSubmissionsLink" value="/robot/DownloadSubmissions">
			<c:param name="projectPK" value="${project.projectPK}"/>
			<c:param name="key" value="${robotKey}"/>

		</c:url>
		<a href="${downloadRobotSubmissionsLink}">
		Robot download link
		</a></p></li>
</ul>


<h3>Student Grades</h3>
<ul>
	<li><p>
		<c:url var="printTestDetailsLink" value="/data/instructor/PrintTestDetailsForDatabase">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>
		<a href="${printTestDetailsLink}">Print test names and points in CSV format</a></p></li>

	<li><p>
		<c:url var="printGradesLink" value="/data/instructor/PrintGrades">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>
		<a href="${printGradesLink}"> Print grades in CSV format for spreadsheet use</a></p></li>

	<li><p>
		<c:url var="printGradesForDatabaseLink" value="/data/instructor/PrintGradesForDatabase">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>
		<a href="${printGradesForDatabaseLink}"> Print grades in CSV format</a></p></li>

	<li><p>
		<c:url var="printGradesForAllSubmissions" value="/data/instructor/PrintGradesForAllSubmissions">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>
		<a href="${printGradesForAllSubmissions}"> Print grades for <i>ALL</i> submissions in CSV format for spreadsheet use</a></p></li>

	<li><p>
		<c:url var="printGradesForAllSubmissionAdjustedByBackgroundRetests" value="/data/instructor/PrintGradesForAllSubmissionsAdjustedByBackgroundRetests">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>
		<a href="${printGradesForAllSubmissionAdjustedByBackgroundRetests}">
			Print grades for <i>ALL</i> submissions in CSV format for spreadsheet use, adjusted by the results of background retests
		</a></p></li>
</ul>



<h3>Code Coverage</h3>

<ul>
	<li><p>
		<c:url var="downloadCodeCoverageLink" value="/data/instructor/DownloadCodeCoverageResultsForProject">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>
		<a href="${downloadCodeCoverageLink}">
			Download code coverage results in CSV format (tab delimited)
		</a></p></li>

	<li><p>
		<c:url var="downloadCodeCoverageByPackageLink" value="/data/instructor/DownloadCodeCoverageResultsByPackage">
			<c:param name="projectPK" value="${project.projectPK}"/>
		</c:url>
		<a href="${downloadCodeCoverageByPackageLink}"> Download code coverage results split up by
			packages in CSV format (tab delimited)
		</a></p></li>


</ul>

  </body>
</html>
