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


package com.ge.research.semtk.edc.client;

import java.net.ConnectException;

import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.PredicateStats;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

public class OntologyInfoClient extends RestClient {

	
	public OntologyInfoClient(OntologyInfoClientConfig config) {
		super ((RestClientConfig) config);
	}
	
	@Override
	public void buildParametersJSON() throws Exception {
		// performed by calls.   Different params for different calls.
	}

	@Override
	public void handleEmptyResponse() throws Exception {
		throw new Exception("Received empty response");
	}
	
	/**
	 * Returns jobId
	 * @throws Exception
	 */
	public String execGetPredicateStats(SparqlConnection conn) throws ConnectException, EndpointNotFoundException, Exception {
		this.parametersJSON.put("conn", conn.toJson().toJSONString());
		conf.setServiceEndpoint("ontologyinfo/getPredicateStats");
		
		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			return res.getJobId();
		} finally {
			// reset conf and parametersJSON
			this.parametersJSON.remove("conn");
			conf.setServiceEndpoint(null);
		}
	}
	
	/**
	 * Gets predicate stats only if they are cached, else null
	 * @throws Exception
	 */
	public PredicateStats execGetCachedPredicateStats(SparqlConnection conn) throws ConnectException, EndpointNotFoundException, Exception {
		this.parametersJSON.put("conn", conn.toJson().toJSONString());
		conf.setServiceEndpoint("ontologyinfo/getCachedPredicateStats");
		
		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			if (res.getResult("cached").equals("none")) {
				return null;
			} else {
				JSONObject statsJson = res.getResultJSON("predicateStats");
				return new PredicateStats(statsJson);
			}
		} finally {
			// reset conf and parametersJSON
			this.parametersJSON.remove("conn");
			conf.setServiceEndpoint(null);
		}
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public JSONObject execGetOntologyInfoJson(SparqlConnection sparqlConn) throws ConnectException, EndpointNotFoundException, Exception {
		this.parametersJSON.put("jsonRenderedSparqlConnection", sparqlConn.toJson().toJSONString());
		conf.setServiceEndpoint("ontologyinfo/getOntologyInfoJson");
		
		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			return res.getResultJSON("ontologyInfo");
		} finally {
			// reset conf and parametersJSON
			this.parametersJSON.remove("jsonRenderedSparqlConnection");
			conf.setServiceEndpoint(null);
		}
	}
	
	/**
	 * Highest level: get oInfo from a SparqlConn
	 * Note that oInfo service has several optimizations including a cache.
	 * @param conn
	 * @return
	 * @throws ConnectException
	 * @throws EndpointNotFoundException
	 * @throws Exception
	 */
	public OntologyInfo getOntologyInfo(SparqlConnection conn) throws ConnectException, EndpointNotFoundException, Exception {
		
		return new OntologyInfo(this.execGetOntologyInfoJson(conn));
	}
	
	/**
	 * Convenience function for only clearing model portion of a connection
	 * @param conn
	 * @throws ConnectException
	 * @throws EndpointNotFoundException
	 * @throws Exception
	 */
	public void uncacheChangedModel(SparqlConnection conn) throws ConnectException, EndpointNotFoundException, Exception {
		SparqlConnection conn2 = SparqlConnection.deepCopy(conn);
		conn2.clearDataInterfaces();
		this.uncacheChangedConn(conn2);
	}
	
	public void uncacheChangedConn(SparqlEndpointInterface sei) throws ConnectException, EndpointNotFoundException, Exception {
		SparqlConnection conn = new SparqlConnection();
		conn.addDataInterface(sei);   // doesn't matter which, Data or Model are treated the same.
		this.uncacheChangedConn(conn);
	}
	/**
	 * Clears PredicateStats and Ontology cache entries that contain any Sei in the connection.
	 * Note that model and data connections are treated equally.
	 * @param conn
	 * @throws ConnectException
	 * @throws EndpointNotFoundException
	 * @throws Exception
	 */
	public void uncacheChangedConn(SparqlConnection conn) throws ConnectException, EndpointNotFoundException, Exception {
		this.parametersJSON.put("jsonRenderedSparqlConnection", conn.toJson().toJSONString());
		conf.setServiceEndpoint("ontologyinfo/uncacheChangedConn");
		
		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			
		} finally {
			// reset conf and parametersJSON
			this.parametersJSON.remove("jsonRenderedSparqlConnection");
			conf.setServiceEndpoint(null);
		}
	}
	
	public void uncacheOntology(SparqlConnection conn) throws ConnectException, EndpointNotFoundException, Exception {
		this.parametersJSON.put("jsonRenderedSparqlConnection", conn.toJson().toJSONString());
		conf.setServiceEndpoint("ontologyinfo/uncacheOntology");
		
		try {
			SimpleResultSet res = this.executeWithSimpleResultReturn();
			res.throwExceptionIfUnsuccessful();
			
		} finally {
			// reset conf and parametersJSON
			this.parametersJSON.remove("jsonRenderedSparqlConnection");
			conf.setServiceEndpoint(null);
		}
	}
	

}
