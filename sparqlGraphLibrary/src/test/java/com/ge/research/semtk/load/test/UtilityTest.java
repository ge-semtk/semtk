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

import static org.junit.Assert.*;

import org.junit.Test;

import com.ge.research.semtk.utility.Utility;

public class UtilityTest {

	
	@Test
	public void getSPARQLDateString() throws Exception{

		assertEquals("2011-12-03T10:15:30+01:00",	Utility.getSPARQLDateTimeString("2011-12-03T10:15:30+01:00"));
		assertEquals("1979-03-05T12:00:00-04:00",	Utility.getSPARQLDateTimeString("1979-03-05T12:00:00-04:00"));
		
		assertEquals("2017-03-22T20:00:00-04:00", Utility.getSPARQLDateTimeString("Wed Mar 22 20:00:00 EST 2017"));

		assertEquals("2011-12-03T20:15:30",	Utility.getSPARQLDateTimeString("12/03/2011 20:15:30"));
		assertEquals("2011-12-03T10:15:30",	Utility.getSPARQLDateTimeString("2011-12-03T10:15:30"));
		
		assertEquals("2008-06-12T05:00:00",	Utility.getSPARQLDateTimeString("12-Jun-2008 05:00:00"));
		assertEquals("2008-05-12T05:00:00",	Utility.getSPARQLDateTimeString("12-MAY-2008 05:00:00"));
		assertEquals("2011-12-03T00:00:00",	Utility.getSPARQLDateTimeString("12/03/2011"));
		assertEquals("2011-12-03T00:00:00",	Utility.getSPARQLDateTimeString("12-03-2011"));
		assertEquals("2011-12-03T00:00:00",	Utility.getSPARQLDateTimeString("2011-12-03"));
		assertEquals("2008-06-12T00:00:00",	Utility.getSPARQLDateTimeString("12-Jun-2008"));
		assertEquals("2008-05-12T00:00:00",	Utility.getSPARQLDateTimeString("12-MAY-2008"));
		
		assertEquals("2011-12-03",	Utility.getSPARQLDateString("12/03/2011"));
		assertEquals("2011-12-03",	Utility.getSPARQLDateString("12-03-2011"));
		assertEquals("2011-12-03",	Utility.getSPARQLDateString("2011-12-03"));
		assertEquals("2008-06-12",	Utility.getSPARQLDateString("12-Jun-2008"));
		assertEquals("2008-05-12",	Utility.getSPARQLDateString("12-MAY-2008"));
		
	}
	
	
	@Test
	public void getSPARQLCurrentDateString() throws Exception{
		// expect format 2017-06-13. Changes daily ...so just do some basic checks	
		String dateString = Utility.getSPARQLCurrentDateString();
		assertTrue(dateString.length() == 10);
		assertTrue(dateString.startsWith("20"));  // the year
		assertTrue(dateString.indexOf("-") == 4); // the dash after the year
	}

}