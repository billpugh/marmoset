<%@ attribute name="title" required="true" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ss" uri="http://www.cs.umd.edu/marmoset/ss"%>
                
 <title><c:out value="${title}"/></title>
 <meta name=viewport content="width=device-width, initial-scale=1">
 <c:url var="css" value="/styles.css"/>
 <link rel="stylesheet" type="text/css" href="${css}">
 <script
	src="https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js"
	type="text/javascript"></script>
	
    <script type="text/javascript">
 function toggle(item) {
     $(document.getElementById(item)).slideToggle("slow");
 }
 function showItem(item) {
     $(document.getElementById(item)).slideDown("slow");
 }
 function hideItem(item) {
     $(document.getElementById(item)).slideUp("slow");
 }
</script>
 <ss:brandingProperty key="branding.analytics" safeHtml="true" />

