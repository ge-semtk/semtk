package com.ge.research.semtk.auth;

import java.util.ArrayList;
import java.util.Calendar;

import com.ge.research.semtk.edc.EndpointProperties;
import com.ge.research.semtk.edc.JobEndpointProperties;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
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
	public static final String ADMIN = "admin";
	
	private static final String GRAPH_NAME = "http://research.ge.com/semtk/services";
	private static EndpointProperties endpointProps = null;
	private static AuthorizationProperties authProps = null;
	private static long lastUpdate = 0;

	// list of usernames that can access jobs
	// in addition to ADMIN and the job owner
	private static ArrayList<String> jobAdmins = new ArrayList<String>();

	/**
	 * Turn on Authorization by providing a SPARQL endpoint and frequency
	 * @param jeProps
	 * @param authorizProps
	 * @throws Exception
	 */
	public static void authorize(EndpointProperties jeProps, AuthorizationProperties authorizProps) {
		AuthorizationManager.endpointProps = jeProps;
		AuthorizationManager.authProps = authorizProps;
		updateAuthorization();
	}
	
	/** 
	 * Exposed for testing
	 * @return
	 */
	public static ArrayList<String> getJobAdmins() {
		return jobAdmins;
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
		if (endpointProps == null) return;
		
		long now = Calendar.getInstance().getTime().getTime();
		
		// return if authorization is up-to-date
		if (now - lastUpdate < authProps.getRefreshFreqSeconds() * 1000  ) {
			return;
		}
		
		LocalLogger.logToStdOut("Authorization Manager: refreshing authorization.");
		
		try {
			// update
			lastUpdate = now;
			SparqlEndpointInterface sei = SparqlEndpointInterface.getInstance(
					endpointProps.getJobEndpointType(),
					endpointProps.getJobEndpointServerUrl(), 
					GRAPH_NAME,
					endpointProps.getJobEndpointUsername(),
					endpointProps.getJobEndpointPassword());
			
			updateJobAdmins(sei);
			
		} catch (Exception e) {
			// Some kind of exception occurred while reading authorization.
			// Clear all permissions.
			LocalLogger.printStackTrace(e);
			jobAdmins.clear();
		}
	}
	
	/**
	 * Read job admins from triplestore
	 * @param sei
	 * @throws Exception
	 */
	private static void updateJobAdmins(SparqlEndpointInterface sei) throws Exception {
		jobAdmins.clear();
		
		// read nodegroup and query
		SparqlGraphJson sgjAuthJobs = new SparqlGraphJson(Utility.getResourceAsJson(new AuthorizationManager(), "/nodegroups/auth_job_admin.json"));
		Table jobAdminTable = sei.executeQueryToTable(sgjAuthJobs.getNodeGroup().generateSparqlSelect());
		
		// save into jobAdmins
		for (String user : jobAdminTable.getColumn("name")) {
			jobAdmins.add(user);
		}
	}
	
	/**
	 * check if this thread's user owns an item with:
	 * @param owner - user_name of item to be checked
	 * @param itemName - name of item to be checked
	 * @throws AuthorizationException - if not
	 * @throws Exception - if there's trouble reading auth info from triplestore
	 */
	public static void throwExceptionIfNotJobOwner(String owner, String itemName) throws AuthorizationException {
		updateAuthorization();
		String user = ThreadAuthenticator.getThreadUserName();
		
		// is user_name equal, or thread is a job admin
		if (!threadIsJobAdmin() && !user.equals(owner)) {
			throw new AuthorizationException("Permission denied: " + user + " may not access " + itemName + " owned by " + owner);
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
			return (jobAdmins.contains(user));
		}
	}
}
