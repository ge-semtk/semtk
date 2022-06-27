package com.ge.research.semtk.services.nodeGroupService.requests;

import java.util.ArrayList;

import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import io.swagger.v3.oas.annotations.media.Schema;

public class BuildNodeGroupRequest extends SparqlConnectionRequest {


    @Schema(
    		description = "List of nodes: id and class",
            required = true)
    private ArrayList<SparqlIdClassUriTuple> nodeList;
    
    @Schema(
    		description = "List of connections between nodes",
            required = true)
    private ArrayList<PropertyConnectionTriple> connList;

	public ArrayList<SparqlIdClassUriTuple> getNodeList() {
		return nodeList;
	}

	public ArrayList<PropertyConnectionTriple> getConnList() {
		return connList;
	}

   
}
