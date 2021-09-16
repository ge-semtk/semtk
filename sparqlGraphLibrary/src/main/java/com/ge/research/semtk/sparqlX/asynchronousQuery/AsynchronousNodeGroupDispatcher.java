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

package com.ge.research.semtk.sparqlX.asynchronousQuery;


import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * @author Justin
 *
 * Open source dispatcher.
 */
public class AsynchronousNodeGroupDispatcher extends AsynchronousNodeGroupBasedQueryDispatcher {
	
	protected static final int MAX_NUMBER_SIMULTANEOUS_QUERIES_PER_USER = 50;  // maybe move this to a configured value?
	

	
	/**
	 * create a new instance of the AsynchronousNodeGroupExecutor.
	 * @param encodedNodeGroup
	 * @throws Exception
	 */
	public AsynchronousNodeGroupDispatcher(String jobId, SparqlGraphJson sgJson, SparqlEndpointInterface jobTrackerSei, ResultsClientConfig resConfig, SparqlEndpointInterface extConfigSei, boolean unusedFlag, OntologyInfoClient oInfoClient, NodeGroupStoreRestClient ngStoreClient) throws Exception{
		super(jobId, sgJson, jobTrackerSei, resConfig, extConfigSei, unusedFlag, oInfoClient, ngStoreClient);
		
	}
	
	/**
	 * return the JobID. the clients will need this.
	 * @return
	 */
	public String getJobId(){
		return this.jobID;
	}
	
	
	@Override
	public String getConstraintType() {
		// not supported in this sub-type.
		return null;
	}

	@Override
	public String[] getConstraintVariableNames() throws Exception {
		// not supported in this sub-type.
		return null;
	}

	/**
	 * Simplest form of dispatcher execute:  get SPARQL and execute it.
	 */
	@Override
	public void execute(Object executionSpecificObject1, Object executionSpecificObject2, DispatcherSupportedQueryTypes qt, SparqlResultTypes rt, String targetSparqlID) {
		
		try{
			String sparqlQuery = this.getSparqlQuery(qt, targetSparqlID);
			this.executePlainSparqlQuery(sparqlQuery, rt);
		}
		catch(Exception e){
			// something went awry. set the job to failure. 
			this.updateStatusToFailed(e.getMessage());
			LocalLogger.printStackTrace(e);
		}
	}

}
