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
import java.util.Set;
import java.util.UUID;

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

	JSONObject importspec = null; 	 // TODO deprecate
	
	JSONObject ngJson = null;
	HashMap<String, Integer> colIndexHash = new HashMap<String, Integer>();
	HashMap<String, Transform> transformHash = new HashMap<String, Transform>();
	HashMap<String, String> textHash = new HashMap<String, String>();
	HashMap<String, String> colNameHash = new HashMap<String, String>();
	HashMap<String, Integer> colsUsed = new HashMap<String, Integer>();    // count of cols used.  Only includes counts > 0
	
	ImportMapping mappings[] = null;
	
	UriResolver uriResolver;
	
	
	public ImportSpecHandler(JSONObject importSpecJson, JSONObject ngJson, OntologyInfo oInfo) throws Exception {
		this.importspec = importSpecJson; 
		
		this.ngJson = ngJson;
		this.setupColNameHash(   (JSONArray) importSpecJson.get("columns"));
		this.setupTransforms((JSONArray) importSpecJson.get("transforms"));
		this.setupTextHash(     (JSONArray) importSpecJson.get("texts"));
		
		String userUriPrefixValue = (String) this.importspec.get("baseURI");
		
		// check the value of the UserURI Prefix
		// LocalLogger.logToStdErr("User uri prefix set to: " +  userUriPrefixValue);
		
		this.uriResolver = new UriResolver(userUriPrefixValue, oInfo);
	}
	
	public ImportSpecHandler(JSONObject importSpecJson, ArrayList<String> headers, JSONObject ngJson, OntologyInfo oInfo) throws Exception{
		this(importSpecJson, ngJson, oInfo);
		this.setHeaders(headers);
	}

	public void setHeaders(ArrayList<String> headers) throws Exception{
		int counter = 0;
		for(String h : headers){
			this.colIndexHash.put(h, counter);
			counter += 1;
		}
		
		// TODO:  bad (unfixed from original code write)
		//  setupNodes happens later because setHeaders happens later.
		//  it seems like we could/should require this at instantiation time
		this.setupNodes(     (JSONArray) this.importspec.get("nodes"));
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
	private void setupTextHash(JSONArray textsJsonArr) throws Exception{
		
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
	private void setupColNameHash(JSONArray columnsJsonArr) throws Exception{
		
		if(columnsJsonArr == null){ 
			return;
		}
		
		for (int j = 0; j < columnsJsonArr.size(); ++j) {
			JSONObject colsJson = (JSONObject) columnsJsonArr.get(j);
			String colId = (String) colsJson.get("colId");      
			String colName = ((String) colsJson.get("colName")).toLowerCase();  
			this.colNameHash.put(colId, colName);
		}
	}
	
	/**
	 * Sets this.colsUsed to number of times each column is used.  Skipping ZEROS.
	 * @throws Exception 
	 */
	private void setupNodes(JSONArray nodesJsonArr) throws Exception {
		NodeGroup tmpNodegroup = NodeGroup.getInstanceFromJson(this.ngJson);
		ArrayList<ImportMapping> mappingsList = new ArrayList<ImportMapping>();
		// clear cols used
		colsUsed = new HashMap<String, Integer>();  
		ImportMapping mapping = null;
		
		// loop through .nodes
		for (int i = 0; i < nodesJsonArr.size(); i++){  
			JSONObject nodeJson = (JSONObject) nodesJsonArr.get(i);
			
			// URI 
			mapping = new ImportMapping();
			
			// get node index
			String nodeSparqlID = nodeJson.get("sparqlID").toString();
			int nodeIndex = tmpNodegroup.getNodeIndexBySparqlID(nodeSparqlID);
			mapping.setsNodeIndex(nodeIndex);
			
			// add mapping
			if (nodeJson.containsKey("mapping")) {
				JSONArray mappingJsonArr = (JSONArray) nodeJson.get("mapping");
				setupMappingItemList(mappingJsonArr, mapping);
			}
			mappingsList.add(mapping);
			
			// Properties
			if (nodeJson.containsKey("props")) {
				JSONArray propsJsonArr = (JSONArray) nodeJson.get("props");
				Node snode = tmpNodegroup.getNode(nodeIndex);
				
				for (int p=0; p < propsJsonArr.size(); p++) {
					JSONObject propJson = (JSONObject) propsJsonArr.get(p);
					mapping = new ImportMapping();
					mapping.setsNodeIndex(nodeIndex);
					int propIndex = snode.getPropertyIndexByURIRelation((String)propJson.get("URIRelation"));
					mapping.setPropItemIndex(propIndex);
					
					// add mapping
					if (propJson.containsKey("mapping")) {
						JSONArray mappingJsonArr = (JSONArray) propJson.get("mapping");
						setupMappingItemList(mappingJsonArr, mapping);
					}
				}
				mappingsList.add(mapping);
			}
		}
		this.mappings = mappingsList.toArray(new ImportMapping[mappingsList.size()]);
	}
	
	/**
	 * Put mapping items from json into an ImportMapping and update colUsed
	 * @param mappingJsonArr
	 * @param mapping
	 * @throws Exception 
	 */
	private void setupMappingItemList(JSONArray mappingJsonArr, ImportMapping mapping) throws Exception {
		
		for (int j=0; j < mappingJsonArr.size(); j++) {
			JSONObject itemJson = (JSONObject) mappingJsonArr.get(j);
			
			MappingItem mItem = new MappingItem();
			mItem.fromJson(	itemJson, 
							this.colNameHash, this.colIndexHash, this.textHash, this.transformHash);
			mapping.addItem(mItem);
			
			if (itemJson.containsKey("colId")) {
				// column item
				String colId = (String) itemJson.get("colId");
				
				// colsUsed
				if (colsUsed.containsKey(colId)) {
					colsUsed.put(colId, colsUsed.get(colId) + 1);
				} else {
					colsUsed.put(colId, 1);
				}
			} 
		}
	}
	
	/**
	 * Create a nodegroup from a single record (row) of data
	 */
	public NodeGroup importRecord(ArrayList<String> record) throws Exception{

		// create a new nodegroup copy
		NodeGroup retVal = NodeGroup.getInstanceFromJson(this.ngJson);
		
		if(record  == null){ throw new Exception("incoming record cannot be null for ImportSpecHandler.getValues"); }
		if(this.colIndexHash.isEmpty()){ throw new Exception("the header positions were never set for the importspechandler"); }
		
		for (int i=0; i < this.mappings.length; i++) {
			ImportMapping mapping = mappings[i];
			String val = mapping.buildString(record);
			Node node = retVal.getNode(mapping.getsNodeIndex());
			PropertyItem propItem = null;
			
			if (mapping.isProperty()) {
				// properties
				if(val.length() > 0) {
					propItem = node.getPropertyItem(mapping.getPropItemIndex());
					val = this.validateDataType(val, propItem.getValueType());						
					propItem.addInstanceValue(val);
				}
				
			} else {
				// nodes
				if(val.length() < 1){
					node.setInstanceValue(null);
				}
				else{
					String uri = this.uriResolver.getInstanceUriWithPrefix(node.getFullUriName(), val);
					if (! SparqlToXUtils.isLegalURI(uri)) { throw new Exception("Attempting to insert ill-formed URI: " + uri); }
					node.setInstanceValue(uri);
				}
			}
			
		}
			
		// prune nodes that no longer belong (no uri and no properties)
		retVal.pruneAllUnused(true);
		
		// set URI for nulls
		retVal = this.setURIsForBlankNodes(retVal);
		
		return retVal;
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
	 * Get all column names that were actually used (lowercased)
	 * @return
	 */
	public String[] getColNamesUsed(){
		// ugly betrayal of Paul's lack of Java skills...
		Set<String> colIds = colsUsed.keySet();
		String [] ret = new String[colIds.size()];
		int i=0;
		for (String colId : colIds) {
			ret[i++] = colNameHash.get(colId);
		}
		return ret;
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
			 ret = SparqlToXUtils.safeSparqlString(input);
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
