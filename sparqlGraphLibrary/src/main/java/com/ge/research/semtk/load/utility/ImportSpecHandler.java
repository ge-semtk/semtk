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

import java.sql.Time;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.load.utility.UriResolver;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.load.transform.Transform;
import com.ge.research.semtk.load.transform.TransformInfo;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;

public class ImportSpecHandler {

	JSONObject importspec = null; 	// ultimately, this should be replaced by a more complext object 
									// that does not endlessly reprocessthe same json.
	HashMap<String, Integer> headerPositioningInfo = new HashMap<String, Integer>();
	HashMap<String, Transform> transformsAvailable = new HashMap<String, Transform>();
	HashMap<String, String> textsAvailable = new HashMap<String, String>();
	HashMap<String, String> colsAvailable = new HashMap<String, String>();
	HashMap<String, Integer> colsUsed = new HashMap<String, Integer>();    // count of cols used.  Only includes counts > 0
	
	UriResolver uriResolver;
	
	
	public ImportSpecHandler(JSONObject spec, OntologyInfo oInf) throws Exception {
		this.importspec = spec;
		this.setupColumns();
		this.setupColsUsed();
		this.setupTransforms();
		this.setupTexts();
		String userUriPrefixValue = (String) this.importspec.get("baseURI");
		
		// check the value of the UserURI Prefix
		System.err.println("User uri prefix set to: " +  userUriPrefixValue);
		
		this.uriResolver = new UriResolver(userUriPrefixValue, oInf);
	}
	
	public ImportSpecHandler(JSONObject spec, ArrayList<String> headers, OntologyInfo oInf) throws Exception{
		this(spec, oInf);
		this.setHeaders(headers);
	}

	public void setHeaders(ArrayList<String> headers){
		int counter = 0;
		for(String h : headers){
			this.headerPositioningInfo.put(h, counter);
			counter += 1;
		}
	}
	
	public String getUriPrefix() {
		return uriResolver.getUriPrefix();
	}
	
	/**
	 * Populate the transforms with the correct instances based on the inportspec.
	 * @throws Exception 
	 */
	private void setupTransforms() throws Exception{
		// get the transforms, if any
		JSONArray transformInfo = (JSONArray) this.importspec.get("transforms");
		if(transformInfo == null){ 
			// in the event there was no transform block found in the JSON, just return.
			// thereafter, there are no transforms looked up or found.
			return;}
		
		for (int j = 0; j < transformInfo.size(); ++j) {
			JSONObject xform = (JSONObject) transformInfo.get(j);
			String instanceID = (String) xform.get("transId"); // get the instanceID for the transform
			String transType = (String) xform.get("transType"); // get the xform type 
			
			// go through all the entries besides "name", "transType", "transId" and 
			// add them to the outgoing HashMap to be sent to the transform creation.
			int totalArgs = TransformInfo.getArgCount(transType);
			
			// get the args.
			HashMap<String, String> args = new HashMap<String, String>();
			for(int argCounter = 1; argCounter <= totalArgs; argCounter += 1){
				// get the current argument
				args.put("arg" + argCounter, (String) xform.get("arg" + argCounter));
			}
			
			// get the transform instance.
			Transform currXform = TransformInfo.buildTransform(transType, instanceID, args);
			
			// add it to the hashMap.
			this.transformsAvailable.put(instanceID, currXform);
		}
	}
	
	/**
	 * Populate the texts with the correct instances based on the importspec.
	 * @throws Exception 
	 */
	private void setupTexts() throws Exception{
		// get the texts, if any
		JSONArray textsInfo = (JSONArray) this.importspec.get("texts");
		if(textsInfo == null){ 
			return;
		}
		
		for (int j = 0; j < textsInfo.size(); ++j) {
			JSONObject textJson = (JSONObject) textsInfo.get(j);
			String instanceID = (String) textJson.get("textId"); 
			String textVal = (String) textJson.get("text");  
			this.textsAvailable.put(instanceID, textVal);
		}
	}

	/**
	 * Populate the texts with the correct instances based on the importspec.
	 * @throws Exception 
	 */
	private void setupColumns() throws Exception{
		// get the texts, if any
		JSONArray colsInfo = (JSONArray) this.importspec.get("columns");
		if(colsInfo == null){ 
			return;
		}
		
		for (int j = 0; j < colsInfo.size(); ++j) {
			JSONObject colsJson = (JSONObject) colsInfo.get(j);
			String colId = (String) colsJson.get("colId");      
			String colName = ((String) colsJson.get("colName")).toLowerCase();  
			this.colsAvailable.put(colId, colName);
		}
	}
	/**
	 * Populate nodegroup with a single record (row) of data
	 */
	public NodeGroup importRecord(NodeGroup ng, ArrayList<String> record) throws Exception{
		// this is a really naive implementation. it could probably be sped up drastically by caching
		// the digested template instead of re-processing it for eac incoming record.
		// the lack of conditional logic in this method would make that do-able. 
		
		NodeGroup retval = ng;
		if(ng == null){ throw new Exception("Null nodegroup passed to ImportSpecHandler.getValues"); }
		if(record  == null){ throw new Exception("incoming record cannot be null for ImportSpecHandler.getValues"); }
		if(this.headerPositioningInfo.isEmpty()){ throw new Exception("the header positions were never set for the importspechandler"); }
		
		JSONArray nodes = (JSONArray) this.importspec.get("nodes");  // the "nodes" part of the JSON
		int nodesArraySize = nodes.size();
		
		for (int i = 0; i < nodesArraySize; i++){  
			// loop through all of the values and get the parts we need to fill in the nodegroup entries. 
			JSONObject currnode = (JSONObject) nodes.get(i);
			JSONArray uriMap = (JSONArray) currnode.get("mapping");
			JSONArray props  = (JSONArray) currnode.get("props");
						
			// get the related node from the NodeGroup
			String sparqlID = currnode.get("sparqlID").toString();
			//System.out.println("sparqlID: " + sparqlID);
			Node curr = ng.getNodeBySparqlID(sparqlID);
			
			// generate the uri value. all of this can be simplified later into a structure that actually remembers the format 
			// because position info will not drift as each line is processed. this was done for expedience of development. 

			String uri = this.buildMappingString(uriMap, record);
			
			// check for a null column mapping having been found. if so, change the URI to a guid and make this a blank node. 
			// encode uri and set it. 
			if(StringUtils.isBlank(uri)){
				curr.setInstanceValue(null);
			}
			else{
				uri = this.uriResolver.getInstanceUriWithPrefix(curr.getFullUriName(), uri);
				if (! SparqlToXUtils.isLegalURI(uri)) { throw new Exception("Attempting to insert ill-formed URI: " + uri); }
				curr.setInstanceValue(uri);
			}
			
			
			// set any applicable property values. again, this could be greatly simplified in terms of number of look ups and, 
			// potentially, running time. it should be encapsulated into an object that understands the nodes itself and stores
			// this sort of info. this was done the hard way in the interest of simplifying implementation and debugging. 
			
			ArrayList<PropertyItem> inScopeProperties = curr.getPropertyItems();
			
			Iterator<JSONObject> pMap = props.iterator();
			while(pMap.hasNext()) {
				String[] namesOfTransformsToApplyProp = null;
				
				// go through all the parts of the property as well. 
				JSONObject currProp = pMap.next();
				String uriRelation = currProp.get("URIRelation").toString();
				
				JSONArray mapping = (JSONArray) currProp.get("mapping");
				
				String instanceValue = this.buildMappingString(mapping, record);

				// find and set the actual property value. 
				for(PropertyItem pi : inScopeProperties){
					if(pi.getUriRelationship().equals(uriRelation)){  // e.g. http://research.ge.com/sofc/testconfig#cellId
						
						if(this.notEmpty(instanceValue)){
							if(pi.getValueType().equalsIgnoreCase("string")){
								instanceValue = SparqlToXUtils.safeSparqlString(instanceValue);
							}
				
							instanceValue = this.validateDataType(instanceValue, pi.getValueType());						
							pi.addInstanceValue(instanceValue);
						}
						break;
					}
				}
			}
		}
			
		// prune nodes that no longer belong (no uri and no properties)
		ng.pruneAllUnused(true);
		
		// set URI for nulls
		ng = this.setURIsForBlankNodes(ng);
		
		return retval;
	}
	
	/**
	 * Build a value from a mapping array and a data record
	 * @param mappingArrayJson
	 * @param record
	 * @return result or Null if any column is empty
	 * @throws Exception
	 */
	private String buildMappingString(JSONArray mappingArrayJson, ArrayList<String> record) throws Exception {
		String ret = "";
		
		
		// loop through the mapping array
		Iterator<JSONObject> it = mappingArrayJson.iterator();
		for (int i=0; i < mappingArrayJson.size(); i++) {
			JSONObject mapItem = (JSONObject) mappingArrayJson.get(i);
			
			if (mapItem.containsKey("textId")) {
				String text = null; 
				// get text id
				String id = mapItem.get("textId").toString();
				
				// look up text
				try {
					text = this.textsAvailable.get(id);
				} catch (Exception e) {
					throw new Exception("Failed to look up textId: " + id);
				}
				
				// if all is well, append the value
				if(!StringUtils.isEmpty(text)){
					ret += text;
				}
				
			} else if (mapItem.containsKey("colId")) {
				String colText = null;
				Integer pos = null;
				String id = mapItem.get("colId").toString();
				try{
					String textColLabel = this.colsAvailable.get(id);
					pos = this.headerPositioningInfo.get(textColLabel);
					colText = record.get(pos);		// set the text equal to the correct column. 					
				}
				catch(Exception e){
					colText = "";
					if(pos == null){ throw new Exception("Cannot find column in header list.");}
				}
				
				if(! StringUtils.isBlank(colText)){
					ret += this.applyTransforms(colText, mapItem);
				}
				else {
					// found an empty column
					return null;
				}
			} else {
				throw new Exception("importSpec mapping item has no known type: " + mapItem.toString());
			}
		}
		return ret;
	}
	
	/**
	 * Sets this.colsUsed to number of times each column is used.  Skipping ZEROS.
	 */
	private void setupColsUsed() {
		// clear cols used
		colsUsed = new HashMap<String, Integer>();  
		
		JSONArray nodes = (JSONArray) importspec.get("nodes");  	
		
		for (int i = 0; i < nodes.size(); i++){  
			JSONObject node = (JSONObject) nodes.get(i);
			
			// check mappings in nodes
			if (node.containsKey("mapping")) {
				JSONArray mapItems = (JSONArray) node.get("mapping");
				for (int j=0; j < mapItems.size(); j++) {
					JSONObject item = (JSONObject) mapItems.get(j);
					if (item.containsKey("colId")) {
						String colId = (String) item.get("colId");
						if (colsUsed.containsKey(colId)) {
							colsUsed.put(colId, colsUsed.get(colId) + 1);
						} else {
							colsUsed.put(colId, 1);
						}
					}
				}
			}
			if (node.containsKey("props")) {
				JSONArray propItems = (JSONArray) node.get("props");
				for (int p=0; p < propItems.size(); p++) {
					JSONObject prop = (JSONObject) propItems.get(p);

					// check mappings in props
					if (prop.containsKey("mapping")) {
						JSONArray mapItems = (JSONArray) prop.get("mapping");
						for (int j=0; j < mapItems.size(); j++) {
							JSONObject item = (JSONObject) mapItems.get(j);
							if (item.containsKey("colId")) {
								String colId = (String) item.get("colId");
								if (colsUsed.containsKey(colId)) {
									colsUsed.put(colId, colsUsed.get(colId) + 1);
								} else {
									colsUsed.put(colId, 1);
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Get all column names that were actually used (lowercased)
	 * @return
	 */
	public String[] getColNamesUsed(){
		// ugly betrayal of Paul's lack of Java skills...
		Set<String> colIds = colsUsed.keySet();
		String [] ret = new String[colIds.size()];
		int i=0;
		for (String colId : colIds) {
			ret[i++] = colsAvailable.get(colId);
		}
		return ret;
	}

	/**
	 * If a column has transforms in the mapping, apply them to the input raw text
	 * @param raw - untransformed string
	 * @param mappingJson - single mapping entry
	 * @return
	 */
	private String applyTransforms(String raw, JSONObject mappingJson) {
		if (mappingJson.containsKey("transformList")) {
			String ret = raw;
			JSONArray transforms = (JSONArray) mappingJson.get("transformList");
			for(int i=0; i < transforms.size(); i += 1){
				ret = transformsAvailable.get( (String)transforms.get(i) ).applyTransform(ret);
			}
			return ret;
			
		} else {
			return raw;
		}
	}
	
	private NodeGroup setURIsForBlankNodes(NodeGroup ng) throws Exception{
		for(Node n : ng.getNodeList()){
			if(n.getInstanceValue() == null ){
				n.setInstanceValue(this.uriResolver.getInstanceUriWithPrefix(n.getFullUriName(), UUID.randomUUID().toString()) );
			}
		}
		// return the patched results.
		return ng;
	}
	
	
	private Boolean notEmpty(String instVal){
		Boolean retval = true;
		// simple checks for "emptiness"
		if(instVal == null || instVal.isEmpty() || instVal == "" || instVal.length() ==0 ){
			retval = false;
		}
		
		return retval;
	}

	/**
	 * Check that an input string is loadable as a certain SPARQL data type, and tweak it if necessary.
	 * Throws exception if not.
	 * Expects to only get the last part of the type, e.g. "float"
	 */
	@SuppressWarnings("deprecation")
	private static String validateDataType(String input, String expectedSparqlGraphType) throws Exception{
		 		 
		 //   string | boolean | decimal | int | integer | negativeInteger | nonNegativeInteger | 
		 //   positiveInteger | nonPositiveInteger | long | float | double | duration | 
		 //   dateTime | time | date | unsignedByte | unsignedInt | anySimpleType |
		 //   gYearMonth | gYear | gMonthDay;
		 
		
		/**
		 *  Please keep the wiki up to date
		 *  https://github.com/ge-semtk/semtk/wiki/Ingestion-type-handling
		 */
		String ret = input;
		
		 if(expectedSparqlGraphType.equalsIgnoreCase("string")){
			 
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("boolean")){
			 try{
				 Boolean.parseBoolean(input);
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("decimal")){
			 try{
				 Double.parseDouble(input);
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("int")){
			 try{
				 Integer.parseInt(input);
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("integer")){
			 try {
				 Integer.parseInt(input);
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("negativeInteger")){
			 try{
				 int test = Integer.parseInt(input);
				 if(test >= 0){
					 throw new Exception("value in model is negative integer. non-negative integer given as input");
			 		}
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("nonNegativeInteger")){
			 try{
				 int test = Integer.parseInt(input);
				 if(test < 0){
					 throw new Exception("value in model is nonnegative integer. negative integer given as input");
				 }
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("positiveInteger")){
			try{
				 int test = Integer.parseInt(input);
				 if(test <= 0){
					 throw new Exception("value in model is positive integer. integer <= 0 given as input");
				 } 
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("nonPositiveInteger")){
			 try{
				 int test = Integer.parseInt(input);
				 if(test > 0){
					 throw new Exception("value in model is nonpositive integer. integer > 0 given as input");
				 }
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("long")){
			 try {
				 long test = Long.parseLong(input);
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("float")){
			 try{
				 float test = Float.parseFloat(input);
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("double")){
			 try{
				 double test = Double.parseDouble(input);
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("duration")){
			 // not sure how to check this one. this might not match the expectation from SADL
			 try{
				 Duration.parse(input);
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("dateTime")){
			 try{				 
				 return Utility.getSPARQLDateTimeString(input);				 				 
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("time")){
			 try{
				 Time.parse(input);
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("date")){
			 try{
				 return Utility.getSPARQLDateString(input);				 
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("unsignedByte")){
			 try{
				 Byte.parseByte(input);
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("unsignedint")){
			 try{
				 Integer.parseUnsignedInt(input);
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("anySimpleType")){
			 // do nothing. 
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("gYearMonth")){
			 try{
				 String[] all = input.split("-");
				 // check them all
				 if(all.length != 2){ throw new Exception("year-month did not have two parts."); }
				 if(all[0].length() != 4 && all[1].length() != 2){ throw new Exception("year-month format was wrong. " + input + " given was not YYYY-MM"); }
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("gYear")){
			 try{
				 if(input.length() != 4){ throw new Exception("year-month format was wrong. " + input + " given was not YYYY-MM"); }
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else if(expectedSparqlGraphType.equalsIgnoreCase("gMonthDay")){
			 try {
			 String[] all = input.split("-");
			 // check them all
			 if(all.length != 2){ throw new Exception("month-day did not have two parts."); }
			 if(all[0].length() != 2 && all[1].length() != 2){ throw new Exception("month-day format was wrong. " + input + " given was not MM-dd"); }
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause:" + e.getMessage());
			 }
		 }
		 else {
			 	// assume it is cool for now.
		 }
		 
		 return ret;
	 }
	
}
