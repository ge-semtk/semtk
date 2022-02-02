/**
 ** Copyright 2021 General Electric Company
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


package com.ge.research.semtk.ontologyTools;

import java.util.HashMap;
import java.util.HashSet;

import org.json.simple.JSONArray;

import com.ge.research.semtk.sparqlX.XSDSupportedType;

/** 
 * Represents Datatype 
 * 
 * ?x a rdfs:Datatype                #instead of class
 * ?x owl:equivalentClass ?blank     # is typically written by SADL
 * ?blank owl:onDatatype ?equivType  # ?equivType is an XSDSupportedType
 * 
 * @author 200001934
 *
 */
public class OntologyDatatype extends AnnotatableElement{

	private OntologyName  name = null;
	private HashSet<XSDSupportedType> xsdTypes = new HashSet<XSDSupportedType>();
	private HashSet<String> strTypes = new HashSet<String>();
	private HashMap<String, OntologyRestriction> restrictions = new HashMap<String, OntologyRestriction>();   // HashMap instead of HashSet to prevent duplicates

	public OntologyDatatype(String name, String equivalentType) throws Exception {
		this.name  = new OntologyName(name);
		this.addEquivalentType(equivalentType);
	}
	
	public OntologyName getName() {
		return this.name;
	}
	
	public String getNameStr(Boolean stripNamespace){
		if(stripNamespace){
			return this.name.getLocalName();
		}
		else{
			return this.name.getFullName();
		}
	}
	
	/**
	 * Adds equivalentType.  No-op if the type is already added.
	 * @param equivalentType
	 * @throws Exception
	 */
	public void addEquivalentType(String equivalentType) throws Exception {
		this.strTypes.add(equivalentType);
		OntologyName typeName = new OntologyName(equivalentType);
		this.xsdTypes.add(XSDSupportedType.getMatchingValue(typeName.getLocalName()));
	}
	
	/**
	 * Adds a restriction.  No-op if it already is added.
	 * @param pred
	 * @param obj
	 */
	public void addRestriction(String pred, String obj) {
		OntologyRestriction restriction = new OntologyRestriction(pred, obj);
		
		this.restrictions.put(restriction.getUniqueKey(), restriction);
	}
	
	public HashSet<String> getEquivalentTypes() {
		return this.strTypes;
	}
	
	public HashSet<XSDSupportedType> getEquivalentXSDTypes() throws Exception {
		return this.xsdTypes;
	}
	
	public HashSet<OntologyRestriction> getRestrictions() {
		HashSet<OntologyRestriction> ret = new HashSet<OntologyRestriction>();
		for (String key : this.restrictions.keySet()) {
			ret.add(this.restrictions.get(key));
		}
		return ret;
	}

	/**
	 * Throws exception if builtString fails any restrictions
	 * @param builtString
	 * @throws Exception
	 */
	public void validateRestrictions(String builtString) throws Exception {
		Boolean passedOR = null;  // null:  no OR;  false: failed all;  true: passed one
		String msgOR = null;
		
		// loop through restrictions
		for (OntologyRestriction r : this.restrictions.values()) {
			String msg = r.validate(builtString, this.xsdTypes);
			
			if (r.isOR()) {
				// handle OR (enumerations) where it must pass one
				if (msg == null) {
					// OR success sets passedOR to true
					passedOR = true;
				} else if (passedOR == null) {
					// OR failure only sets passedOR to false if not true
					passedOR = false; 
					msgOR = msg;
				}
			
			} else if (msg != null) {
				// handle regular AND restrictions
				throw new Exception(this.name.getLocalName() + " " + msg);
			}
				
		}
		
		// If there was at least one OR restriction and none passed
		if (passedOR != null && passedOR != true) {
			throw new Exception(this.name.getLocalName() + " " + msgOR);
		}
		
	}
	
}
