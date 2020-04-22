/**
 ** Copyright 2020 General Electric Company
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

package com.ge.research.semtk.querygen.timeseries.test;

import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;

import com.ge.research.semtk.querygen.timeseries.TimeSeriesConstraint;
import com.ge.research.semtk.querygen.timeseries.fragmentbuilder.AthenaQueryFragmentBuilder;
import com.ge.research.semtk.querygen.timeseries.fragmentbuilder.HiveQueryFragmentBuilder;
import com.ge.research.semtk.utility.Utility;

public class TimeSeriesConstraintTest {
	
	// these are reused in many tests
	public static String getSampleTimeSeriesConstraintJsonStr(){
		return "{\"@var\":\"_Time_\",\"@operator1\":\">=\",\"@value1\":{\"@value\":\"04/07/2016 2:00:00 AM\",\"@type\":\"datetime\"},\"@operator2\":\"<=\",\"@value2\":{\"@value\":\"04/09/2016 4:00:00 AM\",\"@type\":\"datetime\"}}";
	}	
	public static TimeSeriesConstraint getSampleTimeSeriesConstraint() throws Exception{
		return new TimeSeriesConstraint(Utility.getJsonObjectFromString(getSampleTimeSeriesConstraintJsonStr()));
	}	
	public static String getSampleTimeSeriesConstraintQueryFragment_Hive(){
		return "((unix_timestamp(to_utc_timestamp(`ts_time_utc`,'Ect/GMT+0'), 'yyyy-MM-dd HH:mm:ss') >= unix_timestamp('2016-04-07 02:00:00','yyyy-MM-dd HH:mm:ss')) AND (unix_timestamp(to_utc_timestamp(`ts_time_utc`,'Ect/GMT+0'), 'yyyy-MM-dd HH:mm:ss') <= unix_timestamp('2016-04-09 04:00:00','yyyy-MM-dd HH:mm:ss')))";
	}
	
	
	@Test
	public void test() throws Exception {
		TimeSeriesConstraint tsc = new TimeSeriesConstraint(Utility.getJsonObjectFromString("{\"@var\":\"PRESSURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"50\",\"@type\":\"number\"},\"@operator2\":\"<\",\"@value2\":{\"@value\":\"60\",\"@type\":\"number\"}}"));
		HashMap<String, String> colNames = new HashMap<String,String>();
		colNames.put("p22","PRESSURE");
		assertEquals(tsc.getConstraintQueryFragment(colNames, new HiveQueryFragmentBuilder()), "(`p22` > 50 AND `p22` < 60)");
	}

	@Test
	public void testOperators() throws Exception{
		TimeSeriesConstraint tsc;
		tsc = new TimeSeriesConstraint(Utility.getJsonObjectFromString("{\"@var\":\"PRESSURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"50\",\"@type\":\"number\"},\"@operator2\":\"<\",\"@value2\":{\"@value\":\"60\",\"@type\":\"number\"}}"));
		assertEquals(tsc.getNumOperators(),2);
		assertTrue(tsc.hasLowerBound());
		assertTrue(tsc.hasUpperBound());
		tsc = new TimeSeriesConstraint(Utility.getJsonObjectFromString("{\"@var\":\"PRESSURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"50\",\"@type\":\"number\"}}"));
		assertEquals(tsc.getNumOperators(),1);
		assertTrue(tsc.hasLowerBound());
		assertFalse(tsc.hasUpperBound());
		tsc = new TimeSeriesConstraint(Utility.getJsonObjectFromString("{\"@var\":\"PRESSURE\",\"@operator1\":\"<=\",\"@value1\":{\"@value\":\"60\",\"@type\":\"number\"}}"));
		assertEquals(tsc.getNumOperators(),1);
		assertFalse(tsc.hasLowerBound());
		assertTrue(tsc.hasUpperBound());
		tsc = new TimeSeriesConstraint(Utility.getJsonObjectFromString("{\"@var\":\"PRESSURE\"}"));
		assertEquals(tsc.getNumOperators(),0);
		assertFalse(tsc.hasLowerBound());
		assertFalse(tsc.hasUpperBound());
	}
	

	@Test
	public void testTimeConstraint() throws Exception {
		TimeSeriesConstraint tsc = getSampleTimeSeriesConstraint();
		assertEquals(tsc.getTimeConstraintQueryFragment("ts_time_utc", new HiveQueryFragmentBuilder()), getSampleTimeSeriesConstraintQueryFragment_Hive());
		assertEquals(tsc.getTimeConstraintQueryFragment_typeTimestamp("ts_time_utc", new HiveQueryFragmentBuilder()), "((ts_time_utc >= to_utc_timestamp('2016-04-07 02:00:00.000', 'GMT')) AND (ts_time_utc <= to_utc_timestamp('2016-04-09 04:00:00.000', 'GMT')))");
		assertEquals(tsc.getTimeConstraintQueryFragment("ts_time_utc", new AthenaQueryFragmentBuilder()), "((from_unixtime(\"ts_time_utc\") >= timestamp '2016-04-07 02:00:00') AND (from_unixtime(\"ts_time_utc\") <= timestamp '2016-04-09 04:00:00'))");
	}
	
	@Test 
	public void testTimeConstraint_parseDateTime() throws Exception{
		
		LocalDateTime dateTime;
		
		dateTime = LocalDateTime.parse("2016-04-07 02:00:00", Utility.DATETIME_FORMATTER_yyyyMMddHHmmss);
		assertEquals(TimeSeriesConstraint.parseDateTime("2016-04-07 02:00:00"), dateTime);
		assertEquals(TimeSeriesConstraint.parseDateTime("04/07/2016 2:00:00 AM"), dateTime);
		assertEquals(TimeSeriesConstraint.parseDateTime("04/07/2016 02:00:00 AM"), dateTime);
		
		dateTime = LocalDateTime.parse("2016-04-07 00:00:00", Utility.DATETIME_FORMATTER_yyyyMMddHHmmss);
		assertEquals(TimeSeriesConstraint.parseDateTime("2016-04-07 00:00:00"), dateTime);
		assertEquals(TimeSeriesConstraint.parseDateTime("04/07/2016 12:00:00 AM"), dateTime);

		dateTime = LocalDateTime.parse("2016-04-07 12:00:00", Utility.DATETIME_FORMATTER_yyyyMMddHHmmss);
		assertEquals(TimeSeriesConstraint.parseDateTime("2016-04-07 12:00:00"), dateTime);
		assertEquals(TimeSeriesConstraint.parseDateTime("04/07/2016 12:00:00 PM"), dateTime);
		
		dateTime = LocalDateTime.parse("2016-04-07 22:00:00", Utility.DATETIME_FORMATTER_yyyyMMddHHmmss);
		assertEquals(TimeSeriesConstraint.parseDateTime("2016-04-07 22:00:00"), dateTime);
		assertEquals(TimeSeriesConstraint.parseDateTime("04/07/2016 10:00:00 PM"), dateTime);
		
		boolean exceptionThrown = false;
		try{
			TimeSeriesConstraint.parseDateTime("2016-04-07 12:00:00 PM");  // invalid format
			fail(); // should not get here
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot parse timestamp"));
		}
		assertTrue(exceptionThrown);
	}

	
	@Test
	public void testTimeConstraintDurationSecsAndDates() throws Exception {

		boolean exceptionThrown;
		TimeSeriesConstraint tsc;
		
		// 50 hours
		tsc = new TimeSeriesConstraint(Utility.getJsonObjectFromString("{\"@var\":\"_Time_\",\"@operator1\":\">=\",\"@value1\":{\"@value\":\"04/07/2016 2:00:00 AM\",\"@type\":\"datetime\"},\"@operator2\":\"<=\",\"@value2\":{\"@value\":\"04/09/2016 4:00:00 AM\",\"@type\":\"datetime\"}}"));
		assertEquals(tsc.getTimeConstraintDurationSecs(), 180000); 
		assertEquals(tsc.getTimeConstraintDates(), new ArrayList<String>(Arrays.asList("20160407", "20160408", "20160409")));
		
		// 50 hours, crossing year boundary
		tsc = new TimeSeriesConstraint(Utility.getJsonObjectFromString("{\"@var\":\"_Time_\",\"@operator1\":\">=\",\"@value1\":{\"@value\":\"12/30/2016 2:00:00 AM\",\"@type\":\"datetime\"},\"@operator2\":\"<=\",\"@value2\":{\"@value\":\"01/01/2017 4:00:00 AM\",\"@type\":\"datetime\"}}"));
		assertEquals(tsc.getTimeConstraintDurationSecs(), 180000); 
		assertEquals(tsc.getTimeConstraintDates(), new ArrayList<String>(Arrays.asList("20161230", "20161231", "20170101")));
		
		// 50 hours - with operator2 first
		tsc = new TimeSeriesConstraint(Utility.getJsonObjectFromString("{\"@var\":\"_Time_\",\"@operator2\":\"<=\",\"@value2\":{\"@value\":\"04/09/2016 4:00:00 AM\",\"@type\":\"datetime\"},\"@operator1\":\">=\",\"@value1\":{\"@value\":\"04/07/2016 2:00:00 AM\",\"@type\":\"datetime\"}}"));
		assertEquals(tsc.getTimeConstraintDurationSecs(), 180000); // 50 hours
		assertEquals(tsc.getTimeConstraintDates(), new ArrayList<String>(Arrays.asList("20160407", "20160408", "20160409")));
		
		// 12 hours
		tsc = new TimeSeriesConstraint(Utility.getJsonObjectFromString("{\"@var\":\"_Time_\",\"@operator1\":\">=\",\"@value1\":{\"@value\":\"04/07/2016 2:00:00 AM\",\"@type\":\"datetime\"},\"@operator2\":\"<=\",\"@value2\":{\"@value\":\"04/07/2016 2:00:01 PM\",\"@type\":\"datetime\"}}"));
		assertEquals(tsc.getTimeConstraintDurationSecs(), 43201); 
		assertEquals(tsc.getTimeConstraintDates(), new ArrayList<String>(Arrays.asList("20160407")));
		
		// 1 second
		tsc = new TimeSeriesConstraint(Utility.getJsonObjectFromString("{\"@var\":\"_Time_\",\"@operator1\":\">=\",\"@value1\":{\"@value\":\"04/07/2016 2:00:00 AM\",\"@type\":\"datetime\"},\"@operator2\":\"<=\",\"@value2\":{\"@value\":\"04/07/2016 2:00:01 AM\",\"@type\":\"datetime\"}}"));
		assertEquals(tsc.getTimeConstraintDurationSecs(), 1); 
		assertEquals(tsc.getTimeConstraintDates(), new ArrayList<String>(Arrays.asList("20160407")));
		
		// single equals operator - 0 seconds
		tsc = new TimeSeriesConstraint(Utility.getJsonObjectFromString("{\"@var\":\"_Time_\",\"@operator1\":\"=\",\"@value1\":{\"@value\":\"04/07/2016 2:00:00 AM\",\"@type\":\"datetime\"}}"));
		assertEquals(tsc.getTimeConstraintDurationSecs(), 0);
		assertEquals(tsc.getTimeConstraintDates(), new ArrayList<String>(Arrays.asList("20160407")));
		
		// fail if only 1 min/max operator
		tsc = new TimeSeriesConstraint(Utility.getJsonObjectFromString("{\"@var\":\"_Time_\",\"@operator1\":\">=\",\"@value1\":{\"@value\":\"04/07/2016 2:00:00 AM\",\"@type\":\"datetime\"}}"));
		exceptionThrown = false;
		try{			
			tsc.getTimeConstraintDurationSecs(); // should throw exception
			fail();
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot compute time constraint duration - requires a single equals operator or a pair of min/max operators"));
		}
		assertTrue(exceptionThrown);
		exceptionThrown = false;
		try{			
			tsc.getTimeConstraintDates(); // should throw exception
			fail();
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot compute time constraint duration - requires a single equals operator or a pair of min/max operators"));
		}
		assertTrue(exceptionThrown);
		
		
		// fail if 2 mins, no maxes
		tsc = new TimeSeriesConstraint(Utility.getJsonObjectFromString("{\"@var\":\"_Time_\",\"@operator1\":\">=\",\"@value1\":{\"@value\":\"04/07/2016 2:00:00 AM\",\"@type\":\"datetime\"},\"@operator2\":\">\",\"@value2\":{\"@value\":\"04/09/2016 4:00:00 AM\",\"@type\":\"datetime\"}}"));
		exceptionThrown = false;
		try{
			tsc.getTimeConstraintDurationSecs(); // should throw exception
			fail();
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot compute time constraint duration - requires a single equals operator or a pair of min/max operators"));
		}
		assertTrue(exceptionThrown);
		exceptionThrown = false;
		try{
			tsc.getTimeConstraintDates(); // should throw exception
			fail();
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot compute time constraint duration - requires a single equals operator or a pair of min/max operators"));
		}
		assertTrue(exceptionThrown);
		
		
		// fail if try to do this on a non-time constraint
		tsc = new TimeSeriesConstraint(Utility.getJsonObjectFromString("{\"@var\":\"PRESSURE\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"50\",\"@type\":\"number\"},\"@operator2\":\"<\",\"@value2\":{\"@value\":\"60\",\"@type\":\"number\"}}"));
		exceptionThrown = false;
		try{
			tsc.getTimeConstraintDurationSecs(); // should throw exception
			fail();
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot parse as timestamp"));
		}
		assertTrue(exceptionThrown);
		exceptionThrown = false;
		try{
			tsc.getTimeConstraintDates(); // should throw exception
			fail();
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot parse as timestamp"));
		}
		assertTrue(exceptionThrown);
		
	}
	
}
