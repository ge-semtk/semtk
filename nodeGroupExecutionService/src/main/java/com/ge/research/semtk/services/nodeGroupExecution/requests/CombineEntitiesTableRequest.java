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
import java.util.Hashtable;

import org.json.simple.JSONObject;

import com.ge.research.semtk.ontologyTools.CombineEntitiesInputTable;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import io.swagger.v3.oas.annotations.media.Schema;


public class CombineEntitiesTableRequest extends SparqlConnectionRequest {

	
	@Schema(
            required = true,
            example = "one,two\na,b\nA,B\n")
    private String csvString;
	
	@Schema(
			description = "list of column names used to lookup target entity",
			required = false,
			example = "[[\"target_name\", \"target_type\"]]")
	private ArrayList<String> targetColNames = null;
	
	@Schema(
			description = "list of properties for target columns",
			required = false,
			example = "[[\"uri://namespace#name\", \"#type\"]]")
	private ArrayList<String> targetColProperties = null;
	
	@Schema(
			description = "list of column names used to lookup duplicate entity",
			required = false,
			example = "[[\"duplicate_name\", \"duplicate_type\"]]")
	private ArrayList<String> duplicateColNames = null;
	
	@Schema(
			description = "list of properties for duplicate columns",
			required = false,
			example = "[[\"uri://namespace#name\", \"#type\"]]")
	private ArrayList<String> duplicateColProperties = null;
	
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


	public String getCsvString() {
		return csvString;
	}

	public ArrayList<String> getTargetColNames() {
		return targetColNames;
	}

	public ArrayList<String> getTargetColProperties() {
		return targetColProperties;
	}

	public ArrayList<String> getDuplicateColNames() {
		return duplicateColNames;
	}

	public ArrayList<String> getDuplicateColProperties() {
		return duplicateColProperties;
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

	private Hashtable<String,String> buildTargetColumnHash() {
		Hashtable<String,String> ret = new Hashtable<String,String>();
		for (int i=0; i < this.targetColNames.size(); i++) {
			ret.put(this.targetColNames.get(i), this.targetColProperties.get(i));
		}
		return ret;
	}
	
	private Hashtable<String,String> buildDuplicateColumnHash() {
		Hashtable<String,String> ret = new Hashtable<String,String>();
		for (int i=0; i < this.duplicateColNames.size(); i++) {
			ret.put(this.duplicateColNames.get(i), this.duplicateColProperties.get(i));
		}
		return ret;
	}

	public CombineEntitiesInputTable buildTable() throws Exception {
		return new CombineEntitiesInputTable(this.buildTargetColumnHash(), this.buildDuplicateColumnHash(), Table.fromCsvData(this.csvString));
	}
	

}
