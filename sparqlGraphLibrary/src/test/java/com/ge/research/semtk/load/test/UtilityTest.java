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


package com.ge.research.semtk.load.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ge.research.semtk.load.utility.Utility;

public class UtilityTest {

	
	@Test
	public void getSPARQLDateString() throws Exception{

		assertTrue(Utility.getSPARQLDateString("12/03/2011").equals("2011-12-03"));
		assertTrue(Utility.getSPARQLDateString("12-03-2011").equals("2011-12-03"));
		assertTrue(Utility.getSPARQLDateString("2011-12-03").equals("2011-12-03"));
		
		assertTrue(Utility.getSPARQLDateTimeString("12/03/2011 20:15:30").equals("2011-12-03T20:15:30"));
		assertTrue(Utility.getSPARQLDateTimeString("2011-12-03T10:15:30").equals("2011-12-03T10:15:30"));
		assertTrue(Utility.getSPARQLDateTimeString("2011-12-03T10:15:30+01:00").equals("2011-12-03T10:15:30+01:00"));
		assertTrue(Utility.getSPARQLDateTimeString("1979-03-05T12:00:00-04:00").equals("1979-03-05T12:00:00-04:00"));
		assertTrue(Utility.getSPARQLDateTimeString("12/03/2011").equals("2011-12-03T00:00:00"));
		assertTrue(Utility.getSPARQLDateTimeString("12-03-2011").equals("2011-12-03T00:00:00"));
		assertTrue(Utility.getSPARQLDateTimeString("2011-12-03").equals("2011-12-03T00:00:00"));
		
	}

}