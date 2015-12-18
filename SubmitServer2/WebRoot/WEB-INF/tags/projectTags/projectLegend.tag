<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="ss" uri="http://www.cs.umd.edu/marmoset/ss"%>

<table>
	<tr class="r0">
		<th colspan="3"> Test Results Key </th>
	</tr>
	<tr class="r0">
		<th> outcome </th>
		<th> color </th>
		<th> description </th>
	</tr>
	<tr class="r1">
		<td> passed </td>
		<td class="passed"> &nbsp; </td>
		<td class="description"> The test case passed. </td>
	</tr>
	<tr class="r0">
		<td> failed </td>
		<td class="failed"> &nbsp; </td>
		<td class="description"> The test completed but did not produce the correct results.  For Java projects,
		this can mean junit.framework.AssertionFailedError or an junit.framework.ComparisonFailure,
		(the normal exceptions JUnit uses to signal failure), or a run-time exception
		was raised in the test case (for example because the student returned null where
		the test case did not expect null).
		For C projects, this means that the test executable returned a non-zero value.
		</td>
	</tr>
	<tr class="r1">
		<td> could not run </td>
		<td class="could_not_run"> &nbsp; </td>
		<td class="description">
			The test case could not be executed, typically because the submissions could
			not be compiled.  Java projects may also fail because they lack necessary resources
			(such as jar libraries).  It is also possible for a submission to expose a bug
			in the BuildServer that prevents code from executing, although these bugs are
			increasingly rare.
		</td>
	</tr>
	<tr class="r0">
		<td> timeout </td>
		<td class="timeout"> &nbsp; </td>
		<td class="description"> The test case took too long and was killed by the BuildServer.
		</td>
	</tr>
	<c:choose>
	<c:when test="${testProperties.language=='java'}">
	<tr class="r1">
		<td> error </td>
		<td class="error"> &nbsp; </td>
		<td class="description">  The test case failed due to a run-time exception,
		such as a NullPointerException or a ClassCastException.  If a run-time
		exception happens in the JUnit test case, this is considered a "failure" rather than
		an "error".
		</td>
	</tr>
	<tr class="r0">
		<td> not_implemented </td>
		<td class="not_implemented"> &nbsp; </td>
		<td class="description">  The test case failed because it has not yet been implemented.
		Many Java projects initially contain "stub" implementations
		of each method that throw java.lang.UnsupportedOperationException.
		</td>
	</tr>
	<tr class="r1">
		<td> missing component </td>
		<td class="missing_component"> &nbsp; </td>
		<td class="description"> The code tried to access a non-existent class, method or field.
		</td>
	</tr>
	<tr class="r1">
		<td> huh (security manager exception) </td>
		<td class="huh"> &nbsp; </td>
		<td class="description"> The test case invokes an operation that was blocked by the
		security manager.
		<p>
		<b>Note:</b> Everything on the server is tested using a security manager.  It is very
		unlikely that you are using a security manager on your computer.  Thus if code executes
		correctly on your machine but fails on the server due to a security manager exception,
		check if you are not calling operations typically blocked by the security manager, such
		as calling System.exit(), trying to read/write external files, and opening network
		connections.  If you're doing these things and you're an instructor, you may need to
		add these permissions to the security.policy file in the test-setup.
		If you are a student, don't use these operations, or if you feel they're necessary, ask
		your instructor if this is indeed the case.
		</td>
	</tr>
	</c:when>
	<c:otherwise>
		<tr class="r1">
		<td> fork bomb </td>
		<td class="fork_bomb"> &nbsp; </td>
		<td class="description"> The test case took too long and more than 100 processes were running when killed.
		</td>
	</tr>
	</c:otherwise>
	</c:choose>

</table>