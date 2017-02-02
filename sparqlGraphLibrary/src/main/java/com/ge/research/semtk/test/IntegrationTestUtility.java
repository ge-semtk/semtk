package com.ge.research.semtk.test;

import com.ge.research.semtk.utility.Utility;

/**
 * Utility class for configuring integration tests.
 * 
 * NOTE: This class cannot be put in under src/test/java because it must remain accessible to other projects.
 */
public class IntegrationTestUtility {
	
	// property file with integration test configurations
	public static final String INTEGRATION_TEST_PROPERTY_FILE = "src/test/resources/integrationtest.properties";

	
	// protocol for all services
	public static String getServiceProtocol() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.protocol");
	}
	
	// sparql endpoint
	public static String getSparqlServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.sparqlServer");
	}
	public static String getSparqlServerType() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.sparqlServerType");
	}
	public static String getSparqlServerUsername() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.sparqlServerUsername");
	}
	public static String getSparqlServerPassword() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.sparqlServerPassword");
	}
	
	// sparql query service
	public static String getSparqlQueryServiceServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.sparqlqueryservice.server");
	}
	public static int getSparqlQueryServicePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.sparqlqueryservice.port")).intValue();	
	}	
	
	// status service
	public static String getStatusServiceServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.statusservice.server");
	}
	public static int getStatusServicePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.statusservice.port")).intValue();
	}
	
	// results service
	public static String getResultsServiceServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.resultsservice.server");
	}
	public static int getResultsServicePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.resultsservice.port")).intValue();
	}
	
}
