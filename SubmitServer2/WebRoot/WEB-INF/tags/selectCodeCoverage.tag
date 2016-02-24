<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="ss" uri="http://www.cs.umd.edu/marmoset/ss"%>

<c:if
	test="${project.tested 
    and testProperties.language == 'java' 
    and testProperties.performCodeCoverage}">
    <p style="margin-left: 1em">Test type: <c:out value="${testType}"/>
	<c:if
		test="${! empty testType  and testTypeString != 'none' and ! empty testNumber}">
		<p style="margin-left: 1em">
			<b>Coverage information for ${hybridTestType} ${testTypeString} <c:choose>
					<c:when test="${testNumber != 'all'}">
         test #${testNumber}: ${testName}</c:when>
					<c:otherwise>tests</c:otherwise>
				</c:choose> </b>
		</p>
	</c:if>
    <c:set var="escapedFileName"><c:out value="${sourceFileName}"/></c:set>
	<p>
	<form class="form" action="${pageContext.request.requestURL}"
		method="GET">
		<input type=hidden name="submissionPK" value="${submission.submissionPK}" />
		<input type=hidden name="testNumber" value="all" /> 
        <input type=hidden name="sourceFileName" value="${sourceFileName}"/>
        <input type="hidden" name="previousTestType" value="${testType}"/> 
		Change code coverage:
	   <select	name="testType" onchange="javascript:this.form.submit();">
			<option value="none" ${ss:selectedOrNull(testType,"none")} >no
				code coverage</option>
			<option value="public" ${ss:selected(testType,"public")} >Public</option>
			<option value="public-student" ${ss:selected(testType,"public-student")} >Public
				and Student</option>
			<c:if test="${instructorCapability}">
			<option value="cardinal" ${ss:selected(testType,"cardinal")}  >All
				Instructor</option>
			<option value="release-unique" ${ss:selected(testType,"release-unique")}  >Instructor
				but not student</option>
			<option value="all" ${ss:selected(testType,"all")}>All</option>
			</c:if>
		</select>
	</form>
</c:if>
