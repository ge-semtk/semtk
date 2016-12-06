package com.ge.research.semtk.load.client;

import org.json.simple.JSONObject;

import com.ge.research.semtk.services.client.RestClientConfig;

public class IngestorClientConfig  extends RestClientConfig{

	private String csvData = null;
	private String template = null;
	
	public IngestorClientConfig(String serviceProtocol, String serviceServer,
			int servicePort) throws Exception {
		
		
		super(serviceProtocol, serviceServer, servicePort, "fake");
		
	}

	@SuppressWarnings("unchecked")
	public void addParameters(JSONObject param) {
	}
}
