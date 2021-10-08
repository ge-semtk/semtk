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
import java.util.List;

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

	private static final String AUTH_UNSET_MESSAGE="Authorization setup failed: auth.authFilePath must be set to an auth_file.json, or NO_AUTH";
	public static String AUTH_FILE_NO_AUTH = "NO_AUTH";
	private static String AUTH_FILE_UNSET = "unset";
	private static String DEFAULT_GROUP = "ALL_USERS";
	private static String DEFAULT_GRAPH = "DEFAULT";

	private static int refreshFreqSeconds = 301;
	private static String authFilePath = AUTH_FILE_UNSET;
	private static boolean nextQuerySuper = false;
	private static boolean modeSuper = false;

	// userGroup.get(name) = ArrayList of user names
	private static HashMap<String, ArrayList<String>> userGroups = new HashMap<String, ArrayList<String>>();

	// graphXers(graph_name) = ArrayList of group names
	private static HashMap<String, ArrayList<String>> graphReaders = new HashMap<String, ArrayList<String>>();
	private static HashMap<String, ArrayList<String>> graphWriters = new HashMap<String, ArrayList<String>>();
	
	private static HashMap<String, ArrayList<String>> graphIDMReaders = new HashMap<String, ArrayList<String>>();
	private static HashMap<String, ArrayList<String>> graphIDMWriters = new HashMap<String, ArrayList<String>>();

	public static void clear() {
		lastUpdate = 0;
		refreshFreqSeconds = 301;
		authFilePath = AUTH_FILE_UNSET;
		AuthorizationException.setAuthLogPath(null);
		userGroups.clear();
		graphReaders.clear();
		graphWriters.clear();
		nextQuerySuper = false;
		modeSuper = false;
	}

	/**
	 * Turn on Authorization by providing a SPARQL endpoint and frequency.
	 * Called at service start-up
	 * 
	 * 
	 *  // Typical usage:  process should die if this fails
	 *	try {
	 *		AuthorizationManager.authorize(auth_prop);
	 *	} catch (Exception e) {
	 *		e.printStackTrace();
	 *		System.exit(1);
	 *	}
	 *
	 * Having your service call this guarantees that it will use authorization or die.
	 * Once this is activated, all calls to modify a triplestore will be authenticated and authorized.
	 * 
	 * @param endpointProps - location of the semtk services
	 * @param authProps - authorization properties
	 * @throws AuthorizationException
	 * @returns boolean - was authorization applied
	 */
	public static boolean authorize(AuthorizationProperties authProps) throws AuthorizationException {
		clear();

		// Is Authorization turned off properly
		if ( authProps.getSettingsFilePath().equals("NO_AUTH") ) {
			LocalLogger.logToStdErr("NOTICE: Running with no authorization auth.authFilePath=NO_AUTH");
			authFilePath = AUTH_FILE_NO_AUTH;
			return false;
		}

		// else set up authorization	
		try {
			if ( authProps.getSettingsFilePath().isEmpty() ) {
				authFilePath = AUTH_FILE_UNSET;
				throw new AuthorizationException(AUTH_UNSET_MESSAGE);
			}

			refreshFreqSeconds = authProps.getRefreshFreqSeconds();
			authFilePath = authProps.getSettingsFilePath();
			ThreadAuthenticator.setUsernameKey(authProps.getUsernameKey());
			ThreadAuthenticator.setGroupKey(authProps.getGroupKey());
			AuthorizationException.setAuthLogPath( authProps.getLogPath() );
			updateAuthorization();

		} catch (Exception e) {
			clear();
			authFilePath = null;
			throw new AuthorizationException("Authorization setup failed", e);
		}
		return true;
	}

	public static void authorizeWithExit(AuthorizationProperties authProps) {
		try {
			authorize(authProps);
		} catch (Exception e) {
			LocalLogger.printStackTrace(e);
			System.exit(1);;
		}
	}

	// return if authorize() was never called, or if it was called with 'NO_AUTH'
	// This means legacy users or others using just semTK libraries
	// don't need to know about security.
	// All "offical" services have authorize() in their start-ups
	// so they will either properly authorize or exit.
	private static boolean authProperlyDisabled() {

		return authFilePath.equals(AUTH_FILE_NO_AUTH) || authFilePath.equals(AUTH_FILE_UNSET) ;
	}
	
	// for tests
	public static boolean authFileDisabled(String filePath) {
		return filePath == null || filePath.isEmpty() || filePath.equals(AUTH_FILE_NO_AUTH) || filePath.equals(AUTH_FILE_UNSET) ;
	}

	/**
	 * Call this before performing any authorization task.
	 * 
	 * 1) Make sure that authorization has been set up
	 * 
	 * 2) Update authorization values from JSON file iff 
	 *  - authorization has been turned on by authorize()
	 *  - refreshSeconds has passed since last update
	 * 
	 * Eats all exceptions and clears permissions
	 */
	private static void updateAuthorization() throws AuthorizationException {

		// return if we're not using authorization
		if (authProperlyDisabled()) return;

		// check whether it's too soon to re-read the auth file
		long now = Calendar.getInstance().getTime().getTime();

		if ((now - lastUpdate) >= (refreshFreqSeconds * 1000)  ) {

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
	 * Read user groups from json
	 * @param querySei
	 * @throws Exception
	 */
	private static void updateUserGroups(JSONObject authJson) throws Exception {
		userGroups.clear();

		JSONArray groups = (JSONArray) authJson.get("groups");
		if (groups != null) {
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
	}

	/**
	 * Read graph admin from json
	 * @param querySei
	 * @throws Exception
	 */
	private static void updateGraphAuthorization(JSONObject authJson) throws Exception {
		graphReaders.clear();
		graphWriters.clear();

		JSONArray graphs = (JSONArray) authJson.get("graphs");
		for (int i=0; i < graphs.size(); i++) {
			JSONObject graph = (JSONObject) graphs.get(i);

			String graphName = (String) graph.get("name");
			
			// read groups
			JSONArray readersJson = (JSONArray) graph.get("readGroups");
			ArrayList<String> readGroups = new ArrayList<String>();

			if (readersJson != null) {
				for (int j=0; j < readersJson.size(); j++) {
					String groupName = (String)readersJson.get(j);
					if ( ! userGroups.containsKey(groupName) && !groupName.equals(DEFAULT_GROUP)) {
						throw new AuthorizationException("Authorization setup failed.  Unknown group: " + groupName);
					}
					readGroups.add(groupName);
				}
			}
			graphReaders.put(graphName, readGroups);

			// write groups
			JSONArray writersJson = (JSONArray) graph.get("writeGroups");
			ArrayList<String> writeGroups = new ArrayList<String>();

			if (writersJson != null) {
				for (int j=0; j < writersJson.size(); j++) {
					String groupName = (String)writersJson.get(j);
					if ( ! userGroups.containsKey(groupName) && !groupName.equals(DEFAULT_GROUP)) {
						throw new AuthorizationException("Authorization setup failed.  Unknown group: " + groupName);
					}
					writeGroups.add(groupName);
				}
			}
			graphWriters.put(graphName, writeGroups);
			
			// IDM writers
			JSONArray writersIDMJson = (JSONArray) graph.get("writeIDMGroups");			
			ArrayList<String> writeIDMGroups = new ArrayList<String>();
			
			if (writersIDMJson != null) {
				for (int j=0; j < writersIDMJson.size(); j++) {
					String groupName = (String)writersIDMJson.get(j);
					writeIDMGroups.add(groupName);
				}
			}
			graphIDMWriters.put(graphName, writeIDMGroups);
			
			// IDM readers
			JSONArray readersIDMJson = (JSONArray) graph.get("readIDMGroups");			
			ArrayList<String> readIDMGroups = new ArrayList<String>();

			if (readersIDMJson != null) {
				for (int j=0; j < readersIDMJson.size(); j++) {
					String groupName = (String)readersIDMJson.get(j);
					readIDMGroups.add(groupName);
				}
			}
			graphIDMReaders.put(graphName, readIDMGroups);
			
		}
	}

	/**
	 * check if this thread's user owns an item with:
	 * 
	 * NOTE: this works even if auth is turned off or failed.
	 * 
	 * @param owner - user_name of item to be checked
	 * @param itemName - name of item to be checked
	 * @throws AuthorizationException - if not
	 * @throws Exception - if there's trouble reading auth info from triplestore
	 */
	public static void throwExceptionIfNotJobOwner(String owner, String itemName) throws AuthorizationException, Exception {

		String user = ThreadAuthenticator.getThreadUserName();


		if (authProperlyDisabled() || isSemtkSuper()) return;
		updateAuthorization();

		// is user_name equal, or thread is a super user
		if (!user.equals(owner) && ! isSemtkSuper()) {
			throw new AuthorizationException("Permission denied on thread" + Thread.currentThread().getName() + ": " + user + " may not access " + itemName + " owned by " + owner);
		}


	}

	/**
	 * Run the next query as semtk-super-user.
	 * Authorization will pass no matter what.
	 * 
	 * Note: unlike authentication, this doesn't pass along through RestClients or stay in a thread.
	 *       It is much more confined.
	 *       If semtk a service needs to make a super-user query, 
	 *       it must use a SparqlEndpointInterface and call this function before the query.
	 */
	public static void nextQuerySemtkSuper() {
		nextQuerySuper = true;
	}

	public static void setSemtkSuper() {
		modeSuper = true;
	}

	public static void clearSemtkSuper() {
		modeSuper = false;
		nextQuerySuper = false;
	}

	public static boolean isSemtkSuper() {
		return nextQuerySuper || modeSuper;
	}
	/**
	 * Check if query is authorized 
	 * @param query
	 * @throws AuthorizationException
	 */
	public static void authorizeQuery(SparqlEndpointInterface sei, String queryStr) throws AuthorizationException {

		// handle semtk-super-user query
		if (nextQuerySuper) {
			nextQuerySuper = false;   // used up one authorization
			return;
		}
		if (modeSuper) {
			return;
		}

		// get latest auth info
		if (authProperlyDisabled()) return;

		// start authorizing the query
		ArrayList<String> graphURIs = null;
		boolean readOnlyFlag = false;
		long startTime = System.nanoTime();
		String user = ThreadAuthenticator.getThreadUserName();

		// log the first half
		AuthorizationException.logAuthEvent("Query:    " + queryStr);
		AuthorizationException.logAuthEvent("User:     " + user);

		SparqlQueryInterrogator sqi = new SparqlQueryInterrogator(queryStr);
		readOnlyFlag = sqi.isReadOnly();
		graphURIs = sqi.getGraphNames();
		if (! graphURIs.contains(sei.getGraph()) ) {
			graphURIs.add(sei.getGraph());
		}

		// log the second half
		AuthorizationException.logAuthEvent("Graphs:   " + graphURIs                                       );
		AuthorizationException.logAuthEvent("Endpoint: " + sei.getServerAndPort() + " " + sei.getGraph() );
		AuthorizationException.logAuthEvent("Type:     " + (readOnlyFlag ? "read" : "write")               );
		AuthorizationException.logAuthEvent("Time:     " + (System.nanoTime() - startTime) / 1000000 + " msec\n");

		// do the actual authorization
		for (String graphURI : graphURIs) {
			if (readOnlyFlag) {
				throwExceptionIfNotGraphReader(graphURI);					
			} else {
				throwExceptionIfNotGraphReader(graphURI);
			}
		}

	}

	/**
	 * Focal point for all read authorization.
	 * Note that user is retrieved from ThreadAuthenticator
	 * @param graphName
	 * @throws AuthorizationException
	 */
	public static void throwExceptionIfNotGraphReader(String graphName) throws AuthorizationException {

		if (authProperlyDisabled() || isSemtkSuper()) return;
		updateAuthorization();

		String threadUser = ThreadAuthenticator.getThreadUserName();
		List<String> threadIDMGroups = ThreadAuthenticator.getThreadGroups();
		
		// get read groups
		ArrayList<String> graphGroups = null;
		if (graphReaders.containsKey(graphName)) {
			graphGroups = graphReaders.get(graphName);
		} else if (graphReaders.containsKey(DEFAULT_GRAPH)) {
			graphGroups = graphReaders.get(DEFAULT_GRAPH);
		} else {
			graphGroups = new ArrayList<String>();
		}

		// does thread user belong to one of the graph groups
		for (String graphGroup : graphGroups) {
			List<String> members = userGroups.get(graphGroup);
			if (graphGroup.equals(DEFAULT_GROUP) || members != null && members.contains(threadUser)) {
				AuthorizationException.logAuthEvent("User " + threadUser + " granted read permission on graph " + graphName);
				return;
			}
		}
		
		// get IDM read groups for this graph
		ArrayList<String> graphIDMGroups = null;
		if (graphIDMReaders.containsKey(graphName)) {
			graphIDMGroups = graphIDMReaders.get(graphName);
		} else if (graphIDMReaders.containsKey(DEFAULT_GRAPH)) {
			graphIDMGroups = graphIDMReaders.get(DEFAULT_GRAPH);
		} else {
			graphIDMGroups = new ArrayList<String>();
		}
				
		// does thread IDM group list contain any of the graph IDM groups
		for (String graphIDMGroup : graphIDMGroups) {
			if (threadIDMGroups.contains(graphIDMGroup)) {
				AuthorizationException.logAuthEvent("User " + threadUser + " member of " + graphIDMGroup + " granted read permission on graph " + graphName);
				return;
			}
		}
			
		throw new AuthorizationException("Read Access Denied.  graph=" + graphName + " user=" + threadUser);

	}

	/**
	 * Focal point for all write authorization.
	 * Note that user is retrieved from ThreadAuthenticator
	 * @param graphName
	 * @throws AuthorizationException
	 */
	public static void throwExceptionIfNotGraphWriter(String graphName) throws AuthorizationException {
		if (authProperlyDisabled() || isSemtkSuper()) return;
		updateAuthorization();

		String threadUser = ThreadAuthenticator.getThreadUserName();
		List<String> threadIDMGroups = ThreadAuthenticator.getThreadGroups();
		
		// get graph write groups
		ArrayList<String> graphWriteGroups = null;
		if (graphWriters.containsKey(graphName)) {
			graphWriteGroups = graphWriters.get(graphName);
		} else if (graphWriters.containsKey(DEFAULT_GRAPH)) {
			graphWriteGroups = graphWriters.get(DEFAULT_GRAPH);
		} else {
			graphWriteGroups = new ArrayList<String>();
		}

		// does thread user belong to one of the graph groups
		for (String graphGroup : graphWriteGroups) {
			List<String> members = userGroups.get(graphGroup);
			if (graphGroup.equals(DEFAULT_GROUP) || members != null && members.contains(threadUser)) {
				AuthorizationException.logAuthEvent("User " + threadUser + " granted write permission on graph " + graphName);
				return;
			}
		}
		
		// get IDM write groups for this graph
		ArrayList<String> graphIDMGroups = null;
		if (graphIDMWriters.containsKey(graphName)) {
			graphIDMGroups = graphIDMWriters.get(graphName);
		} else if (graphIDMWriters.containsKey(DEFAULT_GRAPH)) {
			graphIDMGroups = graphIDMWriters.get(DEFAULT_GRAPH);
		} else {
			graphIDMGroups = new ArrayList<String>();
		}

		// does thread IDM group list contain any of the graph IDM groups
		for (String graphIDMGroup : graphIDMGroups) {
			
			if (threadIDMGroups.contains(graphIDMGroup)) {
				AuthorizationException.logAuthEvent("User " + threadUser + " member of " + graphIDMGroup + " granted write permission on graph " + graphName);
				return;
			}
		}
		
		throw new AuthorizationException("Write Access Denied.  graph=" + graphName + " user=" + threadUser);


	}
}
