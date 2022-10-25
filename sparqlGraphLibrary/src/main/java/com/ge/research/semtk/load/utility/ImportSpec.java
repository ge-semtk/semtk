package com.ge.research.semtk.load.utility;
/**
 ** Copyright 2020-2021 General Electric Company
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.load.transform.TransformInfo;

/** 
 * Handles the JSON for an importSpec.
 * All actual logic and instantiating objects is in ImportSpecHandler
 * 
 * @author 200001934
 *
 */
@SuppressWarnings("unchecked") // suppress all the json .put type warnings
public class ImportSpec {
	public static final String LOOKUP_MODE_ERR_IF_MISSING = 	"noCreate";    
	public static final String LOOKUP_MODE_CREATE_IF_MISSING =	"createIfMissing";
	public static final String LOOKUP_MODE_ERR_IF_EXISTS = 		"errorIfExists";  
	
	public static final String JKEY_IS_VERSION = "version";
	public static final String JKEY_IS_BASE_URI = "baseURI";
	public static final String JKEY_IS_COLUMNS = "columns";
	public static final String JKEY_IS_COL_COL_ID = "colId";
	public static final String JKEY_IS_COL_COL_NAME = "colName";
	public static final String JKEY_IS_DATA_VALIDATOR = "dataValidator";
	public static final String JKEY_IS_TEXTS = "texts";
	public static final String JKEY_IS_TEXT_ID = "textId";
	public static final String JKEY_IS_TEXT_TEXT = "text";
	public static final String JKEY_IS_TRANSFORMS = "transforms";
	public static final String JKEY_IS_TRANS_ID = "transId";
	public static final String JKEY_IS_TRANS_NAME = "name";
	public static final String JKEY_IS_TRANS_TYPE = "transType";
	public static final String JKEY_IS_TRANS_ARG1 = "arg1";
	public static final String JKEY_IS_TRANS_ARG2 = "arg2";
	public static final String JKEY_IS_TYPE_RESTRICTION = "type_restriction";
	public static final String JKEY_IS_NODES = "nodes";
	public static final String JKEY_IS_NODE_SPARQL_ID = "sparqlID";
	public static final String JKEY_IS_NODE_TYPE = "type";
	public static final String JKEY_IS_NODE_LOOKUP_MODE = "URILookupMode";
	public static final String JKEY_IS_URI_LOOKUP = "URILookup";
	public static final String JKEY_IS_MAPPING = "mapping";
	public static final String JKEY_IS_MAPPING_TEXT_ID = "textId";
	public static final String JKEY_IS_MAPPING_TEXT = "text";
	public static final String JKEY_IS_MAPPING_COL_ID = "colId";
	public static final String JKEY_IS_MAPPING_COL_NAME = "colName";
	public static final String JKEY_IS_MAPPING_TRANSFORM_LIST = "transformList";
	public static final String JKEY_IS_PROPS = "props";
	public static final String JKEY_IS_MAPPING_PROPS_URI_REL = "URIRelation";
	
    public static final int TYPE_RESTRICTION_PROP = -1;

	JSONObject json = new JSONObject();

	public ImportSpec() {

		json.put(JKEY_IS_VERSION, "1");
		json.put(JKEY_IS_BASE_URI, "");
		
		json.put(JKEY_IS_TEXTS, new JSONArray());
		json.put(JKEY_IS_TRANSFORMS, new JSONArray());
		
		json.put(JKEY_IS_COLUMNS, new JSONArray());
		json.put(JKEY_IS_NODES, new JSONArray());


	}
	
	public ImportSpec(JSONObject json) {
		this.json = json;
	}
	
	public JSONObject toJson() {
		return this.json;
	}
	
	//********* getters and setters ********/

	//*** columns ***/
	public int getNumColumns() {
		JSONArray cols = (JSONArray) this.json.get(JKEY_IS_COLUMNS);
		return cols == null ? 0 : cols.size();
	}
	
	private JSONObject getCol(int index) {
		return (JSONObject) ((JSONArray) this.json.get(JKEY_IS_COLUMNS)).get(index);

	}
	public String getColId(int index) {
		return (String) this.getCol(index).get(JKEY_IS_COL_COL_ID);
	}
	
	public String getColName(int index) {
		String name = (String) this.getCol(index).get(JKEY_IS_COL_COL_NAME);
		return name;
	}
	
	//** transforms **/
	public int getNumTransforms() {
		JSONArray transforms = (JSONArray) this.json.get(JKEY_IS_TRANSFORMS);
		return transforms == null ? 0 : transforms.size();
	}
	
	/**
	 * 
	 * @param name
	 * @param type
	 * @param arg1
	 * @param arg2
	 * @return the new transform's ID
	 */
	public String addTransform(String name, String type, String arg1, String arg2) throws Exception {
		// build id
		String id = "trans_" + String.valueOf(this.getNumTransforms());
		
		// error check
		if (this.getTransform(name) != null) 
			throw new Exception("Transform already exists with name: " + name);
		if (!TransformInfo.getTypes().contains(type)) 
			throw new Exception("Invalid transform type: " + type);
		if (arg2 != null && TransformInfo.getArgCount(type) < 2)
			throw new Exception("Transform " + name + " does not accept arg2");
		if (arg1 != null && TransformInfo.getArgCount(type) < 1)
			throw new Exception("Transform " + name + " does not accept args");
		
		// build transform json
		JSONObject transform = new JSONObject();
		transform.put(JKEY_IS_TRANS_ID, id);
		transform.put(JKEY_IS_TRANS_NAME, name);
		transform.put(JKEY_IS_TRANS_TYPE, type);
		transform.put(JKEY_IS_TRANS_ARG1, arg1);
		transform.put(JKEY_IS_TRANS_ARG2, arg2);
		
		// add
		JSONArray transforms = (JSONArray) this.json.get(JKEY_IS_TRANSFORMS);
		transforms.add(transform);
		
		return id;
	}
	
	private JSONObject getTransform(String name) {
		for (Object o : (JSONArray) this.json.get(JKEY_IS_TRANSFORMS)) {
			JSONObject j = (JSONObject) o;
			if (j.get(JKEY_IS_TRANS_NAME).equals(name)) {
				return j;
			}
		}
		return null;
	}
	private JSONObject getTransform(int index) {
		return (JSONObject) ((JSONArray) this.json.get(JKEY_IS_TRANSFORMS)).get(index);

	}
	public String getTransformArg1(int index) {
		return (String) this.getTransform(index).get(JKEY_IS_TRANS_ARG1);
	}
	public String getTransformArg2(int index) {
		return (String) this.getTransform(index).get(JKEY_IS_TRANS_ARG2);
	}
	public String getTransformId(int index) {
		return (String) this.getTransform(index).get(JKEY_IS_TRANS_ID);
	}
	public String getTransformName(int index) {
		return (String) this.getTransform(index).get(JKEY_IS_TRANS_NAME);
	}
	public String getTransformType(int index) {
		return (String) this.getTransform(index).get(JKEY_IS_TRANS_TYPE);
	}
	
	//** texts **/
	public int getNumTexts() {
		JSONArray arr = (JSONArray) this.json.get(JKEY_IS_TEXTS);
		return arr == null ? 0 : arr.size();
	}
	
	private JSONObject getText(int index) {
		return (JSONObject) ((JSONArray) this.json.get(JKEY_IS_TEXTS)).get(index);

	}
	public String getTextId(int index) {
		return (String) this.getText(index).get(JKEY_IS_TEXT_ID);
	}
	
	public String getTextText(int index) {
		return (String) this.getText(index).get(JKEY_IS_TEXT_TEXT);
	}
	
	//** nodes **/
	public int getNumNodes() {
		JSONArray arr = (JSONArray) this.json.get(JKEY_IS_NODES);
		return arr == null ? 0 : arr.size();
	}
	private JSONObject getNode(int index) {
		return (JSONObject) ((JSONArray) this.json.get(JKEY_IS_NODES)).get(index);
	}
	private JSONObject getNodeMapping(int nodeIndex, int mappingIndex) {
		return (JSONObject) ((JSONArray)this.getNode(nodeIndex).get(JKEY_IS_MAPPING)).get(mappingIndex);
	}
	public int getNodeIndex(String sparqlID) {
		for (int i=0; i < this.getNumNodes(); i++) {
			if (this.getNodeSparqlID(i).equals(sparqlID)) {
				return i;
			}
		}
		return -1;
	}
	
	public int getNodePropIndex(int n, String propURI) {
		JSONArray props = (JSONArray) this.getNode(n).get(JKEY_IS_PROPS);
		for (int i=0; i < props.size(); i++) {
			JSONObject prop = (JSONObject) props.get(i);
			if (((String) prop.get(JKEY_IS_MAPPING_PROPS_URI_REL)).equals(propURI)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Due to backwards compatibility, could be null
	 * @param index
	 * @return
	 */
	public String getNodeLookupMode(int index) {
		return (String) this.getNode(index).get(JKEY_IS_NODE_LOOKUP_MODE);
	}
	public String getNodeSparqlID(int index) {
		return (String) this.getNode(index).get(JKEY_IS_NODE_SPARQL_ID);
	}
	public String getNodeType(int index) {
		return (String) this.getNode(index).get(JKEY_IS_NODE_TYPE);
	}
	public int getNodeNumMappings(int index) {
		JSONArray mappingJsonArr = (JSONArray) this.getNode(index).get(JKEY_IS_MAPPING);
		return mappingJsonArr == null ? 0 : mappingJsonArr.size();
	}
	/**
	 * Can be null
	 * @param nodeIndex
	 * @param mappingIndex
	 * @return
	 */
	public String getNodeMappingTextId(int nodeIndex, int mappingIndex) {
		return (String) this.getNodeMapping(nodeIndex, mappingIndex).get(JKEY_IS_MAPPING_TEXT_ID);
	}
	/**
	 * Can be null
	 * @param nodeIndex
	 * @param mappingIndex
	 * @return
	 */
	public String getNodeMappingColId(int nodeIndex, int mappingIndex) {
		return (String) this.getNodeMapping(nodeIndex, mappingIndex).get(JKEY_IS_MAPPING_COL_ID);
	}
	public ArrayList<String> getNodeMappingTransformList(int nodeIndex, int mappingIndex) {
		JSONArray arr = (JSONArray) this.getNodeMapping(nodeIndex, mappingIndex).get(JKEY_IS_MAPPING_TRANSFORM_LIST);
		return toStringList(arr);
	}
	
	public boolean getNodeHasTypeRestriction(int nodeIndex) {
		return this.getNodeProp(nodeIndex, TYPE_RESTRICTION_PROP) != null;
	}
	
	/**
	 * Count number of normal properties (does not include special type_restriction)
	 * @param index
	 * @return
	 */
	public int getNodeNumProperties(int index) {
		JSONArray arr = (JSONArray) this.getNode(index).get(JKEY_IS_PROPS);
		return arr == null ? 0 : arr.size();
	}
	
	/**
	 * Get property: where there are special property constants beyond the normal [0,n]
	 * @param n - node index
	 * @param p - prop index or special prop constant (TYPE_RESTRICTION_PROP)
	 * @return
	 */
	private JSONObject getNodeProp(int n, int p) {
		if (p == TYPE_RESTRICTION_PROP) {
			return (JSONObject) this.getNode(n).get(JKEY_IS_TYPE_RESTRICTION);
		} else {
			JSONArray arr = (JSONArray) this.getNode(n).get(JKEY_IS_PROPS);
			return (JSONObject) arr.get(p);
		}
	}
	
	public String getNodePropUriRel(int n, int p) {
		if (p == TYPE_RESTRICTION_PROP) {
			return ImportMapping.TYPE_URI;
		} else {
			return (String) this.getNodeProp(n, p).get(JKEY_IS_MAPPING_PROPS_URI_REL);
		}
	}

	
	public int getNodePropNumMappings(int n, int p) {
		JSONObject prop = this.getNodeProp(n, p);
		JSONArray arr = (JSONArray) prop.get(JKEY_IS_MAPPING);
		return (arr == null) ? 0 : arr.size();
	}
	
	private JSONObject getNodePropertyMapping(int n, int p, int m) {
		return (JSONObject) ((JSONArray)this.getNodeProp(n,p).get(JKEY_IS_MAPPING)).get(m);
	}
	public String getNodePropMappingTextId(int n, int p, int m) {
		return (String) this.getNodePropertyMapping(n, p, m).get(JKEY_IS_MAPPING_TEXT_ID);
	}
	public String getNodePropMappingColId(int n, int p, int m) {
		return (String) this.getNodePropertyMapping(n, p, m).get(JKEY_IS_MAPPING_COL_ID);
	}
	public ArrayList<String> getNodePropMappingTransformList(int n, int p, int m) {
		JSONArray arr = (JSONArray) this.getNodePropertyMapping(n, p, m).get(JKEY_IS_MAPPING_TRANSFORM_LIST);
		return toStringList(arr);
	}
	public int getNodePropNumURILookups(int n, int p) {
		JSONArray arr = (JSONArray) this.getNodeProp(n,p).get(JKEY_IS_URI_LOOKUP);
		return arr == null ? 0 : arr.size();
	}
	
	public ArrayList<String> getNodePropURILookupList(int n, int p) {
		JSONArray arr = (JSONArray) this.getNodeProp(n,p).get(JKEY_IS_URI_LOOKUP);
		return toStringList(arr);
	}
	public String getBaseURI() {
		return (String) this.json.get(JKEY_IS_BASE_URI);
	}
	
	
	public int getNodeNumURILookups(int index) {
		JSONArray arr = (JSONArray) this.getNode(index).get(JKEY_IS_URI_LOOKUP);
		return arr == null ? 0 : arr.size();
	}
	
	public ArrayList<String> getNodeURILookupList(int index) {
		JSONArray arr = (JSONArray) this.getNode(index).get(JKEY_IS_URI_LOOKUP);
		return toStringList(arr);
	}
	
	private static ArrayList<String> toStringList(JSONArray arr) {
		ArrayList<String> ret = new ArrayList<String>();
		if (arr != null) {
			for (Object o : arr) {
				ret.add((String)o);
			}
		}
		return ret;
	}
	
	public JSONArray getDataValidatorJson() {
		return (JSONArray) this.json.get(JKEY_IS_DATA_VALIDATOR);
	}
	
	/********* end getters ********/
	
	public void setBaseURI(String override) {
		this.json.put(JKEY_IS_BASE_URI, override);
	}
	
	public String addColumn(String colName) {
		String name = colName.trim();
		
		JSONArray columnsJson = (JSONArray) this.json.get(JKEY_IS_COLUMNS);

		JSONObject colJson = new JSONObject();
		String id = this.generateColID();
		
		colJson.put(JKEY_IS_COL_COL_ID, id);
		colJson.put(JKEY_IS_COL_COL_NAME, name);
		columnsJson.add(colJson);
		return id;
	}
	
	private String generateColID() {
		
		// hash existing col names
		HashSet<String> colIds = new HashSet<String>();
		for (Object o : (JSONArray) this.json.get(JKEY_IS_COLUMNS)) {
			colIds.add((String)((JSONObject) o).get(JKEY_IS_COL_COL_ID));
		}
		
		// generate a new one not in the hash
		int i = this.getNumColumns();
		String ret;
		do {
			ret = "col_" + i++;
		} while (colIds.contains(ret));
		
		return ret;
	}
	
	public String findOrAddColByName(String colName) {
		try {
			return this.findColId(colName);
		} catch (Exception e) {
			return this.addColumn(colName);
		}
	}
	
	private String findColId(String colName) throws Exception {
		String name = colName.toLowerCase().trim();
		JSONArray columnsJson = (JSONArray) this.json.get(JKEY_IS_COLUMNS);
		
		for (Object o : columnsJson) {
			JSONObject colJson = (JSONObject) o;
			String compareName = ((String)colJson.get(JKEY_IS_COL_COL_NAME)).toLowerCase().trim();
			if (compareName.equals(name)) {
				return (String) colJson.get(JKEY_IS_COL_COL_ID);
			}
		}
		
		throw new Exception ("Can't find column id " + colName);
	}
	
	private JSONObject findColumn(String colId) throws Exception {
		JSONArray columnsJson = (JSONArray) this.json.get(JKEY_IS_COLUMNS);
		
		for (Object o : columnsJson) {
			JSONObject colJson = (JSONObject) o;
			if (colId.equals((String)colJson.get(JKEY_IS_COL_COL_ID))) {
				return colJson;
			}
		}
		
		throw new Exception ("Can't find column " + colId);
	}
	
	private JSONObject findNode(String sparqlId) throws Exception {
		
		JSONArray nodesJson = (JSONArray) this.json.get(JKEY_IS_NODES);
		
		for (Object o : nodesJson) {
			JSONObject nodeJson = (JSONObject) o;
			if (((String)nodeJson.get(JKEY_IS_NODE_SPARQL_ID)).equals(sparqlId)) {
				return (JSONObject) nodeJson;
			}
		}
		throw new Exception("Can't find node with sparqlId: " + sparqlId);
	}
	
	private JSONObject findProp(String nodeSparqlId, String propUri) throws Exception {
		
		JSONObject node = this.findNode(nodeSparqlId);
		
		JSONArray props = (JSONArray) node.get(JKEY_IS_PROPS);
		if (props != null) {
			for (Object o : props) {
				JSONObject prop = (JSONObject) o;
				if (((String)prop.get(JKEY_IS_MAPPING_PROPS_URI_REL)).equals(propUri)) {
					return (JSONObject) prop;
				}
			}
		}
		throw new Exception("Can't find property in importSpec: " + propUri + " in node " + nodeSparqlId);
		
	}
	
	/**
	 * Add node to the importspec.
	 * Importspec must have all the nodes in a nodegroup
	 * @param sparqlId
	 * @param typeUri
	 * @param lookupMode
	 */
	public JSONObject addNode(String sparqlId, String typeUri, String lookupMode) {
		
		JSONArray nodeArr = (JSONArray) json.get(JKEY_IS_NODES);
		JSONObject node = new JSONObject();
		node.put(JKEY_IS_NODE_SPARQL_ID, sparqlId);
		node.put(JKEY_IS_NODE_TYPE, typeUri);
		if (lookupMode != null) {
			node.put(JKEY_IS_NODE_LOOKUP_MODE, lookupMode);
		}
		node.put(JKEY_IS_MAPPING, new JSONArray());
		node.put(JKEY_IS_PROPS, new JSONArray());
		nodeArr.add(node);
		return node;
	}
	
	public void addLookupMode(String nodeSparqlId, String lookupMode) throws Exception {
		JSONObject node = this.findNode(nodeSparqlId);
		node.put(JKEY_IS_NODE_LOOKUP_MODE, lookupMode);
	}
	
	/**
	 * Add prop to a node in the importspec
	 * Props only need to exist if they are mapped
	 * @param nodeSparqlId
	 * @param relationUri
	 * @throws Exception
	 */
	public JSONObject addProp(String nodeSparqlId, String relationUri) throws Exception {
		JSONObject node = this.findNode(nodeSparqlId);
		
		JSONObject prop = new JSONObject();
		prop.put(JKEY_IS_MAPPING_PROPS_URI_REL, relationUri);
		prop.put(JKEY_IS_MAPPING, new JSONArray());
		
		JSONArray props = (JSONArray) node.get(JKEY_IS_PROPS);
		props.add(prop);
		return prop;
	}
	
	/**
	 * Create simple mapping with just one column name
	 * @param colName
	 * @return
	 * @throws Exception
	 */
	public JSONObject buildMappingWithCol(String colName) throws Exception {
		String colId = this.findColId(colName);
		
		JSONObject ret = new JSONObject();
		ret.put(JKEY_IS_COL_COL_ID, colId);
		return ret;
	}
	
	public JSONObject buildMappingWithCol(String colName, String[] transformIds) throws Exception {
		
		JSONObject ret = this.buildMappingWithCol(colName);
		
		JSONArray transformList = new JSONArray();
		for (String id : transformIds) {
			transformList.add(id);
		}
		ret.put(JKEY_IS_MAPPING_TRANSFORM_LIST, transformList);
		
		return ret;
	}
	
	public void addMapping(String nodeSparqlId, JSONObject mapping) throws Exception {
		JSONObject node = this.findNode(nodeSparqlId);
		JSONArray mapArr = new JSONArray();
		mapArr.add(mapping);
		node.put(JKEY_IS_MAPPING, mapArr);
	}
	
	public void addMapping(String nodeSparqlId, String propUri, JSONObject mapping) throws Exception {
		JSONObject prop = this.findProp(nodeSparqlId, propUri);
		JSONArray mapArr = new JSONArray();
		mapArr.add(mapping);
		prop.put(JKEY_IS_MAPPING, mapArr);
	}
	
	public boolean isUsed(String nodeSparqlId) throws Exception {
		return this.hasMapping(nodeSparqlId);
	}
	public boolean isUsed(String nodeSparqlId, String propUri) throws Exception {
		return this.hasMapping(nodeSparqlId, propUri);
	}
	public boolean hasMapping(String nodeSparqlId) throws Exception {
		JSONObject node = this.findNode(nodeSparqlId);
		JSONArray mapArr = (JSONArray) node.get(JKEY_IS_MAPPING);
		return mapArr != null && mapArr.size() > 0;
	}
	
	public boolean hasMapping(String nodeSparqlId, String propUri) throws Exception {
		JSONObject prop = null;
		try {
			prop = this.findProp(nodeSparqlId, propUri);
		} catch (Exception e) {
			return false;    // node doesn't even have the property
		}
		JSONArray mapArr = (JSONArray) prop.get(JKEY_IS_MAPPING);
		return mapArr != null && mapArr.size() > 0;
	}
	
	public void addURILookup(String nodeSparqlId, String propUri, String lookupSparqlId) throws Exception {
		JSONObject prop = this.findProp(nodeSparqlId, propUri);
		JSONArray lookupArr = new JSONArray();
		lookupArr.add(lookupSparqlId);
		prop.put(JKEY_IS_URI_LOOKUP, lookupArr);
	}
	
	public static ImportSpec createEmptySpec(NodeGroup ng) {
		ImportSpec spec = new ImportSpec();
		String lookupMode = null;
		
		// Add all nodes to the spec
		for (Node node : ng.getOrderedNodeList()) {
			spec.addNode(node.getSparqlID(), node.getUri(), lookupMode);
		}
		
		// Since it is an empty spec, props are not needed
		
		return spec;
	}
	
	
	/**
	 * Delete import spec columns which are not nodegroup sparqlIds
	 * @param ng
	 * @throws Exception
	 */
	public void deleteInvalidColumns(NodeGroup ng) throws Exception {
		JSONArray newCols = new JSONArray();
		for (int i=0; i < this.getNumColumns(); i++) {
			String colName = this.getColName(i);
			if (ng.getItemByBindingOrSparqlID(ImportSpec.colnameToSparqlID(colName)) != null) {
				newCols.add(this.getCol(i));
			}
		}
		this.json.put(JKEY_IS_COLUMNS, newCols);
	}
	
	public void addMissingColumns(NodeGroup ng) throws Exception {
		for (String id : ng.getReturnedSparqlIDs()) {
			String colName = ImportSpec.sparqlIDToColname(id);
			try {
				this.findColId(colName);
			} catch (Exception e) {
				this.addColumn(colName);
			}
		}
	}
	
	public void deleteInvalidMappings() throws Exception {
		
		for (Object n : (JSONArray) this.json.get(JKEY_IS_NODES)) {
			JSONObject nodeJson = (JSONObject) n;
			
			// copy only good mappings
			JSONArray good1 = new JSONArray();
			for (Object m1 : (JSONArray) nodeJson.get(JKEY_IS_MAPPING)) {
				JSONObject mapping = (JSONObject) m1;
				try {
					this.findColumn((String) mapping.get(JKEY_IS_MAPPING_COL_ID));
					good1.add(mapping);
				} catch (Exception e) {}
			}
			nodeJson.put(JKEY_IS_MAPPING, good1);
		
			for (Object p : (JSONArray) nodeJson.get(JKEY_IS_PROPS)) {
				JSONObject propJson = (JSONObject) p;
				
				// copy only good mappings
				JSONArray good2 = new JSONArray();
				for (Object m1 : (JSONArray) propJson.get(JKEY_IS_MAPPING)) {
					JSONObject mapping = (JSONObject) m1;
					// if mapping is a column

					try {
						// only copy good columns
						if (mapping.containsKey(JKEY_IS_MAPPING_COL_ID)) {
							this.findColumn((String) mapping.get(JKEY_IS_MAPPING_COL_ID));
						}
						good2.add(mapping);
					} catch (Exception e) {}
				}
				propJson.put(JKEY_IS_MAPPING, good2);
			}
		}
	}
	
	private void deleteInvalidProperties(NodeGroup ng) {
		// loop through nodes
		for (Object n : (JSONArray) this.json.get(JKEY_IS_NODES)) {
			JSONObject nodeJson = (JSONObject) n;
			
			// copy only good props
			JSONArray good = new JSONArray();
			
			for (Object p : (JSONArray) nodeJson.get(JKEY_IS_PROPS)) {
				JSONObject propJson = (JSONObject) p;
				
				Node node = ng.getNodeBySparqlID((String) nodeJson.get(JKEY_IS_NODE_SPARQL_ID));
				PropertyItem prop = node.getPropertyByURIRelation((String) propJson.get(JKEY_IS_MAPPING_PROPS_URI_REL));
				
				if (prop != null) {
					good.add(propJson);
				}
			}
			nodeJson.put(JKEY_IS_PROPS, good);
		}
	}
	
	public void updateSpecFromReturns(NodeGroup ng) throws Exception {
		String lookupMode = null;
		
		this.deleteInvalidColumns(ng);
		this.deleteInvalidMappings();
		this.addMissingColumns(ng);
		this.deleteInvalidProperties(ng);
		
		// Update each node in ng
		for (Node node : ng.getOrderedNodeList()) {
			JSONObject nObj;
			try {
				// find node and make sure TYPE is correct
				nObj = this.findNode(node.getSparqlID());
				nObj.put(JKEY_IS_NODE_TYPE, node.getUri());
				
			} catch (Exception e) {
				// add the node if it wasn't found
				nObj = this.addNode(node.getSparqlID(), node.getUri(), lookupMode);
			}
			
			// if node is returned in nodegroup
			if (node.getIsReturned() || node.getIsBindingReturned()) {
				JSONArray mapping = (JSONArray) nObj.get(JKEY_IS_MAPPING);
				// if mapping is empty
				if (mapping.size() == 0) {
					// build a new mapping
					String colName = ImportSpec.sparqlIDToColname(node.getBindingOrSparqlID());
					
					this.addMapping(node.getSparqlID(), this.buildMappingWithCol(colName));
				} // else leave old mapping
			} else {
				// node not returned so make sure mapping is empty
				nObj.put(JKEY_IS_MAPPING, new JSONArray());
			}
			
			// if node is returned in nodegroup
			if (node.getIsTypeReturned()) {
				
				// find or create type restriction
				JSONObject trObj = (JSONObject) nObj.get(JKEY_IS_TYPE_RESTRICTION);
				if (trObj == null) {
					trObj = new JSONObject();
					nObj.put(JKEY_IS_TYPE_RESTRICTION, trObj);
				}
				
				String colName = ImportSpec.sparqlIDToColname(node.getBindingOrSparqlID() + "_type");
				JSONArray mapArr = new JSONArray();
				mapArr.add( this.buildMappingWithCol(colName));
				trObj.put(JKEY_IS_MAPPING, mapArr);
				
			} else {
				// remove if no longer returned
				nObj.remove(JKEY_IS_TYPE_RESTRICTION);
			}
			
			
			// loop through properties that are returned
			for (PropertyItem prop : node.getPropertyItems()) {
				JSONObject pObj;
				
				// props are only found/added if returned
				if (prop.getIsReturned() || prop.getIsBindingReturned()) {
					// find or add
					try {
						pObj = this.findProp(node.getSparqlID(), prop.getUriRelationship());
					} catch (Exception e) {
						pObj = this.addProp(node.getSparqlID(), prop.getUriRelationship());
					}
					
					// if mapping is empty
					JSONArray mapping = (JSONArray) pObj.get(JKEY_IS_MAPPING);
					if (mapping.size() == 0) {
						// add a column and simple mapping
						String colName = ImportSpec.sparqlIDToColname(prop.getBindingOrSparqlID());
						this.addMapping(node.getSparqlID(), prop.getUriRelationship(), this.buildMappingWithCol(colName));
					}
				} else {
					// remove if no longer returned
					try {
						pObj = this.findProp(node.getSparqlID(), prop.getUriRelationship());
						JSONArray mapping = (JSONArray) pObj.get(JKEY_IS_MAPPING);
						if (mapping.size() == 0)
							this.deleteProperty(node.getSparqlID(), prop.getUriRelationship());
					} catch (Exception e) {
						// ignore if findProp failed
					}
				}
			}
		}
	}
	
	public static ImportSpec createSpecFromReturns(NodeGroup ng) throws Exception {
		ImportSpec spec = new ImportSpec();
		spec.updateSpecFromReturns(ng);
		
		return spec;
	}
	
	/**
	 * addsURILookup where property's bindingOrSparqlID contains propRegex AND the property has a mapping
	 * sets the lookup to the property's node
	 * sets the node lookup mode to lookupMode
	 * @param ng - ng matching this importSpec
	 * @param propRegex - regular express to search property bindings or sparqlID
	 * @param lookupMode - mode to set the property's node's uri lookup mode
	 * @throws Exception - if importSpec doesn't contain elements of ng, etc.
	 */
	public void addURILookups(NodeGroup ng, String propRegex, String lookupMode) throws Exception {
		switch (lookupMode) {
		case LOOKUP_MODE_ERR_IF_MISSING:
		case LOOKUP_MODE_CREATE_IF_MISSING:
		case LOOKUP_MODE_ERR_IF_EXISTS:
			break;
		default:
			throw new Exception("Invalid lookupMode: " + lookupMode);
		}
		Pattern p = Pattern.compile(propRegex);
		for (Node node : ng.getOrderedNodeList()) {
			String nodeID = node.getSparqlID();
			
			// loop through properties that are returned
			for (PropertyItem prop : node.getPropertyItems()) {
				String name = prop.getBindingOrSparqlID();
				if (p.matcher(name).find()) {
					JSONObject pObj = this.findProp(nodeID, prop.getUriRelationship());
					JSONArray mArr = (JSONArray) pObj.get(JKEY_IS_MAPPING);
					
					// if the property is mapped, add lookup
					if (mArr != null && mArr.size() > 0) {
						// add lookup to property
						JSONArray lookupArray = (JSONArray) pObj.get(JKEY_IS_URI_LOOKUP);
						if (lookupArray == null) {
							lookupArray = new JSONArray();
						}
						if (!lookupArray.contains(nodeID)) {
							lookupArray.add(nodeID);
						}
						pObj.put(JKEY_IS_URI_LOOKUP, lookupArray);
						
						// set node lookup mode
						JSONObject nodeObj = this.findNode(nodeID);
						nodeObj.put(JKEY_IS_NODE_LOOKUP_MODE, lookupMode);
					}
				}
			}
		}
	}
	

	public void changePropertyDomain(String nodeSparqlID, String oldURI, String newURI) throws Exception {
		
		int n = this.getNodeIndex(nodeSparqlID);
		if (n < 0) throw new Exception("Can't find node in import spec: " + nodeSparqlID);
		int p = this.getNodePropIndex(n, oldURI);
		
		// if property had no mappings.  Done.
		// if property is here, change it.
		if (p >= 0) {
			JSONObject prop  = this.getNodeProp(n,p);
			prop.put(JKEY_IS_MAPPING_PROPS_URI_REL, newURI);
		}
		
	}

	public void deleteProperty(String nodeSparqlID, String uriRelationship) throws Exception {
		int n = this.getNodeIndex(nodeSparqlID);
		if (n < 0) throw new Exception("Can't find node in import spec: " + nodeSparqlID);
		int p = this.getNodePropIndex(n, uriRelationship);
		
		// if property is here, delete it
		if (p >= 0) {
			int numProps = this.getNodeNumProperties(n);
			JSONArray newProps = new JSONArray();
			for (int i=0; i < numProps; i++) {
				if (!this.getNodePropUriRel(n, i).equals(uriRelationship)) {
					newProps.add(this.getNodeProp(n, i));
				}
			}
			this.getNode(n).put(JKEY_IS_PROPS, newProps);
		}
		
	}

	public void changeNodeDomain(String nodeSparqlID, String newURI) {
		int n = this.getNodeIndex(nodeSparqlID);
		this.getNode(n).put(JKEY_IS_NODE_TYPE, newURI);
	}
	
	public boolean containsNode(String nodeSparqlID) {
		return (this.getNodeIndex(nodeSparqlID) > -1);
	}
	public static String sparqlIDToColname(String sparqlID) {
		if (sparqlID.startsWith("?"))
			return sparqlID.substring(1);
		else
			return sparqlID;
	}
	
	public static String colnameToSparqlID(String colname) {
		return "?" + colname;
	}
	
}
