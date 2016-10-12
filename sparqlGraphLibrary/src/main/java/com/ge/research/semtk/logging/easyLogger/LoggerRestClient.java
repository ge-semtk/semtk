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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Stack;
import java.util.UUID;

import com.ge.research.semtk.logging.Details;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;

import com.ge.research.semtk.logging.DetailsTuple;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;

public class LoggerRestClient {

	// short hand to identify the logs written by this tool. 
	private static final String VERSION_IDENTIFIER = "000001.EXPERIMENTAL";
	private static final String VERSION_MAKE_AND_MODEL = "JAVA EASY LOG CLIENT";
	private static final String URL_PLACEHOLDER = "FROM APPLICATION API CALL";
	
	private LoggerClientConfig conf	= null;			// used to coordinate all the actual writing. 
	private Stack parentEventStack = new Stack<UUID>();		// this may be used in the long run.
	private String sessionID = UUID.randomUUID().toString();		// the ID that is used for the logging session.
	private long sequenceNumber = -1;				// starts at -1 because the first call will result in it being set to zero
	private String user;
	
	public LoggerRestClient(LoggerClientConfig conf){
		this.conf = conf;
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
		retval[0] = this.VERSION_MAKE_AND_MODEL;
		retval[1] = this.VERSION_IDENTIFIER;
		
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
		logEvent(action, details == null ? null : details.asList(), highLevelTask);
	}

	// log event
	public void logEvent(String action, ArrayList<DetailsTuple> details, String highLevelTask){
		// log an event without a known/labeled parent.
		try {
			UUID eventID = this.generateActionID();	
			this.logEvent(action, details, highLevelTask, eventID, false);
		} catch (Exception e) {
			// Exceptions are swallowed as a log attempt generates no feedback
			System.err.println("logging failed due to: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void logEventUseParent(String action, Details details, String highLevelTask) {
		logEvent(action, details == null ? null : details.asList(), highLevelTask);
	}

	public void logEventUseParent(String action, ArrayList<DetailsTuple> details, String highLevelTask, Boolean pushParent){
		// log an event which includes a parent event from the stack
		try {
			UUID eventID = this.generateActionID();
			if(pushParent){ this.parentEventStack.push(eventID); }
			this.logEvent(action, details, highLevelTask, eventID, true);
			
		} catch (Exception e) {
			// Exceptions are swallowed as a log attempt generates no feedback
			System.err.println("logging failed due to: " + e.getMessage());

		}
	}
	
	private void logEvent(String action,  ArrayList<DetailsTuple> details, String highLevelTask, UUID eventID, Boolean useParent) throws Exception{
		// the method actually perform the log message sent
	
		// used to serialize and send the details. 
		String bigDetailsString = this.serializeDetails(details);
		
		/*
		 *  @RequestParam("AppID") String applicationID, 
		 *  @RequestParam("Browser") String browser, 
		 *  @RequestParam("Version") String browserVersion, 
		 *  @RequestParam("URL") String visitURL,
		 *	@RequestParam("Main") String mainAction, 
		 *	@RequestParam("Details") String detailActions, 
		 *	@RequestParam("Parent") String parentAction, 
		 *	@RequestParam("Session") String session,
		 *	@RequestParam("EventID") String eventId, 
		 *	@RequestParam("Task") String highLevelTask, 
		 *	@RequestParam("LogSeq") String seqNum, 
		 *	@RequestHeader(SSO_ID_HEADER) String sso
		 */
		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(this.conf.getLoggingURLInfo());
		System.err.println("logging url was : " + this.conf.getLoggingURLInfo() );

		// create a JSON params object. 
		JSONObject paramsJson = new JSONObject();
		paramsJson.put("AppID", this.conf.getApplicationName());
		paramsJson.put("Browser", this.VERSION_MAKE_AND_MODEL);
		paramsJson.put("Version", this.VERSION_IDENTIFIER);
		paramsJson.put("URL", this.URL_PLACEHOLDER);
		paramsJson.put("Main", action);
		paramsJson.put("Details", bigDetailsString);
		paramsJson.put("Session", this.sessionID);
		paramsJson.put("EventID", eventID.toString());
		paramsJson.put("Task", highLevelTask);
		paramsJson.put("LogSeq", this.getNextSeqNumber() + "" );
		if(useParent){
			paramsJson.put("Parent", this.parentEventStack.peek().toString());
		}
		if (user != null) {
			paramsJson.put("SSO", this.user);
		}
		
		HttpEntity entity = new ByteArrayEntity(paramsJson.toJSONString().getBytes("UTF-8"));
		//httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
	    httppost.setEntity(entity);
		HttpHost targetHost = new HttpHost(this.conf.getServerName(), this.conf.getLoggingPort(), this.conf.getProtocol());

		// attempt the logging
		httppost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		HttpResponse httpresponse = httpclient.execute(targetHost, httppost);
		
		// some diagnostic output
		System.err.println("\n\n\n\n\nLogging response was : " + httpresponse.getStatusLine() );
	
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
				System.err.println(val);
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
	 *  Logger logger = Logger.loggerConfigInitialization(log_prop);	
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
			logger.logEvent(action, (ArrayList) null, highLevelTask);
		}
	}
	
	/**
	 * Check that logger was successfully created and perform log with one details pair
	 */
	public static void easyLog(LoggerRestClient logger, String action, String highLevelTask, String detailName1, String detailVal1) {
		if (logger != null) {
			ArrayList<DetailsTuple> deets = new ArrayList<DetailsTuple>();	 
			deets.add(new DetailsTuple(detailName1, detailVal1));
			logger.logEvent(action, deets, highLevelTask);
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
			logger.logEvent(action, deets, highLevelTask);
		}
	}
	/**
	 *  Create a logger from an EasyLogEnabledConfigProperties
	 */
	public static LoggerRestClient loggerConfigInitialization(EasyLogEnabledConfigProperties logProps){
		// send a log of the load having occurred.
		LoggerRestClient logger = null;
		try{	// wrapped in a try block because logging never announces a failure.
			LoggerClientConfig lcc = null;
			if(logProps.getLoggingEnabled()){
				// logging was set to occur. 
				lcc = new LoggerClientConfig(	logProps.getApplicationLogName(), 
												logProps.getLoggingProtocol(), 
												logProps.getLoggingServer(), 
												Integer.parseInt(logProps.getLoggingPort()), 
												logProps.getLoggingServiceLocation());
				logger = new LoggerRestClient(lcc);
			}
		}
		catch(Exception eee){
			// do nothing. 
			System.err.println("logging failed. No other details available.");
			eee.printStackTrace();
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

