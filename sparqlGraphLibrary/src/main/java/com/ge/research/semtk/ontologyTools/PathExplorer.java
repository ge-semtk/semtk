/**
 ** Copyright 2019 General Electric Company
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


package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.NodeItem;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlToXLib.SparqlToXLibUtil;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Path and nodegroup builder that checks instance data for path validity
 * @author 200001934
 *
 *
 * Todo:  
 * 	- add predicate stats so that findAllPaths() can use it
 *	- decide on approach for finding paths for FDC or equation chaining
 *		- add to predicate stats?   Also need to incorporate into: this.pathHasInstance()	
 *		- add a list of crap that this.pathHasInstance can check
 *		- try with instance data first, then model
 *		- know we care and go straight to only checking the model
 *
 *  - re-understand the path caching
 *
 */
public class PathExplorer {
	// A NodeGroupCache for each unique cacheSei in memory 
	private static HashMap<String, NodeGroupCache> cache = new HashMap<String, NodeGroupCache>();
	private static String NO_CACHE = "No cache";
	private OntologyInfo oInfo;
	private SparqlConnection conn;
	private NodeGroupExecutionClient ngeClient;
	private String cacheKey;
	private PredicateStats predStats;  
	
	public PathExplorer(SparqlConnection conn, NodeGroupExecutionClient ngeClient, SparqlEndpointInterface cacheSei) throws Exception {
		this(new OntologyInfo(conn), conn, ngeClient, cacheSei);
	}
	
	/**
	 * 
	 * @param oInfo
	 * @param conn - graphs being explored
	 * @param ngeClient
	 * @param cacheSei - location of ng cache, or null     WRONG: this is the Cache key.  The sei being explored.
	 * @throws Exception
	 */
	public PathExplorer(OntologyInfo oInfo, SparqlConnection conn, NodeGroupExecutionClient ngeClient, SparqlEndpointInterface cacheSei) throws Exception {
		this(oInfo, conn, ngeClient, cacheSei, null);
	}
	
	/**
	 * 
	 * @param oInfo
	 * @param conn
	 * @param ngeClient
	 * @param cacheSei
	 * @param predStats - if null path-finding uses this.pathHasInstance(), if non-null search each hop in PredicateStats
	 * @throws Exception
	 */
	public PathExplorer(OntologyInfo oInfo, SparqlConnection conn, NodeGroupExecutionClient ngeClient, SparqlEndpointInterface cacheSei, PredicateStats predStats) throws Exception {
		this.oInfo = oInfo;
		this.conn = conn;
		this.ngeClient = ngeClient;
		this.predStats = predStats;
		
		// generate key for cacheSei, or use NO_CACHE
		this.cacheKey = cacheSei != null ? Utility.hashMD5(cacheSei.toJson().toJSONString()) : NO_CACHE;
		
		// init nodegroup cache
		if (this.cacheKey != NO_CACHE && ! PathExplorer.cache.containsKey(this.cacheKey)) {
			PathExplorer.cache.put(cacheKey, new NodeGroupCache(cacheSei, oInfo));
		}
	}
	

	/**
	 * Checks cache first
	 * Splits returns into property or enum (TODO still ignores domainHints)
	 * Handles special case of one-node nodegroup
	 * 
	 * @param classInstanceList  - class/instance uri pairs that need to be in ng.  (instances may be null)
	 * @param returns  - data property or enum classes that need to be returned  (Data property domain must be in classInstanceList) 
	 * @return NodeGroup or null
	 * @throws Exception
	 */
	public NodeGroup buildNgWithData(ArrayList<PathItemRequest> requestList) throws Exception {
		
	
		// try pulling from cache
		NodeGroup cached = this.getFromCache(requestList);
		if (cached != null) {
			this.addNodegroupInstances(cached, requestList);
			this.addNodegroupReturns(cached, requestList);
			LocalLogger.logToStdOut("Retrieved nodegroup from cache");
			return cached;
		}
		LocalLogger.logToStdOut("Building new nodegroup");
		
		// error-check instances
		for (PathItemRequest req : requestList) {
			String uri = req.getInstanceUri();
			if (uri != null && ! uri.isBlank()) {
				if (!this.instanceHasClassTriple(uri)) {
					throw new Exception("Can't find instance of class " + req.getClassUri() + ": " + uri);
				}
			}
		}
		
		// handle special simple  case: just one class
		if (requestList.size() == 1) {
			
			NodeGroup ng = new NodeGroup();
			Node n = ng.addNode(requestList.get(0).getClassUri(), this.oInfo);
			ng.constrainNodeToInstance(n,  requestList.get(0).getInstanceUri());
			this.addNodegroupReturns(ng, requestList);
			
			LocalLogger.logToStdOut("...succeeded");
			return ng;
			
		} 
	

		// get all model paths between all pairs
		ArrayList<OntologyPath> pathList = new ArrayList<OntologyPath>(); 
		ArrayList<PathItemRequest> anchorRequestList = new ArrayList<PathItemRequest>();
		ArrayList<PathItemRequest> endRequestList = new ArrayList<PathItemRequest>();
		
		// lists of problems for each path
		ArrayList<ArrayList<PathItemRequest>> missingClassLists = new ArrayList<ArrayList<PathItemRequest>>();
		ArrayList<ArrayList<PathItemRequest>> missingTripleHintsLists  = new ArrayList<ArrayList<PathItemRequest>>();
		
		// build all paths that might have data
		int smallest = 9999;
		final boolean LOG_PATHS = true;
		for (int i=0; i < requestList.size() - 1; i++) {
			PathItemRequest anchorRequest = requestList.get(i);
			OntologyPath anchorRequestPath = anchorRequest.getIncomingPath();
			
			for (int j=i+1; j < requestList.size(); j++) {
				
				PathItemRequest endRequest = requestList.get(j);
				OntologyPath endRequestPath = endRequest.getIncomingPath();
				
				if (LOG_PATHS) LocalLogger.logToStdOut(endRequest.getClassUri() + "," + anchorRequest.getClassUri());
				
				// candidate Paths use predicate stats : only existing data
				ArrayList<OntologyPath> candidatePaths = this.oInfo.findAllPaths(endRequest.getClassUri() , anchorRequest.getClassUri(), this.predStats);
				
				for (OntologyPath path : candidatePaths ) {
					
					// if either request contains a path (incoming class, incoming predicate) then path must contain it.
					if ((anchorRequestPath == null || path.containsSubPath(anchorRequestPath)) &&
							(endRequestPath == null || path.containsSubPath(endRequestPath)) ) {
						
						pathList.add(path);
						if (LOG_PATHS) LocalLogger.logToStdOut(path.asString());
						anchorRequestList.add(anchorRequest);
						endRequestList.add(endRequest);
						
						// calculate missing instances for path
						ArrayList<PathItemRequest> missingClassList = this.calcMissingClasses(path, requestList);
						ArrayList<PathItemRequest> missingPathHints = this.calcMissingTripleHints(path, requestList);					
						
						missingClassLists.add(missingClassList);
						missingTripleHintsLists.add(missingPathHints);
	
						// PEC TODO: this logic is too simple
						// why would all of these be "weighted" the same
						// Note that missingClasses could be added below if no linear path finds them all
						if (missingClassList.size() + missingPathHints.size() < smallest) {
							smallest = missingClassList.size() + missingPathHints.size();
						}
					}
				}
			}
		}
		
		// choose first good path
		// loop through paths with least missing classes first
		for (int size=smallest; size < requestList.size(); size++) {
			for (int i = 0; i < pathList.size(); i++) {
				// loop through paths of length "size"
				boolean success = true;
				if (missingClassLists.get(i).size() + missingTripleHintsLists.get(i).size()== size) {
					if (LOG_PATHS) LocalLogger.logToStdOut("buildNgWithData trying path: " + pathList.get(i).asString());
					
					// build nodegroup with anchor node
					NodeGroup ng = new NodeGroup();
					Node anchor = null;
					
					PathItemRequest anchorRequest = anchorRequestList.get(i);
					PathItemRequest endRequest = endRequestList.get(i);
					
					anchor = ng.addNode(anchorRequest.getClassUri(), this.oInfo);
					ng.constrainNodeToInstance(anchor, anchorRequest.getInstanceUri());
					
					// Two modes of operation:
					// 1) we're using predStats != null OR 
					// 2) pathHasInstance()
					if (this.predStats != null || this.pathHasInstance(ng, anchor, pathList.get(i), endRequest.getInstanceUri())) {
						
						// add the path
						Node added = ng.addPath(pathList.get(i), anchor, oInfo);
						ng.constrainNodeToInstance(added, endRequest.getInstanceUri());
						
						// try adding all the missing instances
						for (PathItemRequest missingClassRequest : missingClassLists.get(i)) {
							if (missingClassRequest.getIncomingClassUri() != null) {
								// there's a path in the request: add it at desired location
								try {
									ArrayList<Node> nList = ng.getNodesByURI(missingClassRequest.getIncomingClassUri());
									if (nList.size() != 1) throw new Exception("Did not find exactly one place to add: " + missingClassRequest.getClassUri());
									Node existingNode = nList.get(0);
									ng.addNode(missingClassRequest.getClassUri(), existingNode, null, missingClassRequest.getIncomingPropUri());
								
								} catch (Exception e) {
									// silently break on any error
									LocalLogger.logToStdOut(e.getMessage());
									success = false;
									break;
								}
								
							} else {
								// no path in request: addClassFirstPath()
								Node addedMissing = this.addClassFirstPath(ng, missingClassRequest.getClassUri(), missingClassRequest.getInstanceUri());
								if (addedMissing == null) {
									success = false;
									LocalLogger.logToStdOut("...failed addClassFirstPath: " + missingClassRequest.getClassUri());
									break;
								}
							}
						}
						
						// if we started off with missing triple hints make sure they're fixed
						if (success && missingTripleHintsLists.get(i).size() != 0) {
							// re-check that everything is cleared up
							if (this.calcMissingClassesAndTripleHints(ng, requestList).size() > 0) {
								success = false;
							}
						}
						
						// classes to add but failed
						if (success) {
							this.putToCache(NodeGroup.deepCopy(ng), requestList);
							this.addNodegroupReturns(ng, requestList);
							LocalLogger.logToStdOut("...succeeded");
							return ng;
						}
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Return any triples that don't appear in the path
	 * @param requestList
	 * @return
	 */
	public ArrayList<PathItemRequest> calcMissingTripleHints(OntologyPath path, ArrayList<PathItemRequest> requestList) {
		ArrayList<PathItemRequest> ret = new ArrayList<PathItemRequest>();
		
		for (PathItemRequest req : requestList) {
			boolean found = false;
			Triple hint = req.getTripleHint();
			if (hint != null) {
				for (Triple t : path.getTripleList()) {
					found = (hint.getSubject().equals(t.getSubject())) &&
							 (hint.getPredicate().equals(t.getPredicate())) &&
						     (hint.getObject().equals(t.getObject())     );
					if (found) break;
				}
				if (!found) {
					ret.add(req);
				}
			}
		}
		return ret;
	}
	
	
	/**
	 * Return any triples that don't appear in the path
	 * "" subject, pred, or object will match anything.
	 * @param requestList
	 * @return
	 */
	public ArrayList<PathItemRequest> calcMissingClassesAndTripleHints(NodeGroup ng, ArrayList<PathItemRequest> requestList) {
		ArrayList<PathItemRequest> ret = new ArrayList<PathItemRequest>();
		
		for (PathItemRequest req : requestList) {
			boolean found = false;
			Triple hint = req.getTripleHint();
			if (hint != null) {
				for (Node n : ng.getNodesByURI(hint.getSubject())) {
					NodeItem nItem = n.getNodeItem(hint.getPredicate());
					if (nItem != null) {
						for (Node objectNode :  nItem.getNodeList()) {
							if (objectNode.getUri().equals(hint.getObject())) {
								found = true;
								break;
							}
						}
					}
					if (found) break;
				}
				
				if (!found) {
					ret.add(req);
				}
			} else {
				// no path hint, just check that classUri exists
				if (ng.getNodesByURI(req.getClassUri()) == null) {
					ret.add(req);
				}
			}
		}
		return ret;
	}
	/**
	 * Return copy of classList with classes from the path removed
	 * @param classList
	 * @return
	 */
	public ArrayList<PathItemRequest> calcMissingClasses(OntologyPath path, ArrayList<PathItemRequest> requestList) {
		
		ArrayList<String> pathClassList = path.getClassList();
		
		ArrayList<PathItemRequest> ret = new ArrayList<PathItemRequest>();
		
		for (PathItemRequest req : requestList) {
			int i = pathClassList.indexOf(req.getClassUri());
			if (i > -1) {
				// found.  Remove from classList in order to correctly count duplicates
				pathClassList.remove(i);
			} else {
				// not found: add to returns
				ret.add(req);
			}
		}
		return ret;
	}
	
	/**
	 * Adds returns to nodegroup  PREREQUISITE: add instances first
	 * @param ng
	 * @param propRetList
	 * @return false if any property could not be found
	 */
	private boolean addNodegroupReturns(NodeGroup ng, ArrayList<PathItemRequest> requestList) throws Exception {
	
		// TODO: does not attempt to add the prop's class if it's missing
		ArrayList<String> allReturns = new ArrayList<String>();
		
		for (PathItemRequest request : requestList) {
			if (request.getPropUriList() != null) {
				Node foundNode = this.findNodeMatchingRequest(ng, request, true);
				
				if (request.getPropUriList() == null || request.getPropUriList().size() == 0) {
					// No properties.  Return the node.
					ArrayList<NodeItem> incoming = ng.getConnectingNodeItems(foundNode);
					if (incoming != null && incoming.size() > 0) {
						incoming.get(0).setOptionalMinus(foundNode, NodeItem.OPTIONAL_TRUE);
					}
					ng.setIsReturned(foundNode, true);
					allReturns.add(foundNode.getBindingOrSparqlID());
					
				} else {
					for (String propUri : request.getPropUriList()) {
						PropertyItem propItem = foundNode.getPropertyByURIRelation(propUri);
						if (propItem == null) throw new Exception("Could not find property " + propUri + " in node " + foundNode.getUri());
						
						// funcs
						String func = request.getFuncName(propUri);
						
						String sparqlID = request.getSparqlID(propUri);
						if (sparqlID != null) {
							ng.changeSparqlID(propItem, sparqlID);
						}
						ng.setIsReturned(propItem, true);
						allReturns.add(propItem.getBindingOrSparqlID());
						
						if (func != null) {
							propItem.setOptMinus(PropertyItem.OPT_MINUS_NONE);
							ng.appendOrderBy(propItem.getBindingOrSparqlID(), func.equals("MAX") ? "DESC" : "ASC");
							ng.setLimit(1);
						} else {
							propItem.setOptMinus(PropertyItem.OPT_MINUS_OPTIONAL);
						}
						
						// filters
						String filter = request.getFilter(propUri);
						if (filter != null) {
							String split[] = filter.split("\\s+");
							propItem.setValueConstraint(new ValueConstraint(ValueConstraint.buildFilterConstraint(propItem, split[0], split[1])));
							ng.appendOrderBy(propItem.getBindingOrSparqlID(), split[0].contains("<") ? "DESC" : "ASC");
							propItem.setOptMinus(PropertyItem.OPT_MINUS_NONE);
						}
					}
				}
			}
		}
		
		// try column order in request order
		for (String id : allReturns) {
			ng.appendColumnOrder(id);
		}
		return true;
	}
	
	/**
	 * Adds returns to nodegroup
	 * @param ng
	 * @param propRetList
	 * @return false if any property could not be found
	 */
	private boolean addNodegroupInstances(NodeGroup ng, ArrayList<PathItemRequest> requestList) throws Exception {
	
		for (PathItemRequest request : requestList) {
			if (request.getInstanceUri() != null) {
				Node foundNode = this.findNodeMatchingRequest(ng, request, false);
				
				foundNode.addValueConstraint(ValueConstraint.buildFilterInConstraint(foundNode, request.getInstanceUri()));
			}
		}
		return true;
	}
	
	/**
	 * Find node in nodegroup because it has request's class and 
	 *      - instance matches if any, or
	 *      - incoming property matches if any, or
	 *      - no instance or incomingProp in the request
	 * @param ng
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public Node findNodeMatchingRequest(NodeGroup ng, PathItemRequest request, boolean matchInstance) throws Exception {
		for (Node n : ng.getNodesByURI(request.getClassUri())) {
			// match node because request has instance and node has a value constraint
			if ( matchInstance && request.getInstanceUri() != null && n.getValueConstraint() != null && n.getValueConstraintStr().contains(request.getInstanceUri())) {
				return n;
			
			// match node because request has incoming prop and node has same incoming prop
			} else if (request.getIncomingPropUri() != null) {
				
				for (NodeItem nItem : ng.getNodeItems(request.getIncomingPropUri())) {
					if (nItem.getNodeList().contains(n)) {
						return n;
					}
				}
			// else match simply on class
			} else {
				return n;
			}
		}
			
		throw new Exception("Could not find " + request.getClassUri() + " in the nodegroup");
	}
	
	public Node addClassFirstPath(NodeGroup ng, String classUri) throws Exception {
		return this.addClassFirstPath(ng, classUri, null);
	}


	/**
	 * Starting with all legal paths from oInfo
	 * Find the first one that would actually have some data in it.
	 * @param ng - starting nodegroup (which must have some data)
	 * @param classUri - class to add
	 * @return - boolean : was ng modified
	 * @throws Exception
	 */
	public Node addClassFirstPath(NodeGroup ng, String classUri, String instanceUri) throws Exception {
		
		
		ArrayList<Node> nodeList = ng.getNodeList();
		HashSet<String> classSet = new HashSet<String>();
		
		// get unique list of node URI's in ng
		for (Node n : nodeList) {
			classSet.add(n.getFullUriName());
		}
		
		// get list of all oInfo model paths from classUri to the ng nodes.
		HashSet<String> classList = new HashSet<String>();
		classList.addAll(classSet);
		ArrayList<OntologyPath> pathList = oInfo.findAllPaths(classUri, classList, this.predStats);
		
		// for each possible path
		for (OntologyPath path : pathList) {
			
			// for each possible connection point
			for (Node connectPoint : ng.getNodesByURI(path.getAnchorClassName())) {
				
				// Success requires both: 
				//    path exists either in predicate stats or pathHasInstance() 
				//    request path is null or new path contains the request path
				if (this.predStats != null || this.pathHasInstance(ng, connectPoint, path, instanceUri)) {
					// first success: modify ng and return true
					Node n =  ng.addPath(path, ng.getNodeBySparqlID(connectPoint.getSparqlID()), this.oInfo);
					ng.constrainNodeToInstance(n, instanceUri);
					return n;
				} 
			}
		}
		
		// couldn't find any legal path
		return null;
	}
	
	/**
	 * Build copy of ng and see if it is supported by instance data
	 * @param ng
	 * @param anchorNode
	 * @param path
	 * @param endClassInstanceUri
	 * @return
	 * @throws Exception - exceptions building the nodegroup, which are probably logic errors
	 */
	public boolean pathHasInstance(NodeGroup ng, Node anchorNode, OntologyPath path, String endClassInstanceUri) throws Exception {
		
		// add node with given path
		NodeGroup ngTemp = NodeGroup.deepCopy(ng);
		Node connectPointTemp = ngTemp.getNodeBySparqlID(anchorNode.getSparqlID());
		Node nodeAdded = ngTemp.addPath(path, connectPointTemp, this.oInfo);
		// return new node
		nodeAdded.setIsReturned(true);
		
		if (endClassInstanceUri == null) {
			// no instance: make it unique from any other nodes with same classURI
			ngTemp.addUniqueInstanceConstraint(nodeAdded);
		} else {
			// else constrain node to instance early (before we check data)
			nodeAdded.addValueConstraint(ValueConstraint.buildFilterInConstraint(nodeAdded, endClassInstanceUri));
		}
		
		// see if any exist in instance data
		ngTemp.setLimit(1);
		long count = 0;
		try {
			count = this.ngeClient.dispatchCountByNodegroup(ngTemp, this.conn, null, null);
		} catch (Exception e) {
			// ignore FDC errors on bad paths
			LocalLogger.logToStdOut("Error during path " + path.toJson().toJSONString() + "\nconnecting id: " + nodeAdded.getSparqlID() + "\nMessage:" + e.getMessage());
		}
		
		return (count > 0);
	}
	
	/**
	 * Find this object's cache using seiKey and then look for nodegroup
	 * @param classInstanceList
	 * @param enumRetList
	 * @param propRetList
	 * @return null if nodegroup is not there
	 * @throws Exception
	 */
	private NodeGroup getFromCache(ArrayList<PathItemRequest> requestList) throws Exception {
		NodeGroup ng = null;
		String ngKey = getNgKey(requestList);
		
		if (this.cacheKey != NO_CACHE) {
			try {
				// get nodegroup
				ng = PathExplorer.cache.get(this.cacheKey).get(ngKey);
				
			} catch (ValidationException e) {
				// on ValidationError: delete and return null
				LocalLogger.logToStdErr("Error validating cached nodegroup.  Deleting id " + ngKey);
				PathExplorer.cache.get(this.cacheKey).delete(ngKey);
				return null;
			}
		}
		
		return ng;
	}
	
	private void putToCache(NodeGroup ng, ArrayList<PathItemRequest> requestList) throws Exception {
		
		// add to cache unless NO_CACHE
		if (this.cacheKey != NO_CACHE) {
			String ngKey = PathExplorer.getNgKey(requestList);
			ng.unsetAllConstraints();
			ng.unsetAllReturns();
			
			// generate some comments showing class list
			ArrayList<String> classList = new ArrayList<String>();
			classList.addAll(PathItemRequest.getClassesInList(requestList));
			Collections.sort(classList);
			String comments = classList.toString();
		
			PathExplorer.cache.get(this.cacheKey).put(ngKey, ng, conn, comments);
		}
	}
	
	/**
	 * Key is a hash of classes and path hints
	 * @param requestList
	 * @return
	 * @throws Exception
	 */
	private static String getNgKey(ArrayList<PathItemRequest> requestList) throws Exception {
		ArrayList<String> keyList = new ArrayList<String>();
		ArrayList<String> tmpList = new ArrayList<String>();
		
		for (PathItemRequest req : requestList) {
			HashSet<String> set = req.getClassList();
			if (set.size() > 0) 
				tmpList.addAll(set);
		}
		Collections.sort(tmpList);
		keyList.addAll(tmpList);
		
		for (PathItemRequest req : requestList) {
			Triple t = req.getTripleHint();
			if (t != null) 
				tmpList.add(t.toCsvString());
		}
		Collections.sort(tmpList);
		keyList.addAll(tmpList);
	    
	    return Utility.hashMD5(keyList.toString());
	}

	/**
	 * Check if a uri has a triple:  <uri> a ?class
	 * @param instanceUri
	 * @return
	 * @throws Exception
	 */
	public boolean instanceHasClassTriple(String instanceUri) throws Exception {
		String sparql = SparqlToXLibUtil.generateGetInstanceClass(this.conn, this.oInfo, instanceUri);
		Table targetTab = this.conn.getDefaultQueryInterface().executeQueryToTable(sparql);
		return (targetTab.getNumRows() > 0);
	}
}
