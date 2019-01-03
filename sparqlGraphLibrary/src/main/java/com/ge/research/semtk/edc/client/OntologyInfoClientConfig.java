package com.ge.research.semtk.edc.client;

import com.ge.research.semtk.services.client.RestClientConfig;

public class OntologyInfoClientConfig extends RestClientConfig {

	public OntologyInfoClientConfig(String serviceProtocol, String serviceServer, int servicePort) throws Exception {
		super(serviceProtocol, serviceServer, servicePort, "fake", Methods.POST);
		// TODO Auto-generated constructor stub
	}

}
