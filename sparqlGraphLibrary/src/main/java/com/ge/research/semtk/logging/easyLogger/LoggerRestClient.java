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


package com.ge.research.semtk.logging.easyLogger;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Stack;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;

import com.ge.research.semtk.logging.Details;
import com.ge.research.semtk.logging.DetailsTuple;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.utility.LocalLogger;

// TODO fully take advantage of extending RestClient
public class LoggerRestClient extends RestClient {

	// short hand to identify the logs written by this tool. 
	private static final String VERSION_IDENTIFIER = "000002.EXPERIMENTAL";   		// bumped for debug later
	private static final String VERSION_MAKE_AND_MODEL = "JAVA EASY LOG CLIENT";
	private static final String URL_PLACEHOLDER = "FROM APPLICATION API CALL";
	
	private LoggerClientConfig loggerClientConfig	= null;			// used to coordinate all the actual writing. 
	private Stack parentEventStack = new Stack<UUID>();		// this may be used in the long run.
	private String sessionID = UUID.randomUUID().toString();		// the ID that is used for the logging session.
	private long sequenceNumber = -1;				// starts at -1 because the first call will result in it being set to zero
	private String user;
	
	public LoggerRestClient(LoggerClientConfig loggerClientConfig) throws Exception {
		super(new RestClientConfig(loggerClientConfig.getProtocol(), loggerClientConfig.getServerName(), loggerClientConfig.getLoggingPort(), loggerClientConfig.getLoggingServiceLocation()));
		this.loggerClientConfig = loggerClientConfig;
	}

	// get
	public long getLastSequenceNumber(){
		// what is the current count
		return this.sequenceNumber;
	}
	
	public synchronized long getNextSeqNumber(){
		// increment the sequence number and then return it.
		this.sequenceNumber += 1;
		return this.sequenceNumber;
	}
	
	private UUID generateActionID(){
		return UUID.randomUUID();
	}

	private String[] getBrowserDetails(){
		String[] retval = new String[2];
		retval[0] = VERSION_MAKE_AND_MODEL;
		retval[1] = VERSION_IDENTIFIER;
		
		return retval;
	}
	//etc
	public void pushParentEvent(UUID pEvent){
		this.parentEventStack.push(pEvent); 	// push a new parent event
	}
	
	public void popParentEvent(){
		this.parentEventStack.pop();			// pop the top event
	}

	public void logEvent(String action, Details details, String highLevelTask) {
		logEvent(action, details == null ? null : details.asList(), null, highLevelTask);
	}

	public void logEvent(String action, Details details, ArrayList<String> tenants, String highLevelTask) {
		logEvent(action, details == null ? null : details.asList(), tenants, highLevelTask);
	}
	
	// log event
	public void logEvent(String action, ArrayList<DetailsTuple> details, ArrayList<String> tenants, String highLevelTask){
		// log an event without a known/labeled parent.
		try {
			UUID eventID = this.generateActionID();	
			this.logEvent(action, details, tenants, highLevelTask, eventID, false);
		} catch (Exception e) {
			// Exceptions are swallowed as a log attempt generates no feedback
			LocalLogger.logToStdErr("logging failed due to: " + e.getMessage());
			LocalLogger.printStackTrace(e);
		}
	}
	
	public void logEvent(String action, ArrayList<DetailsTuple> details, String highLevelTask){
		logEvent(action, details, null, highLevelTask);
	}

	public void logEventUseParent(String action, Details details, String highLevelTask) {
		logEvent(action, details == null ? null : details.asList(), null, highLevelTask);
	}

	public void logEventUseParent(String action, Details details, ArrayList<String> tenants, String highLevelTask) {
		logEvent(action, details == null ? null : details.asList(), tenants, highLevelTask);
	}
	
	public void logEventUseParent(String action, ArrayList<DetailsTuple> details, String highLevelTask, Boolean pushParent){
		logEventUseParent(action, details, null, highLevelTask, pushParent);
	}
	
	public void logEventUseParent(String action, ArrayList<DetailsTuple> details, ArrayList<String> tenants, String highLevelTask, Boolean pushParent){
		// log an event which includes a parent event from the stack
		try {
			UUID eventID = this.generateActionID();
			if(pushParent){ this.parentEventStack.push(eventID); }
			this.logEvent(action, details, tenants, highLevelTask, eventID, true);
			
		} catch (Exception e) {
			// Exceptions are swallowed as a log attempt generates no feedback
			LocalLogger.logToStdErr("logging failed due to: " + e.getMessage());

		}
	}
	public void logEvent(String action,  ArrayList<DetailsTuple> details, ArrayList<String> tenants, String highLevelTask, String parent) throws Exception{
		logEvent(action, details, tenants, highLevelTask, this.generateActionID(), parent);
	}

	private void logEvent(String action,  ArrayList<DetailsTuple> details, ArrayList<String> tenants, String highLevelTask, UUID eventID, Boolean useParent) throws Exception{
		this.logEvent(action, details, tenants, highLevelTask, eventID, useParent ? this.parentEventStack.peek().toString() : null);
	}
	
	@SuppressWarnings("unchecked")
	private void logEvent(String action,  ArrayList<DetailsTuple> details, ArrayList<String> tenants, String highLevelTask, UUID eventID, String parent) throws Exception{
		if (this.loggerClientConfig.getServerName().isEmpty()) {
			LocalLogger.logToStdErr("logging is off.  action=" + action);
			return;
		}
		
		// the method actually perform the log message sent
	
		// used to serialize and send the details. 
		String bigDetailsString = this.serializeDetails(details);
		String bigTenantString = this.serializeTenants(tenants);
		
		
		/*
		 *  @RequestParam("AppID") String applicationID, 
		 *  @RequestParam("Browser") String browser, 
		 *  @RequestParam("Version") String browserVersion, 
		 *  @RequestParam("URL") String visitURL,
		 *	@RequestParam("Main") String mainAction, 
		 *	@RequestParam("Details") String detailActions, 
		 *	@RequestParam("Tenants") String tenantInfo, 
		 *	@RequestParam("Parent") String parentAction, 
		 *	@RequestParam("Session") String session,
		 *	@RequestParam("EventID") String eventId, 
		 *	@RequestParam("Task") String highLevelTask, 
		 *	@RequestParam("LogSeq") String seqNum, 
		 *	@RequestHeader(SSO_ID_HEADER) String sso
		 */
		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(this.loggerClientConfig.getLoggingURLInfo());
		//LocalLogger.logToStdErr("Logging to: " + this.loggerClientConfig.getLoggingURLInfo() );

		// create a JSON params object. 
		JSONObject paramsJson = new JSONObject();
		paramsJson.put("AppID", this.loggerClientConfig.getApplicationName());
		paramsJson.put("Browser", VERSION_MAKE_AND_MODEL);
		paramsJson.put("Version", VERSION_IDENTIFIER);
		paramsJson.put("URL", URL_PLACEHOLDER);
		paramsJson.put("Main", action);
		if (bigDetailsString != null && !bigDetailsString.isEmpty()) {
			paramsJson.put("Details", bigDetailsString);
		}
		if (bigTenantString != null && !bigTenantString.isEmpty()) {
			paramsJson.put("Tenants", bigTenantString);
		}
		paramsJson.put("Session", this.sessionID);
		paramsJson.put("EventID", eventID.toString());
		paramsJson.put("Task", highLevelTask);
		paramsJson.put("LogSeq", this.getNextSeqNumber() + "" );
		if(parent != null && !parent.equals("UNKNOWN")){
			paramsJson.put("Parent", parent);
		}
		if (user != null) {
			paramsJson.put("UserID", this.user);
		}
		
		HttpEntity entity = new ByteArrayEntity(paramsJson.toJSONString().getBytes("UTF-8"));
		//httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
	    httppost.setEntity(entity);
		HttpHost targetHost = new HttpHost(this.loggerClientConfig.getServerName(), this.loggerClientConfig.getLoggingPort(), this.loggerClientConfig.getProtocol());

		// attempt the logging
		httppost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		HttpResponse httpresponse = httpclient.execute(targetHost, httppost);
	
	}

	/**
	 * Get log events
	 */
	public Table execGetLogEvents(String applicationID) throws Exception {
		TableResultSet retval = new TableResultSet();
		
		conf.setServiceEndpoint("/Logging/getLogEvents");
		this.parametersJSON.put("AppID", applicationID);
		
		try{
			retval = this.executeWithTableResultReturn();
		} finally{
			this.reset();
		}
		
		if (! retval.getSuccess()) {
			throw new Exception(String.format("Failed to retrieve log events for application '%s' Message='%s'", applicationID, retval.getRationaleAsString("\n")));
		}
		
		return retval.getTable();
	}

	
	
	private String serializeTenants(ArrayList<String> tenants) {
		String retval = null;
		
		if(tenants != null && tenants.size() > 0){
			retval = StringUtils.join(tenants, "::");
		}
		return retval;
	}

	private String serializeDetails(ArrayList<DetailsTuple> details) throws UnsupportedEncodingException {
		String retval = "";
		
		// check if there are no details.
		if(details == null){ return null;}
		
		// cycle through the Detail pairs and add the K/V to the string representation.
		int counterForBreaks = 0;
		for(DetailsTuple dt : details){
			// do we add a delimiter?
			if(counterForBreaks > 0){ retval += "::"; }
			
			// clean the values to remove illegal characters.
			String key = dt.getName().replaceAll("\\n|\\r/g", " ");

			// TODO: URL-encoded info looks terrible in the logger but i have not yet 
			// found a solution that makes it look like the js URI encoder results. 
			// maybe write a static method to handle this?
			
			String val = SparqlToXUtils.safeSparqlString(dt.getValue());
			/* String val = URLEncoder.encode(dt.getValue(), "UTF-8")		
					.replaceAll("\\+", "%20")
	                .replaceAll("\\%21", "!")
	                .replaceAll("\\%27", "'")
	                .replaceAll("\\%28", "(")
	                .replaceAll("\\%29", ")")
	                .replaceAll("\\%7E", "~")
	                .replaceAll("%20", " ");
			 */
			if(key.equalsIgnoreCase("template")){
				LocalLogger.logToStdErr(val);
			}
			
			retval += key + "," + val;
						
			counterForBreaks += 1;
		}
		// send it back.
		return retval;
	}
	/**
	 * take a new K,V and add it to an existing ArrayList of Details. 
	 * if the list is null, make one.
	 * @param currentKey
	 * @param currentValue
	 * @param details
	 * @return
	 */
	public static ArrayList<DetailsTuple> addDetails(String currentKey, String currentValue, ArrayList<DetailsTuple> details){
		if(details == null){ details = new ArrayList<DetailsTuple>();} // initialize it.
	
		// create the new tuple and add it.
		DetailsTuple dt = new DetailsTuple(currentKey, currentValue);
		details.add(dt);
		
		return details;	// send it back
	}
	public static String renderSafeString(String inputStr){
		// a lot of sparql queries (and other objects with embedded quotes and the like0 need to be preprocessed 
		// to avoid creating issues when inserting into the logs.
		String retval = "";
		
		
		return retval;
	}
	/*
	 *  Example code for static "easy" functions:
	 *  
	 *  <ampersand>Autowired
	 *  ResultsLoggingProperties log_prop;
	 *  
	 *  Logger logger = Logger.getInstance(log_prop);	
	 *  Logger.easyLog(logger, "action start", "task weather station");
	 *  ...
	 *  Logger.easyLog(logger, "action done", "task weather station", "temperature", "37 deg", "humidity", "60%");
	 *  
	 */
	
	/**
	 * Check that logger was successfully created and perform log with no details
	 */
	public static void easyLog(LoggerRestClient logger, String action, String highLevelTask) {
		if (logger != null) {
			logger.logEvent(action, (ArrayList) null, null, highLevelTask);
		}
	}
	
	/**
	 * Check that logger was successfully created and perform log with one details pair
	 */
	public static void easyLog(LoggerRestClient logger, String action, String highLevelTask, String detailName1, String detailVal1) {
		if (logger != null) {
			ArrayList<DetailsTuple> deets = new ArrayList<DetailsTuple>();	 
			deets.add(new DetailsTuple(detailName1, detailVal1));
			logger.logEvent(action, deets, null, highLevelTask);
		}
	}
	
	/**
	 * Check that logger was successfully created and perform log with two details pairs
	 */
	public static void easyLog(LoggerRestClient logger, String action, String highLevelTask, String detailName1, String detailVal1, String detailName2, String detailVal2) {
		if (logger != null) {
			ArrayList<DetailsTuple> deets = new ArrayList<DetailsTuple>();	 
			deets.add(new DetailsTuple(detailName1, detailVal1));
			deets.add(new DetailsTuple(detailName2, detailVal2));
			logger.logEvent(action, deets, null, highLevelTask);
		}
	}
	
	@Deprecated // renamed to getInstance()
	public static LoggerRestClient loggerConfigInitialization(EasyLogEnabledConfigProperties logProps, String userName) {
		return getInstance(logProps, userName);
	}
	
	public static LoggerRestClient getInstance(EasyLogEnabledConfigProperties logProps, String userName) {
		LoggerRestClient ret = getInstance(logProps);
		if (ret != null) { ret.setUser(userName); }
		return ret;
	}
	
	@Deprecated // renamed to getInstance()
	public static LoggerRestClient loggerConfigInitialization(EasyLogEnabledConfigProperties logProps){
		return getInstance(logProps);
	}
	
	/**
	 *  Create a logger client from an EasyLogEnabledConfigProperties
	 */
	public static LoggerRestClient getInstance(EasyLogEnabledConfigProperties logProps){
		// send a log of the load having occurred.
		LoggerRestClient logger = null;
		try{	// wrapped in a try block because logging never announces a failure.
			LoggerClientConfig lcc = null;
			int port = logProps.getLoggingPort().isEmpty() ? 80 : Integer.parseInt(logProps.getLoggingPort());
			if(logProps.getLoggingEnabled()){
				// logging was set to occur. 
				lcc = new LoggerClientConfig(	logProps.getApplicationLogName(), 
												logProps.getLoggingProtocol(), 
												logProps.getLoggingServer(), 
												port, 
												logProps.getLoggingServiceLocation());
				logger = new LoggerRestClient(lcc);
			}
		}
		catch(Exception eee){
			// do nothing. 
			LocalLogger.logToStdErr("logging failed. No other details available.");
			LocalLogger.printStackTrace(eee);
		}
		return logger;
	}

	public String getSessionID() {
		return sessionID;
	}

	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}
}

