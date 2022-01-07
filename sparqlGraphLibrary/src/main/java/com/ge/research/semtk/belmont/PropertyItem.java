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
import java.util.HashSet;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.ontologyTools.OntologyName;
import com.ge.research.semtk.sparqlX.XSDSupportedType;

/**
 * Property Item
 * @author 200001934
 *
 */
public class PropertyItem extends Returnable {
	public static final int OPT_MINUS_NONE = 0;
	public static final int OPT_MINUS_OPTIONAL = 1;
	public static final int OPT_MINUS_MINUS = 2;
	public static final int OPT_MINUS_EXIST = 3;
	
	private HashSet<XSDSupportedType> valueTypes = null;   // if range is a OntologyDatatype, could be multiple
	                                                       // only the XSDSupportedTypes are in the PropertyItem
	private String rangeURI = null;          
	private String domainURI = null;       
	
	private int optMinus = OPT_MINUS_NONE;
	private ArrayList<String> instanceValues = new ArrayList<String>();
	
	private Boolean isMarkedForDeletion = false;

	public PropertyItem(XSDSupportedType valueType, String valueTypeURI, String uriRelationship){
		this.valueTypes = new HashSet<XSDSupportedType>();
		this.valueTypes.add(valueType);
		this.rangeURI = valueTypeURI;   
		this.domainURI = uriRelationship;
	}
	
	public PropertyItem(HashSet<XSDSupportedType> valueTypes, String valueTypeURI, String uriRelationship){
		this.valueTypes = valueTypes;
		this.rangeURI = valueTypeURI;   
		this.domainURI = uriRelationship;
	}
		
	
	public PropertyItem(JSONObject jObj) throws Exception {
		// keeps track of the properties who are in the domain of a given node.
		
		this.fromReturnableJson(jObj);
		
		this.valueTypes = new HashSet<XSDSupportedType>();
		// old single ValueType
		if (jObj.containsKey("ValueType")) {
			String typeStr = (String) (jObj.get("ValueType"));
			try {
				XSDSupportedType typeVal =  XSDSupportedType.getMatchingValue(typeStr);
				this.valueTypes.add(typeVal);
			} catch (Exception e) {
				// treat unknowns as a NODE_URI, outside of semTK "domain"
				this.valueTypes.add(XSDSupportedType.NODE_URI);
			}
		// newer array valueTypes
		} else {
			JSONArray typeArr = (JSONArray) (jObj.get("valueTypes"));
			for (Object o : typeArr) {
				String typeStr = (String) o;
				try {
					XSDSupportedType typeVal =  XSDSupportedType.getMatchingValue(typeStr);
					this.valueTypes.add(typeVal);
				} catch (Exception e) {
					// treat unknowns as a NODE_URI, outside of semTK "domain"
					this.valueTypes.add(XSDSupportedType.NODE_URI);
				}
			}
		}
		
		if (jObj.containsKey("relationship")) {
			this.rangeURI = jObj.get("relationship").toString();  // note that label "relationship" in the JSON is misleading
			this.domainURI = jObj.get("UriRelationship").toString();
		} else {
			this.rangeURI = jObj.get("rangeURI").toString();  // note that label "relationship" in the JSON is misleading
			this.domainURI = jObj.get("domainURI").toString();
		}
				
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
		JSONArray valTypes = new JSONArray();
		for (XSDSupportedType t : this.valueTypes) {
			valTypes.add(t.getSimpleName());
		}
		ret.put("valueTypes", valTypes);
		ret.put("rangeURI", this.rangeURI);
		ret.put("domainURI", this.domainURI);
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
		return new OntologyName(domainURI).getLocalName();
	}
	
	public String getDomainURI() {
		return this.domainURI;
	}
	
	public void setDomainURI(String uri) {
		this.domainURI = uri;
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

	// Returnable needs to be plural, and a bunch of strings?  should it be the URI?
	public HashSet<XSDSupportedType> getValueTypes() {
		return this.valueTypes;
	}
	
	public String getRangeURI() {
		return this.rangeURI;
	}
	
	@Deprecated
	public String getValueTypeURI() {
		return this.getRangeURI();
	}
	
	public void addInstanceValue(String value) {
		this.instanceValues.add(value);
	}
	
	public void setRange(String fullURI, HashSet<XSDSupportedType> types) {
		this.valueTypes = types;
		this.rangeURI = fullURI;
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

	public String buildRDF11ValueString(String val) {
		return buildRDF11ValueString(val, null);
	}
	
	public String buildRDF11ValueString(String val, String typePrefixOverride) {
		if (this.valueTypes.size() == 1) {
			for (XSDSupportedType t : this.valueTypes) {
				// return first-and-only one
				return t.buildRDF11ValueString(val, typePrefixOverride);
			}
		} else {
			for (XSDSupportedType t : this.valueTypes) {
				try {
					// return first valid type
					t.validate(val);
					return t.buildRDF11ValueString(val, typePrefixOverride);
				} catch (Exception e) {
					// try next one
				}
			}
		}
		// if nothing was valid return a string
		return XSDSupportedType.STRING.buildRDF11ValueString(val, typePrefixOverride);
	}
	
	/**
	 * For validating: are these the current valueTypes
	 * @param types
	 * @return boolean
	 */
	public boolean valueTypesEqual(HashSet<XSDSupportedType> types) {
		if (types.size() != this.valueTypes.size()) {
			return false;
		}
		for (XSDSupportedType t : types) {
			if (! this.valueTypes.contains(t)) {
				return false;
			}
		}
		return true;
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
