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
<c:choose>
	<c:when test="${course != null}">

		<ss:head title="Register Student for ${course.courseName}" />
	</c:when>
	<c:otherwise>
		<ss:head title="Register New Student" />
	</c:otherwise>
</c:choose>
<body>
	<ss:header />
	<ss:instructorBreadCrumb />

	<div class="sectionTitle">
		<h1>Student Registration</h1>

		<c:choose>
			<c:when test="${course != null}">
				<p class="sectionDescription">Use this form to add a person to
					${course.courseName}</p>
			</c:when>
			<c:otherwise>
				<p class="sectionDescription">Use this form to register a person
					(but not enroll them in any courses)</p>
			</c:otherwise>
		</c:choose>
	</div>

	<c:if test="${course != null}">

		<div>
		<h2>Register existing student</h2>
			<form id="registerExistingStudentForm"
				action='<c:url value="/action/instructor/RegisterOneStudent"/>'
				method="post">
				<input type="hidden" name="coursePK" value="${course.coursePK}" />
				<table class="form">
					<tfoot>
						<tr class="submit">
							<td class="submit" colspan="2"><input type="submit"
								value="Register existing student" /></td>
						</tr>
					</tfoot>
					<tbody>
						<tr>
							<td class="label">Student</td>

							<td class="input"><select name="existingStudentPK">
									<c:forEach var="student" items="${allStudents}">
										<option value="${student.studentPK}">
											<c:out value="${student.lastname}" />,
											<c:out value="${student.firstname}" /> 
											(<c:out value="${student.loginName}" />)
										</option>
									</c:forEach>
							</select></td>
						</tr>
						<tr>
							<td class="label">Class account</td>
							<td class="input"><input name="classAccount" type="text"
								value="" autocorrect="off" autocapitalize="off"
								placeholder="leave blank to use login name" size="30" /></td>
						</tr>
						<c:if test="${not empty course.sections}">
							<tr>
								<td class="label">Section</td>
								<td class="input"><select name="section">
										<c:forEach var="s" items="${course.sections}">
											<option>
												<c:out value="${s}"></c:out>
											</option>
										</c:forEach>
								</select></td>
							</tr>
						</c:if>
						<tr>
							<td class="label">Capability</td>
							<td class="input"><select name="capability">
									<option value="">Student</option>
									<option value="modify">Instructor</option>
									<option value="read-only">Grader</option>
									<option value="pseudo-student"
										title="does not participate in code reviews">Pseudo
										student</option>
							</select></td>
						</tr>
					</tbody>

				</table>
			</form>

		</div>
		<div>
		<h2>Promote student to staff</h2>
			<form id="promoteStudentToTA"
				action='<c:url value="/action/instructor/TurnStudentIntoTA"/>'
				method="post">
				<input type="hidden" name="coursePK" value="${course.coursePK}" />
				<table class="form">
					<tfoot>
						<tr class="submit">
							<td class="submit" colspan="2"><input type="submit"
								value="Turn student into TA" /></td>
						</tr>
					</tfoot>
					<tbody>
						<tr>
							<td class="label">Student</td>

							<td class="input"><select name="studentRegistrationPK">
									<c:forEach var="studentRegistration"
										items="${justStudentRegistrationSet}">
										<option value="${studentRegistration.studentRegistrationPK}">
											<c:out value="${studentRegistration.fullname}" />
										</option>
									</c:forEach>
							</select></td>
						</tr>
						<tr>
							<td class="label">Capability</td>
							<td class="input"><select name="capability">
									<option value="modify">Instructor/TA</option>
									<option value="read-only">Grader</option>
							</select></td>
						</tr>
					</tbody>

				</table>
			</form>

		</div>
	</c:if>

	<c:if test="${authType == 'ldap' || authType == 'cas'}">
	<div>
		<h2>Register new student</h2>
		<form id="registerOneStudentForm"
			action='<c:url value="/action/instructor/RegisterOneStudent"/>'
			method="post">
			<c:if test="${course != null}">
				<input type="hidden" name="coursePK" value="${course.coursePK}" />
			</c:if>
			<table class="form">
				<tfoot>
					<tr class="submit">
						<td class="submit" colspan="2"><input type="submit"
							value="Register new student" /></td>
					</tr>
				</tfoot>
				<tbody>
					<tr>
						<td class="label">First name</td>
						<td class="input"><input name="firstname" type="text"
							value="" required /></td>
					</tr>
					<tr>
						<td class="label">Last name</td>
						<td class="input"><input name="lastname" type="text" value=""
							required /></td>
					</tr>
					<tr>
						<td class="label">Email</td>
						<td class="input"><input name="email" type="text" value=""
							autocorrect="off" autocapitalize="off" required /></td>
					</tr>
					<tr>
						<td class="label">Login name</td>
						<td class="input"><input name="loginName" type="text"
							value="" autocorrect="off" autocapitalize="off" required /></td>
					</tr>
					<tr>
						<td class="label">Campus uid</td>
						<td class="input"><input name="campusUID" type="text"
							value="" autocorrect="off" autocapitalize="off" required /></td>
					</tr>
					<c:if test="${course != null}">
						<tr>
							<td class="label">Class account</td>
							<td class="input"><input name="classAccount" type="text"
								value="" autocorrect="off" autocapitalize="off" /></td>
						</tr>
						<tr>
							<td class="label">Capability</td>
							<td class="input"><select name="capability">
									<option value="">Student</option>
									<option value="modify">Instructor</option>
									<option value="read-only">Grader</option>
									<option value="pseudo-student"
										title="does not participate in code reviews">Pseudo
										student</option>
							</select></td>
						</tr>
					</c:if>
				</tbody>

			</table>
		</form>

	</div>
	</c:if>
	
</body>
</html>
