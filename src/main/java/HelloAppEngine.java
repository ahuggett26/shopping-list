import java.io.Console;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.logging.ConsoleHandler;
import java.io.InputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.users.*;
import com.google.appengine.tools.cloudstorage.*;
import com.google.cloud.storage.*;

@WebServlet(
    name = "ShoppingList",
    urlPatterns = {"/shoppinglist"}
)
public class HelloAppEngine extends HttpServlet {

	GcsService service;
	GcsFilename fileName;
	GcsInputChannel input;
	
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) 
      throws IOException {

	response.setContentType("text/html");	// set text type
	response.setCharacterEncoding("UTF-8");

    UserService userService = UserServiceFactory.getUserService();
    String Uri = request.getRequestURI();
    String urlToLogin;

    if (userService.isUserLoggedIn() == false) {	//checks whether the user needs to be signed in
    	response.getWriter().println("<p>You are not logged in</p>");
    	urlToLogin = userService.createLoginURL(Uri);
    	
    	response.getWriter().println("<p>Please <a href=\"" +urlToLogin+ "\">sign in</a>.</p>");
    	return;
    }
    
    Storage storage = StorageOptions.getDefaultInstance().getService();	// ensure that the bucket exists
    Bucket bucket;
    if (storage.get("appproject1-199022.appspot.com") == null) {
    	bucket = storage.create(BucketInfo.of("appproject1-199022.appspot.com"));
    }
    else {
    	bucket = storage.get("appproject1-199022.appspot.com");
    }
    
    service = GcsServiceFactory.createGcsService();
    fileName = new GcsFilename(bucket.getName(),userService.getCurrentUser().getUserId());	//set the file to be the users name
    
    if (service.getMetadata(fileName) != null) { //says that the file exists and thus there may be items to print out
    	response.getWriter().println("<h2>Items in your shopping list:</h2>");
    	input = service.openReadChannel(fileName, 0);
    	String contents = readData(Channels.newInputStream(input));
    	if (contents != "") {	// checks whether the contents file actually contains anything
    		String[] itemArray = contents.split(";");
    		if (contents.endsWith("EDIT;")) {	// Check if an item has been selected to be edited
    			String minusEdit = contents.substring(contents.length()-8 ,contents.length()-5);	//end index is exclusive, start index inclusive
    			int noToEdit = Integer.parseInt(minusEdit);
    			
    			response.getWriter().println("<form name=\"itemForm\" method=\"POST\">");
    			for (int i = 0; i < itemArray.length - 1; i++) {	// -1 & < to remove the edit value at the end and the end "" after the last ';'
    				if (i == noToEdit) {		//find the item that should be edited
    					response.getWriter().println("<p><input type=\"Text\" name=\"editInput\" value=\"" + itemArray[i] + "\">");
    					response.getWriter().println("<input type=\"Submit\" name=\"saveEdit\" value=\"Save Edit\">");
    					response.getWriter().println("<input type=\"Submit\" name=\"deleteItem\" value=\"Delete Item\"></p>");
    				}
    				else {
						response.getWriter().println("<p><input type=\"radio\" name=\"rBtnItem\" value=\"itemNo" + i +"\">" + itemArray[i] + "</p>");
    				}
        		}
    		}
    		else {
    			response.getWriter().println("<form name=\"itemForm\" method=\"POST\">");
    			for (int i = 0; i < itemArray.length; i++) {
        			// split the string into an array with the same ';' as the split point
        			// if there is a number in the file, then that item in the array is edited
        			response.getWriter().println("<p><input type=\"radio\" name=\"rBtnItem\" value=\"itemNo" + i +"\">" + itemArray[i] + "</p>");
    				
        		}
    		}
    		response.getWriter().println("<input type=\"Submit\" name=\"editItem\" value=\"Edit Selected Item\">");
    	    response.getWriter().println("</form>");
    	}
    	else {
    		response.getWriter().println("<h2>You have no items currently in your shopping list</h2>");
    	}
    }
    else { // the file does not exist, no items to print
    	response.getWriter().println("<h2>You have no items currently in your shopping list</h2>");
    }
    
    response.getWriter().println("<p>Enter a new item for your shopping list below:</p>");
    response.getWriter().println("<form name=\"inputForm\" method=\"POST\">");	// Allow the user to input a item to the shopping list
    response.getWriter().println("<input type=\"Text\" name=\"input\">" );
    response.getWriter().println("<input type=\"Submit\" name=\"enterItem\" value=\"Enter\">");
    response.getWriter().println("</form>");
    response.getWriter().println("<form name=\"deleteItemsForm\" method=\"POST\">");	//Allow the user to delete the shopping list
    response.getWriter().println("<input type=\"Submit\" name=\"deleteAll\" value=\"Delete All Items\">");
    response.getWriter().println("</form>");
    response.getWriter().println("");
    
    response.getWriter().println("<p><i>Click to go </i><a href=\"index.jsp \">back</a><i> to the home page</i></p>");	//Allow the user to return
  }
  
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
	  if (request.getParameter("enterItem") != null) {
		  String usersInput = request.getParameter("input");	//get the input and convert into a string (merging with current string)
		  String concat = usersInput + ";";
		  
		  boolean f = addToFile(concat, response);
		  if (f == false) {
			  return;
		  }
	  }
	  else if (request.getParameter("deleteAll") != null) {
		  response.getWriter().println("<p>Are you sure that you would like to delete all items in the shopping list?</p>");
		  response.getWriter().println("<form name=\"confirmationForm\" method=\"POST\">");
		  response.getWriter().println("<input type=\"Submit\" name=\"confirmYes\" value =\"Yes\">");
		  response.getWriter().println("<input type=\"Submit\" name=\"confirmNo\" value =\"No\">");
		  response.getWriter().println("</form>");
		  return;
	  }
	  else if (request.getParameter("confirmNo") != null) {
		  // do nothing, the page should refresh to show the shopping list again
	  }
	  else if (request.getParameter("confirmYes") != null) {
		  service.delete(fileName);
	  }
	  else if(request.getParameter("editItem") != null) {
		  //add a value to the file, saying which item is to be edited
		  String param = request.getParameter("rBtnItem");
		  
		  if (param == null) {
			  response.getWriter().println("<p>You must select an item before you edit or delete it.</p>");
			  response.getWriter().println("<p><form name=\"acceptError\" method=\"POST\">");
			  response.getWriter().println("<input type=\"Submit\" name=\"acceptError\" value=\"OK\"></form></p>");
			  return;
		  }
		  
		  if (param.length() == 8) { //if there is a 2 digit index for edited item
			  param = param.substring(0, 6) + "0" + param.substring(6);
		  }
		  else if (param.length() == 7) { //1 digit index
			  param = param.substring(0, 6) + "00" + param.substring(6);
		  }
		  
		  String concat = param + "EDIT;";	// getting the param of rBtnItem gives the number of the element to be edited
		  boolean f = addToFile(concat, response);
		  if (f == false) {
			  return;
		  }
	  }
	  else if (request.getParameter("saveEdit") != null) {
		  //needs to remove the current item before adding the edited item
		  String editedInput = request.getParameter("editInput");
		  String concat = editedInput + ";";
		  
		  boolean f = deleteItem(concat, response);
		  if (f == false) {
			  return;
		  }
	  }
	  else if (request.getParameter("deleteItem") != null) {
		  deleteItem("", response);
	  }
	  else if (request.getParameter("acceptError") != null) {
		  // do nothing, the page should refresh to show the shopping list again
	  }
	  else if (request.getParameter("acceptDuplicate") != null || request.getParameter("acceptDuplicate2") != null) {
		  // ^^
	  }
	  else if(request.getParameter("acceptTooLong") != null) {
		  // ^^
	  }
	  
	  response.sendRedirect(request.getServletPath()); // reload the .java page
  }
  
  private boolean addToFile(String concat, HttpServletResponse response) throws IOException{
	  
	  String currentList = readData(Channels.newInputStream(service.openReadChannel(fileName, 0)));
	  if (currentList.endsWith("EDIT")) {	// remove any current existing edit indicators
		  currentList = currentList.substring(0, currentList.length()-14);
	  }
	  
	  String finalList = currentList.concat(concat);
	  
	  String[] listCheck = finalList.split(";");

	  if (listCheck.length <= 1000) {	// can only be up to 1000 items in the shopping list
		  boolean listOkay = checkList(listCheck);
		  if (listOkay == true) {
			  byte[] byteArray = finalList.getBytes();	//turn into byte array so it can be saved
			  
			  GcsFileOptions options = GcsFileOptions.getDefaultInstance();
			  java.nio.ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
			  service.createOrReplace(fileName, options, byteBuffer);	// create (or replace) the file
			  return(true);
		  }
		  else {
			  response.getWriter().println("<p>That item is already on your shopping list!</p>");
			  response.getWriter().println("<p><form name=\"acceptError2\" method=\"POST\">");
			  response.getWriter().println("<input type=\"Submit\" name=\"acceptDuplicate\" value=\"Return Back\"></form></p>");
			  return(false);
		  }
	  }
	  else {
		  response.getWriter().println("<p>You already have 1000 items on your shopping list, some must be deleted first.</p>");
		  response.getWriter().println("<p><form name=\"acceptError3\" method=\"POST\">");
		  response.getWriter().println("<input type=\"Submit\" name=\"acceptTooLong\" value=\"OK\"></form></p>");
		  return(false);
	  }
	  
  }
  
  private boolean deleteItem(String addEdit, HttpServletResponse response) throws IOException{
	  input = service.openReadChannel(fileName, 0);
	  String itemsString = readData(Channels.newInputStream(input));
	  int editNo = Integer.parseInt(itemsString.substring(itemsString.length()-8 ,itemsString.length()-5));
	  String[] itemList = itemsString.split(";");
	  String finalList = "";
	  
	  for (int i = 0; i < itemList.length - 1; i++) { //-1 & < to remove the last EDIT part and "" after the last ';'
		  if(i != editNo) {
			  finalList = finalList.concat(itemList[i] + ";");
		  }
	  }
	  
	  if (addEdit != "") {
		  finalList = finalList.concat(addEdit);
		  boolean correctList = checkList(finalList.split(";"));
		  if (correctList != true) {
			  response.getWriter().println("<p>That item is already on your shopping list!</p>");
			  response.getWriter().println("<p><form name=\"acceptError4\" method=\"POST\">");
			  response.getWriter().println("<input type=\"Submit\" name=\"acceptDuplicate2\" value=\"Return Back\"></form></p>");
			  return(false);
		  }
	  }
	  
	  byte[] byteArray = finalList.getBytes();
	  GcsFileOptions options = GcsFileOptions.getDefaultInstance();
	  java.nio.ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
	  service.createOrReplace(fileName, options, byteBuffer);
	  return(true);
  }
  
  private boolean checkList(String[] items) {
	  boolean answer = true;
	  String lastAdded = items[items.length - 1];
	  for (int i = 0; i < items.length - 1; i++) {	// find if the added item is a duplicate
		  if (lastAdded.equalsIgnoreCase(items[i])) {
			  answer = false;
		  }
	  }
	  return(answer);
  }
  
  private String readData(InputStream inp) throws IOException {
	  // turn the input stream into a manipulatable string
	  java.util.Scanner s = new java.util.Scanner(inp).useDelimiter("\\A");
	  return s.hasNext() ? s.next() : "";	// ternary if-then-else statement, so if there is another scanner object, return it, else return null.

  }
}