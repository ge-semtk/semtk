package com.ge.research.semtk.sparqlX.dispatch.client;

import com.ge.research.semtk.services.client.RestClientConfig;

public class DispatchClientConfig extends RestClientConfig {

	public DispatchClientConfig(String serviceProtocol, String serviceServer,
			int servicePort) throws Exception {

		super(serviceProtocol, serviceServer, servicePort, "fake");
	}

}
