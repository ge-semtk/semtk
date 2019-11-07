/**
 ** Copyright 2018 General Electric Company
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

package com.ge.research.semtk.services.client;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.ge.research.semtk.auth.HeaderTable;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.edc.client.EndpointNotFoundException;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClientConfig.Methods;
import com.ge.research.semtk.servlet.utility.Utility;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * An  class containing code for a REST client.
 */
@SuppressWarnings("deprecation")
public  class RestClient extends Client implements Runnable {
	
	protected RestClientConfig conf;	
	Object runRes = null;
	protected JSONObject parametersJSON = new JSONObject();
	Exception runException = null;
	protected HeaderTable headerTable = null;
	protected String putContent = null;
	protected File fileParameter = null;
	protected String fileParameterName = "file";
	protected HttpResponse httpResponse = null;
	
	/** 
	 * Get default headers
	 */
	public ArrayList<BasicHeader> getDefaultHeaders() {
		
		ArrayList<BasicHeader> ret = new ArrayList<BasicHeader>();
		
		if (this.headerTable != null) {
			// loop through hashtable and build default headers
			for (String key : this.headerTable.keySet()) {
				String value = this.headerTable.get(key).get(0);
				for (int i=1; i < this.headerTable.get(key).size(); i++) {
					value = value + "," + this.headerTable.get(key).get(i);
				}				
				BasicHeader header = new BasicHeader(key, value);
				ret.add(header);
			}
		}		
		return ret;
	}
	
	/**
	 * Constructor
	 */
	public RestClient() {
		this.headerTable = ThreadAuthenticator.getThreadHeaderTable();
	}
	
	/**
	 * Constructor
	 */
	public RestClient(RestClientConfig conf){
		this.headerTable = ThreadAuthenticator.getThreadHeaderTable();
		this.conf = conf;
	}
	
	public RestClientConfig getConfig() {
		return this.conf;
	}
	
	public void addHeader(String key, List<String> vals) {
		if (this.headerTable == null) {
			this.headerTable = new HeaderTable();
		}
		this.headerTable.put(key, vals);
	}
	
	public void addHeader(String key, String value) {
		ArrayList<String> vals = new ArrayList<String>();
		vals.add(value);
		this.addHeader(key, vals);
	}
	
	public void removeHeader(String key) {
		this.headerTable.remove(key);
	}
	
	public void addParameter(String key, JSONObject val) {
		this.parametersJSON.put(key, val);
	}
	
	public void addParameter(String key, String val) {
		this.parametersJSON.put(key, val);
	}
	
	/** 
	 * Remove endpoint and parameter settings, so this client can be used to connect to a new endpoint. 
	 * Also remove any results and exceptions.
	 */
	public void reset(){
		this.conf.setServiceEndpoint(null);
		this.parametersJSON.clear();
		this.runRes = null;
		this.runException = null;
	}
	
	/**
	 * Execute as a thread,
	 *    puts results in runRes
	 *    and any exception into runException
	 */
	public void run() {
		ThreadAuthenticator.authenticateThisThread(this.headerTable);
		try {
			this.runException = null;
			this.runRes = this.execute();
		} catch (Exception e) {
			this.runException = e;
		}
	}
	
	public Object getRunRes() {
		return runRes;
	}

	public Exception getRunException() {
		return runException;
	}
	
	/**
	 * Abstract method to set up service parameters available upon instantiation
	 */
	public  void buildParametersJSON() throws Exception {};
	
	/**
	 * Abstract method to handle an empty response from the service
	 */
	public void handleEmptyResponse() throws Exception {
		throw new Exception("Received empty response from " + conf.getServiceURL() + ": " + this.httpResponse.toString());
	}
	
	/**
	 * Execute and get result as SimpleResultSet
	 */
	public SimpleResultSet executeWithSimpleResultReturn() throws ConnectException, EndpointNotFoundException, Exception{		
		if (conf.getServiceEndpoint().isEmpty()) {
			throw new Exception("Attempting to execute client with no endpoint specified.");
		}
		JSONObject resultJSON = (JSONObject) execute();			
		return (SimpleResultSet) SimpleResultSet.fromJson(resultJSON);  
	}
	
	/**
	 * Execute and get result as TableResultSet
	 */
	public TableResultSet executeWithTableResultReturn() throws ConnectException, EndpointNotFoundException, Exception{		
		if (conf.getServiceEndpoint().isEmpty()) {
			throw new Exception("Attempting to execute client with no endpoint specified.");
		}
		JSONObject resultJSON = (JSONObject) execute();	 
		return new TableResultSet(resultJSON);
	}
	
	/**
	 * Make the service call. 
	 * Subclasses may override and return a more useful Object. 
	 * Returns the response parsed into JSON. 
	 * @return an Object that can be cast to a JSONObject.    
	 */
	public Object execute() throws ConnectException, Exception {
		return execute(false);
	}
	
	public JSONObject executeToJson() throws Exception {
		return (JSONObject) this.execute(false);
	}
	
	/**
	 * Make the service call.  
	 * Subclasses may override and return a more useful Object.
	 * @param returnRawResponse True to return raw response.  False to return response parsed into JSON.
	 * @return the raw response string (if returnRawResponse is true), or an Object that can be cast to a JSONObject (if returnRawResponse is false).     
	 */
	public Object execute(boolean returnRawResponse) throws ConnectException, Exception {
		
		// set all parameters available upon instantiation
		buildParametersJSON();  
		
		if (conf.method != RestClientConfig.Methods.PUT) {
			if(parametersJSON == null){
				throw new Exception("Service parameters not set");
			}
		} else {
			if(parametersJSON != null && parametersJSON.size() > 0){
				throw new Exception("Service parameters are not implemented for PUT");
			}
		}

		// create entity
		// HttpEntity entity = new ByteArrayEntity(parametersJSON.toJSONString().getBytes("UTF-8")); // perform html encoding in stream	
		// js version:  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/%/g, "&#37;")
		HttpEntity entity = null;
		
		if (putContent != null) {
			if (conf.method == RestClientConfig.Methods.PUT) {
				entity = new StringEntity(putContent);
			} else {
				throw new Exception("Internal: putContent is provided for non-PUT request: " + conf.method.name());
			}
			
		} else if (fileParameter != null) {
			// add the file
			FileBody bin = new FileBody(fileParameter);
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.addPart(this.fileParameterName, bin);
			
			// add parametersJSON as StringBody
			for (Object k : parametersJSON.keySet()) {
				builder.addPart((String) k, new StringBody((String)parametersJSON.get(k)));
			}
			entity = builder.build();
			
		} else {
			// add just parametersJSON
			entity = new ByteArrayEntity(parametersJSON.toString().getBytes("UTF-8"));
		}

		// create a request - GET or POST 
		HttpRequestBase httpreq = null;
		if (conf.method == RestClientConfig.Methods.GET) {
			HttpGet httpget = new HttpGet(this.conf.getServiceURL());
			httpget.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			httpreq = httpget;
		} else if (conf.method == RestClientConfig.Methods.PUT) {
			HttpPut httpput = new HttpPut(this.conf.getServiceURL());
			if (entity != null) {
				httpput.setEntity(entity);
			}
			httpreq = httpput;
			
		} else if (conf.method == RestClientConfig.Methods.DELETE) {
			HttpDelete httpdelete = new HttpDelete(this.conf.getServiceURL());
			httpreq = httpdelete;
			
		} else {
			HttpPost httppost = new HttpPost(this.conf.getServiceURL());
			httppost.setEntity(entity);
			if (fileParameter == null) {
				httppost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			}
			httpreq = httppost;
		}

		// add default headers
		for (BasicHeader header : this.getDefaultHeaders()) {
			httpreq.addHeader(header);
		}
		
		// execute
		HttpHost targetHost = new HttpHost(this.conf.getServiceServer(), this.conf.getServicePort(), this.conf.getServiceProtocol());		
		this.httpResponse = null;
		try {
			HttpClient httpclient = HttpClients.custom().build();
			//HttpClient httpclient = HttpClients.custom().setDefaultHeaders(RestClient.getDefaultHeaders()).build();
			LocalLogger.logToStdOut("Connecting to: " + this.conf.getServiceURL());
			this.httpResponse = httpclient.execute(targetHost, httpreq);
		} catch (Exception e) {
			throw new Exception(String.format("Error connecting to %s", this.conf.getServiceURL()), e);
		}
		
		// handle the output			
		String responseTxt = null;
		HttpEntity responseEntity = this.httpResponse.getEntity();
		if (responseEntity != null) {
			responseTxt = EntityUtils.toString(responseEntity, "UTF-8");		
		}
		
		// Null response for GET or post is Exception
		if(responseTxt == null && (this.conf.method == Methods.GET || this.conf.method == Methods.POST)){ 
			throw new Exception("Received null response text"); 
			
		// Handle other null or empty responses with implementation-specific behavior
		}else if(responseTxt == null || responseTxt.trim().isEmpty()){
			handleEmptyResponse();
		}
		
		if(returnRawResponse){
			// check raw response for HTTP errors, which should be json with status!=200
			try {
				JSONObject responseParsed = (JSONObject) JSONValue.parse(responseTxt);
				if (responseParsed.containsKey("status") && (long)responseParsed.get("status") != 200) {
					throw new Exception(responseTxt);
				}
			} catch (NullPointerException e) {
			} catch (ClassCastException e) {
			} catch (ParseException e) {
			}
			return responseTxt;		// return the raw string
			
		}else{
			// return empty response (legal and common for PUT)
			if (responseTxt == null || responseTxt.isEmpty()) {
				return new JSONObject();
				
			} else {
				// parse response into JSON or throw an error
				JSONObject responseParsed = (JSONObject) JSONValue.parse(responseTxt);
				if (responseParsed == null) {
						throw new Exception(responseTxt);
				}
				return responseParsed;	// return the response parsed into JSON
			}
		}
	}
	
	/**
	 * Get the last run result as a SimpleResultSet.
	 */
	protected SimpleResultSet getRunResAsSimpleResultSet() throws Exception{
		SimpleResultSet retval = new SimpleResultSet();
		if(this.runRes == null){ throw new Exception("last service communication resulted in null and cannot be converted");}
		retval = SimpleResultSet.fromJson((JSONObject)this.runRes);
		return retval;
	}
	
	/**
	 * Get the last run result as a TableResultSet.
	 */
	protected TableResultSet getRunResAsTableResultSet() throws Exception{
		TableResultSet retval = new TableResultSet();
		if(this.runRes == null){ throw new Exception("last service communication resulted in null and cannot be converted");}
		retval = new TableResultSet((JSONObject)this.runRes);
		return retval;
	}

	/**
	 * Append a param onto a GET url
	 * @param url
	 * @param name
	 * @param value
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public static String addGetParam(String url, String name, String value) throws UnsupportedEncodingException {
		String ret = url;
		ret += (url.contains("?") ? "&" : "?") + name + "=" + URLEncoder.encode(value,"UTF-8");
		
		return ret;
	}
	
}
