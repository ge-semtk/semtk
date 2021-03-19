package com.ge.research.semtk.nodeGroupService;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;

public class NodegroupRepairer {
	OntologyInfo oInfo = null;
	NodeGroup nodegroup = null;
	
	public NodegroupRepairer(OntologyInfoClient oClient, SparqlGraphJson sgjson) throws Exception {
		super();
		
		this.nodegroup = sgjson.getNodeGroup();
		this.oInfo = oClient.getOntologyInfo(sgjson.getSparqlConn());
	}
	
	
}
