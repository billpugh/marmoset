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
<%@ taglib prefix='fn' uri='http://java.sun.com/jsp/jstl/functions'%>



<!DOCTYPE HTML>
<html>
<head>
<ss:headContent title="Import and link courses from grade server" />
</head>

<body>
<ss:header />
<ss:instructorBreadCrumb />

<div class="sectionTitle">
<h1>Import/link course(s) from grade server</h1>
</div>

<c:choose>
	<c:when test="${fn:length(gradeCourses) == 0}">
		<p>You are not registered as the instructor for any courses on the
		grade server. Create your course entries on the grade server first
		before trying to import and synchronize them with the submit server.</p>
	</c:when>
	<c:otherwise>


		<c:url var="importCourseLink" value="/action/ImportCourse" />
		<form action="${importCourseLink}" method="post"
			name="importCourseForm"><input type="hidden" name="semester"
			value="${semester}" />
		<table class="form">
		          <tfoot>
                <tr class="submit">
                    <td colspan=2><input type=submit
                        value="Import and Link Course"></td>
                </tr>
            </tfoot>
			<tbody>
				<tr>
					<th colspan=2>Import new course</th>
				</tr>
				<tr>
					<td class="label">Submit server course Name:
					<c:choose>
						<c:when test="${fn:length(gradeCourses) == 1}">
							<td class="input"><input type="text" name="courseName"
								value="${gradeCourses[0][0]}" required>
						</c:when>
						<c:otherwise>
							<td class="input"><input type="text" name="courseName" required>
						</c:otherwise>
					</c:choose>
					(Only letters, digits and hyphens)

					</td>
				</tr>
				<tr>
					<td class="label">Section:</td>
					<td class="input"><input type="text" name="courseName">(optional)</td>
				</tr>

				<tr>
					<td class="label">Semester:</td>
					<td class="input"><c:out value="${semester}" /></td>
				</tr>
 <tr>
            <td class="label">allows baseline/starter code download:</td>
            <td class="input"><input name="download" type="checkbox" checked  />
            </td>
        </tr>
         <tr>
            <td class="label">allows help requests:</td>
            <td class="input"><input name="helpRequests" type="checkbox" checked  />
            </td>
        </tr>
        <tr>
            <td class="label">Use default buildservers:</td>
            <td class="input"><input name="useDefaultBuildservers" type="checkbox" checked  />
            </td>
        </tr>
         <tr>
           <td class="label">Default for editing source code in browser:</td>
            <td class="input">
            <select name="browserEditing">
            <option value="prohibited">Prohibited</option>
            <option value="discouraged" selected="selected">Discouraged</option>
            <option value="allowed">Allowed</option>
            </select>
            </td>
        </tr>
				<tr>
					<td class="label"><a href="https://grades.cs.umd.edu">grades.cs.umd.edu</a> course(s):</td>
					<td class="input" valign="top"><c:choose>
						<c:when test="${fn:length(gradeCourses) == 1}">
							<input type="hidden" name="courseID"
								value="${gradeCourses[0][2]}" />
							<c:out value="${gradeCourses[0][1]}" />
						</c:when>
						<c:otherwise>
							<select name="courseID" MULTIPLE required>
								<c:forEach var="courseInfo" items="${gradeCourses}">
									<option value="${courseInfo[2]}"><c:out
										value="${courseInfo[1]}" /></option>
								</c:forEach>
							</select>
							Select at least one
						</c:otherwise>
					</c:choose></td>
				</tr>
				<tr>
					<td class="label">Description <br>
					(can be empty):</td>
					<td class="input"><input type="text" size="60" name="description"/></td>
				</tr>
				<tr>
					<td class="label">URL:</td>
					<td class="input"><input type="url" name="url" size="60"></td>
				</tr>
			</tbody>


		</table>
		</form>
		<c:if test="${fn:length(gradeCourses) > 1}">
			<p>Typically, you should only select a single grade server course
			to import. The exception to this might be if you have separate
			gradebooks for the honors and non-honors section of a course on <a
				href="http://grades.cs.umd.edu/">grades.cs.umd.edu</a>, but want to
			have a joint/combined entry for them on <a
				href="http://submit.cs.umd.edu/">submit.cs.umd.edu</a>.</p>
		</c:if>

	</c:otherwise>
</c:choose>

<ss:footer />
</body>
</html>
