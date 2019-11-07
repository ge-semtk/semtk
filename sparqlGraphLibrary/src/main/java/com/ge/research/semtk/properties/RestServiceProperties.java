package com.ge.research.semtk.properties;

import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.edc.client.ResultsClientConfig;
import com.ge.research.semtk.services.client.RestClient;
import com.ge.research.semtk.services.client.RestClientConfig;

public class RestServiceProperties extends ServiceProperties {
	
	public RestClient getClient() throws Exception {
		RestClient client = new RestClient(new RestClientConfig(protocol, server, port, "none_yet"));
		return client;
	}
}
