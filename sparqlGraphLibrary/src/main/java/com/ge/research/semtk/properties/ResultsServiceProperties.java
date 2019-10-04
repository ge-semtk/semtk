package com.ge.research.semtk.properties;

import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;

public class ResultsServiceProperties extends ServiceProperties {
	
	public ResultsClient getClient(String jobId) throws Exception {
		ResultsClient client = new ResultsClient(new ResultsClientConfig(protocol, server, port));
		return client;
	}
}
