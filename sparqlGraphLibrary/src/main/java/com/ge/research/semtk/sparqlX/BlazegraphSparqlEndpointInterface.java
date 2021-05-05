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


package com.ge.research.semtk.sparqlX;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.Utility;

/**
 * Interface to Fuseki SPARQL endpoint
 */
public class BlazegraphSparqlEndpointInterface extends SparqlEndpointInterface {
	
	public BlazegraphSparqlEndpointInterface(String server, String graph)	throws Exception {
		super(server, graph);
		if (this.endpoint == null) {
			throw new Exception("Blazegraph URL is missing the namespace: " + server);
		}
	}

	public BlazegraphSparqlEndpointInterface(String server, String graph, String user, String pass)	throws Exception {
		super(server, graph, user, pass);
		if (this.endpoint == null) {
			throw new Exception("Blazegraph URL is missing the namespace: " + server);
		}
	}

	public int getInsertQueryMaxSize()    { return 50000; }
	public int getInsertQueryOptimalSize() { return 5000; }
	
	/**
	 * Fuseki uses different param names for "auth" queries, which Fuseki calls "update"
	 */
	@Override
	protected void addParams(HttpPost httppost, String query, SparqlResultTypes resultType) throws Exception {
		// add params
		List<NameValuePair> params = new ArrayList<NameValuePair>(3);
		
		// if (this.password == null) {
		if (resultType == SparqlResultTypes.TABLE || resultType == SparqlResultTypes.GRAPH_JSONLD) { 
			params.add(new BasicNameValuePair("query", query));
		} else {
			params.add(new BasicNameValuePair("update", query));
		}

		// set entity
		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
	}
	
	/**
	 * Build a GET URL
	 */
	public String getGetURL(){
		return String.format("%s:%s/%s?query=", this.server, this.port, this.endpoint);
	}


	/**
	 * build the HttpPost with headers
	 * @param resultsFormat
	 * @return
	 */
	protected void addHeaders(HttpPost httppost, SparqlResultTypes resultType) throws Exception {
		
		httppost.addHeader("Accept", this.getContentType(resultType));
		httppost.addHeader("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
		// not recognized by blazegraph: httppost.addHeader("X-Sparql-default-graph", this.graph);

	}
	
	@Override
	public boolean isExceptionRetryAble(Exception e) {
		String msg = e.getMessage();
		
		if (msg.contains("Non-JSON response")) {
			return false;
		} else {
			return super.isExceptionRetryAble(e);
		}
				
	}
	/**
	 * Build a POST URL
	 */
	public String getPostURL(SparqlResultTypes resultType) {
		if (resultType == SparqlResultTypes.TABLE || resultType == SparqlResultTypes.GRAPH_JSONLD) { 
			return String.format("%s:%s/%s/sparql", this.server, this.port, this.endpoint);	
		} else{
			// context-uri trick is from
			// https://github.com/blazegraph/database/issues/158
			return String.format("%s:%s/%s/update?context-uri=%s", this.server, this.port, this.endpoint, this.graph);	

		}
	}	
	@Override 
	public JSONObject executeUploadTurtle(byte [] turtle) throws AuthorizationException, Exception {
		return this.executeUpload(turtle, "file.ttl");
	}
	
	@Override 
	public JSONObject executeAuthUploadTurtle(byte [] turtle) throws AuthorizationException, Exception {
		return this.executeUpload(turtle, "file.ttl");
	}
	
	/** 
	 * Default Upload of OWL
	 */
	@Override
	public JSONObject executeUpload(byte[] owl) throws AuthorizationException, Exception {
		return this.executeUpload(owl, "file.owl");
	}
	
	public JSONObject executeUpload(byte[] owl, String filename) throws AuthorizationException, Exception {
		this.authorizeUpload();
		
		HttpHost targetHost = this.buildHttpHost();
        HttpClient httpclient = this.buildHttpClient(targetHost.getSchemeName());
		BasicHttpContext localcontext = this.buildHttpContext(targetHost);
		HttpPost httppost = new HttpPost(this.getUploadURL());
		
		// NOTE: doing this instead of addHeaders because resultType isn't enough info to determine Content-type
		httppost.addHeader("Accept", "*/*");
		if (filename.toLowerCase().endsWith(".ttl")) {
			httppost.addHeader("Content-type", "text/turtle");
		} else {
			httppost.addHeader("Content-type", "application/rdf+xml");
		}

		 
		// NOTE: blazegraph technique is not using multi-part, just straight body
		HttpEntity entity = new ByteArrayEntity(owl);
		httppost.setEntity(entity);
		
		executeTestQuery();

		HttpResponse response_http = httpclient.execute(targetHost, httppost, localcontext);
		HttpEntity resp_entity = response_http.getEntity();
		// get response with HTML tags removed
		String responseTxt = EntityUtils.toString(resp_entity, "UTF-8");

		SimpleResultSet ret = new SimpleResultSet();
		
		if(responseTxt.contains("<data modified=") &&
		   !responseTxt.contains("<data modified=\"0\"") &&
		   !responseTxt.contains("xception")) {
			ret.setSuccess(true);
		} else {
			ret.setSuccess(false);
			ret.addRationaleMessage("BlazegraphEndpointInterface.executeUpload", responseTxt);
		}
		resp_entity.getContent().close();
		return ret.toJson();
	}
	
	/**
	 * Handle an empty response
	 * @throws Exception 
	 */
	@Override
	public JSONObject handleEmptyResponse(SparqlResultTypes resultType) throws Exception {
		throw new Exception("Blazegraph query returned empty response");
	}
	
	@Override
	public JSONObject handleNonJSONResponse(String responseTxt, SparqlResultTypes resulttype) throws DontRetryException, Exception {
		
		// Fuseki seems to only return html when successful
		// SemTK needs either SimpleResults or TableResults style JSON.
		if (responseTxt.toLowerCase().contains("<html>")) {
			
			if (resulttype == SparqlResultTypes.CONFIRM) {
				// change CONFIRM SimpleResultSet @message
				SimpleResultSet ret = new SimpleResultSet(true);
				ret.setMessage(Utility.htmlToPlain(responseTxt));
				return ret.getResultsJSON();
				
			} else if (resulttype == SparqlResultTypes.TABLE) {
				// change TABLE to a tableResultSet
				Table table = new Table(new String[] {"Response"}, new String[] {"string"});
				table.addRow(new String[] {Utility.htmlToPlain(responseTxt)});
				TableResultSet ret = new TableResultSet(true);
				ret.addResults(table);
				return ret.getResultsJSON();
				
			} else {
				throw new Exception("Non-JSON non-HTML reponse: " + responseTxt);
			}
			
		} else {
			if (responseTxt.contains("Error 400")) {
				throw new DontRetryException(responseTxt);
			} else if (responseTxt.contains("Error 404")) {
				throw new DontRetryException(responseTxt + " server=" + this.getServerAndPort());
			}
			throw new Exception("Non-JSON response: " + responseTxt);
		}
	}

	@Override
	public String getUploadURL() throws Exception {
		return String.format("%s:%s/%s/update?context-uri=%s", this.server, this.port, this.endpoint, this.graph);	
	}

	@Override
	public String getServerType() {
		return BLAZEGRAPH_SERVER;
	}
	
	@Override
	public SparqlEndpointInterface copy() throws Exception {
		BlazegraphSparqlEndpointInterface retval = null;
		
		retval = new BlazegraphSparqlEndpointInterface(this.getServerAndPort(), this.graph, this.userName, this.password);
		retval.copyRest(this);
		return (SparqlEndpointInterface) retval;
	}
	
}
