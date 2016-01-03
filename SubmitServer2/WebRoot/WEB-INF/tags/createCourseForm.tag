
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ss" uri="http://www.cs.umd.edu/marmoset/ss" %>

<c:url var="createCourseLink" value="/action/CreateCourse"/>
    <form action="${createCourseLink}" method="post" name="createCourseForm">
    <table class="form">
        <tr><th colspan=2>Create new course</th></tr>
        <tr>
            <td class="label">Course Name:</td>
            <td class="input"><input type="text" name="courseName" maxlength=20 size=20 placeholder="e.g., CMSC 132"></td>
        </tr>
        <tr>
            <td class="label">Semester:</td>
            <td class="input">
                <input type="text" name="semester" value="${ss:webProperty('semester')}" maxlength=15 size=15/>
            </td>
        </tr>
        <tr>
            <td class="label">Title (can be empty): </td>
            <td class="input">
                <input type="text" size="40" name="description" placeholder="e.g, Object Oriented Programming II">
            </td>
        </tr>
        <tr>
        	<td class="label">Sections (optional):</td>
        	<td class="input">
        		<input type="hidden" name="sections" id="hidden-section-list" />
        		<select id="course-section-dropdown"></select>
        		<input type="text" id="section-name-input" placeholder="Letters and numbers only" size="50" /><br />
        		<button type="button" id="edit-course-sections">add</button>
        		<button type="button" id="clear-course-sections">clear</button>
        	</td>
        </tr>
        <tr>
            <td class="label">URL:</td>
            <td class="input"><input type="url" name="url" size="60" placeholder="Where people can find out more about the course"></td>
        </tr>
        <tr>
            <td class="label">allows baseline/starter code download:</td>
            <td class="input"><input name="download" type="checkbox" checked  />
            </td>
        </tr>
         <tr>
            <td class="label">allows help requests:</td>
            <td class="input"><input name="helpRequests" type="checkbox" checked  />
            </td>
        </tr>
         <tr>
            <td class="label">Use default buildservers:</td>
            <td class="input"><input name="useDefaultBuildservers" type="checkbox" checked  />
            </td>
        </tr>
        <tr>
           <td class="label">Default for editing source code in browser:</td>
            <td class="input">
            <select name="browserEditing">
            <option value="prohibited">Prohibited</option>
            <option value="discouraged" selected="selected">Discouraged</option>
            <option value="allowed">Allowed</option>
            </select>
            </td>
        </tr>
        <tr  class="submit"><td colspan=2>
            <input type="submit" value="Create course">
    </table>
    </form>
    <ss:script file="jsrender.js" />
    <script type="text/javascript" src="https://ajax.aspnetcdn.com/ajax/jquery.validate/1.9/jquery.validate.min.js"></script>
    <script type="text/javascript">
   		var $my = {
   				sectionInput: $("#section-name-input"),
   				sectionDropdown: $("#course-section-dropdown"),
   				hiddenInput: $("#hidden-section-list"),
   				sectionList: []
   		};
   		$.template("optionTemplate",
   				"<option value='{{=Section}}'>{{=Section}}</option>");
   		
   		function addSection() {
    		var newVal = $my.sectionInput.val();
    		if (!newVal) {
    			alert("Must specify a section name");
    			return;
    		}
    		if (!newVal.match(/^[a-zA-Z0-9]+$/)) {
    			alert("Invalid section name " + newVal);
    			$my.sectionInput.select();
    			return;
    		}
    		if ($my.sectionList.indexOf(newVal) == -1) {
    			$my.sectionDropdown.append($.render({Section: newVal}, "optionTemplate"));
    			$my.sectionList.push(newVal);
    		}
    		$my.sectionInput.val('');
    		$my.sectionDropdown.val(newVal);
    		$my.sectionInput.focus();
    		$my.hiddenInput.val($my.sectionList.join(','));
   		}
   		
    	$("#edit-course-sections").click(function(event) {
    		addSection();
    	});
    	$my.sectionInput.keypress(function(event) {
    		if (event.which == 13) {
    			event.preventDefault();
    			addSection();
    		}
    	});
    	$("#clear-course-sections").click(function(event){
    		$my.sectionInput.val('');
    		$my.sectionDropdown.empty();
    		$my.hiddenInput.val('');
    		$my.sectionList = [];
    	});
    </script>
