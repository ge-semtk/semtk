/**
 ** Copyright 2022 General Electric Company
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

package com.ge.research.semtk.services.nodeGroupExecution.requests;
import java.util.ArrayList;

import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import io.swagger.v3.oas.annotations.media.Schema;


public class CombineEntitiesRequest extends SparqlConnectionRequest {
	@Schema(
            required = true,
            example = "http://myprefix#MyClass")
    private String classUri;
	
	@Schema(
            required = true,
            example = "http://myprefix#uri_123")
    private String targetUri;
	
	@Schema(
            required = true,
            example = "http://myprefix#uri_124")
    private String duplicateUri;
	
	@Schema(
			description = "list of predicates to delete from duplicate before merge",
			required = false,
			example = "[[\"http:/namespace#class1\", \"http:/namespace#predicate\"]]")
	private ArrayList<String> deletePredicatesFromDuplicate = null;
	
	@Schema(
			description = "list of predicates to delete from target before merge",
			required = false,
			example = "[[\"http:/namespace#class1\", \"http:/namespace#predicate\"]]")
	private ArrayList<String> deletePredicatesFromTarget = null;

	public String getClassUri() {
		return classUri;
	}
	/**
	 * 
	 * @return list, possibly empty
	 */
	public ArrayList<String> getDeletePredicatesFromDuplicate() {
		return deletePredicatesFromDuplicate;
	}
	
	/**
	 * 
	 * @return list, possibly empty
	 */
	public ArrayList<String> getDeletePredicatesFromTarget() {
		return deletePredicatesFromTarget;
	}


	public String getTargetUri() {
		return targetUri;
	}


	public String getDuplicateUri() {
		return duplicateUri;
	}
    
 

}
