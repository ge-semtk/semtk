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

import java.text.SimpleDateFormat;
import java.util.Date;

import com.ge.research.semtk.sparqlX.SparqlToXUtils;

public enum XSDSupportedType {

	STRING("string", "string") , 
	BOOLEAN("boolean", "True") , 
	DECIMAL("decimal", "27.1") , 
	INT("int", "42") , 
	INTEGER("integer", "42") , 
	NEGATIVEINTEGER("negativeInteger", "-42") , 
	NONNEGATIVEINTEGER("nonNegativeInteger", "42") , 
	POSITIVEINTEGER("positiveInteger", "42"), 
	NONPOSISITIVEINTEGER("nonPositiveInteger", "-42") , 
	LONG("long", "4200000") , 
	FLOAT("float", "42.42") , 
	DOUBLE("double", "42.42") , 
	DURATION("duration", "DURATION") , 
	DATETIME("dateTime", "2017-03-23T10:03:16") , 
	TIME("time", "10:03:16") , 
	DATE("date", "2017-03-23") , 
	UNSIGNEDBYTE("unsignedByte", "UNSIGNEDBYTE") , 
	UNSIGNEDINT("unsignedInt", "42") , 
	ANYSIMPLETYPE("anySimpleType", "ANYSIMPLETYPE") ,
	GYEARMONTH("gYearMonth", "GYEARMONTH") , 
	GMONTH("gMonth", "10") , 
	GMONTHDAY("gMonthDay", "28"), 
	NODE_URI("node_uri", "http://uri#uri");
	
	// note: "node_uri" was added for compatibility reasons to the way nodes in a nodegroup spec
	// when their URI is able to be constrained at runtime.
	private static String xmlSchemaRawPrefix = "http://www.w3.org/2001/XMLSchema";
	private static String xmlSchemaPrefix = "^^<" + xmlSchemaRawPrefix + "#";
	private static String xmlSchemaTrailer = ">";
	
	private String camelCaseStr;
	private String sampleValue;
	
	XSDSupportedType(String camelCase, String sampleVal) {
		this.camelCaseStr = camelCase;
		this.sampleValue = sampleVal;
	}
	
	public static XSDSupportedType getMatchingValue(String candidate) throws Exception {
		return XSDSupportedType.valueOf(candidate.toUpperCase());
	}
	public static String getMatchingName(String candidate) throws Exception{
		
		try{
			
		// check the requested type exists in the enumeration.
		XSDSupportedType.valueOf(candidate.toUpperCase());
		
		}catch(Exception e){
			// build a complete list of the allowed values.
			String completeList = "";
			int counter = 0;
			for (XSDSupportedType curr : XSDSupportedType.values()){
				
				if(counter != 0){ completeList += " or "; }
				completeList +=  "( " + curr.name() + " )";
				counter++;
			}
			// tell the user things were wrong.
			throw new Exception("the XSDSupportedTypes enumeration contains no entry matching " + candidate + ". Expected entries are: " + completeList);
		}
		
		return candidate.toUpperCase(); // since this passed the check for existence, we can just hand it back. 
	}

	public String buildRDF11ValueString(String val) {
		return buildRDF11ValueString(val, null);
	}
	
	public String buildRDF11ValueString(String val, String typePrefixOverride) {
		
		if (this.numericOperationAvailable()) {
			return val;
			
		} else if (this.dateOperationAvailable()) {
			return buildTypedValueString(val, typePrefixOverride);
			
		} else if (this == XSDSupportedType.NODE_URI) {
			return buildTypedValueString(val, typePrefixOverride);
			
		} else {
			return "\"" + val + "\"";
		}
				
	}
	/**
	 * Force typed literal
	 * Use buildRDF11ValueString() instead unless you're sure you want this.
	 * @param val
	 * @return
	 */
	public String buildTypedValueString(String val) {
		return this.buildTypedValueString(val, null);
	}
	
	public String buildTypedValueString(String val, String typePrefixOverride) {
		
		if (this == XSDSupportedType.NODE_URI) {
			// check for angle brackets first
			if( val.contains("#")){   // the uri given is prefixed.
				if(val.startsWith("<") && val.endsWith(">")){
					return val;  // already in brackets so probably will not break.
				}
				else {
					return "<" + val + "> ";  // e.g. VALUES ?TimeSeriesTableType { <timeseries:DataScan> } ... no type information needed for URIs
				}
			}
			else {  // the URI is assumed prefixed since it has no # . Assume the user/caller knows what they want and just use it. 
				    // PAUL July 2020.  I'm not sure why this shouldn't be checked for <> also.
			        //                  How could it work otherwise?
				    //                  I don't really want to mess with it and find out there was a reason.
				return val;
			}
			
		} else if (this == XSDSupportedType.BOOLEAN) {   
			return val.toLowerCase();
			
		} else {
			return "\"" + val + "\"" + this.getXsdSparqlTrailer(typePrefixOverride); 
		}
	}
	
	public String getFullName() {
		return xmlSchemaRawPrefix + "#" + this.camelCaseStr;
	}
	
	public String getSimpleName() {
		return this.camelCaseStr;
	}
	
	public String getSampleValue() {
		return this.sampleValue;
	}
	
	public String getPrefixedName() {
		return "XMLSchema:" + this.camelCaseStr;
	}
	
	public static boolean supportedType(String candidate){
		
		try{
			XSDSupportedType.valueOf(candidate.toUpperCase());
			return true;
		}
		catch(IllegalArgumentException e){
			return false;
		}
	}
	
	public String getXsdSparqlTrailer() {
		return getXsdSparqlTrailer(null);
	}
	
	public String getXsdSparqlTrailer(String typePrefixOverride) {
		if (typePrefixOverride == null) {
			// e.g.:  ^^<http://www.w3.org/2001/XMLSchema#int>
			return xmlSchemaPrefix 	+ this.camelCaseStr + xmlSchemaTrailer;
		} else {
			// e.g.: ^^XMLSchema:int
			return "^^" + typePrefixOverride + ":" + this.camelCaseStr;
		}
		
	}
	
	public boolean regexIsAvailable(){
		
		return (this == STRING);
	}
	
	public boolean booleanOperationAvailable(){
		return (this == BOOLEAN);
	}	
	
	public boolean dateOperationAvailable(){
		
		return (this == DATETIME ||
				this == DATE ||
				this == TIME);
	}
	
	public boolean numericOperationAvailable(){
		return (this == INT ||
				this == DECIMAL ||
				this == INTEGER ||
				this == NEGATIVEINTEGER ||
				this == NONNEGATIVEINTEGER ||
				this == POSITIVEINTEGER ||
				this == NONPOSISITIVEINTEGER ||
				this == LONG ||
				this == FLOAT ||
				this == DOUBLE);

	}
	
	public boolean rangeOperationsAvailable() {
		return dateOperationAvailable() || numericOperationAvailable();
	} 
	
	// PEC NOTE:
	//    This is not used by ingestion.  It should be.
	//
	
	/**
	 * Throw exception if proposedValue is not a legal value
	 * @param proposedValue
	 * @throws Exception
	 */
	public void validate(String proposedValue) throws Exception{
		
		// datetime: 2014-05-23T10:20:13+05:30
		// date    : 2014-05-23
		Object ret;
		// for each type: return object, throw special exception, or pass through to normal exception
		switch (this) {
		case DATE:
			try{
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
				ret = formatter.parse(proposedValue);
				return;
			}
			catch(Exception e){
				throw new Exception(proposedValue + " can't be converted to" + this.name() + ". Accepted format is yyyy-MM-dd." );
			}
		case STRING:
			ret = proposedValue;
			return;
		case BOOLEAN:
			try {
				ret = Boolean.parseBoolean(proposedValue);
				return;
			}
			catch(Exception e){
			}
			break;
		case INT:
		case INTEGER:
		case NEGATIVEINTEGER:
		case NONNEGATIVEINTEGER:
		case POSITIVEINTEGER:
		case NONPOSISITIVEINTEGER:
		case UNSIGNEDINT:
			Integer i = null;
			try {
				i = Integer.parseInt(proposedValue);
			}
			catch(Exception e){
			}
			if (this == NEGATIVEINTEGER && i > -1 ||
				this == NONNEGATIVEINTEGER && i < 0 ||
				this == NONPOSISITIVEINTEGER && i > 0 ||
				this == POSITIVEINTEGER && i < 1 ||
				this == NONPOSISITIVEINTEGER && i > 0
				) {
					break;
			} else {
				ret = i;
				return;
			}
		case DECIMAL:	
		case LONG:
			try {
				ret = Long.parseLong(proposedValue);
				return;
			}
			catch(Exception e){
			}
			break;
		case FLOAT:
			try {
				ret = Float.parseFloat(proposedValue);
				return;
			}
			catch(Exception e){
			}
			break;
		case DOUBLE:
			try {
				ret = Double.parseDouble(proposedValue);
				return;
			}
			catch(Exception e){
			}
			break;
		case DATETIME:
			try{
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				ret = formatter.parse(proposedValue);
				return;
			}
			catch(Exception e){
				throw new Exception(proposedValue + " can't be converted to" + this.name() + ". Accepted format is yyyy-MM-dd'T'HH:mm:ss." );
			}
		case TIME:
			try{
				SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
				ret = formatter.parse(proposedValue);
				return;
			}
			catch(Exception e){
				throw new Exception(proposedValue + " can't be converted to" + this.name() + ". Accepted format is HH:mm:ss." );
			}
		case NODE_URI:
			if (SparqlToXUtils.isLegalURI(proposedValue)) {
				ret = proposedValue;
				return;
			}
			// let these types slip through unchecked
		case DURATION:
		case GYEARMONTH:
		case GMONTH:
		case GMONTHDAY:
		case ANYSIMPLETYPE:
		case UNSIGNEDBYTE:
		default:
			ret = proposedValue;
			return;
		}
	
		throw new Exception(proposedValue + " can't be converted to" + this.name());
		
	}

}




