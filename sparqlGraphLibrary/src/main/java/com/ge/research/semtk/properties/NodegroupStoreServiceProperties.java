package com.ge.research.semtk.properties;

import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreConfig;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;

public class NodegroupStoreServiceProperties extends ServiceProperties {
	
	public NodeGroupStoreRestClient getClient() throws Exception {
		NodeGroupStoreRestClient client = new NodeGroupStoreRestClient(new NodeGroupStoreConfig(protocol, server, port));
		return client;
	}
}
