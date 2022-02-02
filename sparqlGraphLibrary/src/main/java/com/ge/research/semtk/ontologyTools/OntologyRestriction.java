/**
 ** Copyright 2022 General Electric Company
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

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.util.Precision;

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
public class OntologyRestriction {

	private String predicate;
	private String localPred;
	private boolean isOR;
	private String object;
	
	// as needed
	private Double numericObj = null;
	private Pattern pattern = null;

	public OntologyRestriction(String pred, String obj) {
		this.predicate  = pred;
		this.object = obj;
		
		// save simple 'local' lowercase form of predicate
		String fields[] = predicate.split("[:#]");
		this.localPred = fields[fields.length - 1].toLowerCase();
		
		// save isOR
		this.isOR = this.localPred.equals("enumeration");
		
		// for efficiency: save the obj in Double or Pattern form too.
		if (this.localPred.endsWith("clusive") || this.localPred.endsWith("ength")) {
			this.numericObj = Double.valueOf(obj);
		
		} else if (this.localPred.equals("pattern")) {
			this.pattern = Pattern.compile(obj);
		}
	}
	
	public String getUniqueKey() {
		return predicate + "," + object;
	}
	
	public String getPredicate() {
		return this.predicate;
	}
	public String getObject() {
		return this.object;
	}

	public boolean isOR() {
		return this.isOR;
	}
	
	/**
	 * Checks a value against a the restriction.
	 * On failure, be sure to check isOrRestriction() since failing an OR isn't necessarily a failure
	 * @param valStr
	 * @param xsdTypes - valid type transforms for the valStr
	 * @return  null - success, or an error message.
	 */
	public String validate(String valStr, HashSet<XSDSupportedType> xsdTypes) {
		
		// get numeric val if it will be needed later
		Double valDouble = null;
		if (this.localPred.endsWith("clusive")) {
			try {
				valDouble = Double.valueOf(valStr);
			} catch (Exception e) {
				return valStr + " can not be converted to numeric to check " + this.localPred + " restriction";
			}
			
			// don't know if the valStr for these ranges should also be type checked?
			if (! XSDSupportedType.listContainsNumeric(xsdTypes)) {
				return valStr + " type is not numeric so it fails restriction " + this.localPred;
			}
		}
		
		switch (this.localPred) {
		case "enumeration":
			// string enumerations: this.object will be surrounded by double quotes
			if (XSDSupportedType.listContainsString(xsdTypes) && ("\"" + valStr + "\"").equals(this.object))
				return null;
			// numeric enumerations match within precision
			else if (XSDSupportedType.listContainsNumeric(xsdTypes) && Precision.equals(Double.valueOf(valStr), Double.valueOf(this.object)))
				return null;
			// URI enumerations match long form :  Inconsistent with enumerated Object properties where SemTK matches short form
			else if (XSDSupportedType.listContainsUri(xsdTypes) && valStr.equals(this.object))
				return null;
			else
				return valStr + " does not match value in enumeration restriction";
			
		case "mininclusive":
			if (valDouble < this.numericObj) {
				return valStr + " fails restriction requiring >= " + this.object;
			}
			break;
		case "minexclusive":
			if (valDouble <= this.numericObj) {
				return valStr + " fails restriction requiring > " + this.object;
			}
			break;
		case "maxinclusive":
			if (valDouble > this.numericObj) {
				return valStr + " fails restriction requiring <= " + this.object;
			}
			break;
		case "maxexclusive":
			if (valDouble >= this.numericObj) {
				return valStr + " fails restriction requiring < " + this.object;
			}
			break;
		case "pattern":
			if (! this.pattern.matcher(valStr).matches()) {
				return valStr + " fails to match restriction pattern: " + this.object;
			}
			break;
		case "minlength":
			if (valStr.length() < this.numericObj) {
				return valStr + " fails restriction for min length: " + this.object;
			}
			break;
		case "maxlength":
			if (valStr.length() > this.numericObj) {
				return valStr + " fails restriction for max length: " + this.object;
			}
			break;
			
		}
		return null;
	}
}
