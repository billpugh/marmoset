<%@ attribute name="title" required="true" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="ss" uri="http://www.cs.umd.edu/marmoset/ss"%>

<c:if test="${anyCodeReviews}">
<c:if test="${not empty title}">
	<h2>${title}</h2>
	</c:if>

	<c:if test="${not empty reviewsOfMyCode}">
		<h3>Reviews of my code</h3>
		<table>
			<tr>
                <c:if test="${empty project}">
                    <th rowspan="2">project</th>
                </c:if>
				<th colspan="2">Submission</th>
                <th rowspan="2">view</th>
				<th rowspan="2">tests</th>
			
                
				<th colspan="4">your comments</th>
				<th colspan="4">other comments</th>

			</tr>
			<tr>
                <th>#</th>
                <th>at</th>
				<th>#</th>
				<th>last</th>
				<th>needs res</th>
				<th>drafts</th>
				<th>#</th>
				<th>last</th>
				<th>needs res</th>
				<th>by</th>

			</tr>
			<c:forEach var="codeReviewSummary" items="${reviewsOfMyCode}"
				varStatus="counter">
				<c:url var="gwtCodeReviewLink" value="/view/codeReview/index.jsp">
                    <c:param name="codeReviewerPK"
                        value="${codeReviewSummary.codeReviewerPK}" />
                    <c:param name="submissionPK"
                        value="${codeReviewSummary.submissionPK}" />
                </c:url>

				<c:choose>

				<c:when test="${! instructorCapability}">
				<c:url var="detailsLink" value="/view/submission.jsp">
					<c:param name="submissionPK"
						value="${codeReviewSummary.submissionPK}" />
					<c:param name="codeReviewerPK"
						value="${codeReviewSummary.codeReviewerPK}" />
				</c:url>
				</c:when>

				<c:otherwise>
					<c:url var="detailsLink" value="/view/instructor/submission.jsp">
					<c:param name="submissionPK"
						value="${codeReviewSummary.submissionPK}" />
					<c:param name="codeReviewerPK"
						value="${codeReviewSummary.codeReviewerPK}" />
				</c:url>
				</c:otherwise>

				</c:choose>

				<tr class="r${counter.index % 2}" title="${codeReviewSummary.description}">
                <c:set var="submission" value="${codeReviewSummary.submission}" />
                <c:if test="${empty project}">
                        <td><c:out value="${codeReviewSummary.project.projectNumber}" /></td>
                    </c:if>
					 <td><c:out value="${submission.submissionNumber}"/></td>
                    <td><fmt:formatDate value="${submission.submissionTimestamp}" pattern="E',' dd MMM 'at' hh:mm a" /></td>
                    <td><a href="${gwtCodeReviewLink}" target="codeReview">View</a></td>
					<td><a href="${detailsLink}">Tests</a></td>
					
                   
					<c:choose>
						<c:when test="${codeReviewSummary.numCommentsByViewer > 0 || codeReviewSummary.anyUnpublishedDraftsByViewer}">
							<td><c:out value="${codeReviewSummary.numCommentsByViewer}" /></td>
							<td><fmt:formatDate
								value="${codeReviewSummary.lastUpdateByViewer}"
								pattern="E',' dd MMM 'at' hh:mm a" /></td>

							<td><c:if test="${codeReviewSummary.awaitingResponse}">yes</c:if></td>
							<td><c:if test="${codeReviewSummary.anyUnpublishedDraftsByViewer}">yes</c:if></td>

						</c:when>
						<c:otherwise>
							<td colspan="4" />
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${codeReviewSummary.numCommentsByOthers > 0 }">
							<td><c:out value="${codeReviewSummary.numCommentsByOthers}" /></td>
							<td><fmt:formatDate
								value="${codeReviewSummary.lastUpdateByOthers}"
								pattern="E',' dd MMM 'at' hh:mm a" /></td>

							<td><c:if test="${codeReviewSummary.needsResponse}"><a href="${gwtCodeReviewLink}" target="codeReview">yes</a></c:if></td>
							<td class="description"><c:out value="${codeReviewSummary.commentAuthors}" /></td>
						</c:when>
						<c:otherwise>
							<td colspan="4" />
						</c:otherwise>
					</c:choose>


				</tr>
			</c:forEach>

		</table>
	</c:if>


	<c:if test="${not empty myAssignments}">
		<h3>My code review assignments</h3>
		<table>
			<tr>
				<th rowspan="2">view</th>
				<th rowspan="2">tests</th>
				<c:if test="${empty project}">
					<th rowspan="2">project</th>
				</c:if>
				<th rowspan="2">due</th>
				<th rowspan="2">author</th>
				<th colspan="4">your comments</th>
				<th colspan="3">other comments</th>

			</tr>
			<tr>
				<th>#</th>
				<th>last</th>
				<th>needs res</th>
				<th>drafts</th>
				<th>#</th>
				<th>last</th>
				<th>needs res</th>

			</tr>
			<c:forEach var="codeReviewSummary" items="${myAssignments}"
				varStatus="counter">
				<c:url var="gwtCodeReviewLink" value="/view/codeReview/index.jsp">
                    <c:param name="codeReviewerPK"
                        value="${codeReviewSummary.codeReviewerPK}" />
                </c:url>
				<c:choose>
					<c:when test="${codeReviewSummary.project.tested}">
					    <c:set var="detailsName" value="Tests"/>
						<c:url var="detailsLink" value="/view/submission.jsp">
							<c:param name="submissionPK"
								value="${codeReviewSummary.submissionPK}" />
							<c:param name="codeReviewerPK"
								value="${codeReviewSummary.codeReviewerPK}" />
						</c:url>
					</c:when>
					<c:otherwise>
					    <c:set var="detailsName" value="Download"/>
						<c:url var="detailsLink" value="/data/DownloadSubmission">
							<c:param name="submissionPK"
								value="${codeReviewSummary.submissionPK}" />
							<c:param name="codeReviewerPK"
								value="${codeReviewSummary.codeReviewerPK}" />
						</c:url>
					</c:otherwise>
				</c:choose>

				<tr class="r${counter.index % 2}" title="${codeReviewSummary.description}">

					<td><a href="${gwtCodeReviewLink}" target="codeReview">View</a>

					<td><a href="${detailsLink}"><c:out value="${detailsName}"/></a>

					<c:if test="${empty project}">
						<td><c:out value="${codeReviewSummary.project.projectNumber}" /></td>
					</c:if>
					<td><fmt:formatDate
						value="${codeReviewSummary.assignment.deadline}"
						pattern="E',' dd MMM 'at' hh:mm a" /></td>

					<td><c:out value="${codeReviewSummary.author.name}" />
					<c:choose>
						<c:when test="${codeReviewSummary.numCommentsByViewer > 0 || codeReviewSummary.anyUnpublishedDraftsByViewer}">
							<td><c:out value="${codeReviewSummary.numCommentsByViewer}" /></td>
							<td><fmt:formatDate
								value="${codeReviewSummary.lastUpdateByViewer}"
								pattern="E',' dd MMM 'at' hh:mm a" /></td>

							<td><c:if test="${codeReviewSummary.awaitingResponse}"><a href="${gwtCodeReviewLink}" target="codeReview">yes</a></c:if></td>
							<td><c:if test="${codeReviewSummary.anyUnpublishedDraftsByViewer}"><a href="${gwtCodeReviewLink}" target="codeReview">yes</a></c:if></td>

						</c:when>
						<c:otherwise>
							<td colspan="4" >
                            <a href="${gwtCodeReviewLink}" target="codeReview">Start</a>
                            </td>
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${codeReviewSummary.numCommentsByOthers > 0 }">
							<td><c:out value="${codeReviewSummary.numCommentsByOthers}" /></td>
							<td><fmt:formatDate
								value="${codeReviewSummary.lastUpdateByOthers}"
								pattern="E',' dd MMM 'at' hh:mm a" /></td>

							<td><c:if test="${codeReviewSummary.needsResponse}">yes</c:if></td>

						</c:when>
						<c:otherwise>
							<td colspan="3" />
						</c:otherwise>
					</c:choose>




				</tr>
			</c:forEach>

		</table>
	</c:if>

	<c:if test="${not empty adHocReviews}">
		<h3>My ad hoc code reviews</h3>
		<table>
			<tr>
				<th rowspan="2">view</th>
				<th rowspan="2">tests</th>
				<c:if test="${empty project}">
					<th rowspan="2">project</th>
				</c:if>
				<th rowspan="2">#</th>
				<th colspan="4">your comments</th>
				<th colspan="3">other comments</th>

			</tr>
			<tr>
				<th>#</th>
				<th>last</th>
				<th>needs res</th>
				<th>drafts</th>
				<th>#</th>
				<th>last</th>
				<th>needs res</th>

			</tr>
			<c:forEach var="codeReviewSummary" items="${adHocReviews}"
				varStatus="counter">
				<c:url var="gwtCodeReviewLink" value="/view/codeReview/index.jsp">
					<c:param name="codeReviewerPK"
						value="${codeReviewSummary.codeReviewerPK}" />
				</c:url>

				<c:choose>
					<c:when test="${codeReviewSummary.project.tested}">
					    <c:set var="detailsName" value="Tests"/>
						<c:url var="detailsLink" value="/view/submission.jsp">
							<c:param name="submissionPK"
								value="${codeReviewSummary.submissionPK}" />
							<c:param name="codeReviewerPK"
								value="${codeReviewSummary.codeReviewerPK}" />
						</c:url>
					</c:when>
					<c:otherwise>
					    <c:set var="detailsName" value="Download"/>
						<c:url var="detailsLink" value="/data/DownloadSubmission">
							<c:param name="submissionPK"
								value="${codeReviewSummary.submissionPK}" />
							<c:param name="codeReviewerPK"
								value="${codeReviewSummary.codeReviewerPK}" />
						</c:url>
					</c:otherwise>
				</c:choose>


			 <tr class="r${counter.index % 2}" title="${codeReviewSummary.description}">
    

					<td><a href="${gwtCodeReviewLink}" target="codeReview">View</a>
					<td><a href="${detailsLink}"><c:out value="${detailsName}"/></a>

					<c:if test="${empty project}">
						<td><c:out value="${codeReviewSummary.project.projectNumber}" /></td>
					</c:if>
					<td><c:out value="${codeReviewSummary.author.name}" />
										<c:choose>
						<c:when test="${codeReviewSummary.numCommentsByViewer > 0 || codeReviewSummary.anyUnpublishedDraftsByViewer }">
							<td><c:out value="${codeReviewSummary.numCommentsByViewer}" /></td>
							<td><fmt:formatDate
								value="${codeReviewSummary.lastUpdateByViewer}"
								pattern="E',' dd MMM 'at' hh:mm a" /></td>

							<td><c:if test="${codeReviewSummary.awaitingResponse}"><a href="${gwtCodeReviewLink}" target="codeReview">yes</a></c:if></td>
							<td><c:if test="${codeReviewSummary.anyUnpublishedDraftsByViewer}"><a href="${gwtCodeReviewLink}" target="codeReview">yes</a></c:if></td>

						</c:when>
						<c:otherwise>
							<td colspan="4" />
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${codeReviewSummary.numCommentsByOthers > 0 }">
							<td><c:out value="${codeReviewSummary.numCommentsByOthers}" /></td>
							<td><fmt:formatDate
								value="${codeReviewSummary.lastUpdateByOthers}"
								pattern="E',' dd MMM 'at' hh:mm a" /></td>

							<td><c:if test="${codeReviewSummary.needsResponse}">yes</c:if></td>

						</c:when>
						<c:otherwise>
							<td colspan="3" />
						</c:otherwise>
					</c:choose>
				</tr>
			</c:forEach>

		</table>
	</c:if>

</c:if>
