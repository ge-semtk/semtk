/*
/**
 ** Copyright 2016-2020 General Electric Company
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

package com.ge.research.semtk.utility;

import static org.hamcrest.CoreMatchers.containsString;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;


/*
 * Utility methods
 */
public abstract class Utility {
	
	private final static Charset CHAR_ENCODING = StandardCharsets.ISO_8859_1; // needed for string compression
	
	public static ArrayList<DateTimeFormatter> DATE_FORMATTERS = new ArrayList<DateTimeFormatter>(); 
	public static ArrayList<DateTimeFormatter> DATETIME_FORMATTERS = new ArrayList<DateTimeFormatter>(); 

	public static final DateTimeFormatter DATETIME_FORMATTER_yyyyMMddHHmmss = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");	// e.g. 2014-12-01 00:00:00 
	public static final DateTimeFormatter DATETIME_FORMATTER_yyyyMMddHHmmssSSS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");	// e.g. 2014-12-01 00:00:00.000 
	public static final DateTimeFormatter DATETIME_FORMATTER_MMddyyyyhmmssa = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm:ss a");	// e.g. 02/02/2018 4:00:00 AM
	public static final DateTimeFormatter DATETIME_FORMATTER_ISO8601 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");     // e.g. 2017-09-16T13:37:04Z
	
	public static final DateTimeFormatter DATE_FORMATTER_yyyyMMdd = DateTimeFormatter.ofPattern("yyyyMMdd");	// e.g. 20141031 
	
	private static StrSubstitutor envSubstitutor = new StrSubstitutor(System.getenv());
	public static final Boolean ENV_TEST = ! envSubstitutor.replace("${HOST_IP}").equals("${HOST_IP}");
	public static final String ENV_TEST_EXCEPTION_STRING = "Can't find environment variable $HOST_IP, suggesting a testing setup problem.";
	static{
		// supported input date formats 
		/**
		 *  Please keep the wiki up to date
		 *  https://github.com/ge-semtk/semtk/wiki/Ingestion-type-handling
		 */
		
		// need a builder for some special ones
		DateTimeFormatterBuilder builder;
		DateTimeFormatter dateFormat;
		
		// date formatters:
		
		DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
		DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
		DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		
		// case-insensitive dd-MMM-yyyy (e.g. 12-Jun-2008 or 12-JUN-2008)
		builder = new DateTimeFormatterBuilder();
		builder.parseCaseInsensitive().appendPattern("dd-MMM-yyyy");
		dateFormat = builder.toFormatter();
		DATE_FORMATTERS.add(dateFormat);
		
		// date time formatters:
		
		DATETIME_FORMATTERS.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"));
		DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"));
		DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
		DATETIME_FORMATTERS.add(DATETIME_FORMATTER_yyyyMMddHHmmss);
		DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss"));
	
		// case-insensitive dd-MMM-yyyy HH:mm:ss (e.g. 12-Jun-2008 05:00:00 or 12-JUN-2008 05:00:00)
		builder = new DateTimeFormatterBuilder();
		builder.parseCaseInsensitive().appendPattern("dd-MMM-yyyy HH:mm:ss");
		dateFormat = builder.toFormatter();
		DATETIME_FORMATTERS.add(dateFormat);
	}

	/**
	 * Change the format of a datetime string.
	 * @param dateTimeString		the string, e.g. "02/02/2018 4:00:00 AM"
	 * @param inputFormatter		the input formatter
	 * @param outputFormatter		the output formatter
	 * @return the formatted string
	 */
	public static String formatDateTime(String dateTimeString, DateTimeFormatter inputFormatter, DateTimeFormatter outputFormatter){
		LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, inputFormatter);		
		return dateTime.format(outputFormatter);
	}
	
	/**
	 * Gets a CSV record string from an arraylist of Strings.
	 * Does not include a record separator (will not append \n at the end of the line)
	 */
	public static String getCSVString(ArrayList<String> row) throws IOException{
		StringWriter stringWriter = new StringWriter();
		CSVFormat csvFormat = CSVFormat.EXCEL.withRecordSeparator("");  // don't include a record separator
		CSVPrinter csvPrinter = new CSVPrinter(stringWriter, csvFormat);		
		csvPrinter.printRecord(row);
		csvPrinter.close();
		
		String ret = stringWriter.toString();
		stringWriter.close();
		return ret;
		
	}
	
	/**
	 * Create a JSONArray from a string
	 */
	public static JSONArray getJsonArrayFromString(String s) throws Exception{
		try{
			return (JSONArray) (new JSONParser()).parse(s);
		}catch(Exception e){
			throw new Exception("Error parsing JSONArray from string '" + s + "': " + e.getMessage());
		}
	}
	
	/**
	 * Create a JSONArray from a string array
	 */
	public static JSONArray getJsonArray(String[] arr) throws Exception{
		JSONArray ret = new JSONArray();
		for(String s : arr){
			ret.add(s);
		}
		return ret;
	}
	
	/**
	 * Create a JSONObject from a string
	 */
	public static JSONObject getJsonObjectFromString(String s) throws Exception{
		try{
			return (JSONObject) (new JSONParser()).parse(s);
		}catch(Exception e){
			throw new Exception("Error parsing JSONObject from string '" + s + "': " + e.getMessage());
		}
   	}
	
	/**
	 * Get the contents of a URL as a string
	 * @throws IOException 
	 */
	public static String getURLContentsAsString(URL url) throws IOException{
		StringBuffer ret = new StringBuffer();
		BufferedReader br = null;
		try{
			URLConnection conn = url.openConnection();
			br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				ret.append(line).append("\n");
			}
		}finally{
			if(br != null){
				br.close();
			}
		}
		return ret.toString();
	}
	
	/**
	 * Built a ping result
	 * @return
	 */
	public static JSONObject buildPingResult() {
		SimpleResultSet retval = null;
		
		try{
			retval = new SimpleResultSet(true);
			retval.addResult("available", "yes");
			
		}
		catch(Exception e){
			retval = new SimpleResultSet(false, e.getMessage());
			LocalLogger.printStackTrace(e);
		}
		
		return retval.toJson();
	}
	
	public static Table getURLResultsContentAsTable(URL url) throws Exception{
		Table retval = null;
		
		// first, get the content
		String contents = getURLContentsAsString(url);
		
		// split it into lines.
		String[] lines = contents.split("\n");
		
		if(lines.length > 0){
			// get our columns
			String[] colName = lines[0].split(",");
			String[] colType = new String[colName.length];
			
			// add col type info
			for(int i = 0; i < colName.length; i++){
				colType[i] = "string";
			}
			
			// make arraylist of the remaining values
			ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
			if(lines.length >=2 ){
				for(int k = 1; k < lines.length; k++){
					ArrayList<String> curr = new ArrayList<String>();
					// add each element
					for(String t : lines[k].split(",")){
						curr.add(t);
					}
					// add to the rows collection
					rows.add(curr);
				}
			}
			
			retval = new Table(colName, colType, rows);
			
		}
		else{
			// do nothing. 
		}
		
		return retval;
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
	
	public static String readFile(String path) throws IOException {
		  return FileUtils.readFileToString(new File(path));
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
			throw new Exception("Could not load JSON from file " + jsonFilePath, e);
		}
		
		return jsonObject;
	}

	public static JSONArray getJSONArrayFromFilePath(String jsonFilePath) throws Exception{
		
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
		JSONArray jsonArr = null;
		try{
			jsonArr = Utility.getJSONArrayFromFile(jsonFile);	
		}catch (Exception e){
			throw new Exception("Could not load JSON from file " + jsonFilePath, e);
		}
		
		return jsonArr;
	}

	/**
	 * Get a JSON object from a file
	 * @param f the file
	 * @return the JSON object
	 */
	public static JSONObject getJSONObjectFromFile(File f) throws Exception{
		InputStreamReader reader = new InputStreamReader(new FileInputStream(f.getAbsolutePath()));
		JSONObject ret = (JSONObject) (new JSONParser()).parse(reader);
		reader.close();
		return ret;
	}	

	/**
	 * Get a JSON array from a file
	 * @param f the file
	 * @return the JSON array
	 */
	public static JSONArray getJSONArrayFromFile(File f) throws Exception{
		InputStreamReader reader = new InputStreamReader(new FileInputStream(f.getAbsolutePath()));
		JSONArray ret = (JSONArray) (new JSONParser()).parse(reader);
		reader.close();
		return ret;	
	}
	
	public static String getStringFromFilePath(String path) throws Exception {
		 byte[] encoded = Files.readAllBytes(Paths.get(path));
		 return new String(encoded, StandardCharsets.UTF_8);
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
			ZonedDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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
	
	/**
	 * Create a SPARQL-friendly string (e.g. 2017-06-13) for the current date.
	 */
	public static String getSPARQLCurrentDateString() throws Exception {
		String s = (new SimpleDateFormat("MM/dd/YYYY")).format(new Date());
		return getSPARQLDateString(s);
	}
	
	
	/**
	 * Retrieve a property from a properties file.
	 * @param propertyFile the property file
	 * @param key the name of the property to retrieve
	 */
	// super inefficent to reload the file each time we need a property
	@Deprecated
	public static String getPropertyFromFile(String propertyFile, String key) throws Exception{
	
		Properties properties = new Properties();
		try {
			properties.load(new FileReader(new File(propertyFile)));
		} catch (Exception e) {
		    throw new Exception("Cannot load properties file " + propertyFile, e);
		}

		return getProperty(properties, key);
	}	
	
	public static String getProperty(Properties properties, String key) throws Exception{
		
		// now read the property		
		String ret = properties.getProperty(key);
		if(ret == null){
		    throw new Exception("Cannot read property '" + key);	
		}
		
		// Replace ENV and trim()
		return expandEnv(ret);
	}	
	
	/**
	 * Get subset of properties containing one of keyMatch
	 * @param properties
	 * @param keyMatch
	 * @return
	 */
	public static Properties withKeyContaining(Properties properties, String [] keyMatch) {
		Properties ret = new Properties();
		for (Object k : properties.keySet()) {
			for (String match : keyMatch) {
				if (((String) k).contains(match)) {
					ret.put(k, properties.get(k));
					break;
				}
			}
		}
		return ret;
	}
	
	/**
	 * Expand properties into string, where string contains ${property}
	 * and then do ENVIRONMENT variable expansion
	 * @param properties
	 * @param str
	 * @return
	 * @throws Exception
	 */
	public static String expandProperties(Properties properties, String str) throws Exception {
		
        for (Object k : properties.keySet()) {
        	String key = ((String) k).trim();
        	String val = properties.getProperty((String) key);
        	str = str.replace("${" + key + "}", val);
        }
        str = Utility.expandEnv(str);
        return str;
	}
	
	public static String expandEnv(String str) {
		return envSubstitutor.replace(str).trim();
	}
	
	public static String getXmlBaseFromOwlRdf(InputStream is) throws Exception {
		String ret;
		
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document document = db.parse(is);
			NodeList nList = document.getElementsByTagName("rdf:RDF");
			Node rdfNode = nList.item(0);
			Element rdfElem = (Element) rdfNode;
			ret = rdfElem.getAttribute("xml:base");
			if (ret == null || ret.isEmpty()) {
				
				// hack in an exception for commonly used at GE: SadlBaseModel.owl
				ret = rdfElem.getAttribute("xmlns:sadlbasemodel");
				if (ret == null || ret.isEmpty()) {
					throw new Exception("xml:base not found or empty");
				} else {
					ret = ret.split("#")[0];
				}
			}
		} catch (Exception e) {
			throw new Exception("Error pulling <rdf:RDF xml:base from file ", e);
		}
		return ret;
	}
	
	/**
	 * Print all properties.  Validate all properties and exit if any are invalid.
	 * @param properties keys are property names, values are property values
	 * 
	 * @deprecated
	 * Use newer pattern in package com.ge.research.semtk.properties
	 */
	@Deprecated
	public static void validatePropertiesAndExitOnFailure(TreeMap<String,String> properties){
		validatePropertiesAndExitOnFailure(properties, new HashSet<String>());
	}
	
	/**
	 * Print all properties.  Validate all properties and exit if any are invalid.
	 * @param properties keys are property names, values are property values
	 * @param properties that should not be validated (e.g. they can be blank/missing/null)
	 */
	public static void validatePropertiesAndExitOnFailure(TreeMap<String,String> properties,  HashSet<String> propertiesSkipValidation){
		LocalLogger.logToStdOut("----- PROPERTIES: --------------------");
		for(String propertyName : properties.keySet()){
			String propertyValue = properties.get(propertyName);
			LocalLogger.logToStdOut(propertyName + ": " + propertyValue);	// print to console
			if(!propertiesSkipValidation.contains(propertyName)){		// skip validation for some properties
				try {
					Utility.validateProperty(propertyValue, propertyName);	// validate
				} catch (Exception e) {
					LocalLogger.printStackTrace(e);
					LocalLogger.logToStdOut("============" + e.getMessage() + "...EXITING ============");	
					System.exit(1);					// kill the process
				} 
			}
		}
		LocalLogger.logToStdOut("--------------------------------------");
	}

	
	/**
	 * Validate a property.
	 * Confirm that it is not null, "null", or empty...and then return it. 
	 * 
	 * @param propertyValue the property value
	 * @param propertyName the property name
	 * @throw Exception if validation fails
	 */
	public static void validateProperty(String propertyValue, String propertyName) throws Exception{
		if(propertyValue == null || propertyValue.trim().equals("null") || propertyValue.trim().isEmpty()){
			throw new Exception("Property " + propertyName + " is missing, empty, or null");	// throw an Exception
		}
	}
	
	
	/**
	 * Compress a string.
	 */
	public static String compress(String s) throws Exception {  
		byte[] inputBytes = s.getBytes(CHAR_ENCODING);
		Deflater deflater = new Deflater();  
		deflater.setInput(inputBytes);  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputBytes.length);   
		deflater.finish();  
		byte[] buffer = new byte[1024];   
		while (!deflater.finished()) {  
			int count = deflater.deflate(buffer); 
			outputStream.write(buffer, 0, count);   
		}  
		outputStream.close();  
		byte[] compressedBytes = outputStream.toByteArray();  
		return new String(compressedBytes, CHAR_ENCODING);
	}  

	/**
	 * Decompress a string.
	 */
	public static String decompress(String s) throws Exception {   
		byte[] inputBytes = s.getBytes(CHAR_ENCODING);
		Inflater inflater = new Inflater();   
		inflater.setInput(inputBytes);  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputBytes.length);  
		byte[] buffer = new byte[1024];  
		while (!inflater.finished()) {  
			int count = inflater.inflate(buffer);  
			outputStream.write(buffer, 0, count);  
		}  
		outputStream.close();  
		byte[] decompressedBytes = outputStream.toByteArray();  
		return new String(decompressedBytes, CHAR_ENCODING);
	}  

	/**
	 *    Prefix a "http://normal/looking#uri" into "1:uri", using and/or updating prefixToIntHash
	 */
	public static String prefixURI(String uri, HashMap<String, String> prefixToIntHash) {
        String [] tok = uri.split("#");
        // if there is a #
        if (tok.length == 2) {
            // add to prefixToIntHash if missing
        	if (! prefixToIntHash.containsKey(tok[0])) {
                prefixToIntHash.put(tok[0], String.valueOf(prefixToIntHash.size()));
            }
            
            return prefixToIntHash.get(tok[0]) + ":" + tok[1];
        } else {
            return uri;
        }
    }
    
	/**
	 * Unprefix a "1:uri" to "http://actual/prefix#uri" using intToPrefixHash
	 */
	public static String unPrefixURI(String uri, HashMap<String, String> intToPrefixHash) {
        String [] tok = uri.split(":");
        if (tok.length == 2 && intToPrefixHash.containsKey(tok[0])) {
            return intToPrefixHash.get(tok[0]) + "#" + tok[1];
        } else {
            return uri;
        }
    }
	
	public static String [] unPrefixJsonTableColumn(JSONArray jArr, int col, HashMap<String, String> intToPrefixHash) {
		String [] ret = new String[jArr.size()];
		
		for (int i=0; i < jArr.size(); i++) {
			ret[i] = Utility.unPrefixURI( (String) ( ((JSONArray)jArr.get(i)) .get(col) ),
					                      intToPrefixHash);
		}
		return ret;
	}
	
	public static String [] getJsonTableColumn(JSONArray jArr, int col) {
		String [] ret = new String[jArr.size()];
		
		for (int i=0; i < jArr.size(); i++) {
			ret[i] =  (String) ( ((JSONArray)jArr.get(i)) .get(col) ) ;
		}
		return ret;
	}

	/**
	 * Make a string json compatible, while minding performance.
	 * @param s the original string
	 * @return a json-safe string
	 * 
	 * http://json.org/   (see the "string" section)
	 *
	 * https://stackoverflow.com/questions/4901133/json-and-escaping-characters
	 * 2.5. Strings
		The representation of strings is similar to conventions used in the C family of programming languages. 	
		A string begins and ends with quotation marks. All Unicode characters may be placed within the quotation 
		marks except for the characters that must be escaped: quotation mark, reverse solidus, 
		and the control characters (U+0000 through U+001F).
		Any character may be escaped.
	 */
	public static String escapeJsonString(String s) {
		for(int i=0; i < s.length(); i++){
			char ch=s.charAt(i);
			
			// for performance reasons, only call JSONObject.escape() if the string *might* not be json compatible
			// everything from space to } is json safe, except \ and "
			if (ch < ' ' || ch > '}' || ch == '\\' || ch == '"') {
				// string may not be json safe, escape it
				return JSONObject.escape(s);  // from Javadoc: Escape quotes, \, /, \r, \n, \b, \f, \t and other control characters (U+0000 through U+001F).
			}
		}
		// string confirmed json safe, return it as-is
		return s;
	}
	
	/**
	 * Get a file resource as a string.  
	 * 
	 * @param obj the calling object
	 * @param fileName the name of the file resource.  May need to prepend this with /
	 * @return the file contents
	 */
	public static String getResourceAsString(Object obj, String fileName) throws Exception {
		return getResourceAsString(obj.getClass(), fileName);
	}
	public static String getResourceAsString(Class c, String fileName) throws Exception {
		String ret = null;
		InputStream in = c.getResourceAsStream(fixResourceName(fileName));
		if (in == null) {
			throw new Exception("Could find resource file: " + fileName);
		}
		
		ret = IOUtils.toString(in, StandardCharsets.UTF_8);
		if (ret == null) {
			throw new Exception("Resource file is empty: " + fileName);
		}
		
		return ret;
	}
	
	public static File getResourceAsFile(Object obj, String fileName) throws Exception {
		return getResourceAsFile(obj.getClass(), fileName);
	}
	public static File getResourceAsFile(Class c, String fileName) throws Exception {
		File ret = null;
		
		URL url = c.getResource(fixResourceName(fileName));
		if (url == null) {
			throw new Exception("Could find resource file: " + fileName);
		}
		
		ret = new File(url.getPath());
		if (ret == null) {
			throw new Exception("Resource file is empty: " + fileName);
		}
		
		return ret;
	}
	
	public static byte [] getResourceAsBytes(Object obj, String fileName) throws Exception {
		return getResourceAsBytes(obj.getClass(), fileName);
	}
	public static byte [] getResourceAsBytes(Class c, String fileName) throws Exception  {
		byte [] ret = null;

		InputStream in = c.getResourceAsStream(fixResourceName(fileName));
		if (in == null) {
			throw new Exception("Could find resource file: " + fileName);
		}
		
		ret = IOUtils.toByteArray(in);
		if (ret == null) {
			throw new Exception("Resource file is empty: " + fileName);
		}
		return ret;
	}
	
	
	/**
	 * Get a file resource as JSON
	 * @param obj
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public static JSONObject getResourceAsJson(Object obj, String fileName) throws Exception {
		return Utility.getJsonObjectFromString(Utility.getResourceAsString(obj, fileName));
	}
	
	public static JSONObject getResourceAsJson(Class c, String fileName) throws Exception {
		return Utility.getJsonObjectFromString(Utility.getResourceAsString(c, fileName));
	}
	
	private static String fixResourceName(String resourceName) {
		if (resourceName.startsWith("/")) {
			return resourceName;
		} else {
			return "/" + resourceName;
		}
	}
	/**
	 * Replace UUIDs in a string with "<UUID>".
	 * Note: does not affect hashes.
	 * @param orig
	 * @return
	 */
	public static String replaceUUIDs(String orig) {
		return orig.replaceAll("\\w\\w\\w\\w\\w\\w\\w\\w-\\w\\w\\w\\w-\\w\\w\\w\\w-\\w\\w\\w\\w-\\w\\w\\w\\w\\w\\w\\w\\w\\w\\w\\w\\w", "<UUID>");
	}
	
	/**
	 * change \r\n to \n 
	 * @param orig
	 * @return
	 */
	public static String standardizeCRLF(String orig) {
		return orig.replaceAll("\r\n", "\n");
	}
	
	public static String removeQuotedSubstrings(String orig, String replacement) {
		//https://www.metaltoad.com/blog/regex-quoted-string-escapable-quotes
		return orig.replaceAll("((?<![\\\\])['\"])((?:.(?!(?<![\\\\])\\1))*.?)\\1", replacement);
	}
	
	public static String escapeQuotes(String aString){
		
		String retval = aString.replaceAll("\"", "\"\"");  // replace the quotes.
		retval = retval.replace("\\\"\"", "\\\\\"\"");  // trying to avoid orphaned quotes.this leads to issues in the csv interpretter.
			
		return retval;
	}
	
	public static String generateRandomURI() {
		return "r" + UUID.randomUUID().toString();
	}
	
	public static String hashMD5(String s) throws Exception {
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
	    messageDigest.update(s.getBytes());
	    byte[] digiest = messageDigest.digest();
	    return DatatypeConverter.printHexBinary(digiest);
	}
	
	public static String htmlToPlain(String html) {
		return html.replaceAll("(<[Bb][Rr]>|<[Pp]>)", "\n")
				.replaceAll("\\s*<[^>]+>\\s*", " ")
				.replaceAll(" +", " ")
				.replaceAll("\n+", "\n");
	}
}
