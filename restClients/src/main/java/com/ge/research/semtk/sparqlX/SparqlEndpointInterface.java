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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.commons.io.IOUtils;
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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Interface to SPARQL endpoint.
 * 
 * INTERNAL USE NOTE: 
 * 		External users will use only the constructor and save/restore from JSON methods.
 *    	Query methods are for use by internal SemTk code, and will be blocked by security, etc.
 *      Use the NodeGroupExecutionClient or SparqlQueryClient instead.
 *       
 * This is an abstract class - create a subclass per implementation (Virtuoso, etc)
 * 
 * NOTE: for HTTPS connections, does not validate certificate chain.
 */
public abstract class SparqlEndpointInterface {

	// NOTE: more than one thread cannot safely share a SparqlEndpointInterface.
	// this is because of the state maintained for the results vars and connection 
	// details. doing so may lead to unexpected results

	private final static String QUERY_SERVER = "kdl";
	public final static String FUSEKI_SERVER = "fuseki";
	public final static String VIRTUOSO_SERVER = "virtuoso";
	public final static String NEPTUNE_SERVER = "neptune";
	public final static String BLAZEGRAPH_SERVER = "blazegraph";
	
	// results types to request
	protected static final String CONTENTTYPE_SPARQL_QUERY_RESULT_JSON = "application/sparql-results+json"; 
	protected static final String CONTENTTYPE_X_JSON_LD = "application/x-json+ld";
	protected static final String CONTENTTYPE_HTML = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
	protected static final String CONTENTTYPE_RDF = "application/rdf+xml";
	
	private static final int MAX_QUERY_TRIES = 4;

	// Column types used for mixed and missing types on cells
	public static String COL_TYPE_UNKNOWN = "";                                         // no cell has a type
	public static String COL_TYPE_MIXED = "http://www.w3.org/2001/XMLSchema#string";    // default mixed type cells
	public static String COL_TYPE_DOUBLE = "http://www.w3.org/2001/XMLSchema#double";   // mixture of numeric cells
	public static String COL_TYPE_INTEGER = "http://www.w3.org/2001/XMLSchema#integer"; // mixture of int-only numeric cells
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static URLStreamHandler handler = null; // set only by unit tests

	protected Table resTable = null;
	
	protected String userName = null;
	
	protected String password = null;
	protected String server = null;
	protected String port = null;
	protected String endpoint = null;    // null or everything "end/point" if url looks like http://server:8000/end/point
	protected String graph = "";
	
	protected boolean logPerformance = false;
	
	protected int retries = 0;
	protected int timeout = 0;
	
	private static HttpClientConnectionManager manager = buildConnectionManager();
	
	private static HttpClientConnectionManager buildConnectionManager() {
		PoolingHttpClientConnectionManager ret =  new PoolingHttpClientConnectionManager();
		ret.setDefaultMaxPerRoute(10);
		return ret;
	}
	
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
	 * @param serverAndPort
	 * @param graph
	 * @param user
	 * @param pass
	 * @throws Exception
	 */
	public SparqlEndpointInterface(String serverAndPort, String graph, String user, String pass) throws Exception {
		this.graph = graph;		
		this.setUserAndPassword(user, pass);
		
		this.setServerAndPort(serverAndPort);
		
	}
	
	public abstract int getInsertQueryMaxSize();
	public abstract int getInsertQueryOptimalSize();
	
	/**
	 * Timeout msec sent along with query
	 * @return
	 */
	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int seconds) {
		this.timeout = seconds;
	}

	/* all should return null when not in use */
	/* This will cause a query to throw a QueryTimeoutException */
	public abstract String getTimeoutSparqlPrefix();    // e.g. AWS query hints
	public abstract String getTimeoutSparqlClause();
	public abstract String getTimeoutPostParamName();   // e.g. Fuseki / Blazegraph REST param
	public abstract String getTimeoutPostParamValue();
	
	/**
	 * Can this endpoint run auth queries
	 * @return
	 */
	public boolean isAuth() {
		return (this.userName != null && this.userName.length() > 0);
	}

	public String getServerAndPort() {		
		String ret = this.server + ":" + this.port;
		if (this.endpoint != null) {
			ret += "/" + this.endpoint;
		}
		return ret;
	}
	
	/**
	 * Print query content and execution time to stdout
	 * 1) hurts performance (potentially a lot)
	 * 2) printing queries to log might not be desirable for security (?)
	 * 
	 * @param flag
	 */
	public void setLogPerformance(boolean flag) {
		this.logPerformance = flag;
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
		
		if (serverAndPortSplit[2].contains("/")) {
			this.endpoint = serverAndPortSplit[2].substring(serverAndPortSplit[2].indexOf('/')+1);
		}
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
	@Deprecated
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
	
	@Deprecated
	public void setDataset(String graph) {
		this.graph = graph;
	}
	
	public abstract String getServerType();
	
	/**
	 * Build a GET URL (implementation-specific)
	 */
	public abstract String getPostURL(SparqlResultTypes resultType);
	
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
	public abstract JSONObject handleEmptyResponse(SparqlResultTypes resultType) throws Exception;
	

	/**
	 * Override in order to separate out DontRetryException from regular ones
	 * and add in any known SemTK explanation
	 * See "internal use" note
	 * @param responseTxt
	 * @param resulttype
	 * @return (JSONObject) version of an acceptable result
	 * @throws DontRetryException - don't retry
	 * @throws Exception - might retry
	 */
	public abstract JSONObject handleNonJSONResponse(String responseTxt, SparqlResultTypes resulttype) throws DontRetryException, Exception;


	/**
	 * Get a default result type
	 */
	protected static SparqlResultTypes getDefaultResultType() {
		return SparqlResultTypes.TABLE;  // if no result type, assume it's a SELECT
	}

	
	public static void setUrlStreamHandler(URLStreamHandler handler) {
		SparqlEndpointInterface.handler = handler;
	}
	
	public static String [] getServerTypes() {
		return new String [] {BLAZEGRAPH_SERVER, FUSEKI_SERVER, NEPTUNE_SERVER, VIRTUOSO_SERVER};
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
	 */
	
	/**
	 * Static method to get an instance of this abstract class
	 * @param serverTypeString - virtuoso|fuseki|neptune
	 * @param server - full url of server (including /dataset for some implementations)
	 * @param graph - graph name
	 * @return
	 * @throws Exception
	 */
	public static SparqlEndpointInterface getInstance(String serverTypeString, String server, String graph) throws Exception{
		return getInstance(serverTypeString, server, graph, null, null);
	}	
	
	public static SparqlEndpointInterface getEmptyInstance() throws Exception {
		return getInstance(VIRTUOSO_SERVER, "http://empty:0", "http://empty/graph", "noone", "nopass");
	}
	
	/**
	 * Static method to get an instance of this abstract class
	 * @param serverTypeString - virtuoso|fuseki|neptune
	 * @param server - full url of server (including /dataset for some implementations)
	 * @param graph - graph name
	 * @param user - for implementations (e.g. virtuoso) that require it for inserts
	 * @param password - for implementations (e.g. virtuoso) that require it for inserts
	 * @return
	 * @throws Exception
	 */
	public static SparqlEndpointInterface getInstance(String serverTypeString, String server, String graph, String user, String password) throws Exception{
		if(serverTypeString.equalsIgnoreCase(VIRTUOSO_SERVER)){
			return new VirtuosoSparqlEndpointInterface(server, graph, user, password);				
			
		}else if(serverTypeString.equalsIgnoreCase(FUSEKI_SERVER)){
			return new FusekiSparqlEndpointInterface(server, graph, user, password);				
			
		}else if(serverTypeString.equalsIgnoreCase(NEPTUNE_SERVER)){
			return new NeptuneSparqlEndpointInterface(server, graph, user, password);				
		
		}else if(serverTypeString.equalsIgnoreCase(BLAZEGRAPH_SERVER)){
			return new BlazegraphSparqlEndpointInterface(server, graph, user, password);				
		
		}else{
			throw new Exception("Invalid SPARQL server type : " + serverTypeString);
		}	
	}
	
	/**
	 * Read json representation
	 * @param jObj
	 * @return
	 * @throws Exception
	 */
	public static SparqlEndpointInterface getInstance(JSONObject jObj) throws Exception {
		for (String key : new String[] {"type", "url"}) {
			if (!jObj.containsKey(key)) {
				throw new Exception("Invalid SparqlEndpointInterface JSON does not contain " + key + ": " + jObj.toJSONString());
			}
		}
		if (jObj.containsKey("graph")) {
			return SparqlEndpointInterface.getInstance((String)jObj.get("type"), (String)jObj.get("url"), (String)jObj.get("graph"));
		} else if (jObj.containsKey("dataset")) {
			return SparqlEndpointInterface.getInstance((String)jObj.get("type"), (String)jObj.get("url"), (String)jObj.get("dataset"));
		} else {
			throw new Exception("Invalid SparqlEndpointInterface JSON does not contain 'graph' or 'dataset': " + jObj.toJSONString());
		}
	}
	/**
	 * Write json
	 * @return
	 */
	public JSONObject toJson() {
		JSONObject ret = new JSONObject();
		
		ret.put("type", this.getServerType());
		ret.put("url", this.getServerAndPort());
		ret.put("graph", this.getGraph());
		
		return ret;
	}
	
	/**
	 * Create a SPARQLEndpointInterface and execute a query
	 * See "internal use" note
	 * @param server
	 * @param graph
	 * @param serverTypeString
	 * @param query
	 * @param resultType
	 * @return
	 * @throws Exception
	 */
	public static SparqlEndpointInterface executeQuery(String server, String graph, String serverTypeString, String query, SparqlResultTypes resultType) throws Exception {
		return executeQuery(server, graph, serverTypeString, query, null, null, resultType);
	}
  
	/**
	 * Create a SPARQLEndpointInterface and execute a query
	 * See "internal use" note
	 * @param server
	 * @param graph
	 * @param serverTypeString
	 * @param query
	 * @param user
	 * @param pass
	 * @param resultType
	 * @return
	 * @throws Exception
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
	 * Execute a TABLE result query.   High-level helper function.
	 * See "internal use" note
	 * @param query
	 * @return
	 * @throws Exception if unsuccessful
	 */
	public Table executeToTable(String query) throws Exception {
		TableResultSet res = (TableResultSet) this.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		res.throwExceptionIfUnsuccessful();
		return res.getTable();
	}
	
	/**
	 * Execute query and assemble a GeneralResultSet object.
	 * See "internal use" note
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
			resultSet = new SimpleResultSet();  // construct queries produce graphs
		}else if(resultType == SparqlResultTypes.CONFIRM){
			resultSet = new SimpleResultSet();		// auth queries produce messages
		}else if(resultType == SparqlResultTypes.TABLE){	
			resultSet = new TableResultSet(); // non-construct queries produce tables
		}else if (resultType == SparqlResultTypes.HTML) {
			resultSet = new SimpleResultSet();
		}else if (resultType == SparqlResultTypes.RDF) {
			resultSet = new SimpleResultSet();
		}
		
		// execute the query
		JSONObject result = executeQuery(query, resultType); 			
		resultSet.setSuccess(true);
		resultSet.addResultsJSON(result);
		
		return resultSet;
	}	
	public String executeQueryToRdf(String query) throws Exception {
		SimpleResultSet res = (SimpleResultSet) this.executeQueryAndBuildResultSet(query, SparqlResultTypes.RDF);
		res.throwExceptionIfUnsuccessful();
		return (String) res.getResult(SparqlResultTypes.RDF.toString());
	}
	
	/**
	 * Execute query to table
	 * See "internal use" note
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public Table executeQueryToTable(String query) throws Exception {
		TableResultSet res = (TableResultSet) this.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		res.throwExceptionIfUnsuccessful();
		return res.getTable();
	}
	
	public JSONArray executeQueryToGraph(String query) throws Exception {
		JSONObject responseJson = this.executeQuery(query, SparqlResultTypes.GRAPH_JSONLD);
		
		if (responseJson.containsKey("@graph")) {
			return (JSONArray)responseJson.get("@graph");
		} else if (responseJson.containsKey("@id")) {
			// only one object returned, no array (fuseki behavior)
			JSONArray ret = new JSONArray();
			ret.add(responseJson);
			return ret;
		} else if (responseJson.containsKey("@context")) {
			// @context, but no @graph or @id: so empty (fuseki behavior)
			JSONArray ret = new JSONArray();
			return ret;
		} else {
			throw new Exception("Invalid @graph response: " + responseJson.toJSONString());
		}
	}	
	
	/**
	 * Executes a confirm query and throws exception
	 * See "internal use" note
	 * @param query
	 * @throws Exception if unsuccessful
	 */
	public void executeQueryAndConfirm(String query) throws Exception {
		GeneralResultSet res = this.executeQueryAndBuildResultSet(query, SparqlResultTypes.CONFIRM);
		res.throwExceptionIfUnsuccessful();
	}
	
	/**
	 * Execute a query. Uses http POST.  
	 * Depending on the type of query (select, insert, construct, etc), decides whether to use auth or non-auth.
	 * See "internal use" note
	  
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

					logFailureAndSleep(e, tryCount);
					this.retries += 1;
					
				}
			}
		}
	}

	public void logFailureAndSleep(Exception e, int tryCount) throws InterruptedException {
		int sleepMsec = 500;
		
		// if we're overwhelming a server, really throttle
		if (e.getMessage() != null && e.getMessage().contains("Address already in use: connect")) {
			sleepMsec = 5000 * tryCount;
		} else {
			// normally: 2 sec per try
			sleepMsec = 2000 * tryCount;
		}
		
		// randomize sleepMsec from 75% to 125% in case threads are colliding at triplestore
		sleepMsec = (int) ((sleepMsec * 0.75) + (Math.random() * sleepMsec * 0.5));
		LocalLogger.logToStdOut (String.format("SPARQL query failed.  Sleeping %d millisec and trying again...", sleepMsec));
		LocalLogger.logToStdErr(e.getMessage());
		
		TimeUnit.MILLISECONDS.sleep(sleepMsec);
	}
	
	/**
	 * Run a test query depending on isAuth()
	 * See "internal use" note
	 * @return
	 * @throws Exception
	 */
	public JSONObject executeTestQuery() throws Exception {
		String sparql;
		SparqlResultTypes resType;
		
		if (this.isAuth()) {
			// nonsense legal delete query for auth connection
			sparql = "delete { ?x ?y ?z } where {\r\n" + 
					"?x a <nothing>.\r\n" + 
					"MINUS { ?x ?y ?z . }\r\n" + 
					"}";
			resType = SparqlResultTypes.CONFIRM;
			
		} else {
			// quick select for non-auth
			sparql = "select ?Concept where {[] a ?Concept} LIMIT 1";
			resType = SparqlResultTypes.TABLE;
		}
		
		try {
			return executeQuery(sparql, resType);
		} catch (Exception e) {
			throw new Exception("Failure executing test query: " + e.toString());
		}
	}
	
	/**
	 * Delete triples chunkSize at a time until the graph is empty.
	 * Designed for Neptune.
	 * @param chunkSize
	 * @throws Exception
	 */
	public void clearGraphInChunks(int chunkSize) throws Exception {
	
		String sparql;
		int count;
		
		while (true) {
			// count all triples
			count = this.executeToTable(SparqlToXUtils.generateCountTriplesSparql(this)).getCellAsInt(0, 0);
			
			if (count == 0) 
				return;
			
			else if (count < chunkSize) {
				// few enough, delete them all
				sparql = SparqlToXUtils.generateDeleteAllQuery(this);
				LocalLogger.logToStdOut(sparql);
				this.executeQueryAndConfirm(sparql);
				LocalLogger.logToStdOut("done deleting triples");
				return;
				
			}  else {
				LocalLogger.logToStdOut("Searching for " + count + " triples...");
				
				// get a random subject
				String s = this.executeToTable(SparqlToXUtils.generateSelectTriplesSparql(this, 1)).getCell(0, 0);
				
				// count how many triples have a subject with this subject's last character, last two characters, last three characters...
				// until the number is lower than chunk size
				for (int i=1; i < s.length(); i++) {
					String regex = s.substring(s.length() - i) + "$";
					count = this.executeToTable(SparqlToXUtils.generateCountBySubjectRegexQuery(this, regex)).getCellAsInt(0, 0);
					if (count < chunkSize) {
						
						// perform a delete
						sparql = SparqlToXUtils.generateDeleteBySubjectRegexQuery(this, regex);
						LocalLogger.logToStdOut(sparql);
						this.executeQueryAndConfirm(sparql);
						break;
					}
				}
			}
		}
	}
	
	/**
	 * how many times has this interface encountered a retry-able error and retried
	 * @return
	 */
	public int getRetries() {
		return this.retries;
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
	 * See "internal use" note
	 * @return a JSONObject wrapping the results. in the event the results were tabular, they can be obtained in the JsonArray "@Table". if the results were a graph, use "@Graph" for json-ld
	 * @throws Exception
	 */
	public JSONObject executeQueryPost(String query, SparqlResultTypes resultType) throws Exception{
		AuthorizationManager.authorizeQuery(this, query);
		
        // get client, adding userName/password credentials if any exist
		HttpHost targetHost = this.buildHttpHost();
        HttpClient httpclient = this.buildHttpClient(targetHost.getSchemeName());
		// get context with digest auth if there is a userName, else null context
		BasicHttpContext localcontext = this.buildHttpContext(targetHost);
     
		// create the HttpPost
		HttpPost httppost = new HttpPost(this.getPostURL(resultType));
		
		this.addParams(httppost, query, resultType);
		this.addHeaders(httppost, resultType);
		
		
		// parse the response
		HttpEntity entity = null;
		try {
			// execute
			long startTime=0;
			if (this.logPerformance) { 
				startTime = System.nanoTime();
			}
				
			HttpResponse response_http = httpclient.execute(targetHost, httppost, localcontext);
			
			if (this.logPerformance) { 
				LocalLogger.logToStdOut(query);
				LocalLogger.logElapsedToStdOut("query timer", startTime);
			}
			
				
			entity = response_http.getEntity();
			String responseTxt = EntityUtils.toString(entity, "UTF-8");
		
			// parse response
			return this.parseResponse(resultType, responseTxt);
		} finally {
			if (entity != null) {
				EntityUtils.consume(entity);
			}
		}
	}
	
	/**
	 * clear the graph
	 * See "internal use" note
	 * @throws Exception
	 */
	public void clearGraph() throws Exception {
		SimpleResultSet res = (SimpleResultSet) this.executeQueryAndBuildResultSet(SparqlToXUtils.generateClearGraphSparql(this), SparqlResultTypes.CONFIRM);
		res.throwExceptionIfUnsuccessful();
		this.throwExceptionIfClearGraphFailed(res);
        
	}
	
	/**
	 * See "internal use" note
	 * @param res
	 * @throws Exception
	 */
	protected void throwExceptionIfClearGraphFailed(SimpleResultSet res) throws Exception {
		String s = res.getMessage();
        String sLower = s.toLowerCase();
        if (sLower.contains("fail") || sLower.contains("error")){
        	throw new Exception(s);
        }
	}
	
	/**
	 * Delete all URI's starting with prefix
	 * See "internal use" note
	 * @param prefix
	 * @throws Exception
	 */
	public void clearPrefix(String prefix) throws Exception {
		SimpleResultSet res = (SimpleResultSet) this.executeQueryAndBuildResultSet(SparqlToXUtils.generateDeletePrefixQuery(this, prefix), SparqlResultTypes.CONFIRM);
		res.throwExceptionIfUnsuccessful();
		
        String s = res.getMessage();
        String sLower = s.toLowerCase();
        if (sLower.contains("fail") || sLower.contains("error")){
        	throw new Exception(s);
        }
	}
	
	/**
	 * See "internal use" note
	 * It has been observed that this behaves differently on different triple-stores.
	 * e.g. virtuoso throws an error if you create a graph that already exists
	 *      Neptune doesn't seem to support this command at all (?)
	 * @throws Exception
	 */
	public void createGraph() throws Exception {
		SimpleResultSet res = (SimpleResultSet) this.executeQueryAndBuildResultSet(SparqlToXUtils.generateCreateGraphSparql(this), SparqlResultTypes.CONFIRM);
		res.throwExceptionIfUnsuccessful();
		
        String s = res.getMessage();
        String sLower = s.toLowerCase();
        if (sLower.contains("fail") || sLower.contains("error")){
        	throw new Exception(s);
        }
	}
	
	/**
	 * See "internal use" note
	 * It has been observed that this behaves differently on different triple-stores.
	 * e.g. virtuoso throws an error if you drop a graph that doesn't exist
	 *      while Neptune succeeds.
	 * @throws Exception
	 */
	public void dropGraph() throws Exception {
		SimpleResultSet res = (SimpleResultSet) this.executeQueryAndBuildResultSet(SparqlToXUtils.generateDropGraphSparql(this), SparqlResultTypes.CONFIRM);
		res.throwExceptionIfUnsuccessful();
		
        String s = res.getMessage();
        String sLower = s.toLowerCase();
        if (sLower.contains("fail") || sLower.contains("error")){
        	throw new Exception(s);
        }
	}
	
	/**
	 * Get an HttpClient, handling credentials and HTTPS if needed
	 * NOTE: for HTTPS connections, does not validate certificate chain.
	 * @schemeName http or https
	 */
	protected HttpClient buildHttpClient(String schemeName) throws Exception {
		
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		clientBuilder.setConnectionManager(SparqlEndpointInterface.manager);
		
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
		
		if (this.getTimeoutPostParamName() != null && this.getTimeoutPostParamValue() != null) {
			params.add(new BasicNameValuePair(this.getTimeoutPostParamName(), this.getTimeoutPostParamValue()));
			LocalLogger.logToStdErr("Setting timeout to: " + this.getTimeoutPostParamValue());

		}

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
		
		if(responseTxt == null || responseTxt.trim().isEmpty()) {
			// empty
			return this.handleEmptyResponse(resultType); 
		}
		
		// check for RDF first, return { "rdf": "<rdf ....    >" } 
		
		if (resultType==SparqlResultTypes.RDF) {
			String beginning = responseTxt.length() > 100 ? responseTxt.substring(0,100) : responseTxt;
			beginning = beginning.trim();
			if (!beginning.trim().contains("<rdf")) {
				throw new Exception("non-rdf response, starting with: " + beginning);
			}
			JSONObject res = new JSONObject();
			res.put(SparqlResultTypes.RDF.toString(), responseTxt);
			return res;
		}
		
		// it now must be JSON.  Start by parsing.
		try {
			responseObj = new JSONParser().parse(responseTxt);
		} catch(Exception e) {
			
			// non-JSON
			return this.handleNonJSONResponse(responseTxt, resultType);
		}

		if (responseObj == null) {
			// empty JSON
			return this.handleNonJSONResponse(responseTxt, resultType);

		} else {
			// Normal path: get results
			return this.getResultsFromResponse(responseObj, resultType);
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
	 * See "internal use" note
	 * @param e
	 * @return
	 */
	public boolean isExceptionRetryAble(Exception e) {
		if (e instanceof AuthorizationException) {
			return false;
		} //else if (e instanceof ConnectException) {
			// if this connection timed out, caller is already in jeopardy of timing out
			//return false;
		//}
		return true;
	}
	
	public void uploadOwl(byte [] owl) throws Exception {
		SimpleResultSet res = new SimpleResultSet(
				this.executeUpload(owl)
				);
		res.throwExceptionIfUnsuccessful("Error uploading owl");
	}
	
	public void authUploadOwl(byte [] owl) throws Exception {
		SimpleResultSet res = new SimpleResultSet(
				this.executeAuthUploadOwl(owl)
				);
		res.throwExceptionIfUnsuccessful("Error uploading owl");
	}
	public void uploadTurtle(byte [] turtle) throws Exception {
		SimpleResultSet res = new SimpleResultSet(
				this.executeUploadTurtle(turtle)
				);
		res.throwExceptionIfUnsuccessful("Error uploading turtle");
	}
	
	public void authUploadTurtle(byte [] turtle) throws Exception {
		SimpleResultSet res = new SimpleResultSet(
				this.executeAuthUploadTurtle(turtle)
				);
		res.throwExceptionIfUnsuccessful("Error uploading turtle");
	}
	
	/**
	 * Upload turtle.  Many triplestores treat ttl and owl the same.
	 * See "internal use" note
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
	 * See "internal use" note
	 * @return a JSONObject wrapping the results. in the event the results were tabular, they can be obtained in the JsonArray "@Table". if the results were a graph, use "@Graph" for json-ld
	 * @throws Exception
	 */

	public JSONObject executeAuthUploadOwl(byte[] owl) throws Exception{
		return this.executeUpload(owl);
	}
	
	/**
	 * Execute an auth query using POST, and using AUTH if this.userName != null
	 * See "internal use" note
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
	

	public String downloadOwl() throws AuthorizationException, Exception {
		String query = SparqlToXUtils.generateConstructSPOSparql(this, "");
		AuthorizationManager.authorizeQuery(this, query);
		
		JSONObject res = this.executeQueryPost(query, SparqlResultTypes.RDF);
		return (String) res.get(SparqlResultTypes.RDF.toString());
	}
	
	/**
	 * Execute query using GET (use should be rare - in cases where POST is not supported) 
	 * See "internal use" note
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
	 * See "internal use" note
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
		HttpClient httpclient = this.buildHttpClient(targetHost.getSchemeName());
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
			EntityUtils.consume(entity);
		}
	}
	
	/**
	 * Execute a query, retrieving a HashMap of data for a given set of headers.
	 * See "internal use" note
	 */
	public HashMap<String,String[]> executeQuery(String query, String[] colNames) throws Exception {
		return executeQuery(query, colNames, false);
	}
	

	/**
	 * Execute a query, retrieving a HashMap of data for a given set of headers.
	 *
	 * See "internal use" note
	 * Note: PEC 3/17/2020 Not sure why this is here.  Only seems to be used by testing.
	 * 
	 * @param query	the query
	 * @param colNames	the headers of the result columns desired.  If null, use all column headers.
	 * @param expectOneResult if true, expect a single row result per column - if zero or multiple results are returned, then error
	 * @return a hashmap: keys are resultHeaders, values are String arrays with contents
	 *
	 */
	public HashMap<String,String[]> executeQuery(String query, String[] colNames, boolean expectOneResult) throws Exception {

		TableResultSet res = (TableResultSet) executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		Table tab = res.getTable();
		HashMap<String,String[]> results = new HashMap<String,String[]>();
		
		String colArr[] = tab.getColumnNames();
		
		for (String colName: colArr){

			String[] column = tab.getColumn(colName);

			if(expectOneResult){
				// expect each column to have exactly one entry
				if(column.length > 1){
					throw new Exception("Expected 1 result for " + colName + ", but retrieved multiple results");
				}else if (column.length < 1){
					throw new Exception("Expected 1 result for " + colName + ", but retrieved zero results");
				}
			}

			results.put(colName, column);
		}

		return results;
	}


	public JSONObject getResponse() throws Exception {
		return this.resTable.toJson();
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
		} else if (resultType == SparqlResultTypes.RDF) { 
			return CONTENTTYPE_RDF; 
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

			this.resTable = this.getTable(this.getHeadVars(resp), this.getResultsBindings((JSONObject) resp));
			// put on the results
			retval = new JSONObject();
			retval.put(TableResultSet.TABLE_JSONKEY, this.resTable.toJson());		
			
		} else if (resultType == SparqlResultTypes.CONFIRM) {
			
			retval = new JSONObject();
			retval.put(SimpleResultSet.MESSAGE_JSONKEY, this.getConfirmMessage(responseObj)); // @message

		} else if(resultType == SparqlResultTypes.GRAPH_JSONLD) {
			retval = getJsonldResponse(responseObj);
		
		} else if (resultType == SparqlResultTypes.RDF) {
			retval = (JSONObject) responseObj;
			
		} else {
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
	private Table getTable(JSONArray colNamesJsonArray, JSONArray rowsJsonArray) throws Exception{

		String key, valueValue, valueType, valueDataType;
		JSONObject jsonCell;
		
		ArrayList<String> colsForNewTable = new ArrayList<String>();
		ArrayList<String> colTypesForNewTable = new ArrayList<String>();
		ArrayList<String> rowForNewTable;
		ArrayList<ArrayList<String>> rowsForNewTable = new ArrayList<ArrayList<String>>();
		HashMap<String, Integer> colNumHash = new HashMap<String, Integer>();
		HashMap<String, String> colTypeHash = new HashMap<String, String>();
		String curType = null;
		
		
		// **** Column Names and types.   Parallel arrays.  Types still unknown. ****
		for (Object colObj : colNamesJsonArray ) {
			String colStr = (String) colObj;
			colsForNewTable.add(colStr);
			colTypesForNewTable.add(COL_TYPE_UNKNOWN);	
			
			colNumHash.put(colStr, colsForNewTable.size()-1);   // hash the column indices
			colTypeHash.put(colStr, COL_TYPE_UNKNOWN);                 // hash the column types
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
					if (valueType.endsWith("literal") && jsonCell.containsKey("datatype") ) {
						valueDataType = (String) jsonCell.get("datatype");

					} else {
						valueDataType = valueType;
					}
					
					// add the value to the row
					rowForNewTable.add(valueValue);
					
					// check the type
					curType = colTypeHash.get(key);
					
					// fix UNKNOWN's as they become known.
					// note an entire empty column will remain unknown
					if (curType.equals(COL_TYPE_MIXED) || valueValue.equals("")) {
						// do nothing if cell is already MIXED or it is empty
					}
					else if (curType.equals(COL_TYPE_UNKNOWN)) {
						colTypeHash.put(key, valueDataType);
						colTypesForNewTable.set(colNumHash.get(key), valueDataType);
					
					} else if (!curType.equals(valueDataType)) {
						// column contains mixed types.
						
						String newType = resolveTypes(curType, valueDataType, COL_TYPE_MIXED);
						colTypeHash.put(key, newType);
						colTypesForNewTable.set(colNumHash.get(key), newType);
					}	
				}
			}
			rowsForNewTable.add(rowForNewTable); // add the row to the set of rows
		}
		
		String[] colsForNewTableArray = colsForNewTable.toArray(new String[0]);
		String[] colTypesForNewTableArray = colTypesForNewTable.toArray(new String[0]);
		// create JSON to return 
		return new Table(colsForNewTableArray, colTypesForNewTableArray, rowsForNewTable);  
		
	}	
	
	/**
	 * What to do when a column is curType and the next value is valueDataType
	 * @param curType
	 * @param valueDataType
	 * @param defaultType
	 * @return
	 */
	private static String resolveTypes(String curType, String valueDataType, String defaultType) {

		String curLow = curType.toLowerCase();
		String valLow = valueDataType.toLowerCase();
		boolean curInt = curLow.contains("int") || curLow.contains("long") || curLow.contains("short");
		boolean valInt = valLow.contains("int") || valLow.contains("long") || valLow.contains("short");
		String newType = null;
		if (curInt && valInt) {
			// promote to "decimal" if all ints
			newType = COL_TYPE_INTEGER;
		} else {
			boolean curDbl = curLow.contains("float") || curLow.contains("double") || curLow.contains("decimal");
			boolean valDbl = valLow.contains("float") || valLow.contains("double") || curLow.contains("decimal");
			if ((curDbl || curInt) && (valDbl || valInt)) {
				// promote to double if all ints or floats
				newType = COL_TYPE_DOUBLE;
			} else {
				// stumped: so use string
				newType = defaultType;
			}
		}
		return newType;
	}
	
	
	public void uploadOwlModelNoClear(byte [] owl) throws Exception {
		JSONObject retJson = this.executeAuthUploadOwl(owl);
		SimpleResultSet res = SimpleResultSet.fromJson(retJson);
		res.throwExceptionIfUnsuccessful();
	}
	
	/**
	 * 
	 * See "internal use" note
	 * @param owlInputStream
	 * @throws Exception
	 */
	public void updateOwlModel(InputStream owlInputStream) throws Exception {
		
		byte [] owl = IOUtils.toByteArray(owlInputStream);
		String base = Utility.getXmlBaseFromOwlRdf(new ByteArrayInputStream(owl));
		this.clearPrefix(base);
		this.uploadOwlModelNoClear(owl);
	}


	public abstract SparqlEndpointInterface copy() throws Exception;
	
	public void copyRest(SparqlEndpointInterface other) {
		this.logPerformance = other.logPerformance;
	}
	/**
	 * Get query results as ints
	* 
	 * Deprecated in favor of executeToTable()
	 */
	@Deprecated
	public Integer [] getIntResultsColumn(String colName) throws Exception {
		Integer ret[] = null;

		this.checkResultsCol(colName);
		int col = this.resTable.getColumnIndex(colName);
		
		ret = new Integer[this.resTable.getNumRows()];
		for (int i=0; i < this.resTable.getNumRows(); i++) {
			if (this.resTable.getCell(0,  col).length() < 1) {
				ret[i] = null;
			} else {
				ret[i] = this.resTable.getCellAsInt(0,  col);
			}
		}
		return ret;
	}

	/**
	 * Get query results as Strings
	 * 
	 * Deprecated in favor of executeToTable()
	 */
	@Deprecated
	public String [] getStringResultsColumn(String colName) throws Exception {
		String ret[] = null;

		this.checkResultsCol(colName);
		return this.resTable.getColumn(colName);
	}

	/**
	 * Get query results as doubles
	 * 
	 * Deprecated in favor of executeToTable()
	 */
	@Deprecated
	public Double [] getDoubleResultsColumn(String colName) throws Exception {
		Double ret[] = null;
		String s = null;

		this.checkResultsCol(colName);
		int col = this.resTable.getColumnIndex(colName);
		
		ret = new Double[this.resTable.getNumRows()];
		for (int i=0; i < this.resTable.getNumRows(); i++) {
			if (this.resTable.getCell(0,  col).length() < 1) {
				ret[i] = null;
			} else {
				ret[i] = new Double(this.resTable.getCellAsFloat(0,  col));
			}
		}
		return ret;
	}

	/**
	 * Get query results as Dates
	 * 
	 * Deprecated in favor of executeToTable()
	 */
	@Deprecated
	public Date [] getDateResultsColumn(String colName) throws Exception {
		Date[] ret = null;
		String s = null;

		this.checkResultsCol(colName);
		int col = this.resTable.getColumnIndex(colName);
		
		ret = new Date[this.resTable.getNumRows()];
		for (int i=0; i < this.resTable.getNumRows(); i++) {
			if (this.resTable.getCell(0,  col).length() < 1) {
				ret[i] = null;
			} else {
				ret[i] = SparqlEndpointInterface.DATE_FORMAT.parse(this.resTable.getCell(0,  col));
			}
		}
		return ret;
	}

	/**
	 * Check if the query results contain a given column name (throw exception if not)
	 * 
	 * Deprecated in favor of executeToTable()
	 */
	@Deprecated 
	public void checkResultsCol(String colName) throws Exception {
		if (this.resTable.getColumnIndex(colName) == -1)
			throw new Exception(String.format("SparqlEndpointInterface: Asked for column %s which was not returned by the Sparql query.", colName));
	}


	/**
	 * Return all result column names.
	 * 
	 * Deprecated in favor of executeToTable()
	 */
	@Deprecated
	public ArrayList<String> getResultsColumnName(){
		return new ArrayList<String>(Arrays.asList(this.resTable.getColumnNames()));
	}

}
