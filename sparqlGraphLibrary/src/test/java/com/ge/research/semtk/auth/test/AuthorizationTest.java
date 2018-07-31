package com.ge.research.semtk.auth.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;

public class AuthorizationTest {
	
	@Test
	public void testCheckOwnership() {
		ThreadAuthenticator.authenticateThisThread("user1");
		
		// same owner
		try {
			AuthorizationManager.checkJobOwnership("user1", "item");
		} catch (com.ge.research.semtk.auth.AuthorizationException e) {
			e.printStackTrace();
			fail("Authorization failed");
		}

		// different owner
		try {
			AuthorizationManager.checkJobOwnership("user2", "item");
			fail("No exception thrown for bad ownership");
		} catch (com.ge.research.semtk.auth.AuthorizationException e) {
		}
	}
	
	@Test
	public void testCheckAdmin() {
		ThreadAuthenticator.authenticateThisThread("user1");
		ThreadAuthenticator.setAdmin();
		
		try {
			AuthorizationManager.checkJobOwnership("user5", "item");
		} catch (com.ge.research.semtk.auth.AuthorizationException e) {
			e.printStackTrace();
			fail("Authorization failed");
		} 
		
		try {
			AuthorizationManager.checkJobOwnership("user5", "item");
			fail("Admin didn't reset after first usage");
		} catch (com.ge.research.semtk.auth.AuthorizationException e) {

		} 
		
	}

}
