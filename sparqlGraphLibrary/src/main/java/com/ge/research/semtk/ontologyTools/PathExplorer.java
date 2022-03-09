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
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Path and nodegroup builder that checks instance data for path validity
 * @author 200001934
 *
 */
public class PathExplorer {
	// A NodeGroupCache for each unique cacheSei
	private static HashMap<String, NodeGroupCache> cache = new HashMap<String, NodeGroupCache>();

	private OntologyInfo oInfo;
	private SparqlConnection conn;
	private NodeGroupExecutionClient ngeClient;
	private String cacheKey;
	
	public PathExplorer(SparqlConnection conn, NodeGroupExecutionClient ngeClient, SparqlEndpointInterface cacheSei) throws Exception {
		this(new OntologyInfo(conn), conn, ngeClient, cacheSei);
	}
	
	public PathExplorer(OntologyInfo oInfo, SparqlConnection conn, NodeGroupExecutionClient ngeClient, SparqlEndpointInterface cacheSei) throws Exception {
		this.oInfo = oInfo;
		this.conn = conn;
		this.ngeClient = ngeClient;
		
		// generate key for cacheSei
		this.cacheKey = Utility.hashMD5(cacheSei.toJson().toJSONString());
		
		// init nodegroup cache from disk if needed
		if (! PathExplorer.cache.containsKey(this.cacheKey)) {
			PathExplorer.cache.put(cacheKey, new NodeGroupCache(cacheSei, oInfo));
		}
	}
	
	public NodeGroup buildNgWithData(ArrayList<ClassInstance> classInstanceList, ArrayList<ReturnRequest> returns) throws Exception {
		return this.buildNgWithData(classInstanceList, returns, new ArrayList<Triple>());
	}
	/**
	 * Checks cache first
	 * Splits returns into property or enum (TODO still ignores domainHints)
	 * Handles special case of one-node nodegroup
	 * Calls the real buildNgWithData
	 * 
	 * @param classInstanceList  - class/instance uri pairs that need to be in ng.  (instances may be null)
	 * @param returns  - data property or enum classes that need to be returned  (Data property domain must be in classInstanceList) 
	 * @return NodeGroup or null
	 * @throws Exception
	 */
	public NodeGroup buildNgWithData(ArrayList<ClassInstance> classInstanceList, ArrayList<ReturnRequest> returns, ArrayList<Triple> pathHintList) throws Exception {
		
		ArrayList<String> dataPropRetList = new ArrayList<String>();
		ArrayList<String> enumRetList = new ArrayList<String>();

		// build propRetList and enumRetList
		// TODO: actally use the ReturnRequest domainHintURI
		for (ReturnRequest r : returns) {
			String uri = r.getReturnUri();
			if (this.oInfo.containsClass(uri)) {
				enumRetList.add(uri);
			} else {
				dataPropRetList.add(uri);
			}
		}
		
	
		// try pulling from cache
		NodeGroup cached = this.getFromCache(classInstanceList);
		if (cached != null) {
			this.addDataPropReturn(cached, dataPropRetList);
			this.addEnumReturns(cached, enumRetList);

			return cached;
		}
		
		// handle special simple case: just one class
		if (classInstanceList.size() == 1) {
			NodeGroup ng = new NodeGroup();
			ng.addNodeInstance(classInstanceList.get(0).classUri, this.oInfo, classInstanceList.get(0).instanceUri);
			if (! this.addEnumReturns(ng, enumRetList)) {
				throw new Exception("Error adding enumerated class returns from : " + enumRetList.toString());
			}
			
			
			if (! this.addDataPropReturn(ng, dataPropRetList)) {
				throw new Exception("Error adding property returns from : " + dataPropRetList);
			}
			
			LocalLogger.logToStdOut("...succeeded");
			return ng;
			
		} else {
			// actual work
			return this.buildNgWithData(classInstanceList, dataPropRetList, enumRetList, pathHintList);
		}
	}
	

	/**
	 * Build a nodegroup with all classes in clasInstanceList, possibly constrained to URI's
	 * Add property and enum returns 
	 * Saves to cache 
	 *   
	 * @param classInstanceList - classes which must be in the nodegroup
	 * @param dataPropRetList - list of data properties to be returned
	 * @param objRetList - (enum) classes to be returned
	 * @return
	 * @throws Exception
	 */
	private NodeGroup buildNgWithData(ArrayList<ClassInstance> classInstanceList, ArrayList<String> dataPropRetList, ArrayList<String> objRetList, ArrayList<Triple> pathHints) throws Exception {

		// get all model paths between all pairs
		ArrayList<OntologyPath> pathList = new ArrayList<OntologyPath>(); 
		ArrayList<ClassInstance> anchorInstanceList = new ArrayList<ClassInstance>();
		ArrayList<ClassInstance> endInstanceList = new ArrayList<ClassInstance>();
		
		// lists of problems for each path
		ArrayList<ArrayList<ClassInstance>> missingClassLists = new ArrayList<ArrayList<ClassInstance>>();
		ArrayList<ArrayList<String>> unaddablePropLists = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<Triple>> missingTripleHintsLists  = new ArrayList<ArrayList<Triple>>();
		
		int smallest = 9999;
		final boolean LOG_PATHS = true;
		for (int i=0; i < classInstanceList.size() - 1; i++) {
			for (int j=i+1; j < classInstanceList.size(); j++) {
				
				if (LOG_PATHS) LocalLogger.logToStdOut(classInstanceList.get(j).classUri + "," + classInstanceList.get(i).classUri);
				for (OntologyPath path : this.oInfo.findAllPaths(classInstanceList.get(j).classUri, classInstanceList.get(i).classUri)) {
					pathList.add(path);
					if (LOG_PATHS) LocalLogger.logToStdOut(path.asString());
					anchorInstanceList.add(classInstanceList.get(i));
					endInstanceList.add(classInstanceList.get(j));
					
					// calculate missing instances for path
					ArrayList<ClassInstance> missingClassList = path.calcMissingClasses(classInstanceList);
					ArrayList<String> unaddablePropsList = path.calcUnaddableDataProps(dataPropRetList, this.oInfo);
					ArrayList<Triple> missingPathHints = path.caclMissingTripleHints(pathHints);					
					
					missingClassLists.add(missingClassList);
					unaddablePropLists.add(unaddablePropsList);
					missingTripleHintsLists.add(missingPathHints);

					// PEC TODO: this logic is too simple
					// why would all of these be "weighted" the same
					// Note that missingClasses could be added below if no linear path finds them all
					if (missingClassList.size() + unaddablePropsList.size() + missingPathHints.size() < smallest) {
						smallest = missingClassList.size() + unaddablePropsList.size() + missingPathHints.size();
					}
				}
			}
		}
		
		// PEC TODO: also tenuous logic.  Not even sure this error is correct.
		if (smallest >= classInstanceList.size() + dataPropRetList.size() + pathHints.size()) {
			throw new Exception("Could not find instance(s)");
		}
		
		// loop through paths with least missing classes first
		for (int size=smallest; size < classInstanceList.size() + dataPropRetList.size() + pathHints.size(); size++) {
			for (int i = 0; i < pathList.size(); i++) {
				
				// loop through paths of length "size"
				boolean success = true;
				if (missingClassLists.get(i).size() + unaddablePropLists.get(i).size() + missingTripleHintsLists.get(i).size()== size) {
					if (LOG_PATHS) LocalLogger.logToStdOut("buildNgWithData trying path: " + pathList.get(i).asString());
					
					// build nodegroup with anchor node
					NodeGroup ng = new NodeGroup();
					Node anchor = null;
					if (anchorInstanceList.get(i) == null) {
						anchor = ng.addNode(anchorInstanceList.get(i).classUri, this.oInfo);
					} else {
						anchor = ng.addNodeInstance(anchorInstanceList.get(i).classUri, this.oInfo, anchorInstanceList.get(i).instanceUri);
					}
					
					// continue only iff this path has instances
					if (this.pathHasInstance(ng, anchor, pathList.get(i), endInstanceList.get(i).instanceUri)) {
						
						// add the path
						Node added = ng.addPath(pathList.get(i), anchor, oInfo);
						if (endInstanceList.get(i).instanceUri != null) {
							added.addValueConstraint(ValueConstraint.buildFilterInConstraint(added, endInstanceList.get(i).instanceUri));
						}
						
						// try adding all the missing instances
						for (ClassInstance missingInstance : missingClassLists.get(i)) {
							Node addedMissing = this.addClassFirstPath(ng, missingInstance.classUri, missingInstance.instanceUri);
							if (addedMissing == null) {
								success = false;
								LocalLogger.logToStdOut("...failed to add class: " + missingInstance.classUri);
								break;
							}
						}
						
						// PEC TODO HERE:  If we added more classes, we need to re-check that missingPathHints is empty
						//                 This only checks if missingPathHints was empty to begin with.
						if (missingTripleHintsLists.get(i).size() != 0) {
							if (missingClassLists.get(i).size() > 0) {
								throw new Exception("Not implmented: re-checking triple hints after added classes after initial path.");
							} else {
								// still missing triple hints
								break;
							}
						}
						
						// classes to add but failed
						if (! success) 
							break;
				
						// make a copy in hopes we can cache this
						NodeGroup cacheNg = NodeGroup.deepCopy(ng);
						
						success = this.addEnumReturns(ng, objRetList);
						if (! success) 
							break;
						
						success = this.addDataPropReturn(ng, dataPropRetList);
						if (! success) 
							break;
						
						LocalLogger.logToStdOut("...succeeded");
						this.putToCache(cacheNg, classInstanceList);
						return ng;
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Adds enum classes to a nodegroup.
	 * @param ng
	 * @param enumRetList
	 * @return
	 * @throws Exception
	 */
	private boolean addEnumReturns(NodeGroup ng, ArrayList<String> enumRetList) throws Exception {
		// add all enum returns
		// TODO: this presumes enums are always add-ons at the end
		for (String enumClassUri : enumRetList) {
			Node addedEnum = this.addClassFirstPath(ng, enumClassUri);
			ng.setIsReturned(addedEnum, true);
			if (addedEnum == null) {
				LocalLogger.logToStdOut("...failed to add enum: " + enumClassUri);
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Adds property returns to every class containing them
	 * @param ng
	 * @param propRetList
	 * @return false if any property could not be found
	 */
	private boolean addDataPropReturn(NodeGroup ng, ArrayList<String> propRetList) throws Exception {
	
		// TODO: does not attempt to add the prop's class if it's missing
		
		for (String propUri : propRetList) {
			boolean found = false;
			for (Node n : ng.getNodeList()) {
				PropertyItem propItem = n.getPropertyByURIRelation(propUri);
				if (propItem != null) {
					ng.setIsReturned(propItem, true);
					propItem.setOptMinus(PropertyItem.OPT_MINUS_OPTIONAL);
					found = true;
				}
			}
			if (!found) {
				LocalLogger.logToStdErr("...failed to add prop: " + propUri);
				return false;
			}
		}
		return true;
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
		ArrayList<OntologyPath> pathList = oInfo.findAllPaths(classUri, classList);
		
		// for each possible path
		for (OntologyPath path : pathList) {
			
			// for each possible connection point
			for (Node connectPoint : ng.getNodesByURI(path.getAnchorClassName())) {
				
				if (this.pathHasInstance(ng, connectPoint, path, instanceUri)) {
					// first success: modify ng and return true
					return ng.addPath(path, ng.getNodeBySparqlID(connectPoint.getSparqlID()), this.oInfo);
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
	private NodeGroup getFromCache(ArrayList<ClassInstance> classInstanceList) throws Exception {
		String ngKey = getNgKey(classInstanceList);
		NodeGroup ng = null;
		
		try {
			// get nodegroup
			ng = PathExplorer.cache.get(this.cacheKey).get(ngKey);
			
		} catch (ValidationException e) {
			// on ValidationError: delete and return null
			LocalLogger.logToStdErr("Error validating cached nodegroup.  Deleting id " + ngKey);
			PathExplorer.cache.get(this.cacheKey).delete(ngKey);
			return null;
		}
		
		if (ng != null) {
			// add in classInstanceList
			for (int i=0; i < classInstanceList.size(); i++) {
				String instanceUri = classInstanceList.get(i).instanceUri;
				if (instanceUri != null) {
					Node n = ng.getNodeBySparqlID("classInstance_" + String.valueOf(i));
					if (n == null) {
						LocalLogger.logToStdErr("Error trying to add instance to cached nodegroup.  Deleting id " + ngKey);
						PathExplorer.cache.get(this.cacheKey).delete(ngKey);
						ng = null;
					} else {
						n.addValueConstraint(ValueConstraint.buildFilterInConstraint(n, classInstanceList.get(i).instanceUri));
					}
				}
			}
		}
		
		return ng;
	}
	
	private void putToCache(NodeGroup ng, ArrayList<ClassInstance> classInstanceList) throws Exception {
		
		// remove all node value contraints
		for (Node n : ng.getNodeList()) {
			for (int i=0; i < classInstanceList.size(); i++) {
				String constraint = n.getValueConstraintStr();
				String instanceUri = classInstanceList.get(i).instanceUri;
				if (constraint != null && instanceUri != null && constraint.contains(instanceUri)) {
					ng.changeSparqlID(n, "classInstance_" + String.valueOf(i));
					break;
				}
			}
			n.setValueConstraint(null);
		}
		
		// generate a key for this nodegroup
		String ngKey = getNgKey(classInstanceList);
		
		// generate some comments showing class list
		ArrayList<String> classList = ClassInstance.getClassList(classInstanceList);
		Collections.sort(classList);
		String comments = classList.toString();
		
		// add to cache
		PathExplorer.cache.get(this.cacheKey).put(ngKey, ng, conn, comments);
	}
	
	/**
	 * Key is a hash of sorted classes in classInstanceList
	 * @param classInstanceList
	 * @return
	 * @throws Exception
	 */
	private static String getNgKey(ArrayList<ClassInstance> classInstanceList) throws Exception {
		ArrayList<String> classList = ClassInstance.getClassList(classInstanceList);
		Collections.sort(classList);
	    
	    return Utility.hashMD5(classList.toString());
	}

}
