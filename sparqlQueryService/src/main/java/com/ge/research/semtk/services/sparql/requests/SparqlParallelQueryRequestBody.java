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


package com.ge.research.semtk.services.sparql.requests;

import org.json.simple.parser.JSONParser;

/**
 * For service calls to the parallel query endpoint.
 */
public class SparqlParallelQueryRequestBody {
	
	public String subqueriesJson;		// JSON containing triple store connection and subqueries
	public String subqueryType;			// e.g. "select"
	public boolean isSubqueryOptional;	// true to return records from one subquery result that do not match another subquery result
	public String columnsToFuseOn;		// columns to merge subquery results, comma-separated
	public String columnsToReturn;		// columns to return from the overall query, comma-separated

    
    /**
     * Validate request contents.  Throws an exception if validation fails.
     */
    public void validate() throws Exception{
		if(subqueriesJson == null || subqueriesJson.trim().isEmpty()){
			throw new Exception("No subqueries specified");
		}
		try{
			(new JSONParser()).parse(subqueriesJson);
		}catch(Exception e){
			throw new Exception("Cannot parse subqueries json:\n" + e);
		}
		if(subqueryType == null || subqueryType.trim().isEmpty() || !subqueryType.equals("select")){
			throw new Exception("Subquery type must be \"select\" - other types not currently supported");
		}		
		if(columnsToFuseOn == null || columnsToFuseOn.trim().isEmpty()){
			throw new Exception("Columns to fuse on are not specified");
		}		
		if(columnsToReturn == null || columnsToReturn.trim().isEmpty()){
			throw new Exception("Columns to return are not specified");
		}	
    }
    

    
}
