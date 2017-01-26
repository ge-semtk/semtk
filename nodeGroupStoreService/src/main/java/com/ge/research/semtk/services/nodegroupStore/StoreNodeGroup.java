package com.ge.research.semtk.services.nodegroupStore;

import java.util.ArrayList;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstrainedItems;
import com.ge.research.semtk.load.client.IngestorClientConfig;
import com.ge.research.semtk.load.client.IngestorRestClient;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.ingestion.IngestionFromStringsRequestBody;
import com.ge.research.semtk.sparqlX.SparqlConnection;

public class StoreNodeGroup {

	
	public static boolean storeNodeGroup(JSONObject ng, JSONObject connectionInfo, String id, String comments, String insertTemplate, String ingestorLocation, String ingestorProtocol, String ingestorPort) throws Exception{
		boolean retval = true;
		
		// get everything we need to send. 
		String data = SparqlQueries.getHeaderRow() + getInsertRow(ng, connectionInfo, id, comments); 

	//	System.err.println(":: csv data output ::");
	//	System.err.println(data);
		
		// should add better error handling here. 
		try{
			
			// some diagnostic output 
			System.err.println("attempting to write a new nodegroup to the ingestor at " + ingestorLocation + " using the protocol " + ingestorProtocol);
			
			// create the rest client
			IngestorClientConfig icc = new IngestorClientConfig(ingestorProtocol, ingestorLocation, Integer.parseInt(ingestorPort));
			IngestorRestClient irc   = new IngestorRestClient(icc);
			
			irc.execIngestionFromCsv(insertTemplate, data);
			TableResultSet tbl = irc.getLastResult();			
			System.err.println("does the return believes the run was a succes?" + tbl.getSuccess() );
			tbl.throwExceptionIfUnsuccessful();
			
		}
		catch(Exception eee){
			System.err.println(eee.getMessage());
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
	
	public static String getInsertRow(JSONObject ng, JSONObject connectionInfo, String id, String comments) throws Exception{
		// the desired return format is this:
		// "id", "nodegroup", "comments", "connectionAlias", "domain", "dsDataset", "dsKsURL", "dsURL", "originalServerType"

		// see which strings we can get from the connectionInfo.
		SparqlConnection tempConn = new SparqlConnection();
		tempConn.fromJson(connectionInfo);
		
		// get the Data and Knowledge Service URLS:
		// the knowledge one can be blank if they are the same. in that case, make sure to use the same value for both, rather than leaving it up to guesswork.
		String dsInfo = tempConn.getDataSourceDataset();
		String ksInfo = tempConn.getDataSourceKnowledgeServiceURL();
		
		if(ksInfo == null || ksInfo.length() == 0 || ksInfo.isEmpty()) { ksInfo = dsInfo; }
		
		String retval = id + ",\"" + escapeQuotes(ng.toJSONString()) 
				+ "\",\"" + escapeQuotes(comments) + "\"," + tempConn.getConnectionName() + "," + tempConn.getDomain() + "," + dsInfo + "," +
				ksInfo  + "," + tempConn.getDataSourceURL() + "," + tempConn.getServerType();
		
		return retval;	// ready to ship it all out. 
	}
	
	public static String escapeQuotes(String quotedString){
		String retval = quotedString.replaceAll("\"", "\"\"");  // replace the quotes.
		//String retval = StringEscapeUtils.escapeCsv(quotedString);  // using the apache lib. might have to pair this with unescaping later, using the same lib...
		
		return retval;
	}
	
	public static Table getConstrainedItems(JSONObject jsonEncodedNodeGroup) throws Exception{
		Table retval = null;		// the thing we are shipping back out.
		
		// process encoded node group
		NodeGroup temp = new NodeGroup();
		temp.addJsonEncodedNodeGroup(jsonEncodedNodeGroup);
	
		
		// get the constrained values from the NodeGroup.
		RuntimeConstrainedItems rtci = new RuntimeConstrainedItems(temp);
		
		ArrayList<String> items = rtci.getConstrainedItemIds();
		
		// get the associated types for each.
		
		ArrayList<ArrayList<String>> itemInfo = new ArrayList<ArrayList<String>>();
		for(String currItem : items){
			ArrayList<String> currentItemInfo = new ArrayList<String>();
			
			currentItemInfo.add(currItem);
			currentItemInfo.add(rtci.getItemType(currItem));
			currentItemInfo.add(rtci.getValueType(currItem));
			
			itemInfo.add(currentItemInfo);
		}
		String cols[] = {"valueId", "itemType", "valueType"};
		String type[] = {"string", "string", "string"};
		
		retval = new Table(cols, type, itemInfo);
		
		return retval;					/// send it. 
	}
}
