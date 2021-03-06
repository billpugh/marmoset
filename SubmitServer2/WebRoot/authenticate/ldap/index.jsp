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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ss" uri="http://www.cs.umd.edu/marmoset/ss"%>

<c:if test="${not empty initializationNeeded}">
<jsp:forward page="initialize.jsp"/>
</c:if>
<!DOCTYPE HTML>
<html>
  <ss:head title="Submit Server Login page"/>
  <body>
  <ss:header/>
  <ss:loginBreadCrumb/>

  <ss:loginTitle/>
  
  <c:choose>
     
    <c:when test="${missingIDOrPasswordException == true}">
      <ss:missingIdOrPasswordMessage/>
    </c:when>
    
    <c:when test="${canNotFindDirectoryID == true}">
      <ss:noSuchIdMessage/>
    </c:when>
  
    <c:when test="${badPassword == true}">
      <ss:authenticationFailedMessage/>
    </c:when>
  
    <c:when test="${noSuchStudentInDB == true}">
      <ss:noSuchStudentInDBMessage/>
    </c:when>

    <c:when test="${otherError == true}">
      <ss:authenticationFailedMessageGeneric/>
    </c:when>
  
  </c:choose>

  <ss:loginForm/>

  <ss:footer/>
  </body>
</html>
