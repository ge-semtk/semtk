package com.ge.research.semtk.properties;

import com.ge.research.semtk.load.client.IngestorClientConfig;
import com.ge.research.semtk.load.client.IngestorRestClient;

public class IngestorServiceProperties extends ServiceProperties {
	
	public IngestorRestClient getClient() throws Exception {
		IngestorRestClient client = new IngestorRestClient(new IngestorClientConfig(protocol, server, port));
		return client;
	}
}
