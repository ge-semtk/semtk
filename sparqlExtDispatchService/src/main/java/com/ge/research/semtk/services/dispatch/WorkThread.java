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

package com.ge.research.semtk.services.dispatch;

import java.net.ConnectException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.asynchronousQuery.AsynchronousNodeGroupBasedQueryDispatcher;
import com.ge.research.semtk.sparqlX.dispatch.QueryFlags;
import com.ge.research.semtk.utility.LocalLogger;


public class WorkThread extends Thread {
	AsynchronousNodeGroupBasedQueryDispatcher dispatcher;
	JSONObject externalConstraintsJson;
	QueryFlags queryFlags;

	AutoGeneratedQueryTypes myQT;
	SparqlResultTypes myRT;
	String targetObjectSparqlID;
	String rawSparqlQuery;
	HeaderTable headerTable = null;
	
	/**
	 * 
	 * @param dsp
	 * @param constraintJson
	 * @param flags
	 * @param qt - may be null if this thread will only be used for raw SPARQL
	 * @param rt
	 */
	public WorkThread(AsynchronousNodeGroupBasedQueryDispatcher dsp, JSONObject constraintJson, QueryFlags flags, AutoGeneratedQueryTypes qt, SparqlResultTypes rt){
		this.dispatcher = dsp;
		this.myQT = qt;
		this.myRT = rt;
		this.externalConstraintsJson = constraintJson;
		this.queryFlags = flags;
		headerTable = ThreadAuthenticator.getThreadHeaderTable();
	}
	
	public void setTargetObjectSparqlID(String targetObjectSparqlID) throws Exception{
		if(targetObjectSparqlID == null || targetObjectSparqlID == "" || targetObjectSparqlID.isEmpty()){
			throw new Exception("Target object for filter constraint was null or empty.");
		}
		this.targetObjectSparqlID = targetObjectSparqlID;
	}
	
	public void setRawSparqlSquery(String query) throws Exception{
		if(query == null || query == "" || query.isEmpty()){
			throw new Exception("Given SPARQL query was null or empty.");
		}
		this.rawSparqlQuery = query;
	}
	
    public void run() {
    	ThreadAuthenticator.authenticateThisThread(this.headerTable);
		try {
			if(this.rawSparqlQuery != null){
				// a query was passed. use it.
				this.dispatcher.executePlainSparqlQuery(rawSparqlQuery, myRT);
			}
			
			else{
				// query from the node group itself. 
				this.dispatcher.execute(externalConstraintsJson, queryFlags, this.myQT, this.myRT, targetObjectSparqlID);
			}	
		} catch (Exception e) {
			LocalLogger.printStackTrace(e);  // swallow error, since dispatcher set the status to failed
		}
	
    	this.dispatcher = null;
    }


}