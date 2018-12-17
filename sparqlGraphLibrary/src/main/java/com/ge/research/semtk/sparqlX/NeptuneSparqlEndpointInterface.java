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
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.aws.client.AwsS3Client;
import com.ge.research.semtk.aws.client.AwsS3ClientConfig;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/**
 * Interface to Virtuoso SPARQL endpoint
 */
public class NeptuneSparqlEndpointInterface extends SparqlEndpointInterface {
	
	protected static final String CONTENTTYPE_LD_JSON = "application/ld+json";

	private static String STATUS_COMPLETE = "LOAD_COMPLETED";
	private static String STATUS_IN_PROGRESS = "LOAD_IN_PROGRESS";
	private static String STATUS_NOT_STARTED = "LOAD_NOT_STARTED";

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

	@Override
	public void createGraph() throws Exception {
		// Not support in Neptune ???
	}
	
	/**
	 * Success criteria: contains "done"
	 * @throws Exception
	 */
	@Override
	public void clearGraph() throws Exception {
		SimpleResultSet res = (SimpleResultSet) this.executeQueryAndBuildResultSet(SparqlToXUtils.generateClearGraphSparql(this), SparqlResultTypes.CONFIRM);
		res.throwExceptionIfUnsuccessful();
		
        String s = res.getMessage();
        String sLower = s.toLowerCase();
        if (!s.contains("succeeded")){
        	throw new Exception(s);
        }
	}
	
	/**
	  * Success criteria: contains "done"
	 * @throws Exception
	 */
	@Override
	public void dropGraph() throws Exception {
		SimpleResultSet res = (SimpleResultSet) this.executeQueryAndBuildResultSet(SparqlToXUtils.generateDropGraphSparql(this), SparqlResultTypes.CONFIRM);
		res.throwExceptionIfUnsuccessful();
		
        String s = res.getMessage();
        String sLower = s.toLowerCase();
        if (!s.contains("succeeded")){
        	throw new Exception(s);
        }
	}
	/**
	 * Build a upload URL
	 */
	public String getUploadURL() throws Exception{
			return this.server + "/loader";
	}
	@Override 
	public JSONObject executeUploadTurtle(byte [] turtle) throws AuthorizationException, Exception {
		return this.executeUpload(turtle, "turtle");
	}
	
	@Override 
	public JSONObject executeAuthUploadTurtle(byte [] turtle) throws AuthorizationException, Exception {
		return this.executeUpload(turtle, "turtle");
	}
	
	@Override
	public JSONObject executeAuthUpload(byte[] owl) throws Exception {
		return this.executeUpload(owl);
	}
	
	@Override
	public JSONObject executeUpload(byte[] owl) throws Exception {
		return this.executeUpload(owl, "rdfxml");
	}
		
	public JSONObject executeUpload(byte[] owl, String format) throws Exception {
		
		this.authorizeUpload();

		// throw some exceptions if setup looks sketchy
		if (this.s3Config == null) throw new Exception ("No S3 bucket has been configured for owl upload to Neptune.");
		this.s3Config.verifySetup();
		
		SimpleResultSet ret = new SimpleResultSet();
		// upload file to S3
		String keyName = UUID.randomUUID().toString() + "." + format;
		AwsS3ClientConfig config = new AwsS3ClientConfig(
				this.s3Config.getName(), this.s3Config.getAccessId(), this.s3Config.getSecret());
        AwsS3Client s3Client = new AwsS3Client(config);
        
		try {
			
	        s3Client.execUploadFile(new String(owl), keyName);
        	String loadId = this.uploadFromS3(keyName, format);
        	
        	
        	// Allow 20 seconds for load to start.
        	// Clearly this should be configurable in the future
        	int tries = 0;
        	while (this.getLoadStatus(loadId).equals(STATUS_NOT_STARTED)) {
        		Thread.sleep(1000);
        		if (++tries > 20) {
        			throw new Exception("S3 load timed out");
        		}
        	}
        	
        	// wait for STATUS_COMPLETE 
        	tries = 0;
        	while (this.getLoadStatus(loadId).equals(STATUS_IN_PROGRESS)) {
        		Thread.sleep(250);
        		if (++tries > 80) {
        			throw new Exception("S3 load timed out");
        		}
        	}
        	
        	ret.setSuccess(true);
        	
		} catch (Exception e) {
			ret.setSuccess(false);
			ret.addRationaleMessage("NeptuneSparqlEndpointInterface.executeUpload()", e);
			
        } finally {
	        // delete the file from S3
	        s3Client.execDeleteFile(keyName);
        }
        
        // return something
		return ret.toJson();
	}
	
	/**
	 * 
	 * @param keyName
	 * @param format - https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-format.html
	 * @return
	 * @throws Exception
	 */
	private String uploadFromS3(String keyName, String format) throws Exception {
		// start the upload
        //curl blast-cluster.cluster-ceg7ggop9fho.us-east-1.neptune.amazonaws.com:8182/loader
        //-X POST 
        //-H 'Content-Type: application/json'
        //-d 
        //'{ "source" : "s3://blast-neptune/Proposal.owl", 
        //   "format" : "rdfxml", 
        //   "iamRoleArn" : "arn:aws:iam::378058653094:role/NeptuneLoadFromS3", 
        //   "region" : "us-east-1", 
        //   "failOnError" : "TRUE", 
        //   "parserConfiguration" : { "namedGraphUri" : "http://kdl.ge.com/blast-ontology" } 
        // }'
		
		HttpHost targetHost = this.buildHttpHost();
        CloseableHttpClient httpclient = this.buildHttpClient(targetHost.getSchemeName());
		BasicHttpContext localcontext = this.buildHttpContext(targetHost);
		HttpPost httppost = new HttpPost(this.getUploadURL());
		
		httppost.addHeader("Content-Type", "application/json");
		
		JSONObject parametersJSON = new JSONObject();
		parametersJSON.put("source", "s3://" + this.s3Config.getName() + "/" + keyName);
		parametersJSON.put("format", format);
		parametersJSON.put("iamRoleArn", this.s3Config.getIamRoleArn());
		parametersJSON.put("region", this.s3Config.getRegion());
		parametersJSON.put("failOnError", "TRUE");
		
		JSONObject parserConfig = new JSONObject();
		parserConfig.put("namedGraphUri", this.getGraph());
		parametersJSON.put("parserConfiguration", parserConfig);
		
		HttpEntity entity = new ByteArrayEntity(parametersJSON.toString().getBytes("UTF-8"));
		httppost.setEntity(entity);
		
		HttpResponse response_http = httpclient.execute(targetHost, httppost, localcontext);
		
		HttpEntity resp_entity = response_http.getEntity();
		String responseTxt = EntityUtils.toString(resp_entity, "UTF-8");
		JSONObject response = (JSONObject) new JSONParser().parse(responseTxt);
		
		// make sure there's a payload.loadId, otherwise throw an Exception
		if (response.containsKey("status") && response.get("status").equals("200 OK")) {
			if (!response.containsKey("payload") || ! ((JSONObject)response.get("payload")).containsKey("loadId")) {
				throw new Exception("Neptune responded with success but can't find payload.loadId in: " + response.toJSONString());
			}
		} else if (response.containsKey("detailedMessage")) {
			throw new Exception((String)response.get("detailedMessage"));
		} else {
			throw new Exception(responseTxt);
		}
		
		return (String)((JSONObject)response.get("payload")).get("loadId");
	}
	
	/**
	 * Get status or throw exception if any kind of failure
	 * @param loadId
	 * @return STATUS_IN_PROGRESS or STATUS_COMPLETE
	 * @throws Exception
	 */
	private String getLoadStatus(String loadId) throws Exception {
		
		
		HttpHost targetHost = this.buildHttpHost();
        CloseableHttpClient httpclient = this.buildHttpClient(targetHost.getSchemeName());
		BasicHttpContext localcontext = this.buildHttpContext(targetHost);
		HttpGet httpget = new HttpGet(this.getUploadURL() + "/" + loadId + "?details=TRUE&errors=TRUE" );
		
		HttpResponse response_http = httpclient.execute(targetHost, httpget, localcontext);
		
		HttpEntity resp_entity = response_http.getEntity();
		String responseTxt = EntityUtils.toString(resp_entity, "UTF-8");
		JSONObject response = (JSONObject) new JSONParser().parse(responseTxt);
		

		
		// {"payload":{
		//     "feedCount":[{"LOAD_FAILED":1}],
		//     "overallStatus":{
		//       "totalRecords":1,
		//       "totalTimeSpent":3,
		//       "insertErrors":0,
		//       "totalDuplicates":0,
		//       "datatypeMismatchErrors":0,
		//       "retryNumber":0,
		//       "runNumber":1,
		//       "fullUri":"s3:\/\/blast-neptune\/94bb3c8d-c3ec-4aad-b402-2a72a5272b4e.owl",
		//       "parsingErrors":2,
		//       "status":"LOAD_FAILED"
		//     },
		//     "errors":{
		//       "startIndex":1,
		//       "loadId":"1517eb74-4858-416e-8707-e64db46d3d8d",
		//       "endIndex":2,
		//       "errorLogs":[{
		//          "fileName":"s3:\/\/blast-neptune\/94bb3c8d-c3ec-4aad-b402-2a72a5272b4e.owl",
		//          "errorMessage":"Content is not allowed in prolog.",
		//          "errorCode":"PARSING_ERROR",
		//          "recordNum":1
		//       },{
		//          "fileName":"s3:\/\/blast-neptune\/94bb3c8d-c3ec-4aad-b402-2a72a5272b4e.owl",
		//          "errorMessage":"Fatal parsing error: Content is not allowed in prolog. [line 1, column 1]",
		//          "errorCode":"PARSING_ERROR",
		//          "recordNum":0
		//       }]
		//     },
		//     "failedFeeds":[{"totalRecords":1,"totalTimeSpent":3,"insertErrors":0,"totalDuplicates":0,"datatypeMismatchErrors":0,"retryNumber":0,"runNumber":1,"fullUri":"s3:\/\/blast-neptune\/94bb3c8d-c3ec-4aad-b402-2a72a5272b4e.owl","parsingErrors":2,"status":"LOAD_FAILED"}]},
		//     "status":"200 OK"}

		String status = null;
		try {
			JSONObject payload = (JSONObject)(response.get("payload"));
			JSONObject overallStatus = (JSONObject)(payload.get("overallStatus"));
			status = (String)(overallStatus.get("status"));
		} catch (Exception e) {
			throw new Exception(responseTxt);
		}

		if (!status.equals(STATUS_IN_PROGRESS) && !status.equals(STATUS_NOT_STARTED) && !status.equals(STATUS_COMPLETE)) {
			throw new Exception(responseTxt);
		}
		
		return status;
	}
	
	@Override
	public boolean isExceptionRetryAble(Exception e) {
		String msg = e.getMessage();
		return super.isExceptionRetryAble(e) &&
				! msg.contains("Malformed query");
				
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
			try {
				JSONObject responseObj = (JSONObject) resp;
				if (responseObj.containsKey("detailedMessage")) {
					throw new Exception((String) responseObj.get("detailedMessage"));
				} else {
					throw new Exception("ee");
				}
			} catch (Exception ee) {
				throw new Exception("Failed to parse Neptune confirm message: " + resp.toString());
			}
		}
	}
	
	/**
	 * Get a results content type to be set in the HTTP header.
	 */
	@Override
	protected String getContentType(SparqlResultTypes resultType) throws Exception{
		
		if (resultType == null) {
			return this.getContentType(getDefaultResultType());
			
		} else if (resultType == SparqlResultTypes.TABLE || resultType == SparqlResultTypes.CONFIRM) { 
			return CONTENTTYPE_SPARQL_QUERY_RESULT_JSON; 
			
		} else if (resultType == SparqlResultTypes.GRAPH_JSONLD) { 
			return CONTENTTYPE_LD_JSON;    // this is neptune-specific
		} else if (resultType == SparqlResultTypes.HTML) { 
			return CONTENTTYPE_HTML; 
		} 
		
		// fail and throw an exception if the value was not valid.
		throw new Exception("Cannot get content type for query type " + resultType);
	}
	/**
	 * Add the @graph {} wrapper to make Neptune look like Virtuoso
	 * This is only so that unit tests pass.
	 * At the moment (12/2018) it doesn't appear that anyone uses this.
	 * If you want to use it, go ahead and change it
	 * but note that Virtuoso and Neptune return different MIME types and have quirkily different returns.
	 * -Paul
	 */
	@Override
	protected JSONObject getJsonldResponse(Object responseObj) {
		JSONObject ret =  new JSONObject();
		ret.put("@graph", (JSONArray) responseObj);
		return ret;
	}
	/**
	 * Get head.vars with some error checking
	 * @param resp
	 * @return
	 * @throws Exception
	 */
	@Override
	protected JSONArray getHeadVars(JSONObject resp) throws Exception {
		// response.head
		JSONObject head = (JSONObject)resp.get("head");
		if (head != null) {
			JSONArray vars = (JSONArray) head.get("vars");
			if (vars != null){
				return vars;
			}
		}
		if (resp.containsKey("detailedMessage")) {
			throw new Exception((String)resp.get("detailedMessage"));
		} else {
			throw new Exception("Unexepected response (no head.vars): " + resp.toJSONString());
		}
	}
	@Override
	protected CloseableHttpClient buildHttpClient(String schemeName) throws Exception {
		
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		
		// skip  userName and password for Neptune
		
		// if https, use SSL context that will not validate certificate
		if(schemeName.equalsIgnoreCase("https")){
			clientBuilder.setSSLContext(getTrustingSSLContext());
		}
		
		return clientBuilder.build();
	}
}
