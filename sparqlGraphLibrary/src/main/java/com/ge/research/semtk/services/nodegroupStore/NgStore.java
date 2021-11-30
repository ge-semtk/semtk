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
 * Store nodegroups in triplestore graph "sei"
 * @author 200001934
 *
 */
public class NgStore {
	
	public static enum StringBlobTypes { Report };
	
	private String dataGraph = null;
	private SparqlEndpointInterface sei = null;
	
	public NgStore(SparqlEndpointInterface sei) {
		this.dataGraph = sei.getGraph();
		this.sei = sei;
	}
	
	/**
	 * Get sgJson or null
	 * @param id
	 * @return
	 * @throws Exception
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
	
	public String getStringBlob(String id, StringBlobTypes blobType) throws Exception {
		Table tbl = this.getStringBlobTable(id, blobType);
		if (tbl.getNumRows() == 0) {
			return null;
		} else if (tbl.getNumRows() == 1) {
			return tbl.getCell(0, "stringChunk");
		} else {
			throw new Exception("Internal error: muliple rows found");
		}
		
	}
	
	public Table getNodegroupTable(String id)  throws Exception {
		return this.getNodegroupTable(id, false);
	}
	
	public Table getStringBlobTable(String id, StringBlobTypes blobType)  throws Exception {
		return this.getStringBlobTable(id, blobType, false);
	}
	
	/**
	 * Return nodegroup table with zero rows, or one row containing full nodegroup json string
	 * @param id
	 * @return table with 0 or 1 rows
	 * @throws Exception
	 */
	public Table getNodegroupTable(String id, boolean suFlag)  throws Exception {
		StringBuilder ngStr = new StringBuilder();
		ArrayList<String> queries = this.genSparqlGetNodegroupById(id);
		
		Table retTable = this.executeQuery(queries.get(0), suFlag);
		
		if (retTable.getNumRows() > 0) {
			ngStr = new StringBuilder(retTable.getCellAsString(0,  "NodeGroup"));

			// look for additional text, using second query
			Table catTable =  this.executeQuery(queries.get(1), suFlag);
		
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
	public Table getStringBlobTable(String id, StringBlobTypes blobType, boolean suFlag)  throws Exception {
		String query = this.genSparqlGetStringBlobById(id, blobType);
		
		Table retTable = this.executeQuery(query, suFlag);

		// combine rows into one by appending "stringChunk"
		StringBuilder blob = new StringBuilder();

		for (int i=0; i < retTable.getNumRows(); i++) {
			blob.append(retTable.getCellAsString(i, "stringChunk"));
		}
		
		ArrayList<String> row0 = retTable.getRow(0);
		retTable.clearRows();
		retTable.addRow(row0);
		retTable.setCell(0, "stringChunk", SparqlToXUtils.unescapeFromSparql(blob.toString()));		
		
		return retTable;
		
	}
	
	public Table getNodeGroupIdList() throws Exception {
		return this.getNodeGroupIdList(false);
	}
	
	public Table getNodeGroupIdList(boolean suFlag) throws Exception {
		return this.executeQuery(this.genSparqlGetNodeGroupIdList(), suFlag);
	}
	
	public Table getFullNodeGroupList() throws Exception {
		return this.getFullNodeGroupList(false);
	}
	
	public Table getFullNodeGroupList(boolean suFlag) throws Exception {
		return this.executeQuery(this.genSparqlGetFullNodeGroupList(), suFlag);
	}
	
	public Table getNodeGroupMetadata() throws Exception {
		return this.getNodeGroupMetadata(false);
	}
	
	public Table getNodeGroupMetadata(boolean suFlag) throws Exception {
		return this.executeQuery(this.genSparqlGetNodeGroupMetadata(), suFlag);
	}
	
	public Table getStringBlobMetadata(StringBlobTypes blobType) throws Exception {
		return this.getStringBlobMetadata(blobType, false);
	}
	
	public Table getStringBlobMetadata(StringBlobTypes blobType, boolean suFlag) throws Exception {
		return this.executeQuery(this.genSparqlGetStringBlobMetadata(blobType), suFlag);
	}
	
	public void deleteNodeGroup(String id) throws Exception {
		this.deleteNodeGroup(id, false);
	}
	
	public void deleteNodeGroup(String id, boolean suFlag) throws Exception {
		this.executeConfirmQuery(this.genSparqlDeleteNodeGroup(id), suFlag);
	}
	
	public void deleteStringBlob(String id, StringBlobTypes blobType) throws Exception {
		this.deleteStringBlob(id, blobType, false);
	}
	
	public void deleteStringBlob(String id, StringBlobTypes blobType, boolean suFlag) throws Exception {
		this.executeConfirmQuery(this.genSparqlDeleteStringBlob(id), suFlag);
	}
	
	public void insertNodeGroup(JSONObject sgJsonJson, JSONObject connJson, String id, String comments, String creator ) throws Exception {
		this.insertNodeGroup(sgJsonJson, connJson, id, comments, creator, false);
	}
	
	public void insertNodeGroup(JSONObject sgJsonJson, JSONObject connJson, String id, String comments, String creator, boolean suFlag ) throws Exception {
		ArrayList<String> insertQueries = this.genSparqlInsertNodeGroup(sgJsonJson, connJson, id, comments, creator);
	
		for (String insertQuery : insertQueries) {
			this.executeConfirmQuery(insertQuery, suFlag);
		}
	}
	
	public void insertStringBlob(String blob, StringBlobTypes blobType, String id, String comments, String creator ) throws Exception {
		this.insertStringBlob(blob, blobType, id, comments, creator, false);
	}
	
	public void insertStringBlob(String blob, StringBlobTypes blobType, String id, String comments, String creator, boolean suFlag ) throws Exception {
		String safeBlob = SparqlToXUtils.escapeForSparql(blob);
		ArrayList<String> insertQueries = this.genSparqlInsertStringBlob(safeBlob, blobType, id, comments, creator);
	
		for (String insertQuery : insertQueries) {
			this.executeConfirmQuery(insertQuery, suFlag);
		}
	}
	
	//-------------------- private -------------------
	
	private Table executeQuery(String sparql, boolean suFlag) throws Exception {
		if (suFlag) {
			AuthorizationManager.nextQuerySemtkSuper();
		}
		return this.sei.executeQueryToTable(sparql);
	}
	
	private void executeConfirmQuery(String sparql, boolean suFlag) throws Exception {
		if (suFlag) {
			AuthorizationManager.nextQuerySemtkSuper();
		}
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
	private String genSparqlGetStringBlobById(String id, StringBlobTypes blobType){
		String rdf10ValuesClause = "VALUES ?ID { \"" + id + "\"} . ";

		String query  = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
				"SELECT distinct ?stringChunk ?counter \n" +
				"FROM <" + this.dataGraph + "> WHERE { \n" +
				"?blob a prefabNodeGroup:" + blobType.toString() + " . \n" +
				"?blob prefabNodeGroup:id ?ID . \n" +
				rdf10ValuesClause +
				"?blob prefabNodeGroup:stringChunk ?stringChunkObj . \n" +
				"  ?strungChunkObj prefabNodeGroup:chunk ?stringChunk . \n" +
				"  ?strungChunkObj prefabNodeGroup:counter ?counter . \n" +
				"} ORDER BY ?counter";		
		
		return query;
	}
	
	
	
	
	
	private String genSparqlGetFullNodeGroupList(){
		String retval = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"SELECT distinct ?ID ?NodeGroup ?comments " +
						"FROM <" + this.dataGraph + "> WHERE { " +
						"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
						"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
						"?PrefabNodeGroup prefabNodeGroup:NodeGroup ?NodeGroup . " +
						"optional { ?PrefabNodeGroup prefabNodeGroup:comments ?comments . } " +
						"}";
		return retval;
	}

	
	private String genSparqlGetNodeGroupIdList(){
		String retval = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"SELECT distinct ?ID " +
						"FROM <" + this.dataGraph + "> WHERE { " +
						"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
						"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
						"}";
		return retval;
	}
	
	private String genSparqlGetNodeGroupMetadata(){
		String retval = "PREFIX XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
						"PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"SELECT distinct ?ID ?comments ?creationDate ?creator " +
						"FROM <" + this.dataGraph + "> WHERE { " +
						"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
						"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
						"optional { ?PrefabNodeGroup prefabNodeGroup:comments ?comments . } " +
				   		"optional { ?PrefabNodeGroup prefabNodeGroup:creationDate ?creationDate . } " +
				   		"optional { ?PrefabNodeGroup prefabNodeGroup:creator ?creator . } " +
						"}";
		return retval;
	}
	
	private String genSparqlGetStringBlobMetadata(StringBlobTypes blobType){
		String retval = "PREFIX XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
						"PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"SELECT distinct ?ID ?comments ?creationDate ?creator " +
						"FROM <" + this.dataGraph + "> WHERE { " +
						"?blob a prefabNodeGroup:" + blobType.toString() + " . " +
						"?blob prefabNodeGroup:id ?ID . " +
						"optional { ?blob prefabNodeGroup:comments ?comments . } " +
				   		"optional { ?blob prefabNodeGroup:creationDate ?creationDate . } " +
				   		"optional { ?blob prefabNodeGroup:creator ?creator . } " +
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
		String rdf10ValuesClause = "VALUES ?id { \"" + id + "\" \"" + id + "\" } . ";

		String ret = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
				"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"WITH <" + this.dataGraph + "> DELETE { " +
                "  ?stringChunkObj ?scPred ?scObj . " +
				"  ?PrefabNodeGroup ?pred ?obj." +
				"} WHERE { " +
				"  ?PrefabNodeGroup prefabNodeGroup:id ?id . " + 
				   rdf10ValuesClause +
                "  ?PrefabNodeGroup prefabNodeGroup:stringChunk ?stringChunkObj . " +
                "  ?stringChunkObj ?scPred ?scObj . " +
				"  ?PrefabNodeGroup ?pred ?obj." +
				"}";
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
	private ArrayList<String> genSparqlInsertStringBlob(String blob, StringBlobTypes blobType, String id, String comments, String creator) throws Exception {
		final int SPLIT = 20000;
		
		// extract the connJson
		
		ArrayList<String> ret = new ArrayList<String>();
		String uri = "generateSparqlInsert:semtk_blob_" +  UUID.randomUUID().toString();
		String query = "PREFIX prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
				"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"PREFIX generateSparqlInsert:<http://semtk.research.ge.com/generated#> " +
				"PREFIX XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
				"INSERT { GRAPH <" + this.dataGraph + "> { " +
			    "	   ?BlobString__0 a prefabNodeGroup:" + blobType + " . " +
			    "	   ?BlobString__0 prefabNodeGroup:id \""            + id +   "\" ." +
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
	 * @param chunk
	 * @param split
	 * @return
	 * @throws Exception
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
