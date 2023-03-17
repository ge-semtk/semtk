package com.ge.research.semtk.servlet.utility;

import java.io.InputStream;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.Utility;

public class StartupUtilities {
	private static final int OINFO_SERVICE_TIMEOUT = 60000;
	private static final int TRIPLESTORE_TIMEOUT = 20000;
	
	/**
	 * Reads the owl file
	 * Checks the sei for the ontology that matches base, with version matching versionInfo
	 * If they don't match then clear the prefix and upload the owl.
	 * Tells the oinfo service to uncache the model.
	 * Wait for timeouts in case system is slow starting
	 * @param sei
	 * @param oInfoClient - to update cache.  Can be NULL if you're uploading owl that will never see nodegroups (aka jobTracker)
	 * @param c
	 * @param resourceName - must be file small enough to fit into memory
	 * @return - boolean: was anything uploaded
	 * @throws Exception
	 */
	public static boolean updateOwlIfNeeded(SparqlEndpointInterface sei, OntologyInfoClient oInfoClient, @SuppressWarnings("rawtypes") Class c, String resourceName ) throws Exception {
		// try to clear base and upload owl
		// for timeout sec
		long timeout = System.currentTimeMillis() + TRIPLESTORE_TIMEOUT;
		
		InputStream is = c.getResourceAsStream(resourceName);
		Utility.OwlRdfInfo info = Utility.getInfoFromOwlRdf(is);
		is.close();
		
		
		// check if correct version is already loaded
		String version = sei.getVersionOfOntologyLoaded(info.getBase());
		if (version == null || !version.equals(info.getVersion())) {
			
			// try til timeout or success to clear and upload correct version
			while (true) {
				try {
					AuthorizationManager.setSemtkSuper();
					if (version != null) {
						sei.clearPrefix(info.getBase());
					}
					is = c.getResourceAsStream(resourceName);
					sei.uploadOwl(c.getResourceAsStream(resourceName)); 
					is.close();
					//TODO junit this  
					break;
				} catch (Exception e) {
					if (System.currentTimeMillis() > timeout) {
						throw new Exception("Error uploading jobs owl model", e);
					}
				} finally {
					AuthorizationManager.clearSemtkSuper();
				}
			}
			
			
			// Try to uncache connection
			// for timeout sec
			timeout = System.currentTimeMillis() + (OINFO_SERVICE_TIMEOUT);
			while (oInfoClient != null) {
				try {
					oInfoClient.uncacheChangedConn(sei);
					break;
				} catch (Exception e) {
					if (System.currentTimeMillis() > timeout) {
						throw new Exception("Error uncaching connection", e);
					}
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
}
