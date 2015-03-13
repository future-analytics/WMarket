package org.fiware.apps.marketplace.bo;

/*
 * #%L
 * FiwareMarketplace
 * %%
 * Copyright (C) 2012 SAP
 * Copyright (C) 20154444 CoNWeT Lab, Universidad Politécnica de Madrid
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.fiware.apps.marketplace.bo.impl.UserBoImpl;
import org.fiware.apps.marketplace.dao.UserDao;
import org.fiware.apps.marketplace.exceptions.NotAuthorizedException;
import org.fiware.apps.marketplace.exceptions.UserNotFoundException;
import org.fiware.apps.marketplace.exceptions.ValidationException;
import org.fiware.apps.marketplace.model.User;
import org.fiware.apps.marketplace.model.validators.UserValidator;
import org.fiware.apps.marketplace.security.auth.UserAuth;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.springframework.security.authentication.ClientAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserBoImplTest {
	
	@Mock private UserAuth userAuthMock;
	@Mock private UserValidator userValidatorMock;
	@Mock private UserDao userDaoMock;
	@Mock private PasswordEncoder passwordEncoder;
	@InjectMocks private UserBoImpl userBo;
	
	private static final String ENCODED_PASSWORD = "ENCODED PASSWORD";
	
	@Before 
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(passwordEncoder.encode(anyString())).thenReturn(ENCODED_PASSWORD);
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////// SAVE ////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	private void testSaveException(User user) throws Exception {
		// Call the method
		try {
			userBo.save(user);
			fail("Exception expected");
		} catch (Exception e) {
			// Verify that the DAO has not been called
			verify(userDaoMock, never()).save(user);
			
			// Throw the exception
			throw e;
		}

	}

	@Test(expected=NotAuthorizedException.class)
	public void testSaveNotAuthorized() throws Exception {
		
		User user = mock(User.class);
		doThrow(new NotAuthorizedException(user, "create user")).when(userAuthMock).canCreate(user);
		
		// Call the method and check that DAO is not called
		testSaveException(user);
	}
	
	@Test(expected=ValidationException.class)
	public void testSaveInvalidUser() throws Exception {
		
		User user = mock(User.class);
		doThrow(new ValidationException("invalid")).when(userValidatorMock).validateUser(user, true);
		
		// Call the method and check that DAO is not called
		testSaveException(user);
	}
	
	private void testSave(User user, String expectedUserName) {
		
		try {
			userBo.save(user);
			
			// Verify that the DAO has been called
			verify(userDaoMock).save(user);
			
			// Assert that the password has been encoded
			assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
			
			// Check user name
			assertThat(user.getUserName()).isEqualTo(expectedUserName);
			
		} catch (Exception e) {
			fail("Exception not expected", e);
		}
	}
	
	@Test
	public void testSaveUserNameChange() {
		User user = new User();
		user.setDisplayName("MaRkEt PlAcE&1??0");
		user.setPassword("password");
		
		// Save & check
		testSave(user, "market-place10");
	}
	
	@Test
	public void testSaveUserNameNoChanges() {
		User user = new User();
		user.setDisplayName("MaRkEt PlAcE&1??0");
		user.setPassword("password");
		user.setUserName("fiware");
		
		// Save & check
		testSave(user, "fiware");
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////// UPDATE ////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	private void testUpdateException(User user) throws Exception {
		// Call the method
		try {
			userBo.update(user);
			fail("Exception expected");
		} catch (Exception e) {
			// Verify that the DAO has not been called
			verify(userDaoMock, never()).update(user);
			
			// Throw the exception
			throw e;
		}

	}

	@Test(expected=NotAuthorizedException.class)
	public void testUpdateNotAuthorized() throws Exception {
		
		User user = mock(User.class);
		doThrow(new NotAuthorizedException(user, "create user")).when(userAuthMock).canUpdate(user);
		
		// Call the method and check that DAO is not called
		testUpdateException(user);
	}
	
	@Test(expected=ValidationException.class)
	public void testUpdateInvalidUser() throws Exception {
		
		User user = mock(User.class);
		doThrow(new ValidationException("invalid")).when(userValidatorMock).validateUser(user, false);
		
		// Call the method and check that DAO is not called
		testUpdateException(user);
	}
	
	@Test
	public void testUpdate() {
		
		try {
			User user = new User();
			user.setPassword("password");
			userBo.update(user);
			
			// Verify that the DAO has been called
			verify(userDaoMock).update(user);
			
			// Assert that the password has been encoded
			assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
						
		} catch (Exception e) {
			fail("Exception not expected", e);
		}
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////// DELETE ////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	@Test(expected=NotAuthorizedException.class)
	public void testDeleteNotAuthorized() throws Exception {

		User user = mock(User.class);
		doThrow(new NotAuthorizedException(user, "create user")).when(userAuthMock).canUpdate(user);
		
		try {			
			// Call the method
			userBo.update(user);
			fail("Exception expected");
		} catch (Exception e) {
			// Verify that the DAO has not been called
			verify(userDaoMock, never()).delete(user);
			
			// Throw the exception
			throw e;
		}

	}
	
	public void testDelete() throws NotAuthorizedException {
		User user = new User();
		
		// Call the method
		userBo.delete(user);
		
		// Verify that the method has been called
		verify(userDaoMock).delete(user);
	}

	
	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// FIND BY NAME /////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	@Test(expected=UserNotFoundException.class)
	public void testFindByNameException() throws Exception {
		String userName = "username";
		doThrow(new UserNotFoundException("store not found")).when(userDaoMock).findByName(userName);
		
		userBo.findByName(userName);
	}
	
	private void testFindByNameUserFound(boolean authorized) throws Exception {
		String userName = "username";
		User user = new User();
		user.setId(2);
		user.setUserName(userName);
		
		// Set up mocks
		when(userDaoMock.findByName(userName)).thenReturn(user);
		
		if (!authorized) {
			doThrow(new NotAuthorizedException(null, "find user")).when(userAuthMock).canGet(user);
		}
		
		// Call the function
		try {
			User returnedUser = userBo.findByName(userName);
			// If an exception is risen, this check is not executed
			assertThat(returnedUser).isEqualTo(user);
		} catch (Exception ex) {
			verify(userDaoMock).findByName(userName);
			
			throw ex;
		}
	}
	
	@Test(expected=NotAuthorizedException.class)
	public void testFinByNameNotAuthorized() throws Exception{
		testFindByNameUserFound(false);
	}

	@Test
	public void testFindByName() throws Exception {
		testFindByNameUserFound(true);
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////// GET USERS PAGE ////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	@Test(expected=NotAuthorizedException.class)
	public void testGetUsersPageNotAuthorized() throws Exception{
		doThrow(new NotAuthorizedException(null, "get users")).when(userAuthMock).canList();
		userBo.getUsersPage(0, 100);
	}
	
	@Test
	public void testGetUsersPage() throws NotAuthorizedException {
		List<User> users = new ArrayList<>();
		
		for (int i = 0; i < 5; i++) {
			User user = new User();
			user.setId(i);
			users.add(user);
		}
		
		when(userDaoMock.getUsersPage(anyInt(), anyInt())).thenReturn(users);
		
		// Call the function
		int offset = 0;
		int max = 100;
		assertThat(userBo.getUsersPage(offset, max)).isEqualTo(users);
		
		// Verify that the DAO is called
		verify(userDaoMock).getUsersPage(offset, max);
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////// GET ALL USERS /////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	@Test(expected=NotAuthorizedException.class)
	public void testGetAllUsersNotAuthorized() throws Exception{
		doThrow(new NotAuthorizedException(null, "get users")).when(userAuthMock).canList();
		userBo.getAllUsers();
	}
	
	@Test
	public void testGetAllUsers() throws NotAuthorizedException {
		List<User> users = new ArrayList<>();
		
		for (int i = 0; i < 5; i++) {
			User user = new User();
			user.setId(i);
			users.add(user);
		}
		
		when(userDaoMock.getAllUsers()).thenReturn(users);
		
		// Call the function
		assertThat(userBo.getAllUsers()).isEqualTo(users);
		
		// Verify that the DAO is called
		verify(userDaoMock).getAllUsers();
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////// GET CURRENT USER ///////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	
	
	private void testGetCurrentUser(User user, SecurityContext context) {
		try {
			String userName = user.getUserName();

			// Configure the mocks
			when(userDaoMock.findByName(userName)).thenReturn(user);

			// Configure the context returned by the SecurityContextHolder
			SecurityContextHolder.setContext(context);

			// Call the function
			User returnedUser = userBo.getCurrentUser();

			verify(userDaoMock).findByName(userName);
			assertThat(returnedUser).isEqualTo(user);
		} catch (Exception ex) {
			fail("Exception " + ex + " not expected");
		}
	}

	@Test
	public void testGetCurrentUserOAuth2() {
		// User
		final String userName = "oauth2username";
		User user = new User();
		user.setUserName(userName);

		// The context
		SecurityContext context = new SecurityContext() {

			private static final long serialVersionUID = 1L;

			@Override
			public void setAuthentication(Authentication auth) {
			}

			@Override
			public Authentication getAuthentication() {
				UserProfile userProfile = new UserProfile();
				userProfile.setId(userName);
				ClientAuthenticationToken auth = new ClientAuthenticationToken(null, "FIWARE", userProfile, null);	
				return auth;
			}
		};

		testGetCurrentUser(user, context);
	}
	
	@Test
	public void testGetCurrentUserLocal() {
		// The user
		final String userName = "oauth2username";
		User user = new User();
		user.setUserName(userName);

		// The context
		SecurityContext context = new SecurityContext() {

			private static final long serialVersionUID = 1L;

			@Override
			public void setAuthentication(Authentication arg0) {

			}

			@Override
			public Authentication getAuthentication() {
				UserProfile userProfile = new UserProfile();
				userProfile.setId(userName);
				Authentication auth = new Authentication() {
					
					private static final long serialVersionUID = 1L;

					@Override
					public String getName() {
						return userName;
					}
					
					@Override
					public void setAuthenticated(boolean arg0) throws IllegalArgumentException {						
					}
					
					@Override
					public boolean isAuthenticated() {
						return true;
					}
					
					@Override
					public Object getPrincipal() {
						return null;
					}
					
					@Override
					public Object getDetails() {
						return null;
					}
					
					@Override
					public Object getCredentials() {
						return null;
					}
					
					@Override
					public Collection<? extends GrantedAuthority> getAuthorities() {
						return null;
					}
				};

				return auth;
			}
		};

		testGetCurrentUser(user, context);
	}


	
	


	
	
	

	
	

	

	
	
	

}