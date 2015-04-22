package org.fiware.apps.marketplace.it;

/*
 * #%L
 * FiwareMarketplace
 * %%
 * Copyright (C) 2015 CoNWeT Lab, Universidad Politécnica de Madrid
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of copyright holders nor the names of its contributors
 *    may be used to endorse or promote products derived from this software 
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;

import org.apache.catalina.startup.Tomcat;
import org.apache.commons.codec.binary.Base64;
import org.fiware.apps.marketplace.model.APIError;
import org.fiware.apps.marketplace.model.ErrorType;
import org.fiware.apps.marketplace.model.User;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.vorburger.mariadb4j.DB;


public class UsersServiceIT {
	
	private static TemporaryFolder baseDir = new TemporaryFolder();	
	private static Tomcat tomcat = new Tomcat();
	private static DB embeddedDB;
	private static String endPoint;
	
	private final static String MODIFIED_WAR_NAME = "WMarket-Integration.war";
	private final static String DATABASE = "marketplace-test";
	
	private final static String MESSAGE_TOO_LONG = "This field must not exceed %d chars."; 
	private final static String MESSAGE_TOO_SHORT = "This field must be at least %d chars.";
	private final static String MESSAGE_FIELD_REQUIRED = "This field is required.";
	private final static String MESSAGE_INVALID_DISPLAY_NAME = 
			"This field only accepts letters and white spaces.";
	private final static String MESSAGE_EMAIL_ALREADY_REGISTERED = "This email is already registered.";
	private final static String MESSAGE_INVALID_EMAIL = "This field must be a valid email.";
	private final static String MESSAGE_INVALID_PASSWORD = "Password must contain one number, one letter and one "
				+ "unique character such as !#$%&?";
	private final static String MESSAGE_NOT_AUTHORIZED = "You are not authorized to update user";
	
	
	private static int getFreePort() throws IOException {
		
		ServerSocket socket = new ServerSocket(0);
		int port = socket.getLocalPort();
		socket.close();
		
		return port;
	}
	
	@BeforeClass
	public static void startUp() throws Exception {
		
		// Initialize DB
		int port = getFreePort();
		embeddedDB = DB.newEmbeddedDB(port);
		embeddedDB.start();
		embeddedDB.createDB(DATABASE);
						
		// Initialize baseDir and the webapps directory
		baseDir.create();
		File webApps = baseDir.newFolder("webapps");
		
		// Set up Tomcat
		tomcat.setPort(0);											// Automatic port
		tomcat.setBaseDir(baseDir.getRoot().getAbsolutePath());		// Base Dir
		tomcat.addContext("/", webApps.getAbsolutePath());			// Context
		
		// Create properties
		Properties properties = new Properties();
		properties.setProperty("jdbc.driverClassName", "com.mysql.jdbc.Driver");
		properties.setProperty("jdbc.url", String.format("jdbc:mysql://localhost:%d/%s", port, DATABASE));
		properties.setProperty("jdbc.username", "root");
		properties.setProperty("jdbc.password", "");
		
		File propertiesFile = baseDir.newFile("properties.properties");
		propertiesFile.createNewFile();
		properties.store(new FileOutputStream(propertiesFile), "");
		
		// Copy the WAR (the original one cannot be modified)
		String projectDirectory = Paths.get(".").toAbsolutePath().toString();
		String modifiedWarPath = projectDirectory + "/target/" + MODIFIED_WAR_NAME;
		
	    Path originalWar = Paths.get(projectDirectory + "/target/FiwareMarketplace.war");
	    Path modifiedWar = Paths.get(modifiedWarPath);
	    Files.copy(originalWar, modifiedWar, StandardCopyOption.REPLACE_EXISTING);
	    
	    // Modify properties using the file created previously
	    FileSystem fs = FileSystems.newFileSystem(modifiedWar, null);
        Path fileInsideZipPath = fs.getPath("WEB-INF/classes/properties/database.properties");
        // Copy properties into the WAR
        Files.copy(propertiesFile.toPath(), fileInsideZipPath, StandardCopyOption.REPLACE_EXISTING);
        fs.close();
        
        // Add modified WAR       
		tomcat.addWebapp("FiwareMarketplace", modifiedWarPath);
        
		// Start up
		tomcat.start();

		// End Point depends on the Tomcat port
		endPoint = String.format("http://localhost:%d/FiwareMarketplace", tomcat.getConnector().getLocalPort());
	}
	
	@AfterClass
	public static void tearDown() {
		// Delete
		baseDir.delete();
	}
	
	@Before
	public void setUp() throws Exception {
		// Truncate all tables...
		embeddedDB.run("DELETE FROM offerings;", "root", null, DATABASE);
		embeddedDB.run("DELETE FROM descriptions;", "root", null, DATABASE);
		embeddedDB.run("DELETE FROM stores;", "root", null, DATABASE);
		embeddedDB.run("DELETE FROM users;", "root", null, DATABASE);
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////// CREATE ///////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	private void checkUser(String userName, String displayName, String company) {
		Client client = ClientBuilder.newClient();
		User user = client.target(endPoint + "/api/v2/user/" + userName).request(MediaType.APPLICATION_JSON)
				.get(User.class);
		
		assertThat(user.getUserName()).isEqualTo(userName);
		assertThat(user.getDisplayName()).isEqualTo(displayName);
		assertThat(user.getCompany()).isEqualTo(company);

	}
	
	private void checkAPIError(Response response, int status, String field, String message, ErrorType errorType) {
		
		assertThat(response.getStatus()).isEqualTo(status);
				
		APIError error = response.readEntity(APIError.class);
		assertThat(error.getField()).isEqualTo(field);
		assertThat(error.getErrorMessage()).isEqualTo(message);
		assertThat(error.getErrorType()).isEqualTo(errorType);

	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////// CREATE ///////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	private Response createUser(String displayName, String email, String password, String company) {
		
		SerializableUser user = new SerializableUser();
		user.setDisplayName(displayName);
		user.setEmail(email);
		user.setPassword(password);
		user.setCompany(company);
		
		Client client = ClientBuilder.newClient();
		Response response = client.target(endPoint + "/api/v2/user").request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(user, MediaType.APPLICATION_JSON));
		
		return response;

	}
	
	private Response createUser(String displayName, String email, String password) {
		return createUser(displayName, email, password, null);
	}
		
	@Test
	public void testUserCreation() {
		
		String userName = "fiware-example";
		String displayName = "FIWARE Example";
		String email = "example@example.com";
		String password = "password!1";
		String company = "UPM";
		
		Response response = createUser(displayName, email, password, company);
		
		assertThat(response.getStatus()).isEqualTo(201);
		assertThat(response.getHeaderString("Location")).isEqualTo(endPoint + "/api/v2/user/" + userName);
		
		checkUser(userName, displayName, company);
	}
	
	@Test
	public void testUserCreationUserRegistered() throws InterruptedException {
		
		String mail = "example@example.com";
		
		createUser("FIWARE Example", mail, "password!1a");
		Response userExistResponse = createUser("Other user name", mail, "anotherPassword!1");
				
		checkAPIError(userExistResponse, 400, "email", MESSAGE_EMAIL_ALREADY_REGISTERED, 
				ErrorType.VALIDATION_ERROR);		
	}
	
	@Test
	public void testUserCreationInvalidDisplayName() {
		Response response = createUser("FIWARE Example 1", "example@example.com", "password!1");
		checkAPIError(response, 400, "displayName", MESSAGE_INVALID_DISPLAY_NAME, 
				ErrorType.VALIDATION_ERROR);	

	}
	
	@Test
	public void testUserCreationDisplayNameMissing() {
		Response response = createUser("", "example@example.com", "password!1");
		// userName is got based on displayName. userName is checked previously
		checkAPIError(response, 400, "userName", MESSAGE_FIELD_REQUIRED, ErrorType.VALIDATION_ERROR);	
	}
	
	@Test
	public void testUserCrationDisplayNameTooLong() {
		Response response = createUser("ABCDEFGHIJKMLNOPQRSTUVWXYZABCDEFGHIJKMLNOPQRSTUVWXYZ", 
				"example@example.com", "password!1");
		checkAPIError(response, 400, "displayName", String.format(MESSAGE_TOO_LONG, 30), 
				ErrorType.VALIDATION_ERROR);	
	}
	
	public void testUserCrationDisplayNameTooShort() {
		Response response = createUser("a", "example@example.com", "password!1");
		checkAPIError(response, 400, "displayName", String.format(MESSAGE_TOO_SHORT, 3), 
				ErrorType.VALIDATION_ERROR);	
	}
	
	@Test
	public void testUserCreationInvalidMail() {
		Response response = createUser("FIWARE Example", "example@examplecom", "password!1");
		checkAPIError(response, 400, "email", MESSAGE_INVALID_EMAIL, ErrorType.VALIDATION_ERROR);	
	}
	
	@Test
	public void testUserCreationDisplayEmailMissing() {
		Response response = createUser("FIWARE Example", "", "password!1");
		checkAPIError(response, 400, "email", MESSAGE_FIELD_REQUIRED, ErrorType.VALIDATION_ERROR);	
	}
	
	@Test
	public void testUserCreationInvalidPassword() {
		Response response = createUser("FIWARE Example", "example@example.com", "password!");
		checkAPIError(response, 400, "password", MESSAGE_INVALID_PASSWORD, ErrorType.VALIDATION_ERROR);	
	}
	
	@Test
	public void testUserCreationDisplayPasswordMissing() {
		Response response = createUser("FIWARE Example", "example@example.com", "");
		checkAPIError(response, 400, "password", MESSAGE_FIELD_REQUIRED, ErrorType.VALIDATION_ERROR);	
	}
	
	@Test
	public void testUserCrationPasswordTooLong() {
		Response response = createUser("FIWARE EXAMPLE", "example@example.com", 
				"ABCDEFGHIJKMLNOPQRSTUVWXYZABCDEFGHIJKMLNOPQRSTU!1VWXYZ");
		checkAPIError(response, 400, "password", String.format(MESSAGE_TOO_LONG, 30), ErrorType.VALIDATION_ERROR);	
	}
	
	public void testUserCrationPasswordTooShort() {
		Response response = createUser("FIWARE EXAMPLE", "example@example.com", "passw1!");
		checkAPIError(response, 400, "password", String.format(MESSAGE_TOO_SHORT, 8), ErrorType.VALIDATION_ERROR);	
	}
	
	@Test
	public void testUserCrationCompanyTooLong() {
		Response response = createUser("FIWARE EXAMPLE", "example@example.com", "password!1", 
				"ABCDEFGHIJKMLNOPQRSTUVWXYZABCDEFGHIJKMLNOPQRSTU");
		checkAPIError(response, 400, "company", String.format(MESSAGE_TOO_LONG, 30), ErrorType.VALIDATION_ERROR);	
	}
	
	public void testUserCrationCompanyTooShort() {
		Response response = createUser("FIWARE EXAMPLE", "example@example.com", "password!1", "a");
		checkAPIError(response, 400, "company", String.format(MESSAGE_TOO_SHORT, 8), ErrorType.VALIDATION_ERROR);	
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////// UPDATE///////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	private Response updateUser(String authUserName, String authPassword, String userName, 
			String displayName, String email, String newPassword, String company) {
		
		SerializableUser user = new SerializableUser();
		user.setDisplayName(displayName);
		user.setEmail(email);
		user.setPassword(newPassword);
		user.setCompany(company);
		
		String authorization = authUserName + ":" + authPassword;
		String encodedAuthorization = new String(Base64.encodeBase64(authorization.getBytes()));
		
		Client client = ClientBuilder.newClient();
		Response response = client.target(endPoint + "/api/v2/user/" + userName)
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", "Basic " + encodedAuthorization)
				.post(Entity.entity(user, MediaType.APPLICATION_JSON));
		
		return response;

	}
	
	private Response updateUser(String authUserName, String authPassword, String userName, 
			String displayName, String email, String newPassword) {
		return updateUser(authUserName, authPassword, userName, displayName, email, newPassword, null);
	}
	
	@Test
	public void testUpdateDisplayName() {
	
		String userName = "marketplace";
		String displayName = "Marketplace";
		String newDisplayName = "FIWARE Example";
		String password = "password1!a";
		String email = "example@example.com";
		String company = "UPM";
		
		// Create user
		Response createUserReq = createUser(displayName, email, password, company);
		assertThat(createUserReq.getStatus()).isEqualTo(201);
		
		// Update user
		Response updateUserReq = updateUser(userName, password, userName, newDisplayName, email, password, company);
		assertThat(updateUserReq.getStatus()).isEqualTo(200);
		
		checkUser(userName, newDisplayName, company);
	}
	
	private void testUpdateFieldError(String field, String errorMessage, String newDisplayName, 
			String newEmail, String newPassword, String newCompany) {
		String userName = "marketplace";
		String displayName = "Marketplace";
		String password = "password1!a";
		String email = "example@example.com";
		String company = "UPM";
		
		// Create user
		Response createUserReq = createUser(displayName, email, password, company);
		assertThat(createUserReq.getStatus()).isEqualTo(201);
		
		// Update user
		Response updateUserReq = updateUser(userName, password, userName, newDisplayName, 
				newEmail, newPassword, newCompany);
		checkAPIError(updateUserReq, 400, field, errorMessage, ErrorType.VALIDATION_ERROR);	

		// Check that displayName remains the same
		checkUser(userName, displayName, company);

	}
	
	private void testUpdateDisplayNameError(String newDisplayName, String errorMessage) {
		testUpdateFieldError("displayName", errorMessage, newDisplayName, null, null, null);
	}
	
	@Test
	public void testUpdateDisplayNameTooLong() {
		testUpdateDisplayNameError("ABCDEFGHIJKMLNOPQRSTUVWXYZABCDEFGHIJKMLNOPQRSTUVWXYZ", 
				String.format(MESSAGE_TOO_LONG, 30));
	}
	
	@Test
	public void testUpdateUserDisplayNameTooShort() {
		testUpdateDisplayNameError("a", String.format(MESSAGE_TOO_SHORT, 3));
	}
	
	@Test
	public void testUpdateUserPassword() {
	
		String userName = "marketplace";
		String displayName = "Marketplace";
		String password = "password1!a";
		String newPassword = "passworda!1";
		String email = "example@example.com";
		
		// Create user
		Response createUserReq = createUser(displayName, email, password);
		assertThat(createUserReq.getStatus()).isEqualTo(201);
		
		// Update user
		Response updateUserReq = updateUser(userName, password, userName, displayName, email, newPassword);
		assertThat(updateUserReq.getStatus()).isEqualTo(200);
		
		// Check that the user cannot be updated with the old password
		Response updateUserReq1 = updateUser(userName, password, userName, displayName, email, newPassword);
		assertThat(updateUserReq1.getStatus()).isEqualTo(401);
		
		// Check that the user can be updated with the new password
		Response updateUserReq2 = updateUser(userName, newPassword, userName, displayName, email, newPassword);
		assertThat(updateUserReq2.getStatus()).isEqualTo(200);

	}
	
	private void testUpdatePasswordError(String newPassword, String errorMessage) {
		testUpdateFieldError("password", errorMessage, null, null, newPassword, null);
	}
	
	@Test
	public void testUpdatePasswordInvalid() {
		testUpdatePasswordError("password!", MESSAGE_INVALID_PASSWORD);
	}
	
	@Test
	public void testUpdatePasswordTooLong() {
		testUpdatePasswordError("ABCDEFGHIJKMLNOPQRSTUVWXYZABCDEFGHIJKMLNOPQRSTU!1VWXYZ", 
				String.format(MESSAGE_TOO_LONG, 30));
	}
	
	@Test
	public void testUpdatePasswordTooShort() {
		testUpdatePasswordError("a", String.format(MESSAGE_TOO_SHORT, 8));
	}
	
	@Test
	public void testUpdateEmailInvalid() {
		testUpdateFieldError("email", MESSAGE_INVALID_EMAIL, null, "t@", null, null);
	}
	
	@Test
	public void testUpdateEmailAlreadyExist() {
		
		String repeatedEmail = "example@example.com";		
		String userName = "fiware-example";
		String password = "anotherPassword!1";
		
		createUser("Fiware Example", "new_email@example.com", password);
		createUser("Other user name", repeatedEmail, "password!1a");				
		Response updateUserReq = updateUser(userName, password, userName, null, repeatedEmail, null);
		checkAPIError(updateUserReq, 400, "email", MESSAGE_EMAIL_ALREADY_REGISTERED, ErrorType.VALIDATION_ERROR);	
	}
	
	private void testUpdateCompanyError(String newCompany, String errorMessage) {
		testUpdateFieldError("company", errorMessage, null, null, null, newCompany);
	}
	
	@Test
	public void testUpdateCompanyTooShort() {
		testUpdateCompanyError("a", String.format(MESSAGE_TOO_SHORT, 3));
	}
	
	@Test
	public void testUpdateCompanyTooLong() {
		testUpdateCompanyError("ABCDEFGHIJKMLNOPQRSTUVWXYZABCDEFGHIJKMLNOPQRSTUVWXYZ", 
				String.format(MESSAGE_TOO_LONG, 30));
	}
	
	@Test
	public void testUpdateUserWithAnotherUser() {
		
		String userName1 = "marketplace";
		String displayName1 = "Marketplace";
		String password1 = "password1!a";
		String email1 = "example@example.com";
		
		String userName2 = "marketplacebb";
		String displayName2 = "MarketplaceBB";
		String password2 = "password1!b";
		String email2 = "example2@example.com";
		
		// Create both users
		Response createUserReq1 = createUser(displayName1, email1, password1);
		assertThat(createUserReq1.getStatus()).isEqualTo(201);
		Response createUserReq2 = createUser(displayName2, email2, password2);
		assertThat(createUserReq2.getStatus()).isEqualTo(201);
		
		// Update user with another user should fail
		Response updateUserReq = updateUser(userName2, password2, userName1, displayName1, email1, password1);
		checkAPIError(updateUserReq, 403, null, MESSAGE_NOT_AUTHORIZED, ErrorType.FORBIDDEN);
	}

	
	
	
	public static class SerializableUser {
		
		private String displayName;
		private String email;
		private String password;
		private String company;
		
		@XmlElement
		public String getDisplayName() {
			return displayName;
		}
		
		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}
		
		@XmlElement
		public String getEmail() {
			return email;
		}
		
		public void setEmail(String email) {
			this.email = email;
		}
		
		@XmlElement
		public String getPassword() {
			return password;
		}
		
		public void setPassword(String password) {
			this.password = password;
		}

		@XmlElement
		public String getCompany() {
			return company;
		}

		public void setCompany(String company) {
			this.company = company;
		}
		
	}

}
