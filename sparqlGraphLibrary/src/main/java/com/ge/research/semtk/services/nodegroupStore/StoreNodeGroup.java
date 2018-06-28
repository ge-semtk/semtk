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

import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintManager;
import com.ge.research.semtk.load.client.IngestorClientConfig;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.RecordProcessResults;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

public class StoreNodeGroup {

	
	public static boolean storeNodeGroup(JSONObject sgJsonJson, JSONObject connJson, String id, String comments, String creator, SparqlConnection overrideConn, String ingestorLocation, String ingestorProtocol, String ingestorPort) throws Exception{
		boolean retval = true;
		
		// generate creation date string, format MM/DD/YYYY
		String creationDateString = Utility.getSPARQLCurrentDateString(); 
		
		// get everything we need to send. 
		String data = SparqlQueries.getHeaderRow() + getInsertRow(sgJsonJson, connJson, id, comments, creator, creationDateString);  

//		LocalLogger.logToStdErr(":: csv data output ::");	
//		LocalLogger.logToStdErr(data);
		
		// should add better error handling here. 
		try{
			
			// some diagnostic output 
			LocalLogger.logToStdErr("attempting to write a new nodegroup to the ingestor at " + ingestorLocation + " using the protocol " + ingestorProtocol);
			
			// create the rest client
			IngestorClientConfig icc = new IngestorClientConfig(ingestorProtocol, ingestorLocation, Integer.parseInt(ingestorPort));
			IngestorRestClient irc   = new IngestorRestClient(icc);
			
			// get store.json
			String templateStr = Utility.getResourceAsString(icc, "/nodegroups/store.json");
			
			// ingest
			irc.execIngestionFromCsv(templateStr, data, overrideConn.toString());
			RecordProcessResults tbl = irc.getLastResult();			
			LocalLogger.logToStdErr("does the return believes the run was a succes?" + tbl.getSuccess() );
			tbl.throwExceptionIfUnsuccessful();
			
		}
		catch(Exception eee){
			LocalLogger.logToStdErr(eee.getMessage());
			retval = false;		// set this to false. hopefully this functions as a response. 
		}
		
		return retval;
	}
	
	public static TableResultSet getStoredNodeGroupList(){
		TableResultSet retval = null;
		
		
		
		// send the results
		return retval;
	}
	
	public static TableResultSet getStoredNodeGroupListByConnection(String connectionAlias){
		TableResultSet retval = null;

		
		// send the results
		return retval;
	}
	
	public static TableResultSet getStoredNodeGroupListByID(String id){
		TableResultSet retval = null;
		
		
		
		// send the results
		return retval;
	}
	
	public static TableResultSet getStoredNodeGroupIdAndCommentList(){
		TableResultSet retval = null;
		
		
		
		
		return retval;
	}
	
	public static String getInsertRow(JSONObject ng, JSONObject connectionInfo, String id, String comments, String creator, String creationDateString) throws Exception{
		// the desired return format is this:
		// "id", "nodegroup", "comments", "creator", "creationDate", "connectionAlias", "domain", "dsDataset", "dsKsURL", "dsURL", "originalServerType"

		// see which strings we can get from the connectionInfo.
		SparqlConnection tempConn = new SparqlConnection();
		tempConn.fromJson(connectionInfo);
		
		// get the Data and Knowledge Service URLS:
		// the knowledge one can be blank if they are the same. in that case, make sure to use the same value for both, rather than leaving it up to guesswork.
		SparqlEndpointInterface sei = tempConn.getDefaultQueryInterface();
		String dsInfo = sei.getDataset();
		String ksInfo = ""; // tempConn.getDataSourceKnowledgeServiceURL();
		String datasourceURL = sei.getServerAndPort();
		String serverType = sei.getServerType();
		
		//if(ksInfo == null || ksInfo.length() == 0 || ksInfo.isEmpty()) { ksInfo = dsInfo; }
		
		String retval = id + ",\"" + escapeQuotes(ng.toJSONString()) 
				+ "\",\"" + escapeQuotes(comments) + "\",\"" + escapeQuotes(creator.trim()) + "\"," + creationDateString + "," + tempConn.getName() + "," + tempConn.getDomain() + "," + dsInfo + "," +
				ksInfo  + "," + datasourceURL + "," + serverType;
		
		return retval;	// ready to ship it all out. 
	}
	
	public static String escapeQuotes(String quotedString){
		
		String retval = quotedString.replaceAll("\"", "\"\"");  // replace the quotes.
		retval = retval.replace("\\\"\"", "\\\\\"\"");  // trying to avoid orphaned quotes.this leads to issues in the csv interpretter.
		
		//String retval = StringEscapeUtils.escapeCsv(quotedString);  // using the apache lib. might have to pair this with unescaping later, using the same lib...
	
		return retval;
	}
	
	public static Table getConstrainedItems(JSONObject jsonEncodedNodeGroup) throws Exception{
		Table retval = null;		// the thing we are shipping back out.
		
		// process encoded node group
		NodeGroup temp = new NodeGroup();
		temp.addJsonEncodedNodeGroup(jsonEncodedNodeGroup);
	
		
		// get the constrained values from the NodeGroup.
		RuntimeConstraintManager rtci = new RuntimeConstraintManager(temp);
		
		retval = rtci.getConstrainedItemsDescription();
		
		return retval;					/// send it. 
	}
}
