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
<ss:head
	title="Students registered for ${course.courseName} in semester ${course.semester}" />
<body>
<ss:header />
<ss:instructorBreadCrumb />

<h1>Students registered</h1>

       <table>
            <tr>
               <th>Name</th>
                <th>class account</th>
            </tr>
            <c:forEach var="studentRegistration" items="${registeredStudents}" varStatus="counter">
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
        </table>

<c:if test="${not empty errors}">
<h1>Registration errors</h1>
  <c:forEach var="error" items="${errors}" varStatus="counter">
  <pre><c:out value="${error}"/></pre>
  </c:forEach>
</c:if>


<ss:footer />
</body>
</html>
