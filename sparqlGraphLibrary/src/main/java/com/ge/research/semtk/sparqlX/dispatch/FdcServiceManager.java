/**
 ** Copyright 2016-2020 General Electric Company
 **
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
package com.ge.research.semtk.sparqlX.dispatch;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyName;
import com.ge.research.semtk.ontologyTools.OntologyPath;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

public class FdcServiceManager {
	
	static Table fdcTypeCache = null;           // cache of all fdc type info
	static long lastCacheMillis = 0;
	static final Object lock = new Object();
	
	private Table currFdcTypeCache = null;       // subset of fdcTypeCache matching current FDC node
	
	private NodeGroup nodegroup = null;
	private OntologyInfo oInfo = null;
	
	private ArrayList<Node> fdcDataNodes = null;     // FDC Nodes in the nodegroup 
	private Node currFdcDataNode = null;             
	
	                                                          // for each fdc node sparqlId, a hash of NodeGroup that generates each param set
	private HashMap<String,HashMap<String, NodeGroup>> paramNodegroups;     //  paramNodegroups.get(fdc_orig_sparqlId).get(param_set_int)=NodeGroup
	private HashMap<String,HashMap<String, Node>> paramHeadNodes;   // paramHeadNodes.get(fdc_orig_sparqlId).get(param_set_int)=head node in param nodegroup
	private HashSet<String> unprocessedFdcNodes;                   // set of unprocessed fdc node ?fdc_orig_sparqlId
	private HashMap<String,HashSet<Node>> fdcNodeCopies;   // fdcNodeCopies.get(fdc_orig_sparqlId)=copies of orig node in param nodegroups
	
	static String FDC_DATA_SUPERCLASS = "http://research.ge.com/semtk/federatedDataConnection#FDCData";
	
	/**
	 * 
	 * Federated Data Connection service manager
	 * @param extConfigSei - semtk services SEI
	 * @param nodegroup - the nodegroup being dispatched
	 * @param oInfo - oInfo of nodegroup being dispatched
	 * @param domain - (deprecated?) domain of the query connection
	 * @param nodegroupSei - the nodegroup's default query sei
	 * @throws Exception
	 */
	public FdcServiceManager(SparqlEndpointInterface extConfigSei, NodeGroup nodegroup, OntologyInfo oInfo, String domain, SparqlEndpointInterface nodegroupSei,  OntologyInfoClient oInfoClient) throws FdcConfigException, Exception {
		Object UNUSED;
		
		this.nodegroup = nodegroup;
		this.oInfo = oInfo;
		UNUSED = domain;
		UNUSED = nodegroupSei;
		
		// note that junit integration tests cache to a different connection
		// so if re-caching is implemented, it must honor the last cacheSei not necessarily the extConfigSei

		if (FdcServiceManager.getFdcTypeCache() == null) {
			FdcServiceManager.cacheFdcConfig(extConfigSei, oInfoClient);
		}

		calculate();
	}
	
	/**
	 * Read fdc configuration information into fdcTypeCache
	 * @param fdcServiceConn
	 * @throws Exception
	 */
	public static void cacheFdcConfig(SparqlEndpointInterface extConfigSei, OntologyInfoClient oInfoClient) throws Exception {

		// load the owl if needed, so that nodegroups will work
		InputStream owlStream = DispatchServiceManager.class.getResourceAsStream("/semantics/OwlModels/fdcServices.owl");
		try {
			AuthorizationManager.setSemtkSuper();
			OntologyInfo.uploadOwlModelIfNeeded(extConfigSei, owlStream);
		} finally {
			AuthorizationManager.clearSemtkSuper();
		}
		owlStream.close();

		// build static cache
		FdcServiceManager.lastCacheMillis = 0;
		FdcServiceManager.suggestReCache(extConfigSei, oInfoClient);
	}
	
	private void calculate() throws FdcConfigException, Exception {
		
		// inflate in case some of the fdc parameters aren't yet used and have been deflated away
		this.nodegroup.inflateAndValidate(oInfo);
		
		// fdcDataNodes is main indicator of whether this is an FDC nodegroup
		// non-null list of FDCData subclass-ed nodes
		this.fdcDataNodes = this.nodegroup.getNodesBySuperclassURI(FDC_DATA_SUPERCLASS, this.oInfo);
		
		// build the hashes
		this.paramNodegroups = new HashMap<String, HashMap<String, NodeGroup>>();
		this.paramHeadNodes = new HashMap<String, HashMap<String, Node>>();
		this.unprocessedFdcNodes = new HashSet<String>();
		
		// init fdcNodeCopies to empty set for each fdc node
		this.fdcNodeCopies = new HashMap<String, HashSet<Node>>();
		for (Node n : fdcDataNodes) {
			this.fdcNodeCopies.put(n.getSparqlID(), new HashSet<Node>());
		}

		for (Node n : fdcDataNodes) {
			String sparqlId = n.getSparqlID();
			
			// ngHash
			// one entry for the SparqlID of each FDC node in the nodegroup
			// the entry hashes the paramset string to a nodegroup that will generate the param sets;
			
			/*
			 * This is too blunt of a tool.
			 * - some errors are real errors and should be reported
			 * - some errors are missing connections that should silently return nothing
			 */
			try {
				this.calcParameterNodegroups(n);
				this.unprocessedFdcNodes.add(sparqlId);
			} catch (FdcConfigException f) {
				throw f;
			} catch (Exception e) {
				LocalLogger.logToStdOut("Can't process FDC node " + n.getSparqlID() + ": " + e.getMessage() );
				LocalLogger.printStackTrace(e);
			}
		}
	}
	
	/**
	 * Build a nodegroup that will return each parameter set
	 * @param fdcNode
	 * @return
	 * @throws Exception
	 */
	private void calcParameterNodegroups(Node fdcNode) throws FdcConfigException, Exception {

		HashMap<String, NodeGroup> paramSetToNgHash = new HashMap<String, NodeGroup>();
		String className = fdcNode.getFullUriName();
		
		// loop through this fdc node type's parameters
		Table thisConfigCache = fdcTypeCache.getSubsetWhereMatches("fdcClass", className );
		
		// make sure some config rows exist
		if (thisConfigCache.getNumRows() < 1) {
			throw new FdcConfigException("Could not find FDC configuration for class: " + className);
		}
		
		this.paramHeadNodes.put(fdcNode.getSparqlID(), new HashMap<String, Node>());
		
		// loop through param sets
		String inputList [] = thisConfigCache.getColumnUniqueValues("inputIndex");
		for (String inputIndex : inputList) {
			HashMap<String,String> origIdHash = new HashMap<String,String>();
			// make a nodegroup for this set
			NodeGroup ng = NodeGroup.deepCopy(this.nodegroup);
			Node fdcNodeCopy = ng.getNodeBySparqlID(fdcNode.getSparqlID());
			ng.unsetAllReturns();
			ng.setLimit(0);
			ng.clearOrderBy();
	
			// make a do-not-prune list
			ArrayList<Node> dontPruneList = new ArrayList<Node>();
			
			// get cache of only this input
			Table inputCache = thisConfigCache.getSubsetWhereMatches("inputIndex", inputIndex);
			
			// build link to subgraph
			String subjectClass = inputCache.getCell(0, "subjectClass");
			String predicateProp = inputCache.getCell(0, "predicateProp");
			String objectClass = inputCache.getCell(0, "objectClass");
			OntologyPath path = new OntologyPath(fdcNodeCopy.getFullUriName());
			path.addTriple(subjectClass,  predicateProp,  objectClass);   


			// get input subgraph head node
			Node inputHeadNode = ng.followPathToNode(fdcNodeCopy, path);
			this.paramHeadNodes.get(fdcNode.getSparqlID()).put(inputIndex, inputHeadNode);

			// shrink nodegroup to only the subgraph needed for this input
			ng.deleteSubGraph(fdcNodeCopy, inputHeadNode);


			// loop through params in set
			for (int i=0; i < inputCache.getNumRows(); i++) {

				String classURI = inputCache.getCell(i, "classURI");
				String propertyURI = inputCache.getCell(i, "propertyURI");
				String columnName = inputCache.getCell(i, "columnName");

				// get or add node
				Node paramNode = ng.getClosestOrAddNode(inputHeadNode, classURI, this.oInfo, true);
				if (paramNode == null) {
					throw new FdcConfigException("Error trying to add node " + classURI + " to input set " + inputIndex + " nodegroup");
				}

				Returnable paramItem = null;

				if (propertyURI.isEmpty()) {
					paramItem = paramNode;
				} else {
					paramItem = paramNode.getPropertyByURIRelation(propertyURI);
				}

				// set param item returned to true and set paramName
				ng.setIsReturned(paramItem, true);
				String suggestedId = (columnName.startsWith("?") ? "" : "?") + columnName;
				String oldId = paramItem.getSparqlID();
				String newId = ng.changeSparqlID(paramItem, suggestedId);
				if (! newId.equals(suggestedId)) {
					throw new FdcConfigException("Duplicate sparqlID problem in param retrieval nodegroup.  SparqlID: " + suggestedId);
				}

				origIdHash.put(newId, oldId);

				// add node to dont prune list
				if (!dontPruneList.contains(paramNode)) {
					dontPruneList.add(paramNode);
				}
			}

			// done adding params
			// now delete fdcNode and subgraph connections to/from it that have no input/params	
			ng.deleteSubGraph(fdcNodeCopy, dontPruneList);

			// put each FDC node in this paramNodegroup into fdcNodeCopies
			for (Node subFdcNode : ng.getNodesBySuperclassURI(FDC_DATA_SUPERCLASS, this.oInfo)) {
				String subFdcNodeOrigId = subFdcNode.getSparqlID();
				if (origIdHash.containsKey(subFdcNodeOrigId)) {
					subFdcNodeOrigId = origIdHash.get(subFdcNodeOrigId);
				}
				this.fdcNodeCopies.get(subFdcNodeOrigId).add(subFdcNode);
			}
			
			paramSetToNgHash.put(inputIndex, ng);
		}
		
		this.paramNodegroups.put(fdcNode.getSparqlID(), paramSetToNgHash);

	}
	
	public boolean hasUnresolvedDependencies(String sparqlId) {
		// for each paramNg node
		for (NodeGroup ng : this.paramNodegroups.get(sparqlId).values()) {
			if (this.getUnresolvedNodes(ng).size() > 0) {
				return true;
			}
		}
		return false;
	}
	
	public ArrayList<Node> getUnresolvedNodes(NodeGroup ng) {
		ArrayList<Node> ret = new ArrayList<Node>();
		for (Node n : ng.getNodeList()) {
			// for each set of fdcNodeCopies
			for (String origId : this.unprocessedFdcNodes) {
				HashSet<Node> nodeSet = this.fdcNodeCopies.get(origId);
				if (nodeSet.contains(n)) {
					ret.add(n);
				}
			}
		}
		return ret;
	}
	
	/**
	 * Does this nodegroup have any FDC nodes
	 * @return
	 */
	public boolean isFdc() {
		return this.fdcDataNodes.size() > 0;
	}
	
	/**
	 * Get sparqlIDs of all FDCData nodes
	 * @return
	 */
	public ArrayList<String> getFdcNodeSparqlIDs() {
		ArrayList<String> ret = new ArrayList<String>();

		for (Node node : this.fdcDataNodes) {
			ret.add(node.getSparqlID());
		}
		
		return ret;
	}
	
	/**
	 * Set proccessing to the next Fdc node in the nodegroup
	 * @return boolean success (false means done; no more)
	 */
	public boolean nextFdcNode() throws Exception {
		
		if (this.unprocessedFdcNodes.size() == 0) {
			return false;
		}

		// loop through looking for an easy answer
		for (String sparqlId : this.unprocessedFdcNodes) {
			Node fdcNode = this.nodegroup.getNodeBySparqlID(sparqlId);

			// does it have no unprocessed dependencies
			if (! this.hasUnresolvedDependencies(sparqlId)) {

				// mark this node as processed
				this.unprocessedFdcNodes.remove(sparqlId);

				// return it
				this.currFdcDataNode = fdcNode;
				this.currFdcTypeCache = FdcServiceManager.getFdcTypeCache().getSubsetWhereMatches("fdcClass", this.currFdcDataNode.getFullUriName());
				return true;
			} 

		}
		

		// Didn't find one with no dependencies
		// so loop through again and try removing some dependencies
		for (String sparqlId : this.unprocessedFdcNodes) {
			Node fdcNode = this.nodegroup.getNodeBySparqlID(sparqlId);

			HashMap<String, NodeGroup> ngHashEntryCopy = new HashMap<String, NodeGroup>();
			boolean failFlag = false;

			// loop through paramsets
			for (String paramSet : this.paramNodegroups.get(sparqlId).keySet()) {

				// copy the param nodegroup
				NodeGroup paramNg = this.paramNodegroups.get(sparqlId).get(paramSet);
				NodeGroup paramNgCopy = NodeGroup.deepCopy(paramNg);
				ngHashEntryCopy.put(paramSet, paramNgCopy);

				// delete all unresolved Fdc nodes and their subgraphs
				for (Node unresolvedNode : this.getUnresolvedNodes(paramNg)) {
					Node unresolvedCopy = paramNgCopy.getNodeBySparqlID(unresolvedNode.getSparqlID());
					// if deleting a previous subgraph didn't already delete this unresolved fdc node
					if (unresolvedCopy != null) {
						paramNgCopy.deleteNode(unresolvedCopy, paramHeadNodes.get(sparqlId).get(paramSet));  
					}
				}

				// check to see if new param nodegroup still has all the necessary returns
				ArrayList<String> remainingSparqlIDs = paramNgCopy.getReturnedSparqlIDs();
				String [] columnNames = this.getInputColumnNames(fdcNode.getFullUriName(), paramSet);
				for (String col : columnNames) {
					if (! remainingSparqlIDs.contains("?" + col)) {
						failFlag = true;
					}
				}

			}

			// dependent fdc nodes were removed from all param nodegroups and they still contain all necessary returns
			if (! failFlag) {
				// replace param nodegroups with whittled down copies
				this.paramNodegroups.put(sparqlId, ngHashEntryCopy);

				// mark this node as processed
				this.unprocessedFdcNodes.remove(sparqlId);

				// return it
				this.currFdcDataNode = fdcNode;
				this.currFdcTypeCache = FdcServiceManager.getFdcTypeCache().getSubsetWhereMatches("fdcClass", this.currFdcDataNode.getFullUriName());
				return true;
			}

		}
		
			
		if (this.unprocessedFdcNodes.size() != 0) {
			throw new Exception("Can't untangle FDC dependencies: " + this.unprocessedFdcNodes);
		} else {
			return false;
		}
	}
	
	public String[] getInputColumnNames(String className, String inputIndex) throws Exception {
		return fdcTypeCache.getSubsetWhereMatches("fdcClass", className )
				.getSubsetWhereMatches("inputIndex", inputIndex)
				.getColumnUniqueValues("columnName");
	}
	/**
	 * Return the next FDC node to be processed.
	 * @return  null unless last call to nextFdcNode() was true
	 */
	public Node getCurrentFdcNode() {
		return this.currFdcDataNode;			
	}
	
	public String getCurrentServiceUrl() throws Exception {
		return currFdcTypeCache.getCell(0, "serviceURL");
	}
	
	public String getCurrentIngestNodegroupId() throws Exception {
		return currFdcTypeCache.getCell(0, "ingestNodegroupId");
	}
	
	public String getCurrentOwlImport() {
		OntologyName classUri = new OntologyName(this.getCurrentFdcNode().getFullUriName());
		return classUri.getNamespace();
	}
	
	public int getFdcNodeCount() {
		return this.fdcDataNodes.size();
	}
	/**
	 * Get nodegroups that return params for current fdcNode.
	 * Key is the param set.
	 * @param fdcNode
	 * @return
	 * @throws Exception
	 */
	public HashMap<String, NodeGroup> getParamNodeGroups(Node fdcNode) throws Exception {
		return this.paramNodegroups.get(fdcNode.getSparqlID());
	}

	
	public static boolean ngContainsFDCNodes(NodeGroup ng, OntologyInfo oInfo) {
		return ng.getNodesBySuperclassURI(FDC_DATA_SUPERCLASS, oInfo).size() > 0;
	}

	/**
	 * Recache if 30 seconds have passed
	 * @param extConfigSei
	 * @throws Exception
	 */
	public static void suggestReCache(SparqlEndpointInterface extServicesSei, OntologyInfoClient oInfoClient) throws Exception {
		Long currMillis = System.currentTimeMillis();
		if (currMillis > lastCacheMillis + 30000) {
			LocalLogger.logToStdOut("FdcServiceManager: caching services graph");
			synchronized (lock) {
				try {
					AuthorizationManager.setSemtkSuper();
					FdcServiceManager.fdcTypeCache = SparqlGraphJson.executeSelectToTable(
							Utility.getResourceAsJson(extServicesSei, "/nodegroups/GetFdcConfig.json"), new SparqlConnection("services", extServicesSei), oInfoClient);
					lastCacheMillis = currMillis;
				} finally {
					AuthorizationManager.clearSemtkSuper();
				}
			}

		}

	}

	public static Table getFdcTypeCache() {
		synchronized (lock) {
			return FdcServiceManager.fdcTypeCache;
		}
	}

	/**
	 * For JUNIT:  query the FdcConfig
	 * @param sei
	 * @param oInfoClient
	 * @return
	 * @throws Exception
	 */
	public static Table junitGetFdcConfig(SparqlEndpointInterface sei, OntologyInfoClient oInfoClient) throws Exception {
		try {
			AuthorizationManager.setSemtkSuper();
			return SparqlGraphJson.executeSelectToTable(
					Utility.getResourceAsJson(sei, "/nodegroups/GetFdcConfig.json"), new SparqlConnection("services", sei), oInfoClient);
		} finally {
			AuthorizationManager.clearSemtkSuper();
		}
	}
}
