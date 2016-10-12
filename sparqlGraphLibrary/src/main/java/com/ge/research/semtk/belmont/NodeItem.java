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

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;

public class NodeItem {
	// this is the class that controls the access to nodes that a given belmont node 
	// believes it is linked to...
	public static int OPTIONAL_FALSE = 0;
	public static int OPTIONAL_TRUE = 1;
	public static int OPTIONAL_REVERSE = -1;
	
	private ArrayList<Node> nodes = new ArrayList<Node>();
	private String keyName = "";
	private String valueType = "";
	private String valueTypeURI = "";
	private String connectedBy = "";
	private String uriConnectBy = "";
	private int isOptional = NodeItem.OPTIONAL_FALSE;
	private Boolean connected = false;
	
	/**
	 * Constructor 
	 * @param nome the property name (e.g. screenPrinting)
	 * @param valueType (e.g. ScreenPrinting)
	 * @param UriValueType (e.g. http://research.ge.com/sofc/testconfig#ScreenPrinting)
	 */
	public NodeItem(String nome, String valueType, String UriValueType) {
		this.keyName = nome;
		this.valueType = valueType;
		this.valueTypeURI = UriValueType;
	}
	
	public NodeItem(JSONObject next, NodeGroup ng) throws Exception{
		// get basic values:
		this.keyName = next.get("KeyName").toString();
		this.valueTypeURI = next.get("UriValueType").toString();
		this.valueType = next.get("ValueType").toString();
		this.connectedBy = next.get("ConnectBy").toString();
		this.uriConnectBy = next.get("UriConnectBy").toString();
		this.setIsOptionalBackwardsCompatible(next.get("isOptional").toString());
		this.connected = (Boolean)next.get("Connected");
		
		// add the list of nodes this item attaches to on the far end. 
		JSONArray nodeSparqlIDs = (JSONArray)next.get("SnodeSparqlIDs");
		Iterator<String> nIt = nodeSparqlIDs.iterator();
		while(nIt.hasNext()){
			String currId = nIt.next();
			Node curr = ng.getNodeBySparqlID(currId);
			if(curr == null){ 
				// panic
				//throw new Exception("could not find the node referenced by sparql ID : " + currId);

				// do not panic. add it. 
				curr = new Node(currId, null, null, currId, ng);
				curr.setSparqlID(currId);
				ng.orphanOnCreate.add(curr);
			}
			this.nodes.add(curr);
		}
		
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		// return a JSON object of things needed to serialize
		JSONObject ret = new JSONObject();
		
		JSONArray SNodeSparqlIDs = new JSONArray();
		for (int i=0; i < this.nodes.size(); i++) {
			SNodeSparqlIDs.add(this.nodes.get(i).getSparqlID());
		}
		ret.put("SnodeSparqlIDs", SNodeSparqlIDs);
		ret.put("KeyName", this.keyName);
		ret.put("ValueType", this.valueType);
		ret.put("UriValueType", this.valueTypeURI);
		ret.put("ConnectBy", this.connectedBy);
		ret.put("Connected", this.connected);
		ret.put("UriConnectBy", this.uriConnectBy);
		ret.put("isOptional", this.isOptional);
		
		return ret;
	}
	public String getKeyName() {
		return this.keyName;
	}
	
	public boolean getConnected() {
		return this.connected;
	}

	public void setConnected(boolean b) {
		this.connected = b;
	}

	public void setConnectBy(String connectionLocal) {
		this.connectedBy = connectionLocal;
	}

	public void setUriConnectBy(String connectionURI) {
		this.uriConnectBy = connectionURI;		
	}

	public void setNodes(Node curr) {
		this.nodes.add(curr);
	}
	
	public ArrayList<Node> getNodeList() {
		return this.nodes;
	}

	public String getUriValueType() {
		// TODO Auto-generated method stub
		return valueTypeURI;
	}

	public String getUriConnectBy() {
		return this.uriConnectBy;
	}

	public int getIsOptional() {
		return this.isOptional;
	}
	
	public void removeNode(Node node) {
		this.nodes.remove(node);
		if (this.nodes.size() == 0) {
			this.connected = false;
		}
	}

	public void setIsOptional(int val) {
		this.isOptional = val;
	}
	
	public void setIsOptionalBackwardsCompatible(String str) {
		// handle old or new isOptional
		try {
			// new: integer
			this.setIsOptional(Integer.parseInt(str));
		} catch (Exception e) {
			// old: boolean
			this.setIsOptional(Boolean.parseBoolean(str) ? NodeItem.OPTIONAL_TRUE : NodeItem.OPTIONAL_FALSE);
		}
	}
}
