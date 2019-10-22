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


<!DOCTYPE HTML>
<html>
  <ss:head title="Submit Server Login page"/>
  <body>
  <ss:header/>
  <ss:loginBreadCrumb/>

  <ss:loginTitle/>

<!-- 
<c:url var="loginLink" value="/authenticate/InitializeDatabase"/>
<form id="PerformLogin" method="post" action="${loginLink}" >

  <table class="form" width="%30">
    <tr><th colspan=2>Initialize the submit server; provide your Directory ID</th></tr>
    <tr><td class="label">Directory ID:</td><td class="input"> <input type="text" name="loginName"  autocorrect="off" autocapitalize="off"/></td></tr>
  </table>
</form>
 -->

	
<c:url var="loginLink" value="/authenticate/cas/InitializeDatabase"/>
<form id="PerformLogin" method="get" action="${loginLink}" >

  <table class="form" width="%30">
    <tr><th colspan=2>Initialize the submit server; your directory id will be provided by CAS</th></tr>
    <tr><td class="label">Campus UUID:</td><td class="input"> <input type="text" name="campusUUID" inputmode="numeric" autofocus /></td></tr>
   <tr><td class="label">First name:</td><td class="input"> <input type="text" name="firstName"  autocorrect="off" /></td></tr>
   <tr><td class="label">Last name:</td><td class="input"> <input type="text" name="lastName"  autocorrect="off" /></td></tr>
   <tr><td class="label"></td><td class="submit"><input type="submit" value="Login" name="Login"/></td></tr>

  </table>
</form>


  <ss:footer/>
  </body>
</html>
