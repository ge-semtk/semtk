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

package com.ge.research.semtk.auth;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Cache of triplestore auth graph.
 * Responsible for checking ownership and throwing AuthorizationExceptions
 * @author 200001934
 *
 */
public class AuthorizationManager {
	
	private static long lastUpdate = 0;

	private static final String JOB_ADMIN_GROUP = "JobAdmin";

	private static int refreshFreqSeconds = 301;
	private static boolean authSetupFailed = false;
	private static String authFilePath = null;
		
	// userGroup.get(name) = ArrayList of user names
	private static HashMap<String, ArrayList<String>> userGroups = new HashMap<String, ArrayList<String>>();
	
	// graphXers(graph_name) = ArrayList of group names
	private static HashMap<String, ArrayList<String>> graphReaders = new HashMap<String, ArrayList<String>>();
	private static HashMap<String, ArrayList<String>> graphWriters = new HashMap<String, ArrayList<String>>();


	private static void clear() {
		refreshFreqSeconds = 301;
		authSetupFailed = false;
		authFilePath = null;
		userGroups.clear();
		graphReaders.clear();
		graphWriters.clear();
	}
	
	/**
	 * Turn on Authorization by providing a SPARQL endpoint and frequency.
	 * Called at service start-up
	 * If no authFilePath is given, succeeds and all Authorization requests will pass.
	 * If authFilePath is given but load fails: >> KILLS THE PROCESS. <<
	 * 
	 * @param endpointProps - location of the semtk services
	 * @param authProps - authorization properties
	 * @returns boolean - was authorization applied
	 */
	public static boolean authorize(AuthorizationProperties authProps) throws AuthorizationException {
		clear();
		
		// If any necessary authorization info is empty
		if ( authProps.getSettingsFilePath().isEmpty() ) {
			LocalLogger.logToStdErr("NOTICE: Running with no authorization due empty auth.authFilePath");
			return false;
		}
				
		// else set up authorization	
		try {
			refreshFreqSeconds = authProps.getRefreshFreqSeconds();
			authFilePath = authProps.getSettingsFilePath();
			updateAuthorization();
			
		} catch (Exception e) {
			authSetupFailed = true;
			authFilePath = null;
			throw new AuthorizationException("Authorization setup failed", e);
		}
		return true;
	}
	
	/** 
	 * Exposed for testing.
	 * @return - never null
	 */
	public static ArrayList<String> getJobAdmins() {
		if (userGroups.containsKey(JOB_ADMIN_GROUP)) {
			return userGroups.get(JOB_ADMIN_GROUP);
		} else {
			return new ArrayList<String>();
		}
	}
	
	/**
	 * Update authorization values from SPARQL endpoint iff 
	 *  - authorization has been turned on by authorize()
	 *  - refreshSeconds has passed since last update
	 * 
     * Eats all exceptions and clears permissions
	 */
	private static void updateAuthorization() throws AuthorizationException {
		
		// return if we're not using authorization
		if (authFilePath == null) return;
		
		// check whether it's too soon to re-read the auth file
		long now = Calendar.getInstance().getTime().getTime();
		
		if (now - lastUpdate >= refreshFreqSeconds * 1000  ) {
		
			LocalLogger.logToStdOut("Authorization Manager: refreshing authorization.");
			lastUpdate = now;
			
			try {
				JSONObject authJson = Utility.getJSONObjectFromFilePath(authFilePath);
				updateUserGroups(authJson);
				updateGraphAuthorization(authJson);
				
			} catch (Exception e) {
				String path = authFilePath;
				clear();
				throw new AuthorizationException("Error reading authorization file: " + path, e);
			} 
		}
	}
	
	
	/**
	 * Read user groups from triplestore
	 * @param sei
	 * @throws Exception
	 */
	private static void updateUserGroups(JSONObject authJson) throws Exception {
		userGroups.clear();
		
		JSONArray groups = (JSONArray) authJson.get("groups");
		for (int i=0; i < groups.size(); i++) {
			JSONObject group = (JSONObject) groups.get(i);
			
			JSONArray members = (JSONArray) group.get("members");
			String groupName = (String) group.get("name");
			
			ArrayList<String> userList = new ArrayList<String>();
			for (int j=0; j < members.size(); j++) {
				userList.add((String)members.get(j));
			}
			
			userGroups.put(groupName, userList);
		}
	}
	
	/**
	 * Read graph admin from triplestore
	 * @param sei
	 * @throws Exception
	 */
	private static void updateGraphAuthorization(JSONObject authJson) throws Exception {
		graphReaders.clear();
		graphWriters.clear();
		
		JSONArray graphs = (JSONArray) authJson.get("graphs");
		for (int i=0; i < graphs.size(); i++) {
			JSONObject graph = (JSONObject) graphs.get(i);
			
			String graphName = (String) graph.get("name");
			JSONArray readers = (JSONArray) graph.get("readGroups");
			JSONArray writers = (JSONArray) graph.get("writeGroups");

			// read groups
			ArrayList<String> readGroups = new ArrayList<String>();
			
			for (int j=0; j < readers.size(); j++) {
				readGroups.add((String)readers.get(j));
			}
			graphReaders.put(graphName, readGroups);
			
			// read groups
			ArrayList<String> writeGroups = new ArrayList<String>();
			
			for (int j=0; j < writers.size(); j++) {
				writeGroups.add((String)writers.get(j));
			}
			graphWriters.put(graphName, writeGroups);
		}
	}
	
	public static void throwExceptionIfNotGraphReader(String graphName, String userName) throws AuthorizationException {
		if (graphReaders.containsKey(graphName)) {
			ArrayList<String> groups = graphReaders.get(graphName);
			for (String group : groups) {
				if (graphReaders.get(group).contains(userName)) {
					return;
				}
			}
		}
		throw new AuthorizationException("User " + userName + " does not have permission to read graph " + graphName);
	}
	
	public static void throwExceptionIfNotGraphWriter(String graphName, String userName) throws AuthorizationException {
		if (graphWriters.containsKey(graphName)) {
			ArrayList<String> groups = graphWriters.get(graphName);
			for (String group : groups) {
				if (graphWriters.get(group).contains(userName)) {
					return;
				}
			}
		}
		throw new AuthorizationException("User " + userName + " does not have permission to write to graph " + graphName);
	}
	
	/**
	 * check if this thread's user owns an item with:
	 * @param owner - user_name of item to be checked
	 * @param itemName - name of item to be checked
	 * @throws AuthorizationException - if not
	 * @throws Exception - if there's trouble reading auth info from triplestore
	 */
	public static void throwExceptionIfNotJobOwner(String owner, String itemName) throws AuthorizationException, Exception {
		updateAuthorization();
		String user = ThreadAuthenticator.getThreadUserName();
		final boolean FORGIVE_ALL = false;
		try {
		
			// is user_name equal, or thread is a job admin
			if (!threadIsJobAdmin() && !user.equals(owner)) {
				throw new AuthorizationException("Permission denied on thread" + Thread.currentThread().getName() + ": " + user + " may not access " + itemName + " owned by " + owner);
			}
			
		} catch (AuthorizationException e) {
			if (FORGIVE_ALL) {
				LocalLogger.logToStdErr("FORGIVING EXCEPTION DURING DEBUGGING: ");
				LocalLogger.printStackTrace(e);
			} else {
				throw e;
			}
		}
	}
		
	/**
	 * Throw exception if thread is not job admin
	 * note: uses up the one check of ThreadAuthenticator.isAdmin()
	 * @throws AuthorizationException
	 */
	public static void throwExceptionIfNotJobAdmin() throws AuthorizationException {
		if (!threadIsJobAdmin()) {
			throw new AuthorizationException("Function may only be performed by job admin.");
		}
	}
	
	/**
	 * Is this thread ADMIN or a job admin
	 * note: uses up the one check of ThreadAuthenticator.isAdmin()
	 * @return
	 */
	public static boolean threadIsJobAdmin() {
		if (ThreadAuthenticator.isJobAdmin()) {
			return true;
		} else {
			String user = ThreadAuthenticator.getThreadUserName();
			return (userGroups.containsKey(JOB_ADMIN_GROUP) && userGroups.get(JOB_ADMIN_GROUP).contains(user));
		}
	}
	
	//
	// NOTES
	//	INSERT requires WHERE clause.   Perhaps INSERT DATA does not.
	//  	https://stackoverflow.com/questions/39620846/sparql-expecting-one-of-where-using
	//
	//
	//
	//
	//
	
	public static Pattern REGEX_SERVICE = Pattern.compile("\\sservice\\s*[<?]", Pattern.CASE_INSENSITIVE);
	public static Pattern REGEX_FROM = Pattern.compile("\\sfrom\\s*[<?]", Pattern.CASE_INSENSITIVE);
	public static Pattern REGEX_INTO = Pattern.compile("\\sinto\\s*[<?]", Pattern.CASE_INSENSITIVE);

	private static void logAuthDebug(String msg) {
		LocalLogger.logToStdOut("AUTH_DEBUG " + msg);
	}
	/**
	 * Check if query is authorized 
	 * CORRENTLY JUST IN TEST: ONLY LOGS INFORMATION
	 * @param query
	 * @throws AuthorizationException
	 */
	public static void authorizeQuery(SparqlEndpointInterface sei, String queryStr) throws AuthorizationException {
		try {
			// is authorization turned off or suffering a setup failure
			if (authSetupFailed) {
				throw new AuthorizationException("Authorization failed due to setup failure.  See log entries at startup.");
			} else if (sei == null) {
				// Authorization is turned off.
				return;
			}
			
			// get latest auth info
			updateAuthorization();
			
			// start authorizing the query
		
			StringBuilder logMessage = new StringBuilder();
			ArrayList<String> graphURIs = null;
			boolean readOnlyFlag = false;
			long startTime = System.nanoTime();
			String user = ThreadAuthenticator.getThreadUserName();
			
			// log the first half
	        logAuthDebug("Query:    " + queryStr.replaceAll("\n", "\nAUTH_DEBUG "));
	        logAuthDebug("User:     " + user);
	
			SparqlQueryInterrogator sqi = new SparqlQueryInterrogator(queryStr);
			readOnlyFlag = sqi.isReadOnly();
			graphURIs = sqi.getGraphNames();
			if (! graphURIs.contains(sei.getDataset()) ) {
				graphURIs.add(sei.getDataset());
			}
	        
			
			// log the second half
			logAuthDebug("Graphs:   " + graphURIs                                       );
			logAuthDebug("Endpoint: " + sei.getServerAndPort() + " " + sei.getDataset() );
			logAuthDebug("Type:     " + (readOnlyFlag ? "read" : "write")               );
			logAuthDebug("Time:     " + (System.nanoTime() - startTime) / 1000000 + " msec\n");
	        	        
	        // do the actual authorization
	        for (String graphURI : graphURIs) {
				if (readOnlyFlag) {
					if (graphReaders.containsKey(graphURI)) {
						if (!graphReaders.get(graphURI).contains(user)) {
							throw new AuthorizationException("User \'" + user + "' does not have read permission on '" + graphURI + "'");
						} else {
							logAuthDebug("Access granted");
						}
					} else {
						logAuthDebug("Grpah is not in graphReaders: default access granted");
						//  allowing read access if graphURI isn't in the auth table
					}
				} else {
					if (graphWriters.containsKey(graphURI)) {
						if (!graphWriters.get(graphURI).contains(user)) {
							throw new AuthorizationException("User \'" + user + "' does not have write permission on '" + graphURI + "'");
						}
					} else {
						logAuthDebug("Graph is not in graphWriters: default access granted");
						//  allowing write access if graphURI isn't in the auth table
					}
				}
			}
		} catch (AuthorizationException ae) {
			LocalLogger.logToStdErr(ae.getMessage());
			LocalLogger.logToStdOut("Forgiving AuthorizationException");
		}

	}
	
}
