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

public class SparqlQueries {
	
	// headers to be used for the insertion of new nodegroups into the system.
	public static String insertHeaders[] = {"id", "nodegroup", "comments", "creator", "creationDate", "connectionAlias", "domain", "dsDataset", "dsKsURL", "dsURL", "originalServerType"};

	public static String getHeaderRow(){
		StringBuilder sb = new StringBuilder();
		
		// get all the headers
		int counter = 0;	// we need to keep track of the current postion. it makes the commas easier. 
		for(String curr : SparqlQueries.insertHeaders){
			sb.append(curr);
			counter++;
			if(counter < SparqlQueries.insertHeaders.length){  // add a comma, if we need one.
				sb.append(",");
			}
		}
		sb.append("\n"); 	// add a new line to the end of the input. 
		
		return sb.toString();
	}
	
	
	// get sparql queries for getting the needed info. 
	public static String getNodeGroupByID(String id){
		String retval = "prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"select distinct ?ID ?NodeGroup ?comments where { " +
						"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
						"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
						"FILTER regex(?ID, \"" + id + "\") . " +
						"?PrefabNodeGroup prefabNodeGroup:NodeGroup ?NodeGroup . " +
		   				"?PrefabNodeGroup prefabNodeGroup:comments ?comments . " +
		   				"}";		
		return retval;
	}
	
	public static String getNodeGroupByConnectionAlias(String connectionAlias){
		String retval = "prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"select distinct ?ID ?NodeGroup ?comments where { " +
						"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
						"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
						"?PrefabNodeGroup prefabNodeGroup:NodeGroup ?NodeGroup . " +
						"?PrefabNodeGroup prefabNodeGroup:comments ?comments . " +
						"?PrefabNodeGroup prefabNodeGroup:originalConnection ?SemTkConnection. " +
						"?SemTkConnection prefabNodeGroup:connectionAlias  . " +
						"FILTER regex(?connectionAlias, \"" + connectionAlias + "\") . " +
						"}";
		return retval;
	}
	
	public static String getConnectionInfo(){
		String retval = "prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"select distinct ?connectionAlias ?domain ?dsDataset ?dsKsURL ?dsURL ?originalServerType where { " +
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
	
	public static String getFullNodeGroupList(){
		String retval = "prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"select distinct ?ID ?NodeGroup ?comments where { " +
						"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
						"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
						"?PrefabNodeGroup prefabNodeGroup:NodeGroup ?NodeGroup . " +
						"?PrefabNodeGroup prefabNodeGroup:comments ?comments . " +
						"}";
		return retval;
	}

	public static String getNodeGroupIdAndCommentList(){
		String retval = "prefix prefabNodeGroup:<http://research.ge.com/semtk/prefabNodeGroup#> " +
						"select distinct ?ID ?comments where { " +
						"?PrefabNodeGroup a prefabNodeGroup:PrefabNodeGroup. " +
						"?PrefabNodeGroup prefabNodeGroup:ID ?ID . " +
						"?PrefabNodeGroup prefabNodeGroup:NodeGroup ?NodeGroup . " +
						"?PrefabNodeGroup prefabNodeGroup:comments ?comments . " +
						"}";
		return retval;
	}

}
