/**
 ** Copyright 2016-2020 General Electric Company
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
package com.ge.research.semtk.querygen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.dispatch.QueryFlags;

/**
 * Generates queries to be executed on an external data source.
 */
public abstract class QueryGenerator {
	
	protected QueryFlags flags = null;						// flags to use for query generation
	
	/**
	 * Gets a map of UUID to an object.
	 * The object contains a set of queries, along with any configuration that may be needed by the executor.
	 */
	public abstract HashMap<UUID, Object> getQueries() throws Exception;
	
	/**
	 * Get the configuration needed for the executor to run the query
	 */
	protected abstract JSONObject getConfigForUUIDEntry(Table DataForOneConfig) throws Exception;
	
	/**
	 * Return true if a given flag is set, else false.
	 */
	public boolean isFlagSet(String flag){
		return flags != null && flags.isSet(flag);
	}
	
	/**
	 * Parse flags
	 */
	protected void parseFlags(JSONArray flagsJsonArray) throws Exception{
		this.flags = new QueryFlags(flagsJsonArray);
	}
}
