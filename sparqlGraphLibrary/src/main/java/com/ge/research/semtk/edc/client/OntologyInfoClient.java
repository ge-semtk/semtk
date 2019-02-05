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
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.sparqlX.SparqlConnection;

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
	
	public void uncacheChangedModel(SparqlConnection conn) throws ConnectException, EndpointNotFoundException, Exception {
		this.parametersJSON.put("jsonRenderedSparqlConnection", conn.toJson().toJSONString());
		conf.setServiceEndpoint("ontologyinfo/uncacheChangedModel");
		
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
