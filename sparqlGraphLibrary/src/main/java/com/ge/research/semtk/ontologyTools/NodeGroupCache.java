package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.services.nodegroupStore.NgStore;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

/**
 * Manage a triplestore cache located at sei
 * that contains nodegroups related to oInfo.
 * 
 * Caller manages unique set of keys
 * 
 * @author 200001934
 *
 */
public class NodeGroupCache {
	private HashMap <String, JSONObject> cache = new HashMap <String, JSONObject>();
	private NgStore store = null;
	private OntologyInfo oInfo;
	
	public NodeGroupCache(SparqlEndpointInterface sei, OntologyInfo oInfo) throws Exception {
		super();
		this.oInfo = oInfo;
		this.store = new NgStore(sei);
		this.readFromTriplestore();
		
		if (! sei.isAuth()) {
			throw new Exception("NodeGroupCache requires an auth connection in order to insert new nodegroups");
		}
	}
	
	/**
	 * Initialize the cache from sei
	 * @throws Exception
	 */
	private void readFromTriplestore() throws Exception {

		Table idTable = store.getNodeGroupIdList();
		
		for (int i=0; i < idTable.getNumRows(); i++) {
			String id = idTable.getCell(i, 0);
			SparqlGraphJson sgjson = store.getNodegroup(id);
			if (sgjson == null) {
				throw new Exception ("Internal error: nodegroup was not found : " + id);
			}
			this.cache.put(id, sgjson.getJson());
			
		}
	}
	
	/**
	 * 
	 * @param key
	 * @return ng or null
	 * @throws Exception - copy exception
	 */
	public NodeGroup get(String key) throws ValidationException, Exception {
		JSONObject jObj = this.cache.get(key);
		if (jObj != null) {			
			return (new SparqlGraphJson(jObj).getNodeGroup(this.oInfo));
		} else {
			return null;
		}
	}
	
	/**
	 * Puts - all the way through to triplestore, deleting any current ng at this key
	 * @param key
	 * @param ng
	 */
	public void put(String key, NodeGroup ng, SparqlConnection conn, String comments) throws Exception {
		
		// store in ngStore
		store.deleteNodeGroup(key);
		
		SparqlGraphJson sgJson = new SparqlGraphJson(ng, conn);
		
		store.insertNodeGroup(sgJson.toJson(), conn.toJson(), key, comments, "NodeGroupCache");
		
		// store locally
		this.cache.put(key, sgJson.getJson());
	}
	
	public void delete(String key) throws Exception {
		store.deleteNodeGroup(key);
	}
}
