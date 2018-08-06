package com.ge.research.semtk.auth.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.edc.JobEndpointProperties;
import com.ge.research.semtk.test.IntegrationTestUtility;

public class AuthorizationTest_IT {
	
	@Test
	public void testCheckOwnership() {
		ThreadAuthenticator.authenticateThisThread("user1");
		
		// same owner
		try {
			AuthorizationManager.throwExceptionIfNotJobOwner("user1", "item");
		} catch (com.ge.research.semtk.auth.AuthorizationException e) {
			e.printStackTrace();
			fail("Authorization failed");
		}

		// different owner
		try {
			AuthorizationManager.throwExceptionIfNotJobOwner("user2", "item");
			fail("No exception thrown for bad ownership");
		} catch (com.ge.research.semtk.auth.AuthorizationException e) {
		}
	}
	
	@Test
	public void testCheckAdmin() {
		ThreadAuthenticator.authenticateThisThread("user1");
		ThreadAuthenticator.setJobAdmin(true);
		
		try {
			AuthorizationManager.throwExceptionIfNotJobOwner("user1", "item");
		} catch (com.ge.research.semtk.auth.AuthorizationException e) {
			e.printStackTrace();
			fail("Authorization failed");
		} 
		
		ThreadAuthenticator.setJobAdmin(false);
		try {
			AuthorizationManager.throwExceptionIfNotJobOwner("user5", "item");
			fail("Admin didn't reset");
		} catch (com.ge.research.semtk.auth.AuthorizationException e) {

		} 
		
	}
	
	@Test
	public void testJobAdminAuthorize() throws Exception {
		
		assertEquals("Some Job admins are mysteriously pre-loaded", 0, AuthorizationManager.getJobAdmins().size());
		
		authorize();
		
		// Since test connect to a useful triplestore, we don't want to write authorizations to test
		if (AuthorizationManager.getJobAdmins().size() == 0) {
			System.out.println("Can't test job admin authorization because no job admins are loaded");
			return;
		}
		
		String jobAdmin = AuthorizationManager.getJobAdmins().get(0);
		
		ThreadAuthenticator.authenticateThisThread(jobAdmin);
		
		try {
			AuthorizationManager.throwExceptionIfNotJobOwner("anyOtherUser", "item");
			AuthorizationManager.throwExceptionIfNotJobOwner(jobAdmin, "item");

		} catch (com.ge.research.semtk.auth.AuthorizationException e) {
			e.printStackTrace();
			fail("Authorization failed for a jobAdmin: " + jobAdmin);
		} 
		
	}
	
	private void authorize() throws Exception {
		AuthorizationManager.authorize( IntegrationTestUtility.getEndpointProperties(),
				                        IntegrationTestUtility.getAuthorizationProperties() );
	}

}
