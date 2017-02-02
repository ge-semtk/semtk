/*
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

package com.ge.research.semtk.load.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


/*
 * Utility methods
 */
public abstract class Utility {
	
	public static ArrayList<DateTimeFormatter> DATE_FORMATTERS = new ArrayList<DateTimeFormatter>(); 
	public static ArrayList<DateTimeFormatter> DATETIME_FORMATTERS = new ArrayList<DateTimeFormatter>(); 

	static{
		// supported input date formats 
		/**
		 *  Please keep the wiki up to date
		 *  https://github.com/ge-semtk/semtk/wiki/Ingestion-type-handling
		 */
		
		DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
		DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
		DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		
		DATETIME_FORMATTERS.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"));
		DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"));
		DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
		DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss"));
	}
	
	/**
	 * Determine if two String arrays have the same elements, ignoring order
	 */
	public static boolean arraysSameMinusOrder(String[] arr1, String[] arr2) {
		String[] arr1Clone = arr1.clone();	// clone so as to not reorder the actual array passed in here
		String[] arr2Clone = arr2.clone();
	    Arrays.sort(arr1Clone);
	    Arrays.sort(arr2Clone);
	    return Arrays.equals(arr1Clone, arr2Clone);
	}
	
	/**
	 * Create a JSON object from a path to a JSON file
	 * @param jsonFilePath the file path
	 * @return the JSON object
	 * @throws Exception 
	 */
	public static JSONObject getJSONObjectFromFilePath(String jsonFilePath) throws Exception{
		
		// validate that file has a .json extension
		if(!jsonFilePath.endsWith(".json")){
			throw new Exception("Error: File " + jsonFilePath + " is not a JSON file");
		}
		
		// load the file
		File jsonFile = null;	
		try {
			jsonFile = new File(jsonFilePath);
		} catch (Exception e) {
			throw new Exception("Could not find JSON file " + jsonFilePath);
		}
			
		// load JSON file to JSON object
		JSONObject jsonObject = null;
		try{
			jsonObject = Utility.getJSONObjectFromFile(jsonFile);	
		}catch (Exception e){
			throw new Exception("Could not load JSON from file " + jsonFilePath + "\n" + e.getMessage());
		}
		
		return jsonObject;
	}
	
	
	/**
	 * Get a JSON object from a file
	 * @param f the file
	 * @return the JSON object
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public static JSONObject getJSONObjectFromFile(File f) throws Exception{
		BufferedReader bufferedReader = new BufferedReader(new FileReader(f.getAbsolutePath())); 
		StringBuilder jsonStr = new StringBuilder();
		String line;
		while((line = bufferedReader.readLine()) != null){
			jsonStr.append(" " + line);
		}
		bufferedReader.close();
		JSONParser parser = new JSONParser();
		return (JSONObject) parser.parse(jsonStr.toString());	
	}
	
	/**
	 * Get colName entries from the import spec portion of the JSON template.
	 * Converts all column names to lower case.
	 * @param json the JSON object
	 * @return an array of column names, with no duplicates
	 */
	//public static String[] getColNamesFromJSONTemplate(JSONObject json){
	// MOVED TO ImportSpecHandler	
	
	
	/**
	 * Create a SPARQL-friendly string (e.g. 2011-12-03T10:15:30) from a date time string. 
	 */
	public static String getSPARQLDateTimeString(String s) throws Exception{

		// ISO_OFFSET_DATE_TIME is the only valid format with timezone
		// Try it first
		// If it succeeds then return as-is, since it is also valid SPARQL
		try{
			ZonedDateTime zonedObj = ZonedDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			return s;
		} catch (Exception e) {
        	// move on
        }
		
		// try all formatters until find one that works
		for (DateTimeFormatter formatter : DATETIME_FORMATTERS){  
	        try{        	
	        	LocalDateTime dateObj = LocalDateTime.parse(s, formatter);
	        	return dateObj.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);				 
	        }catch (Exception e) {
	        	// try the next one
	        }
		}			
		
		// none of the datetime formatters worked.  Try formatting as date and appending T00:00:00
		try{
			s = getSPARQLDateString(s); 
			s += "T00:00:00";
			return s; 
		}catch(Exception e){
			// move on 
		}
		
		throw new Exception("Cannot parse " + s + " using available formatters");
	}
	
	/**
	 * Create a SPARQL-friendly string (e.g. 2011-12-03) from a date string
	 */
	public static String getSPARQLDateString(String s) throws Exception{

		// try all formatters until find one that works
		for (DateTimeFormatter formatter : DATE_FORMATTERS){  
	        try{        	
	        	LocalDate dateObj = LocalDate.parse(s, formatter);
				return dateObj.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")); 	
	        }catch (Exception e) {
	        	// try the next one
	        }
		}				
		throw new Exception("Cannot parse " + s + " using available formatters");
	}
	
}
