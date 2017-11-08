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


package com.ge.research.semtk.services.client;

import java.net.ConnectException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.ge.research.semtk.edc.client.EndpointNotFoundException;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * An abstract class containing code for a REST client.
 */
@SuppressWarnings("deprecation")
public abstract class RestClient extends Client implements Runnable {
	
	protected RestClientConfig conf;	
	Object runRes = null;
	protected JSONObject parametersJSON = new JSONObject();
	Exception runException = null;
	
	/**
	 * Constructor
	 */
	public RestClient(){
	}
	
	public RestClientConfig getConfig() {
		return this.conf;
	}
	
	/**
	 * Execute as a thread,
	 *    puts results in runRes
	 *    and any exception into runException
	 */
	public void run() {
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
	public abstract void buildParametersJSON() throws Exception;
	
	/**
	 * Abstract method to handle an empty response from the service
	 */
	public abstract void handleEmptyResponse() throws Exception;
	
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
	
	/**
	 * Make the service call.  
	 * Subclasses may override and return a more useful Object.
	 * @param returnRawResponse True to return raw response.  False to return response parsed into JSON.
	 * @return the raw response string (if returnRawResponse is true), or an Object that can be cast to a JSONObject (if returnRawResponse is false).     
	 */
	public Object execute(boolean returnRawResponse) throws ConnectException, Exception {
		
		// TODO can we do this before calling execute()?
		buildParametersJSON();  // set all parameters available upon instantiation

		if(parametersJSON == null){
			throw new Exception("Service parameters not set");
		}
		
		DefaultHttpClient httpclient = new DefaultHttpClient();
	
		// immediate line below removed to perform htmml encoding in stream
		// HttpEntity entity = new ByteArrayEntity(parametersJSON.toJSONString().getBytes("UTF-8"));
		
		// js version:  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/%/g, "&#37;")

		HttpEntity entity = new ByteArrayEntity(parametersJSON.toString().getBytes("UTF-8"));
		HttpPost httppost = new HttpPost(this.conf.getServiceURL());
	    httppost.setEntity(entity);
		httppost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

		// execute
		HttpHost targetHost = new HttpHost(this.conf.getServiceServer(), this.conf.getServicePort(), this.conf.getServiceProtocol());
		
		HttpResponse httpresponse = null;
		try {
			httpresponse = httpclient.execute(targetHost, httppost);
		} catch (Exception e) {
			throw new Exception(String.format("Error connecting to %s", this.conf.getServiceURL()), e);
		}
		
		// handle the output			
		String responseTxt = EntityUtils.toString(httpresponse.getEntity(), "UTF-8");
		httpclient.close();
		if(responseTxt == null){ 
			throw new Exception("Received null response text"); 
		}
		if(responseTxt.trim().isEmpty()){
			handleEmptyResponse();  // implementation-specific behavior
		}

		if(responseTxt.length() < 500){
			LocalLogger.logToStdErr("RestClient received from " + this.conf.getServiceEndpoint() + ": " + responseTxt);
		}else{
			LocalLogger.logToStdErr("RestClient received from " + this.conf.getServiceEndpoint() + ": " + responseTxt.substring(0,200) + "... (" + responseTxt.length() + " chars)");
		}
		
		if(returnRawResponse){
			return responseTxt;		// return the raw string
		}else{
			JSONObject responseParsed = (JSONObject) JSONValue.parse(responseTxt);
			if (responseParsed == null) {
				LocalLogger.logToStdErr("The response could not be transformed into json for request to " + this.conf.getServiceEndpoint());
				if(responseTxt.contains("Error")){
					throw new Exception(responseTxt); 
				}
			}
			return responseParsed;	// return the response parsed into JSON
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

	
}
