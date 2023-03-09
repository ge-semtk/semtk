package com.ge.research.semtk.properties;

import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClientConfig;

public class NodegroupExecutionServiceProperties extends ServiceProperties {
	
	public NodeGroupExecutionClient getClient() throws Exception {
		NodeGroupExecutionClient client = new NodeGroupExecutionClient(new NodeGroupExecutionClientConfig(protocol, server, port));
		return client;
	}
}
