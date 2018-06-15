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
	
	public String getXsdSparqlTrailer() throws Exception{
		
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

}




