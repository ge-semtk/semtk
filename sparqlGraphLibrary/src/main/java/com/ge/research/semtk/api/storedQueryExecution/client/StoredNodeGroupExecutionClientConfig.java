package com.ge.research.semtk.api.storedQueryExecution.client;

import org.json.simple.JSONObject;

import com.ge.research.semtk.services.client.RestClientConfig;

public class StoredNodeGroupExecutionClientConfig  extends RestClientConfig{

	public StoredNodeGroupExecutionClientConfig(String serviceProtocol, String serviceServer,
			int servicePort) throws Exception {
		
		
		super(serviceProtocol, serviceServer, servicePort, "fake");
		
	}

	@SuppressWarnings("unchecked")
	public void addParameters(JSONObject param) {
	}
	
}
