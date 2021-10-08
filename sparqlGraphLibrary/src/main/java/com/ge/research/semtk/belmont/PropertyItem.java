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


package com.ge.research.semtk.belmont;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.ontologyTools.OntologyName;
import com.ge.research.semtk.sparqlX.XSDSupportedType;

public class PropertyItem extends Returnable {
	public static final int OPT_MINUS_NONE = 0;
	public static final int OPT_MINUS_OPTIONAL = 1;
	public static final int OPT_MINUS_MINUS = 2;
	public static final int OPT_MINUS_EXIST = 3;
	
	private XSDSupportedType valueType = null;
	private String valueTypeURI = null;  
	private String uriRelationship = null; // obsolete, should always be this.valueType.getFullName()
	
	private int optMinus = OPT_MINUS_NONE;
	private ArrayList<String> instanceValues = new ArrayList<String>();
	
	private Boolean isMarkedForDeletion = false;

	
	/**
	 * Constructor
	 * @param nome (e.g. pasteMaterial)
	 * @param valueType (e.g. string)
	 * @param valueTypeURI (e.g. http://www.w3.org/2001/XMLSchema#string)
	 * @param uriRelationship (e.g. http://research.ge.com/print/testconfig#material)
	 */
	@Deprecated
	public PropertyItem(String nomeDEPRECATED, XSDSupportedType valueType, String valueTypeURI, String uriRelationship){
		this.valueType = valueType;
		this.valueTypeURI = valueTypeURI;   
		this.uriRelationship = uriRelationship;
	}
	@Deprecated
	public PropertyItem(String nomeDEPRECATED, String valueTypeStr, String valueTypeURI, String uriRelationship) throws Exception {
		this(nomeDEPRECATED, XSDSupportedType.getMatchingValue(valueTypeStr), valueTypeURI, uriRelationship);
	}

	public PropertyItem(XSDSupportedType valueType, String valueTypeURI, String uriRelationship){
		this.valueType = valueType;
		this.valueTypeURI = valueTypeURI;   
		this.uriRelationship = uriRelationship;
	}
		
	public PropertyItem(String valueTypeStr, String valueTypeURI, String uriRelationship) throws Exception {
		this(XSDSupportedType.getMatchingValue(valueTypeStr), valueTypeURI, uriRelationship);
	}
	
	public PropertyItem(JSONObject jObj) throws Exception {
		// keeps track of the properties who are in the domain of a given node.
		
		this.fromReturnableJson(jObj);
		
		
		String typeStr = (String) (jObj.get("ValueType"));
		try {
			XSDSupportedType typeVal =  XSDSupportedType.getMatchingValue(typeStr);
			this.valueType = typeVal;
		} catch (Exception e) {
			// treat unknowns as a NODE_URI, outside of semTK "domain"
			this.valueType = XSDSupportedType.NODE_URI;
		}
		
		this.valueTypeURI = jObj.get("relationship").toString();  // note that label "relationship" in the JSON is misleading
		this.uriRelationship = jObj.get("UriRelationship").toString();
				
		this.optMinus = OPT_MINUS_NONE;
		if (jObj.containsKey("isOptional")) {
			this.optMinus = ((Boolean) jObj.get("isOptional")) ? OPT_MINUS_OPTIONAL : OPT_MINUS_NONE;
		} else if (jObj.containsKey("optMinus")) {
			this.optMinus = Integer.parseInt(jObj.get("optMinus").toString());
		}
		
		try{
			this.setIsMarkedForDeletion((Boolean)jObj.get("isMarkedForDeletion"));
		}
		catch(Exception eee){
			this.setIsMarkedForDeletion(false);
		}
		
		JSONArray instArr = (JSONArray)jObj.get("instanceValues");
		Iterator<String> it = instArr.iterator();
		while(it.hasNext()){
			this.instanceValues.add(it.next());
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		
		JSONArray iVals = new JSONArray();
		for (int i=0; i < this.instanceValues.size(); i++) {
			iVals.add(this.instanceValues.get(i));
		}

		JSONObject ret = new JSONObject();
		this.addReturnableJson(ret);
		ret.put("ValueType", this.valueType.getSimpleName());
		ret.put("relationship", this.valueTypeURI);
		ret.put("UriRelationship", this.uriRelationship);
		ret.put("optMinus", this.optMinus);
		ret.put("isMarkedForDeletion", this.isMarkedForDeletion);
		ret.put("instanceValues", iVals);
                                           
		return ret;
	}

	/** 
	 * clear everything except names, types, and connections between nodes
	 */
	public void reset() {
		this.constraints = null;
		this.isReturned = false;
		this.isBindingReturned = false;
		this.optMinus = OPT_MINUS_NONE;
		this.isMarkedForDeletion = false;
		this.isRuntimeConstrained = false;
	}
	
	public boolean getIsOptional() {
		return this.optMinus == OPT_MINUS_OPTIONAL;
	}
	
	public int getOptMinus() {
		return this.optMinus;
	}
	
	public String getKeyName() {
		return new OntologyName(uriRelationship).getLocalName();
	}
	
	public String getUriRelationship() {
		return this.uriRelationship;
	}
	
	public void setUriRelationship(String uriRelationship) {
		this.uriRelationship = uriRelationship;
	}


	/**
	 * Return constraints SPARQL or null
	 * @return {String} or null
	 */
	public String getConstraints() {
		if (constraints != null) {
			String constraintStr =  this.constraints.toString();
			constraintStr = constraintStr.replaceAll("%id", this.sparqlID);
			return constraintStr;
		}
		else {
			return null;
		}
		
	}

	public boolean isUsed() {
		return (this.hasAnyReturn() || this.constraints != null || this.isRuntimeConstrained || this.instanceValues.size() > 0 || this.isMarkedForDeletion || this.getOptMinusIsUsed());
	}
	
	/**
	 * Is optMinus alone enough to make the property .isUsed() == true
	 * @return
	 */
	public boolean getOptMinusIsUsed() {
        return this.optMinus == PropertyItem.OPT_MINUS_MINUS || this.optMinus == PropertyItem.OPT_MINUS_EXIST;
    }
	
	public ArrayList<String> getInstanceValues() {
		return this.instanceValues;
	}

	public XSDSupportedType getValueType() {
		return this.valueType;
	}
	
	public String getValueTypeURI() {
		return this.valueTypeURI;
	}
	
	/**
	 * Newer-fangled way to change the range
	 * @param uri
	 * @throws Exception
	 */
	public void setRange(String uri) throws Exception {
		this.valueTypeURI = uri;
		this.valueType =  XSDSupportedType.getMatchingValue(new OntologyName(uri).getLocalName());
	}

	public void addInstanceValue(String value) {
		this.instanceValues.add(value);
	}
	
	public void changeValueType(XSDSupportedType type) {
		this.valueType = type;
		this.valueTypeURI = type.getFullName();
	}
	
	public void setIsReturned(boolean b) throws Exception {
		if (b == true && this.getSparqlID().isEmpty()) {
			throw new Exception("Attempt to return property with no sparqlID");
		}
		this.isReturned = b;
	}
	
	@Deprecated
	public void setIsOptional(boolean b){
		this.optMinus = b ? OPT_MINUS_OPTIONAL : OPT_MINUS_NONE;
	}
	
	public void setOptMinus(int optMin) {
		this.optMinus = optMin;
	}
	
	public void addConstraint(String str) {
		this.constraints = new ValueConstraint(str);
	}
	
	/**
	 * Sets the sparqlID field and updates the constraints if needed.
	 * 
	 * WARNING: sparqlId must be ok'ed by the nodegroup first.
	 *          Heartily recommend using NodeGroup.changeSparqlID(), 
	 *          which will call here.
	 *          
	 * @param sparqlId - sparqlId already cleared by the nodegroup as ok
	 */
	public void setSparqlID(String sparqlId){
		if (this.constraints != null) {
			this.constraints.changeSparqlID(this.sparqlID, sparqlId);
		}
		this.sparqlID = sparqlId;
	}
	
	public void setIsMarkedForDeletion(boolean delete){
		this.isMarkedForDeletion = delete;
	}
	
	public boolean getIsMarkedForDeletion(){
		return this.isMarkedForDeletion;
	}

	/**
	 * Set this property with all the values from other except URIRelationship
	 * @param other
	 * @exception if this property isn't empty
	 */
	public void merge(PropertyItem other) throws Exception {
		if (this.isUsed() || this.getBindingOrSparqlID().length() > 0 ) {
        	throw new Exception ("Target of property merge is not empty: " + this.getKeyName());
        }
		
		this.constraints = other.constraints;
        this.sparqlID = other.sparqlID;
        this.isReturned = other.isReturned;
        this.optMinus = other.optMinus;
        this.isRuntimeConstrained = other.isRuntimeConstrained;
        // instanceValues,
        this.isMarkedForDeletion = other.isMarkedForDeletion;
        this.binding = other.binding;
		
	}

}
