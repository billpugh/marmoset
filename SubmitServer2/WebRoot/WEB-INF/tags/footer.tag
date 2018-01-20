<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<br clear="all">
<div class="footer">
<jsp:useBean id="now" class="java.util.Date" />
<fmt:formatDate value="${now}" pattern="dd MMM, hh:mm a"/>
- <a href="https://umd.edu/web-accessibility">Web Accessibility</a>
</div>