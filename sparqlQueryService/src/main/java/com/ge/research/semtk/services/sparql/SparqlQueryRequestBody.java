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


package com.ge.research.semtk.services.sparql;

import com.ge.research.semtk.utility.LocalLogger;

/**
 * For service calls needing SPARQL connection information and a query.
 */
public class SparqlQueryRequestBody extends SparqlRequestBody {

    public String query;
    public String resultType;
    
    /**
     * Validate request contents.  Throws an exception if validation fails.
     */
    public void validate() throws Exception{
    	
    	super.validate();  // validate items from superclass 
    	
		if(query == null || query.trim().isEmpty()){
			throw new Exception("No query specified");
		}
				
		if(resultType == null || resultType.trim().isEmpty()){
			throw new Exception("No resultType specified");
		}
    }
	
    /**
     * Print request info to console
     */
    public void printInfo(){
    	super.printInfo();
    	LocalLogger.logToStdOut("Query:\n" + query);
    	LocalLogger.logToStdOut("Result Type: " + resultType);
    }
}
