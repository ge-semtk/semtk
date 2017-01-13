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


package com.ge.research.semtk.load;

import java.util.ArrayList;
import java.util.HashMap;

import javax.print.attribute.HashAttributeSet;

import org.json.simple.JSONObject;

import com.ge.research.semtk.belmont.BelmontUtil;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.DataSetExhaustedException;
import com.ge.research.semtk.load.utility.DataToModelTransformer;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;

/**
 * Imports a dataset into a triple store.
 **/
public class DataLoader {
	// actually orchestrates the loading of data from a dataset based on a template.

	NodeGroup master = null;
	ArrayList<NodeGroup> subGraphsToLoad = new ArrayList<NodeGroup>();     // stores the subgraphs to be loaded.  
	SparqlEndpointInterface endpoint = null;
	DataToModelTransformer dttmf = null; 
	int batchSize = 1;	// maximum batch size for insertion to the triple store
	String username = null;
	String password = null;
	OntologyInfo oInfo = null;
	
	public final static String FAILURE_CAUSE_COLUMN_NAME = "Failure Cause";
	public final static String FAILURE_RECORD_COLUMN_NAME = "Failure Record Number";

	int totalRecordsProcessed = 0;
	
	public DataLoader(){
		// default and does nothing special 
	}
	
	public DataLoader(SparqlGraphJson sgJson, int bSize) throws Exception {
		// take a json object which encodes the node group, the import spec and the connection in one package. 
		
		this.batchSize = bSize;
		
		this.endpoint = sgJson.getDataInterface();
		System.out.println("Dataset graph name: " + getDatasetGraphName());
		
		this.master = sgJson.getNodeGroupCopy();
		
		// initialize oInfo object
		this.oInfo = sgJson.getOntologyInfo();
				
		this.dttmf = new DataToModelTransformer(sgJson, this.batchSize);		
	}
	

	public DataLoader(SparqlGraphJson sgJson, int bSize, Dataset ds, String username, String password) throws Exception{
		this(sgJson, bSize);
		this.setCredentials(username, password);
		this.setDataset(ds);
		this.validateColumns(ds);
		
	}
	
	public DataLoader(JSONObject json, int bSize) throws Exception {
		this(new SparqlGraphJson(json), bSize);
	}
	
	public DataLoader(JSONObject json, int bSize, Dataset ds, String username, String password) throws Exception{
		this(new SparqlGraphJson(json), bSize, ds, username, password);
	}
	
	public String getDatasetGraphName(){
		return this.endpoint.getDataset();
	}
	
	public int getTotalRecordsProcessed(){
		return this.totalRecordsProcessed;
	}
	
	private void validateColumns(Dataset ds) throws Exception {
		// validate that the columns specified in the template are present in the dataset
		String[] colNamesToIngest = dttmf.getImportColNames();   // col names from JSON		
		ArrayList<String> colNamesInDataset = ds.getColumnNamesinOrder();
		for(String c: colNamesToIngest){
			if(!colNamesInDataset.contains(c)){
				ds.close();  // close the dataset (e.g. if Oracle, will close the connection)
				throw new Exception("Column '" + c + "' not found in dataset. Available columns are: " + colNamesInDataset.toString());
			}
		}
	}
	public void setCredentials(String user, String pass){
		this.endpoint.setUserAndPassword(user, pass);
	}
	
	public void setDataset(Dataset ds) throws Exception{
		if(this.dttmf == null){
			throw new Exception("There was no DAta to model TRansform initialized when setting dataset.");
		}
		else{
			this.dttmf.setDataset(ds);
		}
		
	}
	
	public void setBatchSize(int bSize){
		// set the maximum batch size for insertion to the triple store
		this.batchSize = bSize;
	}	
	
	public void setRetrivalBatchSize(int rBatchSize){
		this.dttmf.setBatchSize(rBatchSize);
	}
	
	
	public int importData(Boolean checkFirst) throws Exception{

		// check the nodegroup for consistency before continuing.
		
		System.err.println("about to validate against model.");
		this.master.validateAgainstModel(this.oInfo);
		System.err.println("validation completed.");
		
		Boolean dataCheckSucceeded = true;
		this.totalRecordsProcessed = 0;	// reset the counter.
		
		// preflight the data to make sure everything seems okay before a load.
		int totalPreflightRecordsChecked = 0;
		if(checkFirst){
			// try structuring around model but do not load. 	
			while(true){
				try{					
					this.dttmf.getNext();
					totalPreflightRecordsChecked++;
				}
				catch(DataSetExhaustedException e){
					break;
				}
			}
			// inspect the transformer to determine if the checks succeeded
			Table errorReport = this.dttmf.getErrorReport();
			if(errorReport.getRows().size() != 0){
				dataCheckSucceeded = false;
			}
		}
				
		if (dataCheckSucceeded) {
			this.dttmf.resetDataSet();
			// orchestrate the retrieval of new nodegroups and the flushing of
			// that data
			System.out.print("Records processed:");
			long timeMillis = System.currentTimeMillis();  // use this to report # recs loaded every X sec
			while (true) {
				ArrayList<NodeGroup> curr = new ArrayList<NodeGroup>();
				try {
					curr = this.dttmf.getNextBatch();
				} catch (DataSetExhaustedException e) {
					// no more data to get.
					break;
				}
				for (NodeGroup n : curr) {
					// add the returned ones to load list
					this.subGraphsToLoad.add(n);
					this.totalRecordsProcessed += 1;
				}
				// if we are at the max batch size, flush the values to the store
				if (this.subGraphsToLoad.size() >= this.batchSize) {
					this.insertToTripleStore(); // write them out
					this.subGraphsToLoad.clear(); // clear the collection
				}
				if(System.currentTimeMillis() - timeMillis > 1000){  // report # records loaded every 1 second
					System.out.print("..." + this.totalRecordsProcessed);
					timeMillis = System.currentTimeMillis();
				}
			}

			// check for remaining values to flush:
			if (this.subGraphsToLoad.size() > 0) {
				this.insertToTripleStore();
				this.subGraphsToLoad.clear();// cleared for consistency's sake,
												// even though we are done
			}
		}
		System.out.println("..." + this.totalRecordsProcessed + "(DONE)");
		this.dttmf.closeDataSet();			// close all connections and clean up
		return this.totalRecordsProcessed;  // report.
	}
	public void insertToTripleStore() throws Exception{
		// take the values from the current collection of node groups and then send them off to the store. 
	
		HashMap<String, String> prefixHash = new HashMap<String, String>();
		String prefixes = "";
		String totalInsertHead = "";
		String totalInsertWhere = "";
		
		int seqNum = 0;
		NodeGroup lastNg = null;
		for(NodeGroup ng : this.subGraphsToLoad){
			
			// we are going to use one prefix hash and add to is as needed. 
			
			if(seqNum == 0){
				prefixHash = ng.getPrefixHash();
			}
			else{
				ng.rebuildPrefixHash(prefixHash);	// add new elements, as needed.
				prefixHash = ng.getPrefixHash(); 	// get back a copy. 
			}
			
			String seq = "__" + seqNum;
			
			totalInsertHead  += ng.getInsertLeader(seq, this.oInfo);
			totalInsertWhere += ng.getInsertWhereBody(seq, this.oInfo);
			
			seqNum += 1;
			lastNg = ng;
		}
		// make the query and send it off.
		// NOTE: the last NodeGroup should have all the prefixes of all the needed groups.
		//       this way, we only need to get it's prefixes. 
		
		String query =  lastNg.generateSparqlPrefix() + "Insert { " + totalInsertHead + " } where { " + totalInsertWhere + " } "; 

//		// some diagnostic output:
//		System.err.println("Insert generated : ");
//		System.err.println(query);
		
		this.endpoint.executeQuery(query, SparqlResultTypes.CONFIRM);
	}
	
	/**
	 * Returns a table containing the failed data rows, along with failure cause and row number.
	 */
	public Table getLoadingErrorReport(){
		return this.dttmf.getErrorReport();
	}
	
	/**
	 * Returns an error report giving the row number and failure cause for each failure.
	 */
	public String getLoadingErrorReportBrief(){
		String s = "";
		Table errorReport = this.dttmf.getErrorReport();
		int failureCauseIndex = errorReport.getColumnIndex(FAILURE_CAUSE_COLUMN_NAME);
		int failureRowIndex = errorReport.getColumnIndex(FAILURE_RECORD_COLUMN_NAME);
		ArrayList<ArrayList<String>> rows = errorReport.getRows();
		for(ArrayList<String> row:rows){
			s += "Error in row " + row.get(failureRowIndex) + ": " + row.get(failureCauseIndex) + "\n";
		}
		return s;
	}
}
