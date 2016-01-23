
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="ss" uri="http://www.cs.umd.edu/marmoset/ss"%>

<c:set var="serviceName">
	<ss:brandingProperty key="branding.service.fullname" safeHtml="true" />
</c:set>


<c:set var="googleSigninClientId">
	<ss:webProperty key="googleSignin.clientID" safeHtml="true" />
</c:set>

<html>
<head>

<!-- BEGIN Pre-requisites -->
<script src="//ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js">
  </script>
<script src="https://apis.google.com/js/client:platform.js?onload=start"
	async defer>
  </script>
<!-- END Pre-requisites -->

<ss:headContent title="Google signin for ${serviceName}" />
<c:set var="imageBase"
	value="${pageContext.request.contextPath}/images/" />

<!-- Continuing the <head> section -->
<script>
    function start() {
      gapi.load('auth2', function() {
        auth2 = gapi.auth2.init({
          client_id: '${googleSigninClientId}'
        });
      });
    }
  </script>

<meta charset="UTF-8">
<title>Google signin for ${serviceName}</title>
</head>
<body>
	<ss:header />
	<ss:loginBreadCrumb />
	<ss:loginTitle />

	<c:set var="target">
		<c:out value="${param.target}" />
	</c:set>
	<c:set var="serviceURL">
		<ss:brandingProperty key="branding.service.url" safeHtml="true" />
	</c:set>

	<p class="notemessage">
		Welcome to <a href="${serviceURL}">${serviceName}</a>. Cookies must be
		enabled to use Marmoset.

		<c:set var="requestURI" value="${pageContext.request.requestURI}" />

		<c:set var="requestURL" value="${pageContext.request.requestURL}" />
		<c:set var="servletPath" value="${pageContext.request.servletPath}" />
		<c:set var="redirect" value="${requestURL}GoogleSignin" />
	<p>
		Google Signin
		<!-- Add where you want your sign-in button to render -->
		<!-- Use an image that follows the branding guidelines in a real app -->
		<button id="signinButton">Sign in with Google</button>
		<script>
  $('#signinButton').click(function() {
    auth2.grantOfflineAccess({'redirect_uri': 'postmessage'}).then(signInCallback);
  });
</script>
	<div id="result"></div>


	<p class="notemessage">
		<ss:brandingProperty key="branding.login.termsOfUse" safeHtml="true" />

		<ss:footer />

		<!-- Last part of BODY element in file index.html -->
		<script>
function signInCallback(authResult) {
  if (authResult['code']) {
    console.log("Log in successful");
    console.log(authResult['code']);
    
     $('#signinButton').attr('style', 'display: none');

    window.location = "${redirect}?authCode=" + authResult['code'];
      
  } else {
    console.log("Log in failed");
    // There was an error.
  }
}
</script>
</body>
</html>