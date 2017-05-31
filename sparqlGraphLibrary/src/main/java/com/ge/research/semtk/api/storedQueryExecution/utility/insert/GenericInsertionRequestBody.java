package com.ge.research.semtk.api.storedQueryExecution.utility.insert;
import java.lang.reflect.Field;

public class GenericInsertionRequestBody {
	/*
	 * this class can be extended to allow the ingestion of new data to the semantic store from a pretty 
	 * generic request. for now, it is assumed that no arrays or collections are included in the parameters
	 * being passed to the service. 
	 */
	public String getValueByName(String expectedName) throws Exception{
		String retval = "";
		
		// get the field information
		Class<?> thisClass = this.getClass();
		Field requested = thisClass.getField(expectedName);
		retval = requested.get(this).toString(); 
		
		return retval; // return the value itself
	}
}
