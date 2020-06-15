package com.ge.research.semtk.load.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import com.ge.research.semtk.load.DataValidator;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.resultSet.Table;

public class DataValidatorTest {

	@Test
	public void test() throws Exception {
		// simple test that
		//     column a is not empty, contains only digits, but not 7
		String data = "a,b,c\n" +
					  "1,1,1\n" +
					  "2,2,2\n";
		CSVDataset ds = new CSVDataset(data, true);
		JSONObject colJson = (JSONObject) (new JSONParser()).parse(
				"{ \"columns\": [{ \"colName\": \"a\", \"notEmpty\": true, \"regexMatches\": \"[0-9]+\" \"regexNoMatch\": \".*7.*\" }]}"
				);
		DataValidator validator = new DataValidator((JSONArray) colJson.get("columns"));
		int errCount = validator.validate(ds);
		assertEquals(validator.getErrorTable().toCSVString(), 0, errCount);
	}
	
	@Test
	public void testEmptyAndRegex() throws Exception {
		// Tests: two column validated
		//        
		String data = "a,b,c\n" +
					  "1,1,7\n" +
					  "cat,2,2\n" +
					  ",3,3\n";
		CSVDataset ds = new CSVDataset(data, true);
		JSONObject colJson = (JSONObject) (new JSONParser()).parse(
				"{ \"columns\": [{ \"colName\": \"a\", \"notEmpty\": true, \"regexMatches\": \"[0-9]+\" \"regexNoMatch\": \".*7.*\" },"+
				"                { \"colName\": \"c\", \"notEmpty\": true, \"regexMatches\": \"[0-9]+\" \"regexNoMatch\": \".*2.*\" } " +
				"]}");
		
		// c should be fine
		// Three errors:
		//    a contains an empty
		//    a contains 'cat'
		//    c contains a 2
		//
		// Two errors are in same row.
		// The blank also fails the regexMatches, but we shouldn't get two errors for same cell
		//
		DataValidator validator = new DataValidator((JSONArray) colJson.get("columns"));
		int errCount = validator.validate(ds);
		assertEquals("Found these errors: " + validator.getErrorTable().toCSVString(), 3, errCount);
	}
	
	@Test
	public void testArithmetic() throws Exception {
		// Tests: two column validated
		//        
		String data = "a,b,c,d     \n" +
					  "1,1,1.1, 1.1\n" +
					  "2, ,2.1,    \n" +
					  "3,3,3.1,3.1 \n";
		CSVDataset ds = new CSVDataset(data, true);
		JSONObject colJson = (JSONObject) (new JSONParser()).parse(
				"{ \"columns\": [{ \"colName\": \"a\", \"notEmpty\": true, \"type\": \"int\",   \"gt\": 0, \"gte\": 1,   \"lt\": 4, \"lte\": 3,   \"ne\": 5},   " +
				"                { \"colName\": \"c\", \"notEmpty\": true, \"type\": \"float\", \"gt\": 0, \"gte\": 1.0, \"lt\": 4, \"lte\": 3.1, }, " +
				"                { \"colName\": \"b\",                     \"type\": \"int\",   \"gt\": 0, \"gte\": 1  , \"lt\": 4, \"lte\": 3  , \"ne\": 5  }, " +
				"                { \"colName\": \"d\", \"notEmpty\": false \"type\": \"float\", \"gt\": 0, \"gte\": 1.0, \"lt\": 4, \"lte\": 3.1}, " +
				"]}");
		
		// no errors
		DataValidator validator = new DataValidator((JSONArray) colJson.get("columns"));
		int errCount = validator.validate(ds);
		assertEquals("Found these errors: \n" + validator.getErrorTable().toCSVString(), 0, errCount);
		
		colJson = (JSONObject) (new JSONParser()).parse(
				"{ \"columns\": [{ \"colName\": \"a\", \"notEmpty\": true, \"type\": \"float\", \"gt\": 1,    \"gte\": 3,   \"lt\": 3 },   " +
				"                { \"colName\": \"b\", \"notEmpty\": true, \"type\": \"int\",  \"lte\": 2, \"ne\":  1 }" +
				"]}");
		// error for every cell in a,b
		ds.reset();
		validator = new DataValidator((JSONArray) colJson.get("columns"));
		errCount = validator.validate(ds);
		assertEquals("Found these errors:\n" + validator.getErrorTable().toCSVString(), 6, errCount);
		
		colJson = (JSONObject) (new JSONParser()).parse(
				"{ \"columns\": [{ \"colName\": \"c\", \"notEmpty\": true, \"type\": \"float\", \"gt\": 1.0,    \"gte\": 3.0,   \"lt\": 3.0 },   " +
				"                { \"colName\": \"d\", \"notEmpty\": true, \"type\": \"float\",  \"lte\": 2.0 }" +
				"]}");
		// error for every cell in a,b
		ds.reset();
		validator = new DataValidator((JSONArray) colJson.get("columns"));
		errCount = validator.validate(ds);
		assertEquals("Found these errors:\n" + validator.getErrorTable().toCSVString(), 5, errCount);
	}
	
	@Test
	public void testDateTime() throws Exception {
		
		// note different date format (sparql date, not the ISO date)
		String data = "d,t,dt     \n" +
					  "01/01/2001,01:01:01 ,2001-01-01T01:01:01\n" + 
					  "2002-02-02,02:02:02, 2002-02-02T02:02:02\n" +
					  "2003-03-03,03:03:03,2003-03-03T03:03:03\n";
		CSVDataset ds = new CSVDataset(data, true);
		JSONObject colJson = (JSONObject) (new JSONParser()).parse(
				"{ \"columns\": [{ \"colName\": \"d\", \"notEmpty\": true, \"type\": \"date\", \"gt\":\"2001-01-01\", \"lt\":\"2003-03-03\", \"ne\": \"2002-02-02\"  },   " +
				"                { \"colName\": \"t\", \"notEmpty\": true, \"type\": \"time\", \"gt\":\"01:01:01\", \"lt\":\"03:03:03\", \"ne\": \"02:02:02\" }, " +
				"                { \"colName\": \"dt\",\"notEmpty\": true, \"type\": \"datetime\", \"gt\":\"2001-01-01T01:01:01\", \"lt\":\"2003-03-03T03:03:03\", \"ne\": \"2002-02-02T02:02:02\" }, " +
				"]}");
		
		// no errors
		DataValidator validator = new DataValidator((JSONArray) colJson.get("columns"));
		int errCount = validator.validate(ds);
		assertEquals("Found these errors: \n" + validator.getErrorTable().toCSVString(), 9, errCount);
		System.out.println(validator.getErrorTable().toCSVString());
		ds = new CSVDataset(data, true);
		colJson = (JSONObject) (new JSONParser()).parse(
				"{ \"columns\": [{ \"colName\": \"d\", \"notEmpty\": true, \"type\": \"date\", \"gte\":\"2001-01-01\", \"lte\":\"2003-03-03\", \"ne\": \"2002-02-03\"  },   " +
				"                { \"colName\": \"t\", \"notEmpty\": true, \"type\": \"time\", \"gte\":\"01:01:01\", \"lte\":\"03:03:03\", \"ne\": \"02:02:03\" }, " +
				"                { \"colName\": \"dt\",\"notEmpty\": true, \"type\": \"datetime\", \"gte\":\"2001-01-01T01:01:01\", \"lte\":\"2003-03-03T03:03:03\", \"ne\": \"2002-02-02T02:02:03\" }, " +
				"]}");
		
		// all errors
		validator = new DataValidator((JSONArray) colJson.get("columns"));
		errCount = validator.validate(ds);
		assertEquals("Found these errors: \n" + validator.getErrorTable().toCSVString(), 0, errCount);
		
	}

}
