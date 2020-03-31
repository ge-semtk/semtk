/**
 ** Copyright 2016-2020 General Electric Company
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

import com.ge.research.semtk.utility.Utility;

/**
 * Utility class for configuring integration tests.
 * 
 * NOTE: This class cannot be put in under src/test/java because it must remain accessible to other projects.
 */
public class IntegrationTestUtilityGE {
	
	// property file with integration test configurations
	public static final String INTEGRATION_TEST_PROPERTY_FILE = "src/test/resources/integrationtest.properties";
	
	// Athena service
	public static String getAthenaServiceServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.athenaservice.server");
	}
	public static int getAthenaServicePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.athenaservice.port")).intValue();
	}
	
	// Athena
	public static String getAthenaDatabase() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.athena.database");
	}
	
	// EDC query generator service
	public static String getEdcQueryGenerationServiceServer() throws Exception {
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.edcquerygenservice.server");
	}
	public static int getEdcQueryGenerationServicePort() throws Exception {
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.edcquerygenservice.port")).intValue();
	}
	
	// Binary File executor service
	public static String getBinaryFileServiceServer() throws Exception {
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.binaryfileservice.server");
	}
	public static int getBinaryFileServicePort() throws Exception {
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.binaryfileservice.port")).intValue();
	}
	
	// Binary HDFS download service (bigdatatk)
	public static String getBinaryHdfsDownloadServiceServer() throws Exception {
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.binaryhdfsdownloadservice.server");
	}
	
	// KairosDB
	public static String getKairosDBServiceServer() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.kairosdbservice.server");
	}
	public static int getKairosDBServicePort() throws Exception{
		return Integer.valueOf(Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.kairosdbservice.port")).intValue();
	}
	public static String getKairosDBUrl() throws Exception{
		return Utility.getPropertyFromFile(INTEGRATION_TEST_PROPERTY_FILE, "integrationtest.kairosdb.url");
	}
}
