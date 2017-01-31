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


/**
 * An abstract class containing code for a REST client.
 */
@SuppressWarnings("deprecation")
public abstract class RestClient extends Client implements Runnable {
	
	protected RestClientConfig conf;	
	protected JSONObject parametersJSON = new JSONObject();
	Object runRes = null;
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
	 * Make the service call.  
	 * @return an Object that can be cast to a JSONObject.  Subclasses may override and return a more useful Object.   
	 */
	public Object execute() throws ConnectException, Exception {
		
		// TODO can we do this before calling execute()?
		buildParametersJSON();  // set all parameters available upon instantiation
		
		System.out.println("EXECUTE ON " + this.conf.getServiceURL());

		if(parametersJSON == null){
			throw new Exception("Service parameters not set");
		}
		
		DefaultHttpClient httpclient = new DefaultHttpClient();
	
		// immediate line below removed to perform htmml encoding in stream
		// HttpEntity entity = new ByteArrayEntity(parametersJSON.toJSONString().getBytes("UTF-8"));
		
		// js version:  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/%/g, "&#37;");

		String encoded = parametersJSON.toJSONString().replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt").replaceAll("\"", "&quot;").replaceAll("%", "&#37;");
		
		HttpEntity entity = new ByteArrayEntity(encoded.getBytes("UTF-8"));
		
		HttpPost httppost = new HttpPost(this.conf.getServiceURL());
		//httppost.setEntity(new UrlEncodedFormEntity(parametersJSON, "UTF-8"));
	    httppost.setEntity(entity);
		httppost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		
		// execute
		HttpHost targetHost = new HttpHost(this.conf.getServiceServer(), this.conf.getServicePort(), this.conf.getServiceProtocol());
		HttpResponse httpresponse = httpclient.execute(targetHost, httppost);
		
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
			System.err.println("RestClient received: " + responseTxt);
		}else{
			System.err.println("RestClient received: " + responseTxt.substring(0,200) + "... (" + responseTxt.length() + " chars)");
		}
		
		JSONObject responseParsed = (JSONObject) JSONValue.parse(responseTxt);
		if (responseParsed == null) {
			System.err.println("The response could not be transformed into json");
			if(responseTxt.contains("Error")){
				throw new Exception(responseTxt); 
			}
		}
		return responseParsed;
	}
	
	
	
}
