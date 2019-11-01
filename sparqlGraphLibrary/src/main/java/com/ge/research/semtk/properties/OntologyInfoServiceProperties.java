package com.ge.research.semtk.properties;

import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.edc.client.OntologyInfoClientConfig;

public class OntologyInfoServiceProperties extends ServiceProperties {
	
	public OntologyInfoClient getClient() throws Exception {
		OntologyInfoClient client = new OntologyInfoClient(new OntologyInfoClientConfig(protocol, server, port));
		return client;
	}
}
