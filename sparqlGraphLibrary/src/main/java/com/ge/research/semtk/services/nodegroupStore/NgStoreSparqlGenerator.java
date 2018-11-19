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

import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.Utility;

public class NgStoreSparqlGenerator {
	
	private String dataGraph = null;
	
	public NgStoreSparqlGenerator(String dataGraph) {
		this.dataGraph = dataGraph;
	}
	
	
	// get sparql queries for getting the needed info. 
	public ArrayList<String> getNodeGroupByID(String id){
		ArrayList<String> ret = new ArrayList<String>();
		String query  = "prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"select distinct ?ID ?NodeGroup ?comments" +
						"from <" + this.dataGraph + "> where { " +
						"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
						"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
						"VALUES ?ID {\"" + id + "\"^^<http://www.w3.org/2001/XMLSchema#string>} . " +
						"?PrefabNodeGroup prefabNodeGroup:NodeGroup ?NodeGroup . " +
		   				"optional { ?PrefabNodeGroup prefabNodeGroup:comments ?comments . } " +
		   				"}";		
		ret.add(query);
		
		query = "prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
				"select distinct ?NodeGroup ?counter where { " +
				"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
				"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
				"VALUES ?ID {\"" + id + "\"^^<http://www.w3.org/2001/XMLSchema#string>} . " +
				
			    "?PrefabNodeGroup prefabNodeGroup:stringChunk ?SemTkStringChunk . " +
			    "      ?SemTkStringChunk prefabNodeGroup:counter ?counter ." +
				"      ?SemTkStringChunk prefabNodeGroup:chunk ?NodeGroup ."  +
   				"} ORDER BY ?counter";
		
		ret.add(query);
		
		return ret;
	}
	
	public String getNodeGroupByConnectionAlias(String connectionAlias){
		String retval = "prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"select distinct ?ID ?NodeGroup ?comments " +
						"from <" + this.dataGraph + "> where { " +
						"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
						"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
						"?PrefabNodeGroup prefabNodeGroup:NodeGroup ?NodeGroup . " +
						"optional { ?PrefabNodeGroup prefabNodeGroup:comments ?comments . } " +
						"?PrefabNodeGroup prefabNodeGroup:originalConnection ?SemTkConnection. " +
						"?SemTkConnection prefabNodeGroup:connectionAlias  . " +
						"VALUES ?connectionAlias {\"" + connectionAlias + "\"^^<http://www.w3.org/2001/XMLSchema#string>} . " +
						"}";
		return retval;
	}
	
	public String getConnectionInfo(){
		String retval = "prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"select distinct ?connectionAlias ?domain ?dsDataset ?dsKsURL ?dsURL ?originalServerType " +
						"from <" + this.dataGraph + "> where { " +
						"?SemTkConnection a prefabNodeGroup:SemTkConnection. " +
						"?SemTkConnection prefabNodeGroup:connectionAlias ?connectionAlias . " +
						"?SemTkConnection prefabNodeGroup:domain ?domain . " +
						"?SemTkConnection prefabNodeGroup:dsDataset ?dsDataset . " +
						"?SemTkConnection prefabNodeGroup:dsKsURL ?dsKsURL . " +
						"?SemTkConnection prefabNodeGroup:dsURL ?dsURL . " +
						"?SemTkConnection prefabNodeGroup:originalServerType ?originalServerType . " +
						"}";
		return retval;	
	}
	
	public String getFullNodeGroupList(){
		String retval = "prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"select distinct ?ID ?NodeGroup ?comments" +
						"from <" + this.dataGraph + "> where { " +
						"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
						"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
						"?PrefabNodeGroup prefabNodeGroup:NodeGroup ?NodeGroup . " +
						"optional { ?PrefabNodeGroup prefabNodeGroup:comments ?comments . } " +
						"}";
		return retval;
	}

	public String getNodeGroupIdAndCommentList(){
		String retval = "prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"select distinct ?ID ?comments " +
						"from <" + this.dataGraph + "> where { " +
						"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
						"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
						"?PrefabNodeGroup prefabNodeGroup:NodeGroup ?NodeGroup . " +
						"optional { ?PrefabNodeGroup prefabNodeGroup:comments ?comments . } " +
						"}";
		return retval;
	}
	
	public String getNodeGroupMetadata(){
		String retval = "prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
						"prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"select distinct ?ID ?comments ?creationDate ?creator " +
						"from <" + this.dataGraph + "> where { " +
						"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
						"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
						"optional { ?PrefabNodeGroup prefabNodeGroup:comments ?comments . } " +
				   		"optional { ?PrefabNodeGroup prefabNodeGroup:creationDate ?creationDate . } " +
				   		"optional { ?PrefabNodeGroup prefabNodeGroup:creator ?creator . } " +
						"}";
		return retval;
	}

	public  String deleteNodeGroup(String jobId) {
		String ret = "prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
				"Delete " + 
				"{" +
				"  ?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup." +
				"  ?PrefabNodeGroup prefabNodeGroup:ID \"" + jobId + "\"^^<http://www.w3.org/2001/XMLSchema#string> ." +
				"  ?PrefabNodeGroup prefabNodeGroup:NodeGroup ?NodeGroup ." +
				"  ?PrefabNodeGroup prefabNodeGroup:comments ?comments . " +
				"  ?PrefabNodeGroup prefabNodeGroup:creator ?creator . " +
				"  ?PrefabNodeGroup prefabNodeGroup:creationDate ?creationDate . " +
				"      ?PrefabNodeGroup prefabNodeGroup:stringChunk ?SemTkStringChunk__0. " +
				"      ?SemTkStringChunk__0 rdf:type prefabNodeGroup:StringChunk . "    + 
				"      ?SemTkStringChunk__0 ?pred ?predVal."     +
				"}" + 
				"from <" + this.dataGraph + "> where { " +
				"  ?PrefabNodeGroup prefabNodeGroup:ID \"" + jobId  +"\"^^<http://www.w3.org/2001/XMLSchema#string> ." +
				"  ?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
				"  ?PrefabNodeGroup prefabNodeGroup:NodeGroup ?NodeGroup . " +
				"  optional { ?PrefabNodeGroup prefabNodeGroup:comments ?comments . } " +
				"  optional { ?PrefabNodeGroup prefabNodeGroup:creator ?creator . } " +
				"  optional { ?PrefabNodeGroup prefabNodeGroup:creationDate ?creationDate . } " +
				"  optional { " +
				"      ?PrefabNodeGroup prefabNodeGroup:stringChunk ?SemTkStringChunk__0. " +
				"      ?SemTkStringChunk__0 rdf:type prefabNodeGroup:StringChunk . " + 
				"      optional { ?SemTkStringChunk__0 ?pred ?predVal.}"     +
				"  }" +
				"}";
		return ret;
	}
	
	public ArrayList<String> insertNodeGroup(JSONObject sgJsonJson, JSONObject connJson, String id, String comments, String creator) throws Exception {
		final int SPLIT = 25000;
		
		// extract the connJson
		SparqlConnection conn = new SparqlConnection();
		conn.fromJson(connJson);
		SparqlEndpointInterface connSei = conn.getDefaultQueryInterface();

		String ngStr = legalizeSparqlInputString(sgJsonJson.toJSONString());
		String [] chunks = getNextChunk(ngStr, SPLIT);
		
		ArrayList<String> ret = new ArrayList<String>();
		String ngURI = "generateSparqlInsert:semtk_ng_" +  UUID.randomUUID().toString();
		String query = "prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
				"prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"prefix generateSparqlInsert:<belmont/generateSparqlInsert#> " +
				"prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
				"INSERT { GRAPH <" + this.dataGraph + "> { " +
				"      ?SemTkConnection__0 a prefabNodeGroup:SemTkConnection . " +
				"      ?SemTkConnection__0 prefabNodeGroup:connectionAlias \""    + conn.getName() +"\"^^XMLSchema:string ." +
				"      ?SemTkConnection__0 prefabNodeGroup:domain \""             + conn.getDomain() +"\"^^XMLSchema:string ." +
				"      ?SemTkConnection__0 prefabNodeGroup:dsDataset \""          + connSei.getGraph() +"\"^^XMLSchema:string ." +
				"      ?SemTkConnection__0 prefabNodeGroup:dsURL \""              + connSei.getServerAndPort() +"\"^^XMLSchema:string ." +
				"      ?SemTkConnection__0 prefabNodeGroup:originalServerType \"" + connSei.getServerType() +"\"^^XMLSchema:string ." +

			    "	   ?PrefabNodeGroup__0 a prefabNodeGroup:PrefabNodeGroup . " +
			    "	   ?PrefabNodeGroup__0 prefabNodeGroup:ID \""            + id + "\"^^XMLSchema:string . " +
			    "	   ?PrefabNodeGroup__0 prefabNodeGroup:NodeGroup \""     + chunks[0] + "\"^^XMLSchema:string . " +
			    "	   ?PrefabNodeGroup__0 prefabNodeGroup:comments \""      + legalizeSparqlInputString(comments) + "\"^^XMLSchema:string . " +
			    "	   ?PrefabNodeGroup__0 prefabNodeGroup:creationDate \""  + Utility.getSPARQLCurrentDateString() + "\"^^XMLSchema:date . " +
			    "	   ?PrefabNodeGroup__0 prefabNodeGroup:creator \""       + creator.trim() + "\"^^XMLSchema:string . " +
			    "	   ?PrefabNodeGroup__0 prefabNodeGroup:originalConnection ?SemTkConnection__0 . " +
			    "} } " +
			    "WHERE {     " +
			    "      BIND (generateSparqlInsert:semtk_conn_" + UUID.randomUUID().toString() + " AS ?SemTkConnection__0)." +
			    "      BIND (" + ngURI + " AS ?PrefabNodeGroup__0)." +
			    "  }";
		ret.add(query);
		
		int i=0;
		while (chunks[1].length() > 0) {
			chunks = getNextChunk(chunks[1], SPLIT);
			
			query = "prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
					"prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
					"prefix generateSparqlInsert:<belmont/generateSparqlInsert#> " +
					"prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
					"INSERT { GRAPH <" + this.dataGraph + "> { " +
					"      ?SemTkStringChunk__0 a prefabNodeGroup:StringChunk . " +
					"      ?SemTkStringChunk__0 prefabNodeGroup:counter \""    + i++ +"\"^^XMLSchema:int ." +
					"      ?SemTkStringChunk__0 prefabNodeGroup:chunk \""    + chunks[0] +"\"^^XMLSchema:string ." +

				    "	   ?PrefabNodeGroup__0 prefabNodeGroup:stringChunk ?SemTkStringChunk__0 . " +
				    "} } " +
				    "WHERE {     " +
				    "      BIND (generateSparqlInsert:semtk_cat_" + UUID.randomUUID().toString() + " AS ?SemTkStringChunk__0)." +
				    "      BIND (" + ngURI + " AS ?PrefabNodeGroup__0)." +
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

}
