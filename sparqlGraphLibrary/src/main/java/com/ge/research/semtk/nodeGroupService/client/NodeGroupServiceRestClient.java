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

package com.ge.research.semtk.nodeGroupService.client;

import java.util.ArrayList;

import com.ge.research.semtk.belmont.runtimeConstraints.SupportedOperations;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.nodeGroupService.SparqlIdReturnedTuple;
import com.ge.research.semtk.nodeGroupService.SparqlIdTuple;

/**
 *  It seems to make no sense to call these functions.
 *  Since caller has sparqlGraphLibrary, 
 *  they can achieve all the same functionality with java and no REST calls.
 *  
 *  Newer endpoints have functions here simply for unit testing.
 */
public class NodeGroupServiceRestClient extends RestClient {

	
	@Override
	public void buildParametersJSON() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEmptyResponse() throws Exception {
		// TODO Auto-generated method stub
		
	}

	public NodeGroupServiceRestClient (NodeGroupServiceConfig config) {
		this.conf = config;
	}
	
	/**
	 * 
	 * @param sgJson
	 * @param tuples
	 * @return
	 * @throws Exception on failure
	 */
	public SparqlGraphJson execChangeSparqlIds(SparqlGraphJson sgJson, ArrayList<SparqlIdTuple> tuples) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint("nodeGroup/changeSparqlIds");
		JSONArray tuplesJson = new JSONArray();
		for (SparqlIdTuple tuple : tuples) {
			tuplesJson.add(tuple.toJson());
		}
		this.parametersJSON.put("jsonRenderedNodeGroup", sgJson.toJson().toJSONString());
		this.parametersJSON.put("sparqlIdTuples", tuplesJson);
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute());
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("jsonRenderedNodeGroup");
			this.parametersJSON.remove("sparqlIdTuples");
		}

		return new SparqlGraphJson(retval.getResultJSON("nodegroup"));
	}
	
	public String[] getIngestionColumns(SparqlGraphJson sgJson) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint("nodeGroup/getIngestionColumns");
		
		this.parametersJSON.put("jsonRenderedNodeGroup", sgJson.toJson().toJSONString());
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute());
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("jsonRenderedNodeGroup");
		}

		return retval.getResultStringArray("columnNames");
	}
	
	public String getSampleIngestionCSV(SparqlGraphJson sgJson) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint("nodeGroup/getSampleIngestionCSV");
		
		this.parametersJSON.put("jsonRenderedNodeGroup", sgJson.toJson().toJSONString());
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute());
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("jsonRenderedNodeGroup");
		}

		return retval.getResult("sampleCSV");
	}

	public JSONObject buldRuntimeConstraintJSON(String sparqlID, SupportedOperations operation, ArrayList<String> operandList) throws Exception{
		SimpleResultSet retval = null;

		conf.setServiceEndpoint("nodeGroup/buildRuntimeConstraintJSON");

		this.parametersJSON.put("sparqlID", sparqlID);
		this.parametersJSON.put("operation", operation.name());
		this.parametersJSON.put("operandList", operandList);


		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute());
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("sparqlID");
			this.parametersJSON.remove("operation");
			this.parametersJSON.remove("operandList");
		}

		return retval.getResultJSON("constraintJSON");
	}


	public String[] execGetReturnedSparqlIds(SparqlGraphJson sgJson) throws Exception{
		TableResultSet retval = null;
		
		conf.setServiceEndpoint("nodeGroup/getReturnedSparqlIds");
		
		this.parametersJSON.put("jsonRenderedNodeGroup", sgJson.toJson().toJSONString());
		
		try{
			retval = new TableResultSet((JSONObject) this.execute());
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("jsonRenderedNodeGroup");
		}

		return retval.getTable().getColumn(0);
	}
	
	public Table execGetRuntimeConstraints(SparqlGraphJson sgJson) throws Exception{
		TableResultSet retval = null;
		
		conf.setServiceEndpoint("nodeGroup/getRuntimeConstraints");
		
		this.parametersJSON.put("jsonRenderedNodeGroup", sgJson.toJson().toJSONString());
		
		try{
			retval = new TableResultSet((JSONObject) this.execute());
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("jsonRenderedNodeGroup");
		}

		return retval.getTable();
	}
	
	public SparqlGraphJson execSetIsReturned(SparqlGraphJson sgJson, ArrayList<SparqlIdReturnedTuple> tuples) throws Exception{
		SimpleResultSet retval = null;
		
		conf.setServiceEndpoint("nodeGroup/setIsReturned");
		JSONArray tuplesJson = new JSONArray();
		for (SparqlIdReturnedTuple tuple : tuples) {
			tuplesJson.add(tuple.toJson());
		}
		this.parametersJSON.put("jsonRenderedNodeGroup", sgJson.toJson().toJSONString());
		this.parametersJSON.put("sparqlIdReturnedTuples", tuplesJson);
		
		try{
			retval = SimpleResultSet.fromJson((JSONObject) this.execute());
			retval.throwExceptionIfUnsuccessful();
		}
		finally{
			// reset conf and parametersJSON
			conf.setServiceEndpoint(null);
			this.parametersJSON.remove("jsonRenderedNodeGroup");	
			this.parametersJSON.remove("sparqlIdReturnedTuples");
		}

		return new SparqlGraphJson(retval.getResultJSON("nodegroup"));
	}
}
