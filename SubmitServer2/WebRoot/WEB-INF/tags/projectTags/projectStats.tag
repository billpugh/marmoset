<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:if test="${project.tested and not empty allTestSetups and not empty studentRegistrationSet}">

<h3>Test Stats</h3>
<div class="projectvitalstats">
        
    <c:choose>
    <c:when test="${not empty testProperties}">
    <p>Test framework: <c:out value="${test.properties.framework}"/>
    </c:when>
    <c:otherwise>
    <p>No test properties
    </c:otherwise>
    </c:choose>
    
    
    <c:if test="${projectBuildStatusCount['new'] > 0}">
        <p>${projectBuildStatusCount['new']} submissions waiting to be tested
        </p>
    </c:if>
    <c:if test="${projectBuildStatusCount['pending'] > 0}">
        <p>${projectBuildStatusCount['pending']} submissions being tested
        </p>
    </c:if>
  <c:if test="${projectBuildStatusCount['complete'] > 0}">
        <p>${projectBuildStatusCount['complete']} submissions tested
        </p>
    </c:if>
     <c:if test="${projectBuildStatusCount['retest'] > 0}">
        <p>${projectBuildStatusCount['retest']} submissions being retested
        </p>
    </c:if>
     <c:if test="${projectBuildStatusCount['broken'] > 0}">
        <p>${projectBuildStatusCount['broken']} broken submissions
        </p>
    </c:if>

    <c:set var="inconsistentResults"
        value="${fn:length(failedBackgroundRetestSubmissionList)}" />
    <c:if test="${inconsistentResults > 0}">
        <c:url var="smallfailedBackgroundRetestLink"
            value="/view/instructor/failedBackgroundRetests.jsp">
            <c:param name="projectPK" value="${project.projectPK}" />
        </c:url>

        <p><a href="${smallfailedBackgroundRetestLink}"> ${inconsistentResults} submissions with inconsistent
        results (from a background test)
         </a></p>
    </c:if>

    <c:if test="${projectBuildStatusCount['notDone'] == 0 && inconsistentResults == 0}">
    <p>All submissions tested; results consistent.</p>
    </c:if>
</div>
</c:if>