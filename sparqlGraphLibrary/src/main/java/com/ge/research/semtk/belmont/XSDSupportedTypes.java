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




