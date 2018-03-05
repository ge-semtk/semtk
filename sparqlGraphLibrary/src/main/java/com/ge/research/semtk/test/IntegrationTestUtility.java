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

package com.ge.research.semtk.test;

import com.ge.research.semtk.api.nodeGroupExecution.NodeGroupExecutor;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.edc.client.StatusClientConfig;
import com.ge.research.semtk.load.client.IngestorClientConfig;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreConfig;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClient;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchClientConfig;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchRestClient;
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
		return getIntegrationTestProperty("integrationtest.protocol");
	}
	
	// sparql endpoint
	public static String getSparqlServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.sparqlendpoint.server");
	}
	public static String getSparqlServerType() throws Exception{
		return getIntegrationTestProperty("integrationtest.sparqlendpoint.type");
	}
	public static String getSparqlServerUsername() throws Exception{
		return getIntegrationTestProperty("integrationtest.sparqlendpoint.username");
	}
	public static String getSparqlServerPassword() throws Exception{
		return getIntegrationTestProperty("integrationtest.sparqlendpoint.password");
	}
	
	// sparql query service
	public static String getSparqlQueryServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.sparqlqueryservice.server");
	}
	public static int getSparqlQueryServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.sparqlqueryservice.port")).intValue();	
	}	
	
	// ingestion service
	public static String getIngestionServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.ingestionservice.server");
	}
	public static int getIngestionServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.ingestionservice.port")).intValue();	
	}	
		
	// status service
	public static String getStatusServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.statusservice.server");
	}
	public static int getStatusServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.statusservice.port")).intValue();
	}
	
	// results service
	public static String getResultsServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.resultsservice.server");
	}
	public static int getResultsServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.resultsservice.port")).intValue();
	}
	
	// dispatch service
	public static String getDispatchServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.dispatchservice.server");
	}
	public static int getDispatchServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.dispatchservice.port")).intValue();
	}	
		
	// Hive service
	public static String getHiveServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.hiveservice.server");
	}
	public static int getHiveServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.hiveservice.port")).intValue();
	}
	
	// nodegroup store service
	public static String getNodegroupStoreServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.nodegroupstoreservice.server");
	}
	public static int getNodegroupStoreServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.nodegroupstoreservice.port")).intValue();
	}
	
	// nodegroup execution service
	public static String getNodegroupExecutionServiceServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.nodegroupexecution.server");
	}
	public static int getNodegroupExecutionServicePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.nodegroupexecution.port")).intValue();
	}
	
	// Hive
	public static String getHiveServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.hive.server");
	}
	public static int getHivePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.hive.port")).intValue();
	}
	public static String getHiveUsername() throws Exception{
		return getIntegrationTestProperty("integrationtest.hive.username");
	}
	public static String getHivePassword() throws Exception{
		return getIntegrationTestProperty("integrationtest.hive.password");
	}
	public static String getHiveDatabase() throws Exception{
		return getIntegrationTestProperty("integrationtest.hive.database");
	}
	
	// Oracle
	public static String getOracleServer() throws Exception{
		return getIntegrationTestProperty("integrationtest.oracle.server");
	}
	public static int getOraclePort() throws Exception{
		return Integer.valueOf(getIntegrationTestProperty("integrationtest.oracle.port")).intValue();
	}
	public static String getOracleUsername() throws Exception{
		return getIntegrationTestProperty("integrationtest.oracle.username");
	}
	public static String getOraclePassword() throws Exception{
		return getIntegrationTestProperty("integrationtest.oracle.password");
	}
	public static String getOracleDatabase() throws Exception{
		return getIntegrationTestProperty("integrationtest.oracle.database");
	}	
	
	
	
	/**
	 * Get a ResultsClient using the integration test properties.
	 */
	public static ResultsClient getResultsClient() throws Exception{
		return new ResultsClient(new ResultsClientConfig(getServiceProtocol(), getResultsServiceServer(), getResultsServicePort()));
	}
	
	/**
	 * Get a StatusClient using the integration test properties.
	 */
	public static StatusClient getStatusClient(String jobId) throws Exception{
		return new StatusClient(new StatusClientConfig(getServiceProtocol(), getStatusServiceServer(), getStatusServicePort(), jobId));
	}	
	
	/**
	 * Get a SparqlQueryClient using the integration test properties.
	 */
	public static SparqlQueryClient getSparqlQueryClient(String serviceEndpoint, String sparqlServer, String dataset) throws Exception{
		return new SparqlQueryClient(new SparqlQueryClientConfig(getServiceProtocol(), getSparqlQueryServiceServer(), getSparqlQueryServicePort(), serviceEndpoint, sparqlServer, getSparqlServerType(), dataset));
	}
	
	/**
	 * Get a NodeGroupStoreRestClient using the integration test properties.
	 */
	public static NodeGroupStoreRestClient getNodeGroupStoreRestClient() throws Exception{
		return new NodeGroupStoreRestClient(new NodeGroupStoreConfig(getServiceProtocol(), getNodegroupStoreServiceServer(),  getNodegroupStoreServicePort()));
	}
	
	/**
	 * Get a NodeGroupExecutor using the integration test properties.
	 */
	public static NodeGroupExecutor getNodegroupExecutor() throws Exception{
		NodeGroupStoreRestClient ngsrc = getNodeGroupStoreRestClient();
		DispatchRestClient drc = new DispatchRestClient(new DispatchClientConfig(getServiceProtocol(), getDispatchServiceServer(), getDispatchServicePort()));
		StatusClient stc = new StatusClient(new StatusClientConfig(getServiceProtocol(), getStatusServiceServer(), getStatusServicePort(), "totally fake"));
		ResultsClient rc  = new ResultsClient(new ResultsClientConfig(getServiceProtocol(), getResultsServiceServer(), getResultsServicePort()));
		IngestorRestClient ic = new IngestorRestClient(new IngestorClientConfig(getServiceProtocol(), getIngestionServiceServer(), getIngestionServicePort()));		
		return new NodeGroupExecutor(ngsrc, drc, rc, stc, ic);
	}
	
	public static String getIntegrationTestProperty(String key) throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, key);
	}
}
