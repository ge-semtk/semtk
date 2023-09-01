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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;
import com.github.jsonldjava.shaded.com.google.common.io.Files;

/**
 * Interface to MarkLogic SPARQL endpoint
 */
public class MarkLogicSparqlEndpointInterface extends SparqlEndpointInterface {

	protected String database = null;
	private final static String MSG_CLEAR_NONEXIST_GRAPH = "No such RDF";
	private static String MARKLOGIC_MLCP_PATH = System.getenv("MARKLOGIC_MLCP_PATH");
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
		File tempFile = File.createTempFile("upload", ".ttl", null);
		
		try {
			try (FileOutputStream fos = new FileOutputStream(tempFile)) {
				fos.write(turtle);
			}
			return mlcp(tempFile);
		} finally {
			FileUtils.deleteQuietly(tempFile);
		}
	}
	
	@Override 
	/** Main entry point from query service **/
	public JSONObject executeAuthUploadTurtle(byte[] turtle) throws AuthorizationException, Exception {
		return this.executeUploadTurtle(turtle);
	}
	
	@Override 
	/** Main entry point from query service **/
	public JSONObject executeAuthUploadOwl(byte[] owl) throws AuthorizationException, Exception {
		File tempFile = File.createTempFile("upload", ".owl", null);
		
		try {
			try (FileOutputStream fos = new FileOutputStream(tempFile)) {
				fos.write(owl);
			}
			return mlcp(tempFile);

		} finally {
			FileUtils.deleteQuietly(tempFile);
		}
	}
	
	@Override
	/** Default upload of OWL.  Errors if bytes are not OWL **/
	public JSONObject executeUpload(byte[] owl) throws AuthorizationException, Exception {
		return this.executeAuthUploadOwl(owl);
	}
	
	@Override
	public JSONObject executeAuthUploadStreamed(InputStream is, String filename) throws AuthorizationException, Exception {
		File tempFile = File.createTempFile("upload", "." + FileNameUtils.getExtension(filename), null);
		
		try {
			// copy is to tempfile
			try (FileOutputStream fos = new FileOutputStream(tempFile)) {
				byte[] buffer = new byte[1024];
			    int bytesRead;
			    while ((bytesRead = is.read(buffer)) != -1)
			    {
			        fos.write(buffer, 0, bytesRead);
			    }
			}
			
			is.close();
			return mlcp(tempFile);
		} finally {
			FileUtils.deleteQuietly(tempFile);
		}
	}	
	
// ---------------------------------------------------------------
//  Aug 2023 MarkLogic reports this endpoint is buggy.
//  These public functions go with the buggy endpoint functions
//  which are also commented out below
// ---------------------------------------------------------------
//
//	@Override 
//	public JSONObject executeUploadTurtle(byte[] turtle) throws AuthorizationException, Exception {
//		return this.executeUpload(turtle, "file.ttl");
//	}
//	
//	@Override 
//	/** Main entry point from query service **/
//	public JSONObject executeAuthUploadTurtle(byte[] turtle) throws AuthorizationException, Exception {
//		return this.executeUpload(turtle, "file.ttl");
//	}
//	
//	@Override 
//	/** Main entry point from query service **/
//	public JSONObject executeAuthUploadOwl(byte[] turtle) throws AuthorizationException, Exception {
//		return this.executeUpload(turtle, "file.owl");
//	}
//	
//	@Override
//	/** Default upload of OWL.  Errors if bytes are not OWL **/
//	public JSONObject executeUpload(byte[] owl) throws AuthorizationException, Exception {
//		return this.executeUpload(owl, "file.owl");
//	}
//	
//	@Override
//	public JSONObject executeAuthUploadStreamed(InputStream is, String filename) throws AuthorizationException, Exception {
//		EntityBuilder builder = EntityBuilder.create();
//
//		builder.setStream(is);
//		HttpEntity entity = builder.build();
//		return this.executeAuthUpload(new BufferedHttpEntity(entity), filename);
//	}
	
// ---------------------------------------------------------------
//  Aug 2023 MarkLogic reports this endpoint is buggy.
//  Some urls are translated  http://prefix/#classname
//  And some are  translated  http://prefix#classname
//  ..within the same upload.  So there are broken triples.
// ---------------------------------------------------------------
//
//	private JSONObject executeUpload(byte[] owl, String filename) throws AuthorizationException, Exception {
//		EntityBuilder builder = EntityBuilder.create();
//
//		builder.setBinary(owl);
//		HttpEntity entity = builder.build();
//
//		return this.executeAuthUpload(entity, filename);
//	}
//	
//	/**
//	 * Main function for auth uploading
//	 */
//	private JSONObject executeAuthUpload(HttpEntity entity, String filename) throws AuthorizationException, Exception {
//		this.authorizeUpload();
//		
//		HttpHost targetHost = this.buildHttpHost();
//        HttpClient httpclient = this.buildHttpClient(targetHost.getSchemeName());
//		BasicHttpContext localcontext = this.buildHttpContext(targetHost);
//		
//		// PARAMS: graph param is in URL
//		HttpPost httppost = new HttpPost(this.getUploadURL());
//		
//		// HEADERS: content - type
//		if (filename.toLowerCase().endsWith(".owl"))
//			httppost.addHeader("content-type", CONTENTTYPE_RDF);
//		else if (filename.toLowerCase().endsWith(".ttl"))
//			httppost.addHeader("content-type", CONTENTTYPE_TURTLE);
//		else if (filename.toLowerCase().endsWith(".nt"))
//			httppost.addHeader("content-type", CONTENTTYPE_N_TRIPLES);
//
//		// TODO else see what happens when there is no header.  Can MarkLogic figure it out?
//		// TODO how do you specifiy the database
//		httppost.setEntity(entity);
//		
//		executeTestQuery();
//
//		HttpResponse response_http = httpclient.execute(targetHost, httppost, localcontext);
//
//		String responseTxt = this.getResponseText(response_http);  
//		SimpleResultSet ret = new SimpleResultSet();
//		
//		if(responseTxt.isEmpty()) {
//			// marklogic returned nothing
//			ret.setSuccess(true);
//		} else if (responseTxt.matches("^/triplestore.*xml$")) {
//			// marklogic returned an xml file name -ish thing
//			ret.setMessage(responseTxt);
//			ret.setSuccess(true);
//		} else {
//			ret.setSuccess(false);
//			ret.addRationaleMessage("MarkLogicEndpointInterface.executeUpload", responseTxt);
//		}
//		
//		return ret.toJson();
//	}
	
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
	/**
	 * Parse the response to JSON and send  for results parsing
	 * @param resultType
	 * @param responseTxt
	 * @return Json with one of RDF, NTRIPLES, @message, @table   (no json-ld)
	 * @throws Exception
	 */
	protected JSONObject parseResponse(SparqlResultTypes resultType, String responseTxt) throws Exception {
		JSONObject responseObj;
		
		if(responseTxt == null || responseTxt.trim().isEmpty()) {
			// empty
			return this.handleEmptyResponse(resultType); 
		}
		
		// perform MarkLogic error checking : would be in json
		JSONObject errResponse = null;
		try {
			responseObj = (JSONObject) new JSONParser().parse(responseTxt);
			errResponse = (JSONObject) responseObj.get("errorResponse");
		} catch(Exception e) {
		}
			
		// if there is a marklogic json with an error, handle it here first
		if (errResponse != null) {			
			
			// parse the json errorResponse
			String abbrevResponse = responseTxt.length() > 1000 ? responseTxt.substring(0,1000): responseTxt;
			if (errResponse == null) 
				throw new DontRetryException("Error response contained no 'errorResponse': " + abbrevResponse);

			String message = (String) errResponse.get("message");
			if (message == null) 
				throw new DontRetryException("Error response contained no 'message': " + abbrevResponse);

			// handle the parsed values
			if (message.contains(MSG_CLEAR_NONEXIST_GRAPH)) {
				throw new DontRetryException(message.substring(message.lastIndexOf("No such")));
			} else {
				throw new DontRetryException("MarkLogic returned error response: " + message);
			}
			
		} else if (responseTxt.startsWith("<error")) {
			// parse the xml error
			String message = "";
			try {
				InputStream is = IOUtils.toInputStream(responseTxt, StandardCharsets.UTF_8);
	
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
				Document doc = dbf.newDocumentBuilder().parse(is);
				NodeList nodeList = doc.getElementsByTagName("message"); 
				message = nodeList.item(0).getFirstChild().getNodeValue();
			} catch (Exception e) {
				throw new DontRetryException("Error reading xml error response from MarkLogic:" + responseTxt);
			}
			throw new DontRetryException("MarkLogic returned error response: " + message);

		} else {
			// no error, do the usual
			return super.parseResponse(resultType, responseTxt);
		}
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
	 * Get a results content type to be set in the HTTP header.
	 * MarkLogic docs:
	 *   https://docs.marklogic.com/guide/semantics/REST#id_81353
	 */
	@Override
	protected String getAccept(SparqlResultTypes resultType) throws Exception{
		if (resultType == null) {
			return this.getAccept(getDefaultResultType());
			
		} else if (resultType == SparqlResultTypes.TABLE || resultType == SparqlResultTypes.CONFIRM) { 
			return CONTENTTYPE_SPARQL_QUERY_RESULT_JSON; 
			
		} else if (resultType == SparqlResultTypes.GRAPH_JSONLD) { 
			// MarkLogic will not do JSON-LD
			return CONTENTTYPE_RDF_JSON; 
		} else if (resultType == SparqlResultTypes.HTML) { 
			return CONTENTTYPE_HTML; 
		} else if (resultType == SparqlResultTypes.RDF) { 
			return CONTENTTYPE_RDF; 
		} else if (resultType == SparqlResultTypes.N_TRIPLES) {
			return CONTENTTYPE_N_TRIPLES;
		}
		
		// fail and throw an exception if the value was not valid.
		throw new DontRetryException("Cannot get content type for query type " + resultType);
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
			// MarkLogic often returns nothing when successful
			ret.put("@message", "Success.");   
			return ret;
		} else {
			throw new Exception("MarkLogic query returned empty response");
		}
	}
	/**
	 * Throw exception if @Message indicates an error.  MarkLogic messages can be empty.
	 * @param res
	 * @throws Exception
	 */
	@Override
	protected void throwExceptionOnMessage(SimpleResultSet res) throws Exception {
		try {
			String s = res.getMessage();
	        String sLower = s.toLowerCase();
	        if (sLower.contains("fail") || sLower.contains("error")){
	        	throw new Exception(s);
	        }
		} catch (Exception e) {
			// missing message means no error in MarkLogic
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
	
	/**
	 * Upload the file and delete it using locally installed MLCP from MarkLogic
	 * since the rest interface has a bug with randomly slashing URI's  http://prefix/#item
	 *                                                                               ^
	 * @param tempFile
	 * @return
	 */
	private JSONObject mlcp(File tempFile) throws Exception {

		//String MLCP_COMMAND = "C:\\Users\\200001934\\mlcp-11.0.3\\bin\\mlcp.bat";
		if (MARKLOGIC_MLCP_PATH==null)
			throw new Exception("SemTK config error: Missing $MARKLOGIC_MLCP_PATH environment variable.");
		
		String u = this.getUserName();
		String p  = this.getPassword();
		if (u == null || u.isBlank() || p==null || p.isBlank())
			throw new Exception("mlcp upload requires username and password.");
		
		String command = MARKLOGIC_MLCP_PATH  
				+ " import"
				+ " -host " + this.getServerWithoutProtocol() 
				+ " -username " + this.getUserName()
				+ " -password " + this.getPassword()
				+ " -output_graph " + this.getGraph()
				+ " -input_file_type rdf "
				+ " -input_file_path " + tempFile.getAbsolutePath();
				
				
		// TODO: will this work on N_TRIPLES ?
		// TODO: won't work with https?
		
		Runtime rt = Runtime.getRuntime();
		
		Process pr = rt.exec(command);

		BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

		String line=null;
		StringBuffer output = new StringBuffer();
		while((line=input.readLine()) != null) {
			output.append(line + "\n");
		}

		int exitVal = pr.waitFor();
		
		String outputStr = output.toString();
		
		SimpleResultSet simple = new SimpleResultSet();
		if (exitVal==0 && !outputStr.contains("FATAL") && !outputStr.contains("ERROR") ) {
			simple.setSuccess(true);
			simple.setMessage(outputStr);
		} else {
			simple.setSuccess(false);
			simple.setMessage("MLCP ingestion failed");
			simple.addRationaleMessage("MarkLogicSparqlEndpoint.mlcp()", outputStr);
		}
		
		return simple.toJson();
	}
}
