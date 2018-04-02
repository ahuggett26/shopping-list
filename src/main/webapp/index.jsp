<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
    <%@page import = "javax.servlet.http.HttpServlet" %>
    <%@page import = "javax.servlet.http.HttpServletRequest" %>
    <%@page import = "javax.servlet.http.HttpServletResponse" %>
    <%@page import = "com.google.appengine.api.users.*" %>
    <%@page import = "java.net.URI" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <meta http-equiv="content-type" content="application/xhtml+xml; charset=UTF-8" />
    <title>Shopping List Application</title>
  </head>
<% UserService user = UserServiceFactory.getUserService(); %>
  <body>
    <h1>Shopping List Application</h1>
    <table>
      <tr>
        <td colspan="2" style="font-weight:bold;">Click for your shopping list:</td>        
      </tr>
      <tr>
        <td><a href='/shoppinglist'>Shopping List</a></td>
        <td>          </td>
        <td><% if (user.isUserLoggedIn() == true){ 
        String urlToLogout = user.createLogoutURL("/index.jsp");
        %>
        	<a href="<%=urlToLogout%>">Log Out</a>
        <%} else { %>
        <% String urlToLogin = user.createLoginURL("/index.jsp"); %>
        	<a href="<%=urlToLogin%>">Log In</a>
        <%} %>
        </td>
      </tr>
      <tr>
      	<td><i>Currently Logged in as: </i>
	      	<% if(user.isUserLoggedIn() == false) { %>
	      	<i>-</i>
	      	<%} else {%>
	      	<i>"<%= user.getCurrentUser().getNickname() %>"</i>
	      	<%} %>
      	</td>
      </tr>
    </table>
  </body>
</html>