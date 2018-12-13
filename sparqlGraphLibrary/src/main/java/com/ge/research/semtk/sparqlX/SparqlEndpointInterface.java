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

import java.net.URL;
import java.net.URLStreamHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.cert.X509Certificate;

import org.apache.commons.codec.net.URLCodec;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.NodeGroupResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.sparqlX.FusekiSparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.VirtuosoSparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.client.SparqlQueryAuthClientConfig;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Interface to SPARQL endpoint.
 * This is an abstract class - create a subclass per implementation (Virtuoso, etc)
 * 
 * NOTE: for HTTPS connections, does not validate certificate chain.
 */
public abstract class SparqlEndpointInterface {

	// NOTE: more than one thread cannot safely share a SparqlEndpointInterface.
	// this is because of the state maintained for the results vars and connection 
	// details. doing so may lead to unexpected results

	private final static String QUERY_SERVER = "kdl";
	private final static String FUSEKI_SERVER = "fuseki";
	private final static String VIRTUOSO_SERVER = "virtuoso";
	private final static String NEPTUNE_SERVER = "neptune";
	
	// results types to request
	protected static final String CONTENTTYPE_SPARQL_QUERY_RESULT_JSON = "application/sparql-results+json"; 
	protected static final String CONTENTTYPE_X_JSON_LD = "application/x-json+ld";
	protected static final String CONTENTTYPE_HTML = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
	
	private static final int MAX_QUERY_TRIES = 4;

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static URLStreamHandler handler = null; // set only by unit tests

	private JSONObject response = null;
	private JSONArray resVars = null;
	private JSONArray resBindings = null;
	
	protected String userName = null;
	
	protected String password = null;
	protected String server = null;
	protected String port = null;
	protected String graph = "";


	/**
	 * Constructor
	 * @param serverAndPort e.g. "http://localhost:2420"
	 * @param dataset e.g. "http://research.ge.com/energy/dataset"
	 * @throws Exception
	 */
	public SparqlEndpointInterface(String serverAndPort, String graph) throws Exception {
		this(serverAndPort, graph, null, null);
	}


	/**
	 * Constructor used for authenticated connections... like insert/update/delete/clear
	 */
	public SparqlEndpointInterface(String serverAndPort, String graph, String user, String pass) throws Exception {
		this.graph = graph;		
		this.setUserAndPassword(user, pass);
		
		this.setServerAndPort(serverAndPort);
		
	}
	
	/**
	 * Can this endpoint run auth queries
	 * @return
	 */
	public boolean isAuth() {
		return (this.userName != null && this.userName.length() > 0);
	}

	public String getServerAndPort() {		
		return this.server + ":" + this.port;
	}
	
	public void setServerAndPort(String serverAndPort) throws Exception {
		String[] serverAndPortSplit = serverAndPort.split(":");  // protocol:server:port
		if(serverAndPortSplit.length < 2){
			throw new Exception("Error: poorly formatted serverAndPort (e.g. http://localhost:2420): " + serverAndPort);
		}
		this.server = serverAndPortSplit[0] + ":" + serverAndPortSplit[1]; // e.g. http://localhost
		
		if(serverAndPortSplit.length < 3){
			throw new Exception("Error: no port provided for " + this.server);
		}
		
		String[] portandendpoint = serverAndPortSplit[2].split("/");
		this.port = portandendpoint[0];
	}
	
	public String getServer() {
		return server;
	}

	public int getPort() {
		return (int) Integer.parseInt(port);
	}

	public String getGraph() {
		return this.graph;
	}
	
	// deprecated
	public String getDataset() {
		return this.graph;
	}
	
	public String getUserName() {
		return userName;
	}


	public String getPassword() {
		return password;
	}

	public void setGraph(String graph) {
		this.graph = graph;
	}
	
	// deprecated
	public void setDataset(String graph) {
		this.graph = graph;
	}
	
	public abstract String getServerType();
	
	/**
	 * Build a GET URL (implementation-specific)
	 */
	public abstract String getPostURL();
	
	/**
	 * Build an upload URL (implementation-specific)
	 */
	public abstract String getUploadURL() throws Exception;


	/**
	 * Build a POST URL  (implementation-specific)
	 */
	public abstract String getGetURL();


	/**
	 * Handle an empty response (implementation-specific)
	 */
	public abstract void handleEmptyResponse() throws Exception;
	
	/**
	 * Override in order to separate out DontRetryException from regular ones
	 * and add in anyknown SemTK explanation
	 * @param responseTxt
	 * @throws Exception
	 */
	public void handleNonJSONResponse(String responseTxt) throws DontRetryException, Exception {
		throw new Exception("Cannot parse query result into JSON: " + responseTxt);
	}


	/**
	 * Get a default result type
	 */
	protected static SparqlResultTypes getDefaultResultType() {
		return SparqlResultTypes.TABLE;  // if no result type, assume it's a SELECT
	}

	
	public static void setUrlStreamHandler(URLStreamHandler handler) {
		SparqlEndpointInterface.handler = handler;
	}

	/**
	 * Set the username and password
	 */
	public void setUserAndPassword(String user, String pass){
		// make sure blank userName is null
		this.userName = (user != null && user.isEmpty()) ? null : user;
		
		// make sure if there is a userName, then a blank pass is ""
		this.password = (user != null && pass == null) ? "" : pass;
	}	
	
	/**
	 * Static method to get an instance of this abstract class
	 * 
	 * This one takes a query client config (historical fix-up reasons)
	 */
	public static SparqlEndpointInterface getInstance(RestClientConfig conf) throws Exception {
		// auth queries will have these as well
		if(conf instanceof SparqlQueryAuthClientConfig){	
			return SparqlEndpointInterface.getInstance (
					((SparqlQueryClientConfig)conf).getSparqlServerType(),
					((SparqlQueryClientConfig)conf).getSparqlServerAndPort(),
					((SparqlQueryClientConfig)conf).getSparqlDataset(),
					((SparqlQueryAuthClientConfig)conf).getSparqlServerUser(),
					((SparqlQueryAuthClientConfig)conf).getSparqlServerPassword()
					);			
		} else if (conf instanceof SparqlQueryAuthClientConfig) {
			return SparqlEndpointInterface.getInstance (
					((SparqlQueryClientConfig)conf).getSparqlServerType(),
					((SparqlQueryClientConfig)conf).getSparqlServerAndPort(),
					((SparqlQueryClientConfig)conf).getSparqlDataset()
					);			
		} else {
			throw new Exception("Can't create SparqlEndpointInterface out of RestClient that is not SparqlQueryClientConfig or subclass");
		}
	}
	
	/**
	 * Static method to get an instance of this abstract class
	 */
	public static SparqlEndpointInterface getInstance(String serverTypeString, String server, String graph) throws Exception{
		return getInstance(serverTypeString, server, graph, null, null);
	}	
	
	/**
	 * Static method to get an instance of this abstract class
	 */
	public static SparqlEndpointInterface getInstance(String serverTypeString, String server, String graph, String user, String password) throws Exception{
		if(serverTypeString.equalsIgnoreCase(VIRTUOSO_SERVER)){
			return new VirtuosoSparqlEndpointInterface(server, graph, user, password);				
			
		}else if(serverTypeString.equalsIgnoreCase(FUSEKI_SERVER)){
			return new FusekiSparqlEndpointInterface(server, graph, user, password);				
			
		}else if(serverTypeString.equalsIgnoreCase(NEPTUNE_SERVER)){
			return new NeptuneSparqlEndpointInterface(server, graph, user, password);				
		
		}else{
			throw new Exception("Invalid SPARQL server type : " + serverTypeString);
		}	
	}
	
	/**
	 * Static method to create an endpoint and execute a query
	 */
	public static SparqlEndpointInterface executeQuery(String server, String graph, String serverTypeString, String query, SparqlResultTypes resultType) throws Exception {
		return executeQuery(server, graph, serverTypeString, query, null, null, resultType);
	}
  
	/**
	 * Static method to create an endpoint and execute a query
	 */
	public static SparqlEndpointInterface executeQuery(String server, String graph, String serverTypeString, String query, String user, String pass, SparqlResultTypes resultType) throws Exception {
		try {
			SparqlEndpointInterface endpoint;
			if(user != null && pass != null){
				endpoint = SparqlEndpointInterface.getInstance(serverTypeString, server, graph, user, pass);
			}else{
				endpoint = SparqlEndpointInterface.getInstance(serverTypeString, server, graph);
			}
			endpoint.executeQuery(query, resultType);
			return endpoint;
		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
			throw new Exception("Error executing semantic query:\n\t" + e.getMessage());
		}
	}		
	
	
	/**
	 * Execute query and assemble a GeneralResultSet object.
	 * 
	 * Returns:
	 * 	  TableResultSet for a select query (success or failure)
	 * 	  NodeGroupResultSet for a construct query (success or failure)
	 *    SimpleResultSet for an auth query (success or failure)
	 * 
	 * @throws - exception if not successful
	 */
	public GeneralResultSet executeQueryAndBuildResultSet(String query, SparqlResultTypes resultType) throws Exception {
		
		// construct an empty result object to store the results
		GeneralResultSet resultSet = null;
				
		if(resultType == SparqlResultTypes.GRAPH_JSONLD){ 
			resultSet = new NodeGroupResultSet();  // construct queries produce graphs
		}else if(resultType == SparqlResultTypes.CONFIRM){
			resultSet = new SimpleResultSet();		// auth queries produce messages
		}else if(resultType == SparqlResultTypes.TABLE){	
			resultSet = new TableResultSet(); // non-construct queries produce tables
		}else if (resultType == SparqlResultTypes.HTML) {
			resultSet = new SimpleResultSet();
		}
		
		// execute the query
		JSONObject result = executeQuery(query, resultType); 			
		resultSet.setSuccess(true);
		resultSet.addResultsJSON(result);
		
		return resultSet;
	}	
	
	public Table executeQueryToTable(String query) throws Exception {
		TableResultSet res = (TableResultSet) this.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		res.throwExceptionIfUnsuccessful();
		return res.getTable();
	}
	
	public void executeQueryAndConfirm(String query) throws Exception {
		GeneralResultSet res = this.executeQueryAndBuildResultSet(query, SparqlResultTypes.CONFIRM);
		res.throwExceptionIfUnsuccessful();
	}
	
	/**
	 * Execute a query. Uses http POST.  
	 * Depending on the type of query (select, insert, construct, etc), decides whether to use auth or non-auth.
	 * 
	 * Sample result from a SELECT query:
	 *  
	 * Sample result from a CONSTRUCT query:
	 * 
	 * Sample results from an AUTH query:
	 * {"@message":"Insert into <http://research.ge.com/graph>, 1 (or less) triples -- done"}
	 * 
	 * @param query
	 * @return a JSONObject wrapping the results
	 */
	public JSONObject executeQuery(String query, SparqlResultTypes resultType) throws Exception {

		int tryCount = 0;
		// Keep trying the query until it succeeds or reaches a 
		// maximum number of tries, which is checked for in the 
		// exception catch
		while (true) {
			tryCount++;
			try {
				return executeQueryPost(query, resultType);
				
			} catch (DontRetryException e) {
				LocalLogger.logToStdErr(e.getMessage());
				throw e;
				
			} catch (Exception e) {
				if (! this.isExceptionRetryAble(e)) {
					LocalLogger.logToStdErr(e.getMessage());
					throw e;
				
				} else if (tryCount >= MAX_QUERY_TRIES) {
					LocalLogger.logToStdOut (String.format("SPARQL query failed after %d tries.  Giving up.", tryCount));
					LocalLogger.logToStdErr(e.getMessage());
					throw e;
					
				} else {	
					int sleepSec = 2 * tryCount;
					// if we're overwhelming a server, really throttle
					if (e.getMessage().contains("Address already in use: connect")) {
						sleepSec = 10 * tryCount;
					}
					LocalLogger.logToStdOut (String.format("SPARQL query failed.  Sleeping %d seconds and trying again...", sleepSec));
					LocalLogger.logToStdErr(e.getMessage());
					TimeUnit.SECONDS.sleep(sleepSec); // sleep and try again
				}
			}
		}
	}

	public JSONObject executeTestQuery() throws Exception {
		final String sparql = "select ?Concept where {[] a ?Concept} LIMIT 1";
		try {
			return executeQuery(sparql, SparqlResultTypes.TABLE);
		} catch (Exception e) {
			throw new Exception("Failure executing test query.", e);
		}
	}

	/**
	 * Deprecated in favor of executeQueryPost
	 * Uses Authentication iff this.userName is not null.
	 * @param query
	 * @param resultType
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	public JSONObject executeQueryAuthPost(String query, SparqlResultTypes resultType) throws Exception {
		// deprecated in favor of:
		return this.executeQueryPost(query, resultType);
	}	
	

	/**
	 * Execute a query using POST
	 * Adds Auth elements ONLY IF this.userName != null
	 * @return a JSONObject wrapping the results. in the event the results were tabular, they can be obtained in the JsonArray "@Table". if the results were a graph, use "@Graph" for json-ld
	 * @throws Exception
	 */
	public JSONObject executeQueryPost(String query, SparqlResultTypes resultType) throws Exception{
		AuthorizationManager.authorizeQuery(this, query);
		
        // get client, adding userName/password credentials if any exist
		HttpHost targetHost = this.buildHttpHost();
        CloseableHttpClient httpclient = this.buildHttpClient(targetHost.getSchemeName());
		// get context with digest auth if there is a userName, else null context
		BasicHttpContext localcontext = this.buildHttpContext(targetHost);
     
		// create the HttpPost
		HttpPost httppost = new HttpPost(this.getPostURL());
		this.addHeaders(httppost, resultType);
		this.addParams(httppost, query, resultType);
		
		// parse the response
		HttpEntity entity = null;
		try {
			// execute
			HttpResponse response_http = httpclient.execute(targetHost, httppost, localcontext);
			entity = response_http.getEntity();
			String responseTxt = EntityUtils.toString(entity, "UTF-8");
		
			// parse response
			return this.parseResponse(resultType, responseTxt);
		} finally {
			httpclient.close();
			if (entity != null) {
				entity.getContent().close();
			}
		}
	}
	
	public void clearGraph() throws Exception {
		this.executeQueryAndConfirm(SparqlToXUtils.generateClearGraphSparql(this));
	}
	
	/**
	 * Get an CloseableHttpClient, handling credentials and HTTPS if needed
	 * NOTE: for HTTPS connections, does not validate certificate chain.
	 * @schemeName http or https
	 */
	protected CloseableHttpClient buildHttpClient(String schemeName) throws Exception {
		
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		
		// add userName and password, if any
		if (this.isAuth()) {
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(this.userName, this.password));
	        clientBuilder.setDefaultCredentialsProvider(credsProvider);
		} 
		
		// if https, use SSL context that will not validate certificate
		if(schemeName.equalsIgnoreCase("https")){
			clientBuilder.setSSLContext(getTrustingSSLContext());
		}
		
		return clientBuilder.build();
	}
	
	
	/**
	 * Gets a context with an all-trusting trust manager
	 */
	protected static SSLContext getTrustingSSLContext() throws Exception{
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
        	new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
        };
        // Install the trust manager
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
	}
	
	
	/**
	 * build the host from this.server and this.port
	 * @return
	 */
	protected HttpHost buildHttpHost() {
		String[] serverNoProtocol = this.server.split("://");
		return new HttpHost(serverNoProtocol[1], Integer.valueOf(this.port), serverNoProtocol[0]);

	}
	
	/**
	 * build the HttpPost with headers
	 * @param resultsFormat
	 * @return
	 */
	protected void addHeaders(HttpPost httppost, SparqlResultTypes resultType) throws Exception {
		
		httppost.addHeader("Accept", this.getContentType(resultType));
		httppost.addHeader("X-Sparql-default-graph", this.graph);
	}
	
	protected void addParams(HttpPost httppost, String query, SparqlResultTypes resultType) throws Exception {
		// add params
		List<NameValuePair> params = new ArrayList<NameValuePair>(3);
		params.add(new BasicNameValuePair("query", query));
		params.add(new BasicNameValuePair("format", this.getContentType(resultType)));
		params.add(new BasicNameValuePair("default-graph-uri", this.graph));

		// set entity
		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
	}
	
	/**
	 * Return a context with digest Auth if there is a userName, otherwise null context
	 * @param targetHost
	 * @return
	 */
	protected BasicHttpContext buildHttpContext(HttpHost targetHost) {
		if (this.isAuth()) {
			DigestScheme digestAuth = new DigestScheme();
			AuthCache authCache = new BasicAuthCache();
			digestAuth.overrideParamter("realm", "SPARQL");
			// Suppose we already know the expected nonce value
			digestAuth.overrideParamter("nonce", "whatever");
			authCache.put(targetHost, digestAuth);
			BasicHttpContext ret = new BasicHttpContext();
			ret.setAttribute(HttpClientContext.AUTH_CACHE, authCache);
			return ret;
		} else {
			return null;
		}
	}
	
	/**
	 * Parse the response to JSON and send  for results parsing
	 * @param resultType
	 * @param responseTxt
	 * @return
	 * @throws Exception
	 */
	protected JSONObject parseResponse(SparqlResultTypes resultType, String responseTxt) throws Exception {
		
		Object responseObj = null;
		
		// handle empty text
		if(responseTxt == null || responseTxt.trim().isEmpty()) {
			this.handleEmptyResponse(); 
		}
		
		// parse with error handling
		try {
			responseObj = new JSONParser().parse(responseTxt);
		} catch(Exception e) {
			this.handleNonJSONResponse(responseTxt);
		}

		// handle empty JSON
		if (responseObj == null) {
			this.handleNonJSONResponse(responseTxt);

			return null;
			
		} else {
			
			// Normal path: get results
			JSONObject procResp = this.getResultsFromResponse(responseObj, resultType);
			return procResp;
		}
	}

	/**
	 * Deprecated in favor of executeAuthUpload 
	 */
	@Deprecated
	protected String explainResponseTxt(String responseTxt) {
		return "";
	}

	/**
	 * Should system perform the default retry when it receives this exception from the triplestore
	 * @param e
	 * @return
	 */
	public boolean isExceptionRetryAble(Exception e) {
		if (e instanceof AuthorizationException) {
			return false;
		}
		return true;
	}
	
	/**
	 * Upload turtle.  Many triplestores treat ttl and owl the same.
	 * @param turtle
	 * @return
	 * @throws AuthorizationException
	 * @throws Exception
	 */
	public JSONObject executeUploadTurtle(byte [] turtle) throws AuthorizationException, Exception {
		return this.executeUpload(turtle);
	}
	
	public JSONObject executeAuthUploadTurtle(byte [] turtle) throws AuthorizationException, Exception {
		return this.executeUpload(turtle);
	}
	
	/**
	 * Execute an auth query using POST
	 * @return a JSONObject wrapping the results. in the event the results were tabular, they can be obtained in the JsonArray "@Table". if the results were a graph, use "@Graph" for json-ld
	 * @throws Exception
	 */

	public JSONObject executeAuthUploadOwl(byte[] owl) throws Exception{
		return this.executeUpload(owl);
	}
	
	/**
	 * Execute an auth query using POST, and using AUTH if this.userName != null
	 * @return a JSONObject wrapping the results. in the event the results were tabular, they can be obtained in the JsonArray "@Table". if the results were a graph, use "@Graph" for json-ld
	 * @throws Exception
	 */
	public JSONObject executeAuthUpload(byte[] owl) throws Exception{
        return this.executeUpload(owl);
	}

	/**
	 * Upload an owl file, using Auth if this.userName != null
	 * @param owl
	 * @return
	 * @throws Exception
	 */
	public abstract JSONObject executeUpload(byte[] owl) throws AuthorizationException, Exception;

	public void authorizeUpload() throws AuthorizationException {
		AuthorizationManager.throwExceptionIfNotGraphWriter(this.graph);
	}
	

	/**
	 * Execute query using GET (use should be rare - in cases where POST is not supported) 
	 * @return a JSONObject wrapping the results. in the event the results were tabular, they can be obtained in the JsonArray "@Table". if the results were a graph, use "@Graph" for json-ld
	 * @throws Exception
	 */
	public JSONObject executeQueryGet(String query, SparqlResultTypes resultType) throws Exception {
		// encode the query and build a real URL
		// left over from ancient times like 2016
		URLCodec encoder = new URLCodec();
		String cleanURL = getGetURL() + encoder.encode(query);
		URL url = new URL(null, cleanURL, handler);
		String queryAndUrl = url.toString();
		
		return this.executeQueryAuthGet(queryAndUrl, resultType);
	}	
	
	/**
	 * Execute an auth query using GET (use should be rare - in cases where POST is not supported)
	 * @return a JSONObject wrapping the results. in the event the results were tabular, they can be obtained in the JsonArray "@Table". if the results were a graph, use "@Graph" for json-ld
	 * @throws Exception
	 */
	private JSONObject executeQueryAuthGet(String queryAndUrl, SparqlResultTypes resultType) throws Exception{
		AuthorizationManager.authorizeQuery(this, queryAndUrl);
		
		// buid resultsFormat
		String resultsFormat = null;
		if(resultType == null){
			resultsFormat = this.getContentType(getDefaultResultType());
		} else {
			resultsFormat = this.getContentType(resultType);
		}
		
		HttpHost targetHost = this.buildHttpHost();
		CloseableHttpClient httpclient = this.buildHttpClient(targetHost.getSchemeName());
		BasicHttpContext localcontext = this.buildHttpContext(targetHost);

		LocalLogger.logToStdErr(queryAndUrl);
		
		HttpGet httpget = new HttpGet(queryAndUrl);
		httpget.addHeader("Accept", resultsFormat);
		
		LocalLogger.logToStdOut("executing request" + httpget.getRequestLine());

		//        String responseTxt = httpclient.execute(httpget, responseHandler);
		HttpResponse response_http = httpclient.execute(targetHost, httpget, localcontext);
		
		HttpEntity entity = response_http.getEntity();
		String responseTxt = EntityUtils.toString(entity, "UTF-8");

		// parse the response
		try {
			return this.parseResponse(resultType, responseTxt);
		} finally {
			httpclient.close();
			entity.getContent().close();
		}
	}
	
	/**
	 * Execute a query, retrieving a HashMap of data for a given set of headers.
	 */
	public HashMap<String,String[]> executeQuery(String query, String[] resultHeaders) throws Exception {
		return executeQuery(query, resultHeaders, false);
	}
	

	/**
	 * Execute a query, retrieving a HashMap of data for a given set of headers.
	 *
	 * @param query	the query
	 * @param resultHeaders	the headers of the result columns desired.  If null, use all column headers.
	 * @param expectOneResult if true, expect a single row result per column - if zero or multiple results are returned, then error
	 * @return a hashmap: keys are resultHeaders, values are String arrays with contents
	 *
	 */
	public HashMap<String,String[]> executeQuery(String query, String[] resultHeaders, boolean expectOneResult) throws Exception {

		executeQuery(query, SparqlResultTypes.TABLE);

		HashMap<String,String[]> results = new HashMap<String,String[]>();
		
		String colArr[] = null;
		if (resultHeaders != null) {
			colArr = resultHeaders;
		} else {
			List<String> colList = getResultsColumnName();
			colArr = colList.toArray(new String[colList.size()]);
			
		}
		
		for (String resultHeader: colArr){

			String[] column = getStringResultsColumn(resultHeader);

			if(expectOneResult){
				// expect each column to have exactly one entry
				if(column.length > 1){
					throw new Exception("Expected 1 result for " + resultHeader + ", but retrieved multiple results");
				}else if (column.length < 1){
					throw new Exception("Expected 1 result for " + resultHeader + ", but retrieved zero results");
				}
			}

			results.put(resultHeader, column);
		}

		return results;
	}


	public JSONObject getResponse() {
		return this.response;
	}

	/**
	 * Get query results as ints
	 */
	public Integer [] getIntResultsColumn(String colName) throws Exception {
		Integer ret[] = null;
		String s = null;

		this.checkResultsCol(colName);

		// declare the array and populate it
		ret = new Integer[this.resBindings.size()];
		for (int i=0; i < this.resBindings.size(); i++) {
			if (((JSONObject)this.resBindings.get(i)).containsKey(colName) && 
					((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).containsKey("value")) {
				s = (String) ((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).get("value");
				ret[i] = Integer.parseInt(s);
			} else {
				ret[i] = null;
			}
		}
		return ret;
	}

	/**
	 * Get query results as Strings
	 */
	public String [] getStringResultsColumn(String colName) throws Exception {
		String ret[] = null;

		this.checkResultsCol(colName);

		// declare the array and populate it
		ret = new String[this.resBindings.size()];
		for (int i=0; i < this.resBindings.size(); i++) {
			if (((JSONObject)this.resBindings.get(i)).containsKey(colName) && 
					((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).containsKey("value")) {
					
				ret[i] = (String) ((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).get("value");
			} else {
				ret[i] = null;
			}
		}
		return ret;
	}

	/**
	 * Get query results as doubles
	 */	
	public Double [] getDoubleResultsColumn(String colName) throws Exception {
		Double ret[] = null;
		String s = null;

		this.checkResultsCol(colName);

		// declare the array and populate it
		ret = new Double[this.resBindings.size()];
		for (int i=0; i < this.resBindings.size(); i++) {
			if (((JSONObject)this.resBindings.get(i)).containsKey(colName) && 
					((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).containsKey("value")) {
				s = (String) ((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).get("value");
				ret[i] = Double.parseDouble(s);
			} else {
				ret[i] = null;
			}
		}
		return ret;
	}

	/**
	 * Get query results as Dates
	 */
	public Date [] getDateResultsColumn(String colName) throws Exception {
		Date[] ret = null;
		String s = null;

		this.checkResultsCol(colName);

		// declare the array and populate it
		ret = new Date[this.resBindings.size()];
		for (int i=0; i < this.resBindings.size(); i++) {
			if (((JSONObject)this.resBindings.get(i)).containsKey(colName) && 
					((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).containsKey("value")) {
				s = (String) ((JSONObject)((JSONObject)this.resBindings.get(i)).get(colName)).get("value");
				ret[i] = SparqlEndpointInterface.DATE_FORMAT.parse(s);
			} else {
				ret[i] = null;
			}
		}
		return ret;
	}

	/**
	 * Check if the query results contain a given column name (throw exception if not)
	 */
	public void checkResultsCol(String colName) throws Exception {
		int col = -1;

		// find column
		for (int i=0; i < this.resVars.size(); i++) {
			if (((String)this.resVars.get(i)).equals(colName)) {
				col = i;
				break;
			}
		}
		if (col == -1)
			throw new Exception(String.format("SparqlEndpointInterface: Asked for column %s which was not returned by the Sparql query.", colName));
	}


	/**
	 * Return all result column names.
	 */
	public ArrayList<String> getResultsColumnName(){
		ArrayList<String> retval = new ArrayList<String>();
		for (int i=0; i < this.resVars.size(); i++) {
			retval.add((String)(this.resVars.get(i)));
		}
		return retval;
	}


	/**
	 * Get a results content type to be set in the HTTP header.
	 */
	protected String getContentType(SparqlResultTypes resultType) throws Exception{
		if (resultType == null) {
			return this.getContentType(getDefaultResultType());
			
		} else if (resultType == SparqlResultTypes.TABLE || resultType == SparqlResultTypes.CONFIRM) { 
			return CONTENTTYPE_SPARQL_QUERY_RESULT_JSON; 
			
		} else if (resultType == SparqlResultTypes.GRAPH_JSONLD) { 
			return CONTENTTYPE_X_JSON_LD; 
		} else if (resultType == SparqlResultTypes.HTML) { 
			return CONTENTTYPE_HTML; 
		} 
		
		// fail and throw an exception if the value was not valid.
		throw new Exception("Cannot get content type for query type " + resultType);
	}
	
	/**
	 * 
	 * @param responseObj - JSONObject or JSONArray
	 * @param resultType
	 * @return JSONObject with @table, @message, or a jsonld structure
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	protected JSONObject getResultsFromResponse(Object responseObj, SparqlResultTypes resultType) throws Exception{
		
		JSONObject retval = null;		
		
		// check for a result type that creates a table.
		// null is default assumption that one meant a tabular result.
		if(resultType == SparqlResultTypes.TABLE) {
			JSONObject resp = (JSONObject) responseObj;
			this.response = resp;
			this.resVars = this.getHeadVars(resp);
			this.resBindings = this.getResultsBindings((JSONObject) resp);
			
			// put on the results
			retval = new JSONObject();
			retval.put(TableResultSet.TABLE_JSONKEY, this.getTableJSON(this.resVars, this.resBindings));	// @table		
			
		} else if (resultType == SparqlResultTypes.CONFIRM) {
			
			retval = new JSONObject();
			retval.put(SimpleResultSet.MESSAGE_JSONKEY, this.getConfirmMessage(responseObj)); // @message

		} else if(resultType == SparqlResultTypes.GRAPH_JSONLD) {
			retval = getJsonldResponse(responseObj);
		
		} else{
			throw new Exception("an unknown results type was passed to \"getResultsBasedOnExpectedType\". don't know how to handle type: " + resultType);
		}
		return retval;
	}

	protected JSONObject getJsonldResponse(Object responseObj) {
		return new JSONObject((JSONObject) responseObj);
	}
	/**
	 * Get head.vars with some error checking
	 * @param resp
	 * @return
	 * @throws Exception
	 */
	protected JSONArray getHeadVars(JSONObject resp) throws Exception {
		// response.head
		JSONObject head = (JSONObject)resp.get("head");
		if (head == null) {
			throw new Exception("Unexepected response from SPARQL endpoint (no head): " + resp.toJSONString());
		}
		
		// response.head.vars
		JSONArray vars = (JSONArray) head.get("vars");
		if (vars == null){
			throw new Exception("Unexepected response from SPARQL endpoint (no head.vars): " + resp.toJSONString());
		}
		return vars;
	}
	
	/**
	 * Get results.bindings with some error checking
	 * @param resp
	 * @return
	 * @throws Exception
	 */
	protected JSONArray getResultsBindings(JSONObject resp) throws Exception {
		// response.results
		JSONObject results = (JSONObject) resp.get("results");
		if (results == null) {
			throw new Exception("Sparql server did not return a 'results' object");
		}
		
		// response.results.bindings
		JSONArray bindings = (JSONArray) results.get("bindings");
		if (bindings == null){
			throw new Exception("Sparql server response 'results' did not include a 'bindings' array of result rows");
		}
		return bindings;
	}
	
	/**
	 * Parse a confirm query return.  The message will the first entry in the rows array.
	 * @param resp - an Object since it might be JSONObject or JSONArray
	 * @return
	 */
	protected String getConfirmMessage(Object resp) throws Exception {
		
		JSONArray bindings = getResultsBindings((JSONObject)resp);
		
		try {
			JSONObject row0 = (JSONObject) bindings.get(0);
			JSONObject callret0 = (JSONObject) row0.get("callret-0");
			callret0.get("value");
		} catch (Exception e) {
			throw new DontRetryException("Error parsing CONFIRM return: expecting column 'callret-0'");
		}
		
		String message = "";
		for(int i = 0; i < bindings.size(); i++){
			JSONObject row = (JSONObject) bindings.get(i); // e.g. {"callret-0":{"type":"literal","value":"Insert into <http:\/\/research.ge.com\/graph>, 1 (or less) triples -- done"}}
			JSONObject value;
			String key, valueValue;
			
			@SuppressWarnings("unchecked")
			Iterator<String> iter = row.keySet().iterator();			
			while(iter.hasNext()){
				key = (String)iter.next();
				value = (JSONObject) row.get(key);
				valueValue = (String) value.get("value");
				message += valueValue;
			}
		}
		
		return message;
	}
	
	
	/**
	 * Convert response table json to SemTK Table json
	 * 
	 * Sample input SPARQL table json:
	 *{"Test":{"type":"literal","value":"http:\/\/research.ge.com\/dataset#Test_1"},"number":{"datatype":"http:\/\/www.w3.org\/2001\/XMLSchema#integer","type":"typed-literal","value":"1272"}},
	 *{"Test":{"type":"literal","value":"http:\/\/research.ge.com\/dataset#Test_2"},"number":{"datatype":"http:\/\/www.w3.org\/2001\/XMLSchema#integer","type":"typed-literal","value":"1274"}},
	 *{"Test":{"type":"literal","value":"http:\/\/research.ge.com\/dataset#Test_3"},"number":{"datatype":"http:\/\/www.w3.org\/2001\/XMLSchema#integer","type":"typed-literal","value":"1276"}},
	 *
	 * Sample output SemTK table json:
	 * {
	 * "col_names":["Test","testnum"],
	 * "rows":[["http://research.ge.com/energy/dataset#Test_1272","1272"],["http://research.ge.com/dataset#Test_1274","1274"],["http://research.ge.com/dataset#Test_1276","1276"]],
	 * "col_type":["uri","http:\/\/www.w3.org\/2001\/XMLSchema#integer"], // TODO is this correct?
	 * "col_count":2,
	 * "row_count":3
	 * }
	 * 
	 * @param colNamesJsonArray the JSONArray containing the column names
	 * @param rowsJsonArray the JSONArray containing the data rows
	 * @return
	 * @throws Exception 
	 */
	private JSONObject getTableJSON(JSONArray colNamesJsonArray, JSONArray rowsJsonArray) throws Exception{

		String key, valueValue, valueType, valueDataType;
		JSONObject jsonCell;
		
		ArrayList<String> colsForNewTable = new ArrayList<String>();
		ArrayList<String> colTypesForNewTable = new ArrayList<String>();
		ArrayList<String> rowForNewTable;
		ArrayList<ArrayList<String>> rowsForNewTable = new ArrayList<ArrayList<String>>();
		HashMap<String, Integer> colNumHash = new HashMap<String, Integer>();
		HashMap<String, String> colTypeHash = new HashMap<String, String>();
		String curType = null;
		final String UNKNOWN = "unknown";
		final String MIXED = "http://www.w3.org/2001/XMLSchema#string";
		
		// **** Column Names and types.   Parallel arrays.  Types still unknown. ****
		for (Object colObj : colNamesJsonArray ) {
			String colStr = (String) colObj;
			colsForNewTable.add(colStr);
			colTypesForNewTable.add(UNKNOWN);	
			
			colNumHash.put(colStr, colsForNewTable.size()-1);   // hash the column indices
			colTypeHash.put(colStr, UNKNOWN);                 // hash the column types
		}
		
		// **** Rows ****
		for(int i = 0; i < rowsJsonArray.size(); i++){
			// sample row: {"Test":{"type":"literal","value":"http:\/\/research.ge.com\/dataset#Test_1276"},"testnum":{"datatype":"http:\/\/www.w3.org\/2001\/XMLSchema#integer","type":"typed-literal","value":"1276"}}
			JSONObject row = (JSONObject) rowsJsonArray.get(i);		
			
			rowForNewTable = new ArrayList<String>();  // start new row
			
			//**** loop through columns in the correct order ****
			for (Object colObj : colNamesJsonArray ) {
				key = (String) colObj;
				jsonCell = (JSONObject) row.get(key);
				
				if (jsonCell == null) {
					rowForNewTable.add(""); 
					
				} else {
					valueValue = (String) jsonCell.get("value");	
					valueType =  (String) jsonCell.get("type");
					valueDataType = (valueType.equals("typed-literal")) ? (String) jsonCell.get("datatype") : valueType;  // e.g. "http:\/\/www.w3.org\/2001\/XMLSchema#integer", but only if type is "typed-literal" 
					
					// add the value to the row
					rowForNewTable.add(valueValue);
					
					// check the type
					curType = colTypeHash.get(key);
					
					// fix UNKNOWN's as they become known
					if (curType.equals(MIXED)) {
						// do nothing if cell is already MIXED
					}
					else if (curType.equals(UNKNOWN)) {
						colTypeHash.put(key, valueDataType);
						colTypesForNewTable.set(colNumHash.get(key), valueDataType);
					
					// insert MIXED if types are coming back funny
					} else if (!curType.equals(valueDataType)) {
						colTypeHash.put(key, MIXED);
						colTypesForNewTable.set(colNumHash.get(key), MIXED);
					}	
				}
			}
			rowsForNewTable.add(rowForNewTable); // add the row to the set of rows
		}
		
		String[] colsForNewTableArray = colsForNewTable.toArray(new String[0]);
		String[] colTypesForNewTableArray = colTypesForNewTable.toArray(new String[0]);
		// create JSON to return 
		Table table = new Table(colsForNewTableArray, colTypesForNewTableArray, rowsForNewTable);  
		
		JSONObject tableJson = table.toJson();
		return tableJson;
	}	

	public abstract SparqlEndpointInterface copy() throws Exception;
}
