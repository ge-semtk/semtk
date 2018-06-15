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

package com.ge.research.semtk.belmont.runtimeConstraints;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.BelmontUtil;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.belmont.XSDSupportedType;
import com.ge.research.semtk.load.utility.ImportSpecHandler;
import com.ge.research.semtk.resultSet.Table;

public class RuntimeConstraints {

	// date formats are supposed to look like this: 2014-05-23T10:20:13
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
	
	public static enum SupportedTypes { NODE , PROPERTYITEM };
	
	private HashMap<String, RuntimeConstrainedObject> rtcObjectHash;
	
	public RuntimeConstraints() {
		this.rtcObjectHash = new HashMap<String, RuntimeConstrainedObject>();
	}
	
	public RuntimeConstraints(NodeGroup parent){
		
		// set up the constraint items
		this.rtcObjectHash = parent.getConstrainedItems();
	}
	
	public ArrayList<String> getConstrainedItemIds(){
		ArrayList<String> retval = new ArrayList<String>();
		
		for(String s : this.rtcObjectHash.keySet() ){
			retval.add(s);
		}
		return retval;
	}
	public JSONArray toJson(){
		JSONArray retval = new JSONArray();
		
		for(RuntimeConstrainedObject rco : this.rtcObjectHash.values()){
			// create a new JSONObject
			JSONObject curr = new JSONObject();
			
			// add the basics:
			curr.put("SparqlID", rco.getObjectName());
			curr.put("Operator", rco.getOperationName());
			
			// get the operands.
			JSONArray operandArr = new JSONArray();
			
			for(String op : rco.getOperands()){
				operandArr.add(op);
			}
			
			curr.put("Operands", operandArr);

			// add the last "current" entry to the outgoing array.
			retval.add(curr);
		}

		
		// ship it out
		return retval;
	}
	
	// acccept a json describing the runtime constraints and apply them.	
	public void applyConstraintJson(JSONArray runtimeConstraints) throws Exception {
		// read out and apply the various RT constraints from the JSON. 
		// Throw an exception if anything seems amiss.
		
		// NOTE: we will be inferring the type of the operands by type of the constrained item.
		//       this is why we do not require the type in the incoming JSON. this might break one
		//       day if multiple primitive types become supported in ranges for properties.
		
		if(runtimeConstraints == null || runtimeConstraints.isEmpty()){
			return; // nothing to do
		}
		
		for(Object curr : runtimeConstraints){
			JSONObject c1 = (JSONObject) curr; 	// just a shortcut to avoid a multitude of casts.
			
			// check that the sparqlID exists skipped because it will be checked when a direct assignment is made.
			String sparqlId = c1.get("SparqlID").toString();
			String operator = c1.get("Operator").toString();
			XSDSupportedType operandType;
			ArrayList<String> operands = new ArrayList<String>();   // however obvious, the operands will go here. 
			
			// get the object referenced by this sparql ID.
			if(this.rtcObjectHash.get(sparqlId) == null){
				throw new Exception("Cannot find runtime-constrainable item in nodegroup.  sparqlID: " + sparqlId);
			}
			if( this.rtcObjectHash.get(sparqlId).getObjectType().equals(SupportedTypes.NODE) ){
				// this was a node and the type should be URI.
				operandType = XSDSupportedType.NODE_URI;
			}			
			else if ( this.rtcObjectHash.get(sparqlId).getObjectType().equals(SupportedTypes.PROPERTYITEM) ){
				// check the property item itself to get the expected type.
				operandType = this.rtcObjectHash.get(sparqlId).getValueType();  // this should return the expected XSD type with no prefix. 
			}
			else{
				throw new Exception("Can't apply runtime constraints to object type " + this.rtcObjectHash.get(sparqlId).getObjectType() + " for sparqlID: " + sparqlId);
			}
			
			JSONArray opers = (JSONArray) c1.get("Operands");
			
			// step through the array and get the operands.
			try{
				for( Object currOpperand : opers){
					// add the next operand to the list if it passes the tests.
					// check the type are at least convertible. for this, we should reuse the code from the import spec handler.				
					ImportSpecHandler.validateDataType(currOpperand.toString(), operandType);
					operands.add( currOpperand.toString() );  // here we go. 
				}
			} catch(Exception eee){
				// we were passed a bad value that could not be cast to the suggested type.
				throw new Exception("Runtime constraint value for " + sparqlId + " one of the input values could not be cast to " + operandType);
			}
			
			SupportedOperations operationValue = null;
			
			// check the value of the operator for sanity
			try{
				operationValue = SupportedOperations.valueOf(operator);
					// do nothing. we just needed this check to make it all work
			}
			catch(Exception eee){
				throw new Exception("Runtime constraint operator " + operator + " for sparqlID " + sparqlId +  " is not supported");
			}
			
			// create the appropriate constraint and apply it.
			this.rtcObjectHash.get(sparqlId).applyConstraint(operationValue, operands);
			
		}
		
	}
	
	public void applyConstraint(String sparqlId, SupportedOperations operation, ArrayList<String> operands) throws Exception{
		String id = BelmontUtil.formatSparqlId(sparqlId);
		// find the appropriate constrained object and then pass along the work
		this.rtcObjectHash.get(id).applyConstraint(operation, operands);
		
	}
	
	public void selectAndSetConstraint(String sparqlId, String operationID, XSDSupportedType xsdType, ArrayList<String> operands) throws Exception{
		String id = BelmontUtil.formatSparqlId(sparqlId);
		// find the appropriate constrained object and then pass along the work
		this.rtcObjectHash.get(id).selectAndSetConstraint(id, operationID, xsdType, operands);
		
	}
	
	public ValueConstraint getValueConstraint(String itemSparqlId) throws Exception{
		ValueConstraint retval = null;
		String id = BelmontUtil.formatSparqlId(itemSparqlId);
		
		// check to see if this item is in our list.
		if(rtcObjectHash.containsKey(id)){
			retval = rtcObjectHash.get(id).getValueConstraint();
		}
		
		return retval;
	}
	
	public XSDSupportedType getValueType(String itemSparqlId) throws Exception{
		XSDSupportedType retval = null;
		String id = BelmontUtil.formatSparqlId(itemSparqlId);
		// check to see if this item is in our list.
		if(rtcObjectHash.containsKey(id)){
			retval = rtcObjectHash.get(id).getValueType();
		}
		else{
			throw new Exception(itemSparqlId + " does not exist in the available runtime constrained items.");
		}
		
		return retval;
	}
		
	public String getItemType(String itemSparqlId) throws Exception{
		String retval = "";
		String id = BelmontUtil.formatSparqlId(itemSparqlId);
		
		if(rtcObjectHash.containsKey(id)){
			SupportedTypes st = rtcObjectHash.get(id).getObjectType();

			retval = st.name();		// get the name.
		}
		else{
			throw new Exception(itemSparqlId + " does not exist in the available runtime constrained items.");
		}
		return retval;
	}
	
	public Table getConstrainedItemsDescription() throws Exception{
		Table retval = null;
		
		ArrayList<ArrayList<String>> itemInfo = new ArrayList<ArrayList<String>>();
		for(String item : this.getConstrainedItemIds()){
			// add each to the table. 
			ArrayList<String> currentItemInfo = new ArrayList<String>();
			
			currentItemInfo.add(item);
			currentItemInfo.add(this.getItemType(item));
			currentItemInfo.add(this.getValueType(item).name());
			
			// add to outgoing list
			itemInfo.add(currentItemInfo);
		}
		String cols[] = {"valueId", "itemType", "valueType"};
		String type[] = {"string", "string", "string"};
		
		retval = new Table(cols, type, itemInfo);
		
		return retval;
	}
}
