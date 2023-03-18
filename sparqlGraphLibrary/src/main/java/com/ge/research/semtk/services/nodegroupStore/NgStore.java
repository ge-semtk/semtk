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

package com.ge.research.semtk.services.nodegroupStore;

import java.util.ArrayList;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.utility.Utility;

/**
 * Stores items (e.g. nodegroups, reports) in triplestore
 * @author 200001934
 */
public class NgStore {
	
	public static enum StoredItemTypes { Report, StoredItem, PrefabNodeGroup };
	
	private String dataGraph = null;
	private SparqlEndpointInterface sei = null;
	
	public NgStore(SparqlEndpointInterface sei) {
		this.dataGraph = sei.getGraph();
		this.sei = sei;
	}
	 
	/**
	 * Get sgJson or null
	 */
	public SparqlGraphJson getNodegroup(String id) throws Exception {
		Table tbl = this.getNodegroupTable(id);
		if(tbl.getNumRows() > 0){
			// we have a result. for now, let's assume that only the first result is valid.
			ArrayList<String> tmpRow = tbl.getRows().get(0);
			int targetCol = tbl.getColumnIndex("NodeGroup");

			String ngJSONstr = tmpRow.get(targetCol);
			JSONParser jParse = new JSONParser();
			JSONObject json = (JSONObject) jParse.parse(ngJSONstr); 
			SparqlGraphJson sgJson = new SparqlGraphJson(json);
			return sgJson;
		} else {
			return null;
		}
	}
	
	/**
	 * Return any type of stored item
	 */
	public String getStoredItem(String id, StoredItemTypes blobType) throws Exception {
		Table tbl = this.getStoredItemTable(id, blobType);
		if (tbl.getNumRows() == 0) {
			return null;
		} else if (tbl.getNumRows() == 1) {
			if (blobType == StoredItemTypes.PrefabNodeGroup)
				return tbl.getCell(0, "NodeGroup");
			else
				return tbl.getCell(0, "stringChunk");
		} else {
			throw new Exception("Internal error: multiple rows found");
		}
	}
	
	/**
	 * Get the number of stored items with a given ID and blob type.
	 */
	public int getNumStoredItems(String id, StoredItemTypes blobType) throws Exception{
		return getStoredItemTable(id, blobType).getNumRows();
	}

	/**
	 * Return item as a table.  Column will be NodeGroup or stringChunk
	 */
	public Table getStoredItemTable(String id, StoredItemTypes blobType)  throws Exception {
		if (blobType == StoredItemTypes.PrefabNodeGroup) 
			return this.getNodegroupTable(id);
		else
			return this.getStringBlobTable(id, blobType);
	}
	
	/**
	 * Return nodegroup table with zero rows, or one row containing full nodegroup json string
	 * @param id
	 * @return table with 0 or 1 rows
	 * @throws Exception
	 */
	public Table getNodegroupTable(String id)  throws Exception {
		StringBuilder ngStr = new StringBuilder();
		ArrayList<String> queries = this.genSparqlGetNodegroupById(id);
		
		Table retTable = this.executeQuery(queries.get(0));
		
		if (retTable.getNumRows() > 0) {
			ngStr = new StringBuilder(retTable.getCellAsString(0,  "NodeGroup"));

			// look for additional text, using second query
			Table catTable =  this.executeQuery(queries.get(1));
		
			for (int i=0; i < catTable.getNumRows(); i++) {
				ngStr.append(catTable.getCellAsString(i, "NodeGroup"));
			}
			
			int col = retTable.getColumnIndex("NodeGroup");
			retTable.setCell(0, col, ngStr.toString());
		}
		
		return retTable;
	}
	
	/**
	 * Return nodegroup table with zero rows, or one row containing full nodegroup json string
	 * @param id
	 * @return table with 0 or 1 rows
	 * @throws Exception
	 */
	public Table getStringBlobTable(String id, StoredItemTypes blobType)  throws Exception {
		String query = this.genSparqlGetStringBlobById(id, blobType);
		
		Table retTable = this.executeQuery(query);

		if (retTable.getNumRows() > 0) {
			// combine rows into one by appending "stringChunk"
			StringBuilder blob = new StringBuilder();
	
			for (int i=0; i < retTable.getNumRows(); i++) {
				blob.append(retTable.getCellAsString(i, "stringChunk"));
			}
			
			ArrayList<String> row0 = retTable.getRow(0);
			retTable.clearRows();
			retTable.addRow(row0);
			retTable.setCell(0, "stringChunk", SparqlToXUtils.unescapeFromSparql(blob.toString()));		
		}
		return retTable;	
	}

	// use getStoredItemIdList()
	@Deprecated 
	public Table getNodeGroupIdList() throws Exception {
		return this.getNodeGroupIdList(false);
	}
	// use getStoredItemIdList()
	@Deprecated
	public Table getNodeGroupIdList(boolean suFlag) throws Exception {
		return this.executeQuery(this.genSparqlGetStoredItemIdList(StoredItemTypes.PrefabNodeGroup));
	}

	// use getFullStoredItemList()
	@Deprecated
	public Table getFullNodeGroupList() throws Exception {
		return this.getFullNodeGroupList(false);
	}
	
	// use getFullStoredItemList()
	@Deprecated
	public Table getFullNodeGroupList(boolean suFlag) throws Exception {
		return this.executeQuery(this.genSparqlGetFullStoredItemList(StoredItemTypes.PrefabNodeGroup));
	}
	// use getStoredItemMetadata
	@Deprecated
	public Table getNodeGroupMetadata() throws Exception {
		return this.getNodeGroupMetadata(false);
	}
	
	// use getStoredItemMetadata
	@Deprecated
	public Table getNodeGroupMetadata(boolean suFlag) throws Exception {
		return this.executeQuery(this.genSparqlGetStoredItemMetadata(StoredItemTypes.PrefabNodeGroup));
	}
		
	public Table getStoredItemIdList(StoredItemTypes itemType) throws Exception {
		return this.executeQuery(this.genSparqlGetStoredItemIdList(itemType));
	}
	
	public Table getFullStoredItemList(StoredItemTypes itemType) throws Exception {
		return this.executeQuery(this.genSparqlGetFullStoredItemList(itemType));
	}
	
	public Table getStoredItemMetadata(StoredItemTypes itemType) throws Exception {
		return this.executeQuery(this.genSparqlGetStoredItemMetadata(itemType));
	}
	
	public void deleteNodeGroup(String id) throws Exception {
		this.executeConfirmQuery(this.genSparqlDeleteNodeGroup(id));
	}
	
	/**
	 * Delete any type of stored item
	 */
	public void deleteStoredItem(String id, StoredItemTypes blobType) throws Exception {
		if (blobType == StoredItemTypes.PrefabNodeGroup) 
			this.executeConfirmQuery(this.genSparqlDeleteNodeGroup(id));
		else
			this.executeConfirmQuery(this.genSparqlDeleteStringBlob(id));
	}
	
	/**
	 * Rename a stored item
	 * @param id the current id
	 * @param newId the new id
	 * @param blobType
	 * @throws Exception
	 */
	public void renameStoredItem(String id, String newId, StoredItemTypes blobType) throws Exception {

		// validate the current id
		int numStored = this.getNumStoredItems(id, blobType);
		if(numStored < 1){  		// no stored item with this id
			throw new Exception("Cannot rename item with current id '" + id + "' and type: '" + blobType + "': no such item exists");
		} else if(numStored > 1){  // multiple items with this id
			throw new Exception("Cannot rename item with current id '" + id + "' and type: '" + blobType + "': multiple such items exist");
		}

		// validate the new id
		if (newId == null || newId.trim().isEmpty()) {
			throw new Exception("Cannot rename item to null or empty id");
		}else if(this.getNumStoredItems(newId, blobType) > 0){
			throw new Exception("Cannot rename item to new id '" + newId + "' and type: '" + blobType + "': item with this id already exists");
		}

		// perform the rename
		this.executeConfirmQuery(this.genSparqlRenameNodeGroup(id, newId, blobType));
	}

	public void insertNodeGroup(JSONObject sgJsonJson, JSONObject connJson, String id, String comments, String creator ) throws Exception {
		ArrayList<String> insertQueries = this.genSparqlInsertNodeGroup(sgJsonJson, connJson, id, comments, creator);
	
		for (String insertQuery : insertQueries) {
			this.executeConfirmQuery(insertQuery);
		}
	}

	public void insertStringBlob(String blob, StoredItemTypes blobType, String id, String comments, String creator ) throws Exception {
		String safeBlob = SparqlToXUtils.escapeForSparql(blob);
		ArrayList<String> insertQueries = this.genSparqlInsertStringBlob(safeBlob, blobType, id, comments, creator);
	
		for (String insertQuery : insertQueries) {
			this.executeConfirmQuery(insertQuery);
		}
	}
	
	//-------------------- private -------------------
	
	private Table executeQuery(String sparql) throws Exception {
		AuthorizationManager.nextQuerySemtkSuper();
		return this.sei.executeQueryToTable(sparql);
	}
	
	private void executeConfirmQuery(String sparql) throws Exception {
		AuthorizationManager.nextQuerySemtkSuper();
		this.sei.executeQueryAndConfirm(sparql);
	}
	
	// get sparql queries for getting the needed info. 
	private ArrayList<String> genSparqlGetNodegroupById(String id){
		ArrayList<String> ret = new ArrayList<String>();
		String rdf10ValuesClause = "VALUES ?ID { \"" + id + "\" \"" + id + "\"^^<http://www.w3.org/2001/XMLSchema#string>} . ";
		
		String query  = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"SELECT distinct ?ID ?NodeGroup ?comments " +
						"FROM <" + this.dataGraph + "> WHERE { " +
						"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
						"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
						rdf10ValuesClause +
						"?PrefabNodeGroup prefabNodeGroup:NodeGroup ?NodeGroup . " +
		   				"optional { ?PrefabNodeGroup prefabNodeGroup:comments ?comments . } " +
		   				"}";		
		ret.add(query);
		
		query = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
				"SELECT distinct ?NodeGroup ?counter " +
				"FROM <" + this.dataGraph + "> WHERE { " +
				"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
				"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
				rdf10ValuesClause +
				
			    "?PrefabNodeGroup prefabNodeGroup:stringChunk ?SemTkStringChunk . " +
			    "      ?SemTkStringChunk prefabNodeGroup:counter ?counter ." +
				"      ?SemTkStringChunk prefabNodeGroup:chunk ?NodeGroup ."  +
   				"} ORDER BY ?counter";
		
		ret.add(query);
		
		return ret;
	}
	
	// get sparql queries for getting the needed info. 
	// returns multiple rows for each id, sorted by counter
	private String genSparqlGetStringBlobById(String id, StoredItemTypes blobType){
		String rdf10ValuesClause = "VALUES ?ID { \"" + id + "\"} . ";

		String query  = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
				"SELECT distinct ?stringChunk ?counter \n" +
				"FROM <" + this.dataGraph + "> WHERE { \n" +
				"?item a prefabNodeGroup:" + blobType.toString() + " . \n" +
				"?item prefabNodeGroup:ID ?ID . \n" +
				rdf10ValuesClause +
				"?item prefabNodeGroup:stringChunk ?stringChunkObj . \n" +
				"  ?stringChunkObj prefabNodeGroup:chunk ?stringChunk . \n" +
				"  ?stringChunkObj prefabNodeGroup:counter ?counter . \n" +
				"} ORDER BY ?counter";		
		
		return query;
	}
	
	private String genSparqlGetFullStoredItemList(StoredItemTypes itemType){
		String retval = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"SELECT distinct ?ID ?NodeGroup ?comments " +
						"FROM <" + this.dataGraph + "> WHERE { " +
						"?item a prefabNodeGroup:" + itemType.toString() + " . " +
						"?item a prefabNodeGroup:PrefabNodeGroup. " +
						"?item prefabNodeGroup:ID ?ID . " +
						"?item prefabNodeGroup:NodeGroup ?NodeGroup . " +
						"optional { ?item prefabNodeGroup:comments ?comments . } " +
						"}";
		return retval;
	}
	
	private String genSparqlGetStoredItemIdList(StoredItemTypes itemType){
		String retval = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"SELECT distinct ?ID " +
						"FROM <" + this.dataGraph + "> WHERE { " +
						"?item a prefabNodeGroup:" + itemType.toString() + " . " +
						"?item a prefabNodeGroup:PrefabNodeGroup. " +
						"?item prefabNodeGroup:ID ?ID . " +
						"}";
		return retval;
	}
	
	private String genSparqlGetStoredItemMetadata(StoredItemTypes itemType){
		// Note that generating sparql properly requires oInfo and conn.  OInfo requires oinfo_client.  Do that some day.
		
		// StoredItem is a superclass, rest are not.
		String filter;
		if (itemType == StoredItemTypes.StoredItem) {
			filter = "VALUES ?itemType { prefabNodeGroup:StoredItem prefabNodeGroup:PrefabNodeGroup prefabNodeGroup:Report  } . ";
		} else {
			filter = "FILTER (?itemType = prefabNodeGroup:" + itemType.toString() + ") . ";
		}
		String retval = "PREFIX XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
						"PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"SELECT distinct ?ID ?comments ?creationDate ?creator ?itemType " +
						"FROM <" + this.dataGraph + "> WHERE { " +
						"?item a ?itemType . " +
						filter +
						"?item prefabNodeGroup:ID ?ID . " +
						"optional { ?item prefabNodeGroup:comments ?comments . } " +
				   		"optional { ?item prefabNodeGroup:creationDate ?creationDate . } " +
				   		"optional { ?item prefabNodeGroup:creator ?creator . } " +
						"}";
		return retval;
	}

	private String genSparqlDeleteNodeGroup(String jobId) {
		String rdf10ValuesClause = "VALUES ?jobId { \"" + jobId + "\" \"" + jobId + "\"^^<http://www.w3.org/2001/XMLSchema#string>} . ";

		String ret = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
				"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"WITH <" + this.dataGraph + "> DELETE { " +
				"  ?PrefabNodeGroup ?pred ?obj." +
                "  ?SemTkConnection ?pred1 ?obj1. " +
				"  ?SemTkStringChunk ?pred2 ?obj2 . "    + 
				"} WHERE { " +
				"  ?PrefabNodeGroup ?pred ?obj." +
				"  ?PrefabNodeGroup prefabNodeGroup:ID ?jobId . " + 
				rdf10ValuesClause +
                "  ?PrefabNodeGroup prefabNodeGroup:originalConnection ?SemTkConnection . " +
				"  optional { " +
                "     ?SemTkConnection ?pred1 ?obj1. " +
				"  } " +
                "  optional { " +
				"     ?PrefabNodeGroup prefabNodeGroup:stringChunk ?SemTkStringChunk. " +
				"     ?SemTkStringChunk ?pred2 ?obj2 . "    + 
				"  } " +
				"}";
		return ret;
	}
	
	private String genSparqlDeleteStringBlob(String id) {
		String rdf10ValuesClause = "VALUES ?ID { \"" + id + "\" \"" + id + "\" } . ";

		String ret = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
				"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"WITH <" + this.dataGraph + "> DELETE { " +
                "  ?stringChunkObj ?scPred ?scObj . " +
				"  ?PrefabNodeGroup ?pred ?obj." +
				"} WHERE { " +
				"  ?PrefabNodeGroup prefabNodeGroup:ID ?ID . " + 
				   rdf10ValuesClause +
                "  ?PrefabNodeGroup prefabNodeGroup:stringChunk ?stringChunkObj . " +
                "  ?stringChunkObj ?scPred ?scObj . " +
				"  ?PrefabNodeGroup ?pred ?obj." +
				"}";
		return ret;
	}

	/**
	 * Generate SPARQL to rename a nodegroup
	 * @param id the current id
	 * @param newId the new id
	 * @return the SPARQL
	 */
	private String genSparqlRenameNodeGroup(String id, String newId, StoredItemTypes blobType) {
		String ret = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> "
				+ "WITH <" + this.dataGraph + "> "
				+ "DELETE { ?item prefabNodeGroup:ID '" + id + "' } "
				+ "INSERT { ?item prefabNodeGroup:ID '" + newId + "' } "
				+ "WHERE  { "
				+ "	   ?item a prefabNodeGroup:" + blobType + " . " 
				+ "    ?item prefabNodeGroup:ID '" + id + "'"
				+ "}";
		return ret;
	}

	private ArrayList<String> genSparqlInsertNodeGroup(JSONObject sgJsonJson, JSONObject connJson, String id, String comments, String creator) throws Exception {
		final int SPLIT = 20000;
		
		// extract the connJson
		SparqlConnection conn = new SparqlConnection();
		conn.fromJson(connJson);
		SparqlEndpointInterface connSei = conn.getDefaultQueryInterface();

		String ngStr = legalizeSparqlInputString(sgJsonJson.toJSONString());
		String [] chunks = getNextChunk(ngStr, SPLIT);
		
		ArrayList<String> ret = new ArrayList<String>();
		String ngURI = "generateSparqlInsert:semtk_ng_" +  UUID.randomUUID().toString();
		String query = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
				"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"PREFIX generateSparqlInsert:<http://semtk.research.ge.com/generated#> " +
				"PREFIX XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
				"INSERT { GRAPH <" + this.dataGraph + "> { " +
				"      ?SemTkConnection__0 a prefabNodeGroup:SemTkConnection . " + 
				"      ?SemTkConnection__0 prefabNodeGroup:connectionAlias \""    + conn.getName()  +  "\" ." +
				"      ?SemTkConnection__0 prefabNodeGroup:domain \""             + conn.getDomain()  +   "\" ." +
				"      ?SemTkConnection__0 prefabNodeGroup:dsDataset \""          + connSei.getGraph()  +   "\" ." +
				"      ?SemTkConnection__0 prefabNodeGroup:dsURL \""              + connSei.getServerAndPort()  +   "\" ." +
				"      ?SemTkConnection__0 prefabNodeGroup:originalServerType \"" + connSei.getServerType()  +   "\" ." +

			    "	   ?PrefabNodeGroup__0 a prefabNodeGroup:PrefabNodeGroup . " +
			    "	   ?PrefabNodeGroup__0 prefabNodeGroup:ID \""            + id +   "\" ." +
			    "	   ?PrefabNodeGroup__0 prefabNodeGroup:NodeGroup \""     + chunks[0] +   "\" ." +
			    "	   ?PrefabNodeGroup__0 prefabNodeGroup:comments \""      + SparqlToXUtils.safeSparqlString(comments) +   "\" ." +
			    "	   ?PrefabNodeGroup__0 prefabNodeGroup:creationDate \""  + Utility.getSPARQLCurrentDateString() +   "\" ." +
			    "	   ?PrefabNodeGroup__0 prefabNodeGroup:creator \""       + creator.trim() +   "\" ." +
			    "	   ?PrefabNodeGroup__0 prefabNodeGroup:originalConnection ?SemTkConnection__0 . " +
			    "} } " +
			    " WHERE {     " +
			    "      BIND (generateSparqlInsert:semtk_conn_" + UUID.randomUUID().toString() + " AS ?SemTkConnection__0)." +
			    "      BIND (" + ngURI + " AS ?PrefabNodeGroup__0)." +
			    "  }";
		ret.add(query);
		
		int i=0;
		while (chunks[1].length() > 0) {
			chunks = getNextChunk(chunks[1], SPLIT);
			
			query = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
					"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
					"PREFIX generateSparqlInsert:<http://semtk.research.ge.com/generated#> " +
					"PREFIX XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
					"INSERT { GRAPH <" + this.dataGraph + "> { " +
					"      ?SemTkStringChunk__0 a prefabNodeGroup:StringChunk . " +
					"      ?SemTkStringChunk__0 prefabNodeGroup:counter \""    + i++  +   "\" ." +
					"      ?SemTkStringChunk__0 prefabNodeGroup:chunk \""    + chunks[0]  +   "\" ." +

				    "	   ?PrefabNodeGroup__0 prefabNodeGroup:stringChunk ?SemTkStringChunk__0 . " +
				    "} } " +
				    " WHERE {     " +
				    "      BIND (generateSparqlInsert:semtk_cat_" + UUID.randomUUID().toString() + " AS ?SemTkStringChunk__0)." +
				    "      BIND (" + ngURI + " AS ?PrefabNodeGroup__0)." +
				    "  }";
			ret.add(query);
		}
		return ret;
	}

	/**
	 * 
	 * @param blob - MUST BE already made safe with something like SparqlToXUtils.escapeForSparql(blob);
	 * @param blobType
	 * @param id
	 * @param comments
	 * @param creator
	 * @return
	 * @throws Exception
	 */
	private ArrayList<String> genSparqlInsertStringBlob(String blob, StoredItemTypes blobType, String id, String comments, String creator) throws Exception {
		final int SPLIT = 20000;
		
		ArrayList<String> ret = new ArrayList<String>();
		String uri = "generateSparqlInsert:semtk_blob_" +  UUID.randomUUID().toString();
		String query = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
				"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"PREFIX generateSparqlInsert:<http://semtk.research.ge.com/generated#> " +
				"PREFIX XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
				"INSERT { GRAPH <" + this.dataGraph + "> { " +
			    "	   ?BlobString__0 a prefabNodeGroup:" + blobType + " . " +
			    "	   ?BlobString__0 prefabNodeGroup:ID \""            + id +   "\" ." +
			    "	   ?BlobString__0 prefabNodeGroup:comments \""      + SparqlToXUtils.safeSparqlString(comments) +   "\" ." +
			    "	   ?BlobString__0 prefabNodeGroup:creationDate \""  + Utility.getSPARQLCurrentDateString() +   "\" ." +
			    "	   ?BlobString__0 prefabNodeGroup:creator \""       + creator.trim() +   "\" ." +
			    "} } " +
			    " WHERE {     " +
			    "      BIND (" + uri + " AS ?BlobString__0)." +
			    "  }";
		ret.add(query);
		
		// to mimic the nodegroup function, start with chunks[0] empty and chunks[1] containing the whole blob
		String [] chunks = new String [] { "", blob};
		int i=0;
		while (chunks[1].length() > 0) {
			chunks = getNextChunk(chunks[1], SPLIT);
			
			query = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
					"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
					"PREFIX generateSparqlInsert:<http://semtk.research.ge.com/generated#> " +
					"PREFIX XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
					"INSERT { GRAPH <" + this.dataGraph + "> { " +
					"      ?SemTkStringChunk__0 a prefabNodeGroup:StringChunk . " +
					"      ?SemTkStringChunk__0 prefabNodeGroup:counter \""    + i++  +   "\" ." +
					"      ?SemTkStringChunk__0 prefabNodeGroup:chunk \""    + chunks[0]  +   "\" ." +

				    "	   ?BlobString__0 prefabNodeGroup:stringChunk ?SemTkStringChunk__0 . " +
				    "} } " +
				    " WHERE {     " +
				    "      BIND (generateSparqlInsert:semtk_cat_" + UUID.randomUUID().toString() + " AS ?SemTkStringChunk__0)." +
				    "      BIND (" + uri + " AS ?BlobString__0)." +
				    "  }";
			ret.add(query);
		}
		return ret;
	}
	
	/**
	 * Split a string at given split point, but shorter if last char would be a backslash
	 */
	private static String[] getNextChunk(String value, int split) throws Exception {

		String[] ret = new String[2];
		
		if (value.length() > split) {
			while (value.charAt(split - 1) == '\\') {
				split--;
				if (split == 0) {
					throw new Exception("Can't split a string full of backslashes.");
				}
			}
			ret[0] = value.substring(0, split);
			ret[1] = value.substring(split);
			
		} else {
			ret[0] = value;
			ret[1] = "";
		}
		
		return ret;
	}
	
	private static String legalizeSparqlInputString(String input) {
		String ret =  input.replaceAll("(?<!\\\\)\\\\\"", "\\\\\\\\\\\\\"");   // double-escape any escaped quotes
		ret =           ret.replaceAll("(?<!\\\\)\"", "\\\\\"");   // replace un excaped quotes with escaped ones
		ret = ret.replaceAll("\\\\/", "/");  // don't escape forward slash in sparql
		return ret;
	}
	
	public static Table createEmptyStoreCsvTable() throws Exception {
		Table outTable = new Table(
				new String[] {"ID","comments","creator","jsonFile"},
				new String[] {"unknown","unknown","unknown","unknown"});
		return outTable;
	}
	
	public static void addRowToStoreCsvTable(Table csvTable, String id, String comments, String creator, String jsonFileName) throws Exception {
		csvTable.addRow(new String[] {id, comments, creator, jsonFileName});
	}
	
}
