/**
 ** Copyright 2023 General Electric Company
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
package com.ge.research.semtk.validate;

import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlToXLib.SparqlToXLibUtil;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Delete SHACL-identified instances from a knowledge graph (e.g. for garbage collection)
 */
public class ShaclBasedGarbageCollector {

	public final static String HEADER_DELETED_INSTANCES = "Deleted Instances";
	
	/**
	 * Run the garbage collection routine
	 * Iterates until no further deletions are identified by SHACL.
	 * 
	 * @param shaclTtl SHACL shapes producing violations to garbage collect
	 * @param conn connection containing model/data
	 * @return table with single column containing instances deleted
	 */
	public Table run(String shaclTtl, SparqlConnection conn) throws Exception{
		return run(shaclTtl, conn, true);
	}
	
	/**
	 * Run the garbage collection routine
	 *
	 * @param shaclTtl SHACL shapes producing violations to garbage collect
	 * @param conn connection containing model/data
	 * @param repeat true to iterate repeatedly until no further deletions are identified by SHACL
	 * @return table with single column containing instances deleted
	 */
	public Table run(String shaclTtl, SparqlConnection conn, boolean repeat) throws Exception{

		Table retTable = new Table(new String[]{HEADER_DELETED_INSTANCES}, new String[] {"String"});
		
		// for now, only supports a single data interface
		if(conn.getDataInterfaceCount() > 1) {
			throw new Exception("Connection has multiple data interfaces: this is not yet supported");
		}
		SparqlEndpointInterface dataSei = conn.getDataInterface(0);
		
		// run multiple iterations (deletes in Round 1 may uncover new deletes for Round 2)
		LocalLogger.logToStdOut("ShaclBasedGarbageCollector: " + dataSei.getNumTriples() + " triples in graph (start)");
		while(true) {
			
			// identify URIs to delete (SHACL focus nodes)
			ShaclExecutor shaclExecutor = new ShaclExecutor(shaclTtl, conn);
			JSONArray shaclResults = (JSONArray)(shaclExecutor.getResults()).get(ShaclExecutor.JSON_KEY_ENTRIES); // the SHACL output
			ArrayList<String> urisToDelete = new ArrayList<String>();
			for(int i = 0; i < shaclResults.size(); i++) {
				String focusNodeUri = (String)((JSONObject)shaclResults.get(i)).get(ShaclExecutor.JSON_KEY_FOCUSNODE); // get focus node
				urisToDelete.add(focusNodeUri);
				retTable.addRow(new String[]{focusNodeUri});
				LocalLogger.logToStdOut("ShaclBasedGarbageCollector: delete " + focusNodeUri);
			}
			
			// if no further URIs to delete, stop iterating
			if(urisToDelete.size() == 0) {	
				break;
			}
			
			// perform delete
			String deleteQuery = SparqlToXLibUtil.generateDeleteUris(conn, urisToDelete);
			dataSei.executeQueryAndConfirm(deleteQuery);
			LocalLogger.logToStdOut("ShaclBasedGarbageCollector: " + dataSei.getNumTriples() + " triples in graph");
			
			// if not repeating, then break
			if(!repeat) {	
				break;
			}
		}
		LocalLogger.logToStdOut("ShaclBasedGarbageCollector: " + dataSei.getNumTriples() + " triples in graph (final).  Deleted " + retTable.getNumRows() + " instances:\n" + retTable.toCSVString());
		return retTable;
	}

}
