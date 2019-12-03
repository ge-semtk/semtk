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


import static org.hamcrest.CoreMatchers.containsString;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.bind.DatatypeConverter;

import com.ge.research.semtk.api.nodeGroupExecution.client.NodeGroupExecutionClient;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Path and nodegroup builder that checks instance data for path validity
 * @author 200001934
 *
 */
public class PathExplorer {
	// TODO write this to disk and learn over time.
	private static HashMap <String, NodeGroup> cache = new HashMap <String, NodeGroup>();

	private OntologyInfo oInfo;
	private SparqlConnection conn;
	private NodeGroupExecutionClient ngeClient;
	
	public PathExplorer(SparqlConnection conn, NodeGroupExecutionClient ngeClient) throws Exception {
		this.oInfo = new OntologyInfo(conn);
		this.conn = conn;
		this.ngeClient = ngeClient;
	}
	
	public PathExplorer(OntologyInfo oInfo, SparqlConnection conn, NodeGroupExecutionClient ngeClient) {
		this.oInfo = oInfo;
		this.conn = conn;
		this.ngeClient = ngeClient;
	}
	
	/**
	 * Find a nodegroup that contains all the nodes and properties in uriArray
	 * and has some data
	 * 
	 * Missing:
	 *   - presumes that all properties are data properties
	 *   - won't handle branch
	 * 
	 * 
	 * @param classInstanceList  - class/instance uri pairs that need to be in ng.  (instances may be null)
	 * @param returns  - data property or enum classes that need to be returned  (Data property domain must be in classInstanceList) 
	 * @return NodeGroup or null
	 * @throws Exception
	 */
	public NodeGroup buildNgWithData(ArrayList<ClassInstance> classInstanceList, ArrayList<String> returns) throws Exception {
		
		ArrayList<String> propRetList = new ArrayList<String>();
		ArrayList<String> enumRetList = new ArrayList<String>();

		// build propRetList and enumRetList
		for (String uri : returns) {
			if (this.oInfo.containsClass(uri)) {
				enumRetList.add(uri);
			} else {
				propRetList.add(uri);
			}
		}
		
		// try pulling from static cache
		NodeGroup cached = checkCache(classInstanceList, enumRetList, propRetList);
		if (cached != null) {
			this.addDataPropReturn(cached, propRetList);
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
			
			
			if (! this.addDataPropReturn(ng, propRetList)) {
				throw new Exception("Error adding property returns from : " + propRetList);
			}
			
			LocalLogger.logToStdOut("...succeeded");
			return ng;
		}
		
		// get all model paths between all pairs
		ArrayList<OntologyPath> pathList = new ArrayList<OntologyPath>(); 
		ArrayList<ClassInstance> anchorInstanceList = new ArrayList<ClassInstance>();
		ArrayList<ClassInstance> endInstanceList = new ArrayList<ClassInstance>();
		
		ArrayList<ArrayList<ClassInstance>> missingClassLists = new ArrayList<ArrayList<ClassInstance>>();
		ArrayList<ArrayList<String>> missingPropLists = new ArrayList<ArrayList<String>>();

		int smallest = 9999;
		
		for (int i=0; i < classInstanceList.size() - 1; i++) {
			for (int j=i+1; j < classInstanceList.size(); j++) {
				
				for (OntologyPath path : this.oInfo.findAllPaths(classInstanceList.get(j).classUri, classInstanceList.get(i).classUri)) {
					pathList.add(path);
					anchorInstanceList.add(classInstanceList.get(i));
					endInstanceList.add(classInstanceList.get(j));
					
					// calculate missing instances for path
					ArrayList<ClassInstance> missingClassList = path.calcMissingInstances(classInstanceList);
					ArrayList<String> missingPropList = path.calcMissingProperties(propRetList, this.oInfo);
					missingClassLists.add(missingClassList);
					missingPropLists.add(missingPropList);

					if (missingClassList.size() + missingPropList.size() < smallest) {
						smallest = missingClassList.size() + missingPropList.size();
					}
				}
			}
		}
		
		// loop through paths with least missing classes first
		for (int size=smallest; size < classInstanceList.size(); size++) {
			for (int i = 0; i < pathList.size(); i++) {
				boolean success = true;
				if (missingClassLists.get(i).size() + missingPropLists.get(i).size() == size) {
					LocalLogger.logToStdOut("buildNgWithData trying path: " + pathList.get(i).asString());
					// build nodegroup with anchor node
					NodeGroup ng = new NodeGroup();
					Node anchor = null;
					if (anchorInstanceList.get(i) == null) {
						anchor = ng.addNode(anchorInstanceList.get(i).classUri, this.oInfo);
					} else {
						anchor = ng.addNodeInstance(anchorInstanceList.get(i).classUri, this.oInfo, anchorInstanceList.get(i).instanceUri);
					}
					
					// if this path has instances
					if (this.pathHasInstance(ng, anchor, pathList.get(i), endInstanceList.get(i).instanceUri)) {
						
						// add the path
						Node added = ng.addPath(pathList.get(i), anchor, oInfo);
						if (endInstanceList.get(i).instanceUri != null) {
							added.addValueConstraint(ValueConstraint.buildValuesConstraint(added, endInstanceList.get(i).instanceUri));
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
						if (! success) 
							break;
				
						// make a copy in hopes we can cache this
						NodeGroup cacheNg = NodeGroup.deepCopy(ng);
						
						success = this.addEnumReturns(ng, enumRetList);
						if (! success) 
							break;
						
						success = this.addDataPropReturn(ng, propRetList);
						if (! success) 
							break;
						
						LocalLogger.logToStdOut("...succeeded");
						addCache(cacheNg, classInstanceList);
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
				LocalLogger.logToStdOut("...failed to add prop: " + propUri);
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
		ArrayList<String> classList = new ArrayList<String>();
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
			nodeAdded.addValueConstraint(ValueConstraint.buildValuesConstraint(nodeAdded, endClassInstanceUri));
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
	
	private static NodeGroup checkCache(ArrayList<ClassInstance> classInstanceList, ArrayList<String> enumRetList, ArrayList<String> propRetList) throws Exception {
		String key = getCacheKey(classInstanceList);
		NodeGroup ret = null;
		NodeGroup ng = cache.get(key);
		if (ng != null) {
			ret = NodeGroup.deepCopy(ng);
		} 
		return ret;
	}
	
	private static void addCache(NodeGroup ng, ArrayList<ClassInstance> classInstanceList) throws Exception {
		String key = getCacheKey(classInstanceList);
		if (cache.containsKey(key)) {
			LocalLogger.logToStdErr("PathExplorer attempted to cache a new nodegroup for key: " + key);
		} else {
			cache.put(key, ng);
		}
	}
	
	/**
	 * Key is a hash of sorted classes in classInstanceList
	 * @param classInstanceList
	 * @return
	 * @throws Exception
	 */
	private static String getCacheKey(ArrayList<ClassInstance> classInstanceList) throws Exception {
		ArrayList<String> classList = ClassInstance.getClassList(classInstanceList);
		Collections.sort(classList);
	    
	    return Utility.hashMD5(classList.toString());
	}

}
