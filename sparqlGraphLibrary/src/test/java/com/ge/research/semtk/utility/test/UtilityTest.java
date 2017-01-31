package com.ge.research.semtk.utility.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ge.research.semtk.utility.Utility;

public class UtilityTest {

	@Test
	public void testLoadProperties() throws Exception {
		String p = Utility.getPropertyFromFile("src/test/resources/utilitytest.properties","maple.color");
		assertEquals(p,"yellow");
	}
	
	@Test
	public void testNonexistentPropertyFile() throws Exception {
		boolean exceptionThrown = false;
		try{
			Utility.getPropertyFromFile("nonexistent.properties","maple.color");
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot load properties file"));
		}
		assertTrue(exceptionThrown);
	}
	
	@Test
	public void testNonexistentProperty() throws Exception {
		boolean exceptionThrown = false;
		try{
			Utility.getPropertyFromFile("src/test/resources/utilitytest.properties","maple.nonexistentProperty");
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot read property"));
		}
		assertTrue(exceptionThrown);
	}
	
}
