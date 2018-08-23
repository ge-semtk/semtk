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

import com.ge.research.semtk.edc.EndpointProperties;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
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
	private static final String USER_GROUPS_NODEGROUP = "/nodegroups/auth_user_groups.json";
	private static final String GRAPH_NODEGROUP = "/nodegroups/auth_graph.json";
	private static final String DOMAIN = "http://research.ge.com/semtk/authorization";

	private static SparqlEndpointInterface sei = null;
	private static int refreshFreqSeconds = 301;
	
	// userGroup.get(name) = ArrayList of user names
	private static HashMap<String, ArrayList<String>> userGroups = new HashMap<String, ArrayList<String>>();
	
	// graphXers(graph_name) = ArrayList of group names
	private static HashMap<String, ArrayList<String>> graphReaders = new HashMap<String, ArrayList<String>>();
	private static HashMap<String, ArrayList<String>> graphWriters = new HashMap<String, ArrayList<String>>();


	/**
	 * Turn on Authorization by providing a SPARQL endpoint and frequency
	 * @param jeProps
	 * @param authorizProps
	 * @throws Exception
	 */
	public static void authorize(EndpointProperties endpointProps, AuthorizationProperties authProps) {
		refreshFreqSeconds = authProps.getRefreshFreqSeconds();
		
		try {
			sei = SparqlEndpointInterface.getInstance(
					endpointProps.getJobEndpointType(),
					endpointProps.getJobEndpointServerUrl(), 
					authProps.getGraphName(),
					endpointProps.getJobEndpointUsername(),
					endpointProps.getJobEndpointPassword());
			
			updateAuthorization();
			
		} catch (Exception e) {
			LocalLogger.logToStdErr("Failed to load authorization info");
			LocalLogger.printStackTrace(e);
		}
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
	private static void updateAuthorization() {
		// bail if authorize() has not been called
		if (sei == null) return;
		
		long now = Calendar.getInstance().getTime().getTime();
		
		// return if authorization is up-to-date
		if (now - lastUpdate < refreshFreqSeconds * 1000  ) {
			return;
		}
		
		LocalLogger.logToStdOut("Authorization Manager: refreshing authorization.");
		lastUpdate = now;
		
		refresh();
	}
	
	/**
	 * Force refresh of Authorization info from triplestore.
	 */
	public static void refresh() {
		try {
			// update
			updateUserGroups(sei);
			updateGraphAuthorization(sei);
			
		} catch (Exception e) {
			clear();
			LocalLogger.logToStdErr("Failure reading authorization data");
			LocalLogger.printStackTrace(e);;
		}
	}
	
	
	/**
	 * clear local cache of Authorization
	 */
	private static void clear() {
		userGroups.clear();
		graphReaders.clear();
		graphWriters.clear();
	}
	
	/**
	 * Read user groups from triplestore
	 * @param sei
	 * @throws Exception
	 */
	private static void updateUserGroups(SparqlEndpointInterface sei) throws Exception {
		userGroups.clear();
		
		// read nodegroup and query
		SparqlGraphJson sgjAuthJobs = new SparqlGraphJson(Utility.getResourceAsJson(new AuthorizationManager(), USER_GROUPS_NODEGROUP));
		Table userGroupsTable = sei.executeQueryToTable(sgjAuthJobs.getNodeGroup().generateSparqlSelect());
		
		// loop through table
		for (int i=0; i < userGroupsTable.getNumRows(); i++) {
			String group = userGroupsTable.getCell(i, "groupName");
			String user = userGroupsTable.getCell(i, "userName");
			
			// create new group userList or find the hashed one
			ArrayList<String> userList = null;
			if (userGroups.containsKey(group)) {
				userList = userGroups.get(group);
			} else {
				userList = new ArrayList<String>();
				userGroups.put(group, userList);
			}
			
			// add the user
			if (!userList.contains(user)) {
				userList.add(user);
			}
		}
	}
	
	/**
	 * Read graph admin from triplestore
	 * @param sei
	 * @throws Exception
	 */
	private static void updateGraphAuthorization(SparqlEndpointInterface sei) throws Exception {
		graphReaders.clear();
		graphWriters.clear();
		
		// read nodegroup and query
		SparqlGraphJson sgjGraphAdmin = new SparqlGraphJson(Utility.getResourceAsJson(new AuthorizationManager(), GRAPH_NODEGROUP));
		Table graphAdminTable = sei.executeQueryToTable(sgjGraphAdmin.getNodeGroup().generateSparqlSelect());
		
		// loop through table
		for (int i=0; i < graphAdminTable.getNumRows(); i++) {
			String graph = graphAdminTable.getCell(i, "graphName");
			String readGroup = graphAdminTable.getCell(i, "readGroupName");
			String writeGroup = graphAdminTable.getCell(i, "writeGroupName");
			
			// add readGroupName to graphReaders
			if (readGroup != null && ! readGroup.isEmpty()) {
				ArrayList<String> groupList = null;
				if (graphReaders.containsKey(graph)) {
					groupList = graphReaders.get(graph);
				} else {
					groupList = new ArrayList<String>();
					graphReaders.put(graph, groupList);
				}
				
				// add the user
				if (!groupList.contains(readGroup)) {
					groupList.add(readGroup);
				}
			}
			
			// add writeGroupName to graphWriters
			if (writeGroup != null && ! writeGroup.isEmpty()) {
				ArrayList<String> groupList = null;
				if (graphWriters.containsKey(graph)) {
					groupList = graphWriters.get(graph);
				} else {
					groupList = new ArrayList<String>();
					graphWriters.put(graph, groupList);
				}
				
				// add the user
				if (!groupList.contains(writeGroup)) {
					groupList.add(writeGroup);
				}
			}
		}
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
		
//		try {
		
			// is user_name equal, or thread is a job admin
			if (!threadIsJobAdmin() && !user.equals(owner)) {
				throw new AuthorizationException("Permission denied: " + user + " may not access " + itemName + " owned by " + owner);
			}
			
//		} catch (AuthorizationException e) {
//			LocalLogger.logToStdErr("FORGIVING EXCEPTION DURING DEBUGGING: ");
//			LocalLogger.printStackTrace(e);
//		}
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
	
	public static boolean isAuthorizationOwlLoaded() throws Exception {

		OntologyInfo oInfo = new OntologyInfo(getConnection());
		return oInfo.getNumberOfClasses() > 0;
	}
	
	private static SparqlConnection getConnection() {
		SparqlConnection conn = new SparqlConnection();
		conn.setDomain(DOMAIN);
		conn.addModelInterface(sei);
		conn.addDataInterface(sei);
		return conn;
	}
	public static void uploadAuthorizationOwl(String owl) throws Exception {
		
		SimpleResultSet resultSet = SimpleResultSet.fromJson(sei.executeAuthUploadOwl(owl.getBytes()));
		if (!resultSet.getSuccess()) {
			throw new Exception(resultSet.getRationaleAsString(" "));
		}
	}
	
	public static void ingestUserGroups(CSVDataset ds) throws Exception {
		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getResourceAsJson(new AuthorizationManager(), USER_GROUPS_NODEGROUP)); 
		sgJson.setSparqlConn(getConnection());
		// load the data
		DataLoader dl = new DataLoader(sgJson, 2, ds, sei.getUserName(), sei.getPassword());
		String err = dl.importDataGetBriefError(true);
		if (err != null) {
			LocalLogger.logToStdErr(err);
			throw new Exception("ingestion failed");
		}
	}
	
	public static void ingestGraphs(CSVDataset ds) throws Exception {
		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getResourceAsJson(new AuthorizationManager(), GRAPH_NODEGROUP)); 
		sgJson.setSparqlConn(getConnection());
		// load the data
		DataLoader dl = new DataLoader(sgJson, 2, ds, sei.getUserName(), sei.getPassword());
		String err = dl.importDataGetBriefError(true);
		if (err != null) {
			LocalLogger.logToStdErr(err);
			throw new Exception("ingestion failed");
		}
	}
	
}
