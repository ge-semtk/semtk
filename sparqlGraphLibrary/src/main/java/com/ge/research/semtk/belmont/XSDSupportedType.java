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

	STRING("string") , 
	BOOLEAN("boolean") , 
	DECIMAL("decimal") , 
	INT("int") , 
	INTEGER("integer") , 
	NEGATIVEINTEGER("negativeInteger") , 
	NONNEGATIVEINTEGER("nonNegativeInteger") , 
	POSITIVEINTEGER("positiveInteger"), 
	NONPOSISITIVEINTEGER("nonPositiveInteger") , 
	LONG("long") , 
	FLOAT("float") , 
	DOUBLE("double") , 
	DURATION("duration") , 
	DATETIME("dateTime") , 
	TIME("time") , 
	DATE("date") , 
	UNSIGNEDBYTE("unsignedByte") , 
	UNSIGNEDINT("unsignedInt") , 
	ANYSIMPLETYPE("anySimpleType") ,
	GYEARMONTH("gYearMonth") , 
	GMONTH("gMonth") , 
	GMONTHDAY("gMonthDay"), 
	NODE_URI("node_uri");
	
	// note: "node_uri" was added for compatibility reasons to the way nodes in a nodegroup spec
	// when their URI is able to be constrained at runtime.
	private static String xmlSchemaRawPrefix = "http://www.w3.org/2001/XMLSchema";
	private static String xmlSchemaPrefix = "^^<" + xmlSchemaRawPrefix + "#";
	private static String xmlSchemaTrailer = ">";
	
	private String camelCaseStr;
	
	XSDSupportedType(String camelCase) {
		this.camelCaseStr = camelCase;
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

	// get a string version of a value with type trailer
	public String buildTypedValueString(String val) {
		
		if (this == XSDSupportedType.NODE_URI) {
			// check for angle brackets first
			if( val.contains("#")){   // the uri given is not prefixed.
				if(val.startsWith("<") && val.endsWith(">")){
					return val;  // already in brackets so probably will not break.
				}
				else {
					return "<" + val + "> ";  // e.g. VALUES ?TimeSeriesTableType { <timeseries:DataScan> } ... no type information needed for URIs
				}
			}
			else {  // the URI is assumed prefixed since it has no # . Assume the user/caller knows what they want and just use it. 
				return val;
			}
			
		} else {   // not a node uri.
			return "'" + val + "'" + this.getXsdSparqlTrailer(); 
		}
	}
	
	public String getFullName(String delim) {
		return xmlSchemaRawPrefix + delim + this.camelCaseStr;
	}
	
	public String getSimpleName() {
		return this.camelCaseStr;
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
		
		return xmlSchemaPrefix + this.camelCaseStr + xmlSchemaTrailer;
		
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
	 * Throw excpetion if proposedValue is not a legal value
	 * @param proposedValue
	 * @throws Exception
	 */
	public Object parse(String proposedValue) throws Exception{
		
		// datetime: 2014-05-23T10:20:13+05:30
		// date    : 2014-05-23
		
		// for each type: return object, throw special exception, or pass through to normal exception
		switch (this) {
		case DATE:
			try{
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
				return formatter.parse(proposedValue);
			}
			catch(Exception e){
				throw new Exception(proposedValue + " can't be converted to" + this.name() + ". Accepted format is yyyy-MM-dd." );
			}
		case STRING:
			return proposedValue;
		case BOOLEAN:
			try {
				return Boolean.parseBoolean(proposedValue);
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
				return i;
			}
		case DECIMAL:	
		case LONG:
			try {
				return Long.parseLong(proposedValue);
			}
			catch(Exception e){
			}
			break;
		case FLOAT:
			try {
				return Float.parseFloat(proposedValue);
			}
			catch(Exception e){
			}
			break;
		case DOUBLE:
			try {
				return Double.parseDouble(proposedValue);
			}
			catch(Exception e){
			}
			break;
		case DATETIME:
			try{
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				return formatter.parse(proposedValue);
			}
			catch(Exception e){
				throw new Exception(proposedValue + " can't be converted to" + this.name() + ". Accepted format is yyyy-MM-dd'T'HH:mm:ss." );
			}
		case TIME:
			try{
				SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
				return formatter.parse(proposedValue);
			}
			catch(Exception e){
				throw new Exception(proposedValue + " can't be converted to" + this.name() + ". Accepted format is HH:mm:ss." );
			}
		case NODE_URI:
			if (SparqlToXUtils.isLegalURI(proposedValue)) {
				return proposedValue;
			}
			// let these types slip through unchecked
		case DURATION:
		case GYEARMONTH:
		case GMONTH:
		case GMONTHDAY:
		case ANYSIMPLETYPE:
		case UNSIGNEDBYTE:
		default:
			return proposedValue;
		}
	
		throw new Exception(proposedValue + " can't be converted to" + this.name());
		
	}

}




