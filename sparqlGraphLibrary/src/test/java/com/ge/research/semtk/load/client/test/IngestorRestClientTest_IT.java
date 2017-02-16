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


package com.ge.research.semtk.load.client.test;

import org.junit.Test;

import com.ge.research.semtk.load.client.IngestorClientConfig;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.test.IntegrationTestUtility;


public class IngestorRestClientTest_IT {
	
	@Test
	public void test() throws Exception{
		
		// create IngestorRestClietn
		String serviceProtocol = IntegrationTestUtility.getServiceProtocol();
		String ingestionServiceServer = IntegrationTestUtility.getIngestionServiceServer();
		int ingestionServicePort = IntegrationTestUtility.getIngestionServicePort();
		IngestorRestClient irc   = new IngestorRestClient(new IngestorClientConfig(serviceProtocol, ingestionServiceServer, ingestionServicePort));
		//irc.execIngestionFromCsv(insertTemplate, data);
		
		// TODO FINISH THIS!
	}
	
}
