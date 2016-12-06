/**
 ** Copyright 2016 General Electric Company
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


package com.ge.research.semtk.belmont;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.belmont.BelmontUtil;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.NodeItem;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.ontologyTools.OntologyName;

//the nodes which represent any given entity the user/caller intends to manipulate. 

public class Node extends Returnable {
	// keeps track of our properties and collection 	
	private ArrayList<PropertyItem> props = new ArrayList<PropertyItem>();
	private ArrayList<NodeItem> nodes = new ArrayList<NodeItem>();
	
	// basic information required to be a node
	private String nodeName = null;
	private String fullURIname = null;
	private String instanceValue = null;
	private NodeGroup nodeGroup = null;
	
	// a collection of our known subclasses. 
	private ArrayList<String> subclassNames = new ArrayList<String>();
	
	
	public Node(String name, ArrayList<PropertyItem> p, ArrayList<NodeItem> n, String URI, ArrayList<String> subClassNames, NodeGroup ng){
		// just create the basic node.
		this.nodeName = name;
		this.fullURIname = URI;
		this.subclassNames = new ArrayList<String>(subClassNames);
		if(n != null){ this.nodes = n;}
		if(p != null){ this.props = p;}
		this.nodeGroup = ng;
		
		// add code to get the sparqlID
		this.sparqlID = BelmontUtil.generateSparqlID(name, this.nodeGroup.getSparqlNameHash());
	}
	
	// PEC NOTE:  this is missing a param from belmont.js : subClassNames
	//            We might have a serious structural problem.
	public Node(String name, ArrayList<PropertyItem> p, ArrayList<NodeItem> n, String URI, NodeGroup ng){
		// just create the basic node.
		this.nodeName = name;
		this.fullURIname = URI;
		if(n != null){ this.nodes = n;}
		if(p != null){ this.props = p;}
		this.nodeGroup = ng;
		
		// add code to get the sparqlID
		this.sparqlID = BelmontUtil.generateSparqlID(name, this.nodeGroup.getSparqlNameHash());
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		// return a JSON object of things needed to serialize
		JSONObject ret = new JSONObject();
		JSONArray jPropList = new JSONArray();
		JSONArray jNodeList = new JSONArray();
		JSONArray scNames = new JSONArray();
		
		for (int i = 0; i < this.subclassNames.size(); i++) {
			scNames.add(this.subclassNames.get(i));
		}
		for (int i = 0; i < this.props.size(); i++) {
			jPropList.add(this.props.get(i).toJson());
		}
		for (int i = 0; i < this.nodes.size(); i++) {
			jNodeList.add(this.nodes.get(i).toJson());
		}
				
		ret.put("propList", jPropList);
		ret.put("nodeList", jNodeList);
		ret.put("NodeName", this.nodeName);
		ret.put("fullURIName", this.fullURIname);;
		ret.put("SparqlID", this.sparqlID);
		ret.put("isReturned", this.isReturned);
		ret.put("valueConstraint", this.getValueConstraintStr());
		ret.put("instanceValue", this.getInstanceValue());
		ret.put("isRuntimeConstrained", this.getIsRuntimeConstrained());
		
		return ret;
	}
	
	public void setSparqlID(String ID){
		if (this.constraints != null) {
			this.constraints.changeSparqlID(this.sparqlID, ID);
		}
		this.sparqlID = ID;
	}
	 
	public Node(String jsonStr, NodeGroup ng) throws Exception{
		// create the JSON Object we need and then call the other constructor. 
		this((JSONObject)(new JSONParser()).parse(jsonStr), ng);
	}
	
	public Node(JSONObject nodeEncoded, NodeGroup ng) throws Exception{
		// create a new node from JSON, assuming everything is sane. 
		this.nodeGroup = ng;
		
		// build all the parts we need from this incoming JSON Object...
		this.nodeName = nodeEncoded.get("NodeName").toString();
		this.fullURIname = nodeEncoded.get("fullURIName").toString();
		this.sparqlID = nodeEncoded.get("SparqlID").toString();
		
		// get the array of subclass names.
		JSONArray subclasses = (JSONArray)nodeEncoded.get("subClassNames");
		if (subclasses != null) {
			Iterator<String> it = subclasses.iterator();
			while(it.hasNext()){
				this.subclassNames.add(it.next());
			}
		}
		
		this.isReturned = (Boolean)nodeEncoded.get("isReturned");
		
		try{
			this.instanceValue = nodeEncoded.get("instanceValue").toString();
		}
		catch(Exception E){ // the value was missing
			this.instanceValue = null;
		}
		try{
			this.constraints = new ValueConstraint(nodeEncoded.get("valueConstraint").toString());
		}
		catch(Exception E){ // the value was not set
			this.constraints = null;
		}
		try{
			this.setIsRuntimeConstrained((Boolean)nodeEncoded.get("isRuntimeConstrained"));
		}
		catch(Exception E){
			this.setIsRuntimeConstrained(false);
		}
		
		// create the node items and property items.
		// nodeItems 
		JSONArray nodesToProcess = (JSONArray)nodeEncoded.get("nodeList");
		Iterator<JSONObject> nIt = nodesToProcess.iterator();
		while(nIt.hasNext()){
			this.nodes.add(new NodeItem(nIt.next(), this.nodeGroup));
		}
		
		// propertyItems
		JSONArray propertiesToProcess = (JSONArray)nodeEncoded.get("propList");
		Iterator<JSONObject> pIt = propertiesToProcess.iterator();
		while(pIt.hasNext()){
			this.props.add(new PropertyItem(pIt.next()));
		}
		
		
	}

	public NodeItem setConnection(Node curr, String connectionURI) throws Exception {
		// create a display name. 
		String connectionLocal = new OntologyName(connectionURI).getLocalName();

		// actually set the connection. 
		for(int i = 0; i < this.nodes.size(); i += 1){
			NodeItem nd = this.nodes.get(i);
			// did it match?
			if(nd.getKeyName().equals(connectionLocal)){
				nd.setConnected(true);
				nd.setConnectBy(connectionLocal);
				nd.setUriConnectBy(connectionURI);
				nd.setNodes(curr);
				
				return nd;
			}
		}
		throw new Exception("Internal error in SemanticNode.setConnection().  Couldn't find node item connection: " + this.getSparqlID() + "->" + connectionURI);
	}

	public ArrayList<PropertyItem> getReturnedPropertyItems() {
		ArrayList<PropertyItem> retval = new ArrayList<PropertyItem>();
		// spin through the list of values and add teh correct ones. 
		for(int i = 0; i < this.props.size(); i += 1){
			PropertyItem pi = this.props.get(i);
			if(pi.getIsReturned()){		// we are returning this one. add it to the list. 
				retval.add(pi);
			}
		}
		// return the list. 
		return retval;
	}	
	
	
	public boolean checkConnectedTo(Node nodeToCheck) {
		for (NodeItem n : nodes) {
			if (n.getConnected()) {
				ArrayList<Node> nodeList = n.getNodeList();
				for (Node o : nodeList) {
					if (o.getSparqlID().equals(nodeToCheck.getSparqlID())) {
						return true;
					}
				}
			}
		}	
		
		return false;
	}
	
	public ArrayList<Node> getConnectedNodes() {
		
		ArrayList<Node> connectedNodes = new ArrayList<Node>();
		
		for (NodeItem ni : this.nodes) {
			if (ni.getConnected()) {
				connectedNodes.addAll(ni.getNodeList());
			}
		}
		
		return connectedNodes;
	}

	/**
	 * 
	 * @return value constraint string, which might be "" but not null
	 */
	public String getValueConstraintStr() {
		return this.constraints != null ? this.constraints.toString() : "";
	}
	
	public void setValueConstraint(ValueConstraint v) {
		this.constraints = v;
	}
	
	
	public ArrayList<String> getSubClassNames() {
		return this.subclassNames;
	}
	
	public String getUri(boolean localFlag) {
		if (localFlag) {
			return new OntologyName(this.getFullUriName()).getLocalName();
		} else {
			return this.getFullUriName();
		}
	}
	public String getUri() {
		return this.getFullUriName();
	}
	
	public String getFullUriName() {
		return fullURIname;
	}

	public boolean isUsed(boolean instanceOnly) {
		if (instanceOnly) {
			return this.hasInstanceData();
		} else {
			return this.isUsed();
		}
	}
	
	public boolean isUsed() {
		if (this.isReturned || this.constraints != null || this.instanceValue != null) {
			return true;
		}
		for (PropertyItem item : this.props) {
			if (item.isReturned || item.getConstraints() != null || item.getInstanceValues().size() > 0) {
				return true;
			}
		}
		return false;	
	}
	
	public boolean hasInstanceData() {
		if (this.instanceValue != null) {
			return true;
		}
		for (PropertyItem item : this.props) {
			if (item.getInstanceValues().size() > 0) {
				return true;
			}
		}
		return false;	
	}
	
	public ArrayList<String> getSparqlIDList() {
		// list all sparqlID's in use by this node
		ArrayList<String> ret = new ArrayList<String>();
		ret.add(this.getSparqlID());
		
		for (PropertyItem p : this.props) {
			String s = p.getSparqlID();
			if (s != null && ! s.isEmpty()) {
				ret.add(s);
			}
		}
		return ret;
	}
	
	public void removeFromNodeList(Node node) {
		for (NodeItem item : this.nodes) {
			item.removeNode(node);
		}
	}
	
	public ArrayList<NodeItem> getConnectingNodeItems(Node otherNode) {
		ArrayList<NodeItem> items = new ArrayList<NodeItem>();
		
		for (NodeItem item : nodes) {
			if (item.getConnected()) {
				ArrayList<Node> nodeList = item.getNodeList();
				
				for (Node node : nodeList) {
					if (node.getSparqlID().equals(otherNode.getSparqlID())) {
						items.add(item);
					}
				}
			}
		}
		
		return items;
	}

	public ArrayList<PropertyItem> getPropertyItems() {
		return this.props;
	}
	
	public PropertyItem getPropertyByKeyname(String keyname) {
		for (int i = 0; i < this.props.size(); i++) {
			if (this.props.get(i).getKeyName().equals(keyname)) {
				return this.props.get(i);
			}
		}
		return null;
	}
	public String getInstanceValue() {
		return this.instanceValue;
	}
	
	public void setInstanceValue(String value) {
		this.instanceValue = value;
	}

	public void setIsReturned(boolean b){
		this.isReturned = b;
	}
	
	public void setProperties(ArrayList<PropertyItem> p){
		if(p != null){
			this.props = p;
		}
	}
	
	public void setNodeItems(ArrayList<NodeItem> n){
		if(n!= null){
			this.nodes = n;
		}
	}
	
	public ArrayList<NodeItem> getNodeItemList() {
		return this.nodes;
	}
	
	public int countReturns () {
		int ret = this.getIsReturned() ? 1 : 0;
		
		for (PropertyItem p : this.props) {
			ret += (p.getIsReturned() ? 1 : 0);
		}
		
		return ret;
	}
	
	public int getReturnedCount () {
		int ret = 0;
		if (this.getIsReturned()) {
			ret += 1;
		}
		for (PropertyItem p : this.getPropertyItems()) {
			if (p.getIsReturned()) {
				ret += 1;
			}
		}
		return ret;
	}
	
	public ArrayList<PropertyItem> getConstrainedPropertyObjects() {
		ArrayList<PropertyItem> constrainedProperties = new ArrayList<PropertyItem>();
		
		for (PropertyItem pi : props) {
			if (pi.getConstraints() != null && !pi.getConstraints().isEmpty()) {
				constrainedProperties.add(pi);
			}
		}
		
		return constrainedProperties;
	}
	public String getValueType(){
		return "node_uri";
	}
}
