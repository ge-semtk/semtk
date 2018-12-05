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
import java.util.Iterator;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/**
 * Interface to Virtuoso SPARQL endpoint
 */
public class NeptuneSparqlEndpointInterface extends SparqlEndpointInterface {
	
	private S3BucketConfig s3Config = null;
	
	/**
	 * Constructor
	 */
	public NeptuneSparqlEndpointInterface(String server, String graph)	throws Exception {
		super(server, graph);
		// TODO Auto-generated constructor stub
	}
 
	/**
	 * Constructor used for authenticated connections... like insert/update/delete/clear
	 */
	public NeptuneSparqlEndpointInterface(String server, String graph, String user, String pass)	throws Exception {
		super(server, graph, user, pass);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Neptune never requires username or password.  Always auth-capable.
	 */
	@Override
	public boolean isAuth() {
		return true;
	}
	
	public String getServerType() {
		return "neptune";
	}
	
	/**
	 * Build a GET URL
	 */
	public String getGetURL(){
		return String.format("%s:%s/sparql/?default-graph-uri=%s&format=json&query=", this.server, this.port, this.graph); 
	}
	
	/**
	 * Build a POST URL
	 */
	public String getPostURL(){
		return String.format("%s:%s/sparql", this.server, this.port);
	}

	/**
	 * Build a upload URL
	 */
	public String getUploadURL() throws Exception{
		throw new Exception("un-implemented");
	}
	
	public JSONObject executeUpload(byte[] owl) throws Exception {
		// throw some exceptions if setup looks sketchy
		if (this.s3Config == null) throw new Exception ("No S3 bucket has been configured for owl upload to Neptune.");
		this.s3Config.verifySetup();
		
		
		JSONObject ret = null;
		return ret;
	}
	
	@Override
	public boolean isExceptionRetryAble(Exception e) {
		String msg = e.getMessage();
		return super.isExceptionRetryAble(e) &&
				! msg.contains("{\"detailedMessage\":\"Malformed query:");
				
	}
	
	/**
	 * Handle an empty response
	 * (if a Virtuoso response is empty, then something is wrong)
	 * @throws Exception 
	 */
	@Override
	public void handleEmptyResponse() throws Exception {
		throw new Exception("Neptune returned empty response");	
	}

	@Override
	protected void addHeaders(HttpPost httppost, SparqlResultTypes resultType) throws Exception {
		
		httppost.addHeader("Accept", this.getContentType(resultType));
	}
	
	@Override
	protected void addParams(HttpPost httppost, String query, SparqlResultTypes resultType) throws Exception {
		// add params
		List<NameValuePair> params = new ArrayList<NameValuePair>(3);
		
		// PEC TODO: this might be tenuous: presuming all CONFIRM queries use "update"
		//           But the fix in line is a re-design that could affect a lot
		if (resultType == SparqlResultTypes.CONFIRM) {
			params.add(new BasicNameValuePair("update", query));
		} else {
			params.add(new BasicNameValuePair("query", query));
		}

		// set entity
		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
	}
	
	public void setS3Config(S3BucketConfig s3Config) {
		this.s3Config = s3Config;
	}
	
	@Override
	public SparqlEndpointInterface copy() throws Exception {
		NeptuneSparqlEndpointInterface retval = null;
		
		retval = new NeptuneSparqlEndpointInterface(this.getServerAndPort(), this.graph, this.userName, this.password);
		
		return (SparqlEndpointInterface) retval;
	}
	
	@Override
	protected String getConfirmMessage(Object resp) throws Exception {
		
		try {
			JSONArray responseArr = (JSONArray) resp;
		
			Long msec = (long) 0;
			for(int i = 0; i < responseArr.size(); i++) {
				JSONObject obj = (JSONObject) responseArr.get(i); 
				String t = (String) obj.get("type");
				if (t==null || t.isEmpty()) {
					throw new Exception("missing 'type' field");
				}
				Long millis = (Long) obj.get("totalElapsedMillis");
				
				msec += millis;
			}
			return "Succeeded in " + msec + " millisec";
			
		} catch (Exception e) {
			throw new Exception("Failed to parse Neptune confirm message: " + resp.toString(), e);
		}
	}
	
}
