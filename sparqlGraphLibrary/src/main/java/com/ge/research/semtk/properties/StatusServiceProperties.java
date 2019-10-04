package com.ge.research.semtk.properties;

import com.ge.research.semtk.edc.client.StatusClient;
import com.ge.research.semtk.edc.client.StatusClientConfig;

public class StatusServiceProperties extends ServiceProperties {
	
	public StatusClient getClient(String jobId) throws Exception {
		StatusClient client = new StatusClient(new StatusClientConfig(protocol, server, port, jobId));
		return client;
	}
}
