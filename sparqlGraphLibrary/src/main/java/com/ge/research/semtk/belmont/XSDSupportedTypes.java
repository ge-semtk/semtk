package com.ge.research.semtk.belmont;

public enum XSDSupportedTypes {

	STRING , BOOLEAN , DECIMAL , INT , INTEGER , NEGATIVEINTEGER , NONNEGATIVEINTEGER , 
	POSITIVEINTEGER, NONPOSISITIVEINTEGER , LONG , FLOAT , DOUBLE , DURATION , 
	DATETIME , TIME , DATE , UNSIGNEDBYTE , UNSIGNEDINT , ANYSIMPLETYPE ,
	GYEARMONTH , GMONTH , GMONTHDAY, NODE_URI;
	
	// note: "node_uri" was added for compatibility reasons to the way nodes in a nodegroup spec
	// when their URI is able to be constrained at runtime.
}
