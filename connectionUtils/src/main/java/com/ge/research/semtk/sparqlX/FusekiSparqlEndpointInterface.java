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
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Interface to Fuseki SPARQL endpoint
 */
public class FusekiSparqlEndpointInterface extends SparqlEndpointInterface {

	// change for fuseki
	protected static final String CONTENTTYPE_X_JSON_LD = "application/ld+json";

	public FusekiSparqlEndpointInterface(String server, String graph)	throws Exception {
		super(server, graph);
		if (this.endpoint == null) {
			throw new Exception("Fuseki URL is missing the dataset: " + server);
		}
	}

	public FusekiSparqlEndpointInterface(String server, String graph, String user, String pass)	throws Exception {
		super(server, graph, user, pass);
		if (this.endpoint == null) {
			throw new Exception("Fuseki URL is missing the dataset: " + server);
		}
	}

	public int getInsertQueryMaxSize()    { return 50000; }
	public int getInsertQueryOptimalSize() { return 5000; }
	public String getLocalDefaultGraphName() { return  "urn:x-arq:DefaultGraph"; }
	
	/* Timeout is not implemented.  Should be "timeout" REST param */
	public String getTimeoutSparqlPrefix() { return null; }    
	public String getTimeoutSparqlClause() { return null; } 
	public String getTimeoutPostParamName() { return "timeout"; }    
	public String getTimeoutPostParamValue() { return this.sparqlTimeout == 0 ? null : String.valueOf(this.sparqlTimeout); } 
	
	/**
	 * Fuseki uses different param names for "auth" queries, which Fuseki calls "update"
	 */
	@Override
	protected void addParams(HttpPost httppost, String query, SparqlResultTypes resultType) throws Exception {

		List<NameValuePair> params = new ArrayList<NameValuePair>(3);
		
		if (resultType == SparqlResultTypes.TABLE || resultType == SparqlResultTypes.GRAPH_JSONLD || resultType == SparqlResultTypes.RDF || resultType == SparqlResultTypes.N_TRIPLES) { 
			params.add(new BasicNameValuePair("query", query));
		} else {
			params.add(new BasicNameValuePair("update", query));
		}

		// timeout 
		if (this.getTimeoutPostParamName() != null && this.getTimeoutPostParamValue() != null) {
			LocalLogger.logToStdErr("timeout header: " + this.getTimeoutPostParamName() + "=" + this.getTimeoutPostParamValue());
			params.add(new BasicNameValuePair(this.getTimeoutPostParamName(), this.getTimeoutPostParamValue()));
		}
		
		// set entity
		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
	}
	
	/**
	 * Override identical function of parent because some of the CONTENTTYPE_ constants are overridden
	 */
	@Override
	protected String getAccept(SparqlResultTypes resultType) throws Exception{
		if (resultType == null) {
			return this.getAccept(getDefaultResultType());
		} else if (resultType == SparqlResultTypes.TABLE || resultType == SparqlResultTypes.CONFIRM) { 
			return CONTENTTYPE_SPARQL_QUERY_RESULT_JSON; 
		} else if (resultType == SparqlResultTypes.GRAPH_JSONLD) { 
			return CONTENTTYPE_X_JSON_LD; 
		} else if (resultType == SparqlResultTypes.HTML) { 
			return CONTENTTYPE_HTML; 
		}  else if (resultType == SparqlResultTypes.RDF) { 
			return CONTENTTYPE_RDF; 
		}  else if (resultType == SparqlResultTypes.N_TRIPLES) {
			return CONTENTTYPE_N_TRIPLES;
		}
		
		// fail and throw an exception if the value was not valid.
		throw new Exception("Cannot get Fuseki content type for query type " + resultType);
	}

	/**
	 * Build a GET URL
	 */
	public String getGetURL(){
		return String.format("%s:%s/%s?query=", this.server, this.port, this.endpoint);
	}

	/**
	 * Build a POST URL
	 */
	public String getPostURL(SparqlResultTypes resultType) {
		if (resultType == SparqlResultTypes.TABLE || resultType == SparqlResultTypes.GRAPH_JSONLD || resultType == SparqlResultTypes.RDF || resultType == SparqlResultTypes.N_TRIPLES) { 
			return String.format("%s:%s/%s", this.server, this.port, this.endpoint);
		}else {
			return String.format("%s:%s/%s/update", this.server, this.port, this.endpoint);	
		}
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
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();   
		ContentBody fileBody = new ByteArrayBody(owl, filename);
		builder.addPart("files[]", fileBody);
		HttpEntity entity = builder.build();
		return this.executeAuthUpload(entity);
	}
	
	@Override
	public JSONObject executeAuthUploadStreamed(InputStream is, String filename) throws AuthorizationException, Exception {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();   
		builder.addBinaryBody("files[]", is, ContentType.MULTIPART_FORM_DATA, filename);
		HttpEntity entity = builder.build();
		return this.executeAuthUpload(entity);
	}
	
	/**
	 * Main function for auth uploading
	 */
	private JSONObject executeAuthUpload(HttpEntity entity) throws AuthorizationException, Exception {
		this.authorizeUpload();
		
		HttpHost targetHost = this.buildHttpHost();
        HttpClient httpclient = this.buildHttpClient(targetHost.getSchemeName());
		BasicHttpContext localcontext = this.buildHttpContext(targetHost);
		HttpPost httppost = new HttpPost(this.getUploadURL());
		
		this.addHeaders(httppost, SparqlResultTypes.HTML);
		 		
		httppost.setEntity(entity);
		
		executeTestQuery();

		HttpResponse response_http = httpclient.execute(targetHost, httppost, localcontext);
		HttpEntity resp_entity = response_http.getEntity();
		// get response with HTML tags removed
		String responseTxt = EntityUtils.toString(resp_entity, "UTF-8").replaceAll("\\<.*?>"," ");

		SimpleResultSet ret = new SimpleResultSet();
		
		Long count = -1L;
		try {
			JSONObject jObj = (JSONObject) new JSONParser().parse(responseTxt);
			count = (Long) jObj.get("count");
		} catch (Exception e) {}
		
		if(count >= 0){
			ret.setSuccess(true);
		} else {
			ret.setSuccess(false);
			ret.addRationaleMessage("FusekiEndpointInterface.executeUpload", responseTxt);
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
		if (resultType == SparqlResultTypes.N_TRIPLES) {
			JSONObject ret = new JSONObject();
			ret.put("N_TRIPLES", "");
			return ret;
		} else {
			throw new Exception("Fuseki query returned empty response");
		}
	}
	
	@Override
	public JSONObject handleNonJSONResponse(String responseTxt, SparqlResultTypes resulttype) throws DontRetryException, Exception {

		// Fuseki (<4.5) seems to only return html when successful
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
				throw new Exception("Fuseki unsupported HTML reponse: " + responseTxt);
			}
			
		} else {

			// Fuseki 4.5 returns text messages not in HTML (note "No such graph" is not a failure, it creates the graph)
			if(responseTxt.contains("Update succeeded") || responseTxt.contains("No such graph")) {
				if (resulttype == SparqlResultTypes.CONFIRM) {
					SimpleResultSet ret = new SimpleResultSet(true);
					ret.setMessage(responseTxt);
					return ret.getResultsJSON();
				}
			}else if (this.sparqlTimeout > 0 && responseTxt.contains("503")) {
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
			throw new Exception("Fuseki unsupported non-HTML response: " + responseTxt);
		}
	}

	@Override
	public String getUploadURL() throws Exception {
		return String.format("%s:%s/%s/data?graph=%s", this.server, this.port, this.endpoint, this.getGraph());	
	}

	@Override
	public String getServerType() {
		return "fuseki";
	}
	
	@Override
	public SparqlEndpointInterface copy() throws Exception {
		FusekiSparqlEndpointInterface retval = null;
		retval = new FusekiSparqlEndpointInterface(this.getServerAndPort(), this.graph, this.userName, this.password);
		retval.copyRest(this);
		return (SparqlEndpointInterface) retval;
	}
	
}
