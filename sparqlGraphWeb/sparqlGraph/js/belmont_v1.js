/**
 ** Copyright 2016 General Electric Company
 **
 ** Authors:  Paul Cuddihy, Justin McHugh
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

console.log("belmont_v1");

var BelmontV1 = function (ng) {
    this.ng = ng;
};

//--- move start ----

BelmontV1.prototype = {
    generateSparqlPrefix : function(){
		var retval = "";
		
		// check that it is built!
		this.ng.buildPrefixHash(); 
		
		var keys = Object.keys(this.ng.prefixHash);
		for (var i=0; i < keys.length; i++) {
			k = keys[i];
			retval += "prefix " + this.ng.prefixHash[k] + ":<" + k + "#>\n";
		}
				
		return retval ;
	},
    
	tabIndent : function (tab) {
		return tab + "   ";
	},
	tabOutdent : function (tab) {
		return tab.slice(0, -3);
	},

	generateSparqlConstruct : function() {   
        console.log("Using deprecated belmont.js:generateSparqlConstruct.  Replaced by NodeGroupService.");
		this.ng.prefixHash = {};
		this.ng.buildPrefixHash();
		
		var tab = "    ";
		var sparql = this.ng.generateSparqlPrefix() + "\nconstruct {\n";
		var queryType = SemanticNodeGroup.QUERY_CONSTRUCT;

		var doneNodes = [];
		var headNode = this.ng.getNextHeadNode(doneNodes);
		while (headNode != null) {
			sparql += this.ng.generateSparqlSubgraphClauses(queryType, 
														headNode, 
														null, null,   // skip nodeItem.  Null means do them all.
														null,    // no targetObj
														doneNodes, 
														tab);
			headNode = this.ng.getNextHeadNode(doneNodes);
		}
		
		sparql += "}";

		sparql += this.ng.generateSparqlFromClause("");
		
		// jm: borrowed from "GenerateSparql"				
		sparql += "\nwhere {\n";

		queryType = SemanticNodeGroup.QUERY_CONSTRUCT;
		doneNodes = [];
		headNode = this.ng.getNextHeadNode(doneNodes);
		while (headNode != null) {
			sparql += this.ng.generateSparqlSubgraphClauses(queryType, 
														headNode, 
														null, null,  // skip nodeItem.  Null means do them all.
														null,    // no targetObj
														doneNodes, 
														tab);
			headNode = this.ng.getNextHeadNode(doneNodes);
		}
		
		sparql += "}\n";
		console.log("Built SPARQL query:\n" + sparql);
		return sparql;
	},

	generateSparqlInsert : function() {
        console.log("Using deprecated belmont.js:generateSparqlInsert.  Replaced by NodeGroupService.");

		this.ng.prefixHash = {};
		this.ng.buildPrefixHash();
		this.ng.addToPrefixHash(SemanticNodeGroup.INSERT_FULL);   // make sure to force the inclusion of the old ones.
		
		var f = new SparqlFormatter();
		var sparql = this.ng.generateSparqlPrefix() + "\nINSERT {\n";
		
		// perform the insert section by looping through all nodes
		for (var i=0; i < this.ng.SNodeList.length; i++) {
			var sparqlId = this.ng.SNodeList[i].getSparqlID();
			// insert information that the node was of it's own type. 90% of the time, this.ng is redudant.
			sparql += "    " + sparqlId + " a " + this.ng.getPrefixedUri(this.ng.SNodeList[i].fullURIName) + ".\n";
			
			// insert line for each property
			for (var p=0; p < this.ng.SNodeList[i].propList.length; p++) {
				var prop = this.ng.SNodeList[i].propList[p];
				
				var valuesToInsert = prop.getInstanceValues();
				var sizeLimit = valuesToInsert.length;
				for(var j = 0; j < sizeLimit; j += 1){
					sparql += "   " + sparqlId + " " + this.ng.getPrefixedUri(prop.getUriRelation()) + " \"" + f.sparqlSafe(valuesToInsert[j]) + "\"^^" + SemanticNodeGroup.INSERT_PREFIX + prop.getValueType() + ".\n";
				}
			}
			
			// insert line for each node
			for (var n=0; n < this.ng.SNodeList[i].nodeList.length; n++) {
				var node = this.ng.SNodeList[i].nodeList[n];
				for (var s=0; s < node.SNodes.length; s++) {
					sparql += "   " + sparqlId + " " + this.ng.getPrefixedUri(node.getURIConnectBy()) + " " + node.SNodes[s].getSparqlID() + ".\n";
				}
			}
		}
		
		// perform the where section
		
		var whereSparql = "";

		for (var i=0; i < this.ng.SNodeList.length; i++) {
			var sparqlId = this.ng.SNodeList[i].getSparqlID();
			
			// (1) SNode URI was specified
			if (this.ng.SNodeList[i].getInstanceValue() != null) {
				
				// bind to a given URI
				whereSparql += "   BIND (" + this.ng.getPrefixedUri(this.ng.SNodeList[i].getInstanceValue()) + " AS " + sparqlId + ").\n";
				
			} else {
				var propList = this.ng.SNodeList[i].getConstrainedPropertyItems();
				
				
				if (propList.length > 0) {
					// (2) SNode is constrained
					for (var p=0; p < propList.length; p++) {
						whereSparql += "   " + sparqlId + " " + this.ng.getPrefixedUri(propList[p].getUriRelation()) + " " + propList[p].getSparqlID() + ". " + propList[p].getConstraints() + " .\n";

					}
				} else{
					// (3) SNode is not constrained: create new URI
					
					// create new instance
					// we have to be able to check if the Node has "instanceValue" set. if it does. we want to reuse that. if not, kill it. 
					if(this.ng.SNodeList[i].getInstanceValue() != null){
						// use the instanceValue
						whereSparql += "   BIND (iri(" + this.ng.getPrefixedUri(this.ng.SNodeList[i].getInstanceValue()) + ") AS " + sparqlId + ").\n";
					}
					else{
						whereSparql += "   BIND (iri(concat(" + SemanticNodeGroup.INSERT_PREFIX + SparqlUtil.guid() + ")) AS " + sparqlId + ").\n";
					}
				}
			}
		}
		//var bugFix = "<" + this.ng.SNodeList[0].getURI() + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class>.\n";
		sparql += "} WHERE {\n" + whereSparql + "}\n";

		return sparql;
		
	},
	
	generateSparqlDelete : function(postFixString, oInfo) {
        console.log("Using deprecated belmont.js:generateSparqlDelete.  Replaced by NodeGroupService.");

		this.ng.prefixHash = {};			// we need the prefixes set reasonably.
		this.ng.buildPrefixHash();
		
		var retval = ""; 				// eventually, this.ng will be our sparql statement.
		
		var primaryBody = this.ng.getDeletionLeader(postFixString, oInfo);
		var whereBody   = this.ng.getDeletionWhereBody(postFixString, oInfo);
		
		retval = this.ng.generateSparqlPrefix() + "\nDELETE {\n" + primaryBody + "}";
		
		retval += this.ng.generateSparqlFromClause("");
		
		if(whereBody.length > 0){
			retval += "\nWHERE {\n" + whereBody + " }\n";
		}
		
		return retval;
	},
	
	getDeletionLeader : function(postFixString, oInfo) {
		var retval = "";
		
		for(var i = 0; i < this.ng.SNodeList.length; i += 1 ){
			var n = this.ng.SNodeList[i];
			
			var deletionMode = n.getDeletionMode();
			
			if(deletionMode != NodeDeletionTypes.NO_DELETE){
				// there is a deletion intended
				retval += this.ng.generateNodeDeletionSparql(n);
			}
			
			// check the properties
			for(var pCount = 0; pCount < n.propList.length; pCount += 1){
				var pi = n.propList[pCount];
				if( pi.getIsMarkedForDeletion() ){
					retval += "    " + n.getSparqlID() + " " + this.ng.getPrefixedUri( pi.getUriRelation() ) + " " +  pi.getSparqlID() + " . \n";
				}
			}
			// get the NodeItems
			for(var nCount = 0; nCount < n.nodeList.length; nCount += 1){
				var nodeInUse = n.nodeList[nCount];
				var nic = nodeInUse.getSnodesWithDeletionFlagsEnabledOnthis.ngNodeItem();
				// write up the delete for this.ng....
				for(var nicCount = 0; nicCount < nic.length; nicCount++ ){
					var connected = nic[nicCount];
					retval += "    " + n.getSparqlID() + " " + this.ng.getPrefixedUri(nodeInUse.getURIConnectBy() ) + " " + connected.getSparqlID() +  " . \n";
				}
				// this.ng should contain all the deletion info for the node itself.
			}
	
		}	
		// ship it out
		return retval;
	},
	
	generateNodeDeletionSparql : function(nodeInScope) {
		var retval = "";
		var indent = "    ";
		var delMode = nodeInScope.getDeletionMode();
		
		if(delMode === NodeDeletionTypes.TYPE_INFO_ONLY){
			retval += indent + nodeInScope.getSparqlID() + " rdf:type  " + nodeInScope.getSparqlID() + "_type_info . \n";
		}
		else if(delMode === NodeDeletionTypes.FULL_DELETE){
			retval += indent + nodeInScope.getSparqlID() + " rdf:type  " + nodeInScope.getSparqlID() + "_type_info . \n";
			retval += indent + nodeInScope.getSparqlID() + " " + nodeInScope.getSparqlID() + "_related_predicate_outgoing " + nodeInScope.getSparqlID() + "_related_object_target . \n";
			retval += indent + nodeInScope.getSparqlID() + "_related_subject " + nodeInScope.getSparqlID() + "_related_predicate_incoming " + nodeInScope.getSparqlID() + " . \n";
		}
		else if(delMode === NodeDeletionTypes.LIMITED_TO_NODEGROUP){
			retval += indent + nodeInScope.getSparqlID() + " rdf:type  " + nodeInScope.getSparqlID() + "_type_info . \n";
			
			// get all incoming references to this.ng particular node (in the current NodeGroup scope)
			// and schedule them for removal...
			for(var ndCounter = 0; ndCounter < this.ng.SNodeList.length; ndCounter++){
				var nd = this.ng.SNodeList[ndCounter];
				
				// get the node items and check the targets.
				var connections = nd.getConnectingNodeItems(nodeInScope);
				for(var conNum = 0; conNum < connections.length; conNum++ ){
					var ni = connection[conNum];
					// set it so that the consequences of the decision are seen in the post-decision ND
					ni.setSnodeDeletionMarker(n, true);
					// generate the sparql snippet related to this.ng deletion.
					retval += indent + nd.getSparqlID() + " " + this.ng.getPrefixedUri(ni.getUriConnectBy()) + " " + nodeInScope.getSparqlID() + " . \n";
				}
			}
		}
		else if(delMode === NodeDeletionTypes.LIMITED_TO_MODEL){
			throw new Error("NodeDeletionTypes.LIMITED_TO_MODEL is not currently implemented. sorry.");
		}
		else {
			throw new Error("generateNodeDeletionSparql :: node with sparqlID (" + nodeInScope.getSparqlID() + ") has an unimplemented DeletionMode"); 
		}
		
		return retval;
	},
	getDeletionWhereBody : function (postFixString, oInfo) {
		var retval = "";
		
		var doneNodes = [];
		var headNode = this.ng.getNextHeadNode(doneNodes);
		while (headNode != null) {
			retval += this.ng.generateSparqlSubgraphClauses(SemanticNodeGroup.QUERY_DELETE_WHERE, headNode, null, null, null, doneNodes, "   ");
			headNode = this.ng.getNextHeadNode(doneNodes);
		}

		return retval;
	},
	
	generateSparql : function(queryType, optionalFlag, optLimitOverride, optTargetObj, optKeepTargetConstraints) {
		//
		// queryType:
		//     QUERY_DISTINCT - select distinct.   Use of targetObj is undefined.
		//     QUERY_CONSTRAINT - select distinct values for a specific node or prop item (targetObj) after removing any existing constraints
		//     QUERY_COUNT - count results.  If targetObj the use that as the only object of "select distinct".  
		//
        // optLimitOverride - if > -1 then override this.ng.limit
		//
		// targetObj - if not (null/undefined/0/false/'') then it should be a SemanticNode or a PropertyItem
		//    QUERY_CONSTRAINT - must be set.   Return all legal values for this.ng.   Remove constraints.
        //    QUERY_COUNT - if set, count results of entire query but only this.ng distinct return value
		//
		// optKeepTargetConstraints - keep constraints on the target object (default false)
		//
		// Error handling: For each error, inserts a comment at the beginning.
		//    #Error: explanation
        console.log("Using deprecated belmont.js:generateSparql.  Replaced by NodeGroupService.");
		
		this.ng.prefixHash = {};
		this.ng.buildPrefixHash();
		
		var targetObj = (typeof(optTargetObj) === 'undefined') ? null : optTargetObj;
		var keepTargetConstraints = (typeof(optKeepTargetConstraints) === 'undefined') ? false : optKeepTargetConstraints;
		var tab = this.ng.tabIndent("");
		var fmt = new SparqlFormatter();

		if (this.ng.SNodeList.length == 0) {
			return '';
		}

		var orderedSNodes = this.ng.getOrderedNodeList();
		var sparql = this.ng.generateSparqlPrefix() + "\n";

		if (queryType == SemanticNodeGroup.QUERY_COUNT) {
			sparql += "SELECT (COUNT(*) as ?count) { \n";
		}
		
		sparql += 'select distinct';
		var lastLen = sparql.length;
		
		// add the return props names. 
		if (targetObj !== null) {
			// QUERY_CONSTRAINT or QUERY_COUNT or anything that set targetObj:  simple
			// only the targetObj is returned
			sparql += " " + targetObj.SparqlID;

		} else {
			// loop through ordered nodes and add return names to the sparql
			for (var i=0; i < orderedSNodes.length; i++) {
				
				// check if node URI is returned
				if (orderedSNodes[i].getIsReturned()) {
					sparql += " " + orderedSNodes[i].getSparqlID();
				}
				
				// add all the returned props
				var props = orderedSNodes[i].getReturnedPropsReturnNames();
				for (var p=0; p < props.length; p++) {
					sparql += " " + props[p];
				}
			}			
		}

		// if there are no return values, it is an error. Prepend "#Error" to
		// the SPARQL
		if (sparql.length == lastLen) {
			sparql = "#Error: No values selected for return.\n" + sparql;
		}

		sparql += this.ng.generateSparqlFromClause(tab);
		
		sparql += " where {\n";

		var doneNodes = [];
		var headNode = this.ng.getNextHeadNode(doneNodes);
		while (headNode != null) {
			sparql += this.ng.generateSparqlSubgraphClauses(queryType, 
														headNode, 
														null, null,   // skip nodeItem.  Null means do them all.
														keepTargetConstraints ? null : targetObj, 
														doneNodes, 
														tab);
			headNode = this.ng.getNextHeadNode(doneNodes);
		}
		
		sparql += "}\n";

		if (queryType === SemanticNodeGroup.QUERY_CONSTRAINT) {
			// this.ng is too slow.
			// sparql += "ORDER BY " + targetObj.SparqlID + " ";
		}
        
        sparql += this.ng.generateLimitClause(optLimitOverride);
        
		if (queryType === SemanticNodeGroup.QUERY_COUNT) {
			sparql += "\n}";
		}

		//sparql = fmt.prefixQuery(sparql);
		console.log("Built SPARQL query:\n" + sparql);
		return sparql;
	},
	
    /**
     *  optLimitOverride - if > -1 then override this.ng.limit
     */
    generateLimitClause : function (optLimitOverride) {
        var limit = typeof optLimitOverride != "undefined" && optLimitOverride > -1 ? optLimitOverride : this.ng.limit;
        
        if (limit > 0) {
			return "LIMIT " + String(limit);
		} else {
            return "";
        }
    },
        
	/**
	 * Very simple FROM clause logic
	 * Generates FROM clause if this.ng.conn has
	 *     - exactly 1 serverURL
	 *     - more than one datasets (graphs)
	 */
	generateSparqlFromClause : function (tab) {
		
		
		// do nothing if no conn
		if (this.ng.conn == null) return "";
		
		// multiple ServerURLs is not implemented
		if (! this.ng.conn.isSingleServerURL() ) {
			throw new Error("SPARQL generation across multiple servers is not yet supported.");
		}
		
		// get datasets for first model server.  All others must be equal
		// NOT DEPRECATED: proper use of getModelInterface()
		var datasets = this.ng.conn.getDatasetsForServer(this.ng.conn.getModelInterface(0).getServerURL());
		
		if (datasets.length < 2) return "";
		
		var sparql = "\n";
		// multiple datasets: generate FROM clause
		tab = this.ng.tabIndent(tab);
		for (var i=0; i < datasets.length; i++) {
			sparql += tab + "FROM <" + datasets[i] + ">\n";
		}
		tab = this.ng.tabOutdent(tab);
		
		return sparql;
	},
	
	generateSparqlSubgraphClauses : function (queryType, snode, skipNodeItem, skipNodeTarget, targetObj, doneNodes, tab) {
		// recursively generate sparql clauses for nodes in a subtree
		//
		// queryType -    same as generateSparql
		// snode - starting point in subgraph.
		// skipNodeItem - nodeItem that got us here.  Don't cross it when calculating subgraph.
		// targetObj -    same as generateSparql
		// doneNodes - snodes already processed.  Hitting one means there's a loop.  Updated as a side-effect.
		// tab -          same as generateSparql
		
		// done if snode is already in the doneNodes list
		if (doneNodes.indexOf(snode) > -1) {
			console.log("SemanticNodeGroup.generateSparqlSubgraphClauses() detected a loop in the query.")
			return "";
		} else {
			doneNodes.push(snode);
		}
		
		var sparql = "";
		
		// get the type information included in the query result. only valid on the CONTRUCT query
		if (queryType == SemanticNodeGroup.QUERY_CONSTRUCT || queryType == SemanticNodeGroup.QUERY_CONSTRUCT_WHERE){
			sparql += tab + snode.getSparqlID() + " a  " + snode.getSparqlID() + "_type . \n" ;
		}
		
		// added for deletions
		else if (queryType == SemanticNodeGroup.QUERY_DELETE_WHERE && snode.getDeletionMode() != NodeDeletionTypes.NO_DELETE){
			sparql += tab + this.ng.generateNodeDeletionSparql(snode);
		}
		// this.ng is the type-constraining statement for any type that needs
		// NOTE: this.ng is at the top due to a Virtuoso bug
		//       If the first prop is optional and nothing matches then the whole query fails.
		sparql += this.ng.generateSparqlTypeClause(snode, tab);
		
		// PropItems: generate sparql for property and constraints
		var props = snode.getPropsForSparql(targetObj, queryType);
		for (var l = 0; l < props.length; l++) {
			
			if (props[l].getIsOptional() && queryType !== SemanticNodeGroup.QUERY_CONSTRUCT) {
				sparql += tab + "optional {\n";
				tab = this.ng.tabIndent(tab);
			}

			// SPARQL for basic prop
			sparql += tab + snode.getSparqlID() + " " + this.ng.getPrefixedUri(props[l].getUriRelation()) + " " + props[l].getSparqlID() + " .\n";

			// add in attribute range constraint if there is one 
			if (props[l].getConstraints()) {
				// add unless this.ng is a CONSTRAINT query on targetObj
				if ((queryType !== SemanticNodeGroup.QUERY_CONSTRUCT) && 
				(queryType !== SemanticNodeGroup.QUERY_CONSTRAINT || ! targetObj || props[l].getSparqlID() !== targetObj.SparqlID)) {
					tab = this.ng.tabIndent(tab);
					sparql += tab + props[l].getConstraints() + " .\n";
					tab = this.ng.tabOutdent(tab);
				}
			}
			
			// close optional block.
			if (props[l].getIsOptional() && queryType !== SemanticNodeGroup.QUERY_CONSTRUCT) {
				tab = this.ng.tabOutdent(tab);
				sparql += tab + "}\n";
			}
		}

		// add value constraints
		if (snode.getValueConstraint()) {
			// add unless this.ng is a CONSTRAINT query on targetObj
			if ((queryType !== SemanticNodeGroup.QUERY_CONSTRUCT) && (queryType !== SemanticNodeGroup.QUERY_CONSTRAINT || snode != targetObj)) {
				sparql += tab + snode.getValueConstraint() + ".\n";
			}
		}
		
		// Recursively process outgoing nItems   
		for (var i=0; i < snode.nodeList.length; i++) {
			var nItem = snode.nodeList[i];
				
			// each nItem might point to multiple children
			for (var j=0; j < nItem.SNodes.length; j++) {
				var targetNode = nItem.SNodes[j];
				
				if (nItem != skipNodeItem || targetNode != skipNodeTarget) {
					// open optional
					if (nItem.getSNodeOptional(targetNode) == NodeItem.OPTIONAL_TRUE  && (queryType !== SemanticNodeGroup.QUERY_CONSTRUCT)) {
						sparql += tab + "optional {\n";
						tab = this.ng.tabIndent(tab);
					}
	
					sparql += "\n";
					
					// node connection, then recursive call
					sparql += tab + snode.getSparqlID() + " " + this.ng.getPrefixedUri(nItem.getURIConnectBy()) + " " + targetNode.getSparqlID() + ".\n";
					tab = this.ng.tabIndent(tab);
					
					// RECURSION
					sparql += this.ng.generateSparqlSubgraphClauses(queryType, targetNode, nItem, targetNode, targetObj, doneNodes, tab);
					tab = this.ng.tabOutdent(tab);
					
					// close optional
					if (nItem.getSNodeOptional(targetNode) == NodeItem.OPTIONAL_TRUE && (queryType !== SemanticNodeGroup.QUERY_CONSTRUCT)) {
						tab = this.ng.tabOutdent(tab);
						sparql += tab + "}\n";
					}
				}
			}
		}
		
		// Recursively process incoming nItems
		var incomingNItems = this.ng.getConnectingNodeItems(snode);
		
		for (var i=0; i < incomingNItems.length; i++) {
			var nItem = incomingNItems[i];
			
			if (nItem != skipNodeItem || snode != skipNodeTarget) {
			
				// open optional
				if (nItem.getSNodeOptional(snode) == NodeItem.OPTIONAL_REVERSE && queryType !== SemanticNodeGroup.QUERY_CONSTRUCT) {
					sparql += tab + "optional {\n";
					tab = this.ng.tabIndent(tab);
				}
				
				var incomingSNode = this.ng.getNodeItemParentSNode(nItem);
				
				// the incoming connection
				if (incomingSNode != null) {
					sparql += "\n";
					sparql += tab + incomingSNode.getSparqlID() + " " + this.ng.getPrefixedUri(nItem.getURIConnectBy()) + " " + snode.getSparqlID() + ".\n";
					//tab = this.ng.tabIndent(tab);
				}
				
				// RECURSION
				sparql += this.ng.generateSparqlSubgraphClauses(queryType, incomingSNode, nItem, snode, targetObj, doneNodes, tab);
				//tab = this.ng.tabOutdent(tab);
				
				// close optional
				if (nItem.getSNodeOptional(snode) == NodeItem.OPTIONAL_REVERSE  && (queryType !== SemanticNodeGroup.QUERY_CONSTRUCT)) {
					tab = this.ng.tabOutdent(tab);
					sparql += tab + "}\n";
				}
			}
		}
		
		return sparql;
	},
	
	generateSparqlTypeClause : function(node, TAB) {
		// Generates SPARQL to constrain the type of this.ng node if
		// There is no edge that constrains it's type OR
		// the edge(s) that constrain it don't actually include it (they're all
		// super classes, so not enough constraint)
		var sparql = "";
		var constrainedTypes = this.ng.getConnectedRange(node);

		// if no edge is constraining the type, or edge constrains to a
		// different (presumably super-) type
		if (constrainedTypes.length == 0
				|| constrainedTypes.indexOf(node.fullURIName) < 0) {

			// constrain to exactly this.ng type since there are no subtypes
			if (node.subClassNames.length == 0) {
				sparql += TAB + node.getSparqlID() + " a " + this.ng.getPrefixedUri(node.fullURIName) + ".\n";

				// constrain to this.ng type plus a list sub-types
			} else {
				var typeVar = node.getSparqlID() + "_type";
				sparql += TAB + node.getSparqlID() + " a " + typeVar + ".\n";
				sparql += TAB + typeVar + " rdfs:subClassOf* " + this.ng.getPrefixedUri(node.fullURIName) + ".\n";
			}

		}
		return sparql;
	},
	
    //----- move end -----
}