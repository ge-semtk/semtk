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
import java.util.Map;
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
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
// for the API version
//import org.eclipse.rdf4j.model.Value;
//import org.eclipse.rdf4j.query.BindingSet;
//import org.eclipse.rdf4j.query.QueryLanguage;
//import org.eclipse.rdf4j.query.TupleQuery;
//import org.eclipse.rdf4j.query.TupleQueryResult;
//import org.eclipse.rdf4j.query.Update;
//import org.eclipse.rdf4j.repository.Repository;
//import org.eclipse.rdf4j.repository.RepositoryConnection;
//import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
//import org.eclipse.rdf4j.repository.sparql.query.SPARQLBooleanQuery;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.aws.client.AwsS3Client;
import com.ge.research.semtk.aws.client.AwsS3ClientConfig;
import com.ge.research.semtk.resultSet.SimpleResultSet;
//import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;
import com.javaquery.aws.AWSV4Auth;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Interface to Virtuoso SPARQL endpoint
 */
public class NeptuneSparqlEndpointInterface extends SparqlEndpointInterface {
	
	protected static final String CONTENTTYPE_LD_JSON = "application/ld+json";

	private static String STATUS_COMPLETE = "LOAD_COMPLETED";
	private static String STATUS_IN_PROGRESS = "LOAD_IN_PROGRESS";
	private static String STATUS_NOT_STARTED = "LOAD_NOT_STARTED";

	private S3BucketConfig s3Config = null;
	
	TreeMap<String,String> addedParams = new TreeMap<String,String>();
	
	private static final String SUCCESS = "succeeded";
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
	
	public JSONObject executeUpload(byte[] data, String format) throws Exception {
		return this.executeUploadREST(data, format);
	}
	
	/**
	 * Does not use S3 Java API.  Raw REST.
	 * @param owl
	 * @param format
	 * @return
	 * @throws Exception
	 */
	public JSONObject executeUploadREST(byte[] owl, String format) throws Exception {
		
		this.authorizeUpload();

		// throw some exceptions if setup looks sketchy
		if (this.s3Config == null) throw new Exception ("No S3 bucket has been configured for owl upload to Neptune.");
		this.s3Config.verifySetup();
		
		SimpleResultSet ret = new SimpleResultSet();
		// upload file to S3
		String keyName = UUID.randomUUID().toString() + "." + format;
		AwsS3ClientConfig config = new AwsS3ClientConfig(this.s3Config.getName());
        AwsS3Client s3Client = new AwsS3Client(config);
        
		try {
			
	        s3Client.execUploadFileREST(new String(owl), keyName);
	        
	        // try loading from S3, while being a little careful about concurrent load limit
	        String loadId = null;
	        int tries = 0;
	        while (loadId == null) {
		        try {
		        	loadId = this.uploadFromS3(keyName, format);
		        	
		        } catch (Exception e) {
		        	// take 20 shots at concurrent load limit, sleeping 0-2 seconds between
		        	if (tries < 20 && e.getMessage().contains("concurrent load limit")) {
		        		LocalLogger.logToStdOut("Retrying: " + e.getMessage());
		        		tries += 1;
		        		Thread.sleep((long)(2.0 * Math.random()));
		        	} else {
		        		// give up retrying
		        		throw e;
		        	}
		        }
	        }
        	
        	// Allow 20 seconds for load to start.
        	// Clearly this should be configurable in the future
        	tries = 0;
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
	 * S3 examples are from https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/examples-s3-objects.html
	 * NOTE: need to do the "Multiple Parts" example if the data is > 5meg or so
	 * @param data
	 * @param format
	 * @return
	 * @throws Exception
	 */
	public JSONObject executeUploadAPI(byte[] data, String format) throws Exception {
		
		this.authorizeUpload();
		
		Region region = Region.of(this.s3Config.region);
		S3Client s3 = S3Client.builder().region(region).build();	
		String keyName = UUID.randomUUID().toString() + "." + format;
		SimpleResultSet ret = new SimpleResultSet();

		try {
			
			// put the file
			s3.putObject(PutObjectRequest.builder().bucket(this.s3Config.name).key(keyName).build(),
					RequestBody.fromBytes(data));
	        
	        // try loading from S3, while being a little careful about concurrent load limit
	        String loadId = null;
	        int tries = 0;
	        while (loadId == null) {
		        try {
		        	loadId = this.uploadFromS3(keyName, format);
		        	
		        } catch (Exception e) {
		        	// take 20 shots at concurrent load limit, sleeping 0-2 seconds between
		        	if (tries < 20 && e.getMessage().contains("concurrent load limit")) {
		        		LocalLogger.logToStdOut("Retrying: " + e.getMessage());
		        		tries += 1;
		        		Thread.sleep((long)(2.0 * Math.random()));
		        	} else {
		        		// give up retrying
		        		throw e;
		        	}
		        }
	        }
        	
        	// Allow 20 seconds for load to start.
        	// Clearly this should be configurable in the future
        	tries = 0;
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
    		s3.deleteObject(DeleteObjectRequest.builder().bucket(this.s3Config.name).key(keyName).build());
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
		
		if (msg.contains("Malformed query")) {
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
		
		// AWS Auth
		
		TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
        awsHeaders.put("host", this.server);
        
        String region = new DefaultAwsRegionProviderChain().getRegion().id();
        AwsCredentials cred = DefaultCredentialsProvider.create().resolveCredentials();
        String key = cred.accessKeyId();
        String secret = cred.secretAccessKey();
       
		AWSV4Auth aWSV4Auth = new AWSV4Auth.Builder(key, secret)
                .regionName(region)
                .serviceName("neptune-db") // es - elastic search. use your service name
                .httpMethodName("POST") //GET, PUT, POST, DELETE, etc...
                .canonicalURI("/sparql") //end point
                .queryParametes(this.addedParams) //query parameters if any
                .awsHeaders(awsHeaders) //aws header parameters
                .payload(null) // payload if any
                .debug() // turn on the debug mode
                .build();

		/* Get header calculated for request */
		Map<String, String> header = aWSV4Auth.getHeaders();
		for (Map.Entry<String, String> entrySet : header.entrySet()) {
			String k = entrySet.getKey();
			String val = entrySet.getValue();

			httppost.addHeader(k, val);
		}
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
			JSONObject responseObj = null;
			try {
				responseObj = (JSONObject) resp;
			} catch (Exception ee) {
				throw new Exception("Failed to parse Neptune confirm message: " + resp.toString());
			}
			
			if (responseObj.containsKey("detailedMessage")) {
				throw new Exception((String) responseObj.get("detailedMessage"));
			} else {
				throw new Exception("ee");
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
	
	
	
	
	/**
	 * https://docs.aws.amazon.com/neptune/latest/userguide/access-graph-sparql-java.html
	 * 
	 * Currently unused.  Could @Override executeQueryPost()
	 */
//	public JSONObject executeQueryPostAPI(String query, SparqlResultTypes resultType) throws Exception{
//		AuthorizationManager.authorizeQuery(this, query);
//		
//		Repository repo = new SPARQLRepository(this.getPostURL(SparqlResultTypes.TABLE));
//        repo.initialize();
//
//        try (RepositoryConnection conn = repo.getConnection()) {
//
//        	if (resultType == SparqlResultTypes.TABLE) { 
//        		
//	        	TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
//	        
//	        	try (TupleQueryResult result = tupleQuery.evaluate()) {
//	        		
//	        		// PEC HERE : really want the raw JSON just like I get from the REST call
//	        		//            possible step: revert and view the JSON and re-make it
//	        		//            hopefully get a better answer on stackoverflow
//                  //           RUMOR has it that this doesn't do any IAM, so it may not be helpful
//	        		
//	        		String colNames[] = result.getBindingNames().toArray(new String[0]);
//	        		String colTypes[] = new String[colNames.length];
//	        		for (int i=0; i < colTypes.length; i++) {
//	        			colTypes[i] = "string";
//	        		}
//	        		
//	        		Table table = new Table(colNames, colTypes);
//	        		String row[] = new String[colNames.length];
//	        		while (result.hasNext()) {  // iterate over the result
//	        			BindingSet bindingSet = result.next();
//	        			for (int i=0; i < colNames.length; i++) {
//	        				row[i] = bindingSet.getValue(colNames[i]).stringValue();
//	        			}
//	        		}
//	        		return table.toJson();
//	        	}
//	  
//        	} else if (resultType == SparqlResultTypes.CONFIRM) {
//        		final Update update = conn.prepareUpdate(QueryLanguage.SPARQL, query);
//        		update.execute();
//        		
//        		// return { "@message" : "succeeded" }
//        		JSONObject ret = new JSONObject();
//        		ret.put(SimpleResultSet.MESSAGE_JSONKEY, SUCCESS);
//        		return ret;
//        	}
//        }
//        return null;
//	}
}
