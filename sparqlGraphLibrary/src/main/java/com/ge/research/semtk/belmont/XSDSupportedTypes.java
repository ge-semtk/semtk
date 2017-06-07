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

public enum XSDSupportedTypes {

	STRING , BOOLEAN , DECIMAL , INT , INTEGER , NEGATIVEINTEGER , NONNEGATIVEINTEGER , 
	POSITIVEINTEGER, NONPOSISITIVEINTEGER , LONG , FLOAT , DOUBLE , DURATION , 
	DATETIME , TIME , DATE , UNSIGNEDBYTE , UNSIGNEDINT , ANYSIMPLETYPE ,
	GYEARMONTH , GMONTH , GMONTHDAY, NODE_URI;
	
	// note: "node_uri" was added for compatibility reasons to the way nodes in a nodegroup spec
	// when their URI is able to be constrained at runtime.

	public static String getMatchingName(String candidate) throws Exception{
		
		try{
			
		// check the requested type exists in the enumeration.
		XSDSupportedTypes.valueOf(candidate.toUpperCase());
		
		}catch(Exception e){
			// build a complete list of the allowed values.
			String completeList = "";
			int counter = 0;
			for (XSDSupportedTypes curr : XSDSupportedTypes.values()){
				
				if(counter != 0){ completeList += " or "; }
				completeList +=  "( " + curr.name() + " )";
				counter++;
			}
			// tell the user things were wrong.
			throw new Exception("the XSDSupportedTypes enumeration contains no entry matching " + candidate + ". Expected entries are: " + completeList);
		}
		
		return candidate.toUpperCase(); // since this passed the check for existence, we can just hand it back. 
	}


}




