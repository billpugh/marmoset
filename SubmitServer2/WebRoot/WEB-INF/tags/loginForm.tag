<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ss" uri="http://www.cs.umd.edu/marmoset/ss"%>


    <c:url var="loginLink" value="/authenticate/ldap/PerformLogin"/>
	<form id="PerformLogin" method="post" action="${loginLink}" >
		<c:if test="${not empty param.target}">
	    <input type="hidden" name="target" value="${ss:urlEncodePath(param.target)}">
	    </c:if>
	    <table class="form">
	    <tr><th colspan=2><ss:brandingProperty key="branding.login.prompt" safeHtml="true"/></th></tr>
	    <tr><td class="label">Directory ID:<td class="input"> <input type="text" name="loginName" autofocus autocorrect="off" autocapitalize="off"/>
		<tr><td class="label">Directory Password: <td class="input"> <input type="password" name="uidPassword"/>
		<tr><td class="label">Keep me logged in:<td class="input"><input type="checkbox" name="keepMeLoggedIn" value="checked" title="Do not check on a shared computer/account"/> 
        
		<tr class="submit"><td colspan="2"><input type="submit" value="Login" name="Login"/></td></tr>

							</table>
	</form>


<p class="alertmessage" style="font-size: smaller;">
	<ss:brandingProperty key="branding.login.termsOfUse" safeHtml="true" />
</p>







