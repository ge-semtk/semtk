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

import java.net.URI;
import java.sql.Time;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.load.transform.Transform;
import com.ge.research.semtk.load.transform.TransformInfo;
import com.ge.research.semtk.load.utility.UriResolver;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.utility.Utility;

public class ImportSpecHandler {

	JSONObject importspec = null; 	// ultimately, this should be replaced by a more complext object 
									// that does not endlessly reprocessthe same json.
	HashMap<String, Integer> headerColumn = new HashMap<String, Integer>();
	HashMap<String, Transform> transformHash = new HashMap<String, Transform>();
	HashMap<String, String> textHash = new HashMap<String, String>();
	HashMap<String, String> colHash = new HashMap<String, String>();
	HashMap<String, Integer> colsUsed = new HashMap<String, Integer>();    // count of cols used.  Only includes counts > 0
	
	// ImportMapping
	// 	MappingItems
	
	UriResolver uriResolver;
	
	
	public ImportSpecHandler(JSONObject importSpecJson, OntologyInfo oInf) throws Exception {
		this.importspec = importSpecJson;   // TODO deprecate this
		this.setupColumns(   (JSONArray) importSpecJson.get("columns"));
		this.setupTransforms((JSONArray) importSpecJson.get("transforms"));
		this.setupTexts(     (JSONArray) importSpecJson.get("texts"));
		this.setupNodes(     (JSONArray) importSpecJson.get("nodes"));
		
		String userUriPrefixValue = (String) this.importspec.get("baseURI");
		
		// check the value of the UserURI Prefix
		// LocalLogger.logToStdErr("User uri prefix set to: " +  userUriPrefixValue);
		
		this.uriResolver = new UriResolver(userUriPrefixValue, oInf);
	}
	
	public ImportSpecHandler(JSONObject spec, ArrayList<String> headers, OntologyInfo oInf) throws Exception{
		this(spec, oInf);
		this.setHeaders(headers);
	}

	public void setHeaders(ArrayList<String> headers){
		int counter = 0;
		for(String h : headers){
			this.headerColumn.put(h, counter);
			counter += 1;
		}
	}
	
	public String getUriPrefix() {
		return uriResolver.getUriPrefix();
	}
	
	/**
	 * Populate the transforms with the correct instances based on the importspec.
	 * @throws Exception 
	 */
	private void setupTransforms(JSONArray transformsJsonArr) throws Exception{
		
		if(transformsJsonArr == null){ 
			// in the event there was no transform block found in the JSON, just return.
			// thereafter, there are no transforms looked up or found.
			return;}
		
		for (int j = 0; j < transformsJsonArr.size(); ++j) {
			JSONObject xform = (JSONObject) transformsJsonArr.get(j);
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
			this.transformHash.put(instanceID, currXform);
		}
	}
	
	/**
	 * Populate the texts with the correct instances based on the importspec.
	 * @throws Exception 
	 */
	private void setupTexts(JSONArray textsJsonArr) throws Exception{
		
		if(textsJsonArr == null){ 
			return;
		}
		
		for (int j = 0; j < textsJsonArr.size(); ++j) {
			JSONObject textJson = (JSONObject) textsJsonArr.get(j);
			String instanceID = (String) textJson.get("textId"); 
			String textVal = (String) textJson.get("text");  
			this.textHash.put(instanceID, textVal);
		}
	}

	/**
	 * Populate the texts with the correct instances based on the importspec.
	 * @throws Exception 
	 */
	private void setupColumns(JSONArray columnsJsonArr) throws Exception{
		
		if(columnsJsonArr == null){ 
			return;
		}
		
		for (int j = 0; j < columnsJsonArr.size(); ++j) {
			JSONObject colsJson = (JSONObject) columnsJsonArr.get(j);
			String colId = (String) colsJson.get("colId");      
			String colName = ((String) colsJson.get("colName")).toLowerCase();  
			this.colHash.put(colId, colName);
		}
	}
	
	/**
	 * Sets this.colsUsed to number of times each column is used.  Skipping ZEROS.
	 */
	private void setupNodes(JSONArray nodesJsonArr) {
		// clear cols used
		colsUsed = new HashMap<String, Integer>();  
		
		
		for (int i = 0; i < nodesJsonArr.size(); i++){  
			JSONObject node = (JSONObject) nodesJsonArr.get(i);
			
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
	 * Populate nodegroup with a single record (row) of data
	 */
	public NodeGroup importRecord(NodeGroup ng, ArrayList<String> record) throws Exception{
		// this is a really naive implementation. it could probably be sped up drastically by caching
		// the digested template instead of re-processing it for eac incoming record.
		// the lack of conditional logic in this method would make that do-able. 
		
		NodeGroup retval = ng;
		if(ng == null){ throw new Exception("Null nodegroup passed to ImportSpecHandler.getValues"); }
		if(record  == null){ throw new Exception("incoming record cannot be null for ImportSpecHandler.getValues"); }
		if(this.headerColumn.isEmpty()){ throw new Exception("the header positions were never set for the importspechandler"); }
		
		JSONArray nodesJson = (JSONArray) this.importspec.get("nodes");  // TODO: this.importspec should be deprecated / pre-computed
		int nodesArraySize = nodesJson.size();
		
		for (int i = 0; i < nodesArraySize; i++){  
			// loop through all of the values and get the parts we need to fill in the nodegroup entries. 
			JSONObject nodeJson = (JSONObject) nodesJson.get(i);
			JSONArray mappingJson = (JSONArray) nodeJson.get("mapping");
			JSONArray propsJson  = (JSONArray) nodeJson.get("props");
						
			// get the related node from the NodeGroup
			String sparqlID = nodeJson.get("sparqlID").toString();
			Node node = ng.getNodeBySparqlID(sparqlID);   // TODO: node index should be cached
			
			// generate the uri value. all of this can be simplified later into a structure that actually remembers the format 
			// because position info will not drift as each line is processed. this was done for expedience of development. 

			String uri = this.buildMappingString(mappingJson, record);
			
			// check for a null column mapping having been found. if so, change the URI to a guid and make this a blank node. 
			// encode uri and set it. 
			if(StringUtils.isBlank(uri)){
				node.setInstanceValue(null);
			}
			else{
				uri = this.uriResolver.getInstanceUriWithPrefix(node.getFullUriName(), uri);
				if (! SparqlToXUtils.isLegalURI(uri)) { throw new Exception("Attempting to insert ill-formed URI: " + uri); }
				node.setInstanceValue(uri);
			}
			
			
			// set any applicable property values. again, this could be greatly simplified in terms of number of look ups and, 
			// potentially, running time. it should be encapsulated into an object that understands the nodes itself and stores
			// this sort of info. this was done the hard way in the interest of simplifying implementation and debugging. 
			
			ArrayList<PropertyItem> inScopeProperties = node.getPropertyItems();
			
			Iterator<JSONObject> pMap = propsJson.iterator();
			while(pMap.hasNext()) {
				String[] namesOfTransformsToApplyProp = null;
				
				// go through all the parts of the property as well. 
				JSONObject currProp = pMap.next();
				String uriRelation = currProp.get("URIRelation").toString();
				
				JSONArray mapping = (JSONArray) currProp.get("mapping");
				
				String instanceValue = this.buildMappingString(mapping, record);

				// find and set the actual property value. 
				for(PropertyItem pi : inScopeProperties){
					if(pi.getUriRelationship().equals(uriRelation)){  // e.g. http://research.ge.com/print/testconfig#cellId
						
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
	 * Return a pointer to every PropertyItem in ng that has a mapping in the import spec
	 * @param ng
	 * @return
	 */
	public ArrayList<PropertyItem> getMappedPropItems(NodeGroup ng) {
		// TODO: this is only used by tests?
		ArrayList<PropertyItem> ret = new ArrayList<PropertyItem>();
		
		JSONArray nodes = (JSONArray) this.importspec.get("nodes");  // TODO: this.importspec should be deprecated / pre-computed
		
		// loop through the json nodes in the import spec
		for (int i = 0; i < nodes.size(); i++){  
			JSONObject nodeJson = (JSONObject) nodes.get(i);
						
			// get the related node from the NodeGroup
			String sparqlID = nodeJson.get("sparqlID").toString();
			Node snode = ng.getNodeBySparqlID(sparqlID);
			
			// loop through Json node's properties
			JSONArray propsJArr  = (JSONArray) nodeJson.get("props");
			for (int j=0; j < propsJArr.size(); j++) {
				JSONObject propJson = (JSONObject) propsJArr.get(j);
				String uriRelation = propJson.get("URIRelation").toString();
				
				// if propertyJson has a mapping, return the PropertyItem
				JSONArray propMapJArr = (JSONArray) propJson.get("mapping");
				if (propMapJArr != null && propMapJArr.size() > 0) {
					PropertyItem pItem = snode.getPropertyByURIRelation(uriRelation);
					ret.add(pItem);
				}
			}
		}
		
		return ret;
	}
	/**
	 * Build a value from a json mapping array and a data record
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
			JSONObject mapItemJson = (JSONObject) mappingArrayJson.get(i);
			
			if (mapItemJson.containsKey("textId")) {        // TODO: cache to more efficient check
				String text = null; 
				// get text id
				String id = mapItemJson.get("textId").toString();
				
				// look up text
				try {
					text = this.textHash.get(id);
				} catch (Exception e) {
					throw new Exception("Failed to look up textId: " + id);
				}
				
				// if all is well, append the value
				if(!StringUtils.isEmpty(text)){
					ret += text;                            // TODO: cache: this item is a text, here's the value
				}
				
			} else if (mapItemJson.containsKey("colId")) {  // TODO: cache to more efficient check
				String colText = null;
				Integer pos = null;
				String id = mapItemJson.get("colId").toString();
				try{
					String textColLabel = this.colHash.get(id);
					pos = this.headerColumn.get(textColLabel);   // TODO: cache pos
					colText = record.get(pos);		// set the text equal to the correct column. 					
				}
				catch(Exception e){
					colText = "";
					if(pos == null){ throw new Exception("Cannot find column in header list.");}
				}
				
				if(! StringUtils.isBlank(colText)){
					ret += this.applyTransforms(colText, mapItemJson);   // TODO: cache list of transforms instead of using mapItemJson
				}
				else {
					// found an empty column
					return null;
				}
			} else {
				throw new Exception("importSpec mapping item has no known type: " + mapItemJson.toString());
			}
		}
		return ret;
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
			ret[i++] = colHash.get(colId);
		}
		return ret;
	}

	/**
	 * If a column has transforms in the mapping, apply them to the input raw text
	 * @param raw - untransformed string
	 * @param mappingJson - single mapping entry
	 * @return
	 */
	private String applyTransforms(String raw, JSONObject mappingJson) {    // TODO:  mappingJson could be cached ArrayList<Transform> or ArrayList<String> hash keys
		if (mappingJson.containsKey("transformList")) {
			String ret = raw;
			JSONArray transformJsonArr = (JSONArray) mappingJson.get("transformList");
			for(int i=0; i < transformJsonArr.size(); i += 1){
				ret = transformHash.get( (String)transformJsonArr.get(i) ).applyTransform(ret);
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
	public static String validateDataType(String input, String expectedSparqlGraphType) throws Exception{
		 
		 //   from the XSD data types:
		 //   string | boolean | decimal | int | integer | negativeInteger | nonNegativeInteger | 
		 //   positiveInteger | nonPositiveInteger | long | float | double | duration | 
		 //   dateTime | time | date | unsignedByte | unsignedInt | anySimpleType |
		 //   gYearMonth | gYear | gMonthDay;
		 
		// 	  added for the runtimeConstraint:
		//	  NODE_URI
		
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
		 else if(expectedSparqlGraphType.equalsIgnoreCase("NODE_URI")){
			 try {
				 // check that this looks like a URI
				 URI uri = new URI(input);
			 }
			 catch(Exception e){
				 throw new Exception("attempt to use value \"" + input + "\" as type \"" + expectedSparqlGraphType + "\" failed. assumed cause: " + e.getMessage());
			 }
				 
		 }
		 
		 else {
			 	// assume it is cool for now.
		 }
		 
		 return ret;
	 }
	
}
