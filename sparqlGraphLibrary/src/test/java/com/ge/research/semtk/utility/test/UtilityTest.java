/**
 ** Copyright 2016 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.ge.research.semtk.utility.test;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.junit.Test;

import com.ge.research.semtk.utility.Utility;

public class UtilityTest {

	@Test
	public void testGetURLContentsAsString() throws Exception{		
		URL url = new URL(new URL("file:"), "src/test/resources/test.csv");
		String s = Utility.getURLContentsAsString(url);
		assertEquals(s,"HEADER1,HEADER2,HEADER3\na1,a2,a3\nb1,b2,b3\nc1,c2,c3\nd1,d2,d3\ne1,e2,e3\nf1,f2,f3\ng1,g2,g3\n");		
	}
	
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
	
	
	@Test
	public void testValidatePropertiesAndExitOnFailure() throws Exception{
		HashMap<String,String> properties = new HashMap<String,String>();
		properties.put("color1","red");
		properties.put("color2","yellow");
		Utility.validatePropertiesAndExitOnFailure(properties);
		// just making sure it didn't exit!
		// not committing the test for exit on failure because that will kill the test
		
		properties.put("color3",""); // empty property
		HashSet<String> propertiesSkipValidation = new HashSet<String>();
		propertiesSkipValidation.add("color3");
		Utility.validatePropertiesAndExitOnFailure(properties, propertiesSkipValidation);
		// just making sure it didn't exit!
	}
	
	@Test
	public void testValidateProperty() throws Exception{
		// validate succeeds - these should not throw an exception
		try{
			Utility.validateProperty("red", "color");
			Utility.validateProperty("yellow", "color");
		}catch(Exception e){
			fail();
		}
		// validate fails - these SHOULD throw an exception
		try{			
			Utility.validateProperty(null, "color");
			fail();
		}catch(Exception e){
			assertTrue(e.getMessage().contains("missing, empty, or null"));
		}
		try{			
			Utility.validateProperty("", "color");
			fail();
		}catch(Exception e){
			assertTrue(e.getMessage().contains("missing, empty, or null"));
		}
		try{			
			Utility.validateProperty(" ", "color");
			fail();
		}catch(Exception e){
			assertTrue(e.getMessage().contains("missing, empty, or null"));
		}
		try{			
			Utility.validateProperty("null", "color");
			fail();
		}catch(Exception e){
			assertTrue(e.getMessage().contains("missing, empty, or null"));
		}
	}
	
	@Test
	public void testCompress() throws Exception{
		Random random = new Random();
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < 20000; i++){
			builder.append("here is some text " + i + " " + (new BigInteger(130, random).toString(32)) + " "); // make sure it's not too repetitive
		}
		String s = builder.toString();
		String compressedString = Utility.compress(s);
		String decompressedString = Utility.decompress(compressedString);
		assertEquals(s, decompressedString);
	}
}
