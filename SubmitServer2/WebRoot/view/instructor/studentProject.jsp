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
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions"
    prefix="fn" %>
<%@ taglib prefix="ss" uri="http://www.cs.umd.edu/marmoset/ss"%>

<!DOCTYPE HTML>
<html>
<head>
<ss:headContent
	title="Project ${project.projectNumber} for ${course.courseName}" />
	    <script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.7.2/jquery-ui.min.js"></script>
<link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.7.1/themes/blitzer/jquery-ui.css"
    type="text/css" />
<script src="${jsBase}/jquery.easy-confirm-dialog.js"></script>
<script type="text/javascript">
  $(document).ready(function() {
    $(".confirm").easyconfirm();
  });
    </script>
    </head>

<body>
<ss:header />
<ss:instructorBreadCrumb />

<ss:projectTitle />
<ss:projectMenu />

<ss:studentPicture />
<h2><c:out value="${studentRegistration.fullname}"/> </h2>
<p><ss:studentEmail/>

<c:if test="${projectPartner != null}">
<c:url var="removePartnerLink" value="/action/instructor/ClearPartner">
	<c:param name="studentRegistrationPK" value="${studentRegistration.studentRegistrationPK}"/>
	<c:param name="projectPK" value="${project.projectPK}"/>
</c:url>
<p>Partnered with
	<c:out value="${projectPartner.fullname}"/>
	(<a href="${removePartnerLink}" class="confirm" 
	title="Are you sure you want to remove their partner selection?">remove</a>)
		
	</p>

</c:if>

<c:if test="${studentSubmitStatus.extension > 0}">
<p>
Current Extension: ${studentSubmitStatus.extension}<br></c:if>

<c:url var="grantExtensionLink" value="/view/instructor/grantExtension.jsp">
	<c:param name="studentRegistrationPK" value="${studentRegistration.studentRegistrationPK}"/>
	<c:param name="projectPK" value="${project.projectPK}"/>
</c:url>
<p>
<a href="${grantExtensionLink}"> Grant 
<c:out value="${studentRegistration.fullname}"/>
an extension on project
<c:out value="${project.projectNumber}"/></a>

<c:set var="testCols" value="2" />

<c:set var="inconsistentResults"
      value="${fn:length(failedBackgroundRetestSubmissionList)}" />

<c:if test="${project.tested}">
	<c:if test="${inconsistentResults > 0}">
		<c:set var="testCols" value="${1+testCols}" />
	</c:if>
	<c:if test="${testProperties.language=='java'}">
		<c:set var="testCols" value="${2+testCols}" />
	</c:if>
</c:if>

<h2>Submissions</h2>
<table>
	${ss:formattedColumnHeaders(5+testCols, canonicalTestOutcomeCollection)}

	<tr>
			<th rowspan="2">#</th>
			<th rowspan="2">date submitted</th>
			<th rowspan="2"># changed <br />lines
			</th>

			<c:if test="${project.tested }">
				<th rowspan="2">Results<br>
				<abbr title="Public Tests">P</abbr> | <abbr title="Release Tests">R</abbr>
					| <abbr title="Secret Tests">S</abbr> <c:if
						test="${testProperties.language=='java'}">
					 | <abbr title="Findbugs Warnings">F</abbr>
					</c:if>
				</th>
				<th rowspan="2">release tested</th>
				<c:if test="${inconsistentResults > 0}">
					<th rowspan="2"># inconsistent<br>background<br>retests
					</th>
				</c:if>
				<c:if test="${testProperties.language=='java'}">
					<th rowspan="2"># FindBugs<br>warnings
					</th>
					<th rowspan="2"># Student<br>written<br>tests
					</th>
				</c:if>
			</c:if>
			<th rowspan="2">view source</th>
			<th rowspan="2">Download</th>

			${ss:formattedTestHeaderTop(canonicalTestOutcomeCollection, true)}

		</tr>
	<tr>
		${ss:formattedTestHeader(canonicalTestOutcomeCollection, true)}
		</tr>


	<c:forEach var="submission" items="${submissionList}"
		varStatus="counter">
		<c:url var="submissionLink"
						value="/view/instructor/submission.jsp">
						<c:param name="submissionPK" value="${submission.submissionPK}" />
					</c:url>
        <c:set var="testOutcomes" value="${testOutcomesMap[submission.submissionPK]}"/>
		<tr class="r${counter.index % 2}">
			<td>${submission.submissionNumber}</td>

			<td><fmt:formatDate value="${submission.submissionTimestamp}"
				pattern="dd MMM h:mm a" /></td>
			<td><c:out value="${ss:numberChangedLines(connection, submission, project)}"/>


			<c:if test="${project.tested}">
				<c:choose>
					<c:when
						test="${submission.buildStatus == 'COMPLETE' && submission.compileSuccessful}">

						<td><a href="${submissionLink}">
						${submission.valuePublicTestsPassed} |
						${submission.valueReleaseTestsPassed} |
						${submission.valueSecretTestsPassed} <c:if
							test="${testProperties.language=='java'}">
					| ${submission.numFindBugsWarnings}
					 </c:if> </a>
                              <c:if test="${submissionsWithReviews.contains(submission.submissionPK)}">
                <c:url var="reviewLink" value="/view/codeReview/index.jsp">
                    <c:param name="submissionPK" value="${submission.submissionPK}" />
                    </c:url>
                (<a href="${reviewLink}">R</a>)</c:if> 
                     
                     </td>
						<td><fmt:formatDate value="${submission.releaseRequest}"
							pattern="dd MMM h:mm a" /></td>
                     
                        <c:if test="${inconsistentResults > 0}">
						<td>
                        <c:if test="${submission.numFailedBackgroundRetests > 0}">
                        <c:url var="submissionAllTestsLink"
							value="/view/instructor/submissionAllTests.jsp">
							<c:param name="submissionPK" value="${submission.submissionPK}" />
						</c:url> <a href="${submissionAllTestsLink}">
						${submission.numFailedBackgroundRetests} </a></c:if></td>
                        </c:if>

						<c:if test="${testProperties.language=='java'}">
							<td><c:if test="${testOutcomes.numFindBugsWarnings > 0}">
							${testOutcomes.numFindBugsWarnings}</c:if></td>
							<td><c:if test="${testOutcomes.numStudentWrittenTests > 0}">
							${testOutcomes.numStudentWrittenTests}</c:if></td>
						</c:if>

					</c:when>
					<c:when test="${submission.buildStatus == 'COMPLETE'}">
						<td colspan="${testCols}"><a href="${submissionLink}">did
						not compile</a></td>
					</c:when>
					<c:when test="${submission.buildStatus == 'PENDING'}">
						<td colspan="${testCols}"><a href="${submissionLink}">testing started at
						<fmt:formatDate value="${submission.buildRequestTimestamp}"
							pattern="dd MMM h:mm a" /></a></td>
					</c:when>
					<c:when test="${submission.buildStatus == 'BROKEN'}">
						<td colspan="${testCols}"><a href="${submissionLink}">broken</a></td>
					</c:when>
					<c:when test="${submission.buildStatus == 'NEW'}">
						<td colspan="${testCols}"><a href="${submissionLink}">not tested yet</a></td>
					</c:when>
					<c:when test="${submission.buildStatus == 'RETEST'}">
						<td colspan="${testCols}"><a href="${submissionLink}">retest requested</a></td>
					</c:when>
					<c:otherwise>
						<td colspan="${testCols}"><a href="${submissionLink}">Build
						status ${submission.buildStatus}</a></td>
					</c:otherwise>
				</c:choose>
			</c:if>
			<td>
			<c:url var="viewSourceLink" value="/view/allSourceCode.jsp">
				<c:param name="submissionPK" value="${submission.submissionPK}" />
				<c:param name="testType" value="cardinal" />
				<c:param name="testNumber" value="all" />
			</c:url>
			<a href="${viewSourceLink}"> view</a>
			</td>


			<td><c:url var="downloadLink" value="/data/DownloadSubmission">
				<c:param name="submissionPK" value="${submission.submissionPK}" />
			</c:url> <a href="${downloadLink}"> download</a></td>

			${ss:formattedTestResults(canonicalTestOutcomeCollection,testOutcomesMap[submission.submissionPK])}



		</tr>
	</c:forEach>

</table>

<c:if test="${not empty launchSummary && userSession.superUser}">
<h2>Eclipse launches</h2>
<table>
<tr>
<th rowspan="2" >When
<th rowspan="2" >versions
<th rowspan="2" >total
<th colspan="2" >junit
</tr>
<tr>
<th> runs
<th> debugs
</th>
</tr>
	<c:forEach var="summary" items="${launchSummary}"
		varStatus="counter">
		<tr class="r${counter.index % 2}">
<td><fmt:formatDate value="${summary.start}"
				pattern="dd MMM h:mm a" /></td>

			<td><c:out value="${summary.distinctVersions}"/>
			<td><c:out value="${summary.total}"/>
			<td><c:out value="${summary.runJUnit}"/>
			<td><c:out value="${summary.debugJUnit}"/>



</tr>
</c:forEach>
	</table>
	</c:if>

<ss:footer />
</body>
</html>
