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
import com.ge.research.semtk.utility.LocalLogger;
import com.google.protobuf.TextFormat.ParseException;

public class NodeItem {
	// this is the class that controls the access to nodes that a given belmont node 
	// believes it is linked to...
	public static int OPTIONAL_FALSE = 0;
	public static int OPTIONAL_TRUE = 1;
	public static int OPTIONAL_REVERSE = -1;
	
	private ArrayList<Node> nodes = new ArrayList<Node>();
	private ArrayList<Integer> snodeOptionals = new ArrayList<Integer>();
	private ArrayList<Boolean> deletionFlags = new ArrayList<Boolean>();
	private String keyName = "";
	private String valueType = "";
	private String valueTypeURI = "";
	private String connectedBy = "";
	private String uriConnectBy = "";
	private Boolean connected = false;
	
	/**
	 * Constructor 
	 * @param nome the property name (e.g. screenPrinting)
	 * @param valueType (e.g. ScreenPrinting)
	 * @param UriValueType (e.g. http://research.ge.com/print/testconfig#ScreenPrinting)
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
				ng.addOrphanedNode(curr);
			}
			this.nodes.add(curr);
		}
		
		if (next.containsKey("SnodeOptionals")) {
			JSONArray jsonOpt = (JSONArray)next.get("SnodeOptionals");
			for (int i=0; i < jsonOpt.size(); i++) {
				this.snodeOptionals.add(Integer.parseInt(jsonOpt.get(i).toString()));
			}
		} else {
			long opt = NodeItem.OPTIONAL_FALSE;

			if (next.containsKey("isOptional")) {
				Object o = next.get("isOptional");
				if (o instanceof Boolean) {
					opt = ((Boolean) o) ? NodeItem.OPTIONAL_TRUE : NodeItem.OPTIONAL_FALSE;
				}
				else {
					opt = (long) o;
				}
			}
		
			for (int i=0; i < this.nodes.size(); i++) {
				this.snodeOptionals.add((int) opt);
			}
		}
		// get all of the deletion flag values, if any.
		if (next.containsKey("DeletionMarkers")){
			/*
			 * Note: the code below seems to be dependent on the type in the JSONArray being 
			 * boolean. the commented out line was originally in use, matching the SnodeOptionals
			 * retrieval but failed on an exception about not being able to cast an arraylist to 
			 * a jsonArray. this seems odd. following the exception itself, i changed the code to 
			 * directly assume the result of the retrieval was an arrayList of booleans. this works
			 * but i am uncertain as to why as the same approach has not worked in the past.
			 */
			//JSONArray jsonDelMark = (JSONArray)next.get("DeletionMarkers");
			ArrayList<Boolean> jsonDelMark = (ArrayList<Boolean>) next.get("DeletionMarkers");
			for(int i = 0; i < jsonDelMark.size(); i++){
				
				String rawVal = jsonDelMark.get(i).toString();
				try{
					this.deletionFlags.add(Boolean.parseBoolean(rawVal));
				}
				catch( Exception pe){
					LocalLogger.logToStdErr("value for deletion flag was null. this is being replaced with false.");
					this.deletionFlags.add(false);
				}

			}
		}
		// if there were no deletion flag markers, set all of the potential ones to false.
		else{
			for (int i=0; i < this.nodes.size(); i++) {
				this.deletionFlags.add(false);
			}
		}
		
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		// return a JSON object of things needed to serialize
		JSONObject ret = new JSONObject();
		
		JSONArray SNodeSparqlIDs = new JSONArray();
		JSONArray SNodeOptionals = new JSONArray();
		
		for (int i=0; i < this.nodes.size(); i++) {
			SNodeSparqlIDs.add(this.nodes.get(i).getSparqlID());
			SNodeOptionals.add(this.snodeOptionals.get(i));
		}
		ret.put("SnodeSparqlIDs", SNodeSparqlIDs);
		ret.put("SnodeOptionals", SNodeOptionals);
		ret.put("DeletionMarkers", this.deletionFlags);
		ret.put("KeyName", this.keyName);
		ret.put("ValueType", this.valueType);
		ret.put("UriValueType", this.valueTypeURI);
		ret.put("ConnectBy", this.connectedBy);
		ret.put("Connected", this.connected);
		ret.put("UriConnectBy", this.uriConnectBy);
		
		return ret;
	}
	
	public void reset() {
		for (int i=0; i < this.deletionFlags.size(); i++) {
			this.deletionFlags.set(i, false);
		}
		for (int i=0; i < this.snodeOptionals.size(); i++) {
			this.snodeOptionals.set(i, OPTIONAL_FALSE);
		}
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

	public void pushNode(Node curr) {
		this.nodes.add(curr);
		this.snodeOptionals.add(NodeItem.OPTIONAL_FALSE);
		this.deletionFlags.add(false);
	}
	
	public void pushNode(Node curr, int opt) {
		this.nodes.add(curr);
		this.snodeOptionals.add(opt);
		this.deletionFlags.add(false);
	}
	
	public void pushNode(Node curr, Boolean deletionMarker){
		this.nodes.add(curr);
		this.snodeOptionals.add(NodeItem.OPTIONAL_FALSE);
		this.deletionFlags.add(deletionMarker);
	}
	
	public void pushNode(Node curr, int opt, Boolean deletionMarker) {
		this.nodes.add(curr);
		this.snodeOptionals.add(opt);
		this.deletionFlags.add(deletionMarker);
	}
		
	public ArrayList<Node> getNodeList() {
		return this.nodes;
	}

	public String getUriValueType() {
		return this.valueTypeURI;
	}
	public String getValueType() {
		return this.valueType;
	}
	
	public String getUriConnectBy() {
		return this.uriConnectBy;
	}
	
	public void removeNode(Node node) {
		int pos = this.nodes.indexOf(node);
		if(pos > -1){
			this.nodes.remove(pos);
			this.snodeOptionals.remove(pos);
			this.deletionFlags.remove(pos);
		}
		if (this.nodes.size() == 0) {
			this.connected = false;
		}
	}
	
	public void setSnodeDeletionMarker(Node snode, Boolean toDelete) throws Exception{
		for (int i=0; i < this.nodes.size(); i++) {
			if (this.nodes.get(i) == snode) {
				this.deletionFlags.set(i, toDelete);
				return;
			}
		}
		throw new Exception("NodeItem can't find link to semantic node");
	}
	
	public Boolean getSnodeDeletionMarker(Node snode) throws Exception{
		for (int i=0; i < this.nodes.size(); i++) {
			if (this.nodes.get(i) == snode) {
				return this.deletionFlags.get(i);
			}
		}
		throw new Exception("NodeItem can't find link to semantic node");
	}
	
	public ArrayList<Node> getSnodesWithDeletionFlagsEnabledOnThisNodeItem(){
		ArrayList<Node> retval = new ArrayList<Node>();
		
		for(int i=0; i < this.nodes.size(); i++) {
			// if the node is supposed to be deleted, add it to the deletion list.
			if(this.deletionFlags.get(i)){
				// add it.
				retval.add(this.nodes.get(i));
			}
		}
		// ship it back
		return retval;
	}
	
	public void setSNodeOptional(Node snode, int optional) throws Exception {
		for (int i=0; i < this.nodes.size(); i++) {
			if (this.nodes.get(i) == snode) {
				this.snodeOptionals.set(i, optional);
				return;
			}
		}
		throw new Exception("NodeItem can't find link to semantic node");
	}
	
	public int getSNodeOptional(Node snode) throws Exception {
		for (int i=0; i < this.nodes.size(); i++) {
			if (this.nodes.get(i) == snode) {
				return this.snodeOptionals.get(i);
			}
		}
		throw new Exception("NodeItem can't find link to semantic node");
	}
	
	
}
