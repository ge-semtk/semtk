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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.utility.Utility;

/**
 * Interface to MarkLogic SPARQL endpoint
 */
public class MarkLogicSparqlEndpointInterface extends SparqlEndpointInterface {

	protected String database = null;
	private final String MSG_CLEAR_NONEXIST_GRAPH = "No such RDF";
	/**
	 * 
	 * @param server  Optionally includes database.  e.g. http://localhost:8001/MY_DATABASE
	 * @param graph
	 * @throws Exception
	 */
	public MarkLogicSparqlEndpointInterface(String server, String graph) throws Exception {
		super(server, graph, "test", "password");
		
	}

	public MarkLogicSparqlEndpointInterface(String server, String graph, String user, String pass)	throws Exception {
		super(server, graph, user!=null?user:"test", pass!=null?pass:"password");

	}
	// For MarkLogic, we're storing the database in "endpoint"
	private String getDatabase() { return this.endpoint; }
	
	public int getInsertQueryMaxSize()    { return 50000; }
	public int getInsertQueryOptimalSize() { return 5000; }
	public String getLocalDefaultGraphName() { return  "http://marklogic.com/semantics#default-graph"; }
	
	/* Timeout is not implemented.  Should be "timeout" REST param */
	public String getTimeoutSparqlPrefix() { return null; }    
	public String getTimeoutSparqlClause() { return null; } 
	public String getTimeoutPostParamName() { return "timeout"; }    
	public String getTimeoutPostParamValue() { return this.sparqlTimeout == 0 ? null : String.valueOf(this.sparqlTimeout); } 
	
	
	@Override
	protected void addParams(HttpPost httppost, String query, SparqlResultTypes resultType) throws Exception {

		List<NameValuePair> params = new ArrayList<NameValuePair>(3);
		
		if (resultType == SparqlResultTypes.TABLE || resultType == SparqlResultTypes.GRAPH_JSONLD || resultType == SparqlResultTypes.RDF || resultType == SparqlResultTypes.N_TRIPLES) { 
			params.add(new BasicNameValuePair("query", query));
		} else {
			params.add(new BasicNameValuePair("update", query));
		}
		
		if (this.getDatabase() != null) {
			params.add(new BasicNameValuePair("database", this.getDatabase()));
		}

		// timeout TODO : does not seem possible in MarkLogic
		//if (this.getTimeoutPostParamName() != null && this.getTimeoutPostParamValue() != null) {
		//	LocalLogger.logToStdErr("timeout header: " + this.getTimeoutPostParamName() + "=" + this.getTimeoutPostParamValue());
		//	params.add(new BasicNameValuePair(this.getTimeoutPostParamName(), this.getTimeoutPostParamValue()));
		//}
		
		// set entity
		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
	}
	

	/**
	 * Build a GET URL
	 */
	public String getGetURL(){
		return String.format("%s:%s/v1/graphs/sparql?query=", this.server, this.port);
	}

	/**
	 * Build a POST URL
	 */
	public String getPostURL(SparqlResultTypes resultType) {
		return String.format("%s:%s/v1/graphs/sparql", this.server, this.port);
	}
	
	@Override 
	public JSONObject executeUploadTurtle(byte[] turtle) throws AuthorizationException, Exception {
		return this.executeUpload(turtle, "file.ttl");
	}
	
	@Override 
	/** Main entry point from query service **/
	public JSONObject executeAuthUploadTurtle(byte[] turtle) throws AuthorizationException, Exception {
		return this.executeUpload(turtle, "file.ttl");
	}
	
	@Override 
	/** Main entry point from query service **/
	public JSONObject executeAuthUploadOwl(byte[] turtle) throws AuthorizationException, Exception {
		return this.executeUpload(turtle, "file.owl");
	}
	
	@Override
	/** Default upload of OWL.  Errors if bytes are not OWL **/
	public JSONObject executeUpload(byte[] owl) throws AuthorizationException, Exception {
		return this.executeUpload(owl, "file.owl");
	}
	
	private JSONObject executeUpload(byte[] owl, String filename) throws AuthorizationException, Exception {
		EntityBuilder builder = EntityBuilder.create();

		builder.setBinary(owl);
		HttpEntity entity = builder.build();

		return this.executeAuthUpload(entity, filename);
	}
	
	@Override
	public JSONObject executeAuthUploadStreamed(InputStream is, String filename) throws AuthorizationException, Exception {
		EntityBuilder builder = EntityBuilder.create();

		builder.setStream(is);
		HttpEntity entity = builder.build();
		return this.executeAuthUpload(new BufferedHttpEntity(entity), filename);
	}
	
	/**
	 * Main function for auth uploading
	 */
	private JSONObject executeAuthUpload(HttpEntity entity, String filename) throws AuthorizationException, Exception {
		this.authorizeUpload();
		
		HttpHost targetHost = this.buildHttpHost();
        HttpClient httpclient = this.buildHttpClient(targetHost.getSchemeName());
		BasicHttpContext localcontext = this.buildHttpContext(targetHost);
		
		// PARAMS: graph param is in URL
		HttpPost httppost = new HttpPost(this.getUploadURL());
		
		// HEADERS: content - type
		if (filename.toLowerCase().endsWith(".owl"))
			httppost.addHeader("content-type", CONTENTTYPE_RDF);
		else if (filename.toLowerCase().endsWith(".ttl"))
			httppost.addHeader("content-type", CONTENTTYPE_TURTLE);
		else if (filename.toLowerCase().endsWith(".nt"))
			httppost.addHeader("content-type", CONTENTTYPE_N_TRIPLES);

		// TODO else see what happens when there is no header.  Can MarkLogic figure it out?
		// TODO how do you specifiy the database
		httppost.setEntity(entity);
		
		executeTestQuery();

		HttpResponse response_http = httpclient.execute(targetHost, httppost, localcontext);

		String responseTxt = this.getResponseText(response_http);  
		SimpleResultSet ret = new SimpleResultSet();
		
		if(responseTxt.isEmpty()) {
			// marklogic returned nothing
			ret.setSuccess(true);
		} else if (responseTxt.matches("^/triplestore.*xml$")) {
			// marklogic returned an xml file name -ish thing
			ret.setMessage(responseTxt);
			ret.setSuccess(true);
		} else {
			ret.setSuccess(false);
			ret.addRationaleMessage("MarkLogicEndpointInterface.executeUpload", responseTxt);
		}
		
		return ret.toJson();
	}
	
	/**
	 * Return a context with digest Auth if there is a userName, otherwise null context
	 * @param targetHost
	 * @return
	 */
	protected BasicHttpContext buildHttpContext(HttpHost targetHost) {
		if (this.isAuth()) {
			// using basic auth
			// handled in buildHttpClient
			// TODO: this Digest auth code doesn't work
			// TODO: how to switch auth types
			return null;
//			DigestScheme digestAuth = new DigestScheme();
//			AuthCache authCache = new BasicAuthCache();
//			digestAuth.overrideParamter("realm", "SPARQL");
//			// Suppose we already know the expected nonce value
//			digestAuth.overrideParamter("nonce", "whatever");
//			authCache.put(targetHost, digestAuth);
//			BasicHttpContext ret = new BasicHttpContext();
//			ret.setAttribute(HttpClientContext.AUTH_CACHE, authCache);
//			return ret;
		} else {
			return null;
		}
	}
	
	/**
	 * Override since MarkLogic returns null entity when query has no response.
	 */
	@Override
	protected String getResponseText(HttpResponse response) throws Exception {
		HttpEntity entity = response.getEntity();
		if (entity == null) {
			return "";
		} else {
			return EntityUtils.toString(response.getEntity());
		}
	}
	
	@Override
	protected JSONObject parseResponse(SparqlResultTypes resultType, String responseTxt) throws Exception {
		if (responseTxt.contains("errorResponse")) {
			// MarkLogic errors are inside JSON errorResponse
			JSONObject responseObj;
			try {
				responseObj = (JSONObject) new JSONParser().parse(responseTxt);
			} catch(Exception e) {
				return this.handleNonJSONResponse(responseTxt, resultType);
			}
			
			// parse the errorResponse
			String abbrevResponse = responseTxt.length() > 1000 ? responseTxt.substring(0,1000): responseTxt;
			JSONObject errResponse = (JSONObject) responseObj.get("errorResponse");
			if (errResponse == null) 
				throw new DontRetryException("Error response contained no 'errorResponse': " + abbrevResponse);

			String message = (String) errResponse.get("message");
			if (message == null) 
				throw new DontRetryException("Error response contained no 'message': " + abbrevResponse);

			Long statusCode = (Long) errResponse.get("statusCode");
			if (statusCode == null) 
				throw new DontRetryException("Error response contained no 'statusCode': " + abbrevResponse);
	
			// handle the parsed values
			if (message.contains(MSG_CLEAR_NONEXIST_GRAPH)) {
				throw new DontRetryException(message.substring(message.lastIndexOf("No such")));
			} else {
				throw new DontRetryException("MarkLogic returned error response: " + abbrevResponse);
			}
			
		}
		return super.parseResponse(resultType, responseTxt);
	}
	
	/**
	 * build the HttpPost with headers
	 * @param resultsFormat
	 * @return
	 */
	@Override
	protected void addHeaders(HttpPost httppost, SparqlResultTypes resultType) throws Exception {
		
		httppost.addHeader("Accept", this.getAccept(resultType));
	}
	@Override
	protected void addHeaders(HttpURLConnection conn, SparqlResultTypes resultType) throws Exception {
		
		conn.setRequestProperty("Accept", this.getAccept(resultType));
	}
	
	/**
	 * Handle an empty response
	 * @throws Exception 
	 */
	@Override
	public JSONObject handleEmptyResponse(SparqlResultTypes resultType) throws Exception {
		if (resultType == SparqlResultTypes.N_TRIPLES) {
			JSONObject ret = new JSONObject();
			ret.put("N_TRIPLES", "");
			return ret;
		} else if (resultType == SparqlResultTypes.CONFIRM) {
			JSONObject ret = new JSONObject();
			// TODO this might need to be more than {}
			return ret;
		} else {
			throw new Exception("MarkLogic query returned empty response");
		}
	}
	
	@Override
	public JSONObject handleNonJSONResponse(String responseTxt, SparqlResultTypes resulttype) throws DontRetryException, Exception {

		// TODO: update to MarkLogic.  Seems most errors are HTML.
		//       Map out retry process.  (Retry requires an Exception)
		//       For HTML, MarkLogic <title> seems redundant and lots of line returns messy in htmlToPlain()
		//
		//       Many errors come back in JSON, so error handling happens in
		//           parseResponse()
		
		// SemTK needs either SimpleResults or TableResults style JSON.
		if (responseTxt.toLowerCase().contains("<html")) {
			String message = responseTxt;
			if (message.contains("<body>")) {
				message = message.substring(message.indexOf("<body>") + 6, message.lastIndexOf("</body>"));
			}
			message = Utility.htmlToPlain(message);
			
			if (message.contains("401"))
				throw new DontRetryException(message);
			else
				// TODO figure out if there are any non error HTML responses
				throw new DontRetryException(message);
			
//			if (resulttype == SparqlResultTypes.CONFIRM) {
//				// change CONFIRM SimpleResultSet @message
//				SimpleResultSet ret = new SimpleResultSet(true);
//				ret.setMessage(message);
//				return ret.getResultsJSON();
//				
//			} else if (resulttype == SparqlResultTypes.TABLE) {
//				// change TABLE to a tableResultSet
//				Table table = new Table(new String[] {"Response"}, new String[] {"string"});
//				table.addRow(new String[] {message});
//				TableResultSet ret = new TableResultSet(true);
//				ret.addResults(table);
//				return ret.getResultsJSON();
//				
//			} else {
//				throw new Exception("MarkLogic unsupported HTML reponse: " + responseTxt);
//			}
			
		} else {

			if (this.sparqlTimeout > 0 && responseTxt.contains("503")) {
				throw new QueryTimeoutException("Timed out after " + String.valueOf(this.sparqlTimeout) + " sec");
			} else if (responseTxt.contains("Error 400")) {
				throw new DontRetryException(responseTxt);
			} else if (responseTxt.contains("Error 404")) {
				throw new DontRetryException(responseTxt + " server=" + this.getServerAndPort());
			} else if (responseTxt.contains("Encountered ")) {
				throw new DontRetryException("SPARQL syntax error: " + responseTxt);
			} else if (responseTxt.contains("Lexical ")) {
				throw new DontRetryException("SPARQL lexical error: " + responseTxt);
			} else if (responseTxt.contains("Parse ")) {
				throw new DontRetryException("SPARQL parse error: " + responseTxt);
			} else if (responseTxt.contains("heap space") || responseTxt.contains("emory")) {
				throw new DontRetryException("Memory troubles: " + responseTxt);
			}
			throw new Exception("MarkLogic unsupported non-JSON response: " + responseTxt);
		}
	}

	@Override
	/**
	 * get upload url with graph name param included
	 */
	public String getUploadURL() throws Exception {
		return String.format("%s:%s/v1/graphs?graph=%s", this.server, this.port, URLEncoder.encode(this.graph,"UTF-8"));	
	}

	@Override
	public String getServerType() {
		return "marklogic";
	}
	
	@Override
	public SparqlEndpointInterface copy() throws Exception {
		MarkLogicSparqlEndpointInterface retval = null;
		retval = new MarkLogicSparqlEndpointInterface(this.getServerAndPort(), this.graph, this.userName, this.password);
		retval.copyRest(this);
		return (SparqlEndpointInterface) retval;
	}
	
	@Override
	public void clearGraph() throws Exception {
		try {
			super.clearGraph();
		} catch (DontRetryException e) {
			// MarkLogic throws an error when you try to clear a non-existent graph
			// Instead, mimic other triplestores' behavior with a silent no-op.
			if (e.getMessage().contains(MSG_CLEAR_NONEXIST_GRAPH)) {
				return;
			} else {
				throw(e);
			}
		}
        
	}
	
	protected void throwExceptionIfClearGraphFailed(SimpleResultSet res) throws Exception {
		// Marklogic handles this fine in parseResponse
	}
}
