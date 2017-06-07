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
