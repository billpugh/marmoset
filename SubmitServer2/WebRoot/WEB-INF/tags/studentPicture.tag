<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!-- 
<c:if test="${student.hasPicture}">
<c:url var="pictureLink" value="/view/instructor/ViewPicture">
		<c:param name="studentRegistrationPK" value="${studentRegistration.studentRegistrationPK}" />
        <c:param name="studentPK" value="${student.studentPK}" />
	</c:url>
<p><img src="${pictureLink}"/>
</c:if>
 -->

<c:url var="pictureLink" value="https://umeg.umd.edu/showPic">
		<c:param name="gid" value="${student.campusUID}" />
       
	</c:url>
<p><img width="175" src="${pictureLink}"/>
