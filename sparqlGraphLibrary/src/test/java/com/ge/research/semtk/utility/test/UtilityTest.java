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

import org.junit.Test;

import com.ge.research.semtk.load.utility.Utility;

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
