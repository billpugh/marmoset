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


<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="ss" uri="http://www.cs.umd.edu/marmoset/ss"%>
<!-- Don't redirect when there's only a single course; we need to show the list of courses to register for. -->
<c:if test="${false}">
<c:if test="${singleCourse && instructorCapability && !initParam['demo.server']=='true'}">
	<c:redirect url="/view/instructor/course.jsp">
		<c:param name="coursePK" value="${courseList[0].coursePK}" />
	</c:redirect>
</c:if>
<c:if test="${singleCourse}">
	<c:redirect url="/view/course.jsp">
		<c:param name="coursePK" value="${courseList[0].coursePK}" />
	</c:redirect>
</c:if>
</c:if>
<!DOCTYPE HTML>
<html>
<head>
<ss:headContent title="Submit Server Home Page" />
<script
	src="http://ajax.googleapis.com/ajax/libs/jquery/1.4.4/jquery.min.js"
	type="text/javascript"></script>
<style>
#content { padding: 0px 5%; padding-top: 1em;}
#content h2 { margin-top: 0px;}
#enrolled-list { width: 47%; float: left;}
#open-list {width: 47%; margin-left: 50%;}
</style>
</head>
<body>
<ss:header />
<ss:breadCrumb />

<div class="sectionTitle">
	<h1>Home</h1>
	<p class="sectionDescription">Welcome ${user.firstname}</p>
</div>

<c:set var="statusMap" value="${userSession.instructorStatus}"/>
<div id="content">
<div id="enrolled-list">
<h2>Courses Enrolled</h2>
<c:choose>
<c:when test="${empty courseList}">
<p>Not registered for any courses</p>
</c:when>
<c:otherwise>
<ul>
	<c:forEach var="course" items="${courseList}">
		<c:choose>
			<c:when
				test="${user.superUser || statusMap[course.coursePK]}">
				<c:set var="courseURL" value="/view/instructor/course.jsp" />
				<li style="list-style-type:circle">
			</c:when>
			<c:otherwise>
			    <li>
				<c:set var="courseURL" value="/view/course.jsp" />
			</c:otherwise>
		</c:choose>
		<c:url var="courseLink" value="${courseURL}">
			<c:param name="coursePK" value="${course.coursePK}" />
		</c:url> <a href="${courseLink}">
		<c:out value="${course.courseName}"/><c:if test="${not empty course.section}"><c:out value="${course.section}"/></c:if>:
		<c:out value="${course.description}"/> </a>
	</c:forEach>
	<c:if test="${student.canImportCourses}">
	<c:url var="importCourseLink" value="/view/import/importCourse.jsp"/>
	<li><a href="${importCourseLink}">Import course from grade server</a>
	</c:if>
</ul>
</c:otherwise>
</c:choose>
</div>

<c:if test="${not empty openCourses}">
<div id="open-list">
<h2>Open Courses</h2>
<form>
<table id="open-course-table">
	<tr>
		<th><input type="checkbox" id="toggle-all"/></th>
		<th>Course Name</th>
		<th>Description</th>
	</tr>
	<c:forEach var="course" items="${openCourses}">
		<c:set var="checkboxName" value="course-pk-${course.coursePK}" />
		<td><input type="checkbox" name="${checkboxName}" id="${checkboxName}-box"/></td>
		<td><label for="${checkboxName}-box"><c:out value="${course.courseName}" /></label></td>
		<td><c:out value="${course.description}" /></td>
	</c:forEach>
	<tr>
		<td colspan="3">
			<input type="submit" value="Request enrollment" />
		</td>
	</tr>
</table>
</form>
</div>
</c:if>

</div> <!-- content div -->

<ss:footer />

<script type="text/javascript">
window.$marmoset = {
		toggleAll: $("#toggle-all"),
		openCourseTable: $("#open-course-table")
};

$marmoset.toggleAll.click(function(event) {
	var checked = $marmoset.toggleAll.is(":checked");
	$marmoset.openCourseTable
			.find('input[type="checkbox"]')
			.each(function(index, box) {
		box.checked = checked;
	});
});
</script>
</body>
</html>
