<%@page import="java.io.File"%>
<%-- <% File[] listOfFiles = (File[]) request.getAttribute("robotium_files"); %> --%>
<% File[] listOfFiles = (File[]) getServletContext().getAttribute("robotium_files"); %>
<html>
<body>
<form action="/autograder/autograderservlet" method="post" enctype="multipart/form-data">
<table>
<!-- <tr>
<td>App Inventor username:</td>
<td><input type="text" name="username" size="30"></td><td></td>
</tr> -->
<tr><td>Choose the Robotium tester APK:</td><td><select name="robotiumfile">
<% for (int i = 0; i < listOfFiles.length; i++) { %>
<option value="<%= listOfFiles[i].getName() %>"><%= listOfFiles[i].getName() %></option>
<% } %>
</select></td></tr>
<tr>
<td>Run headless?</td><td><input type="checkbox" name="headless" value="true"></td>
</tr>
<tr><td>Upload your APK file to test:</td>
<td><input type="file" name="datasize" size="30"></td>
</tr>
<!-- <p>Activity:
<input type="text" name="action" size="50" value="android.intent.action.MAIN">
<p>Component:
<input type="text" name="class" size="50"> -->
<tr><td></td>
<td><input type="submit" value="Run it!"></td><td></td>
</tr></table>
</form>
</body>
</html>