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

import com.ge.research.semtk.ontologyTools.CombineEntitiesInConnThread;
import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import io.swagger.v3.oas.annotations.media.Schema;


public class CombineEntitiesInConnRequest extends SparqlConnectionRequest {

	
	@Schema(
			description = "Override URI of class that connects entities to be combined",
			required = false,
			example = "http://research.ge.com/semtk/EntityResolution#SameAs")
	private String sameAsClassURI = CombineEntitiesInConnThread.SAME_AS_CLASS_URI;
	
	@Schema(
			description = "Override URI of property connecting sameAsClass instance to target entity to be combined",
			required = false,
			example = "http://research.ge.com/semtk/EntityResolution#target")
	private String targetPropURI = CombineEntitiesInConnThread.TARGET_PROP_URI;
	
	@Schema(
			description = "Override URI of property connecting sameAsClass instance to duplicate entity to be combined",
			required = false,
			example = "http://research.ge.com/semtk/EntityResolution#duplicate")
	private String duplicatePropURI = CombineEntitiesInConnThread.DUPLICATE_PROP_URI;
	
	@Schema(
			description = "list of predicates to delete from duplicate instance before merge",
			required = false,
			example = "[[\"http:/namespace#class1\", \"http:/namespace#predicate\"]]")
	private ArrayList<String> deletePredicatesFromDuplicate = null;
	
	@Schema(
			description = "list of predicates to delete from target instance before merge",
			required = false,
			example = "[[\"http:/namespace#class1\", \"http:/namespace#predicate\"]]")
	private ArrayList<String> deletePredicatesFromTarget = null;


	public String getSameAsClassURI() {
		return sameAsClassURI;
	}
	
	public String getTargetPropURI() {
		return targetPropURI;
	}
	
	public String getDuplicatePropURI() {
		return duplicatePropURI;
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

	
	

}
