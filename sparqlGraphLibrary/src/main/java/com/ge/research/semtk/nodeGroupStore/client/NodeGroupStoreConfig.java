package com.ge.research.semtk.nodeGroupStore.client;

import com.ge.research.semtk.services.client.RestClientConfig;

public class NodeGroupStoreConfig extends RestClientConfig {

	public NodeGroupStoreConfig(String serviceProtocol, String serviceServer,
			int servicePort) throws Exception {
		
		super(serviceProtocol, serviceServer, servicePort, "fake");
		
	}

}
