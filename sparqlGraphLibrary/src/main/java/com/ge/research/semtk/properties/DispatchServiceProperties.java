package com.ge.research.semtk.properties;

import com.ge.research.semtk.sparqlX.dispatch.client.DispatchClientConfig;
import com.ge.research.semtk.sparqlX.dispatch.client.DispatchRestClient;

public class DispatchServiceProperties extends ServiceProperties {
	
	public DispatchRestClient getClient() throws Exception {
		DispatchRestClient client = new DispatchRestClient(new DispatchClientConfig(protocol, server, port));
		return client;
	}
}
