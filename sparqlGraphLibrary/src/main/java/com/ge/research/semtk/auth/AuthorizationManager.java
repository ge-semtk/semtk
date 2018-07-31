package com.ge.research.semtk.auth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.edc.JobEndpointProperties;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.Utility;

public class AuthorizationManager {
	
	private static JobEndpointProperties endpointProps = null;
	private static AuthorizationProperties authProps = null;
	private static long lastUpdate = 0;
	
	// list of usernames that can access other users' jobs
	private static ArrayList<String> jobAdmins = new ArrayList<String>();

	/**
	 * Turn on Authorization by providing a SPARQL endpoint and frequency
	 * @param jeProps
	 * @param authorizProps
	 * @throws Exception
	 */
	public static void authorize(JobEndpointProperties jeProps, AuthorizationProperties authorizProps) throws Exception {
		AuthorizationManager.endpointProps = jeProps;
		AuthorizationManager.authProps = authorizProps;
		updateAuthorization();
	}
	
	/**
	 * Update authorization values from SPARQL endpoint iff authorization has been turned on
	 * @throws Exception
	 */
	private static void updateAuthorization() throws Exception {
		// bail if authorize() has not been called
		if (endpointProps == null) return;
		
		long now = Calendar.getInstance().getTime().getTime();
		
		// return if authorization is up-to-date
		if (now - lastUpdate < authProps.getRefreshFreqSeconds() * 1000  ) {
			return;
		}
		
		// update
		lastUpdate = now;
		SparqlEndpointInterface sei = SparqlEndpointInterface.getInstance(
				endpointProps.getJobEndpointType(),
				endpointProps.getJobEndpointServerUrl(), 
				endpointProps.getJobEndpointDataset(),
				endpointProps.getJobEndpointUsername(),
				endpointProps.getJobEndpointPassword());
		
		updateJobAdmins(sei);
	}
	
	private static void updateJobAdmins(SparqlEndpointInterface sei) throws Exception {
		jobAdmins.clear();
		
		// read nodegroup and query
		SparqlGraphJson sgjAuthJobs = new SparqlGraphJson(Utility.getResourceAsJson(new AuthorizationManager(), "/nodegroups/auth_jobs.json"));
		Table jobAdminTable = sei.executeQueryToTable(sgjAuthJobs.getNodeGroup().generateSparqlSelect());
		
		// save into jobAdmins
		for (String user : jobAdminTable.getColumn(0)) {
			jobAdmins.add(user);
		}
	}
	
	public static void checkJobOwnership(String owner, String itemName) throws AuthorizationException {
		String user = ThreadAuthenticator.getThreadUserName();
		if (!user.equals(owner) && !jobAdmins.contains(user) && !ThreadAuthenticator.isAdmin()) {
			throw new AuthorizationException("Permission denied: " + user + " may not access " + itemName + " owned by " + owner);
		}
	}
}
