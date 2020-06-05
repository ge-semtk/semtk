package com.ge.research.semtk.load.test;

import static org.junit.Assert.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import com.ge.research.semtk.load.DataValidator;
import com.ge.research.semtk.load.dataset.CSVDataset;

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
		DataValidator validator = new DataValidator(ds, (JSONArray) colJson.get("columns"));
		int errs = validator.validate();
		assertEquals(validator.getErrorString(), 0, errs);
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
		DataValidator validator = new DataValidator(ds, (JSONArray) colJson.get("columns"));
		int errs = validator.validate();
		assertEquals("Found these errors: " + validator.getErrorString(), 3, errs);
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
		DataValidator validator = new DataValidator(ds, (JSONArray) colJson.get("columns"));
		int errs = validator.validate();
		assertEquals("Found these errors: \n" + validator.getErrorString(), 0, errs);
		
		colJson = (JSONObject) (new JSONParser()).parse(
				"{ \"columns\": [{ \"colName\": \"a\", \"notEmpty\": true, \"type\": \"float\", \"gt\": 1,    \"gte\": 3,   \"lt\": 3 },   " +
				"                { \"colName\": \"b\", \"notEmpty\": true, \"type\": \"int\",  \"lte\": 2, \"ne\":  1 }" +
				"]}");
		// error for every cell in a,b
		ds.reset();
		validator = new DataValidator(ds, (JSONArray) colJson.get("columns"));
		errs = validator.validate();
		assertEquals("Found these errors:\n" + validator.getErrorString(), 6, errs);
		
		colJson = (JSONObject) (new JSONParser()).parse(
				"{ \"columns\": [{ \"colName\": \"c\", \"notEmpty\": true, \"type\": \"float\", \"gt\": 1.0,    \"gte\": 3.0,   \"lt\": 3.0 },   " +
				"                { \"colName\": \"d\", \"notEmpty\": true, \"type\": \"float\",  \"lte\": 2.0 }" +
				"]}");
		// error for every cell in a,b
		ds.reset();
		validator = new DataValidator(ds, (JSONArray) colJson.get("columns"));
		errs = validator.validate();
		assertEquals("Found these errors:\n" + validator.getErrorString(), 5, errs);
	}

}
