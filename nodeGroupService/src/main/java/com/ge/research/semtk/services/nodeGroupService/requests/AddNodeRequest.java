package com.ge.research.semtk.services.nodeGroupService.requests;

import io.swagger.v3.oas.annotations.media.Schema;

public class AddNodeRequest extends NodegroupRequest {

    @Schema(
            name = "sparqlId of the node within the nodeGroup",
            required = true,
            example = "?Car")
    private String existingNodeSparqlId;

    @Schema(
            name = "the URI of the new node we are adding to the nodeGroup",
            required = true,
            example = "http://namespace#Wheel")
    private String newNodeUri;

    @Schema(
            name = "the URI of the object property that connects two nodes",
            required = true,
            example = "http://namespace#hasWheel")
    private String objectPropertyUri;

    @Schema(
            name = "indicates the direction of the connection. <BR>" +
                    "If this is false, we will connect newNode to existingNode via objectProperty.",
            required = true,
            example = "true")
    private boolean fromExistingToNewNode;

    public String getExistingNodeSparqlId() {
        return existingNodeSparqlId;
    }

    public String getNewNodeUri() {
        return newNodeUri;
    }

    public String getObjectPropertyUri() {
        return objectPropertyUri;
    }

    public boolean isFromExistingToNewNode() {
        return fromExistingToNewNode;
    }
}
