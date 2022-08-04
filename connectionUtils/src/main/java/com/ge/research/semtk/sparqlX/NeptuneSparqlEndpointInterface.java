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

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
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
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.amazonaws.auth.AWS4Signer;
import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.aws.AWSRequestSigningApacheInterceptor;
import com.ge.research.semtk.aws.AWSSessionTokenApacheInterceptor;
import com.ge.research.semtk.aws.AwsCredentialsProviderAdaptor;
import com.ge.research.semtk.aws.S3Connector;
import com.ge.research.semtk.aws.SemtkAwsCredentialsProviderBuilder;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.utility.LocalLogger;

//import com.javaquery.aws.AWSV4Auth;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

/**
 * Interface to Neptune SPARQL endpoint
 * 
 * NOTE: The role NEPTUNE_UPLOAD_S3_AWS_IAM_ROLE_ARN is only used for uploading from the S3 bucket to Neptune - it is not used for placing the file in the S3 bucket.  
 * 		 The latter is enabled by an IAM role attached to the EC2 node.
 */
public class NeptuneSparqlEndpointInterface extends SparqlEndpointInterface {
	
	protected static final String CONTENTTYPE_LD_JSON = "application/ld+json";


	private static String STATUS_COMPLETE = "LOAD_COMPLETED";
	private static String STATUS_IN_PROGRESS = "LOAD_IN_PROGRESS";
	private static String STATUS_NOT_STARTED = "LOAD_NOT_STARTED";
	
	private S3Connector s3Conn = null;
	private String iamRoleArn = null;
	
	TreeMap<String,String> addedParams = new TreeMap<String,String>();
	
	private static final String SUCCESS = "succeeded";
	
	/**
	 * Constructor used for authenticated connections... like insert/update/delete/clear
	 */
	public NeptuneSparqlEndpointInterface(String server, String graph, String user, String pass)	throws Exception {
		super(server, graph, user, pass);
		this.readEnv();
	}
	
	public NeptuneSparqlEndpointInterface(String server, String graph)	throws Exception {
		super(server, graph);
		this.readEnv();
	}
	
	private void readEnv() throws Exception {
		String region = System.getenv("NEPTUNE_UPLOAD_S3_CLIENT_REGION");
		String name = System.getenv("NEPTUNE_UPLOAD_S3_BUCKET_NAME");
		this.iamRoleArn = System.getenv("NEPTUNE_UPLOAD_S3_AWS_IAM_ROLE_ARN");
		String failedVariables = "";

		if (region == null || region.isEmpty()) {
			failedVariables += "NEPTUNE_UPLOAD_S3_CLIENT_REGION ";
		}
		if (name == null || name.isEmpty()) {
			failedVariables += "NEPTUNE_UPLOAD_S3_BUCKET_NAME ";
		}
		if (iamRoleArn == null || iamRoleArn.isEmpty()) {
			failedVariables += "NEPTUNE_UPLOAD_S3_AWS_IAM_ROLE_ARN ";
		}

		if (!failedVariables.isEmpty()) {
			throw new Exception("Config error: can't perform Neptune upload with blank variable(s) in SemTK service environment: \n" + failedVariables);
		}

		this.s3Conn = new S3Connector(region, name);
	}
 
	public int getInsertQueryMaxSize()    { return 100000; }
	public int getInsertQueryOptimalSize()  { return 5000; }
	public String getLocalDefaultGraphName() { return null; }  // look this up and add correct value

	/* Timeout is not implemented. Should be query hint.   */
	/* https://docs.aws.amazon.com/neptune/latest/userguide/sparql-query-hints-queryTimeout.html */
	/* This will require mods to sparql generator */
	public String getTimeoutSparqlPrefix() { return null; }    
	public String getTimeoutSparqlClause() { return null; } 
	public String getTimeoutPostParamName() { return null; }    
	public String getTimeoutPostParamValue() { return null; } 
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
	public String getPostURL(SparqlResultTypes resultType) {
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
        if (!sLower.contains(SUCCESS)){
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
        if (!sLower.contains(SUCCESS)){
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
	
	public JSONObject executeUploadGremlinCSV(byte[] csv) throws Exception {
		return this.executeUploadAPI(csv, "csv");
	}
	
	public JSONObject executeUploadGremlinCSV(String csv) throws Exception {
		return this.executeUploadAPI(csv.getBytes(), "csv");
	}
	
	
	public JSONObject executeUpload(byte[] data, String format) throws Exception {
		return this.executeUploadAPI(data, format);
	}
	
	
	
	/**
	 * S3 examples are from https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/examples-s3-objects.html
	 * NOTE: need to do the "Multiple Parts" example if the data is > 5meg or so
	 * @param data
	 * @param format
	 * @return
	 * @throws Exception
	 */
	public JSONObject executeUploadAPI(byte[] data, String format) throws Exception {
		
		this.authorizeUpload();		
		
		String keyName = UUID.randomUUID().toString() + "." + format;
		SimpleResultSet ret = new SimpleResultSet();

		try {
			
			// put the file
			this.s3Conn.putObject(keyName, data);
			
	        // try loading from S3, while being a little careful about concurrent load limit
	        String loadId = null;
	        int tries = 0;
	        while (loadId == null) {
		        try {
		        	loadId = this.uploadFromS3(keyName, format);
		        	
		        } catch (Exception e) {
		        	// take 20 shots at concurrent load limit, sleeping between
		        	if (tries < 20 && (
		        			e.getMessage().contains("concurrent load limit") ||
		        			e.getMessage().contains("LOAD_COMMITTED_W_WRITE_CONFLICTS")
		        			)) {
		        		LocalLogger.logToStdOut("Retrying: " + e.getMessage());
		        		tries += 1;
		        		Thread.sleep((long)(2000.0 * Math.random()) * tries);
		        	} else {
		        		// give up retrying
		        		throw e;
		        	}
		        }
	        }
        	
        	// wait for load to start.
        	tries = 0;
        	while (this.getLoadStatus(loadId).equals(STATUS_NOT_STARTED)) {
        		Thread.sleep(500);
        	}
        	
        	// wait if IN_PROGRESS 
        	tries = 0;
        	while (this.getLoadStatus(loadId).equals(STATUS_IN_PROGRESS)) {
        		Thread.sleep(250);
        	}
        	
        	// check for COMPLETE (docs state this means no errors)
        	String finalStatus = this.getLoadStatus(loadId);
        	if (! finalStatus.equals(STATUS_COMPLETE)) {
        		throw new Exception("Neptune load from S3 failed to reach STATUS_COMPLETE: " + finalStatus);
        	}
        	
        	ret.setSuccess(true);
        	
		} catch (Exception e) {
			// this error should make it to the return value
			ret.setSuccess(false);
			ret.addRationaleMessage("NeptuneSparqlEndpointInterface.executeUpload()", e);
			LocalLogger.logToStdErr("Exception during executeUploadAPI()");
			LocalLogger.printStackTrace(e);
			
        } finally {
        	// try to delete file from s3
        	LocalLogger.logToStdErr("Attempting to delete file " + keyName + " from bucket " + this.s3Conn.getName());
        	try {
        		this.s3Conn.deleteObject(keyName);
        	} catch (Exception ee) {
        		// if it fails, log it but don't mess up the return
        		LocalLogger.printStackTrace(ee);
        	}
        }
        
        // return something
		return ret.toJson();
	}
	
	
	/**
	 * I can't find docs for using a Java API to do this.
	 * https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load.html
	 *    - doesn't have a 'Java' sub-bullet
	 *    
	 * @param keyName
	 * @param format - https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-format.html
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked" })
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
		parametersJSON.put("source", "s3://" + this.s3Conn.getName() + "/" + keyName);
		parametersJSON.put("format", format);
		parametersJSON.put("iamRoleArn", this.iamRoleArn);
		parametersJSON.put("queueRequest", "TRUE");
		parametersJSON.put("region", this.s3Conn.getRegion());
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
		
		if (msg.contains("Malformed query") || msg.contains("temporary credentials")) {
			return false;
		}
		
		else {
			return super.isExceptionRetryAble(e);
		}
				
	}
	
	/**
	 * Handle an empty response
	 * (if a Virtuoso response is empty, then something is wrong)
	 * @throws Exception 
	 */
	@Override
	public JSONObject handleEmptyResponse(SparqlResultTypes resultType) throws Exception {
		throw new Exception("Neptune returned empty response");	
	}
	
	@Override 
	public JSONObject handleNonJSONResponse(String responseTxt, SparqlResultTypes resulttype) throws DontRetryException, Exception {
		throw new Exception ("Neptune returned non-json: " + responseTxt);
	}

	@Override
	protected void addHeaders(HttpPost httppost, SparqlResultTypes resultType) throws Exception {
		
		httppost.addHeader("Accept", this.getContentType(resultType));
		
		// Added for AWS V4 Signing
		httppost.addHeader("Content-type", "application/x-www-form-urlencoded");
	}
	
	@Override
	protected void addHeaders(HttpURLConnection conn, SparqlResultTypes resultType) throws Exception {
		
		conn.setRequestProperty("Accept", this.getContentType(resultType));
		conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
	}

	
	@Override
	protected void addParams(HttpPost httppost, String query, SparqlResultTypes resultType) throws Exception {
		// add params
		List<NameValuePair> params = new ArrayList<NameValuePair>(3);
		this.addedParams.clear();
		
		// PEC TODO: this might be tenuous: presuming all CONFIRM queries use "update"
		//           But the fix in line is a re-design that could affect a lot
		if (resultType == SparqlResultTypes.CONFIRM) {
			this.addedParams.put("update", query);
			params.add(new BasicNameValuePair("update", query));
		} else {
			this.addedParams.put("query", query);
			params.add(new BasicNameValuePair("query", query));
		}

		// set entity
		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
	}
	
	/**
	 *  This now happens automatically from environment variables
	 *  
	 *  Use this function only to override the environment.
	 * @param s3Config
	 */
	
	public void setS3Config(String region, String bucketName, String iamRoleArn) {
		this.s3Conn = new S3Connector(region, bucketName);
		this.iamRoleArn = iamRoleArn;
	}
	
	@Override
	public SparqlEndpointInterface copy() throws Exception {
		NeptuneSparqlEndpointInterface retval = null;
		
		retval = new NeptuneSparqlEndpointInterface(this.getServerAndPort(), this.graph, this.userName, this.password);
		retval.copyRest(this);
		
		return (SparqlEndpointInterface) retval;
	}
	
	/**
	 * return a successful confirm message otherwise
	 * @exception - confirm message indicates failure
	 */
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
			// could parse to JSON and pull out detailedMessage and code
			// but extra error handling and code gets long-winded with very little advantage.
			throw new Exception("Neptune failure: " + resp.toString());
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
	@SuppressWarnings("unchecked")
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
			return null; // no head vars, no error message
		}
	}
	@SuppressWarnings("unchecked")
	@Override
	protected CloseableHttpClient buildHttpClient(String schemeName) throws Exception {
		
		HttpClientBuilder clientBuilder = HttpClients.custom();
		
		// skip  userName and password for Neptune
		
		String region = new DefaultAwsRegionProviderChain().getRegion().id();
		
		// AWS Neptune signer
		AWS4Signer signer = new AWS4Signer();
		signer.setServiceName("neptune-db");
		signer.setRegionName(region);   
		
		// Interceptor to add credentials
		StaticCredentialsProvider provider = SemtkAwsCredentialsProviderBuilder.getAWSCredentialsProvider();
		AwsCredentials cred = provider.resolveCredentials();
		
		clientBuilder.addInterceptorFirst(new AWSRequestSigningApacheInterceptor("neptune-db", signer, new AwsCredentialsProviderAdaptor(provider)));
		
		// Interceptor to add token if the credentials-provider-hunting above found one and saved it
		if (cred instanceof AwsSessionCredentials) {
			clientBuilder.addInterceptorFirst(new AWSSessionTokenApacheInterceptor(((AwsSessionCredentials)cred).sessionToken()));
		}

		// if https, use SSL context that will not validate certificate
		if(schemeName.equalsIgnoreCase("https")){
			clientBuilder.setSSLContext(getTrustingSSLContext());
		}
		
		return clientBuilder.build();
	}
	
	
}
